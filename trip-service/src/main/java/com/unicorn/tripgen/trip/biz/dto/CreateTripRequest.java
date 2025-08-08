package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.domain.TransportMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 여행 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTripRequest {
    @NotBlank(message = "여행명은 필수입니다")
    @Size(max = 16, message = "여행명은 16자를 초과할 수 없습니다")
    private String tripName;
    
    private String description;
    
    @NotNull(message = "시작일은 필수입니다")
    private LocalDate startDate;
    
    @NotNull(message = "교통수단은 필수입니다")
    private TransportMode transportMode;
}