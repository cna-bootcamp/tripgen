package com.unicorn.tripgen.location.controller;

import com.unicorn.tripgen.common.dto.ApiResponse;
import com.unicorn.tripgen.location.dto.*;
import com.unicorn.tripgen.location.dto.weather.WeatherRequest;
import com.unicorn.tripgen.location.dto.weather.WeatherResponse;
import com.unicorn.tripgen.location.dto.route.RouteRequest;
import com.unicorn.tripgen.location.dto.route.RouteResponse;
import com.unicorn.tripgen.location.service.LocationService;
import com.unicorn.tripgen.location.service.WeatherService;
import com.unicorn.tripgen.location.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 위치 서비스 컨트롤러
 * 장소 검색, 상세 정보 조회, 날씨 정보, 경로 정보 등을 제공
 */
@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Location Service", description = "장소 검색 및 상세정보 제공 서비스 API")
public class LocationController {
    
    private final LocationService locationService;
    private final WeatherService weatherService;
    private final RouteService routeService;
    
    /**
     * 주변 장소 검색
     */
    @PostMapping("/search/nearby")
    @Operation(
        summary = "주변 장소 검색",
        description = "출발지 기준으로 이동수단과 시간 범위에 따른 주변 장소 검색"
    )
    public ResponseEntity<ApiResponse<NearbyPlacesResponse>> searchNearbyPlaces(
            @Valid @RequestBody NearbyPlacesRequest request) {
        log.info("Searching nearby places: transport={}, timeRange={}min", 
                request.getTransportMode(), request.getTimeRange());
        
        NearbyPlacesResponse response = locationService.searchNearbyPlaces(request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 키워드 검색
     */
    @GetMapping("/search/keyword")
    @Operation(
        summary = "키워드 검색",
        description = "장소명이나 카테고리로 장소 검색"
    )
    public ResponseEntity<ApiResponse<LocationSearchResponse>> searchByKeyword(
            @Parameter(description = "검색 키워드", required = true, example = "마리엔플라츠")
            @RequestParam String keyword,
            
            @Parameter(description = "검색 기준 위도", required = true, example = "48.1374")
            @RequestParam BigDecimal latitude,
            
            @Parameter(description = "검색 기준 경도", required = true, example = "11.5755")
            @RequestParam BigDecimal longitude,
            
            @Parameter(description = "검색 반경 (미터)", example = "5000")
            @RequestParam(defaultValue = "5000") Integer radius,
            
            @Parameter(description = "카테고리 필터", example = "tourist")
            @RequestParam(defaultValue = "all") String category,
            
            @Parameter(description = "정렬 기준", example = "distance")
            @RequestParam(defaultValue = "distance") String sort,
            
            @Parameter(description = "페이지 번호", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {
        
        log.info("Searching locations by keyword: {} at ({}, {})", keyword, latitude, longitude);
        
        SearchLocationRequest request = SearchLocationRequest.builder()
            .keyword(keyword)
            .latitude(latitude)
            .longitude(longitude)
            .radius(radius)
            .category(category)
            .sort(sort)
            .page(page)
            .size(size)
            .build();
        
        LocationSearchResponse response = locationService.searchLocationsByKeyword(request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 장소 상세정보 조회
     */
    @GetMapping("/places/{placeId}")
    @Operation(
        summary = "장소 상세정보 조회",
        description = "특정 장소의 상세정보, 운영시간, AI 추천정보 조회"
    )
    public ResponseEntity<ApiResponse<LocationDetailResponse>> getPlaceDetails(
            @Parameter(description = "장소 ID", required = true, example = "ChIJU5LMbCt1nkcRGwGLFraPTBg")
            @PathVariable String placeId,
            
            @Parameter(description = "AI 추천정보 포함 여부", example = "true")
            @RequestParam(defaultValue = "true") Boolean includeAI,
            
            @Parameter(description = "리뷰 포함 여부", example = "true")
            @RequestParam(defaultValue = "true") Boolean includeReviews) {
        
        log.info("Getting place details: placeId={}, includeAI={}, includeReviews={}", 
                placeId, includeAI, includeReviews);
        
        LocationDetailResponse response = locationService.getLocationDetail(
            placeId, includeAI, includeReviews, "ko"
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 장소의 AI 추천정보 조회
     */
    @GetMapping("/locations/{placeId}/recommendations")
    @Operation(
        summary = "장소의 AI 추천정보 조회",
        description = "특정 장소에 대한 AI 추천정보를 조회합니다 (캐시 우선)"
    )
    public ResponseEntity<ApiResponse<Object>> getPlaceRecommendations(
            @Parameter(description = "장소 ID", required = true)
            @PathVariable String placeId,
            
            @Parameter(description = "여행 ID (사용자 프로필 조회용)")
            @RequestParam(required = false) String tripId) {
        
        log.info("Getting place recommendations: placeId={}, tripId={}", placeId, tripId);
        
        Object response = locationService.getLocationRecommendations(placeId, tripId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 실시간 영업시간 조회
     */
    @GetMapping("/places/{placeId}/business-hours")
    @Operation(
        summary = "실시간 영업시간 조회",
        description = "장소의 실시간 영업 상태 및 영업시간 조회"
    )
    public ResponseEntity<ApiResponse<LocationDetailResponse.BusinessHours>> getBusinessHours(
            @Parameter(description = "장소 ID", required = true, example = "ChIJU5LMbCt1nkcRGwGLFraPTBg")
            @PathVariable String placeId) {
        
        log.info("Getting business hours: placeId={}", placeId);
        
        LocationDetailResponse.BusinessHours response = locationService.getBusinessHours(placeId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 날씨 정보 조회
     */
    @GetMapping("/weather")
    @Operation(
        summary = "날씨 정보 조회",
        description = "특정 위치의 현재 날씨 및 예보 정보 조회"
    )
    public ResponseEntity<ApiResponse<WeatherResponse>> getWeather(
            @Parameter(description = "위도", required = true, example = "48.1374")
            @RequestParam BigDecimal latitude,
            
            @Parameter(description = "경도", required = true, example = "11.5755")
            @RequestParam BigDecimal longitude,
            
            @Parameter(description = "언어 코드", example = "ko")
            @RequestParam(defaultValue = "ko") String language,
            
            @Parameter(description = "예보 정보 포함 여부", example = "false")
            @RequestParam(defaultValue = "false") Boolean includeForecast,
            
            @Parameter(description = "예보 일수", example = "3")
            @RequestParam(defaultValue = "3") Integer forecastDays) {
        
        log.info("Getting weather info: ({}, {}), includeForecast={}", latitude, longitude, includeForecast);
        
        WeatherRequest request = WeatherRequest.builder()
            .latitude(latitude.doubleValue())
            .longitude(longitude.doubleValue())
            .lang(language)
            .build();
        
        WeatherResponse response = weatherService.getWeather(request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 경로 정보 조회
     */
    @GetMapping("/route")
    @Operation(
        summary = "경로 정보 조회",
        description = "출발지와 목적지 간의 경로 정보 조회"
    )
    public ResponseEntity<ApiResponse<RouteResponse>> getRoute(
            @Parameter(description = "출발지 위도", required = true, example = "48.1374")
            @RequestParam("origin_lat") BigDecimal originLat,
            
            @Parameter(description = "출발지 경도", required = true, example = "11.5755")
            @RequestParam("origin_lng") BigDecimal originLng,
            
            @Parameter(description = "목적지 위도", required = true, example = "48.1351")
            @RequestParam("dest_lat") BigDecimal destLat,
            
            @Parameter(description = "목적지 경도", required = true, example = "11.5820")
            @RequestParam("dest_lng") BigDecimal destLng,
            
            @Parameter(description = "이동수단", example = "public_transport")
            @RequestParam(defaultValue = "public_transport") String transportMode,
            
            @Parameter(description = "대안 경로 포함 여부", example = "false")
            @RequestParam(defaultValue = "false") Boolean includeAlternatives,
            
            @Parameter(description = "언어 코드", example = "ko")
            @RequestParam(defaultValue = "ko") String language) {
        
        log.info("Getting route info: ({}, {}) -> ({}, {}), transport={}", 
                originLat, originLng, destLat, destLng, transportMode);
        
        RouteRequest request = RouteRequest.builder()
            .fromLatitude(originLat.doubleValue())
            .fromLongitude(originLng.doubleValue())
            .toLatitude(destLat.doubleValue())
            .toLongitude(destLng.doubleValue())
            .mode(transportMode)
            .build();
        
        RouteResponse response = routeService.getRoute(request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 인기 장소 목록 조회
     */
    @GetMapping("/popular")
    @Operation(
        summary = "인기 장소 목록 조회",
        description = "리뷰 수 기준 인기 장소 목록 조회"
    )
    public ResponseEntity<ApiResponse<Object>> getPopularLocations(
            @Parameter(description = "카테고리 필터")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "지역 필터")
            @RequestParam(required = false) String region,
            
            @Parameter(description = "페이지 번호", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {
        
        log.info("Getting popular locations: category={}, region={}", category, region);
        
        Pageable pageable = PageRequest.of(page - 1, size);
        Object response = locationService.getPopularLocations(category, region, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 최고 평점 장소 목록 조회
     */
    @GetMapping("/top-rated")
    @Operation(
        summary = "최고 평점 장소 목록 조회",
        description = "평점 기준 최고 평점 장소 목록 조회"
    )
    public ResponseEntity<ApiResponse<Object>> getTopRatedLocations(
            @Parameter(description = "카테고리 필터")
            @RequestParam(required = false) String category,
            
            @Parameter(description = "최소 리뷰 수", example = "10")
            @RequestParam(required = false) Integer minReviewCount,
            
            @Parameter(description = "페이지 번호", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") Integer size) {
        
        log.info("Getting top-rated locations: category={}, minReviewCount={}", category, minReviewCount);
        
        Pageable pageable = PageRequest.of(page - 1, size);
        Object response = locationService.getTopRatedLocations(category, minReviewCount, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 위치 자동완성
     */
    @GetMapping("/autocomplete")
    @Operation(
        summary = "위치 자동완성",
        description = "입력 텍스트에 대한 위치 자동완성 결과 제공"
    )
    public ResponseEntity<ApiResponse<Object>> getLocationAutocomplete(
            @Parameter(description = "입력 텍스트", required = true, example = "마리")
            @RequestParam String input,
            
            @Parameter(description = "검색 중심 위도")
            @RequestParam(required = false) BigDecimal latitude,
            
            @Parameter(description = "검색 중심 경도")
            @RequestParam(required = false) BigDecimal longitude,
            
            @Parameter(description = "언어 코드", example = "ko")
            @RequestParam(defaultValue = "ko") String language,
            
            @Parameter(description = "결과 개수 제한", example = "10")
            @RequestParam(defaultValue = "10") Integer limit) {
        
        log.info("Getting location autocomplete: input={}", input);
        
        Object response = locationService.getLocationAutocomplete(
            input, latitude, longitude, language, limit
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 위치 데이터 동기화
     */
    @PostMapping("/places/{placeId}/sync")
    @Operation(
        summary = "위치 데이터 동기화",
        description = "외부 API에서 최신 정보를 가져와서 로컬 데이터 업데이트"
    )
    public ResponseEntity<ApiResponse<LocationDetailResponse>> syncLocationData(
            @Parameter(description = "장소 ID", required = true)
            @PathVariable String placeId,
            
            @Parameter(description = "강제 업데이트 여부", example = "false")
            @RequestParam(defaultValue = "false") Boolean forceUpdate) {
        
        log.info("Syncing location data: placeId={}, forceUpdate={}", placeId, forceUpdate);
        
        LocationDetailResponse response = locationService.syncLocationData(placeId, forceUpdate);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 서비스 상태 확인
     */
    @GetMapping("/health")
    @Operation(
        summary = "서비스 상태 확인",
        description = "Location Service의 상태 및 외부 API 연결 상태 확인"
    )
    public ResponseEntity<ApiResponse<Object>> getHealthStatus() {
        log.info("Checking location service health");
        
        Object status = new Object() {
            public String getService() { return "location-service"; }
            public String getStatus() { return "healthy"; }
            public String getVersion() { return "1.0.0"; }
            public java.time.LocalDateTime getTimestamp() { return java.time.LocalDateTime.now(); }
        };
        
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}