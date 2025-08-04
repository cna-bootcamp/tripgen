package com.unicorn.tripgen.user.controller;

import com.unicorn.tripgen.common.constant.CommonMessages;
import com.unicorn.tripgen.common.dto.ApiResponse;
import com.unicorn.tripgen.common.util.SecurityUtils;
import com.unicorn.tripgen.user.dto.*;
import com.unicorn.tripgen.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 사용자 컨트롤러
 * 사용자 인증 및 프로필 관리 API를 제공
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "사용자 인증 및 프로필 관리 API")
public class UserController {
    
    private final UserService userService;
    
    /**
     * 회원가입
     */
    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "신규 사용자 회원가입 처리")
    public ResponseEntity<ApiResponse<RegisterResponse>> registerUser(
            @Valid @RequestBody RegisterRequest request) {
        
        log.info("회원가입 요청: username={}", request.getUsername());
        
        RegisterResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, CommonMessages.REGISTRATION_SUCCESS));
    }
    
    /**
     * 로그인
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 로그인 및 토큰 발급")
    public ResponseEntity<ApiResponse<LoginResponse>> loginUser(
            @Valid @RequestBody LoginRequest request) {
        
        log.info("로그인 요청: username={}", request.getUsername());
        
        LoginResponse response = userService.loginUser(request);
        return ResponseEntity.ok(ApiResponse.success(response, CommonMessages.LOGIN_SUCCESS));
    }
    
    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "로그아웃", description = "사용자 로그아웃 처리")
    public ResponseEntity<ApiResponse<Map<String, String>>> logoutUser() {
        
        String userId = SecurityUtils.getCurrentUsername();
        log.info("로그아웃 요청: userId={}", userId);
        
        userService.logoutUser(userId);
        
        Map<String, String> response = Map.of("message", CommonMessages.LOGOUT_SUCCESS);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 프로필 조회
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "프로필 조회", description = "현재 로그인한 사용자의 프로필 정보 조회")
    public ResponseEntity<ApiResponse<UserProfile>> getProfile() {
        
        String userId = SecurityUtils.getCurrentUsername();
        log.debug("프로필 조회 요청: userId={}", userId);
        
        UserProfile profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
    
    /**
     * 프로필 수정
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "프로필 수정", description = "사용자 프로필 정보 수정")
    public ResponseEntity<ApiResponse<UserProfile>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        
        String userId = SecurityUtils.getCurrentUsername();
        log.info("프로필 수정 요청: userId={}", userId);
        
        UserProfile profile = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(profile, CommonMessages.PROFILE_UPDATED));
    }
    
    /**
     * 프로필 이미지 업로드
     */
    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "프로필 이미지 업로드", description = "사용자 프로필 이미지 업로드")
    public ResponseEntity<ApiResponse<AvatarUploadResponse>> uploadAvatar(
            @Parameter(description = "프로필 이미지 파일 (최대 5MB, JPG/PNG)")
            @RequestParam("file") MultipartFile file) {
        
        String userId = SecurityUtils.getCurrentUsername();
        log.info("프로필 이미지 업로드 요청: userId={}, fileName={}", userId, file.getOriginalFilename());
        
        AvatarUploadResponse response = userService.uploadAvatar(userId, file);
        return ResponseEntity.ok(ApiResponse.success(response, CommonMessages.AVATAR_UPLOADED));
    }
    
    /**
     * 비밀번호 변경
     */
    @PutMapping("/profile/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "비밀번호 변경", description = "사용자 비밀번호 변경")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        
        String userId = SecurityUtils.getCurrentUsername();
        log.info("비밀번호 변경 요청: userId={}", userId);
        
        userService.changePassword(userId, request);
        
        Map<String, String> response = Map.of("message", CommonMessages.PASSWORD_CHANGED);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 아이디 중복 확인
     */
    @GetMapping("/check/username/{username}")
    @Operation(summary = "아이디 중복 확인", description = "회원가입 시 아이디 중복 확인")
    public ResponseEntity<ApiResponse<UsernameCheckResponse>> checkUsername(
            @Parameter(description = "확인할 아이디", example = "tripuser123")
            @PathVariable String username) {
        
        log.debug("아이디 중복 확인 요청: username={}", username);
        
        UsernameCheckResponse response = userService.checkUsername(username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 이메일 중복 확인
     */
    @GetMapping("/check/email/{email}")
    @Operation(summary = "이메일 중복 확인", description = "회원가입 시 이메일 중복 확인")
    public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(
            @Parameter(description = "확인할 이메일", example = "user@tripgen.com")
            @PathVariable String email) {
        
        log.debug("이메일 중복 확인 요청: email={}", email);
        
        EmailCheckResponse response = userService.checkEmail(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}