# 물리 아키텍처 설계서 - 개발환경

## 1. 개요

### 1.1 설계 목적
- AI 기반 여행 일정 생성 서비스의 **개발환경** 물리 아키텍처 설계
- MVP 단계의 빠른 개발과 검증을 위한 최소 구성
- 비용 효율성과 개발 편의성 우선

### 1.2 설계 원칙
- **MVP 우선**: 빠른 개발과 검증을 위한 최소 구성
- **비용 최적화**: Spot Instances, Local Storage 활용
- **개발 편의성**: 복잡한 설정 최소화, 빠른 배포
- **단순성**: 운영 복잡도 최소화

### 1.3 참조 아키텍처
- 마스터 아키텍처: design/backend/physical/physical-architecture.md
- HighLevel아키텍처정의서: design/high-level-architecture.md
- 논리아키텍처: design/backend/logical/logical-architecture.md

## 2. 개발환경 아키텍처 개요

### 2.1 환경 특성
- **목적**: 빠른 개발과 검증
- **사용자**: 개발팀 (5명)
- **가용성**: 95% (월 36시간 다운타임 허용)
- **확장성**: 제한적 (고정 리소스)
- **보안**: 기본 보안 (복잡한 보안 설정 최소화)

### 2.2 전체 아키텍처
```
[개발자] 
  ↓ (HTTP/HTTPS)
[Kubernetes Ingress Controller (NGINX)]
  ↓
[AKS 클러스터 - 단일 서브넷]
  ├── User Service Pod
  ├── Trip Service Pod  
  ├── AI Service Pod
  ├── Location Service Pod
  ├── PostgreSQL Pod (Local Storage)
  ├── Redis Pod (Memory Only)
  └── Service Bus 연결 (Basic Tier)
```

## 3. 컴퓨팅 아키텍처

### 3.1 Azure Kubernetes Service (AKS) 구성

#### 3.1.1 클러스터 설정

| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| Kubernetes 버전 | 1.29 | 안정화된 최신 버전 |
| 서비스 계층 | Basic | 비용 최적화 |
| Network Plugin | Azure CNI | Azure 네이티브 네트워킹 |
| Network Policy | Kubernetes Network Policies | 기본 Pod 통신 제어 |
| Ingress Controller | NGINX Ingress Controller | 오픈소스 Ingress |
| DNS | CoreDNS | 클러스터 DNS |

#### 3.1.2 노드 풀 구성

| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| VM 크기 | Standard_B2s | 2 vCPU, 4GB RAM |
| 노드 수 | 2 | 고정 노드 수 |
| 자동 스케일링 | Disabled | 비용 절약을 위한 고정 크기 |
| 최대 Pod 수 | 30 | 노드당 최대 Pod |
| 가용 영역 | Zone-1 | 단일 영역 (비용 절약) |
| 가격 정책 | Spot Instance | 70% 비용 절약 |

### 3.2 서비스별 리소스 할당

#### 3.2.1 애플리케이션 서비스
| 서비스 | CPU Requests | Memory Requests | CPU Limits | Memory Limits | Replicas |
|--------|--------------|-----------------|------------|---------------|----------|
| User Service | 50m | 128Mi | 200m | 256Mi | 1 |
| Trip Service | 100m | 256Mi | 500m | 512Mi | 1 |
| AI Service | 200m | 512Mi | 1000m | 1Gi | 1 |
| Location Service | 50m | 128Mi | 200m | 256Mi | 1 |

#### 3.2.2 백킹 서비스
| 서비스 | CPU Requests | Memory Requests | CPU Limits | Memory Limits | Storage |
|--------|--------------|-----------------|------------|---------------|---------|
| PostgreSQL | 500m | 1Gi | 1000m | 2Gi | 20GB (hostPath) |
| Redis | 100m | 256Mi | 500m | 1Gi | Memory Only |

## 4. 네트워크 아키텍처

### 4.1 네트워크 구성

#### 4.1.1 네트워크 토폴로지
```
[인터넷]
  ↓ (LoadBalancer Service)
[NGINX Ingress Controller]
  ↓ (ClusterIP)
[Application Services]
  ↓ (ClusterIP)
[Database Services]
```

#### 4.1.2 네트워크 보안

**기본 Network Policy:**
| 정책 유형 | 설정 | 설명 |
|-----------|------|---------|
| Default Policy | ALLOW_ALL_NAMESPACES | 개발 편의성을 위한 허용적 정책 |
| Complexity Level | Basic | 단순한 보안 구성 |

**Database 접근 제한:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 허용 대상 | Application Tier Pods | tier: application 레이블 |
| 프로토콜 | TCP | 데이터베이스 연결 |
| 포트 | 5432 | PostgreSQL 기본 포트 |

### 4.2 서비스 디스커버리

| 서비스 | 내부 주소 | 포트 | 용도 |
|--------|-----------|------|------|
| User Service | user-service.default.svc.cluster.local | 8080 | 사용자 관리 API |
| Trip Service | trip-service.default.svc.cluster.local | 8080 | 여행 계획 API |
| AI Service | ai-service.default.svc.cluster.local | 8080 | AI 일정 생성 API |
| Location Service | location-service.default.svc.cluster.local | 8080 | 위치 정보 API |
| PostgreSQL | postgresql.default.svc.cluster.local | 5432 | 메인 데이터베이스 |
| Redis | redis.default.svc.cluster.local | 6379 | 캐시 서버 |

## 5. 데이터 아키텍처

### 5.1 데이터베이스 구성

#### 5.1.1 PostgreSQL Pod 구성

**기본 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 이미지 | bitnami/postgresql:16 | 안정화된 PostgreSQL 16 |
| CPU 요청 | 500m | 기본 CPU 할당 |
| Memory 요청 | 1Gi | 기본 메모리 할당 |
| CPU 제한 | 1000m | 최대 CPU 사용량 |
| Memory 제한 | 2Gi | 최대 메모리 사용량 |

**스토리지 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 스토리지 타입 | hostPath | 로컬 스토리지 (비용 절약) |
| 스토리지 크기 | 20Gi | 개발용 충분한 용량 |
| 마운트 경로 | /data/postgresql | 데이터 저장 경로 |

**데이터베이스 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 최대 연결 수 | 100 | 동시 연결 제한 |
| Shared Buffers | 256MB | 공유 버퍼 크기 |
| Effective Cache Size | 1GB | 효과적 캐시 크기 |
| 백업 전략 | 수동 백업 | 주 1회 수동 실행 |

#### 5.1.2 Redis Pod 구성

**기본 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 이미지 | bitnami/redis:7.2 | 최신 안정 Redis 버전 |
| CPU 요청 | 100m | 기본 CPU 할당 |
| Memory 요청 | 256Mi | 기본 메모리 할당 |
| CPU 제한 | 500m | 최대 CPU 사용량 |
| Memory 제한 | 1Gi | 최대 메모리 사용량 |

**Redis 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 데이터 지속성 | Disabled | 개발용, 재시작 시 데이터 손실 허용 |
| 최대 메모리 | 512MB | 메모리 사용 제한 |
| 메모리 정책 | allkeys-lru | LRU 방식 캐시 제거 |

### 5.2 데이터 관리 전략

#### 5.2.1 데이터 초기화
```bash
# 개발 데이터 자동 생성
kubectl apply -f k8s/data-init/
# - 테스트 사용자 데이터
# - 샘플 여행지 데이터
# - AI 서비스 테스트 데이터
```

#### 5.2.2 백업 전략

| 서비스 | 백업 방법 | 주기 | 보존 전략 | 참고사항 |
|--------|----------|------|-----------|----------|
| PostgreSQL | kubectl exec + pg_dump | 수동 (필요 시) | 로컬 파일 저장 | 개발용 데이터 수동 관리 |
| Redis | 없음 | - | 메모리 전용 | 재시작 시 캐시 재구성 |

## 6. 메시징 아키텍처

### 6.1 Azure Service Bus 구성

#### 6.1.1 Basic Tier 설정

**Service Bus 전체 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 서비스 계층 | Basic | 비용 최적화 계층 |
| 네임스페이스 | sb-tripgen-dev | 개발환경 전용 |
| 최대 큐 크기 | 1GB | 전체 큐 사이즈 제한 |
| 메시지 TTL | 14일 | 메시지 생존 기간 |

**큐 별 설정:**
| 큐 이름 | 최대 크기 | 중복 감지 | 용도 |
|--------|----------|----------|------|
| ai-schedule-generation | 256MB | Disabled | AI 일정 생성 요청 |
| location-search | 256MB | Disabled | 위치 검색 요청 |
| notification | 256MB | Disabled | 알림 메시지 |

#### 6.1.2 연결 설정

| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 인증 방식 | Azure Managed Identity | 운영환경 대비 보안 강화 |
| 연결 풀링 | Disabled | 개발환경 단순 구성 |
| 재시도 정책 | Basic | 기본 재시도 설정 |

## 7. 보안 아키텍처

### 7.1 개발환경 보안 정책

#### 7.1.1 기본 보안 설정

**보안 계층 설정:**
| 계층 | 설정 | 수준 | 설명 |
|------|------|------|----------|
| 전체 복잡도 | Basic | 기본 | 개발 편의성 우선 |
| 인증 | JWT | 기본 | 개발용 고정 시크릿 |
| 인증 검증 | API Gateway | 단순 | Gateway 레벨만 검증 |
| 인가 | Role-based | 기본 | 단순한 역할 기반 |
| 내부 암호화 | Disabled | 없음 | 개발환경 단순화 |
| 외부 암호화 | HTTPS | Ingress | Ingress 레벨 HTTPS만 |

#### 7.1.2 시크릿 관리

**시크릿 관리 전략:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 관리 방식 | Kubernetes Secrets | 기본 K8s 에 내장된 방식 |
| 순환 정책 | 수동 | 개발환경 단순 관리 |
| 저장소 | etcd | 클러스터 기본 저장소 |

**관리 대상 시크릿:**
| 시크릿 이름 | 용도 | 순환 주기 |
|-------------|------|----------|
| database_password | PostgreSQL 접근 | 수동 |
| redis_password | Redis 접근 | 수동 |
| jwt_secret | JWT 토큰 서명 | 수동 |
| openai_api_key | OpenAI API 접근 | 수동 |

### 7.2 Network Policies

#### 7.2.1 기본 정책

**Network Policy 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| Policy 이름 | dev-basic-policy | 개발환경 기본 정책 |
| API 버전 | networking.k8s.io/v1 | Kubernetes Network Policy v1 |
| Pod 선택자 | {} (전체) | 모든 Pod에 적용 |
| 정책 유형 | Ingress, Egress | 인바운드/아웃바운드 모두 제어 |
| Ingress 규칙 | {} (전체 허용) | 개발환경 편의상 모든 인바운드 허용 |
| Egress 규칙 | {} (전체 허용) | 모든 아웃바운드 허용 |

## 8. 모니터링 및 로깅

### 8.1 기본 모니터링

#### 8.1.1 Kubernetes 기본 모니터링

**모니터링 스택:**
| 구성요소 | 상태 | 설명 |
|-----------|------|----------|
| Metrics Server | Enabled | 기본 리소스 메트릭 수집 |
| Kubernetes Dashboard | Enabled | 개발용 대시보드 |

**기본 알림 설정:**
| 알림 유형 | 임계값 | 설명 |
|-----------|----------|----------|
| Pod Crash Loop | 5회 이상 | Pod 재시작 반복 감지 |
| Node Not Ready | 5분 이상 | 노드 비정상 상태 감지 |
| High Memory Usage | 90% 이상 | 메모리 사용량 과다 감지 |

#### 8.1.2 애플리케이션 모니터링

**헬스체크 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| Health Check Path | /actuator/health | Spring Boot Actuator 엔드포인트 |
| 체크 주기 | 30초 | 상태 확인 간격 |
| 타임아웃 | 5초 | 응답 대기 시간 |

**수집 메트릭:**
| 메트릭 유형 | 도구 | 용도 |
|-----------|------|------|
| JVM Metrics | Micrometer | 자바 가상머신 성능 |
| HTTP Request Metrics | Micrometer | API 요청 통계 |
| Database Connection Pool | Micrometer | DB 연결 풀 상태 |

### 8.2 로깅

#### 8.2.1 로그 수집

**로그 수집 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 수집 방식 | kubectl logs | Kubernetes 기본 로그 명령어 |
| 저장 방식 | 로컬 파일 시스템 | 노드 로컬 스토리지 |
| 보존 기간 | 7일 | 개발환경 단기 보존 |

**로그 레벨 설정:**
| 로거 유형 | 레벨 | 설명 |
|-----------|------|----------|
| Root Logger | INFO | 전체 시스템 기본 레벨 |
| Application Logger | DEBUG | 개발용 상세 로그 |
| Database Logger | WARN | 데이터베이스 주요 이벤트만 |

## 9. CI/CD 및 배포

### 9.1 개발환경 CI/CD

#### 9.1.1 빌드 파이프라인

**파이프라인 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 트리거 | push to develop branch | develop 브랜치 푸시 시 자동 실행 |

**빌드 단계:**
| 단계 | 작업 | 설명 |
|------|------|----------|
| Unit Tests | 기본 테스트만 | 필수 단위 테스트 |
| Build Image | Docker build | 컨테이너 이미지 빌드 |
| Push Registry | ACR push | Azure Container Registry 업로드 |

**품질 게이트:**
| 게이트 유형 | 임계값 | 필수 여부 |
|-----------|----------|----------|
| Unit Test Coverage | 50% 이상 | 선택 |
| Build Success | 성공 | 필수 |

#### 9.1.2 배포 전략

**배포 방식 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 배포 방식 | Rolling Update | 점진적 업데이트 |
| 최대 비가용 | 1 | 동시 업데이트 가능 Pod 수 |
| 최대 추가 | 1 | 기존 Pod 수 대비 추가 가능 수 |

**자동화 설정:**
| 자동화 유형 | 설정 | 설명 |
|-----------|------|----------|
| 자동 배포 | develop 브랜치 | 소스 변경 시 자동 배포 |
| 롤백 | 수동 | 수동 롤백 작업 |
| 헬스체크 | 기본 | 기본 liveness/readiness 체크 |

### 9.2 개발 워크플로우

#### 9.2.1 일상 개발 프로세스
```bash
# 1. 코드 변경
git push origin feature/new-feature

# 2. 자동 빌드 및 배포
# GitHub Actions가 자동 실행

# 3. 개발환경 확인
kubectl get pods
curl http://dev-tripgen.local/api/health

# 4. 로그 확인
kubectl logs -f deployment/trip-service
```

## 10. 비용 최적화

### 10.1 개발환경 비용 구조

#### 10.1.1 주요 비용 요소
| 구성요소 | 사양 | 월간 예상 비용 (USD) | 절약 방안 |
|----------|------|---------------------|-----------|
| AKS 노드 | Standard_B2s × 2 | $120 | Spot Instance |
| PostgreSQL | Pod 기반 | $0 | 로컬 스토리지 |
| Redis | Pod 기반 | $0 | 메모리 전용 |
| Service Bus | Basic | $10 | 최소 구성 |
| Load Balancer | Basic | $20 | 단일 인스턴스 |
| **총합** | | **$150** | |

#### 10.1.2 비용 절약 전략

**컴퓨팅 비용 절약:**
| 절약 방안 | 절약률 | 설명 |
|-----------|----------|----------|
| Spot Instances 사용 | 70% | 예비 인스턴스 활용 |
| 비업무시간 자동 종료 | 반적 | 스케줄링 기반 종료 |
| 리소스 최적화 | 20-30% | requests/limits 정밀 설정 |

**스토리지 비용 절약:**
| 절약 방안 | 절약률 | 설명 |
|-----------|----------|----------|
| hostPath 사용 | 100% | Managed Disk 대비 완전 절약 |
| emptyDir 활용 | 100% | 임시 데이터 지역 저장소 |

**네트워킹 비용 절약:**
| 절약 방안 | 절약률 | 설명 |
|-----------|----------|----------|
| 단일 Load Balancer | 50% | 하나의 로드밸런서로 다중 서비스 |
| ClusterIP 활용 | 100% | 내부 통신 비용 없음 |

## 11. 개발환경 운영 가이드

### 11.1 일상 운영

#### 11.1.1 환경 시작/종료
```bash
# 환경 시작
kubectl scale deployment --replicas=1 --all

# 환경 종료 (야간/주말)
kubectl scale deployment --replicas=0 --all
# PostgreSQL은 유지 (데이터 보존)
```

#### 11.1.2 데이터 관리
```bash
# 테스트 데이터 초기화
kubectl apply -f k8s/data-init/

# 개발 데이터 백업
kubectl exec postgresql-0 -- pg_dump tripgen > backup.sql

# 데이터 복원
kubectl exec -i postgresql-0 -- psql tripgen < backup.sql
```

### 11.2 트러블슈팅

#### 11.2.1 일반적인 문제 해결

| 문제 유형 | 원인 | 해결방안 | 예방법 |
|-----------|------|----------|----------|
| Pod Pending | 리소스 부족 | 노드 스케일 업 또는 리소스 조정 | 리소스 모니터링 강화 |
| Database Connection Failed | PostgreSQL Pod 재시작 | Pod 상태 확인 및 재시작 | Health Check 강화 |
| Out of Memory | 메모리 한계 초과 | limits 조정 또는 불필요한 Pod 종료 | 메모리 사용량 모니터링 |

## 12. 운영환경 전환 준비

### 12.1 운영환경 차이점

#### 12.1.1 주요 차이사항
| 구성요소 | 개발환경 | 운영환경 |
|----------|----------|----------|
| 가용성 | 95% | 99.9% |
| 백업 | 수동 | 자동 (일일) |
| 보안 | 기본 | 엔터프라이즈 |
| 모니터링 | 기본 | 고급 (APM) |
| 스토리지 | hostPath | Azure Managed |

#### 12.1.2 전환 체크리스트

**데이터 마이그레이션:**
| 체크 항목 | 상태 | 우선순위 | 비고 |
|-----------|------|----------|------|
| 개발 데이터 백업 | ☐ | 높음 | pg_dump 사용 |
| 스키마 마이그레이션 스크립트 준비 | ☐ | 높음 | Flyway/Liquibase 고려 |
| 테스트 데이터 정리 | ☐ | 중간 | 운영 데이터와 분리 |

**설정 변경:**
| 체크 항목 | 상태 | 우선순위 | 비고 |
|-----------|------|----------|------|
| 환경 변수 분리 | ☐ | 높음 | ConfigMap/Secret 분리 |
| 시크릿 관리 방식 변경 | ☐ | 높음 | Azure Key Vault 전환 |
| 네트워크 정책 강화 | ☐ | 중간 | Private Endpoint 설정 |

**모니터링 설정:**
| 체크 항목 | 상태 | 우선순위 | 비고 |
|-----------|------|----------|------|
| 로그 수집 방식 변경 | ☐ | 중간 | Azure Monitor 연동 |
| 메트릭 수집 설정 | ☐ | 중간 | Application Insights |
| 알림 정책 수립 | ☐ | 낮음 | PagerDuty/Teams 연동 |

## 13. 결론

### 13.1 개발환경 핵심 가치
1. **빠른 개발**: 복잡한 설정 최소화로 개발 속도 향상
2. **비용 효율**: Spot Instance와 로컬 스토리지로 비용 최소화
3. **단순성**: 운영 복잡도 최소화로 개발 집중
4. **실험성**: 새로운 기능 빠른 검증 가능

### 13.2 개발환경 제약사항
- **가용성**: 95% (Spot Instance 사용으로 인한 불안정성)
- **확장성**: 제한적 (고정 리소스)
- **보안**: 기본 수준 (운영 대비 단순화)
- **백업**: 수동 관리 필요

이 개발환경은 **빠른 MVP 개발과 검증**에 최적화되어 있으며, 운영환경으로의 점진적 전환을 지원합니다.