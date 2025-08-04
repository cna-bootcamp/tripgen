package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.domain.Gender;
import com.unicorn.tripgen.trip.biz.domain.HealthStatus;
import com.unicorn.tripgen.trip.biz.domain.Preference;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 여행 기본정보 일괄 업데이트 요청 DTO
 */
public record UpdateTripBasicInfoRequest(
    @NotBlank(message = "여행명은 필수입니다")
    @Size(max = 16, message = "여행명은 16자를 초과할 수 없습니다")
    String tripName,
    
    @NotBlank(message = "교통수단은 필수입니다")
    String transportMode,
    
    @NotEmpty(message = "최소 1명의 멤버가 필요합니다")
    @Valid
    List<MemberInfo> members
) {
    public record MemberInfo(
        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하여야 합니다")
        String name,
        
        int age,
        Gender gender,
        HealthStatus healthStatus,
        List<Preference> preferences
    ) {}
}