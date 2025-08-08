package com.unicorn.tripgen.trip.biz.exception;

/**
 * 접근 권한이 없을 때 발생하는 예외
 */
public class UnauthorizedAccessException extends TripException {
    
    public UnauthorizedAccessException() {
        super("접근 권한이 없습니다");
    }
    
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}