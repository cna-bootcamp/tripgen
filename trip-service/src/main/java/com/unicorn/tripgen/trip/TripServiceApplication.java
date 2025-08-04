package com.unicorn.tripgen.trip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Trip Service Application
 * 여행 일정 관리 서비스
 */
@SpringBootApplication
@EnableFeignClients
@EnableCaching
public class TripServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(TripServiceApplication.class, args);
    }
}