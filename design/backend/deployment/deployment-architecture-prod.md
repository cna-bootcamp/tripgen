# 배포 아키텍처 설계서 - 운영환경

## 1. 개요

### 1.1 설계 목적
- AI 기반 여행 일정 생성 서비스의 **운영환경** 배포 아키텍처 설계
- 고가용성 및 확장성을 고려한 엔터프라이즈급 CI/CD 파이프라인 구성
- Blue-Green, Canary 배포를 통한 무중단 서비스 운영

### 1.2 설계 원칙
- **안정성 우선**: 운영 서비스 안정성 최우선 고려
- **무중단 배포**: Blue-Green, Canary 배포 전략 적용
- **자동화된 품질 관리**: 다단계 품질 게이트 적용
- **모니터링 기반**: 배포 전후 지속적 모니터링

### 1.3 참조 아키텍처
- 물리아키텍처: design/backend/physical/physical-architecture-prod.md
- HighLevel아키텍처정의서: design/high-level-architecture.md
- 논리아키텍처: design/backend/logical/logical-architecture.md
- 개발환경 배포아키텍처: design/backend/deployment/deployment-architecture-dev.md

## 2. CI/CD 및 배포 전략

### 2.1 운영환경 CI/CD

#### 2.1.1 배포 파이프라인

**파이프라인 트리거:**
```yaml
deployment_pipeline:
  trigger: tag creation (v*.*.*)
  
  stages:
    quality_gates:
      - unit_tests: 80% coverage required
      - integration_tests: all pass
      - security_scan: no critical vulnerabilities
      - performance_test: baseline comparison
      
    deployment_strategy:
      - canary: 10% traffic
      - monitoring: 15분 관찰
      - full_deployment: 90% → 100%
      - auto_rollback: error rate > 1%
```

**품질 게이트 상세:**
| 단계 | 임계값 | 필수 여부 | 실패 시 조치 |
|------|--------|----------|-------------|
| 단위 테스트 | 80% 이상 커버리지 | 필수 | 배포 중단 |
| 통합 테스트 | 100% 통과 | 필수 | 배포 중단 |
| 보안 스캔 | Critical 0개 | 필수 | 보안팀 검토 |
| 성능 테스트 | 기준선 대비 ±10% | 필수 | 성능팀 검토 |
| 코드 품질 | SonarQube A등급 | 권장 | 경고 알림 |

#### 2.1.2 Blue-Green 배포

**Blue-Green 배포 전략:**
```yaml
blue_green_deployment:
  strategy: Blue-Green with validation
  
  phases:
    preparation:
      - green_environment: 100% ready
      - database_migration: if required
      - configuration_sync: environment variables
      
    switch:
      - traffic_routing: Application Gateway
      - validation_tests: automated health checks
      - monitoring: 30분 관찰기간
      
    rollback:
      - trigger: error_rate > 0.5% OR response_time > 10초
      - action: immediate traffic switch
      - notification: teams + pagerduty
```

**환경별 배포 매트릭스:**
| 환경 | 배포 전략 | 승인 프로세스 | 모니터링 기간 | 자동 롤백 |
|------|-----------|---------------|---------------|------------|
| Staging | Rolling Update | 자동 | 10분 | 활성화 |
| Production | Blue-Green | 수동 승인 | 30분 | 활성화 |
| Canary | Canary (10%) | 자동 | 15분 | 활성화 |

### 2.2 GitOps 워크플로우

#### 2.2.1 ArgoCD 구성

**ArgoCD 설정:**
```yaml
argocd_configuration:
  namespace: argocd-system
  
  applications:
    - name: tripgen-prod
      repo: https://github.com/tripgen/k8s-manifests
      path: environments/production
      sync_policy: automated
      
  sync_windows:
    - kind: allow
      schedule: "0 2-4 * * 1-5"  # 월-금 02:00-04:00
      duration: 2h
      applications: ["tripgen-prod"]
```

**GitOps 워크플로우:**
1. **코드 변경** → Feature Branch
2. **PR 생성** → 자동 테스트 실행
3. **코드 리뷰** → 승인 후 Main Branch 머지
4. **태그 생성** → 운영 배포 파이프라인 트리거
5. **매니페스트 업데이트** → K8s Manifests Repository
6. **ArgoCD 동기화** → 자동 배포 실행

#### 2.2.2 매니페스트 관리 전략

**Repository 구조:**
```
k8s-manifests/
├── base/
│   ├── user-service/
│   ├── trip-service/
│   ├── ai-service/
│   └── location-service/
├── environments/
│   ├── staging/
│   │   ├── kustomization.yaml
│   │   └── patches/
│   └── production/
│       ├── kustomization.yaml
│       └── patches/
└── argocd/
    ├── applications/
    └── projects/
```

## 3. 배포 전략 상세

### 3.1 Canary 배포

#### 3.1.1 Canary 배포 설정

**Istio를 통한 트래픽 분할:**
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: trip-service-canary
  namespace: production
spec:
  hosts:
  - trip-service
  http:
  - match:
    - headers:
        canary:
          exact: "true"
    route:
    - destination:
        host: trip-service
        subset: canary
      weight: 100
  - route:
    - destination:
        host: trip-service
        subset: stable
      weight: 90
    - destination:
        host: trip-service
        subset: canary
      weight: 10
```

**Canary 단계별 트래픽 분할:**
| 단계 | Canary 트래픽 | Stable 트래픽 | 모니터링 기간 | 성공 조건 |
|------|---------------|---------------|---------------|-----------|
| 1단계 | 5% | 95% | 10분 | 에러율 < 0.1% |
| 2단계 | 25% | 75% | 15분 | 에러율 < 0.1% |
| 3단계 | 50% | 50% | 20분 | 에러율 < 0.1% |
| 4단계 | 100% | 0% | 30분 | 에러율 < 0.1% |

#### 3.1.2 자동화된 Canary 분석

**Flagger를 통한 자동 분석:**
```yaml
apiVersion: flagger.app/v1beta1
kind: Canary
metadata:
  name: trip-service
  namespace: production
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: trip-service
  progressDeadlineSeconds: 60
  service:
    port: 8080
    targetPort: 8080
  analysis:
    interval: 1m
    threshold: 5
    maxWeight: 50
    stepWeight: 10
    metrics:
    - name: request-success-rate
      thresholdRange:
        min: 99
      interval: 1m
    - name: request-duration
      thresholdRange:
        max: 500
      interval: 1m
```

### 3.2 Blue-Green 배포 상세

#### 3.2.1 환경 준비 단계

**Green 환경 구성:**
```yaml
# Green Environment Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: trip-service-green
  namespace: production
  labels:
    app: trip-service
    version: green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: trip-service
      version: green
  template:
    metadata:
      labels:
        app: trip-service
        version: green
    spec:
      containers:
      - name: trip-service
        image: acrtrpgenprod.azurecr.io/trip-service:v1.2.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
          limits:
            cpu: 2000m
            memory: 2Gi
```

#### 3.2.2 트래픽 전환 과정

**Application Gateway 트래픽 전환:**
```yaml
# 1단계: Green 환경 검증
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: trip-service-validation
  namespace: production
  annotations:
    kubernetes.io/ingress.class: azure/application-gateway
spec:
  rules:
  - host: validation.tripgen.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: trip-service-green
            port:
              number: 8080

# 2단계: 프로덕션 트래픽 전환
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: trip-service-prod
  namespace: production
spec:
  rules:
  - host: api.tripgen.com
    http:
      paths:
      - path: /api/trips
        pathType: Prefix
        backend:
          service:
            name: trip-service-green  # Blue에서 Green으로 전환
            port:
              number: 8080
```

### 3.3 데이터베이스 마이그레이션 전략

#### 3.3.1 무중단 스키마 변경

**호환 가능한 스키마 변경 순서:**
```sql
-- 1단계: 새 컬럼 추가 (NULL 허용)
ALTER TABLE trips ADD COLUMN ai_confidence_score DECIMAL(3,2);

-- 2단계: 애플리케이션 배포 (새 컬럼 사용 시작)
-- (Blue-Green 배포 진행)

-- 3단계: 기존 데이터 마이그레이션
UPDATE trips SET ai_confidence_score = 0.5 WHERE ai_confidence_score IS NULL;

-- 4단계: NOT NULL 제약조건 추가
ALTER TABLE trips ALTER COLUMN ai_confidence_score SET NOT NULL;
```

#### 3.3.2 복잡한 스키마 변경

**View를 활용한 단계적 마이그레이션:**
```sql
-- 1단계: 새 테이블 생성
CREATE TABLE trips_v2 (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    -- 새로운 스키마 구조
);

-- 2단계: 데이터 동기화 프로세스 시작
-- (ETL 프로세스를 통한 점진적 데이터 이관)

-- 3단계: View를 통한 호환성 유지
CREATE VIEW trips AS SELECT * FROM trips_v2;

-- 4단계: 애플리케이션 전환 후 구 테이블 제거
DROP TABLE trips_legacy;
```

## 4. 배포 자동화 스크립트

### 4.1 GitHub Actions 워크플로우

#### 4.1.1 운영환경 배포 워크플로우

**production-deploy.yml:**
```yaml
name: Production Deploy

on:
  push:
    tags:
      - 'v*.*.*'

env:
  AZURE_CONTAINER_REGISTRY: acrtrpgenprod
  RESOURCE_GROUP: rg-tripgen-prod
  CLUSTER_NAME: aks-tripgen-prod
  NAMESPACE: production

jobs:
  quality-gates:
    runs-on: ubuntu-latest
    outputs:
      should-deploy: ${{ steps.quality-check.outputs.result }}
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Setup Java 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        
    - name: Run comprehensive tests
      run: |
        mvn clean test -Dtest.profile=comprehensive
        mvn verify -Pintegration-tests
        
    - name: Security scan
      uses: securecodewarrior/github-action-add-sarif@v1
      with:
        sarif-file: security-scan-results.sarif
        
    - name: Performance baseline test
      run: |
        mvn clean test -Dtest.profile=performance
        
    - name: Quality gate check
      id: quality-check
      run: |
        # SonarQube 품질 게이트 확인
        if [ "$SONAR_QUALITY_GATE" == "OK" ]; then
          echo "result=true" >> $GITHUB_OUTPUT
        else
          echo "result=false" >> $GITHUB_OUTPUT
        fi

  build-and-push:
    needs: quality-gates
    if: needs.quality-gates.outputs.should-deploy == 'true'
    runs-on: ubuntu-latest
    
    strategy:
      matrix:
        service: [user-service, trip-service, ai-service, location-service]
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Login to Azure Container Registry
      uses: azure/docker-login@v1
      with:
        login-server: ${{ env.AZURE_CONTAINER_REGISTRY }}.azurecr.io
        username: ${{ secrets.ACR_USERNAME }}
        password: ${{ secrets.ACR_PASSWORD }}
        
    - name: Build and push Docker image
      run: |
        docker build -t ${{ env.AZURE_CONTAINER_REGISTRY }}.azurecr.io/${{ matrix.service }}:${{ github.ref_name }} ./${{ matrix.service }}
        docker push ${{ env.AZURE_CONTAINER_REGISTRY }}.azurecr.io/${{ matrix.service }}:${{ github.ref_name }}
        
        # 취약점 스캔
        docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
          -v ~/.cache:/root/.cache/ aquasec/trivy:latest image \
          --exit-code 1 --severity HIGH,CRITICAL \
          ${{ env.AZURE_CONTAINER_REGISTRY }}.azurecr.io/${{ matrix.service }}:${{ github.ref_name }}

  deploy-staging:
    needs: [quality-gates, build-and-push]
    runs-on: ubuntu-latest
    environment: staging
    
    steps:
    - name: Deploy to staging
      run: |
        # Staging 환경 배포
        helm upgrade --install tripgen-staging ./helm/tripgen \
          --namespace staging \
          --set image.tag=${{ github.ref_name }} \
          --set environment=staging
          
    - name: Run E2E tests
      run: |
        # E2E 테스트 실행
        npm run test:e2e -- --env=staging
        
    - name: Performance validation
      run: |
        # 성능 테스트 실행
        k6 run --env STAGE=staging performance-tests/load-test.js

  deploy-production:
    needs: [quality-gates, build-and-push, deploy-staging]
    runs-on: ubuntu-latest
    environment: production
    
    steps:
    - name: Manual approval gate
      uses: trstringer/manual-approval@v1
      with:
        secret: ${{ github.TOKEN }}
        approvers: devops-team,tech-leads
        minimum-approvals: 2
        
    - name: Blue-Green deployment
      run: |
        # Green 환경 준비
        kubectl apply -f k8s/production/green-deployment.yaml
        
        # Green 환경 헬스체크
        kubectl wait --for=condition=available --timeout=300s deployment/tripgen-green -n production
        
        # 트래픽 전환
        kubectl patch service tripgen-service -n production -p '{"spec":{"selector":{"version":"green"}}}'
        
        # 모니터링 기간
        sleep 1800  # 30분 대기
        
        # Blue 환경 정리
        kubectl delete deployment tripgen-blue -n production
```

#### 4.1.2 롤백 워크플로우

**rollback.yml:**
```yaml
name: Production Rollback

on:
  workflow_dispatch:
    inputs:
      target_version:
        description: 'Target version to rollback to'
        required: true
        type: string
      rollback_reason:
        description: 'Reason for rollback'
        required: true
        type: string

jobs:
  rollback:
    runs-on: ubuntu-latest
    environment: production
    
    steps:
    - name: Emergency approval
      uses: trstringer/manual-approval@v1
      with:
        secret: ${{ github.TOKEN }}
        approvers: devops-team,tech-leads
        minimum-approvals: 1
        
    - name: Execute rollback
      run: |
        # 즉시 트래픽 전환
        kubectl patch service tripgen-service -n production \
          -p '{"spec":{"selector":{"version":"${{ inputs.target_version }}"}}}'
          
        # 헬스체크 확인
        kubectl wait --for=condition=available --timeout=120s \
          deployment/tripgen-${{ inputs.target_version }} -n production
          
    - name: Post-rollback validation
      run: |
        # 롤백 후 검증
        curl -f https://api.tripgen.com/health
        
    - name: Notify teams
      uses: 8398a7/action-slack@v3
      with:
        status: custom
        custom_payload: |
          {
            "text": "🚨 Production Rollback Executed",
            "blocks": [
              {
                "type": "section",
                "text": {
                  "type": "mrkdwn",
                  "text": "*Production Rollback*\n• Target Version: ${{ inputs.target_version }}\n• Reason: ${{ inputs.rollback_reason }}\n• Executed by: ${{ github.actor }}"
                }
              }
            ]
          }
```

### 4.2 배포 검증 스크립트

#### 4.2.1 헬스체크 및 검증

**validate-deployment.sh:**
```bash
#!/bin/bash

set -e

NAMESPACE="production"
VERSION=$1
TIMEOUT=300

if [ -z "$VERSION" ]; then
    echo "Usage: $0 <version>"
    exit 1
fi

echo "=== 운영환경 배포 검증 시작 (Version: $VERSION) ==="

# 1. Deployment 상태 확인
SERVICES=("user-service" "trip-service" "ai-service" "location-service")

for service in "${SERVICES[@]}"; do
    echo "[$service] 배포 상태 확인 중..."
    
    if kubectl rollout status deployment/$service -n $NAMESPACE --timeout=${TIMEOUT}s; then
        echo "[$service] 배포 성공"
    else
        echo "[$service] 배포 실패 - 롤백 시작"
        kubectl rollout undo deployment/$service -n $NAMESPACE
        exit 1
    fi
done

# 2. 서비스 엔드포인트 검증
echo "API 엔드포인트 검증 중..."

ENDPOINTS=(
    "https://api.tripgen.com/api/users/health"
    "https://api.tripgen.com/api/trips/health"
    "https://api.tripgen.com/api/ai/health"
    "https://api.tripgen.com/api/locations/health"
)

for endpoint in "${ENDPOINTS[@]}"; do
    echo "검증 중: $endpoint"
    
    for i in {1..5}; do
        if curl -f -s "$endpoint" > /dev/null; then
            echo "✅ $endpoint 정상"
            break
        else
            if [ $i -eq 5 ]; then
                echo "❌ $endpoint 실패 - 롤백 필요"
                exit 1
            fi
            echo "재시도 중... ($i/5)"
            sleep 10
        fi
    done
done

# 3. 성능 검증
echo "성능 기준선 검증 중..."

# API 응답시간 체크
RESPONSE_TIME=$(curl -o /dev/null -s -w '%{time_total}' https://api.tripgen.com/api/trips)
THRESHOLD=2.0

if (( $(echo "$RESPONSE_TIME > $THRESHOLD" | bc -l) )); then
    echo "❌ 응답시간 임계값 초과: ${RESPONSE_TIME}s > ${THRESHOLD}s"
    exit 1
else
    echo "✅ 응답시간 정상: ${RESPONSE_TIME}s"
fi

# 4. 데이터베이스 연결 검증
echo "데이터베이스 연결 검증 중..."

kubectl exec -n $NAMESPACE deployment/trip-service -- \
    curl -f http://localhost:8080/actuator/health/db

echo "=== 배포 검증 성공 ==="
```

#### 4.2.2 성능 모니터링

**performance-monitor.sh:**
```bash
#!/bin/bash

NAMESPACE="production"
DURATION=1800  # 30분 모니터링

echo "=== 성능 모니터링 시작 (Duration: ${DURATION}s) ==="

START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION))

while [ $(date +%s) -lt $END_TIME ]; do
    # CPU/Memory 사용률 체크
    kubectl top pods -n $NAMESPACE | grep -E "(user-service|trip-service|ai-service|location-service)"
    
    # 에러율 체크 (Prometheus 메트릭 기준)
    ERROR_RATE=$(curl -s "http://prometheus.monitoring.svc.cluster.local:9090/api/v1/query?query=rate(http_requests_total{status=~\"5..\"}[5m])" | jq -r '.data.result[0].value[1]')
    
    if (( $(echo "$ERROR_RATE > 0.01" | bc -l) )); then
        echo "🚨 에러율 임계값 초과: $ERROR_RATE"
        
        # Slack 알림
        curl -X POST -H 'Content-type: application/json' \
            --data '{"text":"🚨 Production Error Rate Alert: '$ERROR_RATE'"}' \
            $SLACK_WEBHOOK_URL
            
        # 자동 롤백 트리거
        if (( $(echo "$ERROR_RATE > 0.05" | bc -l) )); then
            echo "긴급 롤백 실행..."
            kubectl rollout undo deployment/trip-service -n $NAMESPACE
            break
        fi
    fi
    
    sleep 60  # 1분 간격 체크
done

echo "=== 성능 모니터링 완료 ==="
```

## 5. 모니터링 및 알림

### 5.1 배포 모니터링 대시보드

#### 5.1.1 Grafana 대시보드 구성

**배포 상태 메트릭:**
| 메트릭 | 설명 | 임계값 | 알림 |
|--------|------|--------|------|
| deployment_success_rate | 배포 성공률 | < 95% | 즉시 |
| rollback_frequency | 롤백 빈도 | > 2회/일 | 즉시 |
| deployment_duration | 배포 소요시간 | > 30분 | 경고 |
| canary_error_rate | Canary 에러율 | > 0.5% | 즉시 |
| blue_green_switch_time | BG 전환 시간 | > 5분 | 경고 |

#### 5.1.2 실시간 배포 추적

**배포 파이프라인 시각화:**
```yaml
dashboard_config:
  title: "Production Deployment Pipeline"
  
  panels:
    - title: "Current Deployment Status"
      type: stat
      targets:
        - expr: deployment_pipeline_status
        
    - title: "Deployment History"
      type: table
      targets:
        - expr: deployment_history{environment="production"}
        
    - title: "Quality Gates Status"
      type: heatmap
      targets:
        - expr: quality_gate_results
        
    - title: "Traffic Distribution"
      type: pie
      targets:
        - expr: istio_request_total{destination_version=~".*"}
```

### 5.2 알림 및 에스컬레이션

#### 5.2.1 알림 매트릭스

| 심각도 | 알림 채널 | 응답 시간 | 에스컬레이션 |
|--------|-----------|----------|-------------|
| Critical | PagerDuty + SMS | 즉시 | 5분 후 팀 리더 |
| High | Slack + Email | 5분 | 15분 후 상급자 |
| Medium | Slack | 15분 | 1시간 후 관리자 |
| Low | Email | 1시간 | 24시간 후 주간 리포트 |

#### 5.2.2 자동 대응 시나리오

**배포 실패 자동 대응:**
```yaml
incident_response:
  deployment_failure:
    - action: immediate_rollback
      trigger: error_rate > 5%
      
    - action: notify_oncall
      channel: pagerduty
      
    - action: create_incident
      system: jira
      
  canary_failure:
    - action: stop_canary
      trigger: error_rate > 1%
      
    - action: preserve_logs
      retention: 30d
      
    - action: notify_team
      channel: slack
```

## 6. 보안 및 컴플라이언스

### 6.1 배포 보안 강화

#### 6.1.1 이미지 보안 스캔

**Trivy를 통한 취약점 스캔:**
```yaml
security_scanning:
  image_scan:
    tool: trivy
    severity_threshold: HIGH
    fail_on_critical: true
    
  compliance_check:
    - cis_kubernetes_benchmark
    - pci_dss_requirements  
    - gdpr_compliance
    
  secret_scanning:
    - github_secrets
    - vault_integration
    - rotation_policy: 90d
```

#### 6.1.2 배포 승인 프로세스

**다단계 승인 워크플로우:**
```yaml
approval_process:
  staging:
    required_approvers: 1
    approver_groups: ["developers"]
    
  production:
    required_approvers: 2
    approver_groups: ["tech-leads", "devops-team"]
    emergency_override: security-team
    
  hotfix:
    required_approvers: 1
    approver_groups: ["tech-leads"]
    auto_approve_conditions:
      - security_patch: true
      - severity: critical
```

### 6.2 컴플라이언스 자동화

#### 6.2.1 감사 로그 수집

**배포 관련 감사 추적:**
```yaml
audit_logging:
  events:
    - deployment_initiated
    - approval_granted
    - traffic_switched
    - rollback_executed
    
  retention: 7_years
  storage: azure_blob_immutable
  
  compliance_reports:
    - frequency: monthly
    - recipients: ["compliance-team", "audit-team"]
    - format: ["pdf", "csv"]
```

## 7. 재해복구 및 고가용성

### 7.1 재해복구 전략

#### 7.1.1 백업 및 복구 목표

**재해복구 메트릭:**
```yaml
disaster_recovery:
  rto: 30분  # Recovery Time Objective
  rpo: 15분  # Recovery Point Objective
  
  backup_strategy:
    primary_region: Korea Central
    dr_region: Korea South
    
    data_replication:
      postgresql: 지속적 복제
      redis: RDB + AOF 백업
      application_state: stateless (복구 불필요)
```

#### 7.1.2 자동 장애조치

**자동 페일오버 설정:**
```yaml
failover_configuration:
  database:
    postgresql:
      auto_failover: enabled
      failover_time: <60초
      
  cache:
    redis:
      geo_replication: enabled
      manual_failover: 관리자 승인 필요
      
  application:
    multi_region_deployment: Phase 3에서 구현
    traffic_manager: Azure Traffic Manager
```

### 7.2 고가용성 배포

#### 7.2.1 다중 리전 배포 준비

**다중 리전 아키텍처:**
```yaml
multi_region_setup:
  primary_region: Korea Central
  secondary_region: Korea South
  
  deployment_strategy:
    active_passive: Phase 2
    active_active: Phase 3
    
  data_synchronization:
    postgresql: streaming_replication
    redis: geo_replication
    static_assets: cdn_replication
```

## 8. 성능 최적화 및 모니터링

### 8.1 배포 성능 최적화

#### 8.1.1 이미지 최적화

**멀티스테이지 빌드 최적화:**
```dockerfile
# Optimized Production Dockerfile
FROM maven:3.9-openjdk-21-slim AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src src
RUN mvn clean package -DskipTests -B

FROM gcr.io/distroless/java21-debian12:nonroot
COPY --from=builder /app/target/trip-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### 8.1.2 배포 병렬화

**병렬 배포 전략:**
```yaml
parallel_deployment:
  strategy: service-level-parallelism
  
  dependency_groups:
    group_1: [database-migrations]
    group_2: [user-service, location-service]  # 독립적 서비스
    group_3: [trip-service]  # 의존성 있는 서비스
    group_4: [ai-service]    # 높은 리소스 요구
    
  resource_allocation:
    max_concurrent_deployments: 2
    cpu_limit_per_deployment: 4000m
    memory_limit_per_deployment: 8Gi
```

### 8.2 배포 메트릭 수집

#### 8.2.1 핵심 배포 메트릭

**배포 KPI 대시보드:**
| 메트릭 | 목표값 | 현재값 | 트렌드 |
|--------|--------|--------|--------|
| 배포 빈도 | 1회/주 | 0.8회/주 | ↗️ |
| 배포 성공률 | 98% | 96% | ↗️ |
| 평균 배포 시간 | 20분 | 25분 | ↘️ |
| 롤백률 | <5% | 4% | ↘️ |
| MTTR | 15분 | 18분 | ↘️ |

## 9. 트러블슈팅 가이드

### 9.1 일반적인 배포 이슈

#### 9.1.1 배포 실패 패턴별 해결방안

| 실패 패턴 | 원인 | 해결방안 | 예방책 |
|-----------|------|----------|--------|
| 품질 게이트 실패 | 테스트 커버리지 부족 | 테스트 보강 후 재배포 | 품질 메트릭 모니터링 |
| 이미지 취약점 | 보안 스캔 실패 | 베이스 이미지 업데이트 | 정기 보안 스캔 |
| 리소스 부족 | 노드 용량 초과 | 노드 스케일 아웃 | 리소스 모니터링 강화 |
| 네트워크 정책 위반 | 보안 정책 충돌 | 네트워크 정책 수정 | 배포 전 정책 검증 |
| 데이터베이스 마이그레이션 실패 | 스키마 충돌 | 수동 롤백 및 수정 | 마이그레이션 사전 검증 |

#### 9.1.2 긴급 대응 절차

**Production 장애 발생 시:**
```bash
# 1. 즉시 상황 파악
kubectl get pods -n production
kubectl get events -n production --sort-by='.lastTimestamp'

# 2. 트래픽 분석
kubectl logs -f deployment/trip-service -n production --tail=100
curl -s https://api.tripgen.com/health | jq

# 3. 긴급 롤백 (필요시)
kubectl rollout undo deployment/trip-service -n production
kubectl rollout status deployment/trip-service -n production

# 4. 상황 안정화 확인
watch -n 5 'kubectl get pods -n production'

# 5. 사후 분석을 위한 로그 수집
kubectl logs deployment/trip-service -n production --previous > rollback-$(date +%Y%m%d-%H%M%S).log
```

### 9.2 성능 이슈 대응

#### 9.2.1 배포 후 성능 저하

**성능 저하 진단 체크리스트:**
```bash
# 1. 리소스 사용량 확인
kubectl top pods -n production
kubectl describe nodes

# 2. 애플리케이션 메트릭 확인
curl -s https://api.tripgen.com/actuator/metrics/jvm.memory.used
curl -s https://api.tripgen.com/actuator/metrics/http.server.requests

# 3. 데이터베이스 성능 확인
kubectl exec -n production deployment/trip-service -- \
  curl -s http://localhost:8080/actuator/metrics/hikaricp.connections

# 4. 네트워크 지연 확인
kubectl exec -n production deployment/trip-service -- \
  ping -c 5 postgresql.production.svc.cluster.local
```

## 10. 결론

### 10.1 운영환경 배포 아키텍처 핵심 가치

**핵심 설계 원칙**: 안정성 > 무중단 서비스 > 자동화 > 모니터링  
**주요 특징**: Blue-Green 배포, Canary 분석, GitOps 워크플로우, 자동 롤백

### 10.2 향후 발전 방향

- **멀티 클러스터**: 다중 리전 Active-Active 구성
- **AI 기반 이상 탐지**: 배포 후 자동 이상 탐지 및 대응
- **Progressive Delivery**: 더욱 정교한 트래픽 제어
- **Chaos Engineering**: 장애 내성 테스트 자동화

이 배포 아키텍처는 **엔터프라이즈급 안정성**과 **지속적 서비스 개선**을 동시에 달성하는 것을 목표로 설계되었습니다.