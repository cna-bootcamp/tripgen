package com.unicorn.tripgen.trip.biz.domain;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * 교통 정보 값 객체 (Value Object)
 */
@Embeddable
public class Transportation {
    /**
     * 교통수단 유형
     */
    public enum Type {
        WALK("도보"),
        CAR("자동차"),
        PUBLIC("대중교통");
        
        private final String description;
        
        Type(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type")
    private Type type;
    
    @Column(name = "transport_duration")
    private int duration; // 이동 시간 (분)
    
    @Column(name = "transport_distance")
    private double distance; // 거리 (km)
    
    @Column(name = "transport_route")
    private String route; // 경로 설명
    
    // JPA 기본 생성자
    protected Transportation() {}
    
    private Transportation(Type type, int duration, double distance, String route) {
        this.type = Objects.requireNonNull(type, "교통수단 유형은 필수입니다");
        this.duration = validateDuration(duration);
        this.distance = validateDistance(distance);
        this.route = route;
    }
    
    /**
     * 교통 정보 생성 팩토리 메서드
     */
    public static Transportation of(Type type, int duration, double distance, String route) {
        return new Transportation(type, duration, distance, route);
    }
    
    /**
     * 경로 설명 없는 교통 정보 생성
     */
    public static Transportation of(Type type, int duration, double distance) {
        return new Transportation(type, duration, distance, null);
    }
    
    /**
     * 도보 이동 정보 생성
     */
    public static Transportation walking(int duration, double distance) {
        return of(Type.WALK, duration, distance);
    }
    
    /**
     * 자동차 이동 정보 생성
     */
    public static Transportation driving(int duration, double distance, String route) {
        return of(Type.CAR, duration, distance, route);
    }
    
    /**
     * 대중교통 이동 정보 생성
     */
    public static Transportation publicTransport(int duration, double distance, String route) {
        return of(Type.PUBLIC, duration, distance, route);
    }
    
    /**
     * 평균 속도 계산 (km/h)
     */
    public double getAverageSpeed() {
        if (duration == 0) {
            return 0;
        }
        return distance / (duration / 60.0);
    }
    
    /**
     * 도보 이동인지 확인
     */
    public boolean isWalking() {
        return type == Type.WALK;
    }
    
    /**
     * 장거리 이동인지 확인 (5km 이상)
     */
    public boolean isLongDistance() {
        return distance >= 5.0;
    }
    
    /**
     * 이동 시간 검증
     */
    private int validateDuration(int duration) {
        if (duration < 0) {
            throw new IllegalArgumentException("이동 시간은 0 이상이어야 합니다");
        }
        if (duration > 480) { // 8시간
            throw new IllegalArgumentException("이동 시간은 8시간을 초과할 수 없습니다");
        }
        return duration;
    }
    
    /**
     * 거리 검증
     */
    private double validateDistance(double distance) {
        if (distance < 0) {
            throw new IllegalArgumentException("거리는 0 이상이어야 합니다");
        }
        if (distance > 1000) { // 1000km
            throw new IllegalArgumentException("거리는 1000km를 초과할 수 없습니다");
        }
        return distance;
    }
    
    // Getters
    public Type getType() {
        return type;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public double getDistance() {
        return distance;
    }
    
    public String getRoute() {
        return route;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transportation that = (Transportation) o;
        return duration == that.duration &&
               Double.compare(that.distance, distance) == 0 &&
               type == that.type &&
               Objects.equals(route, that.route);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, duration, distance, route);
    }
    
    @Override
    public String toString() {
        return "Transportation{" +
                "type=" + type +
                ", duration=" + duration +
                ", distance=" + distance +
                ", route='" + route + '\'' +
                '}';
    }
}