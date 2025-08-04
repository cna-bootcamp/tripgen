package com.unicorn.tripgen.ai.client;

import com.unicorn.tripgen.ai.entity.AIModelType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * AI 모델 통합 클라이언트
 * 다양한 AI 모델에 대한 통일된 인터페이스 제공
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AIModelClient {
    
    private final OpenAIClient openAIClient;
    private final ClaudeClient claudeClient;
    
    /**
     * AI 모델을 통한 일정 생성
     */
    public Mono<String> generateSchedule(AIModelType modelType, String prompt, Map<String, Object> context) {
        log.debug("AI 일정 생성 요청: modelType={}", modelType);
        
        if (modelType.isOpenAI()) {
            return openAIClient.generateSchedule(modelType.getModelId(), prompt, context);
        } else if (modelType.isClaude()) {
            return claudeClient.generateSchedule(modelType.getModelId(), prompt, context);
        } else {
            return Mono.error(new IllegalArgumentException("지원하지 않는 AI 모델 타입: " + modelType));
        }
    }
    
    /**
     * AI 모델을 통한 추천 생성
     */
    public Mono<String> generateRecommendation(AIModelType modelType, String prompt, Map<String, Object> context) {
        log.debug("AI 추천 생성 요청: modelType={}", modelType);
        
        if (modelType.isOpenAI()) {
            return openAIClient.generateRecommendation(modelType.getModelId(), prompt, context);
        } else if (modelType.isClaude()) {
            return claudeClient.generateRecommendation(modelType.getModelId(), prompt, context);
        } else {
            return Mono.error(new IllegalArgumentException("지원하지 않는 AI 모델 타입: " + modelType));
        }
    }
    
    /**
     * AI 모델 사용 가능 여부 확인
     */
    public Mono<Boolean> isModelAvailable(AIModelType modelType) {
        log.debug("AI 모델 사용가능 여부 확인: modelType={}", modelType);
        
        if (modelType.isOpenAI()) {
            return openAIClient.isModelAvailable(modelType.getModelId());
        } else if (modelType.isClaude()) {
            return claudeClient.isModelAvailable(modelType.getModelId());
        } else {
            return Mono.just(false);
        }
    }
    
    /**
     * 최적 모델 선택
     * 사용 가능한 모델 중에서 성능과 비용을 고려하여 선택
     */
    public Mono<AIModelType> selectOptimalModel(boolean requireHighPerformance) {
        if (requireHighPerformance) {
            // 고성능이 필요한 경우 우선순위: GPT-4 > Claude Opus > GPT-3.5 > Claude Sonnet
            return checkAndSelectModel(AIModelType.OPENAI_GPT4)
                    .switchIfEmpty(checkAndSelectModel(AIModelType.CLAUDE_OPUS))
                    .switchIfEmpty(checkAndSelectModel(AIModelType.OPENAI_GPT35))
                    .switchIfEmpty(checkAndSelectModel(AIModelType.CLAUDE_SONNET))
                    .switchIfEmpty(Mono.error(new RuntimeException("사용 가능한 고성능 AI 모델이 없습니다")));
        } else {
            // 일반적인 경우 우선순위: GPT-3.5 > Claude Sonnet > Claude Haiku > GPT-4
            return checkAndSelectModel(AIModelType.OPENAI_GPT35)
                    .switchIfEmpty(checkAndSelectModel(AIModelType.CLAUDE_SONNET))
                    .switchIfEmpty(checkAndSelectModel(AIModelType.CLAUDE_HAIKU))
                    .switchIfEmpty(checkAndSelectModel(AIModelType.OPENAI_GPT4))
                    .switchIfEmpty(Mono.error(new RuntimeException("사용 가능한 AI 모델이 없습니다")));
        }
    }
    
    /**
     * 특정 모델의 사용 가능 여부를 확인하고 선택
     */
    private Mono<AIModelType> checkAndSelectModel(AIModelType modelType) {
        return isModelAvailable(modelType)
                .filter(available -> available)
                .map(available -> modelType)
                .doOnNext(selected -> log.debug("AI 모델 선택됨: {}", selected));
    }
}