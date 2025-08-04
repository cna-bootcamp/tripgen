# Azure Service Bus 연결 가이드

## 1. Managed Identity를 사용한 연결 (권장)

### .NET 애플리케이션
```csharp
using Azure.Identity;
using Azure.Messaging.ServiceBus;

// Managed Identity를 사용한 연결
var client = new ServiceBusClient(
    "sb-tripgen-ai-dev.servicebus.windows.net",
    new DefaultAzureCredential()
);

// Queue 사용 예시
var sender = client.CreateSender("ai-processing");
await sender.SendMessageAsync(new ServiceBusMessage("Hello World"));
```

### Java 애플리케이션
```java
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.messaging.servicebus.*;

// Managed Identity를 사용한 연결
ServiceBusClientBuilder builder = new ServiceBusClientBuilder()
    .fullyQualifiedNamespace("sb-tripgen-ai-dev.servicebus.windows.net")
    .credential(new DefaultAzureCredentialBuilder().build());

ServiceBusSenderClient sender = builder
    .sender()
    .queueName("ai-processing")
    .buildClient();
```

### Python 애플리케이션
```python
from azure.identity import DefaultAzureCredential
from azure.servicebus import ServiceBusClient

# Managed Identity를 사용한 연결
credential = DefaultAzureCredential()
client = ServiceBusClient(
    fully_qualified_namespace="sb-tripgen-ai-dev.servicebus.windows.net",
    credential=credential
)

# Queue 사용 예시
with client:
    sender = client.get_queue_sender(queue_name="ai-processing")
    with sender:
        sender.send_messages(ServiceBusMessage("Hello World"))
```

### Node.js 애플리케이션
```javascript
const { DefaultAzureCredential } = require("@azure/identity");
const { ServiceBusClient } = require("@azure/service-bus");

// Managed Identity를 사용한 연결
const credential = new DefaultAzureCredential();
const client = new ServiceBusClient(
    "sb-tripgen-ai-dev.servicebus.windows.net",
    credential
);

// Queue 사용 예시
const sender = client.createSender("ai-processing");
await sender.sendMessages({ body: "Hello World" });
```

## 2. 연결 문자열을 사용한 연결 (개발 환경용)

### 연결 문자열 얻기
```bash
# AI Service
az servicebus namespace authorization-rule keys list \
  --resource-group rg-tripgen-dev \
  --namespace-name sb-tripgen-ai-dev \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString \
  --output tsv

# Location Service
az servicebus namespace authorization-rule keys list \
  --resource-group rg-tripgen-dev \
  --namespace-name sb-tripgen-location-dev \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString \
  --output tsv

# Trip Service
az servicebus namespace authorization-rule keys list \
  --resource-group rg-tripgen-dev \
  --namespace-name sb-tripgen-trip-dev \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString \
  --output tsv

# User Service
az servicebus namespace authorization-rule keys list \
  --resource-group rg-tripgen-dev \
  --namespace-name sb-tripgen-user-dev \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString \
  --output tsv
```

## 3. 환경 변수 설정

### Linux/Mac
```bash
export AZURE_SERVICE_BUS_NAMESPACE="sb-tripgen-ai-dev.servicebus.windows.net"
export AZURE_CLIENT_ID="2b5c10ac-9359-4c1e-9920-c7f8364d89f5"
```

### Windows
```powershell
$env:AZURE_SERVICE_BUS_NAMESPACE = "sb-tripgen-ai-dev.servicebus.windows.net"
$env:AZURE_CLIENT_ID = "2b5c10ac-9359-4c1e-9920-c7f8364d89f5"
```

### Docker/Kubernetes
```yaml
env:
- name: AZURE_SERVICE_BUS_NAMESPACE
  value: "sb-tripgen-ai-dev.servicebus.windows.net"
- name: AZURE_CLIENT_ID
  value: "2b5c10ac-9359-4c1e-9920-c7f8364d89f5"
```

## 4. 서비스별 연결 정보

| Service | Namespace | Principal ID |
|---------|-----------|--------------|
| AI Service | sb-tripgen-ai-dev.servicebus.windows.net | 2b5c10ac-9359-4c1e-9920-c7f8364d89f5 |
| Location Service | sb-tripgen-location-dev.servicebus.windows.net | fee949bb-d9ed-4ba1-b2ca-0980be233de7 |
| Trip Service | sb-tripgen-trip-dev.servicebus.windows.net | 4522bac2-b479-4fe5-bbb4-a713410f0e0f |
| User Service | sb-tripgen-user-dev.servicebus.windows.net | 2854302a-ebf0-4a8c-a83f-31e2d37825be |

## 5. 테스트 연결

### Azure CLI로 테스트
```bash
# Service Bus Explorer 설치 (선택사항)
# https://github.com/paolosalvatori/ServiceBusExplorer

# 또는 Azure Portal에서 Service Bus Explorer 사용
# 1. Azure Portal 접속
# 2. 해당 Service Bus namespace로 이동
# 3. "Service Bus Explorer" 메뉴 선택
# 4. Queue 선택 후 메시지 송수신 테스트
```

## 6. 문제 해결

### 연결 실패 시 확인사항
1. Managed Identity가 올바르게 설정되었는지 확인
2. RBAC 권한이 부여되었는지 확인
3. 네트워크 방화벽 규칙 확인
4. Service Bus namespace가 Active 상태인지 확인

### 로그 확인
```bash
# Application Insights 로그 확인
az monitor app-insights query \
  --app <app-insights-name> \
  --analytics-query "traces | where message contains 'ServiceBus'"
```