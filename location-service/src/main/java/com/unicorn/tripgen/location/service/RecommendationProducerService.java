package com.unicorn.tripgen.location.service;

import com.unicorn.tripgen.location.dto.RecommendationRequest;
import com.unicorn.tripgen.location.dto.WeatherRequest;
import com.unicorn.tripgen.location.dto.WeatherResponse;
import com.unicorn.tripgen.location.service.CacheService;
import com.unicorn.tripgen.location.service.ExternalApiService;
import com.unicorn.tripgen.location.service.WeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * AI 추천 요청 메시지 프로듀서 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationProducerService {
    
    private final StreamBridge streamBridge;
    private final CacheService cacheService;
    private final ExternalApiService externalApiService;
    private final WeatherService weatherService;
    private final ObjectMapper objectMapper;
    
    /**
     * AI 추천 생성 요청 메시지 발행
     */
    public String sendRecommendationRequest(String placeId, String tripId, 
            RecommendationRequest.SearchContext searchContext) {
        
        String requestId = UUID.randomUUID().toString();
        
        // 1. 캐시에서 장소 정보 조회
        RecommendationRequest.PlaceInfo placeInfo = getPlaceInfo(placeId);
        
        // 2. 날씨 정보 조회 (장소 정보가 있는 경우)
        RecommendationRequest.WeatherInfo weatherInfo = null;
        if (placeInfo != null && placeInfo.getLatitude() != null && placeInfo.getLongitude() != null) {
            weatherInfo = getWeatherInfo(placeInfo.getLatitude(), placeInfo.getLongitude());
        }
        
        // 3. 요청 메시지 생성
        RecommendationRequest request = RecommendationRequest.builder()
                .requestId(requestId)
                .placeId(placeId)
                .tripId(tripId)
                .context(tripId != null ? "trip" : "search")
                .searchContext(searchContext)
                .placeInfo(placeInfo)
                .weatherInfo(weatherInfo)
                .requestTime(LocalDateTime.now())
                .build();
        
        try {
            // 3. 발송 데이터 상세 로깅
            String requestJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            log.info("=== AI Recommendation Request to Service Bus ===\n" +
                    "Topic: location-search\n" +
                    "RequestId: {}\n" +
                    "PlaceId: {}\n" +
                    "TripId: {}\n" +
                    "Context: {}\n" +
                    "Full Message:\n{}", 
                    requestId, placeId, tripId, request.getContext(), requestJson);
            
            // 4. Service Bus로 메시지 발송
            boolean sent = streamBridge.send("aiRecommendationProducer-out-0", request);
            
            if (sent) {
                log.info("✅ AI recommendation request successfully sent: requestId={}, placeId={}", requestId, placeId);
            } else {
                log.error("❌ Failed to send AI recommendation request: requestId={}, placeId={}", requestId, placeId);
                throw new RuntimeException("메시지 전송 실패");
            }
            
            return requestId;
            
        } catch (Exception e) {
            log.error("❌ Error sending AI recommendation request: requestId={}, placeId={}", requestId, placeId, e);
            throw new RuntimeException("AI 추천 요청 전송 중 오류가 발생했습니다", e);
        }
    }
    
    /**
     * 장소 정보 조회 (캐시 우선, 폴백으로 외부 API 호출)
     */
    private RecommendationRequest.PlaceInfo getPlaceInfo(String placeId) {
        try {
            // 1. 캐시에서 장소 정보 조회
            String cacheKey = "place_detail:" + placeId;
            Object cachedPlace = cacheService.getObject(cacheKey);
            
            if (cachedPlace != null) {
                log.debug("Place info found in cache: {}", placeId);
                // 캐시된 객체를 PlaceInfo로 변환
                return convertToPlaceInfo(cachedPlace, placeId);
            }
            
            // 2. 캐시에 없으면 외부 API 호출 (폴백 처리)
            log.info("Place info not in cache, fetching from external API: {}", placeId);
            var placeDetail = externalApiService.getGooglePlaceDetail(placeId, false, "ko");
            
            if (placeDetail != null) {
                // 캐시에 저장 (30분)
                cacheService.cacheObject(cacheKey, placeDetail, 1800);
                
                // PlaceInfo 객체 생성
                return RecommendationRequest.PlaceInfo.builder()
                        .placeId(placeId)
                        .name(placeDetail.getName())
                        .category(placeDetail.getCategory())
                        .latitude(placeDetail.getLocation() != null && placeDetail.getLocation().getLatitude() != null 
                                ? placeDetail.getLocation().getLatitude().doubleValue() : null)
                        .longitude(placeDetail.getLocation() != null && placeDetail.getLocation().getLongitude() != null 
                                ? placeDetail.getLocation().getLongitude().doubleValue() : null)
                        .address(placeDetail.getLocation() != null ? placeDetail.getLocation().getAddress() : null)
                        .build();
            }
            
            log.warn("Could not fetch place info for placeId: {}", placeId);
            return null;
            
        } catch (Exception e) {
            log.error("Error fetching place info for placeId: {}", placeId, e);
            // 에러 발생 시에도 null 반환하여 메시지는 전송되도록 함
            return null;
        }
    }
    
    /**
     * 캐시된 객체를 PlaceInfo로 변환
     */
    @SuppressWarnings("unchecked")
    private RecommendationRequest.PlaceInfo convertToPlaceInfo(Object cachedPlace, String placeId) {
        try {
            if (cachedPlace instanceof Map) {
                Map<String, Object> placeMap = (Map<String, Object>) cachedPlace;
                
                return RecommendationRequest.PlaceInfo.builder()
                        .placeId(placeId)
                        .name((String) placeMap.get("name"))
                        .category((String) placeMap.get("category"))
                        .latitude(getDoubleValue(placeMap.get("latitude")))
                        .longitude(getDoubleValue(placeMap.get("longitude")))
                        .address((String) placeMap.get("address"))
                        .build();
            }
            
            // LocationDetailResponse 타입인 경우 처리
            return objectMapper.convertValue(cachedPlace, RecommendationRequest.PlaceInfo.class);
            
        } catch (Exception e) {
            log.error("Error converting cached object to PlaceInfo", e);
            return null;
        }
    }
    
    private Double getDoubleValue(Object value) {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * 날씨 정보 조회 (캐시 우선, 폴백으로 외부 API 호출)
     */
    private RecommendationRequest.WeatherInfo getWeatherInfo(Double latitude, Double longitude) {
        try {
            // 1. 캐시에서 날씨 정보 조회
            String cacheKey = String.format("weather:%.4f:%.4f", latitude, longitude);
            Object cachedWeather = cacheService.getObject(cacheKey);
            
            if (cachedWeather != null) {
                log.debug("Weather info found in cache for location: {},{}", latitude, longitude);
                return convertToWeatherInfo(cachedWeather);
            }
            
            // 2. 캐시에 없으면 WeatherService를 통해 조회
            log.info("Weather info not in cache, fetching from external API: {},{}", latitude, longitude);
            
            WeatherRequest weatherRequest = WeatherRequest.builder()
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
                    
            WeatherResponse weatherResponse = weatherService.getWeather(weatherRequest);
            
            if (weatherResponse != null) {
                // 캐시에 저장 (15분 - 날씨 정보는 자주 변경되므로)
                cacheService.cacheObject(cacheKey, weatherResponse, 900);
                
                // WeatherInfo 객체 생성
                return RecommendationRequest.WeatherInfo.builder()
                        .temperature(weatherResponse.getTemperature())
                        .feelsLike(weatherResponse.getFeelsLike())
                        .condition(weatherResponse.getMainWeather())
                        .description(weatherResponse.getDescription())
                        .humidity(weatherResponse.getHumidity() != null ? weatherResponse.getHumidity().doubleValue() : null)
                        .windSpeed(weatherResponse.getWindSpeed())
                        .visibility(weatherResponse.getVisibility() != null ? weatherResponse.getVisibility() / 1000.0 : null) // m to km
                        .precipitation(weatherResponse.getRainVolume())
                        .sunrise(convertTimestampToString(weatherResponse.getSunrise()))
                        .sunset(convertTimestampToString(weatherResponse.getSunset()))
                        .observedTime(LocalDateTime.now())
                        .build();
            }
            
            log.warn("Could not fetch weather info for location: {},{}", latitude, longitude);
            return null;
            
        } catch (Exception e) {
            log.error("Error fetching weather info for location: {},{}", latitude, longitude, e);
            // 에러 발생 시에도 null 반환하여 메시지는 전송되도록 함
            return null;
        }
    }
    
    /**
     * 캐시된 날씨 객체를 WeatherInfo로 변환
     */
    @SuppressWarnings("unchecked")
    private RecommendationRequest.WeatherInfo convertToWeatherInfo(Object cachedWeather) {
        try {
            if (cachedWeather instanceof WeatherResponse) {
                WeatherResponse response = (WeatherResponse) cachedWeather;
                return RecommendationRequest.WeatherInfo.builder()
                        .temperature(response.getTemperature())
                        .feelsLike(response.getFeelsLike())
                        .condition(response.getMainWeather())
                        .description(response.getDescription())
                        .humidity(response.getHumidity() != null ? response.getHumidity().doubleValue() : null)
                        .windSpeed(response.getWindSpeed())
                        .visibility(response.getVisibility() != null ? response.getVisibility() / 1000.0 : null)
                        .precipitation(response.getRainVolume())
                        .sunrise(convertTimestampToString(response.getSunrise()))
                        .sunset(convertTimestampToString(response.getSunset()))
                        .observedTime(LocalDateTime.now())
                        .build();
            }
            
            // Map 타입인 경우 처리
            if (cachedWeather instanceof Map) {
                Map<String, Object> weatherMap = (Map<String, Object>) cachedWeather;
                return RecommendationRequest.WeatherInfo.builder()
                        .temperature(getDoubleValue(weatherMap.get("temperature")))
                        .feelsLike(getDoubleValue(weatherMap.get("feelsLike")))
                        .condition((String) weatherMap.get("condition"))
                        .description((String) weatherMap.get("description"))
                        .humidity(getDoubleValue(weatherMap.get("humidity")))
                        .windSpeed(getDoubleValue(weatherMap.get("windSpeed")))
                        .visibility(getDoubleValue(weatherMap.get("visibility")))
                        .precipitation(getDoubleValue(weatherMap.get("precipitation")))
                        .sunrise((String) weatherMap.get("sunrise"))
                        .sunset((String) weatherMap.get("sunset"))
                        .observedTime(LocalDateTime.now())
                        .build();
            }
            
            return objectMapper.convertValue(cachedWeather, RecommendationRequest.WeatherInfo.class);
            
        } catch (Exception e) {
            log.error("Error converting cached object to WeatherInfo", e);
            return null;
        }
    }
    
    /**
     * Unix timestamp를 시간 문자열로 변환
     */
    private String convertTimestampToString(Long timestamp) {
        if (timestamp == null) return null;
        try {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(timestamp), 
                    ZoneId.of("Asia/Seoul")
            );
            return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            log.error("Error converting timestamp to string: {}", timestamp, e);
            return null;
        }
    }
}