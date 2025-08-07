package com.unicorn.tripgen.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 추천 요청 메시지 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequest {
    
    private String requestId;
    private String placeId;
    private String tripId;
    private String context; // "search" 또는 "trip"
    private SearchContext searchContext;
    private PlaceInfo placeInfo;
    private WeatherInfo weatherInfo;
    private LocalDateTime requestTime;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchContext {
        private String searchQuery;      // 검색 쿼리 (예: "일반 추천", "가족 여행", "비즈니스")
        private String[] searchIntents;   // 검색 의도 (예: ["general"], ["family", "outdoor"])
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlaceInfo {
        private String placeId;
        private String name;
        private String category;
        private Double latitude;
        private Double longitude;
        private String address;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeatherInfo {
        private Double temperature;        // 현재 온도 (°C)
        private Double feelsLike;          // 체감 온도 (°C)
        private String condition;           // 날씨 상태 (예: "맑음", "흐림", "비")
        private String description;         // 상세 설명
        private Double humidity;            // 습도 (%)
        private Double windSpeed;           // 풍속 (m/s)
        private Double precipitation;       // 강수량 (mm)
        private Double visibility;          // 가시거리 (km)
        private String sunrise;            // 일출 시간
        private String sunset;             // 일몰 시간
        private LocalDateTime observedTime; // 관측 시간
    }
}