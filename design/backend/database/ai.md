# AI 서비스 데이터베이스 설계서

## 1. 데이터베이스 개요

### 1.1 서비스 개요
- **서비스명**: AI Service
- **목적**: AI 기반 여행 일정 생성 및 장소 추천 정보 제공
- **아키텍처**: Layered Architecture
- **데이터베이스**: PostgreSQL

### 1.2 설계 원칙
- 서비스별 독립적인 데이터베이스 구성
- 비동기 작업 처리를 위한 Job 관리
- AI 생성 결과의 효율적인 캐싱
- 대용량 JSON 데이터 처리 최적화

## 2. 테이블 설계

### 2.1 ai_schedules 테이블

AI가 생성한 여행 일정 정보를 저장하는 테이블

```sql
CREATE TABLE ai_schedules (
    id                  BIGSERIAL PRIMARY KEY,
    request_id          VARCHAR(36) UNIQUE NOT NULL,     -- 요청 식별자
    trip_id             VARCHAR(36) NOT NULL,            -- Trip 서비스의 tripId
    status              VARCHAR(20) NOT NULL,            -- 생성 상태
    schedule_data       JSONB,                           -- 생성된 일정 데이터
    generated_at        TIMESTAMP WITH TIME ZONE,        -- 생성 완료 시간
    expired_at          TIMESTAMP WITH TIME ZONE,        -- 만료 시간
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_ai_schedules_status CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_ai_schedules_expiry CHECK (expired_at > generated_at OR expired_at IS NULL)
);

-- 댓글
COMMENT ON TABLE ai_schedules IS 'AI 생성 여행 일정 저장 테이블';
COMMENT ON COLUMN ai_schedules.request_id IS '일정 생성 요청 고유 식별자';
COMMENT ON COLUMN ai_schedules.trip_id IS 'Trip 서비스의 여행 ID';
COMMENT ON COLUMN ai_schedules.schedule_data IS '생성된 일정 JSON 데이터';
COMMENT ON COLUMN ai_schedules.expired_at IS '캐시 만료 시간 (NULL이면 영구 보존)';
```

### 2.2 ai_jobs 테이블

AI 작업 진행 상황을 관리하는 테이블

```sql
CREATE TABLE ai_jobs (
    id                  BIGSERIAL PRIMARY KEY,
    request_id          VARCHAR(36) UNIQUE NOT NULL,     -- 작업 요청 식별자
    job_type            VARCHAR(30) NOT NULL,            -- 작업 유형
    status              VARCHAR(20) NOT NULL,            -- 작업 상태
    payload             JSONB NOT NULL,                  -- 작업 입력 데이터
    result              JSONB,                           -- 작업 결과 데이터
    progress            INTEGER NOT NULL DEFAULT 0,      -- 진행률 (0-100)
    current_step        VARCHAR(100),                    -- 현재 진행 단계
    error_message       TEXT,                            -- 오류 메시지
    started_at          TIMESTAMP WITH TIME ZONE,        -- 작업 시작 시간
    completed_at        TIMESTAMP WITH TIME ZONE,        -- 작업 완료 시간
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_ai_jobs_type CHECK (job_type IN ('SCHEDULE_GENERATION', 'SCHEDULE_REGENERATION', 'RECOMMENDATION_GENERATION', 'WEATHER_IMPACT_ANALYSIS')),
    CONSTRAINT chk_ai_jobs_status CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_ai_jobs_progress CHECK (progress >= 0 AND progress <= 100)
);

-- 댓글
COMMENT ON TABLE ai_jobs IS 'AI 작업 관리 테이블';
COMMENT ON COLUMN ai_jobs.job_type IS '작업 유형 (일정생성/재생성/추천/날씨분석)';
COMMENT ON COLUMN ai_jobs.payload IS '작업 입력 매개변수 JSON';
COMMENT ON COLUMN ai_jobs.result IS '작업 결과 JSON';
COMMENT ON COLUMN ai_jobs.progress IS '작업 진행률 (0-100%)';
COMMENT ON COLUMN ai_jobs.current_step IS '현재 처리 중인 단계 설명';
```

### 2.3 ai_recommendations 테이블

AI가 생성한 장소 추천 정보를 저장하는 테이블

```sql
CREATE TABLE ai_recommendations (
    id                      BIGSERIAL PRIMARY KEY,
    place_id               VARCHAR(100) NOT NULL,           -- Location 서비스의 placeId
    profile_hash           VARCHAR(64) NOT NULL,            -- 사용자 프로파일 해시
    recommendation_data    JSONB NOT NULL,                  -- 추천 정보 JSON
    generated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_ai_recommendations_expiry CHECK (expired_at > created_at),
    
    -- 복합 유니크 키
    CONSTRAINT uk_ai_recommendations_place_profile UNIQUE (place_id, profile_hash)
);

-- 댓글
COMMENT ON TABLE ai_recommendations IS 'AI 생성 장소 추천 정보 저장 테이블';
COMMENT ON COLUMN ai_recommendations.place_id IS 'Location 서비스의 장소 ID';
COMMENT ON COLUMN ai_recommendations.profile_hash IS '사용자 프로파일 해시 (캐시 키)';
COMMENT ON COLUMN ai_recommendations.recommendation_data IS '추천 이유 및 팁 JSON 데이터';
COMMENT ON COLUMN ai_recommendations.expired_at IS '추천 정보 만료 시간';
```

## 3. 인덱스 설계

### 3.1 Primary Index
```sql
-- 기본 키 인덱스 (자동 생성)
-- ai_schedules: PRIMARY KEY (id)
-- ai_jobs: PRIMARY KEY (id)  
-- ai_recommendations: PRIMARY KEY (id)
```

### 3.2 Unique Index
```sql
-- 요청 ID 유니크 인덱스
CREATE UNIQUE INDEX idx_ai_schedules_request_id ON ai_schedules(request_id);
CREATE UNIQUE INDEX idx_ai_jobs_request_id ON ai_jobs(request_id);

-- 추천 정보 복합 유니크 인덱스
CREATE UNIQUE INDEX idx_ai_recommendations_place_profile ON ai_recommendations(place_id, profile_hash);
```

### 3.3 Performance Index
```sql
-- ai_schedules 성능 인덱스
CREATE INDEX idx_ai_schedules_trip_id ON ai_schedules(trip_id);
CREATE INDEX idx_ai_schedules_status ON ai_schedules(status);
CREATE INDEX idx_ai_schedules_expired_at ON ai_schedules(expired_at) WHERE expired_at IS NOT NULL;

-- ai_jobs 성능 인덱스
CREATE INDEX idx_ai_jobs_status ON ai_jobs(status);
CREATE INDEX idx_ai_jobs_type_status ON ai_jobs(job_type, status);
CREATE INDEX idx_ai_jobs_created_at ON ai_jobs(created_at);
CREATE INDEX idx_ai_jobs_processing ON ai_jobs(status, started_at) WHERE status = 'PROCESSING';

-- ai_recommendations 성능 인덱스
CREATE INDEX idx_ai_recommendations_place_id ON ai_recommendations(place_id);
CREATE INDEX idx_ai_recommendations_expired_at ON ai_recommendations(expired_at);
CREATE INDEX idx_ai_recommendations_generated_at ON ai_recommendations(generated_at);
```

### 3.4 JSON 컬럼 인덱스
```sql
-- 일정 데이터 JSON 인덱스
CREATE INDEX idx_ai_schedules_schedule_day ON ai_schedules USING GIN ((schedule_data->'schedules'));

-- 작업 결과 JSON 인덱스  
CREATE INDEX idx_ai_jobs_result_gin ON ai_jobs USING GIN (result);

-- 추천 데이터 JSON 인덱스
CREATE INDEX idx_ai_recommendations_data_gin ON ai_recommendations USING GIN (recommendation_data);
```

## 4. 트리거 설계

### 4.1 Updated At 자동 갱신
```sql
-- 모든 테이블에 updated_at 자동 갱신 트리거 적용
CREATE TRIGGER update_ai_schedules_updated_at
    BEFORE UPDATE ON ai_schedules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ai_jobs_updated_at
    BEFORE UPDATE ON ai_jobs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ai_recommendations_updated_at
    BEFORE UPDATE ON ai_recommendations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### 4.2 만료 시간 자동 설정
```sql
-- 일정 만료 시간 자동 설정 (7일)
CREATE OR REPLACE FUNCTION set_schedule_expiry()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.expired_at IS NULL AND NEW.generated_at IS NOT NULL THEN
        NEW.expired_at = NEW.generated_at + INTERVAL '7 days';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_ai_schedules_expiry
    BEFORE INSERT OR UPDATE ON ai_schedules
    FOR EACH ROW
    EXECUTE FUNCTION set_schedule_expiry();

-- 추천 정보 만료 시간 자동 설정 (24시간)
CREATE OR REPLACE FUNCTION set_recommendation_expiry()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.expired_at IS NULL THEN
        NEW.expired_at = NEW.created_at + INTERVAL '24 hours';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_ai_recommendations_expiry
    BEFORE INSERT ON ai_recommendations
    FOR EACH ROW
    EXECUTE FUNCTION set_recommendation_expiry();
```

## 5. 캐시 설계 (Redis)

### 5.1 캐시 전략

#### AI 작업 진행 상태 캐시
```
키 패턴: ai:job:status:{request_id}
데이터: JobStatus JSON
TTL: 1시간
```

#### 생성된 일정 캐시
```
키 패턴: ai:schedule:{trip_id}
데이터: Schedule JSON
TTL: 7일
```

#### 장소 추천 캐시
```
키 패턴: ai:recommendation:{place_id}:{profile_hash}
데이터: Recommendation JSON
TTL: 24시간
```

#### AI 모델 응답 캐시
```
키 패턴: ai:model:response:{request_hash}
데이터: AI Raw Response
TTL: 1시간 (디버깅용)
```

### 5.2 캐시 무효화 정책
- 일정 재생성 시: `ai:schedule:{trip_id}` 삭제
- 작업 완료 시: `ai:job:status:{request_id}` TTL 연장
- 추천 정보 갱신 시: 관련 `ai:recommendation:*` 삭제

## 6. 비동기 처리 설계

### 6.1 메시지 큐 구조
```yaml
큐 이름: ai.schedule.generation
메시지 형식:
  request_id: string
  job_type: string
  payload: object
  priority: integer (1-10)
  max_retries: integer (기본값: 3)
```

### 6.2 재시도 정책
- 즉시 재시도: 네트워크 오류
- 지연 재시도: API 제한 (exponential backoff)
- 포기: 3회 재시도 후 FAILED 상태로 변경

## 7. JSON 스키마 설계

### 7.1 schedule_data 스키마
```json
{
  "trip_id": "string",
  "schedules": [
    {
      "day": "integer",
      "date": "string (YYYY-MM-DD)",
      "city": "string",
      "weather": {
        "condition": "string",
        "min_temperature": "number",
        "max_temperature": "number",
        "icon": "string"
      },
      "places": [
        {
          "place_id": "string",
          "place_name": "string",
          "category": "string",
          "start_time": "string (HH:mm)",
          "duration": "integer",
          "transportation": {
            "type": "string",
            "duration": "integer",
            "distance": "number",
            "route": "string"
          },
          "health_consideration": {
            "rest_points": ["string"],
            "accessibility": ["string"],
            "walking_distance": "number"
          },
          "order": "integer"
        }
      ]
    }
  ],
  "metadata": {
    "generated_at": "string (ISO 8601)",
    "ai_model": "string",
    "generation_time_ms": "integer",
    "total_places": "integer",
    "total_days": "integer"
  }
}
```

### 7.2 recommendation_data 스키마
```json
{
  "place_id": "string",
  "recommendations": {
    "reasons": [
      "string"
    ],
    "tips": {
      "description": "string",
      "events": ["string"],
      "best_visit_time": "string",
      "estimated_duration": "string",
      "photo_spots": ["string"],
      "practical_tips": ["string"],
      "weather_tips": "string",
      "alternative_places": [
        {
          "name": "string",
          "reason": "string",
          "distance": "string"
        }
      ]
    }
  },
  "context": {
    "user_profile_hash": "string",
    "generation_context": "object",
    "ai_model": "string"
  }
}
```

## 8. 성능 최적화

### 8.1 커넥션 풀 설정
```yaml
spring.datasource.hikari:
  maximum-pool-size: 15
  minimum-idle: 5
  idle-timeout: 300000
  connection-timeout: 20000
  max-lifetime: 1200000
```

### 8.2 JSON 쿼리 최적화
```sql
-- 특정 일차 일정 조회
SELECT schedule_data->'schedules' -> (day - 1) as day_schedule
FROM ai_schedules 
WHERE trip_id = ? AND status = 'COMPLETED';

-- 장소별 추천 정보 조회
SELECT recommendation_data->'recommendations'
FROM ai_recommendations
WHERE place_id = ? AND expired_at > NOW();
```

## 9. 배치 작업 설계

### 9.1 만료 데이터 정리
```sql
-- 만료된 일정 데이터 삭제 (매일 2시 실행)
DELETE FROM ai_schedules 
WHERE expired_at < NOW() - INTERVAL '1 day';

-- 만료된 추천 정보 삭제 (매시간 실행)
DELETE FROM ai_recommendations 
WHERE expired_at < NOW();

-- 완료된 작업 정리 (일주일 후 삭제)
DELETE FROM ai_jobs 
WHERE status IN ('COMPLETED', 'FAILED', 'CANCELLED') 
  AND completed_at < NOW() - INTERVAL '7 days';
```

### 9.2 통계 수집
```sql
-- AI 서비스 사용 통계
SELECT 
    job_type,
    status,
    COUNT(*) as count,
    AVG(EXTRACT(EPOCH FROM (completed_at - started_at))) as avg_duration_seconds
FROM ai_jobs 
WHERE created_at >= NOW() - INTERVAL '1 day'
GROUP BY job_type, status;
```

## 10. 모니터링 및 알람

### 10.1 모니터링 지표
- AI 작업 성공/실패율
- 평균 처리 시간
- 큐 대기 작업 수
- 캐시 히트율

### 10.2 알람 설정
- 작업 실패율 > 5%
- 평균 처리 시간 > 30초
- 큐 대기 작업 > 100개
- 데이터베이스 연결 실패

## 11. 백업 및 복구

### 11.1 백업 전략
- 전체 백업: 일 1회 (새벽 3시)
- JSON 컬럼 압축 백업
- 캐시 데이터는 백업 제외 (재생성 가능)

### 11.2 복구 우선순위
1. ai_jobs (작업 상태 복구)
2. ai_schedules (생성된 일정)
3. ai_recommendations (추천 정보)

## 12. 보안 설계

### 12.1 데이터 접근 제어
- AI 서비스 전용 DB 사용자
- 필요 최소 권한만 부여
- JSON 데이터 접근 로깅

### 12.2 개인정보 보호
- 사용자 식별 정보는 해시화
- AI 응답에서 민감 정보 필터링
- 만료된 데이터 자동 삭제