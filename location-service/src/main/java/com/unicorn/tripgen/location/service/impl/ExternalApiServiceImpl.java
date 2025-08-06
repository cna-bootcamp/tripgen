package com.unicorn.tripgen.location.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.tripgen.common.exception.BusinessException;
import com.unicorn.tripgen.common.exception.ErrorCodes;
import com.unicorn.tripgen.location.client.GooglePlacesClient;
import com.unicorn.tripgen.location.client.KakaoMapClient;
import com.unicorn.tripgen.location.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    
    @Value("${external.api.preferred-source:google}")
    private String preferredSource;
    
    @Override
    public List<LocationSearchResponse.PlaceCard> searchLocationsByKeyword(SearchLocationRequest request) {
        log.info("Searching locations with external APIs: keyword={}, source={}", request.getKeyword(), request.getPreferredSource());
        
        try {
            List<LocationSearchResponse.PlaceCard> results = new ArrayList<>();
            
            // 선호하는 소스 결정
            String source = request.getPreferredSource() != null ? request.getPreferredSource() : preferredSource;
            
            // 주 소스로 검색
            if ("google".equalsIgnoreCase(source)) {
                results.addAll(searchWithGooglePlaces(request));
            } else if ("kakao".equalsIgnoreCase(source)) {
                results.addAll(searchWithKakaoMap(request));
            } else {
                // 기본값: Google 사용
                results.addAll(searchWithGooglePlaces(request));
            }
            
            // 결과가 부족하면 보조 소스에서 추가 검색
            if (results.size() < request.getSize()) {
                if ("google".equalsIgnoreCase(source)) {
                    results.addAll(searchWithKakaoMap(request));
                } else {
                    results.addAll(searchWithGooglePlaces(request));
                }
            }
            
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
            
            // Google Places로 주변 검색
            results.addAll(searchNearbyWithGoogle(request));
            
            // Kakao Map으로 추가 검색
            results.addAll(searchNearbyWithKakao(request));
            
            // 중복 제거 및 거리순 정렬
            results = deduplicateNearbyResults(results);
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
            
            Map<String, Object> response = googlePlacesClient.searchNearbyPlaces(
                location,
                radius,
                mapCategoryToGoogleType(request.getCategory()),
                null, // keyword
                request.getLanguage(),
                googleApiKey
            );
            
            return parseGoogleNearbyResponse(response, request);
            
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
            
            return parseKakaoNearbyResponse(response, request);
            
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
    public LocationDetailResponse.BusinessHours getBusinessHours(String placeId) {
        log.info("Getting business hours from external API: placeId={}", placeId);
        
        try {
            if (isGooglePlaceId(placeId)) {
                Map<String, Object> response = googlePlacesClient.getPlaceDetails(
                    placeId, 
                    "opening_hours,business_status,current_opening_hours",
                    "ko", 
                    googleApiKey
                );
                
                return parseGoogleBusinessHours(response);
            } else {
                throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "지원하지 않는 장소 ID 형식입니다");
            }
            
        } catch (Exception e) {
            log.error("Error getting business hours from external API: {}", placeId, e);
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "영업시간 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public List<LocationSearchResponse.PlaceCard> getLocationAutocomplete(
            String input, String latitude, String longitude, String language, Integer limit) {
        
        log.debug("Getting location autocomplete: input={}", input);
        
        try {
            String location = latitude != null && longitude != null ? latitude + "," + longitude : null;
            
            Map<String, Object> response = googlePlacesClient.getPlaceAutocomplete(
                input, location, 50000, null, null, language, googleApiKey
            );
            
            return parseGoogleAutocompleteResponse(response, limit);
            
        } catch (Exception e) {
            log.error("Error getting location autocomplete", e);
            return new ArrayList<>();
        }
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
        // Google Place ID는 일반적으로 특정 패턴을 가짐
        return placeId != null && placeId.length() > 20 && placeId.matches("^[A-Za-z0-9_-]+$");
    }
    
    private String buildGooglePlaceFields(Boolean includeReviews) {
        StringBuilder fields = new StringBuilder();
        fields.append("place_id,name,formatted_address,geometry,rating,user_ratings_total,");
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
    
    // 나머지 파싱 메서드들도 스텁으로 구현
    private List<NearbyPlacesResponse.NearbyPlace> parseGoogleNearbyResponse(Map<String, Object> response, NearbyPlacesRequest request) {
        return new ArrayList<>();
    }
    
    private List<NearbyPlacesResponse.NearbyPlace> parseKakaoNearbyResponse(Map<String, Object> response, NearbyPlacesRequest request) {
        return new ArrayList<>();
    }
    
    private LocationDetailResponse parseGooglePlaceDetail(Map<String, Object> response) {
        return LocationDetailResponse.builder().build();
    }
    
    private LocationDetailResponse.BusinessHours parseGoogleBusinessHours(Map<String, Object> response) {
        return LocationDetailResponse.BusinessHours.builder().build();
    }
    
    private List<LocationSearchResponse.PlaceCard> parseGoogleAutocompleteResponse(Map<String, Object> response, Integer limit) {
        return new ArrayList<>();
    }
    
    private String parseKakaoAddressResponse(Map<String, Object> response) {
        return "";
    }
    
    private LocationDetailResponse.LocationInfo parseKakaoCoordinatesResponse(Map<String, Object> response, String address) {
        return LocationDetailResponse.LocationInfo.builder().build();
    }
}