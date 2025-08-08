package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.domain.TransportMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 여행 기본정보 일괄 저장 요청 DTO
 * API 설계서의 UpdateTripBasicInfoRequest에 해당
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTripBasicInfoRequest {
    
    /**
     * 여행명 (최대 16자)
     */
    @NotBlank(message = "여행명은 필수입니다")
    @Size(max = 16, message = "여행명은 최대 16자까지 입력 가능합니다")
    private String tripName;
    
    /**
     * 여행 설명
     */
    private String description;
    
    /**
     * 여행 시작일
     */
    @NotNull(message = "시작일은 필수입니다")
    private LocalDate startDate;
    
    
    /**
     * 이동수단 (public: 대중교통, car: 자동차)
     */
    @NotNull(message = "이동수단은 필수입니다")
    private TransportMode transportMode;
    
    /**
     * 멤버 목록 (기존 멤버는 모두 교체됩니다)
     */
    @NotEmpty(message = "최소 1명의 멤버가 필요합니다")
    @Valid
    private List<CreateMemberRequest> members;
}