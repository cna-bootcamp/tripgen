package com.unicorn.tripgen.location.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransportType {
    WALKING("도보"),
    CAR("자동차"),
    PUBLIC_TRANSPORT("대중교통"),
    BICYCLE("자전거"),
    TAXI("택시");
    
    private final String description;
}