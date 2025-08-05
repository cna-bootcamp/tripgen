# AI Service 개발환경 데이터베이스 설치 가이드

## 개요

AI Service는 AI 기반 일정 생성 및 추천 정보 관리를 담당하는 서비스입니다. PostgreSQL 14를 사용하여 AI 작업 관리, 생성된 일정 데이터, 추천 정보를 효율적으로 관리합니다.

### 주요 기능
- 비동기 AI 일정 생성 작업 관리
- 생성된 일정 데이터 저장 및 관리
- 장소별 AI 추천 정보 캐싱
- 작업 진행률 실시간 추적
- JSON 기반 복합 데이터 처리

## 사전 요구사항

### 필수 소프트웨어
- Docker Desktop
- kubectl
- 최소 2GB 여유 디스크 공간
- 최소 1GB 여유 메모리

### 확인 명령어
```bash
# Docker 상태 확인
docker --version
kubectl version --client
kubectl cluster-info
```

## 설치 절차

### 1단계: 네임스페이스 확인

```bash
# 개발 네임스페이스 확인 (없으면 생성)
kubectl get namespace tripgen-dev || kubectl create namespace tripgen-dev
```

### 2단계: Kubernetes 매니페스트 파일 생성

#### ai-db-configmap.yaml
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ai-db-config
  namespace: tripgen-dev
data:
  POSTGRES_DB: "tripgen_ai_db"
  POSTGRES_USER: "tripgen_ai"
  POSTGRES_SCHEMA: "tripgen_ai"
  PGDATA: "/var/lib/postgresql/data/pgdata"
```

#### ai-db-secret.yaml
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: ai-db-secret
  namespace: tripgen-dev
type: Opaque
data:
  # Base64 인코딩된 값 (실제 운영시 변경 필요)
  POSTGRES_PASSWORD: dHJpcGdlbl9haV8xMjM=  # tripgen_ai_123
```

#### ai-db-pvc.yaml
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: ai-db-pvc
  namespace: tripgen-dev
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 8Gi
  storageClassName: managed
```

#### ai-db-deployment.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-db
  namespace: tripgen-dev
  labels:
    app: ai-db
    component: database
    service: ai-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ai-db
  template:
    metadata:
      labels:
        app: ai-db
        component: database
        service: ai-service
    spec:
      containers:
      - name: postgres
        image: postgres:14-alpine
        ports:
        - containerPort: 5432
          name: postgres
        env:
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: ai-db-secret
              key: POSTGRES_PASSWORD
        envFrom:
        - configMapRef:
            name: ai-db-config
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        - name: init-sql
          mountPath: /docker-entrypoint-initdb.d
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - exec pg_isready -U tripgen_ai -d tripgen_ai_db -h 127.0.0.1 -p 5432
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
          successThreshold: 1
          failureThreshold: 6
        readinessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - exec pg_isready -U tripgen_ai -d tripgen_ai_db -h 127.0.0.1 -p 5432
          initialDelaySeconds: 5
          periodSeconds: 10
          timeoutSeconds: 5
          successThreshold: 1
          failureThreshold: 3
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: ai-db-pvc
      - name: init-sql
        configMap:
          name: ai-db-init-sql
      restartPolicy: Always
```

#### ai-db-service.yaml
```yaml
apiVersion: v1
kind: Service
metadata:
  name: ai-db-service
  namespace: tripgen-dev
  labels:
    app: ai-db
    component: database
    service: ai-service
spec:
  type: ClusterIP
  ports:
  - port: 5432
    targetPort: 5432
    protocol: TCP
    name: postgres
  selector:
    app: ai-db
```

### 3단계: 초기화 SQL 스크립트 준비

#### ai-db-init-configmap.yaml
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ai-db-init-sql
  namespace: tripgen-dev
data:
  01-init-database.sql: |
    -- Database and Schema Creation
    CREATE SCHEMA IF NOT EXISTS tripgen_ai;
    SET search_path TO tripgen_ai;
    
    -- Extensions
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "btree_gin";
    
  02-create-ai-schedules-table.sql: |
    -- Set search path
    SET search_path TO tripgen_ai;
    
    -- ai_schedules 테이블 (AI 생성 일정)
    CREATE TABLE ai_schedules (
        id                  BIGSERIAL PRIMARY KEY,
        schedule_id         VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
        trip_id             VARCHAR(36) NOT NULL,
        user_id             VARCHAR(36) NOT NULL,
        request_data        JSONB NOT NULL,
        generated_schedule  JSONB,
        status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
        start_time          TIME,
        special_requests    TEXT,
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
    
  03-create-ai-jobs-table.sql: |
    -- Set search path
    SET search_path TO tripgen_ai;
    
    -- ai_jobs 테이블 (비동기 작업 관리)
    CREATE TABLE ai_jobs (
        id                  BIGSERIAL PRIMARY KEY,
        job_id              VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
        schedule_id         VARCHAR(36) NOT NULL,
        job_type            VARCHAR(30) NOT NULL,
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
    
  04-create-ai-recommendations-table.sql: |
    -- Set search path
    SET search_path TO tripgen_ai;
    
    -- ai_recommendations 테이블 (장소 추천 정보)
    CREATE TABLE ai_recommendations (
        id                  BIGSERIAL PRIMARY KEY,
        recommendation_id   VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
        place_id            VARCHAR(36) NOT NULL,
        user_id             VARCHAR(36) NOT NULL,
        recommendation_text TEXT NOT NULL,
        useful_tips         TEXT,
        user_context        JSONB NOT NULL,
        place_context       JSONB,
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
    
  05-create-indexes.sql: |
    -- Set search path
    SET search_path TO tripgen_ai;
    
    -- ai_schedules 인덱스
    CREATE UNIQUE INDEX idx_ai_schedules_schedule_id ON ai_schedules(schedule_id);
    CREATE INDEX idx_ai_schedules_trip_id ON ai_schedules(trip_id);
    CREATE INDEX idx_ai_schedules_user_id ON ai_schedules(user_id);
    CREATE INDEX idx_ai_schedules_status ON ai_schedules(status);
    CREATE INDEX idx_ai_schedules_created_at ON ai_schedules(created_at);
    CREATE INDEX idx_ai_schedules_request_data_gin ON ai_schedules USING GIN(request_data);
    CREATE INDEX idx_ai_schedules_generated_schedule_gin ON ai_schedules USING GIN(generated_schedule) WHERE generated_schedule IS NOT NULL;
    
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
    
  06-create-functions.sql: |
    -- Set search path
    SET search_path TO tripgen_ai;
    
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
    
  07-create-triggers.sql: |
    -- Set search path
    SET search_path TO tripgen_ai;
    
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
    
  08-insert-sample-data.sql: |
    -- Set search path
    SET search_path TO tripgen_ai;
    
    -- 샘플 AI 일정 데이터
    INSERT INTO ai_schedules (
        trip_id, user_id, request_data, status, start_time, special_requests,
        created_by, updated_by
    ) VALUES (
        'test-trip-id-001', 
        'test-user-id-001',
        '{
            "members": [
                {
                    "name": "김여행",
                    "age": 35,
                    "health_status": "GOOD",
                    "activity_preferences": ["문화체험", "맛집탐방"]
                }
            ],
            "destinations": [
                {
                    "city_name": "뮌헨",
                    "country": "독일",
                    "nights": 3
                }
            ],
            "transportation": "PUBLIC_TRANSPORT",
            "travel_dates": {
                "start_date": "2024-06-15",
                "end_date": "2024-06-18"
            }
        }'::jsonb,
        'PENDING',
        '09:00:00',
        '전통 맥주 투어 포함',
        uuid_generate_v4()::text,
        uuid_generate_v4()::text
    );
    
    -- 방금 생성된 일정의 schedule_id 가져오기
    WITH latest_schedule AS (
        SELECT schedule_id FROM ai_schedules ORDER BY created_at DESC LIMIT 1
    )
    -- 샘플 AI 작업 데이터
    INSERT INTO ai_jobs (
        schedule_id, job_type, status, input_data, expires_at,
        created_by, updated_by
    ) 
    SELECT 
        ls.schedule_id,
        'GENERATE_SCHEDULE',
        'QUEUED',
        '{
            "priority": "normal",
            "options": {
                "include_weather": true,
                "include_travel_time": true,
                "optimize_for": "experience"
            }
        }'::jsonb,
        CURRENT_TIMESTAMP + INTERVAL '1 hour',
        uuid_generate_v4()::text,
        uuid_generate_v4()::text
    FROM latest_schedule ls;
    
    -- 샘플 AI 추천 데이터
    INSERT INTO ai_recommendations (
        place_id, user_id, recommendation_text, useful_tips, 
        user_context, place_context,
        created_by, updated_by
    ) VALUES (
        'place-marienplatz-001',
        'test-user-id-001',
        '마리엔플라츠는 뮌헨의 심장부로, 고딕 양식의 신시청사와 아름다운 글로켄슈필로 유명합니다. 35세 여성 여행객이 문화체험을 선호하신다면 꼭 방문해야 할 필수 명소입니다.',
        '글로켄슈필 공연은 매일 11시, 12시, 17시(3월~10월)에 있으니 시간을 맞춰 방문하세요. 주변에 전통 독일 레스토랑들이 많아 점심 식사하기에도 좋습니다.',
        '{
            "age": 35,
            "gender": "FEMALE",
            "preferences": ["문화체험", "맛집탐방"],
            "health_status": "GOOD"
        }'::jsonb,
        '{
            "weather_dependency": "low",
            "visit_duration": 60,
            "best_time": "MORNING",
            "crowd_level": "HIGH"
        }'::jsonb,
        uuid_generate_v4()::text,
        uuid_generate_v4()::text
    );
    
  99-comments-and-analyze.sql: |
    -- Set search path
    SET search_path TO tripgen_ai;
    
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
```

### 4단계: 리소스 배포

```bash
# 1. ConfigMap과 Secret 생성
kubectl apply -f ai-db-configmap.yaml
kubectl apply -f ai-db-secret.yaml
kubectl apply -f ai-db-init-configmap.yaml

# 2. PVC 생성
kubectl apply -f ai-db-pvc.yaml

# 3. Deployment와 Service 생성
kubectl apply -f ai-db-deployment.yaml
kubectl apply -f ai-db-service.yaml

# 4. 배포 상태 확인
kubectl get all -n tripgen-dev -l app=ai-db
```

## 데이터베이스 초기화

### 초기화 확인

```bash
# Pod 상태 확인
kubectl get pods -n tripgen-dev -l app=ai-db

# Pod 로그 확인 (초기화 과정 모니터링)
kubectl logs -n tripgen-dev -l app=ai-db -f

# 데이터베이스 접속 테스트
kubectl exec -it -n tripgen-dev deployment/ai-db -- psql -U tripgen_ai -d tripgen_ai_db
```

### 수동 초기화 확인

```bash
# Pod 내부 접속
kubectl exec -it -n tripgen-dev deployment/ai-db -- bash

# 데이터베이스 접속
psql -U tripgen_ai -d tripgen_ai_db

# 스키마 및 테이블 확인
\dt tripgen_ai.*

# 샘플 데이터 확인
SET search_path TO tripgen_ai;
SELECT schedule_id, trip_id, status, generation_attempts FROM ai_schedules;
SELECT job_id, job_type, status, progress_percentage FROM ai_jobs;
SELECT recommendation_id, place_id, LENGTH(recommendation_text) as text_length FROM ai_recommendations;
```

## 연결 테스트

### 1. 포트 포워딩 설정

```bash
# 로컬에서 데이터베이스 접근
kubectl port-forward -n tripgen-dev service/ai-db-service 5434:5432
```

### 2. 외부 클라이언트 연결

```bash
# psql 클라이언트 연결
psql -h localhost -p 5434 -U tripgen_ai -d tripgen_ai_db

# 연결 정보
# Host: localhost
# Port: 5434
# Database: tripgen_ai_db
# Username: tripgen_ai
# Password: tripgen_ai_123
# Schema: tripgen_ai
```

### 3. 애플리케이션 연결 설정

#### Spring Boot application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://ai-db-service.tripgen-dev.svc.cluster.local:5432/tripgen_ai_db
    username: tripgen_ai
    password: tripgen_ai_123
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    properties:
      hibernate:
        default_schema: tripgen_ai
```

## JSON 데이터 타입 지원 설정

### JSONB 활용 테스트

```sql
SET search_path TO tripgen_ai;

-- 요청 데이터에서 특정 필드 조회
SELECT schedule_id, 
       request_data->>'transportation' as transportation,
       request_data->'members'->0->>'name' as first_member_name
FROM ai_schedules;

-- 멤버의 활동 선호도 검색
SELECT recommendation_id, place_id
FROM ai_recommendations 
WHERE user_context->'preferences' ? '문화체험';

-- 복잡한 JSON 쿼리
SELECT schedule_id, 
       jsonb_array_length(request_data->'members') as member_count,
       request_data->'travel_dates'->>'start_date' as start_date
FROM ai_schedules 
WHERE request_data->'destinations'->0->>'country' = '독일';
```

### 진행률 자동 계산 테스트

```sql
SET search_path TO tripgen_ai;

-- 작업 상태 변경 및 진행률 확인
SELECT job_id, status, progress_percentage, progress_message FROM ai_jobs;

-- 상태 변경 테스트
UPDATE ai_jobs SET status = 'PROCESSING' WHERE job_type = 'GENERATE_SCHEDULE';
SELECT job_id, status, progress_percentage, progress_message, started_at FROM ai_jobs;

UPDATE ai_jobs SET progress_percentage = 50, progress_message = 'AI 분석 중' WHERE status = 'PROCESSING';
SELECT job_id, status, progress_percentage, progress_message FROM ai_jobs;

UPDATE ai_jobs SET status = 'COMPLETED' WHERE status = 'PROCESSING';
SELECT job_id, status, progress_percentage, progress_message, completed_at FROM ai_jobs;
```

### 알림 기능 테스트

```sql
SET search_path TO tripgen_ai;

-- 알림 리스너 설정 (별도 세션에서)
LISTEN schedule_completed;

-- 일정 완료 상태로 변경 (알림 발생)
UPDATE ai_schedules SET status = 'COMPLETED' WHERE status = 'PENDING';
SELECT schedule_id, status, generated_at FROM ai_schedules;
```

## 문제 해결

### 일반적인 문제

#### 1. Pod이 시작되지 않는 경우

```bash
# 리소스 상태 확인
kubectl describe pod -n tripgen-dev -l app=ai-db

# 이벤트 확인
kubectl get events -n tripgen-dev --sort-by='.lastTimestamp'
```

#### 2. JSONB 인덱스 성능 문제

```sql
SET search_path TO tripgen_ai;

-- GIN 인덱스 성능 확인
EXPLAIN ANALYZE SELECT * FROM ai_schedules WHERE request_data->'members'->0->>'name' = '김여행';

-- 인덱스 사용량 확인
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch 
FROM pg_stat_user_indexes 
WHERE schemaname = 'tripgen_ai';
```

#### 3. 트리거 함수 오류

```sql
-- 함수 존재 확인
SELECT proname FROM pg_proc WHERE proname LIKE '%ai%';

-- 트리거 확인
SELECT tgname, tgrelid::regclass FROM pg_trigger WHERE tgname LIKE '%ai%';

-- 함수 재생성
SET search_path TO tripgen_ai;
\i /docker-entrypoint-initdb.d/06-create-functions.sql
```

### 성능 최적화

#### JSONB 최적화 설정

```sql
SET search_path TO tripgen_ai;

-- JSONB 압축 설정
ALTER TABLE ai_schedules ALTER COLUMN request_data SET STORAGE EXTENDED;
ALTER TABLE ai_schedules ALTER COLUMN generated_schedule SET STORAGE EXTENDED;

-- 통계 수집
ANALYZE ai_schedules;
ANALYZE ai_jobs;
ANALYZE ai_recommendations;
```

#### 메모리 설정 최적화

```sql
-- JSON 처리를 위한 메모리 설정
ALTER SYSTEM SET work_mem = '16MB';
ALTER SYSTEM SET maintenance_work_mem = '256MB';
ALTER SYSTEM SET shared_buffers = '512MB';
SELECT pg_reload_conf();
```

### 데이터 정합성 검사

```sql
SET search_path TO tripgen_ai;

-- 외래키 무결성 확인
SELECT COUNT(*) FROM ai_jobs aj 
WHERE NOT EXISTS (SELECT 1 FROM ai_schedules s WHERE s.schedule_id = aj.schedule_id);

-- 만료된 데이터 확인
SELECT COUNT(*) as expired_recommendations 
FROM ai_recommendations 
WHERE expires_at < CURRENT_TIMESTAMP;

SELECT COUNT(*) as expired_jobs 
FROM ai_jobs 
WHERE expires_at IS NOT NULL AND expires_at < CURRENT_TIMESTAMP;

-- JSON 데이터 유효성 확인
SELECT schedule_id, request_data 
FROM ai_schedules 
WHERE NOT (request_data ? 'members' AND request_data ? 'destinations');
```

## 개발환경 특화 설정

### 개발용 유틸리티 함수

```sql
SET search_path TO tripgen_ai;

-- 개발용 데이터 정리 함수
CREATE OR REPLACE FUNCTION reset_ai_dev_data()
RETURNS VOID AS $$
BEGIN
    DELETE FROM ai_recommendations;
    DELETE FROM ai_jobs;
    DELETE FROM ai_schedules;
    
    -- 시퀀스 리셋
    ALTER SEQUENCE ai_schedules_id_seq RESTART WITH 1;
    ALTER SEQUENCE ai_jobs_id_seq RESTART WITH 1;
    ALTER SEQUENCE ai_recommendations_id_seq RESTART WITH 1;
END;
$$ LANGUAGE plpgsql;

-- 샘플 데이터 생성 함수
CREATE OR REPLACE FUNCTION create_sample_ai_data(user_id_param VARCHAR DEFAULT 'test-user-001')
RETURNS VARCHAR AS $$
DECLARE
    new_schedule_id VARCHAR;
BEGIN
    -- 샘플 일정 생성
    INSERT INTO ai_schedules (trip_id, user_id, request_data, status)
    VALUES (
        'trip-' || user_id_param,
        user_id_param,
        format('{
            "members": [{"name": "테스트사용자", "age": 30, "preferences": ["관광"]}],
            "destinations": [{"city_name": "서울", "nights": 2}],
            "transportation": "PUBLIC_TRANSPORT"
        }')::jsonb,
        'PENDING'
    ) RETURNING schedule_id INTO new_schedule_id;
    
    -- 관련 작업 생성
    INSERT INTO ai_jobs (schedule_id, job_type, input_data)
    VALUES (
        new_schedule_id,
        'GENERATE_SCHEDULE',
        '{"priority": "normal"}'::jsonb
    );
    
    RETURN new_schedule_id;
END;
$$ LANGUAGE plpgsql;
```

### 모니터링 쿼리

```sql
SET search_path TO tripgen_ai;

-- 작업 상태 대시보드
SELECT 
    status,
    COUNT(*) as count,
    AVG(progress_percentage) as avg_progress,
    MAX(created_at) as latest_created
FROM ai_jobs 
GROUP BY status 
ORDER BY count DESC;

-- 생성 성공률
SELECT 
    status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM ai_schedules 
GROUP BY status;

-- 추천 정보 히트율
SELECT 
    DATE(created_at) as date,
    COUNT(*) as total_recommendations,
    COUNT(*) FILTER (WHERE expires_at > CURRENT_TIMESTAMP) as active_recommendations
FROM ai_recommendations 
WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY DATE(created_at)
ORDER BY date DESC;
```

---

**다음 단계**: [Location Service 데이터베이스 설치](db-location-dev.md)

**관련 문서**:
- [User Service 데이터베이스 설치](db-user-dev.md)
- [Trip Service 데이터베이스 설치](db-trip-dev.md)
- [전체 개발환경 구성 가이드](../README-dev.md)