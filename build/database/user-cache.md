# User 서비스 캐시 설치 가이드 - 개발환경

## 1. 개요

### 1.1 서비스 정보
- **서비스명**: User Service Cache
- **캐시 타입**: Redis 7.2 (Alpine)
- **데이터베이스 번호**: 1
- **컨테이너**: redis.tripgen-dev.svc.cluster.local
- **포트**: 6379
- **영속성**: 비활성화 (개발환경)

### 1.2 캐시 전략
- **주요 사용 사례**:
  - JWT 토큰 블랙리스트
  - 사용자 세션 관리
  - 로그인 시도 추적
  - 사용자 프로필 캐싱
  - 권한 캐싱

## 2. 설치 단계

### 2.1 Redis 구성
User 서비스용 Redis 구성 생성:

```bash
# Redis 구성용 ConfigMap 생성
kubectl create configmap user-redis-config \
  --from-literal=redis.conf="
# User 서비스용 Redis 구성
port 6379
bind 0.0.0.0
protected-mode yes
requirepass UserCacheDev2025!

# 메모리 관리
maxmemory 200mb
maxmemory-policy allkeys-lru

# 개발환경에서는 영속성 비활성화
save \"\"
appendonly no

# 데이터베이스 선택
databases 16

# 연결 설정
timeout 300
tcp-keepalive 60

# 로깅
loglevel notice
" \
  -n tripgen-dev
```

### 2.2 캐시 키 패턴
User 서비스용 캐시 키 패턴 정의:

```yaml
# user-cache-keys.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-cache-keys
  namespace: tripgen-dev
data:
  cache-keys.properties: |
    # JWT 토큰 블랙리스트
    jwt.blacklist.pattern=jwt:blacklist:{jti}
    jwt.blacklist.ttl=86400
    
    # 사용자 세션
    session.pattern=session:{userId}
    session.ttl=3600
    
    # 로그인 시도
    login.attempts.pattern=login:attempts:{username}
    login.attempts.ttl=1800
    
    # 사용자 프로필 캐시
    user.profile.pattern=user:profile:{userId}
    user.profile.ttl=600
    
    # 사용자 권한 캐시
    user.permissions.pattern=user:permissions:{userId}
    user.permissions.ttl=300
    
    # 이메일 검증 토큰
    email.verification.pattern=email:verify:{token}
    email.verification.ttl=3600
    
    # 패스워드 재설정 토큰
    password.reset.pattern=password:reset:{token}
    password.reset.ttl=900
```

### 2.3 Redis 데이터베이스 초기화
초기화 스크립트 생성:

```bash
# user-redis-init.sh
#!/bin/bash

echo "User 서비스 Redis 캐시 초기화 중..."

# 패스워드로 Redis에 연결
redis-cli -h redis.tripgen-dev.svc.cluster.local -a UserCacheDev2025! <<EOF
# User 서비스용 데이터베이스 1 선택
SELECT 1

# 기존 데이터 삭제 (개발용)
FLUSHDB

# 초기 구성 키 설정
SET config:service:name "user-service"
SET config:cache:version "1.0.0"
SET config:initialized:at "$(date -u +%Y-%m-%dT%H:%M:%SZ)"

# 개발용 테스트 데이터 생성
# 테스트 사용자 세션
SETEX session:550e8400-e29b-41d4-a716-446655440001 3600 '{
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "username": "testuser1",
  "roles": ["USER"],
  "loginTime": "2025-01-30T10:00:00Z",
  "ipAddress": "127.0.0.1"
}'

# 테스트 사용자 프로필 캐시
SETEX user:profile:550e8400-e29b-41d4-a716-446655440001 600 '{
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "username": "testuser1",
  "name": "테스트 사용자 1",
  "email": "test1@tripgen.com",
  "avatarUrl": null,
  "status": "ACTIVE"
}'

# 테스트 권한 캐시
SETEX user:permissions:550e8400-e29b-41d4-a716-446655440001 300 '["VIEW_TRIPS", "CREATE_TRIP", "EDIT_OWN_TRIP", "DELETE_OWN_TRIP"]'

# 캐시 통계 표시
INFO keyspace
EOF

echo "User 서비스 Redis 캐시 초기화 완료."
```

### 2.4 초기화 Job 생성
```yaml
# user-redis-init-job.yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: user-redis-init
  namespace: tripgen-dev
spec:
  template:
    spec:
      restartPolicy: Never
      containers:
      - name: redis-init
        image: redis:7.2-alpine
        resources:
          requests:
            cpu: 100m
            memory: 64Mi
          limits:
            cpu: 200m
            memory: 128Mi
        command: ["/scripts/init.sh"]
        volumeMounts:
        - name: init-script
          mountPath: /scripts
      volumes:
      - name: init-script
        configMap:
          name: user-redis-init-script
          defaultMode: 0755
```

### 2.5 서비스 연결 구성
User 서비스에 다음 환경 변수 추가:

```yaml
# Redis 연결 환경 변수
env:
- name: REDIS_HOST
  value: redis.tripgen-dev.svc.cluster.local
- name: REDIS_PORT
  value: "6379"
- name: REDIS_PASSWORD
  valueFrom:
    secretKeyRef:
      name: user-redis-secret
      key: password
- name: REDIS_DATABASE
  value: "1"
- name: REDIS_SSL_ENABLED
  value: "false"
- name: REDIS_TIMEOUT
  value: "5000"
- name: REDIS_CONNECTION_POOL_SIZE
  value: "10"
```

## 3. 캐시 구현 가이드

### 3.1 Spring Boot 구성
```yaml
# application-dev.yml
spring:
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    database: ${REDIS_DATABASE}
    timeout: ${REDIS_TIMEOUT}
    lettuce:
      pool:
        max-active: 10
        max-idle: 5
        min-idle: 2
        max-wait: -1ms
  cache:
    type: redis
    redis:
      time-to-live: 600000  # 10분 기본값
      cache-null-values: false
      use-key-prefix: true
      key-prefix: "user:"
```

### 3.2 캐시 사용 예제

#### JWT 토큰 블랙리스트
```java
// 블랙리스트에 토큰 추가
redisTemplate.opsForValue().set(
    "jwt:blacklist:" + jti,
    "true",
    Duration.ofSeconds(86400)
);

// 토큰이 블랙리스트에 있는지 확인
Boolean isBlacklisted = redisTemplate.hasKey("jwt:blacklist:" + jti);
```

#### 사용자 세션 관리
```java
// 사용자 세션 저장
String sessionKey = "session:" + userId;
redisTemplate.opsForValue().set(
    sessionKey,
    sessionData,
    Duration.ofHours(1)
);

// 사용자 세션 조회
UserSession session = redisTemplate.opsForValue().get(sessionKey);
```

#### 로그인 시도 추적
```java
// 로그인 시도 횟수 증가
String attemptKey = "login:attempts:" + username;
Long attempts = redisTemplate.opsForValue().increment(attemptKey);
redisTemplate.expire(attemptKey, Duration.ofMinutes(30));

// 로그인 시도 횟수 확인
String attempts = redisTemplate.opsForValue().get(attemptKey);
```

## 4. 모니터링 및 유지보수

### 4.1 캐시 사용량 모니터링
```bash
# Redis에 연결
kubectl exec -it redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025!

# User 서비스 데이터베이스 선택
SELECT 1

# 메모리 사용량 확인
INFO memory

# 키 개수 확인
DBSIZE

# 키 패턴 확인
KEYS *

# 실시간 명령 모니터링
MONITOR
```

### 4.2 캐시 통계
```bash
# 캐시 히트/미스 비율 확인
kubectl exec redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025! --stat

# 느린 쿼리 확인
kubectl exec redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025! SLOWLOG GET 10

# 클라이언트 연결 확인
kubectl exec redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025! CLIENT LIST
```

### 4.3 캐시 정리
```bash
# User 서비스 전체 캐시 삭제 (DB 1)
kubectl exec redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025! -n 1 FLUSHDB

# 특정 패턴 삭제
kubectl exec redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025! -n 1 --scan --pattern "session:*" | xargs redis-cli -a UserCacheDev2025! -n 1 DEL
```

## 5. 성능 튜닝

### 5.1 메모리 최적화
| 설정 | 값 | 설명 |
|------|-----|------|
| maxmemory | 200MB | User 서비스 캐시 최대 메모리 |
| maxmemory-policy | allkeys-lru | 가장 오래 사용하지 않은 키 제거 |
| maxmemory-samples | 5 | LRU 샘플 크기 |

### 5.2 연결 풀 설정
| 설정 | 값 | 설명 |
|------|-----|------|
| max-active | 10 | 최대 활성 연결 수 |
| max-idle | 5 | 최대 유휴 연결 수 |
| min-idle | 2 | 최소 유휴 연결 수 |
| max-wait | -1 | 대기 시간 제한 없음 |

## 6. 문제 해결

### 6.1 일반적인 문제

| 문제 | 원인 | 해결방법 |
|------|------|----------|
| 연결 타임아웃 | 네트워크 문제 또는 Redis 다운 | Pod 상태 및 네트워크 확인 |
| 메모리 부족 | 캐시 크기 초과 | maxmemory 조정 또는 제거 정책 변경 |
| 느린 쿼리 | 대용량 키 작업 | 키 구조 및 쿼리 최적화 |
| 높은 지연시간 | 네트워크 또는 CPU 문제 | 리소스 사용량 및 네트워크 확인 |

### 6.2 디버그 명령어
```bash
# Redis 로그 확인
kubectl logs redis-0 -n tripgen-dev

# Redis 구성 확인
kubectl exec redis-0 -n tripgen-dev -- redis-cli -a UserCacheDev2025! CONFIG GET "*"

# 연결 테스트
kubectl exec -it deployment/user-service -n tripgen-dev -- redis-cli -h redis.tripgen-dev.svc.cluster.local -a UserCacheDev2025! ping
```

## 7. 모범 사례

1. **키 네이밍**: 일관된 패턴 사용 (service:type:identifier)
2. **TTL 관리**: 모든 키에 적절한 TTL 설정
3. **에러 처리**: 폴백이 있는 캐시 어사이드 패턴 구현
4. **모니터링**: 메모리 사용량 및 히트율 정기 모니터링
5. **보안**: 강력한 패스워드 및 네트워크 정책 사용

## 8. 개발 참고사항

- 개발환경에서는 Redis 영속성이 비활성화됨
- Pod 재시작시 데이터가 손실됨
- GUI 접근을 위해 Redis Commander 또는 RedisInsight 사용 가능
- 프로덕션에서는 고가용성을 위해 Redis Sentinel 사용 고려