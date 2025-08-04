package com.unicorn.tripgen.common.util;

import com.unicorn.tripgen.common.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * 검증 관련 유틸리티 클래스
 */
public final class ValidationUtils {
    
    /**
     * private 생성자 - 유틸리티 클래스이므로 인스턴스 생성 방지
     */
    private ValidationUtils() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * 객체가 null이 아님을 검증
     * 
     * @param object 검증할 객체
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 객체가 null인 경우
     */
    public static void notNull(Object object, String errorCode, String message) {
        if (object == null) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 문자열이 비어있지 않음을 검증
     * 
     * @param str 검증할 문자열
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 문자열이 null이거나 빈 문자열인 경우
     */
    public static void notEmpty(String str, String errorCode, String message) {
        if (StringUtils.isEmpty(str)) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 문자열이 공백이 아님을 검증
     * 
     * @param str 검증할 문자열
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 문자열이 null이거나 공백만 포함하는 경우
     */
    public static void hasText(String str, String errorCode, String message) {
        if (!StringUtils.hasText(str)) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 컬렉션이 비어있지 않음을 검증
     * 
     * @param collection 검증할 컬렉션
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 컬렉션이 null이거나 비어있는 경우
     */
    public static void notEmpty(Collection<?> collection, String errorCode, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 문자열 길이가 최소 길이 이상임을 검증
     * 
     * @param str 검증할 문자열
     * @param minLength 최소 길이
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 문자열 길이가 최소 길이 미만인 경우
     */
    public static void minLength(String str, int minLength, String errorCode, String message) {
        if (str == null || str.length() < minLength) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 문자열 길이가 최대 길이 이하임을 검증
     * 
     * @param str 검증할 문자열
     * @param maxLength 최대 길이
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 문자열 길이가 최대 길이 초과인 경우
     */
    public static void maxLength(String str, int maxLength, String errorCode, String message) {
        if (str != null && str.length() > maxLength) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 숫자가 양수임을 검증
     * 
     * @param number 검증할 숫자
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 숫자가 0 이하인 경우
     */
    public static void positive(Number number, String errorCode, String message) {
        if (number == null || number.doubleValue() <= 0) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 숫자가 0 이상임을 검증
     * 
     * @param number 검증할 숫자
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 숫자가 음수인 경우
     */
    public static void notNegative(Number number, String errorCode, String message) {
        if (number == null || number.doubleValue() < 0) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 이메일 형식이 유효함을 검증
     * 
     * @param email 검증할 이메일
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 이메일 형식이 유효하지 않은 경우
     */
    public static void validEmail(String email, String errorCode, String message) {
        if (!StringUtils.isValidEmail(email)) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 휴대폰 번호 형식이 유효함을 검증
     * 
     * @param phone 검증할 휴대폰 번호
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 휴대폰 번호 형식이 유효하지 않은 경우
     */
    public static void validPhone(String phone, String errorCode, String message) {
        if (!StringUtils.isValidPhone(phone)) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 사용자명 형식이 유효함을 검증
     * 
     * @param username 검증할 사용자명
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 사용자명 형식이 유효하지 않은 경우
     */
    public static void validUsername(String username, String errorCode, String message) {
        if (!StringUtils.isValidUsername(username)) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 비밀번호 형식이 유효함을 검증
     * 
     * @param password 검증할 비밀번호
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 비밀번호 형식이 유효하지 않은 경우
     */
    public static void validPassword(String password, String errorCode, String message) {
        if (!StringUtils.isValidPassword(password)) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 날짜가 미래 날짜임을 검증
     * 
     * @param date 검증할 날짜
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 날짜가 과거이거나 현재인 경우
     */
    public static void futureDate(LocalDate date, String errorCode, String message) {
        if (!DateUtils.isFutureDate(date)) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 날짜가 과거 날짜임을 검증
     * 
     * @param date 검증할 날짜
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 날짜가 미래이거나 현재인 경우
     */
    public static void pastDate(LocalDate date, String errorCode, String message) {
        if (!DateUtils.isPastDate(date)) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 시작 날짜가 종료 날짜보다 이전임을 검증
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param errorCode 에러 코드  
     * @param message 에러 메시지
     * @throws ValidationException 시작 날짜가 종료 날짜보다 늦은 경우
     */
    public static void dateRange(LocalDate startDate, LocalDate endDate, String errorCode, String message) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 시작 날짜시간이 종료 날짜시간보다 이전임을 검증
     * 
     * @param startDateTime 시작 날짜시간
     * @param endDateTime 종료 날짜시간
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 시작 날짜시간이 종료 날짜시간보다 늦은 경우
     */
    public static void dateTimeRange(LocalDateTime startDateTime, LocalDateTime endDateTime, String errorCode, String message) {
        if (startDateTime == null || endDateTime == null || startDateTime.isAfter(endDateTime)) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 조건이 참임을 검증
     * 
     * @param condition 검증할 조건
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 조건이 거짓인 경우
     */
    public static void isTrue(boolean condition, String errorCode, String message) {
        if (!condition) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 조건이 거짓임을 검증
     * 
     * @param condition 검증할 조건
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @throws ValidationException 조건이 참인 경우
     */
    public static void isFalse(boolean condition, String errorCode, String message) {
        if (condition) {
            throw new ValidationException(errorCode, message);
        }
    }
    
    /**
     * 조건이 참임을 검증 (지연 평가 메시지)
     * 
     * @param condition 검증할 조건
     * @param errorCode 에러 코드
     * @param messageSupplier 에러 메시지 공급자
     * @throws ValidationException 조건이 거짓인 경우
     */
    public static void isTrue(boolean condition, String errorCode, Supplier<String> messageSupplier) {
        if (!condition) {
            throw new ValidationException(errorCode, messageSupplier.get());
        }
    }
}