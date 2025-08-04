package com.unicorn.tripgen.ai.service;

import com.unicorn.tripgen.ai.entity.AIJob;
import com.unicorn.tripgen.ai.entity.AIModelType;
import com.unicorn.tripgen.ai.entity.JobStatus;
import com.unicorn.tripgen.ai.repository.AIJobRepository;
import com.unicorn.tripgen.common.exception.ErrorCodes;
import com.unicorn.tripgen.common.exception.BusinessException;
import com.unicorn.tripgen.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AI 작업 관리 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AIJobServiceImpl implements AIJobService {
    
    private final AIJobRepository aiJobRepository;
    
    @Override
    public AIJob createJob(String tripId, String jobType, AIModelType aiModelType, String requestData) {
        log.debug("AI 작업 생성: tripId={}, jobType={}, modelType={}", tripId, jobType, aiModelType);
        
        String requestId = generateRequestId(tripId, jobType);
        
        AIJob job = AIJob.builder()
                .requestId(requestId)
                .jobType(jobType)
                .tripId(tripId)
                .status(JobStatus.QUEUED)
                .aiModelType(aiModelType)
                .progress(0)
                .currentStep("대기 중")
                .requestData(requestData)
                .retryCount(0)
                .maxRetry(3)
                .build();
        
        AIJob savedJob = aiJobRepository.save(job);
        log.info("AI 작업 생성 완료: requestId={}", requestId);
        
        return savedJob;
    }
    
    @Override
    public AIJob startJob(String requestId) {
        log.debug("AI 작업 시작: requestId={}", requestId);
        
        AIJob job = getJobByRequestIdOrThrow(requestId);
        
        if (job.getStatus() != JobStatus.QUEUED) {
            throw new BusinessException(
                ErrorCodes.AI_JOB_ALREADY_COMPLETED,
                "이미 시작되었거나 완료된 작업입니다"
            );
        }
        
        job.start();
        AIJob savedJob = aiJobRepository.save(job);
        
        log.info("AI 작업 시작됨: requestId={}", requestId);
        return savedJob;
    }
    
    @Override
    public AIJob completeJob(String requestId, String resultData) {
        log.debug("AI 작업 완료: requestId={}", requestId);
        
        AIJob job = getJobByRequestIdOrThrow(requestId);
        
        if (job.getStatus().isCompleted()) {
            throw new BusinessException(
                ErrorCodes.AI_JOB_ALREADY_COMPLETED,
                "이미 완료된 작업입니다"
            );
        }
        
        job.complete(resultData);
        AIJob savedJob = aiJobRepository.save(job);
        
        log.info("AI 작업 완료됨: requestId={}", requestId);
        return savedJob;
    }
    
    @Override
    public AIJob failJob(String requestId, String errorMessage) {
        log.debug("AI 작업 실패: requestId={}, error={}", requestId, errorMessage);
        
        AIJob job = getJobByRequestIdOrThrow(requestId);
        
        if (job.getStatus().isCompleted()) {
            throw new BusinessException(
                ErrorCodes.AI_JOB_ALREADY_COMPLETED,
                "이미 완료된 작업입니다"
            );
        }
        
        job.fail(errorMessage);
        AIJob savedJob = aiJobRepository.save(job);
        
        log.warn("AI 작업 실패됨: requestId={}, error={}", requestId, errorMessage);
        return savedJob;
    }
    
    @Override
    public AIJob cancelJob(String requestId) {
        log.debug("AI 작업 취소: requestId={}", requestId);
        
        AIJob job = getJobByRequestIdOrThrow(requestId);
        
        if (job.getStatus().isCompleted()) {
            throw new BusinessException(
                ErrorCodes.AI_JOB_ALREADY_COMPLETED,
                "이미 완료된 작업은 취소할 수 없습니다"
            );
        }
        
        job.cancel();
        AIJob savedJob = aiJobRepository.save(job);
        
        log.info("AI 작업 취소됨: requestId={}", requestId);
        return savedJob;
    }
    
    @Override
    public AIJob updateProgress(String requestId, int progress, String currentStep) {
        log.debug("AI 작업 진행 상황 업데이트: requestId={}, progress={}%, step={}", 
                requestId, progress, currentStep);
        
        AIJob job = getJobByRequestIdOrThrow(requestId);
        
        if (job.getStatus() != JobStatus.PROCESSING) {
            throw new BusinessException(
                ErrorCodes.AI_JOB_FAILED,
                "진행 중인 작업이 아닙니다"
            );
        }
        
        job.updateProgress(progress, currentStep);
        
        // 예상 시간 업데이트 (간단한 로직)
        if (progress > 0) {
            long elapsedTime = java.time.Duration.between(job.getStartedAt(), LocalDateTime.now()).getSeconds();
            int estimatedTime = (int) ((elapsedTime * (100 - progress)) / progress);
            job.setEstimatedTime(estimatedTime);
        }
        
        return aiJobRepository.save(job);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<AIJob> getJobByRequestId(String requestId) {
        return aiJobRepository.findByRequestId(requestId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AIJob> getJobsByTripId(String tripId) {
        return aiJobRepository.findByTripIdOrderByCreatedAtDesc(tripId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AIJob> getInProgressJobs() {
        return aiJobRepository.findInProgressJobs();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AIJob> getJobsForRetry() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30); // 30분 전 실패한 작업들
        return aiJobRepository.findFailedJobsForRetry(cutoffTime);
    }
    
    @Override
    public AIJob retryJob(String requestId) {
        log.debug("AI 작업 재시도: requestId={}", requestId);
        
        AIJob job = getJobByRequestIdOrThrow(requestId);
        
        if (!job.canRetry()) {
            throw new BusinessException(
                ErrorCodes.AI_JOB_FAILED,
                "재시도할 수 없는 작업입니다"
            );
        }
        
        job.incrementRetry();
        job.setStatus(JobStatus.QUEUED);
        job.setProgress(0);
        job.setCurrentStep("재시도 대기 중");
        job.setErrorMessage(null);
        job.setStartedAt(null);
        job.setCompletedAt(null);
        
        AIJob savedJob = aiJobRepository.save(job);
        log.info("AI 작업 재시도 설정 완료: requestId={}, retryCount={}", requestId, job.getRetryCount());
        
        return savedJob;
    }
    
    @Override
    public int cleanupOldJobs(int daysOld) {
        log.debug("오래된 AI 작업 정리: daysOld={}", daysOld);
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysOld);
        List<AIJob> oldJobs = aiJobRepository.findCompletedJobsOlderThan(cutoffTime);
        
        if (!oldJobs.isEmpty()) {
            aiJobRepository.deleteAll(oldJobs);
            log.info("오래된 AI 작업 정리 완료: 삭제된 작업 수={}", oldJobs.size());
        }
        
        return oldJobs.size();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getJobStatistics() {
        return aiJobRepository.getJobStatistics();
    }
    
    /**
     * 요청 ID로 작업을 조회하고, 존재하지 않으면 예외 발생
     */
    private AIJob getJobByRequestIdOrThrow(String requestId) {
        return aiJobRepository.findByRequestId(requestId)
                .orElseThrow(() -> new NotFoundException(
                    ErrorCodes.AI_JOB_NOT_FOUND,
                    "요청 ID에 해당하는 작업을 찾을 수 없습니다: " + requestId
                ));
    }
    
    /**
     * 고유한 요청 ID 생성
     */
    private String generateRequestId(String tripId, String jobType) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s_%s_%s", jobType, tripId, timestamp, uuid);
    }
}