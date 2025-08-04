package com.unicorn.tripgen.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 에러 응답 클래스
 * 에러 코드와 메시지를 포함한 표준화된 에러 응답 제공
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse extends BaseResponse {
    
    /**
     * 에러 코드
     */
    private String error;
    
    /**
     * 에러 메시지
     */
    private String message;
    
    /**
     * 추가 에러 상세 정보
     */
    private Object details;
    
    /**
     * 기본 생성자
     */
    public ErrorResponse() {
        super.setSuccess(false);
    }
    
    /**
     * 에러 코드와 메시지 포함 생성자
     * 
     * @param error 에러 코드
     * @param message 에러 메시지
     */
    public ErrorResponse(String error, String message) {
        super.setSuccess(false);
        this.error = error;
        this.message = message;
    }
    
    /**
     * 에러 코드, 메시지, 상세 정보 포함 생성자
     * 
     * @param error 에러 코드
     * @param message 에러 메시지
     * @param details 상세 정보
     */
    public ErrorResponse(String error, String message, Object details) {
        super.setSuccess(false);
        this.error = error;
        this.message = message;
        this.details = details;
    }
    
    /**
     * 에러 응답 생성 팩토리 메소드
     * 
     * @param error 에러 코드
     * @param message 에러 메시지
     * @return 에러 응답 객체
     */
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message);
    }
    
    /**
     * 에러 응답 생성 팩토리 메소드 (상세 정보 포함)
     * 
     * @param error 에러 코드
     * @param message 에러 메시지
     * @param details 상세 정보
     * @return 에러 응답 객체
     */
    public static ErrorResponse of(String error, String message, Object details) {
        return new ErrorResponse(error, message, details);
    }
}