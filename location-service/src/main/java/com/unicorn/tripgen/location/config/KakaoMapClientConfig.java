package com.unicorn.tripgen.location.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Kakao Map Client 설정
 */
@Configuration
class KakaoMapClientConfig {

    @Bean
    public Request.Options kakaoMapRequestOptions() {
        return new Request.Options(5000, TimeUnit.MILLISECONDS, 20000, TimeUnit.MILLISECONDS, true);
    }

    @Bean
    public Retryer kakaoMapRetryer() {
        return new Retryer.Default(1000, 3000, 2);
    }

    @Bean
    public Logger.Level kakaoMapLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
