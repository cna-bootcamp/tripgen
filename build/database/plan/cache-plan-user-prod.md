# User Service 캐시 운영환경 설치 가이드

## 개요

User Service의 운영환경에서 사용할 Azure Cache for Redis Premium 설치 및 구성 가이드입니다.
사용자 세션, 프로필, 로그인 제한, JWT 블랙리스트 등을 고성능으로 캐싱하기 위한 클러스터 환경을 구축합니다.

### 주요 특징
- **Azure Cache for Redis Premium** (6샤드 클러스터)
- **Zone Redundant** 고가용성 구성
- **Private Endpoint** 보안 연결
- **Key Vault** 연결 문자열 관리
- **자동 백업** 및 **모니터링**

## Azure 리소스 구성

### 1. Redis Cache Premium 구성
```
- SKU: Premium P2 (26GB)
- 샤드 개수: 6개
- 가용성: Zone Redundant
- 지역: Korea Central
- 네트워크: Private Endpoint
```

### 2. 캐시 전략
- **세션 관리**: 사용자 세션 정보 (TTL: 24시간)
- **프로필 캐싱**: 사용자 프로필 데이터 (TTL: 4시간)
- **로그인 제한**: 실패 시도 카운트 (TTL: 1시간)
- **JWT 블랙리스트**: 무효화된 토큰 (TTL: 토큰 만료시간까지)

## Terraform 배포 스크립트

### main.tf
```hcl
# Resource Group
resource "azurerm_resource_group" "user_cache_rg" {
  name     = "rg-tripgen-user-cache-prod"
  location = "Korea Central"

  tags = {
    Environment = "Production"
    Service     = "UserService"
    Component   = "Cache"
  }
}

# Azure Cache for Redis Premium
resource "azurerm_redis_cache" "user_cache" {
  name                = "redis-tripgen-user-prod"
  location            = azurerm_resource_group.user_cache_rg.location
  resource_group_name = azurerm_resource_group.user_cache_rg.name
  
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
    maxmemory_reserved             = 2048
    maxmemory_delta                = 2048
    maxmemory_policy               = "allkeys-lru"
    notify_keyspace_events         = "Ex"
    rdb_backup_enabled             = true
    rdb_backup_frequency           = 1440
    rdb_backup_max_snapshot_count  = 1
    rdb_storage_connection_string  = azurerm_storage_account.backup_storage.primary_blob_connection_string
  }

  tags = {
    Environment = "Production"
    Service     = "UserService"
    Component   = "Cache"
  }
}

# 백업용 Storage Account
resource "azurerm_storage_account" "backup_storage" {
  name                     = "stusercacheprodbackup"
  resource_group_name      = azurerm_resource_group.user_cache_rg.name
  location                 = azurerm_resource_group.user_cache_rg.location
  account_tier             = "Standard"
  account_replication_type = "ZRS"
  
  tags = {
    Environment = "Production"
    Service     = "UserService"
    Component   = "CacheBackup"
  }
}

# Virtual Network
resource "azurerm_virtual_network" "user_cache_vnet" {
  name                = "vnet-tripgen-user-cache-prod"
  address_space       = ["10.20.0.0/16"]
  location            = azurerm_resource_group.user_cache_rg.location
  resource_group_name = azurerm_resource_group.user_cache_rg.name

  tags = {
    Environment = "Production"
    Service     = "UserService"
    Component   = "Network"
  }
}

# Subnet for Cache
resource "azurerm_subnet" "cache_subnet" {
  name                 = "snet-cache"
  resource_group_name  = azurerm_resource_group.user_cache_rg.name
  virtual_network_name = azurerm_virtual_network.user_cache_vnet.name
  address_prefixes     = ["10.20.1.0/24"]
}

# Subnet for Private Endpoints
resource "azurerm_subnet" "private_endpoint_subnet" {
  name                 = "snet-private-endpoints"
  resource_group_name  = azurerm_resource_group.user_cache_rg.name
  virtual_network_name = azurerm_virtual_network.user_cache_vnet.name
  address_prefixes     = ["10.20.2.0/24"]
  
  private_endpoint_network_policies_enabled = false
}
```

### private_endpoint.tf
```hcl
# Private DNS Zone
resource "azurerm_private_dns_zone" "redis_dns_zone" {
  name                = "privatelink.redis.cache.windows.net"
  resource_group_name = azurerm_resource_group.user_cache_rg.name

  tags = {
    Environment = "Production"
    Service     = "UserService"
    Component   = "DNS"
  }
}

# Private DNS Zone Virtual Network Link
resource "azurerm_private_dns_zone_virtual_network_link" "redis_dns_link" {
  name                  = "dns-link-user-cache"
  resource_group_name   = azurerm_resource_group.user_cache_rg.name
  private_dns_zone_name = azurerm_private_dns_zone.redis_dns_zone.name
  virtual_network_id    = azurerm_virtual_network.user_cache_vnet.id
  registration_enabled  = false

  tags = {
    Environment = "Production"
    Service     = "UserService"
    Component   = "DNS"
  }
}

# Private Endpoint
resource "azurerm_private_endpoint" "redis_private_endpoint" {
  name                = "pe-redis-user-prod"
  location            = azurerm_resource_group.user_cache_rg.location
  resource_group_name = azurerm_resource_group.user_cache_rg.name
  subnet_id           = azurerm_subnet.private_endpoint_subnet.id

  private_service_connection {
    name                           = "psc-redis-user-prod"
    private_connection_resource_id = azurerm_redis_cache.user_cache.id
    subresource_names              = ["redisCache"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "dns-zone-group"
    private_dns_zone_ids = [azurerm_private_dns_zone.redis_dns_zone.id]
  }

  tags = {
    Environment = "Production"
    Service     = "UserService"
    Component   = "Network"
  }
}
```

### key_vault.tf
```hcl
# Key Vault for Connection Strings
resource "azurerm_key_vault" "user_cache_kv" {
  name                = "kv-tripgen-user-cache-prod"
  location            = azurerm_resource_group.user_cache_rg.location
  resource_group_name = azurerm_resource_group.user_cache_rg.name
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
    Service     = "UserService"
    Component   = "Security"
  }
}

# Store Redis Connection String in Key Vault
resource "azurerm_key_vault_secret" "redis_connection_string" {
  name         = "redis-user-connection-string"
  value        = azurerm_redis_cache.user_cache.primary_connection_string
  key_vault_id = azurerm_key_vault.user_cache_kv.id

  tags = {
    Environment = "Production"
    Service     = "UserService"
    Component   = "Cache"
  }
}

# Store Redis Primary Key
resource "azurerm_key_vault_secret" "redis_primary_key" {
  name         = "redis-user-primary-key"
  value        = azurerm_redis_cache.user_cache.primary_access_key
  key_vault_id = azurerm_key_vault.user_cache_kv.id

  tags = {
    Environment = "Production"
    Service     = "UserService"
    Component   = "Cache"
  }
}

data "azurerm_client_config" "current" {}
```

### monitoring.tf
```hcl
# Log Analytics Workspace
resource "azurerm_log_analytics_workspace" "user_cache_logs" {
  name                = "log-tripgen-user-cache-prod"
  location            = azurerm_resource_group.user_cache_rg.location
  resource_group_name = azurerm_resource_group.user_cache_rg.name
  sku                 = "PerGB2018"
  retention_in_days   = 30

  tags = {
    Environment = "Production"
    Service     = "UserService"
    Component   = "Monitoring"
  }
}

# Diagnostic Settings
resource "azurerm_monitor_diagnostic_setting" "redis_diagnostics" {
  name                       = "redis-user-diagnostics"
  target_resource_id         = azurerm_redis_cache.user_cache.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.user_cache_logs.id

  enabled_log {
    category = "ConnectedClientList"
  }

  metric {
    category = "AllMetrics"
    enabled  = true
  }
}

# Action Group for Alerts
resource "azurerm_monitor_action_group" "user_cache_alerts" {
  name                = "ag-user-cache-prod"
  resource_group_name = azurerm_resource_group.user_cache_rg.name
  short_name          = "usercache"

  email_receiver {
    name          = "admin"
    email_address = "admin@tripgen.com"
  }

  tags = {
    Environment = "Production"
    Service     = "UserService"
    Component   = "Monitoring"
  }
}

# CPU Utilization Alert
resource "azurerm_monitor_metric_alert" "cpu_alert" {
  name                = "redis-user-cpu-alert"
  resource_group_name = azurerm_resource_group.user_cache_rg.name
  scopes              = [azurerm_redis_cache.user_cache.id]
  description         = "User Cache CPU utilization is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.Cache/Redis"
    metric_name      = "percentProcessorTime"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 80
  }

  action {
    action_group_id = azurerm_monitor_action_group.user_cache_alerts.id
  }

  tags = {
    Environment = "Production"
    Service     = "UserService"
    Component   = "Monitoring"
  }
}

# Memory Usage Alert
resource "azurerm_monitor_metric_alert" "memory_alert" {
  name                = "redis-user-memory-alert"
  resource_group_name = azurerm_resource_group.user_cache_rg.name
  scopes              = [azurerm_redis_cache.user_cache.id]
  description         = "User Cache memory usage is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.Cache/Redis"
    metric_name      = "usedmemorypercentage"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 85
  }

  action {
    action_group_id = azurerm_monitor_action_group.user_cache_alerts.id
  }

  tags = {
    Environment = "Production"
    Service     = "UserService"
    Component   = "Monitoring"
  }
}
```

### outputs.tf
```hcl
output "redis_hostname" {
  description = "Redis Cache hostname"
  value       = azurerm_redis_cache.user_cache.hostname
  sensitive   = false
}

output "redis_port" {
  description = "Redis Cache port"
  value       = azurerm_redis_cache.user_cache.ssl_port
  sensitive   = false
}

output "redis_primary_key" {
  description = "Redis Cache primary key"
  value       = azurerm_redis_cache.user_cache.primary_access_key
  sensitive   = true
}

output "key_vault_uri" {
  description = "Key Vault URI"
  value       = azurerm_key_vault.user_cache_kv.vault_uri
  sensitive   = false
}

output "private_endpoint_ip" {
  description = "Private endpoint IP address"
  value       = azurerm_private_endpoint.redis_private_endpoint.private_service_connection[0].private_ip_address
  sensitive   = false
}
```

## 보안 설정

### 1. Private Endpoint 구성
- VNet 내부에서만 액세스 가능
- 퍼블릭 액세스 차단
- Private DNS Zone을 통한 이름 해석

### 2. Key Vault 연동
- 연결 문자열 안전한 저장
- 액세스 키 자동 로테이션 지원
- RBAC 기반 액세스 제어

### 3. 네트워크 보안
- TLS 1.2 강제
- SSL 포트만 사용
- 비SSL 포트 비활성화

## 캐시 샤딩 및 클러스터링 설정

### 1. 샤드 구성
```yaml
샤드 분산 전략:
  - 샤드 0-1: 사용자 세션 데이터
  - 샤드 2-3: 사용자 프로필 데이터
  - 샤드 4: 로그인 제한 데이터
  - 샤드 5: JWT 블랙리스트
```

### 2. 키 분산 패턴
```
세션: user:session:{user_id}
프로필: user:profile:{user_id}
로그인 제한: user:login_limit:{user_id}
JWT 블랙리스트: user:jwt_blacklist:{token_hash}
```

### 3. 클러스터 모니터링
- 샤드별 메모리 사용량 모니터링
- 키 분산 균등성 확인
- 노드 간 동기화 상태 확인

## 모니터링 및 알림 설정

### 1. 핵심 메트릭
- **CPU 사용률**: 임계값 80%
- **메모리 사용률**: 임계값 85%
- **연결 수**: 임계값 1000개
- **응답 시간**: 임계값 10ms

### 2. 로그 모니터링
- 연결된 클라이언트 목록
- 명령 실행 통계
- 오류 및 경고 로그

### 3. 대시보드 구성
- Azure Monitor 대시보드
- 실시간 성능 메트릭
- 히트맵 및 트렌드 분석

## 성능 최적화

### 1. 메모리 정책
```redis
maxmemory-policy: allkeys-lru
maxmemory-reserved: 2048MB
maxmemory-delta: 2048MB
```

### 2. TTL 최적화
```yaml
세션 TTL: 24시간 (86400초)
프로필 TTL: 4시간 (14400초)
로그인 제한 TTL: 1시간 (3600초)
JWT 블랙리스트 TTL: 토큰 만료시간
```

### 3. 연결 풀링
```yaml
최대 연결 수: 1000
연결 타임아웃: 5초
읽기 타임아웃: 3초
```

## 비용 최적화

### 1. 인스턴스 최적화
- **P2 인스턴스**: 26GB 메모리, 적정 성능
- **Zone Redundancy**: 고가용성 대비 비용 효율성
- **예약 인스턴스**: 1년 예약으로 30% 비용 절감

### 2. 백업 최적화
- **일일 백업**: RDB 스냅샷
- **ZRS 스토리지**: 지역 중복성 보장
- **보존 기간**: 30일

### 3. 모니터링 비용
- **Log Analytics**: 30일 보존
- **메트릭 저장**: 90일
- **알림**: 필수 메트릭만 설정

## 배포 가이드

### 1. 사전 준비
```bash
# Terraform 초기화
terraform init

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
    timeout: 3000ms
    password: ${REDIS_PASSWORD}
```

### 2. 캐시 구성 클래스
```java
@Configuration
@EnableCaching
public class UserCacheConfig {
    
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisClusterConfiguration clusterConfig = 
            new RedisClusterConfiguration();
        clusterConfig.clusterNode(redisHost, redisPort);
        
        LettuceClientConfiguration clientConfig = 
            LettuceClientConfiguration.builder()
                .useSsl()
                .build();
                
        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }
}
```

## 운영 가이드

### 1. 일상 모니터링
- 메모리 사용률 확인
- 연결 수 모니터링
- 응답 시간 확인

### 2. 정기 점검
- 주간 성능 리포트 검토
- 백업 상태 확인
- 보안 패치 적용

### 3. 장애 대응
- 알림 수신 시 즉시 대응
- 로그 분석을 통한 원인 파악
- 필요시 스케일링 적용