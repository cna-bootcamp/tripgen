---
command: "/develop-mq-remove"
category: "Development & Implementation"
purpose: "Message Queue 설치 제거"
wave-enabled: false
performance-profile: "standard"
---

# Message Queue 설치 제거 요청

## 프롬프트
```
@backing-service 
[요구사항]
- "MQ설치결과서"를 보고 관련된 모든 리소스를 삭제
- 현재 OS에 맞게 수행  
- 서브 에이젼트를 병렬로 수행하여 삭제
- 결과파일은 생성할 필요 없고 화면에만 결과 표시 
[참고자료]
- MQ설치결과서
```

## 설명
설치된 Message Queue 관련 리소스를 완전히 제거하는 명령어입니다.

## 작업 내용
- MQ 설치 결과서 분석
- 리소스 삭제 실행
- 병렬 처리로 효율적 삭제
- 삭제 결과 확인

## 관련 가이드
- MQ설치결과서

## 산출물
- 화면 출력으로 삭제 결과 표시