package com.unicorn.tripgen.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * AI 일정 생성 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateScheduleRequest {
    
    @NotBlank(message = "여행 ID는 필수입니다")
    private String tripId;
    
    @NotBlank(message = "여행명은 필수입니다")
    private String tripName;
    
    @NotBlank(message = "이동수단은 필수입니다")
    @Pattern(regexp = "^(public|car)$", message = "이동수단은 public 또는 car만 가능합니다")
    private String transportMode;
    
    @NotBlank(message = "시작 시간은 필수입니다")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "시작 시간은 HH:mm 형식이어야 합니다")
    private String startTime;
    
    @NotEmpty(message = "멤버 정보는 최소 1명 이상이어야 합니다")
    @Valid
    private List<MemberInfo> members;
    
    @NotEmpty(message = "여행지 정보는 최소 1개 이상이어야 합니다")
    @Valid
    private List<DestinationInfo> destinations;
    
    @Size(max = 500, message = "특별 요청사항은 500자를 초과할 수 없습니다")
    private String specialRequests;
    
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    
    private Map<String, Object> preferences;
    
    private Map<String, Object> constraints;
    
    /**
     * 멤버 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        
        @NotBlank(message = "멤버 이름은 필수입니다")
        private String name;
        
        @NotNull(message = "나이는 필수입니다")
        @Min(value = 1, message = "나이는 1세 이상이어야 합니다")
        @Max(value = 150, message = "나이는 150세 이하여야 합니다")
        private Integer age;
        
        @NotBlank(message = "성별은 필수입니다")
        @Pattern(regexp = "^(male|female)$", message = "성별은 male 또는 female만 가능합니다")
        private String gender;
        
        @NotBlank(message = "건강 상태는 필수입니다")
        @Pattern(regexp = "^(excellent|good|caution|limited)$", 
                message = "건강 상태는 excellent, good, caution, limited 중 하나여야 합니다")
        private String healthStatus;
        
        private List<String> preferences;
    }
    
    /**
     * 여행지 정보 내부 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DestinationInfo {
        
        @NotBlank(message = "여행지명은 필수입니다")
        private String destinationName;
        
        @NotNull(message = "숙박 일수는 필수입니다")
        @Min(value = 1, message = "숙박 일수는 1일 이상이어야 합니다")
        private Integer nights;
        
        @NotNull(message = "시작 날짜는 필수입니다")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate startDate;
        
        @NotNull(message = "종료 날짜는 필수입니다")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate endDate;
        
        @NotBlank(message = "숙소명은 필수입니다")
        private String accommodation;
        
        @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "체크인 시간은 HH:mm 형식이어야 합니다")
        private String checkInTime;
        
        @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "체크아웃 시간은 HH:mm 형식이어야 합니다")
        private String checkOutTime;
    }
}