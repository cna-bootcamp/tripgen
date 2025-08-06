package com.unicorn.tripgen.location.service;

import com.unicorn.tripgen.common.exception.BusinessException;
import com.unicorn.tripgen.common.exception.ErrorCodes;
import com.unicorn.tripgen.common.exception.NotFoundException;
import com.unicorn.tripgen.location.dto.*;
import com.unicorn.tripgen.location.entity.Location;
import com.unicorn.tripgen.location.entity.LocationType;
import com.unicorn.tripgen.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final WeatherService weatherService;
    private final RouteService routeService;
    
    @Override
    public LocationSearchResponse searchLocationsByKeyword(SearchLocationRequest request) {
        log.info("Searching locations by keyword: {}", request.getKeyword());
        
        try {
            // 캐시에서 검색 결과 확인
            String cacheKey = buildSearchCacheKey(request);
            LocationSearchResponse cachedResult = cacheService.getLocationSearchResult(cacheKey);
            if (cachedResult != null) {
                log.debug("Returning cached search result for keyword: {}", request.getKeyword());
                return cachedResult;
            }
            
            long startTime = System.currentTimeMillis();
            
            // 로컬 DB에서 검색
            List<LocationSearchResponse.PlaceCard> localResults = searchLocalLocations(request);
            
            // 외부 API에서 검색
            List<LocationSearchResponse.PlaceCard> externalResults = externalApiService.searchLocationsByKeyword(request);
            
            // 결과 병합 및 중복 제거
            List<LocationSearchResponse.PlaceCard> mergedResults = mergeAndDeduplicateResults(localResults, externalResults);
            
            // 정렬 및 페이징 적용
            List<LocationSearchResponse.PlaceCard> sortedResults = applySortingAndPaging(mergedResults, request);
            
            // 응답 생성
            LocationSearchResponse response = LocationSearchResponse.builder()
                .keyword(request.getKeyword())
                .totalCount((long) mergedResults.size())
                .page(request.getPage())
                .size(request.getSize())
                .hasNext(calculateHasNext(mergedResults.size(), request))
                .places(sortedResults)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .dataSource("merged")
                .build();
            
            // 캐시에 저장
            cacheService.cacheLocationSearchResult(cacheKey, response, 300); // 5분 캐시
            
            log.info("Location search completed: {} results found", response.getTotalCount());
            return response;
            
        } catch (Exception e) {
            log.error("Error searching locations by keyword: {}", request.getKeyword(), e);
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "위치 검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public NearbyPlacesResponse searchNearbyPlaces(NearbyPlacesRequest request) {
        log.info("Searching nearby places: origin=({}, {}), transport={}, timeRange={}min", 
                request.getOrigin().getLatitude(), request.getOrigin().getLongitude(), 
                request.getTransportMode(), request.getTimeRange());
        
        try {
            // 캐시에서 검색 결과 확인
            String cacheKey = buildNearbyCacheKey(request);
            NearbyPlacesResponse cachedResult = cacheService.getNearbySearchResult(cacheKey);
            if (cachedResult != null) {
                log.debug("Returning cached nearby search result");
                return cachedResult;
            }
            
            long startTime = System.currentTimeMillis();
            
            // 반경 계산 (시간 기반)
            double radiusKm = calculateRadiusByTimeAndTransport(request.getTimeRange(), request.getTransportMode());
            
            // 주변 장소 검색
            List<NearbyPlacesResponse.NearbyPlace> nearbyPlaces = findNearbyPlaces(request, radiusKm);
            
            // 경로 및 시간 정보 계산
            if (request.getIncludeCost() || "travel_time".equals(request.getSort())) {
                enrichWithRouteInformation(nearbyPlaces, request);
            }
            
            // 날씨 정보 추가 (요청 시)
            if (request.getIncludeWeather()) {
                enrichWithWeatherInformation(nearbyPlaces, request);
            }
            
            // AI 추천 정보 추가 (요청 시)
            if (request.getIncludeAI()) {
                enrichWithAIRecommendations(nearbyPlaces);
            }
            
            // 정렬 및 페이징
            List<NearbyPlacesResponse.NearbyPlace> sortedPlaces = applySortingAndPaging(nearbyPlaces, request);
            
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
            
            // 캐시에 저장
            cacheService.cacheNearbySearchResult(cacheKey, response, 600); // 10분 캐시
            
            log.info("Nearby places search completed: {} results found", response.getTotalCount());
            return response;
            
        } catch (Exception e) {
            log.error("Error searching nearby places", e);
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "주변 장소 검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public LocationDetailResponse getLocationDetail(String placeId, Boolean includeAI, Boolean includeReviews, String language) {
        log.info("Getting location detail: placeId={}, includeAI={}, includeReviews={}", placeId, includeAI, includeReviews);
        
        try {
            // 캐시에서 상세 정보 확인
            String cacheKey = buildDetailCacheKey(placeId, includeAI, includeReviews, language);
            LocationDetailResponse cachedDetail = cacheService.getLocationDetail(cacheKey);
            if (cachedDetail != null) {
                log.debug("Returning cached location detail: {}", placeId);
                cachedDetail.setFromCache(true);
                return cachedDetail;
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
            
            // AI 추천 정보 추가 (요청 시)
            if (includeAI != null && includeAI) {
                LocationDetailResponse.AIRecommendation aiRecommendation = getOrGenerateAIRecommendation(placeId);
                response.setAiRecommendation(aiRecommendation);
            }
            
            // 응답 메타데이터 설정
            response.setFromCache(false);
            response.setLastUpdated(LocalDateTime.now());
            
            // 캐시에 저장
            cacheService.cacheLocationDetail(cacheKey, response, 1800); // 30분 캐시
            
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
            
            // AI 서비스에 추천 정보 생성 요청
            // TODO: AI 서비스 클라이언트 구현 후 실제 호출
            log.info("AI recommendation not found in cache, requesting generation: {}", placeId);
            
            // 임시 응답 (AI 서비스에서 생성 중)
            return new Object() {
                public String getMessage() { return "AI 추천정보를 생성 중입니다. 잠시 후 다시 시도해주세요."; }
                public Integer getEstimatedTime() { return 3; }
            };
            
        } catch (Exception e) {
            log.error("Error getting location recommendations: {}", placeId, e);
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "AI 추천 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public LocationDetailResponse.BusinessHours getBusinessHours(String placeId) {
        log.info("Getting business hours: placeId={}", placeId);
        
        try {
            // 외부 API에서 실시간 영업시간 조회
            return externalApiService.getBusinessHours(placeId);
            
        } catch (Exception e) {
            log.error("Error getting business hours: {}", placeId, e);
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "영업시간 조회 중 오류가 발생했습니다: " + e.getMessage());
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
    
    @Override
    public List<LocationSearchResponse.PlaceCard> getPopularLocations(String category, String region, Pageable pageable) {
        Page<Location> locations = locationRepository.findByIsActiveTrueOrderByReviewCountDesc(pageable);
        return locations.getContent().stream()
            .map(this::convertToPlaceCard)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<LocationSearchResponse.PlaceCard> getTopRatedLocations(String category, Integer minReviewCount, Pageable pageable) {
        Page<Location> locations = locationRepository.findTopRatedLocations(pageable);
        return locations.getContent().stream()
            .filter(location -> minReviewCount == null || location.getReviewCount() >= minReviewCount)
            .map(this::convertToPlaceCard)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public LocationDetailResponse syncLocationData(String placeId, Boolean forceUpdate) {
        log.info("Syncing location data: placeId={}, forceUpdate={}", placeId, forceUpdate);
        
        try {
            // 강제 업데이트가 아닌 경우 캐시 무효화만 수행
            if (forceUpdate == null || !forceUpdate) {
                cacheService.evictLocationCache(placeId);
            }
            
            // 외부 API에서 최신 정보 조회
            LocationDetailResponse response = externalApiService.getLocationDetail(placeId, true, "ko");
            
            // 로컬 DB 업데이트
            updateLocalLocationSync(response);
            
            // 캐시 업데이트
            String[] cacheKeys = cacheService.getLocationCacheKeys(placeId);
            for (String cacheKey : cacheKeys) {
                cacheService.evictCache(cacheKey);
            }
            
            log.info("Location data sync completed: {}", placeId);
            return response;
            
        } catch (Exception e) {
            log.error("Error syncing location data: {}", placeId, e);
            throw new BusinessException(ErrorCodes.EXTERNAL_API_ERROR, "위치 데이터 동기화 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    // Helper methods
    private String buildSearchCacheKey(SearchLocationRequest request) {
        return String.format("search:%s:%s:%d:%d:%s:%s", 
            request.getKeyword(), request.getCategory(), 
            request.getRadius(), request.getPage(), 
            request.getSize(), request.getSort());
    }
    
    private String buildNearbyCacheKey(NearbyPlacesRequest request) {
        return String.format("nearby:%s:%s:%d:%s:%d:%d", 
            request.getOrigin().getLatitude(), request.getOrigin().getLongitude(),
            request.getTimeRange(), request.getTransportMode(),
            request.getPage(), request.getSize());
    }
    
    private String buildDetailCacheKey(String placeId, Boolean includeAI, Boolean includeReviews, String language) {
        return String.format("detail:%s:%s:%s:%s", placeId, includeAI, includeReviews, language);
    }
    
    private List<LocationSearchResponse.PlaceCard> searchLocalLocations(SearchLocationRequest request) {
        // 로컬 DB 검색 로직 구현
        return new ArrayList<>();
    }
    
    private List<LocationSearchResponse.PlaceCard> mergeAndDeduplicateResults(
            List<LocationSearchResponse.PlaceCard> local, List<LocationSearchResponse.PlaceCard> external) {
        // 중복 제거 및 병합 로직
        List<LocationSearchResponse.PlaceCard> merged = new ArrayList<>(local);
        merged.addAll(external);
        return merged;
    }
    
    private List<LocationSearchResponse.PlaceCard> applySortingAndPaging(
            List<LocationSearchResponse.PlaceCard> results, SearchLocationRequest request) {
        // 정렬 및 페이징 로직
        return results.stream()
            .skip((long) (request.getPage() - 1) * request.getSize())
            .limit(request.getSize())
            .collect(Collectors.toList());
    }
    
    private Boolean calculateHasNext(int totalSize, SearchLocationRequest request) {
        return totalSize > request.getPage() * request.getSize();
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
    
    // Stub implementations for remaining methods
    @Override
    public List<LocationSearchResponse.PlaceCard> getLocationAutocomplete(String input, BigDecimal latitude, BigDecimal longitude, String language, Integer limit) {
        return new ArrayList<>();
    }
    
    @Override
    public Boolean addLocationBookmark(String userId, String placeId) {
        return true;
    }
    
    @Override
    public Boolean removeLocationBookmark(String userId, String placeId) {
        return true;
    }
    
    @Override
    public List<LocationSearchResponse.PlaceCard> getUserBookmarkedLocations(String userId, Pageable pageable) {
        return new ArrayList<>();
    }
    
    @Override
    public Boolean addLocationVisit(String userId, String placeId, String visitDate) {
        return true;
    }
    
    @Override
    public List<LocationSearchResponse.PlaceCard> getUserVisitedLocations(String userId, Pageable pageable) {
        return new ArrayList<>();
    }
    
    @Override
    public Boolean updateLocationRating(String placeId, BigDecimal rating, Integer reviewCount) {
        return true;
    }
    
    @Override
    public Object getLocationStatistics(String placeId) {
        return new Object();
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
        return new ArrayList<>();
    }
    
    private void enrichWithRouteInformation(List<NearbyPlacesResponse.NearbyPlace> places, NearbyPlacesRequest request) {
        // 경로 정보 추가 로직
    }
    
    private void enrichWithWeatherInformation(List<NearbyPlacesResponse.NearbyPlace> places, NearbyPlacesRequest request) {
        // 날씨 정보 추가 로직
    }
    
    private void enrichWithAIRecommendations(List<NearbyPlacesResponse.NearbyPlace> places) {
        // AI 추천 정보 추가 로직
    }
    
    private List<NearbyPlacesResponse.NearbyPlace> applySortingAndPaging(List<NearbyPlacesResponse.NearbyPlace> places, NearbyPlacesRequest request) {
        return places.stream()
            .skip((long) (request.getPage() - 1) * request.getSize())
            .limit(request.getSize())
            .collect(Collectors.toList());
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
}