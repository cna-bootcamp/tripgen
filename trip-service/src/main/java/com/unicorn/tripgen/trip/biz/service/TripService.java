package com.unicorn.tripgen.trip.biz.service;

import com.unicorn.tripgen.trip.biz.domain.Trip;
import com.unicorn.tripgen.trip.biz.domain.TripStatus;
import com.unicorn.tripgen.trip.biz.exception.TripNotFoundException;
import com.unicorn.tripgen.trip.biz.usecase.in.TripUseCase;
import com.unicorn.tripgen.trip.biz.usecase.out.TripRepository;
import com.unicorn.tripgen.trip.biz.dto.TripDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService implements TripUseCase {
    
    private final TripRepository tripRepository;

    @Override
    public TripListResult getTripList(GetTripListQuery query) {
        log.info("Getting trip list for user: {}", query.userId());
        
        // Repository에서 여행 목록 조회
        List<Trip> trips = tripRepository.findByUserId(
            query.userId(), 
            query.status(), 
            query.search(), 
            query.sort(), 
            query.page(), 
            query.size()
        );
        
        // 총 개수 조회
        int totalCount = tripRepository.countByUserId(query.userId(), query.status(), query.search());
        int totalPages = (int) Math.ceil((double) totalCount / query.size());
        
        // Trip 엔티티를 TripSummary로 변환
        List<TripSummary> tripSummaries = trips.stream()
            .map(trip -> new TripSummary(
                trip.getTripId(),
                trip.getTripName(),
                trip.getStatus(),
                trip.getCurrentStep(),
                trip.getStartDate() != null ? trip.getStartDate().toString() : null,
                trip.getEndDate() != null ? trip.getEndDate().toString() : null,
                trip.getMembers().size(),
                trip.getDestinations().size(),
                calculateProgress(trip),
                trip.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                trip.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ))
            .toList();
        
        return new TripListResult(tripSummaries, totalCount, query.page(), totalPages);
    }

    @Override
    public Optional<TripDetailResponse> getTripDetail(String tripId, String userId) {
        log.info("Getting trip detail - tripId: {}, userId: {}", tripId, userId);
        
        return tripRepository.findByIdAndUserId(tripId, userId)
                           .map(TripDetailResponse::from);
    }

    @Override
    @Transactional
    public Trip createTrip(CreateTripCommand command) {
        log.info("Creating trip - userId: {}, tripName: {}", command.userId(), command.tripName());
        
        String tripId = tripRepository.generateTripId();
        Trip trip = Trip.create(tripId, command.tripName(), command.userId(), command.transportMode());
        
        // 추가 정보 설정
        if (command.description() != null) {
            trip.setDescription(command.description());
        }
        
        if (command.startDate() != null) {
            // startDate만 설정, endDate는 여행지 추가 시 자동 계산됨
            trip.setTravelDates(command.startDate(), null);
        }
        
        return tripRepository.save(trip);
    }

    @Override
    @Transactional
    public Trip updateTrip(UpdateTripCommand command) {
        log.info("Updating trip - tripId: {}, userId: {}", command.tripId(), command.userId());
        
        // Repository에서 기존 Trip 조회
        Trip trip = tripRepository.findByIdAndUserId(command.tripId(), command.userId())
                                 .orElseThrow(() -> new TripNotFoundException(command.tripId(), command.userId()));
        
        // Trip 정보 업데이트
        trip.updateBasicInfo(command.tripName(), command.transportMode());
        
        // 설명 업데이트
        if (command.description() != null) {
            trip.setDescription(command.description());
        }
        
        // 여행 시작일 업데이트 (endDate는 여행지 추가 시 자동 계산됨)
        if (command.startDate() != null) {
            trip.setTravelDates(command.startDate(), trip.getEndDate());
        }
        
        return tripRepository.save(trip);
    }

    @Override
    @Transactional
    public void deleteTrip(DeleteTripCommand command) {
        log.info("Deleting trip - tripId: {}, userId: {}", command.tripId(), command.userId());
        
        // Repository에서 Trip 조회 + 권한 확인
        Trip trip = tripRepository.findByIdAndUserId(command.tripId(), command.userId())
                                 .orElseThrow(() -> new TripNotFoundException(command.tripId(), command.userId()));
        
        // Trip 삭제
        tripRepository.delete(trip);
        
        log.info("Trip deleted successfully - tripId: {}", command.tripId());
    }

    @Override
    @Transactional
    public Trip changeTripStatus(String tripId, String userId, TripStatus newStatus) {
        log.info("Changing trip status - tripId: {}, userId: {}, newStatus: {}", tripId, userId, newStatus);
        
        // Repository에서 기존 Trip 조회
        Trip trip = tripRepository.findByIdAndUserId(tripId, userId)
                                 .orElseThrow(() -> new TripNotFoundException(tripId, userId));
        
        // 상태 변경
        trip.changeStatus(newStatus);
        
        return tripRepository.save(trip);
    }

    @Override
    public boolean isOwner(String tripId, String userId) {
        log.info("Checking ownership - tripId: {}, userId: {}", tripId, userId);
        
        return tripRepository.isOwner(tripId, userId);
    }
    
    /**
     * 여행 진행률 계산
     */
    private int calculateProgress(Trip trip) {
        // 기본 설정 (여행 생성): 10%
        int progress = 10;
        
        // 멤버가 있으면: +20%
        if (!trip.getMembers().isEmpty()) {
            progress += 20;
        }
        
        // 목적지가 있으면: +30%
        if (!trip.getDestinations().isEmpty()) {
            progress += 30;
        }
        
        // 일정이 있으면: +40%
        if (trip.hasSchedule()) {
            progress += 40;
        }
        
        return Math.min(progress, 100);
    }
}