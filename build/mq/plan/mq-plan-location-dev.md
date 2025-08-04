# Location Service Message Queue 설치 계획서 (개발환경)

## 개요
- **서비스명**: Location Service
- **환경**: Development
- **Message Queue**: Azure Service Bus Basic Tier
- **목적**: 위치 정보 관리 및 지리적 데이터 처리 관련 비동기 메시징

## Azure Service Bus 설정

### 1. Service Bus Namespace 생성
```bash
# Resource Group 확인 (기존 그룹 사용)
az group show --name rg-tripgen-dev

# Service Bus Namespace 생성 (Basic Tier)
az servicebus namespace create \
    --resource-group rg-tripgen-dev \
    --name sb-tripgen-location-dev \
    --location koreacentral \
    --sku Basic
```

### 2. Queue 설정

#### 2.1 Location Events Queue
```bash
# 위치 이벤트 처리용 큐 생성
az servicebus queue create \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-location-dev \
    --name location-events \
    --max-size 5120 \
    --lock-duration PT30S \
    --default-message-time-to-live P14D \
    --enable-partitioning false \
    --enable-duplicate-detection false
```

#### 2.2 Geocoding Queue
```bash
# 지오코딩 처리용 큐 생성
az servicebus queue create \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-location-dev \
    --name geocoding \
    --max-size 5120 \
    --lock-duration PT60S \
    --default-message-time-to-live P14D \
    --enable-partitioning false \
    --enable-duplicate-detection true \
    --duplicate-detection-history-time-window PT5M
```

#### 2.3 POI Updates Queue
```bash
# POI(Point of Interest) 업데이트용 큐 생성
az servicebus queue create \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-location-dev \
    --name poi-updates \
    --max-size 5120 \
    --lock-duration PT120S \
    --default-message-time-to-live P30D \
    --enable-partitioning false \
    --enable-duplicate-detection true \
    --duplicate-detection-history-time-window PT10M
```

#### 2.4 Route Calculation Queue
```bash
# 경로 계산용 큐 생성
az servicebus queue create \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-location-dev \
    --name route-calculation \
    --max-size 5120 \
    --lock-duration PT180S \
    --default-message-time-to-live P14D \
    --enable-partitioning false \
    --enable-duplicate-detection true \
    --duplicate-detection-history-time-window PT15M
```

### 3. 접근 정책 설정

#### 3.1 Managed Identity 생성
```bash
# Location Service용 Managed Identity 생성
az identity create \
    --resource-group rg-tripgen-dev \
    --name mi-location-service-dev
```

#### 3.2 Service Bus 권한 할당
```bash
# Service Bus Data Owner 권한 할당
az role assignment create \
    --assignee $(az identity show --resource-group rg-tripgen-dev --name mi-location-service-dev --query principalId -o tsv) \
    --role "Azure Service Bus Data Owner" \
    --scope /subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-tripgen-dev/providers/Microsoft.ServiceBus/namespaces/sb-tripgen-location-dev
```

## 연결 설정

### 4. Connection String 구성
```json
{
  "ServiceBus": {
    "ConnectionString": "Endpoint=sb://sb-tripgen-location-dev.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=<ACCESS_KEY>",
    "QueueNames": {
      "LocationEvents": "location-events",
      "Geocoding": "geocoding",
      "POIUpdates": "poi-updates",
      "RouteCalculation": "route-calculation"
    }
  }
}
```

### 5. Managed Identity 연결 (권장)
```json
{
  "ServiceBus": {
    "FullyQualifiedNamespace": "sb-tripgen-location-dev.servicebus.windows.net",
    "ManagedIdentity": {
      "ClientId": "<MANAGED_IDENTITY_CLIENT_ID>"
    },
    "QueueNames": {
      "LocationEvents": "location-events",
      "Geocoding": "geocoding",
      "POIUpdates": "poi-updates", 
      "RouteCalculation": "route-calculation"
    }
  }
}
```

## Message 핸들링 설정

### 6. Queue 구성 세부사항
- **최대 큐 크기**: 5GB
- **메시지 Lock Duration**: 
  - Events: 30초 - 빠른 이벤트 처리
  - Geocoding: 60초 - API 호출 시간 고려
  - POI Updates: 120초 - 데이터 검증 및 저장
  - Route Calculation: 180초 - 복잡한 경로 계산
- **메시지 TTL**: 
  - Events: 14일
  - POI Updates: 30일 (재처리 가능성)
- **중복 감지**: Geocoding, POI Updates, Route Calculation에서 활성화

### 7. 메시지 유형별 설정

#### Location Events 메시지
```json
{
  "MessageType": "LocationEvent",
  "Properties": {
    "EventType": "LocationAdded|LocationUpdated|LocationDeleted|LocationViewed",
    "LocationId": "string",
    "UserId": "string",
    "Coordinates": {
      "Latitude": "decimal",
      "Longitude": "decimal"
    },
    "LocationName": "string",
    "Category": "string",
    "Timestamp": "datetime",
    "Source": "location-service"
  }
}
```

#### Geocoding 메시지
```json
{
  "MessageType": "GeocodingRequest",
  "Properties": {
    "RequestId": "string",
    "RequestType": "Forward|Reverse",
    "Address": "string",
    "Coordinates": {
      "Latitude": "decimal",
      "Longitude": "decimal"
    },
    "Country": "string",
    "Language": "string",
    "Precision": "High|Medium|Low",
    "CallbackQueue": "string"
  }
}
```

#### POI Updates 메시지
```json
{
  "MessageType": "POIUpdate",
  "Properties": {
    "UpdateId": "string",
    "UpdateType": "Create|Update|Delete|BulkUpdate",
    "POIData": {
      "PlaceId": "string",
      "Name": "string",
      "Category": "string",
      "Coordinates": {
        "Latitude": "decimal",
        "Longitude": "decimal"
      },
      "Address": "string",
      "Rating": "decimal",
      "Reviews": "int",
      "OpeningHours": "object",
      "ContactInfo": "object"
    },
    "DataSource": "GooglePlaces|Foursquare|Manual",
    "Priority": "High|Medium|Low"
  }
}
```

#### Route Calculation 메시지
```json
{
  "MessageType": "RouteCalculationRequest",
  "Properties": {
    "RequestId": "string",
    "Origin": {
      "Latitude": "decimal",
      "Longitude": "decimal",
      "Name": "string"
    },
    "Destination": {
      "Latitude": "decimal",
      "Longitude": "decimal", 
      "Name": "string"
    },
    "Waypoints": "array",
    "TravelMode": "Driving|Walking|Transit|Cycling",
    "Preferences": {
      "AvoidTolls": "boolean",
      "AvoidHighways": "boolean",
      "OptimizeFor": "Time|Distance|Fuel"
    },
    "UserId": "string",
    "CallbackQueue": "string"
  }
}
```

## 초기화 명령어

### 8. 검증 스크립트
```bash
#!/bin/bash
# Location Service Message Queue 연결 테스트
echo "Testing Location Service Bus connection..."

# Queue 상태 확인
echo "Checking location-events queue..."
az servicebus queue show \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-location-dev \
    --name location-events \
    --query '{Name:name, Status:status, MessageCount:messageCount}'

echo "Checking geocoding queue..."
az servicebus queue show \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-location-dev \
    --name geocoding \
    --query '{Name:name, Status:status, MessageCount:messageCount}'

echo "Checking poi-updates queue..."
az servicebus queue show \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-location-dev \
    --name poi-updates \
    --query '{Name:name, Status:status, MessageCount:messageCount}'

echo "Checking route-calculation queue..."
az servicebus queue show \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-location-dev \
    --name route-calculation \
    --query '{Name:name, Status:status, MessageCount:messageCount}'

echo "Location Service Message Queue setup completed!"
```

## 외부 API 통합

### 9. Google Maps API 연동 설정
```json
{
  "GoogleMaps": {
    "ApiKey": "<GOOGLE_MAPS_API_KEY>",
    "Services": {
      "Geocoding": "https://maps.googleapis.com/maps/api/geocode/json",
      "Places": "https://maps.googleapis.com/maps/api/place/nearbysearch/json",
      "Directions": "https://maps.googleapis.com/maps/api/directions/json",
      "PlaceDetails": "https://maps.googleapis.com/maps/api/place/details/json"
    },
    "RateLimiting": {
      "RequestsPerSecond": 50,
      "DailyQuota": 100000
    },
    "Caching": {
      "Enabled": true,
      "CacheDuration": "PT24H",
      "MaxCacheSize": "1GB"
    }
  }
}
```

### 10. 카카오맵 API 연동 설정
```json
{
  "KakaoMap": {
    "RestApiKey": "<KAKAO_REST_API_KEY>",
    "Services": {
      "AddressSearch": "https://dapi.kakao.com/v2/local/search/address.json",
      "KeywordSearch": "https://dapi.kakao.com/v2/local/search/keyword.json",
      "CategorySearch": "https://dapi.kakao.com/v2/local/search/category.json",
      "Coord2Address": "https://dapi.kakao.com/v2/local/geo/coord2address.json"
    },
    "RateLimiting": {
      "RequestsPerSecond": 10,
      "DailyQuota": 300000
    }
  }
}
```

## 모니터링 설정

### 11. Application Insights 연동
```bash
# Application Insights 생성
az monitor app-insights component create \
    --app ai-location-service-dev \
    --location koreacentral \
    --resource-group rg-tripgen-dev \
    --kind web
```

### 12. 알림 규칙 설정
```bash
# Geocoding Queue 처리 지연 알림
az monitor metrics alert create \
    --name "Location Geocoding Queue Alert" \
    --resource-group rg-tripgen-dev \
    --scopes /subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-tripgen-dev/providers/Microsoft.ServiceBus/namespaces/sb-tripgen-location-dev/queues/geocoding \
    --condition "avg ActiveMessages > 10" \
    --description "Alert when geocoding queue has many pending messages"

# POI 업데이트 실패 알림
az monitor metrics alert create \
    --name "Location POI Update Failed Alert" \
    --resource-group rg-tripgen-dev \
    --scopes /subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-tripgen-dev/providers/Microsoft.ServiceBus/namespaces/sb-tripgen-location-dev/queues/poi-updates \
    --condition "avg DeadletteredMessages > 0" \
    --description "Alert when POI update messages fail"

# Route Calculation 타임아웃 알림
az monitor metrics alert create \
    --name "Location Route Calculation Timeout Alert" \
    --resource-group rg-tripgen-dev \
    --scopes /subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-tripgen-dev/providers/Microsoft.ServiceBus/namespaces/sb-tripgen-location-dev/queues/route-calculation \
    --condition "avg ActiveMessages > 5" \
    --description "Alert when route calculation takes too long"
```

## 성능 최적화

### 13. 지리적 데이터 캐싱 전략
```json
{
  "GeoDataCaching": {
    "GeocodingCache": {
      "Enabled": true,
      "TTL": "P7D",
      "MaxEntries": 100000,
      "Strategy": "LRU"
    },
    "POICache": {
      "Enabled": true,
      "TTL": "P1D",
      "MaxEntries": 50000,
      "Strategy": "LFU"
    },
    "RouteCache": {
      "Enabled": true,
      "TTL": "PT4H",
      "MaxEntries": 10000,
      "Strategy": "LRU"
    }
  }
}
```

### 14. 배치 처리 최적화
```json
{
  "BatchProcessing": {
    "GeocodingBatch": {
      "Enabled": true,
      "MaxBatchSize": 25,
      "BatchTimeout": "00:00:10"
    },
    "POIUpdateBatch": {
      "Enabled": true,
      "MaxBatchSize": 100,
      "BatchTimeout": "00:01:00"
    },
    "RouteCalculationBatch": {
      "Enabled": false,
      "Reason": "Each route calculation is unique"
    }
  }
}
```

## 데이터 품질 관리

### 15. 데이터 검증 및 정제
```json
{
  "DataQuality": {
    "CoordinateValidation": {
      "Enabled": true,
      "LatitudeRange": [-90, 90],
      "LongitudeRange": [-180, 180],
      "PrecisionCheck": true
    },
    "AddressValidation": {
      "Enabled": true,
      "MinLength": 5,
      "MaxLength": 500,
      "FormatCheck": true
    },
    "POIValidation": {
      "Enabled": true,
      "RequiredFields": ["name", "coordinates", "category"],
      "DuplicateCheck": true,
      "RadiusCheck": 50
    }
  }
}
```

### 16. 데이터 동기화
```json
{
  "DataSynchronization": {
    "ExternalAPISync": {
      "Enabled": true,
      "SyncInterval": "PT6H",
      "Sources": ["GooglePlaces", "KakaoMap"],
      "ConflictResolution": "LatestWins"
    },
    "CacheSync": {
      "Enabled": true,
      "SyncInterval": "PT1H",
      "SyncBatchSize": 1000
    }
  }
}
```

## 보안 고려사항

### 17. API 키 관리
```bash
# Key Vault 생성
az keyvault create \
    --name kv-tripgen-location-dev \
    --resource-group rg-tripgen-dev \
    --location koreacentral \
    --sku standard

# API 키 저장
az keyvault secret set \
    --vault-name kv-tripgen-location-dev \
    --name "GoogleMapsApiKey" \
    --value "<GOOGLE_MAPS_API_KEY>"

az keyvault secret set \
    --vault-name kv-tripgen-location-dev \
    --name "KakaoRestApiKey" \
    --value "<KAKAO_REST_API_KEY>"
```

### 18. 데이터 프라이버시
```json
{
  "DataPrivacy": {
    "LocationTracking": {
      "UserConsent": true,
      "DataRetention": "P1Y",
      "Anonymization": true
    },
    "PersonalData": {
      "Encryption": true,
      "AccessLogging": true,
      "DeletionPolicy": "OnRequest"
    }
  }
}
```

## 비용 최적화

### 19. API 사용량 최적화
```json
{
  "CostOptimization": {
    "APIUsageControl": {
      "DailyBudget": 50,
      "RequestThrottling": true,
      "CacheFirstPolicy": true
    },
    "DataStorageOptimization": {
      "DataCompression": true,
      "ArchivalPolicy": "P6M",
      "TierBasedStorage": true
    }
  }
}
```

### 20. Basic Tier 활용
- 무료 API 할당량 최대 활용
- 캐싱으로 API 호출 최소화
- 배치 처리로 효율성 증대

## 트러블슈팅

### 21. 일반적인 문제점
1. **Geocoding API 제한 초과**:
   - Rate Limiting 모니터링
   - 캐시 우선 정책 적용
   - 대체 API 사용

2. **POI 데이터 불일치**:
   - 데이터 검증 강화
   - 중복 감지 활성화
   - 충돌 해결 정책 적용

3. **Route Calculation 실패**:
   - 좌표 유효성 검사
   - 대체 경로 제공
   - 타임아웃 설정 조정

### 22. 로그 및 메트릭 확인
```bash
# Location Service 큐 상태 모니터링
az monitor metrics list \
    --resource /subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-tripgen-dev/providers/Microsoft.ServiceBus/namespaces/sb-tripgen-location-dev \
    --metric "ActiveMessages,CompletedMessages,DeadletteredMessages" \
    --interval PT1M \
    --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S.000Z)

# API 사용량 통계
az monitor log-analytics query \
    --workspace <WORKSPACE_ID> \
    --analytics-query "
    LocationServiceLogs 
    | where EventType == 'APICall'
    | summarize RequestCount=count(), SuccessRate=avg(case(Success==true, 1.0, 0.0)) by APIProvider, bin(TimeGenerated, 1h)
    | order by TimeGenerated desc
    "

# Geocoding 성능 분석
az monitor log-analytics query \
    --workspace <WORKSPACE_ID> \
    --analytics-query "
    ServiceBusQueueEvents 
    | where QueueName == 'geocoding'
    | extend ProcessingTime = CompletedTime - StartTime
    | summarize avg(ProcessingTime), max(ProcessingTime), count() by bin(TimeGenerated, 1h)
    | order by TimeGenerated desc
    "
```