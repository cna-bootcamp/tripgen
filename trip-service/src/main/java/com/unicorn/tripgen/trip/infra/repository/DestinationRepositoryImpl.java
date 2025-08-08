package com.unicorn.tripgen.trip.infra.repository;

import com.unicorn.tripgen.trip.biz.domain.Destination;
import com.unicorn.tripgen.trip.biz.usecase.out.DestinationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Destination Repository 구현체
 * Clean Architecture의 Infrastructure Layer
 * Domain Layer의 DestinationRepository 인터페이스를 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DestinationRepositoryImpl implements DestinationRepository {
    
    private final DestinationJpaRepository destinationJpaRepository;
    
    @Override
    public Destination save(Destination destination) {
        log.debug("Saving destination: {} for trip: {}", destination.getDestinationId(), destination.getTripId());
        return destinationJpaRepository.save(destination);
    }
    
    @Override
    public List<Destination> saveAll(List<Destination> destinations) {
        log.debug("Saving {} destinations", destinations.size());
        return destinationJpaRepository.saveAll(destinations);
    }
    
    @Override
    public Optional<Destination> findById(String destinationId) {
        log.debug("Finding destination by id: {}", destinationId);
        return destinationJpaRepository.findById(destinationId);
    }
    
    @Override
    public List<Destination> findByTripId(String tripId) {
        log.debug("Finding destinations for trip: {}", tripId);
        return destinationJpaRepository.findByTripIdOrderByOrderAsc(tripId);
    }
    
    @Override
    public int countByTripId(String tripId) {
        log.debug("Counting destinations for trip: {}", tripId);
        return destinationJpaRepository.countByTripId(tripId);
    }
    
    @Override
    public void delete(Destination destination) {
        log.debug("Deleting destination: {}", destination.getDestinationId());
        destinationJpaRepository.delete(destination);
    }
    
    @Override
    public void deleteByTripId(String tripId) {
        log.debug("Deleting all destinations for trip: {}", tripId);
        destinationJpaRepository.deleteByTripId(tripId);
    }
    
    @Override
    public boolean existsById(String destinationId) {
        log.debug("Checking if destination exists: {}", destinationId);
        return destinationJpaRepository.existsById(destinationId);
    }
    
    @Override
    public String generateDestinationId() {
        String destinationId = UUID.randomUUID().toString();
        log.debug("Generated destination id: {}", destinationId);
        return destinationId;
    }
    
    @Override
    public int getNextOrder(String tripId) {
        Integer maxOrder = destinationJpaRepository.findMaxOrderByTripId(tripId);
        return (maxOrder == null) ? 1 : maxOrder + 1;
    }
}