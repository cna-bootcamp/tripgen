package com.unicorn.tripgen.trip.biz.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 멤버 생성 요청 DTO
 * API 설계서 스키마를 준수하는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemberRequest {
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 1, max = 20, message = "이름은 1-20자 사이여야 합니다")
    private String name;
    
    @NotNull(message = "나이는 필수입니다")
    @Min(value = 1, message = "나이는 1세 이상이어야 합니다")
    @Max(value = 120, message = "나이는 120세 이하여야 합니다")
    private Integer age;
    
    @NotBlank(message = "성별은 필수입니다")
    @Pattern(regexp = "^(male|female)$", message = "성별은 male 또는 female이어야 합니다")
    private String gender;
    
    @NotBlank(message = "건강상태는 필수입니다")
    @Pattern(regexp = "^(excellent|good|caution|limited)$", message = "건강상태는 excellent, good, caution, limited 중 하나여야 합니다")
    private String healthStatus;
    
    private List<@Pattern(regexp = "^(sightseeing|shopping|culture|nature|sports|rest)$", 
                         message = "선호도는 sightseeing, shopping, culture, nature, sports, rest 중에서 선택해야 합니다") 
                 String> preferences;
}