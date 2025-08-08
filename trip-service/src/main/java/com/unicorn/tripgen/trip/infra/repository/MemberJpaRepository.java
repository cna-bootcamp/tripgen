package com.unicorn.tripgen.trip.infra.repository;

import com.unicorn.tripgen.trip.biz.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Member JPA Repository 인터페이스
 * Spring Data JPA를 사용한 데이터 접근 계층
 */
@Repository
public interface MemberJpaRepository extends JpaRepository<Member, String> {
    
    /**
     * 여행의 모든 멤버 조회
     */
    List<Member> findByTripId(String tripId);
    
    /**
     * 여행의 멤버 개수 조회
     */
    int countByTripId(String tripId);
    
    /**
     * 여행의 모든 멤버 삭제
     */
    void deleteByTripId(String tripId);
    
    /**
     * 특정 여행의 멤버 존재 여부 확인
     */
    boolean existsByTripIdAndMemberId(String tripId, String memberId);
}