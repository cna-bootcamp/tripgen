package com.unicorn.tripgen.trip.biz.usecase.out;

import com.unicorn.tripgen.trip.biz.domain.Trip;
import com.unicorn.tripgen.trip.biz.domain.TripStatus;

import java.util.List;
import java.util.Optional;

/**
 * 여행 Repository 인터페이스 (Output Port)
 * Clean Architecture의 Use Case Layer에서 Infrastructure Layer로의 의존성을 역전시킴
 */
public interface TripRepository {
    
    /**
     * 여행 저장
     */
    Trip save(Trip trip);
    
    /**
     * 여행 ID로 조회
     */
    Optional<Trip> findById(String tripId);
    
    /**
     * 여행 ID와 사용자 ID로 조회
     */
    Optional<Trip> findByIdAndUserId(String tripId, String userId);
    
    /**
     * 사용자의 여행 목록 조회 (페이징)
     */
    List<Trip> findByUserId(String userId, TripStatus status, String search, String sort, int page, int size);
    
    /**
     * 사용자의 총 여행 개수
     */
    int countByUserId(String userId, TripStatus status, String search);
    
    /**
     * 여행 삭제
     */
    void delete(Trip trip);
    
    /**
     * 여행 존재 여부 확인
     */
    boolean existsById(String tripId);
    
    /**
     * 여행 소유자 확인
     */
    boolean isOwner(String tripId, String userId);
    
    /**
     * 여행 ID 생성
     */
    String generateTripId();
}