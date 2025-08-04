package com.unicorn.tripgen.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 모든 API 응답의 기본 클래스
 * 성공/실패 여부와 타임스탬프를 포함
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse {
    
    /**
     * 요청 처리 성공 여부
     */
    private boolean success = true;
    
    /**
     * 응답 생성 시간
     */
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 성공 응답 생성 팩토리 메소드
     * 
     * @return 성공 응답 객체
     */
    public static BaseResponse success() {
        return new BaseResponse(true, LocalDateTime.now());
    }
    
    /**
     * 실패 응답 생성 팩토리 메소드
     * 
     * @return 실패 응답 객체
     */
    public static BaseResponse failure() {
        return new BaseResponse(false, LocalDateTime.now());
    }
}