package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.usecase.in.ScheduleUseCase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 일정 생성 응답 DTO
 * API 설계서 스키마를 준수하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateScheduleResponse {
    
    private String requestId;
    private String status;
    private String message;
    
    public static GenerateScheduleResponse from(ScheduleUseCase.GenerateScheduleResult result) {
        return GenerateScheduleResponse.builder()
            .requestId(result.requestId())
            .status(result.status())
            .message(result.message())
            .build();
    }
}