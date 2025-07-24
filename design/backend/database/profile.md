# 프로파일 서비스 데이터베이스 설계서

## 1. 개요
- **서비스명**: 프로파일 서비스 (Profile Service)
- **데이터베이스**: PostgreSQL 14+
- **스키마명**: profile_service
- **문자셋**: UTF-8
- **타임존**: UTC

## 2. 설계 원칙
- 3NF(Third Normal Form) 정규화 적용
- UUID 기반 Primary Key 사용
- Soft Delete 미적용 (물리적 삭제)
- Audit 정보 포함 (생성/수정 시간 및 사용자)
- 낙관적 잠금을 위한 Version 컬럼 (Trip 테이블)
- 복합 인덱스 전략 적용

## 3. 테이블 상세 설계

### 3.1 MEMBER (회원)
```sql
CREATE TABLE member (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    age INTEGER NOT NULL CHECK (age >= 0 AND age <= 150),
    health_status VARCHAR(20) NOT NULL,
    health_note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT chk_health_status CHECK (health_status IN ('GOOD', 'CAUTION', 'LIMITED'))
);

-- 인덱스
CREATE INDEX idx_member_name ON member(name);
CREATE INDEX idx_member_health_status ON member(health_status);
CREATE INDEX idx_member_age_range ON member(age);
```

### 3.2 MEMBER_PREFERENCE (회원 선호도)
```sql
CREATE TABLE member_preference (
    member_id UUID NOT NULL,
    preference_type VARCHAR(20) NOT NULL,
    
    CONSTRAINT pk_member_preference PRIMARY KEY (member_id, preference_type),
    CONSTRAINT fk_member_preference_member FOREIGN KEY (member_id) 
        REFERENCES member(id) ON DELETE CASCADE,
    CONSTRAINT chk_preference_type CHECK (preference_type IN 
        ('CULTURE', 'NATURE', 'ACTIVITY', 'FOOD', 'SHOPPING'))
);

-- 인덱스
CREATE INDEX idx_member_preference_type ON member_preference(preference_type);
```

### 3.3 TRIP (여행)
```sql
CREATE TABLE trip (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_name VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    
    -- Origin Location (Embedded)
    origin_country VARCHAR(100) NOT NULL,
    origin_city VARCHAR(100) NOT NULL,
    origin_address VARCHAR(500),
    origin_latitude DECIMAL(10, 8),
    origin_longitude DECIMAL(11, 8),
    
    -- Destination Location (Embedded)
    destination_country VARCHAR(100) NOT NULL,
    destination_city VARCHAR(100) NOT NULL,
    destination_address VARCHAR(500),
    destination_latitude DECIMAL(10, 8),
    destination_longitude DECIMAL(11, 8),
    
    -- Accommodation (Embedded)
    accommodation_name VARCHAR(200),
    accommodation_address VARCHAR(500),
    accommodation_phone VARCHAR(50),
    accommodation_check_in_time TIME,
    accommodation_check_out_time TIME,
    
    -- Audit & Version
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT chk_date_range CHECK (end_date >= start_date),
    CONSTRAINT chk_origin_coordinates CHECK (
        (origin_latitude IS NULL AND origin_longitude IS NULL) OR 
        (origin_latitude IS NOT NULL AND origin_longitude IS NOT NULL)
    ),
    CONSTRAINT chk_destination_coordinates CHECK (
        (destination_latitude IS NULL AND destination_longitude IS NULL) OR 
        (destination_latitude IS NOT NULL AND destination_longitude IS NOT NULL)
    )
);

-- 인덱스
CREATE INDEX idx_trip_dates ON trip(start_date, end_date);
CREATE INDEX idx_trip_destination_city ON trip(destination_city);
CREATE INDEX idx_trip_name ON trip(trip_name);
CREATE UNIQUE INDEX idx_trip_unique_name_dates ON trip(trip_name, start_date, end_date);
```

### 3.4 TRIP_MEMBER (여행-회원 매핑)
```sql
CREATE TABLE trip_member (
    trip_id UUID NOT NULL,
    member_id UUID NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_trip_member PRIMARY KEY (trip_id, member_id),
    CONSTRAINT fk_trip_member_trip FOREIGN KEY (trip_id) 
        REFERENCES trip(id) ON DELETE CASCADE,
    CONSTRAINT fk_trip_member_member FOREIGN KEY (member_id) 
        REFERENCES member(id) ON DELETE RESTRICT
);

-- 인덱스
CREATE INDEX idx_trip_member_member_id ON trip_member(member_id);
```

### 3.5 TRANSPORT_SETTING (교통수단 설정)
```sql
CREATE TABLE transport_setting (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL,
    date DATE,
    transport_type VARCHAR(20) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    
    CONSTRAINT fk_transport_setting_trip FOREIGN KEY (trip_id) 
        REFERENCES trip(id) ON DELETE CASCADE,
    CONSTRAINT chk_transport_type CHECK (transport_type IN 
        ('PUBLIC_TRANSPORT', 'PRIVATE_CAR', 'WALKING', 'BICYCLE', 'TAXI')),
    CONSTRAINT chk_date_or_default CHECK (
        (date IS NOT NULL AND is_default = FALSE) OR 
        (date IS NULL AND is_default = TRUE)
    )
);

-- 인덱스
CREATE INDEX idx_transport_setting_trip_id ON transport_setting(trip_id);
CREATE INDEX idx_transport_setting_trip_date ON transport_setting(trip_id, date);
CREATE UNIQUE INDEX idx_transport_default_per_trip ON transport_setting(trip_id) 
    WHERE is_default = TRUE;
```

## 4. 시퀀스 및 ID 생성 전략
- **Primary Key**: PostgreSQL의 gen_random_uuid() 함수 사용
- **UUID v4** 형식 사용으로 분산 환경에서의 충돌 방지
- 클라이언트 생성 UUID도 허용 (INSERT 시 ID 지정 가능)

## 5. 데이터 타입 선택 근거

### 5.1 문자열 타입
- **VARCHAR**: 최대 길이가 예측 가능한 필드
  - name, city, country: 일반적인 이름 길이 고려
  - trip_name: 여행 이름의 설명적 특성 고려
  - address: 주소의 다양성 고려
- **TEXT**: 길이 제한이 없는 필드
  - health_note: 건강 관련 상세 정보

### 5.2 숫자 타입
- **INTEGER**: age (0-150 범위 제약)
- **DECIMAL(10,8), DECIMAL(11,8)**: 위도/경도 (고정밀 좌표)
- **BIGINT**: version (낙관적 잠금용)

### 5.3 날짜/시간 타입
- **DATE**: start_date, end_date (시간 불필요)
- **TIME**: check_in_time, check_out_time (날짜 불필요)
- **TIMESTAMP WITH TIME ZONE**: 감사 필드 (시간대 정보 포함)

### 5.4 논리 타입
- **BOOLEAN**: is_default (기본값 설정 여부)

## 6. 제약조건 설계

### 6.1 참조 무결성
- **CASCADE DELETE**: 
  - trip_member: 여행 삭제 시 매핑 정보도 삭제
  - transport_setting: 여행 삭제 시 교통수단 설정도 삭제
  - member_preference: 회원 삭제 시 선호도도 삭제
- **RESTRICT DELETE**:
  - trip_member의 member_id: 여행에 참여 중인 회원은 삭제 불가

### 6.2 비즈니스 규칙
- 여행 날짜: 종료일은 시작일 이후여야 함
- 나이: 0-150 범위
- 좌표: 위도와 경도는 함께 존재하거나 함께 NULL
- 교통수단: 날짜별 설정과 기본 설정은 상호 배타적
- 기본 교통수단: 여행당 하나만 존재

## 7. 인덱스 전략

### 7.1 Primary Key 인덱스
- 모든 테이블의 id 컬럼에 자동 생성

### 7.2 Foreign Key 인덱스
- 모든 외래키에 대해 인덱스 생성 (조인 성능 향상)

### 7.3 비즈니스 인덱스
- **검색 최적화**:
  - member.name: 이름으로 회원 검색
  - member.health_status: 건강상태별 회원 조회
  - trip.destination_city: 목적지별 여행 검색
- **범위 검색**:
  - trip(start_date, end_date): 날짜 범위 검색
  - member.age: 연령대별 검색
- **유니크 제약**:
  - trip(trip_name, start_date, end_date): 동일 기간 중복 여행 방지
  - transport_setting의 기본값: 여행당 하나의 기본 교통수단

### 7.4 복합 인덱스
- trip_member(member_id): 회원별 여행 목록 조회
- transport_setting(trip_id, date): 여행의 특정 날짜 교통수단 조회

## 8. 성능 고려사항

### 8.1 정규화 vs 비정규화
- **정규화 적용**: member_preference, trip_member (다대다 관계)
- **비정규화 적용**: Location, Accommodation (Embedded Value Object)
  - 이유: 조회 성능 향상, 별도 관리 불필요

### 8.2 인덱스 선택성
- 높은 선택성: id, trip_name, member.name
- 중간 선택성: destination_city, age
- 낮은 선택성: health_status, transport_type

### 8.3 쿼리 최적화
- 복합 인덱스를 통한 커버링 인덱스 활용
- 날짜 범위 검색을 위한 B-Tree 인덱스
- 부분 인덱스(Partial Index)로 기본 교통수단 유니크 보장

## 9. 확장성 고려사항

### 9.1 파티셔닝 전략 (향후)
- trip 테이블: start_date 기준 범위 파티셔닝
- transport_setting: trip_id 기준 해시 파티셔닝

### 9.2 아카이빙 전략
- 2년 이상 지난 여행 데이터는 별도 아카이브 테이블로 이동
- 탈퇴한 회원 정보는 개인정보 보호를 위해 익명화 후 보관

## 10. 마이그레이션 스크립트 예시

```sql
-- 데이터베이스 및 스키마 생성
CREATE DATABASE profile_service;
\c profile_service;

-- UUID 확장 기능 활성화
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 스키마 생성
CREATE SCHEMA IF NOT EXISTS profile_service;
SET search_path TO profile_service;

-- 테이블 생성 (위의 DDL 순서대로 실행)

-- 초기 데이터 (선택사항)
INSERT INTO member (name, age, health_status, health_note, created_by, updated_by)
VALUES ('테스트 사용자', 30, 'GOOD', '건강함', 'system', 'system');
```