# Location 서비스 캐시 설치 가이드

## 1. 설치 개요

### 1.1 캐시 정보
- **캐시 시스템**: Redis 7.0+
- **데이터베이스 번호**: 4
- **포트**: 6379
- **설정**: 지리정보 검색 최적화 구성

### 1.2 캐시 용도
- 장소 상세 정보 캐시
- 주변 장소 검색 결과 캐시
- 키워드 검색 결과 캐시
- AI 추천 정보 캐시
- 영업시간 및 실시간 정보 캐시

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
requirepass LocationServiceCache2025!

# 바인드 주소 설정
bind 127.0.0.1
```

## 4. Location 서비스 캐시 구성

### 4.1 데이터베이스 4번 선택
```bash
# Redis CLI 접속
redis-cli

# 데이터베이스 4번 선택
SELECT 4

# 현재 데이터베이스 확인
CLIENT LIST
```

### 4.2 캐시 키 구조 정의
```bash
# 장소 상세 정보 캐시 키 패턴
# location:place:{place_id}

# 주변 장소 검색 결과 캐시 키 패턴
# location:nearby:{lat}:{lng}:{radius}:{category}

# 키워드 검색 결과 캐시 키 패턴
# location:search:{keyword}:{location_hash}

# AI 추천 정보 캐시 키 패턴
# location:recommendation:{place_id}:{trip_id}

# 영업시간 캐시 키 패턴
# location:business_hours:{place_id}

# 최근 검색 기록 캐시 키 패턴
# location:recent_searches:{user_id}

# 카테고리 목록 캐시 키 패턴
# location:categories:all
```

## 5. 초기 캐시 데이터 설정

### 5.1 장소 상세 정보 캐시
```bash
# Redis CLI에서 데이터베이스 4번 선택
redis-cli -n 4

# 장소 상세 정보 캐시 (TTL: 3600초 = 1시간)
SETEX "location:place:LOC-001" 3600 '{
  "placeId": "LOC-001",
  "name": "경복궁",
  "category": "TOURIST",
  "description": "조선왕조의 정궁으로 아름다운 건축물과 역사를 체험할 수 있는 곳",
  "rating": 4.5,
  "reviewCount": 15847,
  "priceLevel": 2,
  "latitude": 37.5796,
  "longitude": 126.9770,
  "address": "서울특별시 종로구 사직로 161",
  "regionType": "DOMESTIC",
  "images": ["https://example.com/gyeongbok1.jpg", "https://example.com/gyeongbok2.jpg"],
  "contactPhone": "02-3700-3900",
  "businessHours": {
    "mon": "09:00-18:00",
    "tue": "09:00-18:00", 
    "wed": "09:00-18:00",
    "thu": "09:00-18:00",
    "fri": "09:00-18:00",
    "sat": "09:00-18:00",
    "sun": "09:00-18:00"
  }
}'

SETEX "location:place:LOC-002" 3600 '{
  "placeId": "LOC-002",
  "name": "인사동 맛집",
  "category": "RESTAURANT",
  "description": "전통 한식을 맛볼 수 있는 인사동의 유명 맛집",
  "rating": 4.3,
  "reviewCount": 2847,
  "priceLevel": 3,
  "latitude": 37.5735,
  "longitude": 126.9854,
  "address": "서울특별시 종로구 인사동길 62",
  "regionType": "DOMESTIC",
  "images": ["https://example.com/insadong1.jpg"],
  "contactPhone": "02-123-4567",
  "businessHours": {
    "mon": "11:00-22:00",
    "tue": "11:00-22:00",
    "wed": "11:00-22:00", 
    "thu": "11:00-22:00",
    "fri": "11:00-23:00",
    "sat": "11:00-23:00",
    "sun": "11:00-22:00"
  }
}'

SETEX "location:place:LOC-003" 3600 '{
  "placeId": "LOC-003", 
  "name": "해운대 해수욕장",
  "category": "TOURIST",
  "description": "부산의 대표적인 해수욕장으로 아름다운 바다 경치를 즐길 수 있음",
  "rating": 4.2,
  "reviewCount": 8953,
  "priceLevel": 1,
  "latitude": 35.1595,
  "longitude": 129.1603,
  "address": "부산광역시 해운대구 우동",
  "regionType": "DOMESTIC",
  "images": ["https://example.com/haeundae1.jpg", "https://example.com/haeundae2.jpg"],
  "contactPhone": "051-749-4000",
  "businessHours": {
    "always_open": true
  }
}'
```

### 5.2 주변 장소 검색 결과 캐시
```bash
# 주변 장소 검색 결과 캐시 (TTL: 1800초 = 30분)
SETEX "location:nearby:37.5665:126.9780:1000:ALL" 1800 '{
  "places": [
    {
      "placeId": "LOC-001",
      "name": "경복궁", 
      "category": "TOURIST",
      "rating": 4.5,
      "distance": 142,
      "latitude": 37.5796,
      "longitude": 126.9770,
      "address": "서울특별시 종로구 사직로 161"
    },
    {
      "placeId": "LOC-002",
      "name": "인사동 맛집",
      "category": "RESTAURANT", 
      "rating": 4.3,
      "distance": 687,
      "latitude": 37.5735,
      "longitude": 126.9854,
      "address": "서울특별시 종로구 인사동길 62"
    }
  ],
  "totalCount": 2,
  "searchCenter": {
    "latitude": 37.5665,
    "longitude": 126.9780
  },
  "searchRadius": 1000,
  "category": "ALL"
}'

SETEX "location:nearby:35.1595:129.1603:2000:TOURIST" 1800 '{
  "places": [
    {
      "placeId": "LOC-003",
      "name": "해운대 해수욕장",
      "category": "TOURIST",
      "rating": 4.2,
      "distance": 0,
      "latitude": 35.1595,
      "longitude": 129.1603,
      "address": "부산광역시 해운대구 우동"
    }
  ],
  "totalCount": 1,
  "searchCenter": {
    "latitude": 35.1595,
    "longitude": 129.1603
  },
  "searchRadius": 2000,
  "category": "TOURIST"
}'
```

### 5.3 키워드 검색 결과 캐시
```bash
# 키워드 검색 결과 캐시 (TTL: 1800초 = 30분)
SETEX "location:search:경복궁:seoul" 1800 '{
  "places": [
    {
      "placeId": "LOC-001",
      "name": "경복궁",
      "category": "TOURIST",
      "rating": 4.5,
      "reviewCount": 15847,
      "latitude": 37.5796,
      "longitude": 126.9770,
      "address": "서울특별시 종로구 사직로 161",
      "searchRelevance": 100
    }
  ],
  "totalCount": 1,
  "searchKeyword": "경복궁",
  "searchLocation": "seoul"
}'

SETEX "location:search:음식점:insadong" 1800 '{
  "places": [
    {
      "placeId": "LOC-002", 
      "name": "인사동 맛집",
      "category": "RESTAURANT",
      "rating": 4.3,
      "reviewCount": 2847,
      "latitude": 37.5735,
      "longitude": 126.9854,
      "address": "서울특별시 종로구 인사동길 62",
      "searchRelevance": 95
    }
  ],
  "totalCount": 1,
  "searchKeyword": "음식점", 
  "searchLocation": "insadong"
}'
```

### 5.4 AI 추천 정보 캐시
```bash
# AI 추천 정보 캐시 (TTL: 86400초 = 24시간)
SETEX "location:recommendation:LOC-001:trip-001" 86400 '{
  "placeId": "LOC-001",
  "tripId": "trip-001",
  "recommendReason": "AI가 추천하는 서울의 대표 궁궐로 한국 전통 문화를 체험하기에 최적의 장소입니다.",
  "tipsData": {
    "description": "조선 왕조의 정궁",
    "specialEvents": "수문장 교대식",
    "bestVisitTime": "오전 10시",
    "estimatedDuration": "2시간",
    "photoSpots": ["근정전", "경회루"],
    "practicalTips": ["편한 신발 착용", "오디오 가이드 대여 추천"],
    "weatherTips": "우천시 실내 전시관 관람 가능",
    "alternativePlaces": [
      {
        "name": "창덕궁",
        "reason": "유네스코 세계문화유산으로 후원이 아름다움",
        "distance": "2.1km"
      }
    ]
  },
  "fromCache": false,
  "generatedAt": "2024-12-10T09:00:00Z",
  "expiresAt": "2024-12-11T09:00:00Z"
}'

SETEX "location:recommendation:LOC-002:" 86400 '{
  "placeId": "LOC-002",
  "tripId": null,
  "recommendReason": "인사동 전통 거리의 대표 맛집으로 정통 한식을 맛볼 수 있습니다.",
  "tipsData": {
    "description": "인사동 전통 한식당",
    "specialEvents": "",
    "bestVisitTime": "점심시간",
    "estimatedDuration": "1시간", 
    "photoSpots": ["전통 한상차림"],
    "practicalTips": ["예약 필수", "현금 결제 우대"],
    "weatherTips": "실내 식당으로 날씨 무관",
    "alternativePlaces": []
  },
  "fromCache": false,
  "generatedAt": "2024-12-10T12:00:00Z",
  "expiresAt": "2024-12-11T12:00:00Z"
}'
```

### 5.5 영업시간 캐시
```bash
# 영업시간 캐시 (TTL: 21600초 = 6시간)
SETEX "location:business_hours:LOC-001" 21600 '{
  "placeId": "LOC-001",
  "isOpen": true,
  "currentStatus": "open",
  "todayHours": "09:00-18:00",
  "weeklyHours": [
    {"day": "monday", "hours": "09:00-18:00", "isToday": false},
    {"day": "tuesday", "hours": "09:00-18:00", "isToday": false},
    {"day": "wednesday", "hours": "09:00-18:00", "isToday": true},
    {"day": "thursday", "hours": "09:00-18:00", "isToday": false},
    {"day": "friday", "hours": "09:00-18:00", "isToday": false},
    {"day": "saturday", "hours": "09:00-18:00", "isToday": false},
    {"day": "sunday", "hours": "09:00-18:00", "isToday": false}
  ],
  "lastUpdated": "2024-12-10T14:30:00Z"
}'

SETEX "location:business_hours:LOC-002" 21600 '{
  "placeId": "LOC-002", 
  "isOpen": true,
  "currentStatus": "open",
  "todayHours": "11:00-22:00",
  "weeklyHours": [
    {"day": "monday", "hours": "11:00-22:00", "isToday": false},
    {"day": "tuesday", "hours": "11:00-22:00", "isToday": false},
    {"day": "wednesday", "hours": "11:00-22:00", "isToday": true},
    {"day": "thursday", "hours": "11:00-22:00", "isToday": false},
    {"day": "friday", "hours": "11:00-23:00", "isToday": false},
    {"day": "saturday", "hours": "11:00-23:00", "isToday": false},
    {"day": "sunday", "hours": "11:00-22:00", "isToday": false}
  ],
  "lastUpdated": "2024-12-10T14:30:00Z"
}'

SETEX "location:business_hours:LOC-003" 21600 '{
  "placeId": "LOC-003",
  "isOpen": true,
  "currentStatus": "always_open", 
  "todayHours": "24시간 개방",
  "weeklyHours": [
    {"day": "monday", "hours": "24시간", "isToday": false},
    {"day": "tuesday", "hours": "24시간", "isToday": false},
    {"day": "wednesday", "hours": "24시간", "isToday": true},
    {"day": "thursday", "hours": "24시간", "isToday": false},
    {"day": "friday", "hours": "24시간", "isToday": false},
    {"day": "saturday", "hours": "24시간", "isToday": false},
    {"day": "sunday", "hours": "24시간", "isToday": false}
  ],
  "lastUpdated": "2024-12-10T14:30:00Z"
}'
```

### 5.6 최근 검색 기록 캐시
```bash
# 사용자별 최근 검색 기록 캐시 (TTL: 7200초 = 2시간)
SETEX "location:recent_searches:user-001" 7200 '[
  {
    "searchQuery": "경복궁",
    "searchType": "KEYWORD",
    "searchLocation": "서울시 종로구",
    "searchLatitude": 37.5665,
    "searchLongitude": 126.9780,
    "resultCount": 5,
    "searchedAt": "2024-12-10T14:25:00Z"
  },
  {
    "searchQuery": "음식점",
    "searchType": "CATEGORY", 
    "searchLocation": "인사동",
    "searchLatitude": 37.5735,
    "searchLongitude": 126.9854,
    "resultCount": 12,
    "searchedAt": "2024-12-10T13:15:00Z"
  }
]'

SETEX "location:recent_searches:user-002" 7200 '[
  {
    "searchQuery": "주변 관광지",
    "searchType": "NEARBY",
    "searchLocation": "해운대",
    "searchLatitude": 35.1595,
    "searchLongitude": 129.1603,
    "resultCount": 8,
    "searchedAt": "2024-12-10T11:30:00Z"
  }
]'
```

### 5.7 카테고리 목록 캐시
```bash
# 카테고리 목록 캐시 (TTL: 43200초 = 12시간)
SETEX "location:categories:all" 43200 '[
  {
    "categoryCode": "ALL",
    "categoryName": "전체",
    "categoryNameEn": "All",
    "displayOrder": 1,
    "isActive": true
  },
  {
    "categoryCode": "TOURIST", 
    "categoryName": "관광지",
    "categoryNameEn": "Tourist Attraction",
    "displayOrder": 2,
    "isActive": true
  },
  {
    "categoryCode": "RESTAURANT",
    "categoryName": "음식점",
    "categoryNameEn": "Restaurant", 
    "displayOrder": 3,
    "isActive": true
  },
  {
    "categoryCode": "LAUNDRY",
    "categoryName": "빨래방",
    "categoryNameEn": "Laundry",
    "displayOrder": 4,
    "isActive": true
  },
  {
    "categoryCode": "ACCOMMODATION",
    "categoryName": "숙박",
    "categoryNameEn": "Accommodation",
    "displayOrder": 5,
    "isActive": true
  }
]'
```

## 6. 캐시 성능 최적화

### 6.1 메모리 사용량 모니터링
```bash
# 메모리 정보 확인
redis-cli -n 4 INFO memory

# 키 개수 확인
redis-cli -n 4 DBSIZE

# 특정 패턴 키 검색
redis-cli -n 4 KEYS "location:*"
```

### 6.2 TTL 관리
```bash
# TTL 확인
redis-cli -n 4 TTL "location:place:LOC-001"

# TTL 설정
redis-cli -n 4 EXPIRE "location:place:LOC-001" 3600

# 만료된 키 확인
redis-cli -n 4 KEYS "*" | while read key; do 
  ttl=$(redis-cli -n 4 TTL "$key")
  if [ "$ttl" = "-2" ]; then
    echo "Expired key: $key"
  fi
done
```

### 6.3 캐시 통계 확인
```bash
# 캐시 히트율 확인
redis-cli -n 4 INFO stats | grep keyspace_hits
redis-cli -n 4 INFO stats | grep keyspace_misses

# 초당 명령 처리 수 확인
redis-cli -n 4 INFO stats | grep instantaneous_ops_per_sec

# 지리정보 캐시 성능 확인
redis-cli -n 4 KEYS "location:nearby:*" | wc -l
redis-cli -n 4 KEYS "location:search:*" | wc -l
```

## 7. 캐시 무효화 스크립트

### 7.1 장소 관련 캐시 무효화
```bash
#!/bin/bash
# location_cache_invalidate.sh - 장소 관련 캐시 무효화 스크립트

PLACE_ID=$1

if [ -z "$PLACE_ID" ]; then
    echo "Usage: $0 <place_id>"
    exit 1
fi

# 장소 상세 정보 캐시 삭제
redis-cli -n 4 DEL "location:place:$PLACE_ID"

# 영업시간 캐시 삭제
redis-cli -n 4 DEL "location:business_hours:$PLACE_ID"

# 장소별 추천 정보 캐시 삭제 (모든 trip_id)
redis-cli -n 4 KEYS "location:recommendation:$PLACE_ID:*" | xargs redis-cli -n 4 DEL

# 주변 검색 결과 캐시 삭제 (해당 장소가 포함된 결과)
redis-cli -n 4 KEYS "location:nearby:*" | xargs redis-cli -n 4 DEL

# 키워드 검색 결과 캐시 삭제 (해당 장소가 포함된 결과)
redis-cli -n 4 KEYS "location:search:*" | xargs redis-cli -n 4 DEL

echo "Cache invalidated for place: $PLACE_ID"
```

### 7.2 검색 결과 캐시 무효화
```bash
#!/bin/bash
# location_search_cache_invalidate.sh - 검색 결과 캐시 무효화 스크립트

CACHE_TYPE=$1

if [ -z "$CACHE_TYPE" ]; then
    echo "Usage: $0 <cache_type: nearby|search|all>"
    exit 1
fi

case $CACHE_TYPE in
    "nearby")
        redis-cli -n 4 KEYS "location:nearby:*" | xargs redis-cli -n 4 DEL
        echo "Nearby search cache invalidated"
        ;;
    "search")
        redis-cli -n 4 KEYS "location:search:*" | xargs redis-cli -n 4 DEL
        echo "Keyword search cache invalidated"
        ;;
    "all")
        redis-cli -n 4 KEYS "location:nearby:*" | xargs redis-cli -n 4 DEL
        redis-cli -n 4 KEYS "location:search:*" | xargs redis-cli -n 4 DEL
        echo "All search cache invalidated"
        ;;
    *)
        echo "Invalid cache type. Use: nearby, search, or all"
        exit 1
        ;;
esac
```

### 7.3 사용자별 캐시 무효화
```bash
#!/bin/bash
# user_location_cache_invalidate.sh - 사용자별 캐시 무효화 스크립트

USER_ID=$1

if [ -z "$USER_ID" ]; then
    echo "Usage: $0 <user_id>"
    exit 1
fi

# 사용자 최근 검색 기록 캐시 삭제
redis-cli -n 4 DEL "location:recent_searches:$USER_ID"

# 사용자별 개인화된 추천 캐시 삭제 (trip_id 기반)
redis-cli -n 4 KEYS "location:recommendation:*:trip-*" | while read key; do
    # trip_id에서 user_id 추출하여 매칭 (실제 구현시 trip 서비스와 연동 필요)
    redis-cli -n 4 DEL "$key"
done

echo "Location cache invalidated for user: $USER_ID"
```

## 8. 캐시 백업 및 복원

### 8.1 캐시 백업 스크립트
```bash
#!/bin/bash
# location_cache_backup.sh - Location 캐시 백업 스크립트

BACKUP_DIR="/backup/location-cache"
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/location_cache_backup_$DATE.rdb"

# 백업 디렉토리 생성
mkdir -p $BACKUP_DIR

# Redis 데이터베이스 4번 백업
redis-cli -n 4 --rdb $BACKUP_FILE

# 백업 파일 압축
gzip $BACKUP_FILE

echo "Location cache backup completed: ${BACKUP_FILE}.gz"
```

### 8.2 캐시 복원 스크립트
```bash
#!/bin/bash
# location_cache_restore.sh - Location 캐시 복원 스크립트

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

echo "Location cache restore completed from: $BACKUP_FILE"
```

## 9. 모니터링 및 알람

### 9.1 성능 모니터링 스크립트
```bash
#!/bin/bash
# location_cache_monitor.sh - Location 캐시 모니터링 스크립트

echo "=== Location Cache Monitoring Report ==="
echo "Date: $(date)"
echo

# 데이터베이스 4번 선택하여 모니터링
echo "1. Database Info:"
redis-cli -n 4 INFO keyspace | grep db4

echo "2. Memory Usage:"
redis-cli -n 4 INFO memory | grep used_memory_human

echo "3. Cache Performance:"
HITS=$(redis-cli -n 4 INFO stats | grep keyspace_hits | cut -d: -f2 | tr -d '\r')
MISSES=$(redis-cli -n 4 INFO stats | grep keyspace_misses | cut -d: -f2 | tr -d '\r') 
TOTAL=$((HITS + MISSES))
if [ $TOTAL -gt 0 ]; then
    HIT_RATE=$(echo "scale=2; $HITS * 100 / $TOTAL" | bc)
    echo "Hit Rate: ${HIT_RATE}%"
else
    echo "Hit Rate: N/A"
fi

echo "4. Key Patterns Count:"
echo "Place Details: $(redis-cli -n 4 KEYS 'location:place:*' | wc -l)"
echo "Nearby Searches: $(redis-cli -n 4 KEYS 'location:nearby:*' | wc -l)"
echo "Keyword Searches: $(redis-cli -n 4 KEYS 'location:search:*' | wc -l)"
echo "Recommendations: $(redis-cli -n 4 KEYS 'location:recommendation:*' | wc -l)"
echo "Business Hours: $(redis-cli -n 4 KEYS 'location:business_hours:*' | wc -l)"
echo "Recent Searches: $(redis-cli -n 4 KEYS 'location:recent_searches:*' | wc -l)"

echo "5. Expiring Soon (TTL < 300s):"
redis-cli -n 4 KEYS "location:*" | while read key; do
    ttl=$(redis-cli -n 4 TTL "$key")
    if [ "$ttl" -gt 0 ] && [ "$ttl" -lt 300 ]; then
        echo "$key: ${ttl}s"
    fi
done

echo "6. Geographic Cache Distribution:"
echo "Domestic Places: $(redis-cli -n 4 KEYS 'location:place:*' | xargs redis-cli -n 4 MGET | grep -c 'DOMESTIC')"
echo "International Places: $(redis-cli -n 4 KEYS 'location:place:*' | xargs redis-cli -n 4 MGET | grep -c 'INTERNATIONAL')"

echo "=== End of Report ==="
```

### 9.2 자동 정리 스크립트
```bash
#!/bin/bash
# location_cache_cleanup.sh - 만료된 캐시 정리 스크립트

echo "Starting Location cache cleanup..."

# 만료된 키 개수 확인
EXPIRED_COUNT=0

# 모든 Location 관련 키 확인
redis-cli -n 4 KEYS "location:*" | while read key; do
    ttl=$(redis-cli -n 4 TTL "$key")
    if [ "$ttl" = "-2" ]; then
        redis-cli -n 4 DEL "$key"
        EXPIRED_COUNT=$((EXPIRED_COUNT + 1))
    fi
done

echo "Cleanup completed. Removed $EXPIRED_COUNT expired keys."

# 오래된 검색 결과 캐시 정리 (1일 이상된 검색 결과)
redis-cli -n 4 KEYS "location:nearby:*" | while read key; do
    ttl=$(redis-cli -n 4 TTL "$key")
    if [ "$ttl" = "-1" ]; then  # TTL이 설정되지 않은 키
        redis-cli -n 4 EXPIRE "$key" 1800  # 30분 TTL 설정
    fi
done

redis-cli -n 4 KEYS "location:search:*" | while read key; do
    ttl=$(redis-cli -n 4 TTL "$key")
    if [ "$ttl" = "-1" ]; then  # TTL이 설정되지 않은 키
        redis-cli -n 4 EXPIRE "$key" 1800  # 30분 TTL 설정
    fi
done

# 메모리 정리
redis-cli -n 4 MEMORY PURGE

echo "Memory purge completed."
```

## 10. 연결 테스트

### 10.1 기본 연결 테스트
```bash
# Redis 연결 테스트
redis-cli -n 4 ping

# 데이터베이스 4번 선택 확인
redis-cli -n 4 CLIENT LIST | grep db=4
```

### 10.2 캐시 기능 테스트
```bash
# 테스트 데이터 설정
redis-cli -n 4 SET "test:location:001" "test place data" EX 60

# 테스트 데이터 조회
redis-cli -n 4 GET "test:location:001"

# TTL 확인
redis-cli -n 4 TTL "test:location:001"

# 테스트 데이터 삭제
redis-cli -n 4 DEL "test:location:001"
```

### 10.3 지리정보 캐시 테스트
```bash
# 주변 검색 캐시 테스트
redis-cli -n 4 SET "test:nearby:37.5665:126.9780:1000:ALL" '{"places":[{"name":"테스트장소","distance":100}]}' EX 300

# 주변 검색 캐시 조회 테스트
redis-cli -n 4 GET "test:nearby:37.5665:126.9780:1000:ALL"

# JSON 파싱 테스트 (jq 사용)
redis-cli -n 4 GET "test:nearby:37.5665:126.9780:1000:ALL" | jq '.places[0].name'

# 테스트 데이터 정리
redis-cli -n 4 DEL "test:nearby:37.5665:126.9780:1000:ALL"
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
redis-cli -n 4 INFO memory

# 큰 키 찾기
redis-cli -n 4 --bigkeys

# 불필요한 캐시 정리
redis-cli -n 4 KEYS "location:search:*" | head -100 | xargs redis-cli -n 4 DEL  # 오래된 검색 결과만 삭제

# 전체 캐시 정리 (주의: 모든 데이터 삭제)
# redis-cli -n 4 FLUSHDB
```

### 11.3 성능 저하 대응
```bash
# 느린 쿼리 로그 확인
redis-cli -n 4 SLOWLOG GET 10

# 클라이언트 연결 확인
redis-cli -n 4 CLIENT LIST

# 캐시 히트율 확인
redis-cli -n 4 INFO stats | grep keyspace_hits

# 지리정보 캐시 최적화
redis-cli -n 4 KEYS "location:nearby:*" | wc -l  # 주변 검색 캐시 수
redis-cli -n 4 KEYS "location:search:*" | wc -l   # 키워드 검색 캐시 수
```

## 12. 설치 완료 확인

### 12.1 설치 검증 체크리스트
- [ ] Redis 서비스 정상 구동
- [ ] 데이터베이스 4번 사용 확인
- [ ] 기본 설정 완료 
- [ ] 테스트 데이터 저장/조회 성공
- [ ] TTL 설정 동작 확인
- [ ] 캐시 키 패턴 검증
- [ ] 지리정보 캐시 동작 확인
- [ ] JSON 데이터 처리 확인
- [ ] 성능 모니터링 스크립트 동작
- [ ] 백업/복원 스크립트 준비
- [ ] 무효화 스크립트 동작 확인

### 12.2 최종 상태 확인
```bash
# 전체 캐시 상태 요약
echo "=== Location Cache Status ==="
echo "Redis Version: $(redis-cli --version)"
echo "Database: 4"
echo "Keys Count: $(redis-cli -n 4 DBSIZE)"
echo "Memory Usage: $(redis-cli -n 4 INFO memory | grep used_memory_human | cut -d: -f2)"
echo "Cache Hit Rate: $(redis-cli -n 4 INFO stats | grep keyspace_hits)"
echo "Place Cache: $(redis-cli -n 4 KEYS 'location:place:*' | wc -l) entries"
echo "Search Cache: $(redis-cli -n 4 KEYS 'location:nearby:*' 'location:search:*' | wc -l) entries"
echo "Recommendation Cache: $(redis-cli -n 4 KEYS 'location:recommendation:*' | wc -l) entries"
echo "Status: Ready"
```

설치가 완료되었습니다. Location 서비스 캐시가 준비되었으며, Redis 데이터베이스 4번을 사용하여 장소 검색, 지리정보 처리, AI 추천 정보 캐싱을 위한 모든 구성이 완료되었습니다.