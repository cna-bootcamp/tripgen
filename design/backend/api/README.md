# TripGen API 설계서

## 개요
본 문서는 TripGen 서비스의 API 설계를 정의합니다. OpenAPI 3.0 명세를 따르며, 각 마이크로서비스별로 독립적인 API를 제공합니다.

## API 문서 구성

### 1. User Service API (`user-service-api.yaml`)
- **포트**: 8081
- **기본 경로**: `/user`
- **주요 기능**:
  - 회원가입/로그인/로그아웃
  - 프로필 관리
  - 중복 확인
  - JWT 기반 인증

### 2. Trip Service API (`trip-service-api.yaml`)
- **포트**: 8082
- **기본 경로**: `/trip`
- **주요 기능**:
  - 여행 관리 (CRUD)
  - 멤버 관리
  - 여행지 설정
  - AI 일정 생성 요청
  - 일정 조회/수정/재생성
  - 일정 내보내기

### 3. AI Service API (`ai-service-api.yaml`)
- **포트**: 8083
- **기본 경로**: `/ai`
- **주요 기능**:
  - AI 일정 생성
  - 진행 상태 실시간 조회
  - 장소별 AI 추천정보 생성
  - 날씨 영향 분석

### 4. Location Service API (`location-service-api.yaml`)
- **포트**: 8084
- **기본 경로**: `/location`
- **주요 기능**:
  - 주변 장소 검색
  - 키워드 검색
  - 장소 상세정보 조회
  - 실시간 영업시간 확인

## API 테스트 방법

### 1. Swagger Editor 사용
1. https://editor.swagger.io/ 접속
2. 각 YAML 파일 내용을 복사하여 붙여넣기
3. 우측 패널에서 API 문서 확인 및 테스트

### 2. SwaggerHub Mock 서버
각 API는 SwaggerHub Mock 서버 URL이 설정되어 있어 실제 구현 전에도 테스트 가능:
- User Service: `https://virtserver.swaggerhub.com/TRIPGEN/user-service/1.0.0`
- Trip Service: `https://virtserver.swaggerhub.com/TRIPGEN/trip-service/1.0.0`
- AI Service: `https://virtserver.swaggerhub.com/TRIPGEN/ai-service/1.0.0`
- Location Service: `https://virtserver.swaggerhub.com/TRIPGEN/location-service/1.0.0`

## 주요 설계 특징

### 1. 유저스토리 연결
모든 API 엔드포인트는 `x-user-story` 필드를 통해 해당 유저스토리와 명확히 연결됩니다.

### 2. 컨트롤러 매핑
`x-controller` 필드를 통해 구현 시 사용할 컨트롤러 클래스를 지정합니다.

### 3. 상세한 예제
모든 요청/응답에 실제 사용 가능한 예제 데이터가 포함되어 있습니다.

### 4. 일관된 에러 처리
모든 서비스가 동일한 에러 응답 형식을 사용합니다.

### 5. 페이지네이션 지원
목록 조회 API는 일관된 페이지네이션 파라미터를 제공합니다.

## 서비스 간 통신

### 동기 통신
- User → Trip: 사용자 정보 확인
- Trip → AI: 일정 생성 상태 확인
- Location → AI: 추천정보 요청

### 비동기 통신 (Message Queue)
- Trip → AI: 일정 생성 요청
- AI → Location: 장소 정보 대량 조회

### 캐시 활용
- Redis를 통한 서비스 간 데이터 공유
- 성능 최적화 및 의존성 최소화

## API 보안

### 인증
- JWT Bearer Token 사용
- API Gateway에서 토큰 검증

### 권한
- 사용자별 리소스 접근 제어
- 서비스 간 통신은 내부 네트워크에서만 허용

## 버전 관리
- 현재 버전: 1.0.0
- URL 경로에 버전 포함 고려 (향후 `/v1`, `/v2` 등)

## 문법 검증
모든 API 파일은 swagger-cli를 통해 검증되었습니다:
```bash
swagger-cli validate design/backend/api/*.yaml
```