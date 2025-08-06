package com.unicorn.tripgen.location.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 테스트용 JWT 토큰 생성 유틸리티
 */
public class TestJwtTokenGenerator {
    
    private static final String JWT_SECRET = "thisisaverylongsecretkeythatisatleast256bitsforhmacsignaturealgorithm";
    private static final long EXPIRATION_TIME = 3600000; // 1시간
    
    public static void main(String[] args) {
        String token = generateToken("testuser");
        System.out.println("Generated JWT Token:");
        System.out.println(token);
        System.out.println("\nUse this token with Authorization header:");
        System.out.println("Authorization: Bearer " + token);
    }
    
    public static String generateToken(String userId) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);
        
        return Jwts.builder()
                .subject(userId)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }
}