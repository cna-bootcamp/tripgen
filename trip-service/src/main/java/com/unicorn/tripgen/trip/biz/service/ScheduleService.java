package com.unicorn.tripgen.trip.biz.service;

import com.unicorn.tripgen.trip.biz.domain.Schedule;
import com.unicorn.tripgen.trip.biz.usecase.in.ScheduleUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService implements ScheduleUseCase {

    @Override
    @Transactional
    public GenerateScheduleResult generateSchedule(GenerateScheduleCommand command) {
        log.info("Generating schedule for trip: {}", command.tripId());
        
        String requestId = UUID.randomUUID().toString();
        
        // TODO: AI 서비스로 일정 생성 요청 전송 (비동기)
        
        return new GenerateScheduleResult(
            requestId,
            "processing",
            "AI 일정 생성을 시작했습니다."
        );
    }

    @Override
    public Optional<GenerationStatusResult> getGenerationStatus(String tripId, String requestId, String userId) {
        log.info("Getting generation status - tripId: {}, requestId: {}", tripId, requestId);
        
        // TODO: 실제 구현 - AI 서비스에서 상태 조회
        
        return Optional.of(new GenerationStatusResult(
            requestId,
            "processing",
            50,
            "일정 생성 중입니다...",
            60,
            null
        ));
    }

    @Override
    public List<Schedule> getSchedules(String tripId, String userId, Integer day) {
        log.info("Getting schedules for trip: {}, day: {}", tripId, day);
        
        // TODO: 실제 구현 - Repository에서 조회
        
        return new ArrayList<>();
    }

    @Override
    @Transactional
    public Schedule updateDaySchedule(UpdateScheduleCommand command) {
        log.info("Updating schedule for trip: {}, day: {}", command.tripId(), command.day());
        
        // TODO: 실제 구현 - 일자별 일정 수정
        
        String scheduleId = UUID.randomUUID().toString();
        Schedule schedule = Schedule.create(
            scheduleId,
            command.tripId(),
            command.day(),
            LocalDate.now(), // TODO: 실제 날짜로 계산 필요
            "Seoul", // TODO: 실제 도시 정보 필요
            null // TODO: 날씨 정보 필요
        );
        
        return schedule;
    }

    @Override
    @Transactional
    public GenerateScheduleResult regenerateDaySchedule(RegenerateScheduleCommand command) {
        log.info("Regenerating schedule for trip: {}, day: {}", command.tripId(), command.day());
        
        String requestId = UUID.randomUUID().toString();
        
        // TODO: AI 서비스로 일자별 재생성 요청 전송
        
        return new GenerateScheduleResult(
            requestId,
            "processing",
            String.format("%d일차 일정을 재생성 중입니다.", command.day())
        );
    }

    @Override
    public ExportScheduleResult exportSchedule(ExportScheduleCommand command) {
        log.info("Exporting schedule for trip: {} as {}", command.tripId(), command.format());
        
        // TODO: 실제 구현 - 일정 내보내기
        
        String filename = String.format("trip_%s.%s", command.tripId(), command.format());
        byte[] data = new byte[0]; // TODO: 실제 데이터 생성
        
        return new ExportScheduleResult(
            command.format(),
            data,
            filename
        );
    }

    @Override
    public PlaceRecommendationsResult getSchedulePlaceRecommendations(GetPlaceRecommendationsCommand command) {
        log.info("Getting recommendations for place: {} in trip: {}", command.placeId(), command.tripId());
        
        // TODO: 실제 구현 - AI 서비스에서 추천 정보 조회 또는 캐시에서 조회
        
        TipInfo tips = new TipInfo(
            "Sample description",
            List.of("Event 1", "Event 2"),
            "10:00 AM - 12:00 PM",
            "2 hours",
            List.of("Photo spot 1"),
            List.of("Tip 1", "Tip 2")
        );
        
        RecommendationInfo recommendations = new RecommendationInfo(
            List.of("Reason 1", "Reason 2"),
            tips
        );
        
        ContextInfo context = new ContextInfo(
            command.day(),
            "Previous Place",
            "Next Place"
        );
        
        return new PlaceRecommendationsResult(
            command.placeId(),
            "Sample Place",
            recommendations,
            context,
            false
        );
    }
}