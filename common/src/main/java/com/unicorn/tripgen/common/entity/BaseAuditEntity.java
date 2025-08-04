package com.unicorn.tripgen.common.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 감사(Audit) 정보를 포함한 엔티티의 기본 클래스
 * 생성자/수정자 정보를 추가로 포함
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public abstract class BaseAuditEntity extends BaseEntity {
    
    /**
     * 엔티티 생성자 ID
     */
    @CreatedBy
    @Column(name = "created_by", length = 36, updatable = false)
    private String createdBy;
    
    /**
     * 엔티티 마지막 수정자 ID
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 36)
    private String updatedBy;
    
    /**
     * 엔티티 버전 (낙관적 락킹용)
     */
    @Version
    @Column(name = "version")
    private Long version;
    
    /**
     * 삭제 여부 (소프트 삭제용)
     */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
    
    /**
     * 소프트 삭제 처리
     */
    public void delete() {
        this.deleted = true;
    }
    
    /**
     * 삭제 취소 처리
     */
    public void restore() {
        this.deleted = false;
    }
}