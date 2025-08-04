package com.unicorn.tripgen.trip.biz.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 건강 고려사항 값 객체 (Value Object)
 */
public class HealthConsideration {
    /**
     * 접근성 시설 유형
     */
    public enum AccessibilityType {
        ELEVATOR("엘리베이터"),
        RAMP("경사로"),
        WHEELCHAIR("휠체어");
        
        private final String description;
        
        AccessibilityType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final List<String> restPoints;
    private final List<AccessibilityType> accessibility;
    private final double walkingDistance; // 도보 거리 (km)
    
    private HealthConsideration(List<String> restPoints, List<AccessibilityType> accessibility, double walkingDistance) {
        this.restPoints = restPoints != null ? new ArrayList<>(restPoints) : new ArrayList<>();
        this.accessibility = accessibility != null ? new ArrayList<>(accessibility) : new ArrayList<>();
        this.walkingDistance = validateWalkingDistance(walkingDistance);
    }
    
    /**
     * 건강 고려사항 생성 팩토리 메서드
     */
    public static HealthConsideration of(List<String> restPoints, List<AccessibilityType> accessibility, double walkingDistance) {
        return new HealthConsideration(restPoints, accessibility, walkingDistance);
    }
    
    /**
     * 도보 거리만 있는 건강 고려사항 생성
     */
    public static HealthConsideration withWalkingDistance(double walkingDistance) {
        return new HealthConsideration(null, null, walkingDistance);
    }
    
    /**
     * 휴식 지점만 있는 건강 고려사항 생성
     */
    public static HealthConsideration withRestPoints(List<String> restPoints) {
        return new HealthConsideration(restPoints, null, 0);
    }
    
    /**
     * 접근성 시설만 있는 건강 고려사항 생성
     */
    public static HealthConsideration withAccessibility(List<AccessibilityType> accessibility) {
        return new HealthConsideration(null, accessibility, 0);
    }
    
    /**
     * 휴식 지점 추가
     */
    public HealthConsideration addRestPoint(String restPoint) {
        List<String> newRestPoints = new ArrayList<>(this.restPoints);
        newRestPoints.add(restPoint);
        return new HealthConsideration(newRestPoints, this.accessibility, this.walkingDistance);
    }
    
    /**
     * 접근성 시설 추가
     */
    public HealthConsideration addAccessibility(AccessibilityType accessibilityType) {
        List<AccessibilityType> newAccessibility = new ArrayList<>(this.accessibility);
        newAccessibility.add(accessibilityType);
        return new HealthConsideration(this.restPoints, newAccessibility, this.walkingDistance);
    }
    
    /**
     * 휴식 지점이 있는지 확인
     */
    public boolean hasRestPoints() {
        return !restPoints.isEmpty();
    }
    
    /**
     * 접근성 시설이 있는지 확인
     */
    public boolean hasAccessibility() {
        return !accessibility.isEmpty();
    }
    
    /**
     * 특정 접근성 시설이 있는지 확인
     */
    public boolean hasAccessibility(AccessibilityType type) {
        return accessibility.contains(type);
    }
    
    /**
     * 장거리 도보인지 확인 (2km 이상)
     */
    public boolean isLongWalk() {
        return walkingDistance >= 2.0;
    }
    
    /**
     * 건강 상태에 따른 적합성 확인
     */
    public boolean isSuitableFor(HealthStatus healthStatus) {
        switch (healthStatus) {
            case LIMITED:
                return walkingDistance <= 0.5 && hasRestPoints();
            case CAUTION:
                return walkingDistance <= 1.5;
            case GOOD:
            case EXCELLENT:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * 도보 거리 검증
     */
    private double validateWalkingDistance(double walkingDistance) {
        if (walkingDistance < 0) {
            throw new IllegalArgumentException("도보 거리는 0 이상이어야 합니다");
        }
        if (walkingDistance > 50) { // 50km
            throw new IllegalArgumentException("도보 거리는 50km를 초과할 수 없습니다");
        }
        return walkingDistance;
    }
    
    // Getters
    public List<String> getRestPoints() {
        return new ArrayList<>(restPoints);
    }
    
    public List<AccessibilityType> getAccessibility() {
        return new ArrayList<>(accessibility);
    }
    
    public double getWalkingDistance() {
        return walkingDistance;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HealthConsideration that = (HealthConsideration) o;
        return Double.compare(that.walkingDistance, walkingDistance) == 0 &&
               Objects.equals(restPoints, that.restPoints) &&
               Objects.equals(accessibility, that.accessibility);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(restPoints, accessibility, walkingDistance);
    }
    
    @Override
    public String toString() {
        return "HealthConsideration{" +
                "restPoints=" + restPoints +
                ", accessibility=" + accessibility +
                ", walkingDistance=" + walkingDistance +
                '}';
    }
}