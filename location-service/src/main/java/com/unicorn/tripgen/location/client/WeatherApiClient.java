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
    url = "${external.api.weather.base-url:https://api.openweathermap.org/data/2.5}"
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

/**
 * WeatherAPI.com 클라이언트 (대안 날씨 API)
 */
@FeignClient(
    name = "weatherapi-client",
    url = "${external.api.weatherapi.base-url:https://api.weatherapi.com/v1}"
)
interface WeatherApiComClient {
    
    /**
     * 현재 날씨 정보 조회 (WeatherAPI.com)
     * 
     * @param key API 키
     * @param q 위치 (위도,경도 또는 도시명)
     * @param lang 언어 코드
     * @param aqi 대기질 정보 포함 여부
     * @return 현재 날씨 정보
     */
    @GetMapping("/current.json")
    Map<String, Object> getCurrentWeather(
        @RequestParam("key") String key,
        @RequestParam("q") String q,
        @RequestParam(value = "lang", defaultValue = "ko") String lang,
        @RequestParam(value = "aqi", defaultValue = "yes") String aqi
    );
    
    /**
     * 예보 정보 조회 (WeatherAPI.com)
     * 
     * @param key API 키
     * @param q 위치 (위도,경도 또는 도시명)
     * @param days 예보 일수 (1-10일)
     * @param lang 언어 코드
     * @param aqi 대기질 정보 포함 여부
     * @param alerts 기상 경보 포함 여부
     * @return 예보 정보
     */
    @GetMapping("/forecast.json")
    Map<String, Object> getForecast(
        @RequestParam("key") String key,
        @RequestParam("q") String q,
        @RequestParam(value = "days", defaultValue = "3") Integer days,
        @RequestParam(value = "lang", defaultValue = "ko") String lang,
        @RequestParam(value = "aqi", defaultValue = "yes") String aqi,
        @RequestParam(value = "alerts", defaultValue = "yes") String alerts
    );
    
    /**
     * 과거 날씨 정보 조회 (WeatherAPI.com)
     * 
     * @param key API 키
     * @param q 위치 (위도,경도 또는 도시명)
     * @param dt 날짜 (yyyy-MM-dd)
     * @param lang 언어 코드
     * @return 과거 날씨 정보
     */
    @GetMapping("/history.json")
    Map<String, Object> getHistoricalWeather(
        @RequestParam("key") String key,
        @RequestParam("q") String q,
        @RequestParam("dt") String dt,
        @RequestParam(value = "lang", defaultValue = "ko") String lang
    );
    
    /**
     * 검색/자동완성 (WeatherAPI.com)
     * 
     * @param key API 키
     * @param q 검색어
     * @return 검색 결과
     */
    @GetMapping("/search.json")
    Map<String, Object> searchLocations(
        @RequestParam("key") String key,
        @RequestParam("q") String q
    );
}