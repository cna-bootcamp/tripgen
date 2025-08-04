package com.unicorn.tripgen.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 일정 생성 상태 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerationStatusResponse {
    
    private String requestId;
    private String status;
    private Integer progress;
    private String currentStep;
    private List<StepInfo> steps;
    private Integer estimatedTime;
    private String error;
    
    /**
     * 단계 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepInfo {
        private String step;
        private String status;
    }
    
    /**
     * 진행 중 상태 응답 생성
     */
    public static GenerationStatusResponse processing(String requestId, int progress, String currentStep, 
                                                    List<StepInfo> steps, Integer estimatedTime) {
        return GenerationStatusResponse.builder()
                .requestId(requestId)
                .status("processing")
                .progress(progress)
                .currentStep(currentStep)
                .steps(steps)
                .estimatedTime(estimatedTime)
                .build();
    }
    
    /**
     * 완료 상태 응답 생성
     */
    public static GenerationStatusResponse completed(String requestId) {
        return GenerationStatusResponse.builder()
                .requestId(requestId)
                .status("completed")
                .progress(100)
                .currentStep("완료")
                .estimatedTime(0)
                .build();
    }
    
    /**
     * 실패 상태 응답 생성
     */
    public static GenerationStatusResponse failed(String requestId, String error) {
        return GenerationStatusResponse.builder()
                .requestId(requestId)
                .status("failed")
                .progress(0)
                .currentStep("실패")
                .error(error)
                .build();
    }
}