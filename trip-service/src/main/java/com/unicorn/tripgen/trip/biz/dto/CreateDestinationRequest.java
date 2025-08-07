package com.unicorn.tripgen.trip.biz.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 여행지 생성 요청 DTO
 * API 설계서 스키마를 준수하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDestinationRequest {
    
    @NotBlank(message = "여행지명은 필수입니다")
    @Size(max = 20, message = "여행지명은 20자 이하여야 합니다")
    private String destinationName;
    
    @NotNull(message = "숙박일수는 필수입니다")
    @Min(value = 1, message = "숙박일수는 1일 이상이어야 합니다")
    private Integer nights;
    
    @Size(max = 20, message = "숙소명은 20자 이하여야 합니다")
    private String accommodation;
    
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "체크인 시간은 HH:MM 형식이어야 합니다")
    private String checkInTime;
    
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "체크아웃 시간은 HH:MM 형식이어야 합니다")
    private String checkOutTime;
}