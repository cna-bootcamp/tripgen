package com.unicorn.tripgen.location.service.impl;

import com.unicorn.tripgen.common.exception.BusinessException;
import com.unicorn.tripgen.common.exception.ErrorCodes;
import com.unicorn.tripgen.common.exception.NotFoundException;
import com.unicorn.tripgen.location.dto.*;
import com.unicorn.tripgen.location.service.LocationService;
import com.unicorn.tripgen.location.service.ExternalApiService;
import com.unicorn.tripgen.location.service.CacheService;
import com.unicorn.tripgen.location.service.RouteService;
import com.unicorn.tripgen.location.service.RecommendationProducerService;
import com.unicorn.tripgen.location.entity.Location;
import com.unicorn.tripgen.location.entity.LocationType;
import com.unicorn.tripgen.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * 위치 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LocationServiceImpl implements LocationService {
    
    private final LocationRepository locationRepository;
    private final ExternalApiService externalApiService;
    private final CacheService cacheService;
    private final RouteService routeService;
    private final Environment environment;
    private final RecommendationProducerService recommendationProducerService;
    
    // 구글 API 통일로 인해 한국 좌표 판별 불필요
    
    // 키워드 검색 메서드 제거됨 - 주변 검색으로 충분
    
    /**
     * 캐시 사용 여부 확인 (dev 프로파일에서는 캐시 비활성화)
     */
    private boolean isCacheEnabled() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("dev".equals(profile)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public NearbyPlacesResponse searchNearbyPlaces(NearbyPlacesRequest request) {
        log.info("Searching nearby places: origin=({}, {}), transport={}, timeRange={}min", 
                request.getOrigin().getLatitude(), request.getOrigin().getLongitude(), 
                request.getTransportMode(), request.getTimeRange());
        
        try {
            // 프로파일 기반 캐시 사용 여부 확인
            String cacheKey = null;
            if (isCacheEnabled()) {
                cacheKey = buildNearbyCacheKey(request);
                NearbyPlacesResponse cachedResult = cacheService.getNearbySearchResult(cacheKey);
                if (cachedResult != null) {
                    log.debug("Returning cached nearby search result");
                    return cachedResult;
                }
            }
            
            long startTime = System.currentTimeMillis();
            
            // 반경 계산 (시간 기반)
            double radiusKm = calculateRadiusByTimeAndTransport(request.getTimeRange(), request.getTransportMode());
            
            // 주변 장소 검색
            List<NearbyPlacesResponse.NearbyPlace> nearbyPlaces = findNearbyPlaces(request, radiusKm);
            
            // 검색 결과가 없는 경우에도 정상 처리
            if (nearbyPlaces == null) {
                nearbyPlaces = new ArrayList<>();
            }
            
            log.debug("Found {} nearby places before filtering", nearbyPlaces.size());
            
            // 경로 및 시간 정보 계산 (travel_time 정렬인 경우에만)
            if ("travel_time".equals(request.getSort())) {
                enrichWithRouteInformation(nearbyPlaces, request);
            }
            
            // 정렬 및 페이징
            List<NearbyPlacesResponse.NearbyPlace> sortedPlaces = applySortingAndPaging(nearbyPlaces, request);
            
            log.debug("Final filtered results: {}", sortedPlaces.size());
            
            // 응답 생성
            NearbyPlacesResponse response = NearbyPlacesResponse.builder()
                .totalCount((long) nearbyPlaces.size())
                .page(request.getPage())
                .size(request.getSize())
                .hasNext(calculateHasNext(nearbyPlaces.size(), request.getPage(), request.getSize()))
                .places(sortedPlaces)
                .searchCriteria(buildSearchCriteria(request))
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .dataSource("merged")
                .responseTime(LocalDateTime.now())
                .build();
            
            // 프로파일 기반 캐시 저장
            if (isCacheEnabled() && cacheKey != null) {
                cacheService.cacheNearbySearchResult(cacheKey, response, 600); // 10분 캐시
            }
            
            log.info("Nearby places search completed: {} results found", response.getTotalCount());
            return response;
            
        } catch (Exception e) {
            log.error("Error searching nearby places", e);
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "주변 장소 검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public LocationDetailResponse getLocationDetail(String placeId, Boolean includeReviews, String language) {
        log.info("Getting location detail: placeId={}, includeReviews={}", placeId, includeReviews);
        
        try {
            // 프로파일 기반 캐시 사용 여부 확인
            String cacheKey = null;
            if (isCacheEnabled()) {
                cacheKey = buildDetailCacheKey(placeId, includeReviews, language);
                LocationDetailResponse cachedDetail = cacheService.getLocationDetail(cacheKey);
                if (cachedDetail != null) {
                    log.debug("Returning cached location detail: {}", placeId);
                    cachedDetail.setFromCache(true);
                    return cachedDetail;
                }
            }
            
            // 로컬 DB에서 위치 정보 조회
            Optional<Location> locationOpt = locationRepository.findByPlaceId(placeId);
            Location location = null;
            if (locationOpt.isPresent()) {
                location = locationOpt.get();
                log.debug("Found location in local DB: {}", location.getName());
            }
            
            // 외부 API에서 상세 정보 조회
            LocationDetailResponse response = externalApiService.getLocationDetail(placeId, includeReviews, language);
            
            // 로컬 데이터와 병합
            if (location != null) {
                response = mergeWithLocalData(response, location);
            }
            
            
            // 응답 메타데이터 설정
            response.setFromCache(false);
            response.setLastUpdated(LocalDateTime.now());
            
            // 프로파일 기반 캐시 저장
            if (isCacheEnabled() && cacheKey != null) {
                cacheService.cacheLocationDetail(cacheKey, response, 1800); // 30분 캐시
            }
            
            // 로컬 DB 업데이트 (비동기)
            updateLocalLocationAsync(response);
            
            log.info("Location detail retrieved successfully: {}", placeId);
            return response;
            
        } catch (Exception e) {
            log.error("Error getting location detail: {}", placeId, e);
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "위치 상세 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public Object getLocationRecommendations(String placeId, String tripId) {
        log.info("Getting location recommendations: placeId={}, tripId={}", placeId, tripId);
        
        try {
            // 캐시에서 AI 추천 정보 확인
            String cacheKey = "ai_recommendation:" + placeId + (tripId != null ? ":" + tripId : "");
            Object cachedRecommendation = cacheService.getObject(cacheKey);
            
            if (cachedRecommendation != null) {
                log.debug("Returning cached AI recommendation: {}", placeId);
                return cachedRecommendation;
            }
            
            // MQ를 통한 AI 추천 요청 발행
            RecommendationRequest.SearchContext searchContext = RecommendationRequest.SearchContext.builder()
                    .searchQuery("일반 추천") // 기본 검색 컨텍스트
                    .searchIntents(new String[]{"general"})
                    .build();
            
            String requestId = recommendationProducerService.sendRecommendationRequest(placeId, tripId, searchContext);
            
            // 처리 중 상태를 Redis에 저장
            String statusCacheKey = "rec_status_" + requestId;
            cacheService.cacheObject(statusCacheKey, "processing", 60); // 1분
            
            log.info("AI recommendation request sent: requestId={}, placeId={}", requestId, placeId);
            
            // 비동기 처리 응답
            return Map.of(
                    "requestId", requestId,
                    "status", "processing",
                    "message", "AI 추천정보를 생성 중입니다",
                    "pollingUrl", "/api/v1/locations/recommendations/" + requestId + "/status",
                    "websocketUrl", "/ws/recommendations/" + requestId,
                    "estimatedTime", 30
            );
            
        } catch (Exception e) {
            log.error("Error getting location recommendations: {}", placeId, e);
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "AI 추천 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    
    // 나머지 메서드들은 구현 복잡도로 인해 기본 골격만 제공
    @Override
    public List<LocationSearchResponse.PlaceCard> getLocationsWithinRadius(
            BigDecimal latitude, BigDecimal longitude, Double radiusKm, 
            String locationType, Pageable pageable) {
        
        Page<Location> locations;
        if (locationType != null) {
            locations = locationRepository.findLocationsByTypeWithinRadius(
                latitude, longitude, radiusKm, locationType, pageable);
        } else {
            locations = locationRepository.findLocationsWithinRadius(
                latitude, longitude, radiusKm, pageable);
        }
        
        return locations.getContent().stream()
            .map(this::convertToPlaceCard)
            .collect(Collectors.toList());
    }
    
    
    
    // Helper methods
    
    private String buildNearbyCacheKey(NearbyPlacesRequest request) {
        return String.format("nearby:%s:%s:%d:%s:%d:%d:%s:%s:%s", 
            request.getOrigin().getLatitude(), request.getOrigin().getLongitude(),
            request.getTimeRange(), request.getTransportMode(),
            request.getPage(), request.getSize(),
            request.getMinRating(), request.getMaxPriceLevel(), request.getOpenNow());
    }
    
    private String buildDetailCacheKey(String placeId, Boolean includeReviews, String language) {
        return String.format("detail:%s:%s:%s", placeId, includeReviews, language);
    }
    
    
    
    
    
    private Boolean calculateHasNext(int totalSize, int page, int size) {
        return totalSize > page * size;
    }
    
    private LocationSearchResponse.PlaceCard convertToPlaceCard(Location location) {
        return LocationSearchResponse.PlaceCard.builder()
            .placeId(location.getPlaceId())
            .name(location.getName())
            .category(location.getLocationType().name())
            .rating(location.getRating())
            .reviewCount(location.getReviewCount())
            .address(location.getAddress())
            .latitude(location.getLatitude())
            .longitude(location.getLongitude())
            .imageUrl(location.getImageUrl())
            .phone(location.getPhone())
            .website(location.getWebsite())
            .build();
    }
    
    
    
    
    
    
    
    
    
    // Private helper methods (stubs)
    private double calculateRadiusByTimeAndTransport(Integer timeRange, String transportMode) {
        // 교통수단과 시간에 따른 반경 계산
        return switch (transportMode) {
            case "walking" -> timeRange * 0.08; // 시속 5km 기준
            case "public_transport" -> timeRange * 0.5; // 시속 30km 기준
            case "car" -> timeRange * 0.8; // 시속 50km 기준
            default -> timeRange * 0.3;
        };
    }
    
    private List<NearbyPlacesResponse.NearbyPlace> findNearbyPlaces(NearbyPlacesRequest request, double radiusKm) {
        log.info("Finding nearby places: origin=({}, {}), radius={}km", 
                request.getOrigin().getLatitude(), request.getOrigin().getLongitude(), radiusKm);
        
        try {
            // 모든 지역에서 Google Places API 사용 (속도 및 평점 정보 제공을 위해)
            List<NearbyPlacesResponse.NearbyPlace> results = new ArrayList<>();
            List<NearbyPlacesResponse.NearbyPlace> googleResults = findNearbyPlacesWithGoogle(request, radiusKm);
            
            if (googleResults != null) {
                results.addAll(googleResults);
            }
            
            log.info("Found {} nearby places using Google API", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("Error finding nearby places", e);
            // 에러 발생 시에도 빈 리스트 반환하여 정상 응답 유지
            log.warn("Returning empty list due to search error");
            return new ArrayList<>();
        }
    }
    
    // 구글 API 통일로 인해 Kakao 검색 메서드 제거됨
    
    private List<NearbyPlacesResponse.NearbyPlace> findNearbyPlacesWithGoogle(NearbyPlacesRequest request, double radiusKm) {
        log.debug("Using Google Places API for nearby places search");
        
        try {
            // ExternalApiService를 통해 Google 주변 검색 수행
            List<NearbyPlacesResponse.NearbyPlace> results = externalApiService.searchNearbyWithGoogle(request);
            
            // null 체크 및 안전한 반환
            if (results == null) {
                log.debug("External API returned null, returning empty list");
                return new ArrayList<>();
            }
            
            log.debug("Google API returned {} places", results.size());
            return results;
            
        } catch (Exception e) {
            log.error("Error searching nearby places with Google API", e);
            // 외부 API 오류 시에도 빈 리스트 반환하여 서비스 중단 방지
            log.warn("Returning empty list due to Google API error: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // 구글 API 통일로 인해 Kakao 카테고리 매핑 불필요
    
    private String mapCategoryToGoogleType(String category) {
        // 카테고리를 Google Places 타입으로 매핑
        if (category == null) return null;
        
        switch (category.toLowerCase()) {
            case "restaurant": case "food": return "restaurant";
            case "accommodation": case "hotel": return "lodging";
            case "tourism": case "tourist": return "tourist_attraction";
            case "shopping": return "shopping_mall";
            case "gas_station": return "gas_station";
            case "hospital": return "hospital";
            default: return "point_of_interest";
        }
    }
    
    private void enrichWithRouteInformation(List<NearbyPlacesResponse.NearbyPlace> places, NearbyPlacesRequest request) {
        // 경로 정보 추가 로직
    }
    
    
    private void enrichWithAIRecommendations(List<NearbyPlacesResponse.NearbyPlace> places) {
        // AI 추천 정보 추가 로직
    }
    
    private List<NearbyPlacesResponse.NearbyPlace> applySortingAndPaging(List<NearbyPlacesResponse.NearbyPlace> places, NearbyPlacesRequest request) {
        // 빈 리스트 안전 처리
        if (places == null || places.isEmpty()) {
            log.debug("No places to filter, returning empty list");
            return new ArrayList<>();
        }
        
        List<NearbyPlacesResponse.NearbyPlace> filteredPlaces = places.stream()
            .filter(place -> applyMinRatingFilter(place, request.getMinRating()))
            .filter(place -> applyMaxPriceLevelFilter(place, request.getMaxPriceLevel()))
            .filter(place -> applyOpenNowFilter(place, request.getOpenNow()))
            .skip((long) (request.getPage() - 1) * request.getSize())
            .limit(request.getSize())
            .collect(Collectors.toList());
            
        log.debug("Filtering applied: {} -> {} places", places.size(), filteredPlaces.size());
        return filteredPlaces;
    }
    
    private NearbyPlacesResponse.SearchCriteria buildSearchCriteria(NearbyPlacesRequest request) {
        return NearbyPlacesResponse.SearchCriteria.builder()
            .originLatitude(request.getOrigin().getLatitude())
            .originLongitude(request.getOrigin().getLongitude())
            .originAddress(request.getOrigin().getAddress())
            .transportMode(request.getTransportMode())
            .timeRange(request.getTimeRange())
            .category(request.getCategory())
            .sort(request.getSort())
            .minRating(request.getMinRating())
            .maxPriceLevel(request.getMaxPriceLevel())
            .openNow(request.getOpenNow())
            .build();
    }
    
    private LocationDetailResponse mergeWithLocalData(LocationDetailResponse response, Location location) {
        // 로컬 데이터와 외부 API 데이터 병합
        return response;
    }
    
    private LocationDetailResponse.AIRecommendation getOrGenerateAIRecommendation(String placeId) {
        // AI 추천 정보 조회 또는 생성
        return LocationDetailResponse.AIRecommendation.builder().build();
    }
    
    private void updateLocalLocationAsync(LocationDetailResponse response) {
        // 비동기 로컬 DB 업데이트
    }
    
    private void updateLocalLocationSync(LocationDetailResponse response) {
        // 동기 로컬 DB 업데이트
    }
    
    
    
    private boolean applyMinRatingFilter(NearbyPlacesResponse.NearbyPlace place, BigDecimal minRating) {
        // minRating이 null이면 필터링하지 않음
        if (minRating == null) {
            return true;
        }
        
        // 장소의 rating이 null이면 제외
        if (place.getRating() == null) {
            return false;
        }
        
        // minRating 이상인 장소만 포함
        return place.getRating().compareTo(minRating) >= 0;
    }
    
    private boolean applyMaxPriceLevelFilter(NearbyPlacesResponse.NearbyPlace place, Integer maxPriceLevel) {
        // maxPriceLevel이 null이면 필터링하지 않음
        if (maxPriceLevel == null) {
            return true;
        }
        
        // 장소의 priceLevel이 null이면 제외 (엄격한 가격 필터링 정책)
        if (place.getPriceLevel() == null) {
            return false;
        }
        
        // maxPriceLevel 이하인 장소만 포함
        return place.getPriceLevel() <= maxPriceLevel;
    }
    
    private boolean applyOpenNowFilter(NearbyPlacesResponse.NearbyPlace place, Boolean openNow) {
        // openNow가 null이거나 false이면 필터링하지 않음
        if (openNow == null || !openNow) {
            return true;
        }
        
        // 장소의 isOpenNow가 null이면 제외 (영업시간 정보 없는 경우)
        if (place.getIsOpenNow() == null) {
            return false;
        }
        
        // 영업 중인 장소만 포함
        return place.getIsOpenNow();
    }
    
    @Override
    public ExternalApiService getExternalApiService() {
        return externalApiService;
    }
}