package com.unicorn.tripgen.trip.biz.usecase.out;

import com.unicorn.tripgen.trip.biz.domain.Destination;

import java.util.List;
import java.util.Optional;

/**
 * 여행지 Repository 인터페이스 (Output Port)
 */
public interface DestinationRepository {
    
    /**
     * 여행지 저장
     */
    Destination save(Destination destination);
    
    /**
     * 여행지 목록 저장
     */
    List<Destination> saveAll(List<Destination> destinations);
    
    /**
     * 여행지 ID로 조회
     */
    Optional<Destination> findById(String destinationId);
    
    /**
     * 여행의 모든 여행지 조회 (순서대로)
     */
    List<Destination> findByTripIdOrderByOrder(String tripId);
    
    /**
     * 여행의 여행지 개수 조회
     */
    int countByTripId(String tripId);
    
    /**
     * 여행지 삭제
     */
    void delete(Destination destination);
    
    /**
     * 여행의 모든 여행지 삭제
     */
    void deleteByTripId(String tripId);
    
    /**
     * 여행지 존재 여부 확인
     */
    boolean existsById(String destinationId);
    
    /**
     * 여행지 ID 생성
     */
    String generateDestinationId();
}