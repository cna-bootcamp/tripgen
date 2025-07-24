# 일정 서비스(Itinerary Service) 데이터베이스 설계서

## 1. 개요

### 1.1 목적
- 일정 서비스의 독립적인 데이터베이스 설계
- 마이크로서비스 아키텍처 원칙 준수
- 3NF 정규화 수준 적용
- 성능 최적화를 위한 인덱스 전략

### 1.2 데이터베이스 정보
- 데이터베이스명: itinerary_db
- 문자셋: UTF8MB4
- 콜레이션: utf8mb4_unicode_ci
- 타임존: UTC

## 2. 테이블 설계

### 2.1 itineraries (일정 테이블)
```sql
CREATE TABLE itineraries (
    id VARCHAR(36) NOT NULL,
    trip_id VARCHAR(36) NOT NULL,
    date DATE NOT NULL,
    day_number INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    total_distance DECIMAL(10, 2) DEFAULT 0.00,
    total_duration INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    generation_method VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    validation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    generation_job_id VARCHAR(36),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36) NOT NULL,
    updated_by VARCHAR(36) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (generation_job_id) REFERENCES generation_jobs(job_id) ON DELETE SET NULL,
    UNIQUE KEY uk_trip_date (trip_id, date),
    INDEX idx_trip_id (trip_id),
    INDEX idx_date (date),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_generation_method (generation_method),
    INDEX idx_validation_status (validation_status),
    CONSTRAINT chk_status CHECK (status IN ('DRAFT', 'GENERATING', 'GENERATED', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_generation_method CHECK (generation_method IN ('CLAUDE_API', 'MANUAL')),
    CONSTRAINT chk_validation_status CHECK (validation_status IN ('PENDING', 'VALIDATED', 'PARTIALLY_VALIDATED')),
    CONSTRAINT chk_day_number CHECK (day_number > 0),
    CONSTRAINT chk_total_distance CHECK (total_distance >= 0),
    CONSTRAINT chk_total_duration CHECK (total_duration >= 0)
) ENGINE=InnoDB;
```

**테이블 설명:**
- 여행 일정의 일별 정보를 저장하는 핵심 테이블
- generation_method: 일정 생성 방법 (Claude API 또는 수동 생성)
- validation_status: 생성된 일정의 검증 상태
- generation_job_id: 비동기 생성 작업과의 연결

### 2.2 daily_activities (일일 활동 테이블)
```sql
CREATE TABLE daily_activities (
    id VARCHAR(36) NOT NULL,
    itinerary_id VARCHAR(36) NOT NULL,
    place_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    recommend_reason TEXT,
    address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    order_num INT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration INT NOT NULL,
    category VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE,
    UNIQUE KEY uk_itinerary_order (itinerary_id, order_num),
    INDEX idx_itinerary_id (itinerary_id),
    INDEX idx_place_id (place_id),
    INDEX idx_category (category),
    INDEX idx_location (latitude, longitude),
    CONSTRAINT chk_order_num CHECK (order_num > 0),
    CONSTRAINT chk_duration CHECK (duration > 0),
    CONSTRAINT chk_latitude CHECK (latitude >= -90 AND latitude <= 90),
    CONSTRAINT chk_longitude CHECK (longitude >= -180 AND longitude <= 180),
    CONSTRAINT chk_time_order CHECK (end_time > start_time)
) ENGINE=InnoDB;
```

### 2.3 attachments (첨부파일 테이블)
```sql
CREATE TABLE attachments (
    id VARCHAR(36) NOT NULL,
    place_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    -- Photo fields
    file_name VARCHAR(255),
    file_size BIGINT,
    mime_type VARCHAR(100),
    url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    caption VARCHAR(500),
    width INT,
    height INT,
    taken_at TIMESTAMP,
    location_latitude DECIMAL(10, 8),
    location_longitude DECIMAL(11, 8),
    -- Memo fields
    content TEXT,
    content_updated_at TIMESTAMP,
    -- Common fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_place_id (place_id),
    INDEX idx_type (type),
    INDEX idx_created_at (created_at),
    INDEX idx_created_by (created_by),
    CONSTRAINT chk_type CHECK (type IN ('PHOTO', 'MEMO')),
    CONSTRAINT chk_photo_fields CHECK (
        (type = 'PHOTO' AND file_name IS NOT NULL AND url IS NOT NULL) OR
        (type = 'MEMO' AND content IS NOT NULL)
    ),
    CONSTRAINT chk_file_size CHECK (file_size IS NULL OR file_size > 0),
    CONSTRAINT chk_dimensions CHECK (
        (width IS NULL AND height IS NULL) OR
        (width > 0 AND height > 0)
    )
) ENGINE=InnoDB;
```

### 2.4 routes (경로 테이블)
```sql
CREATE TABLE routes (
    id VARCHAR(36) NOT NULL,
    itinerary_id VARCHAR(36) NOT NULL,
    from_place_id VARCHAR(36) NOT NULL,
    to_place_id VARCHAR(36) NOT NULL,
    distance DECIMAL(10, 2) NOT NULL,
    duration INT NOT NULL,
    transport_type VARCHAR(30) NOT NULL,
    polyline TEXT,
    steps JSON,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE CASCADE,
    INDEX idx_itinerary_id (itinerary_id),
    INDEX idx_from_to (from_place_id, to_place_id),
    INDEX idx_transport_type (transport_type),
    CONSTRAINT chk_distance CHECK (distance > 0),
    CONSTRAINT chk_duration CHECK (duration > 0),
    CONSTRAINT chk_transport_type CHECK (transport_type IN ('PUBLIC_TRANSPORT', 'PRIVATE_CAR', 'WALKING', 'BICYCLE', 'TAXI')),
    CONSTRAINT chk_different_places CHECK (from_place_id != to_place_id)
) ENGINE=InnoDB;
```

### 2.5 itinerary_jobs (비동기 작업 테이블)
```sql
CREATE TABLE itinerary_jobs (
    id VARCHAR(36) NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    itinerary_id VARCHAR(36),
    trip_id VARCHAR(36) NOT NULL,
    request_data JSON NOT NULL,
    response_data JSON,
    error_message TEXT,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    priority INT DEFAULT 0,
    scheduled_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (itinerary_id) REFERENCES itineraries(id) ON DELETE SET NULL,
    INDEX idx_status (status),
    INDEX idx_job_type (job_type),
    INDEX idx_trip_id (trip_id),
    INDEX idx_scheduled_at (scheduled_at),
    INDEX idx_priority_scheduled (priority DESC, scheduled_at ASC),
    CONSTRAINT chk_job_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_retry_count CHECK (retry_count >= 0),
    CONSTRAINT chk_max_retries CHECK (max_retries >= 0),
    CONSTRAINT chk_priority CHECK (priority >= 0 AND priority <= 10)
) ENGINE=InnoDB;
```

### 2.6 saga_transactions (사가 트랜잭션 테이블)
```sql
CREATE TABLE saga_transactions (
    id VARCHAR(36) NOT NULL,
    saga_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'STARTED',
    current_step VARCHAR(100),
    step_status VARCHAR(20),
    context JSON NOT NULL,
    compensation_data JSON,
    error_message TEXT,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_saga_type (saga_type),
    INDEX idx_started_at (started_at),
    CONSTRAINT chk_saga_status CHECK (status IN ('STARTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'COMPENSATING', 'COMPENSATED'))
) ENGINE=InnoDB;
```

### 2.7 place_cache (장소 캐시 테이블)
```sql
CREATE TABLE place_cache (
    place_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    rating DECIMAL(2, 1),
    price_level INT,
    phone VARCHAR(50),
    business_hours JSON,
    parking_info JSON,
    congestion_level VARCHAR(20),
    additional_info JSON,
    last_verified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (place_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_category (category),
    INDEX idx_location (latitude, longitude),
    CONSTRAINT chk_rating CHECK (rating IS NULL OR (rating >= 0 AND rating <= 5)),
    CONSTRAINT chk_price_level CHECK (price_level IS NULL OR (price_level >= 1 AND price_level <= 4))
) ENGINE=InnoDB;
```

### 2.8 claude_generation_logs (Claude 생성 로그 테이블)
```sql
CREATE TABLE claude_generation_logs (
    id VARCHAR(36) NOT NULL,
    trip_id VARCHAR(36) NOT NULL,
    request_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    prompt_text TEXT NOT NULL,
    response_text TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_trip_id (trip_id),
    INDEX idx_created_at (created_at),
    INDEX idx_trip_created (trip_id, created_at),
    INDEX idx_status (status),
    CONSTRAINT chk_status CHECK (status IN ('SUCCESS', 'FAILED')),
    CONSTRAINT chk_retry_count CHECK (retry_count >= 0)
) ENGINE=InnoDB;
```

**테이블 설명:**
- Claude API 호출 이력을 저장하는 로그 테이블
- prompt_text: Claude API에 전송한 프롬프트
- response_text: Claude API로부터 받은 응답
- retry_count: 재시도 횟수
- 디버깅 및 감사 목적으로 활용

### 2.9 generation_jobs (생성 작업 테이블)
```sql
CREATE TABLE generation_jobs (
    job_id VARCHAR(36) NOT NULL,
    trip_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    progress INT NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (job_id),
    INDEX idx_trip_id (trip_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_progress CHECK (progress >= 0 AND progress <= 100)
) ENGINE=InnoDB;
```

**테이블 설명:**
- 비동기 일정 생성 작업을 추적하는 테이블
- progress: 작업 진행률 (0-100%)
- 사용자에게 실시간 진행 상황 제공
- 실패 시 error_message에 상세 내용 저장

## 3. 파티셔닝 전략

### 3.1 itineraries 테이블 파티셔닝
```sql
ALTER TABLE itineraries
PARTITION BY RANGE (YEAR(date) * 100 + MONTH(date)) (
    PARTITION p2024_01 VALUES LESS THAN (202402),
    PARTITION p2024_02 VALUES LESS THAN (202403),
    PARTITION p2024_03 VALUES LESS THAN (202404),
    PARTITION p2024_04 VALUES LESS THAN (202405),
    PARTITION p2024_05 VALUES LESS THAN (202406),
    PARTITION p2024_06 VALUES LESS THAN (202407),
    PARTITION p2024_07 VALUES LESS THAN (202408),
    PARTITION p2024_08 VALUES LESS THAN (202409),
    PARTITION p2024_09 VALUES LESS THAN (202410),
    PARTITION p2024_10 VALUES LESS THAN (202411),
    PARTITION p2024_11 VALUES LESS THAN (202412),
    PARTITION p2024_12 VALUES LESS THAN (202501),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### 3.2 itinerary_jobs 테이블 파티셔닝
```sql
ALTER TABLE itinerary_jobs
PARTITION BY RANGE (TO_DAYS(created_at)) (
    PARTITION p_old VALUES LESS THAN (TO_DAYS('2024-01-01')),
    PARTITION p_2024_q1 VALUES LESS THAN (TO_DAYS('2024-04-01')),
    PARTITION p_2024_q2 VALUES LESS THAN (TO_DAYS('2024-07-01')),
    PARTITION p_2024_q3 VALUES LESS THAN (TO_DAYS('2024-10-01')),
    PARTITION p_2024_q4 VALUES LESS THAN (TO_DAYS('2025-01-01')),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

## 4. 인덱스 전략

### 4.1 복합 인덱스
```sql
-- 일정 조회 최적화
CREATE INDEX idx_trip_status_date ON itineraries(trip_id, status, date);

-- 활동 조회 최적화
CREATE INDEX idx_itinerary_order_time ON daily_activities(itinerary_id, order_num, start_time);

-- 첨부파일 조회 최적화
CREATE INDEX idx_place_type_created ON attachments(place_id, type, created_at DESC);

-- 경로 조회 최적화
CREATE INDEX idx_itinerary_from_to ON routes(itinerary_id, from_place_id, to_place_id);

-- Job 처리 최적화
CREATE INDEX idx_status_scheduled_priority ON itinerary_jobs(status, scheduled_at, priority DESC);

-- Claude 로그 조회 최적화
CREATE INDEX idx_trip_created ON claude_generation_logs(trip_id, created_at);
```

### 4.2 커버링 인덱스
```sql
-- 일정 목록 조회용
CREATE INDEX idx_covering_itinerary_list ON itineraries(trip_id, date, status, title, day_number);

-- 활동 목록 조회용
CREATE INDEX idx_covering_activity_list ON daily_activities(itinerary_id, order_num, name, start_time, end_time);
```

## 5. 시퀀스 및 ID 생성 전략

### 5.1 UUID v4 사용
- 모든 테이블의 기본키는 UUID v4 형식 사용
- 분산 시스템에서의 충돌 방지
- 애플리케이션 레벨에서 생성

### 5.2 자동 증가 필드
- order_num: 애플리케이션 레벨에서 관리
- version: 낙관적 잠금을 위한 버전 관리

## 6. 데이터 타입 선택 근거

### 6.1 문자열 타입
- VARCHAR(36): UUID 저장용
- VARCHAR(255): 일반 문자열 (제목, 이름 등)
- VARCHAR(500): 긴 문자열 (주소, URL 등)
- TEXT: 가변 길이 텍스트 (추천 이유, 메모 내용 등)

### 6.2 숫자 타입
- INT: 일반 정수 (기간, 순서 등)
- BIGINT: 큰 정수 (파일 크기, 버전 등)
- DECIMAL(10, 2): 소수점 2자리 실수 (거리)
- DECIMAL(10, 8), DECIMAL(11, 8): 위도/경도

### 6.3 날짜/시간 타입
- DATE: 날짜만 필요한 경우
- TIME: 시간만 필요한 경우
- TIMESTAMP: 날짜와 시간이 모두 필요한 경우

### 6.4 JSON 타입
- 구조화된 데이터 저장 (영업시간, 주차정보 등)
- 유연한 스키마 확장 가능

## 7. 제약조건 설계

### 7.1 기본키 제약
- 모든 테이블에 PRIMARY KEY 설정
- UUID 형식으로 고유성 보장

### 7.2 외래키 제약
- itineraries → daily_activities: CASCADE DELETE
- itineraries → routes: CASCADE DELETE
- 마이크로서비스 간에는 외래키 제약 없음

### 7.3 유니크 제약
- uk_trip_date: 여행별 날짜 중복 방지
- uk_itinerary_order: 일정별 순서 중복 방지

### 7.4 체크 제약
- 상태값 검증 (ENUM 대체)
- 숫자 범위 검증
- 시간 순서 검증

## 8. 성능 최적화 고려사항

### 8.1 인덱스 최적화
- 자주 조회되는 컬럼에 인덱스 생성
- 복합 인덱스는 카디널리티가 높은 컬럼을 앞에 배치
- 커버링 인덱스로 디스크 I/O 최소화

### 8.2 파티셔닝
- 날짜 기반 파티셔닝으로 조회 성능 향상
- 오래된 데이터 아카이빙 용이

### 8.3 캐싱
- place_cache 테이블로 외부 API 호출 최소화
- 만료 시간 관리로 데이터 신선도 유지

### 8.4 쿼리 최적화
- JOIN 최소화 (필요시 애플리케이션 레벨에서 처리)
- 페이징 처리 필수
- 불필요한 컬럼 조회 방지

## 9. 보안 고려사항

### 9.1 접근 제어
- created_by, updated_by로 사용자 추적
- 애플리케이션 레벨에서 권한 검증

### 9.2 데이터 암호화
- 민감한 정보는 애플리케이션 레벨에서 암호화
- 데이터베이스 레벨 암호화는 필요시 적용

### 9.3 감사 로그
- created_at, updated_at 자동 관리
- 중요 변경사항은 별도 감사 테이블 고려

## 10. Claude API 통합 관련 데이터 흐름

### 10.1 일정 생성 프로세스
1. **작업 생성**: generation_jobs 테이블에 새 작업 생성 (status: PENDING)
2. **API 호출**: Claude API 호출 및 claude_generation_logs에 기록
3. **진행 상황 업데이트**: generation_jobs의 progress 필드 업데이트
4. **일정 생성**: itineraries 테이블에 생성된 일정 저장
   - generation_method: 'CLAUDE_API'
   - validation_status: 'PENDING'
   - generation_job_id 연결
5. **완료 처리**: generation_jobs status를 COMPLETED로 변경

### 10.2 실패 처리
- claude_generation_logs에 실패 이력 저장
- retry_count 증가
- generation_jobs의 error_message 업데이트
- 최대 재시도 횟수 초과 시 FAILED 상태로 변경

### 10.3 검증 프로세스
- 생성된 일정의 validation_status 관리
- PENDING: 초기 상태
- VALIDATED: 모든 장소와 경로 검증 완료
- PARTIALLY_VALIDATED: 일부만 검증 완료