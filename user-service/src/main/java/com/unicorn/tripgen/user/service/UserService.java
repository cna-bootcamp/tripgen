package com.unicorn.tripgen.user.service;

import com.unicorn.tripgen.user.dto.*;
import com.unicorn.tripgen.user.entity.User;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 서비스 인터페이스
 * 사용자 관련 비즈니스 로직을 정의
 */
public interface UserService {
    
    /**
     * 회원가입 처리
     * 
     * @param request 회원가입 요청 정보
     * @return 회원가입 응답 정보
     */
    RegisterResponse registerUser(RegisterRequest request);
    
    /**
     * 로그인 처리
     * 
     * @param request 로그인 요청 정보
     * @return 로그인 응답 정보 (토큰 포함)
     */
    LoginResponse loginUser(LoginRequest request);
    
    /**
     * 로그아웃 처리
     * 
     * @param userId 사용자 ID
     */
    void logoutUser(String userId);
    
    /**
     * 사용자 프로필 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 프로필 정보
     */
    UserProfile getProfile(String userId);
    
    /**
     * 사용자 프로필 수정
     * 
     * @param userId 사용자 ID
     * @param request 프로필 수정 요청 정보
     * @return 수정된 사용자 프로필 정보
     */
    UserProfile updateProfile(String userId, UpdateProfileRequest request);
    
    /**
     * 프로필 이미지 업로드
     * 
     * @param userId 사용자 ID
     * @param file 업로드할 이미지 파일
     * @return 업로드된 이미지 URL
     */
    AvatarUploadResponse uploadAvatar(String userId, MultipartFile file);
    
    /**
     * 비밀번호 변경
     * 
     * @param userId 사용자 ID
     * @param request 비밀번호 변경 요청 정보
     */
    void changePassword(String userId, ChangePasswordRequest request);
    
    /**
     * 아이디 중복 확인
     * 
     * @param username 확인할 아이디
     * @return 중복 확인 결과
     */
    UsernameCheckResponse checkUsername(String username);
    
    /**
     * 이메일 중복 확인
     * 
     * @param email 확인할 이메일
     * @return 중복 확인 결과
     */
    EmailCheckResponse checkEmail(String email);
    
    /**
     * 사용자 ID로 사용자 조회
     * 
     * @param userId 사용자 ID
     * @return 사용자 엔티티
     */
    User findById(String userId);
    
    /**
     * 사용자명으로 사용자 조회
     * 
     * @param username 사용자명
     * @return 사용자 엔티티
     */
    User findByUsername(String username);
}