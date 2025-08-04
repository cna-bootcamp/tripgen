package com.unicorn.tripgen.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Service Application
 * AI 기반 여행 일정 생성 및 추천 서비스
 */
@SpringBootApplication
@EnableFeignClients
@EnableAsync
@EnableScheduling
public class AIServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AIServiceApplication.class, args);
    }
}