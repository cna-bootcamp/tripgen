package com.unicorn.tripgen.user.service;

/**
 * 토큰 서비스 인터페이스
 * JWT 토큰 생성, 검증, 관리를 담당
 */
public interface TokenService {
    
    /**
     * 액세스 토큰 생성
     * 
     * @param userId 사용자 ID
     * @return 액세스 토큰
     */
    String generateAccessToken(String userId);
    
    /**
     * 리프레시 토큰 생성
     * 
     * @param userId 사용자 ID
     * @return 리프레시 토큰
     */
    String generateRefreshToken(String userId);
    
    /**
     * 토큰에서 사용자 ID 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    String getUserIdFromToken(String token);
    
    /**
     * 토큰 유효성 검증
     * 
     * @param token JWT 토큰
     * @return 유효성 여부
     */
    boolean validateToken(String token);
    
    /**
     * 토큰 만료 여부 확인
     * 
     * @param token JWT 토큰
     * @return 만료 여부
     */
    boolean isTokenExpired(String token);
    
    /**
     * 액세스 토큰 만료 시간 (초) 반환
     * 
     * @return 만료 시간 (초)
     */
    Integer getAccessTokenExpirationSeconds();
    
    /**
     * 리프레시 토큰 만료 시간 (초) 반환
     * 
     * @return 만료 시간 (초)
     */
    Integer getRefreshTokenExpirationSeconds();
    
    /**
     * 사용자의 모든 토큰 무효화
     * 
     * @param userId 사용자 ID
     */
    void invalidateUserTokens(String userId);
    
    /**
     * 특정 토큰 무효화
     * 
     * @param token 무효화할 토큰
     */
    void invalidateToken(String token);
    
    /**
     * 토큰이 블랙리스트에 있는지 확인
     * 
     * @param token 확인할 토큰
     * @return 블랙리스트 여부
     */
    boolean isTokenBlacklisted(String token);
    
    /**
     * 리프레시 토큰으로 새 액세스 토큰 생성
     * 
     * @param refreshToken 리프레시 토큰
     * @return 새로운 액세스 토큰
     */
    String refreshAccessToken(String refreshToken);
}