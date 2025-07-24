# High Level 아키텍처 정의서

## 문서 정보
- **프로젝트명**: 여행 상세 일정 생성 서비스
- **작성일**: 2025-07-24
- **작성자**: Technical Architect
- **승인자**: Product Owner
- **버전**: v1.0

---

## 1. 개요 (Executive Summary)

### 1.1 프로젝트 개요
- **비즈니스 목적**: AI 기반으로 멤버 특성을 고려한 맞춤형 여행 일정을 자동 생성하여 여행 계획의 편의성 향상
- **핵심 기능**: 
  - 여행 멤버 프로파일 관리
  - AI 기반 일일 상세 일정 자동 생성
  - 실시간 장소 정보 검증 및 추천
  - 다국어 지원 장소 검색
- **대상 사용자**: 개인 및 그룹 여행을 계획하는 모든 여행자
- **예상 사용자 규모**: 일반 시즌 1,000명/일, 성수기 10,000명/일

### 1.2 아키텍처 범위 및 경계
- **시스템 범위**: 여행 계획 수립부터 일정 관리까지의 전체 프로세스
- **포함되는 시스템**: 
  - 프로파일 서비스 (멤버/여행 정보 관리)
  - 일정 서비스 (AI 일정 생성/관리)
  - 장소 서비스 (장소 검색/검증)
- **제외되는 시스템**: 예약 시스템, 결제 시스템, 실시간 채팅
- **외부 시스템 연동**: 
  - Claude API (AI 일정 생성)
  - 카카오 MCP (국내 지도)
  - 구글 MCP (해외 지도)

### 1.3 문서 구성
이 문서는 4+1 뷰 모델을 기반으로 구성되며, 논리적/물리적/프로세스/개발 관점에서 아키텍처를 정의합니다.

---

## 2. 아키텍처 요구사항

### 2.1 기능 요구사항 요약
| 영역 | 주요 기능 | 우선순위 |
|------|-----------|----------|
| 프로파일 관리 | 멤버 정보 CRUD, 여행 기본정보 설정 | Must |
| AI 일정 생성 | 멤버 특성 고려 일정 자동 생성 | Must |
| 장소 정보 | 실시간 검증, 다국어 검색 | Must |
| 이동 경로 | 경로 계산 및 최적화 | Must |
| 콘텐츠 관리 | 사진/메모 첨부 | Should |

### 2.2 비기능 요구사항 (NFRs)

#### 2.2.1 성능 요구사항
- **응답시간**: 
  - 일반 API: < 500ms (P95)
  - AI 일정 생성: < 30초
  - 장소 검색: < 2초
- **처리량**: 100 TPS (일반), 1,000 TPS (성수기)
- **동시사용자**: 1,000명 (일반), 10,000명 (성수기)
- **데이터 처리량**: 10GB/일

#### 2.2.2 확장성 요구사항
- **수평 확장**: 
  - 프로파일 서비스: Min 1, Max 10 인스턴스
  - 일정 서비스: Min 2, Max 20 인스턴스
  - 장소 서비스: Min 1, Max 15 인스턴스
- **수직 확장**: 필요시 인스턴스 사양 업그레이드
- **글로벌 확장**: 향후 다중 리전 지원 고려

#### 2.2.3 가용성 요구사항
- **목표 가용성**: 99.9%
- **다운타임 허용**: 월 43분
- **재해복구 목표**: RTO 4시간, RPO 1시간

#### 2.2.4 보안 요구사항
- **인증/인가**: JWT 기반 토큰 인증
- **데이터 암호화**: 전송 중 TLS, 저장 시 AES-256
- **네트워크 보안**: VNet 격리, NSG 규칙 적용
- **컴플라이언스**: 개인정보보호법 준수

### 2.3 아키텍처 제약사항
- **기술적 제약**: Azure Cloud 기반, 오픈소스 우선 사용
- **비용 제약**: 월 운영비용 $5,000 이내
- **시간 제약**: 6개월 내 MVP 출시
- **조직적 제약**: 7명 개발팀으로 구성

---

## 3. 아키텍처 설계 원칙

### 3.1 핵심 설계 원칙
1. **확장성 우선**: 수평적 확장이 가능한 구조
2. **장애 격리**: 단일 장애점 제거 및 Circuit Breaker 패턴
3. **느슨한 결합**: 마이크로서비스 간 독립성 보장
4. **관측 가능성**: 로깅, 모니터링, 추적 체계 구축
5. **보안 바이 디자인**: 설계 단계부터 보안 고려

### 3.2 아키텍처 품질 속성 우선순위
| 순위 | 품질 속성 | 중요도 | 전략 |
|------|-----------|--------|------|
| 1 | 성능 | High | 캐싱, 비동기 처리 |
| 2 | 확장성 | High | 마이크로서비스, 자동 스케일링 |
| 3 | 가용성 | Medium | Circuit Breaker, 중복 구성 |
| 4 | 보안 | Medium | 인증/인가, 암호화 |
| 5 | 유지보수성 | Low | 표준화, 문서화 |

---

## 4. 논리 아키텍처 (Logical View)

### 4.1 시스템 컨텍스트 다이어그램
```
[사용자] → [API Gateway] → [마이크로서비스]
                         ↓
                    [외부 API]
                    - Claude API
                    - 카카오 MCP
                    - 구글 MCP
```

### 4.2 도메인 아키텍처
#### 4.2.1 도메인 모델
| 도메인 | 책임 | 주요 엔티티 |
|--------|------|-------------|
| 프로파일 | 여행자 및 여행 정보 관리 | Member, Trip, TransportSetting |
| 일정 | 일정 생성 및 관리 | Itinerary, Place, Route, Attachment |
| 장소 | 장소 검색 및 검증 | Location, PlaceDetail, Recommendation |

#### 4.2.2 바운디드 컨텍스트
```
Profile Context ← 캐시 공유 → Itinerary Context
                              ↓
                        Location Context
```

### 4.3 서비스 아키텍처
#### 4.3.1 마이크로서비스 구성
| 서비스명 | 책임 | 기술스택 | 데이터베이스 |
|----------|------|----------|-------------|
| Profile Service | 멤버/여행 정보 관리 | Node.js 22, Express 4.21 | PostgreSQL 16 |
| Itinerary Service | AI 일정 생성/관리 | Node.js 22, Express 4.21 | PostgreSQL 16 |
| Location Service | 장소 검색/검증 | Node.js 22, Express 4.21 | PostgreSQL 16 |

#### 4.3.2 서비스 간 통신 패턴
- **동기 통신**: REST API (HTTP/JSON)
- **비동기 통신**: Azure Service Bus (AI 일정 생성)
- **데이터 일관성**: Redis Cache를 통한 공유

---

## 5. 프로세스 아키텍처 (Process View)

### 5.1 주요 비즈니스 프로세스
#### 5.1.1 핵심 사용자 여정
```
1. 멤버 등록 → 2. 여행 정보 설정 → 3. AI 일정 생성 → 4. 일정 수정 → 5. 장소 정보 조회
```

#### 5.1.2 시스템 간 통합 프로세스
```
Itinerary Service → Job Queue → Claude API
                  ↓
            Location Service → MCP APIs
```

### 5.2 동시성 및 동기화
- **동시성 처리 전략**: 낙관적 잠금 (Optimistic Locking)
- **락 관리**: Redis 분산 락
- **이벤트 순서 보장**: Message Queue의 FIFO 보장

---

## 6. 개발 아키텍처 (Development View)

### 6.1 개발 언어 및 프레임워크 선정
#### 6.1.1 백엔드 기술스택
| 서비스 | 언어 | 프레임워크 | 선정이유 |
|----------|------|---------------|----------|
| Profile Service | TypeScript 5.5 | Express 4.21, TypeORM 0.3 | 타입 안정성, 생산성 |
| Itinerary Service | TypeScript 5.5 | Express 4.21, TypeORM 0.3 | 타입 안정성, 생산성 |
| Location Service | TypeScript 5.5 | Express 4.21, TypeORM 0.3 | 타입 안정성, 생산성 |

#### 6.1.2 프론트엔드 기술스택
- **언어**: TypeScript 5.5
- **프레임워크**: Next.js 14.2, React 18.3
- **선정 이유**: SSR 지원, 최적화된 성능, 풍부한 생태계

### 6.2 서비스별 아키텍처 패턴
| 서비스 | 아키텍처 패턴 | 선정 이유 |
|--------|---------------|-----------|
| Profile Service | Layered Architecture | 단순한 CRUD 중심 서비스 |
| Itinerary Service | CQRS + Saga Pattern | 복잡한 비즈니스 로직, 분산 트랜잭션 |
| Location Service | Hexagonal Architecture | 다양한 외부 API 통합 |

### 6.3 코드 구조 및 모듈화
```
/src
  /controllers    # API 엔드포인트
  /services       # 비즈니스 로직
  /repositories   # 데이터 접근
  /models        # 도메인 모델
  /utils         # 공통 유틸리티
  /config        # 설정 파일
```

### 6.4 개발 가이드라인
- **코딩 표준**: ESLint + Prettier 설정
- **테스트 전략**: 
  - 단위 테스트: Jest (목표 커버리지 80%)
  - 통합 테스트: Supertest
  - E2E 테스트: Playwright
- **문서화 기준**: 
  - API: OpenAPI 3.0 명세
  - 코드: JSDoc 주석
  - 아키텍처: PlantUML 다이어그램

---

## 7. 물리 아키텍처 (Physical View)

### 7.1 클라우드 아키텍처 패턴
#### 7.1.1 선정된 클라우드 패턴
- **패턴명**: 
  - API Gateway Pattern
  - Cache-aside Pattern
  - Circuit Breaker Pattern
  - CQRS Pattern
  - Saga Pattern
- **적용 이유**: 성능 최적화, 장애 격리, 확장성 확보
- **예상 효과**: 응답시간 50% 개선, 가용성 99.9% 달성

#### 7.1.2 클라우드 제공자
- **주 클라우드**: Azure
- **멀티 클라우드 전략**: 향후 AWS 확장 고려
- **하이브리드 구성**: 해당 없음

### 7.2 인프라스트럭처 구성
#### 7.2.1 컴퓨팅 리소스
| 구성요소 | 사양 | 스케일링 전략 |
|----------|------|---------------|
| API Gateway | Standard_B2s | 수동 스케일링 |
| Profile Service | Standard_B2s | HPA (CPU 70%) |
| Itinerary Service | Standard_B4s | HPA (CPU 70%, Memory 80%) |
| Location Service | Standard_B2s | HPA (CPU 70%) |
| PostgreSQL | Standard_D4s_v3 | 수직 스케일링 |
| Redis Cache | Standard_C2 | 수동 스케일링 |

#### 7.2.2 네트워크 구성
```
인터넷 → Azure Application Gateway → API Gateway (Istio)
                                   ↓
                              서비스 메시 (VNet)
                                   ↓
                            마이크로서비스 (Subnet)
```

#### 7.2.3 보안 구성
- **방화벽**: Azure Firewall
- **WAF**: Azure Application Gateway WAF
- **DDoS 방어**: Azure DDoS Protection Standard
- **VPN/Private Link**: Azure Private Endpoint for Database

---

## 8. 기술 스택 아키텍처

### 8.1 API Gateway & Service Mesh
#### 8.1.1 API Gateway
- **제품**: Istio Gateway 1.22
- **주요 기능**: 인증, 라우팅, 레이트 리미팅, 모니터링
- **설정 전략**: 
  - Rate Limit: 100 req/min per user
  - Circuit Breaker: 5회 실패 시 Open
  - Timeout: 30초 (일반), 60초 (AI 생성)

#### 8.1.2 Service Mesh
- **제품**: Istio 1.22
- **적용 범위**: 모든 마이크로서비스
- **트래픽 관리**: 
  - Retry: 3회, exponential backoff
  - Load Balancing: Round Robin
  - mTLS: 서비스 간 암호화

### 8.2 데이터 아키텍처
#### 8.2.1 데이터베이스 전략
| 용도 | 데이터베이스 | 타입 | 특징 |
|------|-------------|------|------|
| 트랜잭션 | PostgreSQL 16 | RDBMS | 각 서비스별 독립 DB |
| 캐시 | Redis 7.2 | In-Memory | 공유 캐시, TTL 관리 |
| 검색 | PostgreSQL FTS | Search | Full Text Search 활용 |
| 분석 | Azure Monitor | Telemetry | 로그 및 메트릭 수집 |

#### 8.2.2 데이터 파이프라인
```
마이크로서비스 → Write → PostgreSQL
              ↓
         Redis Cache → Read → 클라이언트
```

### 8.3 백킹 서비스 (Backing Services)
#### 8.3.1 메시징 & 이벤트 스트리밍
- **메시지 큐**: Azure Service Bus Standard
- **이벤트 스트리밍**: 향후 Event Hubs 고려
- **이벤트 스토어**: PostgreSQL 이벤트 테이블

#### 8.3.2 스토리지 서비스
- **객체 스토리지**: Azure Blob Storage (사진 저장)
- **블록 스토리지**: Azure Managed Disks
- **파일 스토리지**: 해당 없음

### 8.4 관측 가능성 (Observability)
#### 8.4.1 로깅 전략
- **로그 수집**: Fluent Bit
- **로그 저장**: Azure Log Analytics
- **로그 분석**: KQL (Kusto Query Language)

#### 8.4.2 모니터링 & 알람
- **메트릭 수집**: Prometheus
- **시각화**: Grafana
- **알람 정책**: Azure Monitor Alerts

#### 8.4.3 분산 추적
- **추적 도구**: Jaeger
- **샘플링 전략**: 1% (일반), 100% (에러)
- **성능 분석**: APM Dashboard

---

## 9. AI/ML 아키텍처

### 9.1 AI API 통합 전략
#### 9.1.1 AI 서비스 매핑
| 목적 | AI API 제품/서비스 | Input 데이터 | Output 데이터 | SLA |
|------|-------------------|-------------|-------------|-----|
| 일정 생성 | Claude 3.5 Sonnet | 여행 프로파일, 멤버 정보 | 시간별 일정 JSON | 30초 이내 |

#### 9.1.2 AI 파이프라인
```
Itinerary Service → Job Queue → AI Processor → Claude API
                              ↓
                      Location Service (검증)
```

### 9.2 데이터 과학 플랫폼
- **모델 개발 환경**: 해당 없음 (외부 API 사용)
- **모델 배포 전략**: Claude API 직접 호출
- **모델 모니터링**: API 응답시간 및 성공률 추적

---

## 10. 개발 운영 (DevOps)

### 10.1 CI/CD 파이프라인
#### 10.1.1 지속적 통합 (CI)
- **도구**: GitHub Actions
- **빌드 전략**: 
  - 브랜치별 자동 빌드
  - Docker 이미지 생성
  - 자동화된 테스트 실행
- **테스트 자동화**: 
  - 단위 테스트
  - 통합 테스트
  - 정적 코드 분석

#### 10.1.2 지속적 배포 (CD)
- **배포 도구**: ArgoCD
- **배포 전략**: Blue-Green Deployment
- **롤백 정책**: 자동 롤백 (헬스체크 실패 시)

### 10.2 컨테이너 오케스트레이션
#### 10.2.1 Kubernetes 구성
- **클러스터 전략**: Azure AKS (Azure Kubernetes Service)
- **네임스페이스 설계**: 
  - dev: 개발 환경
  - staging: 스테이징 환경
  - prod: 운영 환경
- **리소스 관리**: 
  - Resource Quota 설정
  - HPA/VPA 자동 스케일링

#### 10.2.2 헬름 차트 관리
- **차트 구조**: 서비스별 독립 차트
- **환경별 설정**: values-{env}.yaml
- **의존성 관리**: Chart.yaml dependencies

---

## 11. 보안 아키텍처

### 11.1 보안 전략
#### 11.1.1 보안 원칙
- **Zero Trust**: 모든 요청 검증
- **Defense in Depth**: 다층 보안 구성
- **Least Privilege**: 최소 권한 원칙

#### 11.1.2 위협 모델링
| 위협 | 영향도 | 대응 방안 |
|------|--------|-----------|
| DDoS 공격 | High | Azure DDoS Protection |
| SQL Injection | High | Parameterized Query, ORM 사용 |
| API Key 노출 | Medium | Azure Key Vault |
| 세션 하이재킹 | Medium | JWT + Refresh Token |

### 11.2 보안 구현
#### 11.2.1 인증 & 인가
- **ID 제공자**: Azure AD B2C
- **토큰 전략**: JWT (Access Token 15분, Refresh Token 7일)
- **권한 모델**: RBAC (Role-Based Access Control)

#### 11.2.2 데이터 보안
- **암호화 전략**: 
  - 전송 중: TLS 1.3
  - 저장 시: AES-256-GCM
- **키 관리**: Azure Key Vault
- **데이터 마스킹**: PII 데이터 마스킹

---

## 12. 품질 속성 구현 전략

### 12.1 성능 최적화
#### 12.1.1 캐싱 전략
| 계층 | 캐시 유형 | TTL | 무효화 전략 |
|------|-----------|-----|-------------|
| API Gateway | Response Cache | 5분 | URL 기반 |
| Application | Redis | 1시간 (장소), 24시간 (프로파일) | Event-based |
| Database | Query Cache | 10분 | 자동 |

#### 12.1.2 데이터베이스 최적화
- **인덱싱 전략**: 
  - 복합 인덱스: (trip_id, date)
  - 부분 인덱스: WHERE is_active = true
- **쿼리 최적화**: 
  - N+1 문제 해결 (Eager Loading)
  - 배치 처리
- **커넥션 풀링**: 
  - Min: 10, Max: 100
  - Idle Timeout: 30초

### 12.2 확장성 구현
#### 12.2.1 오토스케일링
- **수평 확장**: 
  - HPA: CPU 70%, Memory 80%
  - 스케일 아웃: 30초 대기
  - 스케일 인: 5분 대기
- **수직 확장**: 수동 조정
- **예측적 스케일링**: 성수기 사전 확장

#### 12.2.2 부하 분산
- **로드 밸런서**: Azure Load Balancer
- **트래픽 분산 정책**: Round Robin
- **헬스체크**: 
  - Interval: 10초
  - Timeout: 5초
  - Unhealthy Threshold: 3회

### 12.3 가용성 및 복원력
#### 12.3.1 장애 복구 전략
- **Circuit Breaker**: 
  - Failure Threshold: 5회
  - Reset Timeout: 60초
  - Half-Open 시도: 3회
- **Retry Pattern**: 
  - Max Attempts: 3
  - Backoff: Exponential
- **Bulkhead Pattern**: 
  - Thread Pool 격리
  - Queue Size: 100

#### 12.3.2 재해 복구
- **백업 전략**: 
  - DB: 일일 전체 백업, 시간별 증분 백업
  - 보관 기간: 30일
- **RTO/RPO**: 
  - RTO: 4시간
  - RPO: 1시간
- **DR 사이트**: Azure 타 리전 (Korea South → Japan East)

---

## 13. 아키텍처 의사결정 기록 (ADR)

### 13.1 주요 아키텍처 결정
| ID | 결정 사항 | 결정 일자 | 상태 | 결정 이유 |
|----|-----------|-----------|------|-----------|
| ADR-001 | 마이크로서비스 아키텍처 채택 | 2025-07-24 | Accepted | 독립적 확장성, 장애 격리 |
| ADR-002 | PostgreSQL 선택 | 2025-07-24 | Accepted | 오픈소스, 풍부한 기능, JSON 지원 |
| ADR-003 | TypeScript 채택 | 2025-07-24 | Accepted | 타입 안정성, 개발 생산성 |
| ADR-004 | Istio Service Mesh | 2025-07-24 | Accepted | 트래픽 관리, 보안, 관측성 |
| ADR-005 | Redis 공유 캐시 | 2025-07-24 | Accepted | 서비스 간 데이터 공유, 성능 |

### 13.2 트레이드오프 분석
#### 13.2.1 성능 vs 확장성
- **고려사항**: 캐시 일관성 vs 분산 확장
- **선택**: 확장성 우선 (Eventually Consistent)
- **근거**: 여행 데이터는 실시간성보다 확장성이 중요

#### 13.2.2 일관성 vs 가용성 (CAP 정리)
- **고려사항**: 강한 일관성 vs 높은 가용성
- **선택**: AP (가용성 + 분할 허용)
- **근거**: 서비스 가용성이 데이터 일관성보다 중요

---

## 14. 구현 로드맵

### 14.1 개발 단계
| 단계 | 기간 | 주요 산출물 | 마일스톤 |
|------|------|-------------|-----------|
| Phase 1 | 2개월 | 기본 인프라, CI/CD | 개발 환경 구축 완료 |
| Phase 2 | 2개월 | 3개 마이크로서비스 | 핵심 기능 구현 완료 |
| Phase 3 | 1개월 | 통합 테스트, 성능 최적화 | Beta 출시 |
| Phase 4 | 1개월 | 운영 준비, 문서화 | GA 출시 |

### 14.2 마이그레이션 전략 (레거시 시스템이 있는 경우)
- **데이터 마이그레이션**: 해당 없음 (신규 시스템)
- **기능 마이그레이션**: 해당 없음
- **병행 운영**: 해당 없음

---

## 15. 위험 관리

### 15.1 아키텍처 위험
| 위험 | 영향도 | 확률 | 완화 방안 |
|------|--------|------|-----------|
| 외부 API 의존성 | High | Medium | Circuit Breaker, 캐싱, Fallback |
| 성수기 트래픽 폭증 | High | High | Auto-scaling, 부하 테스트 |
| 데이터 불일치 | Medium | Low | 이벤트 순서 보장, 정합성 체크 |
| 보안 취약점 | High | Low | 정기 보안 감사, 자동화 스캔 |

### 15.2 기술 부채 관리
- **식별된 기술 부채**: 
  - 동기식 서비스 간 통신
  - 모놀리식 프론트엔드
- **해결 우선순위**: 
  1. 비동기 메시징 도입
  2. 마이크로 프론트엔드 전환
- **해결 계획**: Phase 4 이후 점진적 개선

---

## 16. 부록

### 16.1 참조 아키텍처
- **업계 표준**: 
  - 12 Factor App
  - Cloud Native Architecture
  - Microsoft Azure Well-Architected Framework
- **내부 표준**: 회사 개발 가이드라인
- **외부 참조**: 
  - Martin Fowler's Microservices
  - Domain-Driven Design

### 16.2 용어집
| 용어 | 정의 |
|------|------|
| MCP | Model Context Protocol - AI 모델과의 통신 프로토콜 |
| HPA | Horizontal Pod Autoscaler - 수평적 자동 확장 |
| VPA | Vertical Pod Autoscaler - 수직적 자동 확장 |
| TTL | Time To Live - 캐시 유효 시간 |
| CQRS | Command Query Responsibility Segregation |

### 16.3 관련 문서
- **요구사항 명세서**: design/Userstory.md
- **API 명세서**: design/backend/api/*.yaml
- **운영 가이드**: 작성 예정

---

## 문서 이력
| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| v1.0 | 2025-07-24 | Technical Architect | 초기 작성 |