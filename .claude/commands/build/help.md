---
command: "/build:help"
---

개발 작업 순서

데이터베이스 관련:
1. 데이터베이스 설치 가이드 작성
/build:db-guide
- 백킹서비스 설치 가이드에 따라 데이터베이스 설치 방법을 안내합니다

2. 데이터베이스 설치 수행
/build:db-install
- 데이터베이스설치가이드에 따라 필요한 모든 데이터베이스를 설치합니다

3. 데이터베이스 설치 제거
/build:db-remove
- 설치된 데이터베이스 관련 모든 리소스를 삭제합니다

Message Queue 관련:
4. Message Queue 설치 가이드 작성
/build:mq-guide
- 백킹서비스 설치 가이드에 따라 Message Queue 설치 방법을 안내합니다

5. Message Queue 설치 수행
/build:mq-install
- MessageQueue설치가이드에 따라 필요한 모든 Message Queue를 설치합니다

6. Message Queue 설치 제거
/build:mq-remove
- 설치된 Message Queue 관련 모든 리소스를 삭제합니다