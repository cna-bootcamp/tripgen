package com.unicorn.tripgen.location.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 위치 상세 정보 응답 DTO
 * 특정 장소의 상세 정보를 담는 응답 클래스
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationDetailResponse {
    
    /**
     * 장소 ID
     */
    private String placeId;
    
    /**
     * 장소명
     */
    private String name;
    
    /**
     * 카테고리
     */
    private String category;
    
    /**
     * 장소 설명
     */
    private String description;
    
    /**
     * 평점
     */
    private BigDecimal rating;
    
    /**
     * 리뷰 수
     */
    private Integer reviewCount;
    
    /**
     * 가격 수준 (0-4)
     */
    private Integer priceLevel;
    
    /**
     * 장소 이미지 URL 목록
     */
    private List<String> images;
    
    /**
     * 위치 정보
     */
    private LocationInfo location;
    
    /**
     * 연락처 정보
     */
    private ContactInfo contact;
    
    /**
     * 최신 리뷰 목록 (최대 5개)
     */
    private List<Review> reviews;
    
    /**
     * 데이터 소스
     */
    private String dataSource;
    
    /**
     * 캐시에서 가져온 데이터 여부
     */
    private Boolean fromCache;
    
    /**
     * 데이터 생성/업데이트 시간
     */
    private LocalDateTime lastUpdated;
    
    /**
     * 위치 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationInfo {
        
        /**
         * 위도
         */
        private BigDecimal latitude;
        
        /**
         * 경도
         */
        private BigDecimal longitude;
        
        /**
         * 전체 주소
         */
        private String address;
        
        /**
         * 지도 검색용 키워드 (한국어)
         */
        private String searchKeywordKo;
        
        /**
         * 지도 검색용 키워드 (영어)
         */
        private String searchKeywordEn;
        
        /**
         * 주변 주차장 정보 (최대 3개)
         */
        private List<ParkingInfo> nearbyParkings;
        
        /**
         * 지역 구분 (domestic/international)
         */
        private String region;
        
        /**
         * 국가
         */
        private String country;
        
        /**
         * 도시
         */
        private String city;
        
        /**
         * 우편번호
         */
        private String postalCode;
    }
    
    /**
     * 주차장 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParkingInfo {
        
        /**
         * 주차장 Place ID
         */
        private String placeId;
        
        /**
         * 주차장명 (한국어)
         */
        private String nameKo;
        
        /**
         * 주차장명 (영어)
         */
        private String nameEn;
        
        /**
         * 주차장 주소
         */
        private String address;
        
        /**
         * 거리 (미터)
         */
        private Double distanceMeters;
        
        /**
         * 도보 소요시간 (분)
         */
        private Integer walkingMinutes;
        
        /**
         * 주차장 타입
         */
        private String parkingType;
        
        /**
         * 평점
         */
        private BigDecimal rating;
        
        /**
         * 위도
         */
        private BigDecimal latitude;
        
        /**
         * 경도
         */
        private BigDecimal longitude;
    }
    
    /**
     * 영업 시간 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BusinessHours {
        
        /**
         * 현재 영업 중 여부
         */
        private Boolean isOpen;
        
        /**
         * 현재 영업 상태
         */
        private String currentStatus;
        
        /**
         * 오늘 영업시간
         */
        private String todayHours;
        
        /**
         * 주간 영업시간
         */
        private List<DayHours> weeklyHours;
        
        /**
         * 다음 영업 시작 시간
         */
        private LocalDateTime nextOpenTime;
        
        /**
         * 다음 영업 종료 시간
         */
        private LocalDateTime nextCloseTime;
        
        /**
         * 특별 영업시간 (공휴일 등)
         */
        private List<SpecialHours> specialHours;
        
        /**
         * 요일별 영업시간
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class DayHours {
            
            /**
             * 요일
             */
            private String day;
            
            /**
             * 영업시간
             */
            private String hours;
            
            /**
             * 오늘 여부
             */
            private Boolean isToday;
            
            /**
             * 영업 중 여부
             */
            private Boolean isOpen;
        }
        
        /**
         * 특별 영업시간
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class SpecialHours {
            
            /**
             * 날짜
             */
            private String date;
            
            /**
             * 설명 (공휴일, 임시휴업 등)
             */
            private String description;
            
            /**
             * 영업시간
             */
            private String hours;
        }
    }
    
    /**
     * 연락처 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ContactInfo {
        
        /**
         * 전화번호
         */
        private String phone;
        
        /**
         * 국제 전화번호
         */
        private String internationalPhone;
        
        /**
         * 웹사이트
         */
        private String website;
        
        /**
         * 이메일
         */
        private String email;
        
        /**
         * 예약 URL
         */
        private String reservationUrl;
        
        /**
         * 소셜 미디어 링크
         */
        private List<SocialMedia> socialMedia;
        
        /**
         * 소셜 미디어 정보
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class SocialMedia {
            
            /**
             * 플랫폼 (facebook, instagram, twitter 등)
             */
            private String platform;
            
            /**
             * URL
             */
            private String url;
        }
    }
    
    /**
     * AI 추천 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AIRecommendation {
        
        /**
         * AI 추천 이유
         */
        private String recommendReason;
        
        /**
         * 추천 팁
         */
        private Tips tips;
        
        /**
         * 생성 시간
         */
        private LocalDateTime generatedAt;
        
        /**
         * 캐시에서 가져온 데이터 여부
         */
        private Boolean fromCache;
        
        /**
         * 추천 팁 상세 정보
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class Tips {
            
            /**
             * 장소 설명
             */
            private String description;
            
            /**
             * 특별 이벤트
             */
            private List<String> events;
            
            /**
             * 추천 방문 시간
             */
            private String bestVisitTime;
            
            /**
             * 예상 소요 시간
             */
            private String estimatedDuration;
            
            /**
             * 베스트 포토 스팟
             */
            private List<String> photoSpots;
            
            /**
             * 실용적 팁
             */
            private List<String> practicalTips;
            
            /**
             * 날씨별 준비사항
             */
            private String weatherTips;
            
            /**
             * 대체 장소
             */
            private List<AlternativePlace> alternativePlaces;
            
            /**
             * 대체 장소 정보
             */
            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            @Builder
            public static class AlternativePlace {
                
                /**
                 * 장소명
                 */
                private String name;
                
                /**
                 * 추천 이유
                 */
                private String reason;
                
                /**
                 * 거리
                 */
                private String distance;
                
                /**
                 * 장소 ID
                 */
                private String placeId;
            }
        }
    }
    
    /**
     * 리뷰 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Review {
        
        /**
         * 작성자 이름
         */
        private String authorName;
        
        /**
         * 평점
         */
        private Integer rating;
        
        /**
         * 리뷰 내용
         */
        private String text;
        
        /**
         * 작성 시간 (YYYYMMDD 형식)
         */
        private String time;
        
        /**
         * 상대 시간 표시
         */
        private String relativeTimeDescription;
        
        /**
         * 언어
         */
        private String language;
        
        /**
         * 작성자 프로필 사진
         */
        private String authorPhotoUrl;
        
        /**
         * 도움이 됨 수
         */
        private Integer helpfulCount;
        
        /**
         * 리뷰 사진
         */
        private List<String> photos;
    }
}