# Location Service 캐시 운영환경 설치 가이드

## 개요

Location Service의 운영환경에서 사용할 Azure Cache for Redis Premium 설치 및 구성 가이드입니다.
장소 상세정보, 검색 결과, 추천 장소를 고성능으로 캐싱하기 위한 클러스터 환경을 구축합니다.

### 주요 특징
- **Azure Cache for Redis Premium** (6샤드 클러스터)
- **Zone Redundant** 고가용성 구성
- **Private Endpoint** 보안 연결  
- **Key Vault** 연결 문자열 관리
- **지리적 분산 캐싱** 및 **위치 기반 최적화**

## Azure 리소스 구성

### 1. Redis Cache Premium 구성
```
- SKU: Premium P3 (52GB)
- 샤드 개수: 6개
- 가용성: Zone Redundant
- 지역: Korea Central
- 네트워크: Private Endpoint
- 지리적 검색: 최적화
```

### 2. 캐시 전략
- **장소 상세**: 장소 상세 정보 (TTL: 12시간)
- **검색 결과**: 위치 기반 검색 결과 (TTL: 1시간)
- **추천 장소**: AI 추천 장소 목록 (TTL: 4시간)
- **인기 장소**: 인기도 기반 랭킹 (TTL: 6시간)

## Terraform 배포 스크립트

### main.tf
```hcl
# Resource Group
resource "azurerm_resource_group" "location_cache_rg" {
  name     = "rg-tripgen-location-cache-prod"
  location = "Korea Central"

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Cache"
  }
}

# Azure Cache for Redis Premium
resource "azurerm_redis_cache" "location_cache" {
  name                = "redis-tripgen-location-prod"
  location            = azurerm_resource_group.location_cache_rg.location
  resource_group_name = azurerm_resource_group.location_cache_rg.name
  
  capacity            = 1
  family              = "P"
  sku_name           = "Premium"
  enable_non_ssl_port = false
  minimum_tls_version = "1.2"
  
  # 클러스터 구성
  shard_count = 6
  
  # Zone Redundancy
  zones = ["1", "2", "3"]
  
  # Redis 구성 - 지리적 데이터 최적화
  redis_configuration {
    enable_authentication           = true
    maxmemory_reserved             = 4096
    maxmemory_delta                = 4096
    maxmemory_policy               = "allkeys-lru"
    notify_keyspace_events         = "Ex"
    rdb_backup_enabled             = true
    rdb_backup_frequency           = 1440  # 24시간마다
    rdb_backup_max_snapshot_count  = 2
    rdb_storage_connection_string  = azurerm_storage_account.backup_storage.primary_blob_connection_string
  }

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Cache"
  }
}

# CDN을 위한 보조 Redis Cache (읽기 전용)
resource "azurerm_redis_cache" "location_cache_cdn" {
  name                = "redis-tripgen-location-prod-cdn"
  location            = "Korea South"
  resource_group_name = azurerm_resource_group.location_cache_rg.name
  
  capacity            = 1
  family              = "P"
  sku_name           = "Premium"
  enable_non_ssl_port = false
  minimum_tls_version = "1.2"
  
  # 읽기 전용 클러스터
  shard_count = 3
  
  # Zone Redundancy
  zones = ["1", "2", "3"]
  
  # Redis 구성
  redis_configuration {
    enable_authentication           = true
    maxmemory_reserved             = 2048
    maxmemory_delta                = 2048
    maxmemory_policy               = "allkeys-lru"
    notify_keyspace_events         = "Ex"
    rdb_backup_enabled             = false
  }

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Cache"
    Role        = "CDN"
  }
}

# 백업용 Storage Account
resource "azurerm_storage_account" "backup_storage" {
  name                     = "stlocationcacheprodbackup"
  resource_group_name      = azurerm_resource_group.location_cache_rg.name
  location                 = azurerm_resource_group.location_cache_rg.location
  account_tier             = "Standard"
  account_replication_type = "ZRS"
  
  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "CacheBackup"
  }
}

# Virtual Network
resource "azurerm_virtual_network" "location_cache_vnet" {
  name                = "vnet-tripgen-location-cache-prod"
  address_space       = ["10.23.0.0/16"]
  location            = azurerm_resource_group.location_cache_rg.location
  resource_group_name = azurerm_resource_group.location_cache_rg.name

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Network"
  }
}

# Subnet for Cache
resource "azurerm_subnet" "cache_subnet" {
  name                 = "snet-cache"
  resource_group_name  = azurerm_resource_group.location_cache_rg.name
  virtual_network_name = azurerm_virtual_network.location_cache_vnet.name
  address_prefixes     = ["10.23.1.0/24"]
}

# Subnet for Private Endpoints
resource "azurerm_subnet" "private_endpoint_subnet" {
  name                 = "snet-private-endpoints"
  resource_group_name  = azurerm_resource_group.location_cache_rg.name
  virtual_network_name = azurerm_virtual_network.location_cache_vnet.name
  address_prefixes     = ["10.23.2.0/24"]
  
  private_endpoint_network_policies_enabled = false
}
```

### private_endpoint.tf
```hcl
# Private DNS Zone
resource "azurerm_private_dns_zone" "redis_dns_zone" {
  name                = "privatelink.redis.cache.windows.net"
  resource_group_name = azurerm_resource_group.location_cache_rg.name

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "DNS"
  }
}

# Private DNS Zone Virtual Network Link
resource "azurerm_private_dns_zone_virtual_network_link" "redis_dns_link" {
  name                  = "dns-link-location-cache"
  resource_group_name   = azurerm_resource_group.location_cache_rg.name
  private_dns_zone_name = azurerm_private_dns_zone.redis_dns_zone.name
  virtual_network_id    = azurerm_virtual_network.location_cache_vnet.id
  registration_enabled  = false

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "DNS"
  }
}

# Private Endpoint for Main Cache
resource "azurerm_private_endpoint" "redis_private_endpoint" {
  name                = "pe-redis-location-prod"
  location            = azurerm_resource_group.location_cache_rg.location
  resource_group_name = azurerm_resource_group.location_cache_rg.name
  subnet_id           = azurerm_subnet.private_endpoint_subnet.id

  private_service_connection {
    name                           = "psc-redis-location-prod"
    private_connection_resource_id = azurerm_redis_cache.location_cache.id
    subresource_names              = ["redisCache"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "dns-zone-group"
    private_dns_zone_ids = [azurerm_private_dns_zone.redis_dns_zone.id]
  }

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Network"
  }
}

# Private Endpoint for CDN Cache
resource "azurerm_private_endpoint" "redis_cdn_private_endpoint" {
  name                = "pe-redis-location-prod-cdn"
  location            = "Korea South"
  resource_group_name = azurerm_resource_group.location_cache_rg.name
  subnet_id           = azurerm_subnet.private_endpoint_subnet.id

  private_service_connection {
    name                           = "psc-redis-location-prod-cdn"
    private_connection_resource_id = azurerm_redis_cache.location_cache_cdn.id
    subresource_names              = ["redisCache"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "dns-zone-group-cdn"
    private_dns_zone_ids = [azurerm_private_dns_zone.redis_dns_zone.id]
  }

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Network"
    Role        = "CDN"
  }
}
```

### key_vault.tf
```hcl
# Key Vault for Connection Strings
resource "azurerm_key_vault" "location_cache_kv" {
  name                = "kv-tripgen-location-cache-prod"
  location            = azurerm_resource_group.location_cache_rg.location
  resource_group_name = azurerm_resource_group.location_cache_rg.name
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
    Service     = "LocationService"
    Component   = "Security"
  }
}

# Store Main Redis Connection String
resource "azurerm_key_vault_secret" "redis_main_connection_string" {
  name         = "redis-location-main-connection-string"
  value        = azurerm_redis_cache.location_cache.primary_connection_string
  key_vault_id = azurerm_key_vault.location_cache_kv.id

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Cache"
  }
}

# Store CDN Redis Connection String
resource "azurerm_key_vault_secret" "redis_cdn_connection_string" {
  name         = "redis-location-cdn-connection-string"
  value        = azurerm_redis_cache.location_cache_cdn.primary_connection_string
  key_vault_id = azurerm_key_vault.location_cache_kv.id

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Cache"
    Role        = "CDN"
  }
}

# Store Primary Access Key
resource "azurerm_key_vault_secret" "redis_primary_key" {
  name         = "redis-location-primary-key"
  value        = azurerm_redis_cache.location_cache.primary_access_key
  key_vault_id = azurerm_key_vault.location_cache_kv.id

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Cache"
  }
}

# Store Geo-spatial Configuration
resource "azurerm_key_vault_secret" "geospatial_config" {
  name         = "redis-location-geospatial-config"
  value = jsonencode({
    default_radius_km = 10
    max_radius_km     = 100
    precision_levels  = {
      city      = 1000   # 1km precision
      district  = 100    # 100m precision
      street    = 10     # 10m precision
    }
    geo_units = "km"
  })
  key_vault_id = azurerm_key_vault.location_cache_kv.id

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "GeoSpatial"
  }
}

data "azurerm_client_config" "current" {}
```

### monitoring.tf
```hcl
# Log Analytics Workspace
resource "azurerm_log_analytics_workspace" "location_cache_logs" {
  name                = "log-tripgen-location-cache-prod"
  location            = azurerm_resource_group.location_cache_rg.location
  resource_group_name = azurerm_resource_group.location_cache_rg.name
  sku                 = "PerGB2018"
  retention_in_days   = 30

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Monitoring"
  }
}

# Diagnostic Settings for Main Cache
resource "azurerm_monitor_diagnostic_setting" "redis_main_diagnostics" {
  name                       = "redis-location-main-diagnostics"
  target_resource_id         = azurerm_redis_cache.location_cache.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.location_cache_logs.id

  enabled_log {
    category = "ConnectedClientList"
  }

  metric {
    category = "AllMetrics"
    enabled  = true
  }
}

# Diagnostic Settings for CDN Cache
resource "azurerm_monitor_diagnostic_setting" "redis_cdn_diagnostics" {
  name                       = "redis-location-cdn-diagnostics"
  target_resource_id         = azurerm_redis_cache.location_cache_cdn.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.location_cache_logs.id

  enabled_log {
    category = "ConnectedClientList"
  }

  metric {
    category = "AllMetrics"
    enabled  = true
  }
}

# Action Group for Alerts
resource "azurerm_monitor_action_group" "location_cache_alerts" {
  name                = "ag-location-cache-prod"
  resource_group_name = azurerm_resource_group.location_cache_rg.name
  short_name          = "loccache"

  email_receiver {
    name          = "admin"
    email_address = "admin@tripgen.com"
  }

  email_receiver {
    name          = "location-team"
    email_address = "location-team@tripgen.com"
  }

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Monitoring"
  }
}

# CPU Utilization Alert
resource "azurerm_monitor_metric_alert" "cpu_alert" {
  name                = "redis-location-cpu-alert"
  resource_group_name = azurerm_resource_group.location_cache_rg.name
  scopes              = [azurerm_redis_cache.location_cache.id]
  description         = "Location Cache CPU utilization is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.Cache/Redis"
    metric_name      = "percentProcessorTime"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 75
  }

  action {
    action_group_id = azurerm_monitor_action_group.location_cache_alerts.id
  }

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Monitoring"
  }
}

# Memory Usage Alert
resource "azurerm_monitor_metric_alert" "memory_alert" {
  name                = "redis-location-memory-alert"
  resource_group_name = azurerm_resource_group.location_cache_rg.name
  scopes              = [azurerm_redis_cache.location_cache.id]
  description         = "Location Cache memory usage is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.Cache/Redis"
    metric_name      = "usedmemorypercentage"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 80
  }

  action {
    action_group_id = azurerm_monitor_action_group.location_cache_alerts.id
  }

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Monitoring"
  }
}

# Cache Hit Rate Alert
resource "azurerm_monitor_metric_alert" "hit_rate_alert" {
  name                = "redis-location-hit-rate-alert"
  resource_group_name = azurerm_resource_group.location_cache_rg.name
  scopes              = [azurerm_redis_cache.location_cache.id]
  description         = "Location Cache hit rate is low"
  severity            = 3

  criteria {
    metric_namespace = "Microsoft.Cache/Redis"
    metric_name      = "cachehitrate"
    aggregation      = "Average"
    operator         = "LessThan"
    threshold        = 85
  }

  action {
    action_group_id = azurerm_monitor_action_group.location_cache_alerts.id
  }

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Monitoring"
  }
}

# Geo Query Performance Alert
resource "azurerm_monitor_metric_alert" "geo_query_alert" {
  name                = "redis-location-geo-query-alert"
  resource_group_name = azurerm_resource_group.location_cache_rg.name
  scopes              = [azurerm_redis_cache.location_cache.id]
  description         = "Location Cache geo query response time is high"
  severity            = 3

  criteria {
    metric_namespace = "Microsoft.Cache/Redis"
    metric_name      = "serverLoad"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 70
  }

  action {
    action_group_id = azurerm_monitor_action_group.location_cache_alerts.id
  }

  tags = {
    Environment = "Production"
    Service     = "LocationService"
    Component   = "Monitoring"
  }
}
```

### outputs.tf
```hcl
output "redis_main_hostname" {
  description = "Main Redis Cache hostname"
  value       = azurerm_redis_cache.location_cache.hostname
  sensitive   = false
}

output "redis_cdn_hostname" {
  description = "CDN Redis Cache hostname"
  value       = azurerm_redis_cache.location_cache_cdn.hostname
  sensitive   = false
}

output "redis_port" {
  description = "Redis Cache port"
  value       = azurerm_redis_cache.location_cache.ssl_port
  sensitive   = false
}

output "redis_primary_key" {
  description = "Redis Cache primary key"
  value       = azurerm_redis_cache.location_cache.primary_access_key
  sensitive   = true
}

output "key_vault_uri" {
  description = "Key Vault URI"
  value       = azurerm_key_vault.location_cache_kv.vault_uri
  sensitive   = false
}

output "private_endpoint_main_ip" {
  description = "Main cache private endpoint IP address"
  value       = azurerm_private_endpoint.redis_private_endpoint.private_service_connection[0].private_ip_address
  sensitive   = false
}

output "private_endpoint_cdn_ip" {
  description = "CDN cache private endpoint IP address"
  value       = azurerm_private_endpoint.redis_cdn_private_endpoint.private_service_connection[0].private_ip_address
  sensitive   = false
}

output "geospatial_config" {
  description = "Geo-spatial configuration"
  value = {
    default_radius_km = 10
    max_radius_km     = 100
    precision_levels  = {
      city      = 1000
      district  = 100
      street    = 10
    }
  }
  sensitive = false
}
```

## 보안 설정

### 1. Private Endpoint 구성
- 메인 및 CDN 캐시 모두 Private Endpoint 사용
- VNet 내부에서만 액세스 가능
- 지리적 데이터 보안 강화

### 2. Key Vault 연동
- 메인/CDN 연결 문자열 분리 저장
- 지리적 검색 설정 정보 보호
- 액세스 키 자동 로테이션

### 3. 지리적 데이터 보안
- 위치 정보 암호화 저장
- 개인 위치 데이터 익명화
- GDPR 준수 데이터 처리

## 캐시 샤딩 및 클러스터링 설정

### 1. 샤드 구성
```yaml
샤드 분산 전략:
  - 샤드 0-2: 장소 상세 정보 및 메타데이터
  - 샤드 3-4: 검색 결과 및 인기 장소
  - 샤드 5: 추천 장소 및 지리적 인덱스
```

### 2. 키 분산 패턴
```
장소 상세: location:detail:{place_id}
검색 결과: location:search:{query_hash}:{lat}:{lng}:{radius}
추천 장소: location:recommend:{user_id}:{city}
인기 장소: location:popular:{city}:{category}
지리적 인덱스: location:geo:{grid_id}
```

### 3. 지리적 분산 전략
- **격자 기반 파티셔닝**: 지리적 격자별 데이터 분산
- **거리 기반 클러스터링**: 인접 지역 데이터 그룹화
- **핫스팟 최적화**: 인기 지역 전용 샤드 할당

## 지리적 검색 최적화

### 1. Redis Geo-spatial 활용
```redis
# 장소 위치 정보 저장
GEOADD locations:seoul 126.9780 37.5665 "seoul_tower"
GEOADD locations:seoul 126.9910 37.5547 "gangnam_station"

# 반경 검색
GEORADIUS locations:seoul 126.9780 37.5665 5 km WITHDIST WITHCOORD
```

### 2. 검색 결과 캐싱 전략
```yaml
검색 캐시 키: "location:search:{lat}:{lng}:{radius}:{category}"
TTL: 1시간
캐시 워밍: 인기 지역 사전 캐싱
무효화: 장소 정보 업데이트 시
```

### 3. 성능 최적화
- **격자 기반 인덱싱**: 지리적 영역을 격자로 분할
- **다단계 캐싱**: 근거리 → 중거리 → 원거리 순차 검색
- **결과 압축**: JSON 압축으로 메모리 절약

## 모니터링 및 알림 설정

### 1. 핵심 메트릭
- **CPU 사용률**: 임계값 75%
- **메모리 사용률**: 임계값 80%
- **캐시 히트율**: 임계값 85% 이상
- **지리적 쿼리 응답시간**: 임계값 50ms

### 2. 지리적 검색 메트릭
- **GEORADIUS 쿼리 성능**: 평균 응답시간
- **지역별 검색 빈도**: 핫스팟 식별
- **검색 결과 정확도**: 거리 계산 정확성

### 3. 비즈니스 메트릭
- **장소 상세 캐시 히트율**: 90% 이상
- **검색 결과 캐시 히트율**: 80% 이상
- **인기 장소 업데이트 빈도**: 실시간 반영

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
장소 상세 TTL: 12시간 (43200초)
검색 결과 TTL: 1시간 (3600초)
추천 장소 TTL: 4시간 (14400초)
인기 장소 TTL: 6시간 (21600초)
```

### 3. 연결 풀링
```yaml
최대 연결 수: 1000
연결 타임아웃: 5초
읽기 타임아웃: 3초
지리적 쿼리 타임아웃: 10초
```

### 4. 압축 및 직렬화
```yaml
압축 알고리즘: Snappy
압축 임계값: 512 bytes
직렬화: MessagePack
지리적 데이터: WKB 포맷
```

## 비용 최적화

### 1. 인스턴스 최적화
- **메인 캐시**: P3 (52GB) - 읽기/쓰기 최적화
- **CDN 캐시**: P2 (26GB) - 읽기 전용 최적화
- **예약 인스턴스**: 1년 예약으로 30% 비용 절감

### 2. 데이터 최적화
- **압축률**: 평균 70% 압축
- **데이터 정제**: 오래된 검색 결과 자동 정리
- **캐시 워밍**: 피크 시간 전 사전 로딩

### 3. 네트워크 비용
- **CDN 캐시**: 지역별 트래픽 분산
- **압축 전송**: 네트워크 대역폭 절약
- **배치 처리**: 다중 쿼리 최적화

## 배포 가이드

### 1. 사전 준비
```bash
# Terraform 초기화
terraform init

# 변수 파일 생성
cp prod.tfvars.example prod.tfvars

# 지리적 데이터 준비
python scripts/prepare-geo-data.py
```

### 2. 단계별 배포
```bash
# 1단계: 네트워크 인프라
terraform apply -target=azurerm_virtual_network.location_cache_vnet -var-file="prod.tfvars"

# 2단계: 메인 캐시 배포
terraform apply -target=azurerm_redis_cache.location_cache -var-file="prod.tfvars"

# 3단계: CDN 캐시 배포
terraform apply -target=azurerm_redis_cache.location_cache_cdn -var-file="prod.tfvars"

# 4단계: 전체 배포
terraform apply -var-file="prod.tfvars"
```

### 3. 배포 후 검증
```bash
# 메인 캐시 연결 테스트
redis-cli -h <main-hostname> -p 6380 --tls ping

# CDN 캐시 연결 테스트
redis-cli -h <cdn-hostname> -p 6380 --tls ping

# 지리적 쿼리 테스트
redis-cli -h <main-hostname> -p 6380 --tls \
  GEOADD test:locations 126.9780 37.5665 "test_location"

redis-cli -h <main-hostname> -p 6380 --tls \
  GEORADIUS test:locations 126.9780 37.5665 1 km
```

## 애플리케이션 연동

### 1. 연결 문자열 구성
```yaml
spring:
  redis:
    cluster:
      nodes:
        - ${REDIS_MAIN_HOSTNAME}:6380    # 읽기/쓰기
        - ${REDIS_CDN_HOSTNAME}:6380     # 읽기 전용
    ssl: true
    timeout: 10000ms
    password: ${REDIS_PASSWORD}
    lettuce:
      pool:
        max-active: 300
        max-idle: 30
        min-idle: 10
```

### 2. 지리적 캐시 구성 클래스
```java
@Configuration
@EnableCaching
public class LocationCacheConfig {
    
    @Bean
    @Primary
    public LettuceConnectionFactory mainRedisConnectionFactory() {
        RedisClusterConfiguration clusterConfig = 
            new RedisClusterConfiguration();
        clusterConfig.clusterNode(mainRedisHost, redisPort);
        
        LettuceClientConfiguration clientConfig = 
            LettuceClientConfiguration.builder()
                .useSsl()
                .commandTimeout(Duration.ofSeconds(10))
                .build();
                
        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }
    
    @Bean("cdnRedisConnectionFactory")
    public LettuceConnectionFactory cdnRedisConnectionFactory() {
        RedisClusterConfiguration clusterConfig = 
            new RedisClusterConfiguration();
        clusterConfig.clusterNode(cdnRedisHost, redisPort);
        
        LettuceClientConfiguration clientConfig = 
            LettuceClientConfiguration.builder()
                .useSsl()
                .readFrom(ReadFrom.REPLICA_PREFERRED)
                .commandTimeout(Duration.ofSeconds(5))
                .build();
                
        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }
}
```

### 3. 지리적 검색 서비스 구현
```java
@Service
public class LocationCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // 장소 상세 정보 캐싱
    @Cacheable(value = "location:detail", key = "#placeId")
    public PlaceDetail getPlaceDetail(String placeId) {
        return placeRepository.findById(placeId);
    }
    
    // 지리적 검색 결과 캐싱
    public List<Place> searchNearbyPlaces(double lat, double lng, double radius) {
        String key = String.format("location:search:%.6f:%.6f:%.1f", lat, lng, radius);
        
        List<Place> cached = (List<Place>) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }
        
        // Redis GEO 명령 사용
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = 
            redisTemplate.opsForGeo().radius("locations", 
                new Circle(new Point(lng, lat), new Distance(radius, Metrics.KILOMETERS)));
        
        List<Place> places = results.getContent().stream()
            .map(result -> getPlaceDetail(result.getContent().getName()))
            .collect(Collectors.toList());
        
        // 결과 캐싱 (1시간)
        redisTemplate.opsForValue().set(key, places, Duration.ofHours(1));
        
        return places;
    }
    
    // 인기 장소 캐싱
    @Cacheable(value = "location:popular", key = "#city + ':' + #category")
    public List<Place> getPopularPlaces(String city, String category) {
        return placeRepository.findPopularByCategory(city, category);
    }
}
```

## 운영 가이드

### 1. 일상 모니터링
- 메모리 사용률 및 캐시 히트율 확인
- 지리적 쿼리 성능 모니터링
- CDN 캐시 동기화 상태 점검

### 2. 정기 점검 (주간)
- 지역별 검색 패턴 분석
- 인기 장소 랭킹 업데이트
- 캐시 데이터 정합성 검증

### 3. 장애 대응
- 메인 캐시 장애 시 CDN 캐시로 읽기 전환
- 지리적 쿼리 성능 저하 시 인덱스 재구성
- 메모리 부족 시 오래된 검색 결과 정리

### 4. 용량 계획
- 월간 검색량 증가율 분석
- 새로운 지역 추가에 따른 용량 계획
- 피크 시간대 지리적 검색 패턴 분석

### 5. 데이터 관리
- 장소 정보 업데이트 시 관련 캐시 무효화
- 계절별 인기 장소 변동 반영
- 지리적 정확도 정기 검증