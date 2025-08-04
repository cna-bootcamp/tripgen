# User 서비스 데이터베이스 설치 가이드 - 개발환경

## 1. 개요

### 1.1 서비스 정보
- **서비스명**: User Service
- **데이터베이스 타입**: PostgreSQL 16 (Alpine)
- **데이터베이스명**: tripgen_user
- **스키마**: user_schema
- **컨테이너**: postgresql.tripgen-dev.svc.cluster.local
- **포트**: 5432

### 1.2 요구사항
- tripgen-dev 네임스페이스가 있는 Kubernetes 클러스터 (AKS)
- 적절한 권한으로 구성된 kubectl
- PostgreSQL 클라이언트 도구 (확인용, 선택사항)

## 2. 설치 단계

### 2.1 데이터베이스 시크릿 생성
```bash
# 데이터베이스 자격 증명 시크릿 생성
kubectl create secret generic user-db-secret \
  --from-literal=username=user_service \
  --from-literal=password=UserServiceDev2025! \
  --from-literal=database=tripgen_user \
  -n tripgen-dev
```

### 2.2 데이터베이스 초기화용 ConfigMap 생성
```bash
# 초기화 스크립트가 포함된 ConfigMap 생성
kubectl create configmap user-db-init \
  --from-file=init.sql=user-db-init.sql \
  -n tripgen-dev
```

### 2.3 데이터베이스 초기화 스크립트
`user-db-init.sql` 파일 생성:

```sql
-- 메인 데이터베이스에 연결하여 서비스 데이터베이스 생성
\c postgres;

-- 서비스 데이터베이스 생성
CREATE DATABASE tripgen_user;

-- 서비스 데이터베이스에 연결
\c tripgen_user;

-- 스키마 생성
CREATE SCHEMA IF NOT EXISTS user_schema;

-- 기본 검색 경로 설정
ALTER DATABASE tripgen_user SET search_path TO user_schema, public;

-- 서비스 사용자 생성
CREATE USER user_service WITH PASSWORD 'UserServiceDev2025!';

-- 권한 부여
GRANT ALL PRIVILEGES ON DATABASE tripgen_user TO user_service;
GRANT ALL PRIVILEGES ON SCHEMA user_schema TO user_service;
ALTER SCHEMA user_schema OWNER TO user_service;

-- 서비스 사용자 컨텍스트로 전환
SET ROLE user_service;

-- 테이블 생성
CREATE TABLE user_schema.users (
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
    
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'LOCKED', 'DELETED')),
    CONSTRAINT chk_users_login_attempts CHECK (login_attempts >= 0),
    CONSTRAINT chk_users_username_length CHECK (LENGTH(username) >= 5),
    CONSTRAINT chk_users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_users_name_length CHECK (LENGTH(name) >= 2)
);

-- 인덱스 생성
CREATE UNIQUE INDEX idx_users_user_id ON user_schema.users(user_id);
CREATE UNIQUE INDEX idx_users_username ON user_schema.users(username);
CREATE UNIQUE INDEX idx_users_email ON user_schema.users(email);
CREATE INDEX idx_users_username_status ON user_schema.users(username, status) WHERE status = 'ACTIVE';
CREATE INDEX idx_users_locked_until ON user_schema.users(locked_until) WHERE locked_until IS NOT NULL;
CREATE INDEX idx_users_last_login_at ON user_schema.users(last_login_at) WHERE last_login_at IS NOT NULL;
CREATE INDEX idx_users_created_at ON user_schema.users(created_at);

-- updated_at 자동 갱신 트리거 함수 생성
CREATE OR REPLACE FUNCTION user_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 트리거 생성
CREATE TRIGGER update_users_updated_at BEFORE UPDATE
    ON user_schema.users FOR EACH ROW
    EXECUTE FUNCTION user_schema.update_updated_at_column();

-- 코멘트 추가
COMMENT ON TABLE user_schema.users IS '사용자 기본 정보';
COMMENT ON COLUMN user_schema.users.user_id IS '서비스 간 연결용 UUID';
COMMENT ON COLUMN user_schema.users.username IS '로그인용 사용자명 (최소 5자)';
COMMENT ON COLUMN user_schema.users.password IS 'BCrypt 암호화된 패스워드';
COMMENT ON COLUMN user_schema.users.status IS '계정 상태 (ACTIVE/INACTIVE/SUSPENDED/LOCKED/DELETED)';
COMMENT ON COLUMN user_schema.users.login_attempts IS '로그인 시도 횟수 (5회 실패시 계정 잠금)';
COMMENT ON COLUMN user_schema.users.locked_until IS '계정 잠금 해제 시간';

-- 개발용 테스트 데이터 삽입
INSERT INTO user_schema.users (user_id, username, password, name, email, phone, status)
VALUES 
('550e8400-e29b-41d4-a716-446655440001', 'testuser1', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '테스트 사용자 1', 'test1@tripgen.com', '010-1234-5678', 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440002', 'testuser2', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '테스트 사용자 2', 'test2@tripgen.com', '010-2345-6789', 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440003', 'testadmin', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '관리자', 'admin@tripgen.com', '010-0000-0000', 'ACTIVE');

-- 참고: 모든 테스트 사용자의 기본 패스워드는 'password123'입니다
```

### 2.4 데이터베이스 초기화 Job 생성
```yaml
# user-db-init-job.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: user-db-init
  namespace: tripgen-dev
spec:
  template:
    spec:
      restartPolicy: Never
      containers:
      - name: postgres-init
        image: postgres:16-alpine
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 500m
            memory: 256Mi
        env:
        - name: PGPASSWORD
          valueFrom:
            secretKeyRef:
              name: postgresql-secret
              key: password
        command: ["/bin/bash"]
        args:
          - -c
          - |
            echo "User 서비스 데이터베이스 초기화 중..."
            psql -h postgresql.tripgen-dev.svc.cluster.local -U postgres -f /scripts/init.sql
            echo "데이터베이스 초기화 완료."
        volumeMounts:
        - name: init-scripts
          mountPath: /scripts
      volumes:
      - name: init-scripts
        configMap:
          name: user-db-init
```

### 2.5 데이터베이스 초기화 실행
```bash
# 초기화 Job 적용
kubectl apply -f user-db-init-job.yaml

# Job 실행 모니터링
kubectl logs -f job/user-db-init -n tripgen-dev

# Job 완료 확인
kubectl get jobs -n tripgen-dev
```

## 3. 검증

### 3.1 데이터베이스 연결 테스트
```bash
# PostgreSQL Pod에 연결
kubectl exec -it postgresql-0 -n tripgen-dev -- psql -U postgres

# PostgreSQL 내에서 데이터베이스 생성 확인
\l

# user 데이터베이스에 연결
\c tripgen_user

# 스키마 확인
\dn

# 테이블 확인
\dt user_schema.*

# 테스트 데이터 확인
SELECT user_id, username, email, status FROM user_schema.users;

# 종료
\q
```

### 3.2 서비스 연결 구성
User 서비스 배포에 다음 환경 변수 추가:

```yaml
env:
- name: DB_HOST
  value: postgresql.tripgen-dev.svc.cluster.local
- name: DB_PORT
  value: "5432"
- name: DB_NAME
  value: tripgen_user
- name: DB_SCHEMA
  value: user_schema
- name: DB_USERNAME
  valueFrom:
    secretKeyRef:
      name: user-db-secret
      key: username
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: user-db-secret
      key: password
```

### 3.3 연결 문자열
```
postgresql://user_service:UserServiceDev2025!@postgresql.tripgen-dev.svc.cluster.local:5432/tripgen_user?schema=user_schema
```

## 4. 유지보수

### 4.1 수동 백업
```bash
# user 데이터베이스 백업
kubectl exec postgresql-0 -n tripgen-dev -- pg_dump -U postgres tripgen_user > user-db-backup-$(date +%Y%m%d).sql
```

### 4.2 백업에서 복원
```bash
# user 데이터베이스 복원
kubectl exec -i postgresql-0 -n tripgen-dev -- psql -U postgres tripgen_user < user-db-backup-20250730.sql
```

### 4.3 데이터베이스 모니터링
```bash
# 데이터베이스 크기 확인
kubectl exec postgresql-0 -n tripgen-dev -- psql -U postgres -d tripgen_user -c "SELECT pg_database_size('tripgen_user');"

# 활성 연결 확인
kubectl exec postgresql-0 -n tripgen-dev -- psql -U postgres -c "SELECT count(*) FROM pg_stat_activity WHERE datname = 'tripgen_user';"

# 테이블 크기 확인
kubectl exec postgresql-0 -n tripgen-dev -- psql -U postgres -d tripgen_user -c "SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size FROM pg_tables WHERE schemaname = 'user_schema' ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;"
```

## 5. 문제 해결

### 5.1 일반적인 문제

| 문제 | 원인 | 해결방법 |
|------|------|----------|
| 데이터베이스를 찾을 수 없음 | 초기화 실패 | 초기화 Job 재실행 |
| 권한 거부됨 | 사용자 권한 미설정 | 사용자 권한 확인 및 재설정 |
| 연결 거부됨 | PostgreSQL 미실행 | Pod 상태 확인 및 필요시 재시작 |
| 스키마를 찾을 수 없음 | 잘못된 검색 경로 | search_path 설정 확인 |

### 5.2 디버그 명령어
```bash
# PostgreSQL Pod 로그 확인
kubectl logs postgresql-0 -n tripgen-dev

# 초기화 Job 로그 확인
kubectl logs job/user-db-init -n tripgen-dev

# Pod 이벤트 상세 정보 확인
kubectl describe pod postgresql-0 -n tripgen-dev
```

## 6. 개발 참고사항

- 기본 테스트 패스워드: 'password123' (BCrypt 해시)
- 모든 타임스탬프는 UTC 시간대 사용
- 5회 로그인 실패시 계정 잠금
- 이메일은 정규표현식 패턴으로 검증
- 사용자명은 최소 5자 이상이어야 함