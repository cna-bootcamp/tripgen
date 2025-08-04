package com.unicorn.tripgen.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 경로 정보 조회 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteRequest {
    
    /**
     * 출발지 정보
     */
    @NotNull(message = "출발지 정보는 필수입니다")
    @Valid
    private Location origin;
    
    /**
     * 목적지 정보
     */
    @NotNull(message = "목적지 정보는 필수입니다")
    @Valid
    private Location destination;
    
    /**
     * 경유지 목록 (선택적)
     */
    @Valid
    private List<Location> waypoints;
    
    /**
     * 이동수단
     */
    @NotBlank(message = "이동수단은 필수입니다")
    @Pattern(regexp = "^(public_transport|car|walking|bicycle|motorcycle)$", 
            message = "이동수단은 public_transport, car, walking, bicycle, motorcycle 중 하나여야 합니다")
    private String transportMode;
    
    /**
     * 출발 시간 (선택적, 기본값: 현재 시간)
     */
    private LocalDateTime departureTime;
    
    /**
     * 도착 시간 (선택적, departureTime과 상호 배타적)
     */
    private LocalDateTime arrivalTime;
    
    /**
     * 경로 최적화 기준
     */
    @Pattern(regexp = "^(time|distance|cost|balanced)$", 
            message = "최적화 기준은 time, distance, cost, balanced 중 하나여야 합니다")
    @Builder.Default
    private String optimize = "time";
    
    /**
     * 피해야 할 요소들
     */
    private List<@Pattern(regexp = "^(tolls|highways|ferries|indoor)$", 
                         message = "피할 요소는 tolls, highways, ferries, indoor 중 하나여야 합니다") String> avoid;
    
    /**
     * 대안 경로 포함 여부
     */
    @Builder.Default
    private Boolean includeAlternatives = false;
    
    /**
     * 최대 대안 경로 수
     */
    @Min(value = 1, message = "대안 경로 수는 최소 1개입니다")
    @Max(value = 5, message = "대안 경로 수는 최대 5개입니다")
    @Builder.Default
    private Integer maxAlternatives = 3;
    
    /**
     * 상세 경로 정보 포함 여부
     */
    @Builder.Default
    private Boolean includeSteps = true;
    
    /**
     * 교통 상황 고려 여부
     */
    @Builder.Default
    private Boolean considerTraffic = true;
    
    /**
     * 언어 코드
     */
    @Pattern(regexp = "^(ko|en|ja|zh)$", 
            message = "언어 코드는 ko, en, ja, zh 중 하나여야 합니다")
    @Builder.Default
    private String language = "ko";
    
    /**
     * 단위 시스템 (metric: 미터법, imperial: 야드파운드법)
     */
    @Pattern(regexp = "^(metric|imperial)$", 
            message = "단위 시스템은 metric 또는 imperial이어야 합니다")
    @Builder.Default
    private String units = "metric";
    
    /**
     * 차량 정보 (자동차 이용 시)
     */
    @Valid
    private VehicleInfo vehicle;
    
    /**
     * 접근성 요구사항 (휠체어 접근 가능 등)
     */
    private AccessibilityOptions accessibility;
    
    /**
     * 외부 API 소스 지정
     */
    private String preferredSource;
    
    /**
     * 위치 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Location {
        
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
         * 장소명 (선택적)
         */
        @Size(max = 200, message = "장소명은 200자를 초과할 수 없습니다")
        private String name;
        
        /**
         * 주소 (선택적)
         */
        @Size(max = 500, message = "주소는 500자를 초과할 수 없습니다")
        private String address;
        
        /**
         * 장소 ID (선택적)
         */
        private String placeId;
    }
    
    /**
     * 차량 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleInfo {
        
        /**
         * 차량 종류 (car, truck, motorcycle)
         */
        @Pattern(regexp = "^(car|truck|motorcycle)$", 
                message = "차량 종류는 car, truck, motorcycle 중 하나여야 합니다")
        private String type;
        
        /**
         * 연료 종류 (gasoline, diesel, electric, hybrid)
         */
        @Pattern(regexp = "^(gasoline|diesel|electric|hybrid)$", 
                message = "연료 종류는 gasoline, diesel, electric, hybrid 중 하나여야 합니다")
        private String fuelType;
        
        /**
         * 하이패스 보유 여부
         */
        @Builder.Default
        private Boolean hasHipass = false;
        
        /**
         * 차량 중량 (톤)
         */
        @DecimalMin(value = "0.0", message = "차량 중량은 0 이상이어야 합니다")
        private BigDecimal weight;
        
        /**
         * 차량 높이 (미터)
         */
        @DecimalMin(value = "0.0", message = "차량 높이는 0 이상이어야 합니다")
        private BigDecimal height;
        
        /**
         * 차량 너비 (미터)
         */
        @DecimalMin(value = "0.0", message = "차량 너비는 0 이상이어야 합니다")
        private BigDecimal width;
        
        /**
         * 차량 길이 (미터)
         */
        @DecimalMin(value = "0.0", message = "차량 길이는 0 이상이어야 합니다")
        private BigDecimal length;
    }
    
    /**
     * 접근성 요구사항
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AccessibilityOptions {
        
        /**
         * 휠체어 접근 가능한 경로만 검색
         */
        @Builder.Default
        private Boolean wheelchairAccessible = false;
        
        /**
         * 계단 없는 경로만 검색
         */
        @Builder.Default
        private Boolean noStairs = false;
        
        /**
         * 엘리베이터 사용 가능한 경로만 검색
         */
        @Builder.Default
        private Boolean elevatorAccess = false;
        
        /**
         * 시각 장애인용 경로 안내
         */
        @Builder.Default
        private Boolean visuallyImpairedSupport = false;
        
        /**
         * 청각 장애인용 경로 안내
         */
        @Builder.Default
        private Boolean hearingImpairedSupport = false;
    }
}