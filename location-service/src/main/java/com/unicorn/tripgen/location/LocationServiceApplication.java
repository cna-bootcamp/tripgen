package com.unicorn.tripgen.location;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Location Service 메인 애플리케이션 클래스
 * 장소 검색 및 정보 제공 서비스
 */
@SpringBootApplication(scanBasePackages = {
    "com.unicorn.tripgen.location",
    "com.unicorn.tripgen.common"
})
@EnableJpaAuditing
@EnableCaching
@EnableFeignClients
public class LocationServiceApplication {
    
    /**
     * 애플리케이션 시작점
     * 
     * @param args 명령행 인수
     */
    public static void main(String[] args) {
        SpringApplication.run(LocationServiceApplication.class, args);
    }
}