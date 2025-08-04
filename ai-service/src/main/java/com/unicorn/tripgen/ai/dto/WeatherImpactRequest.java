package com.unicorn.tripgen.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 날씨 영향 분석 요청 DTO
 */
@Data
public class WeatherImpactRequest {
    
    @NotEmpty(message = "일정 정보는 필수입니다")
    @Valid
    private List<ScheduleInfo> schedules;
    
    @NotEmpty(message = "날씨 변화 정보는 필수입니다")
    @Valid
    private List<WeatherChange> weatherChanges;
    
    /**
     * 일정 정보 내부 클래스
     */
    @Data
    public static class ScheduleInfo {
        
        @NotNull(message = "날짜는 필수입니다")
        private Integer day;
        
        @NotNull(message = "일정 날짜는 필수입니다")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        
        @Valid
        private List<PlaceInfo> places;
        
        /**
         * 장소 정보 내부 클래스
         */
        @Data
        public static class PlaceInfo {
            private String placeId;
            private Boolean isOutdoor;
        }
    }
    
    /**
     * 날씨 변화 정보 내부 클래스
     */
    @Data
    public static class WeatherChange {
        
        @NotNull(message = "날짜는 필수입니다")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        
        private String previousCondition;
        private String newCondition;
        
        @Pattern(regexp = "^(low|medium|high)$", message = "심각도는 low, medium, high 중 하나여야 합니다")
        private String severity;
    }
}