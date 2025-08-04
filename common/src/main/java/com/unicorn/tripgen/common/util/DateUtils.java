package com.unicorn.tripgen.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * 날짜/시간 관련 유틸리티 클래스
 */
public final class DateUtils {
    
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT);
    
    /**
     * private 생성자 - 유틸리티 클래스이므로 인스턴스 생성 방지
     */
    private DateUtils() {
        throw new IllegalStateException("Utility class");
    }
    
    /**
     * 현재 날짜를 문자열로 반환
     * 
     * @return 현재 날짜 (yyyy-MM-dd 형식)
     */
    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }
    
    /**
     * 현재 날짜시간을 문자열로 반환
     * 
     * @return 현재 날짜시간 (yyyy-MM-dd HH:mm:ss 형식)
     */
    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }
    
    /**
     * LocalDate를 문자열로 변환
     * 
     * @param date 변환할 날짜
     * @return 문자열 형태의 날짜
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : null;
    }
    
    /**
     * LocalDateTime을 문자열로 변환
     * 
     * @param dateTime 변환할 날짜시간
     * @return 문자열 형태의 날짜시간
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }
    
    /**
     * 문자열을 LocalDate로 파싱
     * 
     * @param dateString 파싱할 날짜 문자열
     * @return LocalDate 객체
     * @throws DateTimeParseException 파싱 실패시
     */
    public static LocalDate parseDate(String dateString) {
        return StringUtils.hasText(dateString) ? LocalDate.parse(dateString, DATE_FORMATTER) : null;
    }
    
    /**
     * 문자열을 LocalDateTime으로 파싱
     * 
     * @param dateTimeString 파싱할 날짜시간 문자열
     * @return LocalDateTime 객체
     * @throws DateTimeParseException 파싱 실패시
     */
    public static LocalDateTime parseDateTime(String dateTimeString) {
        return StringUtils.hasText(dateTimeString) ? LocalDateTime.parse(dateTimeString, DATETIME_FORMATTER) : null;
    }
    
    /**
     * 두 날짜 사이의 일수 계산
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 일수 차이
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate);
    }
    
    /**
     * 두 날짜시간 사이의 시간 계산 (분 단위)
     * 
     * @param startDateTime 시작 날짜시간
     * @param endDateTime 종료 날짜시간
     * @return 분 차이
     */
    public static long minutesBetween(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        if (startDateTime == null || endDateTime == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(startDateTime, endDateTime);
    }
    
    /**
     * 날짜가 유효한 범위 내에 있는지 확인
     * 
     * @param date 확인할 날짜
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 유효 범위 내 여부
     */
    public static boolean isDateInRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (date == null || startDate == null || endDate == null) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
    
    /**
     * 날짜가 과거인지 확인
     * 
     * @param date 확인할 날짜
     * @return 과거 날짜 여부
     */
    public static boolean isPastDate(LocalDate date) {
        return date != null && date.isBefore(LocalDate.now());
    }
    
    /**
     * 날짜가 미래인지 확인
     * 
     * @param date 확인할 날짜
     * @return 미래 날짜 여부
     */
    public static boolean isFutureDate(LocalDate date) {
        return date != null && date.isAfter(LocalDate.now());
    }
}