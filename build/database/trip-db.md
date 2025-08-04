# Trip 서비스 데이터베이스 설치 가이드

## 1. 설치 개요

### 1.1 데이터베이스 정보
- **데이터베이스 유형**: PostgreSQL 15+
- **데이터베이스명**: tripgen_trip
- **스키마명**: trip_schema
- **서비스 사용자**: trip_service
- **패스워드**: TripServiceDev2025!
- **포트**: 5432

### 1.2 설치 대상
- 여행 계획 관리 데이터베이스
- 멤버/목적지/일정 관리 테이블
- Clean Architecture 기반 설계

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
CREATE DATABASE tripgen_trip
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Trip 서비스 전용 사용자 생성
CREATE USER trip_service WITH
    LOGIN
    NOSUPERUSER
    CREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'TripServiceDev2025!';

-- 데이터베이스 소유권 변경
ALTER DATABASE tripgen_trip OWNER TO trip_service;

-- 사용자 권한 부여
GRANT ALL PRIVILEGES ON DATABASE tripgen_trip TO trip_service;
```

### 3.2 스키마 생성
```sql
-- tripgen_trip 데이터베이스에 연결
\c tripgen_trip

-- 스키마 생성
CREATE SCHEMA IF NOT EXISTS trip_schema AUTHORIZATION trip_service;

-- 기본 스키마 설정
ALTER USER trip_service SET search_path = trip_schema;

-- 확장 기능 활성화
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

## 4. 테이블 생성

### 4.1 기본 함수 생성
```sql
-- updated_at 자동 갱신 함수
CREATE OR REPLACE FUNCTION trip_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

### 4.2 trips 테이블 생성
```sql
CREATE TABLE trip_schema.trips (
    id                  BIGSERIAL PRIMARY KEY,
    trip_id            VARCHAR(36) UNIQUE NOT NULL,     -- UUID
    user_id            VARCHAR(36) NOT NULL,            -- User 서비스의 userId
    trip_name          VARCHAR(200) NOT NULL,
    transport_mode     VARCHAR(20) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PLANNING',
    current_step       VARCHAR(50),
    start_date         DATE,
    end_date           DATE,
    progress           INTEGER NOT NULL DEFAULT 0,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_trips_transport_mode CHECK (transport_mode IN ('PUBLIC', 'CAR')),
    CONSTRAINT chk_trips_status CHECK (status IN ('PLANNING', 'ONGOING', 'COMPLETED')),
    CONSTRAINT chk_trips_progress CHECK (progress >= 0 AND progress <= 100),
    CONSTRAINT chk_trips_date_order CHECK (end_date >= start_date OR (start_date IS NULL AND end_date IS NULL)),
    CONSTRAINT chk_trips_trip_name_length CHECK (LENGTH(trip_name) >= 2 AND LENGTH(trip_name) <= 200)
);

-- 댓글
COMMENT ON TABLE trip_schema.trips IS '여행 기본 정보 테이블';
COMMENT ON COLUMN trip_schema.trips.trip_id IS '여행 고유 식별자 (UUID)';
COMMENT ON COLUMN trip_schema.trips.user_id IS 'User 서비스의 사용자 ID';
COMMENT ON COLUMN trip_schema.trips.transport_mode IS '교통수단 (PUBLIC/CAR)';
COMMENT ON COLUMN trip_schema.trips.status IS '여행 상태 (PLANNING/ONGOING/COMPLETED)';
COMMENT ON COLUMN trip_schema.trips.current_step IS '현재 진행 단계';
COMMENT ON COLUMN trip_schema.trips.progress IS '진행률 (0-100%)';
```

### 4.3 members 테이블 생성
```sql
CREATE TABLE trip_schema.members (
    id                  BIGSERIAL PRIMARY KEY,
    member_id          VARCHAR(36) UNIQUE NOT NULL,     -- UUID
    trip_id            VARCHAR(36) NOT NULL,            -- trips.trip_id 참조
    name               VARCHAR(100) NOT NULL,
    age                INTEGER NOT NULL,
    gender             VARCHAR(10) NOT NULL,
    health_status      VARCHAR(20) NOT NULL,
    preferences        JSONB,                           -- 선호도 배열
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_members_gender CHECK (gender IN ('MALE', 'FEMALE')),
    CONSTRAINT chk_members_health_status CHECK (health_status IN ('EXCELLENT', 'GOOD', 'CAUTION', 'LIMITED')),
    CONSTRAINT chk_members_age CHECK (age >= 0 AND age <= 150),
    CONSTRAINT chk_members_name_length CHECK (LENGTH(name) >= 2 AND LENGTH(name) <= 100),
    
    -- 외래키
    CONSTRAINT fk_members_trip_id FOREIGN KEY (trip_id) REFERENCES trip_schema.trips(trip_id) ON DELETE CASCADE
);

-- 댓글
COMMENT ON TABLE trip_schema.members IS '여행 멤버 정보 테이블';
COMMENT ON COLUMN trip_schema.members.member_id IS '멤버 고유 식별자 (UUID)';
COMMENT ON COLUMN trip_schema.members.trip_id IS '여행 ID (trips.trip_id 참조)';
COMMENT ON COLUMN trip_schema.members.gender IS '성별 (MALE/FEMALE)';
COMMENT ON COLUMN trip_schema.members.health_status IS '건강 상태 (EXCELLENT/GOOD/CAUTION/LIMITED)';
COMMENT ON COLUMN trip_schema.members.preferences IS '선호도 JSON 배열 (SIGHTSEEING/SHOPPING/CULTURE/NATURE/SPORTS/REST)';
```

### 4.4 destinations 테이블 생성
```sql
CREATE TABLE trip_schema.destinations (
    id                  BIGSERIAL PRIMARY KEY,
    destination_id     VARCHAR(36) UNIQUE NOT NULL,     -- UUID
    trip_id            VARCHAR(36) NOT NULL,            -- trips.trip_id 참조
    destination_name   VARCHAR(200) NOT NULL,
    nights             INTEGER NOT NULL,
    start_date         DATE,
    end_date           DATE,
    accommodation      VARCHAR(300),
    check_in_time      VARCHAR(10),                     -- HH:mm 형식
    check_out_time     VARCHAR(10),                     -- HH:mm 형식
    order_seq          INTEGER NOT NULL,               -- 방문 순서
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_destinations_nights CHECK (nights >= 0 AND nights <= 365),
    CONSTRAINT chk_destinations_date_order CHECK (end_date >= start_date OR (start_date IS NULL AND end_date IS NULL)),
    CONSTRAINT chk_destinations_check_in_time CHECK (check_in_time IS NULL OR check_in_time ~ '^([01]?[0-9]|2[0-3]):[0-5][0-9]$'),
    CONSTRAINT chk_destinations_check_out_time CHECK (check_out_time IS NULL OR check_out_time ~ '^([01]?[0-9]|2[0-3]):[0-5][0-9]$'),
    CONSTRAINT chk_destinations_name_length CHECK (LENGTH(destination_name) >= 2 AND LENGTH(destination_name) <= 200),
    CONSTRAINT chk_destinations_order_seq CHECK (order_seq >= 1),
    
    -- 외래키
    CONSTRAINT fk_destinations_trip_id FOREIGN KEY (trip_id) REFERENCES trip_schema.trips(trip_id) ON DELETE CASCADE
);

-- 댓글
COMMENT ON TABLE trip_schema.destinations IS '여행 목적지 정보 테이블';
COMMENT ON COLUMN trip_schema.destinations.destination_id IS '목적지 고유 식별자 (UUID)';
COMMENT ON COLUMN trip_schema.destinations.trip_id IS '여행 ID (trips.trip_id 참조)';
COMMENT ON COLUMN trip_schema.destinations.nights IS '숙박 일수';
COMMENT ON COLUMN trip_schema.destinations.accommodation IS '숙박 시설 정보';
COMMENT ON COLUMN trip_schema.destinations.check_in_time IS '체크인 시간 (HH:mm)';
COMMENT ON COLUMN trip_schema.destinations.check_out_time IS '체크아웃 시간 (HH:mm)';
COMMENT ON COLUMN trip_schema.destinations.order_seq IS '방문 순서';
```

### 4.5 schedules 테이블 생성
```sql
CREATE TABLE trip_schema.schedules (
    id                  BIGSERIAL PRIMARY KEY,
    schedule_id        VARCHAR(36) UNIQUE NOT NULL,     -- UUID
    trip_id            VARCHAR(36) NOT NULL,            -- trips.trip_id 참조
    day                INTEGER NOT NULL,                -- 여행 일차
    date               DATE NOT NULL,                   -- 해당 날짜
    city               VARCHAR(100) NOT NULL,           -- 도시명
    weather_condition  VARCHAR(50),                     -- 날씨 상태
    min_temperature    DECIMAL(5,2),                    -- 최저 기온
    max_temperature    DECIMAL(5,2),                    -- 최고 기온
    weather_icon       VARCHAR(50),                     -- 날씨 아이콘
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_schedules_day CHECK (day >= 1 AND day <= 365),
    CONSTRAINT chk_schedules_city_length CHECK (LENGTH(city) >= 1 AND LENGTH(city) <= 100),
    CONSTRAINT chk_schedules_temperature CHECK (
        (min_temperature IS NULL AND max_temperature IS NULL) OR 
        (min_temperature IS NOT NULL AND max_temperature IS NOT NULL AND min_temperature <= max_temperature)
    ),
    
    -- 외래키
    CONSTRAINT fk_schedules_trip_id FOREIGN KEY (trip_id) REFERENCES trip_schema.trips(trip_id) ON DELETE CASCADE,
    
    -- 복합 유니크 키 (한 여행의 같은 날짜에 하나의 일정만)
    CONSTRAINT uk_schedules_trip_day UNIQUE (trip_id, day)
);

-- 댓글
COMMENT ON TABLE trip_schema.schedules IS '일별 여행 일정 테이블';
COMMENT ON COLUMN trip_schema.schedules.schedule_id IS '일정 고유 식별자 (UUID)';
COMMENT ON COLUMN trip_schema.schedules.trip_id IS '여행 ID (trips.trip_id 참조)';
COMMENT ON COLUMN trip_schema.schedules.day IS '여행 일차';
COMMENT ON COLUMN trip_schema.schedules.date IS '해당 날짜';
COMMENT ON COLUMN trip_schema.schedules.city IS '방문 도시명';
COMMENT ON COLUMN trip_schema.schedules.weather_condition IS '날씨 상태';
COMMENT ON COLUMN trip_schema.schedules.min_temperature IS '최저 기온 (섭씨)';
COMMENT ON COLUMN trip_schema.schedules.max_temperature IS '최고 기온 (섭씨)';
COMMENT ON COLUMN trip_schema.schedules.weather_icon IS '날씨 아이콘 코드';
```

### 4.6 schedule_places 테이블 생성
```sql
CREATE TABLE trip_schema.schedule_places (
    id                      BIGSERIAL PRIMARY KEY,
    schedule_id            VARCHAR(36) NOT NULL,        -- schedules.schedule_id 참조
    place_id               VARCHAR(100) NOT NULL,       -- Location 서비스의 placeId
    place_name             VARCHAR(255) NOT NULL,
    category               VARCHAR(50) NOT NULL,
    start_time             VARCHAR(10) NOT NULL,        -- HH:mm 형식
    duration               INTEGER NOT NULL,            -- 체류 시간 (분)
    transport_type         VARCHAR(20),                 -- 교통수단
    transport_duration     INTEGER,                     -- 이동 시간 (분)
    transport_distance     DECIMAL(10,3),               -- 이동 거리 (km)
    transport_route        TEXT,                        -- 이동 경로
    rest_points            JSONB,                       -- 휴식지 배열
    accessibility          JSONB,                       -- 접근성 정보 배열
    walking_distance       DECIMAL(8,3),                -- 도보 거리 (km)
    order_seq              INTEGER NOT NULL,            -- 방문 순서
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_schedule_places_start_time CHECK (start_time ~ '^([01]?[0-9]|2[0-3]):[0-5][0-9]$'),
    CONSTRAINT chk_schedule_places_duration CHECK (duration >= 0 AND duration <= 1440), -- 최대 24시간
    CONSTRAINT chk_schedule_places_transport_type CHECK (transport_type IS NULL OR transport_type IN ('WALK', 'CAR', 'PUBLIC')),
    CONSTRAINT chk_schedule_places_transport_duration CHECK (transport_duration IS NULL OR (transport_duration >= 0 AND transport_duration <= 1440)),
    CONSTRAINT chk_schedule_places_transport_distance CHECK (transport_distance IS NULL OR transport_distance >= 0),
    CONSTRAINT chk_schedule_places_walking_distance CHECK (walking_distance IS NULL OR walking_distance >= 0),
    CONSTRAINT chk_schedule_places_category CHECK (category IN ('ALL', 'TOURIST', 'RESTAURANT', 'LAUNDRY')),
    CONSTRAINT chk_schedule_places_place_name_length CHECK (LENGTH(place_name) >= 1 AND LENGTH(place_name) <= 255),
    CONSTRAINT chk_schedule_places_order_seq CHECK (order_seq >= 1),
    
    -- 외래키
    CONSTRAINT fk_schedule_places_schedule_id FOREIGN KEY (schedule_id) REFERENCES trip_schema.schedules(schedule_id) ON DELETE CASCADE,
    
    -- 복합 유니크 키 (한 일정의 같은 순서에 하나의 장소만)
    CONSTRAINT uk_schedule_places_schedule_order UNIQUE (schedule_id, order_seq)
);

-- 댓글
COMMENT ON TABLE trip_schema.schedule_places IS '일정별 방문 장소 테이블';
COMMENT ON COLUMN trip_schema.schedule_places.schedule_id IS '일정 ID (schedules.schedule_id 참조)';
COMMENT ON COLUMN trip_schema.schedule_places.place_id IS 'Location 서비스의 장소 ID';
COMMENT ON COLUMN trip_schema.schedule_places.place_name IS '장소명';
COMMENT ON COLUMN trip_schema.schedule_places.category IS '장소 카테고리';
COMMENT ON COLUMN trip_schema.schedule_places.start_time IS '방문 시작 시간 (HH:mm)';
COMMENT ON COLUMN trip_schema.schedule_places.duration IS '체류 시간 (분)';
COMMENT ON COLUMN trip_schema.schedule_places.transport_type IS '교통수단 (WALK/CAR/PUBLIC)';
COMMENT ON COLUMN trip_schema.schedule_places.transport_duration IS '이동 시간 (분)';
COMMENT ON COLUMN trip_schema.schedule_places.transport_distance IS '이동 거리 (km)';
COMMENT ON COLUMN trip_schema.schedule_places.transport_route IS '이동 경로 정보';
COMMENT ON COLUMN trip_schema.schedule_places.rest_points IS '휴식지 정보 JSON 배열';
COMMENT ON COLUMN trip_schema.schedule_places.accessibility IS '접근성 정보 JSON 배열 (ELEVATOR/RAMP/WHEELCHAIR)';
COMMENT ON COLUMN trip_schema.schedule_places.walking_distance IS '도보 거리 (km)';
COMMENT ON COLUMN trip_schema.schedule_places.order_seq IS '방문 순서';
```

## 5. 인덱스 생성

### 5.1 Unique Index
```sql
-- 비즈니스 키 유니크 인덱스
CREATE UNIQUE INDEX idx_trips_trip_id ON trip_schema.trips(trip_id);
CREATE UNIQUE INDEX idx_members_member_id ON trip_schema.members(member_id);
CREATE UNIQUE INDEX idx_destinations_destination_id ON trip_schema.destinations(destination_id);
CREATE UNIQUE INDEX idx_schedules_schedule_id ON trip_schema.schedules(schedule_id);

-- 복합 유니크 인덱스
CREATE UNIQUE INDEX idx_schedules_trip_day ON trip_schema.schedules(trip_id, day);
CREATE UNIQUE INDEX idx_schedule_places_schedule_order ON trip_schema.schedule_places(schedule_id, order_seq);
```

### 5.2 Foreign Key Index
```sql
-- 외래키 성능 최적화 인덱스
CREATE INDEX idx_members_trip_id ON trip_schema.members(trip_id);
CREATE INDEX idx_destinations_trip_id ON trip_schema.destinations(trip_id);
CREATE INDEX idx_schedules_trip_id ON trip_schema.schedules(trip_id);
CREATE INDEX idx_schedule_places_schedule_id ON trip_schema.schedule_places(schedule_id);
```

### 5.3 Performance Index
```sql
-- trips 성능 인덱스
CREATE INDEX idx_trips_user_id ON trip_schema.trips(user_id);
CREATE INDEX idx_trips_user_status ON trip_schema.trips(user_id, status);
CREATE INDEX idx_trips_status ON trip_schema.trips(status);
CREATE INDEX idx_trips_created_at ON trip_schema.trips(created_at);
CREATE INDEX idx_trips_start_date ON trip_schema.trips(start_date) WHERE start_date IS NOT NULL;

-- members 성능 인덱스
CREATE INDEX idx_members_health_status ON trip_schema.members(health_status);
CREATE INDEX idx_members_age ON trip_schema.members(age);

-- destinations 성능 인덱스
CREATE INDEX idx_destinations_trip_order ON trip_schema.destinations(trip_id, order_seq);
CREATE INDEX idx_destinations_start_date ON trip_schema.destinations(start_date) WHERE start_date IS NOT NULL;

-- schedules 성능 인덱스
CREATE INDEX idx_schedules_date ON trip_schema.schedules(date);
CREATE INDEX idx_schedules_city ON trip_schema.schedules(city);

-- schedule_places 성능 인덱스
CREATE INDEX idx_schedule_places_place_id ON trip_schema.schedule_places(place_id);
CREATE INDEX idx_schedule_places_category ON trip_schema.schedule_places(category);
CREATE INDEX idx_schedule_places_start_time ON trip_schema.schedule_places(start_time);
```

### 5.4 JSON 컬럼 Index
```sql
-- JSON 컬럼 GIN 인덱스
CREATE INDEX idx_members_preferences_gin ON trip_schema.members USING GIN (preferences);
CREATE INDEX idx_schedule_places_rest_points_gin ON trip_schema.schedule_places USING GIN (rest_points);
CREATE INDEX idx_schedule_places_accessibility_gin ON trip_schema.schedule_places USING GIN (accessibility);
```

## 6. 트리거 생성

### 6.1 Updated At 자동 갱신 트리거
```sql
-- 모든 테이블에 updated_at 자동 갱신 트리거 적용
CREATE TRIGGER update_trips_updated_at
    BEFORE UPDATE ON trip_schema.trips
    FOR EACH ROW
    EXECUTE FUNCTION trip_schema.update_updated_at_column();

CREATE TRIGGER update_members_updated_at
    BEFORE UPDATE ON trip_schema.members
    FOR EACH ROW
    EXECUTE FUNCTION trip_schema.update_updated_at_column();

CREATE TRIGGER update_destinations_updated_at
    BEFORE UPDATE ON trip_schema.destinations
    FOR EACH ROW
    EXECUTE FUNCTION trip_schema.update_updated_at_column();

CREATE TRIGGER update_schedules_updated_at
    BEFORE UPDATE ON trip_schema.schedules
    FOR EACH ROW
    EXECUTE FUNCTION trip_schema.update_updated_at_column();

CREATE TRIGGER update_schedule_places_updated_at
    BEFORE UPDATE ON trip_schema.schedule_places
    FOR EACH ROW
    EXECUTE FUNCTION trip_schema.update_updated_at_column();
```

### 6.2 UUID 자동 생성 트리거
```sql
-- UUID 자동 생성 함수들
CREATE OR REPLACE FUNCTION trip_schema.generate_trip_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.trip_id IS NULL THEN
        NEW.trip_id = gen_random_uuid()::VARCHAR(36);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trip_schema.generate_member_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.member_id IS NULL THEN
        NEW.member_id = gen_random_uuid()::VARCHAR(36);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trip_schema.generate_destination_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.destination_id IS NULL THEN
        NEW.destination_id = gen_random_uuid()::VARCHAR(36);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION trip_schema.generate_schedule_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.schedule_id IS NULL THEN
        NEW.schedule_id = gen_random_uuid()::VARCHAR(36);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- UUID 생성 트리거들
CREATE TRIGGER generate_trips_trip_id
    BEFORE INSERT ON trip_schema.trips
    FOR EACH ROW
    EXECUTE FUNCTION trip_schema.generate_trip_id();

CREATE TRIGGER generate_members_member_id
    BEFORE INSERT ON trip_schema.members
    FOR EACH ROW
    EXECUTE FUNCTION trip_schema.generate_member_id();

CREATE TRIGGER generate_destinations_destination_id
    BEFORE INSERT ON trip_schema.destinations
    FOR EACH ROW
    EXECUTE FUNCTION trip_schema.generate_destination_id();

CREATE TRIGGER generate_schedules_schedule_id
    BEFORE INSERT ON trip_schema.schedules
    FOR EACH ROW
    EXECUTE FUNCTION trip_schema.generate_schedule_id();
```

### 6.3 여행 진행률 자동 계산 트리거
```sql
-- 여행 진행률 자동 계산 함수
CREATE OR REPLACE FUNCTION trip_schema.update_trip_progress()
RETURNS TRIGGER AS $$
DECLARE
    v_trip_id VARCHAR(36);
    v_total_steps INTEGER := 5; -- 계획 단계 수
    v_completed_steps INTEGER := 0;
    v_has_members INTEGER;
    v_has_destinations INTEGER;
    v_has_schedules INTEGER;
    v_has_dates INTEGER;
    v_new_progress INTEGER;
BEGIN
    -- 트리거 발생 테이블에 따라 trip_id 추출
    IF TG_TABLE_NAME = 'trips' THEN
        v_trip_id = COALESCE(NEW.trip_id, OLD.trip_id);
    ELSIF TG_TABLE_NAME = 'members' THEN
        v_trip_id = COALESCE(NEW.trip_id, OLD.trip_id);
    ELSIF TG_TABLE_NAME = 'destinations' THEN
        v_trip_id = COALESCE(NEW.trip_id, OLD.trip_id);
    ELSIF TG_TABLE_NAME = 'schedules' THEN
        v_trip_id = COALESCE(NEW.trip_id, OLD.trip_id);
    END IF;
    
    -- 각 단계 완료 여부 확인
    SELECT COUNT(*) INTO v_has_members FROM trip_schema.members WHERE trip_id = v_trip_id;
    SELECT COUNT(*) INTO v_has_destinations FROM trip_schema.destinations WHERE trip_id = v_trip_id;
    SELECT COUNT(*) INTO v_has_schedules FROM trip_schema.schedules WHERE trip_id = v_trip_id;
    SELECT COUNT(*) INTO v_has_dates FROM trip_schema.trips WHERE trip_id = v_trip_id AND start_date IS NOT NULL AND end_date IS NOT NULL;
    
    -- 완료된 단계 수 계산
    v_completed_steps = v_completed_steps + 1; -- 기본 여행 생성
    IF v_has_members > 0 THEN v_completed_steps = v_completed_steps + 1; END IF;
    IF v_has_destinations > 0 THEN v_completed_steps = v_completed_steps + 1; END IF;
    IF v_has_dates > 0 THEN v_completed_steps = v_completed_steps + 1; END IF;
    IF v_has_schedules > 0 THEN v_completed_steps = v_completed_steps + 1; END IF;
    
    -- 진행률 계산 (0-100%)
    v_new_progress = (v_completed_steps * 100) / v_total_steps;
    
    -- 여행 테이블 진행률 업데이트
    UPDATE trip_schema.trips 
    SET progress = v_new_progress,
        updated_at = CURRENT_TIMESTAMP
    WHERE trip_id = v_trip_id;
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- 진행률 업데이트 트리거들
CREATE TRIGGER update_trip_progress_on_members
    AFTER INSERT OR UPDATE OR DELETE ON trip_schema.members
    FOR EACH ROW
    EXECUTE FUNCTION trip_schema.update_trip_progress();

CREATE TRIGGER update_trip_progress_on_destinations
    AFTER INSERT OR UPDATE OR DELETE ON trip_schema.destinations
    FOR EACH ROW
    EXECUTE FUNCTION trip_schema.update_trip_progress();

CREATE TRIGGER update_trip_progress_on_schedules
    AFTER INSERT OR UPDATE OR DELETE ON trip_schema.schedules
    FOR EACH ROW
    EXECUTE FUNCTION trip_schema.update_trip_progress();

CREATE TRIGGER update_trip_progress_on_trips
    AFTER UPDATE ON trip_schema.trips
    FOR EACH ROW
    WHEN (OLD.start_date IS DISTINCT FROM NEW.start_date OR OLD.end_date IS DISTINCT FROM NEW.end_date)
    EXECUTE FUNCTION trip_schema.update_trip_progress();
```

### 6.4 목적지 날짜 자동 계산 트리거
```sql
-- 목적지 날짜 자동 계산 함수
CREATE OR REPLACE FUNCTION trip_schema.calculate_destination_dates()
RETURNS TRIGGER AS $$
DECLARE
    v_trip_start_date DATE;
    v_prev_end_date DATE;
    v_current_start_date DATE;
BEGIN
    -- 여행 시작 날짜 조회
    SELECT start_date INTO v_trip_start_date 
    FROM trip_schema.trips 
    WHERE trip_id = NEW.trip_id;
    
    IF v_trip_start_date IS NULL THEN
        RETURN NEW;
    END IF;
    
    -- 이전 목적지의 종료 날짜 조회
    SELECT COALESCE(MAX(end_date), v_trip_start_date - INTERVAL '1 day')
    INTO v_prev_end_date
    FROM trip_schema.destinations 
    WHERE trip_id = NEW.trip_id 
      AND order_seq < NEW.order_seq;
    
    -- 현재 목적지의 시작 날짜 계산
    v_current_start_date = v_prev_end_date + INTERVAL '1 day';
    
    -- 목적지 날짜 설정
    NEW.start_date = v_current_start_date;
    NEW.end_date = v_current_start_date + (NEW.nights || ' days')::INTERVAL - INTERVAL '1 day';
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER calculate_destinations_dates
    BEFORE INSERT OR UPDATE ON trip_schema.destinations
    FOR EACH ROW
    EXECUTE FUNCTION trip_schema.calculate_destination_dates();
```

## 7. 초기 데이터 삽입

### 7.1 테스트 여행 데이터
```sql
-- 테스트 여행 생성
INSERT INTO trip_schema.trips (trip_name, user_id, transport_mode, status, start_date, end_date) VALUES
('서울 3박 4일 여행', 'user-001', 'PUBLIC', 'PLANNING', '2024-12-15', '2024-12-18'),
('부산 주말 여행', 'user-002', 'CAR', 'ONGOING', '2024-12-20', '2024-12-22'),
('제주도 겨울 여행', 'user-003', 'CAR', 'COMPLETED', '2024-12-10', '2024-12-14');

-- 테스트 멤버 데이터
INSERT INTO trip_schema.members (trip_id, name, age, gender, health_status, preferences) VALUES
((SELECT trip_id FROM trip_schema.trips WHERE trip_name = '서울 3박 4일 여행'), '김서울', 28, 'FEMALE', 'EXCELLENT', '["SIGHTSEEING", "CULTURE", "SHOPPING"]'),
((SELECT trip_id FROM trip_schema.trips WHERE trip_name = '서울 3박 4일 여행'), '이서울', 32, 'MALE', 'GOOD', '["CULTURE", "NATURE"]'),
((SELECT trip_id FROM trip_schema.trips WHERE trip_name = '부산 주말 여행'), '박부산', 25, 'MALE', 'EXCELLENT', '["NATURE", "SPORTS", "REST"]'),
((SELECT trip_id FROM trip_schema.trips WHERE trip_name = '제주도 겨울 여행'), '최제주', 35, 'FEMALE', 'CAUTION', '["NATURE", "REST", "SIGHTSEEING"]');

-- 테스트 목적지 데이터
INSERT INTO trip_schema.destinations (trip_id, destination_name, nights, accommodation, check_in_time, check_out_time, order_seq) VALUES
((SELECT trip_id FROM trip_schema.trips WHERE trip_name = '서울 3박 4일 여행'), '명동', 2, '롯데호텔 서울', '15:00', '11:00', 1),
((SELECT trip_id FROM trip_schema.trips WHERE trip_name = '서울 3박 4일 여행'), '강남', 1, '강남 비즈니스 호텔', '15:00', '12:00', 2),
((SELECT trip_id FROM trip_schema.trips WHERE trip_name = '부산 주말 여행'), '해운대', 1, '해운대 그랜드 호텔', '14:00', '11:00', 1),
((SELECT trip_id FROM trip_schema.trips WHERE trip_name = '제주도 겨울 여행'), '제주시', 2, '제주 오션 스위트', '15:00', '11:00', 1),
((SELECT trip_id FROM trip_schema.trips WHERE trip_name = '제주도 겨울 여행'), '서귀포', 1, '서귀포 리조트', '14:00', '12:00', 2);

-- 테스트 일정 데이터
INSERT INTO trip_schema.schedules (trip_id, day, date, city, weather_condition, min_temperature, max_temperature, weather_icon) VALUES
((SELECT trip_id FROM trip_schema.trips WHERE trip_name = '서울 3박 4일 여행'), 1, '2024-12-15', '서울', 'Sunny', -2.5, 5.8, 'sun'),
((SELECT trip_id FROM trip_schema.trips WHERE trip_name = '서울 3박 4일 여행'), 2, '2024-12-16', '서울', 'Cloudy', -1.0, 7.2, 'cloud'),
((SELECT trip_id FROM trip_schema.trips WHERE trip_name = '부산 주말 여행'), 1, '2024-12-20', '부산', 'Clear', 3.2, 12.5, 'clear'),
((SELECT trip_id FROM trip_schema.trips WHERE trip_name = '제주도 겨울 여행'), 1, '2024-12-10', '제주', 'Rainy', 8.5, 15.2, 'rain');

-- 테스트 장소 데이터
INSERT INTO trip_schema.schedule_places (schedule_id, place_id, place_name, category, start_time, duration, transport_type, transport_duration, transport_distance, order_seq, accessibility) VALUES
((SELECT schedule_id FROM trip_schema.schedules s JOIN trip_schema.trips t ON s.trip_id = t.trip_id WHERE t.trip_name = '서울 3박 4일 여행' AND s.day = 1), 'LOC-001', '경복궁', 'TOURIST', '09:00', 120, 'PUBLIC', 30, 5.2, 1, '["ELEVATOR", "RAMP"]'),
((SELECT schedule_id FROM trip_schema.schedules s JOIN trip_schema.trips t ON s.trip_id = t.trip_id WHERE t.trip_name = '서울 3박 4일 여행' AND s.day = 1), 'LOC-002', '인사동 맛집', 'RESTAURANT', '12:30', 90, 'WALK', 15, 1.2, 2, '["WHEELCHAIR"]'),
((SELECT schedule_id FROM trip_schema.schedules s JOIN trip_schema.trips t ON s.trip_id = t.trip_id WHERE t.trip_name = '부산 주말 여행' AND s.day = 1), 'LOC-003', '해운대 해수욕장', 'TOURIST', '10:00', 180, 'CAR', 20, 8.5, 1, '["RAMP"]');
```

## 8. 권한 설정

### 8.1 Trip 서비스 사용자 권한
```sql
-- 스키마에 대한 모든 권한 부여
GRANT ALL PRIVILEGES ON SCHEMA trip_schema TO trip_service;

-- 테이블에 대한 권한 부여
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA trip_schema TO trip_service;

-- 시퀀스에 대한 권한 부여
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA trip_schema TO trip_service;

-- 함수에 대한 권한 부여
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA trip_schema TO trip_service;

-- 향후 생성될 객체에 대한 기본 권한 설정
ALTER DEFAULT PRIVILEGES IN SCHEMA trip_schema GRANT ALL ON TABLES TO trip_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA trip_schema GRANT ALL ON SEQUENCES TO trip_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA trip_schema GRANT EXECUTE ON FUNCTIONS TO trip_service;
```

## 9. 연결 테스트

### 9.1 연결 확인
```bash
# Trip 서비스 사용자로 데이터베이스 연결 테스트
psql -h localhost -p 5432 -U trip_service -d tripgen_trip -c "SELECT current_database(), current_schema(), current_user;"
```

### 9.2 기본 쿼리 테스트
```sql
-- 테이블 목록 확인
SELECT table_name, table_type FROM information_schema.tables WHERE table_schema = 'trip_schema';

-- 여행 목록 조회
SELECT trip_id, trip_name, status, progress FROM trip_schema.trips;

-- 멤버 수 확인
SELECT t.trip_name, COUNT(m.member_id) as member_count 
FROM trip_schema.trips t 
LEFT JOIN trip_schema.members m ON t.trip_id = m.trip_id 
GROUP BY t.trip_id, t.trip_name;
```

## 10. 백업 설정

### 10.1 백업 스크립트 생성
```bash
#!/bin/bash
# Trip 서비스 데이터베이스 백업 스크립트

DB_NAME="tripgen_trip"
BACKUP_DIR="/backup/trip"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/trip_backup_$DATE.sql"

# 백업 디렉토리 생성
mkdir -p $BACKUP_DIR

# 데이터베이스 백업
pg_dump -h localhost -U trip_service -d $DB_NAME > $BACKUP_FILE

# 백업 파일 압축
gzip $BACKUP_FILE

echo "Backup completed: ${BACKUP_FILE}.gz"
```

### 10.2 백업 복원 스크립트
```bash
#!/bin/bash
# Trip 서비스 데이터베이스 복원 스크립트

if [ -z "$1" ]; then
    echo "Usage: $0 <backup_file>"
    exit 1
fi

BACKUP_FILE=$1
DB_NAME="tripgen_trip"

# 백업 파일 압축 해제 (필요한 경우)
if [[ $BACKUP_FILE == *.gz ]]; then
    gunzip $BACKUP_FILE
    BACKUP_FILE=${BACKUP_FILE%.gz}
fi

# 데이터베이스 복원
psql -h localhost -U trip_service -d $DB_NAME < $BACKUP_FILE

echo "Restore completed from: $BACKUP_FILE"
```

## 11. 모니터링 설정

### 11.1 성능 모니터링 쿼리
```sql
-- 활성 연결 수 확인
SELECT count(*) as active_connections 
FROM pg_stat_activity 
WHERE datname = 'tripgen_trip' AND state = 'active';

-- 테이블 크기 확인
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'trip_schema'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 인덱스 사용률 확인
SELECT 
    schemaname, 
    tablename, 
    indexname, 
    idx_scan, 
    idx_tup_read, 
    idx_tup_fetch
FROM pg_stat_user_indexes 
WHERE schemaname = 'trip_schema'
ORDER BY idx_scan DESC;
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
- [ ] 백업 설정 완료

### 12.2 문제 해결
```sql
-- 권한 문제 시
GRANT ALL PRIVILEGES ON DATABASE tripgen_trip TO trip_service;
GRANT ALL PRIVILEGES ON SCHEMA trip_schema TO trip_service;

-- 연결 문제 시 PostgreSQL 설정 확인
-- postgresql.conf: listen_addresses = '*'
-- pg_hba.conf: host tripgen_trip trip_service 0.0.0.0/0 md5
```

설치가 완료되었습니다. Trip 서비스 데이터베이스가 준비되었으며, 여행 계획 관리를 위한 모든 테이블과 기능이 구성되었습니다.