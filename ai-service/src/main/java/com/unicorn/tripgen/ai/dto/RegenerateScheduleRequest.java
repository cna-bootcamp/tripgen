package com.unicorn.tripgen.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * 일자별 일정 재생성 요청 DTO
 */
@Data
public class RegenerateScheduleRequest {
    
    @NotBlank(message = "여행 ID는 필수입니다")
    private String tripId;
    
    @NotNull(message = "재생성할 날짜는 필수입니다")
    @Min(value = 1, message = "날짜는 1일 이상이어야 합니다")
    private Integer day;
    
    @Valid
    private List<ExistingSchedule> existingSchedules;
    
    @Size(max = 500, message = "특별 요청사항은 500자를 초과할 수 없습니다")
    private String specialRequests;
    
    /**
     * 기존 일정 정보 내부 클래스
     */
    @Data
    public static class ExistingSchedule {
        
        @NotNull(message = "날짜는 필수입니다")
        @Min(value = 1, message = "날짜는 1일 이상이어야 합니다")
        private Integer day;
        
        private List<String> places;
    }
}