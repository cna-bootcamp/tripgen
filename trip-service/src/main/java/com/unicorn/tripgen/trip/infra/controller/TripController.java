package com.unicorn.tripgen.trip.infra.controller;

import com.unicorn.tripgen.trip.biz.domain.TransportMode;
import com.unicorn.tripgen.trip.biz.domain.TripStatus;
import com.unicorn.tripgen.trip.biz.dto.*;
import com.unicorn.tripgen.trip.biz.usecase.in.TripUseCase;
import com.unicorn.tripgen.trip.biz.usecase.in.MemberUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 여행 관련 REST API Controller
 * Clean Architecture의 Infrastructure Layer - Web Interface
 */
@Tag(name = "trips", description = "여행 관리")
@RestController
@RequestMapping("/trips")
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
    
    @Operation(summary = "새 여행 생성", description = "새로운 여행을 생성합니다")
    @PostMapping
    public ResponseEntity<CreateTripResponse> createTrip(
            @Valid @RequestBody CreateTripRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        TripUseCase.CreateTripCommand command = new TripUseCase.CreateTripCommand(
            userDetails.getUsername(),
            request.getTitle(),
            null  // TransportMode is not in the new CreateTripRequest
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
        
        var trip = tripUseCase.getTripDetail(tripId, userDetails.getUsername())
                             .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다"));
        
        TripDetailResponse response = TripDetailResponse.from(trip);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "여행 기본정보 수정", description = "여행명과 이동수단을 수정합니다")
    @PutMapping("/{tripId}")
    public ResponseEntity<TripResponse> updateTrip(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Valid @RequestBody UpdateTripRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        TripUseCase.UpdateTripCommand command = new TripUseCase.UpdateTripCommand(
            tripId,
            userDetails.getUsername(),
            request.getTitle(),
            null  // TransportMode is not in the new UpdateTripRequest
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
    
    @Operation(summary = "여행 기본정보 일괄 저장", description = "여행명, 이동수단, 멤버 정보를 한꺼번에 저장합니다")
    @PutMapping("/{tripId}/basic-info")
    public ResponseEntity<TripBasicInfoResponse> updateTripBasicInfo(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Valid @RequestBody UpdateTripBasicInfoRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // 여행 기본 정보 업데이트
        TripUseCase.UpdateTripCommand tripCommand = new TripUseCase.UpdateTripCommand(
            tripId,
            userDetails.getUsername(),
            request.tripName(),
            TransportMode.valueOf(request.transportMode().toUpperCase())
        );
        
        var updatedTrip = tripUseCase.updateTrip(tripCommand);
        
        // 멤버 일괄 업데이트
        var memberInfos = request.members().stream()
                                 .map(m -> new MemberUseCase.MemberInfo(
                                     m.name(), m.age(), m.gender(), m.healthStatus(), m.preferences()
                                 ))
                                 .toList();
        
        MemberUseCase.UpdateMembersBatchCommand memberCommand = new MemberUseCase.UpdateMembersBatchCommand(
            tripId, userDetails.getUsername(), memberInfos
        );
        
        var updatedMembers = memberUseCase.updateMembersBatch(memberCommand);
        
        TripBasicInfoResponse response = TripBasicInfoResponse.from(updatedTrip, updatedMembers);
        return ResponseEntity.ok(response);
    }
}