package com.unicorn.tripgen.trip.infra.controller;

import com.unicorn.tripgen.trip.biz.dto.*;
import com.unicorn.tripgen.trip.biz.usecase.in.ScheduleUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalTime;
import java.util.List;

/**
 * 일정 관리 REST API Controller
 * Clean Architecture의 Infrastructure Layer - Web Interface
 */
@Tag(name = "schedules", description = "일정 관리")
@RestController
@RequestMapping("/api/v1/trips/{tripId}/schedules")
public class ScheduleController {
    
    private final ScheduleUseCase scheduleUseCase;
    
    public ScheduleController(ScheduleUseCase scheduleUseCase) {
        this.scheduleUseCase = scheduleUseCase;
    }
    
    @Operation(summary = "AI 일정 생성 요청", description = "AI를 통해 여행 일정을 생성합니다")
    @PostMapping("/generate")
    public ResponseEntity<GenerateScheduleResponse> generateSchedule(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Valid @RequestBody GenerateScheduleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        
        ScheduleUseCase.GenerateScheduleCommand command = new ScheduleUseCase.GenerateScheduleCommand(
            tripId,
            userDetails.getUsername(),
            startTime,
            request.getSpecialRequests()
        );
        
        var result = scheduleUseCase.generateSchedule(command);
        
        GenerateScheduleResponse response = GenerateScheduleResponse.from(result);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @Operation(summary = "AI 일정 생성 상태 확인", description = "AI 일정 생성 진행 상황을 확인합니다")
    @GetMapping("/generate/{requestId}/status")
    public ResponseEntity<GenerationStatusResponse> getGenerationStatus(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Parameter(description = "생성 요청 ID") @PathVariable String requestId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        var result = scheduleUseCase.getGenerationStatus(tripId, requestId, userDetails.getUsername());
        
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        GenerationStatusResponse response = GenerationStatusResponse.from(result.get());
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "생성된 일정 조회", description = "AI가 생성한 일정을 조회합니다")
    @GetMapping
    public ResponseEntity<ScheduleListResponse> getSchedules(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Parameter(description = "특정 날짜의 일정만 조회") @RequestParam(required = false) Integer day,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        var schedules = scheduleUseCase.getSchedules(tripId, userDetails.getUsername(), day);
        
        ScheduleListResponse response = ScheduleListResponse.from(tripId, schedules);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "일자별 일정 수정", description = "특정 날짜의 일정을 수정합니다")
    @PutMapping("/days/{day}")
    public ResponseEntity<ScheduleResponse> updateDaySchedule(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Parameter(description = "일자 (1부터 시작)") @PathVariable int day,
            @Valid @RequestBody UpdateScheduleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // UpdateScheduleRequest의 PlaceOrderInfo를 ScheduleUseCase.PlaceOrder로 변환
        List<ScheduleUseCase.PlaceOrder> places = request.getPlaces() != null ?
            request.getPlaces().stream()
                   .map(p -> new ScheduleUseCase.PlaceOrder(p.getPlaceId(), p.getOrder()))
                   .toList() : List.of();
        
        ScheduleUseCase.UpdateScheduleCommand command = new ScheduleUseCase.UpdateScheduleCommand(
            tripId,
            userDetails.getUsername(),
            day,
            places
        );
        
        var schedule = scheduleUseCase.updateDaySchedule(command);
        
        ScheduleResponse response = ScheduleResponse.from(schedule);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "일자별 일정 재생성", description = "특정 날짜의 일정을 다시 생성합니다")
    @PostMapping("/days/{day}/regenerate")
    public ResponseEntity<GenerateScheduleResponse> regenerateDaySchedule(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Parameter(description = "일자") @PathVariable int day,
            @RequestBody(required = false) RegenerateScheduleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        ScheduleUseCase.RegenerateScheduleCommand command = new ScheduleUseCase.RegenerateScheduleCommand(
            tripId,
            userDetails.getUsername(),
            day,
            request != null ? request.getSpecialRequests() : null
        );
        
        var result = scheduleUseCase.regenerateDaySchedule(command);
        
        GenerateScheduleResponse response = GenerateScheduleResponse.from(result);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @Operation(summary = "일정 내보내기", description = "생성된 일정을 다양한 형식으로 내보냅니다")
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportSchedule(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Valid @RequestBody ExportScheduleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        ScheduleUseCase.ExportScheduleCommand command = new ScheduleUseCase.ExportScheduleCommand(
            tripId,
            userDetails.getUsername(),
            request.getFormat(),
            request.getIncludeMap() != null ? request.getIncludeMap() : true,
            request.getDays()
        );
        
        var result = scheduleUseCase.exportSchedule(command);
        
        // Content-Type 설정
        MediaType mediaType = "pdf".equals(request.getFormat()) ? 
            MediaType.APPLICATION_PDF : MediaType.IMAGE_PNG;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDispositionFormData("attachment", result.filename());
        
        return ResponseEntity.ok()
                           .headers(headers)
                           .body(result.data());
    }
    
    @Operation(summary = "일정 내 장소의 AI 추천정보 조회", description = "여행 일정에 포함된 장소의 AI 추천정보를 조회합니다")
    @GetMapping("../places/{placeId}/recommendations")
    public ResponseEntity<Object> getSchedulePlaceRecommendations(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Parameter(description = "장소 ID") @PathVariable String placeId,
            @Parameter(description = "일차 (추가 컨텍스트용)") @RequestParam(required = false) Integer day,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        ScheduleUseCase.GetPlaceRecommendationsCommand command = new ScheduleUseCase.GetPlaceRecommendationsCommand(
            tripId,
            placeId,
            userDetails.getUsername(),
            day
        );
        
        var result = scheduleUseCase.getSchedulePlaceRecommendations(command);
        
        // API 스펙에 맞는 응답 구조 생성
        var response = java.util.Map.of(
            "placeId", result.placeId(),
            "placeName", result.placeName(),
            "recommendations", java.util.Map.of(
                "reasons", result.recommendations().reasons(),
                "tips", java.util.Map.of(
                    "description", result.recommendations().tips().description(),
                    "events", result.recommendations().tips().events(),
                    "bestVisitTime", result.recommendations().tips().bestVisitTime(),
                    "estimatedDuration", result.recommendations().tips().estimatedDuration(),
                    "photoSpots", result.recommendations().tips().photoSpots(),
                    "practicalTips", result.recommendations().tips().practicalTips()
                )
            ),
            "context", java.util.Map.of(
                "day", result.context().day(),
                "previousPlace", result.context().previousPlace(),
                "nextPlace", result.context().nextPlace()
            ),
            "fromCache", result.fromCache()
        );
        
        return ResponseEntity.ok(response);
    }
}