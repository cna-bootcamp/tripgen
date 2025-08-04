package com.unicorn.tripgen.ai.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 기본 요청 DTO
 * 모든 AI 서비스 요청의 공통 필드를 포함
 */
@Data
public abstract class BaseRequest {
    
    /**
     * 요청을 보낸 사용자 ID
     */
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    /**
     * 요청 추적을 위한 트레이스 ID
     */
    private String traceId;
}