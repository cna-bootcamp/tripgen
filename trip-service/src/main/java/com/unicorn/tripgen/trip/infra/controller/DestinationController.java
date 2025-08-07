package com.unicorn.tripgen.trip.infra.controller;

import com.unicorn.tripgen.trip.biz.dto.*;
import com.unicorn.tripgen.trip.biz.usecase.in.DestinationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalTime;
import java.util.List;

/**
 * 여행지 관리 REST API Controller
 * Clean Architecture의 Infrastructure Layer - Web Interface
 */
@Tag(name = "destinations", description = "여행지 관리")
@RestController
@RequestMapping("/api/v1/trips/{tripId}/destinations")
public class DestinationController {
    
    private final DestinationUseCase destinationUseCase;
    
    public DestinationController(DestinationUseCase destinationUseCase) {
        this.destinationUseCase = destinationUseCase;
    }
    
    @Operation(summary = "여행지 목록 조회", description = "여행에 포함된 여행지 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<List<DestinationResponse>> getTripDestinations(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        var destinations = destinationUseCase.getTripDestinations(tripId, userDetails.getUsername());
        
        List<DestinationResponse> response = destinations.stream()
                                                       .map(DestinationResponse::from)
                                                       .toList();
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "여행지 추가", description = "새로운 여행지를 추가합니다")
    @PostMapping
    public ResponseEntity<DestinationResponse> addTripDestination(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Valid @RequestBody CreateDestinationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // String을 LocalTime으로 변환
        LocalTime checkInTime = request.getCheckInTime() != null ? 
            LocalTime.parse(request.getCheckInTime()) : null;
        LocalTime checkOutTime = request.getCheckOutTime() != null ?
            LocalTime.parse(request.getCheckOutTime()) : null;
        
        DestinationUseCase.AddDestinationCommand command = new DestinationUseCase.AddDestinationCommand(
            tripId,
            userDetails.getUsername(),
            request.getDestinationName(),
            request.getNights(),
            request.getAccommodation(),
            checkInTime,
            checkOutTime
        );
        
        var destination = destinationUseCase.addDestination(command);
        
        DestinationResponse response = DestinationResponse.from(destination);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(summary = "여행지 수정", description = "여행지 정보를 수정합니다")
    @PutMapping("/{destinationId}")
    public ResponseEntity<DestinationResponse> updateTripDestination(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Parameter(description = "여행지 ID") @PathVariable String destinationId,
            @Valid @RequestBody UpdateDestinationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // 현재 여행지 정보를 먼저 조회해서 null 값들을 처리
        var currentDestinations = destinationUseCase.getTripDestinations(tripId, userDetails.getUsername());
        var currentDestination = currentDestinations.stream()
                                                   .filter(d -> d.getDestinationId().equals(destinationId))
                                                   .findFirst()
                                                   .orElseThrow(() -> new IllegalArgumentException("여행지를 찾을 수 없습니다"));
        
        // 업데이트할 값들 - null이면 기존 값 유지
        String destinationName = request.getDestinationName() != null ? 
            request.getDestinationName() : currentDestination.getDestinationName();
        int nights = request.getNights() != null ? request.getNights() : currentDestination.getNights();
        String accommodation = request.getAccommodation() != null ? 
            request.getAccommodation() : currentDestination.getAccommodation();
        LocalTime checkInTime = request.getCheckInTime() != null ?
            LocalTime.parse(request.getCheckInTime()) : currentDestination.getCheckInTime();
        LocalTime checkOutTime = request.getCheckOutTime() != null ?
            LocalTime.parse(request.getCheckOutTime()) : currentDestination.getCheckOutTime();
        
        DestinationUseCase.UpdateDestinationCommand command = new DestinationUseCase.UpdateDestinationCommand(
            tripId,
            destinationId,
            userDetails.getUsername(),
            destinationName,
            nights,
            accommodation,
            checkInTime,
            checkOutTime
        );
        
        var destination = destinationUseCase.updateDestination(command);
        
        DestinationResponse response = DestinationResponse.from(destination);
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "여행지 삭제", description = "여행지를 삭제합니다")
    @DeleteMapping("/{destinationId}")
    public ResponseEntity<Void> deleteTripDestination(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Parameter(description = "여행지 ID") @PathVariable String destinationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        DestinationUseCase.DeleteDestinationCommand command = new DestinationUseCase.DeleteDestinationCommand(
            tripId, destinationId, userDetails.getUsername()
        );
        
        destinationUseCase.deleteDestination(command);
        
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "여행지 정보 일괄 저장", description = "여행지 정보를 한꺼번에 저장합니다 (기존 여행지는 모두 교체됩니다)")
    @PutMapping("/batch")
    public ResponseEntity<DestinationsBatchResponse> updateTripDestinationsBatch(
            @Parameter(description = "여행 ID") @PathVariable String tripId,
            @Valid @RequestBody UpdateDestinationsBatchRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // CreateDestinationRequest 리스트를 DestinationInfo 리스트로 변환
        var destinationInfos = request.getDestinations().stream()
                                     .map(req -> {
                                         LocalTime checkInTime = req.getCheckInTime() != null ? 
                                             LocalTime.parse(req.getCheckInTime()) : null;
                                         LocalTime checkOutTime = req.getCheckOutTime() != null ?
                                             LocalTime.parse(req.getCheckOutTime()) : null;
                                         
                                         return new DestinationUseCase.DestinationInfo(
                                             req.getDestinationName(),
                                             req.getNights(),
                                             req.getAccommodation(),
                                             checkInTime,
                                             checkOutTime
                                         );
                                     })
                                     .toList();
        
        DestinationUseCase.UpdateDestinationsBatchCommand command = new DestinationUseCase.UpdateDestinationsBatchCommand(
            tripId, userDetails.getUsername(), destinationInfos
        );
        
        var result = destinationUseCase.updateDestinationsBatch(command);
        
        DestinationsBatchResponse response = DestinationsBatchResponse.from(result);
        return ResponseEntity.ok(response);
    }
}