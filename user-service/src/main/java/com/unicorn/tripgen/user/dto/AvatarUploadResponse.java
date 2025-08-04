package com.unicorn.tripgen.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 아바타 업로드 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvatarUploadResponse {
    
    /**
     * 업로드된 아바타 이미지 URL
     */
    @JsonProperty("avatarUrl")
    private String avatarUrl;
    
    /**
     * 성공 응답 생성 팩토리 메소드
     * 
     * @param avatarUrl 아바타 URL
     * @return AvatarUploadResponse 객체
     */
    public static AvatarUploadResponse success(String avatarUrl) {
        return AvatarUploadResponse.builder()
                .avatarUrl(avatarUrl)
                .build();
    }
}