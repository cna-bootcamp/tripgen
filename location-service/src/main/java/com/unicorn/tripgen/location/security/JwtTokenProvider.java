package com.unicorn.tripgen.location.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 토큰 제공자
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${security.jwt.secret:dev-jwt-secret-key-for-development-only}")
    private String jwtSecret;

    private SecretKey getSigningKey() {
        logger.debug("JWT Secret being used: {}", jwtSecret);
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String token) {
        try {
            logger.debug("Validating token: {}", token);
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            logger.debug("Token validation successful");
            return true;
        } catch (Exception e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            logger.error("Full error: ", e);
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        logger.debug("Claims from token: {}", claims);
        
        // userId 또는 sub claim에서 사용자 ID 추출
        String userId = claims.get("userId", String.class);
        if (userId == null) {
            userId = claims.getSubject();
        }
        
        logger.debug("Extracted userId: {}", userId);
        return userId;
    }
}