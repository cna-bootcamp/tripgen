package com.unicorn.tripgen.trip.infra.client;

import com.unicorn.tripgen.trip.biz.usecase.out.LocationServiceClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Location Service Feign Client 구현
 */
@FeignClient(name = "location-service", url = "${LOCATION_SERVICE_URL:http://localhost:8084}")
public interface LocationServiceFeignClient extends LocationServiceClient {
    
    @Override
    @GetMapping("/api/v1/locations")
    Optional<LocationInfo> getLocationInfo(@RequestParam String locationName);
    
    @Override
    @GetMapping("/api/v1/locations/{locationId}")
    Optional<LocationInfo> getLocationById(@PathVariable String locationId);
    
    @Override
    @GetMapping("/api/v1/weather")
    Optional<WeatherInfo> getWeatherInfo(
            @RequestParam String locationName, 
            @RequestParam String date);
    
    @Override
    @GetMapping("/api/v1/routes")
    Optional<RouteInfo> getRouteInfo(
            @RequestParam String fromLocation,
            @RequestParam String toLocation,
            @RequestParam String transportMode);
    
    @Override
    @GetMapping("/api/v1/locations/nearby")
    List<LocationInfo> getNearbyPlaces(
            @RequestParam String locationName,
            @RequestParam String category,
            @RequestParam int radius);
    
    @Override
    @GetMapping("/api/v1/locations/validate")
    boolean isValidLocation(@RequestParam String locationName);
    
    @Override
    @GetMapping("/api/v1/locations/coordinates")
    Optional<LocationInfo> getLocationByCoordinates(
            @RequestParam double latitude,
            @RequestParam double longitude);
}