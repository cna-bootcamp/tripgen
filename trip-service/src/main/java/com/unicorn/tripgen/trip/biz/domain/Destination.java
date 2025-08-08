package com.unicorn.tripgen.trip.biz.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * 여행지 도메인 엔티티
 */
@Entity
@Table(name = "destinations")
public class Destination {
    @Id
    @Column(name = "destination_id")
    private String destinationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;
    
    @Column(name = "trip_id", insertable = false, updatable = false)
    private String tripId;
    @Column(name = "destination_name", nullable = false, length = 20)
    private String destinationName;
    
    @Column(name = "nights", nullable = false)
    private int nights;
    
    @Column(name = "accommodation")
    private String accommodation;
    
    @Column(name = "check_in_time")
    private LocalTime checkInTime;
    
    @Column(name = "check_out_time")
    private LocalTime checkOutTime;
    
    @Column(name = "order_seq", nullable = false)
    private int order;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    // JPA 기본 생성자
    protected Destination() {}
    
    private Destination(String destinationId, String tripId, String destinationName, int nights) {
        this.destinationId = Objects.requireNonNull(destinationId, "Destination ID는 필수입니다");
        this.tripId = Objects.requireNonNull(tripId, "Trip ID는 필수입니다");
        this.destinationName = validateDestinationName(destinationName);
        this.nights = validateNights(nights);
    }
    
    /**
     * 새로운 여행지 생성 팩토리 메서드
     */
    public static Destination create(String destinationId, String tripId, String destinationName, 
                                   int nights, String accommodation, LocalTime checkInTime, 
                                   LocalTime checkOutTime, int order) {
        Destination destination = new Destination(destinationId, tripId, destinationName, nights);
        destination.accommodation = accommodation != null ? accommodation.trim() : null;
        destination.checkInTime = checkInTime;
        destination.checkOutTime = checkOutTime;
        destination.order = order;
        return destination;
    }
    
    /**
     * 기존 여행지 복원 팩토리 메서드
     */
    public static Destination restore(String destinationId, String tripId, String destinationName,
                                    int nights, String accommodation, LocalTime checkInTime,
                                    LocalTime checkOutTime, int order, LocalDate startDate, LocalDate endDate) {
        Destination destination = create(destinationId, tripId, destinationName, nights, 
                                       accommodation, checkInTime, checkOutTime, order);
        destination.startDate = startDate;
        destination.endDate = endDate;
        return destination;
    }
    
    /**
     * 여행지 정보 업데이트
     */
    public void updateInfo(String destinationName, int nights, String accommodation,
                          LocalTime checkInTime, LocalTime checkOutTime) {
        this.destinationName = validateDestinationName(destinationName);
        this.nights = validateNights(nights);
        this.accommodation = accommodation != null ? accommodation.trim() : null;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        calculateDates();
    }
    
    /**
     * 순서만 업데이트 (날짜 재계산 없이)
     */
    public void updateOrder(int order) {
        this.order = order;
    }
    
    /**
     * 순서 업데이트 및 날짜 재계산
     */
    public void updateOrder(int order, LocalDate baseDate) {
        this.order = order;
        calculateDatesFromBase(baseDate);
    }
    
    /**
     * 기준일로부터 날짜 계산
     */
    private void calculateDatesFromBase(LocalDate baseDate) {
        if (baseDate == null) return;
        
        // 이전 여행지들의 총 숙박일 계산이 필요하지만, 
        // 여기서는 단순히 order를 기준으로 계산
        LocalDate calculatedStartDate = baseDate.plusDays((long) order * nights);
        this.startDate = calculatedStartDate;
        this.endDate = calculatedStartDate.plusDays(nights);
    }
    
    /**
     * 날짜 계산 (내부적으로 사용)
     */
    private void calculateDates() {
        if (startDate != null) {
            this.endDate = startDate.plusDays(nights);
        }
    }
    
    /**
     * 날짜 설정
     */
    public void setDates(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    /**
     * 여행지명 검증
     */
    private String validateDestinationName(String destinationName) {
        Objects.requireNonNull(destinationName, "여행지명은 필수입니다");
        String trimmed = destinationName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("여행지명은 필수입니다");
        }
        if (trimmed.length() > 20) {
            throw new IllegalArgumentException("여행지명은 20자를 초과할 수 없습니다");
        }
        return trimmed;
    }
    
    /**
     * 숙박일 검증
     */
    private int validateNights(int nights) {
        if (nights < 1) {
            throw new IllegalArgumentException("숙박일은 1일 이상이어야 합니다");
        }
        if (nights > 30) {
            throw new IllegalArgumentException("숙박일은 30일을 초과할 수 없습니다");
        }
        return nights;
    }
    
    /**
     * 숙소 정보 존재 여부
     */
    public boolean hasAccommodation() {
        return accommodation != null && !accommodation.trim().isEmpty();
    }
    
    /**
     * 체크인/체크아웃 시간 존재 여부
     */
    public boolean hasCheckTimes() {
        return checkInTime != null && checkOutTime != null;
    }
    
    // Getters
    public String getDestinationId() {
        return destinationId;
    }
    
    public String getTripId() {
        return tripId;
    }
    
    public String getDestinationName() {
        return destinationName;
    }
    
    public int getNights() {
        return nights;
    }
    
    public String getAccommodation() {
        return accommodation;
    }
    
    public LocalTime getCheckInTime() {
        return checkInTime;
    }
    
    public LocalTime getCheckOutTime() {
        return checkOutTime;
    }
    
    public int getOrder() {
        return order;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public Trip getTrip() {
        return trip;
    }
    
    /**
     * Trip 참조 설정 (JPA 양방향 관계)
     */
    public void setTrip(Trip trip) {
        this.trip = trip;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Destination that = (Destination) o;
        return Objects.equals(destinationId, that.destinationId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(destinationId);
    }
    
    @Override
    public String toString() {
        return "Destination{" +
                "destinationId='" + destinationId + '\'' +
                ", destinationName='" + destinationName + '\'' +
                ", nights=" + nights +
                ", order=" + order +
                '}';
    }
}