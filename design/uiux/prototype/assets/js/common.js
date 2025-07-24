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

// 전역으로 노출
window.TripGen = {
  showLoading,
  hideLoading,
  showToast,
  openModal,
  closeModal,
  confirmDialog,
  Utils
};