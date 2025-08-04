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
 * Claude API 클라이언트
 * Anthropic Claude 모델을 통한 일정 생성 및 추천 요청 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClaudeClient {
    
    private final WebClient webClient;
    
    @Value("${ai.claude.api-key}")
    private String apiKey;
    
    @Value("${ai.claude.base-url:https://api.anthropic.com/v1}")
    private String baseUrl;
    
    @Value("${ai.claude.timeout:90}")
    private int timeoutSeconds;
    
    @Value("${ai.claude.version:2023-06-01}")
    private String apiVersion;
    
    /**
     * Claude를 통한 여행 일정 생성
     */
    public Mono<String> generateSchedule(String model, String prompt, Map<String, Object> context) {
        log.debug("Claude 일정 생성 요청: model={}, promptLength={}", model, prompt.length());
        
        var requestBody = Map.of(
            "model", model,
            "max_tokens", 4000,
            "temperature", 0.7,
            "messages", List.of(
                Map.of(
                    "role", "user", 
                    "content", buildSchedulePrompt(prompt)
                )
            )
        );
        
        return webClient.post()
                .uri(baseUrl + "/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("anthropic-version", apiVersion)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(this::extractContent)
                .doOnSuccess(result -> log.debug("Claude 일정 생성 완료: length={}", result.length()))
                .doOnError(error -> log.error("Claude 일정 생성 실패", error));
    }
    
    /**
     * Claude를 통한 장소 추천 생성
     */
    public Mono<String> generateRecommendation(String model, String prompt, Map<String, Object> context) {
        log.debug("Claude 추천 생성 요청: model={}, promptLength={}", model, prompt.length());
        
        var requestBody = Map.of(
            "model", model,
            "max_tokens", 2000,
            "temperature", 0.6,
            "messages", List.of(
                Map.of(
                    "role", "user", 
                    "content", buildRecommendationPrompt(prompt)
                )
            )
        );
        
        return webClient.post()
                .uri(baseUrl + "/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("anthropic-version", apiVersion)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .map(this::extractContent)
                .doOnSuccess(result -> log.debug("Claude 추천 생성 완료: length={}", result.length()))
                .doOnError(error -> log.error("Claude 추천 생성 실패", error));
    }
    
    /**
     * 모델 사용 가능 여부 확인 (Claude의 경우 단순히 API 키 유효성 확인)
     */
    public Mono<Boolean> isModelAvailable(String model) {
        var testRequestBody = Map.of(
            "model", model,
            "max_tokens", 1,
            "messages", List.of(
                Map.of("role", "user", "content", "test")
            )
        );
        
        return webClient.post()
                .uri(baseUrl + "/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("anthropic-version", apiVersion)
                .bodyValue(testRequestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> response.containsKey("content") || response.containsKey("id"))
                .onErrorReturn(false)
                .doOnNext(available -> log.debug("Claude 모델 사용가능 여부: {}={}", model, available));
    }
    
    /**
     * 응답에서 콘텐츠 추출
     */
    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        try {
            var content = (List<Map<String, Object>>) response.get("content");
            if (content != null && !content.isEmpty()) {
                var textContent = content.get(0);
                if ("text".equals(textContent.get("type"))) {
                    return (String) textContent.get("text");
                }
            }
            throw new RuntimeException("Claude 응답에서 콘텐츠를 찾을 수 없습니다");
        } catch (Exception e) {
            log.error("Claude 응답 파싱 오류: {}", response, e);
            throw new RuntimeException("Claude 응답 파싱 실패", e);
        }
    }
    
    /**
     * 일정 생성용 프롬프트 구성
     */
    private String buildSchedulePrompt(String userPrompt) {
        return """
            당신은 전문적인 여행 일정 플래너입니다. 다음 요청에 따라 최적화된 여행 일정을 JSON 형태로 생성해주세요.
            
            고려사항:
            1. 여행자의 나이, 건강 상태, 선호도를 반영
            2. 현지 날씨와 계절 특성 고려
            3. 교통수단과 이동 시간 최적화
            4. 각 장소의 운영시간과 혼잡도 고려
            5. 적절한 식사 시간과 휴식 시간 배치
            
            응답은 반드시 유효한 JSON 형식으로 작성하고, schedules 배열을 포함해주세요.
            
            사용자 요청:
            """ + userPrompt;
    }
    
    /**
     * 추천 생성용 프롬프트 구성
     */
    private String buildRecommendationPrompt(String userPrompt) {
        return """
            당신은 현지 여행 전문가입니다. 다음 장소에 대한 맞춤형 추천 정보를 JSON 형태로 제공해주세요.
            
            포함할 정보:
            1. 해당 장소를 추천하는 구체적인 이유 (여행자 프로필 기반)
            2. 실용적인 방문 팁과 주의사항
            3. 최적 방문 시간과 예상 소요시간
            4. 인스타그램 포토 스팟 추천
            5. 날씨나 상황에 따른 대체 장소
            
            응답은 반드시 유효한 JSON 형식으로 작성하고, recommendations 객체를 포함해주세요.
            
            사용자 요청:
            """ + userPrompt;
    }
}