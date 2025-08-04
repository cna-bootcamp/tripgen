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
    List<Route> findByOriginLatitudeAndOriginLongitudeAndDestLatitudeAndDestLongitudeAndTransportTypeAndActiveTrue(
        BigDecimal originLat, BigDecimal originLon, 
        BigDecimal destLat, BigDecimal destLon, 
        TransportType transportType
    );
    
    /**
     * 외부 경로 ID로 조회
     * 
     * @param externalRouteId 외부 API 경로 ID
     * @return 경로 정보
     */
    Optional<Route> findByExternalRouteIdAndActiveTrue(String externalRouteId);
    
    /**
     * 외부 경로 ID와 소스로 조회
     * 
     * @param externalRouteId 외부 API 경로 ID
     * @param externalSource 외부 API 소스
     * @return 경로 정보
     */
    Optional<Route> findByExternalRouteIdAndExternalSourceAndActiveTrue(String externalRouteId, String externalSource);
    
    /**
     * 특정 교통 수단의 활성 경로들 조회
     * 
     * @param transportType 교통 수단
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findByTransportTypeAndActiveTrue(TransportType transportType, Pageable pageable);
    
    /**
     * 특정 거리 범위 내의 경로들 조회
     * 
     * @param minDistance 최소 거리 (미터)
     * @param maxDistance 최대 거리 (미터)
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findByDistanceMetersBetweenAndActiveTrue(
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
    Page<Route> findByDurationMinutesBetweenAndActiveTrue(
        Integer minDuration, Integer maxDuration, Pageable pageable
    );
    
    /**
     * 최단 거리 경로들 조회
     * 
     * @param transportType 교통 수단
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findByTransportTypeAndActiveTrueOrderByDistanceMetersAsc(
        TransportType transportType, Pageable pageable
    );
    
    /**
     * 최단 시간 경로들 조회
     * 
     * @param transportType 교통 수단
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findByTransportTypeAndActiveTrueOrderByDurationMinutesAsc(
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
    @Query("SELECT r FROM Route r WHERE r.active = true AND " +
           "ABS(r.originLatitude - :latitude) < :tolerance AND " +
           "ABS(r.originLongitude - :longitude) < :tolerance")
    Page<Route> findRoutesByOriginNear(
        @Param("latitude") BigDecimal latitude, 
        @Param("longitude") BigDecimal longitude, 
        @Param("tolerance") BigDecimal tolerance,
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
    @Query("SELECT r FROM Route r WHERE r.active = true AND " +
           "ABS(r.destLatitude - :latitude) < :tolerance AND " +
           "ABS(r.destLongitude - :longitude) < :tolerance")
    Page<Route> findRoutesByDestinationNear(
        @Param("latitude") BigDecimal latitude, 
        @Param("longitude") BigDecimal longitude, 
        @Param("tolerance") BigDecimal tolerance,
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
    Page<Route> findByCostLessThanEqualAndTransportTypeAndActiveTrue(
        Integer maxCost, TransportType transportType, Pageable pageable
    );
    
    /**
     * 외부 소스별 경로 수 조회
     * 
     * @param externalSource 외부 API 소스
     * @return 경로 수
     */
    long countByExternalSourceAndActiveTrue(String externalSource);
    
    /**
     * 교통 수단별 경로 수 조회
     * 
     * @param transportType 교통 수단
     * @return 경로 수
     */
    long countByTransportTypeAndActiveTrue(TransportType transportType);
    
    /**
     * 평균 거리 조회 (교통 수단별)
     * 
     * @param transportType 교통 수단
     * @return 평균 거리 (미터)
     */
    @Query("SELECT AVG(r.distanceMeters) FROM Route r WHERE r.transportType = :transportType AND r.active = true")
    Double getAverageDistanceByTransportType(@Param("transportType") TransportType transportType);
    
    /**
     * 평균 소요 시간 조회 (교통 수단별)
     * 
     * @param transportType 교통 수단
     * @return 평균 소요 시간 (분)
     */
    @Query("SELECT AVG(r.durationMinutes) FROM Route r WHERE r.transportType = :transportType AND r.active = true")
    Double getAverageDurationByTransportType(@Param("transportType") TransportType transportType);
    
    /**
     * 최근 생성된 경로들 조회
     * 
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * 특정 기간 내 생성된 경로들 조회
     * 
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    Page<Route> findByCreatedAtBetweenAndActiveTrue(
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
    @Query("SELECT r FROM Route r WHERE r.active = true AND r.transportType = :transportType AND " +
           "ABS(r.originLatitude - :originLat) < :tolerance AND " +
           "ABS(r.originLongitude - :originLon) < :tolerance AND " +
           "ABS(r.destLatitude - :destLat) < :tolerance AND " +
           "ABS(r.destLongitude - :destLon) < :tolerance")
    List<Route> findDuplicateRoutes(
        @Param("originLat") BigDecimal originLat,
        @Param("originLon") BigDecimal originLon,
        @Param("destLat") BigDecimal destLat,
        @Param("destLon") BigDecimal destLon,
        @Param("transportType") TransportType transportType,
        @Param("tolerance") BigDecimal tolerance
    );
    
    /**
     * 효율성이 좋은 경로들 조회 (거리 대비 시간이 좋은 경로)
     * 
     * @param transportType 교통 수단
     * @param pageable 페이징 정보
     * @return 경로 목록
     */
    @Query("SELECT r FROM Route r WHERE r.active = true AND r.transportType = :transportType " +
           "ORDER BY (CAST(r.distanceMeters AS double) / CAST(r.durationMinutes AS double)) DESC")
    Page<Route> findEfficientRoutes(@Param("transportType") TransportType transportType, Pageable pageable);
}