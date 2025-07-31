# TripGen 물리 아키텍처 설계서 - 개발환경

## 1. 개요

### 1.1 설계 목적
AI 기반 여행 일정 생성 서비스 TripGen의 **개발환경** 물리 아키텍처 설계
- MVP 단계의 빠른 개발과 검증을 위한 최소 구성
- 비용 효율성과 개발 편의성을 우선으로 한 환경 구축
- 개발팀의 생산성 극대화를 위한 단순화된 아키텍처

### 1.2 설계 원칙
- **MVP 우선**: 빠른 개발과 검증을 위한 최소 필수 구성
- **비용 최적화**: Pod 기반 백킹서비스, Spot Instance 활용으로 비용 절감
- **개발 편의성**: 복잡한 설정 최소화, 빠른 배포 및 테스트 환경
- **단순성**: 운영 복잡도 최소화, 단일 클러스터 구성

### 1.3 참조 아키텍처
- 마스터 아키텍처: design/backend/physical/physical-architecture.md
- HighLevel 아키텍처: design/high-level-architecture.md
- 논리 아키텍처: design/backend/logical/logical-architecture.md
- 아키텍처 패턴: design/pattern/architecture-pattern.md

## 2. 개발환경 아키텍처 개요

### 2.1 환경 특성
- **목적**: 빠른 개발과 검증, 기능 테스트 및 통합 테스트
- **사용자 규모**: 개발팀 (6명), QA 테스터 (2명)
- **가용성 목표**: 95% (월 36시간 다운타임 허용)
- **확장성**: 제한적 확장성 (고정 리소스 풀)
- **보안 수준**: 기본 보안 설정 (복잡한 보안 구성 최소화)

### 2.2 전체 아키텍처

📄 **[개발환경 물리 아키텍처 다이어그램](./physical-architecture-dev.mmd)**

**주요 구성 요소:**
- Azure Kubernetes Service (AKS) 기본 클러스터
- Kubernetes Ingress Controller → 마이크로서비스 Pod
- 애플리케이션 Pod: User, Trip, AI, Location Service
- 백킹서비스 Pod: PostgreSQL (클라우드 스토리지), Redis (메모리 전용)
- Azure Service Bus Basic Tier 연결

## 3. 컴퓨팅 아키텍처

### 3.1 Kubernetes 클러스터 구성

#### 3.1.1 클러스터 설정

| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| Kubernetes 버전 | 1.29 | 안정화된 최신 버전 |
| 서비스 계층 | Basic | 비용 최적화 계층 |
| Network Plugin | Azure CNI | Azure 네이티브 네트워킹 |
| Network Policy | Azure Network Policies | 기본 Pod 통신 제어 |
| DNS | CoreDNS | 클러스터 내부 DNS |
| Ingress Controller | Kubernetes Ingress | 기본 Ingress 컨트롤러 |

#### 3.1.2 노드 풀 구성

| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| VM 크기 | Standard_B2s | 2 vCPU, 4GB RAM (개발용) |
| 노드 수 | 2 | 고정 노드 수 |
| 자동 스케일링 | Disabled | 비용 절약을 위한 고정 크기 |
| 최대 Pod 수 | 30 | 노드당 최대 Pod 수 |
| 가용 영역 | Zone-1 | 단일 영역 배치 (비용 절약) |
| 가격 정책 | Spot Instance | 70% 비용 절약 |

### 3.2 서비스별 리소스 할당

#### 3.2.1 애플리케이션 서비스
| 서비스 | CPU Requests | Memory Requests | CPU Limits | Memory Limits | Replicas |
|--------|--------------|-----------------|------------|---------------|----------|
| User Service | 100m | 256Mi | 500m | 512Mi | 1 |
| Trip Service | 200m | 512Mi | 1000m | 1Gi | 1 |
| AI Service | 300m | 512Mi | 1500m | 1Gi | 1 |
| Location Service | 100m | 256Mi | 500m | 512Mi | 1 |

#### 3.2.2 백킹 서비스

| 서비스 | CPU Requests | Memory Requests | CPU Limits | Memory Limits | Storage |
|--------|--------------|-----------------|------------|---------------|---------|
| PostgreSQL | 500m | 1Gi | 1000m | 2Gi | 50GB (Azure Disk Standard) |
| Redis | 100m | 512Mi | 500m | 1Gi | Memory Only |

#### 3.2.3 스토리지 클래스 구성

| 스토리지 클래스 | 타입 | 용도 | 성능 | 비용 |
|----------------|------|------|------|------|
| managed-csi | Azure Disk Standard | PostgreSQL 데이터 | IOPS 500 | 표준 |
| managed-csi-premium | Azure Disk Premium | 미사용 | - | - |
| azurefile | Azure Files | 설정 파일 공유 | SMB 3.0 | 표준 |

## 4. 네트워크 아키텍처

### 4.1 네트워크 구성

#### 4.1.1 네트워크 토폴로지

📄 **[개발환경 네트워크 다이어그램](./network-dev.mmd)**

**Virtual Network (VNet) 구성:**
- VNet 주소 공간: 10.1.0.0/16
- Public Subnet: 10.1.1.0/24 (Load Balancer)
- Private Subnet: 10.1.2.0/24 (AKS Cluster)
- Message Subnet: 10.1.3.0/24 (Service Bus)

#### 4.1.2 네트워크 보안

**Network Policy 설정:**
| 정책 유형 | 설정 | 설명 |
|-----------|------|---------|
| Default Policy | ALLOW_INTERNAL | 클러스터 내부 통신 허용 |
| Ingress Policy | Gateway Only | API Gateway를 통한 접근만 허용 |
| Egress Policy | External APIs | 외부 API 접근 허용 |
| Database Access | App Tier Only | 애플리케이션 계층에서만 DB 접근 |

**보안 그룹 규칙:**
| 방향 | 규칙 이름 | 포트 | 소스/대상 | 프로토콜 |
|------|-----------|------|-----------|----------|
| Inbound | HTTPS-Allow | 443 | Internet | TCP |
| Inbound | HTTP-Allow | 80 | Internet | TCP |
| Inbound | K8s-NodePort | 30000-32767 | VNet | TCP |
| Outbound | External-APIs | 443 | Internet | TCP |

### 4.2 서비스 디스커버리
| 서비스 | 내부 DNS 주소 | 포트 | 용도 |
|--------|---------------|------|------|
| User Service | user-service.tripgen-dev.svc.cluster.local | 8080 | 사용자 관리 API |
| Trip Service | trip-service.tripgen-dev.svc.cluster.local | 8080 | 여행 계획 API |
| AI Service | ai-service.tripgen-dev.svc.cluster.local | 8080 | AI 일정 생성 API |
| Location Service | location-service.tripgen-dev.svc.cluster.local | 8080 | 위치 정보 API |
| PostgreSQL | postgresql.tripgen-dev.svc.cluster.local | 5432 | 주 데이터베이스 |
| Redis | redis.tripgen-dev.svc.cluster.local | 6379 | 캐시 서버 |

## 5. 데이터 아키텍처

### 5.1 데이터베이스 구성

#### 5.1.1 주 데이터베이스 Pod 구성

**PostgreSQL 기본 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 컨테이너 이미지 | postgres:16-alpine | 경량화된 PostgreSQL 16 |
| CPU 요청 | 500m | 기본 CPU 할당 |
| Memory 요청 | 1Gi | 기본 메모리 할당 |
| CPU 제한 | 1000m | 최대 CPU 사용량 |
| Memory 제한 | 2Gi | 최대 메모리 사용량 |

**스토리지 구성:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 스토리지 클래스 | managed-csi | Azure Disk Standard |
| 스토리지 크기 | 50Gi | 개발용 적정 용량 |
| 액세스 모드 | ReadWriteOnce | 단일 노드 접근 |
| 마운트 경로 | /var/lib/postgresql/data | PostgreSQL 데이터 디렉토리 |

**데이터베이스 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 최대 연결 수 | 100 | 동시 연결 제한 |
| Shared Buffers | 256MB | 공유 버퍼 크기 |
| Effective Cache Size | 1GB | 효과적 캐시 크기 |
| Work Memory | 4MB | 작업 메모리 |
| WAL Buffers | 16MB | Write-Ahead Log 버퍼 |

#### 5.1.2 캐시 Pod 구성

**Redis 기본 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 컨테이너 이미지 | redis:7.2-alpine | 경량화된 Redis 7.2 |
| CPU 요청 | 100m | 기본 CPU 할당 |
| Memory 요청 | 512Mi | 기본 메모리 할당 |
| CPU 제한 | 500m | 최대 CPU 사용량 |
| Memory 제한 | 1Gi | 최대 메모리 사용량 |

**Redis 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 데이터 지속성 | Disabled | 개발용, 재시작 시 데이터 손실 허용 |
| 최대 메모리 | 800MB | 메모리 사용 제한 |
| 메모리 정책 | allkeys-lru | LRU 방식 캐시 제거 |
| 키 만료 정책 | Enabled | TTL 기반 자동 삭제 |

### 5.2 데이터 관리 전략

#### 5.2.1 데이터 초기화

**Kubernetes Job을 이용한 데이터 초기화:**

```bash
# 데이터베이스 스키마 초기화
kubectl create job db-schema-init --image=postgres:16-alpine \
  --dry-run=client -o yaml > db-init-job.yaml

# Job 실행 명령어
kubectl apply -f k8s/data-init/
```

**초기화 프로세스:**
1. **Database Schema Job**: 테이블 스키마, 인덱스, 제약조건 생성
2. **Sample Data Job**: 개발/테스트용 샘플 데이터 삽입
3. **Validation Job**: 데이터 무결성 검증

**테스트 데이터 구성:**
| 데이터 유형 | 샘플 수 | 설명 |
|------------|---------|------|
| 사용자 계정 | 10개 | 다양한 권한 레벨의 테스트 사용자 |
| 여행지 정보 | 100개 | 국내외 주요 여행지 |
| 샘플 일정 | 20개 | AI 학습용 여행 일정 템플릿 |
| 장소 정보 | 500개 | 카테고리별 장소 정보 |

**Job 실행 설정:**

```yaml
# Database Schema Init Job 예시
apiVersion: batch/v1
kind: Job
metadata:
  name: database-schema-init
spec:
  backoffLimit: 3
  template:
    spec:
      restartPolicy: Never
      containers:
      - name: postgres-client
        image: postgres:16-alpine
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 500m
            memory: 256Mi
        env:
        - name: PGPASSWORD
          valueFrom:
            secretKeyRef:
              name: postgresql-secret
              key: password
        command: ["/bin/bash"]
        args: 
        - -c
        - |
          echo "Starting database schema initialization..."
          psql -h postgresql.tripgen-dev.svc.cluster.local -U postgres -d tripgen -f /scripts/schema.sql
          echo "Schema initialization completed."
        volumeMounts:
        - name: init-scripts
          mountPath: /scripts
      volumes:
      - name: init-scripts
        configMap:
          name: database-init-scripts
```

**초기화 상태 확인:**
```bash
# Job 실행 상태 확인
kubectl get jobs -n tripgen-dev

# Job 로그 확인
kubectl logs job/database-schema-init -n tripgen-dev

# 데이터 초기화 검증
kubectl exec -it postgresql-0 -n tripgen-dev -- psql -U postgres -d tripgen -c "SELECT COUNT(*) FROM users;"
```

#### 5.2.2 백업 전략

| 서비스 | 백업 방법 | 주기 | 보존 전략 | 복구 시간 |
|--------|----------|------|-----------|----------|
| PostgreSQL | pg_dump + PVC 스냅샷 | 수동 실행 | 로컬 스토리지 7일 | 30분 |
| Redis | 없음 | - | 메모리 전용 | N/A |
| 설정 파일 | Git 기반 | 코드 변경 시 | Git 히스토리 | 5분 |

```bash
# PostgreSQL 백업 명령어
kubectl exec postgresql-0 -n tripgen-dev -- pg_dump -U postgres tripgen > dev-backup-$(date +%Y%m%d).sql

# 복원 명령어
kubectl exec -i postgresql-0 -n tripgen-dev -- psql -U postgres tripgen < backup-20250730.sql

# Redis 캐시 초기화 (복원 불필요)
kubectl exec redis-0 -n tripgen-dev -- redis-cli FLUSHALL
```

## 6. 메시징 아키텍처

### 6.1 Message Queue 구성

#### 6.1.1 Basic Tier 설정

**Azure Service Bus 전체 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 서비스 계층 | Basic | 비용 최적화 계층 |
| 네임스페이스 | sb-tripgen-dev | 개발환경 전용 네임스페이스 |
| 최대 큐 크기 | 1GB | 전체 큐 사이즈 제한 |
| 메시지 TTL | 14일 | 메시지 생존 기간 |
| 연결 문자열 | Kubernetes Secret 관리 | 보안 정보 분리 |

**큐별 상세 설정:**
| 큐 이름 | 최대 크기 | 중복 감지 | 메시지 크기 | 용도 |
|--------|----------|----------|------------|------|
| ai-schedule-generation | 256MB | Disabled | 64KB | AI 일정 생성 요청 |
| location-search | 128MB | Disabled | 32KB | 위치 검색 요청 |
| notification | 128MB | Disabled | 16KB | 알림 메시지 전송 |

#### 6.1.2 연결 설정

| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 인증 방식 | Connection String | 개발환경 단순 인증 |
| 연결 풀링 | Disabled | 개발환경 단순 구성 |
| 재시도 정책 | Exponential Backoff | 3회 재시도 |
| 타임아웃 | 30초 | 메시지 처리 타임아웃 |

## 7. 보안 아키텍처

### 7.1 개발환경 보안 정책

#### 7.1.1 기본 보안 설정

**보안 계층별 설정:**
| 계층 | 보안 기술 | 수준 | 설명 |
|------|-----------|------|----------|
| 네트워크 (L1) | Network Security Groups | 기본 | 기본 방화벽 규칙 |
| 애플리케이션 (L2) | JWT 인증 | 기본 | API Gateway 레벨 인증 |
| 데이터 (L3) | TLS 암호화 | 기본 | HTTPS 통신만 |
| 시크릿 (L4) | Kubernetes Secrets | 기본 | 기본 etcd 암호화 |

**관리 대상 시크릿:**
| 시크릿 이름 | 용도 | 순환 주기 | 저장 방식 |
|-------------|------|----------|----------|
| postgresql-secret | PostgreSQL 접근 | 수동 | K8s Secret |
| redis-secret | Redis 접근 | 수동 | K8s Secret |
| jwt-secret | JWT 토큰 서명 | 수동 | K8s Secret |
| servicebus-connection | Service Bus 연결 | 수동 | K8s Secret |
| openai-api-key | OpenAI API 접근 | 수동 | K8s Secret |

#### 7.1.2 시크릿 관리

**시크릿 관리 전략:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 관리 방식 | Kubernetes Secrets | 기본 K8s 시크릿 관리 |
| 암호화 방식 | etcd 기본 암호화 | 클러스터 기본 보안 |
| 순환 정책 | 수동 관리 | 개발환경 단순 관리 |
| 백업 전략 | 코드 저장소 외부 보관 | Git에 시크릿 제외 |

### 7.2 Network Policies

#### 7.2.1 기본 정책

**Network Policy 상세 설정:**
| 정책 이름 | 적용 대상 | 규칙 유형 | 허용 범위 |
|-----------|----------|-----------|----------|
| allow-ingress-to-apps | 애플리케이션 Pod | Ingress | API Gateway에서만 접근 |
| allow-apps-to-db | 데이터베이스 Pod | Ingress | 애플리케이션 Pod에서만 접근 |
| allow-apps-to-cache | Redis Pod | Ingress | 애플리케이션 Pod에서만 접근 |
| allow-external-api | 모든 Pod | Egress | 외부 API 호출 허용 |

```yaml
# Network Policy 예시
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-apps-to-db
  namespace: tripgen-dev
spec:
  podSelector:
    matchLabels:
      app: postgresql
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          tier: application
    ports:
    - protocol: TCP
      port: 5432
```

## 8. 모니터링 및 로깅

### 8.1 기본 모니터링

#### 8.1.1 Kubernetes 기본 모니터링

**모니터링 스택 구성:**
| 구성요소 | 상태 | 설명 |
|-----------|------|----------|
| Metrics Server | Enabled | Pod/Node 리소스 메트릭 수집 |
| Kubernetes Dashboard | Enabled | 웹 기반 클러스터 관리 |
| Azure Monitor | Basic | 기본 클러스터 모니터링 |

**기본 알림 설정:**
| 알림 유형 | 임계값 | 설명 |
|-----------|----------|----------|
| Pod Crash Loop | 5회 이상 | Pod 재시작 반복 감지 |
| Node Not Ready | 5분 이상 | 노드 비정상 상태 감지 |
| High Memory Usage | 90% 이상 | 메모리 사용량 과다 감지 |
| Disk Usage | 85% 이상 | 디스크 공간 부족 감지 |

#### 8.1.2 애플리케이션 모니터링

**헬스체크 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| Health Check Path | /actuator/health | Spring Boot Actuator 엔드포인트 |
| Readiness Probe | /actuator/health/readiness | 트래픽 수신 준비 상태 |
| Liveness Probe | /actuator/health/liveness | 애플리케이션 생존 상태 |
| 체크 주기 | 30초 | 상태 확인 간격 |
| 타임아웃 | 5초 | 응답 대기 시간 |

**수집 메트릭:**
| 메트릭 유형 | 도구 | 용도 |
|-----------|------|------|
| JVM Metrics | Micrometer | 자바 가상머신 성능 |
| HTTP Metrics | Spring Actuator | API 요청/응답 통계 |
| Database Metrics | HikariCP | DB 연결 풀 상태 |
| Custom Metrics | Micrometer | 비즈니스 로직 메트릭 |

### 8.2 로깅

#### 8.2.1 로그 수집

**로그 수집 설정:**
| 설정 항목 | 값 | 설명 |
|-----------|----|---------|
| 수집 방식 | kubectl logs | Kubernetes 기본 로그 명령어 |
| 저장 방식 | 노드 로컬 스토리지 | 컨테이너 런타임 로그 |
| 보존 기간 | 7일 | 개발환경 단기 보존 |
| 로그 로테이션 | 100MB/파일 | 디스크 공간 관리 |

**로그 레벨 설정:**
| 로거 유형 | 레벨 | 설명 |
|-----------|------|----------|
| Root Logger | INFO | 전체 시스템 기본 레벨 |
| Application Logger | DEBUG | 개발용 상세 로그 |
| Database Logger | WARN | 데이터베이스 주요 이벤트만 |
| Security Logger | INFO | 보안 관련 모든 이벤트 |

## 9. 배포 관련 컴포넌트

| 컴포넌트 유형 | 컴포넌트 | 설명 |
|--------------|----------|------|
| Container Registry | Azure Container Registry (Basic) | 개발용 이미지 저장소 (tripgendev.azurecr.io) |
| CI | GitHub Actions | 지속적 통합 파이프라인 |
| CD | ArgoCD | GitOps 패턴 지속적 배포 |
| 패키지 관리 | Helm | Kubernetes 패키지 관리 도구 |
| 환경별 설정 | values-dev.yaml | 개발환경 Helm 설정 파일 |
| 서비스 계정 | AKS Service Account | ArgoCD 접근 권한 관리 |
| 이미지 정책 | Always Pull | 최신 개발 이미지 사용 |

## 10. 비용 최적화

### 10.1 개발환경 비용 구조

#### 10.1.1 주요 비용 요소

| 구성요소 | 사양 | 월간 예상 비용 (USD) | 절약 방안 |
|----------|------|---------------------|-----------|
| AKS Control Plane | Managed | $0 | 무료 계층 |
| 노드 풀 | Standard_B2s × 2 | $120 | Spot Instance |
| Azure Disk | Standard 50GB | $5 | 표준 스토리지 |
| Load Balancer | Basic | $20 | 기본 계층 |
| Service Bus | Basic | $10 | 최소 구성 |
| Azure Monitor | Basic | $15 | 기본 모니터링 |
| **총합** | | **$170** | |

#### 10.1.2 비용 절약 전략

**컴퓨팅 비용 절약:**
| 절약 방안 | 절약률 | 설명 |
|-----------|----------|----------|
| Spot Instances | 60-70% | Azure Spot 인스턴스 활용 |
| 자동 종료 스케줄링 | 50% | 야간/주말 자동 종료 |
| 리소스 제한 최적화 | 20-30% | requests/limits 정밀 설정 |
| 노드 자동 확장 비활성화 | 10% | 고정 크기 클러스터 |

**스토리지 비용 절약:**
| 절약 방안 | 절약률 | 설명 |
|-----------|----------|----------|
| Standard Disk 사용 | 50% | Premium 대비 비용 절감 |
| 스토리지 크기 최적화 | 30% | 필요 최소 용량 할당 |
| 스냅샷 정책 최적화 | 40% | 불필요한 백업 제거 |

**네트워킹 비용 절약:**
| 절약 방안 | 절약률 | 설명 |
|-----------|----------|----------|
| Basic Load Balancer | 60% | Standard 대비 비용 절감 |
| 단일 Public IP | 80% | 최소 네트워크 자원 |
| 트래픽 최적화 | 20% | 불필요한 외부 호출 최소화 |

## 11. 개발환경 운영 가이드

### 11.1 일상 운영

#### 11.1.1 환경 시작/종료

```bash
# 개발환경 시작
kubectl scale deployment --replicas=1 --all -n tripgen-dev

# 데이터베이스 시작 (StatefulSet)
kubectl scale statefulset postgresql --replicas=1 -n tripgen-dev
kubectl scale statefulset redis --replicas=1 -n tripgen-dev

# 환경 상태 확인
kubectl get pods -n tripgen-dev

# 개발환경 종료 (야간/주말 비용 절약)
kubectl scale deployment --replicas=0 --all -n tripgen-dev
# 주의: 데이터베이스는 데이터 보존을 위해 종료하지 않음
```

#### 11.1.2 데이터 관리

```bash
# 테스트 데이터 초기화
kubectl apply -f k8s/data-init/ -n tripgen-dev

# 개발 데이터 백업
kubectl exec postgresql-0 -n tripgen-dev -- pg_dump -U postgres tripgen > backup-$(date +%Y%m%d).sql

# 데이터 복원
kubectl exec -i postgresql-0 -n tripgen-dev -- psql -U postgres tripgen < backup-20240131.sql

# Redis 캐시 초기화
kubectl exec redis-0 -n tripgen-dev -- redis-cli FLUSHALL

# 데이터베이스 상태 확인
kubectl exec postgresql-0 -n tripgen-dev -- psql -U postgres -c "\l"
```

### 11.2 트러블슈팅

#### 11.2.1 일반적인 문제 해결

| 문제 유형 | 원인 | 해결방안 | 예방법 |
|-----------|------|----------|----------|
| Pod Pending | 리소스 부족 | 노드 추가 또는 리소스 조정 | 리소스 모니터링 강화 |
| ImagePullBackOff | 이미지 접근 실패 | ACR 인증 확인, 이미지 태그 검증 | 이미지 정책 점검 |
| Database Connection Failed | PostgreSQL Pod 재시작 | Pod 상태 확인 및 재시작 | Health Check 강화 |
| Out of Memory | 메모리 한계 초과 | limits 조정 또는 불필요한 Pod 종료 | 메모리 사용량 모니터링 |
| Service Bus Connection Error | 연결 문자열 오류 | Secret 값 확인 및 업데이트 | 시크릿 관리 프로세스 정립 |

**일반적인 디버깅 명령어:**
```bash
# Pod 상태 확인
kubectl get pods -n tripgen-dev -o wide

# Pod 로그 확인
kubectl logs <pod-name> -n tripgen-dev --tail=50

# Pod 리소스 사용량 확인
kubectl top pods -n tripgen-dev

# 네트워크 정책 확인
kubectl get networkpolicy -n tripgen-dev

# 이벤트 확인
kubectl get events -n tripgen-dev --sort-by='.lastTimestamp'
```

## 12. 개발환경 특성 요약

**핵심 설계 원칙**: 빠른 개발 > 비용 효율성 > 단순성 > 안정성

**주요 제약사항**: 
- 95% 가용성 목표 (월 36시간 다운타임 허용)
- 제한적 확장성 (고정 리소스 풀)
- 기본 보안 수준 (복잡한 보안 구성 최소화)
- 수동 백업 및 복구 (자동화 최소화)

**최적화 목표**:
- 개발자 생산성 극대화
- 월간 클라우드 비용 $200 이하 유지
- 배포 시간 5분 이내
- 장애 복구 시간 30분 이내

이 개발환경은 **빠른 MVP 개발과 기능 검증**에 최적화되어 있으며, 비용 효율성과 개발 편의성을 우선으로 설계되었습니다. 운영환경으로의 전환 시에는 고가용성, 보안 강화, 자동화된 백업/복구 등이 추가로 필요합니다.