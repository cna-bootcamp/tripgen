package com.unicorn.tripgen.trip.biz.service;

import com.unicorn.tripgen.trip.biz.domain.Destination;
import com.unicorn.tripgen.trip.biz.domain.Trip;
import com.unicorn.tripgen.trip.biz.usecase.in.DestinationUseCase;
import com.unicorn.tripgen.trip.biz.usecase.out.TripRepository;
import com.unicorn.tripgen.trip.biz.exception.TripNotFoundException;
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
    
    private final TripRepository tripRepository;

    @Override
    public List<Destination> getTripDestinations(String tripId, String userId) {
        log.info("Getting destinations for trip: {}, userId: {}", tripId, userId);
        
        // Trip 엔티티를 조회하여 연결된 destinations 반환
        Trip trip = tripRepository.findByIdAndUserId(tripId, userId)
            .orElseThrow(() -> new TripNotFoundException(tripId, userId));
        
        return trip.getDestinations();
    }

    @Override
    @Transactional
    public Destination addDestination(AddDestinationCommand command) {
        log.info("Adding destination to trip: {}", command.tripId());
        
        // Trip 엔티티 조회 (날짜 계산을 위해 필수)
        Trip trip = tripRepository.findById(command.tripId())
            .orElseThrow(() -> new TripNotFoundException(command.tripId(), "Unknown"));
        
        // 기존 여행지 개수를 기반으로 순서 계산
        List<Destination> existingDestinations = trip.getDestinations();
        int order = existingDestinations.size() + 1;
        
        // 새 여행지 생성
        String destinationId = UUID.randomUUID().toString();
        Destination destination = Destination.create(
            destinationId,
            command.tripId(),
            command.destinationName(),
            command.nights(),
            command.accommodation(),
            command.checkInTime(),
            command.checkOutTime(),
            order
        );
        
        // 날짜 계산 및 설정
        LocalDate destinationStartDate = calculateDestinationStartDate(trip, existingDestinations);
        LocalDate destinationEndDate = destinationStartDate.plusDays(command.nights());
        destination.setDates(destinationStartDate, destinationEndDate);
        
        // Trip에 여행지 추가 (이때 Trip의 endDate가 자동으로 업데이트됨)
        trip.addDestination(destination);
        
        // Trip 저장 (cascade로 destination도 함께 저장됨)
        tripRepository.save(trip);
        
        log.info("Destination added successfully - destinationId: {}, dates: {} to {}", 
                 destinationId, destinationStartDate, destinationEndDate);
        
        return destination;
    }
    
    /**
     * 새 여행지의 시작일 계산
     */
    private LocalDate calculateDestinationStartDate(Trip trip, List<Destination> existingDestinations) {
        // 기존 여행지가 없으면 Trip의 시작일 사용
        if (existingDestinations.isEmpty()) {
            if (trip.getStartDate() == null) {
                throw new IllegalStateException("여행 시작일이 설정되지 않았습니다. 먼저 여행 기본정보를 완성해주세요.");
            }
            return trip.getStartDate();
        }
        
        // 기존 여행지가 있으면 마지막 여행지의 종료일이 새 여행지의 시작일
        Destination lastDestination = existingDestinations.get(existingDestinations.size() - 1);
        return lastDestination.getEndDate();
    }

    @Override
    @Transactional
    public Destination updateDestination(UpdateDestinationCommand command) {
        log.info("Updating destination {} in trip: {}", command.destinationId(), command.tripId());
        
        // 1. Trip과 해당 Destination 조회
        Trip trip = tripRepository.findByIdAndUserId(command.tripId(), command.userId())
            .orElseThrow(() -> new TripNotFoundException(command.tripId(), command.userId()));
        
        // 2. 기존 Destination 찾기
        Destination destination = trip.getDestinations().stream()
            .filter(dest -> dest.getDestinationId().equals(command.destinationId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("여행지를 찾을 수 없습니다: " + command.destinationId()));
        
        // 3. Destination 정보 업데이트
        destination.updateInfo(command.destinationName(), command.nights(), 
                              command.accommodation(), command.checkInTime(), command.checkOutTime());
        
        // 4. Trip의 날짜 재계산 (여행지 순서대로 날짜 재배치)
        recalculateDestinationDates(trip);
        
        // 5. Trip 저장 (cascade로 destination도 함께 저장)
        tripRepository.save(trip);
        
        log.info("Destination updated successfully - destinationId: {}, new nights: {}", 
                 command.destinationId(), command.nights());
        
        return destination;
    }
    
    /**
     * Trip의 모든 여행지 날짜를 순서대로 재계산
     */
    private void recalculateDestinationDates(Trip trip) {
        List<Destination> destinations = trip.getDestinations();
        if (destinations.isEmpty()) return;
        
        // 첫 번째 여행지는 Trip의 startDate부터 시작
        LocalDate currentDate = trip.getStartDate();
        if (currentDate == null) {
            throw new IllegalStateException("여행 시작일이 설정되지 않았습니다");
        }
        
        for (Destination dest : destinations) {
            LocalDate startDate = currentDate;
            LocalDate endDate = startDate.plusDays(dest.getNights());
            dest.setDates(startDate, endDate);
            
            // 다음 여행지의 시작일은 현재 여행지의 종료일
            currentDate = endDate;
        }
        
        // Trip의 전체 기간 업데이트
        if (!destinations.isEmpty()) {
            Destination lastDest = destinations.get(destinations.size() - 1);
            trip.setTravelDates(trip.getStartDate(), lastDest.getEndDate());
        }
    }
    
    /**
     * 여행지들의 순서를 연속적으로 재정렬
     */
    private void reorderDestinations(Trip trip) {
        List<Destination> destinations = trip.getDestinations();
        if (destinations.isEmpty()) return;
        
        // order 기준으로 정렬 후 1부터 순차적으로 재할당
        destinations.sort((d1, d2) -> Integer.compare(d1.getOrder(), d2.getOrder()));
        
        for (int i = 0; i < destinations.size(); i++) {
            int newOrder = i + 1;
            destinations.get(i).updateOrder(newOrder);
            log.debug("Reordered destination: {} to order {}", 
                     destinations.get(i).getDestinationName(), newOrder);
        }
    }

    @Override
    @Transactional
    public void deleteDestination(DeleteDestinationCommand command) {
        log.info("Deleting destination {} from trip: {}", command.destinationId(), command.tripId());
        
        // 1. Trip 조회 (권한 확인 포함)
        Trip trip = tripRepository.findByIdAndUserId(command.tripId(), command.userId())
            .orElseThrow(() -> new TripNotFoundException(command.tripId(), command.userId()));
        
        // 2. 여행지가 존재하는지 확인
        log.info("Current destinations count: {}", trip.getDestinations().size());
        boolean destinationExists = trip.getDestinations().stream()
            .anyMatch(dest -> dest.getDestinationId().equals(command.destinationId()));
        
        log.info("Destination exists: {}", destinationExists);
        if (!destinationExists) {
            throw new IllegalArgumentException("여행지를 찾을 수 없습니다: " + command.destinationId());
        }
        
        // 3. Trip에서 여행지 제거 (JPA 양방향 관계 해제 포함)
        log.info("Removing destination from trip...");
        trip.removeDestination(command.destinationId());
        log.info("Destinations count after removal: {}", trip.getDestinations().size());
        
        // 4. 남은 여행지들의 순서 재정렬
        if (!trip.getDestinations().isEmpty()) {
            log.info("Reordering remaining destinations...");
            reorderDestinations(trip);
        }
        
        // 5. 남은 여행지들의 날짜 재계산 (여행지가 남아있고 startDate가 유효한 경우에만)
        if (!trip.getDestinations().isEmpty() && trip.getStartDate() != null) {
            log.info("Recalculating destination dates...");
            recalculateDestinationDates(trip);
        }
        
        // 5. Trip 저장 (cascade로 변경사항 반영)
        log.info("Saving trip...");
        tripRepository.save(trip);
        
        log.info("Destination deleted successfully - destinationId: {}", command.destinationId());
    }

    @Override
    @Transactional
    public DestinationsBatchResult updateDestinationsBatch(UpdateDestinationsBatchCommand command) {
        log.info("Batch updating destinations for trip: {}", command.tripId());
        
        // 1. Trip 조회
        Trip trip = tripRepository.findByIdAndUserId(command.tripId(), command.userId())
            .orElseThrow(() -> new TripNotFoundException(command.tripId(), command.userId()));
        
        log.info("Trip retrieved - ID: {}, Name: {}, StartDate: {}", 
                 trip.getTripId(), trip.getTripName(), trip.getStartDate());
        
        // 2. 기존 Trip의 startDate 보존 (재계산 시 필요)
        LocalDate originalStartDate = trip.getStartDate();
        log.info("Trip startDate before update: {}", originalStartDate);
        if (originalStartDate == null) {
            log.error("Trip startDate is null - Trip: {}, User: {}", command.tripId(), command.userId());
            throw new IllegalStateException("여행지 일괄 업데이트를 위해서는 여행 시작일이 먼저 설정되어야 합니다");
        }
        
        // 3. 새로운 여행지 목록 생성
        List<Destination> newDestinations = new ArrayList<>();
        int totalNights = 0;
        
        for (DestinationInfo info : command.destinations()) {
            String destinationId = UUID.randomUUID().toString();
            int order = newDestinations.size() + 1;
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
            newDestinations.add(destination);
            totalNights += info.nights();
        }
        
        // 4. Trip의 여행지 목록을 새로운 목록으로 일괄 교체 (JPA 양방향 관계 설정 포함)
        trip.updateDestinations(newDestinations);
        log.info("Trip startDate after updateDestinations: {}", trip.getStartDate());
        
        // 5. Trip의 startDate 복원 (updateDestinations에서 손실될 가능성 대비)
        if (trip.getStartDate() == null) {
            log.info("Restoring startDate from {} to {}", trip.getStartDate(), originalStartDate);
            trip.setTravelDates(originalStartDate, null);
        }
        
        // 6. 날짜 재계산
        log.info("Trip startDate before recalculate: {}", trip.getStartDate());
        recalculateDestinationDates(trip);
        
        // 7. Trip 저장 (cascade로 destinations도 함께 저장)
        tripRepository.save(trip);
        
        // 6. 결과 반환
        LocalDate startDate = trip.getStartDate();
        LocalDate endDate = trip.getEndDate();
        
        log.info("Batch update completed - {} destinations, total nights: {}", 
                 newDestinations.size(), totalNights);
        
        return new DestinationsBatchResult(
            command.tripId(),
            newDestinations,
            totalNights,
            startDate != null ? startDate.toString() : null,
            endDate != null ? endDate.toString() : null,
            LocalDate.now().toString()
        );
    }
}