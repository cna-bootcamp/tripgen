package com.unicorn.tripgen.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 인증 실패시 발생하는 예외
 * HTTP 401 Unauthorized 응답으로 처리됨
 */
public class UnauthorizedException extends BaseException {
    
    /**
     * UnauthorizedException 생성자
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     */
    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * UnauthorizedException 생성자 (원인 포함)
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public UnauthorizedException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, HttpStatus.UNAUTHORIZED, cause);
    }
}