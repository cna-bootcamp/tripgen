# MQ 설치 결과서 - 개발환경

## 1. 설치 개요

- **설치 일자**: 2025-08-04
- **설치 환경**: 개발환경 (dev)
- **MQ 솔루션**: Azure Service Bus (Basic Tier)
- **설치 작업자**: DevOps Engineer (한데브옵스/클라우더)

## 2. 설치된 리소스

### 2.1 리소스 그룹
- **이름**: rg-tripgen-dev
- **위치**: Korea Central
- **태그**: 
  - EMAIL: admin@tripgen.com
  - ServiceName: TripGen
  - Name: TripGen-DEV
  - CreateDate: 2025-08-04

### 2.2 Service Bus Namespaces

| 서비스 | Namespace | 상태 | Endpoint | Principal ID |
|--------|-----------|------|----------|--------------|
| AI Service | sb-tripgen-ai-dev | Active | https://sb-tripgen-ai-dev.servicebus.windows.net:443/ | 2b5c10ac-9359-4c1e-9920-c7f8364d89f5 |
| Location Service | sb-tripgen-location-dev | Active | https://sb-tripgen-location-dev.servicebus.windows.net:443/ | fee949bb-d9ed-4ba1-b2ca-0980be233de7 |
| Trip Service | sb-tripgen-trip-dev | Active | https://sb-tripgen-trip-dev.servicebus.windows.net:443/ | 4522bac2-b479-4fe5-bbb4-a713410f0e0f |
| User Service | sb-tripgen-user-dev | Active | https://sb-tripgen-user-dev.servicebus.windows.net:443/ | 2854302a-ebf0-4a8c-a83f-31e2d37825be |

### 2.3 생성된 Queues

#### AI Service (sb-tripgen-ai-dev)
| Queue 이름 | Lock Duration | TTL | 재시도 횟수 | 용도 |
|------------|---------------|-----|------------|------|
| ai-processing | 5분 | 14일 | 3회 | AI 처리 요청 |
| ai-results | 30초 | 14일 | 기본값 | AI 처리 결과 |
| ai-training | 5분 | 14일 | 1회 | AI 모델 훈련 |
| ai-feedback | 30초 | 14일 | 기본값 | AI 피드백 수집 |

#### Location Service (sb-tripgen-location-dev)
| Queue 이름 | Lock Duration | TTL | 용도 |
|------------|---------------|-----|------|
| location-events | 30초 | 14일 | 위치 이벤트 처리 |
| geocoding | 1분 | 14일 | 지오코딩 처리 |
| poi-updates | 2분 | 14일 | POI 업데이트 |
| route-calculation | 3분 | 14일 | 경로 계산 |

#### Trip Service (sb-tripgen-trip-dev)
| Queue 이름 | Lock Duration | TTL | 용도 |
|------------|---------------|-----|------|
| trip-events | 30초 | 14일 | 여행 일정 이벤트 |
| trip-generation | 5분 | 14일 | AI 일정 생성 요청 |
| trip-notifications | 30초 | 14일 | 여행 일정 알림 |

#### User Service (sb-tripgen-user-dev)
| Queue 이름 | Lock Duration | TTL | 용도 |
|------------|---------------|-----|------|
| user-events | 30초 | 14일 | 사용자 이벤트 처리 |
| user-notifications | 30초 | 14일 | 사용자 알림 처리 |

## 3. 보안 설정

### 3.1 Managed Identity
- 각 Service Bus namespace에 System Assigned Managed Identity 활성화
- "Azure Service Bus Data Owner" 역할 부여 필요 (RBAC)

### 3.2 네트워크 설정
- Public Network Access: Enabled
- TLS Version: 1.2 (최소)

## 4. 연결 정보

### 4.1 연결 문자열 획득 명령어

```bash
# AI Service 연결 문자열
az servicebus namespace authorization-rule keys list \
  --resource-group rg-tripgen-dev \
  --namespace-name sb-tripgen-ai-dev \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString \
  --output tsv

# Location Service 연결 문자열
az servicebus namespace authorization-rule keys list \
  --resource-group rg-tripgen-dev \
  --namespace-name sb-tripgen-location-dev \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString \
  --output tsv

# Trip Service 연결 문자열
az servicebus namespace authorization-rule keys list \
  --resource-group rg-tripgen-dev \
  --namespace-name sb-tripgen-trip-dev \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString \
  --output tsv

# User Service 연결 문자열
az servicebus namespace authorization-rule keys list \
  --resource-group rg-tripgen-dev \
  --namespace-name sb-tripgen-user-dev \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString \
  --output tsv
```

### 4.2 Managed Identity 연결
```
Endpoint=sb://<namespace>.servicebus.windows.net/;Authentication=ManagedIdentity
```

## 5. Kubernetes 배포 파일

- **위치**: `build/mq/service-bus-manifest.yaml`
- ConfigMap, Secret, ServiceAccount 포함
- Workload Identity 설정 포함

## 6. 제약사항 (Basic Tier)

- Lock Duration 최대값: 5분
- TTL 최대값: 14일
- 중복 감지 기능 없음
- Topic/Subscription 미지원
- 최대 Queue 크기: 1GB

## 7. 모니터링

### 7.1 메트릭 확인
- Azure Portal > Service Bus namespace > Metrics
- 주요 메트릭: 메시지 수, 활성 메시지, Dead Letter 메시지

### 7.2 로그 확인
- Application Insights 연동 권장
- Azure Monitor 알림 규칙 설정 권장

## 8. 향후 작업

1. RBAC 권한 설정 완료 필요
   - `servicebus-rbac-commands.md` 파일의 명령어 실행
2. Application Insights 연동
3. 모니터링 알림 규칙 설정
4. 프로덕션 환경 구성 시 Standard/Premium tier 고려

## 9. 참고 문서

- [연결 가이드](connection-guide.md)
- [RBAC 설정 명령어](servicebus-rbac-commands.md)
- [Kubernetes Manifest](service-bus-manifest.yaml)

## 10. 설치 검증

```bash
# Service Bus namespace 목록 확인
az servicebus namespace list --resource-group rg-tripgen-dev --output table

# Queue 목록 확인 (예: AI Service)
az servicebus queue list --resource-group rg-tripgen-dev --namespace-name sb-tripgen-ai-dev --output table
```

---

**설치 완료**: 모든 서비스별 Azure Service Bus가 성공적으로 설치되었습니다.