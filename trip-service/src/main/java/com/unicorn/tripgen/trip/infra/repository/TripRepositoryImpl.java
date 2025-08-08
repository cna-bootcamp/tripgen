package com.unicorn.tripgen.trip.infra.repository;

import com.unicorn.tripgen.trip.biz.domain.Trip;
import com.unicorn.tripgen.trip.biz.domain.TripStatus;
import com.unicorn.tripgen.trip.biz.usecase.out.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Trip Repository 구현체
 * Clean Architecture의 Infrastructure Layer
 * Domain Layer의 TripRepository 인터페이스를 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TripRepositoryImpl implements TripRepository {
    
    private final TripJpaRepository tripJpaRepository;
    
    @Override
    public Trip save(Trip trip) {
        log.debug("Saving trip: {}", trip.getTripId());
        return tripJpaRepository.save(trip);
    }
    
    @Override
    public Optional<Trip> findById(String tripId) {
        log.debug("Finding trip by id: {}", tripId);
        return tripJpaRepository.findById(tripId);
    }
    
    @Override
    public Optional<Trip> findByIdAndUserId(String tripId, String userId) {
        log.debug("Finding trip by id: {} and userId: {}", tripId, userId);
        return tripJpaRepository.findByTripIdAndUserId(tripId, userId);
    }
    
    @Override
    public List<Trip> findByUserId(String userId, TripStatus status, String search, String sort, int page, int size) {
        log.debug("Finding trips for user: {}, status: {}, search: {}, sort: {}, page: {}, size: {}", 
                 userId, status, search, sort, page, size);
        
        // 기본값 설정
        String sortCriteria = (sort == null || sort.isEmpty()) ? "latest" : sort;
        
        // Pageable 생성 (Spring Data는 0-based indexing)
        Pageable pageable = PageRequest.of(page - 1, size);
        
        Page<Trip> tripPage = tripJpaRepository.findByUserIdWithFilters(userId, status, search, sortCriteria, pageable);
        return tripPage.getContent();
    }
    
    @Override
    public int countByUserId(String userId, TripStatus status, String search) {
        log.debug("Counting trips for user: {}, status: {}, search: {}", userId, status, search);
        
        long count = tripJpaRepository.countByUserIdWithFilters(userId, status, search);
        return (int) count;
    }
    
    @Override
    public void delete(Trip trip) {
        log.debug("Deleting trip: {}", trip.getTripId());
        tripJpaRepository.delete(trip);
    }
    
    @Override
    public boolean existsById(String tripId) {
        log.debug("Checking if trip exists: {}", tripId);
        return tripJpaRepository.existsById(tripId);
    }
    
    @Override
    public boolean isOwner(String tripId, String userId) {
        log.debug("Checking if user {} owns trip: {}", userId, tripId);
        return tripJpaRepository.existsByTripIdAndUserId(tripId, userId);
    }
    
    @Override
    public String generateTripId() {
        String tripId = UUID.randomUUID().toString();
        log.debug("Generated trip id: {}", tripId);
        return tripId;
    }
}