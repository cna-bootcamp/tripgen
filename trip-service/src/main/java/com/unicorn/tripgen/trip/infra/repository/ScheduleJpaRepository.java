package com.unicorn.tripgen.trip.infra.repository;

import com.unicorn.tripgen.trip.biz.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Schedule JPA Repository 인터페이스
 * Spring Data JPA를 사용한 데이터 접근 계층
 */
@Repository
public interface ScheduleJpaRepository extends JpaRepository<Schedule, String> {
    
    /**
     * 여행의 모든 일정 조회 (일차별로 정렬)
     */
    List<Schedule> findByTripIdOrderByDayAsc(String tripId);
    
    /**
     * 여행의 특정 일차 일정 조회
     */
    Optional<Schedule> findByTripIdAndDay(String tripId, int day);
    
    /**
     * 여행의 일정 개수 조회
     */
    int countByTripId(String tripId);
    
    /**
     * 여행의 모든 일정 삭제
     */
    void deleteByTripId(String tripId);
    
    /**
     * 특정 여행의 일정 존재 여부 확인
     */
    boolean existsByTripIdAndScheduleId(String tripId, String scheduleId);
    
    /**
     * 여행에 일정이 있는지 확인
     */
    boolean existsByTripId(String tripId);
}