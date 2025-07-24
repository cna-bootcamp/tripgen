# High Level 아키텍처 정의서

## 1. 개요 (Executive Summary)

### 1.1 프로젝트 개요
- **비즈니스 목적**: AI 기반 맞춤형 여행 일정을 자동으로 생성하여 여행 계획 수립의 편의성을 극대화
- **핵심 기능**: 
  - 여행 멤버 특성(나이, 건강상태, 선호도) 기반 AI 일정 생성
  - 실시간 장소 정보 검증 및 추천
  - 다국어 검색 및 경로 최적화
  - 사진/메모 첨부를 통한 여행 기록 관리
- **대상 사용자**: 국내외 개인/그룹 여행자, 여행 계획자
- **예상 사용자 규모**: 
  - MVP: 동시 사용자 1,000명
  - 성장기: 동시 사용자 10,000명 (성수기 10배 트래픽)

### 1.2 아키텍처 범위 및 경계
- **시스템 범위**: 여행 일정 생성 및 관리를 위한 End-to-End 서비스
- **포함되는 시스템**: 
  - 프로파일 관리 시스템
  - AI 기반 일정 생성 시스템
  - 장소 검색 및 검증 시스템
  - 캐시 및 데이터 저장 시스템
- **제외되는 시스템**: 
  - 결제/예약 시스템
  - 호텔/항공 예약 연동
  - 실시간 채팅/커뮤니티 기능
- **외부 시스템 연동**: 
  - Claude API (AI 일정 생성)
  - 카카오 MCP API (국내 지도/장소)
  - 구글 MCP API (해외 지도/장소)

### 1.3 문서 구성
이 문서는 4+1 뷰 모델을 기반으로 구성되며, 논리적/물리적/프로세스/개발 관점에서 아키텍처를 정의합니다.

---

## 2. 아키텍처 요구사항

### 2.1 기능 요구사항 요약
| 영역 | 주요 기능 | 우선순위 |
|------|-----------|----------|
| 프로파일 관리 | 멤버 정보 CRUD, 여행 기본정보 설정, 이동수단 설정 | Must |
| AI 일정 생성 | Claude API 기반 맞춤형 일정 자동 생성, 멤버 특성 반영 | Must |
| 장소 검색/검증 | 실시간 장소 정보 검증, 다국어 검색, 주변 장소 추천 | Must |
| 일정 관리 | 일정 편집, 경로 계산, 사진/메모 첨부 | Must |
| 데이터 동기화 | 마이크로서비스 간 실시간 데이터 공유 | Must |

### 2.2 비기능 요구사항 (NFRs)

#### 2.2.1 성능 요구사항
- **응답시간**: 
  - 일반 조회: < 500ms (P95)
  - AI 일정 생성: < 10초
  - 장소 검색: < 2초
  - MCP API 응답: < 2초
- **처리량**: 
  - 동시 요청: 1,000 req/sec
  - AI 일정 생성: 100 req/min
- **동시사용자**: 
  - 평상시: 1,000명
  - 성수기: 10,000명
- **데이터 처리량**: 
  - 일일 생성 일정: 10,000건
  - 장소 검색: 100,000건/일

#### 2.2.2 확장성 요구사항
- **수평 확장**: 
  - 마이크로서비스별 독립적 스케일링
  - 컨테이너 기반 자동 확장 (HPA)
- **수직 확장**: 
  - 데이터베이스 리소스 증설
  - 캐시 메모리 확장
- **글로벌 확장**: 
  - Phase 2에서 Multi-Region 지원 계획

#### 2.2.3 가용성 요구사항
- **목표 가용성**: 99.9% (월 43분 다운타임 허용)
- **다운타임 허용**: 계획된 유지보수 월 1회 (2시간)
- **재해복구 목표**: 
  - RTO (Recovery Time Objective): 4시간
  - RPO (Recovery Point Objective): 1시간

#### 2.2.4 보안 요구사항
- **인증/인가**: JWT 기반 토큰 인증
- **데이터 암호화**: 
  - 전송 중: TLS 1.3
  - 저장 시: AES-256 (민감 데이터)
- **네트워크 보안**: VNet 격리, NSG 규칙 적용
- **컴플라이언스**: 개인정보보호법, GDPR 준수

### 2.3 아키텍처 제약사항
- **기술적 제약**: 
  - Azure Cloud 플랫폼 사용 필수
  - Spring Boot 기반 백엔드 개발
  - PostgreSQL 데이터베이스 사용
- **비용 제약**: 
  - MVP 단계 월 운영비 $600 이내
  - 외부 API 호출 비용 최적화 필요
- **시간 제약**: 
  - MVP 출시: 4개월 이내
  - Phase 1 완료: 6개월
- **조직적 제약**: 
  - 개발팀 규모: 10명 이내
  - 기존 개발팀 스킬셋 활용

---

## 3. 아키텍처 설계 원칙

### 3.1 핵심 설계 원칙
1. **확장성 우선**: 수평적 확장이 가능한 마이크로서비스 구조
2. **장애 격리**: Circuit Breaker 패턴으로 외부 API 장애 격리
3. **느슨한 결합**: 서비스 간 캐시를 통한 간접 통신
4. **관측 가능성**: 분산 추적 및 중앙 집중식 로깅
5. **보안 바이 디자인**: 제로 트러스트 원칙 적용

### 3.2 아키텍처 품질 속성 우선순위
| 순위 | 품질 속성 | 중요도 | 전략 |
|------|-----------|--------|------|
| 1 | 성능 | High | 캐시 계층, 비동기 처리, 쿼리 최적화 |
| 2 | 확장성 | High | 마이크로서비스, 컨테이너화, 자동 스케일링 |
| 3 | 가용성 | High | 장애 격리, 자동 복구, 다중화 |
| 4 | 보안성 | Medium | 인증/인가, 암호화, 접근 제어 |
| 5 | 유지보수성 | Medium | 명확한 아키텍처, 표준화, 문서화 |

---

## 4. 논리 아키텍처 (Logical View)

### 4.1 시스템 컨텍스트 다이어그램
```
┌─────────────┐     ┌─────────────────────────────┐     ┌──────────────┐
│   여행자    │────▶│   여행 일정 생성 시스템    │────▶│  Claude API  │
└─────────────┘     │                             │     └──────────────┘
                    │  ┌──────────────────────┐  │     ┌──────────────┐
                    │  │   Profile Service    │  │────▶│  카카오 MCP  │
                    │  ├──────────────────────┤  │     └──────────────┘
                    │  │  Itinerary Service   │  │     ┌──────────────┐
                    │  ├──────────────────────┤  │────▶│   구글 MCP   │
                    │  │  Location Service    │  │     └──────────────┘
                    │  └──────────────────────┘  │
                    └─────────────────────────────┘
```

### 4.2 도메인 아키텍처
#### 4.2.1 도메인 모델
| 도메인 | 책임 | 주요 엔티티 |
|--------|------|-------------|
| 프로파일 | 멤버/여행 정보 관리 | Member, Trip, TransportSetting, Preference |
| 일정 | AI 일정 생성/관리 | Itinerary, DailyActivity, Route, Attachment |
| 장소 | 장소 검색/검증 | Place, PlaceDetails, Review, BusinessHours |

#### 4.2.2 바운디드 컨텍스트
- **프로파일 컨텍스트**: 사용자 및 여행 기본 정보 관리
- **일정 컨텍스트**: AI 기반 일정 생성 및 활동 관리
- **장소 컨텍스트**: 외부 API 연동 및 장소 정보 제공

### 4.3 서비스 아키텍처
#### 4.3.1 마이크로서비스 구성
| 서비스명 | 책임 |
|----------|------|
| Profile Service | 멤버 정보, 여행 설정, 이동수단 관리 |
| Itinerary Service | AI 일정 생성, 일정 편집, 경로 계산, 첨부파일 관리 |
| Location Service | 장소 검색, 실시간 검증, 다국어 지원, 지역 추천 |

#### 4.3.2 서비스 간 통신 패턴
- **동기 통신**: REST API (Gateway 경유)
- **비동기 통신**: Azure Service Bus (AI 일정 생성)
- **데이터 일관성**: 공유 캐시를 통한 Eventually Consistent

---

## 5. 프로세스 아키텍처 (Process View)

### 5.1 주요 비즈니스 프로세스
#### 5.1.1 핵심 사용자 여정
1. **여행 준비 플로우**
   - 멤버 등록 → 여행 정보 설정 → 이동수단 선택
   
2. **AI 일정 생성 플로우**
   - 일정 생성 요청 → Job Queue 등록 → Claude API 호출 → 장소 검증 → 일정 저장
   
3. **장소 탐색 플로우**
   - 검색 조건 입력 → 캐시 확인 → MCP API 호출 → 결과 캐싱 → 응답

#### 5.1.2 시스템 간 통합 프로세스
- **프로파일 → 일정**: Redis 캐시를 통한 Trip 정보 공유
- **일정 → 장소**: API Gateway 경유 동기 호출
- **장소 → 외부 API**: Circuit Breaker 패턴 적용

### 5.2 동시성 및 동기화
- **동시성 처리 전략**: 
  - Optimistic Locking (일정 편집)
  - 분산 락 (캐시 업데이트)
- **락 관리**: Redis 기반 분산 락
- **이벤트 순서 보장**: Service Bus 파티셔닝

---

## 6. 개발 아키텍처 (Development View)

### 6.1 개발 언어 및 프레임워크 선정
#### 6.1.1 백엔드 기술스택
| 서비스 | 언어 | 프레임워크 | 선정이유 |
|--------|------|-------------|----------|
| Profile Service | Java 17 | Spring Boot 3.2.0 | 안정성, 생산성, 팀 숙련도 |
| Itinerary Service | Java 17 | Spring Boot 3.2.0 | 통일된 기술 스택 |
| Location Service | Java 17 | Spring Boot 3.2.0 | 통일된 기술 스택 |

#### 6.1.2 프론트엔드 기술스택
- **언어**: TypeScript 5.3
- **프레임워크**: Next.js 14.1
- **선정 이유**: SSR 지원, 개발 생산성, SEO 최적화

### 6.2 서비스별 아키텍처 패턴
| 서비스 | 아키텍처 패턴 | 선정 이유 |
|--------|---------------|-----------|
| Profile Service | Layered Architecture | 단순한 CRUD 중심, 빠른 개발 |
| Itinerary Service | Clean Architecture + CQRS + Saga | 복잡한 비즈니스 로직, 읽기/쓰기 분리 |
| Location Service | Clean Architecture | 외부 의존성 격리, 테스트 용이성 |

### 6.3 개발 가이드라인
- **코딩 표준**: Google Java Style Guide
- **테스트 전략**: 
  - 단위 테스트 커버리지 80% 이상
  - 통합 테스트 주요 플로우 100%
  - E2E 테스트 핵심 시나리오
- **문서화 기준**: 
  - OpenAPI 3.0 명세
  - JavaDoc 주석
  - ADR 작성

---

## 7. 물리 아키텍처 (Physical View)

### 7.1 클라우드 아키텍처 패턴
#### 7.1.1 선정된 클라우드 패턴
- **패턴명**: Microservices on Container with Service Mesh
- **적용 이유**: 서비스별 독립 배포, 트래픽 관리, 관측성
- **예상 효과**: 배포 주기 단축, 장애 격리, 세밀한 모니터링

#### 7.1.2 클라우드 제공자
- **주 클라우드**: Azure Cloud
- **멀티 클라우드 전략**: 현재 단일 클라우드, Phase 3에서 검토
- **하이브리드 구성**: 해당 없음

### 7.2 인프라스트럭처 구성
#### 7.2.1 컴퓨팅 리소스
| 구성요소 | 사양 | 스케일링 전략 |
|----------|------|---------------|
| AKS Node Pool | Standard_D2s_v3 (2 vCPU, 8GB) | Auto-scaling (3-10 nodes) |
| Profile Service | Min 2, Max 10 pods | HPA (CPU 70%) |
| Itinerary Service | Min 3, Max 20 pods | HPA (CPU 70%, Memory 80%) |
| Location Service | Min 2, Max 15 pods | HPA (CPU 70%) |

#### 7.2.2 네트워크 구성
- **VNet**: 10.0.0.0/16
- **Subnets**: 
  - AKS: 10.0.1.0/24
  - Database: 10.0.2.0/24
  - Redis: 10.0.3.0/24
- **Load Balancer**: Azure Application Gateway
- **Service Mesh**: Istio (내부 트래픽 관리)

#### 7.2.3 보안 구성
- **방화벽**: Azure Firewall (Egress 제어)
- **WAF**: Application Gateway WAF v2
- **DDoS 방어**: Azure DDoS Protection Basic
- **VPN/Private Link**: Private Endpoint for Database

---

## 8. 기술 스택 아키텍처

### 8.1 API Gateway & Service Mesh
#### 8.1.1 API Gateway
- **제품**: Azure API Management (Basic)
- **주요 기능**: 인증, 라우팅, 레이트 리미팅, 모니터링
- **설정 전략**: 
  - Rate Limit: 1000 req/min per user
  - Circuit Breaker: 5 failures in 60s

#### 8.1.2 Service Mesh
- **제품**: Istio
- **적용 범위**: 모든 마이크로서비스
- **트래픽 관리**: 
  - Retry: 3 times with exponential backoff
  - Timeout: 30s for normal requests, 60s for AI generation

### 8.2 데이터 아키텍처
#### 8.2.1 데이터베이스 전략
| 용도 | 데이터베이스 | 타입 | 특징 |
|------|-------------|------|------|
| 트랜잭션 | PostgreSQL 14 | RDBMS | 서비스별 독립 DB |
| 캐시 | Redis 7.0 | In-Memory | 공유 캐시, 클러스터 구성 |
| 검색 | PostgreSQL + PostGIS | Spatial DB | 위치 기반 검색 |
| 분석 | Application Insights | Telemetry | 로그/메트릭 수집 |

#### 8.2.2 데이터 파이프라인
- **실시간 처리**: Service Bus → Function App → Database
- **배치 처리**: 일일 데이터 정합성 검증
- **데이터 동기화**: Redis Pub/Sub

### 8.3 백킹 서비스 (Backing Services)
#### 8.3.1 메시징 & 이벤트 스트리밍
- **메시지 큐**: Azure Service Bus (Standard)
  - Queue: ai-generation-queue
  - Topic: event-notifications
- **이벤트 스트리밍**: Phase 2에서 Event Hub 도입 검토
- **이벤트 스토어**: CosmosDB (Phase 2)

#### 8.3.2 스토리지 서비스
- **객체 스토리지**: Azure Blob Storage (사진 첨부파일)
- **블록 스토리지**: Managed Disks for AKS
- **파일 스토리지**: Azure Files (로그 아카이빙)

### 8.4 관측 가능성 (Observability)
#### 8.4.1 로깅 전략
- **로그 수집**: Fluent Bit → Log Analytics
- **로그 저장**: 30일 보관 (Hot), 90일 보관 (Archive)
- **로그 분석**: KQL (Kusto Query Language)

#### 8.4.2 모니터링 & 알람
- **메트릭 수집**: Prometheus (Istio 메트릭)
- **시각화**: Grafana + Azure Dashboard
- **알람 정책**: 
  - P1: 서비스 다운, 응답시간 > 5s
  - P2: 에러율 > 5%, CPU > 90%
  - P3: 캐시 히트율 < 70%

#### 8.4.3 분산 추적
- **추적 도구**: Jaeger (Istio 통합)
- **샘플링 전략**: 1% (평상시), 10% (디버깅)
- **성능 분석**: Application Insights APM

---

## 9. AI/ML 아키텍처

### 9.1 AI API 통합 전략
#### 9.1.1 AI 서비스/모델 매핑
| 목적 | 서비스 | 모델 | Input 데이터 | Output 데이터 | SLA |
|------|--------|-------|-------------|-------------|-----|
| 일정 생성 | Claude API | Claude 3 Opus | 멤버정보, 여행정보, 선호도 | 시간별 일정, 장소 추천 | 10초 |
| 장소 추천 | Claude API | Claude 3 Sonnet | 지역정보, 멤버특성 | 추천 장소 목록 | 5초 |

#### 9.1.2 AI 파이프라인
```
사용자 요청 → Service Bus → AI Worker → Claude API → 결과 검증 → 장소 서비스 검증 → DB 저장
```

### 9.2 데이터 과학 플랫폼
- **모델 개발 환경**: Phase 2에서 자체 ML 모델 개발 검토
- **모델 배포 전략**: API 기반 외부 서비스 활용
- **모델 모니터링**: API 응답시간, 품질 메트릭 추적

---

## 10. 개발 운영 (DevOps)

### 10.1 CI/CD 파이프라인
#### 10.1.1 지속적 통합 (CI)
- **도구**: Azure DevOps
- **빌드 전략**: 
  - 코드 커밋 시 자동 빌드
  - 멀티 스테이지 Docker 빌드
- **테스트 자동화**: 
  - 단위 테스트 (JUnit)
  - 통합 테스트 (Spring Boot Test)
  - 정적 분석 (SonarQube)

#### 10.1.2 지속적 배포 (CD)
- **배포 도구**: Azure DevOps + ArgoCD
- **배포 전략**: Blue-Green Deployment
- **롤백 정책**: 자동 헬스체크 실패 시 즉시 롤백

### 10.2 컨테이너 오케스트레이션
#### 10.2.1 Kubernetes 구성
- **클러스터 전략**: 
  - Dev/Test: 단일 노드 풀
  - Production: 멀티 노드 풀 (Zone Redundant)
- **네임스페이스 설계**: 
  - tripgen-dev
  - tripgen-test
  - tripgen-prod
- **리소스 관리**: 
  - Resource Quota per Namespace
  - Pod Disruption Budget

#### 10.2.2 헬름 차트 관리
- **차트 구조**: 
  - 공통 차트 (base)
  - 서비스별 차트 (override)
- **환경별 설정**: values-{env}.yaml
- **의존성 관리**: Chart.yaml dependencies

---

## 11. 보안 아키텍처

### 11.1 보안 전략
#### 11.1.1 보안 원칙
- **Zero Trust**: 모든 요청 검증
- **Defense in Depth**: 다층 보안
- **Least Privilege**: 최소 권한 원칙

#### 11.1.2 위협 모델링
| 위협 | 영향도 | 대응 방안 |
|------|--------|-----------|
| API Key 노출 | High | Azure Key Vault, Managed Identity |
| DDoS 공격 | High | Azure DDoS Protection, Rate Limiting |
| 데이터 유출 | High | 암호화, 접근 제어, 감사 로그 |
| 인젝션 공격 | Medium | Input Validation, Prepared Statement |

### 11.2 보안 구현
#### 11.2.1 인증 & 인가
- **ID 제공자**: Azure AD B2C
- **토큰 전략**: JWT (Access Token 15분, Refresh Token 7일)
- **권한 모델**: RBAC (Role-Based Access Control)

#### 11.2.2 데이터 보안
- **암호화 전략**: 
  - 전송 중: TLS 1.3
  - 저장 시: Transparent Data Encryption
- **키 관리**: Azure Key Vault
- **데이터 마스킹**: PII 데이터 마스킹

---

## 12. 품질 속성 구현 전략

### 12.1 성능 최적화
#### 12.1.1 캐싱 전략
| 계층 | 캐시 유형 | TTL | 무효화 전략 |
|------|-----------|-----|-------------|
| API Gateway | Response Cache | 5분 | Time-based |
| Application | Redis | 프로파일 24h, 장소 1h | Event-based |
| Database | Query Result | 10분 | Time-based |

#### 12.1.2 데이터베이스 최적화
- **인덱싱 전략**: 
  - 복합 인덱스 (trip_id, date)
  - 공간 인덱스 (location)
- **쿼리 최적화**: 
  - N+1 문제 해결 (Fetch Join)
  - 쿼리 실행 계획 분석
- **커넥션 풀링**: HikariCP (Max 20 per service)

### 12.2 확장성 구현
#### 12.2.1 오토스케일링
- **수평 확장**: 
  - HPA: CPU 70%, Memory 80%
  - VPA: 리소스 자동 조정
- **수직 확장**: 
  - Database: 4 vCore → 8 vCore
  - Redis: 1GB → 4GB
- **예측적 스케일링**: 
  - 성수기 사전 스케일 업
  - ML 기반 트래픽 예측 (Phase 2)

#### 12.2.2 부하 분산
- **로드 밸런서**: Azure Application Gateway
- **트래픽 분산 정책**: Round Robin, Least Connection
- **헬스체크**: 
  - Liveness: /health/live
  - Readiness: /health/ready

### 12.3 가용성 및 복원력
#### 12.3.1 장애 복구 전략
- **Circuit Breaker**: 
  - Failure Threshold: 5
  - Reset Timeout: 60s
- **Retry Pattern**: 
  - Max Attempts: 3
  - Backoff: Exponential
- **Bulkhead Pattern**: 
  - Thread Pool Isolation
  - Semaphore Isolation

#### 12.3.2 재해 복구
- **백업 전략**: 
  - Database: 일일 전체 백업, 시간별 증분
  - 보관 기간: 7일 (로컬), 30일 (Archive)
- **RTO/RPO**: 
  - RTO: 4시간
  - RPO: 1시간
- **DR 사이트**: Korea South (Phase 2)

---

## 13. 아키텍처 의사결정 기록 (ADR)

### 13.1 주요 아키텍처 결정
| ID | 결정 사항 | 결정 일자 | 상태 | 결정 이유 |
|----|-----------|-----------|------|-----------|
| ADR-001 | 마이크로서비스 아키텍처 채택 | 2024-01-15 | Accepted | 독립적 확장, 기술 다양성, 장애 격리 |
| ADR-002 | Redis 공유 캐시 사용 | 2024-01-20 | Accepted | 서비스 간 데이터 공유, 성능 최적화 |
| ADR-003 | Spring Boot 통일 | 2024-01-22 | Accepted | 팀 역량, 개발 생산성, 유지보수 |
| ADR-004 | Azure Cloud 선택 | 2024-01-10 | Accepted | 기업 표준, 지원 서비스, 비용 |
| ADR-005 | CQRS 패턴 적용 (일정 서비스) | 2024-01-25 | Accepted | 읽기/쓰기 분리, 성능 최적화 |

### 13.2 트레이드오프 분석
#### 13.2.1 성능 vs 확장성
- **고려사항**: 캐시 일관성 vs 응답 속도
- **선택**: 성능 우선 (Eventually Consistent)
- **근거**: 실시간성이 중요하지 않은 도메인 특성

#### 13.2.2 일관성 vs 가용성 (CAP 정리)
- **고려사항**: 강한 일관성 vs 고가용성
- **선택**: AP (Availability + Partition Tolerance)
- **근거**: 사용자 경험 우선, 최종 일관성으로 충분

---

## 14. 구현 로드맵

### 14.1 개발 단계
| 단계 | 기간 | 주요 산출물 | 마일스톤 |
|------|------|-------------|-----------|
| Phase 1 | 2개월 | 기본 인프라, 3개 서비스 구현 | MVP 출시 |
| Phase 2 | 2개월 | 캐시 최적화, 모니터링 구축 | 성능 최적화 완료 |
| Phase 3 | 2개월 | 고급 패턴 적용, DR 구축 | 엔터프라이즈 준비 |

### 14.2 마이그레이션 전략 (레거시 시스템이 있는 경우)
- **해당 없음** (신규 개발)

---

## 15. 위험 관리

### 15.1 아키텍처 위험
| 위험 | 영향도 | 확률 | 완화 방안 |
|------|--------|------|-----------|
| 외부 API 의존성 | High | Medium | Circuit Breaker, 캐시 전략, Fallback |
| 데이터 불일치 | Medium | Low | 정합성 검증 배치, 모니터링 |
| 성능 목표 미달 | High | Medium | 단계적 최적화, 부하 테스트 |
| 비용 초과 | Medium | Medium | 리소스 모니터링, 자동 스케일링 제한 |

### 15.2 기술 부채 관리
- **식별된 기술 부채**: 
  - 동기 통신 의존성
  - 모놀리식 프론트엔드
- **해결 우선순위**: 
  1. 비동기 메시징 도입
  2. 마이크로 프론트엔드 전환
- **해결 계획**: Phase 2-3에서 점진적 개선

---

## 16. 부록

### 16.1 참조 아키텍처
- **업계 표준**: 
  - Microsoft Azure Well-Architected Framework
  - 12 Factor App
- **내부 표준**: 
  - 사내 개발 가이드라인
  - API 설계 표준
- **외부 참조**: 
  - Spring Cloud Best Practices
  - Kubernetes Patterns

### 16.2 용어집
| 용어 | 정의 |
|------|------|
| MCP | Map Content Provider (지도 콘텐츠 제공자) |
| HPA | Horizontal Pod Autoscaler |
| VPA | Vertical Pod Autoscaler |
| TTL | Time To Live |
| RTO | Recovery Time Objective |
| RPO | Recovery Point Objective |

### 16.3 관련 문서
- 유저스토리: design/Userstory.md
- 화면설계: design/wireframe/
- 아키텍처패턴: design/pattern/아키텍처패턴.puml
- 논리아키텍처: design/backend/논리아키텍처.puml
- API 설계서: design/backend/api/*.yaml
- 외부시퀀스: design/backend/sequence/outer/*.puml
- 내부시퀀스: design/backend/sequence/inner/*.puml
- 클래스 설계서: design/backend/class/*.puml
- 데이터 설계서: design/backend/database/*
- 물리아키텍처: design/backend/system/azure-physical-architecture.md

---

## 문서 이력
| 버전 | 일자 | 작성자 | 변경 내용 | 승인자 |
|------|------|--------|-----------|-------|
| v1.0 | 2024-01-30 | 박기술(코드) | 초기 작성 | - |