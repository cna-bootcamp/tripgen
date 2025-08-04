package com.unicorn.tripgen.trip.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.tripgen.trip.biz.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
@DisplayName("Trip Service E2E 테스트")
public class TripApiE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String tripId;
    private static String memberId;
    private static String destinationId;
    private static final String AUTH_TOKEN = "Bearer test-token";

    @Test
    @Order(1)
    @DisplayName("1. 여행 생성 성공")
    void createTrip_Success() throws Exception {
        // Given
        CreateTripRequest request = CreateTripRequest.builder()
                .title("독일 가족 여행")
                .description("뮌헨과 노이슈반슈타인 성 방문")
                .startDate(LocalDate.now().plusDays(30))
                .endDate(LocalDate.now().plusDays(37))
                .userId("user-123")
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/trips")
                        .header("Authorization", AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tripId").exists())
                .andExpect(jsonPath("$.title").value("독일 가족 여행"))
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        CreateTripResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CreateTripResponse.class
        );
        tripId = response.getTripId();
    }

    @Test
    @Order(2)
    @DisplayName("2. 여행 상세 조회 성공")
    void getTripDetail_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/trips/{tripId}", tripId)
                        .header("Authorization", AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(tripId))
                .andExpect(jsonPath("$.title").value("독일 가족 여행"))
                .andExpect(jsonPath("$.description").value("뮌헨과 노이슈반슈타인 성 방문"))
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.destinations").isArray());
    }

    @Test
    @Order(3)
    @DisplayName("3. 멤버 추가 성공")
    void addMember_Success() throws Exception {
        // Given
        AddMemberRequest request = AddMemberRequest.builder()
                .name("김철수")
                .age(35)
                .gender("M")
                .relationship("본인")
                .preferences(Arrays.asList("culture", "history"))
                .healthStatus("NORMAL")
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/trips/{tripId}/members", tripId)
                        .header("Authorization", AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").exists())
                .andExpect(jsonPath("$.name").value("김철수"))
                .andExpect(jsonPath("$.age").value(35))
                .andExpect(jsonPath("$.gender").value("M"))
                .andReturn();

        MemberResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                MemberResponse.class
        );
        memberId = response.getMemberId();
    }

    @Test
    @Order(4)
    @DisplayName("4. 멤버 목록 조회 성공")
    void getMembers_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/trips/{tripId}/members", tripId)
                        .header("Authorization", AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members", hasSize(1)))
                .andExpect(jsonPath("$.members[0].name").value("김철수"))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    @Order(5)
    @DisplayName("5. 여행지 추가 성공")
    void addDestination_Success() throws Exception {
        // Given
        AddDestinationRequest request = AddDestinationRequest.builder()
                .city("뮌헨")
                .country("독일")
                .arrivalDate(LocalDate.now().plusDays(10))
                .departureDate(LocalDate.now().plusDays(13))
                .latitude(48.1374)
                .longitude(11.5755)
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/trips/{tripId}/destinations", tripId)
                        .header("Authorization", AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.destinationId").exists())
                .andExpect(jsonPath("$.name").value("뮌헨"))
                .andExpect(jsonPath("$.country").value("독일"))
                .andExpect(jsonPath("$.plannedDays").value(3))
                .andReturn();

        DestinationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                DestinationResponse.class
        );
        destinationId = response.getDestinationId();
    }

    @Test
    @Order(6)
    @DisplayName("6. 여행지 목록 조회 성공")
    void getDestinations_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/trips/{tripId}/destinations", tripId)
                        .header("Authorization", AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinations").isArray())
                .andExpect(jsonPath("$.destinations", hasSize(1)))
                .andExpect(jsonPath("$.destinations[0].name").value("뮌헨"))
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    @Order(7)
    @DisplayName("7. AI 일정 생성 요청 성공")
    void generateSchedule_Success() throws Exception {
        // Given
        GenerateScheduleRequest request = GenerateScheduleRequest.builder()
                .aiModel("gpt-4")
                .preferences(GenerateScheduleRequest.Preferences.builder()
                        .pace("moderate")
                        .activityTypes(Arrays.asList("culture", "food"))
                        .budgetLevel("medium")
                        .build())
                .constraints(GenerateScheduleRequest.Constraints.builder()
                        .startTime("09:00")
                        .endTime("21:00")
                        .dailyBudget(100000)
                        .build())
                .build();

        // When & Then
        mockMvc.perform(post("/trips/{tripId}/schedule/generate", tripId)
                        .header("Authorization", AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("일정 생성이 시작되었습니다"))
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.estimatedTime").exists());
    }

    @Test
    @Order(8)
    @DisplayName("8. 일정 조회 성공")
    void getSchedule_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/trips/{tripId}/schedule", tripId)
                        .header("Authorization", AUTH_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules").isArray())
                .andExpect(jsonPath("$.totalDays").exists())
                .andExpect(jsonPath("$.lastUpdated").exists());
    }

    @Test
    @Order(9)
    @DisplayName("9. 여행 정보 수정 성공")
    void updateTrip_Success() throws Exception {
        // Given
        UpdateTripRequest request = UpdateTripRequest.builder()
                .title("독일 & 오스트리아 가족 여행")
                .description("뮌헨, 노이슈반슈타인 성, 잘츠부르크 방문")
                .endDate(LocalDate.now().plusDays(40)) // 일정 연장
                .build();

        // When & Then
        mockMvc.perform(put("/trips/{tripId}", tripId)
                        .header("Authorization", AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("독일 & 오스트리아 가족 여행"))
                .andExpect(jsonPath("$.description").value("뮌헨, 노이슈반슈타인 성, 잘츠부르크 방문"));
    }

    @Test
    @Order(10)
    @DisplayName("10. 멤버 정보 수정 성공")
    void updateMember_Success() throws Exception {
        // Given
        UpdateMemberRequest request = UpdateMemberRequest.builder()
                .preferences(Arrays.asList("culture", "history", "food"))
                .healthStatus("PREGNANT") // 건강 상태 변경
                .build();

        // When & Then
        mockMvc.perform(put("/trips/{tripId}/members/{memberId}", tripId, memberId)
                        .header("Authorization", AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.healthStatus").value("PREGNANT"))
                .andExpect(jsonPath("$.specialNeeds").value("임신 중이므로 무리한 일정 피하기"));
    }

    @Test
    @Order(11)
    @DisplayName("11. 일정 내보내기 성공")
    void exportSchedule_Success() throws Exception {
        // Given
        ExportScheduleRequest request = ExportScheduleRequest.builder()
                .format("PDF")
                .includeMap(true)
                .includeDetails(true)
                .build();

        // When & Then
        mockMvc.perform(post("/trips/{tripId}/schedule/export", tripId)
                        .header("Authorization", AUTH_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportUrl").exists())
                .andExpect(jsonPath("$.format").value("PDF"))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @Order(12)
    @DisplayName("12. 멤버 삭제 성공")
    void deleteMember_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/trips/{tripId}/members/{memberId}", tripId, memberId)
                        .header("Authorization", AUTH_TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(13)
    @DisplayName("13. 여행지 삭제 성공")
    void deleteDestination_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/trips/{tripId}/destinations/{destinationId}", tripId, destinationId)
                        .header("Authorization", AUTH_TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(14)
    @DisplayName("14. 여행 삭제 성공")
    void deleteTrip_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/trips/{tripId}", tripId)
                        .header("Authorization", AUTH_TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(15)
    @DisplayName("15. 삭제된 여행 조회 실패")
    void getTripDetail_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/trips/{tripId}", tripId)
                        .header("Authorization", AUTH_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("여행을 찾을 수 없습니다"));
    }
}