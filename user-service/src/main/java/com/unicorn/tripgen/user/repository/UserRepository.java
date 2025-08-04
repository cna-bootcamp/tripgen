package com.unicorn.tripgen.user.repository;

import com.unicorn.tripgen.user.entity.User;
import com.unicorn.tripgen.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 Repository 인터페이스
 * 사용자 데이터 접근을 담당
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * 사용자명으로 사용자 조회
     * 
     * @param username 사용자명
     * @return 사용자 정보 (Optional)
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 이메일로 사용자 조회
     * 
     * @param email 이메일
     * @return 사용자 정보 (Optional)
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 휴대폰 번호로 사용자 조회
     * 
     * @param phone 휴대폰 번호
     * @return 사용자 정보 (Optional)
     */
    Optional<User> findByPhone(String phone);
    
    /**
     * 사용자명 존재 여부 확인
     * 
     * @param username 사용자명
     * @return 존재 여부
     */
    boolean existsByUsername(String username);
    
    /**
     * 이메일 존재 여부 확인
     * 
     * @param email 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);
    
    /**
     * 휴대폰 번호 존재 여부 확인
     * 
     * @param phone 휴대폰 번호
     * @return 존재 여부
     */
    boolean existsByPhone(String phone);
    
    /**
     * 특정 상태의 사용자 목록 조회
     * 
     * @param status 사용자 상태
     * @return 사용자 목록
     */
    List<User> findByStatus(UserStatus status);
    
    /**
     * 잠금 해제 대상 사용자 조회
     * 
     * @param currentTime 현재 시간
     * @return 잠금 해제 대상 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.status = 'LOCKED' AND u.lockedUntil IS NOT NULL AND u.lockedUntil <= :currentTime")
    List<User> findUsersToUnlock(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 휴면 계정 대상 사용자 조회
     * 
     * @param cutoffDate 기준 날짜
     * @return 휴면 대상 사용자 목록
     */
    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.lastLoginAt IS NOT NULL AND u.lastLoginAt < :cutoffDate")
    List<User> findDormantUsers(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * 활성 사용자 수 조회
     * 
     * @return 활성 사용자 수
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACTIVE'")
    long countActiveUsers();
    
    /**
     * 특정 기간 내 가입한 사용자 수 조회
     * 
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 가입 사용자 수
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    long countUsersByRegistrationPeriod(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
}