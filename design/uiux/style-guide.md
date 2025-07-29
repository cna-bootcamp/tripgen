# 여행 일정 생성 서비스 - UI/UX 스타일 가이드

## 브랜드 아이덴티티

### 디자인 컨셉
**"Smart Journey Companion"**
- **핵심 가치**: 지능적, 개인화된, 신뢰할 수 있는 여행 동반자
- **감성 키워드**: 모험, 발견, 편리함, 개인화, 신뢰성
- **시각적 방향**: 현대적이면서도 친근한, 여행의 설렘과 안정감을 동시에 제공

### 브랜드 성격
- **Smart**: AI 기반 지능형 서비스
- **Personal**: 개인 맞춤형 추천과 배려
- **Accessible**: 모든 사용자를 위한 접근성
- **Reliable**: 신뢰할 수 있는 여행 파트너

---

## 디자인 원칙

### 1. 명확성 (Clarity)
- 사용자가 언제나 현재 위치와 다음 단계를 명확히 알 수 있도록 설계
- 복잡한 정보도 단계별로 분할하여 제공
- 직관적인 아이콘과 라벨 사용

### 2. 일관성 (Consistency)
- 동일한 기능은 항상 같은 방식으로 표현
- 색상, 타이포그래피, 간격의 일관된 적용
- 플랫폼 간 동일한 사용자 경험 제공

### 3. 효율성 (Efficiency)
- 최소한의 클릭/터치로 목표 달성
- 자주 사용하는 기능의 빠른 접근
- 자동저장과 스마트 기본값 제공

### 4. 포용성 (Inclusivity)
- 다양한 건강 상태와 선호도 고려
- 접근성 가이드라인 준수
- 다국어 및 문화적 차이 고려

### 5. 반응성 (Responsiveness)
- 모든 기기에서 최적화된 경험
- 터치와 마우스 인터랙션 모두 지원
- 네트워크 상태에 따른 적응형 콘텐츠

---

## 컬러 시스템

### Primary Colors

#### Travel Blue (#2563EB)
- **용도**: 주요 버튼, 링크, 활성 상태
- **의미**: 신뢰성, 여행의 자유로움, 전문성  
- **RGB**: 37, 99, 235
- **사용법**: CTA 버튼, 네비게이션 활성 상태, 진행 표시

#### Adventure Orange (#F97316)
- **용도**: 강조, 알림, 액센트
- **의미**: 활력, 모험, 열정
- **RGB**: 249, 115, 22
- **사용법**: 중요 알림, 특별 이벤트, 호버 효과

### Secondary Colors

#### Deep Blue (#1E40AF)
- **용도**: Travel Blue의 어두운 변형
- **사용법**: 호버 상태, 활성 버튼

#### Light Orange (#FED7AA)  
- **용도**: Adventure Orange의 밝은 배경
- **사용법**: 알림 배경, 강조 영역

### Status Colors

#### Success Green (#10B981)
- **용도**: 성공 메시지, 완료 상태
- **사용법**: 저장 완료, 일정 생성 성공

#### Warning Yellow (#F59E0B)
- **용도**: 주의 메시지, 대기 상태  
- **사용법**: 입력 오류, 진행 중 상태

#### Error Red (#EF4444)
- **용도**: 오류 메시지, 삭제 버튼
- **사용법**: 필수 입력 누락, 삭제 확인

#### Info Blue (#3B82F6)
- **용도**: 정보 메시지, 도움말
- **사용법**: 안내 정보, 팁 표시

### Neutral Colors

#### Text Gray Scale
- **Primary Text**: #111827 (거의 검정)
- **Secondary Text**: #374151 (진한 회색)
- **Tertiary Text**: #6B7280 (중간 회색)
- **Disabled Text**: #9CA3AF (연한 회색)

#### Background Gray Scale  
- **White**: #FFFFFF (기본 배경)
- **Off White**: #F9FAFB (섹션 배경)
- **Light Gray**: #F3F4F6 (카드 배경)
- **Border Gray**: #E5E7EB (경계선)

### 색상 사용 가이드라인

#### 접근성 기준
- **일반 텍스트**: 4.5:1 이상의 대비비
- **대형 텍스트**: 3:1 이상의 대비비
- **그래픽 요소**: 3:1 이상의 대비비

#### 색상 조합 규칙
- 주색상 + 중성색: 기본 UI 요소
- 주색상 + 보조색: 강조가 필요한 경우
- 상태색 단독 사용: 특정 상태 표현

---

## 타이포그래피

### 폰트 패밀리

#### Primary Font: Noto Sans KR
- **특징**: 한글 최적화, 읽기 쉬운 산세리프
- **용도**: 모든 한글 텍스트
- **웹폰트**: Google Fonts에서 제공

#### Secondary Font: Inter  
- **특징**: 현대적 산세리프, 디지털 최적화
- **용도**: 영문 텍스트, UI 라벨
- **웹폰트**: Google Fonts에서 제공

### 폰트 크기 시스템

#### Heading Scale
```css
/* H1 - 페이지 제목 */
font-size: 32px; /* 2rem */
line-height: 40px; /* 1.25 */
font-weight: 700; /* Bold */

/* H2 - 섹션 제목 */  
font-size: 24px; /* 1.5rem */
line-height: 32px; /* 1.33 */
font-weight: 600; /* SemiBold */

/* H3 - 서브섹션 제목 */
font-size: 20px; /* 1.25rem */
line-height: 28px; /* 1.4 */
font-weight: 600; /* SemiBold */

/* H4 - 카드 제목 */
font-size: 18px; /* 1.125rem */
line-height: 24px; /* 1.33 */
font-weight: 500; /* Medium */
```

#### Body Scale
```css
/* Body Large - 중요한 본문 */
font-size: 18px; /* 1.125rem */
line-height: 28px; /* 1.56 */
font-weight: 400; /* Regular */

/* Body - 기본 본문 */
font-size: 16px; /* 1rem */
line-height: 24px; /* 1.5 */
font-weight: 400; /* Regular */

/* Body Small - 부가 정보 */
font-size: 14px; /* 0.875rem */
line-height: 20px; /* 1.43 */
font-weight: 400; /* Regular */

/* Caption - 라벨, 설명 */
font-size: 12px; /* 0.75rem */
line-height: 16px; /* 1.33 */
font-weight: 500; /* Medium */
```

### 폰트 두께 (Font Weight)
- **Regular (400)**: 일반 본문 텍스트
- **Medium (500)**: 라벨, 캡션, 강조 텍스트
- **SemiBold (600)**: 섹션 제목, 중요 정보
- **Bold (700)**: 페이지 제목, 핵심 메시지

### 타이포그래피 사용 규칙
- 한 화면에 3가지 이상의 폰트 크기 동시 사용 지양
- 텍스트 계층이 명확하도록 크기 차이 충분히 확보  
- 긴 텍스트는 적절한 줄 간격으로 가독성 향상

---

## 간격 시스템

### 기본 단위
**Base Unit**: 4px (0.25rem)
- 모든 간격이 4의 배수로 구성
- 일관된 시각적 리듬 제공
- 개발 시 계산 용이성

### 간격 토큰
```css
/* Extra Small */
--spacing-xs: 4px;   /* 0.25rem */

/* Small */  
--spacing-sm: 8px;   /* 0.5rem */

/* Medium */
--spacing-md: 12px;  /* 0.75rem */

/* Large */
--spacing-lg: 16px;  /* 1rem */

/* Extra Large */
--spacing-xl: 24px;  /* 1.5rem */

/* 2X Large */
--spacing-2xl: 32px; /* 2rem */

/* 3X Large */
--spacing-3xl: 48px; /* 3rem */

/* 4X Large */
--spacing-4xl: 64px; /* 4rem */
```

### 간격 적용 가이드

#### 컴포넌트 내부 여백
- **텍스트 여백**: sm(8px) ~ md(12px)
- **버튼 내부**: md(12px) ~ lg(16px)  
- **카드 내부**: lg(16px) ~ xl(24px)
- **폼 필드**: md(12px) ~ lg(16px)

#### 컴포넌트 간 여백
- **관련 요소**: sm(8px) ~ md(12px)
- **일반 요소**: lg(16px) ~ xl(24px)
- **섹션 구분**: 2xl(32px) ~ 3xl(48px)
- **페이지 레벨**: 3xl(48px) ~ 4xl(64px)

#### 레이아웃 여백
- **모바일 좌우**: lg(16px)
- **태블릿 좌우**: xl(24px)  
- **데스크톱 좌우**: 2xl(32px)
- **컨테이너 최대 너비**: 1200px

---

## 컴포넌트 스타일

### 버튼 (Buttons)

#### Primary Button
```css
/* 기본 상태 */
background: #2563EB;
color: #FFFFFF;
padding: 12px 24px;
border-radius: 8px;
font-weight: 500;
border: none;

/* 호버 상태 */
background: #1E40AF;

/* 활성 상태 */
background: #1E3A8A;
transform: translateY(1px);

/* 비활성 상태 */
background: #9CA3AF;
cursor: not-allowed;
```

#### Secondary Button  
```css
/* 기본 상태 */
background: transparent;
color: #2563EB;
border: 2px solid #2563EB;
padding: 10px 24px;
border-radius: 8px;
font-weight: 500;

/* 호버 상태 */
background: #F0F9FF;
```

#### Danger Button
```css
/* 기본 상태 */
background: #EF4444;
color: #FFFFFF;
padding: 12px 24px;
border-radius: 8px;
font-weight: 500;

/* 호버 상태 */
background: #DC2626;
```

### 입력 필드 (Input Fields)

#### Text Input
```css
/* 기본 상태 */
border: 2px solid #E5E7EB;
padding: 12px 16px;
border-radius: 8px;
font-size: 16px;
background: #FFFFFF;

/* 포커스 상태 */
border-color: #2563EB;
box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);

/* 오류 상태 */
border-color: #EF4444;
box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.1);
```

#### Select Dropdown
```css
/* 기본 상태 */
border: 2px solid #E5E7EB;
padding: 12px 16px;
border-radius: 8px;
background: #FFFFFF;
cursor: pointer;

/* 열린 상태 */
border-color: #2563EB;
```

### 카드 (Cards)

#### Basic Card
```css
background: #FFFFFF;
border-radius: 12px;
box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
padding: 24px;
border: 1px solid #F3F4F6;

/* 호버 효과 */
box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
transform: translateY(-2px);
transition: all 0.2s ease;
```

#### Travel Card  
```css
background: #FFFFFF;
border-radius: 16px;
box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
overflow: hidden;
position: relative;

/* 이미지 영역 */
.card-image {
  width: 100%;
  height: 160px;
  object-fit: cover;
}

/* 콘텐츠 영역 */
.card-content {
  padding: 16px;
}

/* 진행바 */
.progress-bar {
  height: 4px;
  background: #E5E7EB;
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
}
```

### 네비게이션 (Navigation)

#### Header Navigation
```css
background: #FFFFFF;
border-bottom: 1px solid #E5E7EB;
padding: 12px 24px;
display: flex;
align-items: center;
justify-content: space-between;

/* 로고 */
.logo {
  font-size: 24px;
  font-weight: 700;
  color: #2563EB;
}

/* 사용자 프로필 */
.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
}
```

#### Bottom Navigation (Mobile)
```css
background: #FFFFFF;
border-top: 1px solid #E5E7EB;
padding: 8px 0;
position: fixed;
bottom: 0;
left: 0;
right: 0;
display: flex;
justify-content: space-around;
z-index: 100;

/* 네비게이션 아이템 */
.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px;
  min-width: 48px;
  min-height: 48px;
}

/* 활성 상태 */
.nav-item.active {
  color: #2563EB;
}
```

### 모달 & 다이얼로그

#### Modal Overlay
```css
position: fixed;
top: 0;
left: 0;
right: 0;
bottom: 0;
background: rgba(0, 0, 0, 0.5);
display: flex;
align-items: center;
justify-content: center;
z-index: 1000;
```

#### Modal Content  
```css
background: #FFFFFF;
border-radius: 16px;
padding: 32px;
max-width: 480px;
width: 90%;
max-height: 80vh;
overflow-y: auto;
box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
```

---

## 반응형 브레이크포인트

### 브레이크포인트 정의
```css
/* Mobile First 접근 */
/* Small devices (모바일) */
@media (min-width: 320px) { ... }

/* Medium devices (태블릿) */  
@media (min-width: 768px) { ... }

/* Large devices (데스크톱) */
@media (min-width: 1024px) { ... }

/* Extra large devices (큰 화면) */
@media (min-width: 1200px) { ... }
```

### 반응형 컴포넌트 예시

#### 그리드 시스템
```css
/* 모바일: 1열 */
.grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

/* 태블릿: 2열 */
@media (min-width: 768px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
  }
}

/* 데스크톱: 3열 */
@media (min-width: 1024px) {
  .grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 32px;
  }
}
```

#### 타이포그래피 스케일링
```css
/* 모바일 */
.page-title {
  font-size: 24px;
  line-height: 32px;
}

/* 태블릿 이상 */
@media (min-width: 768px) {
  .page-title {
    font-size: 32px;
    line-height: 40px;
  }
}
```

---

## 대상 서비스 특화 컴포넌트

### 여행 일정 관련

#### 진행바 컴포넌트
```css
.progress-stepper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 32px 0;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
}

.step-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  margin-bottom: 8px;
}

/* 완료 단계 */
.step.completed .step-circle {
  background: #10B981;
  color: #FFFFFF;
}

/* 현재 단계 */
.step.current .step-circle {
  background: #2563EB;
  color: #FFFFFF;
}

/* 미완료 단계 */
.step.pending .step-circle {
  background: #E5E7EB;
  color: #6B7280;
}
```

#### 여행지 카드
```css  
.destination-card {
  background: #FFFFFF;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  position: relative;
  cursor: pointer;
  transition: all 0.2s ease;
}

.destination-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.destination-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.destination-info {
  padding: 20px;
}

.destination-title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
}

.destination-period {
  font-size: 14px;
  color: #6B7280;
  margin-bottom: 12px;
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-traveling {
  background: #10B981;
  color: #FFFFFF;
}

.status-planning {
  background: #F59E0B;
  color: #FFFFFF;
}

.status-completed {
  background: #6B7280;
  color: #FFFFFF;
}
```

#### 장소 정보 카드
```css
.place-card {
  background: #FFFFFF;
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  transition: all 0.2s ease;
}

.place-card:hover {
  border-color: #2563EB;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.1);
}

.place-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.place-name {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.travel-time {
  font-size: 14px;
  color: #2563EB;
  font-weight: 500;
}

.place-rating {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.stars {
  color: #F59E0B;
}

.rating-text {
  font-size: 14px;
  color: #6B7280;
}

.place-address {
  font-size: 14px;
  color: #6B7280;
  margin-bottom: 16px;
}

.place-actions {
  display: flex;
  gap: 8px;
}

.action-button {
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-info {
  background: #EBF8FF;
  color: #2563EB;
  border: none;
}

.btn-direction {
  background: #F0FDF4;
  color: #10B981;
  border: none;
}

.btn-delete {
  background: #FEF2F2;
  color: #EF4444;
  border: none;
}
```

### AI 관련 UI 요소

#### AI 생성 진행 표시
```css
.ai-progress {
  background: #F9FAFB;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
}

.progress-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: conic-gradient(#2563EB var(--progress, 0%), #E5E7EB 0%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  position: relative;
}

.progress-circle::before {
  content: '';
  width: 60px;
  height: 60px;
  background: #F9FAFB;
  border-radius: 50%;
  position: absolute;
}

.progress-text {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
  z-index: 1;
}

.progress-status {
  font-size: 14px;
  color: #6B7280;
}
```

---

## 인터랙션 패턴

### 호버 효과
```css
/* 카드 호버 */
.card:hover {
  transform: translateY(-2px);  
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.2s ease;
}

/* 버튼 호버 */
.button:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

/* 링크 호버 */
.link:hover {
  color: #1E40AF;
  text-decoration: underline;
}
```

### 포커스 상태
```css
.focusable:focus {
  outline: 2px solid #2563EB;
  outline-offset: 2px;
}

.input:focus {
  border-color: #2563EB;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}
```

### 터치 인터랙션
```css
/* 터치 대상 최소 크기 */
.touch-target {
  min-width: 48px;
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 터치 피드백 */
.touch-feedback:active {
  transform: scale(0.95);
  transition: transform 0.1s ease;
}
```

### 애니메이션
```css
/* 페이드 인 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 슬라이드 업 */
@keyframes slideUp {
  from { 
    opacity: 0; 
    transform: translateY(20px);
  }
  to { 
    opacity: 1; 
    transform: translateY(0);
  }
}

/* 로딩 스피너 */
@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-spinner {
  animation: spin 1s linear infinite;
}
```

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 | 작성자 |
|------|------|-----------|--------|
| 1.0 | 2025-01-28 | 초기 스타일 가이드 작성 | 뷰어 |