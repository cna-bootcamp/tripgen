package com.unicorn.tripgen.trip.infra.client;

import com.unicorn.tripgen.trip.biz.usecase.out.AiServiceClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * AI Service Feign Client 구현
 */
@FeignClient(name = "ai-service", url = "${AI_SERVICE_URL:http://localhost:8083}")
public interface AiServiceFeignClient extends AiServiceClient {
    
    @Override
    @PostMapping("/api/v1/schedules/generate")
    ScheduleGenerationResponse requestScheduleGeneration(@RequestBody ScheduleGenerationRequest request);
    
    @Override
    @GetMapping("/api/v1/schedules/status/{requestId}")
    Optional<GenerationStatus> getGenerationStatus(@PathVariable String requestId);
    
    @Override
    @GetMapping("/api/v1/schedules/{requestId}")
    List<GeneratedSchedule> getGeneratedSchedule(@PathVariable String requestId);
    
    @Override
    @PostMapping("/api/v1/schedules/regenerate/{tripId}/day/{day}")
    ScheduleGenerationResponse regenerateDaySchedule(
            @PathVariable String tripId, 
            @PathVariable int day, 
            @RequestParam String specialRequests);
    
    @Override
    @PostMapping("/api/v1/recommendations/generate")
    ScheduleGenerationResponse requestPlaceRecommendation(@RequestBody RecommendationRequest request);
    
    @Override
    @GetMapping("/api/v1/recommendations/{requestId}")
    Optional<RecommendationResponse> getPlaceRecommendation(@PathVariable String requestId);
}