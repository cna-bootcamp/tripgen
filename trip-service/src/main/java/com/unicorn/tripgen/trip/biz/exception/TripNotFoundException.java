package com.unicorn.tripgen.trip.biz.exception;

/**
 * 여행을 찾을 수 없을 때 발생하는 예외
 */
public class TripNotFoundException extends TripException {
    
    public TripNotFoundException(String tripId) {
        super("여행을 찾을 수 없습니다. Trip ID: " + tripId);
    }
    
    public TripNotFoundException(String tripId, String userId) {
        super("여행을 찾을 수 없거나 권한이 없습니다. Trip ID: " + tripId + ", User ID: " + userId);
    }
}