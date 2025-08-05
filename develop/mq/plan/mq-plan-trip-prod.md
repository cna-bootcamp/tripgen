# 여행 서비스 Message Queue 설치 계획서 (프로덕션)

## 1. 개요
- **서비스명**: Trip Service
- **환경**: Production
- **MQ 솔루션**: Azure Service Bus Premium Tier
- **지역**: Korea Central (Primary), Korea South (Secondary)

## 2. 인프라 구성

### 2.1 Service Bus Namespace 구성
```yaml
# Azure Resource Manager Template
name: trip-servicebus-prod
sku: Premium
tier: Premium
capacity: 2  # Premium Units (PU) - 높은 처리량 요구사항
location: Korea Central
```

### 2.2 고가용성 구성
- **Geo-DR (Disaster Recovery)**: 활성화
- **Primary Region**: Korea Central
- **Secondary Region**: Korea South
- **Auto-failover**: 비활성화 (수동 장애조치)

### 2.3 네트워크 구성
```yaml
# Private Endpoint 구성
private_endpoint:
  name: trip-servicebus-pe-prod
  subnet: servicebus-subnet-prod
  vnet: tripgen-vnet-prod
  private_dns_zone: privatelink.servicebus.windows.net

# Network Access Rules
network_access:
  default_action: Deny
  trusted_services_allowed: true
  ip_rules:
    - action: Allow
      ip_mask: "10.0.0.0/16"  # Private network range
```

## 3. Queue 구성

### 3.1 Trip Creation Process Queue
```yaml
queue_name: trip-creation-process
properties:
  max_size_in_megabytes: 10240  # 높은 용량
  default_message_time_to_live: "PT48H"  # 48 hours
  lock_duration: "PT10M"  # 10 minutes (복잡한 처리)
  max_delivery_count: 5
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT30M"
  enable_sessions: true  # 순차 처리를 위한 세션 활성화
```

### 3.2 Trip Update Queue
```yaml
queue_name: trip-update
properties:
  max_size_in_megabytes: 5120
  default_message_time_to_live: "PT24H"  # 24 hours
  lock_duration: "PT5M"  # 5 minutes
  max_delivery_count: 10
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT10M"
  enable_sessions: false
```

### 3.3 Trip Booking Queue
```yaml
queue_name: trip-booking
properties:
  max_size_in_megabytes: 5120
  default_message_time_to_live: "PT6H"  # 6 hours (예약 시간 제한)
  lock_duration: "PT5M"  # 5 minutes
  max_delivery_count: 3
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT10M"
  enable_sessions: true  # 예약 순서 보장
```

### 3.4 Trip Notification Queue
```yaml
queue_name: trip-notification
properties:
  max_size_in_megabytes: 2048
  default_message_time_to_live: "PT12H"  # 12 hours
  lock_duration: "PT2M"  # 2 minutes
  max_delivery_count: 5
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: false
  enable_sessions: false
```

## 4. Topic 구성

### 4.1 Trip Events Topic
```yaml
topic_name: trip-events
properties:
  max_size_in_megabytes: 10240
  default_message_time_to_live: "PT48H"
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT30M"
  
subscriptions:
  - name: user-service-subscription
    properties:
      lock_duration: "PT3M"
      max_delivery_count: 10
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'TripCreated' OR EventType = 'TripBooked' OR EventType = 'TripCancelled'"
  
  - name: ai-service-subscription
    properties:
      lock_duration: "PT5M"
      max_delivery_count: 5
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'TripCreated' OR EventType = 'TripUpdated'"
        
  - name: location-service-subscription
    properties:
      lock_duration: "PT3M"
      max_delivery_count: 5
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'TripLocationAdded' OR EventType = 'TripRouteUpdated'"
        
  - name: payment-service-subscription
    properties:
      lock_duration: "PT5M"
      max_delivery_count: 3
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'TripBooked' OR EventType = 'TripCancelled'"
```

### 4.2 Trip Analytics Topic
```yaml
topic_name: trip-analytics
properties:
  max_size_in_megabytes: 5120
  default_message_time_to_live: "PT24H"
  enable_partitioning: true
  enable_duplicate_detection: false
  
subscriptions:
  - name: analytics-service-subscription
    properties:
      lock_duration: "PT2M"
      max_delivery_count: 3
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "1=1"  # 모든 메시지 수신
        
  - name: reporting-service-subscription
    properties:
      lock_duration: "PT5M"
      max_delivery_count: 5
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType LIKE '%Completed%' OR EventType LIKE '%Cancelled%'"
```

## 5. 보안 구성

### 5.1 Managed Identity 구성
```yaml
# Trip Service App의 Managed Identity
managed_identity:
  type: SystemAssigned
  
# Role Assignments
role_assignments:
  - principal_id: "{trip-service-managed-identity-id}"
    role_definition: "Azure Service Bus Data Owner"
    scope: "/subscriptions/{subscription-id}/resourceGroups/{rg-name}/providers/Microsoft.ServiceBus/namespaces/trip-servicebus-prod"
```

### 5.2 Access Policies (Backup용)
```yaml
access_policies:
  - name: trip-service-full-access
    rights:
      - Send
      - Listen
      - Manage
    primary_key: "{auto-generated}"
    secondary_key: "{auto-generated}"
```

## 6. 연결 구성

### 6.1 Connection String (Managed Identity 우선)
```yaml
# Primary Connection (Managed Identity)
connection_endpoint: "trip-servicebus-prod.servicebus.windows.net"
authentication: "managed_identity"

# Fallback Connection String (암호화하여 Key Vault에 저장)
fallback_connection_string: "Endpoint=sb://trip-servicebus-prod.servicebus.windows.net/;SharedAccessKeyName=trip-service-full-access;SharedAccessKey={key}"
```

### 6.2 Application 구성
```yaml
# appsettings.Production.json
ServiceBus:
  Namespace: "trip-servicebus-prod.servicebus.windows.net"
  UseManagedIdentity: true
  ConnectionString: "@Microsoft.KeyVault(SecretUri=https://tripgen-kv-prod.vault.azure.net/secrets/trip-servicebus-connection/)"
  
Queues:
  TripCreationProcess: "trip-creation-process"
  TripUpdate: "trip-update"
  TripBooking: "trip-booking"
  TripNotification: "trip-notification"
  
Topics:
  TripEvents: "trip-events"
  TripAnalytics: "trip-analytics"
```

## 7. 모니터링 및 알림

### 7.1 메트릭 구성
```yaml
monitoring:
  log_analytics_workspace: "tripgen-logs-prod"
  
metrics:
  - name: "Active Message Count"
    threshold: 2000  # 높은 처리량을 고려
    operator: "GreaterThan"
    time_aggregation: "Average"
    
  - name: "Dead Letter Message Count"
    threshold: 20
    operator: "GreaterThan"
    time_aggregation: "Total"
    
  - name: "Server Error Count"
    threshold: 10
    operator: "GreaterThan"
    time_aggregation: "Total"
    
  - name: "Processing Duration"
    threshold: 300  # 5분
    operator: "GreaterThan"
    time_aggregation: "Average"
```

### 7.2 알림 구성
```yaml
alerts:
  - name: "High Trip Processing Queue"
    condition: "Active messages in trip-creation-process > 1000"
    severity: "Warning"
    action_group: "tripgen-alerts-prod"
    
  - name: "Trip Processing Timeout"
    condition: "Messages in queue > 10 minutes"
    severity: "Error"
    action_group: "tripgen-alerts-prod"
    
  - name: "Trip Service Bus Unavailable"
    condition: "Availability < 99.9%"
    severity: "Critical"
    action_group: "tripgen-alerts-prod"
```

## 8. 설치 스크립트

### 8.1 Azure CLI 스크립트
```bash
#!/bin/bash
# Trip Service Bus 프로덕션 환경 설치

# 변수 설정
RESOURCE_GROUP="tripgen-prod-rg"
NAMESPACE_NAME="trip-servicebus-prod"
LOCATION="koreacentral"
SECONDARY_LOCATION="koreasouth"

# Service Bus Namespace 생성 (Premium, 2 Units)
az servicebus namespace create \
  --resource-group $RESOURCE_GROUP \
  --name $NAMESPACE_NAME \
  --location $LOCATION \
  --sku Premium \
  --capacity 2

# Geo-DR 구성
az servicebus georecovery-alias create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --alias trip-servicebus-dr \
  --partner-namespace "/subscriptions/{subscription-id}/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ServiceBus/namespaces/trip-servicebus-prod-secondary"

# Private Endpoint 생성
az network private-endpoint create \
  --resource-group $RESOURCE_GROUP \
  --name trip-servicebus-pe-prod \
  --vnet-name tripgen-vnet-prod \
  --subnet servicebus-subnet-prod \
  --private-connection-resource-id "/subscriptions/{subscription-id}/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ServiceBus/namespaces/$NAMESPACE_NAME" \
  --group-id namespace \
  --connection-name trip-servicebus-connection

# 복잡한 처리를 위한 Queue 생성 (세션 활성화)
az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name trip-creation-process \
  --max-size 10240 \
  --default-message-time-to-live "PT48H" \
  --lock-duration "PT10M" \
  --max-delivery-count 5 \
  --enable-dead-lettering-on-message-expiration true \
  --enable-partitioning true \
  --enable-duplicate-detection true \
  --require-session true

# 예약 처리를 위한 Queue 생성 (세션 활성화)
az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name trip-booking \
  --max-size 5120 \
  --default-message-time-to-live "PT6H" \
  --lock-duration "PT5M" \
  --max-delivery-count 3 \
  --enable-dead-lettering-on-message-expiration true \
  --enable-partitioning true \
  --enable-duplicate-detection true \
  --require-session true

# Topic 및 다중 Subscription 생성
az servicebus topic create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name trip-events \
  --max-size 10240 \
  --enable-partitioning true \
  --enable-duplicate-detection true

# 각 서비스별 Subscription 생성
for service in user-service ai-service location-service payment-service; do
  az servicebus topic subscription create \
    --resource-group $RESOURCE_GROUP \
    --namespace-name $NAMESPACE_NAME \
    --topic-name trip-events \
    --name "${service}-subscription" \
    --lock-duration "PT5M" \
    --max-delivery-count 10
done
```

## 9. 검증 계획

### 9.1 기능 검증
- 세션 기반 메시지 순차 처리 테스트
- 복잡한 여행 일정 생성 프로세스 테스트
- 다중 서비스 이벤트 배포 테스트
- 예약 시나리오 End-to-End 테스트

### 9.2 성능 검증
- 고처리량 테스트 (Premium 2 Units: 2,000 msg/sec)
- 복잡한 메시지 처리 지연시간 측정
- 세션 기반 처리 성능 측정

### 9.3 장애복구 검증
- 여행 예약 중 장애 상황 복구 테스트
- 세션 메시지 장애조치 테스트
- 다중 서비스 연계 장애 시나리오 테스트

## 10. 운영 가이드

### 10.1 일상 운영
- 여행 일정 생성 큐 모니터링 (처리 시간이 길 수 있음)
- 예약 관련 Dead Letter 즉시 처리
- 세션 메시지 처리 상태 모니터링

### 10.2 확장 계획
- Premium Unit 증설 기준: 처리량 > 1,800 msg/sec
- 복잡한 여행 일정 처리 시간 > 8분 시 확장 검토
- 예약 대기 시간 > 30초 시 확장 검토

### 10.3 백업 및 복구
- 여행 일정 생성 중 메시지 보존 정책
- 예약 관련 메시지 우선 복구 절차
- 세션 상태 복구 매뉴얼