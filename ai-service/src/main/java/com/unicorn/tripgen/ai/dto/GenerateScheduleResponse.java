package com.unicorn.tripgen.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 일정 생성 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerateScheduleResponse {
    
    private String requestId;
    private String jobId;
    private String status;
    private String message;
    private String estimatedTime;
    
    /**
     * 성공 응답 생성
     */
    public static GenerateScheduleResponse success(String requestId, String estimatedTime) {
        return GenerateScheduleResponse.builder()
                .requestId(requestId)
                .jobId(requestId)
                .status("PENDING")
                .message("일정 생성이 시작되었습니다")
                .estimatedTime(estimatedTime)
                .build();
    }
    
    /**
     * 대기 상태 응답 생성
     */
    public static GenerateScheduleResponse queued(String requestId, String estimatedTime) {
        return GenerateScheduleResponse.builder()
                .requestId(requestId)
                .jobId(requestId)
                .status("queued")
                .message("AI 일정 생성 요청이 대기열에 추가되었습니다")
                .estimatedTime(estimatedTime)
                .build();
    }
}