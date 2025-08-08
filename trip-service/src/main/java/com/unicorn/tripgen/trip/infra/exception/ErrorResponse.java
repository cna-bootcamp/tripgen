package com.unicorn.tripgen.trip.infra.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API 오류 응답 DTO
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * 오류 코드
     */
    private String code;
    
    /**
     * 오류 메시지
     */
    private String message;
    
    /**
     * 추가 오류 정보 (예: 필드별 검증 오류)
     */
    private Map<String, Object> details;
    
    /**
     * 오류 발생 시각
     */
    private LocalDateTime timestamp;
}