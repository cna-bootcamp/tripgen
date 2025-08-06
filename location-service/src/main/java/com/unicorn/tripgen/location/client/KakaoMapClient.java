package com.unicorn.tripgen.location.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Kakao Map API 클라이언트
 * Kakao Map API를 통한 장소 정보 및 경로 정보 조회
 */
@FeignClient(
    name = "kakao-map-client", 
    url = "${external.api.kakao.base-url:https://dapi.kakao.com}"
)
public interface KakaoMapClient {
    
    /**
     * 키워드로 장소 검색
     * 
     * @param authorization API 키 (Bearer 토큰)
     * @param query 검색 키워드
     * @param category 카테고리 그룹 코드
     * @param x 중심 좌표 X (경도)
     * @param y 중심 좌표 Y (위도)
     * @param radius 반경 (미터)
     * @param rect 사각형 영역 (x1,y1,x2,y2)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 방식 (distance, accuracy)
     * @return 검색 결과
     */
    @GetMapping("/v2/local/search/keyword.json")
    Map<String, Object> searchPlacesByKeyword(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("query") String query,
        @RequestParam(value = "category_group_code", required = false) String category,
        @RequestParam(value = "x", required = false) Double x,
        @RequestParam(value = "y", required = false) Double y,
        @RequestParam(value = "radius", required = false) Integer radius,
        @RequestParam(value = "rect", required = false) String rect,
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "size", defaultValue = "15") Integer size,
        @RequestParam(value = "sort", defaultValue = "accuracy") String sort
    );
    
    /**
     * 카테고리로 장소 검색
     * 
     * @param authorization API 키 (Bearer 토큰)
     * @param categoryGroupCode 카테고리 그룹 코드
     * @param x 중심 좌표 X (경도)
     * @param y 중심 좌표 Y (위도)
     * @param radius 반경 (미터)
     * @param rect 사각형 영역 (x1,y1,x2,y2)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param sort 정렬 방식 (distance, accuracy)
     * @return 검색 결과
     */
    @GetMapping("/v2/local/search/category.json")
    Map<String, Object> searchPlacesByCategory(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("category_group_code") String categoryGroupCode,
        @RequestParam(value = "x", required = false) Double x,
        @RequestParam(value = "y", required = false) Double y,
        @RequestParam(value = "radius", required = false) Integer radius,
        @RequestParam(value = "rect", required = false) String rect,
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "size", defaultValue = "15") Integer size,
        @RequestParam(value = "sort", defaultValue = "distance") String sort
    );
    
    /**
     * 주소 검색 (주소 -> 좌표)
     * 
     * @param authorization API 키 (Bearer 토큰)
     * @param query 주소 검색어
     * @param analyzeType 분석 방법 (similar, exact)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 주소 검색 결과
     */
    @GetMapping("/v2/local/search/address.json")
    Map<String, Object> searchAddress(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("query") String query,
        @RequestParam(value = "analyze_type", defaultValue = "similar") String analyzeType,
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "size", defaultValue = "10") Integer size
    );
    
    /**
     * 좌표 -> 주소 변환
     * 
     * @param authorization API 키 (Bearer 토큰)
     * @param x 좌표 X (경도)
     * @param y 좌표 Y (위도)
     * @param inputCoord 입력 좌표계 (WGS84, WCONGNAMUL, CONGNAMUL, WTM, TM)
     * @return 주소 정보
     */
    @GetMapping("/v2/local/geo/coord2address.json")
    Map<String, Object> getAddressFromCoordinate(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("x") Double x,
        @RequestParam("y") Double y,
        @RequestParam(value = "input_coord", defaultValue = "WGS84") String inputCoord
    );
    
    /**
     * 좌표 -> 행정구역 정보 변환
     * 
     * @param authorization API 키 (Bearer 토큰)
     * @param x 좌표 X (경도)
     * @param y 좌표 Y (위도)
     * @param inputCoord 입력 좌표계
     * @param outputCoord 출력 좌표계
     * @return 행정구역 정보
     */
    @GetMapping("/v2/local/geo/coord2regioncode.json")
    Map<String, Object> getRegionFromCoordinate(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("x") Double x,
        @RequestParam("y") Double y,
        @RequestParam(value = "input_coord", defaultValue = "WGS84") String inputCoord,
        @RequestParam(value = "output_coord", defaultValue = "WGS84") String outputCoord
    );
    
    /**
     * 좌표계 변환
     * 
     * @param authorization API 키 (Bearer 토큰)
     * @param x 좌표 X
     * @param y 좌표 Y
     * @param inputCoord 입력 좌표계
     * @param outputCoord 출력 좌표계
     * @return 변환된 좌표
     */
    @GetMapping("/v2/local/geo/transcoord.json")
    Map<String, Object> transformCoordinate(
        @RequestHeader("Authorization") String authorization,
        @RequestParam("x") Double x,
        @RequestParam("y") Double y,
        @RequestParam("input_coord") String inputCoord,
        @RequestParam("output_coord") String outputCoord
    );
}

