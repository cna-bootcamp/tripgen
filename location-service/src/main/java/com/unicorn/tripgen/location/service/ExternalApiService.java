package com.unicorn.tripgen.location.service;

import com.unicorn.tripgen.location.dto.*;

import java.util.List;

/**
 * 외부 API 서비스 인터페이스
 * Google Places, Kakao Map 등 외부 API 통합 관리
 */
public interface ExternalApiService {
    
    /**
     * 키워드로 위치 검색 (외부 API 통합)
     * 
     * @param request 검색 요청
     * @return 검색 결과 목록
     */
    List<LocationSearchResponse.PlaceCard> searchLocationsByKeyword(SearchLocationRequest request);
    
    /**
     * 주변 장소 검색 (외부 API 통합)
     * 
     * @param request 주변 검색 요청
     * @return 검색 결과 목록
     */
    List<NearbyPlacesResponse.NearbyPlace> searchNearbyPlaces(NearbyPlacesRequest request);
    
    /**
     * 위치 상세 정보 조회 (외부 API 통합)
     * 
     * @param placeId 장소 ID
     * @param includeReviews 리뷰 포함 여부
     * @param language 언어 코드
     * @return 위치 상세 정보
     */
    LocationDetailResponse getLocationDetail(String placeId, Boolean includeReviews, String language);
    
    
    /**
     * 위치 자동완성 (외부 API 통합)
     * 
     * @param input 입력 텍스트
     * @param latitude 중심 위도
     * @param longitude 중심 경도
     * @param language 언어 코드
     * @param limit 결과 개수 제한
     * @return 자동완성 결과
     */
    List<LocationSearchResponse.PlaceCard> getLocationAutocomplete(
        String input, String latitude, String longitude, String language, Integer limit
    );
    
    /**
     * Google Places API로 장소 검색
     * 
     * @param request 검색 요청
     * @return 검색 결과 목록
     */
    List<LocationSearchResponse.PlaceCard> searchWithGooglePlaces(SearchLocationRequest request);
    
    /**
     * Kakao Map API로 장소 검색
     * 
     * @param request 검색 요청
     * @return 검색 결과 목록
     */
    List<LocationSearchResponse.PlaceCard> searchWithKakaoMap(SearchLocationRequest request);
    
    /**
     * Google Places로 주변 장소 검색
     * 
     * @param request 주변 검색 요청
     * @return 검색 결과 목록
     */
    List<NearbyPlacesResponse.NearbyPlace> searchNearbyWithGoogle(NearbyPlacesRequest request);
    
    /**
     * Kakao Map으로 주변 장소 검색
     * 
     * @param request 주변 검색 요청
     * @return 검색 결과 목록
     */
    List<NearbyPlacesResponse.NearbyPlace> searchNearbyWithKakao(NearbyPlacesRequest request);
    
    /**
     * Google Places로 장소 상세 정보 조회
     * 
     * @param placeId Google Place ID
     * @param includeReviews 리뷰 포함 여부
     * @param language 언어 코드
     * @return 상세 정보
     */
    LocationDetailResponse getGooglePlaceDetail(String placeId, Boolean includeReviews, String language);
    
    /**
     * 좌표를 주소로 변환 (외부 API 통합)
     * 
     * @param latitude 위도
     * @param longitude 경도
     * @return 주소 정보
     */
    String getAddressFromCoordinates(Double latitude, Double longitude);
    
    /**
     * 주소를 좌표로 변환 (외부 API 통합)
     * 
     * @param address 주소
     * @return 좌표 정보 (위도, 경도)
     */
    LocationDetailResponse.LocationInfo getCoordinatesFromAddress(String address);
    
    /**
     * 외부 API 서비스 상태 확인
     * 
     * @return 서비스 상태 정보
     */
    Object getExternalApiStatus();
    
    /**
     * API 호출 통계 조회
     * 
     * @return API 사용량 통계
     */
    Object getApiUsageStatistics();
    
    /**
     * 주변 주차장 검색
     * 
     * @param latitude 중심 위도
     * @param longitude 중심 경도
     * @param radiusMeters 검색 반경 (미터)
     * @param limit 결과 개수 제한 (기본 3개)
     * @return 주변 주차장 목록
     */
    List<LocationDetailResponse.ParkingInfo> searchNearbyParkings(
        Double latitude, Double longitude, Integer radiusMeters, Integer limit
    );
}