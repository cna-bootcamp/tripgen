package com.unicorn.tripgen.location.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Google Places API 클라이언트
 * Google Places API를 통한 장소 정보 조회
 */
@FeignClient(
    name = "google-places-client",
    url = "${external.api.google.places.base-url:https://maps.googleapis.com/maps/api/place}"
)
public interface GooglePlacesClient {
    
    /**
     * 주변 장소 검색
     * 
     * @param location 검색 중심 좌표 (latitude,longitude)
     * @param radius 검색 반경 (미터)
     * @param type 장소 타입
     * @param keyword 검색 키워드
     * @param language 언어 코드
     * @param apiKey API 키
     * @return 검색 결과
     */
    @GetMapping("/nearbysearch/json")
    Map<String, Object> searchNearbyPlaces(
        @RequestParam("location") String location,
        @RequestParam("radius") Integer radius,
        @RequestParam(value = "type", required = false) String type,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "language", defaultValue = "ko") String language,
        @RequestParam(value = "fields", required = false) String fields,
        @RequestParam("key") String apiKey
    );
    
    /**
     * 텍스트 기반 장소 검색
     * 
     * @param query 검색 쿼리
     * @param location 검색 중심 좌표 (선택적)
     * @param radius 검색 반경 (선택적)
     * @param type 장소 타입 (선택적)
     * @param language 언어 코드
     * @param apiKey API 키
     * @return 검색 결과
     */
    @GetMapping("/textsearch/json")
    Map<String, Object> searchPlacesByText(
        @RequestParam("query") String query,
        @RequestParam(value = "location", required = false) String location,
        @RequestParam(value = "radius", required = false) Integer radius,
        @RequestParam(value = "type", required = false) String type,
        @RequestParam(value = "language", defaultValue = "ko") String language,
        @RequestParam(value = "fields", required = false) String fields,
        @RequestParam("key") String apiKey
    );
    
    /**
     * 장소 상세 정보 조회
     * 
     * @param placeId Google Place ID
     * @param fields 조회할 필드 목록
     * @param language 언어 코드
     * @param apiKey API 키
     * @return 장소 상세 정보
     */
    @GetMapping("/details/json")
    Map<String, Object> getPlaceDetails(
        @RequestParam("place_id") String placeId,
        @RequestParam(value = "fields", required = false) String fields,
        @RequestParam(value = "language", defaultValue = "ko") String language,
        @RequestParam("key") String apiKey
    );
    
    /**
     * 장소 사진 조회
     * 
     * @param photoReference 사진 참조 ID
     * @param maxWidth 최대 너비
     * @param maxHeight 최대 높이
     * @param apiKey API 키
     * @return 사진 바이너리
     */
    @GetMapping("/photo")
    byte[] getPlacePhoto(
        @RequestParam("photo_reference") String photoReference,
        @RequestParam(value = "maxwidth", required = false) Integer maxWidth,
        @RequestParam(value = "maxheight", required = false) Integer maxHeight,
        @RequestParam("key") String apiKey
    );
    
    /**
     * 장소 자동 완성
     * 
     * @param input 입력 텍스트
     * @param location 검색 중심 좌표 (선택적)
     * @param radius 검색 반경 (선택적)
     * @param types 장소 타입 (선택적)
     * @param components 국가/지역 제한 (선택적)
     * @param language 언어 코드
     * @param apiKey API 키
     * @return 자동 완성 결과
     */
    @GetMapping("/autocomplete/json")
    Map<String, Object> getPlaceAutocomplete(
        @RequestParam("input") String input,
        @RequestParam(value = "location", required = false) String location,
        @RequestParam(value = "radius", required = false) Integer radius,
        @RequestParam(value = "types", required = false) String types,
        @RequestParam(value = "components", required = false) String components,
        @RequestParam(value = "language", defaultValue = "ko") String language,
        @RequestParam("key") String apiKey
    );
    
    /**
     * 쿼리 자동 완성
     * 
     * @param input 입력 텍스트
     * @param location 검색 중심 좌표 (선택적)
     * @param radius 검색 반경 (선택적)
     * @param language 언어 코드
     * @param apiKey API 키
     * @return 자동 완성 결과
     */
    @GetMapping("/queryautocomplete/json")
    Map<String, Object> getQueryAutocomplete(
        @RequestParam("input") String input,
        @RequestParam(value = "location", required = false) String location,
        @RequestParam(value = "radius", required = false) Integer radius,
        @RequestParam(value = "language", defaultValue = "ko") String language,
        @RequestParam("key") String apiKey
    );
}