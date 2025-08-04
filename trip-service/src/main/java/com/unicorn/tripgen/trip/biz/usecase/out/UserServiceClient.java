package com.unicorn.tripgen.trip.biz.usecase.out;

import java.util.Optional;

/**
 * User Service 클라이언트 인터페이스 (Output Port)
 */
public interface UserServiceClient {
    
    /**
     * 사용자 정보
     */
    record UserInfo(
        String userId,
        String username,
        String email,
        String nickname,
        boolean active
    ) {}
    
    /**
     * 사용자 존재 여부 확인
     */
    boolean existsUser(String userId);
    
    /**
     * 사용자 정보 조회
     */
    Optional<UserInfo> getUserInfo(String userId);
    
    /**
     * 사용자 활성 상태 확인
     */
    boolean isActiveUser(String userId);
}