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