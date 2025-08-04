# AI Service 캐시 개발환경 가이드

## 개요

AI Service를 위한 Redis 7.0 기반 캐시 시스템 개발환경 구축 가이드입니다.
인메모리 캐시로 AI 작업 상태, 생성된 일정, 추천 결과의 빠른 처리를 지원합니다.

## 사전 요구사항

- Docker 및 Docker Compose 설치
- Kubernetes 클러스터 (개발용)
- kubectl CLI 도구 설치
- Redis CLI 도구 (테스트용)

## Redis Pod 설치 (Kubernetes)

### 1. ConfigMap 생성

```yaml
# ai-redis-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ai-redis-config
  namespace: tripgen-dev
data:
  redis.conf: |
    # Redis 7.0 설정
    port 6379
    bind 0.0.0.0
    protected-mode no
    
    # 메모리 설정 (AI 결과 캐시용)
    maxmemory 2gb
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
    
    # AI 결과 최적화 설정
    hash-max-ziplist-entries 512
    hash-max-ziplist-value 64
    list-max-ziplist-size 8
    list-compress-depth 0
    
    # Pub/Sub 설정 (실시간 상태 업데이트)
    notify-keyspace-events Ex
```

### 2. Service 생성

```yaml
# ai-redis-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: ai-redis-service
  namespace: tripgen-dev
  labels:
    app: ai-redis
spec:
  selector:
    app: ai-redis
  ports:
    - port: 6379
      targetPort: 6379
      protocol: TCP
  type: ClusterIP
```

### 3. Deployment 생성

```yaml
# ai-redis-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-redis
  namespace: tripgen-dev
  labels:
    app: ai-redis
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ai-redis
  template:
    metadata:
      labels:
        app: ai-redis
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
            memory: "1Gi"
            cpu: "300m"
          limits:
            memory: "2Gi"
            cpu: "800m"
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
          name: ai-redis-config
```

### 4. 배포 실행

```bash
# Namespace 생성 (없는 경우)
kubectl create namespace tripgen-dev

# Redis 캐시 배포
kubectl apply -f ai-redis-config.yaml
kubectl apply -f ai-redis-service.yaml
kubectl apply -f ai-redis-deployment.yaml

# 배포 상태 확인
kubectl get pods -n tripgen-dev -l app=ai-redis
kubectl get services -n tripgen-dev -l app=ai-redis
```

## 캐시 키 규칙 및 네이밍 컨벤션

### 키 네이밍 패턴
```
ai:{category}:{identifier}:{sub-key}
```

### 캐시 키 예시

#### 1. AI 작업 상태
```
ai:job:{job_id}                              # AI 작업 상태
ai:job:user:{user_id}                        # 사용자별 작업 목록  
ai:job:queue:pending                         # 대기 중인 작업 큐
ai:job:queue:processing                      # 처리 중인 작업 큐
```

#### 2. 생성된 일정
```
ai:schedule:result:{job_id}                  # AI 생성 일정 결과
ai:schedule:trip:{trip_id}                   # 여행별 AI 일정
ai:schedule:template:{template_id}           # 일정 템플릿
ai:schedule:draft:{user_id}:{trip_id}        # 임시 저장 일정
```

#### 3. AI 추천 결과
```
ai:recommend:place:{user_id}                 # 장소 추천 결과
ai:recommend:restaurant:{location_id}        # 음식점 추천
ai:recommend:activity:{preferences}          # 활동 추천
ai:recommend:route:{start}:{end}             # 경로 추천
```

#### 4. AI 모델 캐시
```
ai:model:response:{hash}                     # 모델 응답 캐시
ai:model:embedding:{content_hash}            # 임베딩 캐시
ai:model:prompt:{template_id}                # 프롬프트 템플릿
```

## TTL 정책

### 기본 TTL 설정

```yaml
# TTL 정책 (초 단위)
job_status_ttl: 300         # 5분 - AI 작업 상태
schedule_result_ttl: 3600   # 1시간 - 생성된 일정
recommend_ttl: 1800         # 30분 - 추천 결과  
model_response_ttl: 7200    # 2시간 - 모델 응답 캐시
embedding_ttl: 18600        # 24시간 - 임베딩 캐시
prompt_ttl: 86400           # 24시간 - 프롬프트 템플릿
```

### Redis 명령어 예시

```bash
# AI 작업 상태 저장 (5분 TTL)
HSET ai:job:job123 status "processing" progress 45 startTime "2024-06-01T10:00:00Z"
EXPIRE ai:job:job123 300

# 생성된 일정 저장 (1시간 TTL)
SET ai:schedule:result:job123 '{"days":[{"day":1,"places":["제주공항","성산일출봉"]}]}' EX 3600

# 장소 추천 결과 (30분 TTL)
SET ai:recommend:place:user1 '[{"name":"한라산","score":0.95},{"name":"성산일출봉","score":0.88}]' EX 1800

# 모델 응답 캐시 (2시간 TTL)
SET ai:model:response:hash123abc '{"itinerary":"detailed_plan_here","confidence":0.92}' EX 7200
```

## 연결 테스트

### 1. Pod 내부에서 테스트

```bash
# Redis Pod에 접속
kubectl exec -it deployment/ai-redis -n tripgen-dev -- redis-cli

# 기본 연결 테스트
127.0.0.1:6379> PING
PONG

# AI 작업 상태 테스트
127.0.0.1:6379> HSET ai:job:test123 status "processing" progress 50
(integer) 2
127.0.0.1:6379> HGET ai:job:test123 status
"processing"
127.0.0.1:6379> DEL ai:job:test123
(integer) 1
```

### 2. 외부에서 포트 포워딩 테스트

```bash
# 포트 포워딩
kubectl port-forward service/ai-redis-service 6381:6379 -n tripgen-dev

# 로컬에서 Redis CLI 연결
redis-cli -h localhost -p 6381

# Pub/Sub 테스트 (AI 작업 상태 실시간 알림)
# Terminal 1
redis-cli -h localhost -p 6381
127.0.0.1:6381> SUBSCRIBE ai:job:status

# Terminal 2  
redis-cli -h localhost -p 6381
127.0.0.1:6381> PUBLISH ai:job:status '{"jobId":"123","status":"completed"}'
```

### 3. 애플리케이션 연결 설정

```yaml
# application-dev.yml
spring:
  redis:
    host: ai-redis-service.tripgen-dev.svc.cluster.local
    port: 6379
    timeout: 5000
    jedis:
      pool:
        max-active: 50
        max-idle: 20
        min-idle: 10
        max-wait: 3000
```

## 캐시 워밍 전략

### 1. AI 모델 캐시 워밍

```bash
# 자주 사용되는 프롬프트 템플릿 미리 로드
# 애플리케이션 시작 시 수행
```

### 2. 인기 여행지 추천 결과 워밍

```bash
# 인기 여행지에 대한 추천 결과 미리 생성
# 스케줄러로 주기적 갱신
```

### 3. 임베딩 캐시 워밍

```bash
# 자주 검색되는 키워드의 임베딩 미리 계산
# 배치 작업으로 수행
```

### 4. 시스템 리소스 모니터링

```bash
# Redis 메모리 사용량 모니터링
kubectl exec -it deployment/ai-redis -n tripgen-dev -- redis-cli INFO memory

# AI 관련 키 통계
kubectl exec -it deployment/ai-redis -n tripgen-dev -- redis-cli INFO keyspace

# 작업 큐 상태 확인
kubectl exec -it deployment/ai-redis -n tripgen-dev -- redis-cli LLEN ai:job:queue:pending

# AI 작업 상태 모니터링
kubectl exec -it deployment/ai-redis -n tripgen-dev -- redis-cli KEYS "ai:job:*"
```

## AI 작업 흐름별 캐시 전략

### 1. 일정 생성 요청 처리

```java
// 비동기 AI 작업 시작
public String startItineraryGeneration(ItineraryRequest request) {
    String jobId = UUID.randomUUID().toString();
    
    // 작업 상태 초기화
    Map<String, String> jobInfo = Map.of(
        "status", "pending",
        "progress", "0",
        "startTime", Instant.now().toString(),
        "userId", request.getUserId().toString()
    );
    
    redisTemplate.opsForHash().putAll("ai:job:" + jobId, jobInfo);
    redisTemplate.expire("ai:job:" + jobId, Duration.ofMinutes(5));
    
    // 작업 큐에 추가
    redisTemplate.opsForList().rightPush("ai:job:queue:pending", jobId);
    
    return jobId;
}
```

### 2. 실시간 진행 상태 업데이트

```java
// AI 작업 진행률 업데이트
public void updateJobProgress(String jobId, int progress, String message) {
    String jobKey = "ai:job:" + jobId;
    
    redisTemplate.opsForHash().put(jobKey, "progress", String.valueOf(progress));
    redisTemplate.opsForHash().put(jobKey, "message", message);
    redisTemplate.opsForHash().put(jobKey, "updateTime", Instant.now().toString());
    
    // 실시간 알림 발송
    redisTemplate.convertAndSend("ai:job:status", 
        Map.of("jobId", jobId, "progress", progress, "message", message));
}
```

### 3. 결과 캐싱 및 조회

```java
// AI 생성 결과 저장
public void saveItineraryResult(String jobId, Itinerary result) {
    String resultKey = "ai:schedule:result:" + jobId;
    String jobKey = "ai:job:" + jobId;
    
    // 결과 저장 (1시간 TTL)
    redisTemplate.opsForValue().set(resultKey, serialize(result), Duration.ofHours(1));
    
    // 작업 상태 완료로 변경
    redisTemplate.opsForHash().put(jobKey, "status", "completed");
    redisTemplate.opsForHash().put(jobKey, "resultKey", resultKey);
}
```

## 장애 대응

### 1. 캐시 장애 시 폴백 전략
- 작업 상태: DB 기반 상태 관리로 전환
- 생성 결과: 임시 파일 시스템 사용
- 추천 결과: 기본 추천 알고리즘 사용
- 모델 캐시: 직접 AI API 호출

### 2. 캐시 복구 절차
```bash
# Pod 재시작
kubectl rollout restart deployment/ai-redis -n tripgen-dev

# 진행 중인 AI 작업 복구
# DB에서 incomplete 상태 작업 조회 후 캐시 복원
```

### 3. 데이터 일관성 보장
```bash
# AI 작업 상태와 DB 동기화 확인
# 불일치 발견 시 캐시 무효화 후 DB 기준으로 복구
```

## AI 특화 캐시 최적화

### 1. 메모리 효율성
- 대용량 AI 결과 압축 저장
- 중복 제거를 위한 해시 기반 캐싱
- LRU 정책으로 오래된 결과 자동 삭제

### 2. 성능 최적화
- 임베딩 벡터 캐싱으로 계산 비용 절약
- 프롬프트 템플릿 재사용
- 배치 처리를 위한 파이프라인 활용

### 3. 실시간 처리
- Pub/Sub을 활용한 상태 변경 알림
- 스트림을 이용한 작업 로그 관리
- 키 만료 이벤트 활용

## 개발 환경 설정 완료 체크리스트

- [ ] Redis Pod 정상 배포 확인
- [ ] Service 연결성 확인
- [ ] 애플리케이션 연결 설정 완료
- [ ] TTL 정책 적용 확인
- [ ] 캐시 키 네이밍 규칙 적용
- [ ] AI 작업 테스트 시나리오 확인
- [ ] Pub/Sub 실시간 알림 테스트
- [ ] 캐시 워밍 전략 구현 확인
- [ ] 장애 대응 시나리오 테스트

## 모니터링 및 알림

### 1. 작업 큐 모니터링
```bash
# 대기 중인 작업 수
LLEN ai:job:queue:pending

# 처리 중인 작업 수  
LLEN ai:job:queue:processing

# 완료된 작업 통계
KEYS ai:job:* | grep completed | wc -l
```

### 2. 성능 메트릭
```bash
# 평균 응답 시간
INFO latencystats

# 메모리 사용 패턴
INFO memory

# 캐시 히트율
INFO stats
```

## 참고사항

- AI 작업 특성상 메모리 용량 2GB 할당
- 대용량 결과 데이터 처리를 위한 압축 활용
- 실시간 상태 업데이트를 위한 Pub/Sub 활용
- 프로덕션에서는 Redis Cluster 구성 필수
- GPU 리소스와 연동된 캐시 정책 고려