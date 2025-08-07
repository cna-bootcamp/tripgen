package com.unicorn.tripgen.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 날씨 정보 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherInfo {
    
    /**
     * 기온
     */
    private BigDecimal temperature;
    
    /**
     * 날씨 상태
     */
    private String condition;
    
    /**
     * 날씨 설명
     */
    private String description;
    
    /**
     * 강수 확률
     */
    private Integer precipitationProbability;
    
    /**
     * 여행 적합성 점수 (1-10)
     */
    private Integer suitabilityScore;
}