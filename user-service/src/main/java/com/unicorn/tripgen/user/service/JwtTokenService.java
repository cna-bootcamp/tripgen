package com.unicorn.tripgen.user.service;

import com.unicorn.tripgen.common.constant.ErrorCodes;
import com.unicorn.tripgen.common.constant.CommonMessages;
import com.unicorn.tripgen.common.exception.UnauthorizedException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * JWT 기반 토큰 서비스 구현
 */
@Service
@Slf4j
public class JwtTokenService implements TokenService {
    
    private final SecretKey secretKey;
    private final int accessTokenExpirationSeconds;
    private final int refreshTokenExpirationSeconds;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:token:";
    private static final String USER_TOKENS_PREFIX = "user:tokens:";
    
    public JwtTokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration:3600}") int accessTokenExpirationSeconds,
            @Value("${app.jwt.refresh-token-expiration:604800}") int refreshTokenExpirationSeconds,
            RedisTemplate<String, String> redisTemplate) {
        
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public String generateAccessToken(String userId) {
        log.debug("액세스 토큰 생성 시작: userId={}", userId);
        
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(accessTokenExpirationSeconds);
        
        String token = Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .claim("type", "access")
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
        
        // Redis에 사용자 토큰 저장 (세션 관리용)
        String userTokensKey = USER_TOKENS_PREFIX + userId;
        redisTemplate.opsForSet().add(userTokensKey, token);
        redisTemplate.expire(userTokensKey, Duration.ofSeconds(accessTokenExpirationSeconds));
        
        log.debug("액세스 토큰 생성 완료: userId={}", userId);
        return token;
    }
    
    @Override
    public String generateRefreshToken(String userId) {
        log.debug("리프레시 토큰 생성 시작: userId={}", userId);
        
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(refreshTokenExpirationSeconds);
        
        String token = Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .claim("type", "refresh")
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
        
        log.debug("리프레시 토큰 생성 완료: userId={}", userId);
        return token;
    }
    
    @Override
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getSubject();
        } catch (JwtException e) {
            log.error("토큰에서 사용자 ID 추출 실패: {}", e.getMessage());
            throw new UnauthorizedException(ErrorCodes.INVALID_TOKEN, CommonMessages.INVALID_TOKEN);
        }
    }
    
    @Override
    public boolean validateToken(String token) {
        try {
            if (isTokenBlacklisted(token)) {
                log.debug("블랙리스트에 있는 토큰: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
                return false;
            }
            
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            
            return true;
        } catch (JwtException e) {
            log.debug("토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            log.debug("토큰이 유효하지 않음: {}", e.getMessage());
            return true;
        }
    }
    
    @Override
    public Integer getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }
    
    @Override
    public Integer getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }
    
    @Override
    public void invalidateUserTokens(String userId) {
        log.info("사용자 토큰 무효화 시작: userId={}", userId);
        
        String userTokensKey = USER_TOKENS_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);
        
        if (tokens != null && !tokens.isEmpty()) {
            // 각 토큰을 블랙리스트에 추가
            for (String token : tokens) {
                invalidateToken(token);
            }
            
            // 사용자 토큰 세트 삭제
            redisTemplate.delete(userTokensKey);
        }
        
        log.info("사용자 토큰 무효화 완료: userId={}, tokenCount={}", userId, 
                tokens != null ? tokens.size() : 0);
    }
    
    @Override
    public void invalidateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            // 토큰의 만료 시간까지 블랙리스트에 저장
            Date expiration = claims.getExpiration();
            long ttl = expiration.getTime() - System.currentTimeMillis();
            
            if (ttl > 0) {
                String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
                redisTemplate.opsForValue().set(blacklistKey, "true", ttl, TimeUnit.MILLISECONDS);
                log.debug("토큰을 블랙리스트에 추가: ttl={}ms", ttl);
            }
        } catch (JwtException e) {
            log.warn("유효하지 않은 토큰 무효화 시도: {}", e.getMessage());
        }
    }
    
    @Override
    public boolean isTokenBlacklisted(String token) {
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }
    
    @Override
    public String refreshAccessToken(String refreshToken) {
        log.debug("액세스 토큰 갱신 시작");
        
        if (!validateToken(refreshToken)) {
            throw new UnauthorizedException(ErrorCodes.INVALID_TOKEN, "유효하지 않은 리프레시 토큰입니다");
        }
        
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();
            
            String tokenType = claims.get("type", String.class);
            if (!"refresh".equals(tokenType)) {
                throw new UnauthorizedException(ErrorCodes.INVALID_TOKEN, "리프레시 토큰이 아닙니다");
            }
            
            String userId = claims.getSubject();
            String newAccessToken = generateAccessToken(userId);
            
            log.debug("액세스 토큰 갱신 완료: userId={}", userId);
            return newAccessToken;
            
        } catch (JwtException e) {
            log.error("액세스 토큰 갱신 실패: {}", e.getMessage());
            throw new UnauthorizedException(ErrorCodes.INVALID_TOKEN, CommonMessages.INVALID_TOKEN);
        }
    }
}