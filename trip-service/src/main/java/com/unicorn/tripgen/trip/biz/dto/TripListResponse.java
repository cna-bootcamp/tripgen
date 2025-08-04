package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.usecase.in.TripUseCase;

import java.util.List;

/**
 * 여행 목록 응답 DTO
 */
public record TripListResponse(
    List<TripSummaryDto> trips,
    int totalCount,
    int currentPage,
    int totalPages
) {
    public static TripListResponse from(TripUseCase.TripListResult result) {
        List<TripSummaryDto> trips = result.trips().stream()
                                           .map(TripSummaryDto::from)
                                           .toList();
        
        return new TripListResponse(
            trips,
            result.totalCount(),
            result.currentPage(),
            result.totalPages()
        );
    }
    
    public record TripSummaryDto(
        String tripId,
        String tripName,
        String status,
        String currentStep,
        String startDate,
        String endDate,
        int memberCount,
        int destinationCount,
        int progress,
        String createdAt,
        String updatedAt
    ) {
        public static TripSummaryDto from(TripUseCase.TripSummary summary) {
            return new TripSummaryDto(
                summary.tripId(),
                summary.tripName(),
                summary.status().name().toLowerCase(),
                summary.currentStep(),
                summary.startDate(),
                summary.endDate(),
                summary.memberCount(),
                summary.destinationCount(),
                summary.progress(),
                summary.createdAt(),
                summary.updatedAt()
            );
        }
    }
}