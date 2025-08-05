# AI Service Message Queue 설치 계획서 (개발환경)

## 개요
- **서비스명**: AI Service
- **환경**: Development
- **Message Queue**: Azure Service Bus Basic Tier
- **목적**: AI 기반 여행 일정 생성 및 추천 관련 비동기 메시징 처리

## Azure Service Bus 설정

### 1. Service Bus Namespace 생성
```bash
# Resource Group 확인 (기존 그룹 사용)
az group show --name rg-tripgen-dev

# Service Bus Namespace 생성 (Basic Tier)
az servicebus namespace create \
    --resource-group rg-tripgen-dev \
    --name sb-tripgen-ai-dev \
    --location koreacentral \
    --sku Basic
```

### 2. Queue 설정

#### 2.1 AI Processing Queue
```bash
# AI 처리 요청용 큐 생성
az servicebus queue create \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-ai-dev \
    --name ai-processing \
    --max-size 5120 \
    --lock-duration PT600S \
    --default-message-time-to-live P14D \
    --max-delivery-count 3 \
    --enable-partitioning false \
    --enable-duplicate-detection true \
    --duplicate-detection-history-time-window PT30M
```

#### 2.2 AI Results Queue
```bash
# AI 처리 결과용 큐 생성
az servicebus queue create \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-ai-dev \
    --name ai-results \
    --max-size 5120 \
    --lock-duration PT30S \
    --default-message-time-to-live P14D \
    --enable-partitioning false \
    --enable-duplicate-detection false
```

#### 2.3 AI Training Queue
```bash
# AI 모델 훈련용 큐 생성
az servicebus queue create \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-ai-dev \
    --name ai-training \
    --max-size 5120 \
    --lock-duration PT1800S \
    --default-message-time-to-live P30D \
    --max-delivery-count 1 \
    --enable-partitioning false \
    --enable-duplicate-detection true \
    --duplicate-detection-history-time-window PT1H
```

#### 2.4 AI Feedback Queue
```bash
# AI 피드백 수집용 큐 생성
az servicebus queue create \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-ai-dev \
    --name ai-feedback \
    --max-size 5120 \
    --lock-duration PT30S \
    --default-message-time-to-live P90D \
    --enable-partitioning false \
    --enable-duplicate-detection false
```

### 3. 접근 정책 설정

#### 3.1 Managed Identity 생성
```bash
# AI Service용 Managed Identity 생성
az identity create \
    --resource-group rg-tripgen-dev \
    --name mi-ai-service-dev
```

#### 3.2 Service Bus 권한 할당
```bash
# Service Bus Data Owner 권한 할당
az role assignment create \
    --assignee $(az identity show --resource-group rg-tripgen-dev --name mi-ai-service-dev --query principalId -o tsv) \
    --role "Azure Service Bus Data Owner" \
    --scope /subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-tripgen-dev/providers/Microsoft.ServiceBus/namespaces/sb-tripgen-ai-dev
```

## 연결 설정

### 4. Connection String 구성
```json
{
  "ServiceBus": {
    "ConnectionString": "Endpoint=sb://sb-tripgen-ai-dev.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=<ACCESS_KEY>",
    "QueueNames": {
      "AIProcessing": "ai-processing",
      "AIResults": "ai-results",
      "AITraining": "ai-training",
      "AIFeedback": "ai-feedback"
    }
  }
}
```

### 5. Managed Identity 연결 (권장)
```json
{
  "ServiceBus": {
    "FullyQualifiedNamespace": "sb-tripgen-ai-dev.servicebus.windows.net",
    "ManagedIdentity": {
      "ClientId": "<MANAGED_IDENTITY_CLIENT_ID>"
    },
    "QueueNames": {
      "AIProcessing": "ai-processing",
      "AIResults": "ai-results", 
      "AITraining": "ai-training",
      "AIFeedback": "ai-feedback"
    }
  }
}
```

## Message 핸들링 설정

### 6. Queue 구성 세부사항
- **최대 큐 크기**: 5GB
- **메시지 Lock Duration**: 
  - Processing: 600초 (10분) - AI 처리 시간 고려
  - Results: 30초 - 빠른 결과 전달
  - Training: 1800초 (30분) - 모델 훈련 시간 고려
  - Feedback: 30초 - 실시간 피드백 처리
- **메시지 TTL**: 
  - 일반: 14일
  - Training: 30일 (재훈련 가능성)
  - Feedback: 90일 (장기 분석용)
- **재시도 횟수**: 
  - Processing: 3회
  - Training: 1회 (리소스 집약적)
- **중복 감지**: Processing, Training 큐에서 활성화

### 7. 메시지 유형별 설정

#### AI Processing 메시지
```json
{
  "MessageType": "AIProcessingRequest",
  "Properties": {
    "RequestId": "string",
    "RequestType": "TripGeneration|Recommendation|Optimization",
    "UserId": "string",
    "InputData": {
      "Destination": "string",
      "StartDate": "date",
      "EndDate": "date",
      "Budget": "decimal",
      "Preferences": "object",
      "UserHistory": "object"
    },
    "ModelVersion": "string",
    "Priority": "Critical|High|Medium|Low",
    "Timeout": "int",
    "CallbackQueue": "string"
  }
}
```

#### AI Results 메시지
```json
{
  "MessageType": "AIProcessingResult",
  "Properties": {
    "RequestId": "string",
    "Status": "Success|Failed|Timeout",
    "Result": {
      "TripPlan": "object",
      "Confidence": "decimal",
      "AlternativeOptions": "array"
    },
    "ProcessingTime": "int",
    "ModelUsed": "string",
    "ErrorMessage": "string"
  }
}
```

#### AI Training 메시지
```json
{
  "MessageType": "AITrainingRequest",
  "Properties": {
    "TrainingId": "string",
    "TrainingType": "Incremental|FullRetrain|FineTuning",
    "DataSource": "string",
    "ModelType": "TripGeneration|UserPreference|LocationRecommendation",
    "DataPeriod": {
      "StartDate": "date",
      "EndDate": "date"
    },
    "Priority": "High|Medium|Low"
  }
}
```

#### AI Feedback 메시지
```json
{
  "MessageType": "AIFeedback",
  "Properties": {
    "FeedbackId": "string",
    "RequestId": "string",
    "UserId": "string",
    "FeedbackType": "Rating|Correction|UserBehavior",
    "Rating": "int",
    "Comments": "string",
    "UserAction": "Accepted|Modified|Rejected",
    "TripPlanUsed": "boolean",
    "Timestamp": "datetime"
  }
}
```

## 초기화 명령어

### 8. 검증 스크립트
```bash
#!/bin/bash
# AI Service Message Queue 연결 테스트
echo "Testing AI Service Bus connection..."

# Queue 상태 확인
echo "Checking ai-processing queue..."
az servicebus queue show \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-ai-dev \
    --name ai-processing \
    --query '{Name:name, Status:status, MessageCount:messageCount, DeadLetterCount:deadLetterMessageCount}'

echo "Checking ai-results queue..."
az servicebus queue show \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-ai-dev \
    --name ai-results \
    --query '{Name:name, Status:status, MessageCount:messageCount}'

echo "Checking ai-training queue..."
az servicebus queue show \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-ai-dev \
    --name ai-training \
    --query '{Name:name, Status:status, MessageCount:messageCount}'

echo "Checking ai-feedback queue..."
az servicebus queue show \
    --resource-group rg-tripgen-dev \
    --namespace-name sb-tripgen-ai-dev \
    --name ai-feedback \
    --query '{Name:name, Status:status, MessageCount:messageCount}'

echo "AI Service Message Queue setup completed!"
```

## AI 모델 통합

### 9. OpenAI API 연동 설정
```json
{
  "OpenAI": {
    "ApiKey": "<OPENAI_API_KEY>",
    "Organization": "<ORGANIZATION_ID>",
    "Models": {
      "TripGeneration": "gpt-4-turbo",
      "Recommendation": "gpt-3.5-turbo",
      "TextProcessing": "text-embedding-ada-002"
    },
    "RateLimiting": {
      "RequestsPerMinute": 60,
      "TokensPerMinute": 150000
    }
  }
}
```

### 10. Azure Cognitive Services 연동
```bash
# Cognitive Services 계정 생성
az cognitiveservices account create \
    --name cs-tripgen-ai-dev \
    --resource-group rg-tripgen-dev \
    --kind TextAnalytics \
    --sku S0 \
    --location koreacentral
```

## 모니터링 설정

### 11. Application Insights 연동
```bash
# Application Insights 생성
az monitor app-insights component create \
    --app ai-service-insights-dev \
    --location koreacentral \
    --resource-group rg-tripgen-dev \
    --kind web

# AI Service 성능 카운터 설정
az monitor app-insights component create \
    --app ai-ml-performance-dev \
    --location koreacentral \
    --resource-group rg-tripgen-dev \
    --kind other
```

### 12. 알림 규칙 설정
```bash
# AI Processing Queue 처리 시간 알림
az monitor metrics alert create \
    --name "AI Processing Long Running Alert" \
    --resource-group rg-tripgen-dev \
    --scopes /subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-tripgen-dev/providers/Microsoft.ServiceBus/namespaces/sb-tripgen-ai-dev/queues/ai-processing \
    --condition "avg ActiveMessages > 5" \
    --description "Alert when AI processing queue has many pending messages"

# AI Training 실패 알림
az monitor metrics alert create \
    --name "AI Training Failed Alert" \
    --resource-group rg-tripgen-dev \
    --scopes /subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-tripgen-dev/providers/Microsoft.ServiceBus/namespaces/sb-tripgen-ai-dev/queues/ai-training \
    --condition "avg DeadletteredMessages > 0" \
    --description "Alert when AI training messages fail"
```

## 성능 최적화

### 13. AI 처리 최적화 설정
```json
{
  "AIProcessingOptimization": {
    "BatchProcessing": {
      "Enabled": true,
      "MaxBatchSize": 5,
      "BatchTimeout": "00:02:00"
    },
    "Caching": {
      "Enabled": true,
      "CacheDuration": "01:00:00",
      "SimilarityThreshold": 0.8
    },
    "ModelLoadBalancing": {
      "Enabled": true,
      "MaxConcurrentRequests": 10,
      "QueueThreshold": 20
    }
  }
}
```

### 14. 리소스 관리
```json
{
  "ResourceManagement": {
    "GPUUtilization": {
      "MaxUsage": "80%",
      "CooldownPeriod": "00:05:00"
    },
    "MemoryManagement": {
      "MaxMemoryUsage": "16GB",
      "GarbageCollection": "Automatic"
    },
    "ModelCaching": {
      "PreloadModels": ["gpt-4-turbo", "text-embedding-ada-002"],
      "CacheSize": "8GB"
    }
  }
}
```

## 보안 및 데이터 보호

### 15. 데이터 보안
```json
{
  "DataSecurity": {
    "Encryption": {
      "InTransit": true,
      "AtRest": true,
      "KeyManagement": "Azure Key Vault"
    },
    "DataRetention": {
      "TrainingData": "P2Y",
      "ProcessingLogs": "P1Y",
      "UserFeedback": "P3Y"
    },
    "PIIHandling": {
      "Detection": true,
      "Masking": true,
      "Anonymization": true
    }
  }
}
```

### 16. API 보안
- OpenAI API 키 보안 저장
- Rate Limiting 적용
- Request/Response 로깅 제한
- 모델 출력 필터링

## 비용 최적화

### 17. AI 서비스 비용 관리
```json
{
  "CostOptimization": {
    "ModelSelection": {
      "Strategy": "Dynamic",
      "CostThreshold": "$100/day",
      "FallbackModel": "gpt-3.5-turbo"
    },
    "RequestOptimization": {
      "TokenLimiting": true,
      "MaxTokensPerRequest": 4000,
      "ResponseCaching": true
    },
    "TrainingOptimization": {
      "ScheduledTraining": "Weekly",
      "IncrementalOnly": true,
      "CostBudget": "$500/month"
    }
  }
}
```

### 18. Basic Tier 활용
- Queue 크기 모니터링
- 불필요한 메시지 정리
- 배치 처리로 효율성 증대

## 트러블슈팅

### 19. 일반적인 문제점
1. **AI 모델 응답 지연**: 
   - Lock Duration 증가
   - 병렬 처리 수 조정
   - 모델 캐싱 활용

2. **Training Queue 실패**:
   - 데이터 형식 검증
   - 리소스 사용량 확인
   - 타임아웃 설정 조정

3. **API 요청 제한**:
   - Rate Limiting 모니터링
   - Retry 정책 적용
   - 백오프 전략 구현

### 20. 로그 및 메트릭 확인
```bash
# AI Service 큐 상태 모니터링
az monitor metrics list \
    --resource /subscriptions/$(az account show --query id -o tsv)/resourceGroups/rg-tripgen-dev/providers/Microsoft.ServiceBus/namespaces/sb-tripgen-ai-dev \
    --metric "ActiveMessages,CompletedMessages,DeadletteredMessages" \
    --interval PT5M \
    --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S.000Z) \
    --end-time $(date -u +%Y-%m-%dT%H:%M:%S.000Z)

# AI 처리 시간 통계
az monitor log-analytics query \
    --workspace <WORKSPACE_ID> \
    --analytics-query "
    ServiceBusQueueEvents 
    | where QueueName == 'ai-processing'
    | summarize avg(ProcessingTime), max(ProcessingTime), count() by bin(TimeGenerated, 1h)
    | order by TimeGenerated desc
    "
```