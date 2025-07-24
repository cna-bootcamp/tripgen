# 장소 서비스(Location Service) 데이터베이스 설계서

## 1. 개요
- **데이터베이스명**: location_db
- **DBMS**: PostgreSQL 14+ (PostGIS 확장 포함)
- **문자셋**: UTF-8
- **타임존**: UTC
- **정규화 수준**: 3NF

## 2. 테이블 설계

### 2.1 places (장소 기본정보)
```sql
CREATE TABLE places (
    id              VARCHAR(50) PRIMARY KEY,        -- 외부 API ID (kakao_12345, google_67890 형식)
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    category        VARCHAR(50) NOT NULL,           -- PlaceCategory enum
    sub_categories  JSONB DEFAULT '[]',             -- ["한식", "고기요리", "삼겹살"] 형식
    location        GEOGRAPHY(POINT, 4326) NOT NULL,-- PostGIS 지리 타입
    latitude        DECIMAL(10, 8) NOT NULL,        -- 위도 (인덱싱용 중복 저장)
    longitude       DECIMAL(11, 8) NOT NULL,        -- 경도 (인덱싱용 중복 저장)
    address         VARCHAR(500) NOT NULL,
    street_address  VARCHAR(300),
    postal_code     VARCHAR(20),
    country         VARCHAR(50) NOT NULL DEFAULT 'KR',
    region          VARCHAR(100),                   -- 시/도
    district        VARCHAR(100),                   -- 구/군
    neighborhood    VARCHAR(100),                   -- 동/읍/면
    rating          DECIMAL(2, 1) CHECK (rating >= 0 AND rating <= 5),
    review_count    INTEGER DEFAULT 0,
    price_level     SMALLINT CHECK (price_level >= 1 AND price_level <= 4),
    tags            JSONB DEFAULT '[]',             -- ["분위기좋은", "데이트", "가족모임"]
    thumbnail_url   VARCHAR(500),
    is_active       BOOLEAN DEFAULT true,
    data_source     VARCHAR(20) NOT NULL,           -- 'kakao', 'google', 'manual'
    external_id     VARCHAR(100),                   -- 원본 API의 ID
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_validated  TIMESTAMP WITH TIME ZONE,
    version         INTEGER DEFAULT 1,
    
    CONSTRAINT uk_external_source UNIQUE (data_source, external_id),
    CONSTRAINT chk_coordinates CHECK (latitude >= -90 AND latitude <= 90 AND longitude >= -180 AND longitude <= 180)
);

-- 인덱스
CREATE INDEX idx_places_location ON places USING GIST (location);
CREATE INDEX idx_places_category ON places (category);
CREATE INDEX idx_places_lat_lng ON places (latitude, longitude);
CREATE INDEX idx_places_region_district ON places (region, district);
CREATE INDEX idx_places_rating ON places (rating DESC) WHERE rating IS NOT NULL;
CREATE INDEX idx_places_updated ON places (updated_at DESC);
CREATE INDEX idx_places_sub_categories ON places USING GIN (sub_categories);
CREATE INDEX idx_places_tags ON places USING GIN (tags);
```

### 2.2 place_details (장소 상세정보)
```sql
CREATE TABLE place_details (
    place_id        VARCHAR(50) PRIMARY KEY REFERENCES places(id) ON DELETE CASCADE,
    phone           VARCHAR(50),
    email           VARCHAR(100),
    social_media    JSONB DEFAULT '{}',             -- {"facebook": "url", "instagram": "url"}
    website         VARCHAR(500),
    reservation_url VARCHAR(500),
    amenities       JSONB DEFAULT '[]',             -- ["주차", "와이파이", "단체석"]
    photos          JSONB DEFAULT '[]',             -- 사진 정보 배열
    popular_times   JSONB DEFAULT '[]',             -- 인기 시간대 정보
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_place_details_amenities ON place_details USING GIN (amenities);
```

### 2.3 business_hours (영업시간)
```sql
CREATE TABLE business_hours (
    id              BIGSERIAL PRIMARY KEY,
    place_id        VARCHAR(50) NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    day_of_week     SMALLINT NOT NULL CHECK (day_of_week >= 0 AND day_of_week <= 6), -- 0=일요일
    open_time       TIME,
    close_time      TIME,
    break_start     TIME,
    break_end       TIME,
    is_24_hours     BOOLEAN DEFAULT false,
    is_closed       BOOLEAN DEFAULT false,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_place_day UNIQUE (place_id, day_of_week),
    CONSTRAINT chk_hours CHECK (
        (is_closed = true) OR 
        (is_24_hours = true) OR 
        (open_time IS NOT NULL AND close_time IS NOT NULL)
    )
);

-- 인덱스
CREATE INDEX idx_business_hours_place ON business_hours (place_id);
```

### 2.4 special_hours (특별 영업시간)
```sql
CREATE TABLE special_hours (
    id              BIGSERIAL PRIMARY KEY,
    place_id        VARCHAR(50) NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    date            DATE NOT NULL,
    open_time       TIME,
    close_time      TIME,
    is_closed       BOOLEAN DEFAULT false,
    reason          VARCHAR(200),                   -- "크리스마스", "설날" 등
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_place_date UNIQUE (place_id, date)
);

-- 인덱스
CREATE INDEX idx_special_hours_place_date ON special_hours (place_id, date);
```

### 2.5 reviews (리뷰)
```sql
CREATE TABLE reviews (
    id              BIGSERIAL PRIMARY KEY,
    place_id        VARCHAR(50) NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    external_id     VARCHAR(100),                   -- 외부 API 리뷰 ID
    author          VARCHAR(100) NOT NULL,
    rating          DECIMAL(2, 1) NOT NULL CHECK (rating >= 0 AND rating <= 5),
    text            TEXT,
    language        VARCHAR(10) DEFAULT 'ko',
    helpful_count   INTEGER DEFAULT 0,
    photos          JSONB DEFAULT '[]',             -- ["url1", "url2"]
    data_source     VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    imported_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_external_review UNIQUE (data_source, external_id)
);

-- 인덱스
CREATE INDEX idx_reviews_place ON reviews (place_id);
CREATE INDEX idx_reviews_rating ON reviews (place_id, rating DESC);
CREATE INDEX idx_reviews_created ON reviews (place_id, created_at DESC);
CREATE INDEX idx_reviews_language ON reviews (place_id, language);
```

### 2.6 region_info (지역 정보)
```sql
CREATE TABLE region_info (
    region_code     VARCHAR(50) PRIMARY KEY,        -- "seoul_gangnam", "jeju_seogwipo"
    region_name     VARCHAR(100) NOT NULL,
    parent_code     VARCHAR(50),
    characteristics JSONB DEFAULT '[]',             -- ["비즈니스중심", "쇼핑", "맛집"]
    popular_categories JSONB DEFAULT '[]',          -- ["카페", "레스토랑", "관광명소"]
    seasonal_highlights JSONB DEFAULT '{}',         -- {"spring": ["벚꽃명소"], "summer": ["해수욕장"]}
    timezone        VARCHAR(50) DEFAULT 'Asia/Seoul',
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_region_info_parent ON region_info (parent_code);
```

### 2.7 place_search_cache (검색 캐시)
```sql
CREATE TABLE place_search_cache (
    cache_key       VARCHAR(500) PRIMARY KEY,       -- 검색 조건 해시
    search_criteria JSONB NOT NULL,                 -- 검색 조건 상세
    result_ids      JSONB NOT NULL,                 -- 결과 place_id 배열
    result_count    INTEGER NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 인덱스
CREATE INDEX idx_search_cache_expires ON place_search_cache (expires_at);
```

### 2.8 translation_cache (번역 캐시)
```sql
CREATE TABLE translation_cache (
    id              BIGSERIAL PRIMARY KEY,
    original_text   TEXT NOT NULL,
    source_language VARCHAR(10) NOT NULL,
    target_language VARCHAR(10) NOT NULL,
    translated_text TEXT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    
    CONSTRAINT uk_translation UNIQUE (original_text, source_language, target_language)
);

-- 인덱스
CREATE INDEX idx_translation_cache_expires ON translation_cache (expires_at);
CREATE INDEX idx_translation_lookup ON translation_cache (source_language, target_language);
```

## 3. 뷰(View) 설계

### 3.1 v_place_summary (장소 요약 뷰)
```sql
CREATE VIEW v_place_summary AS
SELECT 
    p.id,
    p.name,
    p.category,
    p.sub_categories,
    p.latitude,
    p.longitude,
    p.address,
    p.rating,
    p.review_count,
    p.price_level,
    p.thumbnail_url,
    CASE 
        WHEN bh.is_24_hours THEN true
        WHEN bh.is_closed THEN false
        WHEN CURRENT_TIME BETWEEN bh.open_time AND bh.close_time THEN true
        ELSE false
    END as is_open_now
FROM places p
LEFT JOIN business_hours bh ON p.id = bh.place_id 
    AND bh.day_of_week = EXTRACT(DOW FROM CURRENT_DATE);
```

## 4. 함수 설계

### 4.1 거리 기반 검색 함수
```sql
CREATE OR REPLACE FUNCTION search_nearby_places(
    p_latitude DECIMAL,
    p_longitude DECIMAL,
    p_radius_meters INTEGER,
    p_categories VARCHAR[] DEFAULT NULL
) RETURNS TABLE (
    place_id VARCHAR,
    name VARCHAR,
    distance_meters DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        p.id,
        p.name,
        ST_Distance(
            p.location::geography,
            ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::geography
        ) as distance_meters
    FROM places p
    WHERE 
        ST_DWithin(
            p.location::geography,
            ST_SetSRID(ST_MakePoint(p_longitude, p_latitude), 4326)::geography,
            p_radius_meters
        )
        AND (p_categories IS NULL OR p.category = ANY(p_categories))
        AND p.is_active = true
    ORDER BY distance_meters;
END;
$$ LANGUAGE plpgsql;
```

### 4.2 영업시간 확인 함수
```sql
CREATE OR REPLACE FUNCTION is_place_open(
    p_place_id VARCHAR,
    p_datetime TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
) RETURNS BOOLEAN AS $$
DECLARE
    v_is_open BOOLEAN;
    v_day_of_week INTEGER;
    v_check_time TIME;
BEGIN
    v_day_of_week := EXTRACT(DOW FROM p_datetime);
    v_check_time := p_datetime::TIME;
    
    -- 특별 영업시간 확인
    SELECT NOT is_closed INTO v_is_open
    FROM special_hours
    WHERE place_id = p_place_id 
        AND date = p_datetime::DATE;
    
    IF FOUND THEN
        RETURN v_is_open;
    END IF;
    
    -- 정규 영업시간 확인
    SELECT 
        CASE 
            WHEN is_closed THEN false
            WHEN is_24_hours THEN true
            WHEN v_check_time BETWEEN open_time AND close_time THEN true
            ELSE false
        END INTO v_is_open
    FROM business_hours
    WHERE place_id = p_place_id 
        AND day_of_week = v_day_of_week;
    
    RETURN COALESCE(v_is_open, false);
END;
$$ LANGUAGE plpgsql;
```

## 5. 트리거 설계

### 5.1 updated_at 자동 갱신 트리거
```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_places_updated_at BEFORE UPDATE ON places
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    
CREATE TRIGGER update_place_details_updated_at BEFORE UPDATE ON place_details
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    
CREATE TRIGGER update_business_hours_updated_at BEFORE UPDATE ON business_hours
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 5.2 리뷰 통계 갱신 트리거
```sql
CREATE OR REPLACE FUNCTION update_place_review_stats()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        UPDATE places
        SET 
            rating = (SELECT AVG(rating) FROM reviews WHERE place_id = NEW.place_id),
            review_count = (SELECT COUNT(*) FROM reviews WHERE place_id = NEW.place_id)
        WHERE id = NEW.place_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE places
        SET 
            rating = (SELECT AVG(rating) FROM reviews WHERE place_id = OLD.place_id),
            review_count = (SELECT COUNT(*) FROM reviews WHERE place_id = OLD.place_id)
        WHERE id = OLD.place_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_review_stats AFTER INSERT OR UPDATE OR DELETE ON reviews
    FOR EACH ROW EXECUTE FUNCTION update_place_review_stats();
```

## 6. 시퀀스 설계

### 6.1 ID 생성 전략
- **places.id**: 외부 API ID 사용 (kakao_12345 형식)
- **나머지 테이블**: PostgreSQL SERIAL/BIGSERIAL 사용

## 7. 데이터 타입 선택 근거

### 7.1 공간 데이터
- **GEOGRAPHY(POINT, 4326)**: PostGIS의 지리 타입 사용
  - 구면 거리 계산 지원
  - 공간 인덱스 지원
  - WGS84 좌표계 사용

### 7.2 JSON 데이터
- **JSONB**: 바이너리 JSON 사용
  - GIN 인덱스 지원
  - 효율적인 쿼리 성능
  - 유연한 스키마 확장성

### 7.3 시간 데이터
- **TIMESTAMP WITH TIME ZONE**: 타임존 정보 포함
  - 글로벌 서비스 확장 대비
  - 정확한 시간 계산

## 8. 인덱스 설계 전략

### 8.1 공간 인덱스
- GIST 인덱스: 위치 기반 검색 최적화
- 위도/경도 복합 인덱스: 범위 검색 보조

### 8.2 JSON 인덱스
- GIN 인덱스: JSONB 필드 검색 최적화
- 카테고리, 태그, 편의시설 검색 지원

### 8.3 일반 인덱스
- B-tree 인덱스: 일반 검색 및 정렬
- 부분 인덱스: 조건부 검색 최적화

## 9. 제약조건 설계

### 9.1 참조 무결성
- CASCADE DELETE: 장소 삭제시 관련 데이터 자동 삭제
- 외부 API ID 유니크 제약

### 9.2 데이터 무결성
- CHECK 제약: 유효한 값 범위 확인
- NOT NULL: 필수 필드 지정

## 10. 성능 최적화 전략

### 10.1 파티셔닝
- reviews 테이블: 월별 파티셔닝 고려
- place_search_cache: 날짜별 파티셔닝

### 10.2 인덱스 최적화
- 복합 인덱스 활용
- 부분 인덱스로 저장공간 절약

### 10.3 캐싱 전략
- 검색 결과 캐싱
- 번역 결과 캐싱
- 만료 시간 관리