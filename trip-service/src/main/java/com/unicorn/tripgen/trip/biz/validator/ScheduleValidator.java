package com.unicorn.tripgen.trip.biz.validator;

import com.unicorn.tripgen.trip.biz.domain.Schedule;
import com.unicorn.tripgen.trip.biz.domain.SchedulePlace;
import com.unicorn.tripgen.trip.biz.domain.Trip;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

/**
 * 일정 도메인 검증기
 */
@Component
public class ScheduleValidator {
    
    private static final int MAX_PLACES_PER_DAY = 20;
    private static final int MIN_DURATION = 1; // 최소 1분
    private static final int MAX_DURATION = 720; // 최대 12시간
    private static final LocalTime EARLIEST_START_TIME = LocalTime.of(6, 0); // 오전 6시
    private static final LocalTime LATEST_END_TIME = LocalTime.of(23, 0); // 오후 11시
    
    /**
     * 일정 생성 가능성 검증
     */
    public void validateForGeneration(Trip trip, LocalTime startTime, String specialRequests) {
        validateTrip(trip);
        validateStartTime(startTime);
        validateSpecialRequests(specialRequests);
        
        // 여행 정보 완성도 검증
        if (trip.getMembers().isEmpty()) {
            throw new IllegalArgumentException("일정 생성을 위해서는 최소 1명의 멤버가 필요합니다");
        }
        
        if (trip.getDestinations().isEmpty()) {
            throw new IllegalArgumentException("일정 생성을 위해서는 최소 1개의 여행지가 필요합니다");
        }
    }
    
    /**
     * 일정 수정 시 검증
     */
    public void validateForUpdate(Schedule schedule, List<PlaceOrder> places) {
        if (schedule == null) {
            throw new IllegalArgumentException("일정 정보가 없습니다");
        }
        
        if (places == null || places.isEmpty()) {
            throw new IllegalArgumentException("장소 정보가 필요합니다");
        }
        
        // 장소 순서 검증
        validatePlaceOrders(places, schedule.getPlaces().size());
    }
    
    /**
     * 일자별 일정 재생성 검증
     */
    public void validateForRegeneration(Trip trip, int day, String specialRequests) {
        validateTrip(trip);
        validateDay(day, trip.getTotalDays());
        validateSpecialRequests(specialRequests);
    }
    
    /**
     * 일정 내보내기 검증
     */
    public void validateForExport(Trip trip, List<Schedule> schedules, String format, List<Integer> days) {
        validateTrip(trip);
        validateExportFormat(format);
        
        if (schedules == null || schedules.isEmpty()) {
            throw new IllegalArgumentException("내보낼 일정이 없습니다");
        }
        
        if (days != null && !days.isEmpty()) {
            validateExportDays(days, trip.getTotalDays());
        }
    }
    
    /**
     * 장소 추천 정보 조회 검증
     */
    public void validateForRecommendations(Trip trip, String placeId, Integer day) {
        validateTrip(trip);
        
        if (placeId == null || placeId.trim().isEmpty()) {
            throw new IllegalArgumentException("장소 ID가 필요합니다");
        }
        
        if (day != null) {
            validateDay(day, trip.getTotalDays());
        }
    }
    
    /**
     * 일정 시간 충돌 검증
     */
    public void validateTimeConflicts(List<SchedulePlace> places) {
        if (places == null || places.size() <= 1) {
            return; // 충돌 가능성 없음
        }
        
        // 시간순 정렬 후 충돌 검사
        List<SchedulePlace> sortedPlaces = places.stream()
                                                .filter(place -> place.getStartTime() != null)
                                                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                                                .toList();
        
        for (int i = 0; i < sortedPlaces.size() - 1; i++) {
            SchedulePlace current = sortedPlaces.get(i);
            SchedulePlace next = sortedPlaces.get(i + 1);
            
            LocalTime currentEndTime = current.getEndTime();
            LocalTime nextStartTime = next.getStartTime();
            
            if (currentEndTime != null && currentEndTime.isAfter(nextStartTime)) {
                throw new IllegalArgumentException(
                    String.format("시간 충돌: %s (%s~%s)와 %s (%s~%s)",
                                current.getPlaceName(), current.getStartTime(), currentEndTime,
                                next.getPlaceName(), next.getStartTime(), next.getEndTime())
                );
            }
            
            // 이동 시간 고려
            if (current.hasTransportation()) {
                LocalTime arrivalTime = currentEndTime.plusMinutes(current.getTransportation().getDuration());
                if (arrivalTime.isAfter(nextStartTime)) {
                    throw new IllegalArgumentException(
                        String.format("이동 시간을 고려할 때 %s에서 %s로의 이동이 불가능합니다",
                                    current.getPlaceName(), next.getPlaceName())
                    );
                }
            }
        }
    }
    
    /**
     * 하루 일정의 총 시간 검증
     */
    public void validateDayDuration(List<SchedulePlace> places, LocalTime startTime) {
        if (places == null || places.isEmpty()) {
            return;
        }
        
        // 총 소요 시간 계산
        int totalDuration = places.stream()
                                 .mapToInt(SchedulePlace::getDuration)
                                 .sum();
        
        int totalTravelTime = places.stream()
                                   .filter(SchedulePlace::hasTransportation)
                                   .mapToInt(place -> place.getTransportation().getDuration())
                                   .sum();
        
        int totalTime = totalDuration + totalTravelTime;
        
        // 하루 최대 활동 시간 (18시간) 검증
        if (totalTime > 1080) { // 18시간 = 1080분
            throw new IllegalArgumentException("하루 일정의 총 시간이 18시간을 초과합니다");
        }
        
        // 시작 시간 기준 종료 시간 계산
        if (startTime != null) {
            LocalTime estimatedEndTime = startTime.plusMinutes(totalTime);
            if (estimatedEndTime.isAfter(LATEST_END_TIME)) {
                throw new IllegalArgumentException(
                    String.format("예상 종료 시간(%s)이 너무 늦습니다 (권장: %s 이전)", 
                                estimatedEndTime, LATEST_END_TIME)
                );
            }
        }
    }
    
    /**
     * 여행 정보 검증
     */
    private void validateTrip(Trip trip) {
        if (trip == null) {
            throw new IllegalArgumentException("여행 정보가 없습니다");
        }
    }
    
    /**
     * 시작 시간 검증
     */
    private void validateStartTime(LocalTime startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("시작 시간은 필수입니다");
        }
        
        if (startTime.isBefore(EARLIEST_START_TIME) || startTime.isAfter(LocalTime.of(12, 0))) {
            throw new IllegalArgumentException(
                String.format("시작 시간은 %s에서 %s 사이여야 합니다", 
                            EARLIEST_START_TIME, LocalTime.of(12, 0))
            );
        }
    }
    
    /**
     * 특별 요청사항 검증
     */
    private void validateSpecialRequests(String specialRequests) {
        if (specialRequests != null && specialRequests.length() > 500) {
            throw new IllegalArgumentException("특별 요청사항은 500자를 초과할 수 없습니다");
        }
    }
    
    /**
     * 일차 검증
     */
    private void validateDay(int day, int totalDays) {
        if (day < 1 || day > totalDays) {
            throw new IllegalArgumentException("유효하지 않은 일차입니다 (1~" + totalDays + ")");
        }
    }
    
    /**
     * 장소 순서 검증
     */
    private void validatePlaceOrders(List<PlaceOrder> places, int totalPlaces) {
        if (places.size() > totalPlaces) {
            throw new IllegalArgumentException("기존 장소 수보다 많은 순서가 지정되었습니다");
        }
        
        // 순서 중복 검증
        long uniqueOrders = places.stream()
                                 .mapToInt(PlaceOrder::order)
                                 .distinct()
                                 .count();
        
        if (uniqueOrders != places.size()) {
            throw new IllegalArgumentException("중복된 순서가 있습니다");
        }
        
        // 순서 범위 검증
        boolean hasInvalidOrder = places.stream()
                                       .anyMatch(place -> place.order() < 1 || place.order() > totalPlaces);
        
        if (hasInvalidOrder) {
            throw new IllegalArgumentException("유효하지 않은 순서가 있습니다 (1~" + totalPlaces + ")");
        }
    }
    
    /**
     * 내보내기 형식 검증
     */
    private void validateExportFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("내보내기 형식은 필수입니다");
        }
        
        if (!format.equalsIgnoreCase("pdf") && !format.equalsIgnoreCase("image")) {
            throw new IllegalArgumentException("지원하지 않는 내보내기 형식입니다 (pdf, image만 지원)");
        }
    }
    
    /**
     * 내보내기 일차 검증
     */
    private void validateExportDays(List<Integer> days, int totalDays) {
        for (Integer day : days) {
            if (day == null || day < 1 || day > totalDays) {
                throw new IllegalArgumentException("유효하지 않은 일차가 포함되어 있습니다 (1~" + totalDays + ")");
            }
        }
    }
    
    /**
     * 장소 순서 정보
     */
    public record PlaceOrder(String placeId, int order) {}
}