# User 서비스 데이터베이스 설계서

## 1. 데이터베이스 개요

### 1.1 서비스 개요
- **서비스명**: User Service
- **목적**: 사용자 인증, 인가 및 프로필 관리
- **아키텍처**: Layered Architecture
- **데이터베이스**: PostgreSQL

### 1.2 설계 원칙
- 서비스별 독립적인 데이터베이스 구성
- 서비스 간 직접적인 FK 관계 없음
- 보안 중심 설계 (패스워드 암호화, 계정 잠금 등)
- Redis 캐시를 통한 성능 최적화

## 2. 테이블 설계

### 2.1 users 테이블

사용자 기본 정보 및 인증 정보를 관리하는 메인 테이블

```sql
CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    user_id            VARCHAR(36) UNIQUE NOT NULL,  -- UUID
    username           VARCHAR(50) UNIQUE NOT NULL,
    password           VARCHAR(255) NOT NULL,        -- BCrypt 암호화
    name               VARCHAR(100) NOT NULL,
    email              VARCHAR(255) UNIQUE NOT NULL,
    phone              VARCHAR(20),
    avatar_url         VARCHAR(500),
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    login_attempts     INTEGER NOT NULL DEFAULT 0,
    locked_until       TIMESTAMP WITH TIME ZONE,
    last_login_at      TIMESTAMP WITH TIME ZONE,
    created_by         VARCHAR(36),
    updated_by         VARCHAR(36),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- 제약조건
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'LOCKED', 'DELETED')),
    CONSTRAINT chk_users_login_attempts CHECK (login_attempts >= 0),
    CONSTRAINT chk_users_username_length CHECK (LENGTH(username) >= 5),
    CONSTRAINT chk_users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_users_name_length CHECK (LENGTH(name) >= 2)
);

-- 댓글
COMMENT ON TABLE users IS '사용자 기본 정보';
COMMENT ON COLUMN users.user_id IS '서비스 간 연결용 UUID';
COMMENT ON COLUMN users.username IS '로그인용 사용자명 (5자 이상)';
COMMENT ON COLUMN users.password IS 'BCrypt 암호화된 패스워드';
COMMENT ON COLUMN users.status IS '계정 상태 (ACTIVE/INACTIVE/SUSPENDED/LOCKED/DELETED)';
COMMENT ON COLUMN users.login_attempts IS '로그인 시도 횟수 (5회 초과시 계정 잠금)';
COMMENT ON COLUMN users.locked_until IS '계정 잠금 해제 시간';
```

## 3. 인덱스 설계

### 3.1 Primary Index
```sql
-- 기본 키 인덱스 (자동 생성)
-- PRIMARY KEY (id)
```

### 3.2 Unique Index
```sql
-- 사용자 ID 유니크 인덱스
CREATE UNIQUE INDEX idx_users_user_id ON users(user_id);

-- 사용자명 유니크 인덱스
CREATE UNIQUE INDEX idx_users_username ON users(username);

-- 이메일 유니크 인덱스
CREATE UNIQUE INDEX idx_users_email ON users(email);
```

### 3.3 Performance Index
```sql
-- 로그인 성능 최적화
CREATE INDEX idx_users_username_status ON users(username, status) WHERE status = 'ACTIVE';

-- 계정 잠금 관리
CREATE INDEX idx_users_locked_until ON users(locked_until) WHERE locked_until IS NOT NULL;

-- 마지막 로그인 시간 조회
CREATE INDEX idx_users_last_login_at ON users(last_login_at) WHERE last_login_at IS NOT NULL;

-- 생성일시 조회 최적화
CREATE INDEX idx_users_created_at ON users(created_at);
```

## 4. 트리거 설계

### 4.1 Updated At 자동 갱신
```sql
-- updated_at 자동 갱신 함수
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- users 테이블 트리거
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
```

### 4.2 User ID 자동 생성
```sql
-- user_id UUID 자동 생성 함수
CREATE OR REPLACE FUNCTION generate_user_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.user_id IS NULL THEN
        NEW.user_id = gen_random_uuid()::VARCHAR(36);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- user_id 자동 생성 트리거
CREATE TRIGGER generate_users_user_id
    BEFORE INSERT ON users
    FOR EACH ROW
    EXECUTE FUNCTION generate_user_id();
```

## 5. 캐시 설계 (Redis)

### 5.1 캐시 전략

#### 사용자 세션 관리
```
키 패턴: user:session:{user_id}
데이터: JWT Token 정보
TTL: 24시간 (accessToken 만료시간)
```

#### 사용자 프로필 캐시
```
키 패턴: user:profile:{user_id}
데이터: UserProfile DTO
TTL: 1시간
```

#### 로그인 시도 제한
```
키 패턴: user:login_attempts:{username}
데이터: 시도 횟수
TTL: 30분 (잠금 해제 시간)
```

#### JWT 블랙리스트
```
키 패턴: jwt:blacklist:{token_jti}
데이터: 만료 토큰 정보
TTL: JWT 원래 만료시간까지
```

### 5.2 캐시 무효화 정책
- 사용자 정보 수정 시: `user:profile:{user_id}` 삭제
- 로그아웃 시: `user:session:{user_id}` 삭제
- 패스워드 변경 시: 모든 세션 무효화

## 6. 보안 설계

### 6.1 패스워드 정책
- BCrypt 해싱 (strength: 12)
- 최소 8자 이상, 영문/숫자/특수문자 포함
- 패스워드 히스토리 관리 (최근 5개 재사용 방지)

### 6.2 계정 잠금 정책
- 5회 연속 로그인 실패 시 계정 잠금
- 잠금 시간: 30분
- 잠금 해제 후 로그인 시도 횟수 초기화

### 6.3 데이터 암호화
- 민감 정보는 애플리케이션 레벨에서 암호화
- 데이터베이스 연결은 SSL/TLS 사용

## 7. 성능 최적화

### 7.1 연결 풀 설정
```yaml
spring.datasource.hikari:
  maximum-pool-size: 20
  minimum-idle: 5
  idle-timeout: 300000
  connection-timeout: 20000
  max-lifetime: 1200000
```

### 7.2 쿼리 최적화
- 로그인 쿼리: username 인덱스 활용
- 프로필 조회: user_id 인덱스 활용
- 배치 처리: 계정 잠금 해제, 비활성 계정 정리

## 8. 데이터 마이그레이션

### 8.1 초기 데이터
```sql
-- 시스템 관리자 계정
INSERT INTO users (user_id, username, password, name, email, status, created_by, updated_by)
VALUES (
    gen_random_uuid()::VARCHAR(36),
    'admin',
    '$2a$12$encrypted_password_hash',  -- 실제 운영시 암호화된 값 사용
    'System Administrator',
    'admin@tripgen.com',
    'ACTIVE',
    'SYSTEM',
    'SYSTEM'
);
```

### 8.2 버전별 스키마 변경
```sql
-- V1.0.0: 초기 스키마
-- V1.1.0: avatar_url 컬럼 추가
-- V1.2.0: phone 컬럼 추가
-- V1.3.0: 인덱스 추가 및 최적화
```

## 9. 모니터링 및 알람

### 9.1 모니터링 지표
- 활성 사용자 수
- 로그인 성공/실패율
- 계정 잠금 발생 빈도
- 세션 만료율

### 9.2 알람 설정
- 5분간 로그인 실패율 > 10%
- 계정 잠금 발생률 > 1%
- 데이터베이스 연결 실패
- 세션 생성 실패

## 10. 백업 및 복구

### 10.1 백업 전략
- 전체 백업: 일 1회 (자정)
- 증분 백업: 6시간마다
- 트랜잭션 로그 백업: 15분마다

### 10.2 복구 시나리오
- 일반 장애: 최근 증분 백업으로 복구
- 데이터 손실: 전체 백업 + 트랜잭션 로그 복구
- 재해 복구: 원격지 백업으로 복구

## 11. 데이터 보존 정책

### 11.1 사용자 데이터 보존
- 활성 사용자: 무기한 보존
- 비활성 사용자: 3년 후 삭제 알림, 5년 후 자동 삭제
- 탈퇴 사용자: 즉시 논리 삭제, 1년 후 물리 삭제

### 11.2 로그 데이터 보존
- 로그인 로그: 1년 보존
- 오류 로그: 6개월 보존
- 감사 로그: 7년 보존 (법적 요구사항)