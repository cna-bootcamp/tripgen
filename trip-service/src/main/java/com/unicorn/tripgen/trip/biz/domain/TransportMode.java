package com.unicorn.tripgen.trip.biz.domain;

/**
 * 교통수단 유형
 */
public enum TransportMode {
    /**
     * 대중교통
     */
    PUBLIC("대중교통"),
    
    /**
     * 자동차
     */
    CAR("자동차");
    
    private final String description;
    
    TransportMode(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}