package com.unicorn.tripgen.location.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LocationType {
    ATTRACTION("관광명소"),
    RESTAURANT("음식점"),
    CAFE("카페"),
    HOTEL("숙박"),
    SHOPPING("쇼핑"),
    CULTURE("문화시설"),
    NATURE("자연경관"),
    ACTIVITY("액티비티"),
    TRANSPORT("교통"),
    OTHER("기타");
    
    private final String description;
}