# AI Service 운영환경 데이터베이스 설치 가이드

## 개요

AI Service 운영환경을 위한 Azure Database for PostgreSQL Flexible Server 구성 및 배포 가이드입니다.
AI 모델 결과, 학습 데이터, 추천 정보 처리에 최적화된 Zone Redundant 고가용성, 프라이빗 네트워킹, 자동 백업, 모니터링을 포함한 완전한 운영환경 데이터베이스를 구축합니다.

### 주요 특징
- **고가용성**: Zone Redundant 구성으로 99.99% 가용성 보장
- **보안**: Private Endpoint 및 Azure Key Vault 통합
- **백업**: 매일 자동 백업, 35일 보관
- **성능**: AI 데이터 처리에 최적화된 벡터 검색 및 JSON 인덱스
- **확장성**: AI 모델 결과 증가에 대비한 스토리지 최적화
- **모니터링**: Azure Monitor 및 Log Analytics 통합

## Azure 리소스 구성

### 리소스 그룹 구조
```
rg-tripgen-ai-prod-koreasouth
├── postgresql-ai-prod-server
├── kv-tripgen-ai-prod
├── vnet-tripgen-ai-prod
├── subnet-db-ai-prod
├── pe-ai-db-prod
└── pdnsz-ai-db-prod
```

### 네트워크 구성
- **VNet CIDR**: 10.3.0.0/16
- **DB Subnet CIDR**: 10.3.1.0/24
- **Private Endpoint Subnet**: 10.3.2.0/24

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
resource "azurerm_resource_group" "ai_prod" {
  name     = "rg-tripgen-ai-prod-koreasouth"
  location = "Korea South"

  tags = {
    Environment = "Production"
    Service     = "AI"
    Project     = "TripGen"
    ManagedBy   = "Terraform"
  }
}

# Virtual Network
resource "azurerm_virtual_network" "ai_prod" {
  name                = "vnet-tripgen-ai-prod"
  address_space       = ["10.3.0.0/16"]
  location            = azurerm_resource_group.ai_prod.location
  resource_group_name = azurerm_resource_group.ai_prod.name

  tags = azurerm_resource_group.ai_prod.tags
}

# Database Subnet
resource "azurerm_subnet" "db_ai_prod" {
  name                 = "subnet-db-ai-prod"
  resource_group_name  = azurerm_resource_group.ai_prod.name
  virtual_network_name = azurerm_virtual_network.ai_prod.name
  address_prefixes     = ["10.3.1.0/24"]

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
resource "azurerm_subnet" "pe_ai_prod" {
  name                 = "subnet-pe-ai-prod"
  resource_group_name  = azurerm_resource_group.ai_prod.name
  virtual_network_name = azurerm_virtual_network.ai_prod.name
  address_prefixes     = ["10.3.2.0/24"]
}

# Private DNS Zone
resource "azurerm_private_dns_zone" "ai_db_prod" {
  name                = "privatelink.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.ai_prod.name

  tags = azurerm_resource_group.ai_prod.tags
}

# Private DNS Zone VNet Link
resource "azurerm_private_dns_zone_virtual_network_link" "ai_db_prod" {
  name                  = "pdnsz-link-ai-db-prod"
  resource_group_name   = azurerm_resource_group.ai_prod.name
  private_dns_zone_name = azurerm_private_dns_zone.ai_db_prod.name
  virtual_network_id    = azurerm_virtual_network.ai_prod.id

  tags = azurerm_resource_group.ai_prod.tags
}

# PostgreSQL Flexible Server
resource "azurerm_postgresql_flexible_server" "ai_prod" {
  name                   = "postgresql-ai-prod-server"
  resource_group_name    = azurerm_resource_group.ai_prod.name
  location               = azurerm_resource_group.ai_prod.location
  version                = "15"
  delegated_subnet_id    = azurerm_subnet.db_ai_prod.id
  private_dns_zone_id    = azurerm_private_dns_zone.ai_db_prod.id
  administrator_login    = "tripgen_admin"
  administrator_password = random_password.ai_db_password.result
  zone                   = "1"

  storage_mb   = 131072  # 128GB - AI 데이터 및 모델 결과 저장
  storage_tier = "P10"

  sku_name   = "GP_Standard_D4s_v3"  # 4 vCore - AI 연산 처리
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

  tags = azurerm_resource_group.ai_prod.tags

  depends_on = [azurerm_private_dns_zone_virtual_network_link.ai_db_prod]
}

# Database
resource "azurerm_postgresql_flexible_server_database" "ai_prod" {
  name      = "aidb_prod"
  server_id = azurerm_postgresql_flexible_server.ai_prod.id
  collation = "en_US.utf8"
  charset   = "utf8"
}

# Random Password
resource "random_password" "ai_db_password" {
  length  = 32
  special = true
}

# Key Vault
resource "azurerm_key_vault" "ai_prod" {
  name                       = "kv-tripgen-ai-prod"
  location                   = azurerm_resource_group.ai_prod.location
  resource_group_name        = azurerm_resource_group.ai_prod.name
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

  tags = azurerm_resource_group.ai_prod.tags
}

# Key Vault Secret - DB Password
resource "azurerm_key_vault_secret" "ai_db_password" {
  name         = "ai-db-password"
  value        = random_password.ai_db_password.result
  key_vault_id = azurerm_key_vault.ai_prod.id

  tags = azurerm_resource_group.ai_prod.tags
}

# Key Vault Secret - Connection String
resource "azurerm_key_vault_secret" "ai_db_connection" {
  name  = "ai-db-connection-string"
  value = "Server=${azurerm_postgresql_flexible_server.ai_prod.fqdn};Database=${azurerm_postgresql_flexible_server_database.ai_prod.name};Port=5432;User Id=${azurerm_postgresql_flexible_server.ai_prod.administrator_login};Password=${random_password.ai_db_password.result};Ssl Mode=Require;"
  key_vault_id = azurerm_key_vault.ai_prod.id

  tags = azurerm_resource_group.ai_prod.tags
}

# Key Vault Secret - OpenAI API Key (예시)
resource "azurerm_key_vault_secret" "openai_api_key" {
  name         = "openai-api-key"
  value        = var.openai_api_key
  key_vault_id = azurerm_key_vault.ai_prod.id

  tags = azurerm_resource_group.ai_prod.tags
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
  default     = "ai"
}

variable "openai_api_key" {
  description = "OpenAI API Key"
  type        = string
  sensitive   = true
}
```

### outputs.tf
```hcl
output "postgresql_server_fqdn" {
  description = "PostgreSQL Server FQDN"
  value       = azurerm_postgresql_flexible_server.ai_prod.fqdn
}

output "postgresql_database_name" {
  description = "PostgreSQL Database Name"
  value       = azurerm_postgresql_flexible_server_database.ai_prod.name
}

output "key_vault_uri" {
  description = "Key Vault URI"
  value       = azurerm_key_vault.ai_prod.vault_uri
}

output "resource_group_name" {
  description = "Resource Group Name"
  value       = azurerm_resource_group.ai_prod.name
}
```

## 보안 설정

### Private Endpoint 구성
```hcl
# Private Endpoint
resource "azurerm_private_endpoint" "ai_db_prod" {
  name                = "pe-ai-db-prod"
  location            = azurerm_resource_group.ai_prod.location
  resource_group_name = azurerm_resource_group.ai_prod.name
  subnet_id           = azurerm_subnet.pe_ai_prod.id

  private_service_connection {
    name                           = "psc-ai-db-prod"
    private_connection_resource_id = azurerm_postgresql_flexible_server.ai_prod.id
    subresource_names              = ["postgresqlServer"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "pdnszg-ai-db-prod"
    private_dns_zone_ids = [azurerm_private_dns_zone.ai_db_prod.id]
  }

  tags = azurerm_resource_group.ai_prod.tags
}
```

### Key Vault 접근 정책 추가
```hcl
# Application Access Policy (AI Service)
resource "azurerm_key_vault_access_policy" "ai_service_prod" {
  key_vault_id = azurerm_key_vault.ai_prod.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_user_assigned_identity.ai_service_prod.principal_id

  secret_permissions = [
    "Get",
    "List"
  ]
}

# User Assigned Identity for AI Service
resource "azurerm_user_assigned_identity" "ai_service_prod" {
  name                = "id-ai-service-prod"
  resource_group_name = azurerm_resource_group.ai_prod.name
  location            = azurerm_resource_group.ai_prod.location

  tags = azurerm_resource_group.ai_prod.tags
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
  --resource-group rg-tripgen-ai-prod-koreasouth \
  --name postgresql-ai-prod-server-restored \
  --source-server postgresql-ai-prod-server \
  --restore-time "2024-01-15T10:00:00Z"
```

### 수동 백업 스크립트
```bash
#!/bin/bash
# manual-backup.sh

DB_SERVER="postgresql-ai-prod-server.postgres.database.azure.com"
DB_NAME="aidb_prod"
DB_USER="tripgen_admin"
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="ai_db_backup_${BACKUP_DATE}.sql"

# Key Vault에서 패스워드 가져오기
DB_PASSWORD=$(az keyvault secret show --vault-name kv-tripgen-ai-prod --name ai-db-password --query value -o tsv)

# 백업 실행
PGPASSWORD=$DB_PASSWORD pg_dump \
  -h $DB_SERVER \
  -U $DB_USER \
  -d $DB_NAME \
  -f $BACKUP_FILE \
  --verbose

# Azure Storage에 업로드
az storage blob upload \
  --account-name tripgenaibackupprod \
  --container-name backups \
  --name $BACKUP_FILE \
  --file $BACKUP_FILE

echo "Backup completed: $BACKUP_FILE"
```

## 모니터링 및 알림 설정

### Log Analytics Workspace
```hcl
resource "azurerm_log_analytics_workspace" "ai_prod" {
  name                = "law-tripgen-ai-prod"
  location            = azurerm_resource_group.ai_prod.location
  resource_group_name = azurerm_resource_group.ai_prod.name
  sku                 = "PerGB2018"
  retention_in_days   = 30

  tags = azurerm_resource_group.ai_prod.tags
}

# Diagnostic Settings
resource "azurerm_monitor_diagnostic_setting" "ai_db_prod" {
  name               = "diag-ai-db-prod"
  target_resource_id = azurerm_postgresql_flexible_server.ai_prod.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.ai_prod.id

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
resource "azurerm_monitor_action_group" "ai_db_alerts" {
  name                = "ag-ai-db-alerts-prod"
  resource_group_name = azurerm_resource_group.ai_prod.name
  short_name          = "aidb"

  email_receiver {
    name          = "AI Team"
    email_address = "ai-team@company.com"
  }

  email_receiver {
    name          = "DBA Team"
    email_address = "dba-team@company.com"
  }

  tags = azurerm_resource_group.ai_prod.tags
}

# CPU 사용률 알림
resource "azurerm_monitor_metric_alert" "ai_db_cpu" {
  name                = "alert-ai-db-cpu-prod"
  resource_group_name = azurerm_resource_group.ai_prod.name
  scopes              = [azurerm_postgresql_flexible_server.ai_prod.id]
  description         = "AI DB CPU usage is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "cpu_percent"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 75  # AI 처리 특성상 더 낮은 임계값
  }

  action {
    action_group_id = azurerm_monitor_action_group.ai_db_alerts.id
  }

  tags = azurerm_resource_group.ai_prod.tags
}

# 메모리 사용률 알림
resource "azurerm_monitor_metric_alert" "ai_db_memory" {
  name                = "alert-ai-db-memory-prod"
  resource_group_name = azurerm_resource_group.ai_prod.name
  scopes              = [azurerm_postgresql_flexible_server.ai_prod.id]
  description         = "AI DB memory usage is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "memory_percent"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 85
  }

  action {
    action_group_id = azurerm_monitor_action_group.ai_db_alerts.id
  }

  tags = azurerm_resource_group.ai_prod.tags
}

# 장시간 실행 쿼리 알림
resource "azurerm_monitor_metric_alert" "ai_db_long_queries" {
  name                = "alert-ai-db-long-queries-prod"
  resource_group_name = azurerm_resource_group.ai_prod.name
  scopes              = [azurerm_postgresql_flexible_server.ai_prod.id]
  description         = "AI DB has long running queries"
  severity            = 3

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "active_connections"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 50
  }

  window_size = "PT30M"
  frequency   = "PT5M"

  action {
    action_group_id = azurerm_monitor_action_group.ai_db_alerts.id
  }

  tags = azurerm_resource_group.ai_prod.tags
}
```

## 성능 튜닝

### PostgreSQL 설정 최적화 (AI 워크로드 특화)
```hcl
# Server Parameters - AI Service에 최적화
resource "azurerm_postgresql_flexible_server_configuration" "ai_shared_buffers" {
  name      = "shared_buffers"
  server_id = azurerm_postgresql_flexible_server.ai_prod.id
  value     = "1GB"  # AI 데이터 캐싱을 위한 큰 버퍼
}

resource "azurerm_postgresql_flexible_server_configuration" "ai_effective_cache_size" {
  name      = "effective_cache_size"
  server_id = azurerm_postgresql_flexible_server.ai_prod.id
  value     = "3GB"
}

resource "azurerm_postgresql_flexible_server_configuration" "ai_work_mem" {
  name      = "work_mem"
  server_id = azurerm_postgresql_flexible_server.ai_prod.id
  value     = "64MB"  # 벡터 연산을 위한 큰 work_mem
}

resource "azurerm_postgresql_flexible_server_configuration" "ai_maintenance_work_mem" {
  name      = "maintenance_work_mem"
  server_id = azurerm_postgresql_flexible_server.ai_prod.id
  value     = "256MB"
}

resource "azurerm_postgresql_flexible_server_configuration" "ai_random_page_cost" {
  name      = "random_page_cost"
  server_id = azurerm_postgresql_flexible_server.ai_prod.id
  value     = "1.1"  # SSD 최적화
}

resource "azurerm_postgresql_flexible_server_configuration" "ai_effective_io_concurrency" {
  name      = "effective_io_concurrency"
  server_id = azurerm_postgresql_flexible_server.ai_prod.id
  value     = "200"
}

resource "azurerm_postgresql_flexible_server_configuration" "ai_max_connections" {
  name      = "max_connections"
  server_id = azurerm_postgresql_flexible_server.ai_prod.id
  value     = "150"  # AI 서비스 특성상 적은 동시 연결
}

# JSON 처리 최적화
resource "azurerm_postgresql_flexible_server_configuration" "ai_jit" {
  name      = "jit"
  server_id = azurerm_postgresql_flexible_server.ai_prod.id
  value     = "on"
}
```

### PostgreSQL Extensions 설치
```sql
-- AI Service용 필요 확장 모듈 설치
-- extensions.sql

-- JSON 처리 및 전문 검색
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "btree_gin";
CREATE EXTENSION IF NOT EXISTS "btree_gist";

-- 벡터 유사도 검색 (향후 확장용)
-- CREATE EXTENSION IF NOT EXISTS "vector";

-- UUID 생성
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 암호화
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

### 인덱스 최적화 스크립트 (AI 특화)
```sql
-- ai_service_indexes.sql
-- AI Service용 최적화된 인덱스

-- AI 모델 결과 인덱스
CREATE INDEX CONCURRENTLY idx_ai_model_results_model_type_created 
ON ai_model_results (model_type, created_at) WHERE deleted_at IS NULL;

-- AI 추천 결과 사용자별 인덱스
CREATE INDEX CONCURRENTLY idx_ai_recommendations_user_type_score 
ON ai_recommendations (user_id, recommendation_type, confidence_score DESC) 
WHERE deleted_at IS NULL;

-- AI 학습 데이터 상태별 인덱스
CREATE INDEX CONCURRENTLY idx_ai_training_data_status_created 
ON ai_training_data (status, created_at) WHERE deleted_at IS NULL;

-- JSON 데이터 인덱스 (GIN)
CREATE INDEX CONCURRENTLY idx_ai_model_results_metadata_gin 
ON ai_model_results USING gin (metadata) WHERE deleted_at IS NULL;

-- 벡터 임베딩 인덱스 (향후 확장용)
-- CREATE INDEX CONCURRENTLY idx_ai_embeddings_vector_cosine 
-- ON ai_embeddings USING ivfflat (embedding vector_cosine_ops) 
-- WITH (lists = 100);

-- AI 성능 메트릭 인덱스
CREATE INDEX CONCURRENTLY idx_ai_metrics_model_timestamp 
ON ai_performance_metrics (model_id, recorded_at) WHERE deleted_at IS NULL;

-- 텍스트 검색 인덱스
CREATE INDEX CONCURRENTLY idx_ai_content_text_search 
ON ai_content_analysis USING gin (to_tsvector('english', content)) 
WHERE deleted_at IS NULL;

-- 복합 인덱스 - 추천 시스템용
CREATE INDEX CONCURRENTLY idx_ai_recommendations_complex 
ON ai_recommendations (user_id, recommendation_type, status, created_at) 
WHERE deleted_at IS NULL;
```

### 테이블 파티셀닝 설정 (AI 데이터)
```sql
-- ai_model_results 테이블 월별 파티셔닝
-- AI 모델 결과는 시간에 따라 빠르게 증가하므로 파티셔닝 필요

-- 1. 새로운 파티션 테이블 생성
CREATE TABLE ai_model_results_partitioned (
    LIKE ai_model_results INCLUDING ALL
) PARTITION BY RANGE (created_at);

-- 2. 월별 파티션 생성 (2024년)
CREATE TABLE ai_model_results_2024_01 PARTITION OF ai_model_results_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
    
CREATE TABLE ai_model_results_2024_02 PARTITION OF ai_model_results_partitioned
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- ... 추가 월별 파티션 생성

-- 3. 자동 파티션 생성 함수
CREATE OR REPLACE FUNCTION create_monthly_partition(table_name text, start_date date)
RETURNS void AS $$
DECLARE
    partition_name text;
    end_date date;
BEGIN
    partition_name := table_name || '_' || to_char(start_date, 'YYYY_MM');
    end_date := start_date + interval '1 month';
    
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF %I
                    FOR VALUES FROM (%L) TO (%L)',
                   partition_name, table_name || '_partitioned', start_date, end_date);
END;
$$ LANGUAGE plpgsql;
```

## 비용 최적화

### AI 특화 스토리지 최적화
```hcl
# AI 데이터 아카이빙을 위한 Storage Account
resource "azurerm_storage_account" "ai_archive" {
  name                     = "tripgenaiarchiveprod"
  resource_group_name      = azurerm_resource_group.ai_prod.name
  location                 = azurerm_resource_group.ai_prod.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  access_tier              = "Cool"  # 비용 최적화를 위한 Cool 티어

  tags = azurerm_resource_group.ai_prod.tags
}

# 컨테이너 생성
resource "azurerm_storage_container" "ai_model_archive" {
  name                  = "ai-model-archive"
  storage_account_name  = azurerm_storage_account.ai_archive.name
  container_access_type = "private"
}

# 자동 스케일링을 위한 메트릭 기반 알림
resource "azurerm_monitor_metric_alert" "ai_db_scale_up" {
  name                = "alert-ai-db-scale-up-prod"
  resource_group_name = azurerm_resource_group.ai_prod.name
  scopes              = [azurerm_postgresql_flexible_server.ai_prod.id]
  description         = "AI DB needs scaling up based on CPU and memory"
  severity            = 3

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "cpu_percent"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 70
  }

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "memory_percent"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 80
  }

  window_size        = "PT15M"
  frequency          = "PT5M"
  
  action {
    action_group_id = azurerm_monitor_action_group.ai_db_alerts.id
  }

  tags = azurerm_resource_group.ai_prod.tags
}
```

### 데이터 생명주기 관리
```sql
-- AI 데이터 생명주기 관리 함수
-- cleanup_old_ai_data.sql

CREATE OR REPLACE FUNCTION cleanup_old_ai_data()
RETURNS void AS $$
BEGIN
    -- 6개월 이상 된 AI 모델 결과 아카이브
    INSERT INTO ai_model_results_archive 
    SELECT * FROM ai_model_results 
    WHERE created_at < CURRENT_DATE - INTERVAL '6 months'
    AND status = 'completed';
    
    -- 아카이브된 데이터 삭제
    DELETE FROM ai_model_results 
    WHERE created_at < CURRENT_DATE - INTERVAL '6 months'
    AND status = 'completed';
    
    -- 1년 이상 된 학습 로그 삭제
    DELETE FROM ai_training_logs 
    WHERE created_at < CURRENT_DATE - INTERVAL '1 year';
    
    -- 통계 업데이트
    ANALYZE ai_model_results;
    ANALYZE ai_recommendations;
    
    RAISE NOTICE 'AI data cleanup completed';
END;
$$ LANGUAGE plpgsql;

-- 정기 실행을 위한 cron 작업 (pg_cron 확장 필요)
-- SELECT cron.schedule('ai-data-cleanup', '0 2 1 * *', 'SELECT cleanup_old_ai_data();');
```

### 비용 모니터링
```hcl
# Budget Alert
resource "azurerm_consumption_budget_resource_group" "ai_prod" {
  name              = "budget-ai-prod"
  resource_group_id = azurerm_resource_group.ai_prod.id

  amount     = 800  # AI Service는 컴퓨팅 비용 때문에 높은 예산
  time_grain = "Monthly"

  time_period {
    start_date = "2024-01-01T00:00:00Z"
    end_date   = "2025-12-31T23:59:59Z"
  }

  notification {
    enabled   = true
    threshold = 75
    operator  = "GreaterThan"

    contact_emails = [
      "admin@company.com",
      "ai-team@company.com",
      "billing@company.com"
    ]
  }

  notification {
    enabled   = true
    threshold = 90
    operator  = "GreaterThan"

    contact_emails = [
      "admin@company.com",
      "ai-team@company.com",
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

# 2. 계획 확인 (민감한 변수 포함)
terraform plan -var="openai_api_key=${OPENAI_API_KEY}" -out=ai-prod.tfplan

# 3. 배포 실행
terraform apply ai-prod.tfplan

# 4. 배포 상태 확인
terraform show
```

### 배포 후 검증
```bash
# 1. 연결 테스트
az postgres flexible-server connect \
  --name postgresql-ai-prod-server \
  --admin-user tripgen_admin \
  --database aidb_prod

# 2. 상태 확인
az postgres flexible-server show \
  --resource-group rg-tripgen-ai-prod-koreasouth \
  --name postgresql-ai-prod-server

# 3. Key Vault 접근 테스트
az keyvault secret show \
  --vault-name kv-tripgen-ai-prod \
  --name ai-db-password
```

### 데이터베이스 초기 설정
```sql
-- 초기 설정 스크립트 실행
\i extensions.sql
\i ai-service-schema.sql
\i ai-service-indexes.sql
\i ai-service-partitions.sql
\i ai-service-functions.sql

-- 권한 설정
GRANT CONNECT ON DATABASE aidb_prod TO ai_app_user;
GRANT USAGE ON SCHEMA public TO ai_app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO ai_app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ai_app_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO ai_app_user;
```

## 운영 체크리스트

### 배포 전 체크리스트
- [ ] Azure 구독 및 권한 확인
- [ ] Terraform 버전 확인 (>= 1.0)
- [ ] Azure CLI 로그인 상태 확인
- [ ] OpenAI API Key 준비
- [ ] 네트워크 대역 충돌 확인
- [ ] 리소스 이름 중복 확인
- [ ] 스토리지 용량 계획 확인 (AI 데이터 증가율 고려)

### 배포 후 체크리스트
- [ ] PostgreSQL 서버 상태 확인
- [ ] Private Endpoint 연결 상태 확인
- [ ] Key Vault 시크릿 생성 확인 (DB + OpenAI API Key)
- [ ] 백업 정책 활성화 확인
- [ ] 모니터링 대시보드 설정 확인
- [ ] 알림 규칙 테스트
- [ ] 애플리케이션 연결 테스트
- [ ] PostgreSQL 확장 모듈 설치 확인
- [ ] 인덱스 생성 완료 확인
- [ ] 파티셔닝 설정 확인
- [ ] AI 모델 연동 테스트

### 운영 중 점검 항목
- [ ] 일일 백업 상태 확인
- [ ] AI 모델 성능 메트릭 모니터링
- [ ] 스토리지 사용량 추이 확인 (빠른 증가 예상)
- [ ] 복잡한 AI 쿼리 성능 분석
- [ ] 파티션 관리 (월별 추가)
- [ ] 데이터 아카이빙 정책 실행
- [ ] OpenAI API 사용량 및 비용 모니터링
- [ ] 보안 업데이트 적용
- [ ] 전체 AI 파이프라인 비용 분석
- [ ] 로그 분석 및 AI 관련 이슈 대응