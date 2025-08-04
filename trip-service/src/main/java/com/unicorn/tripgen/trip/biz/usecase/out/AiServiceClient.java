package com.unicorn.tripgen.trip.biz.usecase.out;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * AI Service 클라이언트 인터페이스 (Output Port)
 */
public interface AiServiceClient {
    
    /**
     * 일정 생성 요청
     */
    record ScheduleGenerationRequest(
        String tripId,
        String userId,
        List<String> destinations,
        List<MemberProfile> members,
        String transportMode,
        LocalTime startTime,
        String specialRequests
    ) {}
    
    /**
     * 멤버 프로필
     */
    record MemberProfile(
        String name,
        int age,
        String gender,
        String healthStatus,
        List<String> preferences
    ) {}
    
    /**
     * 일정 생성 응답
     */
    record ScheduleGenerationResponse(
        String requestId,
        String status,
        String message
    ) {}
    
    /**
     * 일정 생성 상태
     */
    record GenerationStatus(
        String requestId,
        String status,
        int progress,
        String message,
        Integer estimatedTime,
        String error
    ) {}
    
    /**
     * 생성된 일정 정보
     */
    record GeneratedSchedule(
        int day,
        String date,
        String city,
        WeatherInfo weather,
        List<SchedulePlace> places
    ) {}
    
    /**
     * 날씨 정보
     */
    record WeatherInfo(
        String condition,
        double minTemperature,
        double maxTemperature,
        String icon
    ) {}
    
    /**
     * 일정 장소
     */
    record SchedulePlace(
        String placeId,
        String placeName,
        String category,
        String startTime,
        int duration,
        TransportationInfo transportation,
        HealthConsiderationInfo healthConsideration,
        int order
    ) {}
    
    /**
     * 교통 정보
     */
    record TransportationInfo(
        String type,
        int duration,
        double distance,
        String route
    ) {}
    
    /**
     * 건강 고려사항
     */
    record HealthConsiderationInfo(
        List<String> restPoints,
        List<String> accessibility,
        double walkingDistance
    ) {}
    
    /**
     * 추천 정보 요청
     */
    record RecommendationRequest(
        String tripId,
        String placeId,
        String placeName,
        List<MemberProfile> members,
        ContextInfo context
    ) {}
    
    /**
     * 컨텍스트 정보
     */
    record ContextInfo(
        int day,
        String previousPlace,
        String nextPlace,
        String currentCity
    ) {}
    
    /**
     * 추천 정보 응답
     */
    record RecommendationResponse(
        String placeId,
        String placeName,
        List<String> reasons,
        TipInfo tips
    ) {}
    
    /**
     * 팁 정보
     */
    record TipInfo(
        String description,
        List<String> events,
        String bestVisitTime,
        String estimatedDuration,
        List<String> photoSpots,
        List<String> practicalTips
    ) {}
    
    /**
     * AI 일정 생성 요청
     */
    ScheduleGenerationResponse requestScheduleGeneration(ScheduleGenerationRequest request);
    
    /**
     * 일정 생성 상태 조회
     */
    Optional<GenerationStatus> getGenerationStatus(String requestId);
    
    /**
     * 생성된 일정 조회
     */
    List<GeneratedSchedule> getGeneratedSchedule(String requestId);
    
    /**
     * 일자별 일정 재생성 요청
     */
    ScheduleGenerationResponse regenerateDaySchedule(String tripId, int day, String specialRequests);
    
    /**
     * 장소 추천 정보 생성 요청
     */
    ScheduleGenerationResponse requestPlaceRecommendation(RecommendationRequest request);
    
    /**
     * 장소 추천 정보 조회
     */
    Optional<RecommendationResponse> getPlaceRecommendation(String requestId);
}