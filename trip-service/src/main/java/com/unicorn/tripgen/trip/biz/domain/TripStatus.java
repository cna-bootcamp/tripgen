package com.unicorn.tripgen.trip.biz.domain;

/**
 * 여행 상태
 */
public enum TripStatus {
    /**
     * 계획 중
     */
    PLANNING("계획 중"),
    
    /**
     * 진행 중
     */
    ONGOING("진행 중"),
    
    /**
     * 완료
     */
    COMPLETED("완료");
    
    private final String description;
    
    TripStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}