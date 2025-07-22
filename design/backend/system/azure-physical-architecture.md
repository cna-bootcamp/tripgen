# Azure Cloud 기반 여행 일정 생성 서비스 - 물리 아키텍처 설계서

## 📋 개요

본 문서는 Azure Cloud 기반 마이크로서비스 아키텍처로 구현되는 여행 일정 생성 서비스의 물리적 배포 아키텍처를 설계합니다.

### 🎯 설계 목표
- **클라우드 네이티브**: Azure 관리형 서비스 최대 활용
- **마이크로서비스**: 서비스별 독립 배포 및 확장
- **고가용성**: 99.9% 이상 SLA 보장
- **보안 우선**: 제로 트러스트 보안 모델
- **비용 효율성**: 자동 스케일링 및 리소스 최적화

## 🏗️ 전체 아키텍처 개요

### Azure 서비스 매핑

| 논리 구성요소 | Azure 서비스 | SKU/티어 | 목적 |
|--------------|-------------|----------|------|
| **API Gateway** | Azure API Management | Developer | API 라우팅, 인증, 정책 |
| **마이크로서비스** | Azure Container Apps | Consumption | 서버리스 컨테이너 |
| **캐시** | Azure Cache for Redis | Premium P1 | 고성능 인메모리 캐시 |
| **메시징** | Azure Service Bus | Premium | 비동기 Job Queue |
| **데이터베이스** | PostgreSQL Flexible Server | General Purpose | 관리형 PostgreSQL |
| **파일 저장소** | Azure Blob Storage | Hot/Cool | 첨부파일 저장 |
| **외부 API 프록시** | Azure Functions | Consumption | 서버리스 함수 |
| **글로벌 라우팅** | Azure Traffic Manager | Standard | DNS 기반 라우팅 |
| **CDN + WAF** | Azure Front Door | Standard | 엣지 캐싱 및 보안 |

## 🌐 네트워크 아키텍처

### Hub-Spoke 토폴로지

```
Hub VNet (10.0.0.0/16)
├── Azure Firewall (10.0.1.0/24)
├── VPN Gateway (10.0.2.0/24)
└── Peering → Spoke VNet

Spoke VNet (10.1.0.0/16)
├── Web Tier (10.1.1.0/24) - Application Gateway, API Management
├── App Tier (10.1.2.0/24) - Container Apps
├── Integration (10.1.3.0/24) - Service Bus, Redis
├── Data Tier (10.1.4.0/24) - PostgreSQL, Blob Storage
└── Private Endpoints (10.1.5.0/24) - Private Link
```

### 네트워크 보안

#### Network Security Groups (NSG)
- **Web Tier**: HTTPS(443), HTTP(80) 인바운드만 허용
- **App Tier**: Web Tier에서 8080 포트만 허용
- **Data Tier**: App Tier에서 5432(PostgreSQL)만 허용
- **기본 정책**: Deny All, 명시적 Allow만 적용

#### Application Security Groups (ASG)
- **WebServers**: Application Gateway 그룹
- **AppServers**: Container Apps 그룹  
- **DataServers**: PostgreSQL 서버 그룹
- **CacheServers**: Redis 클러스터 그룹

## 🔐 보안 아키텍처

### 제로 트러스트 보안 모델

#### 1. **Azure Active Directory 통합**
- **OAuth 2.0/OIDC**: 표준 인증 프로토콜
- **Multi-Factor Authentication**: 관리자 계정 MFA 필수
- **조건부 액세스**: 위치/디바이스 기반 접근 제어
- **Privileged Identity Management**: 권한 최소화

#### 2. **Azure Key Vault (Premium HSM)**
- **시크릿 관리**: 데이터베이스 연결 문자열, API 키
- **인증서 관리**: SSL/TLS 인증서 자동 갱신
- **키 관리**: 데이터 암호화 키 (AES-256)
- **키 순환 정책**: 
  - 데이터베이스 키: 90일
  - API 키: 180일
  - SSL 인증서: 1년

#### 3. **데이터 암호화**
- **전송 중 암호화**: TLS 1.2+ 모든 통신
- **저장 중 암호화**: AES-256, Customer Managed Keys
- **mTLS**: 서비스 메시 내부 통신
- **DB 투명 암호화**: PostgreSQL TDE 활성화

#### 4. **WAF 및 방화벽**
```yaml
Application Gateway WAF 규칙:
- OWASP Core Rule Set 3.2
- DDoS 방어: 초당 1000 요청 제한
- Geo-blocking: 특정 국가 차단
- Custom Rules:
  - Rate Limiting: IP당 분당 100 요청
  - SQL Injection 차단
  - XSS 차단
```

## 🚀 컨테이너 및 서버리스 아키텍처

### Azure Container Apps 구성

#### 프로파일 서비스
```yaml
Profile Service:
  환경: Consumption Plan
  리소스:
    CPU: 0.25-2.0 vCPU
    Memory: 0.5-4.0 GB
  스케일링:
    Min Replicas: 1
    Max Replicas: 10
    스케일 규칙:
      - HTTP 요청: 100 req/instance
      - CPU: 70%
      - Memory: 80%
```

#### 일정 서비스
```yaml  
Itinerary Service:
  환경: Consumption Plan
  리소스:
    CPU: 0.5-4.0 vCPU
    Memory: 1.0-8.0 GB
  스케일링:
    Min Replicas: 2
    Max Replicas: 20
    스케일 규칙:
      - Service Bus 큐: 10 msg/instance
      - HTTP 요청: 50 req/instance
      - CPU: 70%
```

#### 장소 서비스
```yaml
Location Service:
  환경: Consumption Plan  
  리소스:
    CPU: 0.25-2.0 vCPU
    Memory: 0.5-4.0 GB
  스케일링:
    Min Replicas: 1
    Max Replicas: 15
    스케일 규칙:
      - HTTP 요청: 80 req/instance
      - CPU: 70%
```

### Azure Functions (MCP 프록시)
```yaml
MCP Proxy Functions:
  Plan: Consumption
  Runtime: Node.js 18
  Functions:
    - kakao-mcp-proxy: 카카오 API 호출
    - google-mcp-proxy: 구글 API 호출
  Configuration:
    Timeout: 5분
    Memory: 512MB
    Concurrency: 1000
```

## 🗄️ 데이터 저장소 아키텍처

### PostgreSQL Flexible Server

#### 서비스별 데이터베이스
```yaml
Profile Database:
  SKU: General Purpose, 2 vCores
  Storage: 128GB, Auto-grow enabled
  Backup: 7일 보존, Geo-redundant
  High Availability: Zone-redundant
  Network: Private access only

Itinerary Database:  
  SKU: General Purpose, 4 vCores
  Storage: 256GB, Auto-grow enabled  
  Backup: 30일 보존, Geo-redundant
  High Availability: Zone-redundant
  파티셔닝: 날짜 기반 (월단위)

Location Database:
  SKU: General Purpose, 2 vCores
  Storage: 128GB, Auto-grow enabled
  Extensions: PostGIS (공간 데이터)
  Backup: 7일 보존, Geo-redundant
  High Availability: Zone-redundant
```

### Azure Cache for Redis
```yaml
Redis Premium Cluster:
  SKU: Premium P1 (6GB)
  Cluster: 3 샤드, 6 노드 (HA)
  Network: VNet 내부 전용
  Persistence: RDB + AOF
  Backup: 매일 자동 백업
  Eviction Policy: allkeys-lru
```

### Azure Blob Storage
```yaml
Storage Account:
  Performance: Standard
  Replication: GRS (Geo-redundant)
  Access Tiers:
    - Hot: 자주 접근하는 이미지 (7일)
    - Cool: 오래된 첨부파일 (30일 이후)
  Network: Private endpoint only
  Encryption: Customer-managed keys
```

## ⚡ 성능 최적화 및 스케일링

### 자동 스케일링 정책

#### KEDA (Kubernetes Event-Driven Autoscaler)
```yaml
Scaling Rules:
  HTTP Scaler:
    - Target: 100 requests per instance
    - Min Replicas: 1
    - Max Replicas: 20
  
  Service Bus Scaler:
    - Target: 10 messages per instance
    - Queue Length Threshold: 50
    - Min Replicas: 1
    - Max Replicas: 50
  
  CPU/Memory Scaler:
    - CPU Target: 70%
    - Memory Target: 80%
    - Scale-out cooldown: 3분
    - Scale-in cooldown: 10분
```

#### 캐시 최적화 전략
```yaml
캐시 계층:
  L1 - Application Gateway: 정적 콘텐츠 (30분)
  L2 - Service Level: 애플리케이션 캐시 (5분)
  L3 - Redis Cluster: 공유 데이터 캐시 (1-24시간)

캐시 정책:
  프로파일 정보: TTL 1시간, Write-through
  장소 정보: TTL 2시간, Cache-aside  
  검색 결과: TTL 30분, Cache-aside
  번역 결과: TTL 24시간, Cache-aside
```

## 📊 모니터링 및 로깅 아키텍처

### Azure Monitor + Application Insights
```yaml
모니터링 구성:
  Application Map: 서비스 의존성 시각화
  Live Metrics: 실시간 성능 메트릭
  Availability Tests: 5분 간격 엔드포인트 테스트
  Custom Metrics: 비즈니스 메트릭 수집
  
알림 규칙:
  Critical Alerts:
    - 응답시간 > 5초 (3분 연속)
    - 에러율 > 5% (5분 연속)
    - 가용성 < 99% (1분)
  Warning Alerts:
    - 응답시간 > 2초 (5분 연속)
    - CPU > 80% (10분 연속)
    - 메모리 > 85% (10분 연속)
```

### Log Analytics
```yaml
로그 수집:
  Container Logs: 모든 Container Apps
  Network Security Logs: NSG 플로우 로그
  Azure Activity Logs: 리소스 변경 사항
  Application Logs: 애플리케이션 레벨 로그
  
로그 보존:
  Security Logs: 2년
  Application Logs: 90일
  Performance Logs: 30일
  Debug Logs: 7일
```

### Prometheus + Grafana (선택사항)
```yaml
Custom Metrics:
  Business Metrics:
    - 일정 생성 성공률
    - AI 처리 시간
    - 외부 API 응답시간
  Technical Metrics:
    - Cache Hit Ratio
    - Database Connection Pool
    - Queue Processing Rate
```

## 🔄 CI/CD 파이프라인

### GitHub Actions Workflow
```yaml
Pipeline Stages:
  1. Code Quality (병렬):
     - SonarQube 정적 분석
     - Security vulnerability scan
     - Dependency check
  
  2. Build & Test (병렬):
     - Unit tests (JUnit)
     - Integration tests (TestContainers)
     - Contract tests (Pact)
  
  3. Container Build:
     - Multi-stage Docker build
     - 이미지 보안 스캔 (Trivy)
     - Azure Container Registry 푸시
  
  4. Deploy (Blue-Green):
     - Staging environment 배포
     - Smoke tests 실행
     - Production 환경 교체
     - Health check 검증
```

### 배포 전략
```yaml
Blue-Green Deployment:
  환경: Blue (Current) ↔ Green (New)
  Health Check:
    - HTTP 200 OK 확인
    - Database 연결 테스트
    - Cache 연결 테스트
    - 외부 API 호출 테스트
  
  Rollback Strategy:
    - 자동 롤백: Health check 실패시
    - 수동 롤백: 1-click 복원
    - 데이터베이스: 트랜잭션 격리
```

## 🌍 고가용성 및 재해복구

### Multi-Region 구성
```yaml
Primary Region: Korea Central
Secondary Region: Japan East

Traffic Routing:
  Azure Traffic Manager:
    - 라우팅 방법: 성능 기반
    - Health Check: HTTP/HTTPS 엔드포인트
    - Failover Time: 90초
    - DNS TTL: 60초

데이터 복제:
  PostgreSQL:
    - Read Replica: Japan East
    - 자동 페일오버: 가능
    - RTO: 15분, RPO: 5분
  
  Blob Storage:
    - GRS 복제: 자동 동기화
    - RA-GRS: 읽기 전용 액세스
```

### 재해복구 시나리오
```yaml
장애 유형별 대응:
  1. 단일 서비스 장애:
     - Auto-healing: 컨테이너 재시작
     - 복구 시간: 30초-2분
  
  2. 데이터 센터 장애:
     - Zone 간 자동 장애조치
     - 복구 시간: 5-10분
  
  3. 리전 전체 장애:
     - 수동 DR 트리거
     - 복구 시간: 15-30분
     - 데이터 손실: < 5분
```

## 💰 비용 최적화

### 예상 월간 비용 ($USD)
```yaml
컴퓨트 리소스:
  Container Apps: $180 (auto-scaling)
  Azure Functions: $25 (consumption)
  소계: $205

데이터 & 저장소:
  PostgreSQL: $215 (3개 인스턴스)
  Redis Premium: $160
  Blob Storage: $35
  소계: $410

네트워킹:
  Traffic Manager: $18
  Front Door: $35  
  VNet Gateway: $145
  소계: $198

기타 서비스:
  Key Vault: $8
  Monitor/Insights: $65
  Service Bus: $15
  소계: $88

총 예상 비용: $901/월
최적화 후 비용: $758/월 (16% 절약)
```

### 비용 최적화 전략
```yaml
Reserved Instances:
  PostgreSQL: 3년 약정 시 30% 절약
  VM Scale Sets: 1년 약정 시 20% 절약

Spot Instances:
  배치 작업: 70% 비용 절약
  개발/테스트: 60% 비용 절약

Auto-scaling:
  야간 시간: 50% 리소스 감소
  주말: 30% 리소스 감소
  예상 절약: 월 $143

Dev/Test 정책:
  개발 환경: 18시간/일 자동 종료
  테스트 환경: 필요시에만 시작
  예상 절약: 월 $85
```

## 🔧 구현 로드맵

### Phase 1: 기본 인프라 (주 1-2)
- [x] Azure 리소스 그룹 및 VNet 생성
- [x] PostgreSQL Flexible Server 프로비저닝
- [x] Redis Cache 클러스터 구성
- [x] Key Vault 및 시크릿 설정
- [x] Container Registry 설정

### Phase 2: 네트워크 및 보안 (주 3-4)  
- [x] NSG 및 보안 규칙 구성
- [x] Private Link 엔드포인트 생성
- [x] Application Gateway + WAF 설정
- [x] Azure Firewall 구성
- [x] AAD 통합 및 인증 설정

### Phase 3: 애플리케이션 배포 (주 5-7)
- [x] Container Apps 환경 생성
- [x] 마이크로서비스 배포
- [x] API Management 구성
- [x] Service Bus 및 Job Queue 설정
- [x] 외부 API 프록시 Functions 배포

### Phase 4: 운영 준비 (주 8)
- [x] 모니터링 대시보드 구성
- [x] 알림 규칙 설정
- [x] CI/CD 파이프라인 완성
- [x] DR 테스트 및 검증
- [x] 성능 튜닝 및 최적화

## 📋 체크리스트

### 보안 체크리스트
- [x] 모든 PaaS 서비스 Private Link 연결
- [x] NSG 규칙 최소 권한 적용
- [x] Key Vault HSM 키 사용
- [x] 데이터 전송/저장 암호화
- [x] WAF 규칙 OWASP 준수
- [x] AAD MFA 모든 관리자 계정 적용

### 성능 체크리스트  
- [x] 캐시 히트율 > 80% 목표
- [x] 데이터베이스 연결 풀 최적화
- [x] 자동 스케일링 정책 설정
- [x] CDN 정적 리소스 캐싱
- [x] 공간 인덱스 위치 검색 최적화

### 운영 체크리스트
- [x] 99.9% 가용성 SLA 목표
- [x] RTO 15분, RPO 5분 DR 전략
- [x] 모니터링 대시보드 구성
- [x] 알림 규칙 및 에스컬레이션
- [x] 자동화된 배포 파이프라인

## 🎯 핵심 성능 지표 (KPI)

### 서비스 레벨 지표
- **가용성**: 99.9% (월 43분 이하 다운타임)
- **응답 시간**: P95 < 500ms, P99 < 1초
- **처리량**: 초당 1,000 TPS 목표
- **에러율**: < 0.1%

### 비즈니스 지표
- **일정 생성 성공률**: > 95%
- **AI 처리 시간**: < 30초 (P95)
- **사용자 만족도**: > 4.5/5.0
- **시스템 정상 운영율**: > 99.9%

## 📚 참고 문서

### 설계 산출물
- **물리 아키텍처 다이어그램**: `design/backend/system/물리아키텍처.txt`
- **핵심 인프라 설계**: `design/backend/system/azure-core-infrastructure.txt`
- **보안 네트워크 설계**: `design/backend/system/azure-security-network.txt`  
- **DevOps 운영 설계**: `design/backend/system/azure-devops-operations.txt`

### 관련 설계서
- **논리 아키텍처**: `design/backend/논리아키텍처.txt`
- **데이터베이스 설계**: `design/backend/database/`
- **API 설계**: `design/backend/api/`
- **시퀀스 설계**: `design/backend/sequence/`

---

**📝 작성자**: System Architecture Team  
**📅 작성일**: 2025-01-22  
**🔄 버전**: 1.0  
**📋 상태**: 설계 완료