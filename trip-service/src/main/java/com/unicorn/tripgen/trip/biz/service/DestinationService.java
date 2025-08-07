package com.unicorn.tripgen.trip.biz.service;

import com.unicorn.tripgen.trip.biz.domain.Destination;
import com.unicorn.tripgen.trip.biz.usecase.in.DestinationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DestinationService implements DestinationUseCase {

    @Override
    public List<Destination> getTripDestinations(String tripId, String userId) {
        log.info("Getting destinations for trip: {}, userId: {}", tripId, userId);
        
        // TODO: 실제 구현 - Repository에서 조회
        return new ArrayList<>();
    }

    @Override
    @Transactional
    public Destination addDestination(AddDestinationCommand command) {
        log.info("Adding destination to trip: {}", command.tripId());
        
        String destinationId = UUID.randomUUID().toString();
        Destination destination = Destination.create(
            destinationId,
            command.tripId(),
            command.destinationName(),
            command.nights(),
            command.accommodation(),
            command.checkInTime(),
            command.checkOutTime(),
            1 // TODO: order를 정확히 계산해야 함
        );
        
        // TODO: Repository에 저장
        
        return destination;
    }

    @Override
    @Transactional
    public Destination updateDestination(UpdateDestinationCommand command) {
        log.info("Updating destination {} in trip: {}", command.destinationId(), command.tripId());
        
        // TODO: 실제 구현 - Repository에서 조회 후 업데이트
        Destination destination = Destination.create(
            command.destinationId(),
            command.tripId(),
            command.destinationName(),
            command.nights(),
            command.accommodation(),
            command.checkInTime(),
            command.checkOutTime(),
            1 // TODO: order를 정확히 계산해야 함
        );
        
        destination.updateInfo(command.destinationName(), command.nights(), 
                              command.accommodation(), command.checkInTime(), command.checkOutTime());
        
        return destination;
    }

    @Override
    @Transactional
    public void deleteDestination(DeleteDestinationCommand command) {
        log.info("Deleting destination {} from trip: {}", command.destinationId(), command.tripId());
        
        // TODO: 실제 구현 - 권한 확인 후 삭제
    }

    @Override
    @Transactional
    public DestinationsBatchResult updateDestinationsBatch(UpdateDestinationsBatchCommand command) {
        log.info("Batch updating destinations for trip: {}", command.tripId());
        
        List<Destination> destinations = new ArrayList<>();
        LocalDate currentDate = LocalDate.now(); // TODO: 실제 여행 시작일로부터 계산
        int totalDays = 0;
        
        for (DestinationInfo info : command.destinations()) {
            String destinationId = UUID.randomUUID().toString();
            int order = destinations.size() + 1;
            Destination destination = Destination.create(
                destinationId,
                command.tripId(),
                info.destinationName(),
                info.nights(),
                info.accommodation(),
                info.checkInTime(),
                info.checkOutTime(),
                order
            );
            destinations.add(destination);
            totalDays += info.nights();
        }
        
        // TODO: Repository에서 기존 여행지 삭제 후 새 여행지 저장
        
        LocalDate startDate = currentDate;
        LocalDate endDate = currentDate.plusDays(totalDays);
        
        return new DestinationsBatchResult(
            command.tripId(),
            destinations,
            totalDays,
            startDate.toString(),
            endDate.toString(),
            LocalDate.now().toString()
        );
    }
}