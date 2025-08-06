package com.unicorn.tripgen.location.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 외부 API 클라이언트 설정
 */
@Configuration
@EnableFeignClients(basePackages = "com.unicorn.tripgen.location.client")
public class ExternalApiConfig {
    
    @Value("${external.api.timeout.connect:5000}")
    private int connectTimeout;
    
    @Value("${external.api.timeout.read:30000}")
    private int readTimeout;
    
    @Value("${external.api.retry.max-attempts:3}")
    private int maxAttempts;
    
    @Value("${external.api.retry.period:1000}")
    private long retryPeriod;
    
    @Value("${external.api.retry.max-period:5000}")
    private long maxRetryPeriod;
    
    /**
     * Feign 요청 옵션 설정
     */
    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(
            connectTimeout, TimeUnit.MILLISECONDS,
            readTimeout, TimeUnit.MILLISECONDS,
            true
        );
    }
    
    /**
     * Feign 재시도 설정
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public Retryer feignRetryer() {
        return new Retryer.Default(retryPeriod, maxRetryPeriod, maxAttempts);
    }
    
    /**
     * Feign 로깅 레벨 설정
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}

