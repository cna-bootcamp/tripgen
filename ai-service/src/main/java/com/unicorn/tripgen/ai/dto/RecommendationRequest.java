package com.unicorn.tripgen.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 추천 정보 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRequest {
    
    @NotBlank(message = "장소명은 필수입니다")
    private String placeName;
    
    @NotBlank(message = "장소 타입은 필수입니다")
    private String placeType;
    
    private String placeAddress;
    
    @NotNull(message = "사용자 프로필은 필수입니다")
    @Valid
    private UserProfile userProfile;
    
    @Valid
    private TripContext tripContext;
    
    /**
     * 사용자 프로필 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProfile {
        private String memberComposition;
        private String healthStatus;
        private String transportMode;
        private List<String> preferences;
    }
    
    /**
     * 여행 맥락 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripContext {
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate visitDate;
        
        private String visitTime;
        private String previousPlace;
        private String nextPlace;
    }
}