package com.unicorn.tripgen.user.entity;

/**
 * 사용자 상태 열거형
 */
public enum UserStatus {
    
    /**
     * 활성 상태
     */
    ACTIVE("활성"),
    
    /**
     * 비활성 상태
     */
    INACTIVE("비활성"),
    
    /**
     * 잠금 상태
     */
    LOCKED("잠금"),
    
    /**
     * 휴면 상태
     */
    DORMANT("휴면"),
    
    /**
     * 탈퇴 상태
     */
    WITHDRAWN("탈퇴");
    
    private final String description;
    
    /**
     * UserStatus 생성자
     * 
     * @param description 상태 설명
     */
    UserStatus(String description) {
        this.description = description;
    }
    
    /**
     * 상태 설명 반환
     * 
     * @return 상태 설명
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 활성 상태인지 확인
     * 
     * @return 활성 상태 여부
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * 잠금 상태인지 확인
     * 
     * @return 잠금 상태 여부
     */
    public boolean isLocked() {
        return this == LOCKED;
    }
}