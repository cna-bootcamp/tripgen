# Trip 서비스 캐시 설치 가이드

## 1. 설치 개요

### 1.1 캐시 정보
- **캐시 시스템**: Redis 7.0+
- **데이터베이스 번호**: 2
- **포트**: 6379
- **설정**: 메모리 최적화 구성

### 1.2 캐시 용도
- 여행 목록 및 상세 정보 캐시
- 멤버/목적지/일정 데이터 캐시
- 세션 및 임시 데이터 저장
- 검색 결과 캐시

## 2. 사전 준비

### 2.1 Redis 설치 확인
```bash
# Redis 버전 확인
redis-server --version

# Redis 서비스 상태 확인
sudo systemctl status redis
```

### 2.2 Redis 설정 확인
```bash
# Redis 설정 파일 위치 확인
sudo find /etc -name "redis.conf" 2>/dev/null

# Redis 연결 테스트
redis-cli ping
```

## 3. Redis 기본 설정

### 3.1 메모리 설정
```bash
# redis.conf 파일 편집
sudo nano /etc/redis/redis.conf

# 다음 설정 추가/수정
maxmemory 512mb
maxmemory-policy allkeys-lru
timeout 300
tcp-keepalive 60
```

### 3.2 데이터베이스 설정
```bash
# 데이터베이스 개수 설정 (기본 16개)
databases 16

# AOF 활성화 (데이터 영속성)
appendonly yes
appendfsync everysec
```

### 3.3 보안 설정
```bash
# 인증 설정 (옵션)
requirepass TripServiceCache2025!

# 바인드 주소 설정
bind 127.0.0.1
```

## 4. Trip 서비스 캐시 구성

### 4.1 데이터베이스 2번 선택
```bash
# Redis CLI 접속
redis-cli

# 데이터베이스 2번 선택
SELECT 2

# 현재 데이터베이스 확인
CLIENT LIST
```

### 4.2 캐시 키 구조 정의
```bash
# 여행 목록 캐시 키 패턴
# trip:list:{user_id}:{status}:{page}

# 여행 상세 정보 캐시 키 패턴  
# trip:detail:{trip_id}

# 멤버 목록 캐시 키 패턴
# trip:members:{trip_id}

# 목적지 목록 캐시 키 패턴
# trip:destinations:{trip_id}

# 일정 캐시 키 패턴
# trip:schedules:{trip_id}:{day}

# 전체 일정 캐시 키 패턴
# trip:schedules:all:{trip_id}
```

## 5. 초기 캐시 데이터 설정

### 5.1 테스트 여행 목록 캐시
```bash
# Redis CLI에서 데이터베이스 2번 선택
redis-cli -n 2

# 사용자별 여행 목록 캐시 (TTL: 300초 = 5분)
SETEX "trip:list:user-001:PLANNING:1" 300 '{
  "trips": [
    {
      "tripId": "trip-001", 
      "tripName": "서울 3박 4일 여행",
      "status": "PLANNING",
      "progress": 80,
      "startDate": "2024-12-15",
      "endDate": "2024-12-18",
      "memberCount": 2,
      "destinationCount": 2,
      "createdAt": "2024-12-01T10:00:00Z"
    }
  ],
  "totalCount": 1,
  "currentPage": 1,
  "totalPages": 1
}'

SETEX "trip:list:user-002:ONGOING:1" 300 '{
  "trips": [
    {
      "tripId": "trip-002",
      "tripName": "부산 주말 여행", 
      "status": "ONGOING",
      "progress": 100,
      "startDate": "2024-12-20",
      "endDate": "2024-12-22",
      "memberCount": 1,
      "destinationCount": 1,
      "createdAt": "2024-12-18T15:30:00Z"
    }
  ],
  "totalCount": 1,
  "currentPage": 1,
  "totalPages": 1
}'
```

### 5.2 여행 상세 정보 캐시
```bash
# 여행 상세 정보 캐시 (TTL: 1800초 = 30분)
SETEX "trip:detail:trip-001" 1800 '{
  "tripId": "trip-001",
  "userId": "user-001", 
  "tripName": "서울 3박 4일 여행",
  "transportMode": "PUBLIC",
  "status": "PLANNING",
  "currentStep": "destination_setting",
  "startDate": "2024-12-15",
  "endDate": "2024-12-18",
  "progress": 80,
  "hasSchedule": false,
  "createdAt": "2024-12-01T10:00:00Z",
  "updatedAt": "2024-12-10T14:20:00Z"
}'

SETEX "trip:detail:trip-002" 1800 '{
  "tripId": "trip-002",
  "userId": "user-002",
  "tripName": "부산 주말 여행",
  "transportMode": "CAR", 
  "status": "ONGOING",
  "currentStep": "schedule_completed",
  "startDate": "2024-12-20",
  "endDate": "2024-12-22",
  "progress": 100,
  "hasSchedule": true,
  "createdAt": "2024-12-18T15:30:00Z",
  "updatedAt": "2024-12-20T09:00:00Z"
}'
```

### 5.3 멤버 목록 캐시
```bash
# 멤버 목록 캐시 (TTL: 1800초 = 30분)
SETEX "trip:members:trip-001" 1800 '[
  {
    "memberId": "member-001",
    "tripId": "trip-001",
    "name": "김서울",
    "age": 28,
    "gender": "FEMALE", 
    "healthStatus": "EXCELLENT",
    "preferences": ["SIGHTSEEING", "CULTURE", "SHOPPING"]
  },
  {
    "memberId": "member-002", 
    "tripId": "trip-001",
    "name": "이서울",
    "age": 32,
    "gender": "MALE",
    "healthStatus": "GOOD", 
    "preferences": ["CULTURE", "NATURE"]
  }
]'

SETEX "trip:members:trip-002" 1800 '[
  {
    "memberId": "member-003",
    "tripId": "trip-002", 
    "name": "박부산",
    "age": 25,
    "gender": "MALE",
    "healthStatus": "EXCELLENT",
    "preferences": ["NATURE", "SPORTS", "REST"]
  }
]'
```

### 5.4 목적지 목록 캐시
```bash
# 목적지 목록 캐시 (TTL: 1800초 = 30분)
SETEX "trip:destinations:trip-001" 1800 '[
  {
    "destinationId": "dest-001",
    "tripId": "trip-001", 
    "destinationName": "명동",
    "nights": 2,
    "startDate": "2024-12-15",
    "endDate": "2024-12-16", 
    "accommodation": "롯데호텔 서울",
    "checkInTime": "15:00",
    "checkOutTime": "11:00",
    "orderSeq": 1
  },
  {
    "destinationId": "dest-002",
    "tripId": "trip-001",
    "destinationName": "강남", 
    "nights": 1,
    "startDate": "2024-12-17",
    "endDate": "2024-12-17",
    "accommodation": "강남 비즈니스 호텔",
    "checkInTime": "15:00", 
    "checkOutTime": "12:00",
    "orderSeq": 2
  }
]'

SETEX "trip:destinations:trip-002" 1800 '[
  {
    "destinationId": "dest-003",
    "tripId": "trip-002",
    "destinationName": "해운대",
    "nights": 1, 
    "startDate": "2024-12-20",
    "endDate": "2024-12-20",
    "accommodation": "해운대 그랜드 호텔",
    "checkInTime": "14:00",
    "checkOutTime": "11:00",
    "orderSeq": 1
  }
]'
```

### 5.5 일정 캐시
```bash
# 일별 일정 캐시 (TTL: 3600초 = 1시간)
SETEX "trip:schedules:trip-001:1" 3600 '{
  "scheduleId": "schedule-001",
  "tripId": "trip-001",
  "day": 1,
  "date": "2024-12-15", 
  "city": "서울",
  "weatherCondition": "Sunny",
  "minTemperature": -2.5,
  "maxTemperature": 5.8,
  "weatherIcon": "sun",
  "places": [
    {
      "placeId": "LOC-001",
      "placeName": "경복궁",
      "category": "TOURIST", 
      "startTime": "09:00",
      "duration": 120,
      "transportType": "PUBLIC",
      "transportDuration": 30,
      "transportDistance": 5.2,
      "orderSeq": 1,
      "accessibility": ["ELEVATOR", "RAMP"]
    },
    {
      "placeId": "LOC-002",
      "placeName": "인사동 맛집",
      "category": "RESTAURANT",
      "startTime": "12:30", 
      "duration": 90,
      "transportType": "WALK",
      "transportDuration": 15,
      "transportDistance": 1.2,
      "orderSeq": 2,
      "accessibility": ["WHEELCHAIR"]
    }
  ]
}'

SETEX "trip:schedules:trip-002:1" 3600 '{
  "scheduleId": "schedule-002", 
  "tripId": "trip-002",
  "day": 1,
  "date": "2024-12-20",
  "city": "부산",
  "weatherCondition": "Clear",
  "minTemperature": 3.2,
  "maxTemperature": 12.5,
  "weatherIcon": "clear",
  "places": [
    {
      "placeId": "LOC-003",
      "placeName": "해운대 해수욕장", 
      "category": "TOURIST",
      "startTime": "10:00",
      "duration": 180,
      "transportType": "CAR",
      "transportDuration": 20,
      "transportDistance": 8.5,
      "orderSeq": 1,
      "accessibility": ["RAMP"]
    }
  ]
}'
```

### 5.6 전체 일정 캐시
```bash
# 전체 일정 캐시 (TTL: 3600초 = 1시간)
SETEX "trip:schedules:all:trip-001" 3600 '[
  {
    "scheduleId": "schedule-001",
    "day": 1,
    "date": "2024-12-15",
    "city": "서울", 
    "weatherCondition": "Sunny",
    "placesCount": 2
  },
  {
    "scheduleId": "schedule-002", 
    "day": 2,
    "date": "2024-12-16",
    "city": "서울",
    "weatherCondition": "Cloudy", 
    "placesCount": 0
  }
]'

SETEX "trip:schedules:all:trip-002" 3600 '[
  {
    "scheduleId": "schedule-003",
    "day": 1,
    "date": "2024-12-20", 
    "city": "부산",
    "weatherCondition": "Clear",
    "placesCount": 1
  }
]'
```

## 6. 캐시 성능 최적화

### 6.1 메모리 사용량 모니터링
```bash
# 메모리 정보 확인
redis-cli -n 2 INFO memory

# 키 개수 확인
redis-cli -n 2 DBSIZE

# 특정 패턴 키 검색
redis-cli -n 2 KEYS "trip:*"
```

### 6.2 TTL 관리
```bash
# TTL 확인
redis-cli -n 2 TTL "trip:detail:trip-001"

# TTL 설정
redis-cli -n 2 EXPIRE "trip:detail:trip-001" 1800

# 만료된 키 확인
redis-cli -n 2 KEYS "*" | while read key; do 
  ttl=$(redis-cli -n 2 TTL "$key")
  if [ "$ttl" = "-2" ]; then
    echo "Expired key: $key"
  fi
done
```

### 6.3 캐시 통계 확인
```bash
# 캐시 히트율 확인
redis-cli -n 2 INFO stats | grep keyspace_hits
redis-cli -n 2 INFO stats | grep keyspace_misses

# 초당 명령 처리 수 확인
redis-cli -n 2 INFO stats | grep instantaneous_ops_per_sec
```

## 7. 캐시 무효화 스크립트

### 7.1 여행 관련 캐시 무효화
```bash
#!/bin/bash
# trip_cache_invalidate.sh - 여행 관련 캐시 무효화 스크립트

TRIP_ID=$1
USER_ID=$2

if [ -z "$TRIP_ID" ]; then
    echo "Usage: $0 <trip_id> [user_id]"
    exit 1
fi

# 여행 상세 정보 캐시 삭제
redis-cli -n 2 DEL "trip:detail:$TRIP_ID"

# 멤버 목록 캐시 삭제  
redis-cli -n 2 DEL "trip:members:$TRIP_ID"

# 목적지 목록 캐시 삭제
redis-cli -n 2 DEL "trip:destinations:$TRIP_ID"

# 일정 캐시 삭제 (모든 일차)
redis-cli -n 2 KEYS "trip:schedules:$TRIP_ID:*" | xargs redis-cli -n 2 DEL

# 전체 일정 캐시 삭제
redis-cli -n 2 DEL "trip:schedules:all:$TRIP_ID"

# 사용자 여행 목록 캐시 삭제 (모든 상태, 모든 페이지)
if [ -n "$USER_ID" ]; then
    redis-cli -n 2 KEYS "trip:list:$USER_ID:*" | xargs redis-cli -n 2 DEL
fi

echo "Cache invalidated for trip: $TRIP_ID"
```

### 7.2 사용자별 캐시 무효화
```bash
#!/bin/bash
# user_trip_cache_invalidate.sh - 사용자별 여행 캐시 무효화 스크립트

USER_ID=$1

if [ -z "$USER_ID" ]; then
    echo "Usage: $0 <user_id>"
    exit 1
fi

# 사용자의 모든 여행 목록 캐시 삭제
redis-cli -n 2 KEYS "trip:list:$USER_ID:*" | xargs redis-cli -n 2 DEL

echo "Trip list cache invalidated for user: $USER_ID"
```

## 8. 캐시 백업 및 복원

### 8.1 캐시 백업 스크립트
```bash
#!/bin/bash
# trip_cache_backup.sh - Trip 캐시 백업 스크립트

BACKUP_DIR="/backup/trip-cache"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/trip_cache_backup_$DATE.rdb"

# 백업 디렉토리 생성
mkdir -p $BACKUP_DIR

# Redis 데이터베이스 2번 백업
redis-cli -n 2 --rdb $BACKUP_FILE

# 백업 파일 압축
gzip $BACKUP_FILE

echo "Trip cache backup completed: ${BACKUP_FILE}.gz"
```

### 8.2 캐시 복원 스크립트
```bash
#!/bin/bash
# trip_cache_restore.sh - Trip 캐시 복원 스크립트

if [ -z "$1" ]; then
    echo "Usage: $0 <backup_file>"
    exit 1
fi

BACKUP_FILE=$1

# 백업 파일 압축 해제 (필요한 경우)
if [[ $BACKUP_FILE == *.gz ]]; then
    gunzip $BACKUP_FILE
    BACKUP_FILE=${BACKUP_FILE%.gz}
fi

# Redis 서비스 중지
sudo systemctl stop redis

# RDB 파일 복사
sudo cp $BACKUP_FILE /var/lib/redis/dump.rdb
sudo chown redis:redis /var/lib/redis/dump.rdb

# Redis 서비스 시작
sudo systemctl start redis

echo "Trip cache restore completed from: $BACKUP_FILE"
```

## 9. 모니터링 및 알람

### 9.1 성능 모니터링 스크립트
```bash
#!/bin/bash
# trip_cache_monitor.sh - Trip 캐시 모니터링 스크립트

echo "=== Trip Cache Monitoring Report ==="
echo "Date: $(date)"
echo

# 데이터베이스 2번 선택하여 모니터링
echo "1. Database Info:"
redis-cli -n 2 INFO keyspace | grep db2

echo "2. Memory Usage:"
redis-cli -n 2 INFO memory | grep used_memory_human

echo "3. Cache Performance:"
HITS=$(redis-cli -n 2 INFO stats | grep keyspace_hits | cut -d: -f2 | tr -d '\r')
MISSES=$(redis-cli -n 2 INFO stats | grep keyspace_misses | cut -d: -f2 | tr -d '\r') 
TOTAL=$((HITS + MISSES))
if [ $TOTAL -gt 0 ]; then
    HIT_RATE=$(echo "scale=2; $HITS * 100 / $TOTAL" | bc)
    echo "Hit Rate: ${HIT_RATE}%"
else
    echo "Hit Rate: N/A"
fi

echo "4. Key Patterns Count:"
echo "Trip Details: $(redis-cli -n 2 KEYS 'trip:detail:*' | wc -l)"
echo "Trip Lists: $(redis-cli -n 2 KEYS 'trip:list:*' | wc -l)"
echo "Members: $(redis-cli -n 2 KEYS 'trip:members:*' | wc -l)"
echo "Destinations: $(redis-cli -n 2 KEYS 'trip:destinations:*' | wc -l)"
echo "Schedules: $(redis-cli -n 2 KEYS 'trip:schedules:*' | wc -l)"

echo "5. Expiring Soon (TTL < 60s):"
redis-cli -n 2 KEYS "trip:*" | while read key; do
    ttl=$(redis-cli -n 2 TTL "$key")
    if [ "$ttl" -gt 0 ] && [ "$ttl" -lt 60 ]; then
        echo "$key: ${ttl}s"
    fi
done

echo "=== End of Report ==="
```

### 9.2 자동 정리 스크립트
```bash
#!/bin/bash
# trip_cache_cleanup.sh - 만료된 캐시 정리 스크립트

echo "Starting Trip cache cleanup..."

# 만료된 키 개수 확인
EXPIRED_COUNT=0

# 모든 Trip 관련 키 확인
redis-cli -n 2 KEYS "trip:*" | while read key; do
    ttl=$(redis-cli -n 2 TTL "$key")
    if [ "$ttl" = "-2" ]; then
        redis-cli -n 2 DEL "$key"
        EXPIRED_COUNT=$((EXPIRED_COUNT + 1))
    fi
done

echo "Cleanup completed. Removed $EXPIRED_COUNT expired keys."

# 메모리 정리
redis-cli -n 2 MEMORY PURGE

echo "Memory purge completed."
```

## 10. 연결 테스트

### 10.1 기본 연결 테스트
```bash
# Redis 연결 테스트
redis-cli -n 2 ping

# 데이터베이스 2번 선택 확인
redis-cli -n 2 CLIENT LIST | grep db=2
```

### 10.2 캐시 기능 테스트
```bash
# 테스트 데이터 설정
redis-cli -n 2 SET "test:trip:001" "test data" EX 60

# 테스트 데이터 조회
redis-cli -n 2 GET "test:trip:001"

# TTL 확인
redis-cli -n 2 TTL "test:trip:001"

# 테스트 데이터 삭제
redis-cli -n 2 DEL "test:trip:001"
```

### 10.3 JSON 데이터 테스트
```bash
# JSON 데이터 저장 테스트
redis-cli -n 2 SET "test:json" '{"tripId":"test-001","name":"테스트 여행"}' EX 300

# JSON 데이터 조회 테스트
redis-cli -n 2 GET "test:json"

# JSON 파싱 테스트 (jq 사용)
redis-cli -n 2 GET "test:json" | jq '.tripId'

# 테스트 데이터 정리
redis-cli -n 2 DEL "test:json"
```

## 11. 장애 대응

### 11.1 캐시 서버 장애
```bash
# Redis 서비스 상태 확인
sudo systemctl status redis

# Redis 서비스 재시작
sudo systemctl restart redis

# Redis 로그 확인
sudo tail -f /var/log/redis/redis-server.log
```

### 11.2 메모리 부족 대응
```bash
# 메모리 사용량 확인
redis-cli -n 2 INFO memory

# 큰 키 찾기
redis-cli -n 2 --bigkeys

# 메모리 정리
redis-cli -n 2 FLUSHDB  # 주의: 모든 데이터 삭제
```

### 11.3 성능 저하 대응
```bash
# 느린 쿼리 로그 확인
redis-cli -n 2 SLOWLOG GET 10

# 클라이언트 연결 확인
redis-cli -n 2 CLIENT LIST

# 캐시 히트율 확인
redis-cli -n 2 INFO stats | grep keyspace_hits
```

## 12. 설치 완료 확인

### 12.1 설치 검증 체크리스트
- [ ] Redis 서비스 정상 구동
- [ ] 데이터베이스 2번 사용 확인
- [ ] 기본 설정 완료 
- [ ] 테스트 데이터 저장/조회 성공
- [ ] TTL 설정 동작 확인
- [ ] 캐시 키 패턴 검증
- [ ] 성능 모니터링 스크립트 동작
- [ ] 백업/복원 스크립트 준비
- [ ] 무효화 스크립트 동작 확인

### 12.2 최종 상태 확인
```bash
# 전체 캐시 상태 요약
echo "=== Trip Cache Status ==="
echo "Redis Version: $(redis-cli --version)"
echo "Database: 2"
echo "Keys Count: $(redis-cli -n 2 DBSIZE)"
echo "Memory Usage: $(redis-cli -n 2 INFO memory | grep used_memory_human | cut -d: -f2)"
echo "Cache Hit Rate: $(redis-cli -n 2 INFO stats | grep keyspace_hits)"
echo "Status: Ready"
```

설치가 완료되었습니다. Trip 서비스 캐시가 준비되었으며, Redis 데이터베이스 2번을 사용하여 여행 데이터 캐싱을 위한 모든 구성이 완료되었습니다.