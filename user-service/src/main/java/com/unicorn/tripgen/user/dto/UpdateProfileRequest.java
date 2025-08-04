package com.unicorn.tripgen.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 프로필 수정 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {
    
    /**
     * 사용자 이름
     */
    @Size(min = 2, max = 100, message = "이름은 2자 이상 100자 이하로 입력해주세요")
    private String name;
    
    /**
     * 휴대폰 번호
     */
    @Pattern(regexp = "^01[0-9]{1}-[0-9]{3,4}-[0-9]{4}$", 
             message = "휴대폰 번호는 010-1234-5678 형식으로 입력해주세요")
    private String phone;
    
    /**
     * 이메일 주소 (변경 시 재인증 필요)
     */
    @Email(message = "올바른 이메일 형식을 입력해주세요")
    @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요")
    private String email;
}