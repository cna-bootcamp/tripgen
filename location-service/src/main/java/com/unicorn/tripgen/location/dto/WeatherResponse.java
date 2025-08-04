package com.unicorn.tripgen.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 날씨 정보 조회 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherResponse {
    
    /**
     * 위도
     */
    private BigDecimal latitude;
    
    /**
     * 경도
     */
    private BigDecimal longitude;
    
    /**
     * 지역명
     */
    private String locationName;
    
    /**
     * 날씨 조회 날짜
     */
    private LocalDate date;
    
    /**
     * 현재 날씨 정보
     */
    private CurrentWeather current;
    
    /**
     * 예보 정보 (요청 시)
     */
    private List<DailyForecast> forecast;
    
    /**
     * 여행 적합성 정보
     */
    private TravelSuitability travelSuitability;
    
    /**
     * 데이터 소스
     */
    private String dataSource;
    
    /**
     * 캐시에서 가져온 데이터 여부
     */
    private Boolean fromCache;
    
    /**
     * 데이터 업데이트 시간
     */
    private LocalDateTime lastUpdated;
    
    /**
     * 응답 시간
     */
    private LocalDateTime responseTime;
    
    /**
     * 현재 날씨 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CurrentWeather {
        
        /**
         * 기온
         */
        private BigDecimal temperature;
        
        /**
         * 체감 온도
         */
        private BigDecimal feelsLike;
        
        /**
         * 최고 기온
         */
        private BigDecimal tempMax;
        
        /**
         * 최저 기온
         */
        private BigDecimal tempMin;
        
        /**
         * 습도 (%)
         */
        private Integer humidity;
        
        /**
         * 기압 (hPa)
         */
        private Integer pressure;
        
        /**
         * 날씨 상태
         */
        private String condition;
        
        /**
         * 날씨 설명
         */
        private String description;
        
        /**
         * 풍속 (m/s)
         */
        private BigDecimal windSpeed;
        
        /**
         * 풍향 (도)
         */
        private Integer windDirection;
        
        /**
         * 풍향 설명 (북, 남, 동, 서 등)
         */
        private String windDirectionText;
        
        /**
         * 구름량 (%)
         */
        private Integer cloudCoverage;
        
        /**
         * 강수 확률 (%)
         */
        private Integer precipitationProbability;
        
        /**
         * 강수량 (mm)
         */
        private BigDecimal precipitationAmount;
        
        /**
         * 자외선 지수
         */
        private BigDecimal uvIndex;
        
        /**
         * 자외선 지수 설명
         */
        private String uvIndexText;
        
        /**
         * 일출 시간
         */
        private LocalDateTime sunrise;
        
        /**
         * 일몰 시간
         */
        private LocalDateTime sunset;
        
        /**
         * 날씨 아이콘 코드
         */
        private String weatherIcon;
        
        /**
         * 날씨 아이콘 URL
         */
        private String weatherIconUrl;
        
        /**
         * 시야 거리 (km)
         */
        private BigDecimal visibility;
    }
    
    /**
     * 일일 예보 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyForecast {
        
        /**
         * 예보 날짜
         */
        private LocalDate date;
        
        /**
         * 요일
         */
        private String dayOfWeek;
        
        /**
         * 최고 기온
         */
        private BigDecimal tempMax;
        
        /**
         * 최저 기온
         */
        private BigDecimal tempMin;
        
        /**
         * 평균 기온
         */
        private BigDecimal tempAvg;
        
        /**
         * 날씨 상태
         */
        private String condition;
        
        /**
         * 날씨 설명
         */
        private String description;
        
        /**
         * 강수 확률 (%)
         */
        private Integer precipitationProbability;
        
        /**
         * 강수량 (mm)
         */
        private BigDecimal precipitationAmount;
        
        /**
         * 습도 (%)
         */
        private Integer humidity;
        
        /**
         * 풍속 (m/s)
         */
        private BigDecimal windSpeed;
        
        /**
         * 자외선 지수
         */
        private BigDecimal uvIndex;
        
        /**
         * 날씨 아이콘 코드
         */
        private String weatherIcon;
        
        /**
         * 날씨 아이콘 URL
         */
        private String weatherIconUrl;
        
        /**
         * 일출 시간
         */
        private LocalDateTime sunrise;
        
        /**
         * 일몰 시간
         */
        private LocalDateTime sunset;
        
        /**
         * 여행 적합성 점수 (1-10)
         */
        private Integer travelScore;
    }
    
    /**
     * 여행 적합성 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TravelSuitability {
        
        /**
         * 여행 적합성 점수 (1-10, 높을수록 좋음)
         */
        private Integer score;
        
        /**
         * 적합성 등급 (EXCELLENT, GOOD, FAIR, POOR)
         */
        private String grade;
        
        /**
         * 종합 평가
         */
        private String summary;
        
        /**
         * 옷차림 추천
         */
        private String clothingRecommendation;
        
        /**
         * 준비물 추천
         */
        private List<String> recommendedItems;
        
        /**
         * 주의사항
         */
        private List<String> warnings;
        
        /**
         * 활동 추천
         */
        private List<String> recommendedActivities;
        
        /**
         * 피해야 할 활동
         */
        private List<String> activitiesToAvoid;
        
        /**
         * 최적 외출 시간
         */
        private String bestTimeToGoOut;
        
        /**
         * 실내 활동 추천 여부
         */
        private Boolean recommendIndoorActivities;
    }
}