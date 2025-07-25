/**
 * TripGen - 여행 일정 생성 서비스 공통 JavaScript
 */

// DOM 로드 완료 후 실행
document.addEventListener('DOMContentLoaded', function() {
  initMobileMenu();
  initFormValidation();
  initTooltips();
  initAnimations();
});

/**
 * 모바일 메뉴 초기화
 */
function initMobileMenu() {
  const menuToggle = document.querySelector('.mobile-menu-toggle');
  const navMenu = document.querySelector('.nav-menu');
  const overlay = document.querySelector('.mobile-overlay');
  
  if (!menuToggle || !navMenu) return;
  
  // 메뉴 토글 버튼 이벤트
  menuToggle.addEventListener('click', function() {
    navMenu.classList.toggle('active');
    if (overlay) {
      overlay.classList.toggle('active');
    }
    document.body.style.overflow = navMenu.classList.contains('active') ? 'hidden' : '';
  });
  
  // 오버레이 클릭 시 메뉴 닫기
  if (overlay) {
    overlay.addEventListener('click', function() {
      navMenu.classList.remove('active');
      overlay.classList.remove('active');
      document.body.style.overflow = '';
    });
  }
  
  // ESC 키로 메뉴 닫기
  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape' && navMenu.classList.contains('active')) {
      navMenu.classList.remove('active');
      if (overlay) overlay.classList.remove('active');
      document.body.style.overflow = '';
    }
  });
}

/**
 * 폼 유효성 검사 초기화
 */
function initFormValidation() {
  const forms = document.querySelectorAll('form[data-validate]');
  
  forms.forEach(form => {
    form.addEventListener('submit', function(e) {
      if (!validateForm(form)) {
        e.preventDefault();
      }
    });
    
    // 실시간 유효성 검사
    const inputs = form.querySelectorAll('input, textarea, select');
    inputs.forEach(input => {
      input.addEventListener('blur', function() {
        validateField(input);
      });
      
      input.addEventListener('input', function() {
        clearFieldError(input);
      });
    });
  });
}

/**
 * 폼 유효성 검사
 */
function validateForm(form) {
  const inputs = form.querySelectorAll('input[required], textarea[required], select[required]');
  let isValid = true;
  
  inputs.forEach(input => {
    if (!validateField(input)) {
      isValid = false;
    }
  });
  
  return isValid;
}

/**
 * 개별 필드 유효성 검사
 */
function validateField(field) {
  const value = field.value.trim();
  const type = field.type;
  const required = field.hasAttribute('required');
  
  // 필수 필드 검사
  if (required && !value) {
    showFieldError(field, '필수 입력 항목입니다.');
    return false;
  }
  
  // 타입별 유효성 검사
  switch (type) {
    case 'email':
      if (value && !isValidEmail(value)) {
        showFieldError(field, '올바른 이메일 형식이 아닙니다.');
        return false;
      }
      break;
      
    case 'tel':
      if (value && !isValidPhone(value)) {
        showFieldError(field, '올바른 전화번호 형식이 아닙니다.');
        return false;
      }
      break;
      
    case 'date':
      if (value && !isValidDate(value)) {
        showFieldError(field, '올바른 날짜를 입력해주세요.');
        return false;
      }
      break;
  }
  
  // 커스텀 패턴 검사
  const pattern = field.getAttribute('pattern');
  if (pattern && value && !new RegExp(pattern).test(value)) {
    showFieldError(field, field.getAttribute('data-error') || '입력 형식이 올바르지 않습니다.');
    return false;
  }
  
  // 최소/최대 길이 검사
  const minLength = field.getAttribute('minlength');
  const maxLength = field.getAttribute('maxlength');
  
  if (minLength && value.length < parseInt(minLength)) {
    showFieldError(field, `최소 ${minLength}자 이상 입력해주세요.`);
    return false;
  }
  
  if (maxLength && value.length > parseInt(maxLength)) {
    showFieldError(field, `최대 ${maxLength}자까지 입력 가능합니다.`);
    return false;
  }
  
  clearFieldError(field);
  return true;
}

/**
 * 필드 에러 표시
 */
function showFieldError(field, message) {
  field.classList.add('error');
  
  let errorElement = field.parentNode.querySelector('.form-error');
  if (!errorElement) {
    errorElement = document.createElement('span');
    errorElement.className = 'form-error';
    field.parentNode.appendChild(errorElement);
  }
  
  errorElement.textContent = message;
}

/**
 * 필드 에러 제거
 */
function clearFieldError(field) {
  field.classList.remove('error');
  
  const errorElement = field.parentNode.querySelector('.form-error');
  if (errorElement) {
    errorElement.remove();
  }
}

/**
 * 이메일 유효성 검사
 */
function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

/**
 * 전화번호 유효성 검사
 */
function isValidPhone(phone) {
  const phoneRegex = /^(\+82|0)?[1-9][0-9]{1,2}-?[0-9]{3,4}-?[0-9]{4}$/;
  return phoneRegex.test(phone.replace(/\s/g, ''));
}

/**
 * 날짜 유효성 검사
 */
function isValidDate(dateString) {
  const date = new Date(dateString);
  return date instanceof Date && !isNaN(date);
}

/**
 * 툴팁 초기화
 */
function initTooltips() {
  const tooltipElements = document.querySelectorAll('[data-tooltip]');
  
  tooltipElements.forEach(element => {
    element.addEventListener('mouseenter', showTooltip);
    element.addEventListener('mouseleave', hideTooltip);
    element.addEventListener('focus', showTooltip);
    element.addEventListener('blur', hideTooltip);
  });
}

/**
 * 툴팁 표시
 */
function showTooltip(e) {
  const element = e.target;
  const text = element.getAttribute('data-tooltip');
  const position = element.getAttribute('data-tooltip-position') || 'top';
  
  const tooltip = document.createElement('div');
  tooltip.className = `tooltip tooltip-${position}`;
  tooltip.textContent = text;
  tooltip.id = 'tooltip-' + Date.now();
  
  document.body.appendChild(tooltip);
  
  const rect = element.getBoundingClientRect();
  const tooltipRect = tooltip.getBoundingClientRect();
  
  let left, top;
  
  switch (position) {
    case 'top':
      left = rect.left + (rect.width - tooltipRect.width) / 2;
      top = rect.top - tooltipRect.height - 8;
      break;
    case 'bottom':
      left = rect.left + (rect.width - tooltipRect.width) / 2;
      top = rect.bottom + 8;
      break;
    case 'left':
      left = rect.left - tooltipRect.width - 8;
      top = rect.top + (rect.height - tooltipRect.height) / 2;
      break;
    case 'right':
      left = rect.right + 8;
      top = rect.top + (rect.height - tooltipRect.height) / 2;
      break;
  }
  
  tooltip.style.left = Math.max(8, Math.min(left, window.innerWidth - tooltipRect.width - 8)) + 'px';
  tooltip.style.top = Math.max(8, Math.min(top, window.innerHeight - tooltipRect.height - 8)) + 'px';
  
  element.setAttribute('data-tooltip-id', tooltip.id);
}

/**
 * 툴팁 숨기기
 */
function hideTooltip(e) {
  const element = e.target;
  const tooltipId = element.getAttribute('data-tooltip-id');
  
  if (tooltipId) {
    const tooltip = document.getElementById(tooltipId);
    if (tooltip) {
      tooltip.remove();
    }
    element.removeAttribute('data-tooltip-id');
  }
}

/**
 * 애니메이션 초기화
 */
function initAnimations() {
  // Intersection Observer for fade-in animations
  const observerOptions = {
    threshold: 0.1,
    rootMargin: '0px 0px -50px 0px'
  };
  
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('fade-in');
      }
    });
  }, observerOptions);
  
  document.querySelectorAll('.animate-on-scroll').forEach(el => {
    observer.observe(el);
  });
}

/**
 * 로딩 상태 표시
 */
function showLoading(element, text = '로딩 중...') {
  element.classList.add('loading');
  element.innerHTML = `
    <div class="loading-content">
      <div class="spinner"></div>
      <span>${text}</span>
    </div>
  `;
  element.disabled = true;
}

/**
 * 로딩 상태 해제
 */
function hideLoading(element, originalContent) {
  element.classList.remove('loading');
  element.innerHTML = originalContent;
  element.disabled = false;
}

/**
 * 토스트 알림 표시
 */
function showToast(message, type = 'info', duration = 3000) {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <div class="toast-content">
      <span>${message}</span>
      <button class="toast-close" onclick="this.parentElement.parentElement.remove()">&times;</button>
    </div>
  `;
  
  document.body.appendChild(toast);
  
  // 자동 제거
  setTimeout(() => {
    if (toast.parentElement) {
      toast.remove();
    }
  }, duration);
  
  // 클릭으로 제거
  toast.addEventListener('click', () => {
    toast.remove();
  });
}

/**
 * 모달 열기
 */
function openModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.add('active');
    document.body.style.overflow = 'hidden';
    
    // ESC 키로 닫기
    const closeOnEsc = (e) => {
      if (e.key === 'Escape') {
        closeModal(modalId);
        document.removeEventListener('keydown', closeOnEsc);
      }
    };
    
    document.addEventListener('keydown', closeOnEsc);
  }
}

/**
 * 여행 목록 정보 팝업 표시
 */
function showTravelListInfo() {
  TripGen.showModal(
    '여행보기 화면 (개발 예정)',
    `
    <div style="text-align: left; line-height: 1.6;">
        <h3 style="color: var(--primary-blue); margin-bottom: 16px;">📋 제공될 기능</h3>
        <ul style="margin: 0; padding-left: 20px;">
            <li><strong>전체 여행 목록</strong> - 생성된 모든 여행을 한 눈에 조회</li>
            <li><strong>여행 상태별 필터링</strong> - 계획 중, 진행 중, 완료된 여행 분류</li>
            <li><strong>여행 일정별 정렬</strong> - 날짜순, 생성순, 업데이트순 정렬</li>
            <li><strong>여행 검색 기능</strong> - 여행지명, 기간, 키워드로 검색</li>
            <li><strong>여행 관리 기능</strong> - 수정, 삭제, 복사, 공유 기능</li>
            <li><strong>여행 미리보기</strong> - 카드 형태로 여행 요약 정보 표시</li>
        </ul>
        <div style="margin-top: 20px; padding: 12px; background: #f0f9ff; border-radius: 8px; border-left: 4px solid var(--primary-blue);">
            <strong>💡 참고:</strong> 현재는 개별 여행 상세보기만 구현되어 있으며, 전체 여행 목록 화면은 향후 업데이트될 예정입니다.
        </div>
    </div>
    `,
    'info'
  );
}

/**
 * 모달 닫기
 */
function closeModal(modalId) {
  const modal = document.getElementById(modalId);
  if (modal) {
    modal.classList.remove('active');
    document.body.style.overflow = '';
  }
}

/**
 * 확인 다이얼로그
 */
function confirmDialog(message, callback) {
  if (confirm(message)) {
    callback();
  }
}

/**
 * 데이터 포맷팅 유틸리티
 */
const Utils = {
  // 날짜 포맷팅
  formatDate: (date, format = 'YYYY-MM-DD') => {
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    
    return format
      .replace('YYYY', year)
      .replace('MM', month)
      .replace('DD', day);
  },
  
  // 숫자 포맷팅 (천 단위 콤마)
  formatNumber: (number) => {
    return new Intl.NumberFormat('ko-KR').format(number);
  },
  
  // 거리 포맷팅
  formatDistance: (meters) => {
    if (meters < 1000) {
      return `${meters}m`;
    } else {
      return `${(meters / 1000).toFixed(1)}km`;
    }
  },
  
  // 시간 포맷팅
  formatDuration: (minutes) => {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    
    if (hours === 0) {
      return `${mins}분`;
    } else if (mins === 0) {
      return `${hours}시간`;
    } else {
      return `${hours}시간 ${mins}분`;
    }
  },
  
  // 디바운스
  debounce: (func, wait) => {
    let timeout;
    return function executedFunction(...args) {
      const later = () => {
        clearTimeout(timeout);
        func(...args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  },
  
  // 스로틀
  throttle: (func, limit) => {
    let inThrottle;
    return function() {
      const args = arguments;
      const context = this;
      if (!inThrottle) {
        func.apply(context, args);
        inThrottle = true;
        setTimeout(() => inThrottle = false, limit);
      }
    };
  }
};

/**
 * 모달 표시 (showToast를 활용한 확장된 모달)
 */
function showModal(title, content, type = 'info') {
  const modal = document.createElement('div');
  modal.className = `modal-overlay modal-${type}`;
  modal.innerHTML = `
    <div class="modal-content" style="
      max-width: 600px;
      width: 90%;
      max-height: 80vh;
      overflow-y: auto;
      background: white;
      border-radius: 16px;
      padding: 0;
      margin: auto;
      position: relative;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    ">
      <div style="
        padding: 24px 24px 16px;
        border-bottom: 1px solid #e5e7eb;
        display: flex;
        justify-content: space-between;
        align-items: center;
      ">
        <h2 style="
          margin: 0;
          font-size: 1.5rem;
          font-weight: 600;
          color: #1f2937;
        ">${title}</h2>
        <button class="modal-close" style="
          background: none;
          border: none;
          font-size: 1.5rem;
          cursor: pointer;
          color: #6b7280;
          padding: 4px;
          border-radius: 4px;
          transition: all 0.2s;
        " onmouseover="this.style.background='#f3f4f6'" onmouseout="this.style.background='none'">×</button>
      </div>
      <div style="padding: 24px;">
        ${content}
      </div>
    </div>
  `;
  
  modal.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 10000;
    padding: 16px;
  `;
  
  document.body.appendChild(modal);
  document.body.style.overflow = 'hidden';
  
  // 닫기 이벤트
  const closeModal = () => {
    modal.remove();
    document.body.style.overflow = '';
  };
  
  modal.querySelector('.modal-close').addEventListener('click', closeModal);
  modal.addEventListener('click', (e) => {
    if (e.target === modal) closeModal();
  });
  
  // ESC 키로 닫기
  const handleEsc = (e) => {
    if (e.key === 'Escape') {
      closeModal();
      document.removeEventListener('keydown', handleEsc);
    }
  };
  document.addEventListener('keydown', handleEsc);
}

/**
 * localStorage 기본값 설정 유틸리티 함수들
 */

// 기본 여행 설정 데이터
function getDefaultBasicSettings() {
  return {
    tripName: '유럽가족여행',
    destination: '독일 뮌헨',
    members: 4,
    budget: 500,
    accommodation: '뮌헨 마리엔플라츠 호텔',
    transportation: '자동차'
  };
}

// 기본 여행 일정 데이터 (뮌헨 2박, 잘츠부르크 3박, 돌로미티 5박)
function getDefaultTripSchedule() {
  return {
    tripStartDate: '2025-09-01',
    tripEndDate: '2025-09-11',
    startDate_1: '2025-09-01',
    endDate_1: '2025-09-03',
    destination_1: '독일 뮌헨',
    accommodation_1: '뮌헨 마리엔플라츠 호텔',
    stayNights_1: '2',
    checkInTime_1: '15',
    checkOutTime_1: '11',
    startDate_2: '2025-09-03',
    endDate_2: '2025-09-06',
    destination_2: '오스트리아 잘츠부르크',
    accommodation_2: '잘츠부르크 호텔 솔로스 되엠',
    stayNights_2: '3',
    checkInTime_2: '15',
    checkOutTime_2: '11',
    startDate_3: '2025-09-06',
    endDate_3: '2025-09-11',
    destination_3: '이탈리아 돌로미티',
    accommodation_3: '돌로미티 알파인 리조트',
    stayNights_3: '5',
    checkInTime_3: '15',
    checkOutTime_3: '11'
  };
}

// localStorage에 기본값 설정
function initializeDefaultData() {
  // 기본 설정 데이터 초기화
  if (!localStorage.getItem('basicSettingsData')) {
    localStorage.setItem('basicSettingsData', JSON.stringify(getDefaultBasicSettings()));
    console.log('기본 여행 설정 데이터가 설정되었습니다.');
  }
  
  // 여행 일정 데이터 초기화
  if (!localStorage.getItem('tripScheduleData')) {
    localStorage.setItem('tripScheduleData', JSON.stringify(getDefaultTripSchedule()));
    console.log('기본 여행 일정 데이터가 설정되었습니다.');
  }
  
  // 현재 여행 ID 설정
  if (!localStorage.getItem('currentTripId')) {
    localStorage.setItem('currentTripId', 'europe-family-trip');
    console.log('기본 여행 ID가 설정되었습니다.');
  }
}

// localStorage에서 데이터 가져오기 (기본값 포함)
function getBasicSettingsData() {
  const data = localStorage.getItem('basicSettingsData');
  if (data) {
    return JSON.parse(data);
  } else {
    const defaultData = getDefaultBasicSettings();
    localStorage.setItem('basicSettingsData', JSON.stringify(defaultData));
    return defaultData;
  }
}

function getTripScheduleData() {
  const data = localStorage.getItem('tripScheduleData');
  if (data) {
    return JSON.parse(data);
  } else {
    const defaultData = getDefaultTripSchedule();
    localStorage.setItem('tripScheduleData', JSON.stringify(defaultData));
    return defaultData;
  }
}

// 전역으로 노출
window.TripGen = {
  showLoading,
  hideLoading,
  showToast,
  openModal,
  closeModal,
  confirmDialog,
  showModal,
  Utils,
  // 기본값 설정 함수들 추가
  getDefaultBasicSettings,
  getDefaultTripSchedule,
  initializeDefaultData,
  getBasicSettingsData,
  getTripScheduleData
};