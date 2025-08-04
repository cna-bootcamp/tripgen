package com.unicorn.tripgen.location.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.tripgen.location.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Redis 캐시 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService implements CacheService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public void cacheLocationSearchResult(String key, LocationSearchResponse result, int ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            log.debug("Cached location search result: key={}", key);
        } catch (Exception e) {
            log.error("Error caching location search result: key={}", key, e);
        }
    }
    
    @Override
    public LocationSearchResponse getLocationSearchResult(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.debug("Retrieved cached location search result: key={}", key);
                return objectMapper.readValue(json, LocationSearchResponse.class);
            }
        } catch (Exception e) {
            log.error("Error retrieving cached location search result: key={}", key, e);
        }
        return null;
    }
    
    @Override
    public void cacheNearbySearchResult(String key, NearbyPlacesResponse result, int ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            log.debug("Cached nearby search result: key={}", key);
        } catch (Exception e) {
            log.error("Error caching nearby search result: key={}", key, e);
        }
    }
    
    @Override
    public NearbyPlacesResponse getNearbySearchResult(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.debug("Retrieved cached nearby search result: key={}", key);
                return objectMapper.readValue(json, NearbyPlacesResponse.class);
            }
        } catch (Exception e) {
            log.error("Error retrieving cached nearby search result: key={}", key, e);
        }
        return null;
    }
    
    @Override
    public void cacheLocationDetail(String key, LocationDetailResponse detail, int ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(detail);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            log.debug("Cached location detail: key={}", key);
        } catch (Exception e) {
            log.error("Error caching location detail: key={}", key, e);
        }
    }
    
    @Override
    public LocationDetailResponse getLocationDetail(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.debug("Retrieved cached location detail: key={}", key);
                return objectMapper.readValue(json, LocationDetailResponse.class);
            }
        } catch (Exception e) {
            log.error("Error retrieving cached location detail: key={}", key, e);
        }
        return null;
    }
    
    @Override
    public void cacheWeatherInfo(String key, WeatherResponse weather, int ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(weather);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            log.debug("Cached weather info: key={}", key);
        } catch (Exception e) {
            log.error("Error caching weather info: key={}", key, e);
        }
    }
    
    @Override
    public WeatherResponse getWeatherInfo(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.debug("Retrieved cached weather info: key={}", key);
                return objectMapper.readValue(json, WeatherResponse.class);
            }
        } catch (Exception e) {
            log.error("Error retrieving cached weather info: key={}", key, e);
        }
        return null;
    }
    
    @Override
    public void cacheRouteInfo(String key, RouteResponse route, int ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(route);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            log.debug("Cached route info: key={}", key);
        } catch (Exception e) {
            log.error("Error caching route info: key={}", key, e);
        }
    }
    
    @Override
    public RouteResponse getRouteInfo(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.debug("Retrieved cached route info: key={}", key);
                return objectMapper.readValue(json, RouteResponse.class);
            }
        } catch (Exception e) {
            log.error("Error retrieving cached route info: key={}", key, e);
        }
        return null;
    }
    
    @Override
    public void cacheObject(String key, Object value, int ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            log.debug("Cached object: key={}", key);
        } catch (Exception e) {
            log.error("Error caching object: key={}", key, e);
        }
    }
    
    @Override
    public Object getObject(String key) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                log.debug("Retrieved cached object: key={}", key);
                return objectMapper.readValue(json, Object.class);
            }
        } catch (Exception e) {
            log.error("Error retrieving cached object: key={}", key, e);
        }
        return null;
    }
    
    @Override
    public void evictCache(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Evicted cache: key={}, deleted={}", key, deleted);
        } catch (Exception e) {
            log.error("Error evicting cache: key={}", key, e);
        }
    }
    
    @Override
    public void evictLocationCache(String placeId) {
        try {
            String[] patterns = {
                "search:*:" + placeId + ":*",
                "nearby:*:" + placeId + ":*",
                "detail:" + placeId + ":*",
                "ai_recommendation:" + placeId + "*"
            };
            
            for (String pattern : patterns) {
                evictCacheByPattern(pattern);
            }
            
            log.debug("Evicted location cache for placeId: {}", placeId);
        } catch (Exception e) {
            log.error("Error evicting location cache: placeId={}", placeId, e);
        }
    }
    
    @Override
    public String[] getLocationCacheKeys(String placeId) {
        return new String[]{
            "detail:" + placeId + ":*",
            "ai_recommendation:" + placeId + "*"
        };
    }
    
    @Override
    public void evictCacheByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.debug("Evicted cache by pattern: pattern={}, deleted={}", pattern, deleted);
            }
        } catch (Exception e) {
            log.error("Error evicting cache by pattern: pattern={}", pattern, e);
        }
    }
    
    @Override
    public Object getCacheStatistics() {
        // Redis 통계 정보 조회
        return new Object() {
            public String getStatus() { return "active"; }
            public long getKeyCount() {
                try {
                    Set<String> keys = redisTemplate.keys("*");
                    return keys != null ? keys.size() : 0;
                } catch (Exception e) {
                    log.error("Error getting key count", e);
                    return 0;
                }
            }
            public String getInfo() { return "Redis cache service statistics"; }
        };
    }
}