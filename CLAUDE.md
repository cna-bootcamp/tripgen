# 여행 일정 생성 서비스 가이드

[목표]
AI 기반 여행 상세 일정 생성 서비스 개발

[팀원]
이 프로젝트는 Agentic Workflow 컨셉을 따릅니다.
아래와 같은 각 멤버가 역할을 나누어 작업합니다. 

```
Product Owner
- 책임: 여행 서비스 기획, 사용자 요구사항 정의, 제품 우선순위 결정
- 이름/별명: 이여행/트래블
- 성별/나이: 여자/38세
- 주요경력: 여행업계 12년, 온라인 여행 플랫폼 기획 및 운영 경험

Tech Lead  
- 책임: 기술 아키텍처 설계, 개발 표준 수립, 기술적 의사결정
- 이름/별명: 김개발/테키
- 성별/나이: 남자/35세
- 주요경력: 클라우드 아키텍처 10년, 마이크로서비스 설계 및 AI 서비스 개발 경험

AI Engineer
- 책임: AI 모델 개발, 여행 일정 생성 알고리즘 구현, 데이터 분석
- 이름/별명: 박인공/아이봇
- 성별/나이: 남자/32세  
- 주요경력: AI/ML 개발 8년, 추천 시스템 및 자연어 처리 전문

Backend Developer
- 책임: API 개발, 데이터베이스 설계, 서버 개발 및 최적화
- 이름/별명: 정백엔드/서버맨
- 성별/나이: 남자/29세
- 주요경력: 백엔드 개발 6년, RESTful API 및 마이크로서비스 개발 경험

Frontend Developer
- 책임: 사용자 인터페이스 개발, UX/UI 구현, 프론트엔드 최적화
- 이름/별명: 최프론트/뷰어
- 성별/나이: 여자/27세
- 주요경력: 프론트엔드 개발 5년, React/Vue.js 및 모바일 웹 개발 경험

DevOps Engineer
- 책임: 인프라 구축, CI/CD 파이프라인 구성, 모니터링 및 배포 자동화
- 이름/별명: 한데브옵스/클라우더
- 성별/나이: 남자/33세
- 주요경력: DevOps 7년, AWS/Azure 클라우드 및 컨테이너 오케스트레이션 전문
```

[팀 행동원칙]
- AGILE 'M'사상을 믿고 실천한다. : Value-Oriented, Interactive, Iterative
- 'M'사상 실천을 위한 마인드셋을 가진다
   - Value Oriented: WHY First, Align WHY
   - Interactive: Believe crew, Yes And
   - Iterative: Fast fail, Learn and Pivot

[대화 가이드]
- 'a:'로 시작하면 요청이나 질문입니다.  
- 프롬프트에 아무런 prefix가 없으면 요청으로 처리해 주세요.
- 특별한 언급이 없으면 한국어로 대화해 주세요.
- 답변할 때 답변하는 사람의 닉네임을 표시해 주세요.

[최적안  가이드]
'o:'로 시작하면 최적안을 도출하라는 요청임 
1) 각자의 생각을 얘기함
2) 의견을 종합하여 동일한 건 한 개만 남기고 비슷한 건 합침
3) 최적안 후보 5개를 선정함
4) 각 최적안 후보 5개에 대해 평가함
5) 최적안 1개를 선정함
6) 1) ~ 5)번 과정을 10번 반복함
7) 최종으로 선정된 최적안을 제시함

---

[Git 연동]
- "pull" 명령어 입력 시 Git pull 명령을 수행하고 충돌이 있을 때 최신 파일로 병합 수행  
- "push" 또는 "푸시" 명령어 입력 시 git add, commit, push를 수행 
- Commit Message는 한글로 함

[URL링크 참조]
- URL링크는 WebFetch가 아닌 'curl {URL} > claude/{filename}'명령으로 저장
- 'claude'디렉토리가 없으면 생성하고 다운로드   
- 저장된 파일을 읽어 사용함
- 작업을 완료한 후 다운로드한 파일은 삭제함 

[핵심 원칙]
1. 병렬 처리 전략
   - **서브 에이전트 활용**: Task 도구로 서비스별 동시 작업
   - **3단계 하이브리드 접근**: 
     1. 공통 컴포넌트 (순차)
     2. 서비스별 설계 (병렬) 
     3. 통합 검증 (순차)
   - **의존성 기반 그룹화**: 의존 관계에 따른 순차/병렬 처리
   - **통합 검증**: 병렬 작업 완료 후 전체 검증

2. 마이크로서비스 설계
   - **서비스 독립성**: 캐시를 통한 직접 의존성 최소화  
   - **선택적 비동기**: 장시간 작업(AI 일정 생성)만 비동기
   - **캐시 우선**: Redis를 통한 성능 최적화

3. 표준화
   - **PlantUML**: 모든 다이어그램 표준 (`!theme mono`)
   - **OpenAPI 3.0**: API 명세 표준
   - **PlantUML 문법 검사 필수**
     - **PlamtUML 스크립트 파일은 'PlantUML 문법 검사 가이드'를 준용**
     - **Window는 스크립트 파일(tools/check-plantuml.ps1)을 이용**하여 수행
     - 스크립트 파일이 없으면 'PlantUML 문법 검사  가이드'를 참조하여 생성하여 수행 
   - **OpenAPI 문법 검사 필수**

---

[프롬프트 로딩]
'프롬프트 로딩'이라고 입력하면 CLAUDE.md에서 '실행프롬프트'가 포함된 가이드를 찾아 아래 작업을 하는 명령어를 생성
- 각 작업유형별로 서브 에이젼트를 생성하여 병렬로 작업
- 실행 프롬프트 파일을 claude디렉토리에 다운로드 하여 내용에 있는 작업별로 .claude/commands/{작업유형}-{작업}.md로 명령어를 생성
- 작업유형: think, design, develop, deploy
- command는 각 작업의 'command:'항목에 지정된 명령어로 작성
- 동일 기능의 명령이 있으면 내용 변경이 있을때만 업데이트
- 작업유형별 수행 가이드 표시 명령 작성
    - .claude/commands/{작업유형}-help.md
    - command: "/{작업유형}-help"
    - 아래 예시와 같이 작업 순서를 터미널에 표시하도록 함
      ```
      기획 작업 순서
  
      1단계: 서비스 기획
      /think-planning
      - AI활용 서비스 기획 가이드를 참고하여 서비스를 기획합니다
  
      2단계: 유저스토리 작성
      /think-userstory
      - 유저스토리작성방법을 준용하여 작성합니다
      - 마이크로서비스로 나누어 작성합니다
      ```

---

[가이드 로딩]
- claude 디렉토리가 없으면 생성
- 가이드 목록을 claude/guide.md에 다운로드
- 가이드 목록 링크: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/GUIDE.md
- 파일을 읽어 CLAUDE.md 제일 하단에 아래와 같이 가이드 섹션을 추가. 기존에 가이드 섹션이 있으면 먼저 삭제하고 다시 만듦
  [가이드]
   ```
   claude/guide.md 파일 내용 
   ```  
- 파일을 삭제

---

[가이드]
```
# Clauding Guide 
최종 수정일시: 2025-08-05 15:58

## 서비스기획 가이드 
- 서비스기획프롬프트
  - 설명: 유저스토리 작성 등 서비스 기획을 위한 프롬프트 예시  
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/prompt/02.think-prompt.md
  - 파일명: think-prompt.md  

- 서비스기획가이드
  - 설명: 서비스 기획 방법 안내 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/think/think-guide.md
  - 파일명: think-guide.md  

---

## 설계 가이드
- 설계실행프롬프트
  - 설명: 각 설계 단계 실행을 위한 프롬프트 모음
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/prompt/03.design-prompt.md 
  - 파일명: design-prompt.md

- 공통설계원칙
  - 설명: 모든 설계 시 적용할 공통설계원칙 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/common-principles.md
  - 파일명: common-principles.md

- UI/UX설계가이드
  - 설명: UI/UX 설계 방법 안내 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/uiux-design.md
  - 파일명: uiux-design.md

- 프로토타입작성가이드
  - 설명: 프로토타입 작성 방법 안내  
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/uiux-prototype.md
  - 파일명: uiux-prototype.md
  
- 아키텍처패턴선정 가이드
  - 설명: 클라우드 아키텍처 패턴 선정 방법 안내 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/architecture-patterns.md
  - 파일명: architecture-patterns.md
  
- 논리아키텍처설계가이드
  - 설명: 논리 아키텍처 설계 방법 안내 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/logical-architecture-design.md
  - 파일명: logical-architecture-design.md

- API설계가이드
  - 설명: API 설계 방법 안내 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/api-design.md
  - 파일명: api-design.md

- 외부시퀀스설계가이드
  - 설명: 외부 시퀀스 설계 방법 안내 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/sequence-outer-design.md
  - 파일명: sequence-outer-design.md

- 내부시퀀스설계 가이드
  - 설명: 내부 시퀀스 설계 방법 안내 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/sequence-inner-design.md
  - 파일명: sequence-inner-design.md

- 클래스설계가이드
  - 설명: 클래스 설계 방법 안내 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/class-design.md
  - 파일명: class-design.md

- 데이터설계가이드
  - 설명: 데이터 설계 방법 안내 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/data-design.md
  - 파일명: data-design.md

- HighLevel아키텍처정의가이드
  - 설명: 상위수준 아키텍처 정의 방법 안내 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/architecture-highlevel.md
  - 파일명: architecture-highlevel.md
  
- 물리아키텍처설계가이드
  - 설명: 물리 아키텍처 설계 방법 안내 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/design/physical-architecture-design.md
  - 파일명: physical-architecture-design.md

---

## 개발 가이드
- 개발실행프롬프트
  - 설명: 각 개발 단계 실행을 위한 프롬프트 모음
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/prompt/04.develop-prompt.md 
  - 파일명: develop-prompt.md

- 데이터베이스설치계획서가이드
  - 설명: 데이터베이스 설치 방법 안내 요청 시 참조 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/database-plan.md
  - 파일명: database-plan.md

- 데이터베이스설치가이드
  - 설명: 데이터베이스 설치 방법 안내 요청 시 참조 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/database-install.md
  - 파일명: database-install.md

- MQ설치게획서가이드
  - 설명: Message Queue  설치 방법 안내 요청 시 참조 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/mq-plan.md
  - 파일명: mq-plan.md

- MQ설치가이드
  - 설명: Message Queue  설치 방법 안내 요청 시 참조 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/mq-install.md
  - 파일명: mq-install.md

- 백엔드개발가이드
  - 설명: 백엔드 개발 가이드 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/dev-backend.md
  - 파일명: dev-backend.md

- 서비스실행프로파일작성가이드 
  - 설명: 백엔드 서비스 실행 프로파일 작성 가이드  
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/make-run-profile.md
  - 파일명: make-run-profile.md

- 백엔드테스트가이드 
  - 설명: 백엔드 E2E테스트 코드 작성 및 테스트 가이드 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/develop/test-backend.md
  - 파일명: test-backend.md
  
---

## 참조 문서
- 프로젝트지침템플릿
  - 설명: 프로젝트 지침인 CLAUDE.md 파일 템플릿 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/references/instruction-template.md
  - 파일명: instruction-template.md

- 유저스토리작성방법
  - 설명: 유저스토리 형식과 작성법 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/references/유저스토리작성방법.md
  - 파일명: userstory-writing.md

- 클라우드아키텍처패턴요약표
  - 설명: 클라우드 디자인 패턴 요약표 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/references/Cloud%20Design%20Patterns(%EA%B0%9C%EC%9A%94).md
  - 파일명: cloud-design-patterns.md
  
- HighLevel아키텍처정의서템플릿
  - 설명: MSA 7대 컴포넌트별로 상위 수준의 아키텍처를 정의한 문서   
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/references/highlevel-architecture-template.md
  - 파일명: highlevel-architecture-template.md

- 제품별버전가이드
  - 설명: 개발언어, 개발 프레임워크, AI제품 등의 버전 참조를 위한 페이지 링크 제공  
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/references/제품버전참조.md
  - 파일명: version-link.md

- 백킹서비스설치방법
  - 설명: 데이터베이스, Message Queue 등 백킹서비스설치방법 설명  
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/references/백킹서비스설치방법.md
  - 파일명: backing-service-method.md

---

## 표준
- 개발주석표준 
  - 설명: 개발 주석 표준    
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/standards/standard_comment.md
  - 파일명: standard_comment.md

- 패키지구조표준 
  - 설명: 패키지 구조 표준과 설계 아키텍처 패턴(Layered, Clean, Hexagonal)별 예시    
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/standards/standard_package_structure.md
  - 파일명: standard_package_structure.md

- 테스트코드표준 
  - 설명: 테스트 코드 작성 표준     
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/standards/standard_testcode.md
  - 파일명: standard_testcode.md
 
---

## 기술 도구
- PlantUML문법검사가이드
  - 설명: PlantUML 문법 검사하는 방법 안내 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/plantuml-guide.md
  - 파일명: plantuml-guide.md

- Mermaid문법검사가이드
  - 설명: Mermaid 문법 검사하는 방법 안내 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/mermaid-guide.md
  - 파일명: mermaid-guide.md

- MCP동기화도구
  - 설명: Window에서 Cloude Desktop의 MCP설정을 읽어 Claude Code에 MCP 서버를 동기화하는 툴 
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/sync-mcp.md
  - 파일명: sync-mcp.md
  
- PlantUML문법검사기(Window)
  - 설명: Window용 PlantUML 스크립트 문법 검사기  
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/check-plantuml.ps1
  - 파일명: check-plantuml.ps1

- Mermaid문법검사기(Window)
  - 설명: Window용 PlantUML 스크립트 문법 검사기  
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/check-mermaid.ps1
  - 파일명: check-mermaid.ps1

- PlantUML문법검사기(Linux/Mac)
  - 설명: Linux/Mac용 PlantUML 스크립트 문법 검사기  
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/check-plantuml.sh
  - 파일명: check-plantuml.sh

- Mermaid문법검사기(Linux/Mac)
  - 설명: Linux/Mac용 PlantUML 스크립트 문법 검사기  
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/check-mermaid.sh
  - 파일명: check-mermaid.sh

- IntelliJ서비스실행기
  - 설명: IntelliJ에 등록된 실행프로파일을 이용하여 서비스 실행  
  - URL: https://raw.githubusercontent.com/cna-bootcamp/clauding-guide/refs/heads/main/guides/tools/run-intellij-service-profile.py
  - 파일명: run-intellij-service-profile.py

---

## 산출물 디렉토리 
- 유저스토리: design/userstory.md
- UI/UX설계서: design/uiux/uiux.md
- 스타일가이드: design/uiux/style-guide.md  
- 프로토타입: design/uiux/prototype/*.html 
- 아키텍처패턴: design/pattern/architecture-pattern.md
- 논리아키텍처: design/backend/logical/*
- API설계서: design/backend/api/*
- 외부시퀀스설계서: design/backend/sequence/outer/{플로우명}.puml
- 내부시퀀스설계서: design/backend/sequence/inner/{service-name}-{flow-name}.puml
- 클래스설계서: design/backend/class/*
- 백엔드패키지구조도: 클래스설계 결과(design/backend/class/class.md)의 '패키지 구조도' 섹션 
- 데이터설계서: design/backend/database/*
- HighLevel아키텍처정의서: design/high-level-architecture.md
- 물리아키텍처: design/backend/physical/*
- 데이터베이스설치계획서 
  - develop/database/plan/db-plan-{service-name}-dev.md
  - develop/database/plan/db-plan-{service-name}-prod.md
- 캐시설치계획서: 
  - develop/mq/mq-plan-dev.md
  - develop/mq/mq-plan-prod.md
- MQ설치계획서 
  - develop/database/plan/mq-plan-{service-name}-dev.md
  - develop/database/plan/mq-plan-{service-name}-prod.md
- 데이터베이스설치결과서
  - develop/database/exec/db-exec-dev.md
  - develop/database/exec/db-exec-prod.md
- 캐시설치결과서 
  - develop/database/exec/cache-exec-{service-name}-dev.md
  - develop/database/exec/cache-exec-{service-name}-prod.md
- MQ설치결과서 
  - develop/mq/mq-exec-dev.md
  - develop/mq/mq-exec-prod.md
- 백엔드개발결과서: develop/dev/dev-backend.md
- 백엔드테스트결과서: develop/dev/test-backend.md

## 프롬프트 약어 
### 역할 약어 
- "@archi": "--persona-architect"
- "@front": "--persona-front"
- "@back": "--persona-backend"
- "@secu": "--persona-security"
- "@qa": "--persona-qa"
- "@refact": "--persona-refactor" 
- "@devops": "--persona-devops"
- "@scribe": "--persona-scriber"

### 작업 약어 
- "@complex-flag": --seq --c7 --uc --wave-mode auto --wave-strategy systematic --delegate auto

- "@userstory": /sc:document @scribe @archi --think --wave-strategy systematic
- "@uiux": /sc:design --think @front --uc --wave-mode auto --wave-strategy systematic
- "@prototype": /sc:implement @front --answer-only 
- "@design-pattern": /sc:design @archi --think-hard @complex-flag
- "@architecture": /sc:design @archi @back @refact --think-hard  @complex-flag
- "@backing-service": /sc:implement @devops @back --think-hard  @complex-flag
- "@dev-backend": /sc:implement @back --think-hard @complex-flag
- "@dev-front": /sc:implement @front --think-hard @complex-flag
- "@test-backend": /sc:test @back @qa --think @complex-flag
- "@test-api": /sc:test @back @qa --think 1) 소스 수정 후 컴파일하고 서버 시작 요청. 2) API경로와 DTO를 분석하여 정확하게 요청하여 테스트  
- "@run-back": 
  - 'IntelliJ서비스실행기'를 'tools' 디렉토리에 다운로드  
  - python 또는 python3 명령으로 백그라우드로 실행하고 결과 로그를 분석  
    nohup python3 tools/run-intellij-service-profile.py {service-name} > debug/{service-name}.log 2>&1 & echo "Started {service-name} with PID: $!"
- "@test-front": /sc:test @front @qa --play --think @complex-flag
- "@cicd": /sc:implement @devops @archi @back --think @complex-flag
- "@document": /sc:document --think @scribe @complex-flag
- "@fix": /sc:troubleshoot --think --seq 
- "@estimate": /sc:estimate --think-hard @complex-flag
- "@improve": /sc:improve --think @complex-flag
- "@analyze": /sc:analyze --think --seq 
- "@explain": /sc:explain --think --seq --answer-only 

### 파일 약어 
- "@error": debug/error.png파일을 의미함 
- "@info": debug/info.png파일을 의미함

### 작업 단계 가이드 약어  
- "@think-help": "기획실행프롬프트 내용을 터미널에 출력"
- "@design-help": "설계실행프롬프트 내용을 터미널에 출력"
- "@develop-help": "개발실행프롬프트 내용을 터미널에 출력"
- "@deploy-help": "배포실행프롬프트 내용을 터미널에 출력"
```

---

# Trip Service Lessons Learned

## 개요
Trip Service 개발 과정에서 얻은 시행착오와 학습 내용을 기록합니다.
새로운 시행착오가 발생할 때마다 이 섹션에 추가하여 지식을 축적합니다.

## API 테스트 관련

### 2025-08-08: Curl 테스트 시행착오

**문제상황**: Swagger UI에서는 정상 작동하지만 curl 명령어로는 실패하는 현상 발생

**시행착오 과정**:
1. 포트 번호 혼동: 8082 vs 8083 사용
2. Enum 값 대소문자 처리 불일치
3. 응답 파싱 방식 차이

**근본 원인**:
- **포트 차이**: Swagger UI는 8083 포트, 직접 curl은 8082 포트 사용 → 8083이 올바른 포트
- **Enum 매핑 규칙**:
  - `gender`, `healthStatus`, `preferences`: 소문자 사용 (`"male"`, `"good"`, `"culture"`)
  - `transportMode`: 대문자 사용 (`"PUBLIC"`, `"CAR"`)
- **응답 파싱**: JSON 응답을 `python3 -m json.tool` 또는 `jq`로 파싱 필요

**해결책**:
```bash
# 올바른 curl 사용법
curl -X 'POST' \
  'http://localhost:8083/api/v1/trips/basic-setup' \
  -H 'Authorization: Bearer JWT_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "tripName": "테스트여행",
    "transportMode": "CAR",
    "members": [{
      "gender": "male",
      "healthStatus": "good", 
      "preferences": ["nature"]
    }]
  }' | python3 -m json.tool
```

**학습 내용**:
1. **환경 확인**: 서비스 포트는 `application.yml` 또는 실행 로그에서 확인
2. **Enum 규칙**: Controller의 파싱 메서드에서 대소문자 변환 로직 확인 필요
3. **응답 검증**: HTTP 상태 코드와 JSON 구조 모두 확인
4. **도구 활용**: Swagger UI와 curl 명령어의 차이점 인식

**방지 방법**:
- Controller 분석 자동화 스크립트로 enum 매핑 규칙 추출
- 환경별 포트 매핑표 작성 및 관리
- 범용 테스트 스크립트로 일관성 확보

## JPA 양방향 관계 관련

### 2025-08-08: JPA 양방향 관계 설정 누락으로 인한 데이터 연관 오류

**문제상황**: 여행지 추가 API는 성공하지만 여행 조회 시 destinations 배열이 빈 상태로 나타남

**시행착오 과정**:
1. Redis 캐시 문제로 추정하여 캐시 삭제 시도
2. 트랜잭션 문제로 추정하여 서비스 재시작
3. JPA 엔티티 매핑 분석으로 근본 원인 발견

**근본 원인**:
- **JPA 양방향 관계 설정 불완전**: Trip과 Destination 간의 @OneToMany/@ManyToOne 관계에서 양쪽 참조가 모두 설정되지 않음
- **Destination 엔티티 설정**: `@Column(name = "trip_id", insertable = false, updatable = false)` 설정으로 인해 JPA가 오직 @ManyToOne 관계를 통해서만 trip_id를 관리
- **관계 설정 누락**: `Trip.addDestination()`에서 `destination.setTrip(this)` 호출 누락

**해결책**:
1. **Destination.java**: `setTrip(Trip trip)` 메서드 추가
2. **Trip.java**: 모든 여행지 관리 메서드에서 양방향 관계 설정
   - `addDestination()`: `destination.setTrip(this)` 추가
   - `removeDestination()`: `dest.setTrip(null)` 추가
   - `updateDestinations()`: 기존 관계 해제 후 새 관계 설정

**JPA 양방향 관계 필수 규칙**:
```java
// 양쪽 모두 설정해야 함
trip.getDestinations().add(destination);  // 컬렉션 쪽
destination.setTrip(trip);                // 객체 참조 쪽
```

**학습 내용**:
1. **양방향 관계 설정**: JPA에서 @OneToMany/@ManyToOne 관계는 항상 양쪽을 모두 설정해야 함
2. **insertable/updatable = false**: 해당 설정 시 JPA는 객체 참조를 통해서만 외래키를 관리
3. **디버깅 접근**: 캐시나 트랜잭션 문제로 추정하기 전에 JPA 관계 매핑을 먼저 확인
4. **도메인 모델 검증**: 엔티티 간 관계 설정이 비즈니스 로직과 일치하는지 검증

**방지 방법**:
- JPA 양방향 관계 설정 체크리스트 작성
- 엔티티 생성 시 관계 설정 메서드를 함께 구현
- 단위 테스트에서 관계 설정 검증 추가
- 도메인 이벤트를 활용한 관계 무결성 보장

## 테스트 환경 관리 관련

### 2025-08-08: 서비스 중단 중 API 테스트 시행착오

**문제상황**: API 테스트 중 서비스가 중단되어 비정상적인 응답이 발생함

**시행착오 과정**:
1. 첫 번째 여행지 추가: 정상 처리 응답 확인
2. 두 번째 여행지 추가: 비정상 응답 (날짜 계산 오류로 보임)  
3. Trip 조회 결과: destinations 배열이 빈 상태
4. 로그 확인: 서비스가 중단된 상태임을 발견

**근본 원인**:
- **서비스 상태 미확인**: 테스트 진행 중 백그라운드에서 서비스가 중단됨
- **상태 검증 부족**: API 응답은 받았지만 서비스가 정상 상태인지 확인하지 않음
- **인프라 의존성**: Redis 등 외부 인프라 장애로 인한 서비스 중단

**해결책**:
1. **서비스 상태 확인 필수**: API 테스트 전 서비스 정상 상태 검증
2. **사람이 서버 시작**: 서버 재시작은 반드시 사람에게 요청
3. **Health Check 활용**: `/actuator/health` 등으로 서비스 상태 사전 확인
4. **단계별 검증**: 각 API 호출 후 결과 상태 검증

**올바른 테스트 절차**:
```bash
# 1. 서비스 상태 확인
curl -s http://localhost:8083/actuator/health | jq .

# 2. 서비스 중단 시 사람에게 재시작 요청
echo "❌ 서비스가 중단되었습니다. 서버 재시작을 요청합니다."

# 3. 서비스 정상 후 API 테스트 진행
curl -X GET http://localhost:8083/api/v1/trips
```

**학습 내용**:
1. **테스트 환경 사전 확인**: API 테스트 전 필요한 인프라 및 서비스 상태 점검
2. **서버 관리 역할 분리**: 서버 시작/중지는 사람이 담당, AI는 테스트만 수행  
3. **단계별 상태 검증**: 각 단계에서 예상 결과와 실제 결과 비교 검증
4. **장애 상황 인지**: 비정상적인 응답 패턴 발견 시 서비스 상태부터 확인

**방지 방법**:
- 테스트 시작 전 health check 필수
- 비정상 응답 발견 시 즉시 서비스 상태 점검  
- 서버 재시작은 반드시 사람에게 요청
- 테스트 자동화 스크립트에 상태 검증 로직 포함

## 개발 및 배포 워크플로우 관련

### 2025-08-08: 소스 코드 수정 후 서버 재시작 필요성

**문제상황**: 소스 코드를 수정했으나 변경사항이 반영되지 않아 디버깅이 어려운 상황 발생

**근본 원인**:
- **컴파일 및 배포 누락**: Java 소스 코드 변경 시 컴파일과 서버 재시작이 필요함
- **개발 환경 특성**: Spring Boot 개발 환경에서 코드 변경 시 Hot Reloading이 제한적임
- **디버깅 혼란**: 변경사항이 반영되지 않아 수정된 로직이 작동하지 않는 것으로 오인

**해결책**:
1. **소스 수정 후 필수 단계**: 컴파일 → 서버 재시작 → 테스트
2. **사람에게 재시작 요청**: "서버를 재시작해주세요" 명시적 요청
3. **변경사항 확인**: 재시작 후 변경된 로직이 제대로 작동하는지 검증

**올바른 개발 워크플로우**:
```bash
# 1. 소스 코드 수정 완료 후
echo "✅ 소스 코드 수정이 완료되었습니다."
echo "📋 서버 재시작이 필요합니다. 재시작해주세요."

# 2. 사람이 서버 재시작 후
echo "🔄 서버 재시작을 확인했습니다."
curl -s http://localhost:8083/actuator/health | jq .

# 3. 변경사항 테스트 진행
echo "🧪 변경된 로직을 테스트합니다."
```

**학습 내용**:
1. **Spring Boot 특성**: 소스 코드 변경 시 반드시 컴파일 및 재시작 필요
2. **개발 효율성**: 변경사항이 반영되지 않은 상태로 디버깅하는 것은 비효율적
3. **협업 프로토콜**: AI와 사람 간의 명확한 역할 분담 필요
4. **검증 절차**: 재시작 후 변경사항이 올바르게 적용되었는지 확인 필수

**방지 방법**:
- 소스 수정 시 반드시 재시작 요청 메시지 출력
- 재시작 확인 후에만 테스트 진행
- 변경사항이 적용되지 않는 경우 재시작 상태부터 확인
- 개발 환경 Hot Reloading 제한사항 인지

## Redis 설정 관련

### 2025-08-08: Spring Boot 3.x Redis 설정 경로 변경

**문제상황**: 실행 프로파일에 REDIS_HOST가 올바르게 설정되어 있는데도 localhost로 Redis 접속하는 현상 발생

**시행착오 과정**:
1. 환경변수 확인: `REDIS_HOST=20.249.162.81` 정상 설정됨
2. application.yml 설정: `spring.redis.host: ${REDIS_HOST:localhost}` 사용
3. 로그 분석: `Unable to connect to localhost/<unresolved>:6379` 발생

**근본 원인**:
- **Spring Boot 버전 차이**: Trip Service는 Spring Boot 3.3.0 사용
- **설정 경로 변경**: Spring Boot 3.x에서 Redis 설정 네임스페이스 변경
  - Spring Boot 2.x: `spring.redis.*`
  - Spring Boot 3.x: `spring.data.redis.*`
- **자동 설정 무시**: 잘못된 경로로 인해 환경변수가 무시되고 기본값(localhost) 사용

**해결책**:
```yaml
# Before (Spring Boot 2.x 방식)
spring:
  redis:
    host: ${REDIS_HOST:localhost}
    
# After (Spring Boot 3.x 방식)  
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
```

**학습 내용**:
1. **버전 호환성 확인**: Spring Boot 메이저 버전 업그레이드 시 설정 경로 변경 검토 필요
2. **설정 검증 방법**: Health check에서 Redis 연결 상태 확인 가능
3. **일관성 유지**: Location Service는 이미 올바른 경로(`spring.data.redis.*`) 사용 중
4. **Connection Pool**: `jedis` → `lettuce`도 Spring Boot 3.x 기본값으로 변경

**방지 방법**:
- Spring Boot 업그레이드 시 Configuration Properties 문서 확인
- 모든 서비스에서 동일한 설정 패턴 사용
- Health check 엔드포인트로 Redis 연결 상태 사전 검증
- 설정 문서화 시 Spring Boot 버전 명시

## API 인증 관련

### 2025-08-08: API 테스트 시 JWT 토큰 누락으로 인한 403 Forbidden 에러

**문제상황**: Trip Service API 테스트 시 403 Forbidden 에러 발생

**시행착오 과정**:
1. Health check는 정상 (`status: "UP"`)
2. basic-setup API 호출 시 403 Forbidden 응답
3. 토큰 없이 API 호출 시도

**근본 원인**:
- **보안 설정**: Trip Service는 JWT 토큰 기반 인증이 활성화됨
- **인증 누락**: API 테스트 시 Authorization 헤더에 JWT 토큰 미포함
- **Security Config**: `/actuator/health`는 공개, `/api/**`는 인증 필요

**해결책**:
```bash
# 1. 사람에게 JWT 토큰 요청
echo "❌ API 테스트에 JWT 토큰이 필요합니다. 로그인해서 토큰을 제공해주세요."

# 2. 토큰을 받은 후 API 호출
curl -X POST http://localhost:8083/api/v1/trips/basic-setup \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{...}'
```

**학습 내용**:
1. **보안 인식**: 마이크로서비스 환경에서는 대부분의 API가 인증을 요구함
2. **테스트 절차**: Health check(공개) → 토큰 요청(사람) → API 테스트(토큰 포함)
3. **에러 해석**: 403은 인증 문제, 401은 인가 문제를 나타냄
4. **역할 분리**: AI는 테스트만 담당, 인증은 사람이 처리

**올바른 API 테스트 절차**:
```bash
# 1. 서비스 상태 확인
curl -s http://localhost:8083/actuator/health

# 2. 사람에게 JWT 토큰 요청
echo "❌ API 테스트에 JWT 토큰이 필요합니다. 로그인해서 토큰을 제공해주세요."

# 3. 토큰을 받은 후 API 테스트
curl -X POST http://localhost:8083/api/v1/trips/basic-setup \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{...}' | jq .
```

**방지 방법**:
- API 테스트 전 인증 요구사항 확인  
- 403/401 에러 발생 시 토큰 요청을 사람에게 요청
- Swagger UI 활용 시 토큰 설정 확인
- AI는 테스트만 담당, 인증 정보는 사람이 제공

## Redis 캐시 관리 관련

### 2025-08-08: Redis 캐시 삭제를 통한 테스트 환경 초기화

**문제상황**: API 테스트 중 이전 테스트 데이터가 남아있어 새로운 테스트에 영향을 주는 상황

**해결 과정**:
1. redis-cli 설치 필요 확인
2. Redis 서버 연결 테스트
3. 캐시 내용 확인 후 삭제

**해결책**:
```bash
# 1. redis-cli 설치 (macOS)
brew install redis

# 2. Redis 연결 테스트
redis-cli -h 20.249.162.81 -p 6379 ping

# 3. 현재 캐시 키 확인
redis-cli -h 20.249.162.81 -p 6379 keys "*"

# 4. 현재 데이터베이스의 모든 캐시 삭제
redis-cli -h 20.249.162.81 -p 6379 flushdb

# 5. 삭제 확인
redis-cli -h 20.249.162.81 -p 6379 keys "*"
```

**학습 내용**:
1. **캐시 영향도**: Redis 캐시가 API 테스트 결과에 영향을 줄 수 있음
2. **테스트 환경 관리**: 테스트 시 깨끗한 상태에서 시작하는 것이 중요
3. **도구 준비**: redis-cli 설치가 캐시 관리에 필수
4. **안전한 삭제**: `flushdb`는 현재 데이터베이스만, `flushall`은 모든 데이터베이스 삭제

**주의사항**:
```bash
# 주의: 운영 환경에서는 절대 사용 금지
# flushdb: 현재 데이터베이스만 삭제 
# flushall: 모든 데이터베이스 삭제 (더 위험)
```

**방지 방법**:
- 테스트 시작 전 캐시 상태 확인
- 필요시 캐시 삭제 후 테스트 진행
- 운영/개발 환경 구분해서 캐시 관리
- Redis 접속 정보 확인 후 신중하게 조작

## 서버 로그 확인 관련
### 2025-08-08: 서버 로그 파일 출력 설정

**문제상황**: 실행 중인 서버의 실시간 로그를 직접 확인하기 어려운 상황

**해결 방법**:

1. **application.yml 로그 파일 설정**:
   ```yaml
   logging:
     file:
       name: ${LOG_FILE:logs/trip-service.log}
     logback:
       rollingpolicy:
         max-file-size: 10MB
         max-history: 7
         total-size-cap: 100MB
   ```

2. **Gradle bootRun 로그 리다이렉트 실행**:
   ```bash
   # logs 디렉토리 생성
   mkdir -p logs
   
   # 백그라운드 실행 + 로그 파일 출력
   nohup ./gradlew bootRun > logs/app.log 2>&1 &
   
   # 실시간 로그 모니터링
   tail -f logs/app.log
   
   # 오류 로그만 필터링
   tail -f logs/app.log | grep -E "(ERROR|Exception|error)"
   ```

3. **로그 파일 관리**:
   ```bash
   # 특정 시간 로그 확인
   grep "13:48" logs/app.log
   
   # 최근 N줄 확인
   tail -100 logs/app.log
   
   # 오류 패턴 검색
   grep -A 10 -B 5 "Exception\|ERROR" logs/app.log
   ```

**장점**:
- 실시간 로그 모니터링 가능
- 로그 파일 보관으로 이후 분석 용이
- 롤링 정책으로 디스크 공간 관리
- 필터링을 통한 효율적인 오류 추적

**주의사항**:
- 로그 파일 경로와 권한 확인 필요
- 롤링 정책 설정으로 디스크 사용량 제한
- 개발 환경에서는 DEBUG 레벨, 운영 환경에서는 INFO 레벨 권장
