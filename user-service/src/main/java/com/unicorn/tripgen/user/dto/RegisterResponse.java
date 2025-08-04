package com.unicorn.tripgen.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회원가입 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {
    
    /**
     * 생성된 사용자 ID
     */
    @JsonProperty("userId")
    private String userId;
    
    /**
     * 사용자 아이디
     */
    private String username;
    
    /**
     * 결과 메시지
     */
    private String message;
    
    /**
     * 성공 응답 생성 팩토리 메소드
     * 
     * @param userId 사용자 ID
     * @param username 사용자명
     * @param message 메시지
     * @return RegisterResponse 객체
     */
    public static RegisterResponse success(String userId, String username, String message) {
        return RegisterResponse.builder()
                .userId(userId)
                .username(username)
                .message(message)
                .build();
    }
}