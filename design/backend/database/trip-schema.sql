-- ========================================
-- Trip 서비스 데이터베이스 스키마
-- ========================================
-- 서비스: Trip Service
-- 목적: 여행 기본정보, 여행지, 일정 관리
-- 아키텍처: Clean Architecture
-- 데이터베이스: PostgreSQL

-- 필요한 확장 생성
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "btree_gin";

-- trips 테이블 (여행 기본 정보)
CREATE TABLE trips (
    id                  BIGSERIAL PRIMARY KEY,
    trip_id             VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
    user_id             VARCHAR(36) NOT NULL,     -- User 서비스의 userId 참조
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

-- members 테이블 (여행 멤버 정보)
CREATE TABLE members (
    id                  BIGSERIAL PRIMARY KEY,
    member_id           VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
    trip_id             VARCHAR(36) NOT NULL,
    name                VARCHAR(50) NOT NULL,
    age                 INTEGER NOT NULL,
    gender              VARCHAR(10) NOT NULL,
    health_status       VARCHAR(20) NOT NULL,
    activity_preferences JSONB,                   -- 활동 선호도 배열
    dietary_restrictions JSONB,                   -- 식이 제한사항
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

-- schedules 테이블 (일정 정보)
CREATE TABLE schedules (
    id                  BIGSERIAL PRIMARY KEY,
    schedule_id         VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
    trip_id             VARCHAR(36) NOT NULL,
    destination_id      VARCHAR(36) NOT NULL,
    day_number          INTEGER NOT NULL,
    schedule_date       DATE NOT NULL,
    weather_info        JSONB,                    -- 날씨 정보
    daily_theme         VARCHAR(100),             -- 일일 테마
    total_walking_distance INTEGER DEFAULT 0,     -- 총 도보 거리 (미터)
    total_drive_time    INTEGER DEFAULT 0,        -- 총 운전 시간 (분)
    estimated_cost      DECIMAL(10,2),           -- 예상 비용
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

-- schedule_places 테이블 (일정별 장소 정보)
CREATE TABLE schedule_places (
    id                  BIGSERIAL PRIMARY KEY,
    schedule_place_id   VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
    schedule_id         VARCHAR(36) NOT NULL,
    place_id            VARCHAR(36) NOT NULL,     -- Location 서비스의 placeId 참조
    place_name          VARCHAR(200) NOT NULL,
    place_category      VARCHAR(50) NOT NULL,
    start_time          TIME NOT NULL,
    end_time            TIME,
    duration_minutes    INTEGER,
    visit_order         INTEGER NOT NULL DEFAULT 1,
    transportation_to_next VARCHAR(30),           -- 다음 장소로의 이동수단
    travel_time_to_next INTEGER DEFAULT 0,       -- 다음 장소까지 이동시간 (분)
    travel_distance_to_next INTEGER DEFAULT 0,   -- 다음 장소까지 거리 (미터)
    travel_route_info   JSONB,                   -- 이동 경로 상세 정보
    weather_alternative JSONB,                   -- 날씨별 대안 정보
    health_considerations JSONB,                  -- 건강상태 고려사항
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

-- 인덱스 생성
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
    total_steps INTEGER := 4;  -- 총 단계 수
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
    
    -- 진행률 업데이트
    NEW.progress_percentage = current_progress;
    
    -- 상태 자동 변경
    IF NEW.current_step = 'COMPLETED' AND NEW.status = 'PLANNING' THEN
        NEW.status = 'IN_PROGRESS';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 여행 날짜 계산 함수
CREATE OR REPLACE FUNCTION calculate_destination_dates()
RETURNS TRIGGER AS $$
DECLARE
    trip_start_date DATE;
    prev_departure_date DATE;
BEGIN
    -- 여행 시작일 조회
    SELECT start_date INTO trip_start_date
    FROM trips 
    WHERE trip_id = NEW.trip_id;
    
    -- 이전 여행지의 출발일 조회
    SELECT MAX(departure_date) INTO prev_departure_date
    FROM destinations 
    WHERE trip_id = NEW.trip_id 
      AND destination_order < NEW.destination_order;
    
    -- 도착일 계산
    IF prev_departure_date IS NOT NULL THEN
        NEW.arrival_date = prev_departure_date;
    ELSIF trip_start_date IS NOT NULL THEN
        NEW.arrival_date = trip_start_date;
    END IF;
    
    -- 출발일 계산 (숙박일 기준)
    IF NEW.arrival_date IS NOT NULL THEN
        NEW.departure_date = NEW.arrival_date + NEW.nights;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 일정 통계 자동 계산 함수
CREATE OR REPLACE FUNCTION calculate_schedule_statistics()
RETURNS TRIGGER AS $$
DECLARE
    total_walking INTEGER := 0;
    total_driving INTEGER := 0;
    total_cost DECIMAL := 0;
BEGIN
    -- 해당 일정의 통계 재계산
    SELECT 
        COALESCE(SUM(travel_distance_to_next), 0),
        COALESCE(SUM(travel_time_to_next), 0)
    INTO total_walking, total_driving
    FROM schedule_places 
    WHERE schedule_id = COALESCE(NEW.schedule_id, OLD.schedule_id)
      AND transportation_to_next IN ('WALKING');
    
    -- 자동차 이동 시간 계산
    SELECT COALESCE(SUM(travel_time_to_next), 0)
    INTO total_driving
    FROM schedule_places 
    WHERE schedule_id = COALESCE(NEW.schedule_id, OLD.schedule_id)
      AND transportation_to_next IN ('CAR', 'TAXI');
    
    -- 일정 테이블 업데이트
    UPDATE schedules 
    SET 
        total_walking_distance = total_walking,
        total_drive_time = total_driving,
        updated_at = CURRENT_TIMESTAMP
    WHERE schedule_id = COALESCE(NEW.schedule_id, OLD.schedule_id);
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- 여행 상태 자동 전환 함수
CREATE OR REPLACE FUNCTION auto_update_trip_status()
RETURNS TRIGGER AS $$
BEGIN
    -- 시작일이 되면 여행중으로 변경
    IF NEW.start_date = CURRENT_DATE AND NEW.status = 'PLANNING' THEN
        NEW.status = 'IN_PROGRESS';
    END IF;
    
    -- 종료일이 지나면 완료로 변경
    IF NEW.end_date < CURRENT_DATE AND NEW.status = 'IN_PROGRESS' THEN
        NEW.status = 'COMPLETED';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 멤버 순서 자동 조정 함수
CREATE OR REPLACE FUNCTION adjust_member_order()
RETURNS TRIGGER AS $$
BEGIN
    -- 삭제 시 순서 재정렬
    IF TG_OP = 'DELETE' THEN
        UPDATE members 
        SET member_order = member_order - 1
        WHERE trip_id = OLD.trip_id 
          AND member_order > OLD.member_order;
        RETURN OLD;
    END IF;
    
    -- 삽입 시 마지막 순서로 설정
    IF TG_OP = 'INSERT' THEN
        SELECT COALESCE(MAX(member_order), 0) + 1 
        INTO NEW.member_order
        FROM members 
        WHERE trip_id = NEW.trip_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

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

CREATE TRIGGER trg_destinations_dates
    BEFORE INSERT OR UPDATE ON destinations
    FOR EACH ROW
    EXECUTE FUNCTION calculate_destination_dates();

CREATE TRIGGER trg_schedule_places_statistics
    AFTER INSERT OR UPDATE OR DELETE ON schedule_places
    FOR EACH ROW
    EXECUTE FUNCTION calculate_schedule_statistics();

CREATE TRIGGER trg_trips_status_auto
    BEFORE UPDATE ON trips
    FOR EACH ROW
    EXECUTE FUNCTION auto_update_trip_status();

CREATE TRIGGER trg_members_order
    BEFORE INSERT OR DELETE ON members
    FOR EACH ROW
    EXECUTE FUNCTION adjust_member_order();

-- 데이터 정리 함수
CREATE OR REPLACE FUNCTION cleanup_old_trip_data()
RETURNS VOID AS $$
BEGIN
    -- 취소된 여행 중 오래된 것 삭제 (90일 후)
    DELETE FROM trips 
    WHERE status = 'CANCELLED' 
      AND updated_at < CURRENT_TIMESTAMP - INTERVAL '90 days';
    
    -- 완료된 여행 중 일정 데이터가 없는 것 정리 (30일 후)
    DELETE FROM trips 
    WHERE status = 'COMPLETED' 
      AND updated_at < CURRENT_TIMESTAMP - INTERVAL '30 days'
      AND trip_id NOT IN (SELECT DISTINCT trip_id FROM schedules);
      
    -- 고아 데이터 정리
    DELETE FROM members WHERE trip_id NOT IN (SELECT trip_id FROM trips);
    DELETE FROM destinations WHERE trip_id NOT IN (SELECT trip_id FROM trips);
    DELETE FROM schedules WHERE trip_id NOT IN (SELECT trip_id FROM trips);
    DELETE FROM schedule_places WHERE schedule_id NOT IN (SELECT schedule_id FROM schedules);
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