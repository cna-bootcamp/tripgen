package com.unicorn.tripgen.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    
    /**
     * 사용자 이름 (한글/영문)
     */
    @NotBlank(message = "이름은 필수 입력 항목입니다")
    @Size(min = 2, max = 100, message = "이름은 2자 이상 100자 이하로 입력해주세요")
    private String name;
    
    /**
     * 이메일 주소
     */
    @NotBlank(message = "이메일은 필수 입력 항목입니다")
    @Email(message = "올바른 이메일 형식을 입력해주세요")
    @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요")
    private String email;
    
    /**
     * 휴대폰 번호
     */
    @NotBlank(message = "휴대폰 번호는 필수 입력 항목입니다")
    @Pattern(regexp = "^01[0-9]{1}-[0-9]{3,4}-[0-9]{4}$", 
             message = "휴대폰 번호는 010-1234-5678 형식으로 입력해주세요")
    private String phone;
    
    /**
     * 아이디 (5자 이상 영문/숫자)
     */
    @NotBlank(message = "아이디는 필수 입력 항목입니다")
    @Size(min = 5, max = 50, message = "아이디는 5자 이상 50자 이하로 입력해주세요")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", 
             message = "아이디는 영문자와 숫자만 사용할 수 있습니다")
    private String username;
    
    /**
     * 비밀번호 (8자 이상, 영문/숫자/특수문자 포함)
     */
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다")
    @Size(min = 8, max = 255, message = "비밀번호는 8자 이상 255자 이하로 입력해주세요")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
             message = "비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 포함해야 합니다")
    private String password;
    
    /**
     * 비밀번호 확인
     */
    @NotBlank(message = "비밀번호 확인은 필수 입력 항목입니다")
    @JsonProperty("passwordConfirm")
    private String passwordConfirm;
    
    /**
     * 이용약관 동의 여부
     */
    @NotNull(message = "이용약관 동의는 필수입니다")
    @AssertTrue(message = "이용약관에 동의해야 회원가입이 가능합니다")
    @JsonProperty("termsAccepted")
    private Boolean termsAccepted;
    
    /**
     * 비밀번호와 비밀번호 확인이 일치하는지 검증
     * 
     * @return 비밀번호 일치 여부
     */
    public boolean isPasswordMatching() {
        return password != null && password.equals(passwordConfirm);
    }
}