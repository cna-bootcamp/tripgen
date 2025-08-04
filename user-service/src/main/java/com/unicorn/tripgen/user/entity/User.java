package com.unicorn.tripgen.user.entity;

import com.unicorn.tripgen.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 * 사용자 기본 정보와 인증 관련 정보를 관리
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_username", columnList = "username", unique = true),
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_phone", columnList = "phone"),
    @Index(name = "idx_user_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseAuditEntity {
    
    /**
     * 사용자 이름 (실명)
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    /**
     * 사용자 아이디 (로그인용)
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    /**
     * 이메일 주소
     */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    /**
     * 휴대폰 번호
     */
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;
    
    /**
     * 암호화된 비밀번호
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;
    
    /**
     * 프로필 이미지 URL
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    
    /**
     * 사용자 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;
    
    /**
     * 로그인 실패 횟수
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;
    
    /**
     * 계정 잠금 시간
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    /**
     * 마지막 로그인 시간
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    /**
     * 이용약관 동의 여부
     */
    @Column(name = "terms_accepted", nullable = false)
    @Builder.Default
    private boolean termsAccepted = false;
    
    /**
     * 이용약관 동의 시간
     */
    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;
    
    /**
     * 로그인 실패 횟수 증가
     */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }
    
    /**
     * 로그인 실패 횟수 초기화
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }
    
    /**
     * 계정 잠금 처리
     * 
     * @param lockDurationMinutes 잠금 시간 (분)
     */
    public void lockAccount(int lockDurationMinutes) {
        this.status = UserStatus.LOCKED;
        this.lockedUntil = LocalDateTime.now().plusMinutes(lockDurationMinutes);
    }
    
    /**
     * 계정 잠금 해제 처리
     */
    public void unlockAccount() {
        if (this.status == UserStatus.LOCKED) {
            this.status = UserStatus.ACTIVE;
            this.lockedUntil = null;
            this.failedLoginAttempts = 0;
        }
    }
    
    /**
     * 계정이 현재 잠금 상태인지 확인
     * 
     * @return 잠금 상태 여부
     */
    public boolean isAccountLocked() {
        if (this.status != UserStatus.LOCKED) {
            return false;
        }
        
        if (this.lockedUntil == null) {
            return true;
        }
        
        if (LocalDateTime.now().isBefore(this.lockedUntil)) {
            return true;
        }
        
        // 잠금 시간이 지났으면 자동으로 해제
        unlockAccount();
        return false;
    }
    
    /**
     * 마지막 로그인 시간 업데이트
     */
    public void updateLastLoginTime() {
        this.lastLoginAt = LocalDateTime.now();
    }
    
    /**
     * 이용약관 동의 처리
     */
    public void acceptTerms() {
        this.termsAccepted = true;
        this.termsAcceptedAt = LocalDateTime.now();
    }
    
    /**
     * 계정이 활성 상태인지 확인
     * 
     * @return 활성 상태 여부
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE && !isAccountLocked();
    }
}