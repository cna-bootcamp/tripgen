package com.unicorn.tripgen.trip.infra.config;

import com.unicorn.tripgen.trip.biz.service.ScheduleExportService;
import com.unicorn.tripgen.trip.biz.service.ScheduleGenerationService;
import com.unicorn.tripgen.trip.biz.usecase.out.AiServiceClient;
import com.unicorn.tripgen.trip.biz.usecase.out.LocationServiceClient;
import com.unicorn.tripgen.trip.biz.validator.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Trip Service 설정 클래스
 * Clean Architecture의 Infrastructure Layer 설정
 */
@Configuration
@EnableConfigurationProperties({TripConfiguration.TripProperties.class})
public class TripConfiguration {
    
    /**
     * Trip Service 속성 설정
     */
    @ConfigurationProperties(prefix = "trip.service")
    public record TripProperties(
        int maxMembersPerTrip,
        int maxDestinationsPerTrip,
        int maxTripDurationDays,
        int maxPlacesPerDay,
        int maxScheduleGenerationRetries,
        int scheduleGenerationTimeout,
        ExportProperties export,
        CacheProperties cache
    ) {
        public record ExportProperties(
            long maxFileSize,
            String[] supportedFormats,
            String tempDirectory
        ) {}
        
        public record CacheProperties(
            long scheduleTtl,
            long recommendationTtl,
            long tripListTtl
        ) {}
    }
    
    /**
     * 일정 생성 서비스 빈 등록
     */
    @Bean
    public ScheduleGenerationService scheduleGenerationService(
            AiServiceClient aiServiceClient,
            LocationServiceClient locationServiceClient) {
        return new ScheduleGenerationService(aiServiceClient, locationServiceClient);
    }
    
    /**
     * 일정 내보내기 서비스 빈 등록
     */
    @Bean  
    public ScheduleExportService scheduleExportService() {
        return new ScheduleExportService();
    }
    
    /**
     * 여행 검증기 빈 등록
     */
    @Bean
    public TripValidator tripValidator() {
        return new TripValidator();
    }
    
    /**
     * 멤버 검증기 빈 등록
     */
    @Bean
    public MemberValidator memberValidator() {
        return new MemberValidator();
    }
    
    /**
     * 여행지 검증기 빈 등록
     */
    @Bean
    public DestinationValidator destinationValidator() {
        return new DestinationValidator();
    }
    
    /**
     * 일정 검증기 빈 등록
     */
    @Bean
    public ScheduleValidator scheduleValidator() {
        return new ScheduleValidator();
    }
    
    /**
     * 개발 환경 전용 설정
     */
    @Configuration
    @Profile("dev")
    static class DevConfiguration {
        // 개발 환경 전용 빈들...
    }
    
    /**
     * 테스트 환경 전용 설정
     */
    @Configuration
    @Profile("test")
    static class TestConfiguration {
        // 테스트 환경 전용 빈들...
    }
    
    /**
     * 운영 환경 전용 설정
     */
    @Configuration
    @Profile("prod")
    static class ProdConfiguration {
        // 운영 환경 전용 빈들...
    }
}