# Message Queue 설치계획서 - 개발환경

## 1. 설치 계획 개요

### 1.1 설치 목적
- AI 기반 여행 일정 생성 서비스의 **개발환경** Message Queue 구축
- 비동기 메시지 처리를 통한 시스템 성능 향상 및 확장성 확보
- 마이크로서비스 간 느슨한 결합(Loose Coupling) 구현

### 1.2 설치 범위
- **대상 환경**: 개발환경 (tripgen-dev)
- **Message Queue**: Azure Service Bus Basic Tier
- **배포 방식**: Kubernetes 기반 설치
- **설치 대상 큐**: ai-schedule-generation, location-search, notification

### 1.3 참조 문서
- 물리 아키텍처: design/backend/physical/physical-architecture-dev.md
- 백킹서비스설치방법: claude/backing-service-method.md

## 2. 환경 분석

### 2.1 현재 시스템 구성
- **Kubernetes 클러스터**: AKS Basic (tripgen-dev namespace)
- **노드 풀**: Standard_B2s × 2개 (Spot Instance)
- **네트워크**: VNet 10.1.0.0/16, Message Subnet 10.1.3.0/24
- **보안 수준**: 기본 보안 설정

### 2.2 Message Queue 요구사항

#### 2.2.1 비즈니스 요구사항
| 요구사항 | 설명 | 우선순위 |
|----------|------|----------|
| AI 일정 생성 처리 | AI 서비스의 비동기 일정 생성 요청 처리 | 높음 |
| 위치 검색 요청 | Location 서비스의 검색 요청 큐잉 | 중간 |
| 알림 메시지 전송 | 사용자 알림 비동기 처리 | 중간 |
| 개발팀 테스트 | 개발 및 통합 테스트 지원 | 높음 |

#### 2.2.2 기술적 요구사항
| 구분 | 요구사항 | 개발환경 목표값 |
|------|----------|----------------|
| 처리량 | 초당 메시지 처리 | 100 msg/sec |
| 응답시간 | 메시지 처리 지연시간 | 1초 이내 |
| 가용성 | 서비스 가용성 | 95% |
| 메시지 크기 | 최대 메시지 크기 | 64KB |
| 큐 크기 | 큐당 최대 크기 | 256MB |
| 보존 기간 | 메시지 TTL | 14일 |

### 2.3 현재 서비스별 Message Queue 사용 계획

#### AI Service
- **Producer**: Trip Service → AI 일정 생성 요청
- **Consumer**: AI Service → 일정 생성 처리
- **큐명**: ai-schedule-generation
- **메시지 타입**: 일정 생성 요청, 재생성 요청

#### Location Service  
- **Producer**: Trip Service → 장소 검색 요청
- **Consumer**: Location Service → 검색 결과 처리
- **큐명**: location-search
- **메시지 타입**: 주변 장소 검색, 장소 상세 정보 요청

#### Notification Service
- **Producer**: 모든 서비스 → 알림 발송 요청
- **Consumer**: Notification Service → 알림 전송
- **큐명**: notification
- **메시지 타입**: 일정 생성 완료, 오류 알림

## 3. 설치 계획

### 3.1 Azure Service Bus 설치 계획

#### 3.1.1 네임스페이스 구성
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 네임스페이스명 | sb-tripgen-dev | 개발환경 전용 |
| SKU | Basic | 비용 최적화 계층 |
| 위치 | Korea Central | 메인 리전 |
| 최대 큐 크기 | 1GB | Basic Tier 제한 |
| 메시지 TTL | 14일 | 개발환경 적정 기간 |
| 액세스 정책 | RootManageSharedAccessKey | 개발용 통합 키 |

#### 3.1.2 큐별 상세 설정
```yaml
queues:
  ai-schedule-generation:
    max_size: 256MB
    duplicate_detection: false  # Basic tier limitation
    session_support: false     # Basic tier limitation
    partitioning: false        # Basic tier limitation
    dead_letter_queue: false   # Basic tier limitation
    
  location-search:
    max_size: 128MB
    duplicate_detection: false
    auto_delete_idle: P14D     # 14일 후 자동 삭제
    
  notification:
    max_size: 128MB
    duplicate_detection: false
    enable_batched_operations: true
```

### 3.2 Kubernetes 통합 계획

#### 3.2.1 Secret 관리
```yaml
# servicebus-secret-dev.yaml
apiVersion: v1
kind: Secret
metadata:
  name: servicebus-connection
  namespace: tripgen-dev
type: Opaque
data:
  connection-string: <base64-encoded-connection-string>
  namespace: c2ItdHJpcGdlbi1kZXY=  # sb-tripgen-dev
```

#### 3.2.2 ConfigMap 설정
```yaml
# servicebus-config-dev.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: servicebus-config
  namespace: tripgen-dev
data:
  SERVICEBUS_NAMESPACE: "sb-tripgen-dev"
  AI_QUEUE_NAME: "ai-schedule-generation"
  LOCATION_QUEUE_NAME: "location-search"
  NOTIFICATION_QUEUE_NAME: "notification"
  CONNECTION_TIMEOUT: "30"
  RETRY_COUNT: "3"
  MESSAGE_TTL: "P14D"
```

## 4. 설치 절차

### 4.1 사전 준비사항

#### 4.1.1 필수 도구 확인
```bash
# Azure CLI 설치 확인
az --version

# kubectl 설치 확인  
kubectl version --client

# Azure 로그인
az login

# AKS 클러스터 연결
az aks get-credentials --resource-group rg-tripgen-dev --name aks-tripgen-dev
```

#### 4.1.2 권한 확인
- Azure 구독에 대한 Contributor 권한
- AKS 클러스터에 대한 관리자 권한
- Service Bus 리소스 생성 권한

### 4.2 단계별 설치 절차

#### 4.2.1 Azure Service Bus 생성
```bash
# 1단계: 리소스 그룹 생성
az group create --name rg-tripgen-dev --location koreacentral

# 2단계: Service Bus 네임스페이스 생성
az servicebus namespace create \
  --name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --location koreacentral \
  --sku Basic

# 3단계: AI 일정 생성 큐 생성
az servicebus queue create \
  --namespace-name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --name ai-schedule-generation \
  --max-size 256

# 4단계: 위치 검색 큐 생성
az servicebus queue create \
  --namespace-name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --name location-search \
  --max-size 128

# 5단계: 알림 큐 생성
az servicebus queue create \
  --namespace-name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --name notification \
  --max-size 128
```

#### 4.2.2 연결 정보 획득
```bash
# 연결 문자열 획득
CONNECTION_STRING=$(az servicebus namespace authorization-rule keys list \
  --resource-group rg-tripgen-dev \
  --namespace-name sb-tripgen-dev \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString -o tsv)

echo "연결 문자열: $CONNECTION_STRING"
```

#### 4.2.3 Kubernetes 리소스 배포
```bash
# 1단계: Namespace 생성 (없는 경우)
kubectl create namespace tripgen-dev --dry-run=client -o yaml | kubectl apply -f -

# 2단계: Secret 생성
kubectl create secret generic servicebus-connection \
  --namespace tripgen-dev \
  --from-literal=connection-string="$CONNECTION_STRING"

# 3단계: ConfigMap 적용
kubectl apply -f k8s/dev/servicebus-config-dev.yaml

# 4단계: 네트워크 정책 적용 (필요한 경우)
kubectl apply -f k8s/dev/servicebus-network-policy.yaml
```

## 5. 연결 설정

### 5.1 애플리케이션별 연결 설정

#### 5.1.1 AI Service 설정
```yaml
# ai-service deployment에 추가할 환경 변수
env:
- name: SERVICEBUS_CONNECTION_STRING
  valueFrom:
    secretKeyRef:
      name: servicebus-connection
      key: connection-string
- name: AI_QUEUE_NAME
  valueFrom:
    configMapKeyRef:
      name: servicebus-config
      key: AI_QUEUE_NAME
- name: SERVICEBUS_RETRY_COUNT
  valueFrom:
    configMapKeyRef:
      name: servicebus-config
      key: RETRY_COUNT
```

#### 5.1.2 Trip Service 설정
```yaml
# trip-service deployment에 추가할 환경 변수
env:
- name: SERVICEBUS_CONNECTION_STRING
  valueFrom:
    secretKeyRef:
      name: servicebus-connection
      key: connection-string
- name: AI_QUEUE_NAME
  valueFrom:
    configMapKeyRef:
      name: servicebus-config
      key: AI_QUEUE_NAME
- name: LOCATION_QUEUE_NAME
  valueFrom:
    configMapKeyRef:
      name: servicebus-config
      key: LOCATION_QUEUE_NAME
```

#### 5.1.3 Location Service 설정
```yaml
# location-service deployment에 추가할 환경 변수
env:
- name: SERVICEBUS_CONNECTION_STRING
  valueFrom:
    secretKeyRef:
      name: servicebus-connection
      key: connection-string
- name: LOCATION_QUEUE_NAME
  valueFrom:
    configMapKeyRef:
      name: servicebus-config
      key: LOCATION_QUEUE_NAME
- name: NOTIFICATION_QUEUE_NAME
  valueFrom:
    configMapKeyRef:
      name: servicebus-config
      key: NOTIFICATION_QUEUE_NAME
```

### 5.2 Java 애플리케이션 연결 구성

#### 5.2.1 Spring Boot 설정
```yaml
# application-dev.yml
spring:
  cloud:
    azure:
      servicebus:
        connection-string: ${SERVICEBUS_CONNECTION_STRING}
        namespace: ${SERVICEBUS_NAMESPACE:sb-tripgen-dev}
        
servicebus:
  queues:
    ai-schedule-generation:
      receive-mode: RECEIVE_AND_DELETE
      max-auto-renew-duration: PT5M
    location-search:
      receive-mode: PEEK_LOCK
      max-auto-renew-duration: PT30S
    notification:
      receive-mode: RECEIVE_AND_DELETE
      max-auto-renew-duration: PT30S
      
  retry:
    max-retries: 3
    delay: PT1S
    max-delay: PT30S
    retry-mode: EXPONENTIAL
```

#### 5.2.2 Producer/Consumer 구현 예시
```java
// ServiceBusConfig.java
@Configuration
public class ServiceBusConfig {
    
    @Value("${SERVICEBUS_CONNECTION_STRING}")
    private String connectionString;
    
    // Producer Bean
    @Bean
    public ServiceBusSenderClient aiQueueSender() {
        return new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .sender()
            .queueName("ai-schedule-generation")
            .buildClient();
    }
    
    // Consumer Bean
    @Bean
    public ServiceBusProcessorClient aiQueueProcessor() {
        return new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .processor()
            .queueName("ai-schedule-generation")
            .processMessage(this::processAIMessage)
            .processError(this::processError)
            .buildProcessorClient();
    }
    
    private void processAIMessage(ServiceBusReceivedMessageContext context) {
        // AI 일정 생성 요청 처리 로직
    }
    
    private void processError(ServiceBusErrorContext context) {
        // 오류 처리 로직
    }
}
```

## 6. 설치 검증 계획

### 6.1 설치 후 검증 항목

#### 6.1.1 Azure Service Bus 검증
```bash
# 네임스페이스 상태 확인
az servicebus namespace show \
  --name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --query '{name:name,status:status,sku:sku.name}'

# 큐 목록 확인
az servicebus queue list \
  --namespace-name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --query '[].{name:name,status:status,sizeInBytes:sizeInBytes}'

# 큐별 상세 정보 확인
az servicebus queue show \
  --namespace-name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --name ai-schedule-generation \
  --query '{name:name,maxSizeInMegabytes:maxSizeInMegabytes,messageCount:messageCount}'
```

#### 6.1.2 Kubernetes 연결 검증
```bash
# Secret 생성 확인
kubectl get secret servicebus-connection -n tripgen-dev -o yaml

# ConfigMap 확인
kubectl get configmap servicebus-config -n tripgen-dev -o yaml

# Pod에서 환경 변수 확인
kubectl exec deployment/ai-service -n tripgen-dev -- env | grep SERVICEBUS
```

### 6.2 기능 테스트 계획

#### 6.2.1 메시지 송수신 테스트
```bash
# 테스트 메시지 전송 (Azure CLI)
az servicebus queue send \
  --namespace-name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --name ai-schedule-generation \
  --body '{"tripId": "test-001", "userId": "dev-user", "action": "generate"}'

# 메시지 수신 확인
az servicebus queue receive \
  --namespace-name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --name ai-schedule-generation \
  --max-count 1
```

#### 6.2.2 애플리케이션 통합 테스트
```java
// ServiceBusIntegrationTest.java
@Test
public void testAIScheduleMessageFlow() {
    // 1. Trip Service에서 AI 일정 생성 요청 전송
    GenerateScheduleRequest request = new GenerateScheduleRequest();
    request.setTripId("test-trip-001");
    request.setUserId("test-user");
    
    tripService.requestAIScheduleGeneration(request);
    
    // 2. AI Service에서 메시지 수신 및 처리 확인
    await().atMost(5, SECONDS).until(() -> 
        aiService.getJobStatus("test-trip-001").equals(JobStatus.COMPLETED));
    
    // 3. 결과 검증
    assertThat(aiService.getGeneratedSchedule("test-trip-001")).isNotNull();
}
```

## 7. 모니터링 및 관리

### 7.1 모니터링 설정

#### 7.1.1 메트릭 모니터링
```yaml
# Azure Monitor 메트릭
monitoring_metrics:
  - ActiveMessages: 활성 메시지 수
  - CompletedMessages: 완료된 메시지 수
  - IncomingMessages: 수신 메시지 수
  - OutgoingMessages: 발송 메시지 수
  - DeadLetterMessages: 데드레터 메시지 수 (Premium만)

# 알림 설정  
alerts:
  - metric: ActiveMessages
    threshold: 50
    action: Teams 알림
  - metric: CompletedMessages
    threshold: 0 (5분간)
    action: 개발팀 알림
```

#### 7.1.2 로깅 설정
```yaml
# Spring Boot Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info,servicebus
  metrics:
    export:
      azure-monitor:
        enabled: true
        
logging:
  level:
    com.azure.messaging.servicebus: DEBUG
    com.unicorn.tripgen: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 7.2 개발환경 관리 절차

#### 7.2.1 일상 관리
```bash
# 큐 상태 확인 (일 1회)
az servicebus queue list \
  --namespace-name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --query '[].{name:name,messageCount:messageCount,sizeInBytes:sizeInBytes}'

# 메시지 큐 정리 (필요시)
az servicebus queue purge \
  --namespace-name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --name ai-schedule-generation

# 연결 상태 테스트
kubectl exec deployment/ai-service -n tripgen-dev -- \
  curl -s http://localhost:8080/actuator/health | grep servicebus
```

#### 7.2.2 문제해결 절차
```bash
# 연결 문제 진단
kubectl logs deployment/ai-service -n tripgen-dev | grep -i servicebus

# Secret 값 확인
kubectl get secret servicebus-connection -n tripgen-dev -o jsonpath='{.data.connection-string}' | base64 -d

# Service Bus 메트릭 확인
az servicebus namespace show \
  --name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --query '{status:status,createdAt:createdAt,updatedAt:updatedAt}'
```

## 8. 백업 및 복구

### 8.1 백업 전략

#### 8.1.1 메시지 백업 (제한적)
- **Basic Tier 제약**: 자동 백업 미지원
- **수동 백업**: 중요 메시지는 애플리케이션 레벨에서 로깅
- **설정 백업**: Kubernetes YAML 파일을 Git으로 관리

#### 8.1.2 복구 전략
```bash
# Service Bus 재생성 스크립트
./scripts/recreate-servicebus-dev.sh

# Kubernetes 리소스 재배포
kubectl apply -f k8s/dev/servicebus/

# 애플리케이션 재시작
kubectl rollout restart deployment -n tripgen-dev
```

## 9. 보안 설정

### 9.1 개발환경 보안 정책

#### 9.1.1 기본 보안 설정
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 네트워크 액세스 | Public Endpoint | 개발 편의성 우선 |
| 인증 방식 | Connection String | 단순 인증 |
| TLS 버전 | 1.2 이상 | 기본 암호화 |
| IP 필터링 | Disabled | 개발팀 접근성 |

#### 9.1.2 Kubernetes 보안
```yaml
# Network Policy 예시 (선택사항)
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: servicebus-access
  namespace: tripgen-dev
spec:
  podSelector:
    matchLabels:
      tier: application
  policyTypes:
  - Egress
  egress:
  - to: []  # 모든 외부 연결 허용 (개발환경)
    ports:
    - protocol: TCP
      port: 5671  # AMQP over TLS
    - protocol: TCP
      port: 5672  # AMQP
```

## 10. 비용 최적화

### 10.1 개발환경 비용 관리

#### 10.1.1 예상 비용 (월간)
| 구성요소 | 사양 | 예상 비용 | 최적화 방안 |
|----------|------|-----------|-------------|
| Service Bus Basic | 네임스페이스 | $10 | 단일 네임스페이스 공유 |
| 네트워크 송신 | 기본 사용량 | $5 | 불필요한 메시지 최소화 |
| **총 비용** | | **$15** | |

#### 10.1.2 비용 절약 전략
- **야간/주말 자동 정리**: 개발용 메시지 자동 삭제
- **공유 네임스페이스**: 여러 개발자가 동일 Service Bus 사용
- **메시지 최적화**: 불필요한 대용량 메시지 방지

### 10.2 리소스 정리 스케줄

#### 10.2.1 자동 정리 스크립트
```bash
#!/bin/bash
# cleanup-dev-messages.sh
# 매일 자정 실행되는 정리 스크립트

echo "개발환경 메시지 큐 정리 시작..."

# 모든 큐의 메시지 정리 (개발용)
QUEUES=("ai-schedule-generation" "location-search" "notification")

for queue in "${QUEUES[@]}"; do
    echo "큐 정리: $queue"
    az servicebus queue purge \
      --namespace-name sb-tripgen-dev \
      --resource-group rg-tripgen-dev \
      --name $queue
done

echo "정리 완료"
```

## 11. 설치 완료 후 확인사항

### 11.1 최종 검증 체크리스트

- [ ] Azure Service Bus 네임스페이스 생성 완료
- [ ] 3개 큐 (ai-schedule-generation, location-search, notification) 생성 확인
- [ ] Kubernetes Secret 및 ConfigMap 배포 완료
- [ ] 각 서비스에서 연결 문자열 환경 변수 설정 완료
- [ ] 메시지 송수신 테스트 성공
- [ ] 애플리케이션 로그에서 Service Bus 연결 확인
- [ ] 모니터링 메트릭 수집 확인

### 11.2 다음 단계

#### 11.2.1 통합 테스트
1. **Trip Service** → **AI Service** 메시지 플로우 테스트
2. **Location Service** 검색 요청 비동기 처리 테스트  
3. **Notification Service** 알림 발송 테스트
4. 전체 시스템 End-to-End 테스트

#### 11.2.2 성능 테스트
1. 부하 테스트: 100 msg/sec 처리 능력 확인
2. 지연시간 테스트: 1초 이내 메시지 처리 확인
3. 큐 용량 테스트: 256MB 한계 도달 시 동작 확인

#### 11.2.3 문서화
1. 개발팀 사용 가이드 작성
2. 문제해결 가이드 작성
3. API 연동 예시 코드 작성

---

**설치 담당자**: 한데브옵스(클라우더)  
**검토자**: 김개발(테키), 정백엔드(서버맨)  
**승인자**: Product Owner  
**작성일**: 2025-08-07  
**예상 소요시간**: 4시간 (설치 2시간, 테스트 2시간)