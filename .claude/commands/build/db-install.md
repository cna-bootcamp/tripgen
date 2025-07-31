---
command: "/build:db-install"
---

@backing-service   
[요구사항]
- 데이터베이스설치가이드에 따라 필요한 모든 데이터베이스 설치
- 현재 OS에 맞게 설치
- AKS에 설치
  - azure login이 이미 되어 있음
  - AKS: aks-digitalgarage-01
  - Namespace: tripgen
  - Database명: tripgen
- 데이터베이스 종류별로 서브 에이젼트를 병렬로 수행하여 설치
- 설치 후 데이터베이스 종류에 맞게 연결 방법 안내 
[참고자료]
- 데이터베이스설치가이드: build/database-install.md 
[결과파일]
- build/database/데이터베이스 설치 결과-{db종류}.md