package com.unicorn.tripgen.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 날씨 영향 분석 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WeatherImpactResponse {
    
    private List<ImpactedDay> impactedDays;
    
    /**
     * 영향받은 날짜 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImpactedDay {
        
        private Integer day;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        
        private String reason;
        private List<String> affectedPlaces;
        private String recommendation;
        private List<String> alternativeOptions;
    }
}