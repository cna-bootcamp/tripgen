---
command: "/build:mq-remove"
---

@backing-service 
[요구사항]
- "Message Queue 설치 결과"를 보고 관련된 모든 리소스를 삭제
- 현재 OS에 맞게 수행  
- Message Queue 종류별로 서브 에이젼트를 병렬로 수행하여 설치
- 설치 후 "Message Queue 설치 결과"파일 삭제 
- 결과파일은 생성할 필요 없고 화면에만 결과 표시 
[참고자료]
- Message Queue 설치 결과: build/mq/MQ 설치 결과-{MQ종류}.md