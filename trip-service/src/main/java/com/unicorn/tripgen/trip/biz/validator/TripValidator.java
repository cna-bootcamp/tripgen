package com.unicorn.tripgen.trip.biz.validator;

import com.unicorn.tripgen.trip.biz.domain.Trip;
import com.unicorn.tripgen.trip.biz.domain.TransportMode;
import org.springframework.stereotype.Component;

/**
 * 여행 도메인 검증기
 */
@Component
public class TripValidator {
    
    /**
     * 여행 생성 시 검증
     */
    public void validateForCreation(String tripName, TransportMode transportMode) {
        validateTripName(tripName);
        validateTransportMode(transportMode);
    }
    
    /**
     * 여행 업데이트 시 검증
     */
    public void validateForUpdate(String tripName, TransportMode transportMode) {
        if (tripName != null) {
            validateTripName(tripName);
        }
        if (transportMode != null) {
            validateTransportMode(transportMode);
        }
    }
    
    /**
     * 여행 소유권 검증
     */
    public void validateOwnership(Trip trip, String userId) {
        if (trip == null) {
            throw new IllegalArgumentException("여행 정보가 없습니다");
        }
        if (!trip.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 여행에 대한 권한이 없습니다");
        }
    }
    
    /**
     * 여행명 검증
     */
    private void validateTripName(String tripName) {
        if (tripName == null || tripName.trim().isEmpty()) {
            throw new IllegalArgumentException("여행명은 필수입니다");
        }
        
        String trimmed = tripName.trim();
        if (trimmed.length() > 16) {
            throw new IllegalArgumentException("여행명은 16자를 초과할 수 없습니다");
        }
        
        // 특수문자 검증 (선택적)
        if (containsInvalidCharacters(trimmed)) {
            throw new IllegalArgumentException("여행명에 사용할 수 없는 문자가 포함되어 있습니다");
        }
    }
    
    /**
     * 교통수단 검증
     */
    private void validateTransportMode(TransportMode transportMode) {
        if (transportMode == null) {
            throw new IllegalArgumentException("교통수단은 필수입니다");
        }
    }
    
    /**
     * 여행 상태 변경 가능성 검증
     */
    public void validateStatusChange(Trip trip, String newStatus) {
        if (trip == null) {
            throw new IllegalArgumentException("여행 정보가 없습니다");
        }
        
        // 현재 상태에 따른 변경 가능한 상태 검증
        switch (trip.getStatus()) {
            case PLANNING:
                if (!"ongoing".equals(newStatus) && !"completed".equals(newStatus)) {
                    throw new IllegalArgumentException("계획 중인 여행은 진행 중 또는 완료 상태로만 변경할 수 있습니다");
                }
                break;
            case ONGOING:
                if (!"completed".equals(newStatus) && !"planning".equals(newStatus)) {
                    throw new IllegalArgumentException("진행 중인 여행은 완료 또는 계획 중 상태로만 변경할 수 있습니다");
                }
                break;
            case COMPLETED:
                if (!"planning".equals(newStatus)) {
                    throw new IllegalArgumentException("완료된 여행은 계획 중 상태로만 변경할 수 있습니다");
                }
                break;
        }
    }
    
    /**
     * 여행 삭제 가능성 검증
     */
    public void validateForDeletion(Trip trip) {
        if (trip == null) {
            throw new IllegalArgumentException("여행 정보가 없습니다");
        }
        
        // 진행 중인 여행은 삭제 불가 (비즈니스 규칙)
        if (trip.getStatus() == com.unicorn.tripgen.trip.biz.domain.TripStatus.ONGOING) {
            throw new IllegalArgumentException("진행 중인 여행은 삭제할 수 없습니다");
        }
    }
    
    /**
     * 일정 생성 가능성 검증
     */
    public void validateForScheduleGeneration(Trip trip) {
        if (trip == null) {
            throw new IllegalArgumentException("여행 정보가 없습니다");
        }
        
        if (trip.getMembers().isEmpty()) {
            throw new IllegalArgumentException("일정 생성을 위해서는 최소 1명의 멤버가 필요합니다");
        }
        
        if (trip.getDestinations().isEmpty()) {
            throw new IllegalArgumentException("일정 생성을 위해서는 최소 1개의 여행지가 필요합니다");
        }
    }
    
    /**
     * 잘못된 문자 포함 여부 검사
     */
    private boolean containsInvalidCharacters(String text) {
        // 기본적인 특수문자 제한 (필요에 따라 조정)
        String invalidChars = "<>:\"|?*";
        return text.chars().anyMatch(ch -> invalidChars.indexOf(ch) >= 0);
    }
}