package com.unicorn.tripgen.trip.infra.repository;

import com.unicorn.tripgen.trip.biz.domain.Trip;
import com.unicorn.tripgen.trip.biz.domain.TripStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Trip JPA Repository 인터페이스
 * Spring Data JPA를 사용한 데이터 접근 계층
 */
@Repository
public interface TripJpaRepository extends JpaRepository<Trip, String> {
    
    /**
     * 사용자 ID와 여행 ID로 조회
     */
    Optional<Trip> findByTripIdAndUserId(String tripId, String userId);
    
    /**
     * 사용자의 여행 목록 조회 (상태별, 검색어 포함)
     */
    @Query("SELECT t FROM Trip t WHERE t.userId = :userId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:search IS NULL OR t.tripName LIKE %:search%) " +
           "ORDER BY " +
           "CASE WHEN :sort = 'latest' THEN t.createdAt END DESC, " +
           "CASE WHEN :sort = 'oldest' THEN t.createdAt END ASC, " +
           "CASE WHEN :sort = 'name' THEN t.tripName END ASC")
    Page<Trip> findByUserIdWithFilters(@Param("userId") String userId,
                                      @Param("status") TripStatus status,
                                      @Param("search") String search,
                                      @Param("sort") String sort,
                                      Pageable pageable);
    
    /**
     * 사용자의 여행 개수 조회 (상태별, 검색어 포함)
     */
    @Query("SELECT COUNT(t) FROM Trip t WHERE t.userId = :userId " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:search IS NULL OR t.tripName LIKE %:search%)")
    long countByUserIdWithFilters(@Param("userId") String userId,
                                 @Param("status") TripStatus status,
                                 @Param("search") String search);
    
    /**
     * 여행 소유자 확인
     */
    boolean existsByTripIdAndUserId(String tripId, String userId);
    
    /**
     * 사용자의 모든 여행 조회
     */
    List<Trip> findByUserId(String userId);
}