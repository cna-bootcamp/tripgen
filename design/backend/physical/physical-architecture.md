# 물리 아키텍처 설계서

## 1. 개요

### 1.1 설계 목적
- AI 기반 여행 일정 생성 서비스의 Azure Cloud 기반 물리 아키텍처 설계
- MVP 개발환경과 확장 가능한 운영환경 아키텍처 정의
- HighLevel아키텍처정의서와 논리아키텍처 설계서 기반 구현

### 1.2 설계 원칙
- **Azure Cloud 우선**: Azure 네이티브 서비스 활용 극대화
- **HighLevel아키텍처 준수**: 정의된 기술 스택 및 패턴 적용
- **백킹서비스 표준**: 가이드에 정의된 오픈소스 DB 활용
- **단계별 확장**: 개발→운영 단계적 아키텍처 발전

### 1.3 참조 아키텍처
- HighLevel아키텍처정의서: design/high-level-architecture.md
- 논리아키텍처: design/backend/logical/logical-architecture.md
- 아키텍처패턴: design/pattern/아키텍처패턴.md
- API설계서: design/backend/api/*.yaml

## 2. 아키텍처 환경 구성

### 2.1 환경별 구성 전략

#### 2.1.1 개발환경 (MVP 단계)
**목적**: 빠른 개발과 검증을 위한 최소 구성
- **컴퓨팅**: Azure Kubernetes Service (AKS) Basic Tier
- **네트워킹**: Kubernetes Ingress Controller (NGINX)
- **데이터베이스**: Kubernetes Pod 기반 PostgreSQL + Redis
- **메시징**: Azure Service Bus (Basic Tier)
- **모니터링**: 기본 Kubernetes 모니터링

#### 2.1.2 운영환경 (Production)
**목적**: 고가용성, 확장성, 보안을 고려한 엔터프라이즈 구성
- **컴퓨팅**: Azure Kubernetes Service (AKS) Standard Tier + Multi-Zone
- **네트워킹**: Azure Application Gateway + WAF + Private Link
- **데이터베이스**: Azure Database for PostgreSQL Flexible Server + Azure Cache for Redis
- **메시징**: Azure Service Bus (Premium Tier) + Event Hubs
- **보안**: Azure Key Vault + Azure AD + Private Endpoints
- **모니터링**: Azure Monitor + Application Insights + Log Analytics

### 2.2 리소스 할당 전략

#### 2.2.1 개발환경 리소스
| 구성요소 | 사양 | 수량 | 비용 최적화 |
|----------|------|------|-------------|
| AKS Node | Standard B2s (2 vCPU, 4GB) | 2 nodes | Spot Instances |
| PostgreSQL Pod | 1 vCPU, 2GB, 20GB SSD | 1 pod | Local Storage |
| Redis Pod | 0.5 vCPU, 1GB | 1 pod | Memory Optimized |
| Service Bus | Basic Tier | 1 namespace | 최소 구성 |

#### 2.2.2 운영환경 리소스
| 구성요소 | 사양 | 수량 | 가용성 |
|----------|------|------|--------|
| AKS Node | Standard D4s v3 (4 vCPU, 16GB) | 3-10 nodes | Multi-Zone |
| PostgreSQL | GP_Standard_D4s (4 vCPU, 16GB) | Primary + Replica | Zone Redundant |
| Redis | Premium P1 (6GB) | Cluster Mode | Zone Redundant |
| Application Gateway | Standard_v2 | 2 instances | Zone Redundant |

## 3. 네트워크 아키텍처

### 3.1 네트워크 토폴로지

#### 3.1.1 개발환경 네트워크
```
[인터넷] 
  ↓
[Kubernetes Ingress Controller (NGINX)]
  ↓
[AKS 클러스터 - 단일 서브넷]
  ├── User Service Pod
  ├── Trip Service Pod  
  ├── AI Service Pod
  ├── Location Service Pod
  ├── PostgreSQL Pod
  ├── Redis Pod
  └── Service Bus 연결
```

#### 3.1.2 운영환경 네트워크
```
[인터넷]
  ↓
[Azure Application Gateway + WAF]
  ↓  
[Azure Private Link]
  ↓
[AKS 클러스터 - Private Subnet]
  ├── Application Subnet (10.0.1.0/24)
  ├── Database Subnet (10.0.2.0/24) 
  └── Cache Subnet (10.0.3.0/24)
```

### 3.2 보안 네트워크 구성

#### 3.2.1 네트워크 보안 그룹 (NSG)
| 계층 | 인바운드 규칙 | 아웃바운드 규칙 |
|------|---------------|-----------------|
| Application Gateway | 443 (HTTPS), 80 (HTTP) | AKS 클러스터 통신 |
| AKS 노드 | Gateway에서만 | PostgreSQL, Redis, Service Bus |
| 데이터베이스 | AKS에서만 | 인터넷 차단 |

#### 3.2.2 서비스별 네트워크 정책
- **마이크로서비스 간**: Istio Service Mesh mTLS
- **데이터베이스 접근**: Private Endpoint + Azure AD 인증
- **외부 API**: NAT Gateway를 통한 아웃바운드

## 4. 컴퓨팅 아키텍처

### 4.1 컨테이너 오케스트레이션

#### 4.1.1 Azure Kubernetes Service (AKS) 구성
**클러스터 설정**:
- **Kubernetes 버전**: 1.29 (최신 안정 버전)
- **네트워크 플러그인**: Azure CNI (Advanced Networking)
- **서비스 메시**: Istio 1.22
- **인그레스**: NGINX Ingress Controller (개발) / Application Gateway (운영)

**노드 풀 구성**:
```yaml
# 개발환경
system_pool:
  vm_size: Standard_B2s
  node_count: 2
  max_pods: 30

# 운영환경  
system_pool:
  vm_size: Standard_D2s_v3
  node_count: 3
  auto_scaling: true
  min_nodes: 3
  max_nodes: 10
```

#### 4.1.2 서비스별 배포 전략
| 서비스 | CPU Requests | Memory Requests | Replicas (Dev/Prod) | HPA 설정 |
|--------|--------------|-----------------|---------------------|----------|
| User Service | 100m | 256Mi | 1/2 | CPU 70% |
| Trip Service | 200m | 512Mi | 1/3 | CPU 70% |
| AI Service | 500m | 1Gi | 1/2 | CPU 80% |
| Location Service | 100m | 256Mi | 1/2 | CPU 70% |

### 4.2 백킹 서비스 구성

#### 4.2.1 데이터베이스 아키텍처

**개발환경 - Kubernetes Pod 기반**:
```yaml
postgresql_pod:
  image: bitnami/postgresql:16
  resources:
    requests:
      cpu: 1000m
      memory: 2Gi
    limits:
      cpu: 2000m  
      memory: 4Gi
  storage: 20Gi (hostPath)
  backup: 수동 백업
```

**운영환경 - Azure Managed Services**:
```yaml
azure_postgresql:
  tier: GeneralPurpose
  sku: Standard_D4s_v3
  storage: 100GB (Premium SSD)
  backup_retention: 35일
  geo_redundant: true
  high_availability: Zone Redundant
  read_replica: 2개 (읽기 부하 분산)
```

#### 4.2.2 캐시 아키텍처

**개발환경 - Redis Pod**:
```yaml
redis_pod:
  image: bitnami/redis:7.2
  resources:
    requests:
      cpu: 500m
      memory: 1Gi
  persistence: false (개발용)
```

**운영환경 - Azure Cache for Redis**:
```yaml
azure_redis:
  tier: Premium
  capacity: P1 (6GB)
  clustering: enabled
  persistence: RDB + AOF
  geo_replication: 고도화 단계
```

#### 4.2.3 메시징 서비스

**Azure Service Bus 구성**:
```yaml
# 개발환경
service_bus_basic:
  tier: Basic
  max_queue_size: 1GB
  message_ttl: 14일

# 운영환경  
service_bus_premium:
  tier: Premium
  messaging_units: 2
  geo_dr: enabled
  private_endpoint: true
```

**큐 설계**:
- `ai-schedule-generation-queue`: AI 일정 생성 요청
- `location-search-queue`: 장소 검색 비동기 처리
- `notification-queue`: 사용자 알림

## 5. 데이터 아키텍처

### 5.1 데이터 저장소 전략

#### 5.1.1 서비스별 데이터베이스 매핑
| 서비스 | 데이터베이스 | 스키마 | 특징 |
|--------|-------------|--------|------|
| User Service | PostgreSQL | user_db | 트랜잭션 일관성 |
| Trip Service | PostgreSQL | trip_db | 복잡한 관계 데이터 |
| AI Service | PostgreSQL | ai_db | JSON 컬럼 활용 |
| Location Service | PostgreSQL | location_db | GIS 확장 |

#### 5.1.2 캐시 전략
```yaml
cache_layers:
  L1_Application: 
    type: Caffeine Cache
    ttl: 5분
    max_entries: 1000
    
  L2_Distributed:
    type: Redis
    ttl: 30분  
    cluster_mode: true
    eviction_policy: allkeys-lru
```

### 5.2 데이터 파이프라인

#### 5.2.1 데이터 흐름
```
[User Input] → [Application] → [PostgreSQL] → [Change Stream]
                            → [Redis Cache] → [Cache Invalidation]
                            → [Service Bus] → [Async Processing]
```

#### 5.2.2 백업 및 복구 전략
**개발환경**:
- PostgreSQL Pod: 수동 백업 (PVC 스냅샷)
- Redis: 재시작 시 데이터 손실 허용

**운영환경**:
- PostgreSQL: 자동 백업 (35일 보존)
- Point-in-Time Recovery 지원
- Redis: RDB + AOF 지속성
- 지역 간 지리적 복제 (고도화 단계)

## 6. 보안 아키텍처

### 6.1 인증 및 권한 관리

#### 6.1.1 서비스 간 인증
```yaml
istio_security:
  mtls_mode: STRICT
  auto_mtls: true
  jwt_verification: enabled
  
azure_ad_integration:
  workload_identity: enabled  
  managed_identity: SystemAssigned
```

#### 6.1.2 외부 접근 보안
- **API Gateway**: JWT 토큰 검증
- **WAF**: OWASP Top 10 보호 규칙
- **DDoS Protection**: Azure DDoS Protection Standard
- **Rate Limiting**: 사용자당 100 req/min

### 6.2 데이터 보안

#### 6.2.1 암호화 전략
```yaml
encryption_at_rest:
  postgresql: TDE (Transparent Data Encryption)
  redis: 플랫폼 레벨 암호화
  storage: Azure Storage Service Encryption

encryption_in_transit:
  external: TLS 1.3
  internal: Istio mTLS
  database: SSL/TLS 연결
```

#### 6.2.2 비밀 관리
- **Azure Key Vault**: API 키, 연결 문자열, 인증서
- **CSI Secret Store**: Kubernetes Secret 자동 동기화
- **Rotation Policy**: 90일 주기 로테이션

## 7. 모니터링 및 관측 가능성

### 7.1 모니터링 스택

#### 7.1.1 메트릭 수집
```yaml
prometheus_stack:
  node_exporter: 노드 메트릭
  kube_state_metrics: Kubernetes 메트릭  
  application_metrics: Spring Actuator

azure_monitor:
  container_insights: 컨테이너 메트릭
  application_insights: APM
  log_analytics: 로그 집중화
```

#### 7.1.2 로깅 전략
```yaml
logging_pipeline:
  collection: Fluentd DaemonSet
  storage: Azure Log Analytics
  retention: 30일 (개발), 90일 (운영)
  alerting: KQL 기반 알람
```

### 7.2 관측 가능성

#### 7.2.1 분산 추적
- **Jaeger**: 서비스 간 요청 추적
- **Azure Application Insights**: End-to-End 가시성
- **샘플링**: 10% (비용 최적화)

#### 7.2.2 헬스체크
```yaml
health_checks:
  liveness_probe: /actuator/health/liveness
  readiness_probe: /actuator/health/readiness
  startup_probe: /actuator/health/startup
  interval: 30초
  timeout: 5초
```

## 8. CI/CD 및 배포 전략

### 8.1 CI/CD 파이프라인

#### 8.1.1 빌드 파이프라인 (GitHub Actions)
```yaml
ci_pipeline:
  trigger: push to main/develop
  stages:
    - 단위 테스트 (80% 커버리지)
    - 정적 코드 분석 (SonarQube)
    - 보안 스캔 (Trivy, SAST)
    - 컨테이너 이미지 빌드
    - Azure Container Registry 푸시
```

#### 8.1.2 배포 파이프라인 (ArgoCD)
```yaml
cd_pipeline:
  pattern: GitOps
  environments:
    dev: 자동 배포
    prod: 수동 승인 + Blue-Green
  rollback: 자동 헬스체크 실패 시
  canary: 10% → 50% → 100%
```

### 8.2 환경별 배포 전략

#### 8.2.1 개발환경
- **배포 방식**: Rolling Update
- **다운타임**: 허용 (1-2분)
- **테스트**: 기본 헬스체크

#### 8.2.2 운영환경
- **배포 방식**: Blue-Green Deployment
- **다운타임**: Zero Downtime
- **테스트**: 종합 헬스체크 + 스모크 테스트

## 9. 비용 최적화

### 9.1 리소스 최적화

#### 9.1.1 개발환경 비용 절감
```yaml
cost_optimization:
  vm_instances: Spot Instances (70% 절약)
  auto_shutdown: 비업무시간 자동 종료
  reserved_instances: 미적용 (개발용)
  storage: Standard SSD (Premium 대비 50% 절약)
```

#### 9.1.2 운영환경 비용 최적화
```yaml
production_optimization:
  reserved_instances: 1년 예약 (30% 절약)
  auto_scaling: 야간/주말 자동 스케일 다운
  blob_storage: Cool/Archive Tier 활용
  monitoring: 필수 메트릭만 수집
```

### 9.2 예상 비용 분석

#### 9.2.1 월간 비용 추정 (USD)
| 구성요소 | 개발환경 | 운영환경 |
|----------|----------|----------|
| AKS 클러스터 | $150 | $800 |
| PostgreSQL | $50 (Pod) | $300 (Managed) |
| Redis | $20 (Pod) | $200 (Premium) |
| Service Bus | $10 | $100 |
| 네트워킹 | $30 | $150 |
| 모니터링 | $20 | $100 |
| **총합** | **$280** | **$1,650** |

## 10. 재해복구 및 고가용성

### 10.1 가용성 설계

#### 10.1.1 목표 SLA
- **개발환경**: 95% (월 36시간 다운타임 허용)
- **운영환경**: 99.9% (월 43분 다운타임 허용)

#### 10.1.2 고가용성 구성
```yaml
high_availability:
  kubernetes: Multi-Zone 배포
  database: Zone Redundant (Primary + Replica)
  redis: Cluster Mode (3 Master + 3 Slave)
  load_balancer: Multi-Instance
```

### 10.2 재해복구

#### 10.2.1 백업 전략
```yaml
backup_strategy:
  database: 
    automated: 일일 자동 백업
    retention: 35일
    geo_backup: enabled
  configuration:
    gitops: Git 기반 형상 관리
    secrets: Azure Key Vault 복제
```

#### 10.2.2 복구 목표
- **RTO (Recovery Time Objective)**: 30분
- **RPO (Recovery Point Objective)**: 15분
- **DR 사이트**: 고도화 단계에서 다른 Azure 리전 구축

## 11. 확장 계획

### 11.1 단계별 확장 로드맵

#### 11.1.1 Phase 1 (MVP - 현재)
- 개발환경 구축
- 기본 모니터링
- 수동 배포

#### 11.1.2 Phase 2 (확장)
- 운영환경 전환
- 자동화된 CI/CD
- 고급 모니터링

#### 11.1.3 Phase 3 (고도화)
- 멀티 리전 배포
- 글로벌 로드 밸런싱
- 고급 보안 기능

### 11.2 성능 확장성

#### 11.2.1 수평 확장
```yaml
horizontal_scaling:
  application: HPA (CPU/Memory 기반)
  database: 읽기 복제본 추가
  cache: Redis Cluster 노드 증설
  queue: Service Bus 파티션 확장
```

#### 11.2.2 수직 확장
```yaml
vertical_scaling:
  kubernetes: VPA (Vertical Pod Autoscaler)
  database: 상위 SKU 업그레이드
  redis: 메모리 용량 증설
```

## 12. 운영 가이드

### 12.1 일상 운영

#### 12.1.1 모니터링 체크리스트
- [ ] 서비스 헬스 체크 (매시간)
- [ ] 리소스 사용률 확인 (일일)
- [ ] 로그 이상 패턴 확인 (주간)
- [ ] 백업 상태 확인 (주간)

#### 12.1.2 주요 알람 정책
```yaml
alerts:
  critical:
    - Pod 재시작 > 5회/시간
    - 응답시간 > 5초 (AI Service)
    - 메모리 사용률 > 90%
  warning:
    - 응답시간 > 200ms
    - CPU 사용률 > 80%
    - 디스크 사용률 > 85%
```

### 12.2 트러블슈팅

#### 12.2.1 일반적인 문제 해결
```yaml
common_issues:
  pod_crash_loop:
    check: kubectl logs, describe pod
    action: 리소스 한계 조정, 이미지 롤백
    
  network_timeout:
    check: Istio 설정, NSG 규칙
    action: 네트워크 정책 검토
    
  database_connection:
    check: 연결 풀, 인증 정보
    action: Key Vault 시크릿 갱신
```

## 13. 결론

### 13.1 아키텍처 핵심 가치
1. **단계적 발전**: 개발→운영 단계별 아키텍처 진화
2. **Azure 네이티브**: Azure 서비스 활용 극대화
3. **비용 효율성**: 단계별 비용 최적화 전략
4. **확장성**: 트래픽 증가에 대응하는 자동 확장
5. **보안성**: 다층 보안과 Zero Trust 원칙

### 13.2 기대 효과
- **성능**: AI 일정 생성 5초 이내 달성
- **가용성**: 99.9% 서비스 가용성 확보
- **확장성**: 10배 이상 트래픽 증가 대응
- **보안**: 엔터프라이즈급 보안 수준
- **운영성**: GitOps 기반 자동화된 운영

이 물리 아키텍처 설계서는 TripGen 서비스의 Azure Cloud 기반 인프라스트럭처 구축을 위한 완전한 가이드를 제공합니다.