package com.unicorn.tripgen.location.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Google Directions API 클라이언트 (도보, 대중교통 경로)
 */
@FeignClient(
        name = "google-directions-client",
        url = "${external.api.google.directions.base-url:https://maps.googleapis.com}"
)
public interface GoogleDirectionsClient {

    /**
     * Google Directions API 경로 조회
     *
     * @param origin      출발지 (위도,경도)
     * @param destination 목적지 (위도,경도)
     * @param mode        이동 수단 (driving, walking, transit, bicycling)
     * @param key         Google API 키
     * @param language    응답 언어 (기본값: ko)
     * @param alternatives 대안 경로 포함 여부
     * @return 경로 정보
     */
    @GetMapping("/maps/api/directions/json")
    Map<String, Object> getDirections(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam("mode") String mode,
            @RequestParam("key") String key,
            @RequestParam(value = "language", defaultValue = "ko") String language,
            @RequestParam(value = "alternatives", defaultValue = "false") Boolean alternatives
    );

    /**
     * 대중교통 경로 조회 (상세 옵션)
     *
     * @param origin          출발지 (위도,경도)
     * @param destination     목적지 (위도,경도)
     * @param key             Google API 키
     * @param departureTime   출발 시간 (Unix timestamp)
     * @param transitMode     대중교통 모드 (bus, subway, train, tram, rail)
     * @param transitRoutingPreference 경로 선호도 (less_walking, fewer_transfers)
     * @param language        응답 언어
     * @return 대중교통 경로 정보
     */
    @GetMapping("/maps/api/directions/json")
    Map<String, Object> getTransitDirections(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam("mode") String mode,
            @RequestParam("key") String key,
            @RequestParam(value = "departure_time", required = false) Long departureTime,
            @RequestParam(value = "transit_mode", required = false) String transitMode,
            @RequestParam(value = "transit_routing_preference", required = false) String transitRoutingPreference,
            @RequestParam(value = "language", defaultValue = "ko") String language
    );
}