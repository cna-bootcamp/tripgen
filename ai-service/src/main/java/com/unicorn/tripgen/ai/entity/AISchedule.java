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
 * AI 생성 일정 정보 엔티티
 * AI가 생성한 여행 일정 데이터를 저장
 */
@Entity
@Table(name = "ai_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AISchedule extends BaseAuditEntity {
    
    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;
    
    @Column(name = "trip_id", nullable = false, length = 100)
    private String tripId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_model_type", nullable = false)
    private AIModelType aiModelType;
    
    @Column(name = "schedule_data", nullable = false, columnDefinition = "TEXT")
    private String scheduleData;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @Column(name = "generation_time")
    private Integer generationTime;
    
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @PrePersist
    protected void onCreate() {
        super.prePersist();
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
        // 기본적으로 30일 후 만료
        if (expiresAt == null) {
            expiresAt = generatedAt.plusDays(30);
        }
    }
    
    /**
     * 일정 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
    
    /**
     * 일정 활성화
     */
    public void activate() {
        this.isActive = true;
    }
    
    /**
     * 만료 여부 확인
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    /**
     * 유효한 일정인지 확인
     */
    public boolean isValid() {
        return isActive && !isExpired();
    }
}