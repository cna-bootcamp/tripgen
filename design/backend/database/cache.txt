# 캐시 데이터베이스 설계서

## 1. 개요

### 1.1 목적
- 마이크로서비스 간 데이터 공유를 위한 캐시 계층
- 외부 API 호출 최소화 및 응답 시간 개선
- 서비스 간 결합도 최소화

### 1.2 캐시 전략
- **Cache Aside Pattern**: 애플리케이션이 캐시와 DB를 직접 관리
- **Write Through Pattern**: 캐시와 DB를 동시에 업데이트
- **TTL 기반**: Time-To-Live를 통한 자동 만료

### 1.3 캐시 저장소
- **Primary**: Redis Cluster (고가용성)
- **Backup**: Local Cache (Caffeine/Ehcache)

## 2. 캐시 키 설계

### 2.1 키 네이밍 규칙
```
{service}:{domain}:{identifier}:{version}
```

### 2.2 서비스별 키 패턴

#### 프로파일 서비스
```
profile:member:{memberId}:v1
profile:trip:{tripId}:v1
profile:transport:{tripId}:v1
profile:trip:members:{tripId}:v1
```

#### 일정 서비스
```
itinerary:schedule:{itineraryId}:v1
itinerary:activities:{itineraryId}:v1
itinerary:job:{jobId}:v1
itinerary:place:{placeId}:v1
itinerary:route:{fromPlaceId}:{toPlaceId}:v1
```

#### 장소 서비스
```
location:place:{placeId}:v1
location:search:{searchHash}:v1
location:nearby:{lat}:{lng}:{radius}:v1
location:region:{regionCode}:v1
location:translation:{textHash}:{targetLang}:v1
```

## 3. 캐시 데이터 구조

### 3.1 프로파일 서비스 캐시

#### Member 캐시
```json
{
  "key": "profile:member:{memberId}:v1",
  "data": {
    "id": "uuid",
    "name": "string",
    "age": "integer",
    "healthStatus": "enum",
    "preferences": ["array"],
    "lastModified": "timestamp"
  },
  "ttl": 3600
}
```

#### Trip 캐시
```json
{
  "key": "profile:trip:{tripId}:v1",
  "data": {
    "id": "uuid",
    "tripName": "string",
    "startDate": "date",
    "endDate": "date",
    "destination": {
      "country": "string",
      "city": "string",
      "address": "string"
    },
    "memberCount": "integer",
    "lastModified": "timestamp"
  },
  "ttl": 1800
}
```

### 3.2 일정 서비스 캐시

#### Place Info 캐시
```json
{
  "key": "itinerary:place:{placeId}:v1",
  "data": {
    "id": "string",
    "name": "string",
    "location": {
      "latitude": "double",
      "longitude": "double",
      "address": "string"
    },
    "category": "enum",
    "rating": "double",
    "businessHours": "object",
    "sourceService": "location",
    "cachedAt": "timestamp"
  },
  "ttl": 7200
}
```

#### Route 캐시
```json
{
  "key": "itinerary:route:{fromPlaceId}:{toPlaceId}:v1",
  "data": {
    "distance": "double",
    "duration": "integer",
    "transportType": "enum",
    "polyline": "string",
    "steps": ["array"],
    "calculatedAt": "timestamp"
  },
  "ttl": 14400
}
```

### 3.3 장소 서비스 캐시

#### Search Result 캐시
```json
{
  "key": "location:search:{searchHash}:v1",
  "data": {
    "query": "string",
    "location": "object",
    "radius": "integer",
    "results": [{
      "placeId": "string",
      "name": "string",
      "location": "object",
      "category": "string",
      "rating": "double"
    }],
    "searchedAt": "timestamp"
  },
  "ttl": 1800
}
```

#### Translation 캐시
```json
{
  "key": "location:translation:{textHash}:{targetLang}:v1",
  "data": {
    "originalText": "string",
    "translatedText": "string",
    "sourceLanguage": "string",
    "targetLanguage": "string",
    "confidence": "double",
    "translatedAt": "timestamp"
  },
  "ttl": 86400
}
```

## 4. TTL 전략

### 4.1 데이터 타입별 TTL

| 데이터 유형 | TTL (초) | 갱신 전략 |
|------------|----------|-----------|
| 회원 정보 | 3600 (1시간) | Write Through |
| 여행 정보 | 1800 (30분) | Write Through |
| 장소 기본 정보 | 7200 (2시간) | Cache Aside |
| 장소 상세 정보 | 3600 (1시간) | Cache Aside |
| 검색 결과 | 1800 (30분) | Cache Aside |
| 경로 정보 | 14400 (4시간) | Cache Aside |
| 번역 결과 | 86400 (24시간) | Cache Aside |
| 지역 정보 | 43200 (12시간) | Cache Aside |

### 4.2 동적 TTL 조정
- **인기도 기반**: 접근 빈도에 따른 TTL 조정
- **데이터 신선도**: 마지막 갱신 시간 고려
- **시간대별 조정**: 피크 시간 TTL 연장

## 5. 캐시 일관성 전략

### 5.1 캐시 무효화 패턴
- **즉시 무효화**: 데이터 변경 시 즉시 캐시 삭제
- **지연 무효화**: 배치 작업을 통한 일괄 무효화
- **태그 기반 무효화**: 관련 캐시 그룹 단위 무효화

### 5.2 데이터 동기화
```
# 트리거 기반 캐시 무효화
PUBLISH cache:invalidate profile:member:{memberId}:v1
PUBLISH cache:invalidate profile:trip:members:{tripId}:v1

# Lua 스크립트를 통한 원자적 캐시 업데이트
EVAL "
  local key = KEYS[1]
  local data = ARGV[1]
  local ttl = ARGV[2]
  redis.call('SET', key, data, 'EX', ttl)
  redis.call('PUBLISH', 'cache:updated', key)
  return 'OK'
" 1 profile:member:123:v1 '{"name":"John"}' 3600
```

## 6. 캐시 클러스터 구성

### 6.1 Redis Cluster 설정
```yaml
# Redis 클러스터 구성
redis-cluster:
  nodes:
    - redis-node-1:6379  # Master
    - redis-node-2:6379  # Master
    - redis-node-3:6379  # Master
    - redis-node-4:6379  # Slave
    - redis-node-5:6379  # Slave
    - redis-node-6:6379  # Slave
  
  configuration:
    cluster-enabled: yes
    cluster-config-file: nodes.conf
    cluster-node-timeout: 5000
    cluster-require-full-coverage: no
    maxmemory-policy: allkeys-lru
    maxmemory: 2gb
```

### 6.2 샤딩 전략
- **해시 슬롯**: 16384개 슬롯으로 데이터 분산
- **키 기반 샤딩**: 서비스별 키 패턴으로 분산
- **Hot Key 분산**: 자주 접근되는 키 분산 배치

## 7. 모니터링 및 메트릭

### 7.1 캐시 성능 메트릭
```
# Hit Ratio
cache.hit.ratio = cache_hits / (cache_hits + cache_misses)

# Response Time
cache.response.time.p95
cache.response.time.p99

# Memory Usage
cache.memory.used
cache.memory.available
cache.evicted.keys.count

# Network
cache.network.connections
cache.network.bytes.in
cache.network.bytes.out
```

### 7.2 알림 규칙
- Cache Hit Ratio < 80%
- Memory Usage > 85%
- Connection Count > 1000
- Network Latency > 10ms

## 8. 캐시 운영 전략

### 8.1 백업 및 복구
- **RDB Snapshot**: 매일 자정 전체 백업
- **AOF**: 실시간 로그 기반 백업
- **Cross-Region Replication**: 재해 복구용

### 8.2 캐시 워밍
```python
# 캐시 워밍 전략
def warm_cache_on_startup():
    # 1. 자주 사용되는 기본 데이터 로드
    preload_popular_places()
    preload_recent_trips()
    
    # 2. 예상 검색 패턴 사전 캐싱
    preload_common_searches()
    
    # 3. 정적 데이터 캐싱
    preload_region_info()
    preload_translation_pairs()
```

### 8.3 캐시 크기 최적화
- **데이터 압축**: JSON → MessagePack
- **필드 선택**: 필요한 필드만 캐싱
- **배치 처리**: 다중 키 한 번에 처리

## 9. 보안 및 접근 제어

### 9.1 인증 및 권한
- **Redis AUTH**: 패스워드 기반 인증
- **Network Isolation**: VPC 내부 접근만 허용
- **SSL/TLS**: 암호화된 통신

### 9.2 데이터 보호
- **민감 정보 제외**: 개인정보는 캐시하지 않음
- **데이터 마스킹**: 로그에서 민감 정보 마스킹
- **접근 로그**: 모든 캐시 접근 기록

## 10. 장애 대응 시나리오

### 10.1 캐시 장애 시 대응
1. **Fallback to Database**: 캐시 실패 시 DB 직접 조회
2. **Circuit Breaker**: 연속 실패 시 캐시 우회
3. **Graceful Degradation**: 필수 기능 우선 유지

### 10.2 복구 절차
1. Redis 노드 상태 확인
2. 데이터 일관성 검증
3. 점진적 트래픽 복구
4. 캐시 재워밍

## 11. 성능 튜닝

### 11.1 Redis 최적화
```conf
# redis.conf 최적화 설정
save 900 1
save 300 10
save 60 10000
stop-writes-on-bgsave-error no
rdbcompression yes
rdbchecksum yes
maxmemory-policy allkeys-lru
tcp-keepalive 300
timeout 0
```

### 11.2 애플리케이션 레벨 최적화
- **Connection Pooling**: 연결 재사용
- **Pipeline**: 다중 명령어 배치 처리
- **Async Operations**: 비동기 캐시 작업
- **Batch Operations**: MGET/MSET 활용