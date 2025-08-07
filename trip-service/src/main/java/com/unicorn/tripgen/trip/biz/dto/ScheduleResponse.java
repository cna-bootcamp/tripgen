package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.domain.Schedule;
import com.unicorn.tripgen.trip.biz.domain.SchedulePlace;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 일정 응답 DTO
 * API 설계서 스키마를 준수하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {
    
    private Integer day;
    private LocalDate date;
    private String city;
    private WeatherInfo weather;
    private List<PlaceInfo> places;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherInfo {
        private String condition;
        private TemperatureInfo temperature;
        private String icon;
    }
    
    @Getter
    @Builder  
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemperatureInfo {
        private Double min;
        private Double max;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceInfo {
        private String placeId;
        private String placeName;
        private String category;
        private String startTime;
        private Integer duration;
        private TransportationInfo transportation;
        private HealthConsiderationInfo healthConsideration;
        private Integer order;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransportationInfo {
        private String type;
        private Integer duration;
        private Double distance;
        private String route;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthConsiderationInfo {
        private List<String> restPoints;
        private List<String> accessibility;
        private Double walkingDistance;
    }
    
    public static ScheduleResponse from(Schedule schedule) {
        return ScheduleResponse.builder()
            .day(schedule.getDay())
            .date(schedule.getDate())
            .city(schedule.getCity())
            .weather(schedule.getWeather() != null ? WeatherInfo.builder()
                .condition(schedule.getWeather().getCondition())
                .temperature(TemperatureInfo.builder()
                    .min(schedule.getWeather().getMinTemperature())
                    .max(schedule.getWeather().getMaxTemperature())
                    .build())
                .icon(schedule.getWeather().getIcon())
                .build() : null)
            .places(schedule.getPlaces() != null ? 
                schedule.getPlaces().stream()
                        .map(ScheduleResponse::convertToPlaceInfo)
                        .toList() : List.of())
            .build();
    }
    
    private static PlaceInfo convertToPlaceInfo(SchedulePlace place) {
        return PlaceInfo.builder()
            .placeId(place.getPlaceId())
            .placeName(place.getPlaceName())
            .category(place.getCategory())
            .startTime(place.getStartTime() != null ? place.getStartTime().toString() : null)
            .duration(place.getDuration())
            .transportation(place.getTransportation() != null ? TransportationInfo.builder()
                .type(place.getTransportation().getType().name().toLowerCase())
                .duration(place.getTransportation().getDuration())
                .distance(place.getTransportation().getDistance())
                .route(place.getTransportation().getRoute())
                .build() : null)
            .healthConsideration(place.getHealthConsideration() != null ? HealthConsiderationInfo.builder()
                .restPoints(place.getHealthConsideration().getRestPoints())
                .accessibility(place.getHealthConsideration().getAccessibility() != null ?
                    place.getHealthConsideration().getAccessibility().stream()
                                                  .map(a -> a.name().toLowerCase())
                                                  .toList() : List.of())
                .walkingDistance(place.getHealthConsideration().getWalkingDistance())
                .build() : null)
            .order(place.getOrder())
            .build();
    }
}