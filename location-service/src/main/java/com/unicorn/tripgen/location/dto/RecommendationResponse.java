package com.unicorn.tripgen.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 추천 응답 메시지 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {
    
    private String requestId;
    private String placeId;
    private String tripId;
    private String context;
    private String status; // "processing", "completed", "failed"
    private List<RecommendationItem> recommendations;
    private LocalDateTime responseTime;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendationItem {
        private String title;
        private String description;
        private String category;
        private Double matchScore;
        private String[] features;
        private String optimalVisitTime;
        private List<String> nearbyPlaces;
    }
}