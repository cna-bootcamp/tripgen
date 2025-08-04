package com.unicorn.tripgen.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 위치 검색 응답 DTO
 * 키워드 검색 결과를 담는 응답 클래스
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationSearchResponse {
    
    /**
     * 검색 키워드
     */
    private String keyword;
    
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
    private List<PlaceCard> places;
    
    /**
     * 검색 실행 시간 (밀리초)
     */
    private Long executionTimeMs;
    
    /**
     * 사용된 외부 API 소스
     */
    private String dataSource;
    
    /**
     * 장소 카드 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlaceCard {
        
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
         * 검색 기준점으로부터의 거리 (미터)
         */
        private Integer distance;
        
        /**
         * 이동 시간 (분)
         */
        private Integer travelTime;
        
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
         * 추천 점수 (1-10)
         */
        private Integer recommendationScore;
        
        /**
         * 추천 이유 (간단)
         */
        private String recommendationReason;
    }
}