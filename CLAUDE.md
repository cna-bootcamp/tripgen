# 여행 일정 생성 서비스 설계 가이드

> 마이크로서비스 아키텍처 기반 여행 상세 일정 생성 서비스의 체계적인 설계 가이드

## 🎯 프로젝트 개요

**목표**: AI 기반 여행 상세 일정 생성 서비스  
**아키텍처**: 마이크로서비스 (프로파일, 일정, 장소 서비스)  
**기술 스택**: Spring Boot, Redis Cache, MCP (외부 API), Job Queue

## 📚 가이드
링크: https://cna-bootcamp.github.io/clauding-guide/guides/README.md

## 마이크로서비스 구성
| 서비스 | 역할 | 스케일링 정책 |
|--------|------|---------------|
| **Profile Service** | 멤버/여행 정보 관리 | Min 1, Max 10 인스턴스 |
| **Itinerary Service** | AI 일정 생성/관리 | Min 2, Max 20 인스턴스 |
| **Location Service** | 장소 검색/검증 | Min 1, Max 15 인스턴스 |


## ⚡ 핵심 원칙

### 🔄 병렬 처리 전략
- **서브 에이전트 활용**: Task 도구로 서비스별 동시 작업
- **3단계 하이브리드 접근**: 
  1. 공통 컴포넌트 (순차)
  2. 서비스별 설계 (병렬) 
  3. 통합 검증 (순차)
- **의존성 기반 그룹화**: 의존 관계에 따른 순차/병렬 처리
- **통합 검증**: 병렬 작업 완료 후 전체 검증

### 🏗️ 마이크로서비스 설계
- **서비스 독립성**: 캐시를 통한 직접 의존성 최소화  
- **선택적 비동기**: 장시간 작업(AI 일정 생성)만 비동기
- **캐시 우선**: Redis를 통한 성능 최적화

### 📝 표준화
- **PlantUML**: 모든 다이어그램 표준 (`!theme mono`)
- **OpenAPI 3.0**: API 명세 표준
- **자동 검증**: PlantUML, OpenAPI 문법 검사 필수
  - **PlamtUML 스크립트 파일은 PlantUML 문법 검사 가이드를 준용**
  - PlantUML 문법 검사  가이드:  https://cna-bootcamp.github.io/clauding-guide/guides/tools/plantuml-guide.md
  - **Window는 스크립트 파일을 생성**하여 수행 
  
## 📂 가이드 구조

### 🚀 1. 실행 프롬프트 (prompt/)
| 파일 | 설명 | 사용 시점 |
|------|------|-----------|
| [userstory-prompt.md](https://cna-bootcamp.github.io/clauding-guide/guides/prompt/userstory-prompt.md) | 유저스토리 작성 실행 프롬프트 | 요구사항 분석 실행 시 |
| [design-prompt.md](https://cna-bootcamp.github.io/clauding-guide/guides/prompt/design-prompt.md) | 전체 설계 실행 프롬프트 모음 | 각 설계 단계 실행 시 |

### 🔧 2. 설정 (setup/)
| 파일 | 설명 | 사용 시점 |
|------|------|-----------|
| *(향후 추가)* | MCP 서버 동기화 가이드 | 프로젝트 초기 설정 |
| *(향후 추가)* | 개발환경 설정 가이드 | 프로젝트 초기 설정 |

### 📝 3. 요구사항 분석 (requirements/)
| 파일 | 설명 | 사용 시점 |
|------|------|-----------|
| [userstory.md](https://cna-bootcamp.github.io/clauding-guide/guides/requirements/userstory.md) | 유저스토리 작성 가이드 | 프로젝트 요구사항 정의 시 |
| [architecture-patterns.md](https://cna-bootcamp.github.io/clauding-guide/guides/requirements/architecture-patterns.md) | 아키텍처 패턴 선정 가이드 | 시스템 아키텍처 설계 전 |

### 🏗️ 4. 설계 작업 (design/)
| 파일 | 설명 | 사용 시점 |
|------|------|-----------|
| [common-principles.md](https://cna-bootcamp.github.io/clauding-guide/guides/design/common-principles.md) | 공통 설계 원칙 | 모든 설계 단계에서 참조 |
| [architecture.md](https://cna-bootcamp.github.io/clauding-guide/guides/design/architecture.md) | 논리/물리 아키텍처 설계 가이드 | 전체 시스템 구조 설계 시 |
| [api-design.md](https://cna-bootcamp.github.io/clauding-guide/guides/design/api-design.md) | API 설계 가이드 | 서비스별 API 명세 작성 시 |
| [sequence-design.md](https://cna-bootcamp.github.io/clauding-guide/guides/design/sequence-design.md) | 외부/내부 시퀀스 설계 가이드 | 서비스 플로우 설계 시 |
| [class-data-design.md](https://cna-bootcamp.github.io/clauding-guide/guides/design/class-data-design.md) | 클래스/데이터 설계 가이드 | 세부 구현 설계 시 |

### 🛠️ 5. 기술 도구 (tools/)
| 파일 | 설명 | 사용 시점 |
|------|------|-----------|
| [plantuml-guide.md](https://cna-bootcamp.github.io/clauding-guide/guides/tools/plantuml-guide.md) | PlantUML 문법 검사 가이드 | 다이어그램 작성 시 |
| [openapi-tools.md](https://cna-bootcamp.github.io/clauding-guide/guides/tools/openapi-tools.md) | OpenAPI to CSV 변환 도구 | API 명세 CSV 변환 시 |
| [openapi-to-csv] | OpenAPI to CSV 변환기 | API 설계서 생성 시 |
