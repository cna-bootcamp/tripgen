package com.unicorn.tripgen.location.dto.weather;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 날씨 정보 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherResponse {
    
    private Double temperature;
    
    private Double feelsLike;
    
    private String description;
    
    private String icon;
    
    private Integer humidity;
    
    private Double windSpeed;
    
    private Integer cloudiness;
    
    private Long sunrise;
    
    private Long sunset;
}