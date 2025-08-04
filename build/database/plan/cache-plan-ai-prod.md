# AI Service 캐시 운영환경 설치 가이드

## 개요

AI Service의 운영환경에서 사용할 Azure Cache for Redis Premium 설치 및 구성 가이드입니다.
작업 상태, 생성 일정, 추천 결과를 고성능으로 캐싱하고 Pub/Sub 메시징을 지원하는 클러스터 환경을 구축합니다.

### 주요 특징
- **Azure Cache for Redis Premium** (6샤드 클러스터)
- **Zone Redundant** 고가용성 구성
- **Pub/Sub 메시징** 지원
- **Private Endpoint** 보안 연결
- **Key Vault** 연결 문자열 관리
- **실시간 알림** 시스템 지원

## Azure 리소스 구성

### 1. Redis Cache Premium 구성
```
- SKU: Premium P4 (104GB)
- 샤드 개수: 6개
- 가용성: Zone Redundant
- 지역: Korea Central
- 네트워크: Private Endpoint
- Pub/Sub: 활성화
```

### 2. 캐시 전략
- **작업 상태**: AI 작업 진행 상태 (TTL: 1시간)
- **생성 일정**: 완성된 여행 일정 (TTL: 8시간)
- **추천 결과**: AI 추천 결과 (TTL: 2시간)
- **Pub/Sub**: 실시간 작업 상태 알림

## Terraform 배포 스크립트

### main.tf
```hcl
# Resource Group
resource "azurerm_resource_group" "ai_cache_rg" {
  name     = "rg-tripgen-ai-cache-prod"
  location = "Korea Central"

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "Cache"
  }
}

# Azure Cache for Redis Premium with Pub/Sub
resource "azurerm_redis_cache" "ai_cache" {
  name                = "redis-tripgen-ai-prod"
  location            = azurerm_resource_group.ai_cache_rg.location
  resource_group_name = azurerm_resource_group.ai_cache_rg.name
  
  capacity            = 1
  family              = "P"
  sku_name           = "Premium"
  enable_non_ssl_port = false
  minimum_tls_version = "1.2"
  
  # 대용량 클러스터 구성
  shard_count = 6
  
  # Zone Redundancy
  zones = ["1", "2", "3"]
  
  # Redis 구성 - Pub/Sub 최적화
  redis_configuration {
    enable_authentication           = true
    maxmemory_reserved             = 8192
    maxmemory_delta                = 8192
    maxmemory_policy               = "allkeys-lru"
    notify_keyspace_events         = "AKE"  # All keyspace events
    rdb_backup_enabled             = true
    rdb_backup_frequency           = 360    # 6시간마다
    rdb_backup_max_snapshot_count  = 3
    rdb_storage_connection_string  = azurerm_storage_account.backup_storage.primary_blob_connection_string
  }

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "Cache"
  }
}

# 백업용 Storage Account
resource "azurerm_storage_account" "backup_storage" {
  name                     = "staicacheprodbackup"
  resource_group_name      = azurerm_resource_group.ai_cache_rg.name
  location                 = azurerm_resource_group.ai_cache_rg.location
  account_tier             = "Premium"
  account_replication_type = "ZRS"
  account_kind             = "BlockBlobStorage"
  
  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "CacheBackup"
  }
}

# Virtual Network
resource "azurerm_virtual_network" "ai_cache_vnet" {
  name                = "vnet-tripgen-ai-cache-prod"
  address_space       = ["10.22.0.0/16"]
  location            = azurerm_resource_group.ai_cache_rg.location
  resource_group_name = azurerm_resource_group.ai_cache_rg.name

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "Network"
  }
}

# Subnet for Cache
resource "azurerm_subnet" "cache_subnet" {
  name                 = "snet-cache"
  resource_group_name  = azurerm_resource_group.ai_cache_rg.name
  virtual_network_name = azurerm_virtual_network.ai_cache_vnet.name
  address_prefixes     = ["10.22.1.0/24"]
}

# Subnet for Private Endpoints
resource "azurerm_subnet" "private_endpoint_subnet" {
  name                 = "snet-private-endpoints"
  resource_group_name  = azurerm_resource_group.ai_cache_rg.name
  virtual_network_name = azurerm_virtual_network.ai_cache_vnet.name
  address_prefixes     = ["10.22.2.0/24"]
  
  private_endpoint_network_policies_enabled = false
}
```

### private_endpoint.tf
```hcl
# Private DNS Zone
resource "azurerm_private_dns_zone" "redis_dns_zone" {
  name                = "privatelink.redis.cache.windows.net"
  resource_group_name = azurerm_resource_group.ai_cache_rg.name

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "DNS"
  }
}

# Private DNS Zone Virtual Network Link
resource "azurerm_private_dns_zone_virtual_network_link" "redis_dns_link" {
  name                  = "dns-link-ai-cache"
  resource_group_name   = azurerm_resource_group.ai_cache_rg.name
  private_dns_zone_name = azurerm_private_dns_zone.redis_dns_zone.name
  virtual_network_id    = azurerm_virtual_network.ai_cache_vnet.id
  registration_enabled  = false

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "DNS"
  }
}

# Private Endpoint
resource "azurerm_private_endpoint" "redis_private_endpoint" {
  name                = "pe-redis-ai-prod"
  location            = azurerm_resource_group.ai_cache_rg.location
  resource_group_name = azurerm_resource_group.ai_cache_rg.name
  subnet_id           = azurerm_subnet.private_endpoint_subnet.id

  private_service_connection {
    name                           = "psc-redis-ai-prod"
    private_connection_resource_id = azurerm_redis_cache.ai_cache.id
    subresource_names              = ["redisCache"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "dns-zone-group"
    private_dns_zone_ids = [azurerm_private_dns_zone.redis_dns_zone.id]
  }

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "Network"
  }
}
```

### key_vault.tf
```hcl
# Key Vault for Connection Strings
resource "azurerm_key_vault" "ai_cache_kv" {
  name                = "kv-tripgen-ai-cache-prod"
  location            = azurerm_resource_group.ai_cache_rg.location
  resource_group_name = azurerm_resource_group.ai_cache_rg.name
  tenant_id           = data.azurerm_client_config.current.tenant_id
  sku_name            = "premium"

  purge_protection_enabled        = true
  soft_delete_retention_days      = 7
  enabled_for_disk_encryption     = true
  enabled_for_deployment          = true
  enabled_for_template_deployment = true

  access_policy {
    tenant_id = data.azurerm_client_config.current.tenant_id
    object_id = data.azurerm_client_config.current.object_id

    secret_permissions = [
      "Get", "List", "Set", "Delete", "Backup", "Restore", "Recover", "Purge"
    ]
  }

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "Security"
  }
}

# Store Redis Connection String
resource "azurerm_key_vault_secret" "redis_connection_string" {
  name         = "redis-ai-connection-string"
  value        = azurerm_redis_cache.ai_cache.primary_connection_string
  key_vault_id = azurerm_key_vault.ai_cache_kv.id

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "Cache"
  }
}

# Store Redis Primary Key
resource "azurerm_key_vault_secret" "redis_primary_key" {
  name         = "redis-ai-primary-key"
  value        = azurerm_redis_cache.ai_cache.primary_access_key
  key_vault_id = azurerm_key_vault.ai_cache_kv.id

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "Cache"
  }
}

# Store Pub/Sub Configuration
resource "azurerm_key_vault_secret" "pubsub_config" {
  name         = "redis-ai-pubsub-config"
  value = jsonencode({
    channels = {
      job_status      = "ai:job:status:*"
      job_progress    = "ai:job:progress:*"
      job_complete    = "ai:job:complete:*"
      job_error       = "ai:job:error:*"
      recommendations = "ai:recommendations:*"
    }
  })
  key_vault_id = azurerm_key_vault.ai_cache_kv.id

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "PubSub"
  }
}

data "azurerm_client_config" "current" {}
```

### monitoring.tf
```hcl
# Log Analytics Workspace
resource "azurerm_log_analytics_workspace" "ai_cache_logs" {
  name                = "log-tripgen-ai-cache-prod"
  location            = azurerm_resource_group.ai_cache_rg.location
  resource_group_name = azurerm_resource_group.ai_cache_rg.name
  sku                 = "PerGB2018"
  retention_in_days   = 30

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "Monitoring"
  }
}

# Diagnostic Settings
resource "azurerm_monitor_diagnostic_setting" "redis_diagnostics" {
  name                       = "redis-ai-diagnostics"
  target_resource_id         = azurerm_redis_cache.ai_cache.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.ai_cache_logs.id

  enabled_log {
    category = "ConnectedClientList"
  }

  metric {
    category = "AllMetrics"
    enabled  = true
  }
}

# Action Group for Alerts
resource "azurerm_monitor_action_group" "ai_cache_alerts" {
  name                = "ag-ai-cache-prod"
  resource_group_name = azurerm_resource_group.ai_cache_rg.name
  short_name          = "aicache"

  email_receiver {
    name          = "admin"
    email_address = "admin@tripgen.com"
  }

  email_receiver {
    name          = "ai-team"
    email_address = "ai-team@tripgen.com"
  }

  webhook_receiver {
    name        = "slack-webhook"
    service_uri = "https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK"
  }

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "Monitoring"
  }
}

# CPU Utilization Alert
resource "azurerm_monitor_metric_alert" "cpu_alert" {
  name                = "redis-ai-cpu-alert"
  resource_group_name = azurerm_resource_group.ai_cache_rg.name
  scopes              = [azurerm_redis_cache.ai_cache.id]
  description         = "AI Cache CPU utilization is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.Cache/Redis"
    metric_name      = "percentProcessorTime"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 70
  }

  action {
    action_group_id = azurerm_monitor_action_group.ai_cache_alerts.id
  }

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "Monitoring"
  }
}

# Memory Usage Alert
resource "azurerm_monitor_metric_alert" "memory_alert" {
  name                = "redis-ai-memory-alert"
  resource_group_name = azurerm_resource_group.ai_cache_rg.name
  scopes              = [azurerm_redis_cache.ai_cache.id]
  description         = "AI Cache memory usage is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.Cache/Redis"
    metric_name      = "usedmemorypercentage"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 85
  }

  action {
    action_group_id = azurerm_monitor_action_group.ai_cache_alerts.id
  }

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "Monitoring"
  }
}

# Pub/Sub Connection Alert
resource "azurerm_monitor_metric_alert" "pubsub_connection_alert" {
  name                = "redis-ai-pubsub-connection-alert"
  resource_group_name = azurerm_resource_group.ai_cache_rg.name
  scopes              = [azurerm_redis_cache.ai_cache.id]
  description         = "AI Cache Pub/Sub connections are high"
  severity            = 3

  criteria {
    metric_namespace = "Microsoft.Cache/Redis"
    metric_name      = "connectedclients"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 2000
  }

  action {
    action_group_id = azurerm_monitor_action_group.ai_cache_alerts.id
  }

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "Monitoring"
  }
}

# Operations per Second Alert
resource "azurerm_monitor_metric_alert" "ops_per_second_alert" {
  name                = "redis-ai-ops-per-second-alert"
  resource_group_name = azurerm_resource_group.ai_cache_rg.name
  scopes              = [azurerm_redis_cache.ai_cache.id]
  description         = "AI Cache operations per second is high"
  severity            = 3

  criteria {
    metric_namespace = "Microsoft.Cache/Redis"
    metric_name      = "operationsPerSecond"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 50000
  }

  action {
    action_group_id = azurerm_monitor_action_group.ai_cache_alerts.id
  }

  tags = {
    Environment = "Production"
    Service     = "AIService"
    Component   = "Monitoring"
  }
}
```

### outputs.tf
```hcl
output "redis_hostname" {
  description = "Redis Cache hostname"
  value       = azurerm_redis_cache.ai_cache.hostname
  sensitive   = false
}

output "redis_port" {
  description = "Redis Cache port"
  value       = azurerm_redis_cache.ai_cache.ssl_port
  sensitive   = false
}

output "redis_primary_key" {
  description = "Redis Cache primary key"
  value       = azurerm_redis_cache.ai_cache.primary_access_key
  sensitive   = true
}

output "key_vault_uri" {
  description = "Key Vault URI"
  value       = azurerm_key_vault.ai_cache_kv.vault_uri
  sensitive   = false
}

output "private_endpoint_ip" {
  description = "Private endpoint IP address"
  value       = azurerm_private_endpoint.redis_private_endpoint.private_service_connection[0].private_ip_address
  sensitive   = false
}

output "pubsub_channels" {
  description = "Pub/Sub channel configuration"
  value = {
    job_status      = "ai:job:status:*"
    job_progress    = "ai:job:progress:*"
    job_complete    = "ai:job:complete:*"
    job_error       = "ai:job:error:*"
    recommendations = "ai:recommendations:*"
  }
  sensitive = false
}
```

## 보안 설정

### 1. Private Endpoint 구성
- VNet 내부에서만 액세스 가능
- Pub/Sub 채널 보안 강화
- 퍼블릭 액세스 완전 차단

### 2. Key Vault 연동
- 연결 문자열 및 키 안전 저장
- Pub/Sub 채널 구성 정보 보호
- 액세스 키 자동 로테이션

### 3. 채널 보안
- 채널별 액세스 권한 관리
- 메시지 암호화 설정
- 패턴 기반 채널 구독 제한

## 캐시 샤딩 및 클러스터링 설정

### 1. 샤드 구성
```yaml
샤드 분산 전략:
  - 샤드 0-1: AI 작업 상태 및 메타데이터
  - 샤드 2-3: 생성된 여행 일정 데이터
  - 샤드 4-5: 추천 결과 및 Pub/Sub 채널
```

### 2. 키 분산 패턴
```
작업 상태: ai:job:status:{job_id}
작업 진행률: ai:job:progress:{job_id}
생성 일정: ai:schedule:{trip_id}
추천 결과: ai:recommendation:{user_id}:{type}
```

### 3. Pub/Sub 채널 구성
```
작업 상태 알림: ai:job:status:{job_id}
작업 진행률: ai:job:progress:{job_id}
작업 완료: ai:job:complete:{job_id}
작업 오류: ai:job:error:{job_id}
추천 업데이트: ai:recommendations:{user_id}
```

## Pub/Sub 설정

### 1. 채널 구성
```yaml
채널 패턴:
  - 작업 상태: "ai:job:status:*"
  - 작업 진행률: "ai:job:progress:*"
  - 작업 완료: "ai:job:complete:*"
  - 작업 오류: "ai:job:error:*"
  - 추천 결과: "ai:recommendations:*"
```

### 2. 메시지 포맷
```json
{
  "eventType": "job_status_update",
  "jobId": "job_123456",
  "userId": "user_789",
  "status": "processing",
  "progress": 45,
  "timestamp": "2024-03-15T10:30:00Z",
  "data": {
    "step": "generating_itinerary",
    "estimatedTime": 300
  }
}
```

### 3. 구독자 관리
```java
@Component
public class AIJobStatusSubscriber {
    
    @EventListener
    public void handleJobStatusUpdate(RedisMessage message) {
        String channel = message.getChannel();
        String payload = message.getBody();
        
        if (channel.startsWith("ai:job:status:")) {
            // 작업 상태 업데이트 처리
            processJobStatusUpdate(payload);
        }
    }
}
```

## 모니터링 및 알림 설정

### 1. 핵심 메트릭
- **CPU 사용률**: 임계값 70%
- **메모리 사용률**: 임계값 85%
- **연결 수**: 임계값 2000개 (Pub/Sub 포함)
- **초당 작업 수**: 임계값 50,000ops

### 2. Pub/Sub 전용 메트릭
- **채널별 구독자 수**: 실시간 모니터링
- **메시지 발행 속도**: 초당 메시지 수
- **메시지 처리 지연**: 평균 지연 시간

### 3. 비즈니스 메트릭
- **AI 작업 완료율**: 95% 이상
- **작업 처리 시간**: 평균 5분 이하
- **추천 캐시 히트율**: 80% 이상

## 성능 최적화

### 1. 메모리 정책
```redis
maxmemory-policy: allkeys-lru
maxmemory-reserved: 8192MB
maxmemory-delta: 8192MB
notify-keyspace-events: AKE
```

### 2. TTL 최적화
```yaml
작업 상태 TTL: 1시간 (3600초)
생성 일정 TTL: 8시간 (28800초)
추천 결과 TTL: 2시간 (7200초)
임시 데이터 TTL: 30분 (1800초)
```

### 3. 연결 풀링
```yaml
최대 연결 수: 2000 (Pub/Sub 포함)
연결 타임아웃: 10초
읽기 타임아웃: 5초
Pub/Sub 타임아웃: 30초
```

### 4. Pub/Sub 최적화
```yaml
채널 버퍼 크기: 1000
메시지 배치 크기: 100
재연결 간격: 5초
최대 재시도: 3회
```

## 비용 최적화

### 1. 인스턴스 최적화
- **P4 인스턴스**: 104GB 메모리, 높은 처리량
- **Zone Redundancy**: 고가용성 보장
- **예약 인스턴스**: 1년 예약으로 30% 비용 절감

### 2. 백업 최적화
- **백업 주기**: 6시간마다 (높은 빈도)
- **Premium Storage**: 고성능 백업
- **보존 기간**: 7일

### 3. 모니터링 비용
- **Log Analytics**: 30일 보존
- **메트릭 저장**: 90일
- **Slack 알림**: 중요 이벤트만

## 배포 가이드

### 1. 사전 준비
```bash
# Terraform 초기화
terraform init

# 변수 파일 생성
cp prod.tfvars.example prod.tfvars

# 계획 확인
terraform plan -var-file="prod.tfvars"
```

### 2. 배포 실행
```bash
# 배포
terraform apply -var-file="prod.tfvars"

# 출력 확인
terraform output
```

### 3. 배포 후 검증
```bash
# Redis 연결 테스트
redis-cli -h <hostname> -p 6380 --tls ping

# 클러스터 정보 확인
redis-cli -h <hostname> -p 6380 --tls cluster info

# Pub/Sub 테스트
redis-cli -h <hostname> -p 6380 --tls subscribe "ai:job:status:test"
```

## 애플리케이션 연동

### 1. 연결 문자열 구성
```yaml
spring:
  redis:
    cluster:
      nodes:
        - ${REDIS_HOSTNAME}:6380
    ssl: true
    timeout: 10000ms
    password: ${REDIS_PASSWORD}
    lettuce:
      pool:
        max-active: 500
        max-idle: 50
        min-idle: 10
```

### 2. Pub/Sub 구성 클래스
```java
@Configuration
@EnableRedisRepositories
public class AICacheConfig {
    
    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = 
            new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // 채널 패턴 구독
        container.addMessageListener(jobStatusListener(), 
            new PatternTopic("ai:job:status:*"));
        container.addMessageListener(jobProgressListener(), 
            new PatternTopic("ai:job:progress:*"));
        container.addMessageListener(jobCompleteListener(), 
            new PatternTopic("ai:job:complete:*"));
            
        return container;
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

### 3. AI 캐시 서비스 구현
```java
@Service
public class AICacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // 작업 상태 저장
    public void setJobStatus(String jobId, JobStatus status) {
        String key = "ai:job:status:" + jobId;
        redisTemplate.opsForValue().set(key, status, Duration.ofHours(1));
        
        // Pub/Sub 알림 발송
        redisTemplate.convertAndSend("ai:job:status:" + jobId, status);
    }
    
    // 생성된 일정 캐싱
    @Cacheable(value = "ai:schedule", key = "#tripId")
    public TripSchedule getGeneratedSchedule(String tripId) {
        return scheduleRepository.findById(tripId);
    }
    
    // 추천 결과 캐싱
    @Cacheable(value = "ai:recommendation", key = "#userId + ':' + #type")
    public List<Recommendation> getRecommendations(String userId, String type) {
        return recommendationService.generate(userId, type);
    }
}
```

## 운영 가이드

### 1. 일상 모니터링
- 메모리 사용률 및 CPU 사용률 확인
- Pub/Sub 채널별 구독자 수 모니터링
- AI 작업 처리 시간 및 완료율 확인

### 2. 정기 점검 (주간)
- 성능 메트릭 트렌드 분석
- 백업 상태 및 복구 테스트
- 보안 패치 및 업데이트 적용

### 3. 장애 대응
- 메모리 부족 시 즉시 스케일업
- Pub/Sub 연결 장애 시 재연결 로직 확인
- AI 작업 실패 시 에러 채널 모니터링

### 4. 용량 계획
- AI 작업량 증가에 따른 용량 계획
- Pub/Sub 메시지 처리량 증가 대비
- 피크 시간대 리소스 사용량 분석