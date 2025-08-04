# AI 서비스 데이터베이스 설치 가이드

## 1. 설치 개요

### 1.1 데이터베이스 정보
- **데이터베이스 유형**: PostgreSQL 15+
- **데이터베이스명**: tripgen_ai
- **스키마명**: ai_schema
- **서비스 사용자**: ai_service
- **패스워드**: AIServiceDev2025!
- **포트**: 5432

### 1.2 설치 대상
- AI 기반 여행 일정 생성 데이터베이스
- 일정 생성/추천/작업 관리 테이블
- Layered Architecture 기반 설계

## 2. 사전 준비

### 2.1 PostgreSQL 설치 확인
```bash
# PostgreSQL 버전 확인
psql --version

# PostgreSQL 서비스 상태 확인
sudo systemctl status postgresql
```

### 2.2 관리자 접속
```bash
# PostgreSQL 관리자로 접속
sudo -u postgres psql
```

## 3. 데이터베이스 생성

### 3.1 데이터베이스 및 사용자 생성
```sql
-- 데이터베이스 생성
CREATE DATABASE tripgen_ai
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- AI 서비스 전용 사용자 생성
CREATE USER ai_service WITH
    LOGIN
    NOSUPERUSER
    CREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'AIServiceDev2025!';

-- 데이터베이스 소유권 변경
ALTER DATABASE tripgen_ai OWNER TO ai_service;

-- 사용자 권한 부여
GRANT ALL PRIVILEGES ON DATABASE tripgen_ai TO ai_service;
```

### 3.2 스키마 생성
```sql
-- tripgen_ai 데이터베이스에 연결
\c tripgen_ai

-- 스키마 생성
CREATE SCHEMA IF NOT EXISTS ai_schema AUTHORIZATION ai_service;

-- 기본 스키마 설정
ALTER USER ai_service SET search_path = ai_schema;

-- 확장 기능 활성화
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

## 4. 테이블 생성

### 4.1 기본 함수 생성
```sql
-- updated_at 자동 갱신 함수
CREATE OR REPLACE FUNCTION ai_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

### 4.2 ai_schedules 테이블 생성
```sql
CREATE TABLE ai_schema.ai_schedules (
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
COMMENT ON TABLE ai_schema.ai_schedules IS 'AI 생성 여행 일정 저장 테이블';
COMMENT ON COLUMN ai_schema.ai_schedules.request_id IS '일정 생성 요청 고유 식별자';
COMMENT ON COLUMN ai_schema.ai_schedules.trip_id IS 'Trip 서비스의 여행 ID';
COMMENT ON COLUMN ai_schema.ai_schedules.schedule_data IS '생성된 일정 JSON 데이터';
COMMENT ON COLUMN ai_schema.ai_schedules.expired_at IS '캐시 만료 시간 (NULL이면 영구 보존)';
```

### 4.3 ai_jobs 테이블 생성
```sql
CREATE TABLE ai_schema.ai_jobs (
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
COMMENT ON TABLE ai_schema.ai_jobs IS 'AI 작업 관리 테이블';
COMMENT ON COLUMN ai_schema.ai_jobs.job_type IS '작업 유형 (일정생성/재생성/추천/날씨분석)';
COMMENT ON COLUMN ai_schema.ai_jobs.payload IS '작업 입력 매개변수 JSON';
COMMENT ON COLUMN ai_schema.ai_jobs.result IS '작업 결과 JSON';
COMMENT ON COLUMN ai_schema.ai_jobs.progress IS '작업 진행률 (0-100%)';
COMMENT ON COLUMN ai_schema.ai_jobs.current_step IS '현재 처리 중인 단계 설명';
```

### 4.4 ai_recommendations 테이블 생성
```sql
CREATE TABLE ai_schema.ai_recommendations (
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
COMMENT ON TABLE ai_schema.ai_recommendations IS 'AI 생성 장소 추천 정보 저장 테이블';
COMMENT ON COLUMN ai_schema.ai_recommendations.place_id IS 'Location 서비스의 장소 ID';
COMMENT ON COLUMN ai_schema.ai_recommendations.profile_hash IS '사용자 프로파일 해시 (캐시 키)';
COMMENT ON COLUMN ai_schema.ai_recommendations.recommendation_data IS '추천 이유 및 팁 JSON 데이터';
COMMENT ON COLUMN ai_schema.ai_recommendations.expired_at IS '추천 정보 만료 시간';
```

## 5. 인덱스 생성

### 5.1 Unique Index
```sql
-- 비즈니스 키 유니크 인덱스
CREATE UNIQUE INDEX idx_ai_schedules_request_id ON ai_schema.ai_schedules(request_id);
CREATE UNIQUE INDEX idx_ai_jobs_request_id ON ai_schema.ai_jobs(request_id);

-- 추천 정보 복합 유니크 인덱스
CREATE UNIQUE INDEX idx_ai_recommendations_place_profile ON ai_schema.ai_recommendations(place_id, profile_hash);
```

### 5.2 Performance Index
```sql
-- ai_schedules 성능 인덱스
CREATE INDEX idx_ai_schedules_trip_id ON ai_schema.ai_schedules(trip_id);
CREATE INDEX idx_ai_schedules_status ON ai_schema.ai_schedules(status);
CREATE INDEX idx_ai_schedules_expired_at ON ai_schema.ai_schedules(expired_at) WHERE expired_at IS NOT NULL;

-- ai_jobs 성능 인덱스
CREATE INDEX idx_ai_jobs_status ON ai_schema.ai_jobs(status);
CREATE INDEX idx_ai_jobs_type_status ON ai_schema.ai_jobs(job_type, status);
CREATE INDEX idx_ai_jobs_created_at ON ai_schema.ai_jobs(created_at);
CREATE INDEX idx_ai_jobs_processing ON ai_schema.ai_jobs(status, started_at) WHERE status = 'PROCESSING';

-- ai_recommendations 성능 인덱스
CREATE INDEX idx_ai_recommendations_place_id ON ai_schema.ai_recommendations(place_id);
CREATE INDEX idx_ai_recommendations_expired_at ON ai_schema.ai_recommendations(expired_at);
CREATE INDEX idx_ai_recommendations_generated_at ON ai_schema.ai_recommendations(generated_at);
```

### 5.3 JSON 컬럼 Index
```sql
-- 일정 데이터 JSON 인덱스
CREATE INDEX idx_ai_schedules_schedule_day ON ai_schema.ai_schedules USING GIN ((schedule_data->'schedules'));

-- 작업 결과 JSON 인덱스  
CREATE INDEX idx_ai_jobs_result_gin ON ai_schema.ai_jobs USING GIN (result);

-- 추천 데이터 JSON 인덱스
CREATE INDEX idx_ai_recommendations_data_gin ON ai_schema.ai_recommendations USING GIN (recommendation_data);
```

## 6. 트리거 생성

### 6.1 Updated At 자동 갱신 트리거
```sql
-- 모든 테이블에 updated_at 자동 갱신 트리거 적용
CREATE TRIGGER update_ai_schedules_updated_at
    BEFORE UPDATE ON ai_schema.ai_schedules
    FOR EACH ROW
    EXECUTE FUNCTION ai_schema.update_updated_at_column();

CREATE TRIGGER update_ai_jobs_updated_at
    BEFORE UPDATE ON ai_schema.ai_jobs
    FOR EACH ROW
    EXECUTE FUNCTION ai_schema.update_updated_at_column();

CREATE TRIGGER update_ai_recommendations_updated_at
    BEFORE UPDATE ON ai_schema.ai_recommendations
    FOR EACH ROW
    EXECUTE FUNCTION ai_schema.update_updated_at_column();
```

### 6.2 만료 시간 자동 설정 트리거
```sql
-- 일정 만료 시간 자동 설정 (7일)
CREATE OR REPLACE FUNCTION ai_schema.set_schedule_expiry()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.expired_at IS NULL AND NEW.generated_at IS NOT NULL THEN
        NEW.expired_at = NEW.generated_at + INTERVAL '7 days';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_ai_schedules_expiry
    BEFORE INSERT OR UPDATE ON ai_schema.ai_schedules
    FOR EACH ROW
    EXECUTE FUNCTION ai_schema.set_schedule_expiry();

-- 추천 정보 만료 시간 자동 설정 (24시간)
CREATE OR REPLACE FUNCTION ai_schema.set_recommendation_expiry()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.expired_at IS NULL THEN
        NEW.expired_at = NEW.created_at + INTERVAL '24 hours';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_ai_recommendations_expiry
    BEFORE INSERT ON ai_schema.ai_recommendations
    FOR EACH ROW
    EXECUTE FUNCTION ai_schema.set_recommendation_expiry();
```

### 6.3 Request ID 자동 생성 트리거
```sql
-- Request ID 자동 생성 함수들
CREATE OR REPLACE FUNCTION ai_schema.generate_request_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.request_id IS NULL THEN
        NEW.request_id = gen_random_uuid()::VARCHAR(36);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Request ID 생성 트리거들
CREATE TRIGGER generate_ai_schedules_request_id
    BEFORE INSERT ON ai_schema.ai_schedules
    FOR EACH ROW
    EXECUTE FUNCTION ai_schema.generate_request_id();

CREATE TRIGGER generate_ai_jobs_request_id
    BEFORE INSERT ON ai_schema.ai_jobs
    FOR EACH ROW
    EXECUTE FUNCTION ai_schema.generate_request_id();
```

## 7. 초기 데이터 삽입

### 7.1 테스트 일정 생성 작업 데이터
```sql
-- 테스트 AI 작업 데이터
INSERT INTO ai_schema.ai_jobs (job_type, status, payload, progress, current_step) VALUES
('SCHEDULE_GENERATION', 'COMPLETED', '{"trip_id":"trip-001","preferences":["SIGHTSEEING","CULTURE"],"transport_mode":"PUBLIC"}', 100, 'completed'),
('SCHEDULE_GENERATION', 'PROCESSING', '{"trip_id":"trip-002","preferences":["NATURE","SPORTS"],"transport_mode":"CAR"}', 75, 'generating_places'),
('RECOMMENDATION_GENERATION', 'COMPLETED', '{"place_id":"LOC-001","user_profile":"active_tourist"}', 100, 'completed');

-- 테스트 생성된 일정 데이터
INSERT INTO ai_schema.ai_schedules (request_id, trip_id, status, schedule_data, generated_at) VALUES
((SELECT request_id FROM ai_schema.ai_jobs WHERE job_type = 'SCHEDULE_GENERATION' AND status = 'COMPLETED' LIMIT 1), 
 'trip-001', 'COMPLETED', 
 '{
   "trip_id": "trip-001",
   "schedules": [
     {
       "day": 1,
       "date": "2024-12-15",
       "city": "서울",
       "weather": {
         "condition": "Sunny",
         "min_temperature": -2.5,
         "max_temperature": 5.8,
         "icon": "sun"
       },
       "places": [
         {
           "place_id": "LOC-001",
           "place_name": "경복궁",
           "category": "TOURIST",
           "start_time": "09:00",
           "duration": 120,
           "transportation": {
             "type": "PUBLIC",
             "duration": 30,
             "distance": 5.2,
             "route": "지하철 3호선 경복궁역"
           },
           "health_consideration": {
             "rest_points": ["매표소 휴게실", "궁궐 내 벤치"],
             "accessibility": ["엘리베이터", "경사로"],
             "walking_distance": 2.1
           },
           "order": 1
         }
       ]
     }
   ],
   "metadata": {
     "generated_at": "2024-12-01T10:00:00Z",
     "ai_model": "gpt-4o",
     "generation_time_ms": 15420,
     "total_places": 6,
     "total_days": 4
   }
 }', 
 CURRENT_TIMESTAMP);

-- 테스트 장소 추천 데이터
INSERT INTO ai_schema.ai_recommendations (place_id, profile_hash, recommendation_data) VALUES
('LOC-001', SHA256('{"age":28,"preferences":["SIGHTSEEING","CULTURE"],"health":"EXCELLENT"}'::bytea)::text, 
 '{
   "place_id": "LOC-001",
   "recommendations": {
     "reasons": [
       "조선왕조의 대표적인 궁궐로 역사적 가치가 높음",
       "전통 건축미를 감상할 수 있는 최적의 장소",
       "교통 접근성이 우수하여 방문하기 편리함"
     ],
     "tips": {
       "description": "경복궁은 조선시대 정궁으로 웅장한 건축미와 역사적 의미를 동시에 경험할 수 있습니다.",
       "events": ["수문장 교대식 (10시, 14시, 15시)", "야간개장 (4-10월)"],
       "best_visit_time": "오전 9-11시 (관광객이 적고 사진촬영에 최적)",
       "estimated_duration": "2-3시간",
       "photo_spots": ["근정전", "경회루", "향원정"],
       "practical_tips": ["편한 신발 착용 필수", "입장료 현금/카드 결제 가능", "가이드 투어 추천"],
       "weather_tips": "겨울철 방문 시 따뜻한 옷차림 필수, 눈 온 후 방문하면 더욱 아름다운 풍경 감상 가능",
       "alternative_places": [
         {
           "name": "창덕궁",
           "reason": "유네스코 세계문화유산으로 자연과 조화로운 건축미",
           "distance": "1.5km"
         }
       ]
     }
   },
   "context": {
     "user_profile_hash": "' || SHA256('{"age":28,"preferences":["SIGHTSEEING","CULTURE"],"health":"EXCELLENT"}'::bytea)::text || '",
     "generation_context": {
       "season": "winter",
       "group_type": "couple",
       "activity_level": "moderate"
     },
     "ai_model": "gpt-4o"
   }
 }'),
('LOC-002', SHA256('{"age":25,"preferences":["NATURE","SPORTS"],"health":"EXCELLENT"}'::bytea)::text,
 '{
   "place_id": "LOC-002",
   "recommendations": {
     "reasons": [
       "부산 대표 해수욕장으로 자연 경관이 뛰어남",
       "다양한 수상 스포츠 활동 가능",
       "주변 먹거리와 볼거리가 풍부함"
     ],
     "tips": {
       "description": "해운대는 부산의 대표적인 관광지로 아름다운 해변과 다양한 레저 활동을 즐길 수 있습니다.",
       "events": ["해운대 빛축제 (12-1월)", "부산 바다축제 (8월)"],
       "best_visit_time": "일출 시간대 (오전 7-8시) 또는 일몰 시간대 (오후 5-6시)",
       "estimated_duration": "3-4시간",
       "photo_spots": ["해운대 해변", "동백섬", "APEC 누리마루"],
       "practical_tips": ["겨울철에도 산책로 이용 가능", "주차는 유료", "주변 카페에서 바다 전망 감상"],
       "weather_tips": "겨울철 바닷바람이 차가우니 방풍 옷차림 필수",
       "alternative_places": [
         {
           "name": "광안리 해수욕장",
           "reason": "광안대교 야경이 아름다운 또 다른 명소",
           "distance": "3.2km"
         }
       ]
     }
   },
   "context": {
     "user_profile_hash": "' || SHA256('{"age":25,"preferences":["NATURE","SPORTS"],"health":"EXCELLENT"}'::bytea)::text || '",
     "generation_context": {
       "season": "winter",
       "group_type": "solo",
       "activity_level": "high"
     },
     "ai_model": "gpt-4o"
   }
 }');
```

### 7.2 작업 진행 상태 업데이트
```sql
-- 진행 중인 작업의 상태 업데이트
UPDATE ai_schema.ai_jobs 
SET result = '{
  "generated_places": 8,
  "processed_preferences": ["NATURE", "SPORTS"],
  "current_city": "부산",
  "estimated_completion": "2024-12-01T11:30:00Z"
}'
WHERE status = 'PROCESSING';
```

## 8. 권한 설정

### 8.1 AI 서비스 사용자 권한
```sql
-- 스키마에 대한 모든 권한 부여
GRANT ALL PRIVILEGES ON SCHEMA ai_schema TO ai_service;

-- 테이블에 대한 권한 부여
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA ai_schema TO ai_service;

-- 시퀀스에 대한 권한 부여
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA ai_schema TO ai_service;

-- 함수에 대한 권한 부여
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA ai_schema TO ai_service;

-- 향후 생성될 객체에 대한 기본 권한 설정
ALTER DEFAULT PRIVILEGES IN SCHEMA ai_schema GRANT ALL ON TABLES TO ai_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA ai_schema GRANT ALL ON SEQUENCES TO ai_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA ai_schema GRANT EXECUTE ON FUNCTIONS TO ai_service;
```

## 9. 연결 테스트

### 9.1 연결 확인
```bash
# AI 서비스 사용자로 데이터베이스 연결 테스트
psql -h localhost -p 5432 -U ai_service -d tripgen_ai -c "SELECT current_database(), current_schema(), current_user;"
```

### 9.2 기본 쿼리 테스트
```sql
-- 테이블 목록 확인
SELECT table_name, table_type FROM information_schema.tables WHERE table_schema = 'ai_schema';

-- AI 작업 목록 조회
SELECT request_id, job_type, status, progress FROM ai_schema.ai_jobs;

-- 생성된 일정 수 확인
SELECT status, COUNT(*) as count 
FROM ai_schema.ai_schedules 
GROUP BY status;

-- 추천 정보 수 확인
SELECT COUNT(*) as recommendation_count 
FROM ai_schema.ai_recommendations;
```

### 9.3 JSON 쿼리 테스트
```sql
-- 특정 일차 일정 조회
SELECT schedule_data->'schedules' -> 0 as first_day_schedule
FROM ai_schema.ai_schedules 
WHERE trip_id = 'trip-001' AND status = 'COMPLETED';

-- 장소별 추천 정보 조회
SELECT recommendation_data->'recommendations'->'reasons'
FROM ai_schema.ai_recommendations
WHERE place_id = 'LOC-001';

-- 진행 중인 작업의 현재 단계 조회
SELECT request_id, current_step, progress, result->'current_city'
FROM ai_schema.ai_jobs
WHERE status = 'PROCESSING';
```

## 10. 백업 설정

### 10.1 백업 스크립트 생성
```bash
#!/bin/bash
# AI 서비스 데이터베이스 백업 스크립트

DB_NAME="tripgen_ai"
BACKUP_DIR="/backup/ai"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/ai_backup_$DATE.sql"

# 백업 디렉토리 생성
mkdir -p $BACKUP_DIR

# 데이터베이스 백업
pg_dump -h localhost -U ai_service -d $DB_NAME > $BACKUP_FILE

# 백업 파일 압축
gzip $BACKUP_FILE

echo "Backup completed: ${BACKUP_FILE}.gz"
```

### 10.2 백업 복원 스크립트
```bash
#!/bin/bash
# AI 서비스 데이터베이스 복원 스크립트

if [ -z "$1" ]; then
    echo "Usage: $0 <backup_file>"
    exit 1
fi

BACKUP_FILE=$1
DB_NAME="tripgen_ai"

# 백업 파일 압축 해제 (필요한 경우)
if [[ $BACKUP_FILE == *.gz ]]; then
    gunzip $BACKUP_FILE
    BACKUP_FILE=${BACKUP_FILE%.gz}
fi

# 데이터베이스 복원
psql -h localhost -U ai_service -d $DB_NAME < $BACKUP_FILE

echo "Restore completed from: $BACKUP_FILE"
```

## 11. 모니터링 설정

### 11.1 성능 모니터링 쿼리
```sql
-- 활성 연결 수 확인
SELECT count(*) as active_connections 
FROM pg_stat_activity 
WHERE datname = 'tripgen_ai' AND state = 'active';

-- 테이블 크기 확인
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'ai_schema'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 작업 상태별 통계
SELECT 
    job_type,
    status,
    COUNT(*) as count,
    AVG(progress) as avg_progress
FROM ai_schema.ai_jobs 
WHERE created_at >= NOW() - INTERVAL '24 hours'
GROUP BY job_type, status;

-- JSON 데이터 크기 확인
SELECT 
    'ai_schedules' as table_name,
    AVG(pg_column_size(schedule_data)) as avg_json_size
FROM ai_schema.ai_schedules
UNION ALL
SELECT 
    'ai_recommendations' as table_name,
    AVG(pg_column_size(recommendation_data)) as avg_json_size
FROM ai_schema.ai_recommendations;
```

### 11.2 배치 작업 모니터링
```sql
-- 만료 예정 데이터 확인
SELECT 
    'ai_schedules' as table_name,
    COUNT(*) as expiring_soon
FROM ai_schema.ai_schedules 
WHERE expired_at BETWEEN NOW() AND NOW() + INTERVAL '1 day'
UNION ALL
SELECT 
    'ai_recommendations' as table_name,
    COUNT(*) as expiring_soon
FROM ai_schema.ai_recommendations 
WHERE expired_at BETWEEN NOW() AND NOW() + INTERVAL '1 day';

-- 장시간 실행 중인 작업 확인
SELECT 
    request_id,
    job_type,
    current_step,
    progress,
    EXTRACT(EPOCH FROM (NOW() - started_at))/60 as running_minutes
FROM ai_schema.ai_jobs 
WHERE status = 'PROCESSING' 
  AND started_at < NOW() - INTERVAL '30 minutes';
```

## 12. 설치 완료 확인

### 12.1 설치 검증 체크리스트
- [ ] 데이터베이스 생성 완료
- [ ] 스키마 생성 완료
- [ ] 모든 테이블 생성 완료
- [ ] 인덱스 생성 완료
- [ ] 트리거 생성 완료
- [ ] 초기 데이터 삽입 완료
- [ ] 권한 설정 완료
- [ ] 연결 테스트 성공
- [ ] JSON 쿼리 테스트 성공
- [ ] 백업 설정 완료

### 12.2 문제 해결
```sql
-- 권한 문제 시
GRANT ALL PRIVILEGES ON DATABASE tripgen_ai TO ai_service;
GRANT ALL PRIVILEGES ON SCHEMA ai_schema TO ai_service;

-- JSON 인덱스 재생성 (필요한 경우)
REINDEX INDEX ai_schema.idx_ai_schedules_schedule_day;
REINDEX INDEX ai_schema.idx_ai_jobs_result_gin;
REINDEX INDEX ai_schema.idx_ai_recommendations_data_gin;

-- 연결 문제 시 PostgreSQL 설정 확인
-- postgresql.conf: listen_addresses = '*'
-- pg_hba.conf: host tripgen_ai ai_service 0.0.0.0/0 md5
```

설치가 완료되었습니다. AI 서비스 데이터베이스가 준비되었으며, AI 기반 여행 일정 생성 및 추천을 위한 모든 테이블과 기능이 구성되었습니다.