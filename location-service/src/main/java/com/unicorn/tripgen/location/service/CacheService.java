package com.unicorn.tripgen.location.service;

import com.unicorn.tripgen.location.dto.*;

/**
 * 캐시 서비스 인터페이스
 * Redis를 통한 데이터 캐싱 관리
 */
public interface CacheService {
    
    /**
     * 위치 검색 결과 캐시
     * 
     * @param key 캐시 키
     * @param result 검색 결과
     * @param ttlSeconds TTL (초)
     */
    void cacheLocationSearchResult(String key, LocationSearchResponse result, int ttlSeconds);
    
    /**
     * 위치 검색 결과 조회
     * 
     * @param key 캐시 키
     * @return 검색 결과 (없으면 null)
     */
    LocationSearchResponse getLocationSearchResult(String key);
    
    /**
     * 주변 장소 검색 결과 캐시
     * 
     * @param key 캐시 키
     * @param result 검색 결과
     * @param ttlSeconds TTL (초)
     */
    void cacheNearbySearchResult(String key, NearbyPlacesResponse result, int ttlSeconds);
    
    /**
     * 주변 장소 검색 결과 조회
     * 
     * @param key 캐시 키
     * @return 검색 결과 (없으면 null)
     */
    NearbyPlacesResponse getNearbySearchResult(String key);
    
    /**
     * 위치 상세 정보 캐시
     * 
     * @param key 캐시 키
     * @param detail 상세 정보
     * @param ttlSeconds TTL (초)
     */
    void cacheLocationDetail(String key, LocationDetailResponse detail, int ttlSeconds);
    
    /**
     * 위치 상세 정보 조회
     * 
     * @param key 캐시 키
     * @return 상세 정보 (없으면 null)
     */
    LocationDetailResponse getLocationDetail(String key);
    
    /**
     * 날씨 정보 캐시
     * 
     * @param key 캐시 키
     * @param weather 날씨 정보
     * @param ttlSeconds TTL (초)
     */
    void cacheWeatherInfo(String key, WeatherResponse weather, int ttlSeconds);
    
    /**
     * 날씨 정보 조회
     * 
     * @param key 캐시 키
     * @return 날씨 정보 (없으면 null)
     */
    WeatherResponse getWeatherInfo(String key);
    
    /**
     * 경로 정보 캐시
     * 
     * @param key 캐시 키
     * @param route 경로 정보
     * @param ttlSeconds TTL (초)
     */
    void cacheRouteInfo(String key, RouteResponse route, int ttlSeconds);
    
    /**
     * 경로 정보 조회
     * 
     * @param key 캐시 키
     * @return 경로 정보 (없으면 null)
     */
    RouteResponse getRouteInfo(String key);
    
    /**
     * 일반 객체 캐시
     * 
     * @param key 캐시 키
     * @param value 값
     * @param ttlSeconds TTL (초)
     */
    void cacheObject(String key, Object value, int ttlSeconds);
    
    /**
     * 일반 객체 조회
     * 
     * @param key 캐시 키
     * @return 객체 (없으면 null)
     */
    Object getObject(String key);
    
    /**
     * 캐시 삭제
     * 
     * @param key 캐시 키
     */
    void evictCache(String key);
    
    /**
     * 위치 관련 모든 캐시 삭제
     * 
     * @param placeId 장소 ID
     */
    void evictLocationCache(String placeId);
    
    /**
     * 위치 관련 캐시 키 목록 조회
     * 
     * @param placeId 장소 ID
     * @return 캐시 키 배열
     */
    String[] getLocationCacheKeys(String placeId);
    
    /**
     * 패턴으로 캐시 삭제
     * 
     * @param pattern 패턴 (예: "location:*")
     */
    void evictCacheByPattern(String pattern);
    
    /**
     * 캐시 통계 조회
     * 
     * @return 캐시 통계 정보
     */
    Object getCacheStatistics();
}