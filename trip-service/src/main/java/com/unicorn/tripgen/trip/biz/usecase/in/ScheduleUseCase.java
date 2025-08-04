package com.unicorn.tripgen.trip.biz.usecase.in;

import com.unicorn.tripgen.trip.biz.domain.Schedule;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 일정 관련 Use Case 인터페이스
 */
public interface ScheduleUseCase {
    
    /**
     * AI 일정 생성 요청 명령
     */
    record GenerateScheduleCommand(
        String tripId,
        String userId,
        LocalTime startTime,
        String specialRequests
    ) {}
    
    /**
     * 일정 생성 응답
     */
    record GenerateScheduleResult(
        String requestId,
        String status,
        String message
    ) {}
    
    /**
     * 일정 생성 상태 조회 결과
     */
    record GenerationStatusResult(
        String requestId,
        String status,
        int progress,
        String message,
        Integer estimatedTime,
        String error
    ) {}
    
    /**
     * 일정 수정 명령
     */
    record UpdateScheduleCommand(
        String tripId,
        String userId,
        int day,
        List<PlaceOrder> places
    ) {}
    
    /**
     * 장소 순서 정보
     */
    record PlaceOrder(
        String placeId,
        int order
    ) {}
    
    /**
     * 일자별 일정 재생성 명령
     */
    record RegenerateScheduleCommand(
        String tripId,
        String userId,
        int day,
        String specialRequests
    ) {}
    
    /**
     * 일정 내보내기 명령
     */
    record ExportScheduleCommand(
        String tripId,
        String userId,
        String format,
        boolean includeMap,
        List<Integer> days
    ) {}
    
    /**
     * 일정 내보내기 결과
     */
    record ExportScheduleResult(
        String format,
        byte[] data,
        String filename
    ) {}
    
    /**
     * 장소 추천 정보 조회 명령
     */
    record GetPlaceRecommendationsCommand(
        String tripId,
        String placeId,
        String userId,
        Integer day
    ) {}
    
    /**
     * 장소 추천 정보 결과
     */
    record PlaceRecommendationsResult(
        String placeId,
        String placeName,
        RecommendationInfo recommendations,
        ContextInfo context,
        boolean fromCache
    ) {}
    
    /**
     * 추천 정보
     */
    record RecommendationInfo(
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
     * 컨텍스트 정보
     */
    record ContextInfo(
        Integer day,
        String previousPlace,
        String nextPlace
    ) {}
    
    /**
     * AI 일정 생성 요청
     */
    GenerateScheduleResult generateSchedule(GenerateScheduleCommand command);
    
    /**
     * 일정 생성 상태 확인
     */
    Optional<GenerationStatusResult> getGenerationStatus(String tripId, String requestId, String userId);
    
    /**
     * 생성된 일정 목록 조회
     */
    List<Schedule> getSchedules(String tripId, String userId, Integer day);
    
    /**
     * 일자별 일정 수정
     */
    Schedule updateDaySchedule(UpdateScheduleCommand command);
    
    /**
     * 일자별 일정 재생성
     */
    GenerateScheduleResult regenerateDaySchedule(RegenerateScheduleCommand command);
    
    /**
     * 일정 내보내기
     */
    ExportScheduleResult exportSchedule(ExportScheduleCommand command);
    
    /**
     * 일정 내 장소의 AI 추천정보 조회
     */
    PlaceRecommendationsResult getSchedulePlaceRecommendations(GetPlaceRecommendationsCommand command);
}