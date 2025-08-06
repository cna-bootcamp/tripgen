package com.unicorn.tripgen.location.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Weather API Client 설정
 */
@Configuration
public class WeatherApiClientConfig {

    @Bean
    public Request.Options weatherApiRequestOptions() {
        return new Request.Options(3000, TimeUnit.MILLISECONDS, 15000, TimeUnit.MILLISECONDS, true);
    }

    @Bean
    public Retryer weatherApiRetryer() {
        return new Retryer.Default(500, 2000, 2);
    }

    @Bean
    public Logger.Level weatherApiLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
