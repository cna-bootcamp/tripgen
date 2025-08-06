package com.unicorn.tripgen.location.dto.weather;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 날씨 정보 조회 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherRequest {
    
    @NotNull(message = "위도는 필수입니다")
    private Double latitude;
    
    @NotNull(message = "경도는 필수입니다")
    private Double longitude;
    
    @Builder.Default
    private String units = "metric"; // metric, imperial
    
    @Builder.Default
    private String lang = "ko"; // 언어 코드
    
    private String date; // 날짜 (선택사항)
}