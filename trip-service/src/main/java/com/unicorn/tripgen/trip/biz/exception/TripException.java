package com.unicorn.tripgen.trip.biz.exception;

/**
 * 여행 관련 비즈니스 예외
 */
public class TripException extends RuntimeException {
    
    public TripException(String message) {
        super(message);
    }
    
    public TripException(String message, Throwable cause) {
        super(message, cause);
    }
}