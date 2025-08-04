package com.unicorn.tripgen.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 아이디 중복 확인 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsernameCheckResponse {
    
    /**
     * 사용 가능 여부
     */
    private boolean available;
    
    /**
     * 응답 메시지
     */
    private String message;
    
    /**
     * 사용 가능한 아이디 응답 생성
     * 
     * @param message 메시지
     * @return UsernameCheckResponse 객체
     */
    public static UsernameCheckResponse available(String message) {
        return UsernameCheckResponse.builder()
                .available(true)
                .message(message)
                .build();
    }
    
    /**
     * 사용 불가능한 아이디 응답 생성
     * 
     * @param message 메시지
     * @return UsernameCheckResponse 객체
     */
    public static UsernameCheckResponse unavailable(String message) {
        return UsernameCheckResponse.builder()
                .available(false)
                .message(message)
                .build();
    }
}