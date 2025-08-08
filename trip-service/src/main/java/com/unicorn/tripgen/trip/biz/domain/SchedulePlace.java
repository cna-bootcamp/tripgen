package com.unicorn.tripgen.trip.biz.domain;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 일정 내 장소 도메인 엔티티
 */
@Entity
@Table(name = "schedule_places")
public class SchedulePlace {
    @Id
    @Column(name = "place_id")
    private String placeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;
    
    @Column(name = "schedule_id", insertable = false, updatable = false)
    private String scheduleId;
    
    @Column(name = "place_name", nullable = false)
    private String placeName;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "start_time")
    private LocalTime startTime;
    
    @Column(name = "duration", nullable = false)
    private int duration; // 소요 시간 (분)
    
    @Embedded
    private Transportation transportation;
    
    @Embedded
    private HealthConsideration healthConsideration;
    
    @Column(name = "order_seq", nullable = false)
    private int order;
    
    // JPA 기본 생성자
    protected SchedulePlace() {}
    
    private SchedulePlace(String placeId, String scheduleId, String placeName, String category) {
        this.placeId = Objects.requireNonNull(placeId, "Place ID는 필수입니다");
        this.scheduleId = Objects.requireNonNull(scheduleId, "Schedule ID는 필수입니다");
        this.placeName = validatePlaceName(placeName);
        this.category = category;
    }
    
    /**
     * 새로운 일정 장소 생성 팩토리 메서드
     */
    public static SchedulePlace create(String placeId, String scheduleId, String placeName,
                                     String category, LocalTime startTime, int duration,
                                     Transportation transportation, HealthConsideration healthConsideration,
                                     int order) {
        SchedulePlace place = new SchedulePlace(placeId, scheduleId, placeName, category);
        place.startTime = startTime;
        place.duration = validateDuration(duration);
        place.transportation = transportation;
        place.healthConsideration = healthConsideration;
        place.order = order;
        return place;
    }
    
    /**
     * 기존 일정 장소 복원 팩토리 메서드
     */
    public static SchedulePlace restore(String placeId, String scheduleId, String placeName,
                                      String category, LocalTime startTime, int duration,
                                      Transportation transportation, HealthConsideration healthConsideration,
                                      int order) {
        return create(placeId, scheduleId, placeName, category, startTime, duration,
                     transportation, healthConsideration, order);
    }
    
    /**
     * 시작 시간 업데이트
     */
    public void updateStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }
    
    /**
     * 소요 시간 업데이트
     */
    public void updateDuration(int duration) {
        this.duration = validateDuration(duration);
    }
    
    /**
     * 순서 업데이트
     */
    public void updateOrder(int order) {
        this.order = order;
    }
    
    /**
     * 교통 정보 업데이트
     */
    public void updateTransportation(Transportation transportation) {
        this.transportation = transportation;
    }
    
    /**
     * 건강 고려사항 업데이트
     */
    public void updateHealthConsideration(HealthConsideration healthConsideration) {
        this.healthConsideration = healthConsideration;
    }
    
    /**
     * 종료 시간 계산
     */
    public LocalTime getEndTime() {
        if (startTime == null) {
            return null;
        }
        return startTime.plusMinutes(duration);
    }
    
    /**
     * 이동 시간이 있는지 확인
     */
    public boolean hasTransportation() {
        return transportation != null;
    }
    
    /**
     * 건강 고려사항이 있는지 확인
     */
    public boolean hasHealthConsideration() {
        return healthConsideration != null;
    }
    
    /**
     * 장소명 검증
     */
    private String validatePlaceName(String placeName) {
        Objects.requireNonNull(placeName, "장소명은 필수입니다");
        String trimmed = placeName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("장소명은 필수입니다");
        }
        return trimmed;
    }
    
    /**
     * 소요 시간 검증
     */
    private static int validateDuration(int duration) {
        if (duration < 1) {
            throw new IllegalArgumentException("소요 시간은 1분 이상이어야 합니다");
        }
        if (duration > 720) { // 12시간
            throw new IllegalArgumentException("소요 시간은 12시간을 초과할 수 없습니다");
        }
        return duration;
    }
    
    // Getters
    public String getPlaceId() {
        return placeId;
    }
    
    public String getScheduleId() {
        return scheduleId;
    }
    
    public String getPlaceName() {
        return placeName;
    }
    
    public String getCategory() {
        return category;
    }
    
    public LocalTime getStartTime() {
        return startTime;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public Transportation getTransportation() {
        return transportation;
    }
    
    public HealthConsideration getHealthConsideration() {
        return healthConsideration;
    }
    
    public int getOrder() {
        return order;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchedulePlace that = (SchedulePlace) o;
        return Objects.equals(placeId, that.placeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(placeId);
    }
    
    @Override
    public String toString() {
        return "SchedulePlace{" +
                "placeId='" + placeId + '\'' +
                ", placeName='" + placeName + '\'' +
                ", startTime=" + startTime +
                ", duration=" + duration +
                ", order=" + order +
                '}';
    }
}