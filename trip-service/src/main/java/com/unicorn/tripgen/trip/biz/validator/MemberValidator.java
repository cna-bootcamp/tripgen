package com.unicorn.tripgen.trip.biz.validator;

import com.unicorn.tripgen.trip.biz.domain.Gender;
import com.unicorn.tripgen.trip.biz.domain.HealthStatus;
import com.unicorn.tripgen.trip.biz.domain.Member;
import com.unicorn.tripgen.trip.biz.domain.Preference;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 멤버 도메인 검증기
 */
@Component
public class MemberValidator {
    
    private static final int MAX_MEMBERS_PER_TRIP = 10;
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 20;
    private static final int MIN_AGE = 1;
    private static final int MAX_AGE = 120;
    
    /**
     * 멤버 추가 시 검증
     */
    public void validateForCreation(String name, int age, Gender gender, HealthStatus healthStatus, 
                                   List<Preference> preferences, int currentMemberCount) {
        validateMemberCount(currentMemberCount);
        validateName(name);
        validateAge(age);
        validateGender(gender);
        validateHealthStatus(healthStatus);
        validatePreferences(preferences);
    }
    
    /**
     * 멤버 업데이트 시 검증
     */
    public void validateForUpdate(String name, Integer age, Gender gender, HealthStatus healthStatus, 
                                 List<Preference> preferences) {
        if (name != null) {
            validateName(name);
        }
        if (age != null) {
            validateAge(age);
        }
        if (gender != null) {
            validateGender(gender);
        }
        if (healthStatus != null) {
            validateHealthStatus(healthStatus);
        }
        if (preferences != null) {
            validatePreferences(preferences);
        }
    }
    
    /**
     * 멤버 일괄 업데이트 시 검증
     */
    public void validateForBatchUpdate(List<MemberInfo> members) {
        if (members == null || members.isEmpty()) {
            throw new IllegalArgumentException("최소 1명의 멤버가 필요합니다");
        }
        
        if (members.size() > MAX_MEMBERS_PER_TRIP) {
            throw new IllegalArgumentException("멤버는 최대 " + MAX_MEMBERS_PER_TRIP + "명까지 가능합니다");
        }
        
        // 각 멤버 정보 검증
        for (int i = 0; i < members.size(); i++) {
            MemberInfo member = members.get(i);
            try {
                validateName(member.name());
                validateAge(member.age());
                validateGender(member.gender());
                validateHealthStatus(member.healthStatus());
                validatePreferences(member.preferences());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("멤버 " + (i + 1) + "번: " + e.getMessage());
            }
        }
        
        // 중복 이름 검증
        long uniqueNames = members.stream()
                                 .map(MemberInfo::name)
                                 .distinct()
                                 .count();
        
        if (uniqueNames != members.size()) {
            throw new IllegalArgumentException("멤버 이름은 중복될 수 없습니다");
        }
    }
    
    /**
     * 멤버 삭제 가능성 검증
     */
    public void validateForDeletion(List<Member> currentMembers, String memberIdToDelete) {
        if (currentMembers.size() <= 1) {
            throw new IllegalArgumentException("여행에는 최소 1명의 멤버가 필요합니다");
        }
        
        boolean memberExists = currentMembers.stream()
                                           .anyMatch(member -> member.getMemberId().equals(memberIdToDelete));
        
        if (!memberExists) {
            throw new IllegalArgumentException("삭제하려는 멤버를 찾을 수 없습니다");
        }
    }
    
    /**
     * 멤버 수 검증
     */
    private void validateMemberCount(int currentCount) {
        if (currentCount >= MAX_MEMBERS_PER_TRIP) {
            throw new IllegalArgumentException("멤버는 최대 " + MAX_MEMBERS_PER_TRIP + "명까지 가능합니다");
        }
    }
    
    /**
     * 이름 검증
     */
    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다");
        }
        
        String trimmed = name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("이름은 " + MIN_NAME_LENGTH + "자 이상 " + MAX_NAME_LENGTH + "자 이하여야 합니다");
        }
        
        // 특수문자 검증
        if (!isValidName(trimmed)) {
            throw new IllegalArgumentException("이름에 사용할 수 없는 문자가 포함되어 있습니다");
        }
    }
    
    /**
     * 나이 검증
     */
    private void validateAge(int age) {
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new IllegalArgumentException("나이는 " + MIN_AGE + "세 이상 " + MAX_AGE + "세 이하여야 합니다");
        }
    }
    
    /**
     * 성별 검증
     */
    private void validateGender(Gender gender) {
        if (gender == null) {
            throw new IllegalArgumentException("성별은 필수입니다");
        }
    }
    
    /**
     * 건강상태 검증
     */
    private void validateHealthStatus(HealthStatus healthStatus) {
        if (healthStatus == null) {
            throw new IllegalArgumentException("건강상태는 필수입니다");
        }
    }
    
    /**
     * 선호도 검증
     */
    private void validatePreferences(List<Preference> preferences) {
        if (preferences == null) {
            return; // 선호도는 선택사항
        }
        
        if (preferences.size() > 6) {
            throw new IllegalArgumentException("선호도는 최대 6개까지 선택할 수 있습니다");
        }
        
        // 중복 제거 후 크기 비교
        long uniquePreferences = preferences.stream().distinct().count();
        if (uniquePreferences != preferences.size()) {
            throw new IllegalArgumentException("중복된 선호도가 있습니다");
        }
    }
    
    /**
     * 이름 유효성 검사
     */
    private boolean isValidName(String name) {
        // 한글, 영문, 숫자, 공백만 허용
        return name.matches("^[가-힣a-zA-Z0-9\\s]+$");
    }
    
    /**
     * 건강상태별 활동 제한 검증
     */
    public void validateActivityRestriction(List<Member> members, String activityType) {
        long restrictedMembers = members.stream()
                                       .filter(Member::hasActivityRestriction)
                                       .count();
        
        if (restrictedMembers > 0) {
            switch (activityType.toLowerCase()) {
                case "intense":
                case "sports":
                    long cannotDoIntense = members.stream()
                                                 .filter(member -> !member.canDoIntenseActivity())
                                                 .count();
                    if (cannotDoIntense > 0) {
                        throw new IllegalArgumentException("일부 멤버의 건강상태로 인해 격렬한 활동이 제한됩니다");
                    }
                    break;
                case "walking":
                    // 도보 활동에 대한 추가 검증 로직
                    break;
            }
        }
    }
    
    /**
     * 멤버 정보 (내부 사용)
     */
    public record MemberInfo(
        String name,
        int age,
        Gender gender,
        HealthStatus healthStatus,
        List<Preference> preferences
    ) {}
}