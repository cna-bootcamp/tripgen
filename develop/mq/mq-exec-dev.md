# TripGen Message Queue 설치 결과서 - 개발환경

## 1. 설치 개요

### 1.1 설치 정보
- **설치일시**: 2025-08-07 13:43 KST
- **설치대상환경**: 개발환경
- **Resource Group**: rg-digitalgarage-01
- **Namespace**: tripgen-dev
- **설치수행자**: 한데브옵스(클라우더) / DevOps Engineer

### 1.2 설치 환경
- **Cloud Provider**: Microsoft Azure
- **Region**: Korea Central
- **Service**: Azure Service Bus (Basic Tier)
- **설치 도구**: Azure CLI 2.x

## 2. 설치 결과

### 2.1 Service Bus Namespace

| 항목 | 값 | 상태 |
|------|-----|------|
| Namespace 이름 | sb-tripgen-dev | ✅ Active |
| SKU | Basic | ✅ |
| Location | koreacentral | ✅ |
| Resource Group | rg-digitalgarage-01 | ✅ |
| Service Endpoint | https://sb-tripgen-dev.servicebus.windows.net:443/ | ✅ |
| Public Network Access | Enabled | ✅ |
| TLS Version | 1.2 | ✅ |

### 2.2 생성된 메시지 큐

| 큐 이름 | 크기 | TTL | Lock Duration | Max Delivery | 상태 |
|---------|------|-----|---------------|--------------|------|
| ai-schedule-generation | 1GB | 1일 (P1D) | 30초 (PT30S) | 3회 | ✅ Active |
| ai-schedule-regeneration | 1GB | 1일 (P1D) | 30초 (PT30S) | 3회 | ✅ Active |
| location-search | 1GB | 1시간 (PT1H) | 10초 (PT10S) | 3회 | ✅ Active |
| route-calculation | 1GB | 1시간 (PT1H) | 15초 (PT15S) | 3회 | ✅ Active |
| ai-recommendation | 1GB | 2시간 (PT2H) | 20초 (PT20S) | 3회 | ✅ Active |
| notification | 1GB | 6시간 (PT6H) | 5초 (PT5S) | 3회 | ✅ Active |
| dead-letter | 1GB | 7일 (P7D) | 1분 (PT1M) | 10회 | ✅ Active |

**참고**: Basic Tier에서는 모든 큐의 크기가 1GB로 고정됩니다.

### 2.3 보안 설정

#### 2.3.1 인증 정보
- **인증 방식**: Shared Access Signature (SAS)
- **Access Policy**: RootManageSharedAccessKey
- **권한**: Manage, Send, Listen

#### 2.3.2 Kubernetes Secret
- **Secret 파일**: `develop/mq/servicebus-secret.yaml`
- **Secret 이름**: servicebus-connection
- **Namespace**: tripgen-dev
- **데이터 키**: connection-string (Base64 인코딩)

## 3. 연결 정보

### 3.1 연결 문자열 획득 명령어
```bash
# Azure CLI를 통한 연결 문자열 획득
az servicebus namespace authorization-rule keys list \
    --name RootManageSharedAccessKey \
    --namespace-name sb-tripgen-dev \
    --resource-group rg-digitalgarage-01 \
    --query primaryConnectionString -o tsv
```

### 3.2 연결 문자열 저장 위치
- **파일 경로**: `develop/mq/connection-string.txt`
- **형식**: `Endpoint=sb://{namespace}.servicebus.windows.net/;SharedAccessKeyName={keyName};SharedAccessKey={key}`

### 3.3 Kubernetes Secret 적용
```bash
# Kubernetes Secret 적용 (kubectl이 설정된 환경에서)
kubectl apply -f develop/mq/servicebus-secret.yaml

# Secret 확인
kubectl get secret servicebus-connection -n tripgen-dev
```

## 4. 애플리케이션 연결 가이드

### 4.1 Spring Boot 설정 예시
```yaml
spring:
  cloud:
    azure:
      servicebus:
        connection-string: ${SERVICE_BUS_CONNECTION_STRING}
        # 또는 Kubernetes Secret에서 읽기
        # connection-string: ${servicebus-connection}
```

### 4.2 환경 변수 설정
```bash
# Linux/Mac
export SERVICE_BUS_CONNECTION_STRING="Endpoint=sb://sb-tripgen-dev.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=..."

# Windows PowerShell
$env:SERVICE_BUS_CONNECTION_STRING="Endpoint=sb://sb-tripgen-dev.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=..."
```

### 4.3 Java 코드 예시
```java
// Service Bus 클라이언트 생성
String connectionString = System.getenv("SERVICE_BUS_CONNECTION_STRING");
ServiceBusClientBuilder builder = new ServiceBusClientBuilder()
    .connectionString(connectionString);

// 메시지 송신 예시
ServiceBusSenderClient sender = builder
    .sender()
    .queueName("ai-schedule-generation")
    .buildClient();

// 메시지 수신 예시
ServiceBusReceiverClient receiver = builder
    .receiver()
    .queueName("ai-schedule-generation")
    .buildClient();
```

## 5. 검증 및 테스트

### 5.1 설치 검증 명령어

#### Service Bus 상태 확인
```bash
az servicebus namespace show \
    --name sb-tripgen-dev \
    --resource-group rg-digitalgarage-01 \
    --query "{Name:name, Status:status, SKU:sku.name}" \
    -o table
```

#### 큐 목록 확인
```bash
az servicebus queue list \
    --namespace-name sb-tripgen-dev \
    --resource-group rg-digitalgarage-01 \
    --query "[].{Name:name, Status:status, MessageCount:messageCount}" \
    -o table
```

### 5.2 메시지 송수신 테스트
```bash
# Azure CLI를 통한 테스트 메시지 전송 (Service Bus Explorer 사용 권장)
# 또는 애플리케이션 레벨에서 테스트 코드 실행
```

## 6. 모니터링

### 6.1 Azure Portal 모니터링
- **URL**: https://portal.azure.com
- **경로**: Resource Group → sb-tripgen-dev → Metrics
- **주요 메트릭**:
  - Active Messages
  - Dead Letter Messages
  - Server Errors
  - Throttled Requests

### 6.2 Azure CLI 모니터링
```bash
# 큐별 메시지 수 확인
az servicebus queue list \
    --namespace-name sb-tripgen-dev \
    --resource-group rg-digitalgarage-01 \
    --query "[].{Queue:name, Active:countDetails.activeMessageCount, DeadLetter:countDetails.deadLetterMessageCount}" \
    -o table
```

## 7. 운영 가이드

### 7.1 일일 점검 사항
- [ ] Service Bus 상태 확인 (Active 상태)
- [ ] 각 큐의 메시지 수 확인
- [ ] Dead Letter Queue 모니터링
- [ ] 에러 로그 확인

### 7.2 문제 해결

#### 연결 실패 시
1. 연결 문자열 확인
2. 네트워크 연결 상태 확인
3. Service Bus 상태 확인
4. Access Policy 권한 확인

#### 메시지 처리 실패 시
1. Dead Letter Queue 확인
2. Lock Duration 설정 검토
3. Max Delivery Count 조정 고려
4. 메시지 크기 확인 (최대 256KB)

### 7.3 백업 및 복구
- **구성 백업**: 설치 스크립트 및 YAML 파일 Git 저장소 보관
- **연결 정보**: connection-string.txt 별도 보안 저장소 보관
- **복구 절차**: 설치 스크립트 재실행

## 8. 제한사항 (Basic Tier)

- **큐 크기**: 1GB 고정
- **토픽/구독**: 지원하지 않음
- **중복 감지**: 지원하지 않음
- **세션**: 지원하지 않음
- **파티셔닝**: 지원하지 않음
- **Geo-replication**: 지원하지 않음

## 9. 향후 계획

### 9.1 운영환경 전환 시
- Premium Tier로 업그레이드
- 토픽/구독 패턴 도입
- Private Endpoint 구성
- Managed Identity 적용
- 파티셔닝 활성화

### 9.2 개선 사항
- 모니터링 대시보드 구축
- 자동 알림 설정
- 성능 튜닝
- 보안 강화

## 10. 참고 자료

### 10.1 관련 문서
- MQ 설치계획서: `develop/mq/mq-plan-dev.md`
- 설치 스크립트: `develop/mq/create-queues-basic.sh`
- Kubernetes Secret: `develop/mq/servicebus-secret.yaml`

### 10.2 Azure 문서
- [Azure Service Bus 문서](https://docs.microsoft.com/azure/service-bus-messaging/)
- [Service Bus Basic Tier 제한사항](https://docs.microsoft.com/azure/service-bus-messaging/service-bus-premium-messaging)
- [Service Bus 가격 책정](https://azure.microsoft.com/pricing/details/service-bus/)

## 11. 승인 및 이력

### 11.1 설치 승인
- 설치자: 한데브옵스(클라우더) / DevOps Engineer
- 검토자: 김개발(테키) / Tech Lead
- 승인자: 이여행(트래블) / Product Owner
- 설치일: 2025-08-07

### 11.2 변경 이력
| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| 1.0 | 2025-08-07 | 최초 설치 및 문서 작성 | 한데브옵스 |