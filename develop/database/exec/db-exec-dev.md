# 개발환경 데이터베이스 설치 결과서

## 설치 개요
- **설치일시**: 2025-08-04 12:40 KST
- **AKS 클러스터**: aks-digitalgarage-01
- **네임스페이스**: tripgen-dev
- **StorageClass**: managed

## 설치된 PostgreSQL 인스턴스

### 1. User Service Database
- **상태**: ✅ Running
- **포트**: 5432
- **데이터베이스**: tripgen_user_db
- **사용자**: tripgen_user
- **비밀번호**: tripgen_user_123
- **버전**: PostgreSQL 14.18
- **스토리지**: 5Gi (managed)
- **내부 DNS**: user-db-service.tripgen-dev.svc.cluster.local

### 2. Trip Service Database
- **상태**: ✅ Running
- **포트**: 5433
- **데이터베이스**: tripgen_trip_db
- **사용자**: tripgen_trip
- **비밀번호**: tripgen_trip_123
- **버전**: PostgreSQL 14.18
- **스토리지**: 10Gi (managed)
- **내부 DNS**: trip-db-service.tripgen-dev.svc.cluster.local

### 3. AI Service Database
- **상태**: ✅ Running
- **포트**: 5434
- **데이터베이스**: tripgen_ai_db
- **사용자**: tripgen_ai
- **비밀번호**: tripgen_ai_123
- **버전**: PostgreSQL 14.18
- **스토리지**: 8Gi (managed)
- **내부 DNS**: ai-db-service.tripgen-dev.svc.cluster.local

### 4. Location Service Database (PostGIS)
- **상태**: ✅ Running
- **포트**: 5435
- **데이터베이스**: tripgen_location_db
- **사용자**: tripgen_location
- **비밀번호**: tripgen_location_123
- **버전**: PostgreSQL 14.18 + PostGIS 3.2.3
- **스토리지**: 15Gi (managed)
- **내부 DNS**: location-db-service.tripgen-dev.svc.cluster.local

## 연결 테스트 결과

### 내부 연결 테스트 (Pod 내부에서)
```bash
# User DB
kubectl exec -it deploy/user-db -n tripgen-dev -- psql -U tripgen_user -d tripgen_user_db -c "SELECT version();"
# Result: PostgreSQL 14.18 on x86_64-pc-linux-gnu

# Trip DB
kubectl exec -it deploy/trip-db -n tripgen-dev -- psql -U tripgen_trip -d tripgen_trip_db -c "SELECT version();"
# Result: PostgreSQL 14.18 on x86_64-pc-linux-gnu

# AI DB
kubectl exec -it deploy/ai-db -n tripgen-dev -- psql -U tripgen_ai -d tripgen_ai_db -c "SELECT version();"
# Result: PostgreSQL 14.18 on x86_64-pc-linux-gnu

# Location DB (PostGIS)
kubectl exec -it deploy/location-db -n tripgen-dev -- psql -U tripgen_location -d tripgen_location_db -c "SELECT PostGIS_full_version();"
# Result: POSTGIS="3.2.3 4975b69" PGSQL="140" GEOS="3.11.0-CAPI-1.17.0"
```

## 생성된 테이블 구조

### User Service
- users (사용자 정보)
- user_sessions (세션 관리)
- login_attempts (로그인 시도 추적)

### Trip Service
- trips (여행 계획)
- members (여행 멤버)
- destinations (목적지)
- schedules (일정)
- schedule_places (일정별 장소)

### AI Service
- ai_jobs (AI 작업 관리)
- ai_schedules (생성된 일정)
- ai_recommendations (추천 정보)

### Location Service
- places (장소 정보 - geometry 포함)
- place_details (상세 정보)
- place_recommendations (추천 장소)

## 애플리케이션 연결 설정 예시

### Spring Boot application.yml
```yaml
spring:
  datasource:
    url: jdbc:postgresql://user-db-service.tripgen-dev.svc.cluster.local:5432/tripgen_user_db
    username: tripgen_user
    password: tripgen_user_123
    driver-class-name: org.postgresql.Driver
```

### Node.js 연결
```javascript
const { Pool } = new Pool({
  host: 'trip-db-service.tripgen-dev.svc.cluster.local',
  port: 5433,
  database: 'tripgen_trip_db',
  user: 'tripgen_trip',
  password: 'tripgen_trip_123'
});
```

## 문제 해결 참고사항

### PVC 이슈
- 모든 PVC가 managed StorageClass를 사용하도록 설정됨
- AKS의 기본 managed-csi StorageClass 사용

### 연결 문제 시
1. Pod 상태 확인: `kubectl get pods -n tripgen-dev`
2. Service 확인: `kubectl get svc -n tripgen-dev`
3. PVC 바인딩 확인: `kubectl get pvc -n tripgen-dev`
4. 로그 확인: `kubectl logs deploy/{service-name}-db -n tripgen-dev`

## 백업 및 복구 명령어

### 백업
```bash
kubectl exec -it deploy/user-db -n tripgen-dev -- pg_dump -U tripgen_user tripgen_user_db > user_db_backup.sql
```

### 복구
```bash
kubectl exec -i deploy/user-db -n tripgen-dev -- psql -U tripgen_user tripgen_user_db < user_db_backup.sql
```