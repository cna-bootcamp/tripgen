package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.domain.Gender;
import com.unicorn.tripgen.trip.biz.domain.HealthStatus;
import com.unicorn.tripgen.trip.biz.domain.Member;
import com.unicorn.tripgen.trip.biz.domain.Preference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 멤버 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private String memberId;
    private String name;
    private int age;
    private Gender gender;
    private HealthStatus healthStatus;
    private List<Preference> preferences;
    
    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
            .memberId(member.getMemberId())
            .name(member.getName())
            .age(member.getAge())
            .gender(member.getGender())
            .healthStatus(member.getHealthStatus())
            .preferences(member.getPreferences())
            .build();
    }
}