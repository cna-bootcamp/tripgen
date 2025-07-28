# 클래스 설계 통합 문서

## 프로젝트 개요
- **패키지 그룹**: com.unicorn.tripgen
- **아키텍처 패턴**:
  - User Service: Layered Architecture  
  - Trip Service: Clean Architecture
  - Location Service: Layered Architecture
  - AI Service: Layered Architecture

## 전체 패키지 구조

```
com.unicorn.tripgen
├── common/
│   ├── dto/
│   │   ├── ApiResponse.java
│   │   ├── PageRequest.java
│   │   ├── PageResponse.java
│   │   └── SuccessResponse.java
│   ├── entity/
│   │   └── BaseTimeEntity.java
│   ├── exception/
│   │   ├── BaseException.java
│   │   ├── BusinessException.java
│   │   ├── ValidationException.java
│   │   ├── InfraException.java
│   │   └── ErrorCode.java
│   ├── service/
│   │   ├── CacheService.java
│   │   └── RedisCacheService.java
│   ├── security/
│   │   ├── SecurityUtils.java
│   │   └── AuthenticationService.java
│   ├── util/
│   │   ├── PasswordEncoder.java
│   │   ├── JwtTokenProvider.java
│   │   ├── ValidationUtils.java
│   │   └── DateTimeUtils.java
│   └── event/
│       ├── BaseEvent.java
│       ├── EventPublisher.java
│       └── EventHandler.java
├── user/
│   ├── UserApplication.java
│   ├── controller/
│   │   └── UserController.java
│   ├── service/
│   │   ├── UserService.java
│   │   ├── UserServiceImpl.java
│   │   ├── ValidationService.java
│   │   ├── ValidationServiceImpl.java
│   │   ├── AuthService.java
│   │   ├── AuthServiceImpl.java
│   │   ├── ImageService.java
│   │   ├── ImageServiceImpl.java
│   │   ├── EmailService.java
│   │   └── EmailServiceImpl.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   └── UserRepositoryImpl.java
│   ├── entity/
│   │   ├── UserEntity.java
│   │   ├── Gender.java
│   │   └── UserStatus.java
│   ├── dto/
│   │   ├── RegisterRequest.java
│   │   ├── RegisterResponse.java
│   │   ├── LoginRequest.java
│   │   ├── LoginResponse.java
│   │   ├── UserProfile.java
│   │   ├── UpdateProfileRequest.java
│   │   ├── ChangePasswordRequest.java
│   │   ├── AvatarUploadResponse.java
│   │   ├── UsernameCheckResponse.java
│   │   └── EmailCheckResponse.java
│   └── config/
│       ├── UserConfig.java
│       ├── SecurityConfig.java
│       └── ValidationConfig.java
├── trip/
│   ├── TripApplication.java
│   ├── usecase/
│   │   ├── in/
│   │   │   ├── TripUseCase.java
│   │   │   ├── MemberUseCase.java
│   │   │   ├── DestinationUseCase.java
│   │   │   └── ScheduleUseCase.java
│   │   └── out/
│   │       ├── TripPort.java
│   │       ├── MemberPort.java
│   │       ├── DestinationPort.java
│   │       ├── SchedulePort.java
│   │       ├── AIServicePort.java
│   │       ├── LocationServicePort.java
│   │       ├── ExportServicePort.java
│   │       ├── CachePort.java
│   │       └── EventPort.java
│   ├── service/
│   │   ├── TripService.java
│   │   ├── MemberService.java
│   │   ├── DestinationService.java
│   │   └── ScheduleService.java
│   ├── domain/
│   │   ├── Trip.java
│   │   ├── Member.java
│   │   ├── Destination.java
│   │   ├── Schedule.java
│   │   ├── Place.java
│   │   ├── ScheduleGenerationRequest.java
│   │   ├── Weather.java
│   │   ├── Transportation.java
│   │   ├── HealthConsideration.java
│   │   └── [enums]
│   ├── controller/
│   │   ├── TripController.java
│   │   ├── MemberController.java
│   │   ├── DestinationController.java
│   │   └── ScheduleController.java
│   ├── gateway/
│   │   ├── TripJpaAdapter.java
│   │   ├── MemberJpaAdapter.java
│   │   ├── DestinationJpaAdapter.java
│   │   ├── ScheduleJpaAdapter.java
│   │   ├── AIServiceAdapter.java
│   │   ├── LocationServiceAdapter.java
│   │   ├── ExportServiceAdapter.java
│   │   ├── RedisCacheAdapter.java
│   │   └── EventPublisherAdapter.java
│   └── dto/
│       ├── [Command Objects]
│       ├── [Response Objects]
│       └── [External DTOs]
├── location/
│   ├── LocationApplication.java
│   ├── controller/
│   │   └── LocationController.java
│   ├── service/
│   │   ├── LocationService.java
│   │   ├── LocationServiceImpl.java
│   │   ├── ExternalApiService.java
│   │   ├── GooglePlacesService.java
│   │   ├── WeatherService.java
│   │   ├── OpenWeatherService.java
│   │   ├── AIServiceClient.java
│   │   └── AIServiceClientImpl.java
│   ├── repository/
│   │   ├── PlaceRepository.java
│   │   ├── ReviewRepository.java
│   │   └── AIRecommendationRepository.java
│   ├── entity/
│   │   ├── Place.java
│   │   ├── Review.java
│   │   ├── AIRecommendation.java
│   │   └── [enums]
│   ├── dto/
│   │   ├── [Request/Response DTOs]
│   │   ├── [Data Transfer Objects]
│   │   └── [External API DTOs]
│   └── config/
│       ├── LocationConfig.java
│       ├── ExternalApiConfig.java
│       └── CacheConfig.java
└── ai/
    ├── AIApplication.java
    ├── controller/
    │   ├── AIScheduleController.java
    │   ├── AIRecommendationController.java
    │   └── AIExceptionHandler.java
    ├── service/
    │   ├── AIScheduleService.java
    │   ├── AIRecommendationService.java
    │   ├── GenerationStatusService.java
    │   └── AIModelService.java
    ├── repository/
    │   ├── ScheduleRepository.java
    │   ├── GenerationStatusRepository.java
    │   └── RecommendationRepository.java
    ├── entity/
    │   ├── SchedulePlan.java
    │   ├── GenerationStatusEntity.java
    │   ├── PlaceRecommendationEntity.java
    │   └── GenerationStatus.java
    ├── dto/
    │   ├── [Request/Response DTOs]
    │   └── [Supporting DTOs]
    └── config/
        ├── AIServiceConfig.java
        ├── AIModelConfig.java
        ├── CacheConfig.java
        └── AsyncConfig.java
```

## API 엔드포인트 매핑표

### User Service
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

### Trip Service  
| HTTP Method | Path | Controller Method | API Title |
|-------------|------|-------------------|-----------|
| POST | /trips | createTrip() | 여행 생성 |
| GET | /trips | getTrips() | 여행 목록 조회 |
| GET | /trips/{tripId} | getTripDetail() | 여행 상세 조회 |
| PUT | /trips/{tripId} | updateTrip() | 여행 정보 수정 |
| DELETE | /trips/{tripId} | deleteTrip() | 여행 삭제 |
| POST | /trips/{tripId}/members | addMember() | 멤버 추가 |
| DELETE | /trips/{tripId}/members/{memberId} | removeMember() | 멤버 제거 |
| POST | /trips/{tripId}/destinations | addDestination() | 여행지 추가 |
| PUT | /trips/{tripId}/destinations/{destinationId} | updateDestination() | 여행지 수정 |
| DELETE | /trips/{tripId}/destinations/{destinationId} | removeDestination() | 여행지 제거 |
| POST | /trips/{tripId}/schedules/generate | generateSchedule() | AI 일정 생성 요청 |
| GET | /trips/{tripId}/schedules | getSchedules() | 일정 조회 |
| POST | /trips/{tripId}/schedules/export | exportSchedule() | 일정 내보내기 |

### Location Service
| HTTP Method | Path | Controller Method | API Title |
|-------------|------|-------------------|-----------|
| GET | /places/nearby | searchNearbyPlaces() | 주변 장소 검색 |
| GET | /places/search | searchPlacesByKeyword() | 키워드 장소 검색 |
| GET | /places/{placeId} | getPlaceDetails() | 장소 상세 정보 조회 |
| GET | /places/{placeId}/recommendations | getAIRecommendations() | AI 추천 정보 조회 |
| GET | /places/{placeId}/hours | getBusinessHours() | 영업시간 조회 |

### AI Service
| HTTP Method | Path | Controller Method | API Title |
|-------------|------|-------------------|-----------|
| POST | /schedules/generate | generateSchedule() | AI 일정 생성 |
| GET | /schedules/status/{requestId} | getGenerationStatus() | 생성 상태 조회 |
| GET | /schedules/{scheduleId} | getSchedule() | 생성된 일정 조회 |
| POST | /recommendations/places | getPlaceRecommendations() | 장소 추천 정보 생성 |

## API 스키마 매핑표

### User Service
| API Schema | DTO Class | 용도 |
|------------|-----------|------|
| RegisterRequest | RegisterRequest | 회원가입 요청 |
| RegisterResponse | RegisterResponse | 회원가입 응답 |
| LoginRequest | LoginRequest | 로그인 요청 |
| LoginResponse | LoginResponse | 로그인 응답 |
| UserProfile | UserProfile | 사용자 프로필 |
| UpdateProfileRequest | UpdateProfileRequest | 프로필 수정 요청 |
| ChangePasswordRequest | ChangePasswordRequest | 비밀번호 변경 요청 |
| AvatarUploadResponse | AvatarUploadResponse | 아바타 업로드 응답 |
| UsernameCheckResponse | UsernameCheckResponse | 아이디 중복 확인 응답 |
| EmailCheckResponse | EmailCheckResponse | 이메일 중복 확인 응답 |

### Trip Service
| API Schema | DTO Class | 용도 |
|------------|-----------|------|
| CreateTripRequest | CreateTripCommand | 여행 생성 요청 |
| UpdateTripRequest | UpdateTripCommand | 여행 수정 요청 |
| TripSummary | TripSummary | 여행 요약 정보 |
| TripDetail | TripDetail | 여행 상세 정보 |
| AddMemberRequest | AddMemberCommand | 멤버 추가 요청 |
| AddDestinationRequest | AddDestinationCommand | 여행지 추가 요청 |
| GenerateScheduleRequest | GenerateScheduleCommand | 일정 생성 요청 |
| ScheduleExportRequest | ExportCommand | 일정 내보내기 요청 |

### Location Service
| API Schema | DTO Class | 용도 |
|------------|-----------|------|
| NearbySearchRequest | NearbySearchRequest | 주변 검색 요청 |
| NearbySearchResponse | NearbySearchResponse | 주변 검색 응답 |
| KeywordSearchResponse | KeywordSearchResponse | 키워드 검색 응답 |
| PlaceDetails | PlaceDetails | 장소 상세 정보 |
| AIRecommendationResponse | AIRecommendationResponse | AI 추천 응답 |
| BusinessHoursResponse | BusinessHoursResponse | 영업시간 응답 |

### AI Service
| API Schema | DTO Class | 용도 |
|------------|-----------|------|
| GenerateScheduleRequest | GenerateScheduleRequest | 일정 생성 요청 |
| GenerationStatusResponse | GenerationStatusResponse | 생성 상태 응답 |
| ScheduleResponse | ScheduleResponse | 일정 응답 |
| PlaceRecommendationRequest | PlaceRecommendationRequest | 장소 추천 요청 |
| PlaceRecommendationResponse | PlaceRecommendationResponse | 장소 추천 응답 |

## 아키텍처별 특성

### Layered Architecture (User, Location, AI)
- **Controller → Service → Repository → Entity** 순차적 의존성
- 각 계층은 바로 아래 계층에만 의존
- 공통 컴포넌트(Common)를 모든 계층에서 활용
- 전통적인 3-tier 아키텍처 패턴

### Clean Architecture (Trip)
- **Usecase 중심**: 비즈니스 로직이 Use Case 계층에 집중
- **의존성 역전**: 외부 계층이 내부 계층에 의존
- **Port/Adapter 패턴**: 인프라 계층과 비즈니스 로직 분리
- **도메인 모델**: 순수한 비즈니스 로직을 Domain 계층에 캡슐화

## 통합 검증 결과

### 인터페이스 일치성 ✅
- API 설계서와 Controller 메소드 일치
- DTO 스키마와 API 명세 일치
- 서비스 간 연동 인터페이스 정의

### 명명 규칙 통일성 ✅
- 패키지명: kebab-case (user-service, trip-service)
- 클래스명: PascalCase (UserService, TripController)
- 메소드명: camelCase (getUserProfile, createTrip)

### 의존성 검증 ✅
- 순환 의존성 없음
- 계층 간 의존성 방향 준수
- 공통 컴포넌트 재사용성 확보

### 크로스 서비스 참조 검증 ✅
- Trip Service → Location Service: 장소 정보 조회
- Trip Service → AI Service: 일정 생성 요청
- AI Service → Location Service: 장소 추천 정보
- User Service: 독립적 운영 (인증/인가)

## 결론

4개 서비스의 클래스 설계가 완료되었으며, 각각의 아키텍처 패턴에 따라 일관성 있게 설계되었습니다. 마이크로서비스 간 연동은 캐시 우선 전략을 통해 성능을 최적화하고, 공통 컴포넌트를 통해 코드 재사용성을 높였습니다.