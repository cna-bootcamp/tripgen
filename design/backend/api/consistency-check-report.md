# API 설계 일관성 검사 보고서

**작성자**: 김개발/테키, 정백엔드/서버맨, 최프론트/뷰어  
**작성일**: 2025-07-28  
**검사 범위**: 외부/내부 시퀀스 다이어그램 vs API 설계서

## 요약

외부/내부 시퀀스 설계서와 API 설계서 간의 일관성을 검사한 결과, 전체적인 플로우는 일치하나 일부 API 경로, HTTP 메소드, 누락된 엔드포인트 등의 불일치 사항이 발견되었습니다.

## 1. 로그인/프로필 관리 플로우

### ✅ 일치 사항
- 로그인 API: `POST /login`
- 로그아웃 API: `POST /logout`
- 프로필 조회 API: `GET /profile`
- JWT 토큰 기반 인증
- Redis 캐싱 전략
- 계정 잠금 처리 (5회 실패 시)

### ❌ 불일치 사항
| 항목 | 외부 시퀀스 | API 설계서 | 영향도 |
|------|------------|-----------|--------|
| API 경로 prefix | `/api/v1/users/` | `/user/` (base URL) | 낮음 |
| 로그아웃 다이얼로그 | UI에서 표시 | API에 미포함 | 없음 |

### 📌 권장사항
- API Gateway 설정에서 경로 매핑 통일

## 2. 여행계획 생성 플로우

### ✅ 일치 사항
- 일정 생성 요청: `POST /trips/{tripId}/schedules/generate`
- 진행 상태 확인: `GET /trips/{tripId}/schedules/generate/{requestId}/status`
- 비동기 처리 방식 (Message Queue)
- LocalStorage 활용 전략

### ❌ 불일치 사항
| 항목 | 시퀀스 다이어그램 | API 설계서 | 영향도 |
|------|------------------|-----------|--------|
| 기본설정 API | `/trips/basic-info` | `POST /trips` (새 여행 생성) | 높음 |
| 멤버 일괄 저장 | 기본설정에 포함 | 개별 추가만 존재 | 높음 |
| 여행지 일괄 저장 | `/destinations/batch` | 개별 추가만 존재 | 높음 |

### 📌 권장사항
- 다음 API 추가 필요:
  ```yaml
  POST /trips/{tripId}/members/batch    # 멤버 일괄 저장
  POST /trips/{tripId}/destinations/batch  # 여행지 일괄 저장
  ```

## 3. AI 일정 생성 플로우

### ✅ 일치 사항
- 일정 생성: `POST /schedules/generate`
- 상태 조회: `GET /schedules/{requestId}/status`
- 일정 조회: `GET /schedules/{requestId}`
- 비동기 처리 및 진행률 추적
- 캐싱 전략 (날씨, 장소, 일정)

### ❌ 불일치 사항
| 항목 | 시퀀스 다이어그램 | API 설계서 | 영향도 |
|------|------------------|-----------|--------|
| 취소 API | `POST /cancel` | `DELETE /{requestId}` | 낮음 |

### 📌 권장사항
- RESTful 원칙에 따라 DELETE 메소드 사용 권장

## 4. 일정 조회/수정/재생성 플로우

### ✅ 일치 사항
- 일정 조회: `GET /trips/{tripId}/schedules`
- 일자별 재생성: `POST /trips/{tripId}/schedules/days/{day}/regenerate`
- 내보내기: `POST /trips/{tripId}/schedules/export`
- 프론트엔드 최적화 전략

### ❌ 불일치 사항
| 항목 | 시퀀스 다이어그램 | API 설계서 | 영향도 |
|------|------------------|-----------|--------|
| 순서 변경 API | `PATCH /schedules/order` | 없음 | 높음 |
| 전체 재생성 API | `/regenerate-all` | AI Service로 직접 요청 | 중간 |
| 네비게이션 검증 | `/validation/navigation` | 없음 | 중간 |

### 📌 권장사항
- 다음 API 추가 필요:
  ```yaml
  PATCH /trips/{tripId}/schedules/order  # 장소 순서 변경
  GET /trips/{tripId}/navigation/validate  # 진행바 네비게이션 검증
  ```

## 5. 주변장소검색/AI추천정보 플로우

### ✅ 일치 사항 (수정 완료)
- 주변장소검색: `POST /search/nearby` ✅
- 키워드검색: `GET /search/keyword` ✅
- 장소상세정보: `GET /locations/{placeId}` ✅
- AI 추천정보: `GET /locations/{placeId}/recommendations` ✅
- 지역별 API 전략 (국내/해외)
- 캐시 우선 전략

### ✅ 수정 완료 사항
| 항목 | 수정 전 | 수정 후 | 상태 |
|------|--------|---------|------|
| API Base URL | `/v1/locations` | `/api/v1/locations` | ✅ 완료 |
| AI 추천정보 경로 | `/places/{placeId}/recommendations` | `/locations/{placeId}/recommendations` | ✅ 완료 |
| 내부 시퀀스 HTTP 메소드 | `GET /search/nearby` | `POST /search/nearby` | ✅ 완료 |
| 리소스 명명 | `places` 혼용 | `locations`로 통일 | ✅ 완료 |

### 📌 완료된 수정사항
- API Gateway 경로 통일: `/api/v1/locations`
- 리소스 명명 일관성: 모든 엔드포인트에서 `locations` 사용
- HTTP 메소드 일치: 내부 시퀀스 다이어그램 수정

## 6. 전체 개선 사항

### 🔧 즉시 수정 필요 (높은 우선순위)
1. **일괄 처리 API 추가**
   - 멤버 일괄 저장 API
   - 여행지 일괄 저장 API
   - 장소 순서 변경 API

2. **누락된 검증 API 추가**
   - 진행바 네비게이션 검증 API

### 🎯 중장기 개선 사항
1. **API 경로 표준화**
   - Gateway 레벨에서 일관된 경로 prefix 적용
   - 리소스 명명 규칙 통일 (locations vs places)

2. **HTTP 메소드 표준화**
   - RESTful 원칙에 따른 메소드 사용
   - 취소: DELETE, 순서변경: PATCH

3. **응답 형식 표준화**
   - 에러 응답 형식 통일
   - 페이지네이션 응답 구조 표준화

## 7. 결론

전체적인 설계 방향과 아키텍처는 일치하나, 세부 구현 레벨에서 불일치 사항이 존재합니다. 특히 프론트엔드 최적화를 위한 일괄 처리 API들이 누락되어 있어 추가가 필요합니다.

### 다음 단계
1. 높은 우선순위 API 추가 구현
2. API 문서 업데이트
3. 시퀀스 다이어그램과 API 설계서 동기화
4. 통합 테스트 시나리오 작성

---

**최초 작성**: 2025-07-28  
**최종 수정**: 2025-07-28 (주변장소검색/AI추천정보 플로우 수정 반영)
**작성자**: 김개발/테키, 정백엔드/서버맨, 최프론트/뷰어  
**검토자**: (서명 필요)  
**승인**: (서명 필요)