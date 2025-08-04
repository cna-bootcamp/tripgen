package com.unicorn.tripgen.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * BCrypt 기반 비밀번호 서비스 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BCryptPasswordService implements PasswordService {
    
    private final PasswordEncoder passwordEncoder;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@$!%*#?&";
    
    @Override
    public String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("비밀번호는 null일 수 없습니다");
        }
        
        log.debug("비밀번호 암호화 시작");
        String encoded = passwordEncoder.encode(rawPassword);
        log.debug("비밀번호 암호화 완료");
        
        return encoded;
    }
    
    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        
        log.debug("비밀번호 검증 시작");
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        log.debug("비밀번호 검증 완료: matches={}", matches);
        
        return matches;
    }
    
    @Override
    public String generateTemporaryPassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("임시 비밀번호는 최소 8자 이상이어야 합니다");
        }
        
        StringBuilder password = new StringBuilder(length);
        
        // 최소 요구사항을 만족하도록 각 문자 유형에서 하나씩 선택
        password.append(getRandomChar("ABCDEFGHIJKLMNOPQRSTUVWXYZ")); // 대문자
        password.append(getRandomChar("abcdefghijklmnopqrstuvwxyz")); // 소문자
        password.append(getRandomChar("0123456789")); // 숫자
        password.append(getRandomChar("@$!%*#?&")); // 특수문자
        
        // 나머지 길이만큼 랜덤 문자 추가
        for (int i = 4; i < length; i++) {
            password.append(getRandomChar(TEMP_PASSWORD_CHARS));
        }
        
        // 문자 순서 섞기
        return shuffleString(password.toString());
    }
    
    /**
     * 문자열에서 랜덤 문자 선택
     */
    private char getRandomChar(String chars) {
        int index = SECURE_RANDOM.nextInt(chars.length());
        return chars.charAt(index);
    }
    
    /**
     * 문자열 순서 섞기
     */
    private String shuffleString(String str) {
        char[] chars = str.toCharArray();
        
        for (int i = chars.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        
        return new String(chars);
    }
}