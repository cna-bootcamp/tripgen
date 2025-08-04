package com.unicorn.tripgen.ai.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.tripgen.ai.dto.*;
import com.unicorn.tripgen.ai.entity.AIJob;
import com.unicorn.tripgen.ai.entity.AIModelType;
import com.unicorn.tripgen.ai.entity.JobStatus;
import com.unicorn.tripgen.ai.repository.AIJobRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
@DisplayName("AI Service E2E 테스트")
public class AIApiE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AIJobRepository aiJobRepository;

    private static Long scheduleJobId = 1L;
    private static Long recommendationJobId = 2L;

    @Test
    @Order(1)
    @DisplayName("1. AI 일정 생성 요청 성공")
    void generateSchedule_Success() throws Exception {
        // Given
        GenerateScheduleRequest request = GenerateScheduleRequest.builder()
                .tripId("trip-123")
                .tripName("독일 여행")
                .transportMode("car")
                .startTime("09:00")
                .userId("user-456")
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(10))
                .destinations(Arrays.asList(
                        GenerateScheduleRequest.DestinationInfo.builder()
                                .destinationName("뮌헨")
                                .nights(2)
                                .startDate(LocalDate.now().plusDays(7))
                                .endDate(LocalDate.now().plusDays(9))
                                .accommodation("뮌헨 호텔")
                                .checkInTime("15:00")
                                .checkOutTime("11:00")
                                .build(),
                        GenerateScheduleRequest.DestinationInfo.builder()
                                .destinationName("노이슈반슈타인")
                                .nights(1)
                                .startDate(LocalDate.now().plusDays(9))
                                .endDate(LocalDate.now().plusDays(10))
                                .accommodation("성 근처 호텔")
                                .checkInTime("15:00")
                                .checkOutTime("11:00")
                                .build()
                ))
                .preferences(Map.of(
                        "pace", "moderate",
                        "interests", Arrays.asList("culture", "history"),
                        "budget", "medium"
                ))
                .members(Arrays.asList(
                        GenerateScheduleRequest.MemberInfo.builder()
                                .name("홍길동")
                                .age(35)
                                .gender("male")
                                .healthStatus("good")
                                .preferences(Arrays.asList("culture", "history"))
                                .build(),
                        GenerateScheduleRequest.MemberInfo.builder()
                                .name("김영희")
                                .age(32)
                                .gender("female")
                                .healthStatus("good")
                                .preferences(Arrays.asList("nature", "shopping"))
                                .build()
                ))
                .constraints(Map.of(
                        "dailyStartTime", "09:00",
                        "dailyEndTime", "21:00"
                ))
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/schedule/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("일정 생성이 시작되었습니다"))
                .andExpect(jsonPath("$.estimatedTime").value("약 3-5분"))
                .andReturn();

        GenerateScheduleResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                GenerateScheduleResponse.class
        );
        scheduleJobId = Long.parseLong(response.getJobId());
    }

    @Test
    @Order(2)
    @DisplayName("2. AI 일정 생성 상태 조회 - 처리중")
    void getScheduleJobStatus_Processing() throws Exception {
        // Given
        AIJob processingJob = AIJob.builder()
                .requestId("req-123")
                .jobType("SCHEDULE_GENERATION")
                .tripId("trip-123")
                .status(JobStatus.PROCESSING)
                .aiModelType(AIModelType.OPENAI_GPT4)
                .progress(45)
                .currentStep("일정 생성 중")
                .build();
        processingJob.setId(scheduleJobId.toString());
        
        when(aiJobRepository.findById(scheduleJobId))
                .thenReturn(Optional.of(processingJob));

        // When & Then
        mockMvc.perform(get("/schedule/status/{jobId}", scheduleJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(scheduleJobId))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.progress").value(45))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    @Order(3)
    @DisplayName("3. AI 일정 생성 상태 조회 - 완료")
    void getScheduleJobStatus_Completed() throws Exception {
        // Given
        Map<String, Object> scheduleResult = new HashMap<>();
        scheduleResult.put("totalDays", 4);
        scheduleResult.put("schedules", Arrays.asList(
                Map.of(
                        "day", 1,
                        "date", "2025-08-11",
                        "places", Arrays.asList(
                                Map.of(
                                        "name", "마리엔플라츠",
                                        "time", "09:00-11:00",
                                        "duration", 120
                                )
                        )
                )
        ));

        AIJob completedJob = AIJob.builder()
                .requestId("req-123")
                .jobType("SCHEDULE_GENERATION")
                .tripId("trip-123")
                .status(JobStatus.COMPLETED)
                .aiModelType(AIModelType.OPENAI_GPT4)
                .progress(100)
                .resultData(objectMapper.writeValueAsString(scheduleResult))
                .build();
        completedJob.setId(scheduleJobId.toString());
        
        when(aiJobRepository.findById(scheduleJobId))
                .thenReturn(Optional.of(completedJob));

        // When & Then
        mockMvc.perform(get("/schedule/status/{jobId}", scheduleJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(scheduleJobId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progress").value(100))
                .andExpect(jsonPath("$.result").exists())
                .andExpect(jsonPath("$.result.totalDays").value(4))
                .andExpect(jsonPath("$.result.schedules").isArray());
    }

    @Test
    @Order(4)
    @DisplayName("4. AI 추천 정보 생성 요청 성공")
    void generateRecommendation_Success() throws Exception {
        // Given
        RecommendationRequest request = RecommendationRequest.builder()
                .placeName("뮌헨 레스토랑")
                .placeType("RESTAURANT")
                .placeAddress("뮌헨, 독일")
                .userProfile(RecommendationRequest.UserProfile.builder()
                        .memberComposition("couple")
                        .healthStatus("good")
                        .transportMode("walking")
                        .preferences(Arrays.asList("german", "italian"))
                        .build())
                .tripContext(RecommendationRequest.TripContext.builder()
                        .visitDate(LocalDate.now().plusDays(7))
                        .visitTime("19:00")
                        .previousPlace("호텔")
                        .nextPlace("비어 홀")
                        .build())
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/recommendation/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("추천 정보 생성이 시작되었습니다"))
                .andReturn();

        RecommendationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                RecommendationResponse.class
        );
        recommendationJobId = Long.parseLong(response.getJobId());
    }

    @Test
    @Order(5)
    @DisplayName("5. AI 추천 정보 상태 조회 - 완료")
    void getRecommendationJobStatus_Completed() throws Exception {
        // Given
        Map<String, Object> recommendationResult = new HashMap<>();
        recommendationResult.put("recommendations", Arrays.asList(
                Map.of(
                        "name", "호프브로이하우스",
                        "type", "RESTAURANT",
                        "rating", 4.5,
                        "reason", "뮌헨의 대표적인 맥주집으로 독일 전통 음식을 즐길 수 있습니다",
                        "distance", 450
                )
        ));

        AIJob completedJob = AIJob.builder()
                .requestId("req-rec-456")
                .jobType("RECOMMENDATION_GENERATION")
                .tripId("trip-456")
                .status(JobStatus.COMPLETED)
                .aiModelType(AIModelType.OPENAI_GPT4)
                .progress(100)
                .resultData(objectMapper.writeValueAsString(recommendationResult))
                .build();
        completedJob.setId(recommendationJobId.toString());
        
        when(aiJobRepository.findById(recommendationJobId))
                .thenReturn(Optional.of(completedJob));

        // When & Then
        mockMvc.perform(get("/recommendation/status/{jobId}", recommendationJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(recommendationJobId))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.recommendations").isArray())
                .andExpect(jsonPath("$.result.recommendations[0].name").value("호프브로이하우스"))
                .andExpect(jsonPath("$.result.recommendations[0].type").value("RESTAURANT"));
    }

    @Test
    @Order(6)
    @DisplayName("6. AI 일정 생성 요청 실패 - 유효성 검증")
    void generateSchedule_ValidationFailure() throws Exception {
        // Given
        GenerateScheduleRequest request = GenerateScheduleRequest.builder()
                .tripId("trip-123")
                .userId("user-456")
                .startDate(LocalDate.now().plusDays(7))
                .endDate(LocalDate.now().plusDays(3)) // 종료일이 시작일보다 이전
                .build();

        // When & Then
        mockMvc.perform(post("/schedule/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @Order(7)
    @DisplayName("7. 작업 상태 조회 실패 - 존재하지 않는 작업")
    void getJobStatus_NotFound() throws Exception {
        // Given
        when(aiJobRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/schedule/status/{jobId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("작업을 찾을 수 없습니다"));
    }

    @Test
    @Order(8)
    @DisplayName("8. AI 일정 생성 상태 조회 - 실패")
    void getScheduleJobStatus_Failed() throws Exception {
        // Given
        AIJob failedJob = AIJob.builder()
                .requestId("req-failed")
                .jobType("SCHEDULE_GENERATION")
                .tripId("trip-123")
                .status(JobStatus.FAILED)
                .aiModelType(AIModelType.OPENAI_GPT4)
                .progress(0)
                .errorMessage("AI 모델 응답 시간 초과")
                .build();
        failedJob.setId("3");
        
        when(aiJobRepository.findById(3L))
                .thenReturn(Optional.of(failedJob));

        // When & Then
        mockMvc.perform(get("/schedule/status/{jobId}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(3))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value("AI 모델 응답 시간 초과"));
    }

    @Test
    @Order(9)
    @DisplayName("9. AI 추천 정보 생성 - 빈 선호도")
    void generateRecommendation_EmptyPreferences() throws Exception {
        // Given
        RecommendationRequest request = RecommendationRequest.builder()
                .placeName("베를린 광장")
                .placeType("ATTRACTION")
                .placeAddress("베를린, 독일")
                .userProfile(RecommendationRequest.UserProfile.builder()
                        .memberComposition("family")
                        .healthStatus("good")
                        .transportMode("car")
                        .preferences(Arrays.asList())
                        .build())
                .build();

        // When & Then
        mockMvc.perform(post("/recommendation/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @Order(10)
    @DisplayName("10. AI 일정 생성 - 복잡한 제약조건")
    void generateSchedule_ComplexConstraints() throws Exception {
        // Given
        GenerateScheduleRequest request = GenerateScheduleRequest.builder()
                .tripId("trip-789")
                .tripName("프랑스 남부 여행")
                .transportMode("public")
                .startTime("10:00")
                .userId("user-101")
                .startDate(LocalDate.now().plusDays(14))
                .endDate(LocalDate.now().plusDays(21))
                .destinations(Arrays.asList(
                        GenerateScheduleRequest.DestinationInfo.builder()
                                .destinationName("파리")
                                .nights(3)
                                .startDate(LocalDate.now().plusDays(14))
                                .endDate(LocalDate.now().plusDays(17))
                                .accommodation("파리 호텔")
                                .checkInTime("15:00")
                                .checkOutTime("11:00")
                                .build(),
                        GenerateScheduleRequest.DestinationInfo.builder()
                                .destinationName("니스")
                                .nights(3)
                                .startDate(LocalDate.now().plusDays(17))
                                .endDate(LocalDate.now().plusDays(20))
                                .accommodation("니스 리조트")
                                .checkInTime("15:00")
                                .checkOutTime("11:00")
                                .build(),
                        GenerateScheduleRequest.DestinationInfo.builder()
                                .destinationName("모나코")
                                .nights(1)
                                .startDate(LocalDate.now().plusDays(20))
                                .endDate(LocalDate.now().plusDays(21))
                                .accommodation("모나코 호텔")
                                .checkInTime("15:00")
                                .checkOutTime("11:00")
                                .build()
                ))
                .preferences(Map.of(
                        "pace", "relaxed",
                        "interests", Arrays.asList("art", "food", "beach"),
                        "budget", "high"
                ))
                .members(Arrays.asList(
                        GenerateScheduleRequest.MemberInfo.builder()
                                .name("김철수")
                                .age(65)
                                .gender("male")
                                .healthStatus("limited")
                                .preferences(Arrays.asList("art", "history"))
                                .build(),
                        GenerateScheduleRequest.MemberInfo.builder()
                                .name("박영희")
                                .age(62)
                                .gender("female")
                                .healthStatus("good")
                                .preferences(Arrays.asList("shopping", "food"))
                                .build()
                ))
                .constraints(Map.of(
                        "dailyStartTime", "10:00",
                        "dailyEndTime", "18:00",
                        "maxWalkingDistance", 2000,
                        "requiredRestTime", 120,
                        "avoidStairs", true
                ))
                .build();

        // When & Then
        mockMvc.perform(post("/schedule/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.estimatedTime").value("약 3-5분"));
    }
}