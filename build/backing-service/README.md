# 백킹서비스 설치 가이드

여행 일정 생성 서비스의 백킹서비스(데이터베이스) 설치 가이드입니다.

## 개요

물리 아키텍처에서 정의된 백킹서비스 구성:
- **PostgreSQL**: 마이크로서비스별 독립 데이터베이스
- **Redis**: 캐싱 및 성능 최적화

## 필요 백킹서비스

### 1. PostgreSQL (오픈소스)
- **용도**: 영구 데이터 저장소
- **필요 데이터베이스**:
  - `profile_db`: 프로필 서비스 데이터
  - `itinerary_db`: 일정 관리 서비스 데이터
  - `location_db`: 장소 검색 서비스 데이터
- **예상 스펙**: 2 vCores, 8GB RAM

### 2. Redis (오픈소스)
- **용도**: 캐싱 레이어
- **캐시 전략**:
  - 프로파일 데이터: TTL 24시간
  - 장소 정보: TTL 1시간
  - 검색 결과: TTL 10분
- **예상 스펙**: 1GB Memory

## 설치 가이드

### Docker 환경
Docker Compose를 이용한 로컬 개발 환경 구성

- [PostgreSQL Docker 설치 가이드](./db-postgresql.md#1-docker-설치-방법)
- [Redis Docker 설치 가이드](./db-redis.md#1-docker-설치-방법)

### Kubernetes 환경
Azure Kubernetes Service(AKS)를 이용한 프로덕션 환경 구성

- [PostgreSQL k8s 설치 가이드](./db-postgresql.md#2-kubernetes-k8s-설치-방법)
- [Redis k8s 설치 가이드](./db-redis.md#2-kubernetes-k8s-설치-방법)

## 빠른 시작 (Docker Compose)

### 통합 docker-compose.yml
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14-alpine
    container_name: tripgen-postgres
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: Hi5Jessica!
      POSTGRES_MULTIPLE_DATABASES: profile_db,itinerary_db,location_db
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sh:/docker-entrypoint-initdb.d/01-init-db.sh
    networks:
      - tripgen-network
    restart: unless-stopped

  redis:
    image: redis:7.0-alpine
    container_name: tripgen-redis
    command: redis-server --requirepass Hi5Jessica! --maxmemory 1gb --maxmemory-policy allkeys-lru
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - tripgen-network
    restart: unless-stopped

volumes:
  postgres_data:
  redis_data:

networks:
  tripgen-network:
    driver: bridge
```

### 실행
```bash
# 모든 서비스 시작
docker-compose up -d

# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f
```

## 접속 정보

### 개발 환경 (Docker)
- **PostgreSQL**
  - Host: localhost
  - Port: 5432
  - Username: postgres
  - Password: Hi5Jessica!
  
- **Redis**
  - Host: localhost
  - Port: 6379
  - Password: Hi5Jessica!

### 프로덕션 환경 (k8s)
LoadBalancer 서비스의 External IP를 사용하여 접속

## 애플리케이션 설정 예시

### Spring Boot application.yml
```yaml
spring:
  # PostgreSQL 설정
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/${DB_NAME}
    username: postgres
    password: Hi5Jessica!
    
  # Redis 설정
  redis:
    host: ${REDIS_HOST:localhost}
    port: 6379
    password: Hi5Jessica!
```

## 운영 가이드

### 백업
- PostgreSQL: 일일 백업 권장
- Redis: RDB 스냅샷 + AOF 로그

### 모니터링
- 연결 수 모니터링
- 메모리 사용량 체크
- 쿼리 성능 분석

### 스케일링
- PostgreSQL: Read Replica 추가
- Redis: Cluster 모드 전환

## 문제 해결

### 연결 오류
1. 방화벽/보안그룹 설정 확인
2. 서비스 상태 확인
3. 인증 정보 확인

### 성능 이슈
1. 인덱스 최적화
2. 캐시 히트율 확인
3. 리소스 사용량 모니터링

## 참고 자료
- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/14/)
- [Redis 공식 문서](https://redis.io/documentation)
- [Helm Charts - Bitnami](https://github.com/bitnami/charts)