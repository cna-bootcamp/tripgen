# TripGen 논리 아키텍처 설계서

**프로젝트**: 여행 일정 생성 서비스 (TripGen)  
**작성자**: 김개발/테키, 정백엔드/서버맨  
**작성일**: 2025-07-27  
**버전**: 1.0

## 1. 개요

본 문서는 TripGen 서비스의 논리 아키텍처를 정의합니다. 클라우드 아키텍처 패턴 설계서를 기반으로 서비스 간 관계와 사용자 플로우를 중심으로 설계되었습니다.

### 1.1 설계 원칙
- **Context Map 스타일**: 서비스 내부 구조는 생략하고 서비스 간 관계에 집중
- **사용자 플로우 중심**: 주요 사용자 시나리오별 처리 순서 명시
- **명확한 의존성 표현**: 동기/비동기, 필수/선택적 의존성 구분
- **캐시 우선 전략**: 성능 최적화를 위한 Redis 캐시 활용

### 1.2 핵심 컴포넌트
- **Client Layer**: 모바일 클라이언트
- **Gateway Layer**: API Gateway (인증, 라우팅, 로드밸런싱)
- **Service Layer**: User, Trip, AI, Location 서비스
- **Data Layer**: Redis Cache, Message Queue
- **External Layer**: Claude API, 카카오맵 API, 구글맵 API, 날씨 API

## 2. 서비스 아키텍처

### 2.1 서비스별 책임

#### User Service
- **책임**: 사용자 인증/인가, 프로필 관리, 세션 관리
- **주요 기능**: 회원가입, 로그인, 프로필 수정
- **데이터**: 사용자 정보, 인증 토큰
- **외부 의존성**: 없음

#### Trip Service
- **책임**: 여행 계획 관리, AI 일정 생성 요청, 일정 조회/수정
- **주요 기능**: 여행 생성, 멤버 관리, 여행지 설정, 일정 관리
- **데이터**: 여행 정보, 멤버 정보, 일정 데이터
- **외부 의존성**: AI Service (일정 생성)

#### AI Service
- **책임**: AI 기반 일정 생성, AI 추천정보 제공
- **주요 기능**: 일정 생성, 건강상태 고려, 날씨 반영, AI 추천정보 생성
- **데이터**: 생성된 일정, AI 추천 정보
- **내부 의존성**: Location Service (장소 정보), Trip Service (여행 정보)
- **외부 의존성**: Claude API

#### Location Service
- **책임**: 장소 검색, 장소 상세정보 제공, 리뷰 통합
- **주요 기능**: 주변 장소 검색, 장소 상세정보, 리뷰 수집, AI 추천정보 요청
- **데이터**: 장소 정보, 리뷰 데이터
- **내부 의존성**: AI Service (AI 추천정보)
- **외부 의존성**: 카카오맵 API, 구글맵 API, 날씨 API

### 2.2 서비스 간 통신 전략

#### 동기 통신 (실선 →)
- **즉시 응답 필요**: 인증 확인, 기본 정보 조회
- **단순 조회**: 사용자 정보, 여행 목록
- **필수 의존성**: 서비스 동작에 필수적인 정보

#### 비동기 통신 (점선 ->>)
- **장시간 처리**: AI 일정 생성 (5초 이상)
- **Fire-and-forget**: 로그 기록, 통계 수집
- **Message Queue 활용**: 작업 큐잉, 부하 분산

#### Cache-Aside with Async Fallback
- **AI ↔ Location 통신**: 캐시 우선 조회
- **캐시 히트**: 즉시 응답 (목표 80%)
- **캐시 미스**: 기본 정보 사용 + 비동기 갱신

## 3. 주요 사용자 플로우

### 3.1 여행 계획 생성 플로우
1. **사용자 인증**: Mobile → Gateway → User Service
2. **여행 기본정보 설정**: Mobile → Gateway → Trip Service
3. **멤버 정보 입력**: Trip Service → Cache (User 정보)
4. **여행지 설정**: Mobile → Gateway → Trip Service
5. **AI 일정 생성 요청**: Trip Service → Message Queue → AI Service
6. **장소 정보 조회**: AI Service → Cache → Location Service (async)
7. **일정 생성 완료**: AI Service → Cache → Trip Service
8. **결과 조회**: Mobile → Gateway → Trip Service → Cache

### 3.2 주변 장소 검색 플로우
1. **검색 요청**: Mobile → Gateway → Location Service
2. **캐시 확인**: Location Service → Cache
3. **외부 API 호출**: Location Service → 카카오맵/구글맵 API
4. **AI 추천정보 요청**: Location Service → Message Queue → AI Service
5. **AI 추천정보 생성**: AI Service → Claude API
6. **결과 캐싱**: Location Service → Cache
7. **응답 반환**: Location Service → Gateway → Mobile

### 3.3 생성된 일정 조회 플로우
1. **조회 요청**: Mobile → Gateway → Trip Service
2. **캐시 확인**: Trip Service → Cache
3. **장소 상세정보**: Trip Service → Location Service (선택적)
4. **응답 조합**: Trip Service → Gateway → Mobile

## 4. 데이터 흐름 및 캐싱 전략

### 4.1 캐시 계층
- **L1 캐시**: 서비스 내부 메모리 캐시 (1분)
- **L2 캐시**: Redis 공유 캐시 (24시간)
- **캐시 키 전략**: `service:entity:id` 형식

### 4.2 캐시 대상 데이터
- **User Service**: 세션 정보, 프로필 데이터
- **Trip Service**: 여행 목록, 생성된 일정
- **AI Service**: AI 생성 결과, 추천 정보
- **Location Service**: 장소 정보, 리뷰 데이터

### 4.3 캐시 갱신 전략
- **Write-Through**: 중요 데이터 (사용자, 여행 정보)
- **Write-Behind**: 통계, 로그 데이터
- **TTL 기반**: 외부 API 데이터 (장소, 날씨)

## 5. 확장성 및 성능 고려사항

### 5.1 수평 확장 전략
- **Service Layer**: 서비스별 독립적 스케일링
- **Cache Layer**: Redis Cluster 구성
- **Queue Layer**: 파티션 기반 확장

### 5.2 성능 최적화
- **Priority Queue**: AI 작업 우선순위 관리
- **Circuit Breaker**: 외부 API 장애 대응
- **Connection Pooling**: DB/Cache 연결 최적화

### 5.3 모니터링 포인트
- **응답 시간**: 각 서비스별 P95, P99
- **캐시 히트율**: 목표 80% 이상
- **큐 대기 시간**: AI 작업 큐 모니터링
- **외부 API 상태**: Circuit Breaker 상태

## 6. 보안 고려사항

### 6.1 인증/인가
- **JWT 토큰**: Gateway에서 검증
- **서비스 간 통신**: 내부 네트워크, mTLS
- **API 키 관리**: 환경 변수, Secret Manager

### 6.2 데이터 보호
- **전송 구간**: HTTPS 필수
- **저장 데이터**: 민감 정보 암호화
- **로그**: PII 마스킹 처리

## 7. 다이어그램

논리 아키텍처의 상세 다이어그램은 `logical-architecture.mmd` 파일을 참조하세요.

### 7.2 Mermaid 다이어그램 테스트 방법
1. https://mermaid.live/edit 에 접속
2. `logical-architecture.mmd` 파일의 내용을 복사하여 붙여넣기
3. 다이어그램이 올바르게 렌더링되는지 확인

### 7.1 다이어그램 범례
- **실선 화살표 (→)**: 동기적 의존성 (필수)
- **점선 화살표 (-.->)**: 비동기 의존성
- **양방향 화살표 (↔)**: 상호 의존성
- **색상 구분**: 서비스별 고유 색상 적용
- **플로우 라벨 형식**: [요청서비스약어]액션 (예: [Trip]AI 일정 생성 요청)