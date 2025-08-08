package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.domain.TransportMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 여행 정보 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTripRequest {
    @Size(max = 100, message = "여행명은 100자를 초과할 수 없습니다")
    private String tripName;
    
    private String description;
    
    private LocalDate startDate;
    
    @NotNull(message = "교통수단은 필수입니다")
    private TransportMode transportMode;
}