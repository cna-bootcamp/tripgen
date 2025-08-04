package com.unicorn.tripgen.location.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 주변 장소 검색 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbySearchRequest {
    
    @NotNull(message = "기준 위치는 필수입니다")
    @Valid
    private Point origin;
    
    @NotBlank(message = "이동 수단은 필수입니다")
    private String transportMode;
    
    @NotNull(message = "이동 시간은 필수입니다")
    @Min(value = 5, message = "최소 이동 시간은 5분입니다")
    @Max(value = 60, message = "최대 이동 시간은 60분입니다")
    private Integer travelTime;
    
    private Integer timeRange;  // 시간 범위 (분)
    
    private List<String> placeTypes;
    
    private String keyword;
    
    @Min(value = 1, message = "최소 반경은 1미터입니다")
    @Max(value = 50000, message = "최대 반경은 50km입니다")
    private Integer radius;
    
    /**
     * 좌표 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point {
        @NotNull(message = "위도는 필수입니다")
        @Min(value = -90, message = "위도는 -90 이상이어야 합니다")
        @Max(value = 90, message = "위도는 90 이하여야 합니다")
        private Double latitude;
        
        @NotNull(message = "경도는 필수입니다")
        @Min(value = -180, message = "경도는 -180 이상이어야 합니다")
        @Max(value = 180, message = "경도는 180 이하여야 합니다")
        private Double longitude;
    }
}