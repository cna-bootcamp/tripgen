# Trip 서비스 데이터베이스 설계서

## 1. 데이터베이스 개요

### 1.1 서비스 개요
- **서비스명**: Trip Service
- **목적**: 여행 계획 관리, 멤버/목적지/일정 관리
- **아키텍처**: Clean Architecture
- **데이터베이스**: PostgreSQL

### 1.2 설계 원칙
- 서비스별 독립적인 데이터베이스 구성
- 서비스 간 직접적인 FK 관계 없음 (비즈니스 키로 연결)
- Clean Architecture의 Domain Entity 기반 설계
- 복잡한 비즈니스 로직 지원을 위한 정규화된 구조

## 2. 테이블 설계

### 2.1 trips 테이블

여행 기본 정보를 관리하는 메인 테이블

```sql
CREATE TABLE trips (
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
COMMENT ON TABLE trips IS '여행 기본 정보 테이블';
COMMENT ON COLUMN trips.trip_id IS '여행 고유 식별자 (UUID)';
COMMENT ON COLUMN trips.user_id IS 'User 서비스의 사용자 ID';
COMMENT ON COLUMN trips.transport_mode IS '교통수단 (PUBLIC/CAR)';
COMMENT ON COLUMN trips.status IS '여행 상태 (PLANNING/ONGOING/COMPLETED)';
COMMENT ON COLUMN trips.current_step IS '현재 진행 단계';
COMMENT ON COLUMN trips.progress IS '진행률 (0-100%)';
```

### 2.2 members 테이블

여행 멤버 정보를 관리하는 테이블

```sql
CREATE TABLE members (
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
    CONSTRAINT fk_members_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE
);

-- 댓글
COMMENT ON TABLE members IS '여행 멤버 정보 테이블';
COMMENT ON COLUMN members.member_id IS '멤버 고유 식별자 (UUID)';
COMMENT ON COLUMN members.trip_id IS '여행 ID (trips.trip_id 참조)';
COMMENT ON COLUMN members.gender IS '성별 (MALE/FEMALE)';
COMMENT ON COLUMN members.health_status IS '건강 상태 (EXCELLENT/GOOD/CAUTION/LIMITED)';
COMMENT ON COLUMN members.preferences IS '선호도 JSON 배열 (SIGHTSEEING/SHOPPING/CULTURE/NATURE/SPORTS/REST)';
```

### 2.3 destinations 테이블

여행 목적지 정보를 관리하는 테이블

```sql
CREATE TABLE destinations (
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
    CONSTRAINT fk_destinations_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE
);

-- 댓글
COMMENT ON TABLE destinations IS '여행 목적지 정보 테이블';
COMMENT ON COLUMN destinations.destination_id IS '목적지 고유 식별자 (UUID)';
COMMENT ON COLUMN destinations.trip_id IS '여행 ID (trips.trip_id 참조)';
COMMENT ON COLUMN destinations.nights IS '숙박 일수';
COMMENT ON COLUMN destinations.accommodation IS '숙박 시설 정보';
COMMENT ON COLUMN destinations.check_in_time IS '체크인 시간 (HH:mm)';
COMMENT ON COLUMN destinations.check_out_time IS '체크아웃 시간 (HH:mm)';
COMMENT ON COLUMN destinations.order_seq IS '방문 순서';
```

### 2.4 schedules 테이블

일별 여행 일정 정보를 관리하는 테이블

```sql
CREATE TABLE schedules (
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
    CONSTRAINT fk_schedules_trip_id FOREIGN KEY (trip_id) REFERENCES trips(trip_id) ON DELETE CASCADE,
    
    -- 복합 유니크 키 (한 여행의 같은 날짜에 하나의 일정만)
    CONSTRAINT uk_schedules_trip_day UNIQUE (trip_id, day)
);

-- 댓글
COMMENT ON TABLE schedules IS '일별 여행 일정 테이블';
COMMENT ON COLUMN schedules.schedule_id IS '일정 고유 식별자 (UUID)';
COMMENT ON COLUMN schedules.trip_id IS '여행 ID (trips.trip_id 참조)';
COMMENT ON COLUMN schedules.day IS '여행 일차';
COMMENT ON COLUMN schedules.date IS '해당 날짜';
COMMENT ON COLUMN schedules.city IS '방문 도시명';
COMMENT ON COLUMN schedules.weather_condition IS '날씨 상태';
COMMENT ON COLUMN schedules.min_temperature IS '최저 기온 (섭씨)';
COMMENT ON COLUMN schedules.max_temperature IS '최고 기온 (섭씨)';
COMMENT ON COLUMN schedules.weather_icon IS '날씨 아이콘 코드';
```

### 2.5 schedule_places 테이블

일정별 방문 장소 정보를 관리하는 테이블

```sql
CREATE TABLE schedule_places (
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
    CONSTRAINT fk_schedule_places_schedule_id FOREIGN KEY (schedule_id) REFERENCES schedules(schedule_id) ON DELETE CASCADE,
    
    -- 복합 유니크 키 (한 일정의 같은 순서에 하나의 장소만)
    CONSTRAINT uk_schedule_places_schedule_order UNIQUE (schedule_id, order_seq)
);

-- 댓글
COMMENT ON TABLE schedule_places IS '일정별 방문 장소 테이블';
COMMENT ON COLUMN schedule_places.schedule_id IS '일정 ID (schedules.schedule_id 참조)';
COMMENT ON COLUMN schedule_places.place_id IS 'Location 서비스의 장소 ID';
COMMENT ON COLUMN schedule_places.place_name IS '장소명';
COMMENT ON COLUMN schedule_places.category IS '장소 카테고리';
COMMENT ON COLUMN schedule_places.start_time IS '방문 시작 시간 (HH:mm)';
COMMENT ON COLUMN schedule_places.duration IS '체류 시간 (분)';
COMMENT ON COLUMN schedule_places.transport_type IS '교통수단 (WALK/CAR/PUBLIC)';
COMMENT ON COLUMN schedule_places.transport_duration IS '이동 시간 (분)';
COMMENT ON COLUMN schedule_places.transport_distance IS '이동 거리 (km)';
COMMENT ON COLUMN schedule_places.transport_route IS '이동 경로 정보';
COMMENT ON COLUMN schedule_places.rest_points IS '휴식지 정보 JSON 배열';
COMMENT ON COLUMN schedule_places.accessibility IS '접근성 정보 JSON 배열 (ELEVATOR/RAMP/WHEELCHAIR)';
COMMENT ON COLUMN schedule_places.walking_distance IS '도보 거리 (km)';
COMMENT ON COLUMN schedule_places.order_seq IS '방문 순서';
```

## 3. 인덱스 설계

### 3.1 Primary Index
```sql
-- 기본 키 인덱스 (자동 생성)
-- trips: PRIMARY KEY (id)
-- members: PRIMARY KEY (id)  
-- destinations: PRIMARY KEY (id)
-- schedules: PRIMARY KEY (id)
-- schedule_places: PRIMARY KEY (id)
```

### 3.2 Unique Index
```sql
-- 비즈니스 키 유니크 인덱스
CREATE UNIQUE INDEX idx_trips_trip_id ON trips(trip_id);
CREATE UNIQUE INDEX idx_members_member_id ON members(member_id);
CREATE UNIQUE INDEX idx_destinations_destination_id ON destinations(destination_id);
CREATE UNIQUE INDEX idx_schedules_schedule_id ON schedules(schedule_id);

-- 복합 유니크 인덱스
CREATE UNIQUE INDEX idx_schedules_trip_day ON schedules(trip_id, day);
CREATE UNIQUE INDEX idx_schedule_places_schedule_order ON schedule_places(schedule_id, order_seq);
```

### 3.3 Foreign Key Index
```sql
-- 외래키 성능 최적화 인덱스
CREATE INDEX idx_members_trip_id ON members(trip_id);
CREATE INDEX idx_destinations_trip_id ON destinations(trip_id);
CREATE INDEX idx_schedules_trip_id ON schedules(trip_id);
CREATE INDEX idx_schedule_places_schedule_id ON schedule_places(schedule_id);
```

### 3.4 Performance Index
```sql
-- trips 성능 인덱스
CREATE INDEX idx_trips_user_id ON trips(user_id);
CREATE INDEX idx_trips_user_status ON trips(user_id, status);
CREATE INDEX idx_trips_status ON trips(status);
CREATE INDEX idx_trips_created_at ON trips(created_at);
CREATE INDEX idx_trips_start_date ON trips(start_date) WHERE start_date IS NOT NULL;

-- members 성능 인덱스
CREATE INDEX idx_members_health_status ON members(health_status);
CREATE INDEX idx_members_age ON members(age);

-- destinations 성능 인덱스
CREATE INDEX idx_destinations_trip_order ON destinations(trip_id, order_seq);
CREATE INDEX idx_destinations_start_date ON destinations(start_date) WHERE start_date IS NOT NULL;

-- schedules 성능 인덱스
CREATE INDEX idx_schedules_date ON schedules(date);
CREATE INDEX idx_schedules_city ON schedules(city);

-- schedule_places 성능 인덱스
CREATE INDEX idx_schedule_places_place_id ON schedule_places(place_id);
CREATE INDEX idx_schedule_places_category ON schedule_places(category);
CREATE INDEX idx_schedule_places_start_time ON schedule_places(start_time);
```

### 3.5 JSON 컬럼 인덱스
```sql
-- JSON 컬럼 GIN 인덱스
CREATE INDEX idx_members_preferences_gin ON members USING GIN (preferences);
CREATE INDEX idx_schedule_places_rest_points_gin ON schedule_places USING GIN (rest_points);
CREATE INDEX idx_schedule_places_accessibility_gin ON schedule_places USING GIN (accessibility);
```

## 4. 트리거 설계

### 4.1 Updated At 자동 갱신
```sql
-- 모든 테이블에 updated_at 자동 갱신 트리거 적용
CREATE TRIGGER update_trips_updated_at
    BEFORE UPDATE ON trips
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_members_updated_at
    BEFORE UPDATE ON members
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_destinations_updated_at
    BEFORE UPDATE ON destinations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_schedules_updated_at
    BEFORE UPDATE ON schedules
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_schedule_places_updated_at
    BEFORE UPDATE ON schedule_places
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### 4.2 UUID 자동 생성
```sql
-- UUID 자동 생성 함수들
CREATE OR REPLACE FUNCTION generate_trip_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.trip_id IS NULL THEN
        NEW.trip_id = gen_random_uuid()::VARCHAR(36);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_member_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.member_id IS NULL THEN
        NEW.member_id = gen_random_uuid()::VARCHAR(36);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_destination_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.destination_id IS NULL THEN
        NEW.destination_id = gen_random_uuid()::VARCHAR(36);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_schedule_id()
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
    BEFORE INSERT ON trips
    FOR EACH ROW
    EXECUTE FUNCTION generate_trip_id();

CREATE TRIGGER generate_members_member_id
    BEFORE INSERT ON members
    FOR EACH ROW
    EXECUTE FUNCTION generate_member_id();

CREATE TRIGGER generate_destinations_destination_id
    BEFORE INSERT ON destinations
    FOR EACH ROW
    EXECUTE FUNCTION generate_destination_id();

CREATE TRIGGER generate_schedules_schedule_id
    BEFORE INSERT ON schedules
    FOR EACH ROW
    EXECUTE FUNCTION generate_schedule_id();
```

### 4.3 여행 진행률 자동 계산
```sql
-- 여행 진행률 자동 계산 함수
CREATE OR REPLACE FUNCTION update_trip_progress()
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
    SELECT COUNT(*) INTO v_has_members FROM members WHERE trip_id = v_trip_id;
    SELECT COUNT(*) INTO v_has_destinations FROM destinations WHERE trip_id = v_trip_id;
    SELECT COUNT(*) INTO v_has_schedules FROM schedules WHERE trip_id = v_trip_id;
    SELECT COUNT(*) INTO v_has_dates FROM trips WHERE trip_id = v_trip_id AND start_date IS NOT NULL AND end_date IS NOT NULL;
    
    -- 완료된 단계 수 계산
    v_completed_steps = v_completed_steps + 1; -- 기본 여행 생성
    IF v_has_members > 0 THEN v_completed_steps = v_completed_steps + 1; END IF;
    IF v_has_destinations > 0 THEN v_completed_steps = v_completed_steps + 1; END IF;
    IF v_has_dates > 0 THEN v_completed_steps = v_completed_steps + 1; END IF;
    IF v_has_schedules > 0 THEN v_completed_steps = v_completed_steps + 1; END IF;
    
    -- 진행률 계산 (0-100%)
    v_new_progress = (v_completed_steps * 100) / v_total_steps;
    
    -- 여행 테이블 진행률 업데이트
    UPDATE trips 
    SET progress = v_new_progress,
        updated_at = CURRENT_TIMESTAMP
    WHERE trip_id = v_trip_id;
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- 진행률 업데이트 트리거들
CREATE TRIGGER update_trip_progress_on_members
    AFTER INSERT OR UPDATE OR DELETE ON members
    FOR EACH ROW
    EXECUTE FUNCTION update_trip_progress();

CREATE TRIGGER update_trip_progress_on_destinations
    AFTER INSERT OR UPDATE OR DELETE ON destinations
    FOR EACH ROW
    EXECUTE FUNCTION update_trip_progress();

CREATE TRIGGER update_trip_progress_on_schedules
    AFTER INSERT OR UPDATE OR DELETE ON schedules
    FOR EACH ROW
    EXECUTE FUNCTION update_trip_progress();

CREATE TRIGGER update_trip_progress_on_trips
    AFTER UPDATE ON trips
    FOR EACH ROW
    WHEN (OLD.start_date IS DISTINCT FROM NEW.start_date OR OLD.end_date IS DISTINCT FROM NEW.end_date)
    EXECUTE FUNCTION update_trip_progress();
```

### 4.4 목적지 날짜 자동 계산
```sql
-- 목적지 날짜 자동 계산 함수
CREATE OR REPLACE FUNCTION calculate_destination_dates()
RETURNS TRIGGER AS $$
DECLARE
    v_trip_start_date DATE;
    v_prev_end_date DATE;
    v_current_start_date DATE;
BEGIN
    -- 여행 시작 날짜 조회
    SELECT start_date INTO v_trip_start_date 
    FROM trips 
    WHERE trip_id = NEW.trip_id;
    
    IF v_trip_start_date IS NULL THEN
        RETURN NEW;
    END IF;
    
    -- 이전 목적지의 종료 날짜 조회
    SELECT COALESCE(MAX(end_date), v_trip_start_date - INTERVAL '1 day')
    INTO v_prev_end_date
    FROM destinations 
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
    BEFORE INSERT OR UPDATE ON destinations
    FOR EACH ROW
    EXECUTE FUNCTION calculate_destination_dates();
```

## 5. 캐시 설계 (Redis)

### 5.1 캐시 전략

#### 여행 목록 캐시
```
키 패턴: trip:list:{user_id}:{status}:{page}
데이터: TripListResponse JSON
TTL: 5분
```

#### 여행 상세 정보 캐시
```
키 패턴: trip:detail:{trip_id}
데이터: TripDetailResponse JSON
TTL: 30분
```

#### 멤버 목록 캐시
```
키 패턴: trip:members:{trip_id}
데이터: List<MemberResponse> JSON
TTL: 30분
```

#### 목적지 목록 캐시
```
키 패턴: trip:destinations:{trip_id}
데이터: List<DestinationResponse> JSON
TTL: 30분
```

#### 일정 캐시
```
키 패턴: trip:schedules:{trip_id}:{day}
데이터: ScheduleResponse JSON
TTL: 1시간
```

#### 전체 일정 캐시
```
키 패턴: trip:schedules:all:{trip_id}
데이터: List<ScheduleResponse> JSON
TTL: 1시간
```

### 5.2 캐시 무효화 정책
- 여행 정보 수정 시: `trip:*:{trip_id}` 패턴 삭제
- 멤버 추가/수정/삭제 시: `trip:members:{trip_id}`, `trip:detail:{trip_id}` 삭제
- 목적지 추가/수정/삭제 시: `trip:destinations:{trip_id}`, `trip:detail:{trip_id}` 삭제
- 일정 생성/수정 시: `trip:schedules:*:{trip_id}` 패턴 삭제

## 6. JSON 스키마 설계

### 6.1 preferences 스키마 (members 테이블)
```json
[
  "SIGHTSEEING",
  "SHOPPING", 
  "CULTURE",
  "NATURE",
  "SPORTS",
  "REST"
]
```

### 6.2 rest_points 스키마 (schedule_places 테이블)
```json
[
  "카페 A",
  "벤치 구역",
  "휴게소"
]
```

### 6.3 accessibility 스키마 (schedule_places 테이블)
```json
[
  "ELEVATOR",
  "RAMP", 
  "WHEELCHAIR"
]
```

## 7. 성능 최적화

### 7.1 커넥션 풀 설정
```yaml
spring.datasource.hikari:
  maximum-pool-size: 25
  minimum-idle: 10
  idle-timeout: 300000
  connection-timeout: 20000
  max-lifetime: 1200000
```

### 7.2 배치 크기 최적화
```yaml
spring.jpa.properties.hibernate:
  jdbc.batch_size: 50
  order_inserts: true
  order_updates: true
  batch_versioned_data: true
```

### 7.3 쿼리 최적화
```sql
-- 사용자별 여행 목록 조회
SELECT t.*, 
       (SELECT COUNT(*) FROM members m WHERE m.trip_id = t.trip_id) as member_count,
       (SELECT COUNT(*) FROM destinations d WHERE d.trip_id = t.trip_id) as destination_count
FROM trips t 
WHERE t.user_id = ? 
  AND t.status = COALESCE(?, t.status)
ORDER BY t.created_at DESC
LIMIT ? OFFSET ?;

-- 여행 상세 정보 조회 (멤버, 목적지 포함)
WITH trip_data AS (
    SELECT * FROM trips WHERE trip_id = ? AND user_id = ?
),
member_data AS (
    SELECT * FROM members WHERE trip_id = ? ORDER BY created_at
),
destination_data AS (
    SELECT * FROM destinations WHERE trip_id = ? ORDER BY order_seq
)
SELECT t.*, 
       json_agg(DISTINCT m.*) as members,
       json_agg(DISTINCT d.*) as destinations,
       EXISTS(SELECT 1 FROM schedules s WHERE s.trip_id = t.trip_id) as has_schedule
FROM trip_data t
LEFT JOIN member_data m ON true
LEFT JOIN destination_data d ON true
GROUP BY t.id, t.trip_id, t.user_id, t.trip_name, t.transport_mode, 
         t.status, t.current_step, t.start_date, t.end_date, 
         t.progress, t.created_at, t.updated_at;
```

## 8. 배치 작업 설계

### 8.1 진행률 일괄 재계산
```sql
-- 모든 여행의 진행률 재계산 (일 1회 실행)
DO $$
DECLARE
    trip_record RECORD;
BEGIN
    FOR trip_record IN SELECT trip_id FROM trips WHERE status = 'PLANNING' LOOP
        PERFORM update_trip_progress_calculation(trip_record.trip_id);
    END LOOP;
END $$;
```

### 8.2 완료된 여행 정리
```sql
-- 완료된 여행 중 오래된 캐시 데이터 정리 (주 1회 실행)
UPDATE trips 
SET status = 'COMPLETED'
WHERE status = 'ONGOING' 
  AND end_date < CURRENT_DATE - INTERVAL '1 day';

-- 완료된 여행의 일정 데이터 압축 저장 (월 1회 실행)
-- 자주 조회되지 않는 완료된 여행의 schedule_places를 JSON으로 압축
```

### 8.3 통계 데이터 수집
```sql
-- 여행 서비스 사용 통계 (일 1회 실행)
SELECT 
    DATE(created_at) as date,
    COUNT(*) as total_trips,
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_trips,
    AVG(progress) as avg_progress,
    COUNT(CASE WHEN transport_mode = 'PUBLIC' THEN 1 END) as public_transport,
    COUNT(CASE WHEN transport_mode = 'CAR' THEN 1 END) as car_transport
FROM trips 
WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY DATE(created_at)
ORDER BY date DESC;
```

## 9. 모니터링 및 알람

### 9.1 모니터링 지표
- 여행 생성/완료율
- 평균 멤버 수, 목적지 수
- 일정 생성 성공/실패율
- 각 단계별 소요 시간

### 9.2 알람 설정
- 여행 생성 실패율 > 1%
- 일정 생성 실패율 > 5%
- 평균 응답시간 > 2초
- 데이터베이스 연결 실패

## 10. 백업 및 복구

### 10.1 백업 전략
- 전체 백업: 일 1회 (새벽 1시)
- 증분 백업: 6시간마다
- JSON 컬럼 압축 백업
- 관계 무결성 검증 포함

### 10.2 복구 우선순위
1. trips (여행 기본 정보)
2. members, destinations (여행 구성 요소)
3. schedules, schedule_places (생성 가능한 데이터)

## 11. 보안 설계

### 11.1 데이터 접근 제어
- Trip 서비스 전용 DB 사용자
- 사용자별 데이터 접근 제한 (user_id 기반)
- 민감 정보 로깅 제외

### 11.2 데이터 무결성
- 서비스 간 참조 무결성은 애플리케이션 레벨에서 관리
- 캐스케이드 삭제를 통한 관련 데이터 일관성 유지
- 트리거를 통한 자동 계산 값 검증

## 12. 데이터 보존 정책

### 12.1 여행 데이터 보존
- 진행 중인 여행: 무기한 보존
- 완료된 여행: 3년 보존
- 삭제된 여행: 논리 삭제 후 1년간 보존, 이후 물리 삭제

### 12.2 로그 데이터 보존
- 여행 활동 로그: 1년 보존
- 일정 생성 로그: 6개월 보존
- 오류 로그: 3개월 보존