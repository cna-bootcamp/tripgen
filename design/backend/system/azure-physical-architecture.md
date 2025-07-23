# Azure 물리 아키텍처 설계서

## 개요
본 문서는 여행 상세 일정 생성 서비스의 Azure Cloud 기반 물리 아키텍처를 정의합니다.

## 아키텍처 구성

### 1. 리전 및 가용성
- **Primary Region**: Korea Central
- **Disaster Recovery**: Korea South (Phase 2)
- **가용성 목표**: 99.9% SLA

### 2. 계층별 구성

#### Frontend Tier
- **Azure CDN**: 정적 콘텐츠 전역 배포
- **Azure Blob Storage**: SPA 호스팅 (React/Vue.js)

#### API Gateway Tier
- **Azure API Management (Basic)**: 
  - 인증/인가 처리
  - Rate Limiting
  - API 버전 관리
  - 모니터링 통합

#### Compute Tier
- **Azure Kubernetes Service (AKS)**:
  - Node Pool: Standard_D2s_v3 (2 vCPU, 8GB RAM)
  - 초기 노드: 3개 (Auto-scaling: 3-10)
  - 내부 구성:
    - Spring Cloud Gateway (내부 라우팅)
    - Profile Service (Min: 2, Max: 10 pods)
    - Itinerary Service (Min: 3, Max: 20 pods)
    - Location Service (Min: 2, Max: 15 pods)

#### Data Tier
- **Azure Database for PostgreSQL (Flexible Server)**:
  - 2 vCores, 4GB RAM
  - 자동 백업: 7일 보관
  - 데이터베이스 분리:
    - profile_db: 멤버/여행 정보
    - itinerary_db: 일정 데이터
    - location_db: 장소 캐시

- **Azure Cache for Redis (Standard C1)**:
  - 1GB 메모리
  - 캐시 전략:
    - 프로파일: TTL 24시간
    - 장소 정보: TTL 1시간
    - 검색 결과: TTL 10분

#### Messaging Tier
- **Azure Service Bus (Standard)**:
  - Queue: ai-generation-queue
  - AI 일정 생성 비동기 처리
  - Dead Letter Queue 구성

### 3. 외부 연동
- **카카오 MCP API**: 국내 지도/장소 정보
- **구글 MCP API**: 해외 지도/장소 정보

## 보안 구성 (기본)
- HTTPS 전체 구간 암호화
- Managed Identity 사용
- Key Vault를 통한 시크릿 관리

## 모니터링
- **Azure Monitor**: 인프라 메트릭
- **Application Insights**: 애플리케이션 로그/트레이스

## 비용 최적화

### MVP 단계 예상 비용
| 서비스 | 월 비용 (USD) |
|--------|---------------|
| AKS (3 nodes) | $300 |
| PostgreSQL | $150 |
| Redis Cache | $50 |
| Service Bus | $10 |
| API Management | $50 |
| 기타 (Storage, Network) | $40 |
| **총계** | **약 $600** |

### 비용 절감 전략
1. 개발/테스트 환경은 업무 시간만 운영
2. Spot Instance 활용 (개발 환경)
3. Reserved Instance 구매 (프로덕션)
4. 자동 스케일링으로 리소스 최적화

## 확장 계획

### Phase 1 (MVP)
- 단일 리전 구성
- 기본 모니터링
- 수동 백업/복구

### Phase 2 (성장)
- Multi-Region 구성
- Advanced Monitoring
- 자동 장애 복구
- CDN 최적화

### Phase 3 (성숙)
- Global Distribution
- Multi-Cloud 전략
- Edge Computing
- AI/ML 최적화

## 재해 복구 계획
- **RTO**: 4시간
- **RPO**: 1시간
- 일일 백업 자동화
- Cross-region 복제 (Phase 2)