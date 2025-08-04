package com.unicorn.tripgen.user.service;

/**
 * 비밀번호 서비스 인터페이스
 * 비밀번호 암호화 및 검증을 담당
 */
public interface PasswordService {
    
    /**
     * 평문 비밀번호를 암호화
     * 
     * @param rawPassword 평문 비밀번호
     * @return 암호화된 비밀번호
     */
    String encode(String rawPassword);
    
    /**
     * 평문 비밀번호와 암호화된 비밀번호 비교
     * 
     * @param rawPassword 평문 비밀번호
     * @param encodedPassword 암호화된 비밀번호
     * @return 일치 여부
     */
    boolean matches(String rawPassword, String encodedPassword);
    
    /**
     * 임시 비밀번호 생성
     * 
     * @param length 비밀번호 길이
     * @return 임시 비밀번호
     */
    String generateTemporaryPassword(int length);
}