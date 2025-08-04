# 개발환경 캐시 설치 결과서

## 개요
- **설치일시**: 2025-01-04
- **환경**: 개발환경 (Kubernetes - AKS)
- **네임스페이스**: tripgen-dev
- **설치자**: 한데브옵스/클라우더

## 설치 완료 내역

### Redis 캐시 서비스 (4개)

#### 1. User Service Redis
- **상태**: ✅ 정상 운영중
- **Pod**: user-redis-8469f5c756-n4qd7 (Running)
- **Service**: user-redis-service (10.0.77.205:6379)
- **Deployment**: user-redis (1/1 Ready)
- **버전**: Redis 7.0-alpine
- **메모리 설정**: 512MB (maxmemory-policy: allkeys-lru)
- **주요 용도**:
  - 세션 관리 (TTL: 30분)
  - 프로필 캐시 (TTL: 1시간)
  - 로그인 제한 (TTL: 15분)
  - JWT 블랙리스트 (TTL: 24시간)

#### 2. Trip Service Redis
- **상태**: ✅ 정상 운영중
- **Pod**: trip-redis-67c579547-rh8sg (Running)
- **Service**: trip-redis-service (10.0.11.58:6379)
- **Deployment**: trip-redis (1/1 Ready)
- **버전**: Redis 7.0-alpine
- **메모리 설정**: 1GB (maxmemory-policy: allkeys-lru)
- **주요 용도**:
  - 여행 목록 캐시 (TTL: 5분)
  - 여행 상세정보 (TTL: 30분)
  - 일정 캐시 (TTL: 10분)
  - 사용자 통계 (TTL: 1시간)

#### 3. AI Service Redis
- **상태**: ✅ 정상 운영중
- **Pod**: ai-redis-7bfd9987d4-fqtvq (Running)
- **Service**: ai-redis-service (10.0.247.154:6379)
- **Deployment**: ai-redis (1/1 Ready)
- **버전**: Redis 7.0-alpine
- **메모리 설정**: 400MB (maxmemory-policy: allkeys-lru)
- **Keyspace 알림**: 활성화 (Pub/Sub 지원)
- **주요 용도**:
  - AI 작업 상태 (TTL: 2시간)
  - 생성된 일정 (TTL: 24시간)
  - AI 추천 정보 (TTL: 6시간)
  - 모델 응답 캐시 (TTL: 1시간)

#### 4. Location Service Redis
- **상태**: ✅ 정상 운영중
- **Pod**: location-redis-65f874d565-xmms7 (Running)
- **Service**: location-redis-service (10.0.105.214:6379)
- **Deployment**: location-redis (1/1 Ready)
- **버전**: Redis 7.0-alpine
- **메모리 설정**: 1.5GB (maxmemory-policy: allkeys-lru)
- **주요 용도**:
  - 장소 정보 캐시 (TTL: 2시간)
  - 지리 데이터 (Redis GEO 활용)
  - 검색 결과 캐시 (TTL: 30분)
  - 추천 정보 캐시 (TTL: 24시간)

## 캐시 키 네이밍 규칙

### User Service
```
user:session:{session_id}
user:profile:{user_id}
user:login:attempt:{email}
user:jwt:blacklist:{token_jti}
```

### Trip Service
```
trip:list:user:{user_id}
trip:detail:{trip_id}
trip:schedule:{trip_id}
trip:stats:user:{user_id}
```

### AI Service
```
ai:job:status:{job_id}
ai:schedule:result:{schedule_id}
ai:recommendation:{place_id}:{user_hash}
ai:model:response:{request_hash}
```

### Location Service
```
location:place:{place_id}
location:geo:nearby:{lat}:{lng}:{radius}
location:search:{query_hash}
location:recommendation:{place_id}:{user_profile_hash}
```

## 접속 정보

### 클러스터 내부 접속
| 서비스 | 호스트 | 포트 |
|--------|--------|------|
| User | user-redis-service.tripgen-dev.svc.cluster.local | 6379 |
| Trip | trip-redis-service.tripgen-dev.svc.cluster.local | 6379 |
| AI | ai-redis-service.tripgen-dev.svc.cluster.local | 6379 |
| Location | location-redis-service.tripgen-dev.svc.cluster.local | 6379 |

### 포트 포워딩 명령어
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
- ✅ 모든 Redis Pod 정상 Running 상태
- ✅ 모든 Service 정상 생성 및 ClusterIP 할당
- ✅ Redis 연결 테스트 (PING/PONG) 성공
- ✅ 데이터 저장/조회 테스트 완료
- ✅ 메모리 설정 및 LRU 정책 적용 확인
- ✅ TTL 정책 동작 확인
- ✅ AI Service의 Keyspace 알림 기능 확인

## 애플리케이션 연결 설정

### Spring Boot 설정 예시
```yaml
spring:
  redis:
    host: {service-name}-redis-service.tripgen-dev.svc.cluster.local
    port: 6379
    timeout: 2000
    jedis:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 1000
```

## 운영 모니터링 명령어

### 메모리 사용량 확인
```bash
kubectl exec -it deployment/{service}-redis -n tripgen-dev -- redis-cli INFO memory
```

### 키 통계 확인
```bash
kubectl exec -it deployment/{service}-redis -n tripgen-dev -- redis-cli INFO keyspace
```

### 캐시 히트율 확인
```bash
kubectl exec -it deployment/{service}-redis -n tripgen-dev -- redis-cli INFO stats
```

## 주의사항
1. 개발환경이므로 Redis 영속성(RDB, AOF)은 비활성화되어 있음
2. Pod 재시작 시 모든 캐시 데이터가 삭제됨
3. 메모리 한계 도달 시 LRU 정책에 따라 오래된 키부터 삭제됨
4. 프로덕션 환경에서는 Redis Cluster 구성 권장

## 장애 대응 방안

### 캐시 장애 시 폴백 전략
- **User Service**: DB 기반 세션 관리로 자동 전환
- **Trip Service**: DB에서 직접 조회 (성능 저하 감수)
- **AI Service**: 캐시 미스 시 재생성 또는 DB 조회
- **Location Service**: DB 및 외부 API 직접 호출

### 캐시 복구 절차
1. Pod 재시작: `kubectl rollout restart deployment/{service}-redis -n tripgen-dev`
2. 캐시 워밍 스크립트 실행 (애플리케이션별 구현 필요)
3. 모니터링을 통한 정상 동작 확인

## 다음 단계
1. 애플리케이션에서 Redis 연결 설정 적용
2. 캐시 키 TTL 정책 세부 조정
3. 캐시 워밍 전략 구현
4. 캐시 히트율 모니터링 대시보드 구성
5. 장애 시나리오 테스트 수행

---
작성자: 한데브옵스/클라우더
작성일: 2025-01-04