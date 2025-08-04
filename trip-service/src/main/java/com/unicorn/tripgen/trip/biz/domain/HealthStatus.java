package com.unicorn.tripgen.trip.biz.domain;

/**
 * 건강 상태
 */
public enum HealthStatus {
    /**
     * 우수 - 모든 활동 가능
     */
    EXCELLENT("우수"),
    
    /**
     * 양호 - 일반적인 활동 가능
     */
    GOOD("양호"),
    
    /**
     * 주의 - 격렬한 활동 제한
     */
    CAUTION("주의"),
    
    /**
     * 제한 - 휴식 위주 활동
     */
    LIMITED("제한");
    
    private final String description;
    
    HealthStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}