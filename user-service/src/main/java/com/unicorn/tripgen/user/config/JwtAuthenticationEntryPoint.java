package com.unicorn.tripgen.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.tripgen.common.constant.CommonMessages;
import com.unicorn.tripgen.common.constant.ErrorCodes;
import com.unicorn.tripgen.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JWT 인증 진입점
 * 인증되지 않은 요청에 대한 처리를 담당
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    
    private final ObjectMapper objectMapper;
    
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        log.debug("인증되지 않은 요청: uri={}, error={}", 
                request.getRequestURI(), authException.getMessage());
        
        // 401 Unauthorized 응답 설정
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // 에러 응답 생성
        ErrorResponse errorResponse = ErrorResponse.of(
            ErrorCodes.UNAUTHORIZED, 
            CommonMessages.UNAUTHORIZED
        );
        
        // JSON 응답 작성
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}