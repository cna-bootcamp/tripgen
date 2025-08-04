package com.unicorn.tripgen.common.util;

import java.util.regex.Pattern;

/**
 * 문자열 관련 유틸리티 클래스
 */
public final class StringUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^01[0-9]{1}-[0-9]{3,4}-[0-9]{4}$"
    );
    
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]+$"
    );
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$"
    );
    
    /**
     * private 생성자 - 유틸리티 클래스이므로 인스턴스 생성 방지
     */
    private StringUtils() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * 문자열이 null이거나 빈 문자열인지 확인
     * 
     * @param str 확인할 문자열
     * @return null 또는 빈 문자열 여부
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * 문자열이 null이 아니고 빈 문자열도 아닌지 확인
     * 
     * @param str 확인할 문자열
     * @return null이 아니고 빈 문자열도 아닌지 여부
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 문자열이 null이거나 공백으로만 구성되어 있는지 확인
     * 
     * @param str 확인할 문자열
     * @return null이거나 공백 문자열 여부
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 문자열이 null이 아니고 공백이 아닌 문자를 포함하는지 확인
     * 
     * @param str 확인할 문자열
     * @return 유효한 텍스트 포함 여부
     */
    public static boolean hasText(String str) {
        return !isBlank(str);
    }
    
    /**
     * 문자열 앞뒤 공백 제거 (null 안전)
     * 
     * @param str 처리할 문자열
     * @return 공백이 제거된 문자열 (null 입력시 null 반환)
     */
    public static String trim(String str) {
        return str != null ? str.trim() : null;
    }
    
    /**
     * 문자열 앞뒤 공백 제거 후 빈 문자열인 경우 null 반환
     * 
     * @param str 처리할 문자열
     * @return 공백 제거 후 빈 문자열이면 null, 아니면 trim된 문자열
     */
    public static String trimToNull(String str) {
        String trimmed = trim(str);
        return isEmpty(trimmed) ? null : trimmed;
    }
    
    /**
     * 이메일 형식 유효성 검사
     * 
     * @param email 검사할 이메일 주소
     * @return 유효한 이메일 형식 여부
     */
    public static boolean isValidEmail(String email) {
        return hasText(email) && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 휴대폰 번호 형식 유효성 검사 (010-1234-5678 형식)
     * 
     * @param phone 검사할 휴대폰 번호
     * @return 유효한 휴대폰 번호 형식 여부
     */
    public static boolean isValidPhone(String phone) {
        return hasText(phone) && PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * 사용자명 형식 유효성 검사 (영문자, 숫자만 허용)
     * 
     * @param username 검사할 사용자명
     * @return 유효한 사용자명 형식 여부
     */
    public static boolean isValidUsername(String username) {
        return hasText(username) && username.length() >= 5 && USERNAME_PATTERN.matcher(username).matches();
    }
    
    /**
     * 비밀번호 형식 유효성 검사 (8자 이상, 영문/숫자/특수문자 포함)
     * 
     * @param password 검사할 비밀번호
     * @return 유효한 비밀번호 형식 여부
     */
    public static boolean isValidPassword(String password) {
        return hasText(password) && PASSWORD_PATTERN.matcher(password).matches();
    }
    
    /**
     * 문자열 마스킹 처리 (개인정보 보호용)
     * 
     * @param str 마스킹할 문자열
     * @param visibleStart 앞에서 보여줄 문자 수
     * @param visibleEnd 뒤에서 보여줄 문자 수
     * @param maskChar 마스킹 문자
     * @return 마스킹된 문자열
     */
    public static String mask(String str, int visibleStart, int visibleEnd, char maskChar) {
        if (isEmpty(str) || str.length() <= visibleStart + visibleEnd) {
            return str;
        }
        
        StringBuilder masked = new StringBuilder();
        masked.append(str, 0, visibleStart);
        
        int maskLength = str.length() - visibleStart - visibleEnd;
        for (int i = 0; i < maskLength; i++) {
            masked.append(maskChar);
        }
        
        masked.append(str.substring(str.length() - visibleEnd));
        return masked.toString();
    }
    
    /**
     * 이메일 마스킹 처리
     * 
     * @param email 마스킹할 이메일
     * @return 마스킹된 이메일 (예: kim***@tripgen.com)
     */
    public static String maskEmail(String email) {
        if (!isValidEmail(email)) {
            return email;
        }
        
        int atIndex = email.indexOf('@');
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        
        if (localPart.length() <= 3) {
            return email;
        }
        
        return mask(localPart, 2, 1, '*') + domainPart;
    }
    
    /**
     * 휴대폰 번호 마스킹 처리
     * 
     * @param phone 마스킹할 휴대폰 번호
     * @return 마스킹된 휴대폰 번호 (예: 010-****-5678)
     */
    public static String maskPhone(String phone) {
        if (!isValidPhone(phone)) {
            return phone;
        }
        
        String[] parts = phone.split("-");
        if (parts.length != 3) {
            return phone;
        }
        
        return parts[0] + "-****-" + parts[2];
    }
}