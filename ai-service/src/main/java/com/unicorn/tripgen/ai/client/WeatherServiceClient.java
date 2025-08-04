package com.unicorn.tripgen.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Weather Service 클라이언트
 * 날씨 정보 조회를 위한 Feign 클라이언트
 */
@FeignClient(
    name = "location-service",
    url = "${external-services.location-service.url:http://localhost:8082}",
    path = "/api/v1/locations/weather"
)
public interface WeatherServiceClient {
    
    /**
     * 현재 날씨 조회
     */
    @GetMapping
    Map<String, Object> getCurrentWeather(
        @RequestParam("latitude") double latitude,
        @RequestParam("longitude") double longitude
    );
    
    /**
     * 일별 날씨 예보 조회
     */
    @GetMapping("/forecast")
    List<Map<String, Object>> getWeatherForecast(
        @RequestParam("latitude") double latitude,
        @RequestParam("longitude") double longitude,
        @RequestParam("startDate") LocalDate startDate,
        @RequestParam("endDate") LocalDate endDate
    );
    
    /**
     * 특정 날짜의 날씨 조회
     */
    @GetMapping("/date")
    Map<String, Object> getWeatherByDate(
        @RequestParam("latitude") double latitude,
        @RequestParam("longitude") double longitude,
        @RequestParam("date") LocalDate date
    );
    
    /**
     * 여러 위치의 날씨 일괄 조회
     */
    @GetMapping("/batch")
    List<Map<String, Object>> getWeatherBatch(
        @RequestParam("locations") List<String> locations,
        @RequestParam("date") LocalDate date
    );
    
    /**
     * 날씨 경고 정보 조회
     */
    @GetMapping("/alerts")
    List<Map<String, Object>> getWeatherAlerts(
        @RequestParam("latitude") double latitude,
        @RequestParam("longitude") double longitude
    );
}