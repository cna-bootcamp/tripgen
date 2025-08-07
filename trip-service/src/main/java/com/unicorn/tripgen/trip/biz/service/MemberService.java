package com.unicorn.tripgen.trip.biz.service;

import com.unicorn.tripgen.trip.biz.domain.Member;
import com.unicorn.tripgen.trip.biz.usecase.in.MemberUseCase;
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

    @Override
    public List<Member> getTripMembers(String tripId, String userId) {
        log.info("Getting members for trip: {}, userId: {}", tripId, userId);
        
        // TODO: 실제 구현 - Repository에서 조회
        return new ArrayList<>();
    }

    @Override
    @Transactional
    public Member addMember(AddMemberCommand command) {
        log.info("Adding member to trip: {}", command.tripId());
        
        String memberId = UUID.randomUUID().toString();
        Member member = Member.create(
            memberId,
            command.tripId(),
            command.name(),
            command.age(),
            command.gender(),
            command.healthStatus(),
            command.preferences()
        );
        
        // TODO: Repository에 저장
        
        return member;
    }

    @Override
    @Transactional
    public Member updateMember(UpdateMemberCommand command) {
        log.info("Updating member {} in trip: {}", command.memberId(), command.tripId());
        
        // TODO: 실제 구현 - Repository에서 조회 후 업데이트
        Member member = Member.create(
            command.memberId(),
            command.tripId(),
            command.name(),
            command.age(),
            command.gender(),
            command.healthStatus(),
            command.preferences()
        );
        
        member.updateInfo(command.name(), command.age(), command.gender(), 
                         command.healthStatus(), command.preferences());
        
        return member;
    }

    @Override
    @Transactional
    public void deleteMember(DeleteMemberCommand command) {
        log.info("Deleting member {} from trip: {}", command.memberId(), command.tripId());
        
        // TODO: 실제 구현 - 권한 확인 후 삭제
    }

    @Override
    @Transactional
    public List<Member> updateMembersBatch(UpdateMembersBatchCommand command) {
        log.info("Batch updating members for trip: {}", command.tripId());
        
        List<Member> members = new ArrayList<>();
        for (MemberInfo info : command.members()) {
            String memberId = UUID.randomUUID().toString();
            Member member = Member.create(
                memberId,
                command.tripId(),
                info.name(),
                info.age(),
                info.gender(),
                info.healthStatus(),
                info.preferences()
            );
            members.add(member);
        }
        
        // TODO: Repository에서 기존 멤버 삭제 후 새 멤버 저장
        
        return members;
    }
}