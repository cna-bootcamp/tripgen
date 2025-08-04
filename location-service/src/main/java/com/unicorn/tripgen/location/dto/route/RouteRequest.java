package com.unicorn.tripgen.location.dto.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 경로 정보 조회 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteRequest {
    
    @NotNull(message = "출발지 위도는 필수입니다")
    private Double fromLatitude;
    
    @NotNull(message = "출발지 경도는 필수입니다")
    private Double fromLongitude;
    
    @NotNull(message = "도착지 위도는 필수입니다")
    private Double toLatitude;
    
    @NotNull(message = "도착지 경도는 필수입니다")
    private Double toLongitude;
    
    @Builder.Default
    private String mode = "driving"; // driving, walking, transit
    
    private String avoid; // tolls, highways
}