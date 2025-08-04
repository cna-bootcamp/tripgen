package com.unicorn.tripgen.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.unicorn.tripgen.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 프로필 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    
    /**
     * 사용자 ID
     */
    @JsonProperty("userId")
    private String userId;
    
    /**
     * 사용자 아이디
     */
    private String username;
    
    /**
     * 사용자 이름
     */
    private String name;
    
    /**
     * 이메일 주소
     */
    private String email;
    
    /**
     * 휴대폰 번호
     */
    private String phone;
    
    /**
     * 프로필 이미지 URL
     */
    @JsonProperty("avatarUrl")
    private String avatarUrl;
    
    /**
     * 가입일시
     */
    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 마지막 수정일시
     */
    @JsonProperty("updatedAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /**
     * User 엔티티로부터 UserProfile 생성
     * 
     * @param user User 엔티티
     * @return UserProfile 객체
     */
    public static UserProfile from(User user) {
        return UserProfile.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}