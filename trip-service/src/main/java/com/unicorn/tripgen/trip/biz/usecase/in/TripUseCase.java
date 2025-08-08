package com.unicorn.tripgen.trip.biz.usecase.in;

import com.unicorn.tripgen.trip.biz.domain.Trip;
import com.unicorn.tripgen.trip.biz.domain.TransportMode;
import com.unicorn.tripgen.trip.biz.domain.TripStatus;
import com.unicorn.tripgen.trip.biz.dto.TripDetailResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 여행 관련 Use Case 인터페이스
 * Clean Architecture의 Use Case Layer에 속함
 */
public interface TripUseCase {
    
    /**
     * 여행 목록 조회 요청
     */
    record GetTripListQuery(
        String userId,
        TripStatus status,
        String search,
        String sort,
        int page,
        int size
    ) {}
    
    /**
     * 여행 목록 조회 결과
     */
    record TripListResult(
        List<TripSummary> trips,
        int totalCount,
        int currentPage,
        int totalPages
    ) {}
    
    /**
     * 여행 요약 정보
     */
    record TripSummary(
        String tripId,
        String tripName,
        TripStatus status,
        String currentStep,
        String startDate,
        String endDate,
        int memberCount,
        int destinationCount,
        int progress,
        String createdAt,
        String updatedAt
    ) {}
    
    /**
     * 여행 생성 명령
     */
    record CreateTripCommand(
        String userId,
        String tripName,
        String description,
        LocalDate startDate,
        TransportMode transportMode
    ) {}
    
    /**
     * 여행 업데이트 명령
     */
    record UpdateTripCommand(
        String tripId,
        String userId,
        String tripName,
        String description,
        LocalDate startDate,
        TransportMode transportMode
    ) {}
    
    /**
     * 여행 삭제 명령
     */
    record DeleteTripCommand(
        String tripId,
        String userId
    ) {}
    
    /**
     * 여행 목록 조회
     */
    TripListResult getTripList(GetTripListQuery query);
    
    /**
     * 여행 상세 조회
     */
    Optional<TripDetailResponse> getTripDetail(String tripId, String userId);
    
    /**
     * 새로운 여행 생성
     */
    Trip createTrip(CreateTripCommand command);
    
    /**
     * 여행 기본 정보 업데이트
     */
    Trip updateTrip(UpdateTripCommand command);
    
    /**
     * 여행 삭제
     */
    void deleteTrip(DeleteTripCommand command);
    
    /**
     * 여행 상태 변경
     */
    Trip changeTripStatus(String tripId, String userId, TripStatus newStatus);
    
    /**
     * 여행 소유자 확인
     */
    boolean isOwner(String tripId, String userId);
}