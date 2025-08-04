package com.unicorn.tripgen.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주변 장소 검색 응답 DTO
 * 출발지 기준 주변 장소 검색 결과
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyPlacesResponse {
    
    /**
     * 전체 검색 결과 수
     */
    private Long totalCount;
    
    /**
     * 현재 페이지
     */
    private Integer page;
    
    /**
     * 페이지 크기
     */
    private Integer size;
    
    /**
     * 다음 페이지 존재 여부
     */
    private Boolean hasNext;
    
    /**
     * 검색된 장소 목록
     */
    private List<NearbyPlace> places;
    
    /**
     * 검색 조건
     */
    private SearchCriteria searchCriteria;
    
    /**
     * 검색 실행 시간 (밀리초)
     */
    private Long executionTimeMs;
    
    /**
     * 사용된 외부 API 소스
     */
    private String dataSource;
    
    /**
     * 응답 시간
     */
    private LocalDateTime responseTime;
    
    /**
     * 주변 장소 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NearbyPlace {
        
        /**
         * 장소 ID
         */
        private String placeId;
        
        /**
         * 장소명
         */
        private String name;
        
        /**
         * 카테고리
         */
        private String category;
        
        /**
         * 평점
         */
        private BigDecimal rating;
        
        /**
         * 리뷰 수
         */
        private Integer reviewCount;
        
        /**
         * 주소
         */
        private String address;
        
        /**
         * 출발지로부터의 직선 거리 (미터)
         */
        private Integer distance;
        
        /**
         * 이동 시간 (분)
         */
        private Integer travelTime;
        
        /**
         * 이동 거리 (미터) - 실제 경로 기준
         */
        private Integer travelDistance;
        
        /**
         * 교통비 (원)
         */
        private Integer transportCost;
        
        /**
         * 대표 이미지 URL
         */
        private String imageUrl;
        
        /**
         * 위도
         */
        private BigDecimal latitude;
        
        /**
         * 경도
         */
        private BigDecimal longitude;
        
        /**
         * 가격 수준 (1: 저렴, 2: 보통, 3: 비쌈, 4: 매우 비쌈)
         */
        private Integer priceLevel;
        
        /**
         * 영업 상태
         */
        private String businessStatus;
        
        /**
         * 현재 영업 중 여부
         */
        private Boolean isOpenNow;
        
        /**
         * 오늘 영업시간
         */
        private String todayHours;
        
        /**
         * 전화번호
         */
        private String phone;
        
        /**
         * 웹사이트
         */
        private String website;
        
        /**
         * 장소 타입들
         */
        private List<String> types;
        
        /**
         * 외부 API ID
         */
        private String externalId;
        
        /**
         * AI 추천 정보 (간단)
         */
        private SimpleRecommendation aiRecommendation;
        
        /**
         * 날씨 정보 (요청 시)
         */
        private WeatherInfo weather;
        
        /**
         * 이동 경로 정보
         */
        private TravelRoute route;
        
        /**
         * 추천 점수 (1-10)
         */
        private Integer recommendationScore;
        
        /**
         * 인기도 점수
         */
        private BigDecimal popularityScore;
        
        /**
         * 간단한 AI 추천 정보
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class SimpleRecommendation {
            
            /**
             * 추천 이유 (한 줄)
             */
            private String reason;
            
            /**
             * 추천 방문 시간
             */
            private String bestVisitTime;
            
            /**
             * 예상 소요 시간
             */
            private String estimatedDuration;
            
            /**
             * 특별 팁
             */
            private String tip;
        }
        
        /**
         * 날씨 정보
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class WeatherInfo {
            
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
        
        /**
         * 이동 경로 정보
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class TravelRoute {
            
            /**
             * 이동 수단
             */
            private String mode;
            
            /**
             * 이동 시간 (분)
             */
            private Integer duration;
            
            /**
             * 이동 거리 (미터)
             */
            private Integer distance;
            
            /**
             * 교통비 (원)
             */
            private Integer cost;
            
            /**
             * 경로 요약
             */
            private String summary;
            
            /**
             * 환승 횟수 (대중교통 이용 시)
             */
            private Integer transfers;
            
            /**
             * 주요 경유지
             */
            private List<String> steps;
        }
    }
    
    /**
     * 검색 조건 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchCriteria {
        
        /**
         * 출발지 위도
         */
        private BigDecimal originLatitude;
        
        /**
         * 출발지 경도
         */
        private BigDecimal originLongitude;
        
        /**
         * 출발지 주소
         */
        private String originAddress;
        
        /**
         * 이동수단
         */
        private String transportMode;
        
        /**
         * 시간 범위 (분)
         */
        private Integer timeRange;
        
        /**
         * 카테고리
         */
        private String category;
        
        /**
         * 정렬 기준
         */
        private String sort;
        
        /**
         * 최소 평점
         */
        private BigDecimal minRating;
        
        /**
         * 최대 가격 수준
         */
        private Integer maxPriceLevel;
        
        /**
         * 영업 중만 검색
         */
        private Boolean openNow;
    }
}