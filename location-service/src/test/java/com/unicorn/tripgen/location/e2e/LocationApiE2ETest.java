package com.unicorn.tripgen.location.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.tripgen.location.dto.*;
import com.unicorn.tripgen.location.entity.Location;
import com.unicorn.tripgen.location.entity.LocationType;
import com.unicorn.tripgen.location.repository.LocationRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
@DisplayName("Location Service E2E 테스트")
public class LocationApiE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LocationRepository locationRepository;

    private static String testLocationId;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비
        Location marienplatz = Location.builder()
                .name("마리엔플라츠")
                .description("뮌헨의 중심 광장")
                .locationType(LocationType.ATTRACTION)
                .latitude(new BigDecimal("48.1374"))
                .longitude(new BigDecimal("11.5755"))
                .address("Marienplatz, 80331 München, Germany")
                .rating(new BigDecimal("4.7"))
                .reviewCount(15234)
                .placeId("ChIJAV7SIeRzXUcR_nQGKCk")
                .isActive(true)
                .build();

        Location savedLocation = locationRepository.save(marienplatz);
        testLocationId = savedLocation.getLocationId();

        // 추가 테스트 데이터
        locationRepository.save(Location.builder()
                .name("노이슈반슈타인 성")
                .description("독일의 유명한 성")
                .locationType(LocationType.ATTRACTION)
                .latitude(new BigDecimal("47.5576"))
                .longitude(new BigDecimal("10.7498"))
                .address("Neuschwansteinstraße 20, 87645 Schwangau, Germany")
                .rating(new BigDecimal("4.8"))
                .reviewCount(25678)
                .placeId("ChIJVRRRRRBm")
                .isActive(true)
                .build());

        locationRepository.save(Location.builder()
                .name("호프브로이하우스")
                .description("뮌헨의 유명한 맥주집")
                .locationType(LocationType.RESTAURANT)
                .latitude(new BigDecimal("48.1378"))
                .longitude(new BigDecimal("11.5798"))
                .address("Platzl 9, 80331 München, Germany")
                .rating(new BigDecimal("4.5"))
                .reviewCount(8945)
                .priceLevel(2)
                .placeId("ChIJxRRRRbRm")
                .isActive(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        locationRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("1. 주변 장소 검색 - 도보 15분 이내")
    void searchNearbyPlaces_Walking() throws Exception {
        // Given
        NearbySearchRequest request = NearbySearchRequest.builder()
                .origin(NearbySearchRequest.Point.builder()
                        .latitude(48.1374)
                        .longitude(11.5755)
                        .build())
                .transportMode("WALKING")
                .travelTime(15)
                .placeTypes(Arrays.asList("ATTRACTION", "RESTAURANT"))
                .build();

        // When & Then
        mockMvc.perform(post("/search/nearby")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places").isArray())
                .andExpect(jsonPath("$.places", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.places[0].name").exists())
                .andExpect(jsonPath("$.places[0].distance").exists())
                .andExpect(jsonPath("$.places[0].estimatedTime").exists())
                .andExpect(jsonPath("$.searchCriteria.transportMode").value("WALKING"))
                .andExpect(jsonPath("$.searchCriteria.timeRange").value(15));
    }

    @Test
    @Order(2)
    @DisplayName("2. 키워드 검색 - 장소명으로 검색")
    void searchByKeyword_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/search/keyword")
                        .param("keyword", "마리엔")
                        .param("latitude", "48.1374")
                        .param("longitude", "11.5755")
                        .param("radius", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results", hasSize(1)))
                .andExpect(jsonPath("$.results[0].name").value("마리엔플라츠"))
                .andExpect(jsonPath("$.results[0].type").value("ATTRACTION"))
                .andExpect(jsonPath("$.results[0].rating").value(4.7))
                .andExpect(jsonPath("$.keyword").value("마리엔"));
    }

    @Test
    @Order(3)
    @DisplayName("3. 장소 상세정보 조회 성공")
    void getLocationDetail_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/{locationId}", testLocationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationId").value(testLocationId))
                .andExpect(jsonPath("$.name").value("마리엔플라츠"))
                .andExpect(jsonPath("$.description").value("뮌헨의 중심 광장"))
                .andExpect(jsonPath("$.type").value("ATTRACTION"))
                .andExpect(jsonPath("$.coordinates.latitude").value(48.1374))
                .andExpect(jsonPath("$.coordinates.longitude").value(11.5755))
                .andExpect(jsonPath("$.rating").value(4.7))
                .andExpect(jsonPath("$.reviewCount").value(15234));
    }

    @Test
    @Order(4)
    @DisplayName("4. 장소 상세정보 조회 실패 - 존재하지 않는 ID")
    void getLocationDetail_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/{locationId}", "non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("장소를 찾을 수 없습니다"));
    }

    @Test
    @Order(5)
    @DisplayName("5. 날씨 정보 조회 성공")
    void getWeather_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/weather")
                        .param("latitude", "48.1374")
                        .param("longitude", "11.5755"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").exists())
                .andExpect(jsonPath("$.current.temperature").exists())
                .andExpect(jsonPath("$.current.description").exists())
                .andExpect(jsonPath("$.forecast").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("6. 경로 정보 조회 성공")
    void getRoute_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/route")
                        .param("origin", "48.1374,11.5755")
                        .param("destination", "48.1378,11.5798")
                        .param("mode", "WALKING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routes").isArray())
                .andExpect(jsonPath("$.routes[0].distance").exists())
                .andExpect(jsonPath("$.routes[0].duration").exists())
                .andExpect(jsonPath("$.routes[0].steps").isArray())
                .andExpect(jsonPath("$.mode").value("WALKING"));
    }

    @Test
    @Order(7)
    @DisplayName("7. 주변 장소 검색 - 자동차 30분 이내")
    void searchNearbyPlaces_Car() throws Exception {
        // Given
        NearbySearchRequest request = NearbySearchRequest.builder()
                .origin(NearbySearchRequest.Point.builder()
                        .latitude(48.1374)
                        .longitude(11.5755)
                        .build())
                .transportMode("CAR")
                .travelTime(30)
                .placeTypes(Arrays.asList("ATTRACTION"))
                .build();

        // When & Then
        mockMvc.perform(post("/search/nearby")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places").isArray())
                .andExpect(jsonPath("$.searchCriteria.transportMode").value("CAR"))
                .andExpect(jsonPath("$.searchCriteria.timeRange").value(30));
    }

    @Test
    @Order(8)
    @DisplayName("8. 키워드 검색 - 빈 결과")
    void searchByKeyword_EmptyResult() throws Exception {
        // When & Then
        mockMvc.perform(get("/search/keyword")
                        .param("keyword", "존재하지않는장소")
                        .param("latitude", "48.1374")
                        .param("longitude", "11.5755")
                        .param("radius", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results", hasSize(0)))
                .andExpect(jsonPath("$.totalCount").value(0));
    }

    @Test
    @Order(9)
    @DisplayName("9. 주변 장소 검색 실패 - 유효하지 않은 이동수단")
    void searchNearbyPlaces_InvalidTransportMode() throws Exception {
        // Given
        NearbySearchRequest request = NearbySearchRequest.builder()
                .origin(NearbySearchRequest.Point.builder()
                        .latitude(48.1374)
                        .longitude(11.5755)
                        .build())
                .transportMode("INVALID_MODE")
                .timeRange(15)
                .build();

        // When & Then
        mockMvc.perform(post("/search/nearby")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @Order(10)
    @DisplayName("10. 경로 정보 조회 실패 - 잘못된 좌표")
    void getRoute_InvalidCoordinates() throws Exception {
        // When & Then
        mockMvc.perform(get("/route")
                        .param("origin", "invalid-coordinates")
                        .param("destination", "48.1378,11.5798")
                        .param("mode", "WALKING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_COORDINATES"));
    }
}