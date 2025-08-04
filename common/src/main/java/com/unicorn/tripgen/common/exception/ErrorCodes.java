package com.unicorn.tripgen.common.exception;

/**
 * 시스템 전체 에러 코드 정의
 */
public class ErrorCodes {
    
    // Common Errors
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    
    // AI Service Errors
    public static final String AI_JOB_ALREADY_COMPLETED = "AI_JOB_ALREADY_COMPLETED";
    public static final String AI_JOB_FAILED = "AI_JOB_FAILED";
    public static final String AI_JOB_NOT_FOUND = "AI_JOB_NOT_FOUND";
    public static final String AI_RESPONSE_PARSE_ERROR = "AI_RESPONSE_PARSE_ERROR";
    public static final String RECOMMENDATION_GENERATION_FAILED = "RECOMMENDATION_GENERATION_FAILED";
    
    // User Service Errors
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String USERNAME_ALREADY_EXISTS = "USERNAME_ALREADY_EXISTS";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    
    // Trip Service Errors
    public static final String TRIP_NOT_FOUND = "TRIP_NOT_FOUND";
    public static final String TRIP_ACCESS_DENIED = "TRIP_ACCESS_DENIED";
    
    // Location Service Errors
    public static final String LOCATION_NOT_FOUND = "LOCATION_NOT_FOUND";
    public static final String EXTERNAL_API_ERROR = "EXTERNAL_API_ERROR";
    
    private ErrorCodes() {
        // Prevent instantiation
    }
}