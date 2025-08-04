package com.unicorn.tripgen.location.dto.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 경로 정보 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteResponse {
    
    private Integer distance; // 거리 (미터)
    
    private Integer duration; // 소요 시간 (초)
    
    private String polyline; // 경로 좌표 (인코딩)
    
    private String durationText; // 소요 시간 텍스트
    
    private String distanceText; // 거리 텍스트
}