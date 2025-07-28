# 여행 일정 생성 서비스 - 패키지 구조도

## 전체 프로젝트 구조

```
com.unicorn.tripgen
├── common (공통 컴포넌트)
├── user (사용자 서비스 - Layered Architecture)
├── trip (여행 서비스 - Clean Architecture)  
├── location (장소 서비스 - Layered Architecture)
└── ai (AI 서비스 - Layered Architecture)
```

## 1. 공통 컴포넌트 (Common)

```
com.unicorn.tripgen.common
├── dto
│   ├── BaseResponse.java
│   ├── ResponseStatus.java
│   ├── PageRequest.java
│   └── PageResponse.java
├── exception
│   ├── BaseException.java
│   ├── BusinessException.java
│   ├── ValidationException.java
│   ├── SystemException.java
│   ├── ResourceNotFoundException.java
│   └── DuplicateResourceException.java
├── util
│   ├── DateTimeUtil.java
│   ├── ValidationUtil.java
│   ├── StringUtil.java
│   └── SecurityUtil.java
├── config
│   ├── DatabaseConfig.java
│   ├── CacheConfig.java
│   └── SecurityConfig.java
└── constants
    ├── ApiConstants.java
    ├── CacheConstants.java
    └── MessageConstants.java
```

## 2. User 서비스 (Layered Architecture)

```
com.unicorn.tripgen.user
├── UserApplication.java
├── controller
│   └── UserController.java
├── dto
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── ProfileRequest.java
│   ├── ProfileResponse.java
│   ├── PasswordChangeRequest.java
│   └── UserCheckResponse.java
├── service
│   ├── UserService.java
│   ├── UserServiceImpl.java
│   ├── AuthService.java
│   ├── AuthServiceImpl.java
│   ├── ValidationService.java
│   ├── ValidationServiceImpl.java
│   ├── ImageService.java
│   ├── ImageServiceImpl.java
│   ├── JwtTokenService.java
│   └── JwtTokenServiceImpl.java
├── domain
│   ├── User.java
│   ├── UserSession.java
│   └── UserStatus.java
├── repository
│   ├── entity
│   │   ├── UserEntity.java
│   │   └── UserSessionEntity.java
│   └── jpa
│       ├── UserRepository.java
│       ├── UserRepositoryImpl.java
│       ├── UserSessionRepository.java
│       └── UserSessionRepositoryImpl.java
└── config
    ├── UserSecurityConfig.java
    ├── UserDataConfig.java
    └── jwt
        ├── JwtAuthenticationFilter.java
        ├── JwtTokenProvider.java
        └── CustomUserDetailsService.java
```

## 3. Trip 서비스 (Clean Architecture)

```
com.unicorn.tripgen.trip
├── biz
│   ├── domain
│   │   ├── Trip.java
│   │   ├── Member.java
│   │   ├── Destination.java
│   │   ├── Schedule.java
│   │   ├── Place.java
│   │   ├── TripId.java
│   │   ├── MemberId.java
│   │   ├── TransportMode.java
│   │   └── HealthStatus.java
│   ├── usecase
│   │   ├── in
│   │   │   ├── CreateTripUseCase.java
│   │   │   ├── ManageMemberUseCase.java
│   │   │   ├── ManageDestinationUseCase.java
│   │   │   └── ManageScheduleUseCase.java
│   │   └── out
│   │       ├── TripRepository.java
│   │       ├── ScheduleRepository.java
│   │       ├── AIServicePort.java
│   │       ├── LocationServicePort.java
│   │       └── NotificationPort.java
│   ├── service
│   │   ├── TripApplicationService.java
│   │   ├── MemberApplicationService.java
│   │   ├── DestinationApplicationService.java
│   │   ├── ScheduleApplicationService.java
│   │   └── TripDomainService.java
│   └── dto
│       ├── TripCreateRequest.java
│       ├── TripResponse.java
│       ├── MemberRequest.java
│       ├── DestinationRequest.java
│       └── ScheduleResponse.java
└── infra
    ├── TripApplication.java
    ├── controller
    │   ├── TripController.java
    │   ├── MemberController.java
    │   ├── DestinationController.java
    │   └── ScheduleController.java
    ├── gateway
    │   ├── entity
    │   │   ├── TripEntity.java
    │   │   ├── MemberEntity.java
    │   │   ├── DestinationEntity.java
    │   │   ├── ScheduleEntity.java
    │   │   └── PlaceEntity.java
    │   ├── repository
    │   │   ├── TripJpaRepository.java
    │   │   └── ScheduleJpaRepository.java
    │   ├── external
    │   │   ├── AIServiceAdapter.java
    │   │   ├── LocationServiceAdapter.java
    │   │   ├── MessageQueueAdapter.java
    │   │   ├── RedisCacheAdapter.java
    │   │   └── WebSocketNotificationAdapter.java
    │   ├── TripRepositoryImpl.java
    │   └── ScheduleRepositoryImpl.java
    └── config
        ├── TripConfig.java
        ├── MessagingConfig.java
        └── CacheConfig.java
```

## 4. Location 서비스 (Layered Architecture)

```
com.unicorn.tripgen.location
├── LocationApplication.java
├── controller
│   ├── LocationController.java
│   └── PlaceRecommendationController.java
├── dto
│   ├── SearchRequest.java
│   ├── SearchResponse.java
│   ├── PlaceDetailRequest.java
│   ├── PlaceDetailResponse.java
│   ├── BusinessHoursResponse.java
│   ├── RecommendationRequest.java
│   └── RecommendationResponse.java
├── service
│   ├── SearchService.java
│   ├── SearchServiceImpl.java
│   ├── PlaceService.java
│   ├── PlaceServiceImpl.java
│   ├── BusinessHoursService.java
│   ├── BusinessHoursServiceImpl.java
│   ├── PlaceRecommendationService.java
│   ├── PlaceRecommendationServiceImpl.java
│   ├── RegionDetector.java
│   └── RegionDetectorImpl.java
├── domain
│   ├── Place.java
│   ├── LocationInfo.java
│   ├── BusinessHours.java
│   ├── AIRecommendation.java
│   ├── RegionType.java
│   ├── PlaceType.java
│   └── SearchRadius.java
├── repository
│   ├── entity
│   │   ├── PlaceEntity.java
│   │   ├── LocationInfoEntity.java
│   │   ├── BusinessHoursEntity.java
│   │   └── AIRecommendationEntity.java
│   └── jpa
│       ├── LocationRepository.java
│       └── LocationRepositoryImpl.java
├── external
│   ├── client
│   │   ├── KakaoMapClient.java
│   │   ├── KakaoMapClientImpl.java
│   │   ├── GoogleMapsClient.java
│   │   ├── GoogleMapsClientImpl.java
│   │   ├── OpenWeatherClient.java
│   │   └── OpenWeatherClientImpl.java
│   ├── dto
│   │   ├── KakaoPlaceResponse.java
│   │   ├── GooglePlaceResponse.java
│   │   └── WeatherResponse.java
│   └── adapter
│       ├── LocationApiAdapter.java
│       ├── LocationApiAdapterImpl.java
│       ├── WebSocketAdapter.java
│       ├── WebSocketAdapterImpl.java
│       ├── MessageQueueAdapter.java
│       └── MessageQueueAdapterImpl.java
└── config
    ├── LocationConfig.java
    ├── ExternalApiConfig.java
    ├── WebSocketConfig.java
    └── cache
        ├── CacheConfig.java
        └── RedisCacheService.java
```

## 5. AI 서비스 (Layered Architecture)

```
com.unicorn.tripgen.ai
├── AIApplication.java
├── controller
│   ├── AIScheduleController.java
│   └── AIRecommendationController.java
├── dto
│   ├── ScheduleGenerationRequest.java
│   ├── ScheduleGenerationResponse.java
│   ├── GenerationStatusResponse.java
│   ├── RecommendationRequest.java
│   ├── RecommendationResponse.java
│   └── ProgressUpdateRequest.java
├── service
│   ├── ScheduleGenerationService.java
│   ├── ScheduleGenerationServiceImpl.java
│   ├── AIRecommendationService.java
│   ├── AIRecommendationServiceImpl.java
│   ├── ClaudeAIService.java
│   ├── ClaudeAIServiceImpl.java
│   ├── WeatherService.java
│   ├── WeatherServiceImpl.java
│   ├── ContextEnrichmentService.java
│   ├── ContextEnrichmentServiceImpl.java
│   ├── MessageQueueService.java
│   ├── MessageQueueServiceImpl.java
│   ├── RedisCacheService.java
│   └── RedisCacheServiceImpl.java
├── domain
│   ├── GenerationRequest.java
│   ├── GeneratedSchedule.java
│   ├── DaySchedule.java
│   ├── PlaceRecommendation.java
│   ├── GenerationStatus.java
│   ├── RecommendationType.java
│   └── ContextType.java
├── repository
│   ├── entity
│   │   ├── GenerationRequestEntity.java
│   │   ├── GeneratedScheduleEntity.java
│   │   ├── DayScheduleEntity.java
│   │   └── PlaceRecommendationEntity.java
│   └── jpa
│       ├── JpaGenerationRequestRepository.java
│       ├── JpaGeneratedScheduleRepository.java
│       └── RedisRecommendationRepository.java
├── external
│   ├── client
│   │   ├── ClaudeApiClient.java
│   │   ├── ClaudeApiClientImpl.java
│   │   ├── WeatherApiClient.java
│   │   └── WeatherApiClientImpl.java
│   ├── prompt
│   │   ├── PromptBuilder.java
│   │   ├── SchedulePromptBuilder.java
│   │   ├── RecommendationPromptBuilder.java
│   │   └── ResponseParser.java
│   └── messaging
│       ├── ScheduleGenerationProducer.java
│       ├── ScheduleGenerationConsumer.java
│       ├── RecommendationProducer.java
│       └── RecommendationConsumer.java
└── config
    ├── AIConfig.java
    ├── ClaudeConfig.java
    ├── MessageQueueConfig.java
    ├── AsyncConfig.java
    └── cache
        ├── CacheConfig.java
        └── RedisCacheManager.java
```

## 패키지별 특징

### User 서비스 (Layered)
- **전통적인 3계층 구조**: Controller → Service → Repository
- **인증/보안 중심**: JWT, Spring Security 통합
- **캐시 활용**: 사용자 세션, 프로필 정보

### Trip 서비스 (Clean)  
- **도메인 중심 설계**: 비즈니스 로직과 기술 구현 분리
- **의존성 역전**: 인터페이스 기반 외부 연동
- **UseCase 패턴**: 명확한 비즈니스 용도별 구분

### Location 서비스 (Layered)
- **외부 API 통합**: 다중 지도 서비스 연동
- **지역별 전략 패턴**: 국내/해외 API 자동 선택
- **캐싱 전략**: 검색 결과, 장소 정보 캐시

### AI 서비스 (Layered)
- **비동기 처리**: Message Queue 기반 백그라운드 처리
- **AI API 통합**: Claude AI 연동 및 프롬프트 관리
- **컨텍스트 기반**: 상황별 맞춤형 추천 시스템

각 서비스는 독립적으로 배포 가능하며, 공통 컴포넌트를 통해 일관된 표준을 유지합니다.