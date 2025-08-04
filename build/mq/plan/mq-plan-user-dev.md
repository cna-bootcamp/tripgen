# User Service Message Queue 설치 계획서 (개발환경)

## 개요
- **서비스명**: User Service
- **환경**: Development
- **Message Queue**: Azure Service Bus Basic Tier
- **목적**: 사용자 관련 비동기 메시징 처리

## Azure Service Bus 설정

### 1. Service Bus Namespace 생성
```bash
# Resource Group 생성
az group create --name rg-tripgen-dev --location koreacentral

# Service Bus Namespace 생성 (Basic Tier)
az servicebus namespace create \
    --resource-group rg-tripgen-dev \
    --name sb-tripgen-user-dev \
    --location koreacentral \
    --sku Basic
```

### 2. Queue 설정

#### 2.1 User Events Queue
```bash
# 사용자 이벤트 처리용 큐 생성
az servicebus queue create \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-user-dev \
    --name user-events \
    --max-size 5120 \
    --lock-duration PT30S \
    --default-message-time-to-live P14D \
    --enable-partitioning false \
    --enable-duplicate-detection false
```

#### 2.2 User Notifications Queue
```bash
# 사용자 알림 처리용 큐 생성
az servicebus queue create \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-user-dev \
    --name user-notifications \
    --max-size 5120 \
    --lock-duration PT30S \
    --default-message-time-to-live P14D \
    --enable-partitioning false \
    --enable-duplicate-detection false
```

### 3. 접근 정책 설정

#### 3.1 Managed Identity 생성
```bash
# User Service용 Managed Identity 생성
az identity create \
    --resource-group rg-tripgen-dev \
    --name mi-user-service-dev
```

#### 3.2 Service Bus 권한 할당
```bash
# Service Bus Data Owner 권한 할당
az role assignment create \
    --assignee $(az identity show --resource-group rg-tripgen-dev --name mi-user-service-dev --query principalId -o tsv) \
    --role "Azure Service Bus Data Owner" \
    --scope /subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-tripgen-dev/providers/Microsoft.ServiceBus/namespaces/sb-tripgen-user-dev
```

## 연결 설정

### 4. Connection String 구성
```json
{
  "ServiceBus": {
    "ConnectionString": "Endpoint=sb://sb-tripgen-user-dev.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=<ACCESS_KEY>",
    "QueueNames": {
      "UserEvents": "user-events",
      "UserNotifications": "user-notifications"
    }
  }
}
```

### 5. Managed Identity 연결 (권장)
```json
{
  "ServiceBus": {
    "FullyQualifiedNamespace": "sb-tripgen-user-dev.servicebus.windows.net",
    "ManagedIdentity": {
      "ClientId": "<MANAGED_IDENTITY_CLIENT_ID>"
    },
    "QueueNames": {
      "UserEvents": "user-events", 
      "UserNotifications": "user-notifications"
    }
  }
}
```

## Message 핸들링 설정

### 6. Queue 구성 세부사항
- **최대 큐 크기**: 5GB
- **메시지 Lock Duration**: 30초
- **메시지 TTL**: 14일
- **Dead Letter Queue**: 자동 생성됨
- **중복 감지**: 비활성화
- **파티셔닝**: 비활성화 (Basic Tier)

### 7. 메시지 유형별 설정

#### User Events 메시지
```json
{
  "MessageType": "UserEvent",
  "Properties": {
    "EventType": "UserRegistered|UserUpdated|UserDeleted",
    "UserId": "string",
    "Timestamp": "datetime",
    "Source": "user-service"
  }
}
```

#### User Notifications 메시지  
```json
{
  "MessageType": "UserNotification",
  "Properties": {
    "NotificationType": "Email|SMS|Push",
    "UserId": "string", 
    "Message": "string",
    "Priority": "High|Medium|Low"
  }
}
```

## 초기화 명령어

### 8. 검증 스크립트
```bash
#!/bin/bash
# Message Queue 연결 테스트
echo "Testing Service Bus connection..."

# Queue 상태 확인
az servicebus queue show \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-user-dev \
    --name user-events \
    --query '{Name:name, Status:status, MessageCount:messageCount}'

az servicebus queue show \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-user-dev \
    --name user-notifications \
    --query '{Name:name, Status:status, MessageCount:messageCount}'

echo "User Service Message Queue setup completed!"
```

## 모니터링 설정

### 9. Application Insights 연동
```bash
# Application Insights 생성
az monitor app-insights component create \
    --app ai-user-service-dev \
    --location koreacentral \
    --resource-group rg-tripgen-dev \
    --kind web
```

### 10. 알림 규칙 설정
```bash
# Dead Letter Queue 메시지 알림
az monitor metrics alert create \
    --name "User Service Dead Letter Queue Alert" \
    --resource-group rg-tripgen-dev \
    --scopes /subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-tripgen-dev/providers/Microsoft.ServiceBus/namespaces/sb-tripgen-user-dev \
    --condition "avg DeadletteredMessages > 0" \
    --description "Alert when dead letter queue has messages"
```

## 보안 고려사항

### 11. 네트워크 보안
- Private Endpoint 사용 (프로덕션 환경 권장)
- IP 필터링 규칙 적용
- VNet 통합 고려

### 12. 접근 제어
- Managed Identity 사용 권장
- 최소 권한 원칙 적용
- 정기적인 액세스 키 로테이션

## 비용 최적화

### 13. Basic Tier 특징
- 최대 큐 크기: 5GB
- 동시 연결 수: 100개
- 월 100만 개 작업 포함
- 추가 작업당 $0.05

### 14. 개발환경 권장사항  
- 불필요한 큐는 삭제
- 테스트 후 메시지 정리
- 모니터링을 통한 사용량 추적

## 트러블슈팅

### 15. 일반적인 문제점
1. **연결 실패**: Managed Identity 권한 확인
2. **메시지 처리 지연**: Lock Duration 조정
3. **Dead Letter Queue 증가**: 메시지 처리 로직 점검

### 16. 로그 확인
```bash
# Service Bus 로그 확인  
az monitor activity-log list \
    --resource-group rg-tripgen-dev \
    --caller Microsoft.ServiceBus
```