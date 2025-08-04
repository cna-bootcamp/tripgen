package com.unicorn.tripgen.ai.service;

import com.unicorn.tripgen.ai.entity.AIJob;
import com.unicorn.tripgen.ai.entity.AIModelType;
import com.unicorn.tripgen.ai.entity.JobStatus;

import java.util.List;
import java.util.Optional;

/**
 * AI 작업 관리 서비스 인터페이스
 */
public interface AIJobService {
    
    /**
     * 새로운 AI 작업 생성
     */
    AIJob createJob(String tripId, String jobType, AIModelType aiModelType, String requestData);
    
    /**
     * 작업 시작
     */
    AIJob startJob(String requestId);
    
    /**
     * 작업 완료 처리
     */
    AIJob completeJob(String requestId, String resultData);
    
    /**
     * 작업 실패 처리
     */
    AIJob failJob(String requestId, String errorMessage);
    
    /**
     * 작업 취소
     */
    AIJob cancelJob(String requestId);
    
    /**
     * 작업 진행 상황 업데이트
     */
    AIJob updateProgress(String requestId, int progress, String currentStep);
    
    /**
     * 요청 ID로 작업 조회
     */
    Optional<AIJob> getJobByRequestId(String requestId);
    
    /**
     * 여행 ID로 작업 목록 조회
     */
    List<AIJob> getJobsByTripId(String tripId);
    
    /**
     * 진행 중인 작업 목록 조회
     */
    List<AIJob> getInProgressJobs();
    
    /**
     * 재시도 대상 작업 목록 조회
     */
    List<AIJob> getJobsForRetry();
    
    /**
     * 작업 재시도
     */
    AIJob retryJob(String requestId);
    
    /**
     * 오래된 완료 작업 정리
     */
    int cleanupOldJobs(int daysOld);
    
    /**
     * 작업 통계 조회
     */
    List<Object[]> getJobStatistics();
}