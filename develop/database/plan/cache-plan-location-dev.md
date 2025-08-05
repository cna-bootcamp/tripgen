# Location Service 캐시 개발환경 가이드

## 개요

Location Service를 위한 Redis 7.0 기반 캐시 시스템 개발환경 구축 가이드입니다.
인메모리 캐시로 장소 상세정보, 검색 결과, 추천 장소의 빠른 조회를 지원합니다.

## 사전 요구사항

- Docker 및 Docker Compose 설치
- Kubernetes 클러스터 (개발용)
- kubectl CLI 도구 설치
- Redis CLI 도구 (테스트용)

## Redis Pod 설치 (Kubernetes)

### 1. ConfigMap 생성

```yaml
# location-redis-config.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: location-redis-config
  namespace: tripgen-dev
data:
  redis.conf: |
    # Redis 7.0 설정
    port 6379
    bind 0.0.0.0
    protected-mode no
    
    # 메모리 설정 (위치 데이터 캐시용)
    maxmemory 1.5gb
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
    
    # 지리 데이터 최적화 설정
    hash-max-ziplist-entries 512
    hash-max-ziplist-value 64
    set-max-intset-entries 512
    zset-max-ziplist-entries 128
    zset-max-ziplist-value 64
    
    # 검색 최적화를 위한 메모리 정책
    lazyfree-lazy-eviction yes
    lazyfree-lazy-expire yes
```

### 2. Service 생성

```yaml
# location-redis-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: location-redis-service
  namespace: tripgen-dev
  labels:
    app: location-redis
spec:
  selector:
    app: location-redis
  ports:
    - port: 6379
      targetPort: 6379
      protocol: TCP
  type: ClusterIP
```

### 3. Deployment 생성

```yaml
# location-redis-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: location-redis
  namespace: tripgen-dev
  labels:
    app: location-redis
spec:
  replicas: 1
  selector:
    matchLabels:
      app: location-redis
  template:
    metadata:
      labels:
        app: location-redis
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
            memory: "768Mi"
            cpu: "250m"
          limits:
            memory: "1.5Gi"
            cpu: "600m"
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
          name: location-redis-config
```

### 4. 배포 실행

```bash
# Namespace 생성 (없는 경우)
kubectl create namespace tripgen-dev

# Redis 캐시 배포
kubectl apply -f location-redis-config.yaml
kubectl apply -f location-redis-service.yaml
kubectl apply -f location-redis-deployment.yaml

# 배포 상태 확인
kubectl get pods -n tripgen-dev -l app=location-redis
kubectl get services -n tripgen-dev -l app=location-redis
```

## 캐시 키 규칙 및 네이밍 컨벤션

### 키 네이밍 패턴
```
location:{category}:{identifier}:{sub-key}
```

### 캐시 키 예시

#### 1. 장소 상세정보
```
location:place:{place_id}                    # 장소 상세 정보
location:place:{place_id}:summary           # 장소 요약 정보
location:place:{place_id}:photos            # 장소 사진 목록
location:place:{place_id}:reviews           # 장소 리뷰 요약
location:place:{place_id}:hours             # 운영시간 정보
```

#### 2. 검색 결과
```
location:search:{query_hash}                 # 검색 결과
location:search:keyword:{keyword}            # 키워드별 검색
location:search:category:{category}          # 카테고리별 검색
location:search:nearby:{lat}:{lng}:{radius}  # 주변 장소 검색
```

#### 3. 추천 장소
```
location:recommend:popular:{city}            # 인기 장소 추천
location:recommend:category:{category}       # 카테고리별 추천
location:recommend:user:{user_id}            # 개인화 추천
location:recommend:trip:{trip_type}          # 여행 타입별 추천
```

#### 4. 지리 정보
```
location:geo:coords:{place_id}               # 좌표 정보
location:geo:address:{place_id}              # 주소 정보
location:geo:distance:{from}:{to}            # 거리 정보
location:geo:route:{start}:{end}             # 경로 정보
```

## TTL 정책

### 기본 TTL 설정

```yaml
# TTL 정책 (초 단위)
place_detail_ttl: 3600      # 1시간 - 장소 상세정보
search_result_ttl: 1800     # 30분 - 검색 결과
recommend_ttl: 7200         # 2시간 - 추천 장소
geo_info_ttl: 21600         # 6시간 - 지리 정보
popular_places_ttl: 14400   # 4시간 - 인기 장소
```

### Redis 명령어 예시

```bash
# 장소 상세정보 저장 (1시간 TTL)
HSET location:place:123 name "경복궁" category "관광지" rating 4.5 address "서울시 종로구"
EXPIRE location:place:123 3600

# 검색 결과 저장 (30분 TTL)
SET location:search:hash123 '[{"id":1,"name":"경복궁","distance":100}]' EX 1800

# 추천 장소 저장 (2시간 TTL)
ZADD location:recommend:popular:seoul 4.8 "경복궁" 4.7 "창덕궁" 4.6 "덕수궁"
EXPIRE location:recommend:popular:seoul 7200

# 지리 정보 저장 (6시간 TTL)
GEOADD location:geo:seoul 126.977 37.578 "경복궁" 126.991 37.579 "창덕궁"
EXPIRE location:geo:seoul 21600
```

## 연결 테스트

### 1. Pod 내부에서 테스트

```bash
# Redis Pod에 접속
kubectl exec -it deployment/location-redis -n tripgen-dev -- redis-cli

# 기본 연결 테스트
127.0.0.1:6379> PING
PONG

# 지리 데이터 테스트
127.0.0.1:6379> GEOADD test:locations 126.977 37.578 "경복궁"
(integer) 1
127.0.0.1:6379> GEOPOS test:locations "경복궁"
1) 1) "126.97699934244155884"
   2) "37.57800003956299982"
127.0.0.1:6379> DEL test:locations
(integer) 1
```

### 2. 외부에서 포트 포워딩 테스트

```bash
# 포트 포워딩
kubectl port-forward service/location-redis-service 6382:6379 -n tripgen-dev

# 로컬에서 Redis CLI 연결
redis-cli -h localhost -p 6382

# 지리 검색 테스트
redis-cli -h localhost -p 6382
127.0.0.1:6382> GEOADD seoul 126.977 37.578 "경복궁" 126.991 37.579 "창덕궁"
127.0.0.1:6382> GEORADIUS seoul 126.980 37.580 1000 m WITHDIST
```

### 3. 애플리케이션 연결 설정

```yaml
# application-dev.yml
spring:
  redis:
    host: location-redis-service.tripgen-dev.svc.cluster.local
    port: 6379
    timeout: 3000
    jedis:
      pool:
        max-active: 40
        max-idle: 15
        min-idle: 8
        max-wait: 2000
```

## 캐시 워밍 전략

### 1. 인기 장소 워밍 (스케줄링)

```bash
# 도시별 인기 장소 순위 미리 계산
# 매 4시간마다 갱신
```

### 2. 지리 데이터 워밍

```bash
# 주요 도시의 관광지 좌표 정보 미리 로드
# 애플리케이션 시작 시 수행
```

### 3. 검색 패턴 기반 워밍

```bash
# 자주 검색되는 키워드 결과 미리 캐시
# 검색 로그 분석 후 배치 작업으로 수행
```

### 4. 시스템 리소스 모니터링

```bash
# Redis 메모리 사용량 모니터링
kubectl exec -it deployment/location-redis -n tripgen-dev -- redis-cli INFO memory

# 지리 데이터 확인
kubectl exec -it deployment/location-redis -n tripgen-dev -- redis-cli INFO keyspace

# 위치 관련 키 확인
kubectl exec -it deployment/location-redis -n tripgen-dev -- redis-cli KEYS "location:*"

# 지리 인덱스 확인
kubectl exec -it deployment/location-redis -n tripgen-dev -- redis-cli KEYS "*geo*"
```

## 지리 데이터 특화 캐시 전략

### 1. Redis GEO 활용

```java
// 주변 장소 검색
public List<Place> getNearbyPlaces(double lat, double lng, double radiusKm) {
    String geoKey = "location:geo:" + getRegionCode(lat, lng);
    
    // Redis GEO 명령어 사용
    Set<GeoLocation<String>> locations = redisTemplate.opsForGeo()
        .radius(geoKey, new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS)))
        .getContent();
    
    return locations.stream()
        .map(loc -> getPlaceFromCache(loc.getName()))
        .collect(Collectors.toList());
}
```

### 2. 검색 결과 해시 캐싱

```java
// 검색 쿼리 해시 기반 캐싱
public List<Place> searchPlaces(SearchQuery query) {
    String queryHash = generateQueryHash(query);
    String cacheKey = "location:search:" + queryHash;
    
    String cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
        return deserializePlaces(cached);
    }
    
    List<Place> results = performSearch(query);
    redisTemplate.opsForValue().set(cacheKey, serialize(results), Duration.ofMinutes(30));
    
    return results;
}
```

### 3. 계층적 캐싱 구조

```java
// 도시 > 구/군 > 동/면 계층 구조
public Place getPlaceDetail(Long placeId) {
    String detailKey = "location:place:" + placeId;
    String summaryKey = "location:place:" + placeId + ":summary";
    
    // 요약 정보 먼저 확인
    String summary = redisTemplate.opsForValue().get(summaryKey);
    if (summary != null) {
        Place place = deserializePlace(summary);
        if (isDetailRequired()) {
            // 필요시 상세 정보 로드
            String detail = redisTemplate.opsForValue().get(detailKey);
            if (detail != null) {
                return mergeDetailInfo(place, detail);
            }
        }
        return place;
    }
    
    return loadFromDatabase(placeId);
}
```

## 성능 최적화 전략

### 1. 공간 인덱싱
- Redis GEO를 활용한 효율적인 공간 검색
- 지역별 분할을 통한 인덱스 최적화
- 계층적 지리 데이터 구조

### 2. 검색 최적화
- 자주 검색되는 키워드 우선 캐싱
- 검색 결과 페이지네이션 캐싱
- 타이핑 자동완성을 위한 Trie 구조

### 3. 메모리 효율성
- 장소 데이터 압축 저장
- 사용 빈도에 따른 차등 TTL 적용
- 배치 삭제를 통한 메모리 정리

## 장애 대응

### 1. 캐시 장애 시 폴백 전략
- 장소 상세: 외부 API (Google Places, 카카오 로컬) 호출
- 검색 결과: 데이터베이스 기반 검색으로 전환
- 추천 장소: 정적 추천 목록 사용
- 지리 정보: PostGIS 기반 공간 쿼리 활용

### 2. 캐시 복구 절차
```bash
# Pod 재시작
kubectl rollout restart deployment/location-redis -n tripgen-dev

# 핵심 지리 데이터 우선 복구
# 주요 도시 관광지 좌표 정보 먼저 로드
```

### 3. 데이터 정합성 확인
```bash
# 외부 API와 캐시 데이터 비교
# 좌표 정보 불일치 발견 시 캐시 무효화
```

## 외부 API 연동 캐시 전략

### 1. Google Places API 결과 캐싱

```java
// Google Places API 응답 캐싱
public PlaceDetail getPlaceFromGoogle(String placeId) {
    String cacheKey = "location:google:" + placeId;
    
    String cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
        return deserialize(cached);
    }
    
    PlaceDetail result = googlePlacesAPI.getPlaceDetail(placeId);
    redisTemplate.opsForValue().set(cacheKey, serialize(result), Duration.ofHours(6));
    
    return result;
}
```

### 2. 카카오 로컬 API 캐싱

```java
// 카카오 검색 결과 캐싱
public List<Place> searchFromKakao(String keyword, double lat, double lng) {
    String cacheKey = "location:kakao:search:" + keyword + ":" + lat + ":" + lng;
    
    String cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
        return deserializePlaces(cached);
    }
    
    List<Place> results = kakaoLocalAPI.searchPlaces(keyword, lat, lng);
    redisTemplate.opsForValue().set(cacheKey, serialize(results), Duration.ofMinutes(30));
    
    return results;
}
```

## 개발 환경 설정 완료 체크리스트

- [ ] Redis Pod 정상 배포 확인
- [ ] Service 연결성 확인
- [ ] 애플리케이션 연결 설정 완료
- [ ] TTL 정책 적용 확인
- [ ] 캐시 키 네이밍 규칙 적용
- [ ] 지리 데이터 테스트 확인
- [ ] 검색 기능 테스트 확인
- [ ] 외부 API 연동 테스트
- [ ] 캐시 워밍 전략 구현 확인

## 지리 데이터 특화 모니터링

### 1. 공간 쿼리 성능
```bash
# GEO 명령어 성능 모니터링
INFO commandstats

# 지리 인덱스 크기 확인
MEMORY USAGE location:geo:seoul
```

### 2. 검색 패턴 분석
```bash
# 자주 검색되는 키워드 Top 10
ZREVRANGE search:keywords:popular 0 9 WITHSCORES

# 지역별 검색 빈도
ZREVRANGE search:regions:popular 0 9 WITHSCORES
```

### 3. 캐시 효율성 메트릭
```bash
# 히트율 분석
INFO stats

# 키 만료 통계
INFO keyspace
```

## 참고사항

- 지리 데이터 특성상 메모리 용량 1.5GB 할당
- Redis GEO 명령어를 활용한 공간 검색 최적화
- 외부 API 의존성을 줄이기 위한 적극적 캐싱
- 프로덕션에서는 지역별 Redis 샤딩 고려
- 실시간 위치 기반 서비스를 위한 스트리밍 데이터 처리 준비