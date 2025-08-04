package com.unicorn.tripgen.ai.entity;

import lombok.Getter;

/**
 * AI 모델 타입 열거형
 */
@Getter
public enum AIModelType {
    
    OPENAI_GPT4("OpenAI GPT-4", "gpt-4-turbo-preview", "OpenAI의 GPT-4 모델"),
    OPENAI_GPT35("OpenAI GPT-3.5", "gpt-3.5-turbo", "OpenAI의 GPT-3.5 모델"),
    CLAUDE_OPUS("Claude Opus", "claude-3-opus-20240229", "Anthropic의 Claude Opus 모델"),
    CLAUDE_SONNET("Claude Sonnet", "claude-3-sonnet-20240229", "Anthropic의 Claude Sonnet 모델"),
    CLAUDE_HAIKU("Claude Haiku", "claude-3-haiku-20240307", "Anthropic의 Claude Haiku 모델");
    
    private final String displayName;
    private final String modelId;
    private final String description;
    
    AIModelType(String displayName, String modelId, String description) {
        this.displayName = displayName;
        this.modelId = modelId;
        this.description = description;
    }
    
    /**
     * OpenAI 모델인지 확인
     */
    public boolean isOpenAI() {
        return this == OPENAI_GPT4 || this == OPENAI_GPT35;
    }
    
    /**
     * Claude 모델인지 확인
     */
    public boolean isClaude() {
        return this == CLAUDE_OPUS || this == CLAUDE_SONNET || this == CLAUDE_HAIKU;
    }
    
    /**
     * 고성능 모델인지 확인
     */
    public boolean isHighPerformance() {
        return this == OPENAI_GPT4 || this == CLAUDE_OPUS;
    }
}