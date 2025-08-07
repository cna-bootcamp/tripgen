package com.unicorn.tripgen.trip.biz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 일정 내보내기 요청 DTO
 * API 설계서 스키마를 준수하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportScheduleRequest {
    
    @NotBlank(message = "내보내기 형식은 필수입니다")
    @Pattern(regexp = "^(pdf|image)$", message = "내보내기 형식은 pdf 또는 image여야 합니다")
    private String format;
    
    @Builder.Default
    private Boolean includeMap = true;
    
    private List<Integer> days;
}