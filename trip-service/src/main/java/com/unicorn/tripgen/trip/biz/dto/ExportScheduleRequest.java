package com.unicorn.tripgen.trip.biz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일정 내보내기 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportScheduleRequest {
    @NotBlank(message = "포맷은 필수입니다")
    private String format; // PDF, EXCEL, ICS
    
    private boolean includeDetails;
    private boolean includeMap;
}