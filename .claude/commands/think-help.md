---
command: "/think-help"
category: "Planning & Analysis"
purpose: "Display planning workflow steps in terminal"
---

```bash
echo "
기획 작업 순서

1단계: 서비스 기획
/think-planning
- AI활용 서비스 기획 가이드를 참고하여 서비스를 기획합니다
- 서비스 목적, 대상 사용자, 핵심 기능을 정의합니다

2단계: 유저스토리 작성
/think-userstory  
- 유저스토리작성방법을 준용하여 작성합니다
- 마이크로서비스로 나누어 작성합니다
- Epic > Feature > User Story 형태로 구조화합니다

3단계: 요구사항 분석
/think-requirements
- 기능적 요구사항과 비기능적 요구사항을 분석합니다
- 제약사항과 가정사항을 명확히 정의합니다

4단계: 서비스 분할 계획
/think-microservice
- 마이크로서비스 경계를 정의합니다
- 각 서비스 간의 의존성과 통신 방식을 계획합니다

5단계: 기술 스택 선정
/think-techstack
- 프로젝트 요구사항에 맞는 기술 스택을 선정합니다
- 개발, 배포, 운영 환경을 고려합니다

📝 산출물: design/userstory.md
"
```