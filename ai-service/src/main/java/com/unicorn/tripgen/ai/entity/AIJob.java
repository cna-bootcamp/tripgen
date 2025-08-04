package com.unicorn.tripgen.ai.entity;

import com.unicorn.tripgen.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 작업 정보 엔티티
 * 비동기 AI 작업의 상태와 진행 정보를 관리
 */
@Entity
@Table(name = "ai_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIJob extends BaseAuditEntity {
    
    @Column(name = "request_id", nullable = false, unique = true, length = 100)
    private String requestId;
    
    @Column(name = "job_type", nullable = false, length = 50)
    private String jobType;
    
    @Column(name = "trip_id", nullable = false, length = 100)
    private String tripId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_model_type", nullable = false)
    private AIModelType aiModelType;
    
    @Column(name = "progress", nullable = false)
    @Builder.Default
    private Integer progress = 0;
    
    @Column(name = "current_step", length = 200)
    private String currentStep;
    
    @Column(name = "estimated_time")
    private Integer estimatedTime;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;
    
    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData;
    
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "max_retry", nullable = false)
    @Builder.Default
    private Integer maxRetry = 3;
    
    /**
     * 작업 시작
     */
    public void start() {
        this.status = JobStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
        this.progress = 0;
    }
    
    /**
     * 작업 완료
     */
    public void complete(String resultData) {
        this.status = JobStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.progress = 100;
        this.resultData = resultData;
        this.currentStep = "완료";
    }
    
    /**
     * 작업 실패
     */
    public void fail(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.currentStep = "실패";
    }
    
    /**
     * 작업 취소
     */
    public void cancel() {
        this.status = JobStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
        this.currentStep = "취소됨";
    }
    
    /**
     * 진행 상황 업데이트
     */
    public void updateProgress(int progress, String currentStep) {
        this.progress = Math.min(Math.max(progress, 0), 100);
        this.currentStep = currentStep;
    }
    
    /**
     * 재시도 가능 여부 확인
     */
    public boolean canRetry() {
        return retryCount < maxRetry && (status == JobStatus.FAILED);
    }
    
    /**
     * 재시도 횟수 증가
     */
    public void incrementRetry() {
        this.retryCount++;
    }
}