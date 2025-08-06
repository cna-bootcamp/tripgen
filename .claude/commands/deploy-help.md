---
command: "/deploy-help"
category: "Deployment & Operations"
purpose: "Display deployment workflow steps in terminal"
---

```bash
echo "
배포 작업 순서

1단계: 컨테이너화
/deploy-containerize
- Docker 이미지 생성 및 최적화를 수행합니다
- 각 마이크로서비스별 Dockerfile을 작성합니다
- 멀티스테이지 빌드를 통한 이미지 크기 최적화를 적용합니다

2단계: CI/CD 파이프라인 구성
/deploy-pipeline
- Jenkins, GitHub Actions 또는 GitLab CI를 사용하여 파이프라인을 구성합니다
- 자동화된 빌드, 테스트, 배포 프로세스를 설정합니다
- 코드 품질 검사 및 보안 스캔을 포함합니다

3단계: 쿠버네티스 매니페스트 작성
/deploy-k8s-manifest
- Deployment, Service, Ingress 등 K8s 리소스를 정의합니다
- ConfigMap과 Secret을 통한 설정 관리를 구성합니다
- HPA(Horizontal Pod Autoscaler) 및 리소스 제한을 설정합니다

4단계: 모니터링 및 로깅 설정
/deploy-monitoring
- Prometheus와 Grafana를 통한 메트릭 수집 및 대시보드를 구성합니다
- ELK 스택 또는 Loki를 통한 로그 수집 및 분석을 설정합니다
- 알림 및 경고 시스템을 구축합니다

5단계: 개발 환경 배포
/deploy-dev
- 개발 환경에 애플리케이션을 배포합니다
- 개발팀이 사용할 수 있는 환경을 구성합니다
- 개발 데이터베이스 및 외부 서비스 연동을 설정합니다

6단계: 스테이징 환경 배포
/deploy-staging
- 운영 환경과 동일한 구성의 스테이징 환경에 배포합니다
- 사용자 승인 테스트(UAT) 환경을 제공합니다
- 운영 데이터의 일부 또는 마스킹된 데이터를 사용합니다

7단계: 운영 환경 배포 준비
/deploy-prod-preparation
- Blue-Green 또는 Canary 배포 전략을 수립합니다
- 롤백 계획 및 데이터 백업 전략을 준비합니다
- 운영 환경 배포를 위한 체크리스트를 작성합니다

8단계: 운영 환경 배포
/deploy-production
- 무중단 배포를 통해 운영 환경에 애플리케이션을 배포합니다
- 배포 과정에서 실시간 모니터링을 수행합니다
- 헬스체크 및 스모크 테스트를 통해 배포 성공을 확인합니다

9단계: 배포 후 검증
/deploy-verification
- 모든 서비스의 정상 동작을 확인합니다
- 성능 및 리소스 사용률을 모니터링합니다
- 사용자 피드백 및 로그를 분석합니다

10단계: 운영 및 유지보수
/deploy-maintenance
- 정기적인 보안 패치 및 업데이트를 수행합니다
- 백업 및 복구 절차를 운영합니다
- 사용자 요청 및 이슈에 대응합니다

📝 주요 산출물:
- docker/Dockerfile (각 서비스별)
- k8s/*.yaml (쿠버네티스 매니페스트)
- .github/workflows/*.yml (CI/CD 파이프라인)
- monitoring/prometheus.yml (모니터링 설정)
- docs/deployment-guide.md (배포 가이드)
- docs/rollback-plan.md (롤백 계획서)

⚠️ 주의사항:
- 운영 환경 배포 전 반드시 스테이징에서 충분한 테스트 수행
- 민감한 정보는 Secret 또는 외부 키 관리 서비스 사용
- 모든 배포는 버전 태깅 및 변경 이력 관리 필수
- 장애 발생 시 즉시 롤백할 수 있는 체계 구축
"
```