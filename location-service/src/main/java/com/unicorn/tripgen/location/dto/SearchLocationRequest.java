package com.unicorn.tripgen.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 위치 검색 요청 DTO
 * 키워드 기반 위치 검색 시 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchLocationRequest {
    
    /**
     * 검색 키워드
     */
    @NotBlank(message = "검색 키워드는 필수입니다")
    @Size(min = 1, max = 100, message = "검색 키워드는 1-100자 사이여야 합니다")
    private String keyword;
    
    /**
     * 검색 기준 위도
     */
    @NotNull(message = "위도는 필수입니다")
    @DecimalMin(value = "-90.0", message = "위도는 -90도 이상이어야 합니다")
    @DecimalMax(value = "90.0", message = "위도는 90도 이하여야 합니다")
    private BigDecimal latitude;
    
    /**
     * 검색 기준 경도
     */
    @NotNull(message = "경도는 필수입니다")
    @DecimalMin(value = "-180.0", message = "경도는 -180도 이상이어야 합니다")
    @DecimalMax(value = "180.0", message = "경도는 180도 이하여야 합니다")
    private BigDecimal longitude;
    
    /**
     * 검색 반경 (미터)
     */
    @Min(value = 100, message = "검색 반경은 최소 100미터입니다")
    @Max(value = 50000, message = "검색 반경은 최대 50,000미터입니다")
    @Builder.Default
    private Integer radius = 5000;
    
    /**
     * 카테고리 필터
     */
    @Pattern(regexp = "^(all|tourist|restaurant|laundry)$", 
            message = "카테고리는 all, tourist, restaurant, laundry 중 하나여야 합니다")
    @Builder.Default
    private String category = "all";
    
    /**
     * 정렬 기준
     */
    @Pattern(regexp = "^(distance|rating|name|reviews)$", 
            message = "정렬 기준은 distance, rating, name, reviews 중 하나여야 합니다")
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
     * 검색 기준 주소 (선택적)
     */
    private String address;
    
    /**
     * 언어 코드 (검색 결과 언어)
     */
    @Pattern(regexp = "^(ko|en|ja|zh)$", 
            message = "언어 코드는 ko, en, ja, zh 중 하나여야 합니다")
    @Builder.Default
    private String language = "ko";
    
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
    
}