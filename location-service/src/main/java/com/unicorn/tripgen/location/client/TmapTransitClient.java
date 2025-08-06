package com.unicorn.tripgen.location.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * TMAP 대중교통 API 클라이언트
 * 한국 내 대중교통 및 도보 경로 정보 제공
 */
@FeignClient(
        name = "tmap-transit-client",
        url = "${external.api.tmap.transit.base-url:https://apis.openapi.sk.com}"
)
public interface TmapTransitClient {

    /**
     * TMAP 대중교통 경로 조회
     * 출발지/목적지에 대한 대중교통 경로탐색 정보와 전체 보행자 이동 경로를 제공
     *
     * @param appKey  TMAP API 키 (헤더)
     * @param accept  응답 형식 (application/json)
     * @param request 요청 본문 (startX, startY, endX, endY 등)
     * @return 대중교통 경로 정보 (지하철, 버스, 도보 포함)
     */
    @PostMapping(value = "/transit/routes", 
                 consumes = "application/json",
                 produces = "application/json")
    Map<String, Object> getTransitRoutes(
            @RequestHeader("appKey") String appKey,
            @RequestHeader("Accept") String accept,
            @RequestBody Map<String, Object> request
    );
}