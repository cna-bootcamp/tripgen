package com.unicorn.tripgen.trip.biz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 여행지 정보 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDestinationRequest {
    private String city;
    private String country;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private String accommodation;
    private Double latitude;
    private Double longitude;
}