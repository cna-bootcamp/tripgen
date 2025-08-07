package com.unicorn.tripgen.trip.biz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 일정 생성 요청 DTO
 * API 설계서 스키마를 준수하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateScheduleRequest {
    
    @NotBlank(message = "여행 시작 시간은 필수입니다")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "시작 시간은 HH:MM 형식이어야 합니다")
    private String startTime;
    
    @Size(max = 500, message = "특별 요청사항은 500자 이하여야 합니다")
    private String specialRequests;
}