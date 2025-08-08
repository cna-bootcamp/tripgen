package com.unicorn.tripgen.trip.infra.controller;

import com.unicorn.tripgen.trip.biz.domain.Gender;
import com.unicorn.tripgen.trip.biz.domain.HealthStatus;
import com.unicorn.tripgen.trip.biz.domain.Preference;
import com.unicorn.tripgen.trip.biz.dto.CreateMemberRequest;
import com.unicorn.tripgen.trip.biz.dto.MemberResponse;
import com.unicorn.tripgen.trip.biz.dto.UpdateMemberRequest;
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
import java.util.List;

/**
 * 여행 멤버 관리 REST API Controller
 * Clean Architecture의 Infrastructure Layer - Web Interface
 */
@Tag(name = "members", description = "여행 멤버 관리 - 개별 멤버 CRUD (기본설정은 trips/basic-setup 사용 권장)")
@RestController
@RequestMapping("/api/v1/trips/{tripId}/members")
public class MemberController {
    
    private final MemberUseCase memberUseCase;
    
    public MemberController(MemberUseCase memberUseCase) {
        this.memberUseCase = memberUseCase;
    }
    
    @Operation(summary = "여행 멤버 목록 조회", description = "여행에 참여하는 멤버 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<List<MemberResponse>> getTripMembers(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        var members = memberUseCase.getTripMembers(tripId, userDetails.getUsername());
        
        List<MemberResponse> response = members.stream()
                                             .map(MemberResponse::from)
                                             .toList();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "멤버 추가", description = "여행에 새로운 멤버를 개별적으로 추가합니다 (기본설정 화면에서는 trips/basic-setup 사용 권장)")
    @PostMapping
    public ResponseEntity<MemberResponse> addTripMember(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Valid @RequestBody CreateMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // String을 enum으로 변환
        Gender gender = Gender.valueOf(request.getGender().toUpperCase());
        HealthStatus healthStatus = HealthStatus.valueOf(request.getHealthStatus().toUpperCase());
        List<Preference> preferences = request.getPreferences() != null ? 
            request.getPreferences().stream()
                   .map(p -> Preference.valueOf(p.toUpperCase()))
                   .toList() : List.of();
        
        MemberUseCase.AddMemberCommand command = new MemberUseCase.AddMemberCommand(
            tripId,
            userDetails.getUsername(),
            request.getName(),
            request.getAge(),
            gender,
            healthStatus,
            preferences
        );
        
        var member = memberUseCase.addMember(command);
        
        MemberResponse response = MemberResponse.from(member);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "멤버 정보 수정", description = "특정 멤버의 정보를 개별적으로 수정합니다")
    @PutMapping("/{memberId}")
    public ResponseEntity<MemberResponse> updateTripMember(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Parameter(description = "멤버 ID") @PathVariable String memberId,
            @Valid @RequestBody UpdateMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // 현재 멤버 정보를 먼저 조회해서 null 값들을 처리
        var currentMembers = memberUseCase.getTripMembers(tripId, userDetails.getUsername());
        var currentMember = currentMembers.stream()
                                         .filter(m -> m.getMemberId().equals(memberId))
                                         .findFirst()
                                         .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다"));
        
        // 업데이트할 값들 - null이면 기존 값 유지
        String name = request.getName() != null ? request.getName() : currentMember.getName();
        int age = request.getAge() != null ? request.getAge() : currentMember.getAge();
        Gender gender = request.getGender() != null ? 
            Gender.valueOf(request.getGender().toUpperCase()) : currentMember.getGender();
        HealthStatus healthStatus = request.getHealthStatus() != null ?
            HealthStatus.valueOf(request.getHealthStatus().toUpperCase()) : currentMember.getHealthStatus();
        List<Preference> preferences = request.getPreferences() != null ?
            request.getPreferences().stream()
                   .map(p -> Preference.valueOf(p.toUpperCase()))
                   .toList() : currentMember.getPreferences();
        
        MemberUseCase.UpdateMemberCommand command = new MemberUseCase.UpdateMemberCommand(
            tripId,
            memberId,
            userDetails.getUsername(),
            name,
            age,
            gender,
            healthStatus,
            preferences
        );
        
        var member = memberUseCase.updateMember(command);
        
        MemberResponse response = MemberResponse.from(member);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "멤버 삭제", description = "여행에서 멤버를 제거합니다")
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteTripMember(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Parameter(description = "멤버 ID") @PathVariable String memberId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        MemberUseCase.DeleteMemberCommand command = new MemberUseCase.DeleteMemberCommand(
            tripId, memberId, userDetails.getUsername()
        );
        
        memberUseCase.deleteMember(command);
        
        return ResponseEntity.noContent().build();
    }
}