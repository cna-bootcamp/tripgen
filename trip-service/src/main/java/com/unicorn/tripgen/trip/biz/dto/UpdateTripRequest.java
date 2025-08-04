package com.unicorn.tripgen.trip.biz.dto;

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
    @Size(max = 100, message = "여행 제목은 100자를 초과할 수 없습니다")
    private String title;
    
    private String description;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
}