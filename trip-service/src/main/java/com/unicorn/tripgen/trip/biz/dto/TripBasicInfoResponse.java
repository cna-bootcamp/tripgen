package com.unicorn.tripgen.trip.biz.dto;

import com.unicorn.tripgen.trip.biz.domain.Member;
import com.unicorn.tripgen.trip.biz.domain.Trip;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 여행 기본정보 응답 DTO
 */
public record TripBasicInfoResponse(
    String tripId,
    String tripName,
    String transportMode,
    List<MemberResponse> members,
    LocalDateTime updatedAt
) {
    public static TripBasicInfoResponse from(Trip trip, List<Member> members) {
        List<MemberResponse> memberResponses = members.stream()
                                                     .map(MemberResponse::from)
                                                     .toList();
        
        return new TripBasicInfoResponse(
            trip.getTripId(),
            trip.getTripName(),
            trip.getTransportMode().name().toLowerCase(),
            memberResponses,
            trip.getUpdatedAt()
        );
    }
}