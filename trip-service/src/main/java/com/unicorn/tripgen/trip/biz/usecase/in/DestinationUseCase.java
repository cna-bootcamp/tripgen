package com.unicorn.tripgen.trip.biz.usecase.in;

import com.unicorn.tripgen.trip.biz.domain.Destination;

import java.time.LocalTime;
import java.util.List;

/**
 * 여행지 관련 Use Case 인터페이스
 */
public interface DestinationUseCase {
    
    /**
     * 여행지 추가 명령
     */
    record AddDestinationCommand(
        String tripId,
        String userId,
        String destinationName,
        int nights,
        String accommodation,
        LocalTime checkInTime,
        LocalTime checkOutTime
    ) {}
    
    /**
     * 여행지 업데이트 명령
     */
    record UpdateDestinationCommand(
        String tripId,
        String destinationId,
        String userId,
        String destinationName,
        int nights,
        String accommodation,
        LocalTime checkInTime,
        LocalTime checkOutTime
    ) {}
    
    /**
     * 여행지 삭제 명령
     */
    record DeleteDestinationCommand(
        String tripId,
        String destinationId,
        String userId
    ) {}
    
    /**
     * 여행지 일괄 업데이트 명령
     */
    record UpdateDestinationsBatchCommand(
        String tripId,
        String userId,
        List<DestinationInfo> destinations
    ) {}
    
    /**
     * 여행지 정보
     */
    record DestinationInfo(
        String destinationName,
        int nights,
        String accommodation,
        LocalTime checkInTime,
        LocalTime checkOutTime
    ) {}
    
    /**
     * 여행지 일괄 업데이트 결과
     */
    record DestinationsBatchResult(
        String tripId,
        List<Destination> destinations,
        int totalDays,
        String startDate,
        String endDate,
        String updatedAt
    ) {}
    
    /**
     * 여행지 목록 조회
     */
    List<Destination> getTripDestinations(String tripId, String userId);
    
    /**
     * 여행지 추가
     */
    Destination addDestination(AddDestinationCommand command);
    
    /**
     * 여행지 정보 수정
     */
    Destination updateDestination(UpdateDestinationCommand command);
    
    /**
     * 여행지 삭제
     */
    void deleteDestination(DeleteDestinationCommand command);
    
    /**
     * 여행지 목록 일괄 업데이트 (기존 여행지 모두 교체)
     */
    DestinationsBatchResult updateDestinationsBatch(UpdateDestinationsBatchCommand command);
}