package com.unicorn.tripgen.trip.biz.usecase.out;

import com.unicorn.tripgen.trip.biz.domain.Member;

import java.util.List;
import java.util.Optional;

/**
 * 멤버 Repository 인터페이스 (Output Port)
 */
public interface MemberRepository {
    
    /**
     * 멤버 저장
     */
    Member save(Member member);
    
    /**
     * 멤버 목록 저장
     */
    List<Member> saveAll(List<Member> members);
    
    /**
     * 멤버 ID로 조회
     */
    Optional<Member> findById(String memberId);
    
    /**
     * 여행의 모든 멤버 조회
     */
    List<Member> findByTripId(String tripId);
    
    /**
     * 여행의 멤버 개수 조회
     */
    int countByTripId(String tripId);
    
    /**
     * 멤버 삭제
     */
    void delete(Member member);
    
    /**
     * 여행의 모든 멤버 삭제
     */
    void deleteByTripId(String tripId);
    
    /**
     * 멤버 존재 여부 확인
     */
    boolean existsById(String memberId);
    
    /**
     * 멤버 ID 생성
     */
    String generateMemberId();
}