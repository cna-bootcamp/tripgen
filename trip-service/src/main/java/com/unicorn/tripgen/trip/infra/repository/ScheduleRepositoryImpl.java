package com.unicorn.tripgen.trip.infra.repository;

import com.unicorn.tripgen.trip.biz.domain.Schedule;
import com.unicorn.tripgen.trip.biz.usecase.out.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Schedule Repository 구현체
 * Clean Architecture의 Infrastructure Layer
 * Domain Layer의 ScheduleRepository 인터페이스를 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ScheduleRepositoryImpl implements ScheduleRepository {
    
    private final ScheduleJpaRepository scheduleJpaRepository;
    
    @Override
    public Schedule save(Schedule schedule) {
        log.debug("Saving schedule: {} for trip: {}", schedule.getScheduleId(), schedule.getTripId());
        return scheduleJpaRepository.save(schedule);
    }
    
    @Override
    public List<Schedule> saveAll(List<Schedule> schedules) {
        log.debug("Saving {} schedules", schedules.size());
        return scheduleJpaRepository.saveAll(schedules);
    }
    
    @Override
    public Optional<Schedule> findById(String scheduleId) {
        log.debug("Finding schedule by id: {}", scheduleId);
        return scheduleJpaRepository.findById(scheduleId);
    }
    
    @Override
    public List<Schedule> findByTripId(String tripId) {
        log.debug("Finding schedules for trip: {}", tripId);
        return scheduleJpaRepository.findByTripIdOrderByDayAsc(tripId);
    }
    
    @Override
    public Optional<Schedule> findByTripIdAndDay(String tripId, int day) {
        log.debug("Finding schedule for trip: {} and day: {}", tripId, day);
        return scheduleJpaRepository.findByTripIdAndDay(tripId, day);
    }
    
    @Override
    public int countByTripId(String tripId) {
        log.debug("Counting schedules for trip: {}", tripId);
        return scheduleJpaRepository.countByTripId(tripId);
    }
    
    @Override
    public void delete(Schedule schedule) {
        log.debug("Deleting schedule: {}", schedule.getScheduleId());
        scheduleJpaRepository.delete(schedule);
    }
    
    @Override
    public void deleteByTripId(String tripId) {
        log.debug("Deleting all schedules for trip: {}", tripId);
        scheduleJpaRepository.deleteByTripId(tripId);
    }
    
    @Override
    public boolean existsById(String scheduleId) {
        log.debug("Checking if schedule exists: {}", scheduleId);
        return scheduleJpaRepository.existsById(scheduleId);
    }
    
    @Override
    public boolean hasSchedule(String tripId) {
        log.debug("Checking if trip has schedule: {}", tripId);
        return scheduleJpaRepository.existsByTripId(tripId);
    }
    
    @Override
    public String generateScheduleId() {
        String scheduleId = UUID.randomUUID().toString();
        log.debug("Generated schedule id: {}", scheduleId);
        return scheduleId;
    }
}