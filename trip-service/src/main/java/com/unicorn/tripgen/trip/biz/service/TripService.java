package com.unicorn.tripgen.trip.biz.service;

import com.unicorn.tripgen.trip.biz.domain.Trip;
import com.unicorn.tripgen.trip.biz.domain.TripStatus;
import com.unicorn.tripgen.trip.biz.usecase.in.TripUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService implements TripUseCase {

    @Override
    public TripListResult getTripList(GetTripListQuery query) {
        log.info("Getting trip list for user: {}", query.userId());
        
        // TODO: 실제 구현
        return new TripListResult(
            new ArrayList<>(),
            0,
            query.page(),
            0
        );
    }

    @Override
    public Optional<Trip> getTripDetail(String tripId, String userId) {
        log.info("Getting trip detail - tripId: {}, userId: {}", tripId, userId);
        
        // TODO: 실제 구현 - Repository에서 조회
        return Optional.empty();
    }

    @Override
    @Transactional
    public Trip createTrip(CreateTripCommand command) {
        log.info("Creating trip - userId: {}, tripName: {}", command.userId(), command.tripName());
        
        String tripId = UUID.randomUUID().toString();
        Trip trip = Trip.create(tripId, command.tripName(), command.userId(), command.transportMode());
        
        // TODO: Repository에 저장
        
        return trip;
    }

    @Override
    @Transactional
    public Trip updateTrip(UpdateTripCommand command) {
        log.info("Updating trip - tripId: {}, userId: {}", command.tripId(), command.userId());
        
        // TODO: 실제 구현 - Repository에서 조회 후 업데이트
        Trip trip = Trip.create(command.tripId(), command.tripName(), command.userId(), command.transportMode());
        trip.updateBasicInfo(command.tripName(), command.transportMode());
        
        return trip;
    }

    @Override
    @Transactional
    public void deleteTrip(DeleteTripCommand command) {
        log.info("Deleting trip - tripId: {}, userId: {}", command.tripId(), command.userId());
        
        // TODO: 실제 구현 - 권한 확인 후 삭제
    }

    @Override
    @Transactional
    public Trip changeTripStatus(String tripId, String userId, TripStatus newStatus) {
        log.info("Changing trip status - tripId: {}, userId: {}, newStatus: {}", tripId, userId, newStatus);
        
        // TODO: 실제 구현 - Repository에서 조회 후 상태 변경
        Trip trip = Trip.create(tripId, "Sample Trip", userId, null);
        trip.changeStatus(newStatus);
        
        return trip;
    }

    @Override
    public boolean isOwner(String tripId, String userId) {
        log.info("Checking ownership - tripId: {}, userId: {}", tripId, userId);
        
        // TODO: 실제 구현 - Repository에서 조회 후 확인
        return true;
    }
}