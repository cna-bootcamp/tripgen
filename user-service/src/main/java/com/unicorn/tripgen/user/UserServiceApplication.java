package com.unicorn.tripgen.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * User Service 메인 애플리케이션 클래스
 * 사용자 인증 및 프로필 관리 서비스
 */
@SpringBootApplication(scanBasePackages = {
    "com.unicorn.tripgen.user",
    "com.unicorn.tripgen.common"
})
@EnableJpaAuditing
@EnableCaching
@EnableFeignClients
public class UserServiceApplication {
    
    /**
     * 애플리케이션 시작점
     * 
     * @param args 명령행 인수
     */
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}