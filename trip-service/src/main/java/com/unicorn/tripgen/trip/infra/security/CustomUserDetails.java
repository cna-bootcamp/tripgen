package com.unicorn.tripgen.trip.infra.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 사용자 정보를 담는 UserDetails 구현체
 */
public class CustomUserDetails implements UserDetails {
    
    private final String userId;
    private final Collection<? extends GrantedAuthority> authorities;
    
    public CustomUserDetails(String userId, Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.authorities = authorities;
    }
    
    public CustomUserDetails(String userId) {
        this.userId = userId;
        this.authorities = List.of(); // 기본 권한 없음
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public String getPassword() {
        return null; // JWT 기반 인증에서는 비밀번호 불필요
    }
    
    @Override
    public String getUsername() {
        return userId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
}