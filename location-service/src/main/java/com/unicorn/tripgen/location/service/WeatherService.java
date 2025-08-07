package com.unicorn.tripgen.location.service;

import com.unicorn.tripgen.location.dto.WeatherRequest;
import com.unicorn.tripgen.location.dto.WeatherResponse;

public interface WeatherService {
    
    /**
     * 특정 위치의 날씨 정보 조회
     * 
     * @param request 날씨 조회 요청 정보
     * @return 날씨 정보 응답
     */
    WeatherResponse getWeather(WeatherRequest request);
}