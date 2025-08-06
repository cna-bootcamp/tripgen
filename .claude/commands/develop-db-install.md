---
command: "/develop-db-install"
category: "Development & Implementation"
purpose: "데이터베이스 설치 수행"
wave-enabled: false
performance-profile: "standard"
---

# 데이터베이스 설치 수행 요청

## 프롬프트
```
@backing-service   
[요구사항]
'데이터베이스설치가이드'에 따라 설치해 주세요.  
'[설치정보]'섹션이 없으면 수행을 중단하고 안내 메시지를 표시하세요.  
{안내메시지}
'[설치정보]'섹션 하위에 아래 예와 같이 설치에 필요한 정보를 추가해 주세요.  
- 설치대상환경: 개발환경
- AKS Resource Group: rg-digitalgarage-01
- AKS Name: aks-digitalgarage-01
- Namespace: tripgen-dev
```

## 설명
데이터베이스 설치 계획서를 바탕으로 실제 데이터베이스를 설치하는 명령어입니다.

## 작업 내용
- 설치 정보 확인
- 데이터베이스 설치 실행
- 설치 결과 검증
- 설치 결과서 작성

## 관련 가이드
- 데이터베이스설치가이드

## 산출물
- 설치된 데이터베이스 인스턴스
- 데이터베이스 설치결과서