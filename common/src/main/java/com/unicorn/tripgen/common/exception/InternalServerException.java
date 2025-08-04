package com.unicorn.tripgen.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 서버 내부 오류시 발생하는 예외
 * HTTP 500 Internal Server Error 응답으로 처리됨
 */
public class InternalServerException extends BaseException {
    
    /**
     * InternalServerException 생성자
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     */
    public InternalServerException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * InternalServerException 생성자 (원인 포함)
     * 
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public InternalServerException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}