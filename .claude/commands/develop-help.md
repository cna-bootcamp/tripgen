---
command: "/develop-help"
category: "Development & Implementation"
purpose: "Display development workflow steps in terminal"
---

```bash
echo "
개발 작업 순서

1단계: 데이터베이스 설치계획서 작성
/develop-db-guide
- 데이터베이스설치계획서가이드를 참고하여 설치계획서를 작성합니다

2단계: 데이터베이스 설치 수행
/develop-db-install
- 데이터베이스설치가이드에 따라 실제 데이터베이스를 설치합니다

3단계: Message Queue 설치계획서 작성 (필요시)
/develop-mq-guide
- MQ설치게획서가이드에 따라 Message Queue 설치계획서를 작성합니다

4단계: Message Queue 설치 수행 (필요시)
/develop-mq-install
- MQ설치가이드에 따라 Message Queue를 설치합니다

5단계: 백엔드 개발
/develop-dev-backend
- 백엔드개발가이드에 따라 백엔드 서비스를 개발합니다

6단계: 백엔드 오류 해결
/develop-fix-backend
- 개발된 서비스와 common 모듈의 컴파일 에러를 해결합니다

7단계: 서비스 실행파일 작성
/develop-make-run-profile
- 서비스실행파일작성가이드에 따라 실행 환경을 구성합니다

8단계: 백엔드 테스트
/develop-test-backend
- 백엔드테스트가이드에 따라 개발된 백엔드를 테스트합니다

제거 명령어 (필요시):
/develop-db-remove - 데이터베이스 관련 리소스 삭제
/develop-mq-remove - Message Queue 관련 리소스 삭제

📝 주요 산출물:
- develop/database/plan/db-plan-{service-name}-dev.md (DB 설치 계획서)
- develop/database/plan/db-plan-{service-name}-prod.md (DB 설치 계획서)  
- develop/database/exec/db-exec-dev.md (DB 설치 결과서)
- develop/database/exec/db-exec-prod.md (DB 설치 결과서)
- develop/mq/mq-plan-dev.md (MQ 설치 계획서)
- develop/mq/mq-plan-prod.md (MQ 설치 계획서)
- develop/mq/mq-exec-dev.md (MQ 설치 결과서)
- develop/mq/mq-exec-prod.md (MQ 설치 결과서)
- develop/dev/dev-backend.md (백엔드 개발 결과서)
- develop/dev/test-backend.md (백엔드 테스트 결과서)
- 각 서비스별 소스 코드 및 테스트 코드

⚠️ 주의사항:
- PlantUML 문법 검사를 필수로 수행해야 합니다
- OpenAPI 문법 검사를 필수로 수행해야 합니다
- 개발주석표준과 패키지구조표준을 준수해야 합니다
- 테스트코드표준에 따라 테스트를 작성해야 합니다
"
```