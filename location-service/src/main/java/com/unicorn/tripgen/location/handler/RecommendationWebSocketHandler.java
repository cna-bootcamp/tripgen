package com.unicorn.tripgen.location.handler;

import com.unicorn.tripgen.location.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.net.URI;

/**
 * AI 추천 WebSocket 핸들러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationWebSocketHandler implements WebSocketHandler {
    
    private final WebSocketNotificationService webSocketNotificationService;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String requestId = extractRequestIdFromPath(session.getUri());
        
        if (requestId != null) {
            webSocketNotificationService.addSession(requestId, session);
            log.info("WebSocket connection established: requestId={}, sessionId={}", requestId, session.getId());
        } else {
            log.warn("Invalid WebSocket path, no requestId found: {}", session.getUri());
            session.close();
        }
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // 클라이언트로부터 메시지 수신 시 처리 (현재는 사용하지 않음)
        log.debug("WebSocket message received: sessionId={}, message={}", session.getId(), message.getPayload());
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String requestId = extractRequestIdFromPath(session.getUri());
        log.error("WebSocket transport error: requestId={}, sessionId={}", requestId, session.getId(), exception);
        
        if (requestId != null) {
            webSocketNotificationService.removeSession(requestId);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String requestId = extractRequestIdFromPath(session.getUri());
        
        if (requestId != null) {
            webSocketNotificationService.removeSession(requestId);
            log.info("WebSocket connection closed: requestId={}, sessionId={}, status={}", 
                    requestId, session.getId(), closeStatus);
        }
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * WebSocket URI에서 requestId 추출
     */
    private String extractRequestIdFromPath(URI uri) {
        if (uri == null || uri.getPath() == null) {
            return null;
        }
        
        // /ws/recommendations/{requestId} 패턴에서 requestId 추출
        String path = uri.getPath();
        String[] pathSegments = path.split("/");
        
        if (pathSegments.length >= 4 && "ws".equals(pathSegments[1]) && 
            "recommendations".equals(pathSegments[2])) {
            return pathSegments[3];
        }
        
        return null;
    }
}