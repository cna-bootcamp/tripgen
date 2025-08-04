package com.unicorn.tripgen.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 접근 권한 부족시 발생하는 예외
 * HTTP 403 Forbidden 응답으로 처리됨
 */
public class ForbiddenException extends BaseException {
    
    /**
     * ForbiddenException 생성자
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     */
    public ForbiddenException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.FORBIDDEN);
    }
    
    /**
     * ForbiddenException 생성자 (원인 포함)
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public ForbiddenException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, HttpStatus.FORBIDDEN, cause);
    }
}