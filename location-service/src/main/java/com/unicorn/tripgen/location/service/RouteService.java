package com.unicorn.tripgen.location.service;

import com.unicorn.tripgen.location.dto.RouteRequest;
import com.unicorn.tripgen.location.dto.RouteResponse;

public interface RouteService {
    
    /**
     * 두 지점 간의 경로 정보 조회
     * 
     * @param request 경로 조회 요청 정보
     * @return 경로 정보 응답
     */
    RouteResponse getRoute(RouteRequest request);
}