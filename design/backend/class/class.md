# 클래스 설계 통합 문서

## 패키지 구조도

```
com.unicorn.tripgen
├── common
│   ├── exception
│   │   ├── BaseException.java
│   │   ├── BusinessException.java
│   │   ├── ValidationException.java
│   │   ├── InternalServerException.java
│   │   ├── UnauthorizedException.java
│   │   ├── ForbiddenException.java
│   │   └── NotFoundException.java
│   ├── dto
│   │   ├── BaseResponse.java
│   │   ├── ApiResponse.java
│   │   ├── ErrorResponse.java
│   │   └── PageResponse.java
│   ├── entity
│   │   ├── BaseEntity.java
│   │   └── BaseAuditEntity.java
│   ├── util
│   │   ├── DateUtils.java
│   │   ├── StringUtils.java
│   │   ├── ValidationUtils.java
│   │   └── SecurityUtils.java
│   └── constant
│       ├── ErrorCodes.java
│       └── CommonMessages.java
├── user
│   ├── controller
│   │   └── UserController.java
│   ├── service
│   │   ├── UserService.java
│   │   ├── UserServiceImpl.java
│   │   ├── TokenService.java
│   │   ├── JwtTokenService.java
│   │   ├── PasswordService.java
│   │   ├── BCryptPasswordService.java
│   │   ├── FileStorageService.java
│   │   └── LocalFileStorageService.java
│   ├── repository
│   │   ├── UserRepository.java
│   │   └── UserRepositoryImpl.java
│   ├── entity
│   │   ├── User.java
│   │   └── UserStatus.java
│   ├── dto
│   │   ├── RegisterRequest.java
│   │   ├── RegisterResponse.java
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── LogoutRequest.java
│   │   ├── UserProfile.java
│   │   ├── UpdateProfileRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   ├── AvatarUploadResponse.java
│   │   ├── UsernameCheckResponse.java
│   │   └── EmailCheckResponse.java
│   └── config
│       ├── UserSecurityConfig.java
│       ├── JwtTokenProvider.java
│       ├── PasswordEncoder.java
│       └── FileUploadConfig.java
├── location
│   ├── controller
│   │   └── LocationController.java
│   ├── service
│   │   ├── LocationService.java
│   │   ├── LocationServiceImpl.java
│   │   ├── ExternalApiService.java
│   │   ├── ExternalApiServiceImpl.java
│   │   ├── WeatherService.java
│   │   ├── WeatherServiceImpl.java
│   │   ├── RouteService.java
│   │   ├── RouteServiceImpl.java
│   │   ├── CacheService.java
│   │   └── RedisCacheService.java
│   ├── repository
│   │   ├── LocationRepository.java
│   │   ├── LocationRepositoryImpl.java
│   │   ├── RouteRepository.java
│   │   ├── RouteRepositoryImpl.java
│   │   ├── WeatherRepository.java
│   │   └── WeatherRepositoryImpl.java
│   ├── entity
│   │   ├── Location.java
│   │   ├── Route.java
│   │   ├── Weather.java
│   │   ├── LocationType.java
│   │   └── TransportType.java
│   ├── dto
│   │   ├── SearchLocationRequest.java
│   │   ├── LocationSearchResponse.java
│   │   ├── LocationDetailResponse.java
│   │   ├── WeatherRequest.java
│   │   ├── WeatherResponse.java
│   │   ├── RouteRequest.java
│   │   ├── RouteResponse.java
│   │   ├── NearbyPlacesRequest.java
│   │   └── NearbyPlacesResponse.java
│   ├── client
│   │   ├── GooglePlacesClient.java
│   │   ├── KakaoMapClient.java
│   │   └── WeatherApiClient.java
│   └── config
│       ├── LocationConfig.java
│       ├── ExternalApiConfig.java
│       └── RedisConfig.java
├── ai
│   ├── controller
│   │   ├── AIScheduleController.java
│   │   └── AIRecommendationController.java
│   ├── service
│   │   ├── AIScheduleService.java
│   │   ├── AIScheduleServiceImpl.java
│   │   ├── AIRecommendationService.java
│   │   ├── AIRecommendationServiceImpl.java
│   │   ├── AIJobService.java
│   │   ├── AIJobServiceImpl.java
│   │   ├── AIModelService.java
│   │   └── AIModelServiceImpl.java
│   ├── repository
│   │   ├── AIScheduleRepository.java
│   │   ├── AIScheduleRepositoryImpl.java
│   │   ├── AIJobRepository.java
│   │   ├── AIJobRepositoryImpl.java
│   │   ├── AIRecommendationRepository.java
│   │   └── AIRecommendationRepositoryImpl.java
│   ├── entity
│   │   ├── AISchedule.java
│   │   ├── AIJob.java
│   │   ├── AIRecommendation.java
│   │   ├── JobStatus.java
│   │   └── AIModelType.java
│   ├── dto
│   │   ├── GenerateScheduleRequest.java
│   │   ├── GenerateScheduleResponse.java
│   │   ├── ScheduleJobStatusResponse.java
│   │   ├── GenerateRecommendationRequest.java
│   │   ├── RecommendationResponse.java
│   │   └── RecommendationJobStatusResponse.java
│   ├── client
│   │   ├── AIModelClient.java
│   │   ├── OpenAIClient.java
│   │   ├── ClaudeClient.java
│   │   ├── UserServiceClient.java
│   │   ├── LocationServiceClient.java
│   │   └── WeatherServiceClient.java
│   └── config
│       ├── AIConfig.java
│       ├── AIModelConfig.java
│       └── MessageQueueConfig.java
└── trip
    ├── biz
    │   ├── domain
    │   │   ├── Trip.java
    │   │   ├── Member.java
    │   │   ├── Destination.java
    │   │   ├── Schedule.java
    │   │   ├── SchedulePlace.java
    │   │   ├── WeatherInfo.java
    │   │   ├── Transportation.java
    │   │   ├── HealthConsideration.java
    │   │   ├── TransportMode.java
    │   │   ├── TripStatus.java
    │   │   ├── Gender.java
    │   │   ├── HealthStatus.java
    │   │   └── Preference.java
    │   ├── usecase
    │   │   ├── in
    │   │   │   ├── TripUseCase.java
    │   │   │   ├── MemberUseCase.java
    │   │   │   ├── DestinationUseCase.java
    │   │   │   └── ScheduleUseCase.java
    │   │   └── out
    │   │       ├── TripRepository.java
    │   │       ├── MemberRepository.java
    │   │       ├── DestinationRepository.java
    │   │       ├── ScheduleRepository.java
    │   │       ├── UserServiceClient.java
    │   │       ├── LocationServiceClient.java
    │   │       └── AiServiceClient.java
    │   ├── service
    │   │   ├── ScheduleGenerationService.java
    │   │   └── ScheduleExportService.java
    │   ├── validator
    │   │   ├── TripValidator.java
    │   │   ├── MemberValidator.java
    │   │   ├── DestinationValidator.java
    │   │   └── ScheduleValidator.java
    │   └── dto
    │       ├── CreateTripRequest.java
    │       ├── CreateTripResponse.java
    │       ├── TripDetailResponse.java
    │       ├── UpdateTripRequest.java
    │       ├── AddMemberRequest.java
    │       ├── MemberResponse.java
    │       ├── UpdateMemberRequest.java
    │       ├── AddDestinationRequest.java
    │       ├── DestinationResponse.java
    │       ├── UpdateDestinationRequest.java
    │       ├── GenerateScheduleRequest.java
    │       ├── ScheduleResponse.java
    │       ├── UpdateScheduleRequest.java
    │       ├── ExportScheduleRequest.java
    │       └── ScheduleExportResponse.java
    └── infra
        ├── controller
        │   ├── TripController.java
        │   ├── MemberController.java
        │   ├── DestinationController.java
        │   └── ScheduleController.java
        ├── gateway
        │   ├── repository
        │   │   ├── TripRepositoryImpl.java
        │   │   ├── MemberRepositoryImpl.java
        │   │   ├── DestinationRepositoryImpl.java
        │   │   └── ScheduleRepositoryImpl.java
        │   ├── entity
        │   │   ├── TripEntity.java
        │   │   ├── MemberEntity.java
        │   │   ├── DestinationEntity.java
        │   │   └── ScheduleEntity.java
        │   ├── rowmapper
        │   │   ├── TripRowMapper.java
        │   │   ├── MemberRowMapper.java
        │   │   ├── DestinationRowMapper.java
        │   │   └── ScheduleRowMapper.java
        │   ├── client
        │   │   ├── UserServiceClientImpl.java
        │   │   ├── LocationServiceClientImpl.java
        │   │   └── AiServiceClientImpl.java
        │   └── TripGateway.java
        ├── config
        │   ├── TripConfiguration.java
        │   └── TripExceptionHandler.java
        └── TripApplication.java
```

## API 엔드포인트 매핑표

### User Service API
| HTTP Method | Path | Controller Method | API Title |
|-------------|------|-------------------|-----------|
| POST | /register | registerUser() | 회원가입 |
| POST | /login | loginUser() | 로그인 |
| POST | /logout | logoutUser() | 로그아웃 |
| GET | /profile | getProfile() | 프로필 조회 |
| PUT | /profile | updateProfile() | 프로필 수정 |
| POST | /profile/avatar | uploadAvatar() | 프로필 이미지 업로드 |
| PUT | /profile/password | changePassword() | 비밀번호 변경 |
| GET | /check/username/{username} | checkUsername() | 아이디 중복 확인 |
| GET | /check/email/{email} | checkEmail() | 이메일 중복 확인 |

### Location Service API
| HTTP Method | Path | Controller Method | API Title |
|-------------|------|-------------------|-----------|
| GET | /search | searchLocation() | 위치 검색 |
| GET | /{locationId} | getLocationDetail() | 위치 상세 정보 조회 |
| GET | /weather | getWeather() | 날씨 정보 조회 |
| GET | /route | getRoute() | 경로 정보 조회 |
| GET | /nearby | getNearbyPlaces() | 주변 장소 검색 |

### AI Service API
| HTTP Method | Path | Controller Method | API Title |
|-------------|------|-------------------|-----------|
| POST | /schedule/generate | generateSchedule() | 일정 생성 요청 |
| GET | /schedule/status/{jobId} | getScheduleJobStatus() | 일정 생성 상태 조회 |
| POST | /recommendation/generate | generateRecommendation() | 추천 정보 생성 요청 |
| GET | /recommendation/status/{jobId} | getRecommendationJobStatus() | 추천 정보 생성 상태 조회 |

### Trip Service API
| HTTP Method | Path | Controller Method | API Title |
|-------------|------|-------------------|-----------|
| POST | / | createTrip() | 여행 생성 |
| GET | /{tripId} | getTripDetail() | 여행 상세 조회 |
| PUT | /{tripId} | updateTrip() | 여행 정보 수정 |
| DELETE | /{tripId} | deleteTrip() | 여행 삭제 |
| POST | /{tripId}/members | addMember() | 멤버 추가 |
| GET | /{tripId}/members | getMembers() | 멤버 목록 조회 |
| PUT | /{tripId}/members/{memberId} | updateMember() | 멤버 정보 수정 |
| DELETE | /{tripId}/members/{memberId} | deleteMember() | 멤버 삭제 |
| POST | /{tripId}/destinations | addDestination() | 여행지 추가 |
| GET | /{tripId}/destinations | getDestinations() | 여행지 목록 조회 |
| PUT | /{tripId}/destinations/{destinationId} | updateDestination() | 여행지 정보 수정 |
| DELETE | /{tripId}/destinations/{destinationId} | deleteDestination() | 여행지 삭제 |
| POST | /{tripId}/schedule/generate | generateSchedule() | 일정 생성 |
| GET | /{tripId}/schedule | getSchedule() | 일정 조회 |
| PUT | /{tripId}/schedule | updateSchedule() | 일정 수정 |
| POST | /{tripId}/schedule/export | exportSchedule() | 일정 내보내기 |

## API 스키마 매핑표

### User Service Schema
| API Schema | DTO Class | 용도 |
|------------|-----------|------|
| RegisterRequest | RegisterRequest | 회원가입 요청 |
| RegisterResponse | RegisterResponse | 회원가입 응답 |
| LoginRequest | LoginRequest | 로그인 요청 |
| LoginResponse | LoginResponse | 로그인 응답 |
| UserProfile | UserProfile | 사용자 프로필 |
| UpdateProfileRequest | UpdateProfileRequest | 프로필 수정 요청 |
| ChangePasswordRequest | ChangePasswordRequest | 비밀번호 변경 요청 |
| - | AvatarUploadResponse | 아바타 업로드 응답 |
| - | UsernameCheckResponse | 아이디 중복 확인 응답 |
| - | EmailCheckResponse | 이메일 중복 확인 응답 |

### Location Service Schema
| API Schema | DTO Class | 용도 |
|------------|-----------|------|
| SearchLocationRequest | SearchLocationRequest | 위치 검색 요청 |
| LocationSearchResponse | LocationSearchResponse | 위치 검색 응답 |
| LocationDetailResponse | LocationDetailResponse | 위치 상세 정보 응답 |
| WeatherRequest | WeatherRequest | 날씨 정보 요청 |
| WeatherResponse | WeatherResponse | 날씨 정보 응답 |
| RouteRequest | RouteRequest | 경로 정보 요청 |
| RouteResponse | RouteResponse | 경로 정보 응답 |
| NearbyPlacesRequest | NearbyPlacesRequest | 주변 장소 검색 요청 |
| NearbyPlacesResponse | NearbyPlacesResponse | 주변 장소 검색 응답 |

### AI Service Schema
| API Schema | DTO Class | 용도 |
|------------|-----------|------|
| GenerateScheduleRequest | GenerateScheduleRequest | 일정 생성 요청 |
| GenerateScheduleResponse | GenerateScheduleResponse | 일정 생성 응답 |
| ScheduleJobStatusResponse | ScheduleJobStatusResponse | 일정 생성 상태 응답 |
| GenerateRecommendationRequest | GenerateRecommendationRequest | 추천 정보 생성 요청 |
| RecommendationResponse | RecommendationResponse | 추천 정보 응답 |
| RecommendationJobStatusResponse | RecommendationJobStatusResponse | 추천 정보 생성 상태 응답 |

### Trip Service Schema
| API Schema | DTO Class | 용도 |
|------------|-----------|------|
| CreateTripRequest | CreateTripRequest | 여행 생성 요청 |
| CreateTripResponse | CreateTripResponse | 여행 생성 응답 |
| TripDetailResponse | TripDetailResponse | 여행 상세 정보 응답 |
| UpdateTripRequest | UpdateTripRequest | 여행 정보 수정 요청 |
| AddMemberRequest | AddMemberRequest | 멤버 추가 요청 |
| MemberResponse | MemberResponse | 멤버 정보 응답 |
| UpdateMemberRequest | UpdateMemberRequest | 멤버 정보 수정 요청 |
| AddDestinationRequest | AddDestinationRequest | 여행지 추가 요청 |
| DestinationResponse | DestinationResponse | 여행지 정보 응답 |
| UpdateDestinationRequest | UpdateDestinationRequest | 여행지 정보 수정 요청 |
| GenerateScheduleRequest | GenerateScheduleRequest | 일정 생성 요청 |
| ScheduleResponse | ScheduleResponse | 일정 정보 응답 |
| UpdateScheduleRequest | UpdateScheduleRequest | 일정 수정 요청 |
| ExportScheduleRequest | ExportScheduleRequest | 일정 내보내기 요청 |
| ScheduleExportResponse | ScheduleExportResponse | 일정 내보내기 응답 |

## 아키텍처 패턴별 특징

### User Service (Layered Architecture)
- **Controller Layer**: HTTP 요청/응답 처리
- **Service Layer**: 비즈니스 로직 처리
- **Repository Layer**: 데이터 접근
- **Entity Layer**: 도메인 모델

### Location Service (Layered Architecture)
- **Controller Layer**: HTTP 요청/응답 처리
- **Service Layer**: 비즈니스 로직 및 외부 API 연동
- **Repository Layer**: 데이터 접근 및 캐시 관리
- **Entity Layer**: 도메인 모델

### AI Service (Layered Architecture)
- **Controller Layer**: HTTP 요청/응답 처리
- **Service Layer**: AI 모델 연동 및 비동기 작업 관리
- **Repository Layer**: 데이터 접근 및 작업 상태 관리
- **Entity Layer**: 도메인 모델

### Trip Service (Clean Architecture)
- **Framework & Driver Layer**: 웹 컨트롤러
- **Interface Adapter Layer**: 외부 시스템 연동
- **Use Case Layer**: 애플리케이션 비즈니스 로직
- **Entity Layer**: 핵심 비즈니스 규칙

## 공통 컴포넌트 활용

모든 서비스는 다음 공통 컴포넌트를 활용합니다:
- **예외 처리**: BaseException 계층 구조
- **응답 형식**: ApiResponse, ErrorResponse, PageResponse
- **엔티티 기반**: BaseEntity, BaseAuditEntity
- **유틸리티**: DateUtils, StringUtils, ValidationUtils, SecurityUtils

## 검증 결과

### 인터페이스 일치성 ✅
- 모든 Controller 메소드가 API 설계서의 operationId와 일치
- Request/Response DTO가 API 스키마와 정확히 매핑

### 명명 규칙 통일성 ✅
- 패키지명: kebab-case
- 클래스명: PascalCase
- 메소드명: camelCase
- 상수명: UPPER_SNAKE_CASE

### 의존성 검증 ✅
- User, Location, AI 서비스: 독립적 설계
- Trip 서비스: User, Location 서비스 클라이언트 의존성 정의
- 순환 의존성 없음

### 크로스 서비스 참조 검증 ✅
- Trip → User: UserServiceClient
- Trip → Location: LocationServiceClient  
- Trip → AI: AiServiceClient
- 인터페이스 기반 느슨한 결합 설계