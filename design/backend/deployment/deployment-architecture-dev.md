# 배포 아키텍처 설계서 - 개발환경

## 1. 개요

### 1.1 설계 목적
- AI 기반 여행 일정 생성 서비스의 **개발환경** 배포 아키텍처 설계
- CI/CD 파이프라인 구성 및 자동화 배포 전략
- 개발팀의 빠른 개발과 검증을 위한 배포 프로세스 최적화

### 1.2 설계 원칙
- **자동화 우선**: 수동 작업 최소화를 통한 개발 효율성 향상
- **빠른 피드백**: 코드 변경 후 배포까지 최단 시간 목표
- **안전한 배포**: 롤백 가능한 배포 전략 적용
- **개발 편의성**: 복잡한 설정 최소화, 직관적인 워크플로우

### 1.3 참조 아키텍처
- 물리아키텍처: design/backend/physical/physical-architecture-dev.md
- HighLevel아키텍처정의서: design/high-level-architecture.md
- 논리아키텍처: design/backend/logical/logical-architecture.md

## 2. CI/CD 및 배포

### 2.1 개발환경 CI/CD

#### 2.1.1 빌드 파이프라인

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

#### 2.1.2 배포 전략

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

### 2.2 개발 워크플로우

#### 2.2.1 일상 개발 프로세스
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

## 3. 배포 파이프라인 상세

### 3.1 GitHub Actions 워크플로우

#### 3.1.1 메이저 워크플로우 구성

**파일 구조:**
```
.github/workflows/
├── dev-deploy.yml              # 개발환경 배포
├── build-and-test.yml          # 빌드 및 테스트
├── docker-build.yml            # Docker 이미지 빌드
└── k8s-deploy.yml              # Kubernetes 배포
```

#### 3.1.2 개발환경 배포 워크플로우

**dev-deploy.yml:**
```yaml
name: Development Deploy

on:
  push:
    branches: [develop]
  pull_request:
    branches: [develop]

env:
  AZURE_CONTAINER_REGISTRY: acrtrpgendev
  CONTAINER_NAME: tripgen
  RESOURCE_GROUP: rg-tripgen-dev
  CLUSTER_NAME: aks-tripgen-dev
  NAMESPACE: tripgen-dev

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Setup Java 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        
    - name: Cache Maven dependencies
      uses: actions/cache@v4
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        
    - name: Run tests
      run: mvn clean test
      
    - name: Build application
      run: mvn clean package -DskipTests
      
    - name: Login to Azure Container Registry
      uses: azure/docker-login@v1
      with:
        login-server: ${{ env.AZURE_CONTAINER_REGISTRY }}.azurecr.io
        username: ${{ secrets.ACR_USERNAME }}
        password: ${{ secrets.ACR_PASSWORD }}
        
    - name: Build and push Docker images
      run: |
        # User Service
        docker build -t ${{ env.AZURE_CONTAINER_REGISTRY }}.azurecr.io/user-service:${{ github.sha }} ./user-service
        docker push ${{ env.AZURE_CONTAINER_REGISTRY }}.azurecr.io/user-service:${{ github.sha }}
        
        # Trip Service
        docker build -t ${{ env.AZURE_CONTAINER_REGISTRY }}.azurecr.io/trip-service:${{ github.sha }} ./trip-service
        docker push ${{ env.AZURE_CONTAINER_REGISTRY }}.azurecr.io/trip-service:${{ github.sha }}
        
        # AI Service
        docker build -t ${{ env.AZURE_CONTAINER_REGISTRY }}.azurecr.io/ai-service:${{ github.sha }} ./ai-service
        docker push ${{ env.AZURE_CONTAINER_REGISTRY }}.azurecr.io/ai-service:${{ github.sha }}
        
        # Location Service
        docker build -t ${{ env.AZURE_CONTAINER_REGISTRY }}.azurecr.io/location-service:${{ github.sha }} ./location-service
        docker push ${{ env.AZURE_CONTAINER_REGISTRY }}.azurecr.io/location-service:${{ github.sha }}
        
    - name: Setup kubectl
      uses: azure/setup-kubectl@v3
      
    - name: Login to Azure
      uses: azure/login@v1
      with:
        creds: ${{ secrets.AZURE_CREDENTIALS }}
        
    - name: Get AKS credentials
      run: |
        az aks get-credentials --resource-group ${{ env.RESOURCE_GROUP }} --name ${{ env.CLUSTER_NAME }}
        
    - name: Deploy to AKS
      run: |
        # Update image tags in deployment manifests
        sed -i 's|{{IMAGE_TAG}}|${{ github.sha }}|g' k8s/dev/*.yaml
        
        # Apply Kubernetes manifests
        kubectl apply -f k8s/dev/ -n ${{ env.NAMESPACE }}
        
        # Wait for rollout to complete
        kubectl rollout status deployment/user-service -n ${{ env.NAMESPACE }}
        kubectl rollout status deployment/trip-service -n ${{ env.NAMESPACE }}
        kubectl rollout status deployment/ai-service -n ${{ env.NAMESPACE }}
        kubectl rollout status deployment/location-service -n ${{ env.NAMESPACE }}
        
    - name: Verify deployment
      run: |
        kubectl get pods -n ${{ env.NAMESPACE }}
        kubectl get services -n ${{ env.NAMESPACE }}
```

### 3.2 Docker 이미지 빌드 전략

#### 3.2.1 멀티스테이지 빌드

**Dockerfile 예시 (Trip Service):**
```dockerfile
# Build stage
FROM openjdk:21-jdk-slim AS builder

WORKDIR /app
COPY pom.xml .
COPY src src

RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM openjdk:21-jre-slim

WORKDIR /app

# Add non-root user
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY --from=builder /app/target/trip-service-*.jar trip-service.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "trip-service.jar"]
```

#### 3.2.2 이미지 태그 전략

**태그 규칙:**
| 환경 | 태그 형식 | 예시 | 용도 |
|------|-----------|------|------|
| 개발 | `dev-{commit-sha}` | `dev-a1b2c3d` | 개발환경 배포 |
| 개발 | `dev-latest` | `dev-latest` | 최신 개발 버전 |
| PR | `pr-{pr-number}` | `pr-123` | Pull Request 검증 |

### 3.3 Kubernetes 배포 매니페스트

#### 3.3.1 배포 매니페스트 구조

```
k8s/dev/
├── namespace.yaml              # 네임스페이스 정의
├── configmap.yaml              # 설정 정보
├── secret.yaml                 # 민감 정보
├── services/
│   ├── user-service.yaml       # User Service 배포
│   ├── trip-service.yaml       # Trip Service 배포
│   ├── ai-service.yaml         # AI Service 배포
│   └── location-service.yaml   # Location Service 배포
├── backing-services/
│   ├── postgresql.yaml         # PostgreSQL 배포
│   └── redis.yaml              # Redis 배포
└── ingress.yaml                # Ingress 설정
```

#### 3.3.2 서비스별 Deployment 템플릿

**Trip Service Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: trip-service
  namespace: tripgen-dev
  labels:
    app: trip-service
    tier: application
spec:
  replicas: 1
  selector:
    matchLabels:
      app: trip-service
  template:
    metadata:
      labels:
        app: trip-service
        tier: application
    spec:
      containers:
      - name: trip-service
        image: acrtrpgendev.azurecr.io/trip-service:{{IMAGE_TAG}}
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "dev"
        - name: DATABASE_URL
          valueFrom:
            configMapKeyRef:
              name: app-config
              key: database.url
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: database.password
        resources:
          requests:
            cpu: 100m
            memory: 256Mi
          limits:
            cpu: 500m
            memory: 512Mi
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
      imagePullSecrets:
      - name: acr-secret
---
apiVersion: v1
kind: Service
metadata:
  name: trip-service
  namespace: tripgen-dev
spec:
  selector:
    app: trip-service
  ports:
  - protocol: TCP
    port: 8080
    targetPort: 8080
  type: ClusterIP
```

### 3.4 배포 자동화 스크립트

#### 3.4.1 환경 설정 스크립트

**setup-dev-env.sh:**
```bash
#!/bin/bash

set -e

echo "=== 개발환경 배포 설정 시작 ==="

# 환경 변수 설정
RESOURCE_GROUP="rg-tripgen-dev"
CLUSTER_NAME="aks-tripgen-dev"
NAMESPACE="tripgen-dev"
ACR_NAME="acrtrpgendev"

# Azure 로그인 확인
if ! az account show > /dev/null 2>&1; then
    echo "Azure에 로그인해주세요."
    az login
fi

# AKS 클러스터 인증 정보 가져오기
echo "AKS 클러스터 연결 중..."
az aks get-credentials --resource-group $RESOURCE_GROUP --name $CLUSTER_NAME

# 네임스페이스 생성
echo "네임스페이스 생성 중..."
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# ACR 시크릿 생성
echo "Container Registry 시크릿 생성 중..."
kubectl create secret docker-registry acr-secret \
  --docker-server=${ACR_NAME}.azurecr.io \
  --docker-username=$ACR_USERNAME \
  --docker-password=$ACR_PASSWORD \
  --namespace=$NAMESPACE \
  --dry-run=client -o yaml | kubectl apply -f -

# 기타 시크릿 생성
echo "애플리케이션 시크릿 생성 중..."
kubectl create secret generic app-secrets \
  --from-literal=database.password=$DB_PASSWORD \
  --from-literal=jwt.secret=$JWT_SECRET \
  --from-literal=openai.api.key=$OPENAI_API_KEY \
  --namespace=$NAMESPACE \
  --dry-run=client -o yaml | kubectl apply -f -

echo "=== 개발환경 설정 완료 ==="
```

#### 3.4.2 배포 검증 스크립트

**verify-deployment.sh:**
```bash
#!/bin/bash

set -e

NAMESPACE="tripgen-dev"

echo "=== 배포 상태 검증 시작 ==="

# Pod 상태 확인
echo "Pod 상태 확인 중..."
kubectl get pods -n $NAMESPACE

# 서비스 상태 확인
echo "서비스 상태 확인 중..."
kubectl get services -n $NAMESPACE

# Deployment rollout 상태 확인
SERVICES=("user-service" "trip-service" "ai-service" "location-service")

for service in "${SERVICES[@]}"; do
    echo "[$service] 배포 상태 확인 중..."
    
    # Rollout 상태 확인
    if kubectl rollout status deployment/$service -n $NAMESPACE --timeout=300s; then
        echo "[$service] 배포 성공"
    else
        echo "[$service] 배포 실패"
        kubectl describe deployment/$service -n $NAMESPACE
        exit 1
    fi
    
    # Health check
    echo "[$service] 헬스체크 수행 중..."
    POD_NAME=$(kubectl get pods -n $NAMESPACE -l app=$service -o jsonpath='{.items[0].metadata.name}')
    
    if kubectl exec $POD_NAME -n $NAMESPACE -- curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "[$service] 헬스체크 성공"
    else
        echo "[$service] 헬스체크 실패"
        kubectl logs $POD_NAME -n $NAMESPACE --tail=50
        exit 1
    fi
done

echo "=== 배포 검증 완료 ==="
```

### 3.5 롤백 전략

#### 3.5.1 자동 롤백 설정

**Deployment 롤백 설정:**
```yaml
spec:
  progressDeadlineSeconds: 600
  revisionHistoryLimit: 5
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 1
```

#### 3.5.2 수동 롤백 프로세스

**rollback.sh:**
```bash
#!/bin/bash

SERVICE_NAME=$1
NAMESPACE="tripgen-dev"

if [ -z "$SERVICE_NAME" ]; then
    echo "사용법: $0 <service-name>"
    echo "예시: $0 trip-service"
    exit 1
fi

echo "[$SERVICE_NAME] 롤백 시작..."

# 이전 버전으로 롤백
kubectl rollout undo deployment/$SERVICE_NAME -n $NAMESPACE

# 롤백 상태 확인
kubectl rollout status deployment/$SERVICE_NAME -n $NAMESPACE

echo "[$SERVICE_NAME] 롤백 완료"
```

## 4. 환경별 배포 설정

### 4.1 개발환경 특화 설정

#### 4.1.1 개발용 ConfigMap

**dev-config.yaml:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: tripgen-dev
data:
  # Database Configuration
  database.url: "jdbc:postgresql://postgresql.tripgen-dev.svc.cluster.local:5432/tripgen"
  database.driver: "org.postgresql.Driver"
  
  # Redis Configuration
  redis.host: "redis.tripgen-dev.svc.cluster.local"
  redis.port: "6379"
  
  # Logging Configuration
  logging.level.root: "INFO"
  logging.level.com.tripgen: "DEBUG"
  
  # Spring Configuration
  spring.profiles.active: "dev"
  spring.jpa.show-sql: "true"
  spring.jpa.hibernate.ddl-auto: "update"
  
  # AI Service Configuration
  openai.api.url: "https://api.openai.com/v1"
  openai.model: "gpt-4o-mini"
  
  # External Service URLs
  location.service.url: "http://location-service.tripgen-dev.svc.cluster.local:8080"
  ai.service.url: "http://ai-service.tripgen-dev.svc.cluster.local:8080"
```

#### 4.1.2 개발용 Ingress 설정

**dev-ingress.yaml:**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: tripgen-ingress
  namespace: tripgen-dev
  annotations:
    kubernetes.io/ingress.class: nginx
    nginx.ingress.kubernetes.io/rewrite-target: /$2
    nginx.ingress.kubernetes.io/cors-allow-origin: "*"
    nginx.ingress.kubernetes.io/cors-allow-methods: "GET, POST, PUT, DELETE, OPTIONS"
    nginx.ingress.kubernetes.io/cors-allow-headers: "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization"
spec:
  rules:
  - host: dev-tripgen.local
    http:
      paths:
      - path: /api/users(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 8080
      - path: /api/trips(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: trip-service
            port:
              number: 8080
      - path: /api/ai(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: ai-service
            port:
              number: 8080
      - path: /api/locations(/|$)(.*)
        pathType: Prefix
        backend:
          service:
            name: location-service
            port:
              number: 8080
```

## 5. 모니터링 및 로깅

### 5.1 배포 모니터링

#### 5.1.1 배포 상태 대시보드

**모니터링 항목:**
| 메트릭 | 임계값 | 알림 | 설명 |
|--------|--------|------|------|
| Pod Ready | < 100% | 즉시 | Pod 준비 상태 |
| Deployment Success | 실패 시 | 즉시 | 배포 성공/실패 |
| Health Check | 실패 시 | 즉시 | 애플리케이션 상태 |
| Response Time | > 5초 | 5분 후 | API 응답 시간 |

#### 5.1.2 자동 알림 설정

**Slack 알림 설정:**
```yaml
# .github/workflows/notify.yml
- name: Notify Slack on Success
  if: success()
  uses: 8398a7/action-slack@v3
  with:
    status: success
    channel: '#dev-deployments'
    text: '✅ 개발환경 배포 성공'
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}

- name: Notify Slack on Failure
  if: failure()
  uses: 8398a7/action-slack@v3
  with:
    status: failure
    channel: '#dev-deployments'
    text: '❌ 개발환경 배포 실패'
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK }}
```

## 6. 보안 및 시크릿 관리

### 6.1 시크릿 관리 전략

#### 6.1.1 GitHub Secrets 설정

**필수 Secrets:**
| Secret 이름 | 용도 | 예시 |
|-------------|------|------|
| `AZURE_CREDENTIALS` | Azure 서비스 주체 | JSON 형태 |
| `ACR_USERNAME` | Container Registry 사용자명 | acrtrpgendev |
| `ACR_PASSWORD` | Container Registry 암호 | 암호화된 값 |
| `DB_PASSWORD` | 데이터베이스 암호 | 암호화된 값 |
| `JWT_SECRET` | JWT 토큰 시크릿 | 암호화된 값 |
| `OPENAI_API_KEY` | OpenAI API 키 | 암호화된 값 |

#### 6.1.2 시크릿 순환 정책

**순환 전략:**
| 시크릿 유형 | 순환 주기 | 방법 | 책임자 |
|------------|----------|------|---------|
| Azure Credentials | 90일 | 자동 | DevOps |
| Database Password | 180일 | 수동 | Backend |
| JWT Secret | 30일 | 자동 | Backend |
| API Keys | 365일 | 수동 | AI Engineer |

## 7. 성능 최적화

### 7.1 빌드 최적화

#### 7.1.1 캐시 전략

**Maven 캐시:**
```yaml
- name: Cache Maven dependencies
  uses: actions/cache@v4
  with:
    path: ~/.m2
    key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
    restore-keys: |
      ${{ runner.os }}-m2-
```

**Docker 레이어 캐시:**
```yaml
- name: Setup Docker Buildx
  uses: docker/setup-buildx-action@v3
  with:
    driver-opts: |
      image=moby/buildkit:buildx-stable-1
      
- name: Build and push
  uses: docker/build-push-action@v5
  with:
    context: .
    push: true
    tags: ${{ env.ACR_NAME }}.azurecr.io/trip-service:${{ github.sha }}
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

### 7.2 배포 성능

#### 7.2.1 배포 시간 최적화

**병렬 빌드:**
```yaml
strategy:
  matrix:
    service: [user-service, trip-service, ai-service, location-service]
    
steps:
- name: Build ${{ matrix.service }}
  run: |
    docker build -t ${{ env.ACR_NAME }}.azurecr.io/${{ matrix.service }}:${{ github.sha }} ./${{ matrix.service }}
    docker push ${{ env.ACR_NAME }}.azurecr.io/${{ matrix.service }}:${{ github.sha }}
```

## 8. 트러블슈팅 가이드

### 8.1 일반적인 배포 이슈

#### 8.1.1 배포 실패 유형별 해결방안

| 실패 유형 | 원인 | 해결방안 | 예방법 |
|-----------|------|----------|--------|
| 이미지 Pull 실패 | ACR 인증 실패 | 시크릿 재생성 | 정기 시크릿 순환 |
| Health Check 실패 | 애플리케이션 시작 실패 | 로그 확인 후 수정 | 로컬 테스트 강화 |
| Resource 부족 | 메모리/CPU 한계 초과 | 리소스 한계 조정 | 리소스 모니터링 |
| ConfigMap 오류 | 설정 값 불일치 | 설정 값 확인 및 수정 | 설정 검증 자동화 |

#### 8.1.2 긴급 복구 절차

**배포 실패 시 긴급 조치:**
```bash
# 1. 즉시 이전 버전으로 롤백
kubectl rollout undo deployment/trip-service -n tripgen-dev

# 2. 상태 확인
kubectl get pods -n tripgen-dev
kubectl describe deployment/trip-service -n tripgen-dev

# 3. 로그 수집
kubectl logs -f deployment/trip-service -n tripgen-dev --tail=100

# 4. 원인 분석 후 수정
# 수정 후 재배포
```

## 9. 결론

### 9.1 개발환경 배포 아키텍처 핵심 가치

1. **자동화된 배포**: GitHub Actions를 통한 완전 자동화 CI/CD
2. **빠른 피드백**: 코드 변경 후 5분 내 배포 완료
3. **안전한 운영**: 롤백 가능한 배포 전략과 헬스체크
4. **개발 효율성**: 복잡한 수동 작업 제거

### 9.2 향후 개선 방향

- **GitOps 도입**: ArgoCD를 통한 선언적 배포 관리
- **고급 배포 전략**: Blue-Green, Canary 배포 적용
- **테스트 자동화**: E2E 테스트 자동화 확대
- **모니터링 강화**: APM 도구 도입 검토

이 배포 아키텍처는 **개발팀의 생산성 최대화**와 **안정적인 서비스 운영**을 동시에 달성하는 것을 목표로 설계되었습니다.