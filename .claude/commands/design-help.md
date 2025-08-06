---
command: "/design-help"
category: "Architecture & Design"
purpose: "Display design workflow steps in terminal"
---

```bash
echo "
설계 작업 순서

1단계: UI/UX 설계
/design-uiux
- UI/UX설계가이드를 참고하여 사용자 인터페이스를 설계합니다
- 와이어프레임 및 스타일 가이드를 작성합니다

2단계: 프로토타입 작성
/design-prototype
- 프로토타입작성가이드를 참고하여 HTML 프로토타입을 작성합니다
- 사용자 경험을 검증할 수 있는 인터랙티브 프로토타입을 제작합니다

3단계: 아키텍처 패턴 선정
/design-pattern
- 아키텍처패턴선정가이드를 참고하여 적합한 패턴을 선정합니다
- 클라우드 아키텍처 패턴을 분석하고 선택합니다

4단계: High Level 아키텍처 정의
/design-highlevel
- HighLevel아키텍처정의가이드를 참고하여 전체 아키텍처를 정의합니다
- MSA 7대 컴포넌트를 고려한 상위 설계를 수행합니다

5단계: 논리 아키텍처 설계
/design-logical
- 논리아키텍처설계가이드를 참고하여 세부 아키텍처를 설계합니다
- 서비스 간의 논리적 관계와 데이터 흐름을 정의합니다

6단계: API 설계
/design-api
- API설계가이드를 참고하여 RESTful API를 설계합니다
- OpenAPI 3.0 명세서를 작성합니다

7단계: 시퀀스 설계 (외부)
/design-sequence-outer
- 외부시퀀스설계가이드를 참고하여 서비스 간 상호작용을 설계합니다
- PlantUML을 사용하여 시퀀스 다이어그램을 작성합니다

8단계: 시퀀스 설계 (내부)
/design-sequence-inner
- 내부시퀀스설계가이드를 참고하여 서비스 내부 흐름을 설계합니다
- 각 서비스별 내부 컴포넌트 간의 상호작용을 정의합니다

9단계: 클래스 설계
/design-class
- 클래스설계가이드를 참고하여 클래스 구조를 설계합니다
- 패키지 구조와 클래스 다이어그램을 작성합니다

10단계: 데이터 설계
/design-database
- 데이터설계가이드를 참고하여 데이터베이스를 설계합니다
- ERD 및 테이블 스키마를 정의합니다

11단계: 물리 아키텍처 설계
/design-physical
- 물리아키텍처설계가이드를 참고하여 배포 아키텍처를 설계합니다
- 인프라스트럭처 및 네트워크 구성을 정의합니다

📝 주요 산출물:
- design/uiux/uiux.md (UI/UX 설계서)
- design/uiux/prototype/*.html (프로토타입)
- design/pattern/architecture-pattern.md (아키텍처 패턴)
- design/high-level-architecture.md (High Level 아키텍처)
- design/backend/logical/* (논리 아키텍처)
- design/backend/api/* (API 설계서)
- design/backend/sequence/outer/*.puml (외부 시퀀스)
- design/backend/sequence/inner/*.puml (내부 시퀀스)
- design/backend/class/* (클래스 설계서)
- design/backend/database/* (데이터 설계서)
- design/backend/physical/* (물리 아키텍처)
"
```