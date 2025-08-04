# 개발환경 캐시 설치 결과서

## 설치 개요
- **설치일시**: 2025-08-04 12:40 KST
- **AKS 클러스터**: aks-digitalgarage-01
- **네임스페이스**: tripgen-dev
- **설치 방식**: Memory-only (개발환경)

## 설치된 Redis 인스턴스

### 1. User Service Redis
- **상태**: ✅ Running
- **포트**: 6379
- **메모리 할당**: 512MB
- **최대 메모리 정책**: allkeys-lru
- **내부 DNS**: user-redis-service.tripgen-dev.svc.cluster.local
- **용도**: 
  - 사용자 세션 관리
  - 프로필 캐시
  - 로그인 제한 추적
  - JWT 블랙리스트

### 2. Trip Service Redis
- **상태**: ✅ Running
- **포트**: 6380
- **메모리 할당**: 1024MB
- **최대 메모리 정책**: allkeys-lru
- **내부 DNS**: trip-redis-service.tripgen-dev.svc.cluster.local
- **용도**:
  - 여행 목록 캐시
  - 여행 상세정보
  - 일정 데이터
  - 인기 여행 정보

### 3. AI Service Redis
- **상태**: ✅ Running
- **포트**: 6381
- **메모리 할당**: 768MB
- **최대 메모리 정책**: allkeys-lru
- **내부 DNS**: ai-redis-service.tripgen-dev.svc.cluster.local
- **용도**:
  - AI 작업 상태 추적
  - 생성된 일정 캐시
  - 추천 결과 저장
  - 모델 응답 캐시

### 4. Location Service Redis
- **상태**: ✅ Running
- **포트**: 6382
- **메모리 할당**: 512MB
- **최대 메모리 정책**: allkeys-lru
- **내부 DNS**: location-redis-service.tripgen-dev.svc.cluster.local
- **용도**:
  - 장소 상세정보
  - 검색 결과 캐시
  - 추천 장소
  - 인기 장소 랭킹

## 연결 테스트 결과

### PING 테스트
```bash
# User Redis
kubectl exec -it deploy/trip-db -n tripgen-dev -- redis-cli -h user-redis-service -p 6379 ping
# Result: PONG

# Trip Redis
kubectl exec -it deploy/trip-db -n tripgen-dev -- redis-cli -h trip-redis-service -p 6380 ping
# Result: PONG

# AI Redis
kubectl exec -it deploy/trip-db -n tripgen-dev -- redis-cli -h ai-redis-service -p 6381 ping
# Result: PONG

# Location Redis
kubectl exec -it deploy/trip-db -n tripgen-dev -- redis-cli -h location-redis-service -p 6382 ping
# Result: PONG
```

## 캐시 키 네이밍 규칙

### User Service
- 세션: `session:{user_id}`
- 프로필: `profile:{user_id}`
- 로그인 제한: `login_attempts:{email}`
- JWT 블랙리스트: `jwt_blacklist:{token}`

### Trip Service
- 여행 목록: `trips:list:{user_id}:{page}`
- 여행 상세: `trip:detail:{trip_id}`
- 일정: `trip:schedule:{trip_id}`
- 인기 여행: `trips:popular:{period}`

### AI Service
- 작업 상태: `ai:job:{job_id}`
- 생성 일정: `ai:schedule:{trip_id}`
- 추천 결과: `ai:recommendations:{user_id}:{type}`
- 모델 응답: `ai:model:response:{hash}`

### Location Service
- 장소 상세: `place:detail:{place_id}`
- 검색 결과: `search:places:{query}:{filters}`
- 추천 장소: `places:recommended:{category}:{location}`
- 인기 장소: `places:popular:{region}:{period}`

## 애플리케이션 연결 설정 예시

### Spring Boot (Lettuce)
```yaml
spring:
  redis:
    host: user-redis-service.tripgen-dev.svc.cluster.local
    port: 6379
    lettuce:
      pool:
        max-active: 10
        max-idle: 5
        min-idle: 1
```

### Node.js (ioredis)
```javascript
const Redis = require('ioredis');

const redis = new Redis({
  host: 'trip-redis-service.tripgen-dev.svc.cluster.local',
  port: 6380,
  maxRetriesPerRequest: 3,
  reconnectOnError: (err) => {
    return err.message.includes('READONLY');
  }
});
```

### Python (redis-py)
```python
import redis

r = redis.Redis(
    host='ai-redis-service.tripgen-dev.svc.cluster.local',
    port=6381,
    decode_responses=True,
    max_connections=10
)
```

## 모니터링 명령어

### 메모리 사용량 확인
```bash
kubectl exec -it deploy/user-db -n tripgen-dev -- redis-cli -h user-redis-service -p 6379 info memory | grep used_memory_human
```

### 연결된 클라이언트 수
```bash
kubectl exec -it deploy/user-db -n tripgen-dev -- redis-cli -h trip-redis-service -p 6380 info clients | grep connected_clients
```

### 키 개수 확인
```bash
kubectl exec -it deploy/user-db -n tripgen-dev -- redis-cli -h ai-redis-service -p 6381 dbsize
```

## 성능 최적화 설정

### 이미 적용된 설정
- `maxmemory-policy allkeys-lru`: 메모리 부족 시 LRU 정책 적용
- `save ""`: 디스크 저장 비활성화 (메모리 전용)
- `tcp-keepalive 300`: TCP 연결 유지
- `timeout 0`: 클라이언트 타임아웃 비활성화

### 권장 TTL 설정
- 세션 데이터: 30분
- API 응답 캐시: 5분
- 검색 결과: 10분
- 통계 데이터: 1시간

## 문제 해결 가이드

### Redis 연결 불가 시
1. Pod 상태 확인: `kubectl get pods -n tripgen-dev | grep redis`
2. Service 확인: `kubectl get svc -n tripgen-dev | grep redis`
3. 로그 확인: `kubectl logs deploy/{service}-redis -n tripgen-dev`

### 메모리 부족 시
1. 현재 사용량 확인
2. 불필요한 키 삭제: `redis-cli FLUSHDB`
3. TTL 정책 재검토
4. 메모리 할당량 증가 고려

## 백업 참고사항
- 개발환경은 메모리 전용으로 재시작 시 데이터 소실
- 중요 데이터는 PostgreSQL에 저장
- 운영환경에서는 Redis Persistence 설정 필요