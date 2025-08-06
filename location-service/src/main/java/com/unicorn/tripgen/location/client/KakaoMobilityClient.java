package com.unicorn.tripgen.location.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Kakao Mobility API 클라이언트 (자동차 경로 정보 전용)
 * 참고: Kakao Mobility API는 자동차 경로만 지원
 * 도보/대중교통은 별도 API 필요
 */
@FeignClient(
        name = "kakao-mobility-client",
        url = "${external.api.kakao.mobility.base-url:https://apis-navi.kakaomobility.com}"
)
public interface KakaoMobilityClient {

    /**
     * 자동차 경로 안내
     *
     * @param authorization API 키 (Bearer 토큰)
     * @param origin        출발지 좌표 (x,y)
     * @param destination   목적지 좌표 (x,y)
     * @param waypoints     경유지 좌표들
     * @param priority      경로 옵션 (RECOMMEND, TIME, DISTANCE)
     * @param carFuel       차량 연료 (GASOLINE, DIESEL, LPG)
     * @param carHipass     하이패스 유무
     * @param alternatives  대안 경로 포함 여부
     * @param roadDetails   도로 상세 정보 포함 여부
     * @return 경로 정보
     */
    @GetMapping("/v1/directions")
    Map<String, Object> getCarDirections(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam(value = "waypoints", required = false) String waypoints,
            @RequestParam(value = "priority", defaultValue = "RECOMMEND") String priority,
            @RequestParam(value = "car_fuel", defaultValue = "GASOLINE") String carFuel,
            @RequestParam(value = "car_hipass", defaultValue = "false") Boolean carHipass,
            @RequestParam(value = "alternatives", defaultValue = "true") Boolean alternatives,
            @RequestParam(value = "road_details", defaultValue = "false") Boolean roadDetails
    );

    // 참고: Kakao Mobility API는 대중교통과 도보 경로를 지원하지 않음
    // 대중교통: ODsay API 또는 Tmap Transit API 사용 필요
    // 도보: Kakao Local API의 pedestrian 또는 Google Directions API 사용 필요
}
