---
command: "/build:mq-install"
---

@backing-service 
[요구사항]
- "MessageQueue설치가이드"에 따라 필요한 모든 Message Queue 설치
- 현재 OS에 맞게 설치
- Message Queue 종류별로 서브 에이젼트를 병렬로 수행하여 설치
- 설치 후 Message Queue 종류에 맞게 설치 확인 방법 안내 
[참고자료]
- MessageQueue설치가이드: build/mq-install.md 
[결과파일]
- build/mq/MQ 설치 결과-{MQ종류}.md