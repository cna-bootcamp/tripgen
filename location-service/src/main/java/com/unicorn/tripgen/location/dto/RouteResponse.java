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
 * 경로 정보 조회 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteResponse {
    
    /**
     * 최적 경로
     */
    private Route bestRoute;
    
    /**
     * 대안 경로 목록
     */
    private List<Route> alternativeRoutes;
    
    /**
     * 요청 정보
     */
    private RequestInfo request;
    
    /**
     * 데이터 소스
     */
    private String dataSource;
    
    /**
     * 응답 시간
     */
    private LocalDateTime responseTime;
    
    /**
     * 검색 실행 시간 (밀리초)
     */
    private Long executionTimeMs;
    
    /**
     * 경로 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Route {
        
        /**
         * 경로 ID
         */
        private String routeId;
        
        /**
         * 경로 요약
         */
        private String summary;
        
        /**
         * 총 거리 (미터)
         */
        private Integer totalDistance;
        
        /**
         * 총 소요 시간 (분)
         */
        private Integer totalDuration;
        
        /**
         * 교통 상황을 고려한 소요 시간 (분)
         */
        private Integer durationInTraffic;
        
        /**
         * 총 비용 (원)
         */
        private Integer totalCost;
        
        /**
         * 비용 구성
         */
        private CostBreakdown costBreakdown;
        
        /**
         * 출발 시간
         */
        private LocalDateTime departureTime;
        
        /**
         * 도착 시간
         */
        private LocalDateTime arrivalTime;
        
        /**
         * 이동 수단
         */
        private String transportMode;
        
        /**
         * 경로 상세 단계
         */
        private List<RouteStep> steps;
        
        /**
         * 경로 좌표 (폴리라인)
         */
        private String polyline;
        
        /**
         * 경로 바운딩 박스
         */
        private BoundingBox bounds;
        
        /**
         * 교통 정보
         */
        private TrafficInfo traffic;
        
        /**
         * 경로 평가
         */
        private RouteEvaluation evaluation;
        
        /**
         * 경고사항
         */
        private List<String> warnings;
        
        /**
         * 경로 구간별 정보
         */
        private List<RouteLeg> legs;
    }
    
    /**
     * 비용 구성
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CostBreakdown {
        
        /**
         * 교통비
         */
        private Integer transportFare;
        
        /**
         * 통행료
         */
        private Integer tolls;
        
        /**
         * 연료비
         */
        private Integer fuel;
        
        /**
         * 주차비
         */
        private Integer parking;
        
        /**
         * 기타 비용
         */
        private Integer others;
    }
    
    /**
     * 경로 단계
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteStep {
        
        /**
         * 단계 순서
         */
        private Integer stepNumber;
        
        /**
         * 이동 방법
         */
        private String travelMode;
        
        /**
         * 거리 (미터)
         */
        private Integer distance;
        
        /**
         * 소요 시간 (분)
         */
        private Integer duration;
        
        /**
         * 안내 문구
         */
        private String instruction;
        
        /**
         * 출발지 좌표
         */
        private LocationPoint startLocation;
        
        /**
         * 도착지 좌표
         */
        private LocationPoint endLocation;
        
        /**
         * 경로 좌표
         */
        private String polyline;
        
        /**
         * 교통수단 정보 (대중교통 이용 시)
         */
        private TransitDetails transit;
        
        /**
         * 방향 정보
         */
        private String maneuver;
        
        /**
         * 도로명/길 이름
         */
        private String roadName;
    }
    
    /**
     * 위치 좌표
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationPoint {
        
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
     * 대중교통 상세 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransitDetails {
        
        /**
         * 교통수단 종류 (버스, 지하철, 기차 등)
         */
        private String vehicleType;
        
        /**
         * 노선명/번호
         */
        private String line;
        
        /**
         * 출발 정류장/역
         */
        private String departureStop;
        
        /**
         * 도착 정류장/역
         */
        private String arrivalStop;
        
        /**
         * 정거장 수
         */
        private Integer numberOfStops;
        
        /**
         * 배차 간격 (분)
         */
        private Integer headway;
        
        /**
         * 요금
         */
        private Integer fare;
        
        /**
         * 운행 회사
         */
        private String agency;
        
        /**
         * 노선 색상
         */
        private String lineColor;
        
        /**
         * 차량 아이콘
         */
        private String vehicleIcon;
    }
    
    /**
     * 바운딩 박스
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BoundingBox {
        
        /**
         * 북동쪽 좌표
         */
        private LocationPoint northeast;
        
        /**
         * 남서쪽 좌표
         */
        private LocationPoint southwest;
    }
    
    /**
     * 교통 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrafficInfo {
        
        /**
         * 전체 교통 상황 (GOOD, MODERATE, HEAVY, SEVERE)
         */
        private String overallCondition;
        
        /**
         * 지연 시간 (분)
         */
        private Integer delayMinutes;
        
        /**
         * 혼잡 구간
         */
        private List<TrafficIncident> incidents;
        
        /**
         * 교통 상황 업데이트 시간
         */
        private LocalDateTime lastUpdated;
        
        /**
         * 교통 사고/공사 정보
         */
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class TrafficIncident {
            
            /**
             * 사건 유형 (ACCIDENT, CONSTRUCTION, ROAD_CLOSURE)
             */
            private String type;
            
            /**
             * 설명
             */
            private String description;
            
            /**
             * 위치
             */
            private LocationPoint location;
            
            /**
             * 영향도 (LOW, MEDIUM, HIGH)
             */
            private String severity;
            
            /**
             * 예상 지연 시간 (분)
             */
            private Integer expectedDelay;
        }
    }
    
    /**
     * 경로 평가
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteEvaluation {
        
        /**
         * 전체 평가 점수 (1-10)
         */
        private Integer overallScore;
        
        /**
         * 시간 효율성 점수
         */
        private Integer timeEfficiency;
        
        /**
         * 비용 효율성 점수
         */
        private Integer costEfficiency;
        
        /**
         * 편의성 점수
         */
        private Integer convenience;
        
        /**
         * 안전성 점수
         */
        private Integer safety;
        
        /**
         * 추천 이유
         */
        private List<String> reasons;
        
        /**
         * 주의사항
         */
        private List<String> cautions;
    }
    
    /**
     * 경로 구간 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RouteLeg {
        
        /**
         * 구간 번호
         */
        private Integer legNumber;
        
        /**
         * 출발지
         */
        private LocationInfo startLocation;
        
        /**
         * 도착지
         */
        private LocationInfo endLocation;
        
        /**
         * 구간 거리 (미터)
         */
        private Integer distance;
        
        /**
         * 구간 소요 시간 (분)
         */
        private Integer duration;
        
        /**
         * 구간별 단계
         */
        private List<RouteStep> steps;
        
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
             * 장소명
             */
            private String name;
            
            /**
             * 주소
             */
            private String address;
        }
    }
    
    /**
     * 요청 정보
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RequestInfo {
        
        /**
         * 출발지
         */
        private LocationPoint origin;
        
        /**
         * 목적지
         */
        private LocationPoint destination;
        
        /**
         * 이동수단
         */
        private String transportMode;
        
        /**
         * 출발 시간
         */
        private LocalDateTime departureTime;
        
        /**
         * 최적화 기준
         */
        private String optimize;
        
        /**
         * 피해야 할 요소들
         */
        private List<String> avoid;
    }
}