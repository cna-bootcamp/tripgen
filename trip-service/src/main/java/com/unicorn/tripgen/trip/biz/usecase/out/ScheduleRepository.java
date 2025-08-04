package com.unicorn.tripgen.trip.biz.usecase.out;

import com.unicorn.tripgen.trip.biz.domain.Schedule;

import java.util.List;
import java.util.Optional;

/**
 * 일정 Repository 인터페이스 (Output Port)
 */
public interface ScheduleRepository {
    
    /**
     * 일정 저장
     */
    Schedule save(Schedule schedule);
    
    /**
     * 일정 목록 저장
     */
    List<Schedule> saveAll(List<Schedule> schedules);
    
    /**
     * 일정 ID로 조회
     */
    Optional<Schedule> findById(String scheduleId);
    
    /**
     * 여행의 모든 일정 조회 (일차 순으로)
     */
    List<Schedule> findByTripIdOrderByDay(String tripId);
    
    /**
     * 여행의 특정 일차 일정 조회
     */
    Optional<Schedule> findByTripIdAndDay(String tripId, int day);
    
    /**
     * 여행의 일정 개수 조회
     */
    int countByTripId(String tripId);
    
    /**
     * 일정 삭제
     */
    void delete(Schedule schedule);
    
    /**
     * 여행의 모든 일정 삭제
     */
    void deleteByTripId(String tripId);
    
    /**
     * 여행의 특정 일차 일정 삭제
     */
    void deleteByTripIdAndDay(String tripId, int day);
    
    /**
     * 일정 존재 여부 확인
     */
    boolean existsByTripId(String tripId);
    
    /**
     * 특정 일차 일정 존재 여부 확인
     */
    boolean existsByTripIdAndDay(String tripId, int day);
    
    /**
     * 일정 ID 생성
     */
    String generateScheduleId();
}