package com.unicorn.tripgen.ai.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JobStatus {
    QUEUED("대기중"),
    PROCESSING("처리중"),
    COMPLETED("완료"),
    FAILED("실패"),
    CANCELLED("취소됨");
    
    private final String description;
    
    /**
     * 작업이 완료된 상태인지 확인
     */
    public boolean isCompleted() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
    
    /**
     * 작업이 종료 상태인지 확인
     */
    public boolean isFinal() {
        return isCompleted();
    }
}