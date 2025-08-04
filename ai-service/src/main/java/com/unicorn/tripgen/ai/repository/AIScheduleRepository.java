package com.unicorn.tripgen.ai.repository;

import com.unicorn.tripgen.ai.entity.AIModelType;
import com.unicorn.tripgen.ai.entity.AISchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI 일정 Repository
 */
@Repository
public interface AIScheduleRepository extends JpaRepository<AISchedule, Long> {
    
    /**
     * 요청 ID로 일정 조회
     */
    Optional<AISchedule> findByRequestId(String requestId);
    
    /**
     * 여행 ID로 활성 일정 조회
     */
    Optional<AISchedule> findByTripIdAndIsActiveTrue(String tripId);
    
    /**
     * 여행 ID로 모든 일정 조회 (최신순)
     */
    List<AISchedule> findByTripIdOrderByGeneratedAtDesc(String tripId);
    
    /**
     * 활성 상태인 유효한 일정 조회
     */
    @Query("SELECT s FROM AISchedule s WHERE s.tripId = :tripId AND s.isActive = true AND (s.expiresAt IS NULL OR s.expiresAt > :now)")
    Optional<AISchedule> findValidScheduleByTripId(@Param("tripId") String tripId, @Param("now") LocalDateTime now);
    
    /**
     * 만료된 일정 목록 조회
     */
    @Query("SELECT s FROM AISchedule s WHERE s.expiresAt IS NOT NULL AND s.expiresAt < :now AND s.isActive = true")
    List<AISchedule> findExpiredSchedules(@Param("now") LocalDateTime now);
    
    /**
     * AI 모델별 일정 개수 조회
     */
    @Query("SELECT s.aiModelType, COUNT(s) FROM AISchedule s WHERE s.isActive = true GROUP BY s.aiModelType")
    List<Object[]> countByAIModelType();
    
    /**
     * 특정 기간 동안 생성된 일정 조회
     */
    @Query("SELECT s FROM AISchedule s WHERE s.generatedAt BETWEEN :startDate AND :endDate ORDER BY s.generatedAt DESC")
    List<AISchedule> findByGeneratedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate);
    
    /**
     * 오래된 비활성 일정 조회 (정리용)
     */
    @Query("SELECT s FROM AISchedule s WHERE s.isActive = false AND s.updatedAt < :cutoffTime")
    List<AISchedule> findOldInactiveSchedules(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 요청 ID 존재 여부 확인
     */
    boolean existsByRequestId(String requestId);
    
    /**
     * 평균 생성 시간 조회
     */
    @Query("SELECT AVG(s.generationTime) FROM AISchedule s WHERE s.generationTime IS NOT NULL AND s.aiModelType = :modelType")
    Double getAverageGenerationTime(@Param("modelType") AIModelType modelType);
}