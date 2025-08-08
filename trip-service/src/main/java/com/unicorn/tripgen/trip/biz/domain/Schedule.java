package com.unicorn.tripgen.trip.biz.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 일정 도메인 엔티티
 */
@Entity
@Table(name = "schedules")
public class Schedule {
    @Id
    @Column(name = "schedule_id")
    private String scheduleId;
    
    @Column(name = "trip_id", nullable = false)
    private String tripId;
    
    @Column(name = "day", nullable = false)
    private int day;
    
    @Column(name = "date", nullable = false)
    private LocalDate date;
    
    @Column(name = "city", nullable = false)
    private String city;
    
    @Embedded
    private WeatherInfo weather;
    
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SchedulePlace> places;
    
    // JPA 기본 생성자
    protected Schedule() {
        this.places = new ArrayList<>();
    }
    
    private Schedule(String scheduleId, String tripId, int day, LocalDate date, String city) {
        this.scheduleId = Objects.requireNonNull(scheduleId, "Schedule ID는 필수입니다");
        this.tripId = Objects.requireNonNull(tripId, "Trip ID는 필수입니다");
        this.day = validateDay(day);
        this.date = Objects.requireNonNull(date, "날짜는 필수입니다");
        this.city = validateCity(city);
        this.places = new ArrayList<>();
    }
    
    /**
     * 새로운 일정 생성 팩토리 메서드
     */
    public static Schedule create(String scheduleId, String tripId, int day, LocalDate date, 
                                String city, WeatherInfo weather) {
        Schedule schedule = new Schedule(scheduleId, tripId, day, date, city);
        schedule.weather = weather;
        return schedule;
    }
    
    /**
     * 기존 일정 복원 팩토리 메서드
     */
    public static Schedule restore(String scheduleId, String tripId, int day, LocalDate date,
                                 String city, WeatherInfo weather, List<SchedulePlace> places) {
        Schedule schedule = create(scheduleId, tripId, day, date, city, weather);
        if (places != null) {
            schedule.places.addAll(places);
        }
        return schedule;
    }
    
    /**
     * 장소 추가
     */
    public void addPlace(SchedulePlace place) {
        Objects.requireNonNull(place, "장소는 필수입니다");
        if (places.size() >= 20) {
            throw new IllegalStateException("하루 일정에는 최대 20개의 장소만 포함할 수 있습니다");
        }
        this.places.add(place);
    }
    
    /**
     * 장소 제거
     */
    public void removePlace(String placeId) {
        places.removeIf(place -> place.getPlaceId().equals(placeId));
    }
    
    /**
     * 장소 순서 업데이트
     */
    public void updatePlaceOrder(String placeId, int newOrder) {
        places.stream()
               .filter(place -> place.getPlaceId().equals(placeId))
               .findFirst()
               .ifPresent(place -> place.updateOrder(newOrder));
        
        // 순서 재정렬
        places.sort((a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
    }
    
    /**
     * 날씨 정보 업데이트
     */
    public void updateWeather(WeatherInfo weather) {
        this.weather = weather;
    }
    
    /**
     * 장소 목록 일괄 업데이트
     */
    public void updatePlaces(List<SchedulePlace> newPlaces) {
        Objects.requireNonNull(newPlaces, "장소 목록은 필수입니다");
        if (newPlaces.size() > 20) {
            throw new IllegalStateException("하루 일정에는 최대 20개의 장소만 포함할 수 있습니다");
        }
        
        this.places.clear();
        this.places.addAll(newPlaces);
    }
    
    /**
     * 총 예상 소요 시간 계산 (분)
     */
    public int getTotalDuration() {
        return places.stream()
                    .mapToInt(SchedulePlace::getDuration)
                    .sum();
    }
    
    /**
     * 총 이동 시간 계산 (분)
     */
    public int getTotalTravelTime() {
        return places.stream()
                    .filter(place -> place.getTransportation() != null)
                    .mapToInt(place -> place.getTransportation().getDuration())
                    .sum();
    }
    
    /**
     * 특정 순서의 장소 조회
     */
    public SchedulePlace getPlaceByOrder(int order) {
        return places.stream()
                    .filter(place -> place.getOrder() == order)
                    .findFirst()
                    .orElse(null);
    }
    
    /**
     * 일차 검증
     */
    private int validateDay(int day) {
        if (day < 1) {
            throw new IllegalArgumentException("일차는 1 이상이어야 합니다");
        }
        return day;
    }
    
    /**
     * 도시명 검증
     */
    private String validateCity(String city) {
        Objects.requireNonNull(city, "도시명은 필수입니다");
        String trimmed = city.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("도시명은 필수입니다");
        }
        return trimmed;
    }
    
    // Getters
    public String getScheduleId() {
        return scheduleId;
    }
    
    public String getTripId() {
        return tripId;
    }
    
    public int getDay() {
        return day;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public String getCity() {
        return city;
    }
    
    public WeatherInfo getWeather() {
        return weather;
    }
    
    public List<SchedulePlace> getPlaces() {
        return new ArrayList<>(places);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schedule schedule = (Schedule) o;
        return Objects.equals(scheduleId, schedule.scheduleId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(scheduleId);
    }
    
    @Override
    public String toString() {
        return "Schedule{" +
                "scheduleId='" + scheduleId + '\'' +
                ", day=" + day +
                ", date=" + date +
                ", city='" + city + '\'' +
                ", placesCount=" + places.size() +
                '}';
    }
}