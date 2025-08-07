package com.unicorn.tripgen.location.service;

import com.unicorn.tripgen.location.dto.RecommendationRequest;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 추천 요청 메시지 프로듀서 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationProducerService {
    
    private final StreamBridge streamBridge;
    
    /**
     * AI 추천 생성 요청 메시지 발행
     */
    public String sendRecommendationRequest(String placeId, String tripId, 
            RecommendationRequest.SearchContext searchContext) {
        
        String requestId = UUID.randomUUID().toString();
        
        RecommendationRequest request = RecommendationRequest.builder()
                .requestId(requestId)
                .placeId(placeId)
                .tripId(tripId)
                .context(tripId != null ? "trip" : "search")
                .searchContext(searchContext)
                .requestTime(LocalDateTime.now())
                .build();
        
        try {
            boolean sent = streamBridge.send("aiRecommendationProducer-out-0", request);
            
            if (sent) {
                log.info("AI recommendation request sent: requestId={}, placeId={}", requestId, placeId);
            } else {
                log.error("Failed to send AI recommendation request: requestId={}, placeId={}", requestId, placeId);
                throw new RuntimeException("메시지 전송 실패");
            }
            
            return requestId;
            
        } catch (Exception e) {
            log.error("Error sending AI recommendation request: requestId={}, placeId={}", requestId, placeId, e);
            throw new RuntimeException("AI 추천 요청 전송 중 오류가 발생했습니다", e);
        }
    }
}