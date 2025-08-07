package com.unicorn.tripgen.location.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.tripgen.location.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 실시간 알림 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {
    
    private final ObjectMapper objectMapper;
    
    // requestId별 WebSocket 세션 관리
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * WebSocket 세션 등록
     */
    public void addSession(String requestId, WebSocketSession session) {
        activeSessions.put(requestId, session);
        log.info("WebSocket session added: requestId={}, sessionId={}", requestId, session.getId());
    }
    
    /**
     * WebSocket 세션 제거
     */
    public void removeSession(String requestId) {
        WebSocketSession session = activeSessions.remove(requestId);
        if (session != null) {
            log.info("WebSocket session removed: requestId={}, sessionId={}", requestId, session.getId());
        }
    }
    
    /**
     * AI 추천 완료 알림
     */
    public void notifyRecommendationComplete(String requestId, RecommendationResponse response) {
        WebSocketSession session = activeSessions.get(requestId);
        
        if (session != null && session.isOpen()) {
            try {
                String message = objectMapper.writeValueAsString(Map.of(
                        "type", "recommendation_complete",
                        "requestId", requestId,
                        "placeId", response.getPlaceId(),
                        "recommendations", response.getRecommendations()
                ));
                
                session.sendMessage(new TextMessage(message));
                log.info("Recommendation complete notification sent: requestId={}", requestId);
                
            } catch (Exception e) {
                log.error("Error sending WebSocket notification: requestId={}", requestId, e);
                removeSession(requestId);
            }
        } else {
            log.debug("No active WebSocket session for requestId: {}", requestId);
        }
    }
    
    /**
     * AI 추천 실패 알림
     */
    public void notifyRecommendationFailed(String requestId) {
        WebSocketSession session = activeSessions.get(requestId);
        
        if (session != null && session.isOpen()) {
            try {
                String message = objectMapper.writeValueAsString(Map.of(
                        "type", "recommendation_failed",
                        "requestId", requestId,
                        "message", "AI 추천 생성에 실패했습니다"
                ));
                
                session.sendMessage(new TextMessage(message));
                log.info("Recommendation failed notification sent: requestId={}", requestId);
                
            } catch (Exception e) {
                log.error("Error sending WebSocket failure notification: requestId={}", requestId, e);
                removeSession(requestId);
            }
        } else {
            log.debug("No active WebSocket session for failed requestId: {}", requestId);
        }
    }
}