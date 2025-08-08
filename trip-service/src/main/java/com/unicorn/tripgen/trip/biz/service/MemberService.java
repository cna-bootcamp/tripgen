package com.unicorn.tripgen.trip.biz.service;

import com.unicorn.tripgen.trip.biz.domain.Member;
import com.unicorn.tripgen.trip.biz.domain.Trip;
import com.unicorn.tripgen.trip.biz.exception.UnauthorizedAccessException;
import com.unicorn.tripgen.trip.biz.usecase.in.MemberUseCase;
import com.unicorn.tripgen.trip.biz.usecase.out.MemberRepository;
import com.unicorn.tripgen.trip.biz.usecase.out.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService implements MemberUseCase {
    
    private final MemberRepository memberRepository;
    private final TripRepository tripRepository;

    @Override
    public List<Member> getTripMembers(String tripId, String userId) {
        log.info("Getting members for trip: {}, userId: {}", tripId, userId);
        
        // 여행 소유자 확인
        if (!tripRepository.isOwner(tripId, userId)) {
            throw new UnauthorizedAccessException("여행에 대한 접근 권한이 없습니다");
        }
        
        return memberRepository.findByTripId(tripId);
    }

    @Override
    @Transactional
    public Member addMember(AddMemberCommand command) {
        log.info("Adding member to trip: {}", command.tripId());
        
        // 여행 소유자 확인
        if (!tripRepository.isOwner(command.tripId(), command.userId())) {
            throw new UnauthorizedAccessException("여행에 대한 접근 권한이 없습니다");
        }
        
        // Trip 엔티티 조회 (JPA 매핑을 위해 필수)
        Trip trip = tripRepository.findById(command.tripId())
            .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다"));
        
        String memberId = memberRepository.generateMemberId();
        Member member = Member.createWithTrip(
            memberId,
            trip,
            command.name(),
            command.age(),
            command.gender(),
            command.healthStatus(),
            command.preferences()
        );
        
        return memberRepository.save(member);
    }

    @Override
    @Transactional
    public Member updateMember(UpdateMemberCommand command) {
        log.info("Updating member {} in trip: {}", command.memberId(), command.tripId());
        
        // 여행 소유자 확인
        if (!tripRepository.isOwner(command.tripId(), command.userId())) {
            throw new UnauthorizedAccessException("여행에 대한 접근 권한이 없습니다");
        }
        
        // 기존 멤버 조회
        Member member = memberRepository.findById(command.memberId())
            .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다"));
        
        // 멤버 정보 업데이트
        member.updateInfo(command.name(), command.age(), command.gender(), 
                         command.healthStatus(), command.preferences());
        
        return memberRepository.save(member);
    }

    @Override
    @Transactional
    public void deleteMember(DeleteMemberCommand command) {
        log.info("Deleting member {} from trip: {}", command.memberId(), command.tripId());
        
        // 여행 소유자 확인
        if (!tripRepository.isOwner(command.tripId(), command.userId())) {
            throw new UnauthorizedAccessException("여행에 대한 접근 권한이 없습니다");
        }
        
        // 기존 멤버 조회
        Member member = memberRepository.findById(command.memberId())
            .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다"));
        
        memberRepository.delete(member);
    }

    @Override
    @Transactional
    public List<Member> updateMembersBatch(UpdateMembersBatchCommand command) {
        log.info("Batch updating members for trip: {}", command.tripId());
        
        // 여행 소유자 확인
        if (!tripRepository.isOwner(command.tripId(), command.userId())) {
            throw new UnauthorizedAccessException("여행에 대한 접근 권한이 없습니다");
        }
        
        // Trip 엔티티 조회 (JPA 매핑을 위해 필수)
        Trip trip = tripRepository.findById(command.tripId())
            .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다"));
        
        // 기존 멤버 모두 삭제
        memberRepository.deleteByTripId(command.tripId());
        
        // 새 멤버 생성
        List<Member> members = new ArrayList<>();
        for (MemberInfo info : command.members()) {
            String memberId = memberRepository.generateMemberId();
            Member member = Member.createWithTrip(
                memberId,
                trip,
                info.name(),
                info.age(),
                info.gender(),
                info.healthStatus(),
                info.preferences()
            );
            members.add(member);
        }
        
        return memberRepository.saveAll(members);
    }
}