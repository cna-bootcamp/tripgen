package com.unicorn.tripgen.common.constant;

/**
 * 시스템 전체에서 사용하는 공통 메시지 상수 클래스
 */
public final class CommonMessages {
    
    /**
     * private 생성자 - 상수 클래스이므로 인스턴스 생성 방지
     */
    private CommonMessages() {
        throw new IllegalStateException("Constants class");
    }
    
    // =============================================================================
    // Success Messages
    // =============================================================================
    
    /** 성공적으로 처리되었습니다 */
    public static final String SUCCESS = "성공적으로 처리되었습니다";
    
    /** 생성이 완료되었습니다 */
    public static final String CREATED = "생성이 완료되었습니다";
    
    /** 수정이 완료되었습니다 */
    public static final String UPDATED = "수정이 완료되었습니다";
    
    /** 삭제가 완료되었습니다 */
    public static final String DELETED = "삭제가 완료되었습니다";
    
    // =============================================================================
    // Common Error Messages
    // =============================================================================
    
    /** 잘못된 요청입니다 */
    public static final String BAD_REQUEST = "잘못된 요청입니다";
    
    /** 인증이 필요합니다 */
    public static final String UNAUTHORIZED = "인증이 필요합니다";
    
    /** 접근 권한이 없습니다 */
    public static final String FORBIDDEN = "접근 권한이 없습니다";
    
    /** 요청한 리소스를 찾을 수 없습니다 */
    public static final String NOT_FOUND = "요청한 리소스를 찾을 수 없습니다";
    
    /** 서버 내부 오류가 발생했습니다 */
    public static final String INTERNAL_SERVER_ERROR = "서버 내부 오류가 발생했습니다";
    
    /** 데이터베이스 오류가 발생했습니다 */
    public static final String DATABASE_ERROR = "데이터베이스 오류가 발생했습니다";
    
    /** 외부 서비스 호출에 실패했습니다 */
    public static final String EXTERNAL_API_ERROR = "외부 서비스 호출에 실패했습니다";
    
    /** 캐시 처리 중 오류가 발생했습니다 */
    public static final String CACHE_ERROR = "캐시 처리 중 오류가 발생했습니다";
    
    /** 파일 처리 중 오류가 발생했습니다 */
    public static final String FILE_ERROR = "파일 처리 중 오류가 발생했습니다";
    
    /** 네트워크 오류가 발생했습니다 */
    public static final String NETWORK_ERROR = "네트워크 오류가 발생했습니다";
    
    // =============================================================================
    // Validation Error Messages
    // =============================================================================
    
    /** 필수 입력 항목입니다 */
    public static final String REQUIRED_FIELD = "필수 입력 항목입니다";
    
    /** 형식이 올바르지 않습니다 */
    public static final String INVALID_FORMAT = "형식이 올바르지 않습니다";
    
    /** 값이 너무 짧습니다 */
    public static final String VALUE_TOO_SHORT = "값이 너무 짧습니다";
    
    /** 값이 너무 깁니다 */
    public static final String VALUE_TOO_LONG = "값이 너무 깁니다";
    
    /** 올바른 이메일 주소를 입력해주세요 */
    public static final String INVALID_EMAIL = "올바른 이메일 주소를 입력해주세요";
    
    /** 올바른 휴대폰 번호를 입력해주세요 (010-1234-5678 형식) */
    public static final String INVALID_PHONE = "올바른 휴대폰 번호를 입력해주세요 (010-1234-5678 형식)";
    
    /** 비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 포함해야 합니다 */
    public static final String INVALID_PASSWORD = "비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 포함해야 합니다";
    
    /** 올바른 날짜 형식을 입력해주세요 */
    public static final String INVALID_DATE = "올바른 날짜 형식을 입력해주세요";
    
    /** 시작 날짜는 종료 날짜보다 이전이어야 합니다 */
    public static final String INVALID_DATE_RANGE = "시작 날짜는 종료 날짜보다 이전이어야 합니다";
    
    /** 허용된 범위를 벗어났습니다 */
    public static final String VALUE_OUT_OF_RANGE = "허용된 범위를 벗어났습니다";
    
    // =============================================================================
    // User Service Messages
    // =============================================================================
    
    /** 회원가입이 완료되었습니다 */
    public static final String REGISTRATION_SUCCESS = "회원가입이 완료되었습니다";
    
    /** 로그인되었습니다 */
    public static final String LOGIN_SUCCESS = "로그인되었습니다";
    
    /** 안전하게 로그아웃되었습니다 */
    public static final String LOGOUT_SUCCESS = "안전하게 로그아웃되었습니다";
    
    /** 프로필이 수정되었습니다 */
    public static final String PROFILE_UPDATED = "프로필이 수정되었습니다";
    
    /** 비밀번호가 변경되었습니다 */
    public static final String PASSWORD_CHANGED = "비밀번호가 변경되었습니다";
    
    /** 프로필 이미지가 업로드되었습니다 */
    public static final String AVATAR_UPLOADED = "프로필 이미지가 업로드되었습니다";
    
    /** 사용 가능한 아이디입니다 */
    public static final String USERNAME_AVAILABLE = "사용 가능한 아이디입니다";
    
    /** 이미 사용중인 아이디입니다 */
    public static final String USERNAME_UNAVAILABLE = "이미 사용중인 아이디입니다";
    
    /** 사용 가능한 이메일입니다 */
    public static final String EMAIL_AVAILABLE = "사용 가능한 이메일입니다";
    
    /** 이미 사용중인 이메일입니다 */
    public static final String EMAIL_UNAVAILABLE = "이미 사용중인 이메일입니다";
    
    /** 사용자를 찾을 수 없습니다 */
    public static final String USER_NOT_FOUND = "사용자를 찾을 수 없습니다";
    
    /** 아이디 또는 비밀번호를 확인해주세요 */
    public static final String INVALID_CREDENTIALS = "아이디 또는 비밀번호를 확인해주세요";
    
    /** 계정이 잠금되었습니다 */
    public static final String ACCOUNT_LOCKED = "계정이 잠금되었습니다";
    
    /** 현재 비밀번호가 일치하지 않습니다 */
    public static final String PASSWORD_MISMATCH = "현재 비밀번호가 일치하지 않습니다";
    
    /** 토큰이 만료되었습니다 */
    public static final String TOKEN_EXPIRED = "토큰이 만료되었습니다";
    
    /** 유효하지 않은 토큰입니다 */
    public static final String INVALID_TOKEN = "유효하지 않은 토큰입니다";
    
    /** 지원하지 않는 파일 형식입니다 */
    public static final String UNSUPPORTED_FILE_TYPE = "지원하지 않는 파일 형식입니다";
    
    /** 파일 크기가 너무 큽니다 */
    public static final String FILE_TOO_LARGE = "파일 크기가 너무 큽니다";
    
    // =============================================================================
    // Location Service Messages
    // =============================================================================
    
    /** 위치 검색이 완료되었습니다 */
    public static final String LOCATION_SEARCH_SUCCESS = "위치 검색이 완료되었습니다";
    
    /** 위치 정보를 찾을 수 없습니다 */
    public static final String LOCATION_NOT_FOUND = "위치 정보를 찾을 수 없습니다";
    
    /** 날씨 정보를 가져왔습니다 */
    public static final String WEATHER_DATA_SUCCESS = "날씨 정보를 가져왔습니다";
    
    /** 날씨 정보를 가져올 수 없습니다 */
    public static final String WEATHER_DATA_UNAVAILABLE = "날씨 정보를 가져올 수 없습니다";
    
    /** 경로 정보를 가져왔습니다 */
    public static final String ROUTE_DATA_SUCCESS = "경로 정보를 가져왔습니다";
    
    /** 경로를 찾을 수 없습니다 */
    public static final String ROUTE_NOT_FOUND = "경로를 찾을 수 없습니다";
    
    /** 주변 장소 검색이 완료되었습니다 */
    public static final String NEARBY_PLACES_SUCCESS = "주변 장소 검색이 완료되었습니다";
    
    /** 검색 결과가 없습니다 */
    public static final String NO_SEARCH_RESULTS = "검색 결과가 없습니다";
    
    // =============================================================================
    // AI Service Messages
    // =============================================================================
    
    /** AI 일정 생성 요청이 접수되었습니다 */
    public static final String AI_SCHEDULE_REQUEST_ACCEPTED = "AI 일정 생성 요청이 접수되었습니다";
    
    /** AI 추천 생성 요청이 접수되었습니다 */
    public static final String AI_RECOMMENDATION_REQUEST_ACCEPTED = "AI 추천 생성 요청이 접수되었습니다";
    
    /** AI 작업이 완료되었습니다 */
    public static final String AI_JOB_COMPLETED = "AI 작업이 완료되었습니다";
    
    /** AI 작업을 찾을 수 없습니다 */
    public static final String AI_JOB_NOT_FOUND = "AI 작업을 찾을 수 없습니다";
    
    /** AI 작업 실행에 실패했습니다 */
    public static final String AI_JOB_FAILED = "AI 작업 실행에 실패했습니다";
    
    /** AI 작업이 시간 초과되었습니다 */
    public static final String AI_JOB_TIMEOUT = "AI 작업이 시간 초과되었습니다";
    
    // =============================================================================
    // Trip Service Messages
    // =============================================================================
    
    /** 여행이 생성되었습니다 */
    public static final String TRIP_CREATED = "여행이 생성되었습니다";
    
    /** 여행 정보가 수정되었습니다 */
    public static final String TRIP_UPDATED = "여행 정보가 수정되었습니다";
    
    /** 여행이 삭제되었습니다 */
    public static final String TRIP_DELETED = "여행이 삭제되었습니다";
    
    /** 여행을 찾을 수 없습니다 */
    public static final String TRIP_NOT_FOUND = "여행을 찾을 수 없습니다";
    
    /** 멤버가 추가되었습니다 */
    public static final String MEMBER_ADDED = "멤버가 추가되었습니다";
    
    /** 멤버 정보가 수정되었습니다 */
    public static final String MEMBER_UPDATED = "멤버 정보가 수정되었습니다";
    
    /** 멤버가 삭제되었습니다 */
    public static final String MEMBER_DELETED = "멤버가 삭제되었습니다";
    
    /** 멤버를 찾을 수 없습니다 */
    public static final String MEMBER_NOT_FOUND = "멤버를 찾을 수 없습니다";
    
    /** 여행지가 추가되었습니다 */
    public static final String DESTINATION_ADDED = "여행지가 추가되었습니다";
    
    /** 여행지 정보가 수정되었습니다 */
    public static final String DESTINATION_UPDATED = "여행지 정보가 수정되었습니다";
    
    /** 여행지가 삭제되었습니다 */
    public static final String DESTINATION_DELETED = "여행지가 삭제되었습니다";
    
    /** 여행지를 찾을 수 없습니다 */
    public static final String DESTINATION_NOT_FOUND = "여행지를 찾을 수 없습니다";
    
    /** 일정이 생성되었습니다 */
    public static final String SCHEDULE_CREATED = "일정이 생성되었습니다";
    
    /** 일정이 수정되었습니다 */
    public static final String SCHEDULE_UPDATED = "일정이 수정되었습니다";
    
    /** 일정을 찾을 수 없습니다 */
    public static final String SCHEDULE_NOT_FOUND = "일정을 찾을 수 없습니다";
    
    /** 일정이 내보내졌습니다 */
    public static final String SCHEDULE_EXPORTED = "일정이 내보내졌습니다";
    
    /** 여행 권한이 없습니다 */
    public static final String NO_TRIP_PERMISSION = "여행 권한이 없습니다";
    
    /** 이미 참여중인 멤버입니다 */
    public static final String DUPLICATE_MEMBER = "이미 참여중인 멤버입니다";
    
    /** 이미 추가된 여행지입니다 */
    public static final String DUPLICATE_DESTINATION = "이미 추가된 여행지입니다";
    
    /** 최대 멤버 수를 초과했습니다 */
    public static final String MAX_MEMBERS_EXCEEDED = "최대 멤버 수를 초과했습니다";
    
    /** 최대 여행지 수를 초과했습니다 */
    public static final String MAX_DESTINATIONS_EXCEEDED = "최대 여행지 수를 초과했습니다";
}