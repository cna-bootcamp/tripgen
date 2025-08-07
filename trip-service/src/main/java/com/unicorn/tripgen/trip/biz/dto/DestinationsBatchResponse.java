package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.usecase.in.DestinationUseCase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여행지 일괄 업데이트 응답 DTO
 * API 설계서 스키마를 준수하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestinationsBatchResponse {
    
    private String tripId;
    private List<DestinationResponse> destinations;
    private Integer totalDays;
    private String startDate;
    private String endDate;
    private String updatedAt;
    
    public static DestinationsBatchResponse from(DestinationUseCase.DestinationsBatchResult result) {
        return DestinationsBatchResponse.builder()
            .tripId(result.tripId())
            .destinations(result.destinations().stream()
                               .map(DestinationResponse::from)
                               .toList())
            .totalDays(result.totalDays())
            .startDate(result.startDate())
            .endDate(result.endDate())
            .updatedAt(result.updatedAt())
            .build();
    }
}