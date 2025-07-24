# Location Service API 변경 사항 요약

## 수정 완료된 내용

### 1. 새로운 엔드포인트 추가
- **POST /api/location/validate-batch**
  - Claude 추천 장소 일괄 검증 엔드포인트
  - 최대 50개 장소를 한 번에 검증 가능
  - 실제 장소 매칭 및 대체 장소 제안 기능 포함

### 2. 새로운 스키마 추가
- **PlaceValidationRequest**: 검증 요청 본문
- **PlaceToValidate**: 검증할 개별 장소 정보
- **ApproximateLocation**: 대략적인 위치 정보 (도시, 국가, 랜드마크)
- **PlaceValidationResult**: 검증 결과 응답
- **ValidatedPlace**: 검증된 개별 장소 정보
- **MatchedPlace**: 실제 매칭된 장소의 상세 정보
- **AlternativePlaceSuggestion**: 대체 장소 제안 정보

### 3. 기존 엔드포인트 확장
- **/places/search/nearby** GET 엔드포인트에 `fuzzyMatch` 파라미터 추가
  - Claude 추천 장소명과 유사한 장소를 검색할 때 사용
  - 기본값: false

### 4. 태그 업데이트
- `validation` 태그 설명에 "Claude 추천 장소 검증" 추가

### 5. 예제 추가
- "파리의 에펠탑 근처 카페"와 "루브르 박물관 정문 레스토랑" 검증 예시
- 요청/응답 예제에 실제 장소 매칭 결과 포함

## 주요 기능

1. **유사도 기반 매칭**: Claude가 추천한 모호한 장소명을 실제 장소와 매칭
2. **신뢰도 점수**: 0-1 사이의 매칭 신뢰도 제공
3. **대체 장소 제안**: 더 나은 옵션이 있을 경우 최대 5개까지 제안
4. **검증 상태 분류**:
   - `found`: 정확히 일치하는 장소 발견
   - `found_similar`: 유사한 장소 발견
   - `not_found`: 장소를 찾을 수 없음
   - `ambiguous`: 여러 후보가 있어 모호함
5. **다국어 지원**: 결과를 원하는 언어로 반환

## OpenAPI 3.0 표준 준수
- 모든 변경 사항이 OpenAPI 3.0.3 스펙을 준수
- swagger-cli 검증 통과