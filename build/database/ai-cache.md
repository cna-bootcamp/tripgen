# AI 서비스 캐시 설치 가이드

## 1. 설치 개요

### 1.1 캐시 정보
- **캐시 시스템**: Redis 7.0+
- **데이터베이스 번호**: 3
- **포트**: 6379
- **설정**: 메모리 최적화 구성

### 1.2 캐시 용도
- AI 작업 진행 상태 캐시
- 생성된 일정 데이터 캐시
- 장소 추천 정보 캐시
- AI 모델 응답 캐시 (디버깅용)

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
maxmemory 1gb
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
requirepass AIServiceCache2025!

# 바인드 주소 설정
bind 127.0.0.1
```

## 4. AI 서비스 캐시 구성

### 4.1 데이터베이스 3번 선택
```bash
# Redis CLI 접속
redis-cli

# 데이터베이스 3번 선택
SELECT 3

# 현재 데이터베이스 확인
CLIENT LIST
```

### 4.2 캐시 키 구조 정의
```bash
# AI 작업 진행 상태 캐시 키 패턴
# ai:job:status:{request_id}

# 생성된 일정 캐시 키 패턴
# ai:schedule:{trip_id}

# 장소 추천 캐시 키 패턴
# ai:recommendation:{place_id}:{profile_hash}

# AI 모델 응답 캐시 키 패턴
# ai:model:response:{request_hash}

# 작업 큐 상태 캐시 키 패턴
# ai:queue:stats

# 사용자별 생성 이력 캐시 키 패턴
# ai:user:history:{user_id}
```

## 5. 초기 캐시 데이터 설정

### 5.1 테스트 작업 상태 캐시
```bash
# Redis CLI에서 데이터베이스 3번 선택
redis-cli -n 3

# AI 작업 상태 캐시 (TTL: 3600초 = 1시간)
SETEX "ai:job:status:req-001" 3600 '{
  "requestId": "req-001",
  "jobType": "SCHEDULE_GENERATION",
  "status": "PROCESSING",
  "progress": 75,
  "currentStep": "generating_places",
  "estimatedCompletion": "2024-12-01T11:30:00Z",
  "tripId": "trip-001",
  "startedAt": "2024-12-01T11:00:00Z",
  "processingTime": 1800
}'

SETEX "ai:job:status:req-002" 3600 '{
  "requestId": "req-002", 
  "jobType": "RECOMMENDATION_GENERATION",
  "status": "COMPLETED",
  "progress": 100,
  "currentStep": "completed",
  "result": {
    "recommendationsGenerated": 5,
    "placesProcessed": 12
  },
  "tripId": "trip-002",
  "startedAt": "2024-12-01T10:45:00Z",
  "completedAt": "2024-12-01T10:50:00Z",
  "processingTime": 300
}'

SETEX "ai:job:status:req-003" 3600 '{
  "requestId": "req-003",
  "jobType": "SCHEDULE_REGENERATION", 
  "status": "FAILED",
  "progress": 30,
  "currentStep": "weather_analysis",
  "error": {
    "code": "WEATHER_API_ERROR",
    "message": "Weather service unavailable",
    "retryable": true
  },
  "tripId": "trip-003",
  "startedAt": "2024-12-01T10:30:00Z",
  "failedAt": "2024-12-01T10:35:00Z"
}'
```

### 5.2 생성된 일정 캐시
```bash
# 생성된 일정 캐시 (TTL: 604800초 = 7일)
SETEX "ai:schedule:trip-001" 604800 '{
  "tripId": "trip-001",
  "schedules": [
    {
      "day": 1,
      "date": "2024-12-15",
      "city": "서울",
      "weather": {
        "condition": "Sunny",
        "minTemperature": -2.5,
        "maxTemperature": 5.8,
        "icon": "sun"
      },
      "places": [
        {
          "placeId": "LOC-001",
          "placeName": "경복궁",
          "category": "TOURIST",
          "startTime": "09:00",
          "duration": 120,
          "transportation": {
            "type": "PUBLIC",
            "duration": 30,
            "distance": 5.2,
            "route": "지하철 3호선 경복궁역"
          },
          "healthConsideration": {
            "restPoints": ["매표소 휴게실", "궁궐 내 벤치"],
            "accessibility": ["엘리베이터", "경사로"],
            "walkingDistance": 2.1
          },
          "order": 1
        },
        {
          "placeId": "LOC-002",
          "placeName": "인사동 전통찻집",
          "category": "RESTAURANT", 
          "startTime": "12:30",
          "duration": 90,
          "transportation": {
            "type": "WALK",
            "duration": 15,
            "distance": 1.2,
            "route": "도보 15분"
          },
          "healthConsideration": {
            "restPoints": ["찻집 내부"],
            "accessibility": ["휠체어 접근"],
            "walkingDistance": 0.1
          },
          "order": 2
        }
      ]
    }
  ],
  "metadata": {
    "generatedAt": "2024-12-01T10:00:00Z",
    "aiModel": "gpt-4o",
    "generationTimeMs": 15420,
    "totalPlaces": 6,
    "totalDays": 4,
    "requestId": "req-001"
  }
}'

SETEX "ai:schedule:trip-002" 604800 '{
  "tripId": "trip-002",
  "schedules": [
    {
      "day": 1,
      "date": "2024-12-20",
      "city": "부산", 
      "weather": {
        "condition": "Clear",
        "minTemperature": 3.2,
        "maxTemperature": 12.5,
        "icon": "clear"
      },
      "places": [
        {
          "placeId": "LOC-003",
          "placeName": "해운대 해수욕장",
          "category": "TOURIST",
          "startTime": "10:00",
          "duration": 180,
          "transportation": {
            "type": "CAR",
            "duration": 20,
            "distance": 8.5,
            "route": "해운대로 직진"
          },
          "healthConsideration": {
            "restPoints": ["해변 파라솔", "주변 카페"],
            "accessibility": ["경사로"],
            "walkingDistance": 1.5
          },
          "order": 1
        }
      ]
    }
  ],
  "metadata": {
    "generatedAt": "2024-12-01T09:30:00Z",
    "aiModel": "gpt-4o",
    "generationTimeMs": 12800,
    "totalPlaces": 4,
    "totalDays": 2,
    "requestId": "req-002"
  }
}'
```

### 5.3 장소 추천 캐시
```bash
# 장소 추천 캐시 (TTL: 86400초 = 24시간)
SETEX "ai:recommendation:LOC-001:profile-hash-001" 86400 '{
  "placeId": "LOC-001",
  "recommendations": {
    "reasons": [
      "조선왕조의 대표적인 궁궐로 역사적 가치가 높음",
      "전통 건축미를 감상할 수 있는 최적의 장소",
      "교통 접근성이 우수하여 방문하기 편리함"
    ],
    "tips": {
      "description": "경복궁은 조선시대 정궁으로 웅장한 건축미와 역사적 의미를 동시에 경험할 수 있습니다.",
      "events": ["수문장 교대식 (10시, 14시, 15시)", "야간개장 (4-10월)"],
      "bestVisitTime": "오전 9-11시 (관광객이 적고 사진촬영에 최적)",
      "estimatedDuration": "2-3시간",
      "photoSpots": ["근정전", "경회루", "향원정"],
      "practicalTips": ["편한 신발 착용 필수", "입장료 현금/카드 결제 가능", "가이드 투어 추천"],
      "weatherTips": "겨울철 방문 시 따뜻한 옷차림 필수, 눈 온 후 방문하면 더욱 아름다운 풍경 감상 가능",
      "alternativePlaces": [
        {
          "name": "창덕궁",
          "reason": "유네스코 세계문화유산으로 자연과 조화로운 건축미",
          "distance": "1.5km"
        }
      ]
    }
  },
  "context": {
    "userProfileHash": "profile-hash-001",
    "generationContext": {
      "season": "winter",
      "groupType": "couple",
      "activityLevel": "moderate"
    },
    "aiModel": "gpt-4o",
    "generatedAt": "2024-12-01T10:15:00Z"
  }
}'

SETEX "ai:recommendation:LOC-003:profile-hash-002" 86400 '{
  "placeId": "LOC-003",
  "recommendations": {
    "reasons": [
      "부산 대표 해수욕장으로 자연 경관이 뛰어남",
      "다양한 수상 스포츠 활동 가능", 
      "주변 먹거리와 볼거리가 풍부함"
    ],
    "tips": {
      "description": "해운대는 부산의 대표적인 관광지로 아름다운 해변과 다양한 레저 활동을 즐길 수 있습니다.",
      "events": ["해운대 빛축제 (12-1월)", "부산 바다축제 (8월)"],
      "bestVisitTime": "일출 시간대 (오전 7-8시) 또는 일몰 시간대 (오후 5-6시)",
      "estimatedDuration": "3-4시간",
      "photoSpots": ["해운대 해변", "동백섬", "APEC 누리마루"],
      "practicalTips": ["겨울철에도 산책로 이용 가능", "주차는 유료", "주변 카페에서 바다 전망 감상"],
      "weatherTips": "겨울철 바닷바람이 차가우니 방풍 옷차림 필수",
      "alternativePlaces": [
        {
          "name": "광안리 해수욕장",
          "reason": "광안대교 야경이 아름다운 또 다른 명소",
          "distance": "3.2km"
        }
      ]
    }
  },
  "context": {
    "userProfileHash": "profile-hash-002",
    "generationContext": {
      "season": "winter",
      "groupType": "solo",
      "activityLevel": "high"
    },
    "aiModel": "gpt-4o",
    "generatedAt": "2024-12-01T09:45:00Z"
  }
}'
```

### 5.4 AI 모델 응답 캐시 (디버깅용)
```bash
# AI 모델 원본 응답 캐시 (TTL: 3600초 = 1시간, 디버깅용)
SETEX "ai:model:response:hash-001" 3600 '{
  "requestHash": "hash-001",
  "model": "gpt-4o",
  "prompt": "서울 3박4일 여행 일정을 생성해주세요. 대상: 28세 여성, 32세 남성 커플. 선호도: 관광, 문화, 쇼핑",
  "response": {
    "choices": [
      {
        "message": {
          "role": "assistant",
          "content": "서울 3박4일 여행 일정을 제안드립니다...",
          "functionCall": null
        },
        "finishReason": "stop"
      }
    ],
    "usage": {
      "promptTokens": 850,
      "completionTokens": 1200,
      "totalTokens": 2050
    }
  },
  "responseTime": 2340,
  "timestamp": "2024-12-01T10:00:00Z"
}'

SETEX "ai:model:response:hash-002" 3600 '{
  "requestHash": "hash-002",
  "model": "gpt-4o",
  "prompt": "경복궁에 대한 개인화된 추천 정보를 생성해주세요. 사용자: 28세 여성, 문화/관광 선호",
  "response": {
    "choices": [
      {
        "message": {
          "role": "assistant", 
          "content": "경복궁은 조선시대의 정궁으로...",
          "functionCall": null
        },
        "finishReason": "stop"
      }
    ],
    "usage": {
      "promptTokens": 420,
      "completionTokens": 680,
      "totalTokens": 1100
    }
  },
  "responseTime": 1850,
  "timestamp": "2024-12-01T10:15:00Z"
}'
```

### 5.5 작업 큐 상태 캐시
```bash
# 작업 큐 통계 캐시 (TTL: 300초 = 5분)
SETEX "ai:queue:stats" 300 '{
  "totalJobs": 156,
  "queuedJobs": 3,
  "processingJobs": 2,
  "completedJobs": 142,
  "failedJobs": 9,
  "avgProcessingTime": 1875,
  "successRate": 94.0,
  "lastUpdated": "2024-12-01T11:00:00Z",
  "jobTypes": {
    "SCHEDULE_GENERATION": {
      "total": 89,
      "queued": 1,
      "processing": 1,
      "completed": 82,
      "failed": 5,
      "avgTime": 2100
    },
    "RECOMMENDATION_GENERATION": {
      "total": 45,
      "queued": 2,
      "processing": 1,
      "completed": 40,
      "failed": 2,
      "avgTime": 1200
    },
    "SCHEDULE_REGENERATION": {
      "total": 22,
      "queued": 0,
      "processing": 0,
      "completed": 20,
      "failed": 2,
      "avgTime": 2500
    }
  }
}'
```

### 5.6 사용자별 생성 이력 캐시
```bash
# 사용자별 AI 생성 이력 캐시 (TTL: 1800초 = 30분)
SETEX "ai:user:history:user-001" 1800 '{
  "userId": "user-001",
  "totalGenerations": 8,
  "successfulGenerations": 7,
  "failedGenerations": 1,
  "recentGenerations": [
    {
      "requestId": "req-001",
      "tripId": "trip-001",
      "jobType": "SCHEDULE_GENERATION",
      "status": "COMPLETED",
      "createdAt": "2024-12-01T10:00:00Z",
      "completedAt": "2024-12-01T10:15:00Z"
    },
    {
      "requestId": "req-005",
      "tripId": "trip-005", 
      "jobType": "RECOMMENDATION_GENERATION",
      "status": "COMPLETED",
      "createdAt": "2024-11-30T15:30:00Z",
      "completedAt": "2024-11-30T15:35:00Z"
    }
  ],
  "preferences": {
    "favoriteCategories": ["TOURIST", "CULTURE", "RESTAURANT"],
    "avgTripDuration": 3.5,
    "preferredTransport": "PUBLIC"
  },
  "lastActivity": "2024-12-01T10:15:00Z"
}'

SETEX "ai:user:history:user-002" 1800 '{
  "userId": "user-002",
  "totalGenerations": 3,
  "successfulGenerations": 3,
  "failedGenerations": 0,
  "recentGenerations": [
    {
      "requestId": "req-002",
      "tripId": "trip-002",
      "jobType": "SCHEDULE_GENERATION", 
      "status": "COMPLETED",
      "createdAt": "2024-12-01T09:30:00Z",
      "completedAt": "2024-12-01T09:45:00Z"
    }
  ],
  "preferences": {
    "favoriteCategories": ["NATURE", "SPORTS", "RESTAURANT"],
    "avgTripDuration": 2.0,
    "preferredTransport": "CAR"
  },
  "lastActivity": "2024-12-01T09:45:00Z"
}'
```

## 6. 캐시 성능 최적화

### 6.1 메모리 사용량 모니터링
```bash
# 메모리 정보 확인
redis-cli -n 3 INFO memory

# 키 개수 확인
redis-cli -n 3 DBSIZE

# 특정 패턴 키 검색
redis-cli -n 3 KEYS "ai:*"
```

### 6.2 TTL 관리
```bash
# TTL 확인
redis-cli -n 3 TTL "ai:schedule:trip-001"

# TTL 설정
redis-cli -n 3 EXPIRE "ai:job:status:req-001" 3600

# 만료된 키 확인
redis-cli -n 3 KEYS "*" | while read key; do 
  ttl=$(redis-cli -n 3 TTL "$key")
  if [ "$ttl" = "-2" ]; then
    echo "Expired key: $key"
  fi
done
```

### 6.3 캐시 통계 확인
```bash
# 캐시 히트율 확인
redis-cli -n 3 INFO stats | grep keyspace_hits
redis-cli -n 3 INFO stats | grep keyspace_misses

# 초당 명령 처리 수 확인
redis-cli -n 3 INFO stats | grep instantaneous_ops_per_sec

# 큰 키 찾기
redis-cli -n 3 --bigkeys
```

## 7. 캐시 무효화 스크립트

### 7.1 AI 작업 관련 캐시 무효화
```bash
#!/bin/bash
# ai_cache_invalidate.sh - AI 작업 관련 캐시 무효화 스크립트

REQUEST_ID=$1
TRIP_ID=$2

if [ -z "$REQUEST_ID" ]; then
    echo "Usage: $0 <request_id> [trip_id]"
    exit 1
fi

# 작업 상태 캐시 삭제
redis-cli -n 3 DEL "ai:job:status:$REQUEST_ID"

# 여행 일정 캐시 삭제 (trip_id가 제공된 경우)
if [ -n "$TRIP_ID" ]; then
    redis-cli -n 3 DEL "ai:schedule:$TRIP_ID"
fi

# 작업 큐 통계 캐시 무효화
redis-cli -n 3 DEL "ai:queue:stats"

echo "AI cache invalidated for request: $REQUEST_ID"
```

### 7.2 장소 추천 캐시 무효화
```bash
#!/bin/bash
# ai_recommendation_cache_invalidate.sh - 장소 추천 캐시 무효화 스크립트

PLACE_ID=$1

if [ -z "$PLACE_ID" ]; then
    echo "Usage: $0 <place_id>"
    exit 1
fi

# 해당 장소의 모든 추천 캐시 삭제
redis-cli -n 3 KEYS "ai:recommendation:$PLACE_ID:*" | xargs redis-cli -n 3 DEL

echo "Recommendation cache invalidated for place: $PLACE_ID"
```

### 7.3 사용자별 캐시 무효화
```bash
#!/bin/bash
# ai_user_cache_invalidate.sh - 사용자별 AI 캐시 무효화 스크립트

USER_ID=$1

if [ -z "$USER_ID" ]; then
    echo "Usage: $0 <user_id>"
    exit 1
fi

# 사용자 이력 캐시 삭제
redis-cli -n 3 DEL "ai:user:history:$USER_ID"

echo "User AI cache invalidated for user: $USER_ID"
```

## 8. 캐시 백업 및 복원

### 8.1 캐시 백업 스크립트
```bash
#!/bin/bash
# ai_cache_backup.sh - AI 캐시 백업 스크립트

BACKUP_DIR="/backup/ai-cache"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/ai_cache_backup_$DATE.rdb"

# 백업 디렉토리 생성
mkdir -p $BACKUP_DIR

# Redis 데이터베이스 3번 백업
redis-cli -n 3 --rdb $BACKUP_FILE

# 백업 파일 압축
gzip $BACKUP_FILE

echo "AI cache backup completed: ${BACKUP_FILE}.gz"
```

### 8.2 캐시 복원 스크립트
```bash
#!/bin/bash
# ai_cache_restore.sh - AI 캐시 복원 스크립트

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

echo "AI cache restore completed from: $BACKUP_FILE"
```

## 9. 모니터링 및 알람

### 9.1 성능 모니터링 스크립트
```bash
#!/bin/bash
# ai_cache_monitor.sh - AI 캐시 모니터링 스크립트

echo "=== AI Cache Monitoring Report ==="
echo "Date: $(date)"
echo

# 데이터베이스 3번 선택하여 모니터링
echo "1. Database Info:"
redis-cli -n 3 INFO keyspace | grep db3

echo "2. Memory Usage:"
redis-cli -n 3 INFO memory | grep used_memory_human

echo "3. Cache Performance:"
HITS=$(redis-cli -n 3 INFO stats | grep keyspace_hits | cut -d: -f2 | tr -d '\r')
MISSES=$(redis-cli -n 3 INFO stats | grep keyspace_misses | cut -d: -f2 | tr -d '\r') 
TOTAL=$((HITS + MISSES))
if [ $TOTAL -gt 0 ]; then
    HIT_RATE=$(echo "scale=2; $HITS * 100 / $TOTAL" | bc)
    echo "Hit Rate: ${HIT_RATE}%"
else
    echo "Hit Rate: N/A"
fi

echo "4. Key Patterns Count:"
echo "Job Status: $(redis-cli -n 3 KEYS 'ai:job:status:*' | wc -l)"
echo "Schedules: $(redis-cli -n 3 KEYS 'ai:schedule:*' | wc -l)"
echo "Recommendations: $(redis-cli -n 3 KEYS 'ai:recommendation:*' | wc -l)"
echo "Model Responses: $(redis-cli -n 3 KEYS 'ai:model:response:*' | wc -l)"
echo "User Histories: $(redis-cli -n 3 KEYS 'ai:user:history:*' | wc -l)"

echo "5. Queue Statistics:"
QUEUE_STATS=$(redis-cli -n 3 GET "ai:queue:stats")
if [ -n "$QUEUE_STATS" ]; then
    echo "Queue Stats Available: Yes"
    echo "$QUEUE_STATS" | jq '.totalJobs, .queuedJobs, .processingJobs, .successRate'
else
    echo "Queue Stats Available: No"
fi

echo "6. Expiring Soon (TTL < 300s):"
redis-cli -n 3 KEYS "ai:*" | while read key; do
    ttl=$(redis-cli -n 3 TTL "$key")
    if [ "$ttl" -gt 0 ] && [ "$ttl" -lt 300 ]; then
        echo "$key: ${ttl}s"
    fi
done

echo "=== End of Report ==="
```

### 9.2 자동 정리 스크립트
```bash
#!/bin/bash
# ai_cache_cleanup.sh - 만료된 AI 캐시 정리 스크립트

echo "Starting AI cache cleanup..."

# 만료된 키 개수 확인
EXPIRED_COUNT=0

# 모든 AI 관련 키 확인
redis-cli -n 3 KEYS "ai:*" | while read key; do
    ttl=$(redis-cli -n 3 TTL "$key")
    if [ "$ttl" = "-2" ]; then
        redis-cli -n 3 DEL "$key"
        EXPIRED_COUNT=$((EXPIRED_COUNT + 1))
    fi
done

echo "Cleanup completed. Removed $EXPIRED_COUNT expired keys."

# 메모리 정리
redis-cli -n 3 MEMORY PURGE

echo "Memory purge completed."

# 큐 통계 갱신
redis-cli -n 3 DEL "ai:queue:stats"
echo "Queue statistics cache cleared for refresh."
```

## 10. 연결 테스트

### 10.1 기본 연결 테스트
```bash
# Redis 연결 테스트
redis-cli -n 3 ping

# 데이터베이스 3번 선택 확인
redis-cli -n 3 CLIENT LIST | grep db=3
```

### 10.2 캐시 기능 테스트
```bash
# 테스트 데이터 설정
redis-cli -n 3 SET "test:ai:001" "test data" EX 60

# 테스트 데이터 조회
redis-cli -n 3 GET "test:ai:001"

# TTL 확인
redis-cli -n 3 TTL "test:ai:001"

# 테스트 데이터 삭제
redis-cli -n 3 DEL "test:ai:001"
```

### 10.3 JSON 데이터 테스트
```bash
# JSON 데이터 저장 테스트
redis-cli -n 3 SET "test:ai:json" '{"requestId":"test-001","status":"PROCESSING","progress":50}' EX 300

# JSON 데이터 조회 테스트
redis-cli -n 3 GET "test:ai:json"

# JSON 파싱 테스트 (jq 사용)
redis-cli -n 3 GET "test:ai:json" | jq '.status'

# 테스트 데이터 정리
redis-cli -n 3 DEL "test:ai:json"
```

### 10.4 복합 키 패턴 테스트
```bash
# 패턴 매칭 테스트
redis-cli -n 3 SET "ai:test:pattern:001" "value1" EX 60
redis-cli -n 3 SET "ai:test:pattern:002" "value2" EX 60

# 패턴으로 키 검색
redis-cli -n 3 KEYS "ai:test:pattern:*"

# 패턴 키 일괄 삭제
redis-cli -n 3 KEYS "ai:test:pattern:*" | xargs redis-cli -n 3 DEL
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
redis-cli -n 3 INFO memory

# 큰 키 찾기
redis-cli -n 3 --bigkeys

# 특정 패턴 키 정리 (주의: 데이터 손실)
redis-cli -n 3 KEYS "ai:model:response:*" | xargs redis-cli -n 3 DEL
```

### 11.3 성능 저하 대응
```bash
# 느린 쿼리 로그 확인
redis-cli -n 3 SLOWLOG GET 10

# 클라이언트 연결 확인
redis-cli -n 3 CLIENT LIST

# 캐시 히트율 확인
redis-cli -n 3 INFO stats | grep keyspace_hits
```

## 12. 설치 완료 확인

### 12.1 설치 검증 체크리스트
- [ ] Redis 서비스 정상 구동
- [ ] 데이터베이스 3번 사용 확인
- [ ] 기본 설정 완료 
- [ ] 테스트 데이터 저장/조회 성공
- [ ] TTL 설정 동작 확인
- [ ] 캐시 키 패턴 검증
- [ ] JSON 데이터 처리 확인
- [ ] 성능 모니터링 스크립트 동작
- [ ] 백업/복원 스크립트 준비
- [ ] 무효화 스크립트 동작 확인

### 12.2 최종 상태 확인
```bash
# 전체 AI 캐시 상태 요약
echo "=== AI Cache Status ==="
echo "Redis Version: $(redis-cli --version)"
echo "Database: 3"
echo "Keys Count: $(redis-cli -n 3 DBSIZE)"
echo "Memory Usage: $(redis-cli -n 3 INFO memory | grep used_memory_human | cut -d: -f2)"
echo "AI Job Status Keys: $(redis-cli -n 3 KEYS 'ai:job:status:*' | wc -l)"
echo "AI Schedule Keys: $(redis-cli -n 3 KEYS 'ai:schedule:*' | wc -l)"
echo "AI Recommendation Keys: $(redis-cli -n 3 KEYS 'ai:recommendation:*' | wc -l)"
echo "Cache Hit Rate: $(redis-cli -n 3 INFO stats | grep keyspace_hits)"
echo "Status: Ready"
```

설치가 완료되었습니다. AI 서비스 캐시가 준비되었으며, Redis 데이터베이스 3번을 사용하여 AI 작업 상태, 생성된 일정, 장소 추천 정보 캐싱을 위한 모든 구성이 완료되었습니다.