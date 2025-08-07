package com.unicorn.tripgen.location.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 추천 요청 메시지 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequest {
    
    private String requestId;
    private String placeId;
    private String tripId;
    private String context; // "search" 또는 "trip"
    private SearchContext searchContext;
    private PlaceInfo placeInfo;
    private LocalDateTime requestTime;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SearchContext {
        private String searchQuery;
        private String[] searchIntents;
        private UserLocation userLocation;
        private String[] searchHistory;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserLocation {
        private Double latitude;
        private Double longitude;
        private String address;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlaceInfo {
        private String placeId;
        private String name;
        private String category;
        private Double latitude;
        private Double longitude;
        private String address;
    }
}