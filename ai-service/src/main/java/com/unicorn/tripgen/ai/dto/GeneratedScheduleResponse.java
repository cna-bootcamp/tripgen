package com.unicorn.tripgen.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 생성된 AI 일정 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeneratedScheduleResponse {
    
    private String requestId;
    private String tripId;
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;
    
    private List<DaySchedule> schedules;
    private ScheduleMetadata metadata;
    
    /**
     * 일자별 일정 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DaySchedule {
        private Integer day;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        
        private String destinationName;
        private WeatherInfo weather;
        private List<PlaceInfo> places;
    }
    
    /**
     * 날씨 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherInfo {
        private String condition;
        private TemperatureInfo temperature;
        private Double precipitation;
        private List<String> warnings;
    }
    
    /**
     * 온도 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemperatureInfo {
        private Double min;
        private Double max;
    }
    
    /**
     * 장소 정보 내부 클래스
     */
    @Data
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
        private WeatherConsiderationInfo weatherConsideration;
    }
    
    /**
     * 교통 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransportationInfo {
        private String type;
        private Integer duration;
        private Double distance;
        private String route;
        private ParkingInfo parkingInfo;
    }
    
    /**
     * 주차 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParkingInfo {
        private String name;
        private String distance;
        private String estimatedCost;
    }
    
    /**
     * 건강 고려사항 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthConsiderationInfo {
        private List<String> restPoints;
        private OptionalActivitiesInfo optionalActivities;
        private List<String> accessibility;
        private Double walkingDistance;
    }
    
    /**
     * 선택적 활동 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionalActivitiesInfo {
        private String basic;
        private String active;
    }
    
    /**
     * 날씨 고려사항 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherConsiderationInfo {
        private String indoorAlternative;
        private List<String> preparationItems;
    }
    
    /**
     * 일정 메타데이터 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleMetadata {
        private Integer totalDays;
        private Integer totalPlaces;
        private String healthStatusConsidered;
        private Boolean weatherConsidered;
    }
}