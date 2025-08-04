package com.unicorn.tripgen.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 변경 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {
    
    /**
     * 현재 비밀번호
     */
    @NotBlank(message = "현재 비밀번호는 필수 입력 항목입니다")
    @JsonProperty("currentPassword")
    private String currentPassword;
    
    /**
     * 새 비밀번호
     */
    @NotBlank(message = "새 비밀번호는 필수 입력 항목입니다")
    @Size(min = 8, max = 255, message = "비밀번호는 8자 이상 255자 이하로 입력해주세요")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
             message = "비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 포함해야 합니다")
    @JsonProperty("newPassword")
    private String newPassword;
    
    /**
     * 새 비밀번호 확인
     */
    @NotBlank(message = "새 비밀번호 확인은 필수 입력 항목입니다")
    @JsonProperty("newPasswordConfirm")
    private String newPasswordConfirm;
    
    /**
     * 새 비밀번호와 새 비밀번호 확인이 일치하는지 검증
     * 
     * @return 비밀번호 일치 여부
     */
    public boolean isNewPasswordMatching() {
        return newPassword != null && newPassword.equals(newPasswordConfirm);
    }
}