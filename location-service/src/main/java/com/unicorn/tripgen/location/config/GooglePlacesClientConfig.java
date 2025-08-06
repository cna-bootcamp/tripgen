package com.unicorn.tripgen.location.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Google Places Client 설정
 */
@Configuration
class GooglePlacesClientConfig {

    @Bean
    public Request.Options googlePlacesRequestOptions() {
        return new Request.Options(5000, TimeUnit.MILLISECONDS, 30000, TimeUnit.MILLISECONDS, true);
    }

    @Bean
    public Retryer googlePlacesRetryer() {
        return new Retryer.Default(1000, 5000, 3);
    }

    @Bean
    public Logger.Level googlePlacesLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
