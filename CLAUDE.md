
# 유저스토리 작성 가이드
[요청사항]
아래 항목으로 구성 
- 서비스: 마이크로서비스명 입력(ex: 홈페이지, 가입설계)
- ID: User Story ID로서 표준화된 형식(ex:RQ-<서비스약어>-<일련번호>)
- Epic: 유저스토리의 카테고리
- 유저스토리: <유저유형>으로서, 나는 <비즈니스 목적>을 위해, <작업/기능>을(를) 원합니다. 형식으로 입력
- Biz중요도: MoSCoW 분류로 입력. 
  - M:Must-반드시 필요 
  - S:Should-매우 필요하나 대체할 방법은 있음
  - C:Could-있으면 좋으나 우선 순위는 떨어짐(Nice to have)
  - W:Won’t-가장 우선순위가 떨어지므로 보류해도 됨
- 인수테스트 시나리오: 
  - 시나리오명: 테스트 시나리오 명
  - 인수기준: Given(=사전 조건/상황)/When(=Action)/Then(=결과) 형식
  - 체크리스트: 세부 테스트 항목임. 최대한 자세히 작성
- Score: 구현 난이도(피보나치 수열 이용하여 작성)

[예시]
- 링크: https://cna-bootcamp.github.io/clauding-guide/samples/Userstory.pdf 

[결과파일]
- design/Userstory.md
---

# 클라우드 아키텍처 패턴 선정 가이드

[요청사항]
1. 요구사항 분석
- 유저스토리 분석: 각 서비스별 기능적 요구사항과 비기능적 요구사항을 명확히 도출
- 화면설계 분석: wireframe에서 사용자 인터랙션 패턴과 데이터 플로우를 파악하여 시스템 특성 이해
- 통합 분석: 유저스토리와 화면설계를 연계하여 **기술적 도전과제 식별**

2. 패턴 선정
- 평가 기준 적용: 다음 5가지 기준으로 각 패턴을 평가
  - 기능 적합성(35%): 요구사항 직접 해결 능력
  - 성능 효과(25%): 응답시간/처리량 개선
  - 운영 복잡도(20%): 구현/운영 지원 능력 
  - 확장성(15%): 미래 요구사항 대응력
  - 비용 효율성(5%): ROI
- 정량적 평가: 각 패턴별로 평가 점수를 산출하고 우선순위 결정
- 단계별 적용: MVP/확장/고도화 3단계로 구분하여 로드맵 수립

3. 문서 작성
- 구조화된 작성: 요구사항 분석 -> 패턴 평가 -> 적용 설계 -> 구현 계획 순서로 작성
- Mermaid 다이어그램: 서비스 아키텍처와 패턴 적용을 시각적으로 표현
- 실용적 내용: 코드 예시, 구현 시 고려사항, 예상 효과를 구체적으로 기술

[참고자료]
- Userstory.md
- design/wireframe 폴더의 화면설계
- 클라우드아키텍처패턴: https://cna-bootcamp.github.io/clauding-guide/references/Cloud%20Design%20Patterns(개요).md

[결과파일]
- 파일명: design/pattern/cloud-architecture-patterns.md
- 필수 포함 내용:
  - 요구사항 분석 결과
  - 패턴 선정 매트릭스 (평가표)
  - 서비스별 패턴 적용 설계 (Mermaid)
  - Phase별 구현 로드맵
  - 예상 성과 지표

[체크리스트]
- 각 유저스토리가 어떤 패턴으로 해결되는지 명시했는가?
- 패턴 선정 이유를 정량적으로 설명했는가?
- 패턴 간 상호작용과 통합 아키텍처를 표현했는가?
- 구현 우선순위와 단계별 목표가 명확한가?
- 실무자가 바로 활용할 수 있는 수준인가?

# 설계 가이드 
## 공통 가이드
- PlantUML Script 형식으로 답변
  단, API 설계서는 파이프로 구분된 CSV형식으로 답변 
- plantuml script 의 테마 'mono'를 사용하고 제목을 표시(아래 예시 참조)
```
!theme mono

skinparam classAttributeIconSize 0
skinparam classFontSize 12
skinparam classAttributeFontSize 11

title 멤버십 포인트 적립 시스템 - 클래스 다이어그램 
```

## 논리 아키텍처 가이드
[요청사항]
- 사용자 관점의 컴포넌트 다이어그램 작성
- 유저스토리를 꼼꼼히 분석하여 사용자 Flow를 표시
- 처리순서별 번호 부여 및 간략 설명 
- PlantUML Script로 작성 
[참고자료]
- Userstory.md
- design/wireframe 폴더의 화면설계 
- 아키텍처패턴.txt
[예시]
- 링크: https://cna-bootcamp.github.io/clauding-guide/samples/sample_논리아키텍처.txt
[결과파일]
- design/backend/논리아키텍처.txt
  
## API설계 가이드
[요청사항]
- 서비스명: 유저스토리의 '서비스'컬럼 참조 
- 아래 행(row)으로 구성하여 작성
```
서비스명
마이크로서비스 이름
유저스토리 ID
유저스토리 제목
Controller 이름
API 목적
API Method
API 그룹 Path
API Path
Path <변수유형> <변수명>
Query Key
Query <변수유형> <변수명>
Request DTO 이름
Request DTO 배열 여부
Request DTO 구조
Response DTO 이름
Response DTO 배열 여부
Response DTO 구조
```
- 요청 데이터 필드가 1개이면 Path 또는 Query String으로 만들고 Request DTO 이름, 배열여부, 구조는 빈 값으로 할 것

[참고자료]
- Userstory.md
- 아키텍처패턴.txt
- 논리아키텍처.txt
- design/wireframe 폴더의 화면설계 
[예시]
- 링크: https://cna-bootcamp.github.io/clauding-guide/samples/sample_API%20설계서.txt
[결과파일]
- design/backend/API설계서.txt

## 외부 시퀀스설계 가이드 
[요청사항]
- 모든 마이크로서비스와 외부 시스템을 참여자로 추가 
- 각 마이크로서비스의 모든 API를 표시할 것. 한글 설명도 보여줄것.
- 마이크로서비스 내부의 처리 흐름은 표시하지 않음
- 요청만 표시하고 응답은 표시하지 말것
[참고자료]
- Userstory.md
- 아키텍처패턴.txt
- 논리아키텍처.txt
- API 설계서.txt
[예시]
- 링크: https://cna-bootcamp.github.io/clauding-guide/samples/sample_시퀀스설계서.txt
[결과파일]
- design/backend/시퀀스설계서.txt

## 내부 시퀀스설계 가이드 
[요청사항]
- 마이크로서비스의 모든 API를 표시할 것 
- 마이크로서비스 내부의 처리 흐름을 표시 
- 각 {SERVICE}마다 분리하여 각각 작성
[참고자료]
- Userstory.md
- design/wireframe 폴더의 화면설계
- 아키텍처패턴.txt
- 논리아키텍처.txt
- API 설계서.txt
- 시퀀스설계서.txt
[예시]
- 링크: https://cna-bootcamp.github.io/clauding-guide/samples/sample_시퀀스설계서.txt
[결과파일]
- design/backend/시퀀스설계서.txt

## 클래스설계 가이드 
[요청사항]
- 멀티프로젝트 구조로 설계 
- {아키텍처 패턴}을 적용
- '패키지 구조 표준'을 준용
- Clean아키텍처 적용 시 Port/Adapter라는 용어 대신 Clean 아키텍처에 맞는 용어 사용
- API 설계서의 2번째 컬럼부터 있는 API를 누락하지 말고 모두 반영할 것
- 클래스 간의 관계를 표현: Generalization, Realization, Dependency, Association, Aggregation, Composition
- 시퀀스 설계서의 처리 흐름을 꼼꼼히 반영할 것
- 프라퍼티와 메소드를 모두 기술할 것
- 각 {SERVICE}마다 분리하여 각각 작성하고 common은 가장 나중에 작성 
- '패키지 구조 표준'의 예시를 참조하여 모든 서비스가 포함된 패키지 구조도를 한꺼번에 제공

[참고자료]
- Userstory.md
- design/wireframe 폴더의 화면설계
- 아키텍처패턴.txt
- 논리아키텍처.txt
- API 설계서.txt
- 시퀀스설계서.txt
[예시]
- 링크: https://cna-bootcamp.github.io/clauding-guide/samples/sample_시퀀스설계서.txt
- 패키지 구조 표준: https://cna-bootcamp.github.io/clauding-guide/standards/standard_package_structure.txt
  
[결과파일]]
- design/backend/클래스설계서.txt

## 데이터설계 가이드 
[요청사항>
- 클래스설계서의 각 서비스별 entity와 일치해야 함
- 각 {SERVICE}마다 데이터베이스를 분리하여야 함 
[참고자료]
- Userstory.md
- design/wireframe 폴더의 화면설계
- 아키텍처패턴.txt
- 논리아키텍처.txt
- API 설계서.txt
- 시퀀스설계서.txt
- 클래스설계서.txt
[예시]
- 링크: https://cna-bootcamp.github.io/clauding-guide/samples/sample_데이터설계서.txt
[결과파일]]
- design/backend/데이터설계서.txt

## 물리 아키텍처 요구사항
[요청사항]
- {CLOUD} 기반의 배포 아키텍처 작성
- 설계 결과물을 참조하여 {CLOUD}의 최적의 Azure 서비스를 사용 
- 처리순서별 번호 부여 및 간략 설명 
[참고자료]
- Userstory.md
- design/wireframe 폴더의 화면설계
- 아키텍처패턴.txt
- 논리아키텍처.txt
- API 설계서.txt
- 시퀀스설계서.txt
- 클래스설계서.txt
- 데이터설계서.txt
[예시]
- 링크: https://cna-bootcamp.github.io/clauding-guide/samples/sample_물리아키텍처.txt
[결과파일]]
- design/backend/물리아키텍처.txt
