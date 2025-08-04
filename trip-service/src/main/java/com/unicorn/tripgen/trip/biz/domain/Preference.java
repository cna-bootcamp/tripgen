package com.unicorn.tripgen.trip.biz.domain;

/**
 * 활동 선호도
 */
public enum Preference {
    /**
     * 관광
     */
    SIGHTSEEING("관광"),
    
    /**
     * 쇼핑
     */
    SHOPPING("쇼핑"),
    
    /**
     * 문화
     */
    CULTURE("문화"),
    
    /**
     * 자연
     */
    NATURE("자연"),
    
    /**
     * 스포츠
     */
    SPORTS("스포츠"),
    
    /**
     * 휴식
     */
    REST("휴식");
    
    private final String description;
    
    Preference(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}