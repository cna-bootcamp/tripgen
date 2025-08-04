package com.unicorn.tripgen.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Location Service 클라이언트
 * 위치 및 경로 정보 조회를 위한 Feign 클라이언트
 */
@FeignClient(
    name = "location-service",
    url = "${external-services.location-service.url:http://localhost:8082}",
    path = "/api/v1/locations"
)
public interface LocationServiceClient {
    
    /**
     * 위치 검색
     */
    @GetMapping("/search")
    Map<String, Object> searchLocations(
        @RequestParam("query") String query,
        @RequestParam(value = "limit", defaultValue = "10") int limit
    );
    
    /**
     * 위치 상세 정보 조회
     */
    @GetMapping("/{locationId}")
    Map<String, Object> getLocationDetail(@PathVariable("locationId") String locationId);
    
    /**
     * 경로 정보 조회
     */
    @GetMapping("/route")
    Map<String, Object> getRoute(
        @RequestParam("origin") String origin,
        @RequestParam("destination") String destination,
        @RequestParam(value = "mode", defaultValue = "driving") String mode
    );
    
    /**
     * 주변 장소 검색
     */
    @GetMapping("/nearby")
    List<Map<String, Object>> getNearbyPlaces(
        @RequestParam("latitude") double latitude,
        @RequestParam("longitude") double longitude,
        @RequestParam(value = "radius", defaultValue = "1000") int radius,
        @RequestParam(value = "type", required = false) String type
    );
    
    /**
     * 여러 위치 정보 일괄 조회
     */
    @GetMapping("/batch")
    List<Map<String, Object>> getLocationsBatch(@RequestParam("ids") List<String> locationIds);
}