# User Service 운영환경 데이터베이스 설치 가이드

## 개요

User Service 운영환경을 위한 Azure Database for PostgreSQL Flexible Server 구성 및 배포 가이드입니다.
Zone Redundant 고가용성, 프라이빗 네트워킹, 자동 백업, 모니터링을 포함한 완전한 운영환경 데이터베이스를 구축합니다.

### 주요 특징
- **고가용성**: Zone Redundant 구성으로 99.99% 가용성 보장
- **보안**: Private Endpoint 및 Azure Key Vault 통합
- **백업**: 매일 자동 백업, 35일 보관
- **성능**: 버스터블 성능 계층으로 효율적인 리소스 사용
- **모니터링**: Azure Monitor 및 Log Analytics 통합

## Azure 리소스 구성

### 리소스 그룹 구조
```
rg-tripgen-user-prod-koreasouth
├── postgresql-user-prod-server
├── kv-tripgen-user-prod
├── vnet-tripgen-user-prod
├── subnet-db-user-prod
├── pe-user-db-prod
└── pdnsz-user-db-prod
```

### 네트워크 구성
- **VNet CIDR**: 10.1.0.0/16
- **DB Subnet CIDR**: 10.1.1.0/24
- **Private Endpoint Subnet**: 10.1.2.0/24

## Terraform 배포 스크립트

### main.tf
```hcl
terraform {
  required_version = ">= 1.0"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
  }
}

provider "azurerm" {
  features {
    key_vault {
      purge_soft_delete_on_destroy = true
    }
  }
}

# Resource Group
resource "azurerm_resource_group" "user_prod" {
  name     = "rg-tripgen-user-prod-koreasouth"
  location = "Korea South"

  tags = {
    Environment = "Production"
    Service     = "User"
    Project     = "TripGen"
    ManagedBy   = "Terraform"
  }
}

# Virtual Network
resource "azurerm_virtual_network" "user_prod" {
  name                = "vnet-tripgen-user-prod"
  address_space       = ["10.1.0.0/16"]
  location            = azurerm_resource_group.user_prod.location
  resource_group_name = azurerm_resource_group.user_prod.name

  tags = azurerm_resource_group.user_prod.tags
}

# Database Subnet
resource "azurerm_subnet" "db_user_prod" {
  name                 = "subnet-db-user-prod"
  resource_group_name  = azurerm_resource_group.user_prod.name
  virtual_network_name = azurerm_virtual_network.user_prod.name
  address_prefixes     = ["10.1.1.0/24"]

  delegation {
    name = "fs"
    service_delegation {
      name = "Microsoft.DBforPostgreSQL/flexibleServers"
      actions = [
        "Microsoft.Network/virtualNetworks/subnets/join/action",
      ]
    }
  }
}

# Private Endpoint Subnet
resource "azurerm_subnet" "pe_user_prod" {
  name                 = "subnet-pe-user-prod"
  resource_group_name  = azurerm_resource_group.user_prod.name
  virtual_network_name = azurerm_virtual_network.user_prod.name
  address_prefixes     = ["10.1.2.0/24"]
}

# Private DNS Zone
resource "azurerm_private_dns_zone" "user_db_prod" {
  name                = "privatelink.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.user_prod.name

  tags = azurerm_resource_group.user_prod.tags
}

# Private DNS Zone VNet Link
resource "azurerm_private_dns_zone_virtual_network_link" "user_db_prod" {
  name                  = "pdnsz-link-user-db-prod"
  resource_group_name   = azurerm_resource_group.user_prod.name
  private_dns_zone_name = azurerm_private_dns_zone.user_db_prod.name
  virtual_network_id    = azurerm_virtual_network.user_prod.id

  tags = azurerm_resource_group.user_prod.tags
}

# PostgreSQL Flexible Server
resource "azurerm_postgresql_flexible_server" "user_prod" {
  name                   = "postgresql-user-prod-server"
  resource_group_name    = azurerm_resource_group.user_prod.name
  location               = azurerm_resource_group.user_prod.location
  version                = "15"
  delegated_subnet_id    = azurerm_subnet.db_user_prod.id
  private_dns_zone_id    = azurerm_private_dns_zone.user_db_prod.id
  administrator_login    = "tripgen_admin"
  administrator_password = random_password.user_db_password.result
  zone                   = "1"

  storage_mb   = 32768
  storage_tier = "P4"

  sku_name   = "B_Standard_B2s"
  backup_retention_days = 35
  geo_redundant_backup_enabled = false

  high_availability {
    mode                      = "ZoneRedundant"
    standby_availability_zone = "2"
  }

  maintenance_window {
    day_of_week  = 0
    start_hour   = 8
    start_minute = 0
  }

  tags = azurerm_resource_group.user_prod.tags

  depends_on = [azurerm_private_dns_zone_virtual_network_link.user_db_prod]
}

# Database
resource "azurerm_postgresql_flexible_server_database" "user_prod" {
  name      = "userdb_prod"
  server_id = azurerm_postgresql_flexible_server.user_prod.id
  collation = "en_US.utf8"
  charset   = "utf8"
}

# Random Password
resource "random_password" "user_db_password" {
  length  = 32
  special = true
}

# Key Vault
resource "azurerm_key_vault" "user_prod" {
  name                       = "kv-tripgen-user-prod"
  location                   = azurerm_resource_group.user_prod.location
  resource_group_name        = azurerm_resource_group.user_prod.name
  tenant_id                  = data.azurerm_client_config.current.tenant_id
  sku_name                   = "standard"
  soft_delete_retention_days = 7
  purge_protection_enabled   = false

  access_policy {
    tenant_id = data.azurerm_client_config.current.tenant_id
    object_id = data.azurerm_client_config.current.object_id

    secret_permissions = [
      "Get", "List", "Set", "Delete", "Recover", "Backup", "Restore"
    ]
  }

  tags = azurerm_resource_group.user_prod.tags
}

# Key Vault Secret - DB Password
resource "azurerm_key_vault_secret" "user_db_password" {
  name         = "user-db-password"
  value        = random_password.user_db_password.result
  key_vault_id = azurerm_key_vault.user_prod.id

  tags = azurerm_resource_group.user_prod.tags
}

# Key Vault Secret - Connection String
resource "azurerm_key_vault_secret" "user_db_connection" {
  name  = "user-db-connection-string"
  value = "Server=${azurerm_postgresql_flexible_server.user_prod.fqdn};Database=${azurerm_postgresql_flexible_server_database.user_prod.name};Port=5432;User Id=${azurerm_postgresql_flexible_server.user_prod.administrator_login};Password=${random_password.user_db_password.result};Ssl Mode=Require;"
  key_vault_id = azurerm_key_vault.user_prod.id

  tags = azurerm_resource_group.user_prod.tags
}

# Data source
data "azurerm_client_config" "current" {}
```

### variables.tf
```hcl
variable "location" {
  description = "Azure region"
  type        = string
  default     = "Korea South"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "prod"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "tripgen"
}

variable "service_name" {
  description = "Service name"
  type        = string
  default     = "user"
}
```

### outputs.tf
```hcl
output "postgresql_server_fqdn" {
  description = "PostgreSQL Server FQDN"
  value       = azurerm_postgresql_flexible_server.user_prod.fqdn
}

output "postgresql_database_name" {
  description = "PostgreSQL Database Name"
  value       = azurerm_postgresql_flexible_server_database.user_prod.name
}

output "key_vault_uri" {
  description = "Key Vault URI"
  value       = azurerm_key_vault.user_prod.vault_uri
}

output "resource_group_name" {
  description = "Resource Group Name"
  value       = azurerm_resource_group.user_prod.name
}
```

## 보안 설정

### Private Endpoint 구성
```hcl
# Private Endpoint
resource "azurerm_private_endpoint" "user_db_prod" {
  name                = "pe-user-db-prod"
  location            = azurerm_resource_group.user_prod.location
  resource_group_name = azurerm_resource_group.user_prod.name
  subnet_id           = azurerm_subnet.pe_user_prod.id

  private_service_connection {
    name                           = "psc-user-db-prod"
    private_connection_resource_id = azurerm_postgresql_flexible_server.user_prod.id
    subresource_names              = ["postgresqlServer"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "pdnszg-user-db-prod"
    private_dns_zone_ids = [azurerm_private_dns_zone.user_db_prod.id]
  }

  tags = azurerm_resource_group.user_prod.tags
}
```

### Key Vault 접근 정책 추가
```hcl
# Application Access Policy (User Service)
resource "azurerm_key_vault_access_policy" "user_service_prod" {
  key_vault_id = azurerm_key_vault.user_prod.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_user_assigned_identity.user_service_prod.principal_id

  secret_permissions = [
    "Get",
    "List"
  ]
}

# User Assigned Identity for User Service
resource "azurerm_user_assigned_identity" "user_service_prod" {
  name                = "id-user-service-prod"
  resource_group_name = azurerm_resource_group.user_prod.name
  location            = azurerm_resource_group.user_prod.location

  tags = azurerm_resource_group.user_prod.tags
}
```

## 백업 및 복구 정책

### 자동 백업 설정
- **백업 주기**: 매일 자동 백업
- **보관 기간**: 35일
- **백업 시간**: UTC 08:00 (한국시간 17:00)

### Point-in-Time Recovery
```bash
# 특정 시점으로 복구 (Azure CLI)
az postgres flexible-server restore \
  --resource-group rg-tripgen-user-prod-koreasouth \
  --name postgresql-user-prod-server-restored \
  --source-server postgresql-user-prod-server \
  --restore-time "2024-01-15T10:00:00Z"
```

### 수동 백업 스크립트
```bash
#!/bin/bash
# manual-backup.sh

DB_SERVER="postgresql-user-prod-server.postgres.database.azure.com"
DB_NAME="userdb_prod"
DB_USER="tripgen_admin"
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="user_db_backup_${BACKUP_DATE}.sql"

# Key Vault에서 패스워드 가져오기
DB_PASSWORD=$(az keyvault secret show --vault-name kv-tripgen-user-prod --name user-db-password --query value -o tsv)

# 백업 실행
PGPASSWORD=$DB_PASSWORD pg_dump \
  -h $DB_SERVER \
  -U $DB_USER \
  -d $DB_NAME \
  -f $BACKUP_FILE \
  --verbose

# Azure Storage에 업로드
az storage blob upload \
  --account-name tripgenuserbackupprod \
  --container-name backups \
  --name $BACKUP_FILE \
  --file $BACKUP_FILE

echo "Backup completed: $BACKUP_FILE"
```

## 모니터링 및 알림 설정

### Log Analytics Workspace
```hcl
resource "azurerm_log_analytics_workspace" "user_prod" {
  name                = "law-tripgen-user-prod"
  location            = azurerm_resource_group.user_prod.location
  resource_group_name = azurerm_resource_group.user_prod.name
  sku                 = "PerGB2018"
  retention_in_days   = 30

  tags = azurerm_resource_group.user_prod.tags
}

# Diagnostic Settings
resource "azurerm_monitor_diagnostic_setting" "user_db_prod" {
  name               = "diag-user-db-prod"
  target_resource_id = azurerm_postgresql_flexible_server.user_prod.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.user_prod.id

  enabled_log {
    category = "PostgreSQLLogs"
  }

  metric {
    category = "AllMetrics"
    enabled  = true
  }
}
```

### 알림 규칙
```hcl
# Action Group
resource "azurerm_monitor_action_group" "user_db_alerts" {
  name                = "ag-user-db-alerts-prod"
  resource_group_name = azurerm_resource_group.user_prod.name
  short_name          = "userdb"

  email_receiver {
    name          = "DBA Team"
    email_address = "dba-team@company.com"
  }

  tags = azurerm_resource_group.user_prod.tags
}

# CPU 사용률 알림
resource "azurerm_monitor_metric_alert" "user_db_cpu" {
  name                = "alert-user-db-cpu-prod"
  resource_group_name = azurerm_resource_group.user_prod.name
  scopes              = [azurerm_postgresql_flexible_server.user_prod.id]
  description         = "User DB CPU usage is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "cpu_percent"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 80
  }

  action {
    action_group_id = azurerm_monitor_action_group.user_db_alerts.id
  }

  tags = azurerm_resource_group.user_prod.tags
}

# 연결 실패 알림
resource "azurerm_monitor_metric_alert" "user_db_connection_failed" {
  name                = "alert-user-db-connection-failed-prod"
  resource_group_name = azurerm_resource_group.user_prod.name
  scopes              = [azurerm_postgresql_flexible_server.user_prod.id]
  description         = "User DB connection failures detected"
  severity            = 1

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "connections_failed"
    aggregation      = "Total"
    operator         = "GreaterThan"
    threshold        = 10
  }

  action {
    action_group_id = azurerm_monitor_action_group.user_db_alerts.id
  }

  tags = azurerm_resource_group.user_prod.tags
}
```

## 성능 튜닝

### PostgreSQL 설정 최적화
```hcl
# Server Parameters
resource "azurerm_postgresql_flexible_server_configuration" "user_shared_buffers" {
  name      = "shared_buffers"
  server_id = azurerm_postgresql_flexible_server.user_prod.id
  value     = "256MB"
}

resource "azurerm_postgresql_flexible_server_configuration" "user_effective_cache_size" {
  name      = "effective_cache_size"
  server_id = azurerm_postgresql_flexible_server.user_prod.id
  value     = "1GB"
}

resource "azurerm_postgresql_flexible_server_configuration" "user_maintenance_work_mem" {
  name      = "maintenance_work_mem"
  server_id = azurerm_postgresql_flexible_server.user_prod.id
  value     = "64MB"
}

resource "azurerm_postgresql_flexible_server_configuration" "user_checkpoint_completion_target" {
  name      = "checkpoint_completion_target"
  server_id = azurerm_postgresql_flexible_server.user_prod.id
  value     = "0.9"
}

resource "azurerm_postgresql_flexible_server_configuration" "user_wal_buffers" {
  name      = "wal_buffers"
  server_id = azurerm_postgresql_flexible_server.user_prod.id
  value     = "16MB"
}

resource "azurerm_postgresql_flexible_server_configuration" "user_default_statistics_target" {
  name      = "default_statistics_target"
  server_id = azurerm_postgresql_flexible_server.user_prod.id
  value     = "100"
}
```

### 인덱스 최적화 스크립트
```sql
-- user_service_indexes.sql
-- User Service용 최적화된 인덱스

-- 사용자 이메일 인덱스 (고유)
CREATE UNIQUE INDEX CONCURRENTLY idx_users_email_unique 
ON users (email) WHERE deleted_at IS NULL;

-- 사용자 상태 인덱스
CREATE INDEX CONCURRENTLY idx_users_status 
ON users (status) WHERE deleted_at IS NULL;

-- 사용자 생성일자 인덱스
CREATE INDEX CONCURRENTLY idx_users_created_at 
ON users (created_at) WHERE deleted_at IS NULL;

-- 사용자 프로필 조합 인덱스
CREATE INDEX CONCURRENTLY idx_user_profiles_user_type 
ON user_profiles (user_id, profile_type) WHERE deleted_at IS NULL;

-- 사용자 선호도 인덱스
CREATE INDEX CONCURRENTLY idx_user_preferences_user_category 
ON user_preferences (user_id, preference_category) WHERE deleted_at IS NULL;
```

## 비용 최적화

### 자동 스케일링 설정
```hcl
# Auto-scaling 정책 (향후 확장용)
resource "azurerm_postgresql_flexible_server_configuration" "user_autovacuum" {
  name      = "autovacuum"
  server_id = azurerm_postgresql_flexible_server.user_prod.id
  value     = "on"
}

resource "azurerm_postgresql_flexible_server_configuration" "user_autovacuum_scale_factor" {
  name      = "autovacuum_vacuum_scale_factor"
  server_id = azurerm_postgresql_flexible_server.user_prod.id
  value     = "0.1"
}
```

### 비용 모니터링
```hcl
# Budget Alert
resource "azurerm_consumption_budget_resource_group" "user_prod" {
  name              = "budget-user-prod"
  resource_group_id = azurerm_resource_group.user_prod.id

  amount     = 200
  time_grain = "Monthly"

  time_period {
    start_date = "2024-01-01T00:00:00Z"
    end_date   = "2025-12-31T23:59:59Z"
  }

  notification {
    enabled   = true
    threshold = 80
    operator  = "GreaterThan"

    contact_emails = [
      "admin@company.com",
      "billing@company.com"
    ]
  }

  notification {
    enabled   = true
    threshold = 100
    operator  = "GreaterThan"

    contact_emails = [
      "admin@company.com",
      "billing@company.com"
    ]
  }
}
```

## 배포 명령어

### Terraform 배포
```bash
# 1. 초기화
terraform init

# 2. 계획 확인
terraform plan -out=user-prod.tfplan

# 3. 배포 실행
terraform apply user-prod.tfplan

# 4. 배포 상태 확인
terraform show
```

### 배포 후 검증
```bash
# 1. 연결 테스트
az postgres flexible-server connect \
  --name postgresql-user-prod-server \
  --admin-user tripgen_admin \
  --database userdb_prod

# 2. 상태 확인
az postgres flexible-server show \
  --resource-group rg-tripgen-user-prod-koreasouth \
  --name postgresql-user-prod-server

# 3. Key Vault 접근 테스트
az keyvault secret show \
  --vault-name kv-tripgen-user-prod \
  --name user-db-password
```

## 운영 체크리스트

### 배포 전 체크리스트
- [ ] Azure 구독 및 권한 확인
- [ ] Terraform 버전 확인 (>= 1.0)
- [ ] Azure CLI 로그인 상태 확인
- [ ] 네트워크 대역 충돌 확인
- [ ] 리소스 이름 중복 확인

### 배포 후 체크리스트
- [ ] PostgreSQL 서버 상태 확인
- [ ] Private Endpoint 연결 상태 확인
- [ ] Key Vault 시크릿 생성 확인
- [ ] 백업 정책 활성화 확인
- [ ] 모니터링 대시보드 설정 확인
- [ ] 알림 규칙 테스트
- [ ] 애플리케이션 연결 테스트

### 운영 중 점검 항목
- [ ] 일일 백업 상태 확인
- [ ] 성능 메트릭 모니터링
- [ ] 보안 업데이트 적용
- [ ] 비용 사용량 검토
- [ ] 로그 분석 및 이슈 대응