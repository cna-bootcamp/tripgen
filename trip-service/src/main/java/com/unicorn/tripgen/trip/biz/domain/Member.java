package com.unicorn.tripgen.trip.biz.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 여행 멤버 도메인 엔티티
 */
public class Member {
    private final String memberId;
    private final String tripId;
    private String name;
    private int age;
    private Gender gender;
    private HealthStatus healthStatus;
    private final List<Preference> preferences;
    
    private Member(String memberId, String tripId, String name, int age, Gender gender, HealthStatus healthStatus) {
        this.memberId = Objects.requireNonNull(memberId, "Member ID는 필수입니다");
        this.tripId = Objects.requireNonNull(tripId, "Trip ID는 필수입니다");
        this.name = validateName(name);
        this.age = validateAge(age);
        this.gender = Objects.requireNonNull(gender, "성별은 필수입니다");
        this.healthStatus = Objects.requireNonNull(healthStatus, "건강상태는 필수입니다");
        this.preferences = new ArrayList<>();
    }
    
    /**
     * 새로운 멤버 생성 팩토리 메서드
     */
    public static Member create(String memberId, String tripId, String name, int age, 
                               Gender gender, HealthStatus healthStatus, List<Preference> preferences) {
        Member member = new Member(memberId, tripId, name, age, gender, healthStatus);
        if (preferences != null) {
            member.preferences.addAll(preferences);
        }
        return member;
    }
    
    /**
     * 기존 멤버 복원 팩토리 메서드
     */
    public static Member restore(String memberId, String tripId, String name, int age,
                                Gender gender, HealthStatus healthStatus, List<Preference> preferences) {
        return create(memberId, tripId, name, age, gender, healthStatus, preferences);
    }
    
    /**
     * 멤버 정보 업데이트
     */
    public void updateInfo(String name, int age, Gender gender, HealthStatus healthStatus, 
                          List<Preference> preferences) {
        this.name = validateName(name);
        this.age = validateAge(age);
        this.gender = Objects.requireNonNull(gender, "성별은 필수입니다");
        this.healthStatus = Objects.requireNonNull(healthStatus, "건강상태는 필수입니다");
        
        this.preferences.clear();
        if (preferences != null) {
            this.preferences.addAll(preferences);
        }
    }
    
    /**
     * 건강 상태에 따른 활동 제한 여부 확인
     */
    public boolean hasActivityRestriction() {
        return healthStatus == HealthStatus.CAUTION || healthStatus == HealthStatus.LIMITED;
    }
    
    /**
     * 격렬한 활동 가능 여부 확인
     */
    public boolean canDoIntenseActivity() {
        return healthStatus == HealthStatus.EXCELLENT || healthStatus == HealthStatus.GOOD;
    }
    
    /**
     * 특정 선호도 포함 여부 확인
     */
    public boolean hasPreference(Preference preference) {
        return preferences.contains(preference);
    }
    
    /**
     * 이름 검증
     */
    private String validateName(String name) {
        Objects.requireNonNull(name, "이름은 필수입니다");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("이름은 필수입니다");
        }
        if (trimmed.length() < 2 || trimmed.length() > 20) {
            throw new IllegalArgumentException("이름은 2자 이상 20자 이하여야 합니다");
        }
        return trimmed;
    }
    
    /**
     * 나이 검증
     */
    private int validateAge(int age) {
        if (age < 1 || age > 120) {
            throw new IllegalArgumentException("나이는 1세 이상 120세 이하여야 합니다");
        }
        return age;
    }
    
    // Getters
    public String getMemberId() {
        return memberId;
    }
    
    public String getTripId() {
        return tripId;
    }
    
    public String getName() {
        return name;
    }
    
    public int getAge() {
        return age;
    }
    
    public Gender getGender() {
        return gender;
    }
    
    public HealthStatus getHealthStatus() {
        return healthStatus;
    }
    
    public List<Preference> getPreferences() {
        return new ArrayList<>(preferences);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return Objects.equals(memberId, member.memberId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(memberId);
    }
    
    @Override
    public String toString() {
        return "Member{" +
                "memberId='" + memberId + '\'' +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", gender=" + gender +
                ", healthStatus=" + healthStatus +
                '}';
    }
}