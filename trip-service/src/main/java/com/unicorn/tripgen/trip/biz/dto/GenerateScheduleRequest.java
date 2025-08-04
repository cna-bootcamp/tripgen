package com.unicorn.tripgen.trip.biz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 일정 생성 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateScheduleRequest {
    private String aiModel;
    private Preferences preferences;
    private Constraints constraints;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Preferences {
        private List<String> activityTypes;
        private String pace;
        private String budgetLevel;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Constraints {
        private Integer dailyBudget;
        private String startTime;
        private String endTime;
    }
}