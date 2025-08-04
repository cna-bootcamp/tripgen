package com.unicorn.tripgen.trip.biz.service;

import com.unicorn.tripgen.trip.biz.domain.Schedule;
import com.unicorn.tripgen.trip.biz.domain.SchedulePlace;
import com.unicorn.tripgen.trip.biz.domain.Trip;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 일정 내보내기 서비스
 * 여행 일정을 다양한 형식으로 내보내는 도메인 서비스
 */
@Service
public class ScheduleExportService {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    /**
     * 일정을 PDF 형식으로 내보내기
     */
    public byte[] exportToPdf(Trip trip, List<Schedule> schedules, boolean includeMap, List<Integer> days) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // 필터링된 일정
            List<Schedule> filteredSchedules = filterSchedulesByDays(schedules, days);
            
            // PDF 생성 로직 (실제 구현에서는 iText, Apache PDFBox 등 사용)
            String pdfContent = generatePdfContent(trip, filteredSchedules, includeMap);
            outputStream.write(pdfContent.getBytes("UTF-8"));
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("PDF 생성 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 일정을 이미지 형식으로 내보내기
     */
    public byte[] exportToImage(Trip trip, List<Schedule> schedules, boolean includeMap, List<Integer> days) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // 필터링된 일정
            List<Schedule> filteredSchedules = filterSchedulesByDays(schedules, days);
            
            // 이미지 생성 로직 (실제 구현에서는 BufferedImage, Graphics2D 등 사용)
            String imageContent = generateImageContent(trip, filteredSchedules, includeMap);
            outputStream.write(imageContent.getBytes("UTF-8"));
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("이미지 생성 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 파일명 생성
     */
    public String generateFilename(Trip trip, String format, List<Integer> days) {
        String tripName = sanitizeFilename(trip.getTripName());
        String dateRange = "";
        
        if (trip.getStartDate() != null && trip.getEndDate() != null) {
            dateRange = "_" + trip.getStartDate().format(DATE_FORMATTER) + 
                       "_to_" + trip.getEndDate().format(DATE_FORMATTER);
        }
        
        String daysSuffix = "";
        if (days != null && !days.isEmpty()) {
            daysSuffix = "_days_" + days.stream()
                                       .map(String::valueOf)
                                       .collect(Collectors.joining("_"));
        }
        
        return String.format("%s%s%s.%s", tripName, dateRange, daysSuffix, format.toLowerCase());
    }
    
    /**
     * 일차별 필터링
     */
    private List<Schedule> filterSchedulesByDays(List<Schedule> schedules, List<Integer> days) {
        if (days == null || days.isEmpty()) {
            return schedules;
        }
        
        return schedules.stream()
                       .filter(schedule -> days.contains(schedule.getDay()))
                       .collect(Collectors.toList());
    }
    
    /**
     * PDF 콘텐츠 생성 (간단한 텍스트 기반)
     */
    private String generatePdfContent(Trip trip, List<Schedule> schedules, boolean includeMap) {
        StringBuilder content = new StringBuilder();
        
        // 헤더
        content.append("=================================\n");
        content.append("        여행 일정표\n");
        content.append("=================================\n\n");
        
        // 여행 기본 정보
        content.append("여행명: ").append(trip.getTripName()).append("\n");
        content.append("기간: ").append(trip.getStartDate().format(DATE_FORMATTER))
               .append(" ~ ").append(trip.getEndDate().format(DATE_FORMATTER)).append("\n");
        content.append("이동수단: ").append(trip.getTransportMode().getDescription()).append("\n");
        content.append("멤버 수: ").append(trip.getMembers().size()).append("명\n\n");
        
        // 일정 상세
        for (Schedule schedule : schedules) {
            content.append("---------------------------------\n");
            content.append(String.format("Day %d (%s) - %s\n", 
                schedule.getDay(), 
                schedule.getDate().format(DATE_FORMATTER),
                schedule.getCity()));
            
            if (schedule.getWeather() != null) {
                content.append(String.format("날씨: %s (%.1f°C ~ %.1f°C)\n",
                    schedule.getWeather().getCondition(),
                    schedule.getWeather().getMinTemperature(),
                    schedule.getWeather().getMaxTemperature()));
            }
            content.append("---------------------------------\n");
            
            // 장소별 일정
            for (SchedulePlace place : schedule.getPlaces()) {
                content.append(String.format("%d. %s\n", place.getOrder(), place.getPlaceName()));
                content.append(String.format("   시간: %s (%d분)\n", 
                    place.getStartTime().format(TIME_FORMATTER), place.getDuration()));
                
                if (place.getCategory() != null) {
                    content.append(String.format("   카테고리: %s\n", place.getCategory()));
                }
                
                if (place.hasTransportation()) {
                    content.append(String.format("   이동: %s (%d분, %.1fkm)\n",
                        place.getTransportation().getType().getDescription(),
                        place.getTransportation().getDuration(),
                        place.getTransportation().getDistance()));
                }
                content.append("\n");
            }
            content.append("\n");
        }
        
        if (includeMap) {
            content.append("---------------------------------\n");
            content.append("* 지도는 PDF 버전에서만 제공됩니다.\n");
            content.append("---------------------------------\n");
        }
        
        return content.toString();
    }
    
    /**
     * 이미지 콘텐츠 생성 (간단한 텍스트 기반)
     */
    private String generateImageContent(Trip trip, List<Schedule> schedules, boolean includeMap) {
        // 실제로는 이미지 라이브러리를 사용하여 시각적인 이미지 생성
        // 여기서는 텍스트 기반으로 간단히 구현
        return generatePdfContent(trip, schedules, includeMap);
    }
    
    /**
     * 파일명 안전화 (특수문자 제거)
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9가-힣\\-_]", "_");
    }
    
    /**
     * 일정 요약 통계 생성
     */
    public ScheduleSummary generateSummary(Trip trip, List<Schedule> schedules) {
        int totalPlaces = schedules.stream()
                                  .mapToInt(schedule -> schedule.getPlaces().size())
                                  .sum();
        
        int totalDuration = schedules.stream()
                                   .mapToInt(schedule -> schedule.getTotalDuration())
                                   .sum();
        
        int totalTravelTime = schedules.stream()
                                     .mapToInt(schedule -> schedule.getTotalTravelTime())
                                     .sum();
        
        return new ScheduleSummary(
            trip.getTotalDays(),
            totalPlaces,
            totalDuration,
            totalTravelTime,
            trip.getDestinations().size()
        );
    }
    
    /**
     * 일정 요약 정보
     */
    public record ScheduleSummary(
        int totalDays,
        int totalPlaces,
        int totalDuration,
        int totalTravelTime,
        int totalDestinations
    ) {}
}