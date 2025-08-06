package com.unicorn.tripgen.location.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * TMAP 보행자 경로 API 클라이언트
 * 한국 내 도보 경로 정보 제공
 */
@FeignClient(
        name = "tmap-pedestrian-client",
        url = "${external.api.tmap.base-url:https://apis.openapi.sk.com}"
)
public interface TmapPedestrianClient {

    /**
     * TMAP 보행자 경로 조회
     * 출발지/목적지 간 도보 경로 정보 제공
     *
     * @param version API 버전 (1 고정)
     * @param appKey  TMAP API 키 (헤더)
     * @param accept  응답 형식 (application/json)
     * @param contentType 요청 형식 (application/json)
     * @param request 요청 본문 (startX, startY, endX, endY, startName, endName)
     * @return 도보 경로 정보 (FeatureCollection)
     */
    @PostMapping(value = "/tmap/routes/pedestrian",
                 consumes = "application/json",
                 produces = "application/json")
    Map<String, Object> getPedestrianRoute(
            @RequestParam("version") String version,
            @RequestHeader("appKey") String appKey,
            @RequestHeader("accept") String accept,
            @RequestHeader("content-type") String contentType,
            @RequestBody Map<String, Object> request
    );
}