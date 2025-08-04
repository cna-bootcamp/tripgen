package com.unicorn.tripgen.ai.repository;

import com.unicorn.tripgen.ai.entity.AIModelType;
import com.unicorn.tripgen.ai.entity.AIRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI 추천 정보 Repository
 */
@Repository
public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {
    
    /**
     * 장소 ID와 사용자 프로필 해시로 유효한 추천 조회
     */
    @Query("SELECT r FROM AIRecommendation r WHERE r.placeId = :placeId AND r.userProfileHash = :userProfileHash " +
           "AND (r.cacheExpiresAt IS NULL OR r.cacheExpiresAt > :now) ORDER BY r.generatedAt DESC LIMIT 1")
    Optional<AIRecommendation> findValidRecommendation(@Param("placeId") String placeId, 
                                                      @Param("userProfileHash") String userProfileHash,
                                                      @Param("now") LocalDateTime now);
    
    /**
     * 장소 ID로 모든 추천 조회 (최신순)
     */
    List<AIRecommendation> findByPlaceIdOrderByGeneratedAtDesc(String placeId);
    
    /**
     * 장소 타입별 추천 조회
     */
    List<AIRecommendation> findByPlaceTypeOrderByAccessCountDesc(String placeType);
    
    /**
     * 만료된 추천 목록 조회
     */
    @Query("SELECT r FROM AIRecommendation r WHERE r.cacheExpiresAt IS NOT NULL AND r.cacheExpiresAt < :now")
    List<AIRecommendation> findExpiredRecommendations(@Param("now") LocalDateTime now);
    
    /**
     * 인기 추천 목록 조회 (접근 횟수 기준)
     */
    @Query("SELECT r FROM AIRecommendation r WHERE r.accessCount >= :minAccessCount ORDER BY r.accessCount DESC, r.lastAccessedAt DESC")
    List<AIRecommendation> findPopularRecommendations(@Param("minAccessCount") int minAccessCount);
    
    /**
     * AI 모델별 추천 개수 조회
     */
    @Query("SELECT r.aiModelType, COUNT(r) FROM AIRecommendation r GROUP BY r.aiModelType")
    List<Object[]> countByAIModelType();
    
    /**
     * 특정 기간 동안 생성된 추천 조회
     */
    @Query("SELECT r FROM AIRecommendation r WHERE r.generatedAt BETWEEN :startDate AND :endDate ORDER BY r.generatedAt DESC")
    List<AIRecommendation> findByGeneratedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);
    
    /**
     * 사용되지 않는 오래된 추천 조회 (정리용)
     */
    @Query("SELECT r FROM AIRecommendation r WHERE r.lastAccessedAt IS NULL AND r.generatedAt < :cutoffTime " +
           "OR r.lastAccessedAt < :cutoffTime AND r.accessCount = 0")
    List<AIRecommendation> findUnusedOldRecommendations(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 장소별 추천 통계 조회
     */
    @Query("SELECT r.placeType, COUNT(r), AVG(r.accessCount), MAX(r.accessCount) FROM AIRecommendation r GROUP BY r.placeType")
    List<Object[]> getRecommendationStatistics();
    
    /**
     * 사용자 프로필 해시별 추천 개수 조회
     */
    @Query("SELECT COUNT(r) FROM AIRecommendation r WHERE r.userProfileHash = :userProfileHash")
    Long countByUserProfileHash(@Param("userProfileHash") String userProfileHash);
    
    /**
     * 평균 생성 시간 조회
     */
    @Query("SELECT AVG(r.generationTime) FROM AIRecommendation r WHERE r.generationTime IS NOT NULL AND r.aiModelType = :modelType")
    Double getAverageGenerationTime(@Param("modelType") AIModelType modelType);
    
    /**
     * 최근 접근된 추천 목록 조회
     */
    @Query("SELECT r FROM AIRecommendation r WHERE r.lastAccessedAt IS NOT NULL ORDER BY r.lastAccessedAt DESC")
    List<AIRecommendation> findRecentlyAccessed();
}