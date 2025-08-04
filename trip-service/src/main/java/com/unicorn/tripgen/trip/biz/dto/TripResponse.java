package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.domain.Trip;

import java.time.LocalDateTime;

/**
 * 여행 정보 응답 DTO
 */
public record TripResponse(
    String tripId,
    String tripName,
    String transportMode,
    String status,
    String currentStep,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TripResponse from(Trip trip) {
        return new TripResponse(
            trip.getTripId(),
            trip.getTripName(),
            trip.getTransportMode().name().toLowerCase(),
            trip.getStatus().name().toLowerCase(),
            trip.getCurrentStep(),
            trip.getCreatedAt(),
            trip.getUpdatedAt()
        );
    }
}