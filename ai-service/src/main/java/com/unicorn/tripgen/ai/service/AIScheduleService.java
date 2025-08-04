package com.unicorn.tripgen.ai.service;

import com.unicorn.tripgen.ai.dto.*;
import reactor.core.publisher.Mono;

/**
 * AI 일정 생성 서비스 인터페이스
 */
public interface AIScheduleService {
    
    /**
     * AI 일정 생성 요청
     */
    Mono<GenerateScheduleResponse> generateSchedule(GenerateScheduleRequest request);
    
    /**
     * 일정 생성 상태 조회
     */
    Mono<GenerationStatusResponse> getGenerationStatus(String requestId);
    
    /**
     * 생성된 일정 조회
     */
    Mono<GeneratedScheduleResponse> getGeneratedSchedule(String requestId);
    
    /**
     * 일정 생성 취소
     */
    Mono<Void> cancelGeneration(String requestId);
    
    /**
     * 일자별 일정 재생성
     */
    Mono<GenerateScheduleResponse> regenerateDaySchedule(RegenerateScheduleRequest request);
    
    /**
     * 날씨 영향 분석
     */
    Mono<WeatherImpactResponse> analyzeWeatherImpact(String tripId, WeatherImpactRequest request);
}