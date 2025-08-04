package com.unicorn.tripgen.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 추천 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecommendationResponse {
    
    private String jobId;
    private String status;
    private String message;
    private String placeId;
    private RecommendationInfo recommendations;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime generatedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime cacheExpiry;
    
    /**
     * 추천 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationInfo {
        private List<String> reasons;
        private TipInfo tips;
    }
    
    /**
     * 팁 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TipInfo {
        private String description;
        private List<String> events;
        private String bestVisitTime;
        private String estimatedDuration;
        private List<String> photoSpots;
        private List<String> practicalTips;
        private List<AlternativePlace> alternativePlaces;
    }
    
    /**
     * 대체 장소 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlternativePlace {
        private String name;
        private String reason;
        private String distance;
    }
}