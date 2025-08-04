package com.unicorn.tripgen.location.repository;

import com.unicorn.tripgen.location.entity.Weather;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 날씨 정보 저장소 인터페이스
 */
@Repository
public interface WeatherRepository extends JpaRepository<Weather, Long> {
    
    /**
     * 특정 좌표와 날짜의 날씨 정보 조회
     * 
     * @param latitude 위도
     * @param longitude 경도
     * @param weatherDate 날씨 날짜
     * @return 날씨 정보
     */
    Optional<Weather> findByLatitudeAndLongitudeAndWeatherDateAndActiveTrue(
        BigDecimal latitude, BigDecimal longitude, LocalDate weatherDate
    );
    
    /**
     * 특정 좌표 주변의 날씨 정보 조회 (당일)
     * 
     * @param latitude 중심 위도
     * @param longitude 중심 경도
     * @param tolerance 허용 오차 (도 단위)
     * @param weatherDate 날씨 날짜
     * @return 날씨 목록
     */
    @Query("SELECT w FROM Weather w WHERE w.active = true AND w.weatherDate = :weatherDate AND " +
           "ABS(w.latitude - :latitude) < :tolerance AND " +
           "ABS(w.longitude - :longitude) < :tolerance " +
           "ORDER BY ABS(w.latitude - :latitude) + ABS(w.longitude - :longitude)")
    List<Weather> findWeatherNearLocation(
        @Param("latitude") BigDecimal latitude,
        @Param("longitude") BigDecimal longitude,
        @Param("tolerance") BigDecimal tolerance,
        @Param("weatherDate") LocalDate weatherDate
    );
    
    /**
     * 특정 지역명의 날씨 정보 조회
     * 
     * @param locationName 지역명
     * @param weatherDate 날씨 날짜
     * @return 날씨 정보
     */
    Optional<Weather> findByLocationNameAndWeatherDateAndActiveTrue(String locationName, LocalDate weatherDate);
    
    /**
     * 특정 좌표의 날씨 이력 조회 (날짜 범위)
     * 
     * @param latitude 위도
     * @param longitude 경도
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param pageable 페이징 정보
     * @return 날씨 목록
     */
    Page<Weather> findByLatitudeAndLongitudeAndWeatherDateBetweenAndActiveTrueOrderByWeatherDateDesc(
        BigDecimal latitude, BigDecimal longitude, LocalDate startDate, LocalDate endDate, Pageable pageable
    );
    
    /**
     * 특정 날짜의 모든 활성 날씨 정보 조회
     * 
     * @param weatherDate 날씨 날짜
     * @param pageable 페이징 정보
     * @return 날씨 목록
     */
    Page<Weather> findByWeatherDateAndActiveTrue(LocalDate weatherDate, Pageable pageable);
    
    /**
     * 특정 기온 범위의 날씨 정보 조회
     * 
     * @param minTemp 최소 기온
     * @param maxTemp 최대 기온
     * @param weatherDate 날씨 날짜
     * @param pageable 페이징 정보
     * @return 날씨 목록
     */
    Page<Weather> findByTemperatureBetweenAndWeatherDateAndActiveTrue(
        BigDecimal minTemp, BigDecimal maxTemp, LocalDate weatherDate, Pageable pageable
    );
    
    /**
     * 특정 날씨 상태의 정보 조회
     * 
     * @param weatherCondition 날씨 상태
     * @param weatherDate 날씨 날짜
     * @param pageable 페이징 정보
     * @return 날씨 목록
     */
    Page<Weather> findByWeatherConditionAndWeatherDateAndActiveTrue(
        String weatherCondition, LocalDate weatherDate, Pageable pageable
    );
    
    /**
     * 강수 확률이 특정 값 이상인 날씨 정보 조회
     * 
     * @param minPrecipitationProbability 최소 강수 확률
     * @param weatherDate 날씨 날짜
     * @param pageable 페이징 정보
     * @return 날씨 목록
     */
    Page<Weather> findByPrecipitationProbabilityGreaterThanEqualAndWeatherDateAndActiveTrue(
        Integer minPrecipitationProbability, LocalDate weatherDate, Pageable pageable
    );
    
    /**
     * 자외선 지수가 특정 값 이상인 날씨 정보 조회
     * 
     * @param minUvIndex 최소 자외선 지수
     * @param weatherDate 날씨 날짜
     * @param pageable 페이징 정보
     * @return 날씨 목록
     */
    Page<Weather> findByUvIndexGreaterThanEqualAndWeatherDateAndActiveTrue(
        BigDecimal minUvIndex, LocalDate weatherDate, Pageable pageable
    );
    
    /**
     * 외부 소스별 날씨 데이터 수 조회
     * 
     * @param externalSource 외부 API 소스
     * @return 날씨 데이터 수
     */
    long countByExternalSourceAndActiveTrue(String externalSource);
    
    /**
     * 특정 날짜의 평균 기온 조회
     * 
     * @param weatherDate 날씨 날짜
     * @return 평균 기온
     */
    @Query("SELECT AVG(w.temperature) FROM Weather w WHERE w.weatherDate = :weatherDate AND w.active = true")
    Double getAverageTemperatureByDate(@Param("weatherDate") LocalDate weatherDate);
    
    /**
     * 특정 지역의 평균 기온 조회 (기간별)
     * 
     * @param locationName 지역명
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 평균 기온
     */
    @Query("SELECT AVG(w.temperature) FROM Weather w WHERE w.locationName = :locationName AND " +
           "w.weatherDate BETWEEN :startDate AND :endDate AND w.active = true")
    Double getAverageTemperatureByLocationAndPeriod(
        @Param("locationName") String locationName,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * 특정 날짜의 최고/최저 기온 조회
     * 
     * @param weatherDate 날씨 날짜
     * @return 최고 기온
     */
    @Query("SELECT MAX(w.tempMax) FROM Weather w WHERE w.weatherDate = :weatherDate AND w.active = true")
    Optional<BigDecimal> getMaxTemperatureByDate(@Param("weatherDate") LocalDate weatherDate);
    
    @Query("SELECT MIN(w.tempMin) FROM Weather w WHERE w.weatherDate = :weatherDate AND w.active = true")
    Optional<BigDecimal> getMinTemperatureByDate(@Param("weatherDate") LocalDate weatherDate);
    
    /**
     * 여행하기 좋은 날씨 조회 (기온, 강수확률, 날씨상태 종합 고려)
     * 
     * @param weatherDate 날씨 날짜
     * @param maxPrecipitationProbability 최대 강수 확률
     * @param minTemp 최소 기온
     * @param maxTemp 최대 기온
     * @param pageable 페이징 정보
     * @return 날씨 목록
     */
    @Query("SELECT w FROM Weather w WHERE w.active = true AND w.weatherDate = :weatherDate AND " +
           "w.precipitationProbability <= :maxPrecipitationProbability AND " +
           "w.temperature BETWEEN :minTemp AND :maxTemp AND " +
           "w.weatherCondition NOT IN ('stormy', 'snow', 'heavy_rain') " +
           "ORDER BY w.precipitationProbability ASC, w.temperature DESC")
    Page<Weather> findGoodTravelWeather(
        @Param("weatherDate") LocalDate weatherDate,
        @Param("maxPrecipitationProbability") Integer maxPrecipitationProbability,
        @Param("minTemp") BigDecimal minTemp,
        @Param("maxTemp") BigDecimal maxTemp,
        Pageable pageable
    );
    
    /**
     * 최근 업데이트된 날씨 정보 조회
     * 
     * @param pageable 페이징 정보
     * @return 날씨 목록
     */
    Page<Weather> findByActiveTrueOrderByUpdatedAtDesc(Pageable pageable);
    
    /**
     * 특정 기간 내 생성된 날씨 정보 조회
     * 
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @param pageable 페이징 정보
     * @return 날씨 목록
     */
    Page<Weather> findByCreatedAtBetweenAndActiveTrue(
        LocalDateTime startDate, LocalDateTime endDate, Pageable pageable
    );
    
    /**
     * 오래된 날씨 데이터 조회 (정리용)
     * 
     * @param cutoffDate 기준 날짜 (이전 데이터)
     * @param pageable 페이징 정보
     * @return 날씨 목록
     */
    Page<Weather> findByWeatherDateBeforeAndActiveTrue(LocalDate cutoffDate, Pageable pageable);
    
    /**
     * 중복 날씨 데이터 찾기 (같은 좌표, 같은 날짜)
     * 
     * @param latitude 위도
     * @param longitude 경도
     * @param weatherDate 날씨 날짜
     * @param tolerance 허용 오차
     * @return 날씨 목록
     */
    @Query("SELECT w FROM Weather w WHERE w.active = true AND w.weatherDate = :weatherDate AND " +
           "ABS(w.latitude - :latitude) < :tolerance AND " +
           "ABS(w.longitude - :longitude) < :tolerance")
    List<Weather> findDuplicateWeatherData(
        @Param("latitude") BigDecimal latitude,
        @Param("longitude") BigDecimal longitude,
        @Param("weatherDate") LocalDate weatherDate,
        @Param("tolerance") BigDecimal tolerance
    );
}