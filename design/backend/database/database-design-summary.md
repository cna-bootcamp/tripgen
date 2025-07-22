# 여행 일정 생성 서비스 - 데이터 설계 요약

## 📋 개요

본 문서는 마이크로서비스 아키텍처 기반 여행 일정 생성 서비스의 전체 데이터 설계를 요약합니다.

### 🎯 설계 목표
- **마이크로서비스 원칙**: 서비스별 독립적인 데이터베이스
- **데이터 독립성**: 서비스 간 직접 DB 참조 금지
- **성능 최적화**: 캐시 계층을 통한 응답 시간 개선
- **확장성**: 분산 환경에서의 수평적 확장 지원

### 🏗️ 전체 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Profile Service │    │ Itinerary Service│    │ Location Service│
│                 │    │                 │    │                 │
│  profile_db     │    │  itinerary_db   │    │  location_db    │
│  (PostgreSQL)   │    │  (PostgreSQL)   │    │  (PostgreSQL)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   Redis Cache   │
                    │   (Shared)      │
                    └─────────────────┘
```

## 🗄️ 서비스별 데이터베이스 설계

### 1. 프로파일 서비스 (Profile Service)

#### 데이터베이스: `profile_db`
- **엔진**: PostgreSQL
- **테이블 수**: 5개
- **주요 엔티티**: Member, Trip, TransportSetting

#### 핵심 테이블
| 테이블명 | 주요 컬럼 | 용도 |
|---------|-----------|------|
| `members` | id, name, age, health_status | 여행 멤버 관리 |
| `member_preferences` | member_id, preference_type | 멤버 선호도 (다대다) |
| `trips` | id, trip_name, start_date, end_date | 여행 기본 정보 |
| `trip_members` | trip_id, member_id | 여행-멤버 매핑 |
| `transport_settings` | id, trip_id, transport_type | 교통수단 설정 |

#### 특징
- **정규화**: 3NF 적용으로 데이터 중복 최소화
- **비정규화**: Location, Accommodation을 Trip에 임베디드
- **인덱스**: 복합 인덱스로 조회 성능 최적화
- **제약조건**: CHECK 제약으로 데이터 유효성 보장

---

### 2. 일정 서비스 (Itinerary Service)

#### 데이터베이스: `itinerary_db`
- **엔진**: PostgreSQL
- **테이블 수**: 7개
- **주요 엔티티**: Itinerary, DailyActivity, Attachment, Route

#### 핵심 테이블
| 테이블명 | 주요 컬럼 | 용도 |
|---------|-----------|------|
| `itineraries` | id, trip_id, date, status | 일정 기본 정보 |
| `daily_activities` | id, itinerary_id, place_id | 일일 활동 |
| `attachments` | id, place_id, type, content | 사진/메모 첨부 |
| `routes` | id, from_place_id, to_place_id | 경로 정보 |
| `itinerary_jobs` | id, job_type, status | 비동기 작업 관리 |
| `saga_transactions` | id, saga_type, step | 분산 트랜잭션 |
| `place_cache` | place_id, cached_data | 외부 서비스 캐시 |

#### 특징
- **파티셔닝**: 날짜 기반 파티셔닝으로 성능 최적화
- **비동기 처리**: Job Queue 테이블로 AI 일정 생성 지원
- **사가 패턴**: 분산 트랜잭션 관리
- **외부 의존성 최소화**: place_cache로 장소 서비스 데이터 캐싱

---

### 3. 장소 서비스 (Location Service)

#### 데이터베이스: `location_db`
- **엔진**: PostgreSQL + PostGIS
- **테이블 수**: 8개
- **주요 엔티티**: Place, PlaceDetails, Review

#### 핵심 테이블
| 테이블명 | 주요 컬럼 | 용도 |
|---------|-----------|------|
| `places` | id, name, location, category | 장소 기본 정보 |
| `place_details` | place_id, contact, business_hours | 장소 상세 정보 |
| `business_hours` | place_id, day_of_week, open_time | 영업시간 |
| `reviews` | id, place_id, rating, text | 리뷰 |
| `region_info` | region_code, characteristics | 지역 정보 |
| `place_search_cache` | search_key, results | 검색 캐시 |
| `translation_cache` | text_hash, translated_text | 번역 캐시 |

#### 특징
- **공간 데이터**: PostGIS로 위치 기반 검색 최적화
- **JSONB**: 유연한 데이터 구조 지원
- **공간 인덱스**: GIST 인덱스로 지리적 검색 최적화
- **캐시 테이블**: 외부 API 호출 최소화

## 🔄 캐시 계층 설계

### Redis Cluster 구성
- **노드**: 6개 (Master 3, Slave 3)
- **샤딩**: 16,384개 해시 슬롯
- **고가용성**: 자동 페일오버

### 캐시 전략

#### 키 네이밍 규칙
```
{service}:{domain}:{identifier}:{version}
```

#### TTL 전략
| 데이터 유형 | TTL | 갱신 전략 |
|------------|-----|-----------|
| 회원 정보 | 1시간 | Write Through |
| 여행 정보 | 30분 | Write Through |
| 장소 정보 | 2시간 | Cache Aside |
| 검색 결과 | 30분 | Cache Aside |
| 경로 정보 | 4시간 | Cache Aside |
| 번역 결과 | 24시간 | Cache Aside |

### 캐시 무효화
- **즉시 무효화**: 데이터 변경 시 관련 캐시 삭제
- **태그 기반**: 그룹 단위 캐시 무효화
- **Pub/Sub**: Redis 메시징으로 캐시 동기화

## 📊 데이터 흐름 및 일관성

### 서비스 간 데이터 공유

#### 1. 프로파일 → 일정 서비스
```
Trip 정보 요청 → Redis Cache 확인 → Profile Service API 호출 → Cache 저장
```

#### 2. 일정 → 장소 서비스
```
Place 정보 요청 → place_cache 확인 → Location Service API 호출 → 로컬 캐시 저장
```

### 데이터 일관성 보장

#### Eventually Consistent
- 캐시와 원본 DB 간 일관성은 최종적 일관성 보장
- TTL 만료 시 자동으로 최신 데이터 반영

#### Strong Consistency (필요시)
- 결제, 예약 등 중요한 데이터는 직접 DB 조회
- 캐시 우회 옵션 제공

## 🔧 성능 최적화 전략

### 1. 인덱스 전략
- **복합 인덱스**: 다중 컬럼 조건 최적화
- **커버링 인덱스**: 디스크 I/O 최소화
- **부분 인덱스**: 조건부 인덱스로 크기 최적화
- **공간 인덱스**: 위치 기반 검색 최적화

### 2. 파티셔닝
- **수평 파티셔닝**: 날짜/지역 기반
- **수직 파티셔닝**: 자주 사용되는 컬럼 분리
- **샤딩**: 서비스별 데이터 분산

### 3. 캐시 최적화
- **Connection Pooling**: Redis 연결 재사용
- **Pipeline**: 다중 명령어 배치 처리
- **압축**: JSON → MessagePack으로 크기 최소화

## 🚀 확장성 고려사항

### 1. 수평 확장
- **Read Replica**: 읽기 전용 복제본으로 부하 분산
- **Database Sharding**: 데이터베이스 수평 분할
- **Cache Scaling**: Redis Cluster 노드 추가

### 2. 글로벌 배포
- **Multi-Region**: 지역별 데이터베이스 복제
- **CDN**: 정적 데이터 글로벌 캐싱
- **Geo-Partitioning**: 지역별 데이터 분할

## 🛡️ 보안 및 백업

### 보안 정책
- **데이터 암호화**: 민감 정보 AES-256 암호화
- **접근 제어**: 서비스별 DB 사용자 분리
- **네트워크 격리**: VPC 내부 통신만 허용

### 백업 전략
- **일일 백업**: 전체 데이터베이스 백업
- **실시간 복제**: 지역 간 데이터 복제
- **포인트 인 타임 복구**: 트랜잭션 로그 기반

## 📈 모니터링 메트릭

### 데이터베이스 메트릭
- Connection Pool 사용률
- Query 실행 시간 (P95, P99)
- Lock 대기 시간
- Index Hit Ratio

### 캐시 메트릭
- Cache Hit Ratio
- Memory Usage
- Network Latency
- Eviction Rate

## 🔍 데이터 품질 관리

### 데이터 검증
- **스키마 검증**: JSON Schema 기반 데이터 유효성
- **참조 무결성**: 외래키 제약조건
- **비즈니스 룰**: 도메인 로직 기반 검증

### 데이터 정합성
- **정기 점검**: 배치 작업으로 데이터 일관성 검증
- **실시간 모니터링**: 이상 데이터 자동 감지
- **자동 복구**: 데이터 불일치 시 자동 수정

## 📋 파일 구조 요약

```
design/backend/database/
├── profile.txt                 # 프로파일 서비스 DB 설계서
├── profile-erd.txt             # 프로파일 서비스 ERD (PlantUML)
├── itinerary.txt               # 일정 서비스 DB 설계서
├── itinerary-erd.txt           # 일정 서비스 ERD (PlantUML)
├── location.txt                # 장소 서비스 DB 설계서
├── location-erd.txt            # 장소 서비스 ERD (PlantUML)
├── cache.txt                   # 캐시 DB 설계서
└── database-design-summary.md  # 이 문서 (데이터 설계 요약)
```

## ✅ 결론

본 데이터 설계는 다음 원칙을 만족합니다:

1. **마이크로서비스 원칙**: 서비스별 데이터 독립성 보장
2. **성능 최적화**: 다층 캐시와 인덱스 전략
3. **확장성**: 수평적 확장 가능한 구조
4. **일관성**: 적절한 일관성 모델 적용
5. **가용성**: 고가용성 클러스터 구성
6. **보안성**: 데이터 암호화 및 접근 제어

이 설계를 통해 안정적이고 확장 가능한 여행 일정 생성 서비스의 데이터 계층을 구축할 수 있습니다.