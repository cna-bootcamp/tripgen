package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.domain.Trip;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 여행 상세 응답 DTO
 */
public record TripDetailResponse(
    String tripId,
    String tripName,
    String transportMode,
    String status,
    String currentStep,
    LocalDate startDate,
    LocalDate endDate,
    List<MemberResponse> members,
    List<DestinationResponse> destinations,
    boolean hasSchedule,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TripDetailResponse from(Trip trip) {
        List<MemberResponse> members = trip.getMembers().stream()
                                          .map(MemberResponse::from)
                                          .toList();
        
        List<DestinationResponse> destinations = trip.getDestinations().stream()
                                                    .map(DestinationResponse::from)
                                                    .toList();
        
        return new TripDetailResponse(
            trip.getTripId(),
            trip.getTripName(),
            trip.getTransportMode().name().toLowerCase(),
            trip.getStatus().name().toLowerCase(),
            trip.getCurrentStep(),
            trip.getStartDate(),
            trip.getEndDate(),
            members,
            destinations,
            trip.hasSchedule(),
            trip.getCreatedAt(),
            trip.getUpdatedAt()
        );
    }
}