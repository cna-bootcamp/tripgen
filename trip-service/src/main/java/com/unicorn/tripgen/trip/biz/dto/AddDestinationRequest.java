package com.unicorn.tripgen.trip.biz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 여행지 추가 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddDestinationRequest {
    @NotBlank(message = "도시명은 필수입니다")
    private String city;
    
    @NotBlank(message = "국가는 필수입니다")
    private String country;
    
    @NotNull(message = "도착일은 필수입니다")
    private LocalDate arrivalDate;
    
    @NotNull(message = "출발일은 필수입니다")
    private LocalDate departureDate;
    
    private String accommodation;
    
    private Double latitude;
    
    private Double longitude;
}