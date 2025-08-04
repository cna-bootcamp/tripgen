# User Service 캐시 개발환경 가이드

## 개요

User Service를 위한 Redis 7.0 기반 캐시 시스템 개발환경 구축 가이드입니다.
인메모리 캐시로 세션 관리, 프로필 캐시, 로그인 제한, JWT 블랙리스트 처리를 담당합니다.

## 사전 요구사항

- Docker 및 Docker Compose 설치
- Kubernetes 클러스터 (개발용)
- kubectl CLI 도구 설치
- Redis CLI 도구 (테스트용)

## Redis Pod 설치 (Kubernetes)

### 1. ConfigMap 생성

```yaml
# user-redis-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: user-redis-config
  namespace: tripgen-dev
data:
  redis.conf: |
    # Redis 7.0 설정
    port 6379
    bind 0.0.0.0
    protected-mode no
    
    # 메모리 설정
    maxmemory 512mb
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
```

### 2. Service 생성

```yaml
# user-redis-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: user-redis-service
  namespace: tripgen-dev
  labels:
    app: user-redis
spec:
  selector:
    app: user-redis
  ports:
    - port: 6379
      targetPort: 6379
      protocol: TCP
  type: ClusterIP
```

### 3. Deployment 생성

```yaml
# user-redis-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-redis
  namespace: tripgen-dev
  labels:
    app: user-redis
spec:
  replicas: 1
  selector:
    matchLabels:
      app: user-redis
  template:
    metadata:
      labels:
        app: user-redis
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
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "300m"
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
          name: user-redis-config
```

### 4. 배포 실행

```bash
# Namespace 생성 (없는 경우)
kubectl create namespace tripgen-dev

# Redis 캐시 배포
kubectl apply -f user-redis-config.yaml
kubectl apply -f user-redis-service.yaml
kubectl apply -f user-redis-deployment.yaml

# 배포 상태 확인
kubectl get pods -n tripgen-dev -l app=user-redis
kubectl get services -n tripgen-dev -l app=user-redis
```

## 캐시 키 규칙 및 네이밍 컨벤션

### 키 네이밍 패턴
```
user:{category}:{identifier}:{sub-key}
```

### 캐시 키 예시

#### 1. 세션 관리
```
user:session:{session_id}                    # 세션 정보
user:session:by_user:{user_id}              # 사용자별 세션 목록
```

#### 2. 프로필 캐시
```
user:profile:{user_id}                       # 사용자 프로필
user:profile:summary:{user_id}               # 프로필 요약
```

#### 3. 로그인 제한
```
user:login:attempt:{email}                   # 로그인 시도 횟수
user:login:lock:{email}                      # 계정 잠금 상태
```

#### 4. JWT 블랙리스트
```
user:jwt:blacklist:{token_jti}               # 블랙리스트 토큰
user:jwt:refresh:{user_id}                   # 리프레시 토큰
```

## TTL 정책

### 기본 TTL 설정

```yaml
# TTL 정책 (초 단위)
session_ttl: 1800        # 30분 - 세션
profile_ttl: 3600        # 1시간 - 프로필
login_limit_ttl: 900     # 15분 - 로그인 제한
jwt_blacklist_ttl: 86400 # 24시간 - JWT 블랙리스트
```

### Redis 명령어 예시

```bash
# 세션 저장 (30분 TTL)
SET user:session:abc123 '{"userId":1,"email":"user@example.com"}' EX 1800

# 프로필 캐시 (1시간 TTL)
SET user:profile:1 '{"id":1,"name":"홍길동","email":"user@example.com"}' EX 3600

# 로그인 시도 제한 (15분 TTL)
SET user:login:attempt:user@example.com 3 EX 900

# JWT 블랙리스트 (24시간 TTL)
SET user:jwt:blacklist:jti123 "revoked" EX 86400
```

## 연결 테스트

### 1. Pod 내부에서 테스트

```bash
# Redis Pod에 접속
kubectl exec -it deployment/user-redis -n tripgen-dev -- redis-cli

# 기본 연결 테스트
127.0.0.1:6379> PING
PONG

# 데이터 저장/조회 테스트
127.0.0.1:6379> SET test:key "hello"
OK
127.0.0.1:6379> GET test:key
"hello"
127.0.0.1:6379> DEL test:key
(integer) 1
```

### 2. 외부에서 포트 포워딩 테스트

```bash
# 포트 포워딩
kubectl port-forward service/user-redis-service 6379:6379 -n tripgen-dev

# 로컬에서 Redis CLI 연결
redis-cli -h localhost -p 6379

# 성능 테스트
redis-cli -h localhost -p 6379 --latency-history -i 1
```

### 3. 애플리케이션 연결 설정

```yaml
# application-dev.yml
spring:
  redis:
    host: user-redis-service.tripgen-dev.svc.cluster.local
    port: 6379
    timeout: 2000
    jedis:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: 1000
```

## 캐시 워밍 전략

### 1. 세션 워밍 (애플리케이션 시작 시)

```bash
# 활성 세션 복원 (DB에서 조회 후 캐시에 저장)
# 주로 애플리케이션 초기화 시 수행
```

### 2. 프로필 워밍 (자주 조회되는 사용자)

```bash
# VIP 사용자 또는 자주 조회되는 프로필 미리 로드
# 스케줄링 작업으로 주기적 수행
```

### 3. 시스템 리소스 모니터링

```bash
# Redis 메모리 사용량 모니터링
kubectl exec -it deployment/user-redis -n tripgen-dev -- redis-cli INFO memory

# 키 통계 확인
kubectl exec -it deployment/user-redis -n tripgen-dev -- redis-cli INFO keyspace

# 캐시 히트율 모니터링
kubectl exec -it deployment/user-redis -n tripgen-dev -- redis-cli INFO stats
```

## 장애 대응

### 1. 캐시 장애 시 폴백 전략
- 세션: DB 기반 세션 관리로 자동 전환
- 프로필: DB에서 직접 조회
- 로그인 제한: 임시적으로 제한 해제
- JWT: 임시적으로 검증 생략

### 2. 캐시 복구 절차
```bash
# Pod 재시작
kubectl rollout restart deployment/user-redis -n tripgen-dev

# 캐시 워밍 스크립트 실행
# (애플리케이션별 구현)
```

## 개발 환경 설정 완료 체크리스트

- [ ] Redis Pod 정상 배포 확인
- [ ] Service 연결성 확인
- [ ] 애플리케이션 연결 설정 완료
- [ ] TTL 정책 적용 확인
- [ ] 캐시 키 네이밍 규칙 적용
- [ ] 기본 테스트 데이터 입력 확인
- [ ] 모니터링 대시보드 연결 (선택사항)

## 참고사항

- 개발환경이므로 데이터 영속성 비활성화
- 메모리 최적화를 위한 LRU 정책 적용
- 프로덕션 배포 시 Redis Cluster 구성 권장
- 보안을 위해 AUTH 설정 고려 (프로덕션)