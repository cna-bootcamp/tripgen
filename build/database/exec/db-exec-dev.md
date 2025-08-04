# 개발환경 데이터베이스 설치 결과서

## 개요
- **설치일시**: 2025-01-04
- **환경**: 개발환경 (Kubernetes - AKS)
- **네임스페이스**: tripgen-dev
- **설치자**: 한데브옵스/클라우더

## 설치 완료 내역

### 1. PostgreSQL 데이터베이스 (4개 서비스)

#### User Service Database
- **상태**: ✅ 정상 운영중
- **Pod**: user-db-654f88df56-75x7t (Running)
- **Service**: user-db-service (10.0.170.93:5432)
- **PVC**: user-db-pvc (5Gi, Bound)
- **버전**: PostgreSQL 14-alpine
- **초기화 완료**: 
  - users 테이블 및 인덱스 생성
  - 자동 업데이트 트리거 설정
  - 샘플 데이터 (admin, testuser) 입력

#### Trip Service Database  
- **상태**: ✅ 정상 운영중
- **Pod**: trip-db-6d6cc84fcd-jkfvv (Running)
- **Service**: trip-db-service (10.0.131.168:5432)
- **PVC**: trip-db-pvc (10Gi, Bound)
- **버전**: PostgreSQL 14-alpine
- **초기화 완료**:
  - 5개 테이블 생성 (trips, members, destinations, schedules, schedule_places)
  - 33개 인덱스, 7개 함수, 6개 트리거 생성
  - JSONB 타입 지원 활성화
  - 샘플 여행 데이터 입력

#### AI Service Database
- **상태**: ✅ 정상 운영중  
- **Pod**: ai-db-5687dd99cd-hb5t7 (Running)
- **Service**: ai-db-service (10.0.67.38:5432)
- **PVC**: ai-db-pvc (8Gi, Bound)
- **버전**: PostgreSQL 14-alpine
- **초기화 완료**:
  - 3개 테이블 생성 (ai_schedules, ai_jobs, ai_recommendations)
  - GIN 인덱스 포함 13개 성능 최적화 인덱스
  - 진행률 자동 계산 트리거
  - 샘플 AI 작업 데이터 입력

#### Location Service Database (PostGIS)
- **상태**: ✅ 정상 운영중
- **Pod**: location-db-f964b6b6c-5g22j (Running)
- **Service**: location-db-service (10.0.42.103:5432)
- **PVC**: location-db-pvc (15Gi, Bound)
- **버전**: PostgreSQL 14 + PostGIS 3.2
- **초기화 완료**:
  - 3개 테이블 생성 (places, place_details, place_recommendations)
  - PostGIS 공간 인덱스 및 텍스트 검색 인덱스
  - 공간 검색 함수 생성
  - 뮌헨 관광지 샘플 데이터 입력

### 2. Redis 캐시 (4개 서비스)

#### User Service Redis
- **상태**: ✅ 정상 운영중
- **Pod**: user-redis-8469f5c756-n4qd7 (Running)
- **Service**: user-redis-service (10.0.77.205:6379)
- **메모리**: 512MB (LRU 정책)
- **용도**: 세션, 프로필 캐시, 로그인 제한, JWT 블랙리스트

#### Trip Service Redis
- **상태**: ✅ 정상 운영중
- **Pod**: trip-redis-67c579547-rh8sg (Running)  
- **Service**: trip-redis-service (10.0.11.58:6379)
- **메모리**: 1GB (LRU 정책)
- **용도**: 여행 목록, 상세정보, 일정 캐시

#### AI Service Redis
- **상태**: ✅ 정상 운영중
- **Pod**: ai-redis-7bfd9987d4-fqtvq (Running)
- **Service**: ai-redis-service (10.0.247.154:6379)
- **메모리**: 400MB (LRU 정책)
- **용도**: AI 작업 상태, 생성 결과, 모델 응답 캐시

#### Location Service Redis
- **상태**: ✅ 정상 운영중
- **Pod**: location-redis-65f874d565-xmms7 (Running)
- **Service**: location-redis-service (10.0.105.214:6379)
- **메모리**: 1.5GB (LRU 정책)
- **용도**: 장소 정보, 지리 데이터, 검색 결과 캐시

## 접속 정보

### PostgreSQL 데이터베이스
| 서비스 | 호스트 | 포트 | DB명 | 사용자 | 비밀번호 |
|--------|--------|------|------|--------|----------|
| User | user-db-service.tripgen-dev.svc.cluster.local | 5432 | tripgen_user_db | tripgen_user | tripgen_user_123 |
| Trip | trip-db-service.tripgen-dev.svc.cluster.local | 5432 | tripgen_trip_db | tripgen_trip | tripgen_trip_123 |
| AI | ai-db-service.tripgen-dev.svc.cluster.local | 5432 | tripgen_ai_db | tripgen_ai | tripgen_ai_123 |
| Location | location-db-service.tripgen-dev.svc.cluster.local | 5432 | tripgen_location_db | tripgen_location | tripgen_location_123 |

### Redis 캐시
| 서비스 | 호스트 | 포트 |
|--------|--------|------|
| User | user-redis-service.tripgen-dev.svc.cluster.local | 6379 |
| Trip | trip-redis-service.tripgen-dev.svc.cluster.local | 6379 |
| AI | ai-redis-service.tripgen-dev.svc.cluster.local | 6379 |
| Location | location-redis-service.tripgen-dev.svc.cluster.local | 6379 |

## 포트 포워딩 명령어

### PostgreSQL 접속
```bash
# User DB
kubectl port-forward -n tripgen-dev service/user-db-service 5432:5432

# Trip DB  
kubectl port-forward -n tripgen-dev service/trip-db-service 5433:5432

# AI DB
kubectl port-forward -n tripgen-dev service/ai-db-service 5434:5432

# Location DB
kubectl port-forward -n tripgen-dev service/location-db-service 5435:5432
```

### Redis 접속
```bash
# User Redis
kubectl port-forward -n tripgen-dev service/user-redis-service 6379:6379

# Trip Redis
kubectl port-forward -n tripgen-dev service/trip-redis-service 6380:6379

# AI Redis  
kubectl port-forward -n tripgen-dev service/ai-redis-service 6381:6379

# Location Redis
kubectl port-forward -n tripgen-dev service/location-redis-service 6382:6379
```

## 검증 완료 항목
- ✅ 모든 Pod 정상 Running 상태
- ✅ 모든 Service 정상 생성 및 ClusterIP 할당
- ✅ 모든 PVC 정상 Bound 상태
- ✅ 데이터베이스 초기화 스크립트 정상 실행
- ✅ 샘플 데이터 입력 및 조회 확인
- ✅ Redis 연결 테스트 (PING/PONG) 성공
- ✅ 메모리 설정 및 TTL 정책 적용 확인

## 주의사항
1. 개발환경이므로 Redis 영속성은 비활성화되어 있음
2. 데이터베이스 비밀번호는 개발용이므로 프로덕션 배포 시 변경 필요
3. 리소스 제한이 개발환경에 맞게 설정되어 있으므로 프로덕션 배포 시 조정 필요
4. PostGIS 기능은 Location Service에서만 사용 가능

## 다음 단계
1. 애플리케이션에서 연결 설정 적용
2. 데이터베이스 마이그레이션 스크립트 실행 (필요시)
3. 캐시 워밍 전략 구현
4. 모니터링 대시보드 설정

---
작성자: 한데브옵스/클라우더
작성일: 2025-01-04