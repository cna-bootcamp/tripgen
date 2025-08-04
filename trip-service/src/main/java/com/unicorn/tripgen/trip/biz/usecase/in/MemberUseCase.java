package com.unicorn.tripgen.trip.biz.usecase.in;

import com.unicorn.tripgen.trip.biz.domain.Gender;
import com.unicorn.tripgen.trip.biz.domain.HealthStatus;
import com.unicorn.tripgen.trip.biz.domain.Member;
import com.unicorn.tripgen.trip.biz.domain.Preference;

import java.util.List;

/**
 * 멤버 관련 Use Case 인터페이스
 */
public interface MemberUseCase {
    
    /**
     * 멤버 추가 명령
     */
    record AddMemberCommand(
        String tripId,
        String userId,
        String name,
        int age,
        Gender gender,
        HealthStatus healthStatus,
        List<Preference> preferences
    ) {}
    
    /**
     * 멤버 업데이트 명령
     */
    record UpdateMemberCommand(
        String tripId,
        String memberId,
        String userId,
        String name,
        int age,
        Gender gender,
        HealthStatus healthStatus,
        List<Preference> preferences
    ) {}
    
    /**
     * 멤버 삭제 명령
     */
    record DeleteMemberCommand(
        String tripId,
        String memberId,
        String userId
    ) {}
    
    /**
     * 멤버 일괄 업데이트 명령
     */
    record UpdateMembersBatchCommand(
        String tripId,
        String userId,
        List<MemberInfo> members
    ) {}
    
    /**
     * 멤버 정보
     */
    record MemberInfo(
        String name,
        int age,
        Gender gender,
        HealthStatus healthStatus,
        List<Preference> preferences
    ) {}
    
    /**
     * 여행 멤버 목록 조회
     */
    List<Member> getTripMembers(String tripId, String userId);
    
    /**
     * 멤버 추가
     */
    Member addMember(AddMemberCommand command);
    
    /**
     * 멤버 정보 수정
     */
    Member updateMember(UpdateMemberCommand command);
    
    /**
     * 멤버 삭제
     */
    void deleteMember(DeleteMemberCommand command);
    
    /**
     * 멤버 목록 일괄 업데이트 (기존 멤버 모두 교체)
     */
    List<Member> updateMembersBatch(UpdateMembersBatchCommand command);
}