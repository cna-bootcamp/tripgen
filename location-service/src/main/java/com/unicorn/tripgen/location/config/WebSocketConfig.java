package com.unicorn.tripgen.location.config;

import com.unicorn.tripgen.location.handler.RecommendationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 설정
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final RecommendationWebSocketHandler recommendationWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(recommendationWebSocketHandler, "/ws/recommendations/{requestId}")
                .setAllowedOrigins("*"); // 개발환경에서는 모든 Origin 허용
    }
}