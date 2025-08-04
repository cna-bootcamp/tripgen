package com.unicorn.tripgen.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * User Service 클라이언트
 * 사용자 정보 조회를 위한 Feign 클라이언트
 */
@FeignClient(
    name = "user-service",
    url = "${external-services.user-service.url:http://localhost:8081}",
    path = "/api/v1/users"
)
public interface UserServiceClient {
    
    /**
     * 사용자 프로필 조회
     */
    @GetMapping("/{userId}/profile")
    Map<String, Object> getUserProfile(@PathVariable("userId") String userId);
    
    /**
     * 사용자 기본 정보 조회
     */
    @GetMapping("/{userId}")
    Map<String, Object> getUserInfo(@PathVariable("userId") String userId);
}