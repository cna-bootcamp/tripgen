package com.unicorn.tripgen.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    
    /**
     * 사용자 아이디
     */
    @NotBlank(message = "아이디는 필수 입력 항목입니다")
    private String username;
    
    /**
     * 비밀번호
     */
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다")
    private String password;
    
    /**
     * 로그인 유지 여부
     */
    @JsonProperty("rememberMe")
    @Builder.Default
    private Boolean rememberMe = false;
}