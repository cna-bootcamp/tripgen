package com.unicorn.tripgen.location.controller;

import com.unicorn.tripgen.location.dto.route.RouteRequest;
import com.unicorn.tripgen.location.dto.route.RouteResponse;
import com.unicorn.tripgen.location.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Route", description = "경로 정보 API")
public class RouteController {
    
    private final RouteService routeService;
    
    @GetMapping
    @Operation(summary = "경로 조회", description = "출발지와 목적지 간 경로 정보를 조회합니다")
    public ResponseEntity<RouteResponse> getRoute(
            @Parameter(description = "출발지 위도 (예시: 잘츠부르크 중앙역)", required = true, example = "47.8131") 
            @RequestParam Double fromLatitude,
            @Parameter(description = "출발지 경도 (예시: 잘츠부르크 중앙역)", required = true, example = "13.0456") 
            @RequestParam Double fromLongitude,
            @Parameter(description = "도착지 위도 (예시: 오르티세이)", required = true, example = "46.5765") 
            @RequestParam Double toLatitude,
            @Parameter(description = "도착지 경도 (예시: 오르티세이)", required = true, example = "11.6794") 
            @RequestParam Double toLongitude,
            @Parameter(description = "이동 수단 (driving, walking, transit)", example = "driving") 
            @RequestParam(defaultValue = "driving") String mode) {
        
        RouteRequest request = RouteRequest.builder()
                .fromLatitude(fromLatitude)
                .fromLongitude(fromLongitude)
                .toLatitude(toLatitude)
                .toLongitude(toLongitude)
                .mode(mode)
                .build();
        
        log.info("경로 조회 요청: {}", request);
        RouteResponse response = routeService.getRoute(request);
        return ResponseEntity.ok(response);
    }
}