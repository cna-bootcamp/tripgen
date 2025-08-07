package com.unicorn.tripgen.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 날씨 정보 조회 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherRequest {
    
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
     * 날씨 조회 날짜 (선택적, 기본값: 오늘)
     */
    private LocalDate date;
    
    /**
     * 지역명 (선택적)
     */
    @Size(max = 200, message = "지역명은 200자를 초과할 수 없습니다")
    private String locationName;
    
    /**
     * 언어 코드
     */
    @Pattern(regexp = "^(ko|en|ja|zh)$", 
            message = "언어 코드는 ko, en, ja, zh 중 하나여야 합니다")
    @Builder.Default
    private String language = "ko";
    
    /**
     * 온도 단위 (metric: 섭씨, imperial: 화씨)
     */
    @Pattern(regexp = "^(metric|imperial)$", 
            message = "온도 단위는 metric 또는 imperial이어야 합니다")
    @Builder.Default
    private String units = "metric";
    
    /**
     * 상세 정보 포함 여부
     */
    @Builder.Default
    private Boolean includeDetails = true;
    
    /**
     * 예보 정보 포함 여부
     */
    @Builder.Default
    private Boolean includeForecast = false;
    
    /**
     * 예보 일수 (1-7일)
     */
    @Min(value = 1, message = "예보 일수는 최소 1일입니다")
    @Max(value = 7, message = "예보 일수는 최대 7일입니다")
    @Builder.Default
    private Integer forecastDays = 1;
    
    /**
     * 캐시 사용 여부
     */
    @Builder.Default
    private Boolean useCache = true;
    
}