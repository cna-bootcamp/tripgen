package com.unicorn.tripgen.trip.biz.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 여행 도메인 엔티티
 * Clean Architecture의 Domain Layer에 속하며 핵심 비즈니스 규칙을 포함
 */
@Entity
@Table(name = "trips")
public class Trip {
    @Id
    @Column(name = "trip_id")
    private String tripId;
    
    @Column(name = "trip_name", nullable = false, length = 16)
    private String tripName;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", nullable = false)
    private TransportMode transportMode;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TripStatus status;
    
    @Column(name = "current_step")
    private String currentStep;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Member> members;
    
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Destination> destinations;
    
    @Column(name = "has_schedule")
    private boolean hasSchedule;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // JPA 기본 생성자
    protected Trip() {}
    
    // Private constructor for domain integrity
    private Trip(String tripId, String tripName, String userId, TransportMode transportMode) {
        this.tripId = Objects.requireNonNull(tripId, "Trip ID는 필수입니다");
        this.tripName = validateTripName(tripName);
        this.userId = Objects.requireNonNull(userId, "User ID는 필수입니다");
        this.transportMode = Objects.requireNonNull(transportMode, "교통수단은 필수입니다");
        this.status = TripStatus.PLANNING;
        this.currentStep = "기본설정";
        this.members = new ArrayList<>();
        this.destinations = new ArrayList<>();
        this.hasSchedule = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 새로운 여행 생성 팩토리 메서드
     */
    public static Trip create(String tripId, String tripName, String userId, TransportMode transportMode) {
        return new Trip(tripId, tripName, userId, transportMode);
    }
    
    /**
     * 기존 여행 복원 팩토리 메서드 (Repository에서 사용)
     */
    public static Trip restore(String tripId, String tripName, String userId, TransportMode transportMode,
                              TripStatus status, String currentStep, LocalDate startDate, LocalDate endDate,
                              boolean hasSchedule, LocalDateTime createdAt, LocalDateTime updatedAt) {
        Trip trip = new Trip(tripId, tripName, userId, transportMode);
        trip.status = status;
        trip.currentStep = currentStep;
        trip.startDate = startDate;
        trip.endDate = endDate;
        trip.hasSchedule = hasSchedule;
        trip.updatedAt = updatedAt;
        return trip;
    }
    
    /**
     * 여행 기본 정보 업데이트
     */
    public void updateBasicInfo(String tripName, TransportMode transportMode) {
        this.tripName = validateTripName(tripName);
        this.transportMode = Objects.requireNonNull(transportMode, "교통수단은 필수입니다");
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 여행 설명 설정
     */
    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 여행 일정 설정
     */
    public void setTravelDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다");
        }
        this.startDate = startDate;
        this.endDate = endDate;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 멤버 추가
     */
    public void addMember(Member member) {
        Objects.requireNonNull(member, "멤버는 필수입니다");
        if (members.size() >= 10) {
            throw new IllegalStateException("여행 멤버는 최대 10명까지 가능합니다");
        }
        this.members.add(member);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 멤버 제거
     */
    public void removeMember(String memberId) {
        boolean removed = members.removeIf(member -> member.getMemberId().equals(memberId));
        if (removed) {
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 여행지 추가 (JPA 양방향 관계 설정)
     */
    public void addDestination(Destination destination) {
        Objects.requireNonNull(destination, "여행지는 필수입니다");
        if (destinations.size() >= 10) {
            throw new IllegalStateException("여행지는 최대 10개까지 가능합니다");
        }
        this.destinations.add(destination);
        // JPA 양방향 관계 설정 - 매우 중요!
        destination.setTrip(this);
        updateTravelDates();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 여행지 제거 (JPA 양방향 관계 해제)
     */
    public void removeDestination(String destinationId) {
        boolean removed = destinations.removeIf(dest -> {
            if (dest.getDestinationId().equals(destinationId)) {
                // JPA 양방향 관계 해제
                dest.setTrip(null);
                return true;
            }
            return false;
        });
        if (removed) {
            updateTravelDates();
            this.updatedAt = LocalDateTime.now();
        }
    }
    
    /**
     * 여행지 일괄 업데이트 (JPA 양방향 관계 설정) - 날짜 계산은 Service Layer에서 처리
     */
    public void updateDestinations(List<Destination> newDestinations) {
        Objects.requireNonNull(newDestinations, "여행지 목록은 필수입니다");
        if (newDestinations.size() > 10) {
            throw new IllegalStateException("여행지는 최대 10개까지 가능합니다");
        }
        
        // 기존 여행지들의 관계 해제
        for (Destination dest : this.destinations) {
            dest.setTrip(null);
        }
        
        this.destinations.clear();
        
        // 새 여행지들의 관계 설정
        for (Destination dest : newDestinations) {
            dest.setTrip(this);
        }
        
        this.destinations.addAll(newDestinations);
        
        // 일괄 업데이트에서는 날짜 계산을 Service Layer에 위임
        // updateTravelDates()를 호출하지 않음 (recalculateDestinationDates에서 처리)
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 일정 생성 완료 처리
     */
    public void markScheduleCreated() {
        this.hasSchedule = true;
        this.currentStep = "일정생성완료";
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 여행 상태 변경
     */
    public void changeStatus(TripStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus, "여행 상태는 필수입니다");
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 여행 시작/종료일 자동 계산
     */
    private void updateTravelDates() {
        if (destinations.isEmpty()) {
            this.startDate = null;
            this.endDate = null;
            return;
        }
        
        // 첫 번째 여행지의 시작일을 여행 시작일로 설정
        this.startDate = destinations.get(0).getStartDate();
        
        // 마지막 여행지의 종료일을 여행 종료일로 설정
        Destination lastDestination = destinations.get(destinations.size() - 1);
        this.endDate = lastDestination.getEndDate();
    }
    
    /**
     * 총 여행 일수 계산
     */
    public int getTotalDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1);
    }
    
    /**
     * 여행명 검증
     */
    private String validateTripName(String tripName) {
        Objects.requireNonNull(tripName, "여행명은 필수입니다");
        String trimmed = tripName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("여행명은 필수입니다");
        }
        if (trimmed.length() > 16) {
            throw new IllegalArgumentException("여행명은 16자를 초과할 수 없습니다");
        }
        return trimmed;
    }
    
    // Getters
    public String getTripId() {
        return tripId;
    }
    
    public String getTripName() {
        return tripName;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public TransportMode getTransportMode() {
        return transportMode;
    }
    
    public TripStatus getStatus() {
        return status;
    }
    
    public String getCurrentStep() {
        return currentStep;
    }
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public List<Member> getMembers() {
        return new ArrayList<>(members);
    }
    
    public List<Destination> getDestinations() {
        return new ArrayList<>(destinations);
    }
    
    public boolean hasSchedule() {
        return hasSchedule;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trip trip = (Trip) o;
        return Objects.equals(tripId, trip.tripId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(tripId);
    }
    
    @Override
    public String toString() {
        return "Trip{" +
                "tripId='" + tripId + '\'' +
                ", tripName='" + tripName + '\'' +
                ", status=" + status +
                ", currentStep='" + currentStep + '\'' +
                '}';
    }
}