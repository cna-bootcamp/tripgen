package com.unicorn.tripgen.user.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * 모든 HTTP 요청에서 JWT 토큰을 검증하고 인증 정보를 설정
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        boolean shouldSkip = isPublicEndpoint(path);
        if (shouldSkip) {
            log.debug("공개 엔드포인트 감지, JWT 필터 제외: path={}", path);
        }
        return shouldSkip;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        log.debug("JWT 필터 처리: path={}, method={}", path, request.getMethod());
        
        try {
            // 요청에서 JWT 토큰 추출
            String token = jwtTokenProvider.resolveToken(request);
            log.debug("JWT 토큰 추출: token={}", token != null ? "있음" : "없음");
            
            // 토큰이 있고 유효한 경우 인증 정보 설정
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                
                if (authentication != null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT 인증 성공: userId={}, uri={}", 
                            authentication.getName(), request.getRequestURI());
                }
            }
            
        } catch (Exception e) {
            log.error("JWT 인증 처리 중 오류 발생: uri={}, error={}", 
                    request.getRequestURI(), e.getMessage());
            
            // 인증 실패시 SecurityContext 정리
            SecurityContextHolder.clearContext();
        }
        
        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }
    
    private boolean isPublicEndpoint(String path) {
        return path.equals("/api/v1/users/register") ||
               path.equals("/api/v1/users/login") ||
               path.startsWith("/api/v1/users/check/") ||
               path.startsWith("/uploads/") ||
               path.startsWith("/static/") ||
               path.startsWith("/public/") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui");
    }
    
}