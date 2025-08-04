package com.unicorn.tripgen.trip.biz.service;

import com.unicorn.tripgen.trip.biz.domain.*;
import com.unicorn.tripgen.trip.biz.usecase.out.AiServiceClient;
import com.unicorn.tripgen.trip.biz.usecase.out.LocationServiceClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 일정 생성 서비스
 * AI 서비스와 연동하여 여행 일정을 생성하는 도메인 서비스
 */
@Service
public class ScheduleGenerationService {
    
    private final AiServiceClient aiServiceClient;
    private final LocationServiceClient locationServiceClient;
    
    public ScheduleGenerationService(AiServiceClient aiServiceClient, LocationServiceClient locationServiceClient) {
        this.aiServiceClient = aiServiceClient;
        this.locationServiceClient = locationServiceClient;
    }
    
    /**
     * AI 일정 생성 요청
     */
    public AiServiceClient.ScheduleGenerationResponse requestScheduleGeneration(
            Trip trip, LocalTime startTime, String specialRequests) {
        
        // 여행 정보를 AI 서비스 요청 형태로 변환
        AiServiceClient.ScheduleGenerationRequest request = new AiServiceClient.ScheduleGenerationRequest(
            trip.getTripId(),
            trip.getUserId(),
            extractDestinationNames(trip.getDestinations()),
            convertMemberProfiles(trip.getMembers()),
            trip.getTransportMode().name().toLowerCase(),
            startTime,
            specialRequests
        );
        
        return aiServiceClient.requestScheduleGeneration(request);
    }
    
    /**
     * 생성된 일정을 도메인 객체로 변환
     */
    public List<Schedule> convertToSchedules(String tripId, List<AiServiceClient.GeneratedSchedule> generatedSchedules) {
        List<Schedule> schedules = new ArrayList<>();
        
        for (AiServiceClient.GeneratedSchedule generated : generatedSchedules) {
            String scheduleId = generateScheduleId(tripId, generated.day());
            LocalDate date = LocalDate.parse(generated.date());
            
            // 날씨 정보 변환
            WeatherInfo weatherInfo = convertWeatherInfo(generated.weather());
            
            // 일정 생성
            Schedule schedule = Schedule.create(scheduleId, tripId, generated.day(), date, generated.city(), weatherInfo);
            
            // 장소 정보 변환 및 추가
            for (AiServiceClient.SchedulePlace aiPlace : generated.places()) {
                SchedulePlace place = convertSchedulePlace(scheduleId, aiPlace);
                schedule.addPlace(place);
            }
            
            schedules.add(schedule);
        }
        
        return schedules;
    }
    
    /**
     * 일자별 일정 재생성 요청
     */
    public AiServiceClient.ScheduleGenerationResponse regenerateDaySchedule(
            String tripId, int day, String specialRequests) {
        
        return aiServiceClient.regenerateDaySchedule(tripId, day, specialRequests);
    }
    
    /**
     * 장소 추천 정보 요청
     */
    public AiServiceClient.ScheduleGenerationResponse requestPlaceRecommendation(
            Trip trip, String placeId, String placeName, Integer day, String previousPlace, String nextPlace) {
        
        AiServiceClient.RecommendationRequest request = new AiServiceClient.RecommendationRequest(
            trip.getTripId(),
            placeId,
            placeName,
            convertMemberProfiles(trip.getMembers()),
            new AiServiceClient.ContextInfo(day, previousPlace, nextPlace, getCurrentCity(trip, day))
        );
        
        return aiServiceClient.requestPlaceRecommendation(request);
    }
    
    /**
     * 여행지 이름 목록 추출
     */
    private List<String> extractDestinationNames(List<Destination> destinations) {
        return destinations.stream()
                          .map(Destination::getDestinationName)
                          .collect(Collectors.toList());
    }
    
    /**
     * 멤버 프로필 변환
     */
    private List<AiServiceClient.MemberProfile> convertMemberProfiles(List<Member> members) {
        return members.stream()
                     .map(member -> new AiServiceClient.MemberProfile(
                         member.getName(),
                         member.getAge(),
                         member.getGender().name().toLowerCase(),
                         member.getHealthStatus().name().toLowerCase(),
                         member.getPreferences().stream()
                               .map(pref -> pref.name().toLowerCase())
                               .collect(Collectors.toList())
                     ))
                     .collect(Collectors.toList());
    }
    
    /**
     * 날씨 정보 변환
     */
    private WeatherInfo convertWeatherInfo(AiServiceClient.WeatherInfo aiWeather) {
        if (aiWeather == null) {
            return null;
        }
        return WeatherInfo.of(
            aiWeather.condition(),
            aiWeather.minTemperature(),
            aiWeather.maxTemperature(),
            aiWeather.icon()
        );
    }
    
    /**
     * 일정 장소 변환
     */
    private SchedulePlace convertSchedulePlace(String scheduleId, AiServiceClient.SchedulePlace aiPlace) {
        LocalTime startTime = LocalTime.parse(aiPlace.startTime());
        
        // 교통 정보 변환
        Transportation transportation = null;
        if (aiPlace.transportation() != null) {
            AiServiceClient.TransportationInfo transInfo = aiPlace.transportation();
            Transportation.Type type = Transportation.Type.valueOf(transInfo.type().toUpperCase());
            transportation = Transportation.of(type, transInfo.duration(), transInfo.distance(), transInfo.route());
        }
        
        // 건강 고려사항 변환
        HealthConsideration healthConsideration = null;
        if (aiPlace.healthConsideration() != null) {
            AiServiceClient.HealthConsiderationInfo healthInfo = aiPlace.healthConsideration();
            List<HealthConsideration.AccessibilityType> accessibility = healthInfo.accessibility().stream()
                .map(acc -> HealthConsideration.AccessibilityType.valueOf(acc.toUpperCase()))
                .collect(Collectors.toList());
            
            healthConsideration = HealthConsideration.of(
                healthInfo.restPoints(),
                accessibility,
                healthInfo.walkingDistance()
            );
        }
        
        return SchedulePlace.create(
            aiPlace.placeId(),
            scheduleId,
            aiPlace.placeName(),
            aiPlace.category(),
            startTime,
            aiPlace.duration(),
            transportation,
            healthConsideration,
            aiPlace.order()
        );
    }
    
    /**
     * 현재 도시 정보 조회
     */
    private String getCurrentCity(Trip trip, Integer day) {
        if (day == null || trip.getDestinations().isEmpty()) {
            return null;
        }
        
        // 일차에 해당하는 도시 찾기 (간단한 로직)
        // 실제로는 더 복잡한 매핑이 필요할 수 있음
        int dayIndex = Math.min(day - 1, trip.getDestinations().size() - 1);
        return trip.getDestinations().get(dayIndex).getDestinationName();
    }
    
    /**
     * 일정 ID 생성
     */
    private String generateScheduleId(String tripId, int day) {
        return String.format("%s_day_%02d", tripId, day);
    }
}