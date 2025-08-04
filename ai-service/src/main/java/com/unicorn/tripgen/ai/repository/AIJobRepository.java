package com.unicorn.tripgen.ai.repository;

import com.unicorn.tripgen.ai.entity.AIJob;
import com.unicorn.tripgen.ai.entity.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI 작업 정보 Repository
 */
@Repository
public interface AIJobRepository extends JpaRepository<AIJob, Long> {
    
    /**
     * 요청 ID로 작업 조회
     */
    Optional<AIJob> findByRequestId(String requestId);
    
    /**
     * 여행 ID로 작업 목록 조회
     */
    List<AIJob> findByTripIdOrderByCreatedAtDesc(String tripId);
    
    /**
     * 상태별 작업 목록 조회
     */
    List<AIJob> findByStatusOrderByCreatedAtAsc(JobStatus status);
    
    /**
     * 진행 중인 작업 목록 조회
     */
    @Query("SELECT j FROM AIJob j WHERE j.status IN ('QUEUED', 'PROCESSING') ORDER BY j.createdAt ASC")
    List<AIJob> findInProgressJobs();
    
    /**
     * 특정 시간 이전에 생성된 실패한 작업 목록 조회
     */
    @Query("SELECT j FROM AIJob j WHERE j.status = 'FAILED' AND j.createdAt < :cutoffTime AND j.retryCount < j.maxRetry")
    List<AIJob> findFailedJobsForRetry(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 특정 시간 이전에 생성된 완료된 작업 목록 조회 (정리용)
     */
    @Query("SELECT j FROM AIJob j WHERE j.status IN ('COMPLETED', 'FAILED', 'CANCELLED') AND j.completedAt < :cutoffTime")
    List<AIJob> findCompletedJobsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 작업 타입별 통계 조회
     */
    @Query("SELECT j.jobType, j.status, COUNT(j) FROM AIJob j GROUP BY j.jobType, j.status")
    List<Object[]> getJobStatistics();
    
    /**
     * 요청 ID 존재 여부 확인
     */
    boolean existsByRequestId(String requestId);
    
    /**
     * 여행 ID와 작업 타입으로 최근 작업 조회
     */
    @Query("SELECT j FROM AIJob j WHERE j.tripId = :tripId AND j.jobType = :jobType ORDER BY j.createdAt DESC LIMIT 1")
    Optional<AIJob> findLatestByTripIdAndJobType(@Param("tripId") String tripId, @Param("jobType") String jobType);
}