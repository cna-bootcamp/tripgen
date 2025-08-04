package com.unicorn.tripgen.ai.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI API 클라이언트
 * GPT 모델을 통한 일정 생성 및 추천 요청 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIClient {
    
    private final WebClient webClient;
    
    @Value("${ai.openai.api-key}")
    private String apiKey;
    
    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;
    
    @Value("${ai.openai.timeout:60}")
    private int timeoutSeconds;
    
    /**
     * GPT를 통한 여행 일정 생성
     */
    public Mono<String> generateSchedule(String model, String prompt, Map<String, Object> context) {
        log.debug("OpenAI 일정 생성 요청: model={}, promptLength={}", model, prompt.length());
        
        var requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", buildSystemPrompt()),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", 4000,
            "temperature", 0.7,
            "response_format", Map.of("type", "json_object")
        );
        
        return webClient.post()
                .uri(baseUrl + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(this::extractContent)
                .doOnSuccess(result -> log.debug("OpenAI 일정 생성 완료: length={}", result.length()))
                .doOnError(error -> log.error("OpenAI 일정 생성 실패", error));
    }
    
    /**
     * GPT를 통한 장소 추천 생성
     */
    public Mono<String> generateRecommendation(String model, String prompt, Map<String, Object> context) {
        log.debug("OpenAI 추천 생성 요청: model={}, promptLength={}", model, prompt.length());
        
        var requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", buildRecommendationSystemPrompt()),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", 2000,
            "temperature", 0.6,
            "response_format", Map.of("type", "json_object")
        );
        
        return webClient.post()
                .uri(baseUrl + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(this::extractContent)
                .doOnSuccess(result -> log.debug("OpenAI 추천 생성 완료: length={}", result.length()))
                .doOnError(error -> log.error("OpenAI 추천 생성 실패", error));
    }
    
    /**
     * 모델 사용 가능 여부 확인
     */
    public Mono<Boolean> isModelAvailable(String model) {
        return webClient.get()
                .uri(baseUrl + "/models/" + model)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> response.containsKey("id"))
                .onErrorReturn(false)
                .doOnNext(available -> log.debug("OpenAI 모델 사용가능 여부: {}={}", model, available));
    }
    
    /**
     * 응답에서 콘텐츠 추출
     */
    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        try {
            var choices = (List<Map<String, Object>>) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                var message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    return (String) message.get("content");
                }
            }
            throw new RuntimeException("OpenAI 응답에서 콘텐츠를 찾을 수 없습니다");
        } catch (Exception e) {
            log.error("OpenAI 응답 파싱 오류: {}", response, e);
            throw new RuntimeException("OpenAI 응답 파싱 실패", e);
        }
    }
    
    /**
     * 일정 생성용 시스템 프롬프트
     */
    private String buildSystemPrompt() {
        return """
            당신은 전문적인 여행 일정 플래너입니다. 사용자의 요청에 따라 최적화된 여행 일정을 JSON 형태로 생성해주세요.
            
            다음 사항들을 고려하여 일정을 작성해주세요:
            1. 여행자의 나이, 건강 상태, 선호도
            2. 현지 날씨와 계절 특성
            3. 교통수단과 이동 시간
            4. 각 장소의 운영시간과 혼잡도
            5. 식사 시간과 휴식 시간
            
            응답은 반드시 유효한 JSON 형식이어야 하며, schedules 배열을 포함해야 합니다.
            """;
    }
    
    /**
     * 추천 생성용 시스템 프롬프트
     */
    private String buildRecommendationSystemPrompt() {
        return """
            당신은 현지 여행 전문가입니다. 특정 장소에 대한 맞춤형 추천 정보를 JSON 형태로 제공해주세요.
            
            다음 정보를 포함해주세요:
            1. 해당 장소를 추천하는 이유 (여행자 프로필 기반)
            2. 실용적인 방문 팁
            3. 최적 방문 시간
            4. 포토 스팟
            5. 대체 장소 추천
            
            응답은 반드시 유효한 JSON 형식이어야 하며, recommendations 객체를 포함해야 합니다.
            """;
    }
}