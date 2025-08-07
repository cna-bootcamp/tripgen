package com.unicorn.tripgen.location.service;

import com.unicorn.tripgen.location.dto.RecommendationResponse;
import com.unicorn.tripgen.location.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * AI 추천 응답 메시지 컨슈머 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationConsumerService {
    
    private final CacheService cacheService;
    private final WebSocketNotificationService webSocketNotificationService;
    
    /**
     * AI 추천 완료 메시지 처리
     */
    @Bean
    public Consumer<Message<RecommendationResponse>> aiRecommendationConsumer() {
        return message -> {
            try {
                RecommendationResponse response = message.getPayload();
                String requestId = response.getRequestId();
                String placeId = response.getPlaceId();
                
                log.info("AI recommendation response received: requestId={}, placeId={}, status={}", 
                        requestId, placeId, response.getStatus());
                
                // 상태가 완료인 경우에만 처리
                if ("completed".equals(response.getStatus())) {
                    // Redis에 결과 저장
                    String resultCacheKey = "rec_result_" + requestId;
                    cacheService.cacheObject(resultCacheKey, response.getRecommendations(), 1800); // 30분
                    
                    // 상태 업데이트
                    String statusCacheKey = "rec_status_" + requestId;
                    cacheService.cacheObject(statusCacheKey, "completed", 1800); // 30분
                    
                    // AI 추천 정보 캐시 (장기 캐시)
                    String aiCacheKey = "ai_recommendation:" + placeId + 
                            (response.getTripId() != null ? ":" + response.getTripId() : "");
                    cacheService.cacheObject(aiCacheKey, response.getRecommendations(), 7200); // 2시간
                    
                    // WebSocket으로 실시간 알림
                    webSocketNotificationService.notifyRecommendationComplete(requestId, response);
                    
                    log.info("AI recommendation processed successfully: requestId={}, placeId={}", 
                            requestId, placeId);
                    
                } else if ("failed".equals(response.getStatus())) {
                    // 실패 상태 저장
                    String statusCacheKey = "rec_status_" + requestId;
                    cacheService.cacheObject(statusCacheKey, "failed", 1800);
                    
                    // WebSocket으로 실패 알림
                    webSocketNotificationService.notifyRecommendationFailed(requestId);
                    
                    log.warn("AI recommendation failed: requestId={}, placeId={}", requestId, placeId);
                }
                
            } catch (Exception e) {
                log.error("Error processing AI recommendation response", e);
                // 오류 발생 시에도 메시지 ACK되도록 예외를 다시 던지지 않음
            }
        };
    }
}