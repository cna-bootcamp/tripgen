package com.unicorn.tripgen.location.service.impl;

import com.unicorn.tripgen.location.client.GoogleDirectionsClient;
import com.unicorn.tripgen.location.client.GoogleRoutesClient;
import com.unicorn.tripgen.location.client.KakaoMobilityClient;
import com.unicorn.tripgen.location.client.TmapPedestrianClient;
import com.unicorn.tripgen.location.client.TmapTransitClient;
import com.unicorn.tripgen.location.dto.RouteRequest;
import com.unicorn.tripgen.location.dto.RouteResponse;
import com.unicorn.tripgen.location.entity.Route;
import com.unicorn.tripgen.location.entity.TransportType;
import com.unicorn.tripgen.location.repository.RouteRepository;
import com.unicorn.tripgen.location.service.RouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteServiceImpl implements RouteService {
    
    private final KakaoMobilityClient kakaoMobilityClient;
    private final GoogleDirectionsClient googleDirectionsClient;
    private final GoogleRoutesClient googleRoutesClient;
    private final TmapTransitClient tmapTransitClient;
    private final TmapPedestrianClient tmapPedestrianClient;
    private final RouteRepository routeRepository;
    
    @Value("${external.api.kakao.api-key:094feac895a3e4a6d7ffa66d877bf48f}")
    private String kakaoApiKey;
    
    @Value("${external.api.google.api-key:AIzaSyBZTeX67yotCtiwzomdxt4nKD__1vsAsFU}")
    private String googleApiKey;
    
    @Value("${external.api.tmap.app-key:your-tmap-app-key}")
    private String tmapAppKey;
    
    @Override
    @Transactional
    @Cacheable(value = "route", key = "#request.fromLatitude + ':' + #request.fromLongitude + ':' + " +
                                      "#request.toLatitude + ':' + #request.toLongitude + ':' + #request.mode")
    public RouteResponse getRoute(RouteRequest request) {
        log.info("경로 정보 조회 요청: from=({},{}), to=({},{}), mode={}", 
                request.getFromLatitude(), request.getFromLongitude(),
                request.getToLatitude(), request.getToLongitude(),
                request.getMode());
        
        try {
            Map<String, Object> response;
            RouteResponse routeResponse;
            
            if ("driving".equalsIgnoreCase(request.getMode())) {
                // 한국 좌표인지 확인
                boolean isKorea = isKoreanCoordinate(request.getFromLatitude(), request.getFromLongitude()) 
                        && isKoreanCoordinate(request.getToLatitude(), request.getToLongitude());
                
                
                if (isKorea) {
                    // 한국 내 자동차 경로는 Kakao Mobility API 사용
                    String origin = String.format("%f,%f", request.getFromLongitude(), request.getFromLatitude());
                    String destination = String.format("%f,%f", request.getToLongitude(), request.getToLatitude());
                    String authorization = "KakaoAK " + kakaoApiKey;
                    
                    response = kakaoMobilityClient.getCarDirections(
                            authorization, origin, destination, null, 
                            "RECOMMEND", "GASOLINE", false, true, false);
                    
                    // Kakao 응답 파싱
                    routeResponse = parseKakaoResponse(response, request.getMode());
                } else {
                    // 해외 자동차 경로는 Google Routes API 사용
                    try {
                        routeResponse = getGoogleRoutesDirections(request);
                    } catch (Exception e) {
                        log.error("Google Routes API 호출 실패, Directions API 폴백 사용: {}", e.getMessage());
                        // 폴백: 기존 Directions API 사용
                        String origin = String.format("%f,%f", request.getFromLatitude(), request.getFromLongitude());
                        String destination = String.format("%f,%f", request.getToLatitude(), request.getToLongitude());
                        response = googleDirectionsClient.getDirections(
                                origin, destination, "driving", 
                                googleApiKey, "ko", false);
                        routeResponse = parseGoogleResponse(response, "driving");
                    }
                }
                
            } else if ("transit".equalsIgnoreCase(request.getMode())) {
                // 대중교통은 한국 내에서는 TMAP API 사용
                log.info("대중교통 모드로 TMAP Transit API 사용");
                
                // 한국 좌표인지 확인 (대략적인 한국 범위)
                boolean isKorea = isKoreanCoordinate(request.getFromLatitude(), request.getFromLongitude()) 
                        && isKoreanCoordinate(request.getToLatitude(), request.getToLongitude());
                
                if (isKorea) {
                    try {
                        // TMAP Transit API 요청
                        Map<String, Object> tmapRequest = new HashMap<>();
                        tmapRequest.put("startX", String.valueOf(request.getFromLongitude()));
                        tmapRequest.put("startY", String.valueOf(request.getFromLatitude()));
                        tmapRequest.put("endX", String.valueOf(request.getToLongitude()));
                        tmapRequest.put("endY", String.valueOf(request.getToLatitude()));
                        tmapRequest.put("count", 5);
                        tmapRequest.put("lang", 0); // 한국어
                        tmapRequest.put("format", "json");
                        
                        response = tmapTransitClient.getTransitRoutes(
                                tmapAppKey, 
                                "application/json",
                                tmapRequest);
                        
                        // TMAP 응답 파싱
                        routeResponse = parseTmapResponse(response);
                    } catch (Exception e) {
                        log.error("TMAP API 호출 실패, Google API로 폴백: {}", e.getMessage());
                        // 폴백: Google API 사용
                        String origin = String.format("%f,%f", request.getFromLatitude(), request.getFromLongitude());
                        String destination = String.format("%f,%f", request.getToLatitude(), request.getToLongitude());
                        response = googleDirectionsClient.getDirections(
                                origin, destination, "transit", 
                                googleApiKey, "ko", false);
                        routeResponse = parseGoogleResponse(response, "transit");
                    }
                } else {
                    // 해외 좌표는 Google Routes API v2 사용
                    try {
                        log.info("해외 좌표로 Google Routes API v2 사용 (대중교통)");
                        routeResponse = getGoogleRoutesDirections(request);
                    } catch (Exception e) {
                        log.error("Google Routes API 호출 실패, Directions API 폴백 사용: {}", e.getMessage());
                        // 폴백: 기존 Directions API 사용
                        String origin = String.format("%f,%f", request.getFromLatitude(), request.getFromLongitude());
                        String destination = String.format("%f,%f", request.getToLatitude(), request.getToLongitude());
                        response = googleDirectionsClient.getDirections(
                                origin, destination, "transit", 
                                googleApiKey, "ko", false);
                        routeResponse = parseGoogleResponse(response, "transit");
                    }
                }
                
            } else if ("walking".equalsIgnoreCase(request.getMode())) {
                // 도보 모드 처리
                log.info("도보 모드 처리");
                
                boolean isKorea = isKoreanCoordinate(request.getFromLatitude(), request.getFromLongitude()) 
                        && isKoreanCoordinate(request.getToLatitude(), request.getToLongitude());
                
                if (isKorea) {
                    // 한국 내 도보: TMAP 보행자 경로 API 사용
                    try {
                        log.info("한국 내 도보 경로 - TMAP 보행자 API 사용");
                        
                        Map<String, Object> tmapRequest = new HashMap<>();
                        tmapRequest.put("startX", String.valueOf(request.getFromLongitude()));
                        tmapRequest.put("startY", String.valueOf(request.getFromLatitude()));
                        tmapRequest.put("endX", String.valueOf(request.getToLongitude()));
                        tmapRequest.put("endY", String.valueOf(request.getToLatitude()));
                        tmapRequest.put("startName", "출발지");
                        tmapRequest.put("endName", "도착지");
                        
                        response = tmapPedestrianClient.getPedestrianRoute(
                                "1",  // version
                                tmapAppKey,
                                "application/json",
                                "application/json",
                                tmapRequest);
                        
                        // TMAP 보행자 응답 파싱
                        routeResponse = parseTmapPedestrianResponse(response);
                    } catch (Exception e) {
                        log.error("TMAP 보행자 API 호출 실패, 폴백 사용: {}", e.getMessage());
                        return createFallbackResponse(request);
                    }
                } else {
                    // 해외는 Google Routes API v2 사용
                    try {
                        log.info("해외 좌표로 Google Routes API v2 사용 (도보)");
                        routeResponse = getGoogleRoutesDirections(request);
                    } catch (Exception e) {
                        log.error("Google Routes API 호출 실패, Directions API 폴백 사용: {}", e.getMessage());
                        // 폴백: 기존 Directions API 사용
                        String origin = String.format("%f,%f", request.getFromLatitude(), request.getFromLongitude());
                        String destination = String.format("%f,%f", request.getToLatitude(), request.getToLongitude());
                        response = googleDirectionsClient.getDirections(
                                origin, destination, "walking", 
                                googleApiKey, "ko", false);
                        routeResponse = parseGoogleResponse(response, "walking");
                    }
                }
                
            } else {
                // 지원하지 않는 모드
                log.warn("지원하지 않는 이동 모드: {}", request.getMode());
                return createFallbackResponse(request);
            }
            
            // DB에 경로 정보 저장
            saveRoute(request, routeResponse);
            
            log.info("경로 정보 조회 성공: distance={}m, duration={}s", 
                    routeResponse.getDistance(), routeResponse.getDuration());
            
            return routeResponse;
            
        } catch (Exception e) {
            log.error("API 호출 실패: {}", e.getMessage());
            
            // 폴백: DB에서 유사한 경로 검색
            Route cachedRoute = findCachedRoute(request);
            if (cachedRoute != null) {
                log.info("캐시된 경로 정보 사용");
                return convertToResponse(cachedRoute);
            }
            
            // 최종 폴백: 기본값 반환
            return createFallbackResponse(request);
        }
    }
    
    private RouteResponse parseKakaoResponse(Map<String, Object> response, String mode) {
        RouteResponse.RouteResponseBuilder builder = RouteResponse.builder();
        
        try {
            if (response.containsKey("routes") && response.get("routes") instanceof List) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                if (!routes.isEmpty()) {
                    Map<String, Object> route = routes.get(0);
                    Map<String, Object> summary = (Map<String, Object>) route.get("summary");
                    
                    if (summary != null) {
                        // 거리와 시간 정보 추출
                        Integer distance = ((Number) summary.get("distance")).intValue();
                        Integer duration = ((Number) summary.get("duration")).intValue();
                        
                        builder.distance(distance)
                               .duration(duration)
                               .distanceText(formatDistance(distance))
                               .durationText(formatDuration(duration));
                        
                        // 경로 폴리라인 추출 (섹션별 좌표를 연결)
                        if (route.containsKey("sections")) {
                            List<Map<String, Object>> sections = (List<Map<String, Object>>) route.get("sections");
                            String polyline = extractPolyline(sections);
                            builder.polyline(polyline);
                        }
                    }
                }
            } else if (response.containsKey("trans_id")) {
                // Transit API 응답 처리
                handleTransitResponse(response, builder);
            }
        } catch (Exception e) {
            log.error("Kakao 응답 파싱 오류: {}", e.getMessage());
            throw new RuntimeException("경로 정보 파싱 실패", e);
        }
        
        return builder.build();
    }
    
    private RouteResponse parseGoogleResponse(Map<String, Object> response, String mode) {
        RouteResponse.RouteResponseBuilder builder = RouteResponse.builder();
        
        try {
            String status = (String) response.get("status");
            if (!"OK".equals(status)) {
                log.error("Google Directions API 오류: {}", status);
                throw new RuntimeException("Google API 호출 실패: " + status);
            }
            
            if (response.containsKey("routes") && response.get("routes") instanceof List) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                if (!routes.isEmpty()) {
                    Map<String, Object> route = routes.get(0);
                    
                    // Google API는 legs 구조를 사용
                    if (route.containsKey("legs") && route.get("legs") instanceof List) {
                        List<Map<String, Object>> legs = (List<Map<String, Object>>) route.get("legs");
                        if (!legs.isEmpty()) {
                            Map<String, Object> leg = legs.get(0);
                            
                            // 거리 정보
                            if (leg.containsKey("distance")) {
                                Map<String, Object> distance = (Map<String, Object>) leg.get("distance");
                                Integer distanceValue = ((Number) distance.get("value")).intValue();
                                String distanceText = (String) distance.get("text");
                                builder.distance(distanceValue);
                                builder.distanceText(distanceText);
                            }
                            
                            // 시간 정보
                            if (leg.containsKey("duration")) {
                                Map<String, Object> duration = (Map<String, Object>) leg.get("duration");
                                Integer durationValue = ((Number) duration.get("value")).intValue();
                                String durationText = (String) duration.get("text");
                                builder.duration(durationValue);
                                builder.durationText(durationText);
                            }
                        }
                    }
                    
                    // 폴리라인 정보
                    if (route.containsKey("overview_polyline")) {
                        Map<String, Object> polyline = (Map<String, Object>) route.get("overview_polyline");
                        String encodedPolyline = (String) polyline.get("points");
                        builder.polyline(encodedPolyline);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Google 응답 파싱 오류: {}", e.getMessage());
            throw new RuntimeException("경로 정보 파싱 실패", e);
        }
        
        return builder.build();
    }
    
    private void handleTransitResponse(Map<String, Object> response, RouteResponse.RouteResponseBuilder builder) {
        // 대중교통 API는 다른 응답 구조를 가짐
        if (response.containsKey("routes") && response.get("routes") instanceof List) {
            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
            if (!routes.isEmpty()) {
                Map<String, Object> route = routes.get(0);
                Map<String, Object> summary = (Map<String, Object>) route.get("summary");
                
                if (summary != null) {
                    Integer distance = ((Number) summary.getOrDefault("distance", 0)).intValue();
                    Integer duration = ((Number) summary.getOrDefault("duration", 0)).intValue();
                    
                    builder.distance(distance)
                           .duration(duration)
                           .distanceText(formatDistance(distance))
                           .durationText(formatDuration(duration));
                }
            }
        }
    }
    
    private String extractPolyline(List<Map<String, Object>> sections) {
        // 섹션별 좌표를 추출하여 간단한 폴리라인 문자열로 변환
        StringBuilder polyline = new StringBuilder();
        for (Map<String, Object> section : sections) {
            if (section.containsKey("roads")) {
                List<Map<String, Object>> roads = (List<Map<String, Object>>) section.get("roads");
                for (Map<String, Object> road : roads) {
                    if (road.containsKey("vertexes")) {
                        List<Number> vertexes = (List<Number>) road.get("vertexes");
                        // 좌표 배열을 문자열로 변환 (간단한 형태)
                        for (int i = 0; i < vertexes.size(); i += 2) {
                            if (i > 0) polyline.append("|");
                            polyline.append(vertexes.get(i)).append(",").append(vertexes.get(i + 1));
                        }
                    }
                }
            }
        }
        return polyline.toString();
    }
    
    private String formatDistance(Integer distance) {
        if (distance == null) return "Unknown";
        if (distance < 1000) {
            return distance + "m";
        } else {
            double km = distance / 1000.0;
            return String.format("%.1fkm", km);
        }
    }
    
    private String formatDuration(Integer duration) {
        if (duration == null) return "Unknown";
        int hours = duration / 3600;
        int minutes = (duration % 3600) / 60;
        
        if (hours > 0) {
            return String.format("%d시간 %d분", hours, minutes);
        } else {
            return String.format("%d분", minutes);
        }
    }
    
    private void saveRoute(RouteRequest request, RouteResponse response) {
        try {
            // 좌표를 기반으로 origin/destination ID 생성
            String originId = String.format("%.6f,%.6f", request.getFromLatitude(), request.getFromLongitude());
            String destinationId = String.format("%.6f,%.6f", request.getToLatitude(), request.getToLongitude());
            
            Route route = Route.builder()
                    .routeId(UUID.randomUUID().toString())
                    .originId(originId)
                    .destinationId(destinationId)
                    .distance(response.getDistance())
                    .duration(response.getDuration())
                    .transportType(convertToTransportType(request.getMode()))
                    .routePolyline(response.getPolyline())
                    .build();
            
            routeRepository.save(route);
            log.debug("경로 정보 저장 완료: {}", route.getId());
        } catch (Exception e) {
            log.error("경로 정보 저장 실패: {}", e.getMessage());
            // 저장 실패해도 서비스는 계속 진행
        }
    }
    
    private Route findCachedRoute(RouteRequest request) {
        try {
            // 좌표를 기반으로 origin/destination ID 생성
            String originId = String.format("%.6f,%.6f", request.getFromLatitude(), request.getFromLongitude());
            String destinationId = String.format("%.6f,%.6f", request.getToLatitude(), request.getToLongitude());
            
            return routeRepository.findByOriginDestinationAndType(
                    originId,
                    destinationId,
                    convertToTransportType(request.getMode())
            ).orElse(null);
        } catch (Exception e) {
            log.error("캐시된 경로 검색 실패: {}", e.getMessage());
            return null;
        }
    }
    
    private RouteResponse convertToResponse(Route route) {
        return RouteResponse.builder()
                .distance(route.getDistance())
                .duration(route.getDuration())
                .polyline(route.getRoutePolyline())
                .distanceText(formatDistance(route.getDistance()))
                .durationText(formatDuration(route.getDuration()))
                .build();
    }
    
    private TransportType convertToTransportType(String mode) {
        switch (mode.toLowerCase()) {
            case "walking":
                return TransportType.WALKING;
            case "transit":
                return TransportType.PUBLIC_TRANSPORT;
            case "driving":
            default:
                return TransportType.CAR;
        }
    }
    
    private RouteResponse createFallbackResponse(RouteRequest request) {
        // 직선 거리 계산 (Haversine formula)
        double distance = calculateDistance(
                request.getFromLatitude(), request.getFromLongitude(),
                request.getToLatitude(), request.getToLongitude()
        );
        
        // 예상 시간 계산 (평균 속도 기준)
        int duration = calculateDuration(distance, request.getMode());
        
        return RouteResponse.builder()
                .distance((int) distance)
                .duration(duration)
                .distanceText(formatDistance((int) distance))
                .durationText(formatDuration(duration))
                .polyline(String.format("%f,%f|%f,%f", 
                        request.getFromLongitude(), request.getFromLatitude(),
                        request.getToLongitude(), request.getToLatitude()))
                .build();
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // 지구 반경 (미터)
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    private int calculateDuration(double distance, String mode) {
        // 평균 속도 (m/s)
        double speed;
        switch (mode.toLowerCase()) {
            case "walking":
                speed = 1.4; // 5km/h
                break;
            case "transit":
                speed = 8.3; // 30km/h
                break;
            case "driving":
            default:
                speed = 11.1; // 40km/h
                break;
        }
        return (int) (distance / speed);
    }
    
    /**
     * Google Routes API v2를 사용한 경로 조회
     */
    private RouteResponse getGoogleRoutesDirections(RouteRequest request) {
        log.info("Google Routes API v2 사용하여 해외 {} 경로 조회", request.getMode());
        
        // Routes API v2 요청 본문 구성
        Map<String, Object> requestBody = new HashMap<>();
        
        // Origin 설정
        Map<String, Object> origin = new HashMap<>();
        Map<String, Object> originLocation = new HashMap<>();
        Map<String, Object> originLatLng = new HashMap<>();
        originLatLng.put("latitude", request.getFromLatitude());
        originLatLng.put("longitude", request.getFromLongitude());
        originLocation.put("latLng", originLatLng);
        origin.put("location", originLocation);
        requestBody.put("origin", origin);
        
        // Destination 설정
        Map<String, Object> destination = new HashMap<>();
        Map<String, Object> destLocation = new HashMap<>();
        Map<String, Object> destLatLng = new HashMap<>();
        destLatLng.put("latitude", request.getToLatitude());
        destLatLng.put("longitude", request.getToLongitude());
        destLocation.put("latLng", destLatLng);
        destination.put("location", destLocation);
        requestBody.put("destination", destination);
        
        // 이동 수단에 따른 설정
        String travelMode;
        
        switch (request.getMode().toLowerCase()) {
            case "driving":
                travelMode = "DRIVE";
                requestBody.put("routingPreference", "TRAFFIC_AWARE");
                break;
            case "walking":
                travelMode = "WALK";
                // 도보 모드에서는 routingPreference 설정하지 않음
                break;
            case "transit":
                travelMode = "TRANSIT";
                // 대중교통 모드에서는 routingPreference 설정하지 않음
                break;
            default:
                travelMode = "DRIVE";
                requestBody.put("routingPreference", "TRAFFIC_AWARE");
        }
        
        requestBody.put("travelMode", travelMode);
        requestBody.put("computeAlternativeRoutes", false);
        requestBody.put("languageCode", "ko");
        requestBody.put("units", "METRIC");
        
        // Route Modifiers (avoid 옵션)
        Map<String, Object> routeModifiers = new HashMap<>();
        routeModifiers.put("avoidTolls", false);
        routeModifiers.put("avoidHighways", false);
        routeModifiers.put("avoidFerries", false);
        requestBody.put("routeModifiers", routeModifiers);
        
        // API 호출
        String fieldMask = "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline";
        Map<String, Object> response = googleRoutesClient.computeRoutes(
                googleApiKey, fieldMask, requestBody);
        
        return parseGoogleRoutesResponse(response);
    }
    
    /**
     * Google Routes API v2 응답 파싱
     */
    private RouteResponse parseGoogleRoutesResponse(Map<String, Object> response) {
        try {
            if (response.containsKey("routes") && response.get("routes") instanceof List) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                if (!routes.isEmpty()) {
                    Map<String, Object> route = routes.get(0);
                    
                    // 거리 정보 (distanceMeters)
                    Integer distanceMeters = null;
                    if (route.containsKey("distanceMeters")) {
                        distanceMeters = ((Number) route.get("distanceMeters")).intValue();
                    }
                    
                    // 시간 정보 (duration, 예: "165s")
                    Integer durationSeconds = null;
                    if (route.containsKey("duration")) {
                        String duration = (String) route.get("duration");
                        // "165s" 형태에서 숫자만 추출
                        durationSeconds = Integer.parseInt(duration.replaceAll("[^0-9]", ""));
                    }
                    
                    // 폴리라인 정보
                    String encodedPolyline = null;
                    if (route.containsKey("polyline")) {
                        Map<String, Object> polyline = (Map<String, Object>) route.get("polyline");
                        if (polyline.containsKey("encodedPolyline")) {
                            encodedPolyline = (String) polyline.get("encodedPolyline");
                        }
                    }
                    
                    return RouteResponse.builder()
                            .distance(distanceMeters)
                            .duration(durationSeconds)
                            .distanceText(formatDistance(distanceMeters))
                            .durationText(formatDuration(durationSeconds))
                            .polyline(encodedPolyline)
                            .build();
                }
            }
            
            log.warn("Google Routes API 응답에서 경로 정보를 찾을 수 없음");
            return null;
            
        } catch (Exception e) {
            log.error("Google Routes API 응답 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 한국 좌표 범위 확인
     */
    private boolean isKoreanCoordinate(double lat, double lon) {
        // 대한민국 대략적인 범위
        // 위도: 33.0 ~ 38.9
        // 경도: 124.5 ~ 132.0
        return lat >= 33.0 && lat <= 38.9 && lon >= 124.5 && lon <= 132.0;
    }
    
    /**
     * TMAP 대중교통 응답 파싱
     */
    private RouteResponse parseTmapResponse(Map<String, Object> response) {
        RouteResponse.RouteResponseBuilder builder = RouteResponse.builder();
        
        try {
            Map<String, Object> metaData = (Map<String, Object>) response.get("metaData");
            Map<String, Object> plan = (Map<String, Object>) metaData.get("plan");
            List<Map<String, Object>> itineraries = (List<Map<String, Object>>) plan.get("itineraries");
            
            if (itineraries != null && !itineraries.isEmpty()) {
                Map<String, Object> firstRoute = itineraries.get(0);
                
                // 총 시간과 거리
                Integer totalTime = ((Number) firstRoute.get("totalTime")).intValue();
                Integer totalDistance = ((Number) firstRoute.get("totalDistance")).intValue();
                
                builder.duration(totalTime)
                       .distance(totalDistance)
                       .durationText(formatDuration(totalTime))
                       .distanceText(formatDistance(totalDistance));
                
                // 경로 폴리라인 생성 (간단한 형태)
                List<Map<String, Object>> legs = (List<Map<String, Object>>) firstRoute.get("legs");
                if (legs != null) {
                    StringBuilder polyline = new StringBuilder();
                    for (Map<String, Object> leg : legs) {
                        Map<String, Object> start = (Map<String, Object>) leg.get("start");
                        if (start != null && polyline.length() == 0) {
                            polyline.append(start.get("lon")).append(",").append(start.get("lat"));
                        }
                        Map<String, Object> end = (Map<String, Object>) leg.get("end");
                        if (end != null) {
                            polyline.append("|").append(end.get("lon")).append(",").append(end.get("lat"));
                        }
                    }
                    builder.polyline(polyline.toString());
                }
            }
        } catch (Exception e) {
            log.error("TMAP 응답 파싱 오류: {}", e.getMessage());
            throw new RuntimeException("경로 정보 파싱 실패", e);
        }
        
        return builder.build();
    }
    
    /**
     * TMAP 보행자 경로 응답 파싱
     */
    private RouteResponse parseTmapPedestrianResponse(Map<String, Object> response) {
        RouteResponse.RouteResponseBuilder builder = RouteResponse.builder();
        
        try {
            // TMAP 보행자 API는 FeatureCollection 형식으로 응답
            String type = (String) response.get("type");
            if (!"FeatureCollection".equals(type)) {
                throw new RuntimeException("예상하지 못한 응답 형식: " + type);
            }
            
            List<Map<String, Object>> features = (List<Map<String, Object>>) response.get("features");
            if (features != null && !features.isEmpty()) {
                int totalDistance = 0;
                int totalTime = 0;
                StringBuilder polyline = new StringBuilder();
                
                for (Map<String, Object> feature : features) {
                    Map<String, Object> properties = (Map<String, Object>) feature.get("properties");
                    Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
                    
                    if (properties != null) {
                        // 총 거리와 시간 정보 추출
                        if (properties.containsKey("totalDistance")) {
                            totalDistance = ((Number) properties.get("totalDistance")).intValue();
                        }
                        if (properties.containsKey("totalTime")) {
                            totalTime = ((Number) properties.get("totalTime")).intValue();
                        }
                    }
                    
                    if (geometry != null) {
                        String geoType = (String) geometry.get("type");
                        
                        // LineString 좌표들을 폴리라인으로 변환
                        if ("LineString".equals(geoType)) {
                            List<List<Number>> coordinates = (List<List<Number>>) geometry.get("coordinates");
                            if (coordinates != null) {
                                for (List<Number> coord : coordinates) {
                                    if (coord.size() >= 2) {
                                        if (polyline.length() > 0) polyline.append("|");
                                        polyline.append(coord.get(0)).append(",").append(coord.get(1));
                                    }
                                }
                            }
                        }
                    }
                }
                
                builder.distance(totalDistance)
                       .duration(totalTime)
                       .distanceText(formatDistance(totalDistance))
                       .durationText(formatDuration(totalTime))
                       .polyline(polyline.toString());
            }
        } catch (Exception e) {
            log.error("TMAP 보행자 응답 파싱 오류: {}", e.getMessage());
            throw new RuntimeException("도보 경로 정보 파싱 실패", e);
        }
        
        return builder.build();
    }
    
    /**
     * TMAP 응답에서 도보 정보만 추출 (대중교통 API용)
     */
    private RouteResponse parseTmapWalkingResponse(Map<String, Object> response) {
        RouteResponse.RouteResponseBuilder builder = RouteResponse.builder();
        
        try {
            Map<String, Object> metaData = (Map<String, Object>) response.get("metaData");
            Map<String, Object> plan = (Map<String, Object>) metaData.get("plan");
            List<Map<String, Object>> itineraries = (List<Map<String, Object>>) plan.get("itineraries");
            
            if (itineraries != null && !itineraries.isEmpty()) {
                Map<String, Object> firstRoute = itineraries.get(0);
                
                // 총 도보 시간과 거리
                Integer totalWalkTime = ((Number) firstRoute.getOrDefault("totalWalkTime", 0)).intValue();
                Integer totalWalkDistance = ((Number) firstRoute.getOrDefault("totalWalkDistance", 0)).intValue();
                
                // 만약 도보 정보가 없으면 전체 정보 사용
                if (totalWalkTime == 0 || totalWalkDistance == 0) {
                    totalWalkTime = ((Number) firstRoute.get("totalTime")).intValue();
                    totalWalkDistance = ((Number) firstRoute.get("totalDistance")).intValue();
                }
                
                builder.duration(totalWalkTime)
                       .distance(totalWalkDistance)
                       .durationText(formatDuration(totalWalkTime))
                       .distanceText(formatDistance(totalWalkDistance));
                
                // 도보 경로만 추출
                List<Map<String, Object>> legs = (List<Map<String, Object>>) firstRoute.get("legs");
                if (legs != null) {
                    StringBuilder polyline = new StringBuilder();
                    for (Map<String, Object> leg : legs) {
                        String mode = (String) leg.get("mode");
                        if ("WALK".equals(mode)) {
                            List<Map<String, Object>> steps = (List<Map<String, Object>>) leg.get("steps");
                            if (steps != null) {
                                for (Map<String, Object> step : steps) {
                                    String linestring = (String) step.get("linestring");
                                    if (linestring != null) {
                                        if (polyline.length() > 0) polyline.append("|");
                                        polyline.append(linestring);
                                    }
                                }
                            }
                        }
                    }
                    
                    if (polyline.length() == 0) {
                        // 도보 경로가 없으면 시작점과 끝점만
                        List<Map<String, Object>> allLegs = (List<Map<String, Object>>) firstRoute.get("legs");
                        if (allLegs != null && !allLegs.isEmpty()) {
                            Map<String, Object> firstLeg = allLegs.get(0);
                            Map<String, Object> lastLeg = allLegs.get(allLegs.size() - 1);
                            Map<String, Object> start = (Map<String, Object>) firstLeg.get("start");
                            Map<String, Object> end = (Map<String, Object>) lastLeg.get("end");
                            if (start != null && end != null) {
                                polyline.append(start.get("lon")).append(",").append(start.get("lat"))
                                       .append("|").append(end.get("lon")).append(",").append(end.get("lat"));
                            }
                        }
                    }
                    
                    builder.polyline(polyline.toString());
                }
            }
        } catch (Exception e) {
            log.error("TMAP 도보 응답 파싱 오류: {}", e.getMessage());
            throw new RuntimeException("도보 경로 정보 파싱 실패", e);
        }
        
        return builder.build();
    }
}