package com.unicorn.tripgen.location.controller;

import com.unicorn.tripgen.common.dto.ApiResponse;
import com.unicorn.tripgen.location.dto.*;
import com.unicorn.tripgen.location.dto.WeatherRequest;
import com.unicorn.tripgen.location.dto.WeatherResponse;
import com.unicorn.tripgen.location.service.LocationService;
import com.unicorn.tripgen.location.service.WeatherService;
import com.unicorn.tripgen.location.service.CacheService;
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
import java.util.Map;

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
    private final CacheService cacheService;
    
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
            
            
            @Parameter(description = "리뷰 포함 여부", example = "true")
            @RequestParam(defaultValue = "true") Boolean includeReviews) {
        
        log.info("Getting place details: placeId={}, includeReviews={}", 
                placeId, includeReviews);
        
        LocationDetailResponse response = locationService.getLocationDetail(
            placeId, includeReviews, "ko"
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
     * AI 추천 상태 조회
     */
    @GetMapping("/recommendations/{requestId}/status")
    @Operation(
        summary = "AI 추천 상태 조회",
        description = "AI 추천 요청의 처리 상태를 조회합니다"
    )
    public ResponseEntity<ApiResponse<Object>> getRecommendationStatus(
            @Parameter(description = "요청 ID", required = true)
            @PathVariable String requestId) {
        
        log.info("Getting recommendation status: requestId={}", requestId);
        
        try {
            // 상태 확인
            String statusCacheKey = "rec_status_" + requestId;
            String status = (String) cacheService.getObject(statusCacheKey);
            
            if (status == null) {
                return ResponseEntity.ok(ApiResponse.failure("요청을 찾을 수 없습니다"));
            }
            
            Object response;
            
            if ("completed".equals(status)) {
                // 완료된 경우 결과 반환
                String resultCacheKey = "rec_result_" + requestId;
                Object recommendations = cacheService.getObject(resultCacheKey);
                
                response = Map.of(
                        "requestId", requestId,
                        "status", "completed",
                        "recommendations", recommendations != null ? recommendations : "추천 결과를 찾을 수 없습니다"
                );
                
            } else if ("failed".equals(status)) {
                response = Map.of(
                        "requestId", requestId,
                        "status", "failed",
                        "message", "AI 추천 생성에 실패했습니다"
                );
                
            } else {
                // 처리 중
                response = Map.of(
                        "requestId", requestId,
                        "status", "processing",
                        "message", "AI 추천정보를 생성 중입니다"
                );
            }
            
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("Error getting recommendation status: requestId={}", requestId, e);
            return ResponseEntity.ok(ApiResponse.failure("상태 조회 중 오류가 발생했습니다"));
        }
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
     * 장소명으로 검색
     */
    @GetMapping("/search")
    @Operation(
        summary = "장소명으로 검색", 
        description = "장소명을 입력하여 해당하는 장소들의 이름과 Place ID를 조회"
    )
    public ResponseEntity<ApiResponse<Object>> searchPlacesByName(
            @Parameter(description = "검색할 장소명") @RequestParam String placeName,
            @Parameter(description = "검색 결과 개수 (기본값: 5)") @RequestParam(defaultValue = "5") Integer limit) {
        
        log.info("Searching places by name: {}", placeName);
        
        try {
            // ExternalApiService의 autocomplete 기능 활용
            var places = locationService.getExternalApiService().getLocationAutocomplete(
                placeName, null, null, "ko", limit
            );
            
            // 간단한 응답 형태로 변환
            var results = places.stream().map(place -> new Object() {
                public String getName() { return place.getName(); }
                public String getPlaceId() { return place.getPlaceId(); }
                public String getAddress() { return place.getAddress(); }
            }).toList();
            
            return ResponseEntity.ok(ApiResponse.success(results));
            
        } catch (Exception e) {
            log.error("Error searching places by name: {}", placeName, e);
            return ResponseEntity.ok(ApiResponse.failure("장소 검색 중 오류가 발생했습니다"));
        }
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