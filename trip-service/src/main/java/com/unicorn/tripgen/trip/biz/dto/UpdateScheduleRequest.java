package com.unicorn.tripgen.trip.biz.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 일자별 일정 수정 요청 DTO
 * API 설계서 스키마를 준수하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateScheduleRequest {
    
    @Valid
    private List<PlaceOrderInfo> places;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceOrderInfo {
        private String placeId;
        private Integer order;
    }
}