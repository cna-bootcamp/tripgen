# Trip Service 캐시 개발환경 가이드

## 개요

Trip Service를 위한 Redis 7.0 기반 캐시 시스템 개발환경 구축 가이드입니다.
인메모리 캐시로 여행 목록, 상세정보, 일정 데이터의 빠른 조회를 지원합니다.

## 사전 요구사항

- Docker 및 Docker Compose 설치
- Kubernetes 클러스터 (개발용)
- kubectl CLI 도구 설치
- Redis CLI 도구 (테스트용)

## Redis Pod 설치 (Kubernetes)

### 1. ConfigMap 생성

```yaml
# trip-redis-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: trip-redis-config
  namespace: tripgen-dev
data:
  redis.conf: |
    # Redis 7.0 설정
    port 6379
    bind 0.0.0.0
    protected-mode no
    
    # 메모리 설정
    maxmemory 1gb
    maxmemory-policy allkeys-lru
    
    # 영속성 비활성화 (개발환경)
    save ""
    appendonly no
    
    # 로그 설정
    loglevel notice
    logfile ""
    
    # 성능 최적화
    tcp-keepalive 300
    timeout 0
    
    # 압축 설정 (여행 데이터 최적화)
    hash-max-ziplist-entries 512
    hash-max-ziplist-value 64
```

### 2. Service 생성

```yaml
# trip-redis-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: trip-redis-service
  namespace: tripgen-dev
  labels:
    app: trip-redis
spec:
  selector:
    app: trip-redis
  ports:
    - port: 6379
      targetPort: 6379
      protocol: TCP
  type: ClusterIP
```

### 3. Deployment 생성

```yaml
# trip-redis-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: trip-redis
  namespace: tripgen-dev
  labels:
    app: trip-redis
spec:
  replicas: 1
  selector:
    matchLabels:
      app: trip-redis
  template:
    metadata:
      labels:
        app: trip-redis
    spec:
      containers:
      - name: redis
        image: redis:7.0-alpine
        ports:
        - containerPort: 6379
        volumeMounts:
        - name: redis-config
          mountPath: /usr/local/etc/redis/redis.conf
          subPath: redis.conf
        command:
        - redis-server
        - /usr/local/etc/redis/redis.conf
        resources:
          requests:
            memory: "512Mi"
            cpu: "200m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          tcpSocket:
            port: 6379
          initialDelaySeconds: 30
          periodSeconds: 5
        readinessProbe:
          exec:
            command:
            - redis-cli
            - ping
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: redis-config
        configMap:
          name: trip-redis-config
```

### 4. 배포 실행

```bash
# Namespace 생성 (없는 경우)
kubectl create namespace tripgen-dev

# Redis 캐시 배포
kubectl apply -f trip-redis-config.yaml
kubectl apply -f trip-redis-service.yaml
kubectl apply -f trip-redis-deployment.yaml

# 배포 상태 확인
kubectl get pods -n tripgen-dev -l app=trip-redis
kubectl get services -n tripgen-dev -l app=trip-redis
```

## 캐시 키 규칙 및 네이밍 컨벤션

### 키 네이밍 패턴
```
trip:{category}:{identifier}:{sub-key}
```

### 캐시 키 예시

#### 1. 여행 목록
```
trip:list:user:{user_id}                     # 사용자별 여행 목록
trip:list:user:{user_id}:page:{page}         # 페이징된 여행 목록
trip:list:popular                            # 인기 여행 목록
trip:list:recent                             # 최근 여행 목록
```

#### 2. 여행 상세정보
```
trip:detail:{trip_id}                        # 여행 상세 정보
trip:detail:{trip_id}:summary               # 여행 요약 정보
trip:detail:{trip_id}:participants         # 참가자 목록
trip:detail:{trip_id}:budget               # 예산 정보
```

#### 3. 여행 일정
```
trip:schedule:{trip_id}                      # 전체 일정
trip:schedule:{trip_id}:day:{day}           # 일별 일정
trip:schedule:{trip_id}:place:{place_id}   # 장소별 일정
```

#### 4. 여행 통계
```
trip:stats:user:{user_id}                   # 사용자 여행 통계
trip:stats:monthly:{year}:{month}           # 월별 여행 통계
```

## TTL 정책

### 기본 TTL 설정

```yaml
# TTL 정책 (초 단위)
trip_list_ttl: 300      # 5분 - 여행 목록
trip_detail_ttl: 600    # 10분 - 여행 상세정보  
trip_schedule_ttl: 600  # 10분 - 여행 일정
trip_stats_ttl: 1800    # 30분 - 여행 통계
popular_ttl: 3600       # 1시간 - 인기 여행
```

### Redis 명령어 예시

```bash
# 여행 목록 저장 (5분 TTL)
SET trip:list:user:1 '[{"id":1,"title":"제주도 여행","startDate":"2024-06-01"}]' EX 300

# 여행 상세 정보 (10분 TTL)
HSET trip:detail:1 title "제주도 3박4일" description "가족 여행" status "planning"
EXPIRE trip:detail:1 600

# 일별 일정 저장 (10분 TTL)
SET trip:schedule:1:day:1 '[{"time":"09:00","place":"제주공항","activity":"도착"}]' EX 600

# 인기 여행 목록 (1시간 TTL)
SET trip:list:popular '[{"id":1,"title":"제주도","views":1000}]' EX 3600
```

## 연결 테스트

### 1. Pod 내부에서 테스트

```bash
# Redis Pod에 접속
kubectl exec -it deployment/trip-redis -n tripgen-dev -- redis-cli

# 기본 연결 테스트
127.0.0.1:6379> PING
PONG

# 여행 데이터 테스트
127.0.0.1:6379> SET trip:test:1 '{"title":"테스트 여행","destination":"서울"}'
OK
127.0.0.1:6379> GET trip:test:1
"{\"title\":\"테스트 여행\",\"destination\":\"서울\"}"
127.0.0.1:6379> DEL trip:test:1
(integer) 1
```

### 2. 외부에서 포트 포워딩 테스트

```bash
# 포트 포워딩
kubectl port-forward service/trip-redis-service 6380:6379 -n tripgen-dev

# 로컬에서 Redis CLI 연결
redis-cli -h localhost -p 6380

# 성능 테스트
redis-cli -h localhost -p 6380 --latency-history -i 1
```

### 3. 애플리케이션 연결 설정

```yaml
# application-dev.yml
spring:
  redis:
    host: trip-redis-service.tripgen-dev.svc.cluster.local
    port: 6379
    timeout: 3000
    jedis:
      pool:
        max-active: 30
        max-idle: 15
        min-idle: 8
        max-wait: 2000
```

## 캐시 워밍 전략

### 1. 인기 여행 워밍 (스케줄링)

```bash
# 매 시간마다 인기 여행 목록 갱신
# 스케줄러로 DB에서 조회 후 캐시에 저장
```

### 2. 사용자별 여행 목록 워밍

```bash
# 활성 사용자의 여행 목록을 미리 캐시에 로드
# 로그인 시점에 백그라운드로 수행
```

### 3. 최근 조회된 여행 상세정보 유지

```bash
# 자주 조회되는 여행 상세정보 TTL 연장
# 조회 시마다 TTL 갱신 로직 적용
```

### 4. 시스템 리소스 모니터링

```bash
# Redis 메모리 사용량 모니터링
kubectl exec -it deployment/trip-redis -n tripgen-dev -- redis-cli INFO memory

# 키 통계 확인
kubectl exec -it deployment/trip-redis -n tripgen-dev -- redis-cli INFO keyspace

# 히트율 모니터링
kubectl exec -it deployment/trip-redis -n tripgen-dev -- redis-cli INFO stats

# 여행 관련 키 확인
kubectl exec -it deployment/trip-redis -n tripgen-dev -- redis-cli KEYS "trip:*"
```

## 캐시 전략별 구현 패턴

### 1. Cache-Aside 패턴 (읽기 전용)

```java
// 여행 목록 조회
public List<Trip> getTripList(Long userId) {
    String cacheKey = "trip:list:user:" + userId;
    String cached = redisTemplate.opsForValue().get(cacheKey);
    
    if (cached != null) {
        return parseTrips(cached);
    }
    
    List<Trip> trips = tripRepository.findByUserId(userId);
    redisTemplate.opsForValue().set(cacheKey, serialize(trips), Duration.ofMinutes(5));
    return trips;
}
```

### 2. Write-Through 패턴 (데이터 수정)

```java
// 여행 정보 수정
public Trip updateTrip(Long tripId, Trip tripData) {
    Trip updatedTrip = tripRepository.save(tripData);
    
    // 캐시 업데이트
    String detailKey = "trip:detail:" + tripId;
    redisTemplate.opsForValue().set(detailKey, serialize(updatedTrip), Duration.ofMinutes(10));
    
    // 관련 캐시 무효화
    String listKey = "trip:list:user:" + updatedTrip.getUserId();
    redisTemplate.delete(listKey);
    
    return updatedTrip;
}
```

## 장애 대응

### 1. 캐시 장애 시 폴백 전략
- 여행 목록: DB에서 직접 조회 (성능 저하 허용)
- 상세정보: DB 조회 후 응답 (실시간 처리)
- 일정: DB 기반 조회로 자동 전환
- 통계: 캐시된 데이터가 없으면 계산 생략

### 2. 캐시 복구 절차
```bash
# Pod 재시작
kubectl rollout restart deployment/trip-redis -n tripgen-dev

# 캐시 워밍 실행
# 인기 여행, 최근 여행 데이터 우선 로드
```

### 3. 데이터 정합성 체크
```bash
# 캐시-DB 데이터 비교 스크립트 실행
# 불일치 발견 시 캐시 무효화
```

## 개발 환경 설정 완료 체크리스트

- [ ] Redis Pod 정상 배포 확인
- [ ] Service 연결성 확인  
- [ ] 애플리케이션 연결 설정 완료
- [ ] TTL 정책 적용 확인
- [ ] 캐시 키 네이밍 규칙 적용
- [ ] 여행 테스트 데이터 입력 확인
- [ ] 캐시 워밍 전략 구현 확인
- [ ] 장애 대응 시나리오 테스트

## 성능 최적화 팁

### 1. 메모리 최적화
- Hash 자료구조 활용으로 메모리 절약
- JSON 직렬화 대신 MessagePack 사용 고려
- 불필요한 필드 제거 후 캐시 저장

### 2. 네트워크 최적화  
- Pipeline 사용으로 여러 명령어 배치 처리
- Connection Pool 설정 최적화
- 압축 전송 활용

### 3. 캐시 효율성
- 자주 조회되는 데이터 TTL 연장
- 덜 중요한 데이터 TTL 단축
- 캐시 히트율 모니터링 및 조정

## 참고사항

- 개발환경이므로 데이터 영속성 비활성화
- 여행 데이터 특성상 메모리 용량 1GB 할당
- 프로덕션 환경에서는 Redis Sentinel 구성 권장
- 대용량 여행 데이터 처리를 위한 샤딩 고려