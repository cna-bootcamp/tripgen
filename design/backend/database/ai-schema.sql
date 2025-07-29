-- ========================================
-- AI 서비스 데이터베이스 스키마
-- ========================================
-- 서비스: AI Service
-- 목적: AI 기반 일정 생성 및 추천 정보 관리
-- 아키텍처: Layered Architecture
-- 데이터베이스: PostgreSQL

-- 필요한 확장 생성
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "btree_gin";

-- ai_schedules 테이블 (AI 생성 일정)
CREATE TABLE ai_schedules (
    id                  BIGSERIAL PRIMARY KEY,
    schedule_id         VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
    trip_id             VARCHAR(36) NOT NULL,     -- Trip 서비스의 tripId 참조
    user_id             VARCHAR(36) NOT NULL,     -- User 서비스의 userId 참조
    request_data        JSONB NOT NULL,           -- 요청 정보 (멤버, 여행지, 선호도 등)
    generated_schedule  JSONB,                    -- 생성된 일정 데이터
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    start_time          TIME,                     -- 여행 시작 시간
    special_requests    TEXT,                     -- 특별 요청사항
    generation_attempts INTEGER NOT NULL DEFAULT 0,
    error_message       TEXT,
    generated_at        TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_ai_schedules_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_ai_schedules_attempts CHECK (generation_attempts >= 0 AND generation_attempts <= 10),
    CONSTRAINT chk_ai_schedules_start_time CHECK (start_time BETWEEN '06:00:00' AND '23:59:59')
);

-- ai_jobs 테이블 (비동기 작업 관리)
CREATE TABLE ai_jobs (
    id                  BIGSERIAL PRIMARY KEY,
    job_id              VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
    schedule_id         VARCHAR(36) NOT NULL,
    job_type            VARCHAR(30) NOT NULL,     -- GENERATE_SCHEDULE, REGENERATE_DAY, etc.
    status              VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    progress_percentage INTEGER NOT NULL DEFAULT 0,
    progress_message    VARCHAR(200),
    input_data          JSONB NOT NULL,
    result_data         JSONB,
    error_details       JSONB,
    started_at          TIMESTAMP WITH TIME ZONE,
    completed_at        TIMESTAMP WITH TIME ZONE,
    expires_at          TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(36),  
    updated_by          VARCHAR(36),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_ai_jobs_status CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_ai_jobs_progress CHECK (progress_percentage >= 0 AND progress_percentage <= 100),
    CONSTRAINT chk_ai_jobs_type CHECK (job_type IN ('GENERATE_SCHEDULE', 'REGENERATE_DAY', 'GENERATE_RECOMMENDATION')),
    
    -- 외래키
    CONSTRAINT fk_ai_jobs_schedule_id FOREIGN KEY (schedule_id) REFERENCES ai_schedules(schedule_id) ON DELETE CASCADE
);

-- ai_recommendations 테이블 (장소 추천 정보)
CREATE TABLE ai_recommendations (
    id                  BIGSERIAL PRIMARY KEY,
    recommendation_id   VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
    place_id            VARCHAR(36) NOT NULL,     -- Location 서비스의 placeId 참조
    user_id             VARCHAR(36) NOT NULL,     -- User 서비스의 userId 참조
    recommendation_text TEXT NOT NULL,            -- AI 추천 이유
    useful_tips         TEXT,                     -- 유용한 정보/팁
    user_context        JSONB NOT NULL,           -- 사용자 컨텍스트 (멤버, 선호도 등)
    place_context       JSONB,                    -- 장소 컨텍스트 (날씨, 시간대 등)
    generated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '24 hours'),
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_ai_recommendations_text_length CHECK (LENGTH(recommendation_text) >= 10),
    CONSTRAINT chk_ai_recommendations_expires CHECK (expires_at > created_at)
);

-- 인덱스 생성
-- ai_schedules 인덱스
CREATE UNIQUE INDEX idx_ai_schedules_schedule_id ON ai_schedules(schedule_id);
CREATE INDEX idx_ai_schedules_trip_id ON ai_schedules(trip_id);
CREATE INDEX idx_ai_schedules_user_id ON ai_schedules(user_id);
CREATE INDEX idx_ai_schedules_status ON ai_schedules(status);
CREATE INDEX idx_ai_schedules_created_at ON ai_schedules(created_at);
CREATE INDEX idx_ai_schedules_request_data_gin ON ai_schedules USING GIN(request_data);
CREATE INDEX idx_ai_schedules_generated_schedule_gin ON ai_schedules USING GIN(generated_schedule);

-- ai_jobs 인덱스  
CREATE UNIQUE INDEX idx_ai_jobs_job_id ON ai_jobs(job_id);
CREATE INDEX idx_ai_jobs_schedule_id ON ai_jobs(schedule_id);
CREATE INDEX idx_ai_jobs_status ON ai_jobs(status);
CREATE INDEX idx_ai_jobs_job_type ON ai_jobs(job_type);
CREATE INDEX idx_ai_jobs_created_at ON ai_jobs(created_at);
CREATE INDEX idx_ai_jobs_expires_at ON ai_jobs(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_ai_jobs_active ON ai_jobs(id) WHERE status IN ('QUEUED', 'PROCESSING');

-- ai_recommendations 인덱스
CREATE UNIQUE INDEX idx_ai_recommendations_recommendation_id ON ai_recommendations(recommendation_id);
CREATE INDEX idx_ai_recommendations_place_id ON ai_recommendations(place_id);
CREATE INDEX idx_ai_recommendations_user_id ON ai_recommendations(user_id);
CREATE INDEX idx_ai_recommendations_expires_at ON ai_recommendations(expires_at);
CREATE INDEX idx_ai_recommendations_user_context_gin ON ai_recommendations USING GIN(user_context);
CREATE INDEX idx_ai_recommendations_active ON ai_recommendations(id) WHERE expires_at > CURRENT_TIMESTAMP;

-- 자동 갱신 트리거 함수들
CREATE OR REPLACE FUNCTION update_ai_schedules_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_ai_jobs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_ai_recommendations_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 작업 만료 처리 함수
CREATE OR REPLACE FUNCTION expire_old_jobs()
RETURNS TRIGGER AS $$
BEGIN
    -- 만료된 작업 상태 자동 변경
    UPDATE ai_jobs 
    SET status = 'EXPIRED', updated_at = CURRENT_TIMESTAMP
    WHERE expires_at IS NOT NULL 
      AND expires_at <= CURRENT_TIMESTAMP 
      AND status IN ('QUEUED', 'PROCESSING');
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- 진행률 자동 계산 함수
CREATE OR REPLACE FUNCTION calculate_job_progress()
RETURNS TRIGGER AS $$
BEGIN
    -- 상태에 따른 자동 진행률 계산
    IF NEW.status = 'QUEUED' THEN
        NEW.progress_percentage = 0;
        NEW.progress_message = '대기 중';
    ELSIF NEW.status = 'PROCESSING' AND NEW.progress_percentage = 0 THEN
        NEW.progress_percentage = 10;
        NEW.progress_message = '처리 중';
        NEW.started_at = CURRENT_TIMESTAMP;
    ELSIF NEW.status = 'COMPLETED' THEN
        NEW.progress_percentage = 100;
        NEW.progress_message = '완료';
        NEW.completed_at = CURRENT_TIMESTAMP;
    ELSIF NEW.status = 'FAILED' THEN
        NEW.progress_message = '실패';
        NEW.completed_at = CURRENT_TIMESTAMP;
    ELSIF NEW.status = 'CANCELLED' THEN
        NEW.progress_message = '취소됨';
        NEW.completed_at = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 일정 생성 완료 시 알림 함수
CREATE OR REPLACE FUNCTION notify_schedule_completion()
RETURNS TRIGGER AS $$
BEGIN
    -- AI 일정 생성이 완료되면 관련 정보 업데이트
    IF NEW.status = 'COMPLETED' AND OLD.status != 'COMPLETED' THEN
        NEW.generated_at = CURRENT_TIMESTAMP;
        
        -- 알림 이벤트 발생 (선택사항)
        PERFORM pg_notify('schedule_completed', 
            json_build_object(
                'schedule_id', NEW.schedule_id,
                'trip_id', NEW.trip_id,
                'user_id', NEW.user_id
            )::text
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER trg_ai_schedules_updated_at
    BEFORE UPDATE ON ai_schedules
    FOR EACH ROW
    EXECUTE FUNCTION update_ai_schedules_updated_at();

CREATE TRIGGER trg_ai_jobs_updated_at
    BEFORE UPDATE ON ai_jobs
    FOR EACH ROW
    EXECUTE FUNCTION update_ai_jobs_updated_at();

CREATE TRIGGER trg_ai_recommendations_updated_at
    BEFORE UPDATE ON ai_recommendations
    FOR EACH ROW
    EXECUTE FUNCTION update_ai_recommendations_updated_at();

CREATE TRIGGER trg_ai_jobs_progress
    BEFORE UPDATE ON ai_jobs
    FOR EACH ROW
    EXECUTE FUNCTION calculate_job_progress();

CREATE TRIGGER trg_ai_schedules_completion
    BEFORE UPDATE ON ai_schedules
    FOR EACH ROW
    EXECUTE FUNCTION notify_schedule_completion();

-- 만료된 데이터 정리를 위한 함수
CREATE OR REPLACE FUNCTION cleanup_expired_data()
RETURNS VOID AS $$
BEGIN
    -- 만료된 추천 정보 삭제 (7일 후)
    DELETE FROM ai_recommendations 
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '7 days';
    
    -- 만료된 작업 삭제 (30일 후)
    DELETE FROM ai_jobs 
    WHERE status = 'EXPIRED' 
      AND expires_at < CURRENT_TIMESTAMP - INTERVAL '30 days';
      
    -- 완료된 작업 중 오래된 것 삭제 (90일 후)
    DELETE FROM ai_jobs 
    WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED')
      AND completed_at < CURRENT_TIMESTAMP - INTERVAL '90 days';
END;
$$ LANGUAGE plpgsql;

-- 테이블 코멘트
COMMENT ON TABLE ai_schedules IS 'AI 생성 여행 일정 관리';
COMMENT ON TABLE ai_jobs IS '비동기 AI 작업 관리';
COMMENT ON TABLE ai_recommendations IS '장소별 AI 추천 정보';

-- 컬럼 코멘트
COMMENT ON COLUMN ai_schedules.schedule_id IS '외부 서비스 참조용 비즈니스 키';
COMMENT ON COLUMN ai_schedules.request_data IS '일정 생성 요청 데이터 (JSONB)';
COMMENT ON COLUMN ai_schedules.generated_schedule IS '생성된 일정 데이터 (JSONB)';
COMMENT ON COLUMN ai_jobs.progress_percentage IS '작업 진행률 (0-100)';
COMMENT ON COLUMN ai_recommendations.expires_at IS '추천 정보 만료 시간 (24시간)';

-- 성능 통계 수집
ANALYZE ai_schedules, ai_jobs, ai_recommendations;