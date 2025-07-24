# Redis 설치 가이드

물리 아키텍처에서 요구하는 Redis 캐시 서버 설치 방법을 안내합니다.

## Redis 캐시 전략 (물리 아키텍처 기준)
- 프로파일 데이터: TTL 24시간
- 장소 정보: TTL 1시간  
- 검색 결과: TTL 10분
- Eviction Policy: allkeys-lru
- Redis 버전: 7.0+

---

## 1. Docker 설치 방법

### docker-compose.yml 작성
```yaml
version: '3.8'

services:
  redis:
    image: redis:7.0-alpine
    container_name: tripgen-redis
    command: redis-server /usr/local/etc/redis/redis.conf
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
      - ./redis.conf:/usr/local/etc/redis/redis.conf
    networks:
      - tripgen-network
    restart: unless-stopped

volumes:
  redis_data:

networks:
  tripgen-network:
    driver: bridge
```

### redis.conf 작성
```conf
# 네트워크 설정
bind 0.0.0.0
protected-mode yes
port 6379

# 보안 설정
requirepass Hi5Jessica!

# 메모리 설정 (1GB)
maxmemory 1gb
maxmemory-policy allkeys-lru

# 지속성 설정
save 900 1
save 300 10
save 60 10000
stop-writes-on-bgsave-error yes
rdbcompression yes
rdbchecksum yes
dbfilename dump.rdb
dir ./

# 로깅
loglevel notice
logfile ""

# 성능 최적화
tcp-backlog 511
timeout 0
tcp-keepalive 300
databases 16

# 스레드 I/O (Redis 6+)
io-threads 4
io-threads-do-reads yes
```

### 실행 방법
```bash
# Docker Compose 실행
docker-compose up -d

# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f redis
```

### 접속 테스트
```bash
# Redis CLI로 접속
docker exec -it tripgen-redis redis-cli

# 인증
AUTH Hi5Jessica!

# 테스트 데이터 저장
SET test:key "Hello Redis"
GET test:key

# TTL 테스트
SET profile:user:123 "user data" EX 86400  # 24시간
SET location:seoul "location data" EX 3600  # 1시간
SET search:result:abc "search data" EX 600  # 10분

# TTL 확인
TTL profile:user:123
TTL location:seoul
TTL search:result:abc

# 메모리 정보
INFO memory

# 종료
QUIT
```

---

## 2. Kubernetes (k8s) 설치 방법

### 작업 디렉토리 생성
```bash
mkdir -p ~/install/redis && cd ~/install/redis
```

### values.yaml 작성
```yaml
# Redis 아키텍처 (실습시 standalone 권장)
architecture: standalone  # production에서는 replication 사용

# 인증 설정
auth:
  enabled: true
  password: "Hi5Jessica!"

# Master 설정
master:
  persistence:
    enabled: true
    storageClass: "managed-premium"
    size: 8Gi

  # Redis 설정
  configuration: |
    # 메모리 설정
    maxmemory 1gb
    maxmemory-policy allkeys-lru
    
    # 지속성 설정
    appendonly yes
    appendfsync everysec
    save 900 1 300 10 60 10000
    
    # 성능 최적화
    tcp-backlog 511
    timeout 0
    tcp-keepalive 300
    
    # 스레드 I/O
    io-threads 4
    io-threads-do-reads yes
    
  resources:
    limits:
      memory: "1.5Gi"
      cpu: "1"
    requests:
      memory: "1Gi"
      cpu: "0.5"

# 서비스 설정
service:
  type: ClusterIP
  ports:
    redis: 6379

# 보안 설정
securityContext:
  enabled: true
  fsGroup: 1001
  runAsUser: 1001

# 메트릭 (선택사항)
metrics:
  enabled: false  # production에서는 true 권장
```

### Helm 설치
```bash
# namespace 생성 (PostgreSQL과 동일 namespace 사용)
kubectl config set-context --current --namespace=tripgen-db

# Redis 설치
helm upgrade -i redis -f values.yaml bitnami/redis --version 18.4.0

# 설치 상태 확인
kubectl get pods -w
```

### 외부 접속용 Service 생성
redis-external.yaml 작성:
```yaml
apiVersion: v1
kind: Service
metadata:
  name: redis-external
  namespace: tripgen-db
spec:
  type: LoadBalancer
  ports:
  - name: tcp-redis
    port: 6379
    protocol: TCP
    targetPort: redis
  selector:
    app.kubernetes.io/instance: redis
    app.kubernetes.io/name: redis
    app.kubernetes.io/component: master
```

```bash
# Service 생성
kubectl apply -f redis-external.yaml

# LoadBalancer IP 확인
kubectl get svc redis-external
```

### 접속 테스트
```bash
# Pod 내부에서 테스트
kubectl exec -it redis-master-0 -- redis-cli

# 인증
AUTH Hi5Jessica!

# 테스트 데이터
SET test "K8s Redis Test"
GET test

# 정보 확인
INFO server
INFO memory

# 종료
QUIT
```

---

## 3. 애플리케이션 연결 설정

### Spring Boot application.yml 예시
```yaml
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    port: 6379
    password: Hi5Jessica!
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 10
        max-idle: 8
        min-idle: 0
        max-wait: -1ms

# 캐시별 TTL 설정
cache:
  profile:
    ttl: 86400  # 24시간
  location:
    ttl: 3600   # 1시간
  search:
    ttl: 600    # 10분
```

### 연결 문자열
```
# Docker
redis://:Hi5Jessica!@localhost:6379

# Kubernetes (LoadBalancer IP 사용)
redis://:Hi5Jessica!@{EXTERNAL_IP}:6379
```

---

## 4. 캐시 사용 예시

### Spring Boot 캐시 설정
```java
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
    
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> {
            builder
                .withCacheConfiguration("profile",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration("location",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofHours(1)))
                .withCacheConfiguration("search",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10)));
        };
    }
}
```

---

## 5. 모니터링 및 유지보수

### 메모리 사용량 확인
```bash
# Docker
docker exec tripgen-redis redis-cli -a Hi5Jessica! INFO memory

# Kubernetes  
kubectl exec redis-master-0 -- redis-cli -a Hi5Jessica! INFO memory
```

### 키 패턴 분석
```bash
# 모든 키 확인 (주의: production에서는 SCAN 사용)
redis-cli -a Hi5Jessica! --scan --pattern "*"

# 특정 패턴 키 개수
redis-cli -a Hi5Jessica! --scan --pattern "profile:*" | wc -l
redis-cli -a Hi5Jessica! --scan --pattern "location:*" | wc -l
redis-cli -a Hi5Jessica! --scan --pattern "search:*" | wc -l
```

### 캐시 초기화
```bash
# 특정 패턴 키 삭제
redis-cli -a Hi5Jessica! --scan --pattern "search:*" | xargs redis-cli -a Hi5Jessica! DEL

# 전체 캐시 초기화 (주의!)
redis-cli -a Hi5Jessica! FLUSHDB
```

### 백업
```bash
# Docker
docker exec tripgen-redis redis-cli -a Hi5Jessica! BGSAVE

# Kubernetes
kubectl exec redis-master-0 -- redis-cli -a Hi5Jessica! BGSAVE
```

---

## 6. 트러블슈팅

### 메모리 부족
```bash
# 현재 메모리 사용량 확인
redis-cli -a Hi5Jessica! INFO memory | grep used_memory_human

# Eviction 정책 확인
redis-cli -a Hi5Jessica! CONFIG GET maxmemory-policy

# 수동으로 메모리 정리
redis-cli -a Hi5Jessica! MEMORY DOCTOR
```

### 연결 문제
```bash
# 연결 테스트
redis-cli -h {HOST} -p 6379 -a Hi5Jessica! PING

# 연결 수 확인
redis-cli -a Hi5Jessica! INFO clients
```