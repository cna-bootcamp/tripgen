package com.unicorn.tripgen.ai.service;

import com.unicorn.tripgen.ai.dto.RecommendationRequest;
import com.unicorn.tripgen.ai.dto.RecommendationResponse;
import reactor.core.publisher.Mono;

/**
 * AI 추천 정보 생성 서비스 인터페이스
 */
public interface AIRecommendationService {
    
    /**
     * 장소별 AI 추천 정보 생성
     */
    Mono<RecommendationResponse> generatePlaceRecommendations(String placeId, RecommendationRequest request);
    
    /**
     * 캐시된 추천 정보 조회
     */
    Mono<RecommendationResponse> getCachedRecommendation(String placeId, String userProfileHash);
    
    /**
     * 추천 정보 캐시 무효화
     */
    Mono<Void> invalidateRecommendationCache(String placeId);
    
    /**
     * 인기 추천 정보 조회
     */
    Mono<java.util.List<RecommendationResponse>> getPopularRecommendations(int limit);
    
    /**
     * 오래된 추천 정보 정리
     */
    Mono<Integer> cleanupOldRecommendations(int daysOld);
}