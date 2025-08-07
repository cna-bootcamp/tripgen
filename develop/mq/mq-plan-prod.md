# TripGen Message Queue 설치계획서 - 운영환경

## 1. 개요

### 1.1 문서 목적
TripGen 서비스의 운영환경에서 엔터프라이즈급 비동기 메시징 시스템 구축을 위한 Azure Service Bus Premium 설치 계획 수립

### 1.2 적용 환경
- **대상 환경**: 운영환경 (Production)
- **서비스**: Azure Service Bus Premium Tier
- **용도**: 대규모 AI 일정 생성, 실시간 장소/경로 처리, 이벤트 기반 아키텍처 지원

### 1.3 관련 문서
- 외부시퀀스설계서: `design/backend/sequence/outer/*.puml`
- 물리아키텍처: `design/backend/physical/physical-architecture-prod.md`
- 백킹서비스설치방법: `references/백킹서비스설치방법.md`

## 2. 요구사항 분석

### 2.1 비동기 통신 요구사항

#### 2.1.1 식별된 비동기 처리 플로우
| 플로우 | 큐/토픽 | 메시지 타입 | 처리 시간 | 우선순위 | 예상 트래픽 |
|--------|----------|-------------|-----------|----------|------------|
| AI 일정 생성 | ai-schedule-generation | `{tripId, travelData, requestId}` | 5-10초 | 높음 | 10,000/일 |
| 장소 정보 요청 | location-search | `{destination, category, radius}` | 1-3초 | 중간 | 50,000/일 |
| 경로 계산 요청 | route-calculation | `{routes: [{from, to, mode}], tripId}` | 2-5초 | 중간 | 30,000/일 |
| 일정 재생성 | ai-schedule-regeneration | `{tripId, regenerateType, dayNumber?}` | 5-10초 | 높음 | 5,000/일 |
| 알림 메시지 | notification | `{userId, type, message}` | < 1초 | 낮음 | 100,000/일 |
| 이벤트 브로드캐스팅 | trip-events | 다양한 이벤트 타입 | < 1초 | 중간 | 200,000/일 |

#### 2.1.2 서비스별 역할
- **Producer Services**: Trip Service, AI Service, User Service
- **Consumer Services**: AI Service, Location Service, Notification Service
- **이벤트 구독자**: Audit Service, Analytics Service, Monitoring Service

### 2.2 성능 요구사항
- **처리량**: 피크 시 초당 500-1,000 메시지
- **메시지 크기**: 평균 32KB, 최대 100KB
- **지연 시간**: < 50ms (큐 처리 시간)
- **동시 처리**: 10-20개 워커 인스턴스 (Auto-scaling)
- **가용성**: 99.9% (월 43분 다운타임 허용)

### 2.3 엔터프라이즈 요구사항
- **고가용성**: Zone Redundant 구성
- **재해 복구**: Geo-Disaster Recovery
- **보안**: Private Endpoint, Managed Identity
- **규정 준수**: 데이터 보호 규정 준수
- **확장성**: 자동 확장 지원

## 3. Azure Service Bus 설치 계획

### 3.1 서비스 구성

#### 3.1.1 Service Bus Namespace
```yaml
namespace_configuration:
  name: sb-tripgen-prod
  tier: Premium
  messaging_units: 4  # 시작 4 units, 최대 16 units까지 확장
  location: Korea Central
  resource_group: tripgen-prod-rg
  
  high_availability:
    zone_redundant: true
    availability_zones: [1, 2, 3]
    
  disaster_recovery:
    geo_dr_enabled: true
    paired_namespace: sb-tripgen-prod-dr
    paired_region: Korea South
    
  capacity:
    max_size: 80GB per entity
    message_ttl: 14일
    
  security:
    managed_identity: Enabled
    private_endpoint: Required
    encryption: Customer-managed keys (Azure Key Vault)
```

#### 3.1.2 큐 설계
| 큐 이름 | 최대 크기 | 파티션 | 세션 | 중복 감지 | Lock Duration | Max Delivery | 용도 |
|---------|-----------|---------|-------|-----------|---------------|--------------|------|
| ai-schedule-generation | 80GB | 4 | Yes | Yes (10분) | 5분 | 3회 | AI 일정 생성 요청 |
| ai-schedule-regeneration | 80GB | 2 | Yes | Yes (10분) | 5분 | 3회 | AI 일정 재생성 요청 |
| location-search | 80GB | 4 | No | No | 30초 | 3회 | 장소 정보 검색 |
| route-calculation | 80GB | 4 | No | No | 1분 | 3회 | 경로 계산 요청 |
| notification | 80GB | 8 | No | Yes (5분) | 10초 | 5회 | 알림 메시지 |

#### 3.1.3 토픽 설계
| 토픽 이름 | 최대 크기 | 파티션 | 중복 감지 | 구독 | 용도 |
|-----------|-----------|---------|-----------|-------|------|
| trip-events | 80GB | 4 | Yes (30분) | 3 | 여행 이벤트 브로드캐스팅 |
| system-events | 80GB | 2 | Yes (30분) | 5 | 시스템 이벤트 |

**토픽 구독 상세:**
```yaml
trip_events_subscriptions:
  - name: audit-subscription
    filter: "EventType IN ('Created', 'Updated', 'Deleted')"
    forward_to: audit-queue
    max_delivery_count: 10
    
  - name: analytics-subscription  
    filter: "EventType IN ('Completed', 'Cancelled')"
    forward_to: analytics-queue
    max_delivery_count: 5
    
  - name: monitoring-subscription
    filter: "ALL"
    max_delivery_count: 3

system_events_subscriptions:
  - name: alert-subscription
    filter: "Severity IN ('Critical', 'High')"
    forward_to: alert-queue
    
  - name: logging-subscription
    filter: "ALL"
    forward_to: log-queue
```

### 3.2 보안 설정

#### 3.2.1 네트워크 보안
```yaml
network_security:
  private_endpoint:
    name: pe-servicebus-tripgen-prod
    subnet: /subscriptions/xxx/resourceGroups/tripgen-prod-rg/providers/Microsoft.Network/virtualNetworks/vnet-tripgen-prod/subnets/private-endpoint-subnet
    private_dns_zone: privatelink.servicebus.windows.net
    
  network_rules:
    default_action: Deny
    ip_rules: []  # No public IP access
    virtual_network_rules:
      - subnet_id: aks-subnet
      - subnet_id: management-subnet
    
  firewall:
    enabled: true
    trusted_services: Allow
```

#### 3.2.2 인증 및 권한
```yaml
authentication:
  managed_identity:
    system_assigned: true
    user_assigned:
      - name: tripgen-servicebus-identity
        
  rbac_assignments:
    - principal: tripgen-trip-service
      role: Azure Service Bus Data Sender
      scope: /queues/ai-schedule-generation
      
    - principal: tripgen-ai-service
      role: Azure Service Bus Data Owner
      scope: /queues/*
      
    - principal: tripgen-location-service
      role: Azure Service Bus Data Receiver
      scope: /queues/location-search
      
  key_vault_integration:
    vault_name: kv-tripgen-prod
    encryption_key: servicebus-encryption-key
    key_rotation: Automatic (90 days)
```

### 3.3 고가용성 및 재해복구

#### 3.3.1 Zone Redundancy 구성
```yaml
zone_redundancy:
  enabled: true
  zones: [1, 2, 3]
  
  data_replication:
    synchronous: true
    consistency: Strong
    
  failover:
    automatic: true
    rpo: 0  # Zero data loss
    rto: < 60 seconds
```

#### 3.3.2 Geo-Disaster Recovery
```yaml
geo_dr_configuration:
  primary_namespace: sb-tripgen-prod
  primary_region: Korea Central
  
  secondary_namespace: sb-tripgen-prod-dr
  secondary_region: Korea South
  
  pairing:
    alias: sb-tripgen-prod-alias
    failover_policy: Manual
    
  replication:
    metadata: Synchronous
    data: Asynchronous
    
  dr_drill:
    frequency: Quarterly
    procedure: Documented
```

### 3.4 모니터링 및 경보

#### 3.4.1 모니터링 구성
```yaml
monitoring:
  azure_monitor:
    metrics_enabled: true
    logs_enabled: true
    retention: 90 days
    
  diagnostic_settings:
    - name: servicebus-diagnostics
      logs:
        - OperationalLogs
        - VNetAndIPFilteringLogs
        - RuntimeAuditLogs
        - ApplicationMetricsLogs
      metrics:
        - AllMetrics
      destination:
        log_analytics_workspace: law-tripgen-prod
        storage_account: satripgenprodlogs
        
  application_insights:
    enabled: true
    instrumentation_key: ${APP_INSIGHTS_KEY}
    sampling_rate: 100%
```

#### 3.4.2 경보 규칙
| 경보 이름 | 조건 | 임계값 | 심각도 | 조치 |
|-----------|------|---------|--------|------|
| High Message Count | ActiveMessageCount | > 10,000 | Warning | 워커 스케일 아웃 |
| Dead Letter Growth | DeadLetterMessageCount | > 100 | Critical | 즉시 조사 |
| Throttling | ThrottledRequests | > 10/분 | High | 메시징 유닛 증가 |
| Server Errors | ServerErrors | > 1% | Critical | 운영팀 호출 |
| User Errors | UserErrors | > 5% | Warning | 로그 분석 |
| Latency | EndToEndLatency | > 100ms | Warning | 성능 튜닝 |
| Availability | Availability | < 99.9% | Critical | 페일오버 고려 |

### 3.5 성능 최적화

#### 3.5.1 파티셔닝 전략
```yaml
partitioning:
  strategy: Message-based
  
  partition_keys:
    ai_generation: tripId  # 여행별 순서 보장
    location_search: destination  # 목적지별 분산
    notification: userId  # 사용자별 순서 보장
    
  benefits:
    - 처리량 4-16배 증가
    - 병렬 처리 향상
    - 장애 격리
```

#### 3.5.2 배치 처리
```yaml
batch_configuration:
  send_batch:
    max_size: 256KB
    max_messages: 100
    timeout: 100ms
    
  receive_batch:
    prefetch_count: 20
    max_batch_size: 10
    max_wait_time: 50ms
```

## 4. 설치 절차

### 4.1 사전 준비사항
- [ ] Azure 구독 및 권한 확인 (Contributor 이상)
- [ ] 네트워크 구성 완료 (VNet, Subnet, NSG)
- [ ] Private DNS Zone 구성
- [ ] Key Vault 설정 및 암호화 키 생성
- [ ] Managed Identity 생성
- [ ] 모니터링 인프라 준비 (Log Analytics, Application Insights)

### 4.2 설치 스크립트

#### 4.2.1 Infrastructure as Code (Terraform)
```hcl
# main.tf
terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
}

provider "azurerm" {
  features {}
}

# Service Bus Namespace
resource "azurerm_servicebus_namespace" "main" {
  name                = "sb-tripgen-prod"
  location            = "koreacentral"
  resource_group_name = "tripgen-prod-rg"
  sku                 = "Premium"
  capacity            = 4
  zone_redundant      = true
  
  identity {
    type = "SystemAssigned"
  }
  
  customer_managed_key {
    key_vault_key_id = azurerm_key_vault_key.servicebus.id
    identity_id      = azurerm_user_assigned_identity.servicebus.id
  }
  
  network_rule_set {
    default_action = "Deny"
    
    network_rules {
      subnet_id = data.azurerm_subnet.aks.id
      ignore_missing_vnet_service_endpoint = false
    }
  }
  
  tags = {
    Environment = "Production"
    Service     = "TripGen"
    Component   = "MessageQueue"
  }
}

# Queues
locals {
  queues = {
    "ai-schedule-generation" = {
      max_size_in_megabytes = 81920
      enable_partitioning   = true
      enable_session        = true
      duplicate_detection_history_time_window = "PT10M"
    }
    "location-search" = {
      max_size_in_megabytes = 81920
      enable_partitioning   = true
      enable_session        = false
    }
    "route-calculation" = {
      max_size_in_megabytes = 81920
      enable_partitioning   = true
      enable_session        = false
    }
    "notification" = {
      max_size_in_megabytes = 81920
      enable_partitioning   = true
      enable_session        = false
      duplicate_detection_history_time_window = "PT5M"
    }
  }
}

resource "azurerm_servicebus_queue" "queues" {
  for_each = local.queues
  
  name                = each.key
  namespace_id        = azurerm_servicebus_namespace.main.id
  max_size_in_megabytes = each.value.max_size_in_megabytes
  enable_partitioning = each.value.enable_partitioning
  enable_session      = lookup(each.value, "enable_session", false)
  
  duplicate_detection_history_time_window = lookup(
    each.value, 
    "duplicate_detection_history_time_window", 
    null
  )
  
  lock_duration        = "PT1M"
  max_delivery_count   = 3
  dead_lettering_on_message_expiration = true
}

# Topics
resource "azurerm_servicebus_topic" "trip_events" {
  name                = "trip-events"
  namespace_id        = azurerm_servicebus_namespace.main.id
  enable_partitioning = true
  max_size_in_megabytes = 81920
  
  duplicate_detection_history_time_window = "PT30M"
}

# Subscriptions
resource "azurerm_servicebus_subscription" "audit" {
  name               = "audit-subscription"
  topic_id           = azurerm_servicebus_topic.trip_events.id
  max_delivery_count = 10
  
  dead_lettering_on_message_expiration = true
  dead_lettering_on_filter_evaluation_error = true
}

# Private Endpoint
resource "azurerm_private_endpoint" "servicebus" {
  name                = "pe-servicebus-tripgen-prod"
  location            = "koreacentral"
  resource_group_name = "tripgen-prod-rg"
  subnet_id           = data.azurerm_subnet.private_endpoint.id
  
  private_service_connection {
    name                           = "servicebus-connection"
    private_connection_resource_id = azurerm_servicebus_namespace.main.id
    subresource_names              = ["namespace"]
    is_manual_connection           = false
  }
  
  private_dns_zone_group {
    name                 = "servicebus-dns-group"
    private_dns_zone_ids = [data.azurerm_private_dns_zone.servicebus.id]
  }
}
```

#### 4.2.2 PowerShell 설치 스크립트
```powershell
# install-servicebus-prod.ps1
param(
    [Parameter(Mandatory=$true)]
    [string]$ResourceGroup = "tripgen-prod-rg",
    
    [Parameter(Mandatory=$true)]
    [string]$Location = "koreacentral"
)

# 변수 설정
$Namespace = "sb-tripgen-prod"
$KeyVaultName = "kv-tripgen-prod"

# Service Bus Namespace 생성
Write-Host "Creating Service Bus Namespace..." -ForegroundColor Green
az servicebus namespace create `
    --name $Namespace `
    --resource-group $ResourceGroup `
    --location $Location `
    --sku Premium `
    --capacity 4 `
    --zone-redundant true `
    --mi-system-assigned

# 큐 생성
$queues = @(
    @{Name="ai-schedule-generation"; MaxSize="80GB"; Partitions=4; Session=$true; Duplicate="PT10M"},
    @{Name="ai-schedule-regeneration"; MaxSize="80GB"; Partitions=2; Session=$true; Duplicate="PT10M"},
    @{Name="location-search"; MaxSize="80GB"; Partitions=4; Session=$false; Duplicate=$null},
    @{Name="route-calculation"; MaxSize="80GB"; Partitions=4; Session=$false; Duplicate=$null},
    @{Name="notification"; MaxSize="80GB"; Partitions=8; Session=$false; Duplicate="PT5M"}
)

foreach ($queue in $queues) {
    Write-Host "Creating queue: $($queue.Name)" -ForegroundColor Yellow
    
    $cmd = "az servicebus queue create " +
           "--name $($queue.Name) " +
           "--namespace-name $Namespace " +
           "--resource-group $ResourceGroup " +
           "--max-size $($queue.MaxSize) " +
           "--enable-partitioning true " +
           "--lock-duration PT1M " +
           "--max-delivery-count 3 " +
           "--dead-lettering-on-message-expiration true"
    
    if ($queue.Session) {
        $cmd += " --enable-session true"
    }
    
    if ($queue.Duplicate) {
        $cmd += " --duplicate-detection-history-time-window $($queue.Duplicate)"
    }
    
    Invoke-Expression $cmd
}

# 토픽 생성
Write-Host "Creating topics..." -ForegroundColor Green
az servicebus topic create `
    --name "trip-events" `
    --namespace-name $Namespace `
    --resource-group $ResourceGroup `
    --max-size 80GB `
    --enable-partitioning true `
    --duplicate-detection-history-time-window PT30M

# 구독 생성
$subscriptions = @(
    @{Topic="trip-events"; Name="audit-subscription"; MaxDelivery=10},
    @{Topic="trip-events"; Name="analytics-subscription"; MaxDelivery=5},
    @{Topic="trip-events"; Name="monitoring-subscription"; MaxDelivery=3}
)

foreach ($sub in $subscriptions) {
    Write-Host "Creating subscription: $($sub.Name)" -ForegroundColor Yellow
    
    az servicebus topic subscription create `
        --name $sub.Name `
        --topic-name $sub.Topic `
        --namespace-name $Namespace `
        --resource-group $ResourceGroup `
        --max-delivery-count $sub.MaxDelivery `
        --dead-lettering-on-message-expiration true
}

Write-Host "Service Bus installation completed!" -ForegroundColor Green
```

### 4.3 검증 절차

#### 4.3.1 구성 검증
```bash
# Namespace 상태 확인
az servicebus namespace show \
    --name sb-tripgen-prod \
    --resource-group tripgen-prod-rg \
    --query "{Status:status, ZoneRedundant:zoneRedundant, Capacity:sku.capacity}"

# 큐 목록 및 상태 확인
az servicebus queue list \
    --namespace-name sb-tripgen-prod \
    --resource-group tripgen-prod-rg \
    --output table

# Private Endpoint 연결 상태
az network private-endpoint show \
    --name pe-servicebus-tripgen-prod \
    --resource-group tripgen-prod-rg \
    --query "privateLinkServiceConnections[0].privateLinkServiceConnectionState"
```

#### 4.3.2 성능 테스트
```python
# performance_test.py
import asyncio
from azure.servicebus.aio import ServiceBusClient
from azure.identity.aio import DefaultAzureCredential
import time
import statistics

async def performance_test():
    credential = DefaultAzureCredential()
    namespace = "sb-tripgen-prod.servicebus.windows.net"
    
    async with ServiceBusClient(
        fully_qualified_namespace=namespace,
        credential=credential
    ) as client:
        
        # 송신 성능 테스트
        sender = client.get_queue_sender("ai-schedule-generation")
        send_times = []
        
        for i in range(100):
            start = time.time()
            await sender.send_messages(
                ServiceBusMessage(f"Test message {i}")
            )
            send_times.append((time.time() - start) * 1000)
        
        print(f"Send Performance:")
        print(f"  Average: {statistics.mean(send_times):.2f}ms")
        print(f"  P95: {statistics.quantiles(send_times, n=20)[18]:.2f}ms")
        print(f"  P99: {statistics.quantiles(send_times, n=100)[98]:.2f}ms")
        
        # 수신 성능 테스트
        receiver = client.get_queue_receiver("ai-schedule-generation")
        receive_times = []
        
        async for message in receiver:
            start = time.time()
            await receiver.complete_message(message)
            receive_times.append((time.time() - start) * 1000)
            
            if len(receive_times) >= 100:
                break
        
        print(f"Receive Performance:")
        print(f"  Average: {statistics.mean(receive_times):.2f}ms")
        print(f"  P95: {statistics.quantiles(receive_times, n=20)[18]:.2f}ms")

asyncio.run(performance_test())
```

#### 4.3.3 고가용성 테스트
```bash
# Zone 장애 시뮬레이션
# Azure Portal에서 수동으로 Zone 장애 테스트 수행

# 장애 복구 시간 측정
start_time=$(date +%s)
while ! az servicebus namespace show \
    --name sb-tripgen-prod \
    --resource-group tripgen-prod-rg \
    --query "status" -o tsv | grep -q "Active"; do
    sleep 5
done
end_time=$(date +%s)
echo "Recovery Time: $((end_time - start_time)) seconds"
```

## 5. 운영 가이드

### 5.1 일상 운영

#### 5.1.1 모니터링 대시보드
- **Azure Monitor Dashboard**: 실시간 메트릭 및 경보
- **Application Insights**: 애플리케이션 레벨 모니터링
- **Log Analytics**: 로그 분석 및 쿼리
- **Grafana**: 커스텀 시각화 및 경보

#### 5.1.2 운영 체크리스트
**일일 점검 (자동화)**
- [ ] 큐/토픽 상태 확인
- [ ] Dead Letter Queue 모니터링
- [ ] 처리 지연 시간 확인
- [ ] 에러율 모니터링

**주간 점검**
- [ ] 용량 사용률 검토
- [ ] 성능 트렌드 분석
- [ ] 보안 로그 검토
- [ ] 비용 최적화 기회 식별

**월간 점검**
- [ ] DR 테스트 수행
- [ ] 용량 계획 검토
- [ ] 보안 감사
- [ ] SLA 준수 보고서

### 5.2 장애 대응

#### 5.2.1 장애 시나리오별 대응
| 시나리오 | 증상 | 대응 절차 | RTO |
|----------|------|-----------|-----|
| Zone 장애 | 일부 파티션 접근 불가 | 자동 페일오버 | < 60초 |
| 리전 장애 | 전체 서비스 중단 | DR 리전으로 수동 페일오버 | < 15분 |
| 네트워크 분할 | 연결 오류 | Private Endpoint 재구성 | < 10분 |
| 용량 초과 | 429 오류 | 메시징 유닛 증가 | < 5분 |
| 인증 실패 | 401/403 오류 | Managed Identity 권한 확인 | < 10분 |

#### 5.2.2 복구 절차
```powershell
# DR 페일오버 스크립트
# failover-to-dr.ps1

$PrimaryNamespace = "sb-tripgen-prod"
$DRNamespace = "sb-tripgen-prod-dr"
$Alias = "sb-tripgen-prod-alias"

# 1. 현재 상태 확인
$status = az servicebus georecovery-alias show `
    --alias $Alias `
    --namespace-name $PrimaryNamespace `
    --resource-group tripgen-prod-rg `
    --query "provisioningState" -o tsv

if ($status -eq "Succeeded") {
    # 2. 페일오버 시작
    Write-Host "Initiating failover to DR region..." -ForegroundColor Yellow
    
    az servicebus georecovery-alias fail-over `
        --alias $Alias `
        --namespace-name $DRNamespace `
        --resource-group tripgen-prod-dr-rg
    
    # 3. 애플리케이션 연결 문자열 업데이트
    Update-ApplicationConfiguration -Namespace $DRNamespace
    
    # 4. 모니터링 알림
    Send-Alert -Message "Failover completed to DR region"
    
    Write-Host "Failover completed successfully!" -ForegroundColor Green
}
```

### 5.3 백업 및 복구

#### 5.3.1 백업 전략
- **구성 백업**: Terraform 상태 파일, ARM 템플릿
- **메시지 백업**: Dead Letter Queue 정기 아카이빙
- **메타데이터**: 큐/토픽 설정 자동 백업

#### 5.3.2 복구 절차
```bash
# 구성 복구 (Terraform)
terraform plan -out=recovery.tfplan
terraform apply recovery.tfplan

# 메시지 복구
python scripts/restore_messages.py \
    --source storage-account \
    --destination sb-tripgen-prod \
    --queue ai-schedule-generation
```

## 6. 확장 및 최적화

### 6.1 자동 확장

#### 6.1.1 메시징 유닛 자동 확장
```yaml
autoscaling:
  enabled: true
  min_units: 4
  max_units: 16
  
  rules:
    - metric: CPU Percentage
      threshold: 70%
      scale_out: +2 units
      scale_in: -1 unit
      cooldown: 5 minutes
      
    - metric: Active Messages
      threshold: 100,000
      scale_out: +4 units
      scale_in: -2 units
      cooldown: 10 minutes
```

#### 6.1.2 애플리케이션 확장
```yaml
hpa_configuration:
  min_replicas: 2
  max_replicas: 20
  
  metrics:
    - type: External
      name: servicebus_queue_length
      target: 100 messages per replica
      
    - type: Resource
      name: cpu
      target: 70%
```

### 6.2 성능 튜닝

#### 6.2.1 클라이언트 최적화
```python
# 연결 풀 구성
connection_pool_config = {
    "max_pool_size": 100,
    "min_pool_size": 10,
    "keep_alive_interval": 30,
    "retry_total": 3,
    "retry_backoff_factor": 2
}

# 배치 처리 구성
batch_config = {
    "send_batch_size": 100,
    "receive_batch_size": 20,
    "prefetch_count": 50,
    "auto_complete": False
}
```

## 7. 비용 분석

### 7.1 월간 예상 비용

| 항목 | 사양 | 단가 | 월 비용 |
|------|------|------|---------|
| Service Bus Premium | 4 Messaging Units | $650/unit | $2,600 |
| Private Endpoint | 1개 | $10/endpoint | $10 |
| 데이터 전송 (송신) | 100GB | $0.087/GB | $8.70 |
| 데이터 전송 (Zone 간) | 50GB | $0.01/GB | $0.50 |
| Geo-DR | Secondary namespace | 50% of primary | $1,300 |
| **기본 비용** | | | **$3,919.20** |
| | | | |
| **확장 시 (8 units)** | 8 Messaging Units | $650/unit | $5,200 |
| **최대 비용 (16 units)** | 16 Messaging Units | $650/unit | $10,400 |

### 7.2 비용 최적화

#### 7.2.1 최적화 전략
- **Reserved Capacity**: 1년 약정 시 20% 할인
- **자동 확장**: 사용량 기반 동적 확장
- **메시지 압축**: 데이터 전송 비용 절감
- **효율적인 파티셔닝**: 불필요한 파티션 제거

#### 7.2.2 비용 모니터링
```bash
# 월간 비용 추적
az consumption usage list \
    --start-date 2025-01-01 \
    --end-date 2025-01-31 \
    --query "[?contains(instanceId, 'servicebus')].{Service:instanceId, Cost:pretaxCost}" \
    --output table
```

## 8. 마이그레이션 전략

### 8.1 개발환경에서 운영환경 전환

#### 8.1.1 전환 체크리스트
- [ ] Premium Tier로 업그레이드
- [ ] Zone Redundancy 활성화
- [ ] Private Endpoint 구성
- [ ] Managed Identity 설정
- [ ] 파티셔닝 활성화
- [ ] 토픽/구독 패턴 도입
- [ ] Geo-DR 구성
- [ ] 모니터링 강화

#### 8.1.2 무중단 마이그레이션
```bash
# 1단계: 병렬 운영
- 신규 Premium namespace 생성
- 듀얼 라이팅 구현
- 점진적 트래픽 이동

# 2단계: 검증
- 성능 테스트
- 장애 복구 테스트
- 모니터링 검증

# 3단계: 전환
- DNS 업데이트
- 구 시스템 폐기
```

## 9. 규정 준수 및 감사

### 9.1 보안 규정 준수
- **데이터 암호화**: 저장 시 및 전송 시 암호화
- **접근 제어**: RBAC 및 Managed Identity
- **감사 로깅**: 모든 관리 작업 기록
- **데이터 주권**: 한국 리전 내 데이터 보관

### 9.2 감사 로그
```kusto
// KQL 쿼리 - 관리 작업 감사
AzureActivity
| where ResourceProvider == "Microsoft.ServiceBus"
| where OperationNameValue contains "write" or OperationNameValue contains "delete"
| project TimeGenerated, Caller, OperationNameValue, ResourceGroup, ActivityStatus
| order by TimeGenerated desc
```

## 10. 체크리스트

### 10.1 설치 전 체크리스트
- [ ] Azure 구독 엔터프라이즈 계약 확인
- [ ] 네트워크 아키텍처 승인
- [ ] 보안 검토 완료
- [ ] DR 계획 승인
- [ ] 비용 승인 (월 $4,000~$10,000)
- [ ] 변경 관리 프로세스 준수

### 10.2 설치 후 체크리스트
- [ ] Premium Namespace 생성 완료
- [ ] 모든 큐/토픽 생성 완료
- [ ] Private Endpoint 구성 완료
- [ ] Managed Identity 설정 완료
- [ ] Geo-DR 구성 완료
- [ ] 모니터링/경보 설정 완료
- [ ] 성능 테스트 통과
- [ ] 보안 스캔 통과
- [ ] 운영 문서 업데이트
- [ ] 운영팀 교육 완료

## 11. 승인 및 이력

### 11.1 문서 승인
- 작성자: 한데브옵스(클라우더) / DevOps Engineer
- 검토자: 김개발(테키) / Tech Lead
- 보안 검토: 박인공(아이봇) / AI Engineer
- 승인자: 이여행(트래블) / Product Owner
- 작성일: 2025-08-05

### 11.2 변경 이력
| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| 1.0 | 2025-08-05 | 최초 작성 | 한데브옵스 |

## 부록 A: 참고 자료

### Azure Service Bus 문서
- [Service Bus Premium 가이드](https://docs.microsoft.com/azure/service-bus-messaging/)
- [Private Endpoint 구성](https://docs.microsoft.com/azure/service-bus-messaging/private-link-service)
- [Geo-DR 설정](https://docs.microsoft.com/azure/service-bus-messaging/service-bus-geo-dr)
- [성능 모범 사례](https://docs.microsoft.com/azure/service-bus-messaging/service-bus-performance-improvements)

### 도구 및 SDK
- [Azure CLI](https://docs.microsoft.com/cli/azure/servicebus)
- [Terraform Provider](https://registry.terraform.io/providers/hashicorp/azurerm/latest/docs)
- [Python SDK](https://github.com/Azure/azure-sdk-for-python/tree/main/sdk/servicebus)
- [Service Bus Explorer](https://github.com/paolosalvatori/ServiceBusExplorer)