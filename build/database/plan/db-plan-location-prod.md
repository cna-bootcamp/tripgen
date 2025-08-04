# Location Service 운영환경 데이터베이스 설치 가이드

## 개요

Location Service 운영환경을 위한 Azure Database for PostgreSQL Flexible Server + PostGIS 구성 및 배포 가이드입니다.
지리공간 데이터 처리, 위치 기반 검색, 거리 계산에 최적화된 Zone Redundant 고가용성, 프라이빗 네트워킹, 자동 백업, 모니터링을 포함한 완전한 운영환경 데이터베이스를 구축합니다.

### 주요 특징
- **고가용성**: Zone Redundant 구성으로 99.99% 가용성 보장
- **지리공간 기능**: PostGIS 확장을 통한 고급 공간 데이터 처리
- **보안**: Private Endpoint 및 Azure Key Vault 통합
- **백업**: 매일 자동 백업, 35일 보관
- **성능**: 지리공간 인덱스 및 공간 쿼리 최적화
- **확장성**: 위치 데이터 증가에 대비한 파티셔닝
- **모니터링**: Azure Monitor 및 Log Analytics 통합

## Azure 리소스 구성

### 리소스 그룹 구조
```
rg-tripgen-location-prod-koreasouth
├── postgresql-location-prod-server
├── kv-tripgen-location-prod
├── vnet-tripgen-location-prod
├── subnet-db-location-prod
├── pe-location-db-prod
└── pdnsz-location-db-prod
```

### 네트워크 구성
- **VNet CIDR**: 10.4.0.0/16
- **DB Subnet CIDR**: 10.4.1.0/24
- **Private Endpoint Subnet**: 10.4.2.0/24

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
resource "azurerm_resource_group" "location_prod" {
  name     = "rg-tripgen-location-prod-koreasouth"
  location = "Korea South"

  tags = {
    Environment = "Production"
    Service     = "Location"
    Project     = "TripGen"
    ManagedBy   = "Terraform"
  }
}

# Virtual Network
resource "azurerm_virtual_network" "location_prod" {
  name                = "vnet-tripgen-location-prod"
  address_space       = ["10.4.0.0/16"]
  location            = azurerm_resource_group.location_prod.location
  resource_group_name = azurerm_resource_group.location_prod.name

  tags = azurerm_resource_group.location_prod.tags
}

# Database Subnet
resource "azurerm_subnet" "db_location_prod" {
  name                 = "subnet-db-location-prod"
  resource_group_name  = azurerm_resource_group.location_prod.name
  virtual_network_name = azurerm_virtual_network.location_prod.name
  address_prefixes     = ["10.4.1.0/24"]

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
resource "azurerm_subnet" "pe_location_prod" {
  name                 = "subnet-pe-location-prod"
  resource_group_name  = azurerm_resource_group.location_prod.name
  virtual_network_name = azurerm_virtual_network.location_prod.name
  address_prefixes     = ["10.4.2.0/24"]
}

# Private DNS Zone
resource "azurerm_private_dns_zone" "location_db_prod" {
  name                = "privatelink.postgres.database.azure.com"
  resource_group_name = azurerm_resource_group.location_prod.name

  tags = azurerm_resource_group.location_prod.tags
}

# Private DNS Zone VNet Link
resource "azurerm_private_dns_zone_virtual_network_link" "location_db_prod" {
  name                  = "pdnsz-link-location-db-prod"
  resource_group_name   = azurerm_resource_group.location_prod.name
  private_dns_zone_name = azurerm_private_dns_zone.location_db_prod.name
  virtual_network_id    = azurerm_virtual_network.location_prod.id

  tags = azurerm_resource_group.location_prod.tags
}

# PostgreSQL Flexible Server
resource "azurerm_postgresql_flexible_server" "location_prod" {
  name                   = "postgresql-location-prod-server"
  resource_group_name    = azurerm_resource_group.location_prod.name
  location               = azurerm_resource_group.location_prod.location
  version                = "15"
  delegated_subnet_id    = azurerm_subnet.db_location_prod.id
  private_dns_zone_id    = azurerm_private_dns_zone.location_db_prod.id
  administrator_login    = "tripgen_admin"
  administrator_password = random_password.location_db_password.result
  zone                   = "1"

  storage_mb   = 65536  # 64GB - 지리공간 데이터 저장
  storage_tier = "P6"

  sku_name   = "GP_Standard_D2s_v3"  # General Purpose - 공간 연산 처리
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

  tags = azurerm_resource_group.location_prod.tags

  depends_on = [azurerm_private_dns_zone_virtual_network_link.location_db_prod]
}

# Database
resource "azurerm_postgresql_flexible_server_database" "location_prod" {
  name      = "locationdb_prod"
  server_id = azurerm_postgresql_flexible_server.location_prod.id
  collation = "en_US.utf8"
  charset   = "utf8"
}

# Random Password
resource "random_password" "location_db_password" {
  length  = 32
  special = true
}

# Key Vault
resource "azurerm_key_vault" "location_prod" {
  name                       = "kv-tripgen-location-prod"
  location                   = azurerm_resource_group.location_prod.location
  resource_group_name        = azurerm_resource_group.location_prod.name
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

  tags = azurerm_resource_group.location_prod.tags
}

# Key Vault Secret - DB Password
resource "azurerm_key_vault_secret" "location_db_password" {
  name         = "location-db-password"
  value        = random_password.location_db_password.result
  key_vault_id = azurerm_key_vault.location_prod.id

  tags = azurerm_resource_group.location_prod.tags
}

# Key Vault Secret - Connection String
resource "azurerm_key_vault_secret" "location_db_connection" {
  name  = "location-db-connection-string"
  value = "Server=${azurerm_postgresql_flexible_server.location_prod.fqdn};Database=${azurerm_postgresql_flexible_server_database.location_prod.name};Port=5432;User Id=${azurerm_postgresql_flexible_server.location_prod.administrator_login};Password=${random_password.location_db_password.result};Ssl Mode=Require;"
  key_vault_id = azurerm_key_vault.location_prod.id

  tags = azurerm_resource_group.location_prod.tags
}

# Key Vault Secret - Google Maps API Key
resource "azurerm_key_vault_secret" "google_maps_api_key" {
  name         = "google-maps-api-key"
  value        = var.google_maps_api_key
  key_vault_id = azurerm_key_vault.location_prod.id

  tags = azurerm_resource_group.location_prod.tags
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
  default     = "location"
}

variable "google_maps_api_key" {
  description = "Google Maps API Key"
  type        = string
  sensitive   = true
}
```

### outputs.tf
```hcl
output "postgresql_server_fqdn" {
  description = "PostgreSQL Server FQDN"
  value       = azurerm_postgresql_flexible_server.location_prod.fqdn
}

output "postgresql_database_name" {
  description = "PostgreSQL Database Name"
  value       = azurerm_postgresql_flexible_server_database.location_prod.name
}

output "key_vault_uri" {
  description = "Key Vault URI"
  value       = azurerm_key_vault.location_prod.vault_uri
}

output "resource_group_name" {
  description = "Resource Group Name"
  value       = azurerm_resource_group.location_prod.name
}
```

## 보안 설정

### Private Endpoint 구성
```hcl
# Private Endpoint
resource "azurerm_private_endpoint" "location_db_prod" {
  name                = "pe-location-db-prod"
  location            = azurerm_resource_group.location_prod.location
  resource_group_name = azurerm_resource_group.location_prod.name
  subnet_id           = azurerm_subnet.pe_location_prod.id

  private_service_connection {
    name                           = "psc-location-db-prod"
    private_connection_resource_id = azurerm_postgresql_flexible_server.location_prod.id
    subresource_names              = ["postgresqlServer"]
    is_manual_connection           = false
  }

  private_dns_zone_group {
    name                 = "pdnszg-location-db-prod"
    private_dns_zone_ids = [azurerm_private_dns_zone.location_db_prod.id]
  }

  tags = azurerm_resource_group.location_prod.tags
}
```

### Key Vault 접근 정책 추가
```hcl
# Application Access Policy (Location Service)
resource "azurerm_key_vault_access_policy" "location_service_prod" {
  key_vault_id = azurerm_key_vault.location_prod.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_user_assigned_identity.location_service_prod.principal_id

  secret_permissions = [
    "Get",
    "List"
  ]
}

# User Assigned Identity for Location Service
resource "azurerm_user_assigned_identity" "location_service_prod" {
  name                = "id-location-service-prod"
  resource_group_name = azurerm_resource_group.location_prod.name
  location            = azurerm_resource_group.location_prod.location

  tags = azurerm_resource_group.location_prod.tags
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
  --resource-group rg-tripgen-location-prod-koreasouth \
  --name postgresql-location-prod-server-restored \
  --source-server postgresql-location-prod-server \
  --restore-time "2024-01-15T10:00:00Z"
```

### 수동 백업 스크립트 (PostGIS 포함)
```bash
#!/bin/bash
# manual-backup.sh

DB_SERVER="postgresql-location-prod-server.postgres.database.azure.com"
DB_NAME="locationdb_prod"
DB_USER="tripgen_admin"
BACKUP_DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="location_db_backup_${BACKUP_DATE}.sql"

# Key Vault에서 패스워드 가져오기
DB_PASSWORD=$(az keyvault secret show --vault-name kv-tripgen-location-prod --name location-db-password --query value -o tsv)

# PostGIS 데이터 포함 백업 실행
PGPASSWORD=$DB_PASSWORD pg_dump \
  -h $DB_SERVER \
  -U $DB_USER \
  -d $DB_NAME \
  -f $BACKUP_FILE \
  --verbose \
  --format=custom \
  --compress=9

# Azure Storage에 업로드
az storage blob upload \
  --account-name tripgenlocationbackupprod \
  --container-name backups \
  --name $BACKUP_FILE \
  --file $BACKUP_FILE

echo "PostGIS backup completed: $BACKUP_FILE"
```

## 모니터링 및 알림 설정

### Log Analytics Workspace
```hcl
resource "azurerm_log_analytics_workspace" "location_prod" {
  name                = "law-tripgen-location-prod"
  location            = azurerm_resource_group.location_prod.location
  resource_group_name = azurerm_resource_group.location_prod.name
  sku                 = "PerGB2018"
  retention_in_days   = 30

  tags = azurerm_resource_group.location_prod.tags
}

# Diagnostic Settings
resource "azurerm_monitor_diagnostic_setting" "location_db_prod" {
  name               = "diag-location-db-prod"
  target_resource_id = azurerm_postgresql_flexible_server.location_prod.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.location_prod.id

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
resource "azurerm_monitor_action_group" "location_db_alerts" {
  name                = "ag-location-db-alerts-prod"
  resource_group_name = azurerm_resource_group.location_prod.name
  short_name          = "locationdb"

  email_receiver {
    name          = "Location Team"
    email_address = "location-team@company.com"
  }

  email_receiver {
    name          = "DBA Team"
    email_address = "dba-team@company.com"
  }

  tags = azurerm_resource_group.location_prod.tags
}

# CPU 사용률 알림
resource "azurerm_monitor_metric_alert" "location_db_cpu" {
  name                = "alert-location-db-cpu-prod"
  resource_group_name = azurerm_resource_group.location_prod.name
  scopes              = [azurerm_postgresql_flexible_server.location_prod.id]
  description         = "Location DB CPU usage is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "cpu_percent"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 75  # 공간 연산으로 인한 높은 CPU 사용률 고려
  }

  action {
    action_group_id = azurerm_monitor_action_group.location_db_alerts.id
  }

  tags = azurerm_resource_group.location_prod.tags
}

# 스토리지 사용률 알림
resource "azurerm_monitor_metric_alert" "location_db_storage" {
  name                = "alert-location-db-storage-prod"
  resource_group_name = azurerm_resource_group.location_prod.name
  scopes              = [azurerm_postgresql_flexible_server.location_prod.id]
  description         = "Location DB storage usage is high"
  severity            = 2

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "storage_percent"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 85
  }

  action {
    action_group_id = azurerm_monitor_action_group.location_db_alerts.id
  }

  tags = azurerm_resource_group.location_prod.tags
}

# 느린 쿼리 알림 (공간 쿼리 특화)
resource "azurerm_monitor_metric_alert" "location_db_slow_queries" {
  name                = "alert-location-db-slow-queries-prod"
  resource_group_name = azurerm_resource_group.location_prod.name
  scopes              = [azurerm_postgresql_flexible_server.location_prod.id]
  description         = "Location DB has slow spatial queries"
  severity            = 3

  criteria {
    metric_namespace = "Microsoft.DBforPostgreSQL/flexibleServers"
    metric_name      = "active_connections"
    aggregation      = "Average"
    operator         = "GreaterThan"
    threshold        = 40  # 공간 쿼리 특성상 적은 동시 연결
  }

  window_size = "PT15M"
  frequency   = "PT5M"

  action {
    action_group_id = azurerm_monitor_action_group.location_db_alerts.id
  }

  tags = azurerm_resource_group.location_prod.tags
}
```

## 성능 튜닝

### PostgreSQL 설정 최적화 (PostGIS 특화)
```hcl
# Server Parameters - PostGIS 공간 연산에 최적화
resource "azurerm_postgresql_flexible_server_configuration" "location_shared_buffers" {
  name      = "shared_buffers"
  server_id = azurerm_postgresql_flexible_server.location_prod.id
  value     = "512MB"  # 공간 데이터 캐싱
}

resource "azurerm_postgresql_flexible_server_configuration" "location_effective_cache_size" {
  name      = "effective_cache_size"
  server_id = azurerm_postgresql_flexible_server.location_prod.id
  value     = "2GB"
}

resource "azurerm_postgresql_flexible_server_configuration" "location_work_mem" {
  name      = "work_mem"
  server_id = azurerm_postgresql_flexible_server.location_prod.id
  value     = "32MB"  # 공간 연산을 위한 큰 work_mem
}

resource "azurerm_postgresql_flexible_server_configuration" "location_maintenance_work_mem" {
  name      = "maintenance_work_mem"
  server_id = azurerm_postgresql_flexible_server.location_prod.id
  value     = "256MB"  # 공간 인덱스 생성용
}

resource "azurerm_postgresql_flexible_server_configuration" "location_random_page_cost" {
  name      = "random_page_cost"
  server_id = azurerm_postgresql_flexible_server.location_prod.id
  value     = "1.1"  # SSD 최적화
}

resource "azurerm_postgresql_flexible_server_configuration" "location_seq_page_cost" {
  name      = "seq_page_cost"
  server_id = azurerm_postgresql_flexible_server.location_prod.id
  value     = "1"
}

resource "azurerm_postgresql_flexible_server_configuration" "location_max_connections" {
  name      = "max_connections"
  server_id = azurerm_postgresql_flexible_server.location_prod.id
  value     = "100"  # 공간 쿼리 특성상 적은 동시 연결
}

# PostGIS 특화 설정
resource "azurerm_postgresql_flexible_server_configuration" "location_jit" {
  name      = "jit"
  server_id = azurerm_postgresql_flexible_server.location_prod.id
  value     = "on"
}

resource "azurerm_postgresql_flexible_server_configuration" "location_constraint_exclusion" {
  name      = "constraint_exclusion"
  server_id = azurerm_postgresql_flexible_server.location_prod.id
  value     = "partition"
}
```

### PostGIS Extensions 설치
```sql
-- PostGIS 및 공간 분석을 위한 확장 모듈 설치
-- extensions.sql

-- PostGIS 핵심 확장
CREATE EXTENSION IF NOT EXISTS "postgis";
CREATE EXTENSION IF NOT EXISTS "postgis_topology";
CREATE EXTENSION IF NOT EXISTS "postgis_raster";

-- 전문 검색 및 인덱싱
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "btree_gin";
CREATE EXTENSION IF NOT EXISTS "btree_gist";

-- UUID 생성
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 암호화
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 지리공간 클러스터링
CREATE EXTENSION IF NOT EXISTS "dblink";

-- 성능 모니터링
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
```

### 공간 인덱스 최적화 스크립트
```sql
-- location_service_indexes.sql
-- Location Service용 PostGIS 최적화 인덱스

-- 장소 위치 공간 인덱스 (GIST)
CREATE INDEX CONCURRENTLY idx_places_location_gist 
ON places USING gist (location) WHERE deleted_at IS NULL;

-- 장소 카테고리별 공간 인덱스
CREATE INDEX CONCURRENTLY idx_places_category_location 
ON places USING gist (category, location) WHERE deleted_at IS NULL;

-- 장소 평점 및 위치 복합 인덱스
CREATE INDEX CONCURRENTLY idx_places_rating_location 
ON places (rating DESC, location) WHERE deleted_at IS NULL AND status = 'active';

-- 지역별 장소 인덱스
CREATE INDEX CONCURRENTLY idx_places_region_location 
ON places USING gist (region_code, location) WHERE deleted_at IS NULL;

-- 거리 기반 검색을 위한 복합 인덱스
CREATE INDEX CONCURRENTLY idx_places_distance_search 
ON places (category, rating, location) WHERE deleted_at IS NULL AND status = 'active';

-- 장소 리뷰 공간 인덱스
CREATE INDEX CONCURRENTLY idx_place_reviews_location_created 
ON place_reviews (place_id, created_at) WHERE deleted_at IS NULL;

-- 사용자 방문 이력 공간 인덱스
CREATE INDEX CONCURRENTLY idx_user_visits_location_date 
ON user_visits USING gist (visit_location, visit_date) WHERE deleted_at IS NULL;

-- 여행 경로 라인스트링 인덱스
CREATE INDEX CONCURRENTLY idx_trip_routes_path_gist 
ON trip_routes USING gist (route_path) WHERE deleted_at IS NULL;

-- 텍스트 검색 인덱스 (장소명, 주소)
CREATE INDEX CONCURRENTLY idx_places_text_search 
ON places USING gin (to_tsvector('korean', name || ' ' || address)) 
WHERE deleted_at IS NULL;

-- 부분 텍스트 검색 인덱스
CREATE INDEX CONCURRENTLY idx_places_name_trigram 
ON places USING gin (name gin_trgm_ops) WHERE deleted_at IS NULL;
```

### 공간 쿼리 최적화 함수
```sql
-- location_functions.sql
-- 공간 쿼리 최적화 함수들

-- 반경 내 장소 검색 최적화 함수
CREATE OR REPLACE FUNCTION find_places_within_radius(
    center_lat DECIMAL(10,8),
    center_lng DECIMAL(11,8),
    radius_meters INTEGER DEFAULT 1000,
    category_filter TEXT DEFAULT NULL,
    min_rating DECIMAL(3,2) DEFAULT 0.0,
    limit_count INTEGER DEFAULT 50
)
RETURNS TABLE (
    place_id UUID,
    name TEXT,
    category TEXT,
    rating DECIMAL(3,2),
    distance_meters INTEGER,
    location GEOMETRY
) AS $$
DECLARE
    search_point GEOMETRY;
BEGIN
    -- 검색 중심점 생성
    search_point := ST_SetSRID(ST_MakePoint(center_lng, center_lat), 4326);
    
    RETURN QUERY
    SELECT 
        p.id,
        p.name,
        p.category,
        p.rating,
        ST_Distance(ST_Transform(p.location, 3857), ST_Transform(search_point, 3857))::INTEGER,
        p.location
    FROM places p
    WHERE p.deleted_at IS NULL
        AND p.status = 'active'
        AND ST_DWithin(ST_Transform(p.location, 3857), ST_Transform(search_point, 3857), radius_meters)
        AND (category_filter IS NULL OR p.category = category_filter)
        AND p.rating >= min_rating
    ORDER BY ST_Distance(ST_Transform(p.location, 3857), ST_Transform(search_point, 3857))
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

-- 경로 기반 주변 장소 검색
CREATE OR REPLACE FUNCTION find_places_along_route(
    route_points GEOMETRY[],
    buffer_meters INTEGER DEFAULT 1000,
    category_filter TEXT DEFAULT NULL
)
RETURNS TABLE (
    place_id UUID,
    name TEXT,
    category TEXT,
    rating DECIMAL(3,2),
    closest_point_on_route GEOMETRY
) AS $$
DECLARE
    route_line GEOMETRY;
    buffered_route GEOMETRY;
BEGIN
    -- 경로 라인 생성
    route_line := ST_SetSRID(ST_MakeLine(route_points), 4326);
    
    -- 버퍼 생성 (미터 단위)
    buffered_route := ST_Buffer(ST_Transform(route_line, 3857), buffer_meters);
    
    RETURN QUERY
    SELECT 
        p.id,
        p.name,
        p.category,
        p.rating,
        ST_ClosestPoint(route_line, p.location)
    FROM places p
    WHERE p.deleted_at IS NULL
        AND p.status = 'active'
        AND ST_Intersects(ST_Transform(p.location, 3857), buffered_route)
        AND (category_filter IS NULL OR p.category = category_filter)
    ORDER BY p.rating DESC;
END;
$$ LANGUAGE plpgsql;

-- 지역별 인기 장소 집계
CREATE OR REPLACE FUNCTION get_popular_places_by_region(
    region_code TEXT,
    days_back INTEGER DEFAULT 30
)
RETURNS TABLE (
    place_id UUID,
    name TEXT,
    category TEXT,
    visit_count BIGINT,
    avg_rating DECIMAL(3,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        p.id,
        p.name,
        p.category,
        COUNT(uv.id) as visit_count,
        AVG(pr.rating) as avg_rating
    FROM places p
    LEFT JOIN user_visits uv ON p.id = uv.place_id 
        AND uv.visit_date >= CURRENT_DATE - INTERVAL '1 day' * days_back
        AND uv.deleted_at IS NULL
    LEFT JOIN place_reviews pr ON p.id = pr.place_id 
        AND pr.deleted_at IS NULL
    WHERE p.deleted_at IS NULL
        AND p.status = 'active'
        AND p.region_code = get_popular_places_by_region.region_code
    GROUP BY p.id, p.name, p.category
    HAVING COUNT(uv.id) > 0
    ORDER BY visit_count DESC, avg_rating DESC;
END;
$$ LANGUAGE plpgsql;
```

### 테이블 파티셔닝 설정 (지리 기반)
```sql
-- 지역별 파티셔닝 설정
-- places 테이블을 지역 코드로 파티셔닝

-- 1. 새로운 파티션 테이블 생성
CREATE TABLE places_partitioned (
    LIKE places INCLUDING ALL
) PARTITION BY LIST (region_code);

-- 2. 지역별 파티션 생성
CREATE TABLE places_seoul PARTITION OF places_partitioned
    FOR VALUES IN ('KR-11'); -- 서울특별시

CREATE TABLE places_busan PARTITION OF places_partitioned
    FOR VALUES IN ('KR-26'); -- 부산광역시

CREATE TABLE places_daegu PARTITION OF places_partitioned
    FOR VALUES IN ('KR-27'); -- 대구광역시

CREATE TABLE places_incheon PARTITION OF places_partitioned
    FOR VALUES IN ('KR-28'); -- 인천광역시

CREATE TABLE places_gwangju PARTITION OF places_partitioned
    FOR VALUES IN ('KR-29'); -- 광주광역시

CREATE TABLE places_daejeon PARTITION OF places_partitioned
    FOR VALUES IN ('KR-30'); -- 대전광역시

CREATE TABLE places_ulsan PARTITION OF places_partitioned
    FOR VALUES IN ('KR-31'); -- 울산광역시

CREATE TABLE places_sejong PARTITION OF places_partitioned
    FOR VALUES IN ('KR-50'); -- 세종특별자치시

CREATE TABLE places_gyeonggi PARTITION OF places_partitioned
    FOR VALUES IN ('KR-41'); -- 경기도

CREATE TABLE places_other PARTITION OF places_partitioned
    DEFAULT; -- 기타 지역

-- 3. 각 파티션에 공간 인덱스 생성
CREATE INDEX idx_places_seoul_location_gist ON places_seoul USING gist (location);
CREATE INDEX idx_places_busan_location_gist ON places_busan USING gist (location);
-- ... 각 파티션별 인덱스 생성
```

## 비용 최적화

### 지리공간 데이터 아카이빙
```hcl
# 지리공간 데이터 아카이빙을 위한 Storage Account
resource "azurerm_storage_account" "location_archive" {
  name                     = "tripgenlocationarchiveprod"
  resource_group_name      = azurerm_resource_group.location_prod.name
  location                 = azurerm_resource_group.location_prod.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  access_tier              = "Cool"

  tags = azurerm_resource_group.location_prod.tags
}

# 컨테이너 생성
resource "azurerm_storage_container" "location_data_archive" {
  name                  = "location-data-archive"
  storage_account_name  = azurerm_storage_account.location_archive.name
  container_access_type = "private"
}
```

### 데이터 생명주기 관리
```sql
-- 위치 데이터 생명주기 관리
-- cleanup_old_location_data.sql

CREATE OR REPLACE FUNCTION cleanup_old_location_data()
RETURNS void AS $$
BEGIN
    -- 1년 이상 된 사용자 방문 이력 아카이브
    INSERT INTO user_visits_archive 
    SELECT * FROM user_visits 
    WHERE visit_date < CURRENT_DATE - INTERVAL '1 year'
    AND deleted_at IS NULL;
    
    -- 아카이브된 방문 이력 삭제
    DELETE FROM user_visits 
    WHERE visit_date < CURRENT_DATE - INTERVAL '1 year';
    
    -- 6개월 이상 된 검색 로그 삭제
    DELETE FROM location_search_logs 
    WHERE created_at < CURRENT_DATE - INTERVAL '6 months';
    
    -- 비활성 장소 데이터 정리
    UPDATE places 
    SET deleted_at = CURRENT_TIMESTAMP
    WHERE status = 'inactive' 
    AND updated_at < CURRENT_DATE - INTERVAL '2 years'
    AND deleted_at IS NULL;
    
    -- 공간 인덱스 재구성
    REINDEX INDEX CONCURRENTLY idx_places_location_gist;
    
    -- 통계 업데이트
    ANALYZE places;
    ANALYZE user_visits;
    
    RAISE NOTICE 'Location data cleanup completed';
END;
$$ LANGUAGE plpgsql;
```

### 비용 모니터링
```hcl
# Budget Alert
resource "azurerm_consumption_budget_resource_group" "location_prod" {
  name              = "budget-location-prod"
  resource_group_id = azurerm_resource_group.location_prod.id

  amount     = 400  # Location Service 지리공간 처리 비용
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
      "location-team@company.com",
      "billing@company.com"
    ]
  }

  notification {
    enabled   = true
    threshold = 95
    operator  = "GreaterThan"

    contact_emails = [
      "admin@company.com",
      "location-team@company.com",
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

# 2. 계획 확인 (Google Maps API Key 포함)
terraform plan -var="google_maps_api_key=${GOOGLE_MAPS_API_KEY}" -out=location-prod.tfplan

# 3. 배포 실행
terraform apply location-prod.tfplan

# 4. 배포 상태 확인
terraform show
```

### 배포 후 검증
```bash
# 1. 연결 테스트
az postgres flexible-server connect \
  --name postgresql-location-prod-server \
  --admin-user tripgen_admin \
  --database locationdb_prod

# 2. 상태 확인
az postgres flexible-server show \
  --resource-group rg-tripgen-location-prod-koreasouth \
  --name postgresql-location-prod-server

# 3. Key Vault 접근 테스트
az keyvault secret show \
  --vault-name kv-tripgen-location-prod \
  --name location-db-password
```

### PostGIS 데이터베이스 초기 설정
```sql
-- PostGIS 초기 설정 스크립트 실행
\i extensions.sql
\i location-service-schema.sql
\i location-service-indexes.sql
\i location-service-functions.sql
\i location-service-partitions.sql

-- PostGIS 버전 확인
SELECT PostGIS_Version();

-- 공간 참조 시스템 확인
SELECT * FROM spatial_ref_sys WHERE srid IN (4326, 3857);

-- 권한 설정
GRANT CONNECT ON DATABASE locationdb_prod TO location_app_user;
GRANT USAGE ON SCHEMA public TO location_app_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO location_app_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO location_app_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO location_app_user;

-- 샘플 공간 데이터 검증
SELECT ST_AsText(ST_MakePoint(126.9780, 37.5665)); -- 서울시청 좌표
```

## 운영 체크리스트

### 배포 전 체크리스트
- [ ] Azure 구독 및 권한 확인
- [ ] Terraform 버전 확인 (>= 1.0)
- [ ] Azure CLI 로그인 상태 확인
- [ ] Google Maps API Key 준비
- [ ] 네트워크 대역 충돌 확인
- [ ] 리소스 이름 중복 확인
- [ ] PostGIS 라이선스 확인
- [ ] 지리공간 데이터 용량 계획

### 배포 후 체크리스트
- [ ] PostgreSQL 서버 상태 확인
- [ ] PostGIS 확장 설치 확인
- [ ] Private Endpoint 연결 상태 확인
- [ ] Key Vault 시크릿 생성 확인 (DB + Google Maps API Key)
- [ ] 백업 정책 활성화 확인
- [ ] 모니터링 대시보드 설정 확인
- [ ] 알림 규칙 테스트
- [ ] 애플리케이션 연결 테스트
- [ ] 공간 인덱스 생성 완료 확인
- [ ] 지역별 파티셔닝 설정 확인
- [ ] 공간 쿼리 성능 테스트
- [ ] Google Maps API 연동 테스트

### 운영 중 점검 항목
- [ ] 일일 백업 상태 확인 (PostGIS 데이터 포함)
- [ ] 공간 쿼리 성능 메트릭 모니터링
- [ ] 스토리지 사용량 추이 확인 (지리공간 데이터 증가)
- [ ] 공간 인덱스 성능 분석
- [ ] 지역별 파티션 관리
- [ ] Google Maps API 사용량 및 비용 모니터링
- [ ] 위치 데이터 정확성 검증
- [ ] 데이터 아카이빙 정책 실행
- [ ] PostGIS 버전 업데이트 확인
- [ ] 보안 업데이트 적용
- [ ] 전체 위치 서비스 비용 분석
- [ ] 로그 분석 및 위치 관련 이슈 대응