package com.unicorn.tripgen.trip.biz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 멤버 추가 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberRequest {
    @NotBlank(message = "이름은 필수입니다")
    private String name;
    
    @NotNull(message = "나이는 필수입니다")
    @Min(value = 1, message = "나이는 1세 이상이어야 합니다")
    @Max(value = 120, message = "나이는 120세 이하여야 합니다")
    private Integer age;
    
    @NotBlank(message = "성별은 필수입니다")
    private String gender;
    
    @NotBlank(message = "관계는 필수입니다")
    private String relationship;
    
    private List<String> preferences;
    
    private String healthStatus;
}