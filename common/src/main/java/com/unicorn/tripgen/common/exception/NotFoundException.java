package com.unicorn.tripgen.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 요청한 리소스를 찾을 수 없을 때 발생하는 예외
 * HTTP 404 Not Found 응답으로 처리됨
 */
public class NotFoundException extends BaseException {
    
    /**
     * NotFoundException 생성자
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     */
    public NotFoundException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
    
    /**
     * NotFoundException 생성자 (원인 포함)
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public NotFoundException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, HttpStatus.NOT_FOUND, cause);
    }
}