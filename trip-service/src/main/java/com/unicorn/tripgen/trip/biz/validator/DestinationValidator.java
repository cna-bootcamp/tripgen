package com.unicorn.tripgen.trip.biz.validator;

import com.unicorn.tripgen.trip.biz.domain.Destination;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;

/**
 * 여행지 도메인 검증기
 */
@Component
public class DestinationValidator {
    
    private static final int MAX_DESTINATIONS_PER_TRIP = 10;
    private static final int MIN_DESTINATION_NAME_LENGTH = 1;
    private static final int MAX_DESTINATION_NAME_LENGTH = 20;
    private static final int MAX_ACCOMMODATION_NAME_LENGTH = 20;
    private static final int MIN_NIGHTS = 1;
    private static final int MAX_NIGHTS = 30;
    
    /**
     * 여행지 추가 시 검증
     */
    public void validateForCreation(String destinationName, int nights, String accommodation,
                                   LocalTime checkInTime, LocalTime checkOutTime, int currentDestinationCount) {
        validateDestinationCount(currentDestinationCount);
        validateDestinationName(destinationName);
        validateNights(nights);
        validateAccommodation(accommodation);
        validateCheckTimes(checkInTime, checkOutTime);
    }
    
    /**
     * 여행지 업데이트 시 검증
     */
    public void validateForUpdate(String destinationName, Integer nights, String accommodation,
                                 LocalTime checkInTime, LocalTime checkOutTime) {
        if (destinationName != null) {
            validateDestinationName(destinationName);
        }
        if (nights != null) {
            validateNights(nights);
        }
        if (accommodation != null) {
            validateAccommodation(accommodation);
        }
        if (checkInTime != null || checkOutTime != null) {
            validateCheckTimes(checkInTime, checkOutTime);
        }
    }
    
    /**
     * 여행지 일괄 업데이트 시 검증
     */
    public void validateForBatchUpdate(List<DestinationInfo> destinations) {
        if (destinations == null || destinations.isEmpty()) {
            throw new IllegalArgumentException("최소 1개의 여행지가 필요합니다");
        }
        
        if (destinations.size() > MAX_DESTINATIONS_PER_TRIP) {
            throw new IllegalArgumentException("여행지는 최대 " + MAX_DESTINATIONS_PER_TRIP + "개까지 가능합니다");
        }
        
        // 각 여행지 정보 검증
        for (int i = 0; i < destinations.size(); i++) {
            DestinationInfo destination = destinations.get(i);
            try {
                validateDestinationName(destination.destinationName());
                validateNights(destination.nights());
                validateAccommodation(destination.accommodation());
                validateCheckTimes(destination.checkInTime(), destination.checkOutTime());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("여행지 " + (i + 1) + "번: " + e.getMessage());
            }
        }
        
        // 중복 여행지명 검증
        long uniqueNames = destinations.stream()
                                     .map(DestinationInfo::destinationName)
                                     .map(String::trim)
                                     .map(String::toLowerCase)
                                     .distinct()
                                     .count();
        
        if (uniqueNames != destinations.size()) {
            throw new IllegalArgumentException("여행지명은 중복될 수 없습니다");
        }
        
        // 총 여행 기간 검증
        int totalNights = destinations.stream()
                                    .mapToInt(DestinationInfo::nights)
                                    .sum();
        
        if (totalNights > 365) {
            throw new IllegalArgumentException("총 여행 기간은 365일을 초과할 수 없습니다");
        }
    }
    
    /**
     * 여행지 삭제 가능성 검증
     */
    public void validateForDeletion(List<Destination> currentDestinations, String destinationIdToDelete) {
        if (currentDestinations.size() <= 1) {
            throw new IllegalArgumentException("여행에는 최소 1개의 여행지가 필요합니다");
        }
        
        boolean destinationExists = currentDestinations.stream()
                                                      .anyMatch(dest -> dest.getDestinationId().equals(destinationIdToDelete));
        
        if (!destinationExists) {
            throw new IllegalArgumentException("삭제하려는 여행지를 찾을 수 없습니다");
        }
    }
    
    /**
     * 여행지 수 검증
     */
    private void validateDestinationCount(int currentCount) {
        if (currentCount >= MAX_DESTINATIONS_PER_TRIP) {
            throw new IllegalArgumentException("여행지는 최대 " + MAX_DESTINATIONS_PER_TRIP + "개까지 가능합니다");
        }
    }
    
    /**
     * 여행지명 검증
     */
    private void validateDestinationName(String destinationName) {
        if (destinationName == null || destinationName.trim().isEmpty()) {
            throw new IllegalArgumentException("여행지명은 필수입니다");
        }
        
        String trimmed = destinationName.trim();
        if (trimmed.length() < MIN_DESTINATION_NAME_LENGTH || trimmed.length() > MAX_DESTINATION_NAME_LENGTH) {
            throw new IllegalArgumentException("여행지명은 " + MIN_DESTINATION_NAME_LENGTH + "자 이상 " + 
                                             MAX_DESTINATION_NAME_LENGTH + "자 이하여야 합니다");
        }
        
        // 특수문자 검증
        if (containsInvalidCharacters(trimmed)) {
            throw new IllegalArgumentException("여행지명에 사용할 수 없는 문자가 포함되어 있습니다");
        }
    }
    
    /**
     * 숙박일 검증
     */
    private void validateNights(int nights) {
        if (nights < MIN_NIGHTS || nights > MAX_NIGHTS) {
            throw new IllegalArgumentException("숙박일은 " + MIN_NIGHTS + "일 이상 " + MAX_NIGHTS + "일 이하여야 합니다");
        }
    }
    
    /**
     * 숙소명 검증
     */
    private void validateAccommodation(String accommodation) {
        if (accommodation == null) {
            return; // 숙소명은 선택사항
        }
        
        String trimmed = accommodation.trim();
        if (trimmed.isEmpty()) {
            return; // 빈 문자열은 허용
        }
        
        if (trimmed.length() > MAX_ACCOMMODATION_NAME_LENGTH) {
            throw new IllegalArgumentException("숙소명은 " + MAX_ACCOMMODATION_NAME_LENGTH + "자를 초과할 수 없습니다");
        }
        
        if (containsInvalidCharacters(trimmed)) {
            throw new IllegalArgumentException("숙소명에 사용할 수 없는 문자가 포함되어 있습니다");
        }
    }
    
    /**
     * 체크인/체크아웃 시간 검증
     */
    private void validateCheckTimes(LocalTime checkInTime, LocalTime checkOutTime) {
        if (checkInTime == null && checkOutTime == null) {
            return; // 둘 다 없으면 OK
        }
        
        if (checkInTime != null && checkOutTime != null) {
            // 체크아웃이 체크인보다 이른 시간인 경우 (다음날 체크아웃으로 간주)
            if (checkOutTime.isBefore(checkInTime) && !isValidOvernightCheckout(checkInTime, checkOutTime)) {
                throw new IllegalArgumentException("체크아웃 시간이 올바르지 않습니다");
            }
        }
        
        // 체크인 시간 범위 검증 (일반적으로 14:00~18:00)
        if (checkInTime != null && (checkInTime.isBefore(LocalTime.of(12, 0)) || 
                                   checkInTime.isAfter(LocalTime.of(20, 0)))) {
            // 경고만 하고 에러는 발생시키지 않음 (비즈니스 요구사항에 따라 조정)
        }
        
        // 체크아웃 시간 범위 검증 (일반적으로 10:00~12:00)
        if (checkOutTime != null && (checkOutTime.isBefore(LocalTime.of(8, 0)) || 
                                    checkOutTime.isAfter(LocalTime.of(14, 0)))) {
            // 경고만 하고 에러는 발생시키지 않음
        }
    }
    
    /**
     * 여행지 순서 검증
     */
    public void validateDestinationOrder(List<DestinationInfo> destinations) {
        // 논리적 순서 검증 (예: 지리적 위치 고려)
        // 실제 구현에서는 Location Service와 연동하여 최적 경로 검증 가능
        
        for (int i = 0; i < destinations.size() - 1; i++) {
            DestinationInfo current = destinations.get(i);
            DestinationInfo next = destinations.get(i + 1);
            
            // 연속된 여행지 간의 이동 가능성 검증
            if (!isReasonableTravel(current.destinationName(), next.destinationName())) {
                throw new IllegalArgumentException(
                    String.format("%s에서 %s로의 이동이 비현실적입니다", 
                               current.destinationName(), next.destinationName())
                );
            }
        }
    }
    
    /**
     * 잘못된 문자 포함 여부 검사
     */
    private boolean containsInvalidCharacters(String text) {
        // 기본적인 특수문자 제한
        String invalidChars = "<>:\"|?*";
        return text.chars().anyMatch(ch -> invalidChars.indexOf(ch) >= 0);
    }
    
    /**
     * 유효한 다음날 체크아웃인지 확인
     */
    private boolean isValidOvernightCheckout(LocalTime checkIn, LocalTime checkOut) {
        // 체크인이 오후이고 체크아웃이 오전인 경우 다음날 체크아웃으로 간주
        return checkIn.isAfter(LocalTime.of(14, 0)) && checkOut.isBefore(LocalTime.of(14, 0));
    }
    
    /**
     * 합리적인 여행 경로인지 확인
     */
    private boolean isReasonableTravel(String from, String to) {
        // 간단한 검증 로직 (실제로는 더 복잡한 지리적 검증 필요)
        // 같은 도시/지역인지, 이동 거리가 합리적인지 등을 검증
        
        if (from.equals(to)) {
            return false; // 같은 여행지 연속 불가
        }
        
        // 실제 구현에서는 Location Service API를 사용하여 거리/시간 계산
        return true;
    }
    
    /**
     * 여행지 정보 (내부 사용)
     */
    public record DestinationInfo(
        String destinationName,
        int nights,
        String accommodation,
        LocalTime checkInTime,
        LocalTime checkOutTime
    ) {}
}