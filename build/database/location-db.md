# Location 서비스 데이터베이스 설치 가이드

## 1. 설치 개요

### 1.1 데이터베이스 정보
- **데이터베이스 유형**: PostgreSQL 15+ (PostGIS 포함)
- **데이터베이스명**: tripgen_location
- **스키마명**: location_schema
- **서비스 사용자**: location_service
- **패스워드**: LocationServiceDev2025!
- **포트**: 5432

### 1.2 설치 대상
- 장소 검색 및 상세 정보 관리 데이터베이스
- PostGIS 기반 지리정보 처리
- AI 추천 정보 캐시 관리 테이블
- Layered Architecture 기반 설계

## 2. 사전 준비

### 2.1 PostgreSQL 및 PostGIS 설치 확인
```bash
# PostgreSQL 버전 확인
psql --version

# PostGIS 확장 확인
psql -c "SELECT PostGIS_version();"

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
CREATE DATABASE tripgen_location
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Location 서비스 전용 사용자 생성
CREATE USER location_service WITH
    LOGIN
    NOSUPERUSER
    CREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'LocationServiceDev2025!';

-- 데이터베이스 소유권 변경
ALTER DATABASE tripgen_location OWNER TO location_service;

-- 사용자 권한 부여
GRANT ALL PRIVILEGES ON DATABASE tripgen_location TO location_service;
```

### 3.2 스키마 생성
```sql
-- tripgen_location 데이터베이스에 연결
\c tripgen_location

-- 스키마 생성
CREATE SCHEMA IF NOT EXISTS location_schema AUTHORIZATION location_service;

-- 기본 스키마 설정
ALTER USER location_service SET search_path = location_schema;

-- 확장 기능 활성화
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";
CREATE EXTENSION IF NOT EXISTS "btree_gin";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
```

## 4. 테이블 생성

### 4.1 기본 함수 생성
```sql
-- updated_at 자동 갱신 함수
CREATE OR REPLACE FUNCTION location_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
```

### 4.2 places 테이블 생성
```sql
CREATE TABLE location_schema.places (
    id                  BIGSERIAL PRIMARY KEY,
    place_id            VARCHAR(100) UNIQUE NOT NULL,          -- 외부 API의 장소 ID
    name                VARCHAR(255) NOT NULL,                 -- 장소명
    category            VARCHAR(50) NOT NULL,                  -- 카테고리
    description         TEXT,                                  -- 장소 설명
    rating              DECIMAL(2,1),                          -- 평점 (0.0-5.0)
    review_count        INTEGER DEFAULT 0,                     -- 리뷰 수
    price_level         INTEGER,                               -- 가격 수준 (1-4)
    latitude            DECIMAL(10,8) NOT NULL,                -- 위도
    longitude           DECIMAL(11,8) NOT NULL,                -- 경도
    address             VARCHAR(500),                          -- 주소
    search_keyword      VARCHAR(255),                          -- 검색용 키워드
    parking_keyword     VARCHAR(255),                          -- 주차장 검색 키워드
    region_type         VARCHAR(20) NOT NULL,                  -- 지역 구분
    last_accessed_at    TIMESTAMP WITH TIME ZONE,              -- 마지막 접근 시간
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- PostGIS 공간 인덱스를 위한 지리 정보 컬럼
    location_point      GEOMETRY(POINT, 4326),                 -- 공간 인덱스용
    
    -- 제약조건
    CONSTRAINT chk_places_category CHECK (category IN ('ALL', 'TOURIST', 'RESTAURANT', 'LAUNDRY')),
    CONSTRAINT chk_places_rating CHECK (rating >= 0.0 AND rating <= 5.0),
    CONSTRAINT chk_places_price_level CHECK (price_level >= 1 AND price_level <= 4),
    CONSTRAINT chk_places_region CHECK (region_type IN ('DOMESTIC', 'INTERNATIONAL')),
    CONSTRAINT chk_places_latitude CHECK (latitude >= -90 AND latitude <= 90),
    CONSTRAINT chk_places_longitude CHECK (longitude >= -180 AND longitude <= 180)
);

-- 댓글
COMMENT ON TABLE location_schema.places IS '장소 기본 정보 테이블';
COMMENT ON COLUMN location_schema.places.place_id IS '외부 API (Google/Kakao)의 장소 고유 ID';
COMMENT ON COLUMN location_schema.places.category IS '장소 카테고리 (ALL/TOURIST/RESTAURANT/LAUNDRY)';
COMMENT ON COLUMN location_schema.places.location_point IS 'PostGIS 공간 인덱스용 좌표 정보';
COMMENT ON COLUMN location_schema.places.region_type IS '국내/해외 구분 (DOMESTIC/INTERNATIONAL)';
```

### 4.3 place_categories 테이블 생성
```sql
CREATE TABLE location_schema.place_categories (
    id                  BIGSERIAL PRIMARY KEY,
    category_code       VARCHAR(50) UNIQUE NOT NULL,
    category_name       VARCHAR(100) NOT NULL,
    category_name_en    VARCHAR(100),
    parent_category     VARCHAR(50),
    display_order       INTEGER DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_place_categories_code_length CHECK (LENGTH(category_code) >= 2 AND LENGTH(category_code) <= 50),
    CONSTRAINT chk_place_categories_name_length CHECK (LENGTH(category_name) >= 2 AND LENGTH(category_name) <= 100),
    CONSTRAINT chk_place_categories_display_order CHECK (display_order >= 0)
);

-- 댓글
COMMENT ON TABLE location_schema.place_categories IS '장소 카테고리 마스터 테이블';
COMMENT ON COLUMN location_schema.place_categories.category_code IS '카테고리 코드 (시스템에서 사용)';
COMMENT ON COLUMN location_schema.place_categories.parent_category IS '상위 카테고리 코드';
COMMENT ON COLUMN location_schema.place_categories.display_order IS '화면 표시 순서';
```

### 4.4 place_details 테이블 생성
```sql
CREATE TABLE location_schema.place_details (
    id                  BIGSERIAL PRIMARY KEY,
    place_id            VARCHAR(100) NOT NULL,                 -- places.place_id 참조
    images              JSONB,                                 -- 이미지 URL 배열
    contact_phone       VARCHAR(50),                           -- 연락처
    contact_website     VARCHAR(500),                          -- 웹사이트
    business_hours      JSONB,                                 -- 영업시간 JSON
    reviews             JSONB,                                 -- 리뷰 데이터 (최대 5개)
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 외래키
    CONSTRAINT fk_place_details_place_id FOREIGN KEY (place_id) REFERENCES location_schema.places(place_id) ON DELETE CASCADE
);

-- 댓글
COMMENT ON TABLE location_schema.place_details IS '장소 상세 정보 테이블';
COMMENT ON COLUMN location_schema.place_details.images IS '장소 이미지 URL 배열 JSON';
COMMENT ON COLUMN location_schema.place_details.business_hours IS '영업시간 정보 JSON';
COMMENT ON COLUMN location_schema.place_details.reviews IS '구글 리뷰 데이터 (최대 5개) JSON';
```

### 4.5 place_recommendations 테이블 생성
```sql
CREATE TABLE location_schema.place_recommendations (
    id                      BIGSERIAL PRIMARY KEY,
    place_id                VARCHAR(100) NOT NULL,            -- places.place_id 참조
    trip_id                 VARCHAR(36),                      -- Trip 서비스의 tripId (NULL 가능)
    recommend_reason        TEXT NOT NULL,                    -- AI 추천 이유
    tips_data               JSONB NOT NULL,                   -- AI 팁 정보 JSON
    from_cache              BOOLEAN NOT NULL DEFAULT FALSE,   -- 캐시에서 조회 여부
    generated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_place_recommendations_expiry CHECK (expires_at > created_at),
    
    -- 외래키
    CONSTRAINT fk_place_recommendations_place_id FOREIGN KEY (place_id) REFERENCES location_schema.places(place_id) ON DELETE CASCADE
);

-- 댓글
COMMENT ON TABLE location_schema.place_recommendations IS 'AI 생성 장소 추천 정보 테이블';
COMMENT ON COLUMN location_schema.place_recommendations.trip_id IS 'Trip 서비스의 여행 ID (NULL이면 일반 추천)';
COMMENT ON COLUMN location_schema.place_recommendations.tips_data IS 'AI 생성 팁 정보 JSON (설명, 이벤트, 방문시간 등)';
COMMENT ON COLUMN location_schema.place_recommendations.from_cache IS '캐시된 데이터 사용 여부';
```

### 4.6 recent_searches 테이블 생성
```sql
CREATE TABLE location_schema.recent_searches (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             VARCHAR(36) NOT NULL,                  -- User 서비스의 userId
    search_query        VARCHAR(255) NOT NULL,                 -- 검색어
    search_type         VARCHAR(20) NOT NULL,                  -- 검색 타입
    search_location     VARCHAR(255),                          -- 검색 위치
    search_latitude     DECIMAL(10,8),                         -- 검색 중심 위도
    search_longitude    DECIMAL(11,8),                         -- 검색 중심 경도
    result_count        INTEGER DEFAULT 0,                     -- 검색 결과 수
    searched_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_recent_searches_type CHECK (search_type IN ('KEYWORD', 'NEARBY', 'CATEGORY')),
    CONSTRAINT chk_recent_searches_query_length CHECK (LENGTH(search_query) >= 1 AND LENGTH(search_query) <= 255),
    CONSTRAINT chk_recent_searches_result_count CHECK (result_count >= 0)
);

-- 댓글
COMMENT ON TABLE location_schema.recent_searches IS '사용자 최근 검색 기록 테이블';
COMMENT ON COLUMN location_schema.recent_searches.user_id IS 'User 서비스의 사용자 ID';
COMMENT ON COLUMN location_schema.recent_searches.search_type IS '검색 타입 (KEYWORD/NEARBY/CATEGORY)';
COMMENT ON COLUMN location_schema.recent_searches.result_count IS '해당 검색에서 반환된 결과 수';
```

## 5. 인덱스 생성

### 5.1 Unique Index
```sql
-- 비즈니스 키 유니크 인덱스
CREATE UNIQUE INDEX idx_places_place_id ON location_schema.places(place_id);
CREATE UNIQUE INDEX idx_place_categories_category_code ON location_schema.place_categories(category_code);

-- 장소별 상세정보 유니크 인덱스 (1:1 관계)
CREATE UNIQUE INDEX idx_place_details_place_id ON location_schema.place_details(place_id);
```

### 5.2 공간 인덱스 (PostGIS)
```sql
-- PostGIS 공간 인덱스 생성
CREATE INDEX idx_places_location_gist ON location_schema.places USING GIST (location_point);

-- 좌표 기반 검색을 위한 복합 인덱스
CREATE INDEX idx_places_lat_lng ON location_schema.places(latitude, longitude);
```

### 5.3 Performance Index
```sql
-- places 성능 인덱스
CREATE INDEX idx_places_category ON location_schema.places(category);
CREATE INDEX idx_places_region_category ON location_schema.places(region_type, category);
CREATE INDEX idx_places_rating ON location_schema.places(rating DESC) WHERE rating IS NOT NULL;
CREATE INDEX idx_places_name_search ON location_schema.places USING GIN (to_tsvector('korean', name));
CREATE INDEX idx_places_last_accessed ON location_schema.places(last_accessed_at) WHERE last_accessed_at IS NOT NULL;

-- place_categories 성능 인덱스
CREATE INDEX idx_place_categories_parent ON location_schema.place_categories(parent_category) WHERE parent_category IS NOT NULL;
CREATE INDEX idx_place_categories_active_order ON location_schema.place_categories(is_active, display_order) WHERE is_active = TRUE;

-- place_recommendations 성능 인덱스
CREATE INDEX idx_place_recommendations_place_id ON location_schema.place_recommendations(place_id);
CREATE INDEX idx_place_recommendations_trip_id ON location_schema.place_recommendations(trip_id) WHERE trip_id IS NOT NULL;
CREATE INDEX idx_place_recommendations_expires_at ON location_schema.place_recommendations(expires_at);
CREATE INDEX idx_place_recommendations_generated_at ON location_schema.place_recommendations(generated_at);

-- recent_searches 성능 인덱스
CREATE INDEX idx_recent_searches_user_id ON location_schema.recent_searches(user_id);
CREATE INDEX idx_recent_searches_user_searched ON location_schema.recent_searches(user_id, searched_at DESC);
CREATE INDEX idx_recent_searches_query ON location_schema.recent_searches(search_query);
CREATE INDEX idx_recent_searches_type ON location_schema.recent_searches(search_type);
```

### 5.4 JSON 컬럼 인덱스
```sql
-- place_details JSON 인덱스
CREATE INDEX idx_place_details_images_gin ON location_schema.place_details USING GIN (images);
CREATE INDEX idx_place_details_business_hours_gin ON location_schema.place_details USING GIN (business_hours);

-- place_recommendations JSON 인덱스
CREATE INDEX idx_place_recommendations_tips_gin ON location_schema.place_recommendations USING GIN (tips_data);
```

## 6. 트리거 생성

### 6.1 Updated At 자동 갱신 트리거
```sql
-- 모든 테이블에 updated_at 자동 갱신 트리거 적용
CREATE TRIGGER update_places_updated_at
    BEFORE UPDATE ON location_schema.places
    FOR EACH ROW
    EXECUTE FUNCTION location_schema.update_updated_at_column();

CREATE TRIGGER update_place_categories_updated_at
    BEFORE UPDATE ON location_schema.place_categories
    FOR EACH ROW
    EXECUTE FUNCTION location_schema.update_updated_at_column();

CREATE TRIGGER update_place_details_updated_at
    BEFORE UPDATE ON location_schema.place_details
    FOR EACH ROW
    EXECUTE FUNCTION location_schema.update_updated_at_column();

CREATE TRIGGER update_place_recommendations_updated_at
    BEFORE UPDATE ON location_schema.place_recommendations
    FOR EACH ROW
    EXECUTE FUNCTION location_schema.update_updated_at_column();
```

### 6.2 공간 좌표 자동 갱신
```sql
-- 위도/경도 변경시 PostGIS 포인트 자동 갱신
CREATE OR REPLACE FUNCTION location_schema.update_location_point()
RETURNS TRIGGER AS $$
BEGIN
    NEW.location_point = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_places_location_point
    BEFORE INSERT OR UPDATE OF latitude, longitude ON location_schema.places
    FOR EACH ROW
    EXECUTE FUNCTION location_schema.update_location_point();
```

### 6.3 추천 정보 만료 시간 자동 설정
```sql
-- 추천 정보 만료 시간 자동 설정 (24시간)
CREATE OR REPLACE FUNCTION location_schema.set_recommendation_expiry()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.expires_at IS NULL THEN
        NEW.expires_at = NEW.created_at + INTERVAL '24 hours';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_place_recommendations_expiry
    BEFORE INSERT ON location_schema.place_recommendations
    FOR EACH ROW
    EXECUTE FUNCTION location_schema.set_recommendation_expiry();
```

### 6.4 마지막 접근 시간 갱신
```sql
-- 장소 조회시 마지막 접근 시간 자동 갱신
CREATE OR REPLACE FUNCTION location_schema.update_last_accessed()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE location_schema.places 
    SET last_accessed_at = CURRENT_TIMESTAMP 
    WHERE place_id = NEW.place_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_places_last_accessed
    AFTER INSERT ON location_schema.place_details
    FOR EACH ROW
    EXECUTE FUNCTION location_schema.update_last_accessed();
```

## 7. 초기 데이터 삽입

### 7.1 카테고리 마스터 데이터
```sql
-- 장소 카테고리 초기 데이터
INSERT INTO location_schema.place_categories (category_code, category_name, category_name_en, display_order) VALUES
('ALL', '전체', 'All', 1),
('TOURIST', '관광지', 'Tourist Attraction', 2),
('RESTAURANT', '음식점', 'Restaurant', 3),
('LAUNDRY', '빨래방', 'Laundry', 4),
('ACCOMMODATION', '숙박', 'Accommodation', 5),
('SHOPPING', '쇼핑', 'Shopping', 6),
('ENTERTAINMENT', '엔터테인먼트', 'Entertainment', 7),
('TRANSPORTATION', '교통', 'Transportation', 8),
('SERVICE', '서비스', 'Service', 9),
('OTHER', '기타', 'Other', 10);
```

### 7.2 테스트 장소 데이터
```sql
-- 테스트 장소 생성
INSERT INTO location_schema.places (place_id, name, category, description, rating, review_count, price_level, latitude, longitude, address, region_type) VALUES
('LOC-001', '경복궁', 'TOURIST', '조선왕조의 정궁으로 아름다운 건축물과 역사를 체험할 수 있는 곳', 4.5, 15847, 2, 37.5796, 126.9770, '서울특별시 종로구 사직로 161', 'DOMESTIC'),
('LOC-002', '인사동 맛집', 'RESTAURANT', '전통 한식을 맛볼 수 있는 인사동의 유명 맛집', 4.3, 2847, 3, 37.5735, 126.9854, '서울특별시 종로구 인사동길 62', 'DOMESTIC'),
('LOC-003', '해운대 해수욕장', 'TOURIST', '부산의 대표적인 해수욕장으로 아름다운 바다 경치를 즐길 수 있음', 4.2, 8953, 1, 35.1595, 129.1603, '부산광역시 해운대구 우동', 'DOMESTIC'),
('LOC-004', '뮌헨 중앙역', 'TRANSPORTATION', '독일 뮌헨의 주요 교통 허브', 4.2, 15847, 2, 48.1402, 11.5581, 'Bayerstraße 10A, 80335 München, Germany', 'INTERNATIONAL'),
('LOC-005', '마리엔플라츠', 'TOURIST', '뮌헨의 중심광장으로 유명한 관광 명소', 4.6, 28392, 1, 48.1374, 11.5755, 'Marienplatz, 80331 München, Germany', 'INTERNATIONAL');

-- 테스트 장소 상세 정보
INSERT INTO location_schema.place_details (place_id, images, contact_phone, business_hours, reviews) VALUES
('LOC-001', 
 '["https://example.com/gyeongbok1.jpg", "https://example.com/gyeongbok2.jpg"]',
 '02-3700-3900',
 '{"mon":"09:00-18:00","tue":"09:00-18:00","wed":"09:00-18:00","thu":"09:00-18:00","fri":"09:00-18:00","sat":"09:00-18:00","sun":"09:00-18:00","holiday":"09:00-18:00"}',
 '[{"author":"김관광","rating":5,"text":"역사를 느낄 수 있는 좋은 곳입니다","date":"2024-12-01"}]'),
 
('LOC-002',
 '["https://example.com/insadong1.jpg"]',
 '02-123-4567',
 '{"mon":"11:00-22:00","tue":"11:00-22:00","wed":"11:00-22:00","thu":"11:00-22:00","fri":"11:00-23:00","sat":"11:00-23:00","sun":"11:00-22:00"}',
 '[{"author":"이미식","rating":4,"text":"전통 한식이 맛있어요","date":"2024-12-02"}]'),
 
('LOC-003',
 '["https://example.com/haeundae1.jpg", "https://example.com/haeundae2.jpg", "https://example.com/haeundae3.jpg"]',
 '051-749-4000',
 '{"always_open":true}',
 '[{"author":"박바다","rating":4,"text":"여름에 가기 좋은 해수욕장","date":"2024-12-03"}]');

-- 테스트 추천 정보
INSERT INTO location_schema.place_recommendations (place_id, trip_id, recommend_reason, tips_data, expires_at) VALUES
('LOC-001', 'trip-001', 'AI가 추천하는 서울의 대표 궁궐로 한국 전통 문화를 체험하기에 최적의 장소입니다.', 
 '{"description":"조선 왕조의 정궁","special_events":"수문장 교대식","best_visit_time":"오전 10시","estimated_duration":"2시간","photo_spots":["근정전","경회루"],"practical_tips":["편한 신발 착용","오디오 가이드 대여 추천"],"weather_tips":"우천시 실내 전시관 관람 가능"}',
 CURRENT_TIMESTAMP + INTERVAL '24 hours'),
 
('LOC-002', NULL, '인사동 전통 거리의 대표 맛집으로 정통 한식을 맛볼 수 있습니다.',
 '{"description":"인사동 전통 한식당","special_events":"","best_visit_time":"점심시간","estimated_duration":"1시간","photo_spots":["전통 한상차림"],"practical_tips":["예약 필수","현금 결제 우대"],"weather_tips":"실내 식당으로 날씨 무관"}',
 CURRENT_TIMESTAMP + INTERVAL '24 hours');

-- 테스트 최근 검색 기록
INSERT INTO location_schema.recent_searches (user_id, search_query, search_type, search_location, search_latitude, search_longitude, result_count) VALUES
('user-001', '경복궁', 'KEYWORD', '서울시 종로구', 37.5665, 126.9780, 5),
('user-001', '음식점', 'CATEGORY', '인사동', 37.5735, 126.9854, 12),
('user-002', '주변 관광지', 'NEARBY', '해운대', 35.1595, 129.1603, 8),
('user-003', 'restaurant', 'KEYWORD', 'Munich', 48.1374, 11.5755, 15);
```

## 8. 권한 설정

### 8.1 Location 서비스 사용자 권한
```sql
-- 스키마에 대한 모든 권한 부여
GRANT ALL PRIVILEGES ON SCHEMA location_schema TO location_service;

-- 테이블에 대한 권한 부여
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA location_schema TO location_service;

-- 시퀀스에 대한 권한 부여
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA location_schema TO location_service;

-- 함수에 대한 권한 부여
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA location_schema TO location_service;

-- 향후 생성될 객체에 대한 기본 권한 설정
ALTER DEFAULT PRIVILEGES IN SCHEMA location_schema GRANT ALL ON TABLES TO location_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA location_schema GRANT ALL ON SEQUENCES TO location_service;
ALTER DEFAULT PRIVILEGES IN SCHEMA location_schema GRANT EXECUTE ON FUNCTIONS TO location_service;
```

## 9. 연결 테스트

### 9.1 연결 확인
```bash
# Location 서비스 사용자로 데이터베이스 연결 테스트
psql -h localhost -p 5432 -U location_service -d tripgen_location -c "SELECT current_database(), current_schema(), current_user;"
```

### 9.2 기본 쿼리 테스트
```sql
-- 테이블 목록 확인
SELECT table_name, table_type FROM information_schema.tables WHERE table_schema = 'location_schema';

-- 장소 목록 조회
SELECT place_id, name, category, rating FROM location_schema.places;

-- 카테고리별 장소 수 확인
SELECT c.category_name, COUNT(p.place_id) as place_count
FROM location_schema.place_categories c 
LEFT JOIN location_schema.places p ON c.category_code = p.category 
GROUP BY c.category_code, c.category_name
ORDER BY c.display_order;

-- PostGIS 공간 쿼리 테스트 (서울 중심 1km 반경 검색)
SELECT place_id, name, 
       ST_Distance(location_point, ST_SetSRID(ST_MakePoint(126.9780, 37.5665), 4326)) * 111320 as distance_meters
FROM location_schema.places
WHERE ST_DWithin(location_point, ST_SetSRID(ST_MakePoint(126.9780, 37.5665), 4326), 0.01)
ORDER BY distance_meters;
```

## 10. 백업 설정

### 10.1 백업 스크립트 생성
```bash
#!/bin/bash
# Location 서비스 데이터베이스 백업 스크립트

DB_NAME="tripgen_location"
BACKUP_DIR="/backup/location"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/location_backup_$DATE.sql"

# 백업 디렉토리 생성
mkdir -p $BACKUP_DIR

# 데이터베이스 백업 (PostGIS 포함)
pg_dump -h localhost -U location_service -d $DB_NAME --extension=postgis > $BACKUP_FILE

# 백업 파일 압축
gzip $BACKUP_FILE

echo "Backup completed: ${BACKUP_FILE}.gz"
```

### 10.2 백업 복원 스크립트
```bash
#!/bin/bash
# Location 서비스 데이터베이스 복원 스크립트

if [ -z "$1" ]; then
    echo "Usage: $0 <backup_file>"
    exit 1
fi

BACKUP_FILE=$1
DB_NAME="tripgen_location"

# 백업 파일 압축 해제 (필요한 경우)
if [[ $BACKUP_FILE == *.gz ]]; then
    gunzip $BACKUP_FILE
    BACKUP_FILE=${BACKUP_FILE%.gz}
fi

# 데이터베이스 복원
psql -h localhost -U location_service -d $DB_NAME < $BACKUP_FILE

echo "Restore completed from: $BACKUP_FILE"
```

## 11. 모니터링 설정

### 11.1 성능 모니터링 쿼리
```sql
-- 활성 연결 수 확인
SELECT count(*) as active_connections 
FROM pg_stat_activity 
WHERE datname = 'tripgen_location' AND state = 'active';

-- 테이블 크기 확인
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'location_schema'
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
WHERE schemaname = 'location_schema'
ORDER BY idx_scan DESC;

-- PostGIS 공간 인덱스 확인
SELECT 
    tablename,
    indexname,
    schemaname
FROM pg_indexes 
WHERE schemaname = 'location_schema' 
  AND indexdef LIKE '%GIST%';
```

## 12. 설치 완료 확인

### 12.1 설치 검증 체크리스트
- [ ] 데이터베이스 생성 완료
- [ ] PostGIS 확장 활성화 완료
- [ ] 스키마 생성 완료
- [ ] 모든 테이블 생성 완료
- [ ] 공간 인덱스 생성 완료
- [ ] 트리거 생성 완료
- [ ] 초기 데이터 삽입 완료
- [ ] 권한 설정 완료
- [ ] 연결 테스트 성공
- [ ] PostGIS 공간 쿼리 테스트 성공
- [ ] 백업 설정 완료

### 12.2 문제 해결
```sql
-- 권한 문제 시
GRANT ALL PRIVILEGES ON DATABASE tripgen_location TO location_service;
GRANT ALL PRIVILEGES ON SCHEMA location_schema TO location_service;

-- PostGIS 확장 문제 시
CREATE EXTENSION IF NOT EXISTS postgis;
SELECT PostGIS_version();

-- 연결 문제 시 PostgreSQL 설정 확인
-- postgresql.conf: listen_addresses = '*'
-- pg_hba.conf: host tripgen_location location_service 0.0.0.0/0 md5
```

설치가 완료되었습니다. Location 서비스 데이터베이스가 준비되었으며, PostGIS를 활용한 지리정보 처리와 장소 검색, AI 추천 정보 관리를 위한 모든 테이블과 기능이 구성되었습니다.