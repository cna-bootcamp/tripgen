package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.domain.Destination;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 여행지 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationResponse {
    private String destinationId;
    private String destinationName;
    private int nights;
    private LocalDate startDate;
    private LocalDate endDate;
    private String accommodation;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private int order;
    
    public static DestinationResponse from(Destination destination) {
        return DestinationResponse.builder()
            .destinationId(destination.getDestinationId())
            .destinationName(destination.getDestinationName())
            .nights(destination.getNights())
            .startDate(destination.getStartDate())
            .endDate(destination.getEndDate())
            .accommodation(destination.getAccommodation())
            .checkInTime(destination.getCheckInTime())
            .checkOutTime(destination.getCheckOutTime())
            .order(destination.getOrder())
            .build();
    }
}