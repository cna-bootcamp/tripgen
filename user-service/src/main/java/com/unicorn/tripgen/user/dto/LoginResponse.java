package com.unicorn.tripgen.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    
    /**
     * JWT 액세스 토큰
     */
    @JsonProperty("accessToken")
    private String accessToken;
    
    /**
     * JWT 리프레시 토큰
     */
    @JsonProperty("refreshToken")
    private String refreshToken;
    
    /**
     * 토큰 타입
     */
    @JsonProperty("tokenType")
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * 액세스 토큰 만료 시간 (초)
     */
    @JsonProperty("expiresIn")
    private Integer expiresIn;
    
    /**
     * 사용자 프로필 정보
     */
    private UserProfile user;
    
    /**
     * 성공 응답 생성 팩토리 메소드
     * 
     * @param accessToken 액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @param expiresIn 만료 시간
     * @param userProfile 사용자 프로필
     * @return LoginResponse 객체
     */
    public static LoginResponse success(String accessToken, String refreshToken, 
                                      Integer expiresIn, UserProfile userProfile) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userProfile)
                .build();
    }
}