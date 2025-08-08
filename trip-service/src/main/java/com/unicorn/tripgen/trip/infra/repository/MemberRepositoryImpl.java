package com.unicorn.tripgen.trip.infra.repository;

import com.unicorn.tripgen.trip.biz.domain.Member;
import com.unicorn.tripgen.trip.biz.usecase.out.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Member Repository 구현체
 * Clean Architecture의 Infrastructure Layer
 * Domain Layer의 MemberRepository 인터페이스를 구현
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {
    
    private final MemberJpaRepository memberJpaRepository;
    
    @Override
    public Member save(Member member) {
        log.debug("Saving member: {} for trip: {}", member.getMemberId(), member.getTripId());
        return memberJpaRepository.save(member);
    }
    
    @Override
    public List<Member> saveAll(List<Member> members) {
        log.debug("Saving {} members", members.size());
        return memberJpaRepository.saveAll(members);
    }
    
    @Override
    public Optional<Member> findById(String memberId) {
        log.debug("Finding member by id: {}", memberId);
        return memberJpaRepository.findById(memberId);
    }
    
    @Override
    public List<Member> findByTripId(String tripId) {
        log.debug("Finding members for trip: {}", tripId);
        return memberJpaRepository.findByTripId(tripId);
    }
    
    @Override
    public int countByTripId(String tripId) {
        log.debug("Counting members for trip: {}", tripId);
        return memberJpaRepository.countByTripId(tripId);
    }
    
    @Override
    public void delete(Member member) {
        log.debug("Deleting member: {}", member.getMemberId());
        memberJpaRepository.delete(member);
    }
    
    @Override
    public void deleteByTripId(String tripId) {
        log.debug("Deleting all members for trip: {}", tripId);
        memberJpaRepository.deleteByTripId(tripId);
    }
    
    @Override
    public boolean existsById(String memberId) {
        log.debug("Checking if member exists: {}", memberId);
        return memberJpaRepository.existsById(memberId);
    }
    
    @Override
    public String generateMemberId() {
        String memberId = UUID.randomUUID().toString();
        log.debug("Generated member id: {}", memberId);
        return memberId;
    }
}