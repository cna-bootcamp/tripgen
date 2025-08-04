package com.unicorn.tripgen.user.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.tripgen.user.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
@DisplayName("User Service E2E 테스트")
public class UserApiE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String accessToken;
    private static String userId;
    private static final String TEST_USERNAME = "testuser" + System.currentTimeMillis();
    private static final String TEST_EMAIL = "test" + System.currentTimeMillis() + "@tripgen.com";
    private static final String TEST_PASSWORD = "TestPass123!";

    @Test
    @Order(1)
    @DisplayName("1. 아이디 중복 확인 - 사용 가능한 아이디")
    void checkUsername_Available() throws Exception {
        // When & Then
        mockMvc.perform(get("/check/username/{username}", TEST_USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.message").value("사용 가능한 아이디입니다"));
    }

    @Test
    @Order(2)
    @DisplayName("2. 이메일 중복 확인 - 사용 가능한 이메일")
    void checkEmail_Available() throws Exception {
        // When & Then
        mockMvc.perform(get("/check/email/{email}", TEST_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.message").value("사용 가능한 이메일입니다"));
    }

    @Test
    @Order(3)
    @DisplayName("3. 회원가입 성공")
    void registerUser_Success() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .name("테스트유저")
                .email(TEST_EMAIL)
                .phone("010-1234-5678")
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .passwordConfirm(TEST_PASSWORD)
                .termsAccepted(true)
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다"))
                .andReturn();

        RegisterResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                RegisterResponse.class
        );
        userId = response.getUserId();
    }

    @Test
    @Order(4)
    @DisplayName("4. 아이디 중복 확인 - 이미 사용중인 아이디")
    void checkUsername_Unavailable() throws Exception {
        // When & Then
        mockMvc.perform(get("/check/username/{username}", TEST_USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용중인 아이디입니다"));
    }

    @Test
    @Order(5)
    @DisplayName("5. 회원가입 실패 - 중복된 아이디")
    void registerUser_DuplicateUsername() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .name("다른유저")
                .email("another@tripgen.com")
                .phone("010-9876-5432")
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .passwordConfirm(TEST_PASSWORD)
                .termsAccepted(true)
                .build();

        // When & Then
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_USER"))
                .andExpect(jsonPath("$.message").value("이미 사용중인 아이디입니다"));
    }

    @Test
    @Order(6)
    @DisplayName("6. 로그인 성공")
    void loginUser_Success() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD)
                .rememberMe(false)
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.userId").value(userId))
                .andExpect(jsonPath("$.user.username").value(TEST_USERNAME))
                .andReturn();

        LoginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponse.class
        );
        accessToken = response.getAccessToken();
    }

    @Test
    @Order(7)
    @DisplayName("7. 로그인 실패 - 잘못된 비밀번호")
    void loginUser_InvalidPassword() throws Exception {
        // Given
        LoginRequest request = LoginRequest.builder()
                .username(TEST_USERNAME)
                .password("WrongPassword123!")
                .build();

        // When & Then
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호를 확인해주세요"));
    }

    @Test
    @Order(8)
    @DisplayName("8. 프로필 조회 성공")
    void getProfile_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.name").value("테스트유저"))
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.phone").value("010-1234-5678"));
    }

    @Test
    @Order(9)
    @DisplayName("9. 프로필 조회 실패 - 인증 없음")
    void getProfile_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("인증이 필요합니다"));
    }

    @Test
    @Order(10)
    @DisplayName("10. 프로필 수정 성공")
    void updateProfile_Success() throws Exception {
        // Given
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .name("수정된이름")
                .phone("010-1111-2222")
                .build();

        // When & Then
        mockMvc.perform(put("/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("수정된이름"))
                .andExpect(jsonPath("$.phone").value("010-1111-2222"));
    }

    @Test
    @Order(11)
    @DisplayName("11. 비밀번호 변경 성공")
    void changePassword_Success() throws Exception {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword(TEST_PASSWORD)
                .newPassword("NewTestPass456!")
                .newPasswordConfirm("NewTestPass456!")
                .build();

        // When & Then
        mockMvc.perform(put("/profile/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("비밀번호가 성공적으로 변경되었습니다"));
    }

    @Test
    @Order(12)
    @DisplayName("12. 비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void changePassword_WrongCurrentPassword() throws Exception {
        // Given
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("WrongCurrentPassword!")
                .newPassword("NewTestPass789!")
                .newPasswordConfirm("NewTestPass789!")
                .build();

        // When & Then
        mockMvc.perform(put("/profile/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PASSWORD"))
                .andExpect(jsonPath("$.message").value("현재 비밀번호가 일치하지 않습니다"));
    }

    @Test
    @Order(13)
    @DisplayName("13. 로그아웃 성공")
    void logoutUser_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("안전하게 로그아웃되었습니다"));
    }

    @Test
    @Order(14)
    @DisplayName("14. 로그아웃 후 프로필 조회 실패")
    void getProfile_AfterLogout() throws Exception {
        // When & Then
        mockMvc.perform(get("/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(15)
    @DisplayName("15. 회원가입 실패 - 유효성 검증")
    void registerUser_ValidationFailure() throws Exception {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .name("A") // 너무 짧은 이름
                .email("invalid-email") // 잘못된 이메일 형식
                .phone("123456") // 잘못된 전화번호 형식
                .username("ab") // 너무 짧은 아이디
                .password("weak") // 약한 비밀번호
                .passwordConfirm("different") // 비밀번호 불일치
                .termsAccepted(false) // 약관 미동의
                .build();

        // When & Then
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}