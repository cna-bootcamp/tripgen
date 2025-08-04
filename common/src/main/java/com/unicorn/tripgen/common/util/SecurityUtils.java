package com.unicorn.tripgen.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 보안 관련 유틸리티 클래스
 */
public final class SecurityUtils {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    /**
     * private 생성자 - 유틸리티 클래스이므로 인스턴스 생성 방지
     */
    private SecurityUtils() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * 현재 인증된 사용자의 인증 정보 반환
     * 
     * @return Authentication 객체 (인증되지 않은 경우 null)
     */
    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
    
    /**
     * 현재 인증된 사용자의 사용자명 반환
     * 
     * @return 사용자명 (인증되지 않은 경우 null)
     */
    public static String getCurrentUsername() {
        Authentication authentication = getCurrentAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            return (String) principal;
        }
        
        return null;
    }
    
    /**
     * 현재 사용자가 인증되었는지 확인
     * 
     * @return 인증 여부
     */
    public static boolean isAuthenticated() {
        Authentication authentication = getCurrentAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getPrincipal());
    }
    
    /**
     * 현재 사용자가 특정 권한을 가지고 있는지 확인
     * 
     * @param authority 확인할 권한
     * @return 권한 보유 여부
     */
    public static boolean hasAuthority(String authority) {
        Authentication authentication = getCurrentAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(authority));
    }
    
    /**
     * 현재 사용자가 특정 역할을 가지고 있는지 확인
     * 
     * @param role 확인할 역할 (ROLE_ 접두사 제외)
     * @return 역할 보유 여부
     */
    public static boolean hasRole(String role) {
        return hasAuthority("ROLE_" + role);
    }
    
    /**
     * 보안 랜덤 문자열 생성
     * 
     * @param length 생성할 문자열 길이
     * @return Base64 인코딩된 랜덤 문자열
     */
    public static String generateSecureRandomString(int length) {
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * 파일 확장자가 허용된 이미지 형식인지 확인
     * 
     * @param filename 파일명
     * @return 허용된 이미지 형식 여부
     */
    public static boolean isAllowedImageExtension(String filename) {
        if (StringUtils.isEmpty(filename)) {
            return false;
        }
        
        String lowercaseFilename = filename.toLowerCase();
        return lowercaseFilename.endsWith(".jpg") || 
               lowercaseFilename.endsWith(".jpeg") || 
               lowercaseFilename.endsWith(".png") || 
               lowercaseFilename.endsWith(".gif") || 
               lowercaseFilename.endsWith(".webp");
    }
    
    /**
     * 파일 크기가 허용 범위 내인지 확인
     * 
     * @param fileSize 파일 크기 (바이트)
     * @param maxSizeInMB 최대 허용 크기 (MB)
     * @return 허용 크기 내 여부
     */
    public static boolean isAllowedFileSize(long fileSize, int maxSizeInMB) {
        long maxSizeInBytes = maxSizeInMB * 1024L * 1024L;
        return fileSize > 0 && fileSize <= maxSizeInBytes;
    }
    
    /**
     * XSS 공격 방지를 위한 HTML 태그 제거
     * 
     * @param input 입력 문자열
     * @return HTML 태그가 제거된 문자열
     */
    public static String sanitizeHtml(String input) {
        if (StringUtils.isEmpty(input)) {
            return input;
        }
        
        // 기본적인 HTML 태그 제거 (더 강력한 라이브러리 사용 권장)
        return input.replaceAll("<[^>]*>", "")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&quot;", "\"")
                   .replaceAll("&#x27;", "'")
                   .replaceAll("&#x2F;", "/");
    }
    
    /**
     * SQL Injection 방지를 위한 기본적인 특수문자 이스케이프
     * 
     * @param input 입력 문자열
     * @return 이스케이프된 문자열
     */
    public static String escapeSql(String input) {
        if (StringUtils.isEmpty(input)) {
            return input;
        }
        
        return input.replace("'", "''")
                   .replace("\\", "\\\\")
                   .replace("\0", "\\0")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\u001A", "\\Z");
    }
}