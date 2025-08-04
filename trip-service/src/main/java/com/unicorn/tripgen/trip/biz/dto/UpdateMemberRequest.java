package com.unicorn.tripgen.trip.biz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 멤버 정보 수정 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberRequest {
    private String name;
    private Integer age;
    private String gender;
    private String relationship;
    private List<String> preferences;
    private String healthStatus;
}