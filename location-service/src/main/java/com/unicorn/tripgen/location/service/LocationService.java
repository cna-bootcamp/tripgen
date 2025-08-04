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
    
    /**
     * 키워드로 위치 검색
     * 
     * @param request 검색 요청 정보
     * @return 검색 결과
     */
    LocationSearchResponse searchLocationsByKeyword(SearchLocationRequest request);
    
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
     * @param includeAI AI 추천 정보 포함 여부
     * @param includeReviews 리뷰 포함 여부
     * @param language 언어 코드
     * @return 위치 상세 정보
     */
    LocationDetailResponse getLocationDetail(String placeId, Boolean includeAI, Boolean includeReviews, String language);
    
    /**
     * 위치의 AI 추천 정보 조회
     * 
     * @param placeId 장소 ID
     * @param tripId 여행 ID (사용자 프로필 조회용, 선택적)
     * @return AI 추천 정보 또는 생성 요청 응답
     */
    Object getLocationRecommendations(String placeId, String tripId);
    
    /**
     * 위치의 영업시간 정보 조회
     * 
     * @param placeId 장소 ID
     * @return 영업시간 정보
     */
    LocationDetailResponse.BusinessHours getBusinessHours(String placeId);
    
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
     * 인기 위치 목록 조회
     * 
     * @param category 카테고리 필터 (선택적)
     * @param region 지역 필터 (선택적)
     * @param pageable 페이징 정보
     * @return 인기 위치 목록
     */
    List<LocationSearchResponse.PlaceCard> getPopularLocations(String category, String region, Pageable pageable);
    
    /**
     * 최고 평점 위치 목록 조회
     * 
     * @param category 카테고리 필터 (선택적)
     * @param minReviewCount 최소 리뷰 수
     * @param pageable 페이징 정보
     * @return 최고 평점 위치 목록
     */
    List<LocationSearchResponse.PlaceCard> getTopRatedLocations(String category, Integer minReviewCount, Pageable pageable);
    
    /**
     * 위치 정보 동기화 (외부 API에서 최신 정보 가져와서 업데이트)
     * 
     * @param placeId 장소 ID
     * @param forceUpdate 강제 업데이트 여부
     * @return 동기화된 위치 정보
     */
    LocationDetailResponse syncLocationData(String placeId, Boolean forceUpdate);
    
    /**
     * 위치 자동 완성
     * 
     * @param input 입력 텍스트
     * @param latitude 검색 중심 위도 (선택적)
     * @param longitude 검색 중심 경도 (선택적)
     * @param language 언어 코드
     * @param limit 결과 개수 제한
     * @return 자동 완성 결과
     */
    List<LocationSearchResponse.PlaceCard> getLocationAutocomplete(
        String input, BigDecimal latitude, BigDecimal longitude, String language, Integer limit
    );
    
    /**
     * 위치 북마크 추가
     * 
     * @param userId 사용자 ID
     * @param placeId 장소 ID
     * @return 성공 여부
     */
    Boolean addLocationBookmark(String userId, String placeId);
    
    /**
     * 위치 북마크 제거
     * 
     * @param userId 사용자 ID
     * @param placeId 장소 ID
     * @return 성공 여부
     */
    Boolean removeLocationBookmark(String userId, String placeId);
    
    /**
     * 사용자 북마크 위치 목록 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 북마크된 위치 목록
     */
    List<LocationSearchResponse.PlaceCard> getUserBookmarkedLocations(String userId, Pageable pageable);
    
    /**
     * 위치 방문 기록 추가
     * 
     * @param userId 사용자 ID
     * @param placeId 장소 ID
     * @param visitDate 방문 일시
     * @return 성공 여부
     */
    Boolean addLocationVisit(String userId, String placeId, String visitDate);
    
    /**
     * 사용자 방문 기록 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 방문한 위치 목록
     */
    List<LocationSearchResponse.PlaceCard> getUserVisitedLocations(String userId, Pageable pageable);
    
    /**
     * 위치 평점 업데이트
     * 
     * @param placeId 장소 ID
     * @param rating 새로운 평점
     * @param reviewCount 새로운 리뷰 수
     * @return 성공 여부
     */
    Boolean updateLocationRating(String placeId, BigDecimal rating, Integer reviewCount);
    
    /**
     * 위치 통계 정보 조회
     * 
     * @param placeId 장소 ID
     * @return 통계 정보 (방문자 수, 인기도 등)
     */
    Object getLocationStatistics(String placeId);
}