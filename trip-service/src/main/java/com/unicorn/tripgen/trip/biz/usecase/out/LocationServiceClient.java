package com.unicorn.tripgen.trip.biz.usecase.out;

import java.util.List;
import java.util.Optional;

/**
 * Location Service 클라이언트 인터페이스 (Output Port)
 */
public interface LocationServiceClient {
    
    /**
     * 위치 정보
     */
    record LocationInfo(
        String locationId,
        String name,
        String address,
        double latitude,
        double longitude,
        String category,
        String description
    ) {}
    
    /**
     * 날씨 정보
     */
    record WeatherInfo(
        String condition,
        double minTemperature,
        double maxTemperature,
        String icon,
        String description
    ) {}
    
    /**
     * 경로 정보
     */
    record RouteInfo(
        String transportMode,
        int duration,
        double distance,
        String description,
        List<String> steps
    ) {}
    
    /**
     * 위치 정보 조회
     */
    Optional<LocationInfo> getLocationInfo(String locationName);
    
    /**
     * 위치 ID로 상세 정보 조회
     */
    Optional<LocationInfo> getLocationById(String locationId);
    
    /**
     * 날씨 정보 조회
     */
    Optional<WeatherInfo> getWeatherInfo(String locationName, String date);
    
    /**
     * 경로 정보 조회
     */
    Optional<RouteInfo> getRouteInfo(String fromLocation, String toLocation, String transportMode);
    
    /**
     * 주변 장소 검색
     */
    List<LocationInfo> getNearbyPlaces(String locationName, String category, int radius);
    
    /**
     * 위치 유효성 검증
     */
    boolean isValidLocation(String locationName);
    
    /**
     * 좌표로 위치 정보 조회
     */
    Optional<LocationInfo> getLocationByCoordinates(double latitude, double longitude);
}