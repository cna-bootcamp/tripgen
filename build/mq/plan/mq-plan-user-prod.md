# 사용자 서비스 Message Queue 설치 계획서 (프로덕션)

## 1. 개요
- **서비스명**: User Service
- **환경**: Production
- **MQ 솔루션**: Azure Service Bus Premium Tier
- **지역**: Korea Central (Primary), Korea South (Secondary)

## 2. 인프라 구성

### 2.1 Service Bus Namespace 구성
```yaml
# Azure Resource Manager Template
name: user-servicebus-prod
sku: Premium
tier: Premium
capacity: 1  # Premium Units (PU)
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
  name: user-servicebus-pe-prod
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

### 3.1 User Registration Queue
```yaml
queue_name: user-registration
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

### 3.2 User Profile Update Queue
```yaml
queue_name: user-profile-update
properties:
  max_size_in_megabytes: 5120
  default_message_time_to_live: "PT12H"  # 12 hours
  lock_duration: "PT3M"  # 3 minutes
  max_delivery_count: 5
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT10M"
  enable_sessions: false
```

### 3.3 User Notification Queue
```yaml
queue_name: user-notification
properties:
  max_size_in_megabytes: 2048
  default_message_time_to_live: "PT6H"  # 6 hours
  lock_duration: "PT1M"  # 1 minute
  max_delivery_count: 3
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: false
  enable_sessions: false
```

## 4. Topic 구성

### 4.1 User Events Topic
```yaml
topic_name: user-events
properties:
  max_size_in_megabytes: 5120
  default_message_time_to_live: "PT24H"
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT10M"
  
subscriptions:
  - name: trip-service-subscription
    properties:
      lock_duration: "PT5M"
      max_delivery_count: 10
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'UserRegistered' OR EventType = 'UserProfileUpdated'"
  
  - name: ai-service-subscription
    properties:
      lock_duration: "PT3M"
      max_delivery_count: 5
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'UserPreferenceUpdated'"
        
  - name: location-service-subscription
    properties:
      lock_duration: "PT3M"
      max_delivery_count: 5
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'UserLocationUpdated'"
```

## 5. 보안 구성

### 5.1 Managed Identity 구성
```yaml
# User Service App의 Managed Identity
managed_identity:
  type: SystemAssigned
  
# Role Assignments
role_assignments:
  - principal_id: "{user-service-managed-identity-id}"
    role_definition: "Azure Service Bus Data Owner"
    scope: "/subscriptions/{subscription-id}/resourceGroups/{rg-name}/providers/Microsoft.ServiceBus/namespaces/user-servicebus-prod"
```

### 5.2 Access Policies (Backup용)
```yaml
access_policies:
  - name: user-service-send-listen
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
connection_endpoint: "user-servicebus-prod.servicebus.windows.net"
authentication: "managed_identity"

# Fallback Connection String (암호화하여 Key Vault에 저장)
fallback_connection_string: "Endpoint=sb://user-servicebus-prod.servicebus.windows.net/;SharedAccessKeyName=user-service-send-listen;SharedAccessKey={key}"
```

### 6.2 Application 구성
```yaml
# appsettings.Production.json
ServiceBus:
  Namespace: "user-servicebus-prod.servicebus.windows.net"
  UseManagedIdentity: true
  ConnectionString: "@Microsoft.KeyVault(SecretUri=https://tripgen-kv-prod.vault.azure.net/secrets/user-servicebus-connection/)"
  
Queues:
  UserRegistration: "user-registration"
  UserProfileUpdate: "user-profile-update"
  UserNotification: "user-notification"
  
Topics:
  UserEvents: "user-events"
```

## 7. 모니터링 및 알림

### 7.1 메트릭 구성
```yaml
monitoring:
  log_analytics_workspace: "tripgen-logs-prod"
  
metrics:
  - name: "Active Message Count"
    threshold: 1000
    operator: "GreaterThan"
    time_aggregation: "Average"
    
  - name: "Dead Letter Message Count"
    threshold: 10
    operator: "GreaterThan"
    time_aggregation: "Total"
    
  - name: "Server Error Count"
    threshold: 5
    operator: "GreaterThan"
    time_aggregation: "Total"
```

### 7.2 알림 구성
```yaml
alerts:
  - name: "High Message Count Alert"
    condition: "Active messages > 1000"
    severity: "Warning"
    action_group: "tripgen-alerts-prod"
    
  - name: "Dead Letter Alert"
    condition: "Dead letter messages > 0"
    severity: "Error"
    action_group: "tripgen-alerts-prod"
    
  - name: "Service Bus Unavailable"
    condition: "Availability < 99%"
    severity: "Critical"
    action_group: "tripgen-alerts-prod"
```

## 8. 설치 스크립트

### 8.1 Azure CLI 스크립트
```bash
#!/bin/bash
# User Service Bus 프로덕션 환경 설치

# 변수 설정
RESOURCE_GROUP="tripgen-prod-rg"
NAMESPACE_NAME="user-servicebus-prod"
LOCATION="koreacentral"
SECONDARY_LOCATION="koreasouth"

# Service Bus Namespace 생성 (Premium)
az servicebus namespace create \
  --resource-group $RESOURCE_GROUP \
  --name $NAMESPACE_NAME \
  --location $LOCATION \
  --sku Premium \
  --capacity 1

# Geo-DR 구성
az servicebus georecovery-alias create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --alias user-servicebus-dr \
  --partner-namespace "/subscriptions/{subscription-id}/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ServiceBus/namespaces/user-servicebus-prod-secondary"

# Private Endpoint 생성
az network private-endpoint create \
  --resource-group $RESOURCE_GROUP \
  --name user-servicebus-pe-prod \
  --vnet-name tripgen-vnet-prod \
  --subnet servicebus-subnet-prod \
  --private-connection-resource-id "/subscriptions/{subscription-id}/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ServiceBus/namespaces/$NAMESPACE_NAME" \
  --group-id namespace \
  --connection-name user-servicebus-connection

# Queue 생성
az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name user-registration \
  --max-size 5120 \
  --default-message-time-to-live "PT24H" \
  --lock-duration "PT5M" \
  --max-delivery-count 10 \
  --enable-dead-lettering-on-message-expiration true \
  --enable-partitioning true \
  --enable-duplicate-detection true

# Topic 및 Subscription 생성
az servicebus topic create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name user-events \
  --max-size 5120 \
  --enable-partitioning true \
  --enable-duplicate-detection true

az servicebus topic subscription create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --topic-name user-events \
  --name trip-service-subscription \
  --lock-duration "PT5M" \
  --max-delivery-count 10
```

## 9. 검증 계획

### 9.1 기능 검증
- Queue 메시지 송수신 테스트
- Topic/Subscription 메시지 배포 테스트
- Dead Letter Queue 동작 테스트
- Duplicate Detection 테스트

### 9.2 성능 검증
- 처리량 테스트 (Premium: 1,000 msg/sec)
- 지연시간 측정
- 동시 연결 수 테스트

### 9.3 장애복구 검증
- Geo-DR 수동 장애조치 테스트
- Private Endpoint 연결 테스트
- Managed Identity 인증 테스트

## 10. 운영 가이드

### 10.1 일상 운영
- 메시지 큐 깊이 모니터링
- Dead Letter 메시지 처리
- 처리량 및 지연시간 모니터링

### 10.2 확장 계획
- Premium Unit 증설 기준: CPU > 80%, Memory > 80%
- 메시지 처리량 > 800 msg/sec 시 확장 검토

### 10.3 백업 및 복구
- Namespace 메타데이터 정기 백업
- 구성 정보 Git 저장소 관리
- 장애조치 절차서 유지관리