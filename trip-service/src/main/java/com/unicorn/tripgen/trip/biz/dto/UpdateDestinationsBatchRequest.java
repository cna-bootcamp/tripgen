package com.unicorn.tripgen.trip.biz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여행지 일괄 업데이트 요청 DTO
 * API 설계서 스키마를 준수하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDestinationsBatchRequest {
    
    @NotNull(message = "여행지 목록은 필수입니다")
    @Size(min = 1, message = "최소 1개 이상의 여행지가 필요합니다")
    @Valid
    private List<CreateDestinationRequest> destinations;
}