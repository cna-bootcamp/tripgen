package com.unicorn.tripgen.trip.infra.repository;

import com.unicorn.tripgen.trip.biz.domain.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Destination JPA Repository 인터페이스
 * Spring Data JPA를 사용한 데이터 접근 계층
 */
@Repository
public interface DestinationJpaRepository extends JpaRepository<Destination, String> {
    
    /**
     * 여행의 모든 목적지 조회 (순서대로)
     */
    List<Destination> findByTripIdOrderByOrderAsc(String tripId);
    
    /**
     * 여행의 목적지 개수 조회
     */
    int countByTripId(String tripId);
    
    /**
     * 여행의 모든 목적지 삭제
     */
    void deleteByTripId(String tripId);
    
    /**
     * 특정 여행의 목적지 존재 여부 확인
     */
    boolean existsByTripIdAndDestinationId(String tripId, String destinationId);
    
    /**
     * 여행의 마지막 순서 조회
     */
    Integer findMaxOrderByTripId(String tripId);
}