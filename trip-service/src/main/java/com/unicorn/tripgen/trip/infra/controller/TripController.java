package com.unicorn.tripgen.trip.infra.controller;

import com.unicorn.tripgen.trip.biz.domain.TransportMode;
import com.unicorn.tripgen.trip.biz.domain.TripStatus;
import com.unicorn.tripgen.trip.biz.dto.*;
import com.unicorn.tripgen.trip.biz.usecase.in.TripUseCase;
import com.unicorn.tripgen.trip.biz.usecase.in.MemberUseCase;
import com.unicorn.tripgen.trip.biz.domain.Gender;
import com.unicorn.tripgen.trip.biz.domain.HealthStatus;
import com.unicorn.tripgen.trip.biz.domain.Preference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 여행 관련 REST API Controller
 * Clean Architecture의 Infrastructure Layer - Web Interface
 */
@Tag(name = "trips", description = "여행 관리 - 기본설정, 여행CRUD, 상태관리")
@RestController
@RequestMapping("/api/v1/trips")
public class TripController {
    
    private final TripUseCase tripUseCase;
    private final MemberUseCase memberUseCase;
    
    public TripController(TripUseCase tripUseCase, MemberUseCase memberUseCase) {
        this.tripUseCase = tripUseCase;
        this.memberUseCase = memberUseCase;
    }
    
    @Operation(summary = "여행 목록 조회", description = "사용자의 여행 목록을 상태별로 조회합니다")
    @GetMapping
    public ResponseEntity<TripListResponse> getTripList(
            @Parameter(description = "여행 상태 필터") @RequestParam(defaultValue = "all") String status,
            @Parameter(description = "검색 키워드") @RequestParam(required = false) String search,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "latest") String sort,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        TripStatus tripStatus = "all".equals(status) ? null : TripStatus.valueOf(status.toUpperCase());
        
        TripUseCase.GetTripListQuery query = new TripUseCase.GetTripListQuery(
            userDetails.getUsername(), tripStatus, search, sort, page, size
        );
        
        TripUseCase.TripListResult result = tripUseCase.getTripList(query);
        
        TripListResponse response = TripListResponse.from(result);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "새 여행 생성", description = "기본설정: 새로운 여행을 생성합니다 (여행명, 설명, 일정, 이동수단)")
    @PostMapping
    public ResponseEntity<CreateTripResponse> createTrip(
            @Valid @RequestBody CreateTripRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        TripUseCase.CreateTripCommand command = new TripUseCase.CreateTripCommand(
            userDetails.getUsername(),
            request.getTripName(),
            request.getDescription(),
            request.getStartDate(),
            request.getTransportMode()
        );
        
        var trip = tripUseCase.createTrip(command);
        
        CreateTripResponse response = CreateTripResponse.from(trip);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "여행 상세 조회", description = "특정 여행의 상세 정보를 조회합니다")
    @GetMapping("/{tripId}")
    public ResponseEntity<TripDetailResponse> getTripDetail(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        var response = tripUseCase.getTripDetail(tripId, userDetails.getUsername())
                                 .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다"));
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "여행 기본정보 수정", description = "여행의 기본 정보(여행명, 설명, 일정, 이동수단)를 수정합니다")
    @PutMapping("/{tripId}")
    public ResponseEntity<TripResponse> updateTrip(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Valid @RequestBody UpdateTripRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        TripUseCase.UpdateTripCommand command = new TripUseCase.UpdateTripCommand(
            tripId,
            userDetails.getUsername(),
            request.getTripName(),
            request.getDescription(),
            request.getStartDate(),
            request.getTransportMode()
        );
        
        var trip = tripUseCase.updateTrip(command);
        
        TripResponse response = TripResponse.from(trip);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "여행 삭제", description = "여행을 삭제합니다")
    @DeleteMapping("/{tripId}")
    public ResponseEntity<Void> deleteTrip(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        TripUseCase.DeleteTripCommand command = new TripUseCase.DeleteTripCommand(
            tripId, userDetails.getUsername()
        );
        
        tripUseCase.deleteTrip(command);
        
        return ResponseEntity.noContent().build();
    }
    
    @Operation(
        summary = "여행 기본설정으로 새 여행 생성",
        description = "기본설정 화면에서 사용하는 통합 API - 여행명, 설명, 일정, 이동수단, 멤버 목록으로 새 여행을 일괄 생성합니다"
    )
    @PostMapping("/basic-setup")
    public ResponseEntity<CreateTripResponse> createTripWithBasicSetup(
            @Valid @RequestBody UpdateTripBasicInfoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // 1. 여행 생성 (모든 정보 포함)
        TripUseCase.CreateTripCommand command = new TripUseCase.CreateTripCommand(
            userDetails.getUsername(),
            request.getTripName(),
            request.getDescription(),
            request.getStartDate(),
            request.getTransportMode()
        );
        
        var trip = tripUseCase.createTrip(command);
        
        // 2. 멤버 추가
        updateTripMembers(trip.getTripId(), userDetails.getUsername(), request.getMembers());
        
        CreateTripResponse response = CreateTripResponse.from(trip);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 여행 멤버 목록을 일괄 업데이트하는 헬퍼 메서드
     * 기존 멤버를 모두 삭제하고 새 멤버 목록으로 교체합니다
     */
    private void updateTripMembers(String tripId, String userId, List<CreateMemberRequest> memberRequests) {
        if (memberRequests == null || memberRequests.isEmpty()) {
            return;
        }
        
        try {
            // 1. 기존 멤버 목록 조회 및 삭제 (안전하게 처리)
            try {
                var existingMembers = memberUseCase.getTripMembers(tripId, userId);
                for (var member : existingMembers) {
                    MemberUseCase.DeleteMemberCommand deleteCommand = new MemberUseCase.DeleteMemberCommand(
                        tripId, member.getMemberId(), userId
                    );
                    memberUseCase.deleteMember(deleteCommand);
                }
            } catch (Exception e) {
                // 기존 멤버가 없거나 조회 실패해도 새 멤버 추가는 계속 진행
                System.out.println("기존 멤버 조회/삭제 중 오류 (무시하고 계속): " + e.getMessage());
            }
            
            // 2. 새 멤버 목록 추가
            for (CreateMemberRequest memberRequest : memberRequests) {
                try {
                    Gender gender = parseGender(memberRequest.getGender());
                    HealthStatus healthStatus = parseHealthStatus(memberRequest.getHealthStatus());
                    List<Preference> preferences = parsePreferences(memberRequest.getPreferences());
                    
                    MemberUseCase.AddMemberCommand addCommand = new MemberUseCase.AddMemberCommand(
                        tripId, userId, memberRequest.getName(), memberRequest.getAge(),
                        gender, healthStatus, preferences
                    );
                    memberUseCase.addMember(addCommand);
                    
                    System.out.println("멤버 추가 성공: " + memberRequest.getName());
                } catch (Exception e) {
                    System.err.println("멤버 추가 실패 [" + memberRequest.getName() + "]: " + e.getMessage());
                    throw e; // 멤버 추가 실패는 전체 실패로 처리
                }
            }
        } catch (Exception e) {
            System.err.println("멤버 업데이트 전체 실패: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("멤버 업데이트 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * 성별 문자열을 Gender enum으로 변환
     */
    private Gender parseGender(String genderStr) {
        if (genderStr == null) {
            throw new IllegalArgumentException("성별은 필수입니다");
        }
        return switch (genderStr.toLowerCase()) {
            case "male" -> Gender.MALE;
            case "female" -> Gender.FEMALE;
            default -> throw new IllegalArgumentException("유효하지 않은 성별: " + genderStr);
        };
    }
    
    /**
     * 건강상태 문자열을 HealthStatus enum으로 변환
     */
    private HealthStatus parseHealthStatus(String healthStatusStr) {
        if (healthStatusStr == null) {
            throw new IllegalArgumentException("건강상태는 필수입니다");
        }
        return switch (healthStatusStr.toLowerCase()) {
            case "excellent" -> HealthStatus.EXCELLENT;
            case "good" -> HealthStatus.GOOD;
            case "caution" -> HealthStatus.CAUTION;
            case "limited" -> HealthStatus.LIMITED;
            default -> throw new IllegalArgumentException("유효하지 않은 건강상태: " + healthStatusStr);
        };
    }
    
    /**
     * 선호도 문자열 리스트를 Preference enum 리스트로 변환
     */
    private List<Preference> parsePreferences(List<String> preferenceStrs) {
        if (preferenceStrs == null) {
            return List.of();
        }
        return preferenceStrs.stream()
                .map(prefStr -> switch (prefStr.toLowerCase()) {
                    case "sightseeing" -> Preference.SIGHTSEEING;
                    case "shopping" -> Preference.SHOPPING;
                    case "culture" -> Preference.CULTURE;
                    case "nature" -> Preference.NATURE;
                    case "sports" -> Preference.SPORTS;
                    case "rest" -> Preference.REST;
                    default -> throw new IllegalArgumentException("유효하지 않은 선호도: " + prefStr);
                })
                .toList();
    }
}