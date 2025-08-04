---
command: "/build:help"
---

개발 작업 순서

1단계: 데이터베이스 설치계획서 작성
/build:db-guide
- 데이터베이스설치계획서가이드에 따라 설치계획서 작성

2단계: 데이터베이스 설치 수행
/build:db-install
- 데이터베이스설치가이드에 따라 개발환경에 설치

3단계: Message Queue 설치계획서 작성 (필요시)
/build:mq-guide
- MQ설치게획서가이드에 따라 설치계획서 작성

4단계: Message Queue 설치 수행 (필요시)
/build:mq-install
- MQ설치가이드에 따라 개발환경에 설치

5단계: 백엔드 개발
/build:dev-backend
- 백엔드개발가이드에 따라 개발 수행

제거 명령어:
- 데이터베이스 제거: /build:db-remove
- Message Queue 제거: /build:mq-remove