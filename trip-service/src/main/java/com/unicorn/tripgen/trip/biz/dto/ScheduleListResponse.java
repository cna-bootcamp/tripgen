package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.domain.Schedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 일정 목록 응답 DTO
 * API 설계서 스키마를 준수하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleListResponse {
    
    private String tripId;
    private List<ScheduleResponse> schedules;
    
    public static ScheduleListResponse from(String tripId, List<Schedule> schedules) {
        return ScheduleListResponse.builder()
            .tripId(tripId)
            .schedules(schedules.stream()
                              .map(ScheduleResponse::from)
                              .toList())
            .build();
    }
}