package com.unicorn.tripgen.location.repository;

import com.unicorn.tripgen.location.entity.Route;
import com.unicorn.tripgen.location.entity.TransportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 경로 정보 저장소 인터페이스
 */
@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {
    
    /**
     * 출발지와 목적지 좌표로 경로 조회
     * 
     * @param originLat 출발지 위도
     * @param originLon 출발지 경도
     * @param destLat 목적지 위도
     * @param destLon 목적지 경도
     * @param transportType 교통 수단
     * @return 경로 목록
     */
    List<Route> findByOriginIdAndDestinationIdAndTransportType(
        String originId, String destinationId, TransportType transportType
    );
    
    /**
     * 외부 경로 ID로 조회
     * 
     * @param externalRouteId 외부 API 경로 ID
     * @return 경로 정보
     */
    Optional<Route> findByRouteId(String routeId);
    
    /**
     * 외부 경로 ID와 소스로 조회
     * 
     * @param externalRouteId 외부 API 경로 ID
     * @param externalSource 외부 API 소스
     * @return 경로 정보
     */
    Optional<Route> findByRouteIdAndTransportType(String routeId, TransportType transportType);
    
    /**
     * 특정 교통 수단의 활성 경로들 조회
     * 
     * @param transportType 교통 수단
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findByTransportType(TransportType transportType, Pageable pageable);
    
    /**
     * 특정 거리 범위 내의 경로들 조회
     * 
     * @param minDistance 최소 거리 (미터)
     * @param maxDistance 최대 거리 (미터)
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findByDistanceBetween(
        Integer minDistance, Integer maxDistance, Pageable pageable
    );
    
    /**
     * 특정 소요 시간 범위 내의 경로들 조회
     * 
     * @param minDuration 최소 소요 시간 (분)
     * @param maxDuration 최대 소요 시간 (분)
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findByDurationBetween(
        Integer minDuration, Integer maxDuration, Pageable pageable
    );
    
    /**
     * 최단 거리 경로들 조회
     * 
     * @param transportType 교통 수단
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findByTransportTypeOrderByDistanceAsc(
        TransportType transportType, Pageable pageable
    );
    
    /**
     * 최단 시간 경로들 조회
     * 
     * @param transportType 교통 수단
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findByTransportTypeOrderByDurationAsc(
        TransportType transportType, Pageable pageable
    );
    
    /**
     * 특정 좌표 주변의 출발지를 가진 경로들 조회
     * 
     * @param latitude 중심 위도
     * @param longitude 중심 경도
     * @param tolerance 허용 오차 (도 단위)
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    @Query("SELECT r FROM Route r WHERE r.originId = :originId")
    Page<Route> findRoutesByOriginId(
        @Param("originId") String originId,
        Pageable pageable
    );
    
    /**
     * 특정 좌표 주변의 목적지를 가진 경로들 조회
     * 
     * @param latitude 중심 위도
     * @param longitude 중심 경도
     * @param tolerance 허용 오차 (도 단위)
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    @Query("SELECT r FROM Route r WHERE r.destinationId = :destinationId")
    Page<Route> findRoutesByDestinationId(
        @Param("destinationId") String destinationId,
        Pageable pageable
    );
    
    /**
     * 특정 비용 이하의 경로들 조회
     * 
     * @param maxCost 최대 비용
     * @param transportType 교통 수단
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findByPriceLessThanEqualAndTransportType(
        BigDecimal maxPrice, TransportType transportType, Pageable pageable
    );
    
    /**
     * 외부 소스별 경로 수 조회
     * 
     * @param externalSource 외부 API 소스
     * @return 경로 수
     */
    long countByTransportType(TransportType transportType);
    
    /**
     * 교통 수단별 경로 수 조회
     * 
     * @param transportType 교통 수단
     * @return 경로 수
     */
    long countByOriginId(String originId);
    
    /**
     * 평균 거리 조회 (교통 수단별)
     * 
     * @param transportType 교통 수단
     * @return 평균 거리 (미터)
     */
    @Query("SELECT AVG(r.distance) FROM Route r WHERE r.transportType = :transportType")
    Double getAverageDistanceByTransportType(@Param("transportType") TransportType transportType);
    
    /**
     * 평균 소요 시간 조회 (교통 수단별)
     * 
     * @param transportType 교통 수단
     * @return 평균 소요 시간 (분)
     */
    @Query("SELECT AVG(r.duration) FROM Route r WHERE r.transportType = :transportType")
    Double getAverageDurationByTransportType(@Param("transportType") TransportType transportType);
    
    /**
     * 최근 생성된 경로들 조회
     * 
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * 특정 기간 내 생성된 경로들 조회
     * 
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findByCreatedAtBetween(
        LocalDateTime startDate, LocalDateTime endDate, Pageable pageable
    );
    
    /**
     * 중복 경로 찾기 (같은 출발지, 목적지, 교통수단)
     * 
     * @param originLat 출발지 위도
     * @param originLon 출발지 경도
     * @param destLat 목적지 위도
     * @param destLon 목적지 경도
     * @param transportType 교통 수단
     * @param tolerance 허용 오차
     * @return 경로 목록
     */
    @Query("SELECT r FROM Route r WHERE r.transportType = :transportType AND " +
           "r.originId = :originId AND r.destinationId = :destinationId")
    List<Route> findDuplicateRoutes(
        @Param("originId") String originId,
        @Param("destinationId") String destinationId,
        @Param("transportType") TransportType transportType
    );
    
    /**
     * 효율성이 좋은 경로들 조회 (거리 대비 시간이 좋은 경로)
     * 
     * @param transportType 교통 수단
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    @Query("SELECT r FROM Route r WHERE r.transportType = :transportType " +
           "ORDER BY (CAST(r.distance AS double) / CAST(r.duration AS double)) DESC")
    Page<Route> findEfficientRoutes(@Param("transportType") TransportType transportType, Pageable pageable);
    
    /**
     * 유사한 경로 찾기 (출발지와 목적지가 비슷한 경로)
     * 
     * @param originId 출발지 ID
     * @param destinationId 목적지 ID
     * @param transportType 교통 수단
     * @return 경로 정보
     */
    @Query("SELECT r FROM Route r WHERE r.originId = :originId AND " +
           "r.destinationId = :destinationId AND r.transportType = :transportType " +
           "ORDER BY r.createdAt DESC")
    Optional<Route> findByOriginDestinationAndType(
        @Param("originId") String originId,
        @Param("destinationId") String destinationId,
        @Param("transportType") TransportType transportType
    );
}