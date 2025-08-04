# 위치 서비스 Message Queue 설치 계획서 (프로덕션)

## 1. 개요
- **서비스명**: Location Service
- **환경**: Production
- **MQ 솔루션**: Azure Service Bus Premium Tier
- **지역**: Korea Central (Primary), Korea South (Secondary)

## 2. 인프라 구성

### 2.1 Service Bus Namespace 구성
```yaml
# Azure Resource Manager Template
name: location-servicebus-prod
sku: Premium
tier: Premium
capacity: 2  # Premium Units (PU) - 지리적 데이터 처리량 고려
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
  name: location-servicebus-pe-prod
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

### 3.1 Location Search Queue
```yaml
queue_name: location-search
properties:
  max_size_in_megabytes: 5120
  default_message_time_to_live: "PT6H"  # 6 hours
  lock_duration: "PT3M"  # 3 minutes
  max_delivery_count: 5
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT10M"
  enable_sessions: false
```

### 3.2 Location Data Update Queue
```yaml
queue_name: location-data-update
properties:
  max_size_in_megabytes: 10240  # 지리적 데이터 용량
  default_message_time_to_live: "PT48H"  # 48 hours
  lock_duration: "PT10M"  # 10 minutes (외부 API 호출 시간)
  max_delivery_count: 3
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT30M"
  enable_sessions: false
```

### 3.3 Route Calculation Queue
```yaml
queue_name: route-calculation
properties:
  max_size_in_megabytes: 7680
  default_message_time_to_live: "PT12H"  # 12 hours
  lock_duration: "PT5M"  # 5 minutes
  max_delivery_count: 5
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT15M"
  enable_sessions: false
```

### 3.4 POI (Point of Interest) Processing Queue
```yaml
queue_name: poi-processing
properties:
  max_size_in_megabytes: 10240
  default_message_time_to_live: "PT24H"  # 24 hours
  lock_duration: "PT8M"  # 8 minutes (복잡한 POI 분석)
  max_delivery_count: 3
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT20M"
  enable_sessions: false
```

### 3.5 Geofencing Queue
```yaml
queue_name: geofencing
properties:
  max_size_in_megabytes: 3072
  default_message_time_to_live: "PT4H"  # 4 hours (실시간성 중요)
  lock_duration: "PT2M"  # 2 minutes
  max_delivery_count: 5
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: false  # 위치 이벤트는 중복 허용
  enable_sessions: false
```

### 3.6 Location Cache Refresh Queue
```yaml
queue_name: location-cache-refresh
properties:
  max_size_in_megabytes: 5120
  default_message_time_to_live: "PT1H"  # 1 hour (캐시 최신성)
  lock_duration: "PT3M"  # 3 minutes
  max_delivery_count: 3
  dead_lettering_on_message_expiration: true
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT30M"
  enable_sessions: false
```

## 4. Topic 구성

### 4.1 Location Events Topic
```yaml
topic_name: location-events
properties:
  max_size_in_megabytes: 10240
  default_message_time_to_live: "PT24H"
  enable_partitioning: true
  enable_duplicate_detection: true
  duplicate_detection_history_time_window: "PT30M"
  
subscriptions:
  - name: trip-service-subscription
    properties:
      lock_duration: "PT5M"
      max_delivery_count: 10
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'LocationFound' OR EventType = 'RouteCalculated' OR EventType = 'POIDiscovered'"
  
  - name: user-service-subscription
    properties:
      lock_duration: "PT3M"
      max_delivery_count: 5
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'UserLocationUpdated' OR EventType = 'GeofenceEntered' OR EventType = 'GeofenceExited'"
        
  - name: ai-service-subscription
    properties:
      lock_duration: "PT5M"
      max_delivery_count: 5
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'LocationAnalysisReady' OR EventType = 'POIAnalyzed'"
        
  - name: notification-service-subscription
    properties:
      lock_duration: "PT2M"
      max_delivery_count: 3
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType = 'GeofenceEntered' OR EventType = 'GeofenceExited' OR EventType = 'LocationAlert'"
```

### 4.2 Location Analytics Topic
```yaml
topic_name: location-analytics
properties:
  max_size_in_megabytes: 5120
  default_message_time_to_live: "PT12H"
  enable_partitioning: true
  enable_duplicate_detection: false  # 분석 데이터는 중복 가능
  
subscriptions:
  - name: analytics-service-subscription
    properties:
      lock_duration: "PT3M"
      max_delivery_count: 5
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "1=1"  # 모든 위치 분석 데이터 수집
        
  - name: business-intelligence-subscription
    properties:
      lock_duration: "PT5M"
      max_delivery_count: 3
      dead_lettering_on_message_expiration: true
      default_rule:
        filter: "EventType LIKE '%Popular%' OR EventType LIKE '%Traffic%'"
```

## 5. 보안 구성

### 5.1 Managed Identity 구성
```yaml
# Location Service App의 Managed Identity
managed_identity:
  type: SystemAssigned
  
# Role Assignments
role_assignments:
  - principal_id: "{location-service-managed-identity-id}"
    role_definition: "Azure Service Bus Data Owner"
    scope: "/subscriptions/{subscription-id}/resourceGroups/{rg-name}/providers/Microsoft.ServiceBus/namespaces/location-servicebus-prod"
```

### 5.2 Access Policies (Backup용)
```yaml
access_policies:
  - name: location-service-full-access
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
connection_endpoint: "location-servicebus-prod.servicebus.windows.net"
authentication: "managed_identity"

# Fallback Connection String (암호화하여 Key Vault에 저장)
fallback_connection_string: "Endpoint=sb://location-servicebus-prod.servicebus.windows.net/;SharedAccessKeyName=location-service-full-access;SharedAccessKey={key}"
```

### 6.2 Application 구성
```yaml
# appsettings.Production.json
ServiceBus:
  Namespace: "location-servicebus-prod.servicebus.windows.net"
  UseManagedIdentity: true
  ConnectionString: "@Microsoft.KeyVault(SecretUri=https://tripgen-kv-prod.vault.azure.net/secrets/location-servicebus-connection/)"
  
Queues:
  LocationSearch: "location-search"
  LocationDataUpdate: "location-data-update"
  RouteCalculation: "route-calculation"
  POIProcessing: "poi-processing"
  Geofencing: "geofencing"
  LocationCacheRefresh: "location-cache-refresh"
  
Topics:
  LocationEvents: "location-events"
  LocationAnalytics: "location-analytics"

# 위치 서비스 특화 설정
Location:
  MaxSearchRadius: 50000  # 50km
  CacheExpiryMinutes: 30
  RouteCalculationTimeout: "PT5M"
  GeofenceCheckInterval: "PT1M"
```

## 7. 모니터링 및 알림

### 7.1 메트릭 구성
```yaml
monitoring:
  log_analytics_workspace: "tripgen-logs-prod"
  
metrics:
  - name: "Location Search Queue Depth"
    threshold: 1500
    operator: "GreaterThan"
    time_aggregation: "Average"
    
  - name: "Route Calculation Duration"
    threshold: 240  # 4분
    operator: "GreaterThan"
    time_aggregation: "Average"
    
  - name: "POI Processing Duration"
    threshold: 480  # 8분
    operator: "GreaterThan"
    time_aggregation: "Average"
    
  - name: "Geofencing Response Time"
    threshold: 60  # 1분
    operator: "GreaterThan"
    time_aggregation: "Average"
    
  - name: "External API Call Failures"
    threshold: 10
    operator: "GreaterThan"
    time_aggregation: "Total"
```

### 7.2 알림 구성
```yaml
alerts:
  - name: "High Location Search Load"
    condition: "Search queue > 1000 messages"
    severity: "Warning"
    action_group: "tripgen-location-alerts-prod"
    
  - name: "Route Calculation Timeout"
    condition: "Route calculation > 4 minutes"
    severity: "Error"
    action_group: "tripgen-location-alerts-prod"
    
  - name: "External Maps API Failure"
    condition: "API failures > 5 in 5 minutes"
    severity: "Critical"
    action_group: "tripgen-location-alerts-prod"
    
  - name: "Geofencing Service Down"
    condition: "No geofence events in 5 minutes"
    severity: "Critical"
    action_group: "tripgen-location-alerts-prod"
```

## 8. 설치 스크립트

### 8.1 Azure CLI 스크립트
```bash
#!/bin/bash
# Location Service Bus 프로덕션 환경 설치

# 변수 설정
RESOURCE_GROUP="tripgen-prod-rg"
NAMESPACE_NAME="location-servicebus-prod"
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
  --alias location-servicebus-dr \
  --partner-namespace "/subscriptions/{subscription-id}/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ServiceBus/namespaces/location-servicebus-prod-secondary"

# Private Endpoint 생성
az network private-endpoint create \
  --resource-group $RESOURCE_GROUP \
  --name location-servicebus-pe-prod \
  --vnet-name tripgen-vnet-prod \
  --subnet servicebus-subnet-prod \
  --private-connection-resource-id "/subscriptions/{subscription-id}/resourceGroups/$RESOURCE_GROUP/providers/Microsoft.ServiceBus/namespanes/$NAMESPACE_NAME" \
  --group-id namespace \
  --connection-name location-servicebus-connection

# 위치 검색용 Queue 생성
az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name location-search \
  --max-size 5120 \
  --default-message-time-to-live "PT6H" \
  --lock-duration "PT3M" \
  --max-delivery-count 5 \
  --enable-dead-lettering-on-message-expiration true \
  --enable-partitioning true \
  --enable-duplicate-detection true

# 지리적 데이터 업데이트용 대용량 Queue 생성
az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name location-data-update \
  --max-size 10240 \
  --default-message-time-to-live "PT48H" \
  --lock-duration "PT10M" \
  --max-delivery-count 3 \
  --enable-dead-lettering-on-message-expiration true \
  --enable-partitioning true \
  --enable-duplicate-detection true

# 실시간 지오펜싱용 Queue 생성
az servicebus queue create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name geofencing \
  --max-size 3072 \
  --default-message-time-to-live "PT4H" \
  --lock-duration "PT2M" \
  --max-delivery-count 5 \
  --enable-dead-lettering-on-message-expiration true \
  --enable-partitioning true \
  --enable-duplicate-detection false

# 위치 이벤트 Topic 생성
az servicebus topic create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name location-events \
  --max-size 10240 \
  --enable-partitioning true \
  --enable-duplicate-detection true

# 다중 서비스 Subscription 생성
for service in trip-service user-service ai-service notification-service; do
  az servicebus topic subscription create \
    --resource-group $RESOURCE_GROUP \
    --namespace-name $NAMESPACE_NAME \
    --topic-name location-events \
    --name "${service}-subscription" \
    --lock-duration "PT5M" \
    --max-delivery-count 10
done

# 위치 분석 Topic 생성
az servicebus topic create \
  --resource-group $RESOURCE_GROUP \
  --namespace-name $NAMESPACE_NAME \
  --name location-analytics \
  --max-size 5120 \
  --enable-partitioning true \
  --enable-duplicate-detection false
```

## 9. 외부 API 연동 구성

### 9.1 지도 서비스 API 구성
```yaml
external_apis:
  google_maps:
    endpoint: "https://maps.googleapis.com/maps/api"
    rate_limit: 100000  # requests per day
    timeout: "PT30S"
    retry_count: 3
    
  naver_maps:
    endpoint: "https://naveropenapi.apigw.ntruss.com"
    rate_limit: 50000
    timeout: "PT30S"
    retry_count: 3
    
  kakao_maps:
    endpoint: "https://dapi.kakao.com"
    rate_limit: 30000
    timeout: "PT30S"
    retry_count: 3
```

## 10. 검증 계획

### 10.1 기능 검증
- 위치 검색 정확도 테스트
- 경로 계산 성능 테스트
- POI 데이터 품질 검증
- 지오펜싱 실시간 반응 테스트
- 외부 API 연동 안정성 테스트

### 10.2 성능 검증
- 대량 위치 검색 동시 처리 테스트
- 복잡한 경로 계산 성능 측정
- 지도 데이터 캐싱 효율성 검증
- 실시간 위치 추적 지연시간 측정

### 10.3 장애복구 검증
- 외부 지도 API 장애 시 대체 API 전환 테스트
- 위치 데이터 캐시 무효화 및 복구 테스트
- 지오펜싱 서비스 중단 시 복구 테스트

## 11. 운영 가이드

### 11.1 일상 운영
- 위치 검색 응답 시간 모니터링
- 외부 API 호출 한도 및 성공률 추적
- POI 데이터 최신성 확인
- 지오펜싱 이벤트 정확도 모니터링

### 11.2 확장 계획
- Premium Unit 증설 기준:
  - 위치 검색 처리량 > 1,500 req/sec
  - 경로 계산 대기 시간 > 2분
  - POI 처리 대기 시간 > 5분
- 지리적 분산을 위한 지역별 캐시 확장

### 11.3 데이터 관리
- 위치 데이터 정기 업데이트 스케줄
- 사용하지 않는 POI 데이터 정리
- 지오펜스 경계 정확도 검증
- 외부 API 사용량 최적화