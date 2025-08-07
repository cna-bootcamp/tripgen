package com.unicorn.tripgen.location.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.tripgen.common.exception.BusinessException;
import com.unicorn.tripgen.common.exception.ErrorCodes;
import com.unicorn.tripgen.common.exception.NotFoundException;
import com.unicorn.tripgen.location.client.GooglePlacesClient;
import com.unicorn.tripgen.location.client.KakaoMapClient;
import com.unicorn.tripgen.location.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 외부 API 서비스 구현체
 * Google Places API, Kakao Map API 등을 통합 관리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalApiServiceImpl implements ExternalApiService {
    
    private final GooglePlacesClient googlePlacesClient;
    private final KakaoMapClient kakaoMapClient;
    private final ObjectMapper objectMapper;
    
    @Value("${external.api.google.places.api-key}")
    private String googleApiKey;
    
    @Value("${external.api.kakao.api-key}")
    private String kakaoApiKey;
    
    
    @Override
    public List<LocationSearchResponse.PlaceCard> searchLocationsByKeyword(SearchLocationRequest request) {
        log.info("Searching locations with external APIs: keyword={}", request.getKeyword());
        
        try {
            List<LocationSearchResponse.PlaceCard> results = new ArrayList<>();
            
            // Google API로 통일 (속도 및 평점 정보 제공)
            results.addAll(searchWithGooglePlaces(request));
            
            // 중복 제거
            results = deduplicateResults(results);
            
            log.info("External API search completed: {} results found", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("Error searching with external APIs", e);
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "외부 API 검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public List<LocationSearchResponse.PlaceCard> searchWithGooglePlaces(SearchLocationRequest request) {
        log.debug("Searching with Google Places API: {}", request.getKeyword());
        
        try {
            String location = request.getLatitude() + "," + request.getLongitude();
            
            Map<String, Object> response = googlePlacesClient.searchPlacesByText(
                request.getKeyword(),
                location,
                request.getRadius(),
                mapCategoryToGoogleType(request.getCategory()),
                request.getLanguage(),
                null, // fields parameter는 Text Search에서도 지원되지 않음
                googleApiKey
            );
            
            return parseGooglePlacesResponse(response, request);
            
        } catch (Exception e) {
            log.error("Error searching with Google Places API", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<LocationSearchResponse.PlaceCard> searchWithKakaoMap(SearchLocationRequest request) {
        log.debug("Searching with Kakao Map API: {}", request.getKeyword());
        
        try {
            String authorization = "KakaoAK " + kakaoApiKey;
            
            Map<String, Object> response = kakaoMapClient.searchPlacesByKeyword(
                authorization,
                request.getKeyword(),
                mapCategoryToKakaoType(request.getCategory()),
                request.getLongitude().doubleValue(),
                request.getLatitude().doubleValue(),
                request.getRadius(),
                null, // rect
                request.getPage(),
                request.getSize(),
                "accuracy" // sort
            );
            
            return parseKakaoMapResponse(response, request);
            
        } catch (Exception e) {
            log.error("Error searching with Kakao Map API", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<NearbyPlacesResponse.NearbyPlace> searchNearbyPlaces(NearbyPlacesRequest request) {
        log.info("Searching nearby places with external APIs: transport={}, timeRange={}min", 
                request.getTransportMode(), request.getTimeRange());
        
        try {
            List<NearbyPlacesResponse.NearbyPlace> results = new ArrayList<>();
            
            // Google Places로 주변 검색 (속도 및 일관성을 위해 구글 API로 통일)
            results.addAll(searchNearbyWithGoogle(request));
            results.sort(Comparator.comparing(NearbyPlacesResponse.NearbyPlace::getDistance));
            
            log.info("Nearby search completed: {} places found", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("Error searching nearby places with external APIs", e);
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "주변 장소 검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public List<NearbyPlacesResponse.NearbyPlace> searchNearbyWithGoogle(NearbyPlacesRequest request) {
        log.debug("Searching nearby places with Google Places API");
        
        try {
            String location = request.getOrigin().getLatitude() + "," + request.getOrigin().getLongitude();
            Integer radius = calculateRadiusFromTimeRange(request.getTimeRange(), request.getTransportMode());
            String googleType = mapCategoryToGoogleType(request.getCategory());
            
            // API 호출 로깅 강화
            log.info("Google API 호출: location={}, radius={}m, type={}, category={}", 
                location, radius, googleType, request.getCategory());
            
            Map<String, Object> response = googlePlacesClient.searchNearbyPlaces(
                location,
                radius,
                googleType,
                null, // keyword
                request.getLanguage(),
                null, // fields parameter는 Nearby Search에서 지원되지 않음
                googleApiKey
            );
            
            // 응답 로깅
            log.debug("Google API 응답 상태: {}", response.get("status"));
            if (response.containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                log.info("Google API 원본 결과 수: {}", results.size());
            }
            
            List<NearbyPlacesResponse.NearbyPlace> parsedResults = parseGoogleNearbyResponse(response);
            
            // 요청한 카테고리로 필터링
            List<NearbyPlacesResponse.NearbyPlace> filteredResults = filterByRequestedCategory(
                parsedResults, request.getCategory());
            
            log.info("필터링 후 결과 수: {} -> {}", parsedResults.size(), filteredResults.size());
            
            // 결과가 부족한 경우 폴백 검색
            if (filteredResults.size() < 5 && !"all".equals(request.getCategory())) {
                log.info("결과 부족으로 키워드 기반 폴백 검색 실행");
                List<NearbyPlacesResponse.NearbyPlace> fallbackResults = searchWithKeywordFallback(
                    request, location, radius);
                filteredResults.addAll(fallbackResults);
            }
            
            return filteredResults;
            
        } catch (Exception e) {
            log.error("Error searching nearby with Google Places API", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<NearbyPlacesResponse.NearbyPlace> searchNearbyWithKakao(NearbyPlacesRequest request) {
        log.debug("Searching nearby places with Kakao Map API");
        
        try {
            String authorization = "KakaoAK " + kakaoApiKey;
            Integer radius = calculateRadiusFromTimeRange(request.getTimeRange(), request.getTransportMode());
            
            Map<String, Object> response = kakaoMapClient.searchPlacesByCategory(
                authorization,
                mapCategoryToKakaoType(request.getCategory()),
                request.getOrigin().getLongitude().doubleValue(),
                request.getOrigin().getLatitude().doubleValue(),
                radius,
                null, // rect
                request.getPage(),
                request.getSize(),
                "distance"
            );
            
            return parseKakaoNearbyResponse(response);
            
        } catch (Exception e) {
            log.error("Error searching nearby with Kakao Map API", e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public LocationDetailResponse getLocationDetail(String placeId, Boolean includeReviews, String language) {
        log.info("Getting location detail from external API: placeId={}", placeId);
        
        try {
            // 플레이스 ID 형식에 따라 API 선택
            if (isGooglePlaceId(placeId)) {
                return getGooglePlaceDetail(placeId, includeReviews, language);
            } else {
                // Kakao 또는 기타 소스의 경우 적절한 처리
                log.warn("Unsupported place ID format: {}", placeId);
                throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "지원하지 않는 장소 ID 형식입니다");
            }
            
        } catch (Exception e) {
            log.error("Error getting location detail from external API: {}", placeId, e);
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "외부 API에서 위치 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public LocationDetailResponse getGooglePlaceDetail(String placeId, Boolean includeReviews, String language) {
        log.debug("Getting Google Place detail: {}", placeId);
        
        try {
            String fields = buildGooglePlaceFields(includeReviews);
            
            Map<String, Object> response = googlePlacesClient.getPlaceDetails(
                placeId, fields, language, googleApiKey
            );
            
            return parseGooglePlaceDetail(response);
            
        } catch (Exception e) {
            log.error("Error getting Google Place detail: {}", placeId, e);
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "Google Places API에서 상세 정보 조회 중 오류가 발생했습니다");
        }
    }
    
    
    @Override
    public List<LocationSearchResponse.PlaceCard> getLocationAutocomplete(
            String input, String latitude, String longitude, String language, Integer limit) {
        
        log.debug("Getting location autocomplete: input={}", input);
        
        try {
            // 좌표가 있는 경우 국내외 판단하여 API 선택
            if (latitude != null && longitude != null) {
                double lat = Double.parseDouble(latitude);
                double lon = Double.parseDouble(longitude);
                boolean isKorea = isKoreanCoordinate(lat, lon);
                
                if (isKorea) {
                    // 국내: Kakao API 우선 시도
                    try {
                        return getKakaoAutocomplete(input, latitude, longitude, language, limit);
                    } catch (Exception e) {
                        log.warn("Kakao autocomplete failed, falling back to Google: {}", e.getMessage());
                        return getGoogleAutocomplete(input, latitude, longitude, language, limit);
                    }
                } else {
                    // 해외: Google API 사용
                    return getGoogleAutocomplete(input, latitude, longitude, language, limit);
                }
            } else {
                // 좌표 없는 경우 Google API 사용 (글로벌 검색)
                return getGoogleAutocomplete(input, null, null, language, limit);
            }
            
        } catch (Exception e) {
            log.error("Error getting location autocomplete", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 한국 좌표 범위 확인
     */
    private boolean isKoreanCoordinate(double lat, double lon) {
        // 대한민국 대략적인 범위
        // 위도: 33.0 ~ 38.9, 경도: 124.5 ~ 132.0
        return lat >= 33.0 && lat <= 38.9 && lon >= 124.5 && lon <= 132.0;
    }
    
    private List<LocationSearchResponse.PlaceCard> getGoogleAutocomplete(
            String input, String latitude, String longitude, String language, Integer limit) {
        String location = latitude != null && longitude != null ? latitude + "," + longitude : null;
        
        Map<String, Object> response = googlePlacesClient.getPlaceAutocomplete(
            input, location, 50000, null, null, language, googleApiKey
        );
        
        return parseGoogleAutocompleteResponse(response, limit);
    }
    
    private List<LocationSearchResponse.PlaceCard> getKakaoAutocomplete(
            String input, String latitude, String longitude, String language, Integer limit) {
        // 추후 Kakao Places API 자동완성 구현 필요
        // 현재는 Google로 폴백
        return getGoogleAutocomplete(input, latitude, longitude, language, limit);
    }
    
    
    
    private List<NearbyPlacesResponse.NearbyPlace> parseGoogleNearbyResponse(Map<String, Object> response) {
        List<NearbyPlacesResponse.NearbyPlace> places = new ArrayList<>();
        
        try {
            if (response.containsKey("results") && response.get("results") instanceof List) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                
                for (Map<String, Object> place : results) {
                    // opening_hours에서 open_now 정보 추출
                    Boolean isOpenNow = null;
                    if (place.containsKey("opening_hours")) {
                        Map<String, Object> openingHours = (Map<String, Object>) place.get("opening_hours");
                        if (openingHours != null && openingHours.containsKey("open_now")) {
                            isOpenNow = (Boolean) openingHours.get("open_now");
                        }
                    }
                    
                    NearbyPlacesResponse.NearbyPlace nearbyPlace = NearbyPlacesResponse.NearbyPlace.builder()
                        .placeId((String) place.get("place_id"))
                        .name((String) place.get("name"))
                        .rating(place.get("rating") != null ? 
                            BigDecimal.valueOf(((Number) place.get("rating")).doubleValue()) : null)
                        .reviewCount(place.get("user_ratings_total") != null ? 
                            ((Number) place.get("user_ratings_total")).intValue() : null)
                        .priceLevel(place.get("price_level") != null ? 
                            ((Number) place.get("price_level")).intValue() : null)
                        .address((String) place.get("vicinity"))
                        .isOpenNow(isOpenNow)
                        .build();
                    
                    // 좌표 정보 추출
                    if (place.containsKey("geometry")) {
                        Map<String, Object> geometry = (Map<String, Object>) place.get("geometry");
                        if (geometry.containsKey("location")) {
                            Map<String, Object> location = (Map<String, Object>) geometry.get("location");
                            nearbyPlace.setLatitude(BigDecimal.valueOf(((Number) location.get("lat")).doubleValue()));
                            nearbyPlace.setLongitude(BigDecimal.valueOf(((Number) location.get("lng")).doubleValue()));
                        }
                    }
                    
                    // 타입 정보 추가 - 스마트 카테고리 선택
                    if (place.containsKey("types")) {
                        List<String> types = (List<String>) place.get("types");
                        String bestCategory = selectBestCategoryFromTypes(types);
                        nearbyPlace.setCategory(bestCategory);
                    }
                    
                    places.add(nearbyPlace);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Google nearby response", e);
        }
        
        return places;
    }
    
    /**
     * 요청한 카테고리와 일치하는 장소만 필터링
     */
    private List<NearbyPlacesResponse.NearbyPlace> filterByRequestedCategory(
            List<NearbyPlacesResponse.NearbyPlace> places, String requestedCategory) {
        
        if ("all".equals(requestedCategory) || requestedCategory == null) {
            return places;
        }
        
        return places.stream()
            .filter(place -> {
                if (place.getCategory() == null) return false;
                
                String category = place.getCategory().toLowerCase();
                String requested = requestedCategory.toLowerCase();
                
                // 직접 매칭
                if (category.contains(requested)) return true;
                
                // 카테고리별 세부 매칭
                return switch (requested) {
                    case "restaurant", "food" -> 
                        category.contains("restaurant") || category.contains("food") || 
                        category.contains("meal") || category.contains("cafe");
                    case "accommodation", "hotel" -> 
                        category.contains("lodging") || category.contains("hotel");
                    case "tourist", "tourism" -> 
                        category.contains("tourist") || category.contains("attraction") ||
                        category.contains("museum") || category.contains("park");
                    case "shopping" -> 
                        category.contains("shopping") || category.contains("store");
                    default -> false;
                };
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Google types 배열에서 가장 관련성 높은 카테고리 선택
     */
    private String selectBestCategoryFromTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return null;
        }
        
        // 우선순위가 높은 타입부터 확인
        String[] priorityOrder = {
            "restaurant", "food", "meal_takeaway", "meal_delivery",
            "lodging", "accommodation", 
            "tourist_attraction", "museum", "park",
            "shopping_mall", "store",
            "hospital", "pharmacy",
            "gas_station", "parking"
        };
        
        for (String priority : priorityOrder) {
            if (types.contains(priority)) {
                return priority;
            }
        }
        
        // 우선순위에 없으면 첫 번째 타입 반환 (기존 로직)
        return types.get(0);
    }
    
    /**
     * 키워드 기반 폴백 검색
     */
    private List<NearbyPlacesResponse.NearbyPlace> searchWithKeywordFallback(
            NearbyPlacesRequest request, String location, Integer radius) {
        
        try {
            String fallbackKeyword = getCategoryKeyword(request.getCategory());
            log.info("폴백 검색 실행: keyword={}", fallbackKeyword);
            
            Map<String, Object> response = googlePlacesClient.searchNearbyPlaces(
                location,
                radius,
                null, // type 없이 검색
                fallbackKeyword, // 키워드로 검색
                request.getLanguage(),
                null, // fields parameter는 지원되지 않음
                googleApiKey
            );
            
            return parseGoogleNearbyResponse(response);
            
        } catch (Exception e) {
            log.error("폴백 검색 실패", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 카테고리에 해당하는 검색 키워드 반환
     */
    private String getCategoryKeyword(String category) {
        return switch (category != null ? category.toLowerCase() : "restaurant") {
            case "restaurant", "food" -> "restaurant";
            case "accommodation", "hotel" -> "hotel";
            case "tourist", "tourism" -> "tourist attraction";
            case "shopping" -> "shopping mall";
            case "hospital" -> "hospital";
            case "gas_station" -> "gas station";
            default -> "restaurant";
        };
    }
    
    private List<NearbyPlacesResponse.NearbyPlace> parseKakaoNearbyResponse(Map<String, Object> response) {
        List<NearbyPlacesResponse.NearbyPlace> places = new ArrayList<>();
        
        try {
            if (response.containsKey("documents") && response.get("documents") instanceof List) {
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
                
                for (Map<String, Object> doc : documents) {
                    NearbyPlacesResponse.NearbyPlace nearbyPlace = NearbyPlacesResponse.NearbyPlace.builder()
                        .placeId((String) doc.get("id"))
                        .name((String) doc.get("place_name"))
                        .category((String) doc.get("category_name"))
                        .address((String) doc.get("road_address_name"))
                        .phone((String) doc.get("phone"))
                        .website((String) doc.get("place_url"))
                        .build();
                    
                    // 좌표 정보 추출
                    if (doc.containsKey("x") && doc.containsKey("y")) {
                        nearbyPlace.setLatitude(BigDecimal.valueOf(Double.parseDouble((String) doc.get("y"))));
                        nearbyPlace.setLongitude(BigDecimal.valueOf(Double.parseDouble((String) doc.get("x"))));
                    }
                    
                    // 거리 정보 추출 (있는 경우)
                    if (doc.containsKey("distance")) {
                        String distanceStr = (String) doc.get("distance");
                        try {
                            Integer distance = Integer.parseInt(distanceStr);
                            nearbyPlace.setDistance(distance);
                        } catch (NumberFormatException e) {
                            log.debug("Could not parse distance: {}", distanceStr);
                        }
                    }
                    
                    places.add(nearbyPlace);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Kakao nearby response", e);
        }
        
        return places;
    }
    
    @Override
    public String getAddressFromCoordinates(Double latitude, Double longitude) {
        log.debug("Getting address from coordinates: ({}, {})", latitude, longitude);
        
        try {
            String authorization = "KakaoAK " + kakaoApiKey;
            
            Map<String, Object> response = kakaoMapClient.getAddressFromCoordinate(
                authorization, longitude, latitude, "WGS84"
            );
            
            return parseKakaoAddressResponse(response);
            
        } catch (Exception e) {
            log.error("Error getting address from coordinates", e);
            return null;
        }
    }
    
    @Override
    public LocationDetailResponse.LocationInfo getCoordinatesFromAddress(String address) {
        log.debug("Getting coordinates from address: {}", address);
        
        try {
            String authorization = "KakaoAK " + kakaoApiKey;
            
            Map<String, Object> response = kakaoMapClient.searchAddress(
                authorization, address, "similar", 1, 1
            );
            
            return parseKakaoCoordinatesResponse(response, address);
            
        } catch (Exception e) {
            log.error("Error getting coordinates from address", e);
            return null;
        }
    }
    
    @Override
    public Object getExternalApiStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("google_places", "active");
        status.put("kakao_map", "active");
        status.put("last_checked", LocalDateTime.now());
        return status;
    }
    
    @Override
    public Object getApiUsageStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("google_places_calls", 0);
        stats.put("kakao_map_calls", 0);
        stats.put("total_calls", 0);
        stats.put("last_reset", LocalDateTime.now());
        return stats;
    }
    
    // Helper methods
    private String mapCategoryToGoogleType(String category) {
        return switch (category != null ? category.toLowerCase() : "all") {
            case "tourist" -> "tourist_attraction";
            case "restaurant" -> "restaurant";
            case "laundry" -> "laundry";
            case "accommodation" -> "lodging";
            case "shopping" -> "shopping_mall";
            case "entertainment" -> "amusement_park";
            default -> null;
        };
    }
    
    private String mapCategoryToKakaoType(String category) {
        return switch (category != null ? category.toLowerCase() : "all") {
            case "tourist" -> "AT4";  // 관광명소
            case "restaurant" -> "FD6"; // 음식점
            case "laundry" -> "MT1";    // 대형마트
            case "accommodation" -> "AD5"; // 숙박
            case "shopping" -> "MT1";   // 마트
            case "entertainment" -> "CT1"; // 문화시설
            default -> null;
        };
    }
    
    private Integer calculateRadiusFromTimeRange(Integer timeRange, String transportMode) {
        // 교통수단과 시간에 따른 반경 계산 (미터)
        return switch (transportMode) {
            case "walking" -> timeRange * 80; // 시속 5km 기준
            case "public_transport" -> timeRange * 500; // 시속 30km 기준
            case "car" -> timeRange * 800; // 시속 50km 기준
            default -> timeRange * 300;
        };
    }
    
    private boolean isGooglePlaceId(String placeId) {
        if (placeId == null) return false;
        
        // Plus Code 형식 체크 (예: "HW3Q+X5 서울특별시" 또는 "HW3Q+X5")
        if (placeId.matches("^[0-9A-Z]{4}\\+[0-9A-Z]{2}.*")) {
            return true; // Plus Code도 Google에서 처리 가능
        }
        
        // 일반 Google Place ID 형식 체크
        return placeId.length() > 20 && placeId.matches("^[A-Za-z0-9_-]+$");
    }
    
    private String buildGooglePlaceFields(Boolean includeReviews) {
        StringBuilder fields = new StringBuilder();
        fields.append("place_id,name,formatted_address,address_components,geometry,rating,user_ratings_total,");
        fields.append("photos,opening_hours,formatted_phone_number,website,price_level,");
        fields.append("types,vicinity,business_status");
        
        if (includeReviews != null && includeReviews) {
            fields.append(",reviews");
        }
        
        return fields.toString();
    }
    
    private List<LocationSearchResponse.PlaceCard> deduplicateResults(List<LocationSearchResponse.PlaceCard> results) {
        // 중복 제거 로직 (장소명과 좌표 기준)
        Map<String, LocationSearchResponse.PlaceCard> uniqueResults = new LinkedHashMap<>();
        
        for (LocationSearchResponse.PlaceCard place : results) {
            String key = place.getName() + "_" + place.getLatitude() + "_" + place.getLongitude();
            if (!uniqueResults.containsKey(key)) {
                uniqueResults.put(key, place);
            }
        }
        
        return new ArrayList<>(uniqueResults.values());
    }
    
    private List<NearbyPlacesResponse.NearbyPlace> deduplicateNearbyResults(List<NearbyPlacesResponse.NearbyPlace> results) {
        // 중복 제거 로직
        Map<String, NearbyPlacesResponse.NearbyPlace> uniqueResults = new LinkedHashMap<>();
        
        for (NearbyPlacesResponse.NearbyPlace place : results) {
            String key = place.getName() + "_" + place.getLatitude() + "_" + place.getLongitude();
            if (!uniqueResults.containsKey(key)) {
                uniqueResults.put(key, place);
            }
        }
        
        return new ArrayList<>(uniqueResults.values());
    }
    
    // Response parsing methods (스텁 구현)
    private List<LocationSearchResponse.PlaceCard> parseGooglePlacesResponse(Map<String, Object> response, SearchLocationRequest request) {
        List<LocationSearchResponse.PlaceCard> results = new ArrayList<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            
            if (candidates != null) {
                for (Map<String, Object> candidate : candidates) {
                    LocationSearchResponse.PlaceCard place = parseGooglePlace(candidate);
                    if (place != null) {
                        results.add(place);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Google Places response", e);
        }
        
        return results;
    }
    
    private List<LocationSearchResponse.PlaceCard> parseKakaoMapResponse(Map<String, Object> response, SearchLocationRequest request) {
        List<LocationSearchResponse.PlaceCard> results = new ArrayList<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documents = (List<Map<String, Object>>) response.get("documents");
            
            if (documents != null) {
                for (Map<String, Object> document : documents) {
                    LocationSearchResponse.PlaceCard place = parseKakaoPlace(document);
                    if (place != null) {
                        results.add(place);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Kakao Map response", e);
        }
        
        return results;
    }
    
    private LocationSearchResponse.PlaceCard parseGooglePlace(Map<String, Object> place) {
        // Google Place 데이터 파싱 로직
        return LocationSearchResponse.PlaceCard.builder()
            .placeId((String) place.get("place_id"))
            .name((String) place.get("name"))
            .address((String) place.get("formatted_address"))
            .build();
    }
    
    private LocationSearchResponse.PlaceCard parseKakaoPlace(Map<String, Object> place) {
        // Kakao Place 데이터 파싱 로직
        return LocationSearchResponse.PlaceCard.builder()
            .placeId((String) place.get("id"))
            .name((String) place.get("place_name"))
            .address((String) place.get("address_name"))
            .build();
    }
    
    
    private LocationDetailResponse parseGooglePlaceDetail(Map<String, Object> response) {
        try {
            log.debug("Parsing Google place detail response: {}", response);
            
            // Google API 상태 확인
            String status = (String) response.get("status");
            if (!"OK".equals(status)) {
                String errorMessage = (String) response.get("error_message");
                log.warn("Google Places API returned status: {}, error: {}", status, errorMessage);
                
                switch (status) {
                    case "NOT_FOUND":
                        throw new NotFoundException(ErrorCodes.RESOURCE_NOT_FOUND, "장소를 찾을 수 없습니다. Place ID가 유효하지 않거나 더 이상 존재하지 않습니다.");
                    case "ZERO_RESULTS":
                        throw new NotFoundException(ErrorCodes.RESOURCE_NOT_FOUND, "장소 정보가 더 이상 유효하지 않습니다. 사업장이 폐업되었거나 이전했을 수 있습니다.");
                    case "INVALID_REQUEST":
                        throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, 
                            "잘못된 요청입니다. Place ID 형식을 확인해주세요: " + errorMessage);
                    case "OVER_QUERY_LIMIT":
                        throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, 
                            "API 할당량이 초과되었습니다. 잠시 후 다시 시도해주세요.");
                    case "REQUEST_DENIED":
                        throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, 
                            "API 키가 유효하지 않거나 권한이 없습니다.");
                    case "UNKNOWN_ERROR":
                        throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, 
                            "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                    default:
                        throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, 
                            "Google Places API 오류 (" + status + "): " + errorMessage);
                }
            }
            
            if (!response.containsKey("result")) {
                log.warn("No result found in Google Places API response");
                return LocationDetailResponse.builder().build();
            }
            
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            log.debug("Google API result data: {}", result);
            
            LocationDetailResponse.LocationDetailResponseBuilder builder = LocationDetailResponse.builder();
            
            // 기본 정보
            builder.placeId((String) result.get("place_id"));
            builder.name((String) result.get("name"));
            builder.description((String) result.get("editorial_summary"));
            
            // 평점 정보
            if (result.get("rating") != null) {
                builder.rating(BigDecimal.valueOf(((Number) result.get("rating")).doubleValue()));
            }
            if (result.get("user_ratings_total") != null) {
                builder.reviewCount(((Number) result.get("user_ratings_total")).intValue());
            }
            if (result.get("price_level") != null) {
                builder.priceLevel(((Number) result.get("price_level")).intValue());
            }
            
            // 위치 정보
            if (result.containsKey("geometry")) {
                Map<String, Object> geometry = (Map<String, Object>) result.get("geometry");
                if (geometry.containsKey("location")) {
                    Map<String, Object> location = (Map<String, Object>) geometry.get("location");
                    
                    // 주소 구성 요소에서 region, country, city 추출
                    String region = null;
                    String country = null;
                    String city = null;
                    String postalCode = null;
                    
                    if (result.containsKey("address_components")) {
                        List<Map<String, Object>> addressComponents = (List<Map<String, Object>>) result.get("address_components");
                        for (Map<String, Object> component : addressComponents) {
                            List<String> types = (List<String>) component.get("types");
                            String longName = (String) component.get("long_name");
                            
                            if (types.contains("administrative_area_level_1")) {
                                region = longName; // 서울특별시, 경기도 등
                            } else if (types.contains("country")) {
                                country = longName; // 대한민국
                            } else if (types.contains("sublocality_level_1") || types.contains("administrative_area_level_2") || types.contains("locality")) {
                                city = longName; // 중구, 마포구 등
                            } else if (types.contains("postal_code")) {
                                postalCode = longName;
                            }
                        }
                    }
                    
                    // 검색 키워드 생성 (한국어/영어)
                    String nameKo = (String) result.get("name");
                    String nameEn = extractEnglishName(result); // 영문명 추출 로직
                    
                    // 주변 주차장 검색 (500m 반경)
                    Double lat = ((Number) location.get("lat")).doubleValue();
                    Double lng = ((Number) location.get("lng")).doubleValue();
                    List<LocationDetailResponse.ParkingInfo> nearbyParkings = searchNearbyParkings(lat, lng, 500, 3);
                    
                    LocationDetailResponse.LocationInfo locationInfo = LocationDetailResponse.LocationInfo.builder()
                        .latitude(BigDecimal.valueOf(lat))
                        .longitude(BigDecimal.valueOf(lng))
                        .address((String) result.get("formatted_address"))
                        .searchKeywordKo(nameKo)
                        .searchKeywordEn(nameEn != null ? nameEn : nameKo)
                        .nearbyParkings(nearbyParkings)
                        .region(region)
                        .country(country)
                        .city(city)
                        .postalCode(postalCode)
                        .build();
                    
                    builder.location(locationInfo);
                }
            }
            
            // 연락처 정보
            LocationDetailResponse.ContactInfo contactInfo = LocationDetailResponse.ContactInfo.builder()
                .phone((String) result.get("formatted_phone_number"))
                .internationalPhone((String) result.get("international_phone_number"))
                .website((String) result.get("website"))
                .build();
            builder.contact(contactInfo);
            
            // 이미지 URL 목록
            if (result.containsKey("photos")) {
                List<Map<String, Object>> photos = (List<Map<String, Object>>) result.get("photos");
                List<String> imageUrls = photos.stream()
                    .map(photo -> {
                        String photoRef = (String) photo.get("photo_reference");
                        return "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photoreference=" + photoRef + "&key=" + googleApiKey;
                    })
                    .collect(Collectors.toList());
                builder.images(imageUrls);
            }
            
            // 카테고리 설정
            if (result.containsKey("types")) {
                List<String> types = (List<String>) result.get("types");
                String bestCategory = selectBestCategoryFromTypes(types);
                builder.category(bestCategory);
            }
            
            // 리뷰 정보
            if (result.containsKey("reviews")) {
                List<Map<String, Object>> reviews = (List<Map<String, Object>>) result.get("reviews");
                List<LocationDetailResponse.Review> reviewList = reviews.stream()
                    .map(this::parseGoogleReview)
                    .sorted((r1, r2) -> {
                        String time1 = r1.getTime() != null ? r1.getTime() : "00000000";
                        String time2 = r2.getTime() != null ? r2.getTime() : "00000000";
                        return time2.compareTo(time1); // 내림차순 (최신순)
                    })
                    .collect(Collectors.toList());
                builder.reviews(reviewList);
            }
            
            // 메타데이터
            builder.dataSource("google_places");
            builder.fromCache(false);
            builder.lastUpdated(LocalDateTime.now());
            
            return builder.build();
            
        } catch (NotFoundException | BusinessException e) {
            // 비즈니스 예외는 다시 던지기
            throw e;
        } catch (Exception e) {
            log.error("Error parsing Google place detail", e);
            return LocationDetailResponse.builder().build();
        }
    }
    
    /**
     * Unix timestamp를 YYYYMMDD 형식으로 변환
     */
    private String formatUnixTimestampToYYYYMMDD(Object timeValue) {
        if (timeValue == null) {
            return null;
        }
        
        try {
            long timestamp = ((Number) timeValue).longValue();
            LocalDate date = Instant.ofEpochSecond(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            return date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            log.warn("Failed to parse timestamp: {}", timeValue, e);
            return null;
        }
    }
    
    private LocationDetailResponse.Review parseGoogleReview(Map<String, Object> review) {
        try {
            return LocationDetailResponse.Review.builder()
                .authorName((String) review.get("author_name"))
                .rating(review.get("rating") != null ? ((Number) review.get("rating")).intValue() : null)
                .text((String) review.get("text"))
                .time(formatUnixTimestampToYYYYMMDD(review.get("time")))
                .relativeTimeDescription((String) review.get("relative_time_description"))
                .language((String) review.get("language"))
                .authorPhotoUrl((String) review.get("profile_photo_url"))
                .build();
        } catch (Exception e) {
            log.error("Error parsing Google review", e);
            return LocationDetailResponse.Review.builder().build();
        }
    }
    
    
    private List<LocationSearchResponse.PlaceCard> parseGoogleAutocompleteResponse(Map<String, Object> response, Integer limit) {
        List<LocationSearchResponse.PlaceCard> results = new ArrayList<>();
        
        try {
            log.debug("Parsing Google autocomplete response: {}", response);
            
            // Google Autocomplete API 응답 구조 확인
            String status = (String) response.get("status");
            if (!"OK".equals(status)) {
                log.warn("Google Autocomplete API returned status: {}", status);
                return results;
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> predictions = (List<Map<String, Object>>) response.get("predictions");
            
            if (predictions != null) {
                int count = 0;
                for (Map<String, Object> prediction : predictions) {
                    if (limit != null && count >= limit) break;
                    
                    LocationSearchResponse.PlaceCard place = parseGooglePrediction(prediction);
                    if (place != null) {
                        results.add(place);
                        count++;
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error parsing Google autocomplete response", e);
        }
        
        return results;
    }
    
    private LocationSearchResponse.PlaceCard parseGooglePrediction(Map<String, Object> prediction) {
        try {
            String placeId = (String) prediction.get("place_id");
            String description = (String) prediction.get("description");
            
            return LocationSearchResponse.PlaceCard.builder()
                .placeId(placeId)
                .name(description)
                .address(description)
                .category("establishment")
                .build();
                
        } catch (Exception e) {
            log.error("Error parsing Google prediction", e);
            return null;
        }
    }
    
    /**
     * Google Places API 결과에서 영문명 추출
     */
    private String extractEnglishName(Map<String, Object> result) {
        try {
            String name = (String) result.get("name");
            
            // 이미 영문인 경우 그대로 반환
            if (name != null && name.matches("^[a-zA-Z0-9\\s\\-\\.\\&\\'']+$")) {
                return name;
            }
            
            // vicinity에서 영문명 찾기 시도
            String vicinity = (String) result.get("vicinity");
            if (vicinity != null && vicinity.matches("^[a-zA-Z0-9\\s\\-\\.\\&\\'']+$")) {
                return vicinity;
            }
            
            // Google Places API의 international_phone_number가 있으면 국제적인 장소로 간주하여
            // 원래 이름을 영문명으로 사용 (예: McDonald's, KFC 등)
            String intlPhone = (String) result.get("international_phone_number");
            if (intlPhone != null && name != null && name.matches(".*[a-zA-Z].*")) {
                // 이름에 영문이 포함되어 있으면 그대로 사용
                return name;
            }
            
            // 영문명을 찾을 수 없는 경우 null 반환 (한글명 사용)
            return null;
            
        } catch (Exception e) {
            log.error("Error extracting English name", e);
            return null;
        }
    }
    
    @Override
    public List<LocationDetailResponse.ParkingInfo> searchNearbyParkings(
            Double latitude, Double longitude, Integer radiusMeters, Integer limit) {
        
        log.info("Searching nearby parkings: ({}, {}), radius={}m, limit={}", 
                latitude, longitude, radiusMeters, limit);
        
        try {
            // Google Places Nearby Search API로 주차장 검색
            String location = latitude + "," + longitude;
            String radius = radiusMeters != null ? radiusMeters.toString() : "500"; // 기본 500m
            String type = "parking"; // Google Places의 주차장 타입
            
            Map<String, Object> response = googlePlacesClient.searchNearbyPlaces(
                location, Integer.parseInt(radius), type, null, "ko", null, googleApiKey
            );
            
            return parseNearbyParkingsResponse(response, latitude, longitude, limit);
            
        } catch (Exception e) {
            log.error("Error searching nearby parkings", e);
            return new ArrayList<>();
        }
    }
    
    private List<LocationDetailResponse.ParkingInfo> parseNearbyParkingsResponse(
            Map<String, Object> response, Double originLat, Double originLng, Integer limit) {
        
        List<LocationDetailResponse.ParkingInfo> parkings = new ArrayList<>();
        
        try {
            String status = (String) response.get("status");
            if (!"OK".equals(status)) {
                log.warn("Google Places parking search returned status: {}", status);
                return parkings;
            }
            
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null || results.isEmpty()) {
                log.info("No parking results found");
                return parkings;
            }
            
            int maxResults = limit != null ? Math.min(limit, 3) : 3;
            
            for (int i = 0; i < Math.min(results.size(), maxResults); i++) {
                Map<String, Object> place = results.get(i);
                
                try {
                    String placeId = (String) place.get("place_id");
                    String name = (String) place.get("name");
                    String vicinity = (String) place.get("vicinity");
                    
                    // 위치 정보
                    Map<String, Object> geometry = (Map<String, Object>) place.get("geometry");
                    Map<String, Object> location = (Map<String, Object>) geometry.get("location");
                    Double lat = ((Number) location.get("lat")).doubleValue();
                    Double lng = ((Number) location.get("lng")).doubleValue();
                    
                    // 거리 계산 (미터)
                    Double distanceMeters = calculateDistance(originLat, originLng, lat, lng);
                    Integer walkingMinutes = (int) Math.ceil(distanceMeters / 80.0); // 도보 속도 80m/min
                    
                    // 영문명 추출
                    String nameEn = extractEnglishName(place);
                    
                    // 주차장 타입 결정
                    List<String> types = (List<String>) place.get("types");
                    String parkingType = determineParkingType(types);
                    
                    // 평점 정보
                    BigDecimal rating = null;
                    if (place.get("rating") != null) {
                        rating = BigDecimal.valueOf(((Number) place.get("rating")).doubleValue());
                    }
                    
                    LocationDetailResponse.ParkingInfo parking = LocationDetailResponse.ParkingInfo.builder()
                        .placeId(placeId)
                        .nameKo(name)
                        .nameEn(nameEn != null ? nameEn : name)
                        .address(vicinity)
                        .distanceMeters(distanceMeters)
                        .walkingMinutes(walkingMinutes)
                        .parkingType(parkingType)
                        .rating(rating)
                        .latitude(BigDecimal.valueOf(lat))
                        .longitude(BigDecimal.valueOf(lng))
                        .build();
                    
                    parkings.add(parking);
                    
                } catch (Exception e) {
                    log.error("Error parsing parking place", e);
                    continue;
                }
            }
            
            // 거리순으로 정렬
            parkings.sort((p1, p2) -> Double.compare(p1.getDistanceMeters(), p2.getDistanceMeters()));
            
            log.info("Found {} nearby parkings", parkings.size());
            return parkings;
            
        } catch (Exception e) {
            log.error("Error parsing parking search response", e);
            return parkings;
        }
    }
    
    private String determineParkingType(List<String> types) {
        if (types == null) return "일반주차장";
        
        for (String type : types) {
            switch (type) {
                case "parking_garage":
                    return "지하주차장";
                case "parking_lot":
                    return "노상주차장";
                case "parking_meter":
                    return "유료주차장";
                default:
                    continue;
            }
        }
        return "일반주차장";
    }
    
    private Double calculateDistance(Double lat1, Double lng1, Double lat2, Double lng2) {
        // Haversine 공식을 사용한 거리 계산
        final double R = 6371000; // 지구 반지름 (미터)
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // 미터 단위 거리
    }
    
    private String parseKakaoAddressResponse(Map<String, Object> response) {
        return "";
    }
    
    private LocationDetailResponse.LocationInfo parseKakaoCoordinatesResponse(Map<String, Object> response, String address) {
        return LocationDetailResponse.LocationInfo.builder().build();
    }
    
}