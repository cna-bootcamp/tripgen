package com.unicorn.tripgen.location.service;

import com.unicorn.tripgen.location.dto.*;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 위치 서비스 인터페이스
 * 위치 검색, 상세 정보 조회, 주변 장소 검색 등의 기능을 제공
 */
public interface LocationService {
    
    // 키워드 검색 메서드 제거됨 - 주변 검색으로 충분
    
    /**
     * 주변 장소 검색
     * 
     * @param request 주변 검색 요청 정보
     * @return 검색 결과
     */
    NearbyPlacesResponse searchNearbyPlaces(NearbyPlacesRequest request);
    
    /**
     * 위치 상세 정보 조회
     * 
     * @param placeId 장소 ID
     * @param includeReviews 리뷰 포함 여부
     * @param language 언어 코드
     * @return 위치 상세 정보
     */
    LocationDetailResponse getLocationDetail(String placeId, Boolean includeReviews, String language);
    
    /**
     * 위치의 AI 추천 정보 조회
     * 
     * @param placeId 장소 ID
     * @param tripId 여행 ID (사용자 프로필 조회용, 선택적)
     * @return AI 추천 정보 또는 생성 요청 응답
     */
    Object getLocationRecommendations(String placeId, String tripId);
    
    
    /**
     * 특정 반경 내의 위치들 조회
     * 
     * @param latitude 중심 위도
     * @param longitude 중심 경도
     * @param radiusKm 반경 (킬로미터)
     * @param locationType 위치 타입 (선택적)
     * @param pageable 페이징 정보
     * @return 위치 목록
     */
    List<LocationSearchResponse.PlaceCard> getLocationsWithinRadius(
        BigDecimal latitude, BigDecimal longitude, Double radiusKm, 
        String locationType, Pageable pageable
    );
    
    /**
     * ExternalApiService에 접근하기 위한 메서드
     * @return ExternalApiService 인스턴스
     */
    ExternalApiService getExternalApiService();
}