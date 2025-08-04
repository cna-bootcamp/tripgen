# Trip Service 개발환경 데이터베이스 설치 가이드

## 개요

Trip Service는 여행 기본정보, 여행지, 일정 관리를 담당하는 핵심 서비스입니다. PostgreSQL 14를 사용하여 복잡한 여행 데이터를 효율적으로 관리하며, JSONB 타입과 고급 트리거를 활용합니다.

### 주요 기능
- 여행 계획 생성 및 관리
- 여행 멤버 및 선호도 관리
- 여행지별 숙박 정보 관리
- 일정 및 장소별 상세 스케줄링
- 여행 진행률 자동 계산

## 사전 요구사항

### 필수 소프트웨어
- Docker Desktop
- kubectl
- 최소 3GB 여유 디스크 공간
- 최소 2GB 여유 메모리

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

#### trip-db-configmap.yaml
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: trip-db-config
  namespace: tripgen-dev
data:
  POSTGRES_DB: "tripgen_trip_db"
  POSTGRES_USER: "tripgen_trip"
  POSTGRES_SCHEMA: "tripgen_trip"
  PGDATA: "/var/lib/postgresql/data/pgdata"
```

#### trip-db-secret.yaml
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: trip-db-secret
  namespace: tripgen-dev
type: Opaque
data:
  # Base64 인코딩된 값 (실제 운영시 변경 필요)
  POSTGRES_PASSWORD: dHJpcGdlbl90cmlwXzEyMw==  # tripgen_trip_123
```

#### trip-db-pvc.yaml
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: trip-db-pvc
  namespace: tripgen-dev
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
  storageClassName: standard
```

#### trip-db-deployment.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: trip-db
  namespace: tripgen-dev
  labels:
    app: trip-db
    component: database
    service: trip-service
spec:
  replicas: 1
  selector:
    matchLabels:
      app: trip-db
  template:
    metadata:
      labels:
        app: trip-db
        component: database
        service: trip-service
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
              name: trip-db-secret
              key: POSTGRES_PASSWORD
        envFrom:
        - configMapRef:
            name: trip-db-config
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
            - exec pg_isready -U tripgen_trip -d tripgen_trip_db -h 127.0.0.1 -p 5432
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
            - exec pg_isready -U tripgen_trip -d tripgen_trip_db -h 127.0.0.1 -p 5432
          initialDelaySeconds: 5
          periodSeconds: 10
          timeoutSeconds: 5
          successThreshold: 1
          failureThreshold: 3
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: trip-db-pvc
      - name: init-sql
        configMap:
          name: trip-db-init-sql
      restartPolicy: Always
```

#### trip-db-service.yaml
```yaml
apiVersion: v1
kind: Service
metadata:
  name: trip-db-service
  namespace: tripgen-dev
  labels:
    app: trip-db
    component: database
    service: trip-service
spec:
  type: ClusterIP
  ports:
  - port: 5432
    targetPort: 5432
    protocol: TCP
    name: postgres
  selector:
    app: trip-db
```

### 3단계: 초기화 SQL 스크립트 준비

#### trip-db-init-configmap.yaml
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: trip-db-init-sql
  namespace: tripgen-dev
data:
  01-init-database.sql: |
    -- Database and Schema Creation
    CREATE SCHEMA IF NOT EXISTS tripgen_trip;
    SET search_path TO tripgen_trip;
    
    -- Extensions
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "btree_gin";
    
  02-create-trips-table.sql: |
    -- Set search path
    SET search_path TO tripgen_trip;
    
    -- trips 테이블 (여행 기본 정보)
    CREATE TABLE trips (
        id                  BIGSERIAL PRIMARY KEY,
        trip_id             VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
        user_id             VARCHAR(36) NOT NULL,
        title               VARCHAR(100) NOT NULL,
        description         TEXT,
        status              VARCHAR(20) NOT NULL DEFAULT 'PLANNING',
        transportation      VARCHAR(20) NOT NULL,
        start_date          DATE,
        end_date            DATE,
        start_time          TIME DEFAULT '09:00:00',
        special_requests    TEXT,
        progress_percentage INTEGER NOT NULL DEFAULT 0,
        current_step        VARCHAR(30) NOT NULL DEFAULT 'BASIC_SETTINGS',
        total_days          INTEGER GENERATED ALWAYS AS (
            CASE 
                WHEN start_date IS NOT NULL AND end_date IS NOT NULL 
                THEN EXTRACT(days FROM end_date - start_date) + 1
                ELSE NULL 
            END
        ) STORED,
        created_by          VARCHAR(36),
        updated_by          VARCHAR(36),
        created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        
        -- 제약조건
        CONSTRAINT chk_trips_status CHECK (status IN ('PLANNING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
        CONSTRAINT chk_trips_transportation CHECK (transportation IN ('PUBLIC_TRANSPORT', 'CAR', 'WALKING', 'MIXED')),
        CONSTRAINT chk_trips_progress CHECK (progress_percentage >= 0 AND progress_percentage <= 100),
        CONSTRAINT chk_trips_current_step CHECK (current_step IN ('BASIC_SETTINGS', 'DESTINATION_SETTINGS', 'AI_GENERATION', 'SCHEDULE_VIEW', 'COMPLETED')),
        CONSTRAINT chk_trips_title_length CHECK (LENGTH(title) >= 1 AND LENGTH(title) <= 100),
        CONSTRAINT chk_trips_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
        CONSTRAINT chk_trips_start_time CHECK (start_time BETWEEN '06:00:00' AND '23:59:59')
    );
    
  03-create-members-table.sql: |
    -- Set search path
    SET search_path TO tripgen_trip;
    
    -- members 테이블 (여행 멤버 정보)
    CREATE TABLE members (
        id                  BIGSERIAL PRIMARY KEY,
        member_id           VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
        trip_id             VARCHAR(36) NOT NULL,
        name                VARCHAR(50) NOT NULL,
        age                 INTEGER NOT NULL,
        gender              VARCHAR(10) NOT NULL,
        health_status       VARCHAR(20) NOT NULL,
        activity_preferences JSONB,
        dietary_restrictions JSONB,
        special_needs       TEXT,
        member_order        INTEGER NOT NULL DEFAULT 1,
        created_by          VARCHAR(36),
        updated_by          VARCHAR(36),
        created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        
        -- 제약조건
        CONSTRAINT chk_members_age CHECK (age >= 0 AND age <= 120),
        CONSTRAINT chk_members_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
        CONSTRAINT chk_members_health_status CHECK (health_status IN ('EXCELLENT', 'GOOD', 'CAUTION', 'LIMITED')),
        CONSTRAINT chk_members_name_length CHECK (LENGTH(name) >= 2 AND LENGTH(name) <= 50),
        CONSTRAINT chk_members_order CHECK (member_order >= 1),
        
        -- 외래키
        CONSTRAINT fk_members_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE
    );
    
  04-create-destinations-table.sql: |
    -- Set search path
    SET search_path TO tripgen_trip;
    
    -- destinations 테이블 (여행지 정보)
    CREATE TABLE destinations (
        id                  BIGSERIAL PRIMARY KEY,
        destination_id      VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
        trip_id             VARCHAR(36) NOT NULL,
        city_name           VARCHAR(100) NOT NULL,
        country             VARCHAR(50),
        accommodation_name  VARCHAR(100),
        check_in_time       TIME DEFAULT '15:00:00',
        check_out_time      TIME DEFAULT '11:00:00',
        nights              INTEGER NOT NULL,
        arrival_date        DATE,
        departure_date      DATE,
        destination_order   INTEGER NOT NULL DEFAULT 1,
        notes               TEXT,
        created_by          VARCHAR(36),
        updated_by          VARCHAR(36),
        created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        
        -- 제약조건
        CONSTRAINT chk_destinations_nights CHECK (nights >= 1 AND nights <= 30),
        CONSTRAINT chk_destinations_city_name_length CHECK (LENGTH(city_name) >= 1 AND LENGTH(city_name) <= 100),
        CONSTRAINT chk_destinations_accommodation_length CHECK (accommodation_name IS NULL OR LENGTH(accommodation_name) <= 100),
        CONSTRAINT chk_destinations_order CHECK (destination_order >= 1),
        CONSTRAINT chk_destinations_times CHECK (check_out_time != check_in_time),
        CONSTRAINT chk_destinations_dates CHECK (departure_date IS NULL OR arrival_date IS NULL OR departure_date >= arrival_date),
        
        -- 외래키
        CONSTRAINT fk_destinations_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE
    );
    
  05-create-schedules-table.sql: |
    -- Set search path
    SET search_path TO tripgen_trip;
    
    -- schedules 테이블 (일정 정보)
    CREATE TABLE schedules (
        id                  BIGSERIAL PRIMARY KEY,
        schedule_id         VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
        trip_id             VARCHAR(36) NOT NULL,
        destination_id      VARCHAR(36) NOT NULL,
        day_number          INTEGER NOT NULL,
        schedule_date       DATE NOT NULL,
        weather_info        JSONB,
        daily_theme         VARCHAR(100),
        total_walking_distance INTEGER DEFAULT 0,
        total_drive_time    INTEGER DEFAULT 0,
        estimated_cost      DECIMAL(10,2),
        notes               TEXT,
        created_by          VARCHAR(36),
        updated_by          VARCHAR(36),
        created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        
        -- 제약조건
        CONSTRAINT chk_schedules_day_number CHECK (day_number >= 1 AND day_number <= 365),
        CONSTRAINT chk_schedules_walking_distance CHECK (total_walking_distance >= 0),
        CONSTRAINT chk_schedules_drive_time CHECK (total_drive_time >= 0),
        CONSTRAINT chk_schedules_estimated_cost CHECK (estimated_cost IS NULL OR estimated_cost >= 0),
        CONSTRAINT chk_schedules_theme_length CHECK (daily_theme IS NULL OR LENGTH(daily_theme) <= 100),
        
        -- 외래키
        CONSTRAINT fk_schedules_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE,
        CONSTRAINT fk_schedules_destination_id FOREIGN KEY (destination_id) REFERENCES destinations(destination_id) ON DELETE CASCADE
    );
    
  06-create-schedule-places-table.sql: |
    -- Set search path
    SET search_path TO tripgen_trip;
    
    -- schedule_places 테이블 (일정별 장소 정보)
    CREATE TABLE schedule_places (
        id                  BIGSERIAL PRIMARY KEY,
        schedule_place_id   VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
        schedule_id         VARCHAR(36) NOT NULL,
        place_id            VARCHAR(36) NOT NULL,
        place_name          VARCHAR(200) NOT NULL,
        place_category      VARCHAR(50) NOT NULL,
        start_time          TIME NOT NULL,
        end_time            TIME,
        duration_minutes    INTEGER,
        visit_order         INTEGER NOT NULL DEFAULT 1,
        transportation_to_next VARCHAR(30),
        travel_time_to_next INTEGER DEFAULT 0,
        travel_distance_to_next INTEGER DEFAULT 0,
        travel_route_info   JSONB,
        weather_alternative JSONB,
        health_considerations JSONB,
        special_notes       TEXT,
        is_optional         BOOLEAN NOT NULL DEFAULT FALSE,
        created_by          VARCHAR(36),
        updated_by          VARCHAR(36),
        created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
        
        -- 제약조건
        CONSTRAINT chk_schedule_places_times CHECK (end_time IS NULL OR end_time > start_time),
        CONSTRAINT chk_schedule_places_duration CHECK (duration_minutes IS NULL OR (duration_minutes >= 15 AND duration_minutes <= 480)),
        CONSTRAINT chk_schedule_places_visit_order CHECK (visit_order >= 1),
        CONSTRAINT chk_schedule_places_transportation CHECK (transportation_to_next IS NULL OR transportation_to_next IN ('WALKING', 'PUBLIC_TRANSPORT', 'CAR', 'TAXI', 'BIKE')),
        CONSTRAINT chk_schedule_places_travel_time CHECK (travel_time_to_next >= 0 AND travel_time_to_next <= 300),
        CONSTRAINT chk_schedule_places_travel_distance CHECK (travel_distance_to_next >= 0),
        CONSTRAINT chk_schedule_places_place_name_length CHECK (LENGTH(place_name) >= 1 AND LENGTH(place_name) <= 200),
        CONSTRAINT chk_schedule_places_category CHECK (place_category IN ('RESTAURANT', 'TOURIST_ATTRACTION', 'LODGING', 'SHOPPING', 'ENTERTAINMENT', 'TRANSPORTATION', 'SERVICE', 'OTHER')),
        
        -- 외래키
        CONSTRAINT fk_schedule_places_schedule_id FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id) ON DELETE CASCADE
    );
    
  07-create-indexes.sql: |
    -- Set search path
    SET search_path TO tripgen_trip;
    
    -- trips 인덱스
    CREATE UNIQUE INDEX idx_trips_trip_id ON trips(trip_id);
    CREATE INDEX idx_trips_user_id ON trips(user_id);
    CREATE INDEX idx_trips_status ON trips(status);
    CREATE INDEX idx_trips_current_step ON trips(current_step);
    CREATE INDEX idx_trips_start_date ON trips(start_date) WHERE start_date IS NOT NULL;
    CREATE INDEX idx_trips_created_at ON trips(created_at);
    CREATE INDEX idx_trips_user_status ON trips(user_id, status);
    CREATE INDEX idx_trips_active ON trips(id) WHERE status IN ('PLANNING', 'IN_PROGRESS');
    
    -- members 인덱스
    CREATE UNIQUE INDEX idx_members_member_id ON members(member_id);
    CREATE INDEX idx_members_trip_id ON members(trip_id);
    CREATE INDEX idx_members_health_status ON members(health_status);
    CREATE INDEX idx_members_trip_order ON members(trip_id, member_order);
    CREATE INDEX idx_members_activity_preferences_gin ON members USING GIN(activity_preferences);
    
    -- destinations 인덱스
    CREATE UNIQUE INDEX idx_destinations_destination_id ON destinations(destination_id);
    CREATE INDEX idx_destinations_trip_id ON destinations(trip_id);
    CREATE INDEX idx_destinations_city_name ON destinations(city_name);
    CREATE INDEX idx_destinations_arrival_date ON destinations(arrival_date) WHERE arrival_date IS NOT NULL;
    CREATE INDEX idx_destinations_trip_order ON destinations(trip_id, destination_order);
    
    -- schedules 인덱스
    CREATE UNIQUE INDEX idx_schedules_schedule_id ON schedules(schedule_id);
    CREATE INDEX idx_schedules_trip_id ON schedules(trip_id);
    CREATE INDEX idx_schedules_destination_id ON schedules(destination_id);
    CREATE INDEX idx_schedules_schedule_date ON schedules(schedule_date);
    CREATE INDEX idx_schedules_trip_day ON schedules(trip_id, day_number);
    CREATE INDEX idx_schedules_weather_info_gin ON schedules USING GIN(weather_info);
    
    -- schedule_places 인덱스
    CREATE UNIQUE INDEX idx_schedule_places_schedule_place_id ON schedule_places(schedule_place_id);
    CREATE INDEX idx_schedule_places_schedule_id ON schedule_places(schedule_id);
    CREATE INDEX idx_schedule_places_place_id ON schedule_places(place_id);
    CREATE INDEX idx_schedule_places_start_time ON schedule_places(start_time);
    CREATE INDEX idx_schedule_places_place_category ON schedule_places(place_category);
    CREATE INDEX idx_schedule_places_schedule_order ON schedule_places(schedule_id, visit_order);
    CREATE INDEX idx_schedule_places_travel_route_gin ON schedule_places USING GIN(travel_route_info);
    
    -- 복합 인덱스
    CREATE INDEX idx_trips_user_date_status ON trips(user_id, start_date DESC, status);
    CREATE INDEX idx_schedule_places_schedule_time ON schedule_places(schedule_id, start_time, visit_order);
    
  08-create-functions.sql: |
    -- Set search path
    SET search_path TO tripgen_trip;
    
    -- 자동 갱신 트리거 함수들
    CREATE OR REPLACE FUNCTION update_trips_updated_at()
    RETURNS TRIGGER AS $$
    BEGIN
        NEW.updated_at = CURRENT_TIMESTAMP;
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;
    
    CREATE OR REPLACE FUNCTION update_members_updated_at()
    RETURNS TRIGGER AS $$
    BEGIN
        NEW.updated_at = CURRENT_TIMESTAMP;
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;
    
    CREATE OR REPLACE FUNCTION update_destinations_updated_at()
    RETURNS TRIGGER AS $$
    BEGIN
        NEW.updated_at = CURRENT_TIMESTAMP;
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;
    
    CREATE OR REPLACE FUNCTION update_schedules_updated_at()
    RETURNS TRIGGER AS $$
    BEGIN
        NEW.updated_at = CURRENT_TIMESTAMP;
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;
    
    CREATE OR REPLACE FUNCTION update_schedule_places_updated_at()
    RETURNS TRIGGER AS $$
    BEGIN
        NEW.updated_at = CURRENT_TIMESTAMP;
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;
    
    -- 여행 진행률 자동 계산 함수
    CREATE OR REPLACE FUNCTION calculate_trip_progress()
    RETURNS TRIGGER AS $$
    DECLARE
        total_steps INTEGER := 4;
        current_progress INTEGER := 0;
    BEGIN
        -- 단계별 진행률 계산
        CASE NEW.current_step
            WHEN 'BASIC_SETTINGS' THEN current_progress := 25;
            WHEN 'DESTINATION_SETTINGS' THEN current_progress := 50;
            WHEN 'AI_GENERATION' THEN current_progress := 75;
            WHEN 'SCHEDULE_VIEW' THEN current_progress := 90;
            WHEN 'COMPLETED' THEN current_progress := 100;
            ELSE current_progress := 0;
        END CASE;
        
        NEW.progress_percentage = current_progress;
        
        -- 상태 자동 변경
        IF NEW.current_step = 'COMPLETED' AND NEW.status = 'PLANNING' THEN
            NEW.status = 'IN_PROGRESS';
        END IF;
        
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;
    
    -- 여행 통계 조회 함수
    CREATE OR REPLACE FUNCTION get_trip_statistics(input_user_id VARCHAR)
    RETURNS TABLE(
        total_trips INTEGER,
        planning_trips INTEGER,
        in_progress_trips INTEGER,
        completed_trips INTEGER,
        total_destinations INTEGER,
        total_days INTEGER
    ) AS $$
    BEGIN
        RETURN QUERY
        SELECT 
            COUNT(*)::INTEGER as total_trips,
            COUNT(*) FILTER (WHERE status = 'PLANNING')::INTEGER as planning_trips,
            COUNT(*) FILTER (WHERE status = 'IN_PROGRESS')::INTEGER as in_progress_trips,
            COUNT(*) FILTER (WHERE status = 'COMPLETED')::INTEGER as completed_trips,
            (SELECT COUNT(*)::INTEGER FROM destinations d JOIN trips t ON d.trip_id = t.trip_id WHERE t.user_id = input_user_id) as total_destinations,
            COALESCE(SUM(total_days), 0)::INTEGER as total_days
        FROM trips 
        WHERE user_id = input_user_id 
          AND status != 'CANCELLED';
    END;
    $$ LANGUAGE plpgsql;
    
  09-create-triggers.sql: |
    -- Set search path
    SET search_path TO tripgen_trip;
    
    -- 트리거 생성
    CREATE TRIGGER trg_trips_updated_at
        BEFORE UPDATE ON trips
        FOR EACH ROW
        EXECUTE FUNCTION update_trips_updated_at();
    
    CREATE TRIGGER trg_members_updated_at
        BEFORE UPDATE ON members
        FOR EACH ROW
        EXECUTE FUNCTION update_members_updated_at();
    
    CREATE TRIGGER trg_destinations_updated_at
        BEFORE UPDATE ON destinations
        FOR EACH ROW
        EXECUTE FUNCTION update_destinations_updated_at();
    
    CREATE TRIGGER trg_schedules_updated_at
        BEFORE UPDATE ON schedules
        FOR EACH ROW
        EXECUTE FUNCTION update_schedules_updated_at();
    
    CREATE TRIGGER trg_schedule_places_updated_at
        BEFORE UPDATE ON schedule_places
        FOR EACH ROW
        EXECUTE FUNCTION update_schedule_places_updated_at();
    
    CREATE TRIGGER trg_trips_progress
        BEFORE UPDATE ON trips
        FOR EACH ROW
        EXECUTE FUNCTION calculate_trip_progress();
    
  10-insert-sample-data.sql: |
    -- Set search path
    SET search_path TO tripgen_trip;
    
    -- 샘플 여행 데이터
    INSERT INTO trips (
        user_id, title, description, status, transportation, 
        start_date, end_date, start_time, special_requests,
        created_by, updated_by
    ) VALUES (
        'test-user-id-001', 
        '독일 뮌헨 여행', 
        '독일 뮌헨에서의 3박 4일 여행', 
        'PLANNING', 
        'PUBLIC_TRANSPORT',
        '2024-06-15', 
        '2024-06-18', 
        '09:00:00',
        '전통 맥주 투어 포함',
        uuid_generate_v4()::text,
        uuid_generate_v4()::text
    );
    
    -- 방금 생성된 여행의 trip_id 가져오기
    WITH latest_trip AS (
        SELECT trip_id FROM trips WHERE title = '독일 뮌헨 여행' ORDER BY created_at DESC LIMIT 1
    )
    -- 샘플 멤버 데이터
    INSERT INTO members (
        trip_id, name, age, gender, health_status, 
        activity_preferences, dietary_restrictions, special_needs,
        created_by, updated_by
    ) 
    SELECT 
        lt.trip_id,
        '김여행',
        35,
        'FEMALE',
        'GOOD',
        '["문화체험", "맛집탐방", "사진촬영"]'::jsonb,
        '["글루텐프리"]'::jsonb,
        '도보 이동 선호',
        uuid_generate_v4()::text,
        uuid_generate_v4()::text
    FROM latest_trip lt;
    
    WITH latest_trip AS (
        SELECT trip_id FROM trips WHERE title = '독일 뮌헨 여행' ORDER BY created_at DESC LIMIT 1
    )
    -- 샘플 여행지 데이터
    INSERT INTO destinations (
        trip_id, city_name, country, accommodation_name,
        check_in_time, check_out_time, nights, destination_order,
        created_by, updated_by
    ) 
    SELECT 
        lt.trip_id,
        '뮌헨',
        '독일',
        'Hotel München Palace',
        '15:00:00',
        '11:00:00',
        3,
        1,
        uuid_generate_v4()::text,
        uuid_generate_v4()::text
    FROM latest_trip lt;
    
  99-comments-and-analyze.sql: |
    -- Set search path
    SET search_path TO tripgen_trip;
    
    -- 테이블 코멘트
    COMMENT ON TABLE trips IS '여행 기본 정보 및 설정';
    COMMENT ON TABLE members IS '여행 멤버 정보 및 프로필';
    COMMENT ON TABLE destinations IS '여행지별 숙박 및 일정 정보';
    COMMENT ON TABLE schedules IS '일자별 일정 정보';
    COMMENT ON TABLE schedule_places IS '일정별 방문 장소 상세 정보';
    
    -- 컬럼 코멘트
    COMMENT ON COLUMN trips.trip_id IS '외부 서비스 참조용 비즈니스 키';
    COMMENT ON COLUMN trips.progress_percentage IS '여행 계획 진행률 (0-100)';
    COMMENT ON COLUMN trips.total_days IS '총 여행 일수 (자동 계산)';
    COMMENT ON COLUMN members.activity_preferences IS '활동 선호도 배열 (JSONB)';
    COMMENT ON COLUMN destinations.nights IS '숙박일 수';
    COMMENT ON COLUMN schedule_places.travel_route_info IS '이동 경로 상세 정보 (JSONB)';
    COMMENT ON COLUMN schedule_places.health_considerations IS '건강상태별 고려사항 (JSONB)';
    
    -- 성능 통계 수집
    ANALYZE trips, members, destinations, schedules, schedule_places;
```

### 4단계: 리소스 배포

```bash
# 1. ConfigMap과 Secret 생성
kubectl apply -f trip-db-configmap.yaml
kubectl apply -f trip-db-secret.yaml
kubectl apply -f trip-db-init-configmap.yaml

# 2. PVC 생성
kubectl apply -f trip-db-pvc.yaml

# 3. Deployment와 Service 생성
kubectl apply -f trip-db-deployment.yaml
kubectl apply -f trip-db-service.yaml

# 4. 배포 상태 확인
kubectl get all -n tripgen-dev -l app=trip-db
```

## 데이터베이스 초기화

### 초기화 확인

```bash
# Pod 상태 확인
kubectl get pods -n tripgen-dev -l app=trip-db

# Pod 로그 확인 (초기화 과정 모니터링)
kubectl logs -n tripgen-dev -l app=trip-db -f

# 데이터베이스 접속 테스트
kubectl exec -it -n tripgen-dev deployment/trip-db -- psql -U tripgen_trip -d tripgen_trip_db
```

### 수동 초기화 확인

```bash
# Pod 내부 접속
kubectl exec -it -n tripgen-dev deployment/trip-db -- bash

# 데이터베이스 접속
psql -U tripgen_trip -d tripgen_trip_db

# 스키마 및 테이블 확인
\dt tripgen_trip.*

# 샘플 데이터 확인
SET search_path TO tripgen_trip;
SELECT trip_id, title, status, current_step, progress_percentage FROM trips;
SELECT member_id, name, age, health_status FROM members;
SELECT destination_id, city_name, country, nights FROM destinations;
```

## 연결 테스트

### 1. 포트 포워딩 설정

```bash
# 로컬에서 데이터베이스 접근
kubectl port-forward -n tripgen-dev service/trip-db-service 5433:5432
```

### 2. 외부 클라이언트 연결

```bash
# psql 클라이언트 연결
psql -h localhost -p 5433 -U tripgen_trip -d tripgen_trip_db

# 연결 정보
# Host: localhost
# Port: 5433
# Database: tripgen_trip_db
# Username: tripgen_trip
# Password: tripgen_trip_123
# Schema: tripgen_trip
```

### 3. 애플리케이션 연결 설정

#### Spring Boot application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://trip-db-service.tripgen-dev.svc.cluster.local:5432/tripgen_trip_db
    username: tripgen_trip
    password: tripgen_trip_123
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    properties:
      hibernate:
        default_schema: tripgen_trip
```

## 트리거 설정

### 진행률 자동 계산 테스트

```sql
-- 여행 상태 변경 테스트
SET search_path TO tripgen_trip;

-- 현재 상태 확인
SELECT trip_id, current_step, progress_percentage, status FROM trips;

-- 단계 변경 테스트
UPDATE trips SET current_step = 'DESTINATION_SETTINGS' WHERE title = '독일 뮌헨 여행';
SELECT trip_id, current_step, progress_percentage, status FROM trips;

UPDATE trips SET current_step = 'AI_GENERATION' WHERE title = '독일 뮌헨 여행';
SELECT trip_id, current_step, progress_percentage, status FROM trips;
```

### JSONB 데이터 활용 테스트

```sql
SET search_path TO tripgen_trip;

-- 활동 선호도 검색
SELECT name, activity_preferences 
FROM members 
WHERE activity_preferences ? '문화체험';

-- 식이 제한사항 확인
SELECT name, dietary_restrictions 
FROM members 
WHERE dietary_restrictions ? '글루텐프리';
```

## 문제 해결

### 일반적인 문제

#### 1. Pod이 시작되지 않는 경우

```bash
# 리소스 상태 확인
kubectl describe pod -n tripgen-dev -l app=trip-db

# 스토리지 문제 확인
kubectl describe pvc -n tripgen-dev trip-db-pvc
```

#### 2. 초기화 스크립트 실행 실패

```bash
# ConfigMap 내용 확인
kubectl describe configmap -n tripgen-dev trip-db-init-sql

# 스크립트 수동 실행
kubectl exec -it -n tripgen-dev deployment/trip-db -- bash
psql -U tripgen_trip -d tripgen_trip_db -f /docker-entrypoint-initdb.d/02-create-trips-table.sql
```

#### 3. 트리거 함수 문제

```sql
-- 함수 존재 확인
SELECT proname FROM pg_proc WHERE proname LIKE '%trip%';

-- 트리거 확인
SELECT tgname, tgrelid::regclass FROM pg_trigger WHERE tgname LIKE '%trip%';

-- 함수 재생성
SET search_path TO tripgen_trip;
\i /docker-entrypoint-initdb.d/08-create-functions.sql
```

### JSONB 인덱스 최적화

```sql
SET search_path TO tripgen_trip;

-- GIN 인덱스 성능 확인
EXPLAIN ANALYZE SELECT * FROM members WHERE activity_preferences ? '문화체험';

-- 인덱스 재구성 (필요시)
REINDEX INDEX idx_members_activity_preferences_gin;
```

### 데이터 정합성 검사

```sql
SET search_path TO tripgen_trip;

-- 외래키 무결성 확인
SELECT COUNT(*) FROM members m 
WHERE NOT EXISTS (SELECT 1 FROM trips t WHERE t.trip_id = m.trip_id);

SELECT COUNT(*) FROM destinations d 
WHERE NOT EXISTS (SELECT 1 FROM trips t WHERE t.trip_id = d.trip_id);

-- 진행률 일관성 확인
SELECT trip_id, current_step, progress_percentage,
       CASE current_step
           WHEN 'BASIC_SETTINGS' THEN 25
           WHEN 'DESTINATION_SETTINGS' THEN 50
           WHEN 'AI_GENERATION' THEN 75
           WHEN 'SCHEDULE_VIEW' THEN 90
           WHEN 'COMPLETED' THEN 100
           ELSE 0
       END as expected_progress
FROM trips 
WHERE progress_percentage != 
    CASE current_step
        WHEN 'BASIC_SETTINGS' THEN 25
        WHEN 'DESTINATION_SETTINGS' THEN 50
        WHEN 'AI_GENERATION' THEN 75
        WHEN 'SCHEDULE_VIEW' THEN 90
        WHEN 'COMPLETED' THEN 100
        ELSE 0
    END;
```

## 개발환경 특화 설정

### 개발용 유틸리티 함수

```sql
SET search_path TO tripgen_trip;

-- 개발용 데이터 정리 함수
CREATE OR REPLACE FUNCTION reset_dev_data()
RETURNS VOID AS $$
BEGIN
    DELETE FROM schedule_places;
    DELETE FROM schedules;
    DELETE FROM destinations;
    DELETE FROM members;
    DELETE FROM trips;
    
    -- 시퀀스 리셋
    ALTER SEQUENCE trips_id_seq RESTART WITH 1;
    ALTER SEQUENCE members_id_seq RESTART WITH 1;
    ALTER SEQUENCE destinations_id_seq RESTART WITH 1;
    ALTER SEQUENCE schedules_id_seq RESTART WITH 1;
    ALTER SEQUENCE schedule_places_id_seq RESTART WITH 1;
END;
$$ LANGUAGE plpgsql;

-- 사용법
-- SELECT reset_dev_data();
```

### 성능 최적화 (개발용)

```sql
-- 개발환경용 PostgreSQL 설정
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET maintenance_work_mem = '128MB';
ALTER SYSTEM SET work_mem = '8MB';
ALTER SYSTEM SET random_page_cost = 1.1;
SELECT pg_reload_conf();
```

---

**다음 단계**: [AI Service 데이터베이스 설치](db-ai-dev.md)

**관련 문서**:
- [User Service 데이터베이스 설치](db-user-dev.md)
- [Location Service 데이터베이스 설치](db-location-dev.md)
- [전체 개발환경 구성 가이드](../README-dev.md)