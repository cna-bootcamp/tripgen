-- ========================================
-- Location 서비스 데이터베이스 스키마
-- ========================================
-- 서비스: Location Service
-- 목적: 장소 검색, 상세 정보 및 지리정보 관리
-- 아키텍처: Layered Architecture
-- 데이터베이스: PostgreSQL + PostGIS

-- 필요한 확장 생성
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";
CREATE EXTENSION IF NOT EXISTS "btree_gin";

-- places 테이블 (장소 기본 정보)
CREATE TABLE places (
    id                  BIGSERIAL PRIMARY KEY,
    place_id            VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
    external_id         VARCHAR(100),            -- 외부 API ID (Google, Kakao)
    provider            VARCHAR(20) NOT NULL,    -- 데이터 제공자
    name                VARCHAR(200) NOT NULL,
    name_en             VARCHAR(200),            -- 영문명
    category            VARCHAR(50) NOT NULL,
    sub_category        VARCHAR(50),
    address             VARCHAR(500) NOT NULL,
    address_en          VARCHAR(500),           -- 영문 주소
    location            GEOMETRY(POINT, 4326) NOT NULL,  -- WGS84 좌표계
    rating              DECIMAL(2,1),           -- 평점 (0.0-5.0)
    rating_count        INTEGER DEFAULT 0,      -- 리뷰 수
    price_level         INTEGER,                -- 가격 수준 (0-4)
    phone               VARCHAR(30),
    website             VARCHAR(500),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_verified         BOOLEAN NOT NULL DEFAULT FALSE,
    last_updated        TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_places_provider CHECK (provider IN ('GOOGLE', 'KAKAO', 'MANUAL')),
    CONSTRAINT chk_places_category CHECK (category IN ('RESTAURANT', 'TOURIST_ATTRACTION', 'LODGING', 'SHOPPING', 'ENTERTAINMENT', 'TRANSPORTATION', 'SERVICE', 'OTHER')),
    CONSTRAINT chk_places_rating CHECK (rating IS NULL OR (rating >= 0.0 AND rating <= 5.0)),
    CONSTRAINT chk_places_rating_count CHECK (rating_count >= 0),
    CONSTRAINT chk_places_price_level CHECK (price_level IS NULL OR (price_level >= 0 AND price_level <= 4)),
    CONSTRAINT chk_places_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED', 'TEMPORARY_CLOSED')),
    CONSTRAINT chk_places_name_length CHECK (LENGTH(name) >= 1 AND LENGTH(name) <= 200)
);

-- place_details 테이블 (장소 상세 정보)
CREATE TABLE place_details (
    id                  BIGSERIAL PRIMARY KEY,
    place_id            VARCHAR(36) NOT NULL,
    business_hours      JSONB,                  -- 영업시간 정보
    photos              JSONB,                  -- 사진 URL 배열
    reviews             JSONB,                  -- 리뷰 정보 (최대 5개)
    amenities           JSONB,                  -- 편의시설 정보
    accessibility       JSONB,                  -- 접근성 정보
    parking_info        JSONB,                  -- 주차 정보
    weather_impact      JSONB,                  -- 날씨 영향도
    visit_duration      INTEGER,                -- 권장 방문 시간 (분)
    best_visit_time     VARCHAR(50),           -- 최적 방문 시간대
    crowd_level         JSONB,                  -- 시간대별 혼잡도
    special_notes       TEXT,                  -- 특별 안내사항
    last_verified       TIMESTAMP WITH TIME ZONE,
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_place_details_visit_duration CHECK (visit_duration IS NULL OR (visit_duration >= 15 AND visit_duration <= 480)),
    CONSTRAINT chk_place_details_best_visit_time CHECK (best_visit_time IS NULL OR best_visit_time IN ('MORNING', 'AFTERNOON', 'EVENING', 'NIGHT', 'ANYTIME')),
    
    -- 외래키
    CONSTRAINT fk_place_details_place_id FOREIGN KEY (place_id) REFERENCES places(place_id) ON DELETE CASCADE
);

-- place_recommendations 테이블 (AI 추천 정보 캐시)
CREATE TABLE place_recommendations (
    id                      BIGSERIAL PRIMARY KEY,  
    recommendation_id       VARCHAR(36) UNIQUE NOT NULL DEFAULT uuid_generate_v4()::text,
    place_id                VARCHAR(36) NOT NULL,
    user_profile_hash       VARCHAR(64) NOT NULL,   -- 사용자 프로필 해시
    recommendation_reason   TEXT NOT NULL,
    useful_tips            TEXT,
    context_data           JSONB,                   -- 컨텍스트 정보
    generated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '24 hours'),
    access_count           INTEGER NOT NULL DEFAULT 0,
    last_accessed          TIMESTAMP WITH TIME ZONE,
    created_by             VARCHAR(36),
    updated_by             VARCHAR(36),
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_place_recommendations_reason_length CHECK (LENGTH(recommendation_reason) >= 10),
    CONSTRAINT chk_place_recommendations_expires CHECK (expires_at > created_at),
    CONSTRAINT chk_place_recommendations_access_count CHECK (access_count >= 0),
    
    -- 외래키
    CONSTRAINT fk_place_recommendations_place_id FOREIGN KEY (place_id) REFERENCES places(place_id) ON DELETE CASCADE
);

-- 인덱스 생성
-- places 인덱스
CREATE UNIQUE INDEX idx_places_place_id ON places(place_id);
CREATE INDEX idx_places_external_id_provider ON places(external_id, provider) WHERE external_id IS NOT NULL;
CREATE INDEX idx_places_name ON places(name);
CREATE INDEX idx_places_category ON places(category);
CREATE INDEX idx_places_status ON places(status);
CREATE INDEX idx_places_rating ON places(rating) WHERE rating IS NOT NULL;
CREATE INDEX idx_places_is_verified ON places(is_verified);
CREATE INDEX idx_places_created_at ON places(created_at);
CREATE INDEX idx_places_last_updated ON places(last_updated) WHERE last_updated IS NOT NULL;

-- 공간 인덱스 (PostGIS)
CREATE INDEX idx_places_location_gist ON places USING GIST(location);

-- 텍스트 검색 인덱스
CREATE INDEX idx_places_name_trgm ON places USING GIN(name gin_trgm_ops);
CREATE INDEX idx_places_address_trgm ON places USING GIN(address gin_trgm_ops);

-- place_details 인덱스
CREATE UNIQUE INDEX idx_place_details_place_id ON place_details(place_id);
CREATE INDEX idx_place_details_visit_duration ON place_details(visit_duration) WHERE visit_duration IS NOT NULL;
CREATE INDEX idx_place_details_best_visit_time ON place_details(best_visit_time) WHERE best_visit_time IS NOT NULL;
CREATE INDEX idx_place_details_last_verified ON place_details(last_verified) WHERE last_verified IS NOT NULL;
CREATE INDEX idx_place_details_business_hours_gin ON place_details USING GIN(business_hours);
CREATE INDEX idx_place_details_amenities_gin ON place_details USING GIN(amenities);

-- place_recommendations 인덱스
CREATE UNIQUE INDEX idx_place_recommendations_recommendation_id ON place_recommendations(recommendation_id);
CREATE INDEX idx_place_recommendations_place_id ON place_recommendations(place_id);
CREATE INDEX idx_place_recommendations_user_profile_hash ON place_recommendations(user_profile_hash);
CREATE INDEX idx_place_recommendations_expires_at ON place_recommendations(expires_at);
CREATE INDEX idx_place_recommendations_active ON place_recommendations(id) WHERE expires_at > CURRENT_TIMESTAMP;
CREATE INDEX idx_place_recommendations_context_gin ON place_recommendations USING GIN(context_data);

-- 복합 인덱스
CREATE INDEX idx_places_category_status_rating ON places(category, status, rating DESC) WHERE status = 'ACTIVE';
CREATE INDEX idx_place_recommendations_place_user ON place_recommendations(place_id, user_profile_hash);

-- 자동 갱신 트리거 함수들
CREATE OR REPLACE FUNCTION update_places_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_place_details_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_place_recommendations_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 장소 정보 검증 함수
CREATE OR REPLACE FUNCTION validate_place_location()
RETURNS TRIGGER AS $$
BEGIN
    -- 좌표 유효성 검사
    IF ST_X(NEW.location) < -180 OR ST_X(NEW.location) > 180 OR
       ST_Y(NEW.location) < -90 OR ST_Y(NEW.location) > 90 THEN
        RAISE EXCEPTION '유효하지 않은 좌표입니다: lat=%, lng=%', ST_Y(NEW.location), ST_X(NEW.location);
    END IF;
    
    -- 한국 내 좌표인지 확인하여 provider 검증
    IF ST_Contains(ST_GeomFromText('POLYGON((124 33, 132 33, 132 39, 124 39, 124 33))', 4326), NEW.location) THEN
        -- 한국 내 좌표면서 GOOGLE provider인 경우 경고
        IF NEW.provider = 'GOOGLE' THEN
            RAISE NOTICE '한국 내 장소는 KAKAO provider 사용을 권장합니다: %', NEW.name;
        END IF;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 추천 정보 액세스 카운트 업데이트 함수
CREATE OR REPLACE FUNCTION update_recommendation_access()
RETURNS TRIGGER AS $$
BEGIN
    NEW.access_count = NEW.access_count + 1;
    NEW.last_accessed = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 장소 검색을 위한 함수
CREATE OR REPLACE FUNCTION search_places_nearby(
    center_lat DECIMAL,
    center_lng DECIMAL,
    radius_meters INTEGER DEFAULT 1000,
    search_category VARCHAR DEFAULT NULL,
    search_text VARCHAR DEFAULT NULL,
    limit_count INTEGER DEFAULT 20
)
RETURNS TABLE(
    place_id VARCHAR,
    name VARCHAR,
    category VARCHAR,
    address VARCHAR,
    distance_meters INTEGER,
    rating DECIMAL,
    rating_count INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        p.place_id,
        p.name,
        p.category,
        p.address,
        ST_Distance(p.location, ST_SetSRID(ST_MakePoint(center_lng, center_lat), 4326))::INTEGER as distance_meters,
        p.rating,
        p.rating_count
    FROM places p
    WHERE p.status = 'ACTIVE'
      AND ST_DWithin(p.location, ST_SetSRID(ST_MakePoint(center_lng, center_lat), 4326), radius_meters * 0.00001)
      AND (search_category IS NULL OR p.category = search_category)
      AND (search_text IS NULL OR (
          p.name ILIKE '%' || search_text || '%' OR 
          p.address ILIKE '%' || search_text || '%'
      ))
    ORDER BY ST_Distance(p.location, ST_SetSRID(ST_MakePoint(center_lng, center_lat), 4326))
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER trg_places_updated_at
    BEFORE UPDATE ON places
    FOR EACH ROW
    EXECUTE FUNCTION update_places_updated_at();

CREATE TRIGGER trg_place_details_updated_at
    BEFORE UPDATE ON place_details
    FOR EACH ROW
    EXECUTE FUNCTION update_place_details_updated_at();

CREATE TRIGGER trg_place_recommendations_updated_at
    BEFORE UPDATE ON place_recommendations
    FOR EACH ROW
    EXECUTE FUNCTION update_place_recommendations_updated_at();

CREATE TRIGGER trg_places_validate_location
    BEFORE INSERT OR UPDATE ON places
    FOR EACH ROW
    EXECUTE FUNCTION validate_place_location();

-- 만료된 데이터 정리 함수
CREATE OR REPLACE FUNCTION cleanup_expired_location_data()
RETURNS VOID AS $$
BEGIN
    -- 만료된 추천 정보 삭제 (7일 후)
    DELETE FROM place_recommendations 
    WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '7 days';
    
    -- 오래된 장소 정보 중 비활성화된 것 정리 (1년 후)
    UPDATE places 
    SET status = 'INACTIVE' 
    WHERE status = 'ACTIVE' 
      AND last_updated < CURRENT_TIMESTAMP - INTERVAL '1 year'
      AND rating_count = 0;
      
    -- 중복 장소 정리 (같은 좌표, 같은 이름)
    WITH duplicate_places AS (
        SELECT place_id, 
               ROW_NUMBER() OVER (
                   PARTITION BY name, ST_SnapToGrid(location, 0.0001) 
                   ORDER BY rating_count DESC, created_at DESC
               ) as rn
        FROM places 
        WHERE status = 'ACTIVE'
    )
    UPDATE places 
    SET status = 'INACTIVE' 
    WHERE place_id IN (
        SELECT place_id FROM duplicate_places WHERE rn > 1
    );
END;
$$ LANGUAGE plpgsql;

-- 기본 카테고리 데이터 삽입을 위한 함수
CREATE OR REPLACE FUNCTION insert_sample_places()
RETURNS VOID AS $$
BEGIN
    -- 뮌헨 중앙역 (샘플 데이터)
    INSERT INTO places (
        name, name_en, category, address, address_en, location, rating, rating_count, 
        provider, is_verified, created_by, updated_by
    ) VALUES (
        'München Hauptbahnhof', 'Munich Central Station', 'TRANSPORTATION',
        'Bayerstraße 10A, 80335 München, Germany', 'Bayerstraße 10A, 80335 Munich, Germany',
        ST_SetSRID(ST_MakePoint(11.5581, 48.1402), 4326), 4.2, 15847,
        'GOOGLE', TRUE, uuid_generate_v4()::text, uuid_generate_v4()::text
    );
    
    -- 마리엔플라츠 (샘플 데이터)
    INSERT INTO places (
        name, name_en, category, address, address_en, location, rating, rating_count,
        provider, is_verified, created_by, updated_by
    ) VALUES (
        'Marienplatz', 'Marienplatz', 'TOURIST_ATTRACTION',
        'Marienplatz, 80331 München, Germany', 'Marienplatz, 80331 Munich, Germany',
        ST_SetSRID(ST_MakePoint(11.5755, 48.1374), 4326), 4.6, 28392,
        'GOOGLE', TRUE, uuid_generate_v4()::text, uuid_generate_v4()::text
    );
END;
$$ LANGUAGE plpgsql;

-- 샘플 데이터 삽입
SELECT insert_sample_places();

-- 테이블 코멘트
COMMENT ON TABLE places IS '장소 기본 정보 및 지리정보 관리';
COMMENT ON TABLE place_details IS '장소 상세 정보 (영업시간, 리뷰, 편의시설 등)';
COMMENT ON TABLE place_recommendations IS 'AI 추천 정보 캐시 (24시간 TTL)';

-- 컬럼 코멘트
COMMENT ON COLUMN places.place_id IS '외부 서비스 참조용 비즈니스 키';
COMMENT ON COLUMN places.location IS 'PostGIS POINT 타입 (WGS84 좌표계)';
COMMENT ON COLUMN places.provider IS '데이터 제공자 (GOOGLE/KAKAO/MANUAL)';
COMMENT ON COLUMN place_details.business_hours IS '영업시간 정보 (JSONB)';
COMMENT ON COLUMN place_details.crowd_level IS '시간대별 혼잡도 정보 (JSONB)';
COMMENT ON COLUMN place_recommendations.user_profile_hash IS '사용자 프로필 해시 (캐시 키)';

-- 성능 통계 수집
ANALYZE places, place_details, place_recommendations;