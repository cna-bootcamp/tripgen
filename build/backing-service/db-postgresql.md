# PostgreSQL 설치 가이드

물리 아키텍처에서 요구하는 PostgreSQL 데이터베이스 설치 방법을 안내합니다.

## 필요 데이터베이스
- profile_db: 프로필 서비스용
- itinerary_db: 일정 관리 서비스용  
- location_db: 장소 검색 서비스용

---

## 1. Docker 설치 방법

### docker-compose.yml 작성
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

volumes:
  postgres_data:

networks:
  tripgen-network:
    driver: bridge
```

### init-db.sh 작성
```bash
#!/bin/bash
set -e

# 다중 데이터베이스 생성 스크립트
POSTGRES_MULTIPLE_DATABASES=${POSTGRES_MULTIPLE_DATABASES:-}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
    echo "Creating multiple databases: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
            CREATE DATABASE $db;
            GRANT ALL PRIVILEGES ON DATABASE $db TO $POSTGRES_USER;
EOSQL
    done
fi
```

### 실행 방법
```bash
# 실행 권한 부여
chmod +x init-db.sh

# Docker Compose 실행
docker-compose up -d

# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f postgres
```

### 접속 테스트
```bash
# PostgreSQL 컨테이너 접속
docker exec -it tripgen-postgres psql -U postgres

# 데이터베이스 목록 확인
\l

# 각 데이터베이스 접속 테스트
\c profile_db
\c itinerary_db
\c location_db

# 종료
\q
```

---

## 2. Kubernetes (k8s) 설치 방법

### 작업 디렉토리 생성
```bash
mkdir -p ~/install/postgres && cd ~/install/postgres
```

### values.yaml 작성
```yaml
# PostgreSQL 아키텍처 설정 (실습시 standalone 권장)
architecture: standalone  # production에서는 replication 사용

# 글로벌 설정
global:
  postgresql:
    auth:
      postgresPassword: "Hi5Jessica!"
      database: "profile_db"  # 기본 DB
  storageClass: "managed-premium"
  
# Primary 설정
primary:
  persistence:
    enabled: true
    storageClass: "managed-premium"
    size: 10Gi
  
  resources:
    limits:
      memory: "2Gi"
      cpu: "1"
    requests:
      memory: "1Gi"
      cpu: "0.5"
  
  # 성능 최적화 설정  
  extraEnvVars:
    - name: POSTGRESQL_SHARED_BUFFERS
      value: "512MB"
    - name: POSTGRESQL_EFFECTIVE_CACHE_SIZE  
      value: "1.5GB"
    - name: POSTGRESQL_MAX_CONNECTIONS
      value: "200"

  # 초기화 스크립트로 추가 DB 생성
  initdb:
    scripts:
      init-databases.sql: |
        CREATE DATABASE itinerary_db;
        GRANT ALL PRIVILEGES ON DATABASE itinerary_db TO postgres;
        CREATE DATABASE location_db;
        GRANT ALL PRIVILEGES ON DATABASE location_db TO postgres;

# 네트워크 설정
service:
  type: ClusterIP
  ports:
    postgresql: 5432

# 보안 설정
securityContext:
  enabled: true
  fsGroup: 1001
  runAsUser: 1001
```

### Helm 설치
```bash
# namespace 생성
kubectl create ns tripgen-db
kubectl config set-context --current --namespace=tripgen-db

# PostgreSQL 설치
helm upgrade -i postgres -f values.yaml bitnami/postgresql --version 14.3.2

# 설치 상태 확인
kubectl get pods -w
```

### 외부 접속용 Service 생성
postgres-external.yaml 작성:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres-external
  namespace: tripgen-db
spec:
  type: LoadBalancer
  ports:
  - name: tcp-postgresql
    port: 5432
    protocol: TCP
    targetPort: tcp-postgresql
  selector:
    app.kubernetes.io/component: primary
    app.kubernetes.io/instance: postgres
    app.kubernetes.io/name: postgresql
```

```bash
# Service 생성
kubectl apply -f postgres-external.yaml

# LoadBalancer IP 확인
kubectl get svc postgres-external
```

### 접속 테스트
```bash
# Pod 내부에서 테스트
kubectl exec -it postgres-postgresql-0 -- psql -U postgres

# 데이터베이스 목록 확인
\l

# 각 데이터베이스 확인
\c profile_db
\c itinerary_db  
\c location_db

# 종료
\q
```

---

## 3. 접속 정보

### Docker 환경
- Host: localhost (로컬) 또는 Docker 호스트 IP
- Port: 5432
- Username: postgres
- Password: Hi5Jessica!
- Databases: profile_db, itinerary_db, location_db

### Kubernetes 환경
- Host: LoadBalancer External IP
- Port: 5432
- Username: postgres
- Password: Hi5Jessica!
- Databases: profile_db, itinerary_db, location_db

### 애플리케이션 연결 문자열 예시
```
# profile-service
postgresql://postgres:Hi5Jessica!@{host}:5432/profile_db

# itinerary-service
postgresql://postgres:Hi5Jessica!@{host}:5432/itinerary_db

# location-service
postgresql://postgres:Hi5Jessica!@{host}:5432/location_db
```

---

## 4. 유지보수

### 백업
```bash
# Docker
docker exec tripgen-postgres pg_dumpall -U postgres > backup.sql

# Kubernetes
kubectl exec postgres-postgresql-0 -- pg_dumpall -U postgres > backup.sql
```

### 복원
```bash
# Docker
docker exec -i tripgen-postgres psql -U postgres < backup.sql

# Kubernetes
kubectl exec -i postgres-postgresql-0 -- psql -U postgres < backup.sql
```

### 모니터링
- 연결 수: `SELECT count(*) FROM pg_stat_activity;`
- DB 크기: `SELECT pg_database_size('profile_db');`
- 실행 중인 쿼리: `SELECT * FROM pg_stat_activity WHERE state = 'active';`