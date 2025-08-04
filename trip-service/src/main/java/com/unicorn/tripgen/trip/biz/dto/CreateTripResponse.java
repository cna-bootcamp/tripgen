package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.domain.Trip;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 여행 생성 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripResponse {
    private String tripId;
    private String title;
    private String status;
    private LocalDateTime createdAt;
    
    public static CreateTripResponse from(Trip trip) {
        return CreateTripResponse.builder()
            .tripId(trip.getTripId())
            .title(trip.getTripName())
            .status(trip.getStatus().name())
            .createdAt(trip.getCreatedAt())
            .build();
    }
}