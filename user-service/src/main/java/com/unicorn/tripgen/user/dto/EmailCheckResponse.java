package com.unicorn.tripgen.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 이메일 중복 확인 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailCheckResponse {
    
    /**
     * 사용 가능 여부
     */
    private boolean available;
    
    /**
     * 응답 메시지
     */
    private String message;
    
    /**
     * 사용 가능한 이메일 응답 생성
     * 
     * @param message 메시지
     * @return EmailCheckResponse 객체
     */
    public static EmailCheckResponse available(String message) {
        return EmailCheckResponse.builder()
                .available(true)
                .message(message)
                .build();
    }
    
    /**
     * 사용 불가능한 이메일 응답 생성
     * 
     * @param message 메시지
     * @return EmailCheckResponse 객체
     */
    public static EmailCheckResponse unavailable(String message) {
        return EmailCheckResponse.builder()
                .available(false)
                .message(message)
                .build();
    }
}