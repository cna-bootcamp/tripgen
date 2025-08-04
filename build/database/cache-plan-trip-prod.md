# Trip Service 캐시 운영환경 설치 가이드

## 개요

Trip Service의 운영환경에서 사용할 Azure Cache for Redis Premium 설치 및 구성 가이드입니다.
여행 목록, 상세정보, 일정 데이터를 고성능으로 캐싱하기 위한 클러스터 환경을 구축합니다.

### 주요 특징
- **Azure Cache for Redis Premium** (6샤드 클러스터)
- **Zone Redundant** 고가용성 구성
- **Private Endpoint** 보안 연결
- **Key Vault** 연결 문자열 관리
- **지리적 복제** 및 **자동 백업**

## Azure 리소스 구성

### 1. Redis Cache Premium 구성
```
- SKU: Premium P3 (52GB)
- 샤드 개수: 6개
- 가용성: Zone Redundant
- 지역: Korea Central
- 네트워크: Private Endpoint
- 지리적 복제: 활성화
```

### 2. 캐시 전략
- **여행 목록**: 사용자별 여행 목록 (TTL: 2시간)
- **여행 상세**: 여행 상세 정보 (TTL: 6시간)
- **일정 데이터**: 여행 일정 정보 (TTL: 4시간)
- **검색 결과**: 여행 검색 결과 (TTL: 30분)

## Terraform 배포 스크립트

### main.tf
```hcl
# Resource Group
resource "azurerm_resource_group" "trip_cache_rg" {
  name     = "rg-tripgen-trip-cache-prod"
  location = "Korea Central"

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Cache"
  }
}

# Azure Cache for Redis Premium
resource "azurerm_redis_cache" "trip_cache" {
  name                = "redis-tripgen-trip-prod"
  location            = azurerm_resource_group.trip_cache_rg.location
  resource_group_name = azurerm_resource_group.trip_cache_rg.name
  
  capacity            = 1
  family              = "P"
  sku_name           = "Premium"
  enable_non_ssl_port = false
  minimum_tls_version = "1.2"
  
  # 클러스터 구성
  shard_count = 6
  
  # Zone Redundancy
  zones = ["1", "2", "3"]
  
  # Redis 구성
  redis_configuration {
    enable_authentication           = true
    maxmemory_reserved             = 4096
    maxmemory_delta                = 4096
    maxmemory_policy               = "allkeys-lru"
    notify_keyspace_events         = "Ex"
    rdb_backup_enabled             = true
    rdb_backup_frequency           = 720
    rdb_backup_max_snapshot_count  = 2
    rdb_storage_connection_string  = azurerm_storage_account.backup_storage.primary_blob_connection_string
  }

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Cache"
  }
}

# 지리적 복제를 위한 보조 Redis Cache
resource "azurerm_redis_cache" "trip_cache_secondary" {
  name                = "redis-tripgen-trip-prod-sec"
  location            = "Korea South"
  resource_group_name = azurerm_resource_group.trip_cache_rg.name
  
  capacity            = 1
  family              = "P"
  sku_name           = "Premium"
  enable_non_ssl_port = false
  minimum_tls_version = "1.2"
  
  # 클러스터 구성
  shard_count = 6
  
  # Zone Redundancy
  zones = ["1", "2", "3"]
  
  # Redis 구성
  redis_configuration {
    enable_authentication           = true
    maxmemory_reserved             = 4096
    maxmemory_delta                = 4096
    maxmemory_policy               = "allkeys-lru"
    notify_keyspace_events         = "Ex"
    rdb_backup_enabled             = false
  }

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Cache"
    Role        = "Secondary"
  }
}

# 지리적 복제 설정
resource "azurerm_redis_linked_server" "trip_cache_geo_replication" {
  target_redis_cache_name     = azurerm_redis_cache.trip_cache.name
  resource_group_name         = azurerm_resource_group.trip_cache_rg.name
  linked_redis_cache_id       = azurerm_redis_cache.trip_cache_secondary.id
  linked_redis_cache_location = azurerm_redis_cache.trip_cache_secondary.location
  server_role                 = "Secondary"
}

# 백업용 Storage Account
resource "azurerm_storage_account" "backup_storage" {
  name                     = "sttripcacheprodbackup"
  resource_group_name      = azurerm_resource_group.trip_cache_rg.name
  location                 = azurerm_resource_group.trip_cache_rg.location
  account_tier             = "Standard"
  account_replication_type = "ZRS"
  
  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "CacheBackup"
  }
}

# Virtual Network
resource "azurerm_virtual_network" "trip_cache_vnet" {
  name                = "vnet-tripgen-trip-cache-prod"
  address_space       = ["10.21.0.0/16"]
  location            = azurerm_resource_group.trip_cache_rg.location
  resource_group_name = azurerm_resource_group.trip_cache_rg.name

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Network"
  }
}

# Subnet for Cache
resource "azurerm_subnet" "cache_subnet" {
  name                 = "snet-cache"
  resource_group_name  = azurerm_resource_group.trip_cache_rg.name
  virtual_network_name = azurerm_virtual_network.trip_cache_vnet.name
  address_prefixes     = ["10.21.1.0/24"]
}

# Subnet for Private Endpoints
resource "azurerm_subnet" "private_endpoint_subnet" {
  name                 = "snet-private-endpoints"
  resource_group_name  = azurerm_resource_group.trip_cache_rg.name
  virtual_network_name = azurerm_virtual_network.trip_cache_vnet.name
  address_prefixes     = ["10.21.2.0/24"]
  
  private_endpoint_network_policies_enabled = false
}
```

### private_endpoint.tf
```hcl
# Private DNS Zone
resource "azurerm_private_dns_zone" "redis_dns_zone" {
  name                = "privatelink.redis.cache.windows.net"
  resource_group_name = azurerm_resource_group.trip_cache_rg.name

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "DNS"
  }
}

# Private DNS Zone Virtual Network Link
resource "azurerm_private_dns_zone_virtual_network_link" "redis_dns_link" {
  name                  = "dns-link-trip-cache"
  resource_group_name   = azurerm_resource_group.trip_cache_rg.name
  private_dns_zone_name = azurerm_private_dns_zone.redis_dns_zone.name
  virtual_network_id    = azurerm_virtual_network.trip_cache_vnet.id
  registration_enabled  = false

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "DNS"
  }
}

# Private Endpoint for Primary Cache
resource "azurerm_private_endpoint" "redis_private_endpoint" {
  name                = "pe-redis-trip-prod"
  location            = azurerm_resource_group.trip_cache_rg.location
  resource_group_name = azurerm_resource_group.trip_cache_rg.name
  subnet_id           = azurerm_subnet.private_endpoint_subnet.id

  private_service_connection {
    name                           = "psc-redis-trip-prod"
    private_connection_resource_id = azurerm_redis_cache.trip_cache.id
    subresource_names              = ["redisCache"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "dns-zone-group"
    private_dns_zone_ids = [azurerm_private_dns_zone.redis_dns_zone.id]
  }

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Network"
  }
}

# Private Endpoint for Secondary Cache
resource "azurerm_private_endpoint" "redis_secondary_private_endpoint" {
  name                = "pe-redis-trip-prod-sec"
  location            = "Korea South"
  resource_group_name = azurerm_resource_group.trip_cache_rg.name
  subnet_id           = azurerm_subnet.private_endpoint_subnet.id

  private_service_connection {
    name                           = "psc-redis-trip-prod-sec"
    private_connection_resource_id = azurerm_redis_cache.trip_cache_secondary.id
    subresource_names              = ["redisCache"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "dns-zone-group-sec"
    private_dns_zone_ids = [azurerm_private_dns_zone.redis_dns_zone.id]
  }

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Network"
    Role        = "Secondary"
  }
}
```

### key_vault.tf
```hcl
# Key Vault for Connection Strings
resource "azurerm_key_vault" "trip_cache_kv" {
  name                = "kv-tripgen-trip-cache-prod"
  location            = azurerm_resource_group.trip_cache_rg.location
  resource_group_name = azurerm_resource_group.trip_cache_rg.name
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
    Service     = "TripService"
    Component   = "Security"
  }
}

# Store Primary Redis Connection String
resource "azurerm_key_vault_secret" "redis_primary_connection_string" {
  name         = "redis-trip-primary-connection-string"
  value        = azurerm_redis_cache.trip_cache.primary_connection_string
  key_vault_id = azurerm_key_vault.trip_cache_kv.id

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Cache"
  }
}

# Store Secondary Redis Connection String
resource "azurerm_key_vault_secret" "redis_secondary_connection_string" {
  name         = "redis-trip-secondary-connection-string"
  value        = azurerm_redis_cache.trip_cache_secondary.primary_connection_string
  key_vault_id = azurerm_key_vault.trip_cache_kv.id

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Cache"
  }
}

# Store Primary Access Key
resource "azurerm_key_vault_secret" "redis_primary_key" {
  name         = "redis-trip-primary-key"
  value        = azurerm_redis_cache.trip_cache.primary_access_key
  key_vault_id = azurerm_key_vault.trip_cache_kv.id

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Cache"
  }
}

data "azurerm_client_config" "current" {}
```

### monitoring.tf
```hcl
# Log Analytics Workspace
resource "azurerm_log_analytics_workspace" "trip_cache_logs" {
  name                = "log-tripgen-trip-cache-prod"
  location            = azurerm_resource_group.trip_cache_rg.location
  resource_group_name = azurerm_resource_group.trip_cache_rg.name
  sku                 = "PerGB2018"
  retention_in_days   = 30

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Monitoring"
  }
}

# Diagnostic Settings for Primary Cache
resource "azurerm_monitor_diagnostic_setting" "redis_primary_diagnostics" {
  name                       = "redis-trip-primary-diagnostics"
  target_resource_id         = azurerm_redis_cache.trip_cache.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.trip_cache_logs.id

  enabled_log {
    category = "ConnectedClientList"
  }

  metric {
    category = "AllMetrics"
    enabled  = true
  }
}

# Diagnostic Settings for Secondary Cache
resource "azurerm_monitor_diagnostic_setting" "redis_secondary_diagnostics" {
  name                       = "redis-trip-secondary-diagnostics"
  target_resource_id         = azurerm_redis_cache.trip_cache_secondary.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.trip_cache_logs.id

  enabled_log {
    category = "ConnectedClientList"
  }

  metric {
    category = "AllMetrics"
    enabled  = true
  }
}

# Action Group for Alerts
resource "azurerm_monitor_action_group" "trip_cache_alerts" {
  name                = "ag-trip-cache-prod"
  resource_group_name = azurerm_resource_group.trip_cache_rg.name
  short_name          = "tripcache"

  email_receiver {
    name          = "admin"
    email_address = "admin@tripgen.com"
  }

  email_receiver {
    name          = "devops"
    email_address = "devops@tripgen.com"
  }

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Monitoring"
  }
}

# CPU Utilization Alert
resource "azurerm_monitor_metric_alert" "cpu_alert" {
  name                = "redis-trip-cpu-alert"
  resource_group_name = azurerm_resource_group.trip_cache_rg.name
  scopes              = [azurerm_redis_cache.trip_cache.id]
  description         = "Trip Cache CPU utilization is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.Cache/Redis"
    metric_name      = "percentProcessorTime"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 75
  }

  action {
    action_group_id = azurerm_monitor_action_group.trip_cache_alerts.id
  }

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Monitoring"
  }
}

# Memory Usage Alert
resource "azurerm_monitor_metric_alert" "memory_alert" {
  name                = "redis-trip-memory-alert"
  resource_group_name = azurerm_resource_group.trip_cache_rg.name
  scopes              = [azurerm_redis_cache.trip_cache.id]
  description         = "Trip Cache memory usage is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.Cache/Redis"
    metric_name      = "usedmemorypercentage"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 80
  }

  action {
    action_group_id = azurerm_monitor_action_group.trip_cache_alerts.id
  }

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Monitoring"
  }
}

# Hit Rate Alert
resource "azurerm_monitor_metric_alert" "hit_rate_alert" {
  name                = "redis-trip-hit-rate-alert"
  resource_group_name = azurerm_resource_group.trip_cache_rg.name
  scopes              = [azurerm_redis_cache.trip_cache.id]
  description         = "Trip Cache hit rate is low"
  severity            = 3

  criteria {
    metric_namespace = "Microsoft.Cache/Redis"
    metric_name      = "cachehitrate"
    aggregation      = "Average"
    operator         = "LessThan"
    threshold        = 80
  }

  action {
    action_group_id = azurerm_monitor_action_group.trip_cache_alerts.id
  }

  tags = {
    Environment = "Production"
    Service     = "TripService"
    Component   = "Monitoring"
  }
}
```

### outputs.tf
```hcl
output "redis_primary_hostname" {
  description = "Primary Redis Cache hostname"
  value       = azurerm_redis_cache.trip_cache.hostname
  sensitive   = false
}

output "redis_secondary_hostname" {
  description = "Secondary Redis Cache hostname"
  value       = azurerm_redis_cache.trip_cache_secondary.hostname
  sensitive   = false
}

output "redis_port" {
  description = "Redis Cache port"
  value       = azurerm_redis_cache.trip_cache.ssl_port
  sensitive   = false
}

output "redis_primary_key" {
  description = "Redis Cache primary key"
  value       = azurerm_redis_cache.trip_cache.primary_access_key
  sensitive   = true
}

output "key_vault_uri" {
  description = "Key Vault URI"
  value       = azurerm_key_vault.trip_cache_kv.vault_uri
  sensitive   = false
}

output "private_endpoint_primary_ip" {
  description = "Primary cache private endpoint IP address"
  value       = azurerm_private_endpoint.redis_private_endpoint.private_service_connection[0].private_ip_address
  sensitive   = false
}

output "private_endpoint_secondary_ip" {
  description = "Secondary cache private endpoint IP address"
  value       = azurerm_private_endpoint.redis_secondary_private_endpoint.private_service_connection[0].private_ip_address
  sensitive   = false
}
```

## 보안 설정

### 1. Private Endpoint 구성
- 주요 및 보조 캐시 모두 Private Endpoint 사용
- VNet 내부에서만 액세스 가능
- 퍼블릭 액세스 완전 차단

### 2. Key Vault 연동
- 주요/보조 연결 문자열 분리 저장
- 액세스 키 자동 로테이션 지원
- RBAC 기반 세밀한 권한 제어

### 3. 지리적 복제 보안
- SSL/TLS 암호화 복제
- 복제 링크 모니터링
- 장애 조치 시 보안 정책 유지

## 캐시 샤딩 및 클러스터링 설정

### 1. 샤드 구성
```yaml
샤드 분산 전략:
  - 샤드 0-2: 여행 목록 및 메타데이터
  - 샤드 3-4: 여행 상세 정보
  - 샤드 5: 검색 결과 및 임시 데이터
```

### 2. 키 분산 패턴
```
여행 목록: trip:list:{user_id}:{page}
여행 상세: trip:detail:{trip_id}
일정 데이터: trip:schedule:{trip_id}:{day}
검색 결과: trip:search:{query_hash}
```

### 3. 데이터 파티셔닝
- **수평 파티셔닝**: 사용자 ID 기반
- **수직 파티셔닝**: 데이터 타입별 분리
- **시간 기반 파티셔닝**: 최신 데이터 우선

## 모니터링 및 알림 설정

### 1. 핵심 메트릭
- **CPU 사용률**: 임계값 75%
- **메모리 사용률**: 임계값 80%
- **캐시 히트율**: 임계값 80% (미만 시 알림)
- **연결 수**: 임계값 1500개

### 2. 비즈니스 메트릭
- **여행 목록 캐시 히트율**: 85% 이상
- **상세 정보 캐시 히트율**: 90% 이상
- **검색 결과 캐시 히트율**: 70% 이상

### 3. 지리적 복제 모니터링
- 복제 지연 시간
- 복제 연결 상태
- 데이터 동기화 정확성

## 성능 최적화

### 1. 메모리 정책
```redis
maxmemory-policy: allkeys-lru
maxmemory-reserved: 4096MB
maxmemory-delta: 4096MB
notify-keyspace-events: Ex
```

### 2. TTL 최적화
```yaml
여행 목록 TTL: 2시간 (7200초)
여행 상세 TTL: 6시간 (21600초)
일정 데이터 TTL: 4시간 (14400초)
검색 결과 TTL: 30분 (1800초)
```

### 3. 연결 풀링
```yaml
최대 연결 수: 1500
연결 타임아웃: 5초
읽기 타임아웃: 3초
명령 타임아웃: 5초
```

### 4. 압축 설정
```yaml
압축 알고리즘: LZ4
압축 임계값: 1KB 이상
압축률: 평균 60-70%
```

## 비용 최적화

### 1. 인스턴스 최적화
- **P3 인스턴스**: 52GB 메모리, 높은 처리량
- **Zone Redundancy**: 고가용성과 성능 균형
- **예약 인스턴스**: 1년 예약으로 30% 비용 절감

### 2. 지리적 복제 비용
- **보조 인스턴스**: 주요 인스턴스와 동일 사양
- **데이터 전송 비용**: 지역 간 복제 시 발생
- **백업 비용**: 주요 인스턴스만 백업 활성화

### 3. 모니터링 비용
- **Log Analytics**: 30일 보존
- **메트릭 저장**: 90일
- **알림**: 중요 메트릭 중심

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

### 2. 단계별 배포
```bash
# 1단계: 주요 인프라 배포
terraform apply -target=azurerm_resource_group.trip_cache_rg -var-file="prod.tfvars"
terraform apply -target=azurerm_virtual_network.trip_cache_vnet -var-file="prod.tfvars"

# 2단계: 캐시 인스턴스 배포
terraform apply -target=azurerm_redis_cache.trip_cache -var-file="prod.tfvars"
terraform apply -target=azurerm_redis_cache.trip_cache_secondary -var-file="prod.tfvars"

# 3단계: 전체 배포
terraform apply -var-file="prod.tfvars"
```

### 3. 배포 후 검증
```bash
# 주요 캐시 연결 테스트
redis-cli -h <primary-hostname> -p 6380 --tls ping

# 보조 캐시 연결 테스트
redis-cli -h <secondary-hostname> -p 6380 --tls ping

# 클러스터 정보 확인
redis-cli -h <primary-hostname> -p 6380 --tls cluster info

# 지리적 복제 상태 확인
redis-cli -h <primary-hostname> -p 6380 --tls info replication
```

## 애플리케이션 연동

### 1. 연결 문자열 구성
```yaml
spring:
  redis:
    cluster:
      nodes:
        - ${REDIS_PRIMARY_HOSTNAME}:6380
        - ${REDIS_SECONDARY_HOSTNAME}:6380
    ssl: true
    timeout: 5000ms
    password: ${REDIS_PASSWORD}
    lettuce:
      pool:
        max-active: 200
        max-idle: 20
        min-idle: 5
```

### 2. 캐시 구성 클래스
```java
@Configuration
@EnableCaching
public class TripCacheConfig {
    
    @Bean
    @Primary
    public LettuceConnectionFactory primaryRedisConnectionFactory() {
        RedisClusterConfiguration clusterConfig = 
            new RedisClusterConfiguration();
        clusterConfig.clusterNode(primaryRedisHost, redisPort);
        
        LettuceClientConfiguration clientConfig = 
            LettuceClientConfiguration.builder()
                .useSsl()
                .commandTimeout(Duration.ofSeconds(5))
                .build();
                
        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }
    
    @Bean
    public LettuceConnectionFactory secondaryRedisConnectionFactory() {
        RedisClusterConfiguration clusterConfig = 
            new RedisClusterConfiguration();
        clusterConfig.clusterNode(secondaryRedisHost, redisPort);
        
        LettuceClientConfiguration clientConfig = 
            LettuceClientConfiguration.builder()
                .useSsl()
                .commandTimeout(Duration.ofSeconds(5))
                .readFrom(ReadFrom.REPLICA_PREFERRED)
                .build();
                
        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }
}
```

### 3. 캐시 서비스 구현
```java
@Service
public class TripCacheService {
    
    @Cacheable(value = "trip:list", key = "#userId + ':' + #page")
    public List<Trip> getTripList(String userId, int page) {
        // 데이터베이스에서 조회
        return tripRepository.findByUserId(userId, PageRequest.of(page, 20));
    }
    
    @Cacheable(value = "trip:detail", key = "#tripId")
    public TripDetail getTripDetail(String tripId) {
        // 데이터베이스에서 조회
        return tripRepository.findDetailById(tripId);
    }
    
    @CacheEvict(value = {"trip:list", "trip:detail"}, key = "#tripId")
    public void evictTripCache(String tripId) {
        // 캐시 무효화
    }
}
```

## 운영 가이드

### 1. 일상 모니터링
- 메모리 사용률 및 히트율 확인
- 지리적 복제 상태 점검
- 연결 수 및 응답 시간 모니터링

### 2. 정기 점검 (주간)
- 성능 메트릭 트렌드 분석
- 백업 상태 및 복제 지연 확인
- 보안 패치 및 업데이트 적용

### 3. 장애 대응
- 주요 캐시 장애 시 보조 캐시로 자동 전환
- 복제 연결 끊김 시 즉시 복구 작업
- 메모리 부족 시 스케일업 또는 데이터 정리

### 4. 용량 계획
- 월간 데이터 증가율 분석
- 피크 시간대 사용량 패턴 파악
- 필요시 샤드 수 또는 인스턴스 크기 조정