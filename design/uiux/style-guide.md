# 여행 일정 생성 서비스 UI/UX 스타일 가이드

## 🎨 브랜드 아이덴티티

### 컨셉
**"스마트하고 편안한 여행 계획의 동반자"**
- AI 기반의 지능적인 일정 생성
- 직관적이고 사용하기 쉬운 인터페이스
- 신뢰할 수 있는 여행 정보 제공

## 🎯 디자인 원칙

### 1. 직관성 (Intuitive)
- 사용자가 학습 없이도 바로 사용할 수 있는 인터페이스
- 명확한 네비게이션과 정보 계층 구조

### 2. 효율성 (Efficient)
- 최소한의 클릭으로 원하는 기능에 접근
- 빠른 로딩과 반응성 있는 상호작용

### 3. 신뢰성 (Reliable)
- 검증된 정보의 명확한 표시
- 일관된 디자인 패턴과 피드백

## 🎨 컬러 시스템

### Primary Colors
```css
--primary-blue: #2E86AB      /* 신뢰감 있는 메인 블루 */
--primary-blue-light: #A23B72 /* 활동적인 포인트 컬러 */
--primary-blue-dark: #1B5E7F  /* 강조 및 텍스트용 */
```

### Secondary Colors
```css
--secondary-orange: #F18F01   /* 따뜻한 여행 감성 */
--secondary-green: #C73E1D    /* 자연/위치 표시용 */
--secondary-yellow: #FFD23F   /* 알림/하이라이트용 */
```

### Neutral Colors
```css
--gray-50: #F8FAFC
--gray-100: #F1F5F9
--gray-200: #E2E8F0
--gray-300: #CBD5E1
--gray-400: #94A3B8
--gray-500: #64748B
--gray-600: #475569
--gray-700: #334155
--gray-800: #1E293B
--gray-900: #0F172A
```

### Semantic Colors
```css
--success: #10B981
--warning: #F59E0B
--error: #EF4444
--info: #3B82F6
```

## 📝 타이포그래피

### Font Family
```css
--font-primary: 'Pretendard', -apple-system, BlinkMacSystemFont, sans-serif;
--font-secondary: 'Inter', sans-serif;
--font-mono: 'JetBrains Mono', monospace;
```

### Font Scale
```css
--text-xs: 0.75rem;    /* 12px - 보조 정보 */
--text-sm: 0.875rem;   /* 14px - 일반 텍스트 */
--text-base: 1rem;     /* 16px - 기본 텍스트 */
--text-lg: 1.125rem;   /* 18px - 중요 정보 */
--text-xl: 1.25rem;    /* 20px - 소제목 */
--text-2xl: 1.5rem;    /* 24px - 제목 */
--text-3xl: 1.875rem;  /* 30px - 큰 제목 */
--text-4xl: 2.25rem;   /* 36px - 헤더 */
```

### Font Weight
```css
--font-light: 300;
--font-normal: 400;
--font-medium: 500;
--font-semibold: 600;
--font-bold: 700;
```

## 📐 스페이싱 시스템

### Spacing Scale
```css
--space-1: 0.25rem;   /* 4px */
--space-2: 0.5rem;    /* 8px */
--space-3: 0.75rem;   /* 12px */
--space-4: 1rem;      /* 16px */
--space-5: 1.25rem;   /* 20px */
--space-6: 1.5rem;    /* 24px */
--space-8: 2rem;      /* 32px */
--space-10: 2.5rem;   /* 40px */
--space-12: 3rem;     /* 48px */
--space-16: 4rem;     /* 64px */
--space-20: 5rem;     /* 80px */
```

## 🔲 컴포넌트 스타일

### 버튼 (Buttons)
```css
/* Primary Button */
.btn-primary {
  background: var(--primary-blue);
  color: white;
  padding: var(--space-3) var(--space-6);
  border-radius: 8px;
  font-weight: var(--font-medium);
  border: none;
  transition: all 0.2s ease;
}

.btn-primary:hover {
  background: var(--primary-blue-dark);
  transform: translateY(-1px);
}

/* Secondary Button */
.btn-secondary {
  background: transparent;
  color: var(--primary-blue);
  border: 2px solid var(--primary-blue);
  padding: var(--space-3) var(--space-6);
  border-radius: 8px;
}
```

### 카드 (Cards)
```css
.card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  padding: var(--space-6);
  border: 1px solid var(--gray-200);
}

.card:hover {
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}
```

### 입력 필드 (Input Fields)
```css
.input {
  border: 2px solid var(--gray-300);
  border-radius: 8px;
  padding: var(--space-3) var(--space-4);
  font-size: var(--text-base);
  transition: border-color 0.2s ease;
}

.input:focus {
  border-color: var(--primary-blue);
  outline: none;
  box-shadow: 0 0 0 3px rgba(46, 134, 171, 0.1);
}
```

## 📱 반응형 브레이크포인트

### Breakpoints
```css
--mobile: 320px;
--mobile-lg: 480px;
--tablet: 768px;
--desktop: 1024px;
--desktop-lg: 1280px;
```

### Grid System
```css
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--space-4);
}

@media (min-width: 768px) {
  .container {
    padding: 0 var(--space-6);
  }
}
```

## 🎯 특화 컴포넌트

### 여행 카드
- **대표 이미지**: 여행명 기반 자동 생성 이미지
- **정보 표시**: 여행명, 여행 기간, 진행상황
- **진행바**: 
  - 여행중: 전체 일정 대비 진행률
  - 계획중: 4단계 진행 상황
  - 추억중: 표시 없음

### 장소 카드
- **기본 정보**: 장소명, 이동 시간 (강조)
- **평점 표시**: 별점 시각화 + 리뷰 수
- **주소**: 전체 주소 표시
- **액션 버튼**: 상세정보, 길찾기

### 멤버 카드
- **기본 정보**: 이름, 나이, 성별
- **건강상태**: 우수/양호/주의/제한 뱃지
- **선호도**: 태그 형태로 표시
- **편집/삭제**: 카드 선택 시만 표시

## 🔄 상호작용 패턴

### 로딩 상태
- **스켈레톤**: 콘텐츠 구조를 보여주는 플레이스홀더
- **스피너**: AI 처리 중 상태 표시
- **프로그레스 바**: 단계별 진행 상태

### 피드백
- **성공**: 녹색 체크마크와 메시지
- **오류**: 빨간색 경고 아이콘과 해결 방안
- **정보**: 파란색 정보 아이콘과 추가 설명

### 애니메이션
```css
.fade-in {
  animation: fadeIn 0.3s ease-in-out;
}

.slide-up {
  animation: slideUp 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
```

## 📋 체크리스트

### 디자인 일관성
- [ ] 모든 화면에서 동일한 컬러 시스템 적용
- [ ] 일관된 타이포그래피 사용
- [ ] 통일된 버튼 스타일 적용
- [ ] 동일한 간격 시스템 활용

### 접근성
- [ ] 충분한 색상 대비율 (4.5:1 이상)
- [ ] 키보드 네비게이션 지원
- [ ] 스크린 리더 호환성
- [ ] 다양한 디바이스 크기 지원

### 사용성
- [ ] 직관적인 아이콘 사용
- [ ] 명확한 피드백 제공
- [ ] 빠른 로딩 시간 보장
- [ ] 오프라인 상태 안내

---

**마지막 업데이트**: 2025-07-24  
**담당자**: 최프론트/뷰어