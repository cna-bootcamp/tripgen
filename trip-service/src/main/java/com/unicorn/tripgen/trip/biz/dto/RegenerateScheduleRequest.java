package com.unicorn.tripgen.trip.biz.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일자별 일정 재생성 요청 DTO
 * API 설계서 스키마를 준수하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegenerateScheduleRequest {
    
    @Size(max = 500, message = "재생성 특별 요청사항은 500자 이하여야 합니다")
    private String specialRequests;
}