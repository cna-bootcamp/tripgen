---
command: "/develop-fix-backend"
category: "Development & Implementation"
purpose: "백엔드 오류 해결"
wave-enabled: true
performance-profile: "standard"
---

# 백엔드 오류 해결 요청

## 프롬프트
```
@fix as @back
개발된 각 서비스와 common 모듈을 컴파일하고 에러를 해결해 주세요.   
- common 모듈 우선 수행   
- 각 서비스별로 서브 에이젠트를 병렬로 수행  
- 컴파일이 모두 성공할때까지 계속 수행 
```

## 설명
백엔드 서비스들의 컴파일 에러를 체계적으로 해결하는 명령어입니다. common 모듈을 우선 처리하고 각 서비스를 병렬로 처리합니다.

## 작업 내용
- common 모듈 컴파일 및 오류 수정
- 각 서비스별 병렬 에러 해결
- 의존성 문제 해결
- 컴파일 성공까지 반복 수행

## 관련 가이드
- 백엔드개발가이드

## 산출물
- 컴파일 성공한 백엔드 서비스들
- 오류 해결 보고서