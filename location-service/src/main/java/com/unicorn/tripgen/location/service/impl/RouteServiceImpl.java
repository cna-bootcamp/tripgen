package com.unicorn.tripgen.location.service.impl;

import com.unicorn.tripgen.location.dto.route.RouteRequest;
import com.unicorn.tripgen.location.dto.route.RouteResponse;
import com.unicorn.tripgen.location.service.RouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteServiceImpl implements RouteService {
    
    @Override
    public RouteResponse getRoute(RouteRequest request) {
        log.info("경로 정보 조회 요청: from=({},{}), to=({},{})", 
                request.getFromLatitude(), request.getFromLongitude(),
                request.getToLatitude(), request.getToLongitude());
        
        // TODO: Kakao Maps API 연동 구현
        
        // 임시 응답 데이터
        return RouteResponse.builder()
                .distance(1500) // 1.5km
                .duration(600) // 10분
                .polyline("encoded_polyline_string")
                .build();
    }
}