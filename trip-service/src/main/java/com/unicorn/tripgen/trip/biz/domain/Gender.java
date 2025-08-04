package com.unicorn.tripgen.trip.biz.domain;

/**
 * 성별
 */
public enum Gender {
    /**
     * 남성
     */
    MALE("남성"),
    
    /**
     * 여성
     */
    FEMALE("여성");
    
    private final String description;
    
    Gender(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}