package com.unicorn.tripgen.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 주변 장소 검색 요청 DTO
 * 출발지 기준으로 이동수단과 시간 범위에 따른 주변 장소 검색
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyPlacesRequest {
    
    /**
     * 출발지 정보
     */
    @NotNull(message = "출발지 정보는 필수입니다")
    @Valid
    private Origin origin;
    
    /**
     * 이동수단
     */
    @NotBlank(message = "이동수단은 필수입니다")
    @Pattern(regexp = "^(public_transport|car|walking)$", 
            message = "이동수단은 public_transport, car, walking 중 하나여야 합니다")
    private String transportMode;
    
    /**
     * 시간 범위 (분)
     */
    @NotNull(message = "시간 범위는 필수입니다")
    @Min(value = 10, message = "시간 범위는 최소 10분입니다")
    @Max(value = 180, message = "시간 범위는 최대 180분입니다")
    private Integer timeRange;
    
    /**
     * 카테고리 필터
     */
    @Pattern(regexp = "^(all|tourist|restaurant|laundry|accommodation|shopping|entertainment)$", 
            message = "카테고리는 all, tourist, restaurant, laundry, accommodation, shopping, entertainment 중 하나여야 합니다")
    @Builder.Default
    private String category = "all";
    
    /**
     * 정렬 기준
     */
    @Pattern(regexp = "^(distance|rating|name|reviews|travel_time)$", 
            message = "정렬 기준은 distance, rating, name, reviews, travel_time 중 하나여야 합니다")
    @Builder.Default
    private String sort = "distance";
    
    /**
     * 페이지 번호
     */
    @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다")
    @Builder.Default
    private Integer page = 1;
    
    /**
     * 페이지 크기
     */
    @Min(value = 10, message = "페이지 크기는 최소 10개입니다")
    @Max(value = 50, message = "페이지 크기는 최대 50개입니다")
    @Builder.Default
    private Integer size = 20;
    
    /**
     * 최소 평점 필터
     */
    @DecimalMin(value = "0.0", message = "최소 평점은 0.0 이상이어야 합니다")
    @DecimalMax(value = "5.0", message = "최소 평점은 5.0 이하여야 합니다")
    private BigDecimal minRating;
    
    /**
     * 가격 수준 필터 (1: 저렴, 2: 보통, 3: 비쌈, 4: 매우 비쌈)
     */
    @Min(value = 1, message = "가격 수준은 1 이상이어야 합니다")
    @Max(value = 4, message = "가격 수준은 4 이하여야 합니다")
    private Integer maxPriceLevel;
    
    /**
     * 영업 중인 장소만 검색 여부
     */
    @Builder.Default
    private Boolean openNow = false;
    
    /**
     * 언어 코드
     */
    @Pattern(regexp = "^(ko|en|ja|zh)$", 
            message = "언어 코드는 ko, en, ja, zh 중 하나여야 합니다")
    @Builder.Default
    private String language = "ko";
    
    /**
     * AI 추천 정보 포함 여부
     */
    @Builder.Default
    private Boolean includeAI = true;
    
    /**
     * 날씨 정보 포함 여부
     */
    @Builder.Default
    private Boolean includeWeather = false;
    
    /**
     * 교통비 포함 여부
     */
    @Builder.Default
    private Boolean includeCost = false;
    
    /**
     * 선호하는 외부 API 소스
     */
    private String preferredSource;
    
    /**
     * 출발지 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Origin {
        
        /**
         * 위도
         */
        @NotNull(message = "위도는 필수입니다")
        @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다")
        @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다")
        private BigDecimal latitude;
        
        /**
         * 경도
         */
        @NotNull(message = "경도는 필수입니다")
        @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다")
        @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다")
        private BigDecimal longitude;
        
        /**
         * 출발지 주소 (선택적)
         */
        @Size(max = 500, message = "주소는 500자를 초과할 수 없습니다")
        private String address;
        
        /**
         * 출발지 이름 (선택적)
         */
        @Size(max = 200, message = "출발지 이름은 200자를 초과할 수 없습니다")
        private String name;
    }
}