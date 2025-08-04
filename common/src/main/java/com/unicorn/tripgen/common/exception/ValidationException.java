package com.unicorn.tripgen.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 입력값 검증 실패시 발생하는 예외
 * HTTP 400 Bad Request 응답으로 처리됨
 */
public class ValidationException extends BaseException {
    
    /**
     * ValidationException 생성자
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     */
    public ValidationException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * ValidationException 생성자 (원인 포함)
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public ValidationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, HttpStatus.BAD_REQUEST, cause);
    }
}