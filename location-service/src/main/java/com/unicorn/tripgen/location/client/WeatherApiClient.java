package com.unicorn.tripgen.location.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * 날씨 API 클라이언트
 * OpenWeatherMap API를 통한 날씨 정보 조회
 */
@FeignClient(
    name = "weather-api-client",
    url = "${external.api.weather.openweather.base-url:https://api.openweathermap.org/data/2.5}",
    configuration = com.unicorn.tripgen.location.config.WeatherApiClientConfig.class
)
public interface WeatherApiClient {
    
    /**
     * 현재 날씨 정보 조회
     * 
     * @param lat 위도
     * @param lon 경도
     * @param appid API 키
     * @param units 단위 (metric, imperial, kelvin)
     * @param lang 언어 코드
     * @return 현재 날씨 정보
     */
    @GetMapping("/weather")
    Map<String, Object> getCurrentWeather(
        @RequestParam("lat") Double lat,
        @RequestParam("lon") Double lon,
        @RequestParam("appid") String appid,
        @RequestParam(value = "units", defaultValue = "metric") String units,
        @RequestParam(value = "lang", defaultValue = "ko") String lang
    );
    
    /**
     * 5일 예보 정보 조회 (3시간 간격)
     * 
     * @param lat 위도
     * @param lon 경도
     * @param appid API 키
     * @param units 단위 (metric, imperial, kelvin)
     * @param lang 언어 코드
     * @param cnt 반환할 데이터 포인트 수
     * @return 5일 예보 정보
     */
    @GetMapping("/forecast")
    Map<String, Object> getForecast(
        @RequestParam("lat") Double lat,
        @RequestParam("lon") Double lon,
        @RequestParam("appid") String appid,
        @RequestParam(value = "units", defaultValue = "metric") String units,
        @RequestParam(value = "lang", defaultValue = "ko") String lang,
        @RequestParam(value = "cnt", required = false) Integer cnt
    );
    
    /**
     * 대기오염 정보 조회
     * 
     * @param lat 위도
     * @param lon 경도
     * @param appid API 키
     * @return 대기오염 정보
     */
    @GetMapping("/air_pollution")
    Map<String, Object> getAirPollution(
        @RequestParam("lat") Double lat,
        @RequestParam("lon") Double lon,
        @RequestParam("appid") String appid
    );
    
    /**
     * UV 지수 정보 조회
     * 
     * @param lat 위도
     * @param lon 경도
     * @param appid API 키
     * @return UV 지수 정보
     */
    @GetMapping("/uvi")
    Map<String, Object> getUVIndex(
        @RequestParam("lat") Double lat,
        @RequestParam("lon") Double lon,
        @RequestParam("appid") String appid
    );
    
    /**
     * 도시명으로 현재 날씨 조회
     * 
     * @param q 도시명
     * @param appid API 키
     * @param units 단위
     * @param lang 언어 코드
     * @return 현재 날씨 정보
     */
    @GetMapping("/weather")
    Map<String, Object> getCurrentWeatherByCity(
        @RequestParam("q") String q,
        @RequestParam("appid") String appid,
        @RequestParam(value = "units", defaultValue = "metric") String units,
        @RequestParam(value = "lang", defaultValue = "ko") String lang
    );
    
    /**
     * 도시 ID로 현재 날씨 조회
     * 
     * @param id 도시 ID
     * @param appid API 키
     * @param units 단위
     * @param lang 언어 코드
     * @return 현재 날씨 정보
     */
    @GetMapping("/weather")
    Map<String, Object> getCurrentWeatherByCityId(
        @RequestParam("id") Long id,
        @RequestParam("appid") String appid,
        @RequestParam(value = "units", defaultValue = "metric") String units,
        @RequestParam(value = "lang", defaultValue = "ko") String lang
    );
    
    /**
     * 우편번호로 현재 날씨 조회
     * 
     * @param zip 우편번호,국가코드 (예: 94040,us)
     * @param appid API 키
     * @param units 단위
     * @param lang 언어 코드
     * @return 현재 날씨 정보
     */
    @GetMapping("/weather")
    Map<String, Object> getCurrentWeatherByZip(
        @RequestParam("zip") String zip,
        @RequestParam("appid") String appid,
        @RequestParam(value = "units", defaultValue = "metric") String units,
        @RequestParam(value = "lang", defaultValue = "ko") String lang
    );
}

