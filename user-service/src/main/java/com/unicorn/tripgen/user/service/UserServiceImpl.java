package com.unicorn.tripgen.user.service;

import com.unicorn.tripgen.common.constant.ErrorCodes;
import com.unicorn.tripgen.common.constant.CommonMessages;
import com.unicorn.tripgen.common.exception.BusinessException;
import com.unicorn.tripgen.common.exception.NotFoundException;
import com.unicorn.tripgen.common.exception.ValidationException;
import com.unicorn.tripgen.user.dto.*;
import com.unicorn.tripgen.user.entity.User;
import com.unicorn.tripgen.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * 사용자 서비스 구현 클래스
 * 사용자 관련 비즈니스 로직을 처리
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final FileStorageService fileStorageService;
    
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCK_DURATION_MINUTES = 30;
    
    @Override
    public RegisterResponse registerUser(RegisterRequest request) {
        log.info("회원가입 시작: username={}", request.getUsername());
        
        // 입력값 검증
        validateRegisterRequest(request);
        
        // 중복 확인
        validateUserUniqueness(request);
        
        // 사용자 생성
        User user = createUser(request);
        User savedUser = userRepository.save(user);
        
        log.info("회원가입 완료: userId={}, username={}", savedUser.getId(), savedUser.getUsername());
        
        return RegisterResponse.success(
            savedUser.getId(),
            savedUser.getUsername(),
            CommonMessages.REGISTRATION_SUCCESS
        );
    }
    
    @Override
    public LoginResponse loginUser(LoginRequest request) {
        log.info("로그인 시도: username={}", request.getUsername());
        
        User user = findByUsername(request.getUsername());
        
        // 계정 상태 확인
        validateAccountStatus(user);
        
        // 비밀번호 검증
        if (!passwordService.matches(request.getPassword(), user.getPassword())) {
            handleLoginFailure(user);
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, CommonMessages.INVALID_CREDENTIALS);
        }
        
        // 로그인 성공 처리
        handleLoginSuccess(user);
        
        // 토큰 생성
        String accessToken = tokenService.generateAccessToken(user.getId());
        String refreshToken = tokenService.generateRefreshToken(user.getId());
        Integer expiresIn = tokenService.getAccessTokenExpirationSeconds();
        
        UserProfile userProfile = UserProfile.from(user);
        
        log.info("로그인 성공: userId={}, username={}", user.getId(), user.getUsername());
        
        return LoginResponse.success(accessToken, refreshToken, expiresIn, userProfile);
    }
    
    @Override
    public void logoutUser(String userId) {
        log.info("로그아웃 처리: userId={}", userId);
        
        // 토큰 무효화 (Redis에서 블랙리스트 처리)
        tokenService.invalidateUserTokens(userId);
        
        log.info("로그아웃 완료: userId={}", userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserProfile getProfile(String userId) {
        log.debug("프로필 조회: userId={}", userId);
        
        User user = findById(userId);
        return UserProfile.from(user);
    }
    
    @Override
    public UserProfile updateProfile(String userId, UpdateProfileRequest request) {
        log.info("프로필 수정 시작: userId={}", userId);
        
        User user = findById(userId);
        
        // 이메일 변경 시 중복 확인
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException(ErrorCodes.DUPLICATE_EMAIL, CommonMessages.EMAIL_UNAVAILABLE);
            }
            user.setEmail(request.getEmail());
        }
        
        // 휴대폰 번호 변경 시 중복 확인
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new BusinessException(ErrorCodes.DUPLICATE_EMAIL, "이미 사용중인 휴대폰 번호입니다");
            }
            user.setPhone(request.getPhone());
        }
        
        // 이름 변경
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        
        User savedUser = userRepository.save(user);
        
        log.info("프로필 수정 완료: userId={}", userId);
        
        return UserProfile.from(savedUser);
    }
    
    @Override
    public AvatarUploadResponse uploadAvatar(String userId, MultipartFile file) {
        log.info("프로필 이미지 업로드 시작: userId={}", userId);
        
        User user = findById(userId);
        
        // 파일 업로드
        String avatarUrl = fileStorageService.uploadAvatar(userId, file);
        
        // 사용자 정보 업데이트
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        
        log.info("프로필 이미지 업로드 완료: userId={}, avatarUrl={}", userId, avatarUrl);
        
        return AvatarUploadResponse.success(avatarUrl);
    }
    
    @Override
    public void changePassword(String userId, ChangePasswordRequest request) {
        log.info("비밀번호 변경 시작: userId={}", userId);
        
        // 입력값 검증
        if (!request.isNewPasswordMatching()) {
            throw new ValidationException(ErrorCodes.PASSWORD_MISMATCH, "새 비밀번호가 일치하지 않습니다");
        }
        
        User user = findById(userId);
        
        // 현재 비밀번호 확인
        if (!passwordService.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCodes.PASSWORD_MISMATCH, CommonMessages.PASSWORD_MISMATCH);
        }
        
        // 새 비밀번호 암호화 및 저장
        String encodedPassword = passwordService.encode(request.getNewPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);
        
        log.info("비밀번호 변경 완료: userId={}", userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public UsernameCheckResponse checkUsername(String username) {
        log.debug("아이디 중복 확인: username={}", username);
        
        boolean exists = userRepository.existsByUsername(username);
        
        if (exists) {
            return UsernameCheckResponse.unavailable(CommonMessages.USERNAME_UNAVAILABLE);
        } else {
            return UsernameCheckResponse.available(CommonMessages.USERNAME_AVAILABLE);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public EmailCheckResponse checkEmail(String email) {
        log.debug("이메일 중복 확인: email={}", email);
        
        boolean exists = userRepository.existsByEmail(email);
        
        if (exists) {
            return EmailCheckResponse.unavailable(CommonMessages.EMAIL_UNAVAILABLE);
        } else {
            return EmailCheckResponse.available(CommonMessages.EMAIL_AVAILABLE);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public User findById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, CommonMessages.USER_NOT_FOUND));
    }
    
    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, CommonMessages.USER_NOT_FOUND));
    }
    
    /**
     * 회원가입 요청 검증
     */
    private void validateRegisterRequest(RegisterRequest request) {
        if (!request.isPasswordMatching()) {
            throw new ValidationException(ErrorCodes.PASSWORD_MISMATCH, "비밀번호가 일치하지 않습니다");
        }
    }
    
    /**
     * 사용자 중복성 검증
     */
    private void validateUserUniqueness(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCodes.DUPLICATE_USERNAME, CommonMessages.USERNAME_UNAVAILABLE);
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCodes.DUPLICATE_EMAIL, CommonMessages.EMAIL_UNAVAILABLE);
        }
        
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(ErrorCodes.DUPLICATE_EMAIL, "이미 사용중인 휴대폰 번호입니다");
        }
    }
    
    /**
     * 사용자 생성
     */
    private User createUser(RegisterRequest request) {
        String encodedPassword = passwordService.encode(request.getPassword());
        
        User user = User.builder()
                .name(request.getName())
                .username(request.getUsername())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(encodedPassword)
                .termsAccepted(request.getTermsAccepted())
                .termsAcceptedAt(LocalDateTime.now())
                .build();
        
        if (request.getTermsAccepted()) {
            user.acceptTerms();
        }
        
        return user;
    }
    
    /**
     * 계정 상태 검증
     */
    private void validateAccountStatus(User user) {
        if (user.isAccountLocked()) {
            throw new BusinessException(ErrorCodes.ACCOUNT_LOCKED, 
                String.format("계정이 %d분간 잠금되었습니다", ACCOUNT_LOCK_DURATION_MINUTES));
        }
        
        if (!user.isActive()) {
            throw new BusinessException(ErrorCodes.ACCOUNT_LOCKED, "계정이 비활성 상태입니다");
        }
    }
    
    /**
     * 로그인 실패 처리
     */
    private void handleLoginFailure(User user) {
        user.incrementFailedLoginAttempts();
        
        if (user.getFailedLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
            user.lockAccount(ACCOUNT_LOCK_DURATION_MINUTES);
            log.warn("계정 잠금: userId={}, attempts={}", user.getId(), user.getFailedLoginAttempts());
        }
        
        userRepository.save(user);
    }
    
    /**
     * 로그인 성공 처리
     */
    private void handleLoginSuccess(User user) {
        user.resetFailedLoginAttempts();
        user.updateLastLoginTime();
        userRepository.save(user);
    }
}