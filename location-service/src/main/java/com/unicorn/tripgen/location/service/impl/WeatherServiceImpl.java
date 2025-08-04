package com.unicorn.tripgen.location.service.impl;

import com.unicorn.tripgen.location.dto.weather.WeatherRequest;
import com.unicorn.tripgen.location.dto.weather.WeatherResponse;
import com.unicorn.tripgen.location.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherServiceImpl implements WeatherService {
    
    @Override
    public WeatherResponse getWeather(WeatherRequest request) {
        log.info("날씨 정보 조회 요청: lat={}, lon={}", 
                request.getLatitude(), request.getLongitude());
        
        // TODO: OpenWeatherMap API 연동 구현
        
        // 임시 응답 데이터
        return WeatherResponse.builder()
                .temperature(25.0)
                .description("맑음")
                .humidity(60)
                .windSpeed(5.5)
                .build();
    }
}