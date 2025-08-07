package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.usecase.in.ScheduleUseCase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 일정 생성 상태 조회 응답 DTO
 * API 설계서 스키마를 준수하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationStatusResponse {
    
    private String requestId;
    private String status;
    private Integer progress;
    private String message;
    private Integer estimatedTime;
    private String error;
    
    public static GenerationStatusResponse from(ScheduleUseCase.GenerationStatusResult result) {
        return GenerationStatusResponse.builder()
            .requestId(result.requestId())
            .status(result.status())
            .progress(result.progress())
            .message(result.message())
            .estimatedTime(result.estimatedTime())
            .error(result.error())
            .build();
    }
}