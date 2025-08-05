# AI 서비스 Message Queue 설치 계획서 (프로덕션)

## 1. 개요
- **서비스명**: AI Service
- **환경**: Production
- **MQ 솔루션**: Azure Service Bus Premium Tier
- **지역**: Korea Central (Primary), Korea South (Secondary)

## 2. 인프라 구성

### 2.1 Service Bus Namespace 구성
```yaml
# Azure Resource Manager Template
name: ai-servicebus-prod
sku: Premium
tier: Premium
capacity: 4  # Premium Units (PU) - AI 처리 높은 처리량 요구사항
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
  name: ai-servicebus-pe-prod
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

### 3.1 AI Trip Generation Queue
```yaml
queue_name: ai-trip-generation
properties:
  max_size_in_megabytes: 20480  # 매우 높은 용량 (AI 작업용)
  default_message_time_to_live: "PT72H"  # 72 hours (AI 처리 시간 고려)
  lock_duration: "PT30M"  # 30 minutes (AI 처리 시간)
  max_delivery_count: 3
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT1H"
  enable_sessions: true  # AI 모델별 순차 처리
```

### 3.2 AI Recommendation Queue
```yaml
queue_name: ai-recommendation
properties:
  max_size_in_megabytes: 10240
  default_message_time_to_live: "PT24H"  # 24 hours
  lock_duration: "PT10M"  # 10 minutes
  max_delivery_count: 5
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT30M"
  enable_sessions: false
```

### 3.3 AI Analysis Queue
```yaml
queue_name: ai-analysis
properties:
  max_size_in_megabytes: 15360
  default_message_time_to_live: "PT48H"  # 48 hours
  lock_duration: "PT15M"  # 15 minutes
  max_delivery_count: 3
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT30M"
  enable_sessions: true  # 분석 작업 순차 처리
```

### 3.4 AI Model Training Queue
```yaml
queue_name: ai-model-training
properties:
  max_size_in_megabytes: 51200  # 최대 용량 (모델 학습용)
  default_message_time_to_live: "P7D"  # 7 days (모델 학습 시간)
  lock_duration: "PT2H"  # 2 hours (모델 학습 시간)
  max_delivery_count: 2
  dead_lettering_on_message_expiration: true
  enable_partitioning: false  # 학습 데이터 순서 보장
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT24H"
  enable_sessions: true  # 모델별 순차 학습
```

### 3.5 AI Batch Processing Queue
```yaml
queue_name: ai-batch-processing
properties:
  max_size_in_megabytes: 10240
  default_message_time_to_live: "PT12H"  # 12 hours
  lock_duration: "PT5M"  # 5 minutes
  max_delivery_count: 3
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: false  # 배치 작업 중복 허용
  enable_sessions: false
```

## 4. Topic 구성

### 4.1 AI Events Topic
```yaml
topic_name: ai-events
properties:
  max_size_in_megabytes: 20480
  default_message_time_to_live: "PT48H"
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT1H"
  
subscriptions:
  - name: trip-service-subscription
    properties:
      lock_duration: "PT5M"
      max_delivery_count: 10
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'TripGenerated' OR EventType = 'RecommendationReady'"
  
  - name: user-service-subscription
    properties:
      lock_duration: "PT3M"
      max_delivery_count: 5
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'PersonalizationUpdated' OR EventType = 'PreferenceAnalyzed'"
        
  - name: analytics-service-subscription
    properties:
      lock_duration: "PT5M"
      max_delivery_count: 5
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType LIKE '%Completed%' OR EventType LIKE '%Analysis%'"
        
  - name: notification-service-subscription
    properties:
      lock_duration: "PT2M"
      max_delivery_count: 3
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'TripGenerated' OR EventType = 'AnalysisCompleted'"
```

### 4.2 AI Model Events Topic
```yaml
topic_name: ai-model-events
properties:
  max_size_in_megabytes: 5120
  default_message_time_to_live: "PT24H"
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT30M"
  
subscriptions:
  - name: model-management-subscription
    properties:
      lock_duration: "PT10M"
      max_delivery_count: 3
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'ModelTrained' OR EventType = 'ModelDeployed'"
        
  - name: monitoring-service-subscription
    properties:
      lock_duration: "PT2M"
      max_delivery_count: 5
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "1=1"  # 모든 모델 이벤트 모니터링
```

## 5. 보안 구성

### 5.1 Managed Identity 구성
```yaml
# AI Service App의 Managed Identity
managed_identity:
  type: SystemAssigned
  
# Role Assignments
role_assignments:
  - principal_id: "{ai-service-managed-identity-id}"
    role_definition: "Azure Service Bus Data Owner"
    scope: "/subscriptions/{subscription-id}/resourceGroups/{rg-name}/providers/Microsoft.ServiceBus/namespaces/ai-servicebus-prod"
```

### 5.2 Access Policies (Backup용)
```yaml
access_policies:
  - name: ai-service-full-access
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
connection_endpoint: "ai-servicebus-prod.servicebus.windows.net"
authentication: "managed_identity"

# Fallback Connection String (암호화하여 Key Vault에 저장)
fallback_connection_string: "Endpoint=sb://ai-servicebus-prod.servicebus.windows.net/;SharedAccessKeyName=ai-service-full-access;SharedAccessKey={key}"
```

### 6.2 Application 구성
```yaml
# appsettings.Production.json
ServiceBus:
  Namespace: "ai-servicebus-prod.servicebus.windows.net"
  UseManagedIdentity: true
  ConnectionString: "@Microsoft.KeyVault(SecretUri=https://tripgen-kv-prod.vault.azure.net/secrets/ai-servicebus-connection/)"
  
Queues:
  TripGeneration: "ai-trip-generation"
  Recommendation: "ai-recommendation"
  Analysis: "ai-analysis"
  ModelTraining: "ai-model-training"
  BatchProcessing: "ai-batch-processing"
  
Topics:
  AIEvents: "ai-events"
  ModelEvents: "ai-model-events"

# AI 특화 설정
AI:
  MaxProcessingTime: "PT30M"  # 최대 AI 처리 시간
  ModelParallelism: 4  # 병렬 모델 실행 수
  BatchSize: 100  # 배치 처리 크기
```

## 7. 모니터링 및 알림

### 7.1 메트릭 구성
```yaml
monitoring:
  log_analytics_workspace: "tripgen-logs-prod"
  
metrics:
  - name: "AI Processing Queue Depth"
    threshold: 5000  # AI 처리 대기 메시지
    operator: "GreaterThan"
    time_aggregation: "Average"
    
  - name: "AI Processing Duration"
    threshold: 1800  # 30분
    operator: "GreaterThan"
    time_aggregation: "Average"
    
  - name: "Model Training Queue Depth"
    threshold: 10
    operator: "GreaterThan"
    time_aggregation: "Total"
    
  - name: "Dead Letter Message Count"
    threshold: 5  # AI 작업 실패는 심각
    operator: "GreaterThan"
    time_aggregation: "Total"
    
  - name: "GPU Utilization"  # Custom metric
    threshold: 90
    operator: "GreaterThan"
    time_aggregation: "Average"
```

### 7.2 알림 구성
```yaml
alerts:
  - name: "AI Processing Backlog"
    condition: "AI processing queue > 3000 messages"
    severity: "Warning"
    action_group: "tripgen-ai-alerts-prod"
    
  - name: "AI Processing Timeout"
    condition: "Processing time > 25 minutes"
    severity: "Error"
    action_group: "tripgen-ai-alerts-prod"
    
  - name: "Model Training Failed"
    condition: "Dead letter in model-training queue > 0"
    severity: "Critical"
    action_group: "tripgen-ai-alerts-prod"
    
  - name: "AI Service Unavailable"
    condition: "No messages processed in 10 minutes"
    severity: "Critical"
    action_group: "tripgen-ai-alerts-prod"
```

## 8. 설치 스크립트

### 8.1 Azure CLI 스크립트
```bash
#!/bin/bash
# AI Service Bus 프로덕션 환경 설치

# 변수 설정
RESOURCE_GROUP="tripgen-prod-rg"
NAMESPACE_NAME="ai-servicebus-prod"
LOCATION="koreacentral"
SECONDARY_LOCATION="koreasouth"

# Service Bus Namespace 생성 (Premium, 4 Units)
az servicebus namespace create \
  --resource-group $RESOURCE_GROUP \
  --name $NAMESPACE_NAME \
  --location $LOCATION \
  --sku Premium \
  --capacity 4

# Geo-DR 구성
az servicebus georecovery-alias create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --alias ai-servicebus-dr \
  --partner-namespace "/subscriptions/{subscription-id}/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ServiceBus/namespaces/ai-servicebus-prod-secondary"

# Private Endpoint 생성
az network private-endpoint create \
  --resource-group $RESOURCE_GROUP \
  --name ai-servicebus-pe-prod \
  --vnet-name tripgen-vnet-prod \
  --subnet servicebus-subnet-prod \
  --private-connection-resource-id "/subscriptions/{subscription-id}/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ServiceBus/namespaces/$NAMESPACE_NAME" \
  --group-id namespace \
  --connection-name ai-servicebus-connection

# AI 처리용 대용량 Queue 생성
az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name ai-trip-generation \
  --max-size 20480 \
  --default-message-time-to-live "PT72H" \
  --lock-duration "PT30M" \
  --max-delivery-count 3 \
  --enable-dead-lettering-on-message-expiration true \
  --enable-partitioning true \
  --enable-duplicate-detection true \
  --require-session true

# 모델 학습용 초대용량 Queue 생성
az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name ai-model-training \
  --max-size 51200 \
  --default-message-time-to-live "P7D" \
  --lock-duration "PT2H" \
  --max-delivery-count 2 \
  --enable-dead-lettering-on-message-expiration true \
  --enable-partitioning false \
  --enable-duplicate-detection true \
  --require-session true

# AI 이벤트 Topic 생성
az servicebus topic create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name ai-events \
  --max-size 20480 \
  --enable-partitioning true \
  --enable-duplicate-detection true

# 다중 서비스 Subscription 생성
for service in trip-service user-service analytics-service notification-service; do
  az servicebus topic subscription create \
    --resource-group $RESOURCE_GROUP \
    --namespace-name $NAMESPACE_NAME \
    --topic-name ai-events \
    --name "${service}-subscription" \
    --lock-duration "PT5M" \
    --max-delivery-count 10
done
```

## 9. 검증 계획

### 9.1 기능 검증
- AI 모델 추론 처리 시간 측정
- 대용량 데이터 배치 처리 테스트
- 모델 학습 Job 스케줄링 테스트
- GPU 리소스 연동 테스트

### 9.2 성능 검증
- 초고처리량 테스트 (Premium 4 Units: 4,000 msg/sec)
- AI 모델 병렬 처리 테스트
- 메모리 사용량 최적화 테스트
- 장시간 AI 작업 안정성 테스트

### 9.3 장애복구 검증
- AI 처리 중 장애 복구 테스트
- 모델 학습 중단 시 재시작 테스트
- GPU 장애 시 CPU 대체 처리 테스트

## 10. 운영 가이드

### 10.1 일상 운영
- AI 처리 큐 깊이 실시간 모니터링
- 모델 성능 지표 추적
- GPU/CPU 리소스 사용률 모니터링
- 장시간 처리 작업 진행 상황 확인

### 10.2 확장 계획
- Premium Unit 증설 기준: 
  - 처리량 > 3,500 msg/sec
  - AI 처리 대기 시간 > 5분
  - GPU 사용률 > 85%
- 모델 병렬 처리 인스턴스 확장

### 10.3 AI 특화 운영
- 모델 버전 관리 및 배포 자동화
- A/B 테스트를 위한 모델 라우팅
- 모델 성능 저하 감지 및 자동 롤백
- 학습 데이터 품질 모니터링