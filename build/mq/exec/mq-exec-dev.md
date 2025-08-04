# Azure Service Bus 설치 결과서 - TripGen 개발환경

## 1. 설치 개요

### 1.1 설치 정보
- **설치일시**: 2025-08-04 12:17:35 (KST)
- **설치환경**: 개발환경 (dev)
- **설치자**: DevOps 팀 (클라우더)
- **Azure 구독**: sub-900001042-digitalgarage-03
- **설치방법**: Azure CLI 사용

### 1.2 설치 범위
- Azure Service Bus Basic Tier 네임스페이스 생성
- AI 일정 생성, 위치 검색, 알림 큐 생성
- 연결 문자열 획득 및 검증 완료

## 2. 설치 결과

### 2.1 Service Bus 네임스페이스
| 항목 | 설정값 | 상태 |
|------|--------|------|
| 네임스페이스 이름 | sb-tripgen-dev | ✅ Active |
| 리소스 그룹 | rg-tripgen-dev | ✅ 생성됨 |
| 위치 | Korea Central | ✅ 설정됨 |
| SKU | Basic | ✅ 적용됨 |
| 프로비저닝 상태 | Succeeded | ✅ 성공 |
| 엔드포인트 | https://sb-tripgen-dev.servicebus.windows.net:443/ | ✅ 활성화 |

### 2.2 생성된 큐
| 큐 이름 | 최대 크기 | 메시지 TTL | 상태 | 현재 메시지 수 |
|---------|-----------|-------------|------|----------------|
| ai-schedule-generation | 1024MB | 14일 | ✅ Active | 0 |
| location-search | 1024MB | 14일 | ✅ Active | 0 |
| notification | 1024MB | 14일 | ✅ Active | 0 |

### 2.3 태그 정보
정책 준수를 위해 다음 태그가 적용되었습니다:
| 태그 이름 | 값 |
|-----------|-----|
| EMAIL | dev@tripgen.com |
| ServiceName | TripGen |
| Name | sb-tripgen-dev |
| CreateDate | 2025-08-04 |

## 3. 연결 정보

### 3.1 연결 문자열
- **연결 문자열 획득**: ✅ 성공
- **연결 문자열 길이**: 159 문자
- **인증 방식**: SharedAccessKey (RootManageSharedAccessKey)

#### 연결 문자열 획득 명령어
```bash
# Azure Service Bus 연결 문자열 획득
az servicebus namespace authorization-rule keys list \
  --namespace-name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString \
  --output tsv

# 보조 연결 문자열 획득 (백업용)
az servicebus namespace authorization-rule keys list \
  --namespace-name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --name RootManageSharedAccessKey \
  --query secondaryConnectionString \
  --output tsv
```

### 3.2 애플리케이션 연결 방법

#### Spring Boot 애플리케이션 설정 (application.yml)
```yaml
spring:
  cloud:
    azure:
      servicebus:
        connection-string: ${AZURE_SERVICEBUS_CONNECTION_STRING}
        
# 큐별 설정
azure:
  servicebus:
    queues:
      ai-schedule: ai-schedule-generation
      location-search: location-search
      notification: notification
```

#### Node.js 애플리케이션 설정
```javascript
const { ServiceBusClient } = require("@azure/service-bus");

const connectionString = process.env.AZURE_SERVICEBUS_CONNECTION_STRING;
const sbClient = new ServiceBusClient(connectionString);

// 큐 사용 예시
const sender = sbClient.createSender("ai-schedule-generation");
const receiver = sbClient.createReceiver("ai-schedule-generation");
```

#### Python 애플리케이션 설정
```python
from azure.servicebus import ServiceBusClient

connection_str = os.environ['AZURE_SERVICEBUS_CONNECTION_STRING']
servicebus_client = ServiceBusClient.from_connection_string(connection_str)

# 큐 사용 예시
sender = servicebus_client.get_queue_sender(queue_name="ai-schedule-generation")
receiver = servicebus_client.get_queue_receiver(queue_name="ai-schedule-generation")
```

### 3.3 Kubernetes Secret 생성 (권장)
```bash
# 연결 문자열을 Secret으로 저장
kubectl create secret generic servicebus-connection \
  --from-literal=connectionString="<연결문자열>" \
  --namespace tripgen-dev
```

## 4. 검증 결과

### 4.1 설치 검증
| 검증 항목 | 결과 | 비고 |
|-----------|------|------|
| 네임스페이스 생성 | ✅ 성공 | Active 상태 확인 |
| 큐 생성 (3개) | ✅ 성공 | 모든 큐 Active 상태 |
| 연결 문자열 획득 | ✅ 성공 | 159자 확인 |
| 네트워크 접근성 | ✅ 성공 | Public 접근 가능 |

### 4.2 보안 설정
| 보안 항목 | 상태 | 설명 |
|-----------|------|------|
| 공용 네트워크 액세스 | Enabled | 개발환경 편의성 |
| 로컬 인증 | Enabled | Connection String 사용 |
| TLS 버전 | 1.2 | 최소 TLS 버전 |

## 5. 주의사항

### 5.1 개발환경 제약사항
- Basic Tier의 제한사항:
  - Topic/Subscription 기능 미지원
  - 메시지 배치 처리 미지원
  - 세션 및 중복 검색 기능 미지원
  - 최대 메시지 크기: 256KB

### 5.2 비용 정보
- 예상 월간 비용: 약 $10-11
  - Service Bus Basic: $0.05/시간
  - 메시지 작업: $0.80/백만 건

### 5.3 운영환경 전환 시 고려사항
1. Premium Tier로 업그레이드 필요
2. Private Endpoint 구성 추가
3. Zone Redundancy 활성화
4. 고가용성 설정 적용

## 6. 다음 단계

### 6.1 애플리케이션 통합
1. 연결 문자열을 환경 변수로 설정
2. 각 마이크로서비스에서 큐 연결 구성
3. 메시지 송수신 로직 구현
4. 오류 처리 및 재시도 정책 설정

### 6.2 모니터링 설정
1. Azure Monitor에서 Service Bus 메트릭 확인
2. 큐별 메시지 처리량 모니터링
3. 알림 규칙 설정 (선택사항)

## 7. 문제 해결

### 7.1 연결 실패 시
```bash
# Service Bus 상태 확인
az servicebus namespace show \
  --name sb-tripgen-dev \
  --resource-group rg-tripgen-dev \
  --query provisioningState

# 네트워크 연결 테스트
nslookup sb-tripgen-dev.servicebus.windows.net
telnet sb-tripgen-dev.servicebus.windows.net 443
```

### 7.2 권한 문제 시
- 연결 문자열의 SharedAccessKey 권한 확인
- Send, Listen, Manage 권한이 모두 있는지 확인

## 8. 관련 문서
- MQ 설치계획서: build/mq/mq-plan-tripgen-dev.md
- 물리 아키텍처: design/backend/physical/physical-architecture-dev.md
- Azure Service Bus 문서: https://docs.microsoft.com/azure/service-bus-messaging/

---

## 설치 완료 확인
✅ Azure Service Bus 개발환경 설치가 성공적으로 완료되었습니다.
✅ 모든 큐가 정상적으로 생성되고 Active 상태입니다.
✅ 애플리케이션에서 연결할 준비가 완료되었습니다.