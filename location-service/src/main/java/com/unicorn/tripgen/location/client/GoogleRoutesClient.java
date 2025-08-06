package com.unicorn.tripgen.location.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * Google Routes API 클라이언트 (신규 Routes API v2)
 */
@FeignClient(
        name = "google-routes-client",
        url = "${external.api.google.routes.base-url:https://routes.googleapis.com}"
)
public interface GoogleRoutesClient {

    /**
     * Google Routes API v2 computeRoutes 메서드
     *
     * @param apiKey      Google API 키 (헤더)
     * @param fieldMask   응답 필드 마스크 (헤더)
     * @param requestBody Routes API 요청 본문
     * @return 경로 정보
     */
    @PostMapping(value = "/directions/v2:computeRoutes", 
                 consumes = "application/json",
                 produces = "application/json")
    Map<String, Object> computeRoutes(
            @RequestHeader("X-Goog-Api-Key") String apiKey,
            @RequestHeader("X-Goog-FieldMask") String fieldMask,
            @RequestBody Map<String, Object> requestBody
    );
}