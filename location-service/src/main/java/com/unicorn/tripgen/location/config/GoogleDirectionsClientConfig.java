package com.unicorn.tripgen.location.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Google Directions API 클라이언트 설정
 */
@Configuration
@EnableFeignClients(basePackages = "com.unicorn.tripgen.location.client")
public class GoogleDirectionsClientConfig {

    /**
     * Feign 로그 레벨 설정
     */
    @Bean
    public Logger.Level googleDirectionsLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * 타임아웃 설정 (Google API는 안정적이므로 적절한 타임아웃 설정)
     */
    @Bean
    public Request.Options googleDirectionsRequestOptions() {
        return new Request.Options(
                10, TimeUnit.SECONDS,  // 연결 타임아웃
                30, TimeUnit.SECONDS,  // 읽기 타임아웃
                true
        );
    }

    /**
     * 재시도 설정
     */
    @Bean
    public Retryer googleDirectionsRetryer() {
        return new Retryer.Default(
                100,        // 재시도 간격 (ms)
                1000,       // 최대 재시도 간격 (ms)
                3           // 최대 재시도 횟수
        );
    }
}