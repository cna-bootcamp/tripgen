# Trip Service 운영환경 데이터베이스 설치 가이드

## 개요

Trip Service 운영환경을 위한 Azure Database for PostgreSQL Flexible Server 구성 및 배포 가이드입니다.
여행 계획 및 일정 데이터 처리에 최적화된 Zone Redundant 고가용성, 프라이빗 네트워킹, 자동 백업, 모니터링을 포함한 완전한 운영환경 데이터베이스를 구축합니다.

### 주요 특징
- **고가용성**: Zone Redundant 구성으로 99.99% 가용성 보장
- **보안**: Private Endpoint 및 Azure Key Vault 통합
- **백업**: 매일 자동 백업, 35일 보관
- **성능**: 여행 일정 처리에 최적화된 인덱스 및 파티셔닝
- **모니터링**: Azure Monitor 및 Log Analytics 통합

## Azure 리소스 구성

### 리소스 그룹 구조
```
rg-tripgen-trip-prod-koreasouth
├── postgresql-trip-prod-server
├── kv-tripgen-trip-prod
├── vnet-tripgen-trip-prod
├── subnet-db-trip-prod
├── pe-trip-db-prod
└── pdnsz-trip-db-prod
```

### 네트워크 구성
- **VNet CIDR**: 10.2.0.0/16
- **DB Subnet CIDR**: 10.2.1.0/24
- **Private Endpoint Subnet**: 10.2.2.0/24

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
resource "azurerm_resource_group" "trip_prod" {
  name     = "rg-tripgen-trip-prod-koreasouth"
  location = "Korea South"

  tags = {
    Environment = "Production"
    Service     = "Trip"
    Project     = "TripGen"
    ManagedBy   = "Terraform"
  }
}

# Virtual Network
resource "azurerm_virtual_network" "trip_prod" {
  name                = "vnet-tripgen-trip-prod"
  address_space       = ["10.2.0.0/16"]
  location            = azurerm_resource_group.trip_prod.location
  resource_group_name = azurerm_resource_group.trip_prod.name

  tags = azurerm_resource_group.trip_prod.tags
}

# Database Subnet
resource "azurerm_subnet" "db_trip_prod" {
  name                 = "subnet-db-trip-prod"
  resource_group_name  = azurerm_resource_group.trip_prod.name
  virtual_network_name = azurerm_virtual_network.trip_prod.name
  address_prefixes     = ["10.2.1.0/24"]

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
resource "azurerm_subnet" "pe_trip_prod" {
  name                 = "subnet-pe-trip-prod"
  resource_group_name  = azurerm_resource_group.trip_prod.name
  virtual_network_name = azurerm_virtual_network.trip_prod.name
  address_prefixes     = ["10.2.2.0/24"]
}

# Private DNS Zone
resource "azurerm_private_dns_zone" "trip_db_prod" {
  name                = "privatelink.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.trip_prod.name

  tags = azurerm_resource_group.trip_prod.tags
}

# Private DNS Zone VNet Link
resource "azurerm_private_dns_zone_virtual_network_link" "trip_db_prod" {
  name                  = "pdnsz-link-trip-db-prod"
  resource_group_name   = azurerm_resource_group.trip_prod.name
  private_dns_zone_name = azurerm_private_dns_zone.trip_db_prod.name
  virtual_network_id    = azurerm_virtual_network.trip_prod.id

  tags = azurerm_resource_group.trip_prod.tags
}

# PostgreSQL Flexible Server
resource "azurerm_postgresql_flexible_server" "trip_prod" {
  name                   = "postgresql-trip-prod-server"
  resource_group_name    = azurerm_resource_group.trip_prod.name
  location               = azurerm_resource_group.trip_prod.location
  version                = "15"
  delegated_subnet_id    = azurerm_subnet.db_trip_prod.id
  private_dns_zone_id    = azurerm_private_dns_zone.trip_db_prod.id
  administrator_login    = "tripgen_admin"
  administrator_password = random_password.trip_db_password.result
  zone                   = "1"

  storage_mb   = 65536  # 64GB - 여행 데이터 저장을 위한 큰 용량
  storage_tier = "P6"

  sku_name   = "GP_Standard_D2s_v3"  # General Purpose - 더 많은 성능 필요
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

  tags = azurerm_resource_group.trip_prod.tags

  depends_on = [azurerm_private_dns_zone_virtual_network_link.trip_db_prod]
}

# Database
resource "azurerm_postgresql_flexible_server_database" "trip_prod" {
  name      = "tripdb_prod"
  server_id = azurerm_postgresql_flexible_server.trip_prod.id
  collation = "en_US.utf8"
  charset   = "utf8"
}

# Random Password
resource "random_password" "trip_db_password" {
  length  = 32
  special = true
}

# Key Vault
resource "azurerm_key_vault" "trip_prod" {
  name                       = "kv-tripgen-trip-prod"
  location                   = azurerm_resource_group.trip_prod.location
  resource_group_name        = azurerm_resource_group.trip_prod.name
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

  tags = azurerm_resource_group.trip_prod.tags
}

# Key Vault Secret - DB Password
resource "azurerm_key_vault_secret" "trip_db_password" {
  name         = "trip-db-password"
  value        = random_password.trip_db_password.result
  key_vault_id = azurerm_key_vault.trip_prod.id

  tags = azurerm_resource_group.trip_prod.tags
}

# Key Vault Secret - Connection String
resource "azurerm_key_vault_secret" "trip_db_connection" {
  name  = "trip-db-connection-string"
  value = "Server=${azurerm_postgresql_flexible_server.trip_prod.fqdn};Database=${azurerm_postgresql_flexible_server_database.trip_prod.name};Port=5432;User Id=${azurerm_postgresql_flexible_server.trip_prod.administrator_login};Password=${random_password.trip_db_password.result};Ssl Mode=Require;"
  key_vault_id = azurerm_key_vault.trip_prod.id

  tags = azurerm_resource_group.trip_prod.tags
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
  default     = "trip"
}
```

### outputs.tf
```hcl
output "postgresql_server_fqdn" {
  description = "PostgreSQL Server FQDN"
  value       = azurerm_postgresql_flexible_server.trip_prod.fqdn
}

output "postgresql_database_name" {
  description = "PostgreSQL Database Name"
  value       = azurerm_postgresql_flexible_server_database.trip_prod.name
}

output "key_vault_uri" {
  description = "Key Vault URI"
  value       = azurerm_key_vault.trip_prod.vault_uri
}

output "resource_group_name" {
  description = "Resource Group Name"
  value       = azurerm_resource_group.trip_prod.name
}
```

## 보안 설정

### Private Endpoint 구성
```hcl
# Private Endpoint
resource "azurerm_private_endpoint" "trip_db_prod" {
  name                = "pe-trip-db-prod"
  location            = azurerm_resource_group.trip_prod.location
  resource_group_name = azurerm_resource_group.trip_prod.name
  subnet_id           = azurerm_subnet.pe_trip_prod.id

  private_service_connection {
    name                           = "psc-trip-db-prod"
    private_connection_resource_id = azurerm_postgresql_flexible_server.trip_prod.id
    subresource_names              = ["postgresqlServer"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "pdnszg-trip-db-prod"
    private_dns_zone_ids = [azurerm_private_dns_zone.trip_db_prod.id]
  }

  tags = azurerm_resource_group.trip_prod.tags
}
```

### Key Vault 접근 정책 추가
```hcl
# Application Access Policy (Trip Service)
resource "azurerm_key_vault_access_policy" "trip_service_prod" {
  key_vault_id = azurerm_key_vault.trip_prod.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_user_assigned_identity.trip_service_prod.principal_id

  secret_permissions = [
    "Get",
    "List"
  ]
}

# User Assigned Identity for Trip Service
resource "azurerm_user_assigned_identity" "trip_service_prod" {
  name                = "id-trip-service-prod"
  resource_group_name = azurerm_resource_group.trip_prod.name
  location            = azurerm_resource_group.trip_prod.location

  tags = azurerm_resource_group.trip_prod.tags
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
  --resource-group rg-tripgen-trip-prod-koreasouth \
  --name postgresql-trip-prod-server-restored \
  --source-server postgresql-trip-prod-server \
  --restore-time "2024-01-15T10:00:00Z"
```

### 수동 백업 스크립트
```bash
#!/bin/bash
# manual-backup.sh

DB_SERVER="postgresql-trip-prod-server.postgres.database.azure.com"
DB_NAME="tripdb_prod"
DB_USER="tripgen_admin"
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="trip_db_backup_${BACKUP_DATE}.sql"

# Key Vault에서 패스워드 가져오기
DB_PASSWORD=$(az keyvault secret show --vault-name kv-tripgen-trip-prod --name trip-db-password --query value -o tsv)

# 백업 실행
PGPASSWORD=$DB_PASSWORD pg_dump \
  -h $DB_SERVER \
  -U $DB_USER \
  -d $DB_NAME \
  -f $BACKUP_FILE \
  --verbose

# Azure Storage에 업로드
az storage blob upload \
  --account-name tripgentripbackupprod \
  --container-name backups \
  --name $BACKUP_FILE \
  --file $BACKUP_FILE

echo "Backup completed: $BACKUP_FILE"
```

## 모니터링 및 알림 설정

### Log Analytics Workspace
```hcl
resource "azurerm_log_analytics_workspace" "trip_prod" {
  name                = "law-tripgen-trip-prod"
  location            = azurerm_resource_group.trip_prod.location
  resource_group_name = azurerm_resource_group.trip_prod.name
  sku                 = "PerGB2018"
  retention_in_days   = 30

  tags = azurerm_resource_group.trip_prod.tags
}

# Diagnostic Settings
resource "azurerm_monitor_diagnostic_setting" "trip_db_prod" {
  name               = "diag-trip-db-prod"
  target_resource_id = azurerm_postgresql_flexible_server.trip_prod.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.trip_prod.id

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
resource "azurerm_monitor_action_group" "trip_db_alerts" {
  name                = "ag-trip-db-alerts-prod"
  resource_group_name = azurerm_resource_group.trip_prod.name
  short_name          = "tripdb"

  email_receiver {
    name          = "DBA Team"
    email_address = "dba-team@company.com"
  }

  tags = azurerm_resource_group.trip_prod.tags
}

# CPU 사용률 알림
resource "azurerm_monitor_metric_alert" "trip_db_cpu" {
  name                = "alert-trip-db-cpu-prod"
  resource_group_name = azurerm_resource_group.trip_prod.name
  scopes              = [azurerm_postgresql_flexible_server.trip_prod.id]
  description         = "Trip DB CPU usage is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "cpu_percent"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 80
  }

  action {
    action_group_id = azurerm_monitor_action_group.trip_db_alerts.id
  }

  tags = azurerm_resource_group.trip_prod.tags
}

# 스토리지 사용률 알림
resource "azurerm_monitor_metric_alert" "trip_db_storage" {
  name                = "alert-trip-db-storage-prod"
  resource_group_name = azurerm_resource_group.trip_prod.name
  scopes              = [azurerm_postgresql_flexible_server.trip_prod.id]
  description         = "Trip DB storage usage is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "storage_percent"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 85
  }

  action {
    action_group_id = azurerm_monitor_action_group.trip_db_alerts.id
  }

  tags = azurerm_resource_group.trip_prod.tags
}
```

## 성능 튜닝

### PostgreSQL 설정 최적화
```hcl
# Server Parameters - Trip Service에 최적화
resource "azurerm_postgresql_flexible_server_configuration" "trip_shared_buffers" {
  name      = "shared_buffers"
  server_id = azurerm_postgresql_flexible_server.trip_prod.id
  value     = "512MB"  # 더 큰 메모리 할당
}

resource "azurerm_postgresql_flexible_server_configuration" "trip_effective_cache_size" {
  name      = "effective_cache_size"
  server_id = azurerm_postgresql_flexible_server.trip_prod.id
  value     = "2GB"
}

resource "azurerm_postgresql_flexible_server_configuration" "trip_work_mem" {
  name      = "work_mem"
  server_id = azurerm_postgresql_flexible_server.trip_prod.id
  value     = "32MB"  # 복잡한 쿼리를 위한 큰 work_mem
}

resource "azurerm_postgresql_flexible_server_configuration" "trip_maintenance_work_mem" {
  name      = "maintenance_work_mem"
  server_id = azurerm_postgresql_flexible_server.trip_prod.id
  value     = "128MB"
}

resource "azurerm_postgresql_flexible_server_configuration" "trip_checkpoint_completion_target" {
  name      = "checkpoint_completion_target"
  server_id = azurerm_postgresql_flexible_server.trip_prod.id
  value     = "0.9"
}

resource "azurerm_postgresql_flexible_server_configuration" "trip_wal_buffers" {
  name      = "wal_buffers"
  server_id = azurerm_postgresql_flexible_server.trip_prod.id
  value     = "32MB"
}

resource "azurerm_postgresql_flexible_server_configuration" "trip_max_connections" {
  name      = "max_connections"
  server_id = azurerm_postgresql_flexible_server.trip_prod.id
  value     = "200"  # 여행 서비스의 동시 사용자 고려
}
```

### 인덱스 최적화 스크립트
```sql
-- trip_service_indexes.sql
-- Trip Service용 최적화된 인덱스

-- 여행 계획 사용자별 인덱스
CREATE INDEX CONCURRENTLY idx_trip_plans_user_id_status 
ON trip_plans (user_id, status) WHERE deleted_at IS NULL;

-- 여행 계획 날짜별 인덱스
CREATE INDEX CONCURRENTLY idx_trip_plans_date_range 
ON trip_plans (start_date, end_date) WHERE deleted_at IS NULL;

-- 여행 일정 계획별 인덱스
CREATE INDEX CONCURRENTLY idx_trip_schedules_plan_id_day 
ON trip_schedules (trip_plan_id, day_number) WHERE deleted_at IS NULL;

-- 여행 활동 시간별 인덱스
CREATE INDEX CONCURRENTLY idx_trip_activities_schedule_time 
ON trip_activities (trip_schedule_id, start_time) WHERE deleted_at IS NULL;

-- 여행 리뷰 평점 인덱스
CREATE INDEX CONCURRENTLY idx_trip_reviews_rating_created 
ON trip_reviews (rating, created_at) WHERE deleted_at IS NULL;

-- 여행 예산 카테고리별 인덱스
CREATE INDEX CONCURRENTLY idx_trip_budgets_plan_category 
ON trip_budgets (trip_plan_id, category) WHERE deleted_at IS NULL;

-- 파티셔닝을 위한 날짜 기반 인덱스
CREATE INDEX CONCURRENTLY idx_trip_plans_created_month 
ON trip_plans (DATE_TRUNC('month', created_at)) WHERE deleted_at IS NULL;
```

### 테이블 파티셔닝 설정
```sql
-- trip_plans 테이블 월별 파티셔닝
-- 기존 테이블을 파티션 테이블로 변환하는 스크립트

-- 1. 새로운 파티션 테이블 생성
CREATE TABLE trip_plans_partitioned (
    LIKE trip_plans INCLUDING ALL
) PARTITION BY RANGE (created_at);

-- 2. 월별 파티션 생성 (2024년)
CREATE TABLE trip_plans_2024_01 PARTITION OF trip_plans_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
    
CREATE TABLE trip_plans_2024_02 PARTITION OF trip_plans_partitioned
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- ... 추가 월별 파티션 생성

-- 3. 데이터 이전 및 테이블 교체 (운영 시간 외 수행)
-- INSERT INTO trip_plans_partitioned SELECT * FROM trip_plans;
-- ALTER TABLE trip_plans RENAME TO trip_plans_old;
-- ALTER TABLE trip_plans_partitioned RENAME TO trip_plans;
```

## 비용 최적화

### 자동 스케일링 설정
```hcl
# Read Replica for reporting (선택사항)
resource "azurerm_postgresql_flexible_server" "trip_read_replica" {
  count = var.enable_read_replica ? 1 : 0
  
  name                   = "postgresql-trip-prod-replica"
  resource_group_name    = azurerm_resource_group.trip_prod.name
  location               = azurerm_resource_group.trip_prod.location
  create_mode           = "Replica"
  source_server_id      = azurerm_postgresql_flexible_server.trip_prod.id
  
  sku_name = "B_Standard_B2s"  # 리포팅용으로 작은 사이즈

  tags = azurerm_resource_group.trip_prod.tags
}

# Auto-scaling을 위한 메트릭 기반 알림
resource "azurerm_monitor_metric_alert" "trip_db_scale_up" {
  name                = "alert-trip-db-scale-up-prod"
  resource_group_name = azurerm_resource_group.trip_prod.name
  scopes              = [azurerm_postgresql_flexible_server.trip_prod.id]
  description         = "Trip DB needs scaling up"
  severity            = 3

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "cpu_percent"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 70

    dimension {
      name     = "DatabaseName"
      operator = "Include"
      values   = ["tripdb_prod"]
    }
  }

  window_size        = "PT15M"
  frequency          = "PT5M"
  
  action {
    action_group_id = azurerm_monitor_action_group.trip_db_alerts.id
  }

  tags = azurerm_resource_group.trip_prod.tags
}
```

### 비용 모니터링
```hcl
# Budget Alert
resource "azurerm_consumption_budget_resource_group" "trip_prod" {
  name              = "budget-trip-prod"
  resource_group_id = azurerm_resource_group.trip_prod.id

  amount     = 500  # Trip Service는 더 많은 데이터 처리로 높은 예산
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
terraform plan -out=trip-prod.tfplan

# 3. 배포 실행
terraform apply trip-prod.tfplan

# 4. 배저 상태 확인
terraform show
```

### 배포 후 검증
```bash
# 1. 연결 테스트
az postgres flexible-server connect \
  --name postgresql-trip-prod-server \
  --admin-user tripgen_admin \
  --database tripdb_prod

# 2. 상태 확인
az postgres flexible-server show \
  --resource-group rg-tripgen-trip-prod-koreasouth \
  --name postgresql-trip-prod-server

# 3. Key Vault 접근 테스트
az keyvault secret show \
  --vault-name kv-tripgen-trip-prod \
  --name trip-db-password
```

### 데이터베이스 초기 설정
```sql
-- 초기 설정 스크립트 실행
\i trip-service-schema.sql
\i trip-service-indexes.sql
\i trip-service-initial-data.sql

-- 권한 설정
GRANT CONNECT ON DATABASE tripdb_prod TO trip_app_user;
GRANT USAGE ON SCHEMA public TO trip_app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO trip_app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO trip_app_user;
```

## 운영 체크리스트

### 배포 전 체크리스트
- [ ] Azure 구독 및 권한 확인
- [ ] Terraform 버전 확인 (>= 1.0)
- [ ] Azure CLI 로그인 상태 확인
- [ ] 네트워크 대역 충돌 확인
- [ ] 리소스 이름 중복 확인
- [ ] 스토리지 용량 계획 확인

### 배포 후 체크리스트
- [ ] PostgreSQL 서버 상태 확인
- [ ] Private Endpoint 연결 상태 확인
- [ ] Key Vault 시크릿 생성 확인
- [ ] 백업 정책 활성화 확인
- [ ] 모니터링 대시보드 설정 확인
- [ ] 알림 규칙 테스트
- [ ] 애플리케이션 연결 테스트
- [ ] 인덱스 생성 완료 확인
- [ ] 파티셔닝 설정 확인

### 운영 중 점검 항목
- [ ] 일일 백업 상태 확인
- [ ] 성능 메트릭 모니터링
- [ ] 스토리지 사용량 추이 확인
- [ ] 쿼리 성능 분석
- [ ] 파티션 관리 (월별 추가)
- [ ] 보안 업데이트 적용
- [ ] 비용 사용량 검토
- [ ] 로그 분석 및 이슈 대응