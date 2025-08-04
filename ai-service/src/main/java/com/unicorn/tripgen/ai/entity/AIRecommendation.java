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
 * AI 추천 정보 엔티티
 * AI가 생성한 장소별 추천 정보를 저장
 */
@Entity
@Table(name = "ai_recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIRecommendation extends BaseAuditEntity {
    
    @Column(name = "place_id", nullable = false, length = 100)
    private String placeId;
    
    @Column(name = "place_name", nullable = false, length = 200)
    private String placeName;
    
    @Column(name = "place_type", nullable = false, length = 50)
    private String placeType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_model_type", nullable = false)
    private AIModelType aiModelType;
    
    @Column(name = "user_profile_hash", nullable = false, length = 64)
    private String userProfileHash;
    
    @Column(name = "recommendation_data", nullable = false, columnDefinition = "TEXT")
    private String recommendationData;
    
    @Column(name = "generation_time")
    private Integer generationTime;
    
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
    
    @Column(name = "cache_expires_at")
    private LocalDateTime cacheExpiresAt;
    
    @Column(name = "access_count", nullable = false)
    @Builder.Default
    private Integer accessCount = 0;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @PrePersist
    protected void onCreate() {
        super.prePersist();
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
        // 기본적으로 7일 후 캐시 만료
        if (cacheExpiresAt == null) {
            cacheExpiresAt = generatedAt.plusDays(7);
        }
    }
    
    /**
     * 접근 기록 업데이트
     */
    public void recordAccess() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    /**
     * 캐시 만료 여부 확인
     */
    public boolean isCacheExpired() {
        return cacheExpiresAt != null && LocalDateTime.now().isAfter(cacheExpiresAt);
    }
    
    /**
     * 캐시 만료 시간 연장
     */
    public void extendCache(int days) {
        this.cacheExpiresAt = LocalDateTime.now().plusDays(days);
    }
    
    /**
     * 인기 추천인지 확인 (접근 횟수 기준)
     */
    public boolean isPopular() {
        return accessCount >= 10;
    }
}