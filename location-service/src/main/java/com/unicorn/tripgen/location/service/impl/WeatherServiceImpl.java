package com.unicorn.tripgen.location.service.impl;

import com.unicorn.tripgen.location.client.WeatherApiClient;
import com.unicorn.tripgen.location.dto.WeatherRequest;
import com.unicorn.tripgen.location.dto.WeatherResponse;
import com.unicorn.tripgen.location.service.WeatherService;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WeatherServiceImpl implements WeatherService {
    
    private final WeatherApiClient weatherApiClient;
    private final String apiKey;
    
    public WeatherServiceImpl(
            WeatherApiClient weatherApiClient,
            @Value("${external.api.weather.openweather.api-key}") String apiKey) {
        this.weatherApiClient = weatherApiClient;
        this.apiKey = apiKey;
    }
    
    @Override
    @Cacheable(value = "weather", key = "#request.latitude + '_' + #request.longitude", unless = "#result == null")
    public WeatherResponse getWeather(WeatherRequest request) {
        log.info("날씨 정보 조회 요청: lat={}, lon={}, date={}", 
                request.getLatitude(), request.getLongitude(), request.getDate());
        
        try {
            // OpenWeatherMap API 호출
            Map<String, Object> weatherData = weatherApiClient.getCurrentWeather(
                    request.getLatitude(),
                    request.getLongitude(),
                    apiKey,
                    "metric",
                    "ko"
            );
            
            // 응답 데이터 파싱
            return parseWeatherResponse(weatherData);
            
        } catch (FeignException e) {
            log.error("OpenWeatherMap API 호출 실패: status={}, message={}", 
                    e.status(), e.getMessage());
            
            // API 키 문제인 경우
            if (e.status() == 401) {
                log.error("Invalid API key: {}", apiKey);
                throw new RuntimeException("날씨 API 인증 실패");
            }
            
            // 기본값 반환
            return getDefaultWeatherResponse();
            
        } catch (Exception e) {
            log.error("날씨 정보 조회 중 오류 발생", e);
            return getDefaultWeatherResponse();
        }
    }
    
    /**
     * OpenWeatherMap API 응답 데이터 파싱
     */
    private WeatherResponse parseWeatherResponse(Map<String, Object> weatherData) {
        try {
            // main 섹션에서 온도, 습도 정보 추출
            Map<String, Object> main = (Map<String, Object>) weatherData.get("main");
            Double temperature = getDoubleValue(main.get("temp"));
            Double feelsLike = getDoubleValue(main.get("feels_like"));
            Integer humidity = getIntegerValue(main.get("humidity"));
            
            // weather 배열에서 날씨 설명 추출
            List<Map<String, Object>> weatherList = (List<Map<String, Object>>) weatherData.get("weather");
            String description = "";
            String mainWeather = "";
            String icon = "";
            if (weatherList != null && !weatherList.isEmpty()) {
                Map<String, Object> weather = weatherList.get(0);
                description = (String) weather.get("description");
                mainWeather = (String) weather.get("main");
                icon = (String) weather.get("icon");
            }
            
            // wind 섹션에서 바람 정보 추출
            Map<String, Object> wind = (Map<String, Object>) weatherData.get("wind");
            Double windSpeed = wind != null ? getDoubleValue(wind.get("speed")) : 0.0;
            Double windDirection = wind != null ? getDoubleValue(wind.get("deg")) : 0.0;
            
            // clouds 섹션에서 구름 정보 추출
            Map<String, Object> clouds = (Map<String, Object>) weatherData.get("clouds");
            Integer cloudiness = clouds != null ? getIntegerValue(clouds.get("all")) : 0;
            
            // sys 섹션에서 일출/일몰 시간 추출
            Map<String, Object> sys = (Map<String, Object>) weatherData.get("sys");
            Long sunrise = sys != null ? getLongValue(sys.get("sunrise")) : null;
            Long sunset = sys != null ? getLongValue(sys.get("sunset")) : null;
            
            // rain 섹션에서 강수량 정보 추출 (있는 경우)
            Map<String, Object> rain = (Map<String, Object>) weatherData.get("rain");
            Double rainVolume = null;
            if (rain != null) {
                rainVolume = getDoubleValue(rain.get("1h")); // 1시간 강수량
                if (rainVolume == null) {
                    rainVolume = getDoubleValue(rain.get("3h")); // 3시간 강수량
                }
            }
            
            // visibility 추출
            Integer visibility = weatherData.get("visibility") != null ? 
                    getIntegerValue(weatherData.get("visibility")) : null;
            
            log.info("날씨 정보 파싱 완료: temp={}, desc={}, humidity={}, wind={}", 
                    temperature, description, humidity, windSpeed);
            
            return WeatherResponse.builder()
                    .temperature(temperature)
                    .feelsLike(feelsLike)
                    .description(description)
                    .mainWeather(mainWeather)
                    .humidity(humidity)
                    .windSpeed(windSpeed)
                    .windDirection(windDirection)
                    .cloudiness(cloudiness)
                    .visibility(visibility)
                    .rainVolume(rainVolume)
                    .sunrise(sunrise)
                    .sunset(sunset)
                    .icon(icon)
                    .build();
                    
        } catch (Exception e) {
            log.error("날씨 데이터 파싱 중 오류 발생", e);
            return getDefaultWeatherResponse();
        }
    }
    
    /**
     * 기본 날씨 응답 데이터
     */
    private WeatherResponse getDefaultWeatherResponse() {
        log.debug("기본 날씨 정보 반환");
        return WeatherResponse.builder()
                .temperature(20.0)
                .feelsLike(20.0)
                .description("날씨 정보를 가져올 수 없습니다")
                .mainWeather("Unknown")
                .humidity(50)
                .windSpeed(0.0)
                .windDirection(0.0)
                .cloudiness(0)
                .build();
    }
    
    /**
     * Object를 Double로 안전하게 변환
     */
    private Double getDoubleValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Object를 Integer로 안전하게 변환
     */
    private Integer getIntegerValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Object를 Long으로 안전하게 변환
     */
    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}