# Location 서비스 데이터베이스 설계서

## 1. 데이터베이스 개요

### 1.1 서비스 개요
- **서비스명**: Location Service
- **목적**: 장소 검색, 상세 정보 제공, AI 추천 정보 관리
- **아키텍처**: Layered Architecture
- **데이터베이스**: PostgreSQL (공간 데이터 처리를 위한 PostGIS 확장)

### 1.2 설계 원칙
- 서비스별 독립적인 데이터베이스 구성
- 지리 정보 시스템(GIS) 기능 활용
- 외부 API 데이터 효율적 캐싱
- 대용량 검색 쿼리 최적화

## 2. 테이블 설계

### 2.1 places 테이블

장소 기본 정보를 저장하는 메인 테이블

```sql
CREATE TABLE places (
    id                      BIGSERIAL PRIMARY KEY,
    place_id               VARCHAR(100) UNIQUE NOT NULL,    -- 외부 API의 장소 ID
    name                   VARCHAR(255) NOT NULL,           -- 장소명
    category               VARCHAR(50) NOT NULL,            -- 카테고리
    description            TEXT,                            -- 장소 설명
    rating                 DECIMAL(2,1),                    -- 평점 (0.0-5.0)
    review_count           INTEGER DEFAULT 0,               -- 리뷰 수
    price_level            INTEGER,                         -- 가격 수준 (1-4)
    latitude               DECIMAL(10,8) NOT NULL,          -- 위도
    longitude              DECIMAL(11,8) NOT NULL,          -- 경도
    address                VARCHAR(500),                    -- 주소
    search_keyword         VARCHAR(255),                    -- 검색용 키워드
    parking_keyword        VARCHAR(255),                    -- 주차장 검색 키워드
    region_type            VARCHAR(20) NOT NULL,            -- 지역 구분
    last_accessed_at       TIMESTAMP WITH TIME ZONE,        -- 마지막 접근 시간
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- PostGIS 공간 인덱스를 위한 지리 정보 컬럼
    location_point         GEOMETRY(POINT, 4326),           -- 공간 인덱스용
    
    -- 제약조건
    CONSTRAINT chk_places_category CHECK (category IN ('ALL', 'TOURIST', 'RESTAURANT', 'LAUNDRY')),
    CONSTRAINT chk_places_rating CHECK (rating >= 0.0 AND rating <= 5.0),
    CONSTRAINT chk_places_price_level CHECK (price_level >= 1 AND price_level <= 4),
    CONSTRAINT chk_places_region CHECK (region_type IN ('DOMESTIC', 'INTERNATIONAL')),
    CONSTRAINT chk_places_latitude CHECK (latitude >= -90 AND latitude <= 90),
    CONSTRAINT chk_places_longitude CHECK (longitude >= -180 AND longitude <= 180)
);

-- 댓글
COMMENT ON TABLE places IS '장소 기본 정보 테이블';
COMMENT ON COLUMN places.place_id IS '외부 API (Google/Kakao)의 장소 고유 ID';
COMMENT ON COLUMN places.category IS '장소 카테고리 (ALL/TOURIST/RESTAURANT/LAUNDRY)';
COMMENT ON COLUMN places.location_point IS 'PostGIS 공간 인덱스용 좌표 정보';
COMMENT ON COLUMN places.region_type IS '국내/해외 구분 (DOMESTIC/INTERNATIONAL)';
```

### 2.2 place_details 테이블

장소 상세 정보를 저장하는 테이블

```sql
CREATE TABLE place_details (
    id                  BIGSERIAL PRIMARY KEY,
    place_id           VARCHAR(100) NOT NULL,              -- places.place_id 참조
    images             JSONB,                               -- 이미지 URL 배열
    contact_phone      VARCHAR(50),                         -- 연락처
    contact_website    VARCHAR(500),                        -- 웹사이트
    business_hours     JSONB,                               -- 영업시간 JSON
    reviews            JSONB,                               -- 리뷰 데이터 (최대 5개)
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 외래키
    CONSTRAINT fk_place_details_place_id FOREIGN KEY (place_id) REFERENCES places(place_id) ON DELETE CASCADE
);

-- 댓글
COMMENT ON TABLE place_details IS '장소 상세 정보 테이블';
COMMENT ON COLUMN place_details.images IS '장소 이미지 URL 배열 JSON';
COMMENT ON COLUMN place_details.business_hours IS '영업시간 정보 JSON';
COMMENT ON COLUMN place_details.reviews IS '구글 리뷰 데이터 (최대 5개) JSON';
```

### 2.3 place_recommendations 테이블

AI가 생성한 장소별 추천 정보를 저장하는 테이블

```sql
CREATE TABLE place_recommendations (
    id                      BIGSERIAL PRIMARY KEY,
    place_id               VARCHAR(100) NOT NULL,           -- places.place_id 참조
    trip_id                VARCHAR(36),                     -- Trip 서비스의 tripId (NULL 가능)
    recommend_reason       TEXT NOT NULL,                   -- AI 추천 이유
    tips_data              JSONB NOT NULL,                  -- AI 팁 정보 JSON
    from_cache             BOOLEAN NOT NULL DEFAULT FALSE,  -- 캐시에서 조회 여부
    generated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_place_recommendations_expiry CHECK (expires_at > created_at),
    
    -- 외래키
    CONSTRAINT fk_place_recommendations_place_id FOREIGN KEY (place_id) REFERENCES places(place_id) ON DELETE CASCADE
);

-- 댓글
COMMENT ON TABLE place_recommendations IS 'AI 생성 장소 추천 정보 테이블';
COMMENT ON COLUMN place_recommendations.trip_id IS 'Trip 서비스의 여행 ID (NULL이면 일반 추천)';
COMMENT ON COLUMN place_recommendations.tips_data IS 'AI 생성 팁 정보 JSON (설명, 이벤트, 방문시간 등)';
COMMENT ON COLUMN place_recommendations.from_cache IS '캐시된 데이터 사용 여부';
```

## 3. 인덱스 설계

### 3.1 Primary Index
```sql
-- 기본 키 인덱스 (자동 생성)
-- places: PRIMARY KEY (id)
-- place_details: PRIMARY KEY (id)
-- place_recommendations: PRIMARY KEY (id)
```

### 3.2 Unique Index
```sql
-- 장소 ID 유니크 인덱스
CREATE UNIQUE INDEX idx_places_place_id ON places(place_id);

-- 장소별 상세정보 유니크 인덱스 (1:1 관계)
CREATE UNIQUE INDEX idx_place_details_place_id ON place_details(place_id);
```

### 3.3 공간 인덱스 (PostGIS)
```sql
-- PostGIS 공간 인덱스 생성
CREATE INDEX idx_places_location_gist ON places USING GIST (location_point);

-- 좌표 기반 검색을 위한 복합 인덱스
CREATE INDEX idx_places_lat_lng ON places(latitude, longitude);
```

### 3.4 Performance Index
```sql
-- places 성능 인덱스
CREATE INDEX idx_places_category ON places(category);
CREATE INDEX idx_places_region_category ON places(region_type, category);
CREATE INDEX idx_places_rating ON places(rating DESC) WHERE rating IS NOT NULL;
CREATE INDEX idx_places_name_search ON places USING GIN (to_tsvector('korean', name));
CREATE INDEX idx_places_last_accessed ON places(last_accessed_at) WHERE last_accessed_at IS NOT NULL;

-- place_recommendations 성능 인덱스
CREATE INDEX idx_place_recommendations_place_id ON place_recommendations(place_id);
CREATE INDEX idx_place_recommendations_trip_id ON place_recommendations(trip_id) WHERE trip_id IS NOT NULL;
CREATE INDEX idx_place_recommendations_expires_at ON place_recommendations(expires_at);
CREATE INDEX idx_place_recommendations_generated_at ON place_recommendations(generated_at);
```

### 3.5 JSON 컬럼 인덱스
```sql
-- place_details JSON 인덱스
CREATE INDEX idx_place_details_images_gin ON place_details USING GIN (images);
CREATE INDEX idx_place_details_business_hours_gin ON place_details USING GIN (business_hours);

-- place_recommendations JSON 인덱스
CREATE INDEX idx_place_recommendations_tips_gin ON place_recommendations USING GIN (tips_data);
```

## 4. 트리거 설계

### 4.1 Updated At 자동 갱신
```sql
-- 모든 테이블에 updated_at 자동 갱신 트리거 적용
CREATE TRIGGER update_places_updated_at
    BEFORE UPDATE ON places
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_place_details_updated_at
    BEFORE UPDATE ON place_details
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_place_recommendations_updated_at
    BEFORE UPDATE ON place_recommendations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### 4.2 공간 좌표 자동 갱신
```sql
-- 위도/경도 변경시 PostGIS 포인트 자동 갱신
CREATE OR REPLACE FUNCTION update_location_point()
RETURNS TRIGGER AS $$
BEGIN
    NEW.location_point = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_places_location_point
    BEFORE INSERT OR UPDATE OF latitude, longitude ON places
    FOR EACH ROW
    EXECUTE FUNCTION update_location_point();
```

### 4.3 추천 정보 만료 시간 자동 설정
```sql
-- 추천 정보 만료 시간 자동 설정 (24시간)
CREATE OR REPLACE FUNCTION set_recommendation_expiry()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.expires_at IS NULL THEN
        NEW.expires_at = NEW.created_at + INTERVAL '24 hours';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_place_recommendations_expiry
    BEFORE INSERT ON place_recommendations
    FOR EACH ROW
    EXECUTE FUNCTION set_recommendation_expiry();
```

### 4.4 마지막 접근 시간 갱신
```sql
-- 장소 조회시 마지막 접근 시간 자동 갱신
CREATE OR REPLACE FUNCTION update_last_accessed()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE places 
    SET last_accessed_at = CURRENT_TIMESTAMP 
    WHERE place_id = NEW.place_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_places_last_accessed
    AFTER INSERT ON place_details
    FOR EACH ROW
    EXECUTE FUNCTION update_last_accessed();
```

## 5. 캐시 설계 (Redis)

### 5.1 캐시 전략

#### 장소 상세 정보 캐시
```
키 패턴: location:place:{place_id}
데이터: PlaceDetails JSON
TTL: 1시간
```

#### 주변 장소 검색 결과 캐시
```
키 패턴: location:nearby:{lat}:{lng}:{radius}:{category}
데이터: List<Place> JSON
TTL: 30분
```

#### 키워드 검색 결과 캐시
```
키 패턴: location:search:{keyword}:{location_hash}
데이터: List<Place> JSON
TTL: 30분
```

#### AI 추천 정보 캐시
```
키 패턴: location:recommendation:{place_id}:{trip_id}
데이터: PlaceRecommendation JSON
TTL: 24시간
```

#### 영업시간 캐시
```
키 패턴: location:business_hours:{place_id}
데이터: BusinessHours JSON
TTL: 6시간
```

### 5.2 캐시 무효화 정책
- 장소 정보 업데이트 시: 관련 모든 캐시 삭제
- 일일 배치 작업으로 만료된 캐시 정리
- 외부 API 오류 시 캐시 TTL 연장

## 6. JSON 스키마 설계

### 6.1 business_hours 스키마
```json
{
  "is_open": "boolean",
  "current_status": "string",
  "today_hours": "string",
  "weekly_hours": [
    {
      "day": "string",
      "hours": "string",
      "is_today": "boolean"
    }
  ]
}
```

### 6.2 reviews 스키마 (최대 5개)
```json
[
  {
    "review_id": "string",
    "author_name": "string",
    "rating": "integer",
    "text": "string",
    "time": "number",
    "relative_time_description": "string",
    "language": "string"
  }
]
```

### 6.3 tips_data 스키마
```json
{
  "description": "string",
  "special_events": "string",
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
```

## 7. 외부 API 연동 설계

### 7.1 API 선택 로직
```sql
-- 국내 장소: Kakao Map API 우선
-- 해외 장소: Google Places API 우선
CREATE OR REPLACE FUNCTION select_api_provider(region VARCHAR(20))
RETURNS VARCHAR(20) AS $$
BEGIN
    CASE region
        WHEN 'DOMESTIC' THEN RETURN 'KAKAO';
        WHEN 'INTERNATIONAL' THEN RETURN 'GOOGLE';
        ELSE RETURN 'GOOGLE';
    END CASE;
END;
$$ LANGUAGE plpgsql;
```

### 7.2 API 응답 캐싱
- 외부 API 응답을 원본 그대로 캐싱
- 변환된 내부 데이터도 별도 캐싱
- API 제한 회수 관리를 위한 카운터

## 8. 공간 쿼리 최적화

### 8.1 주변 장소 검색 쿼리
```sql
-- PostGIS를 이용한 반경 검색
SELECT p.*, ST_Distance(
    p.location_point,
    ST_SetSRID(ST_MakePoint(?, ?), 4326)
) * 111320 as distance_meters
FROM places p
WHERE ST_DWithin(
    p.location_point,
    ST_SetSRID(ST_MakePoint(?, ?), 4326),
    ? / 111320.0  -- 미터를 도(degree)로 변환
)
AND category = COALESCE(?, category)
ORDER BY distance_meters
LIMIT ? OFFSET ?;
```

### 8.2 지역별 통계 쿼리
```sql
-- 지역별 장소 분포 통계
SELECT 
    region_type,
    category,
    COUNT(*) as place_count,
    AVG(rating) as avg_rating,
    ST_Centroid(ST_Collect(location_point)) as region_center
FROM places
WHERE created_at >= NOW() - INTERVAL '30 days'
GROUP BY region_type, category;
```

## 9. 성능 최적화

### 9.1 커넥션 풀 설정
```yaml
spring.datasource.hikari:
  maximum-pool-size: 20
  minimum-idle: 5
  idle-timeout: 300000
  connection-timeout: 20000
```

### 9.2 배치 크기 최적화
```yaml
spring.jpa.properties.hibernate:
  jdbc.batch_size: 50
  order_inserts: true
  order_updates: true
```

## 10. 배치 작업 설계

### 10.1 만료 데이터 정리
```sql
-- 만료된 추천 정보 삭제 (매시간 실행)
DELETE FROM place_recommendations 
WHERE expires_at < NOW();

-- 오래된 미사용 장소 정리 (월 1회 실행)
DELETE FROM places 
WHERE last_accessed_at < NOW() - INTERVAL '6 months'
  AND id NOT IN (SELECT DISTINCT place_id FROM place_recommendations);
```

### 10.2 외부 API 데이터 동기화
```sql
-- 인기 장소 정보 주기적 업데이트 (일 1회 실행)
UPDATE places 
SET updated_at = CURRENT_TIMESTAMP
WHERE last_accessed_at > NOW() - INTERVAL '7 days'
  AND updated_at < NOW() - INTERVAL '1 day';
```

## 11. 모니터링 및 알람

### 11.1 모니터링 지표
- 외부 API 호출 성공/실패율
- 캐시 히트율
- 평균 검색 응답 시간
- 장소별 조회 빈도

### 11.2 알람 설정
- 외부 API 오류율 > 5%
- 검색 응답 시간 > 2초
- 캐시 히트율 < 70%
- PostGIS 공간 쿼리 오류

## 12. 백업 및 복구

### 12.1 백업 전략
- 전체 백업: 일 1회 (새벽 2시)
- 공간 인덱스 포함 백업
- JSON 컬럼 압축 백업

### 12.2 복구 우선순위
1. places (장소 기본 정보)
2. place_details (상세 정보)
3. place_recommendations (추천 정보)

## 13. 보안 설계

### 13.1 데이터 접근 제어
- Location 서비스 전용 DB 사용자
- 읽기 전용 복제본 활용
- 외부 API 키 암호화 저장

### 13.2 개인정보 보호
- 리뷰 데이터에서 개인정보 마스킹
- 검색 이력 로그 익명화
- 사용자 위치 정보 암호화