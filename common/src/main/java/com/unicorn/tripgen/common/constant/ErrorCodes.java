package com.unicorn.tripgen.common.constant;

/**
 * 시스템 전체에서 사용하는 에러 코드 상수 클래스
 */
public final class ErrorCodes {
    
    /**
     * private 생성자 - 상수 클래스이므로 인스턴스 생성 방지
     */
    private ErrorCodes() {
        throw new IllegalStateException("Constants class");
    }
    
    // =============================================================================
    // Common Error Codes (COMMON_XXX)
    // =============================================================================
    
    /** 일반적인 잘못된 요청 */
    public static final String BAD_REQUEST = "COMMON_001";
    
    /** 인증 실패 */
    public static final String UNAUTHORIZED = "COMMON_002";
    
    /** 접근 권한 부족 */
    public static final String FORBIDDEN = "COMMON_003";
    
    /** 리소스를 찾을 수 없음 */
    public static final String NOT_FOUND = "COMMON_004";
    
    /** 서버 내부 오류 */
    public static final String INTERNAL_SERVER_ERROR = "COMMON_005";
    
    /** 데이터베이스 연결 오류 */
    public static final String DATABASE_ERROR = "COMMON_006";
    
    /** 외부 API 호출 실패 */
    public static final String EXTERNAL_API_ERROR = "COMMON_007";
    
    /** 캐시 오류 */
    public static final String CACHE_ERROR = "COMMON_008";
    
    /** 파일 처리 오류 */
    public static final String FILE_ERROR = "COMMON_009";
    
    /** 네트워크 오류 */
    public static final String NETWORK_ERROR = "COMMON_010";
    
    // =============================================================================
    // Validation Error Codes (VALID_XXX)
    // =============================================================================
    
    /** 필수 값 누락 */
    public static final String REQUIRED_FIELD_MISSING = "VALID_001";
    
    /** 잘못된 형식 */
    public static final String INVALID_FORMAT = "VALID_002";
    
    /** 값이 너무 짧음 */
    public static final String VALUE_TOO_SHORT = "VALID_003";
    
    /** 값이 너무 긺 */
    public static final String VALUE_TOO_LONG = "VALID_004";
    
    /** 잘못된 이메일 형식 */
    public static final String INVALID_EMAIL_FORMAT = "VALID_005";
    
    /** 잘못된 휴대폰 번호 형식 */
    public static final String INVALID_PHONE_FORMAT = "VALID_006";
    
    /** 잘못된 비밀번호 형식 */
    public static final String INVALID_PASSWORD_FORMAT = "VALID_007";
    
    /** 잘못된 날짜 형식 */
    public static final String INVALID_DATE_FORMAT = "VALID_008";
    
    /** 잘못된 날짜 범위 */
    public static final String INVALID_DATE_RANGE = "VALID_009";
    
    /** 값이 허용 범위를 벗어남 */
    public static final String VALUE_OUT_OF_RANGE = "VALID_010";
    
    // =============================================================================
    // User Service Error Codes (USER_XXX)
    // =============================================================================
    
    /** 중복된 사용자명 */
    public static final String DUPLICATE_USERNAME = "USER_001";
    
    /** 중복된 이메일 */
    public static final String DUPLICATE_EMAIL = "USER_002";
    
    /** 사용자를 찾을 수 없음 */
    public static final String USER_NOT_FOUND = "USER_003";
    
    /** 잘못된 로그인 정보 */
    public static final String INVALID_CREDENTIALS = "USER_004";
    
    /** 계정 잠금 */
    public static final String ACCOUNT_LOCKED = "USER_005";
    
    /** 비밀번호 불일치 */
    public static final String PASSWORD_MISMATCH = "USER_006";
    
    /** 토큰 만료 */
    public static final String TOKEN_EXPIRED = "USER_007";
    
    /** 잘못된 토큰 */
    public static final String INVALID_TOKEN = "USER_008";
    
    /** 잘못된 파일 형식 */
    public static final String INVALID_FILE_TYPE = "USER_009";
    
    /** 파일 크기 초과 */
    public static final String FILE_SIZE_EXCEEDED = "USER_010";
    
    // =============================================================================
    // Location Service Error Codes (LOCATION_XXX)
    // =============================================================================
    
    /** 위치를 찾을 수 없음 */
    public static final String LOCATION_NOT_FOUND = "LOCATION_001";
    
    /** 날씨 정보를 가져올 수 없음 */
    public static final String WEATHER_DATA_UNAVAILABLE = "LOCATION_002";
    
    /** 경로를 찾을 수 없음 */
    public static final String ROUTE_NOT_FOUND = "LOCATION_003";
    
    /** 외부 지도 API 오류 */
    public static final String MAP_API_ERROR = "LOCATION_004";
    
    /** 외부 날씨 API 오류 */
    public static final String WEATHER_API_ERROR = "LOCATION_005";
    
    /** 잘못된 좌표 */
    public static final String INVALID_COORDINATES = "LOCATION_006";
    
    /** 검색 결과 없음 */
    public static final String NO_SEARCH_RESULTS = "LOCATION_007";
    
    // =============================================================================
    // AI Service Error Codes (AI_XXX)
    // =============================================================================
    
    /** AI 작업을 찾을 수 없음 */
    public static final String AI_JOB_NOT_FOUND = "AI_001";
    
    /** AI 작업 실행 실패 */
    public static final String AI_JOB_EXECUTION_FAILED = "AI_002";
    
    /** AI 모델 응답 오류 */
    public static final String AI_MODEL_ERROR = "AI_003";
    
    /** AI 작업 시간 초과 */
    public static final String AI_JOB_TIMEOUT = "AI_004";
    
    /** 잘못된 AI 요청 */
    public static final String INVALID_AI_REQUEST = "AI_005";
    
    /** AI 서비스 사용량 초과 */
    public static final String AI_QUOTA_EXCEEDED = "AI_006";
    
    // =============================================================================
    // Trip Service Error Codes (TRIP_XXX)
    // =============================================================================
    
    /** 여행을 찾을 수 없음 */
    public static final String TRIP_NOT_FOUND = "TRIP_001";
    
    /** 여행 멤버를 찾을 수 없음 */
    public static final String MEMBER_NOT_FOUND = "TRIP_002";
    
    /** 여행지를 찾을 수 없음 */
    public static final String DESTINATION_NOT_FOUND = "TRIP_003";
    
    /** 일정을 찾을 수 없음 */
    public static final String SCHEDULE_NOT_FOUND = "TRIP_004";
    
    /** 중복된 여행 멤버 */
    public static final String DUPLICATE_MEMBER = "TRIP_005";
    
    /** 중복된 여행지 */
    public static final String DUPLICATE_DESTINATION = "TRIP_006";
    
    /** 여행 권한 없음 */
    public static final String NO_TRIP_PERMISSION = "TRIP_007";
    
    /** 여행 상태 변경 불가 */
    public static final String INVALID_TRIP_STATUS_CHANGE = "TRIP_008";
    
    /** 여행 기간 충돌 */
    public static final String TRIP_DATE_CONFLICT = "TRIP_009";
    
    /** 최대 멤버 수 초과 */
    public static final String MAX_MEMBERS_EXCEEDED = "TRIP_010";
    
    /** 최대 여행지 수 초과 */
    public static final String MAX_DESTINATIONS_EXCEEDED = "TRIP_011";
    
    /** 일정 생성 실패 */
    public static final String SCHEDULE_GENERATION_FAILED = "TRIP_012";
}