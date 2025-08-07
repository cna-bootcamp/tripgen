package com.unicorn.tripgen.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 날씨 정보 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherResponse implements Serializable {
    
    private Double temperature;
    
    private Double feelsLike;
    
    private String description;
    
    private String mainWeather;
    
    private String icon;
    
    private Integer humidity;
    
    private Double windSpeed;
    
    private Double windDirection;
    
    private Integer cloudiness;
    
    private Integer visibility;
    
    private Double rainVolume;
    
    private Long sunrise;
    
    private Long sunset;
}