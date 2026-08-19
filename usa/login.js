(function () {
  const REMEMBER_KEY = 'nt_remember_user';

  const form = document.getElementById('loginForm');
  const userIdInput = document.getElementById('userId');
  const passwordInput = document.getElementById('password');
  const rememberInput = document.getElementById('rememberMe');
  const submitLoginBtn = document.getElementById('submitLogin');
  const alertEl = document.getElementById('loginAlert');
  const alertText = document.getElementById('loginAlertText');
  const capsWarning = document.getElementById('capsWarning');

  const otpModal = document.getElementById('loginOtpModal');
  const otpAccountLabel = document.getElementById('otpAccountLabel');
  const otpModalAlert = document.getElementById('otpModalAlert');
  const otpDigits = Array.from(document.querySelectorAll('.login-otp-digit'));
  const submitOtpBtn = document.getElementById('submitOtp');
  const resendOtpBtn = document.getElementById('resendOtp');
  const backBtn = document.getElementById('backToCredentials');
  const closeOtpBtn = document.getElementById('closeOtpModal');

  const savedUser = localStorage.getItem(REMEMBER_KEY);
  if (savedUser) {
    userIdInput.value = savedUser;
    rememberInput.checked = true;
  }

  function apiUrl(path) {
    return NTAuth.resolveApiBase(path);
  }

  function showFormError(message) {
    alertText.textContent = message;
    alertEl.classList.add('is-visible');
    alertEl.setAttribute('role', 'alert');
  }

  function hideFormError() {
    alertEl.classList.remove('is-visible');
    alertEl.removeAttribute('role');
  }

  function showOtpError(message) {
    otpModalAlert.textContent = message;
    otpModalAlert.hidden = !message;
  }

  function setButtonLoading(btn, loading) {
    btn.disabled = loading;
    btn.classList.toggle('is-loading', loading);
    btn.setAttribute('aria-busy', loading ? 'true' : 'false');
  }

  function getOtpCode() {
    return otpDigits.map(function (el) { return el.value.trim(); }).join('');
  }

  function clearOtpDigits() {
    otpDigits.forEach(function (el) { el.value = ''; });
  }

  function openOtpModal(accountNumber) {
    NTAuth.setPendingLogin(accountNumber);
    otpAccountLabel.textContent = accountNumber;
    clearOtpDigits();
    showOtpError('');
    otpModal.hidden = false;
    document.body.style.overflow = 'hidden';
    otpDigits[0].focus();
  }

  function closeOtpModal() {
    otpModal.hidden = true;
    document.body.style.overflow = '';
    NTAuth.clearPendingLogin();
    clearOtpDigits();
    showOtpError('');
  }

  otpDigits.forEach(function (input, index) {
    input.addEventListener('input', function () {
      input.value = input.value.replace(/\D/g, '').slice(0, 1);
      if (input.value && index < otpDigits.length - 1) {
        otpDigits[index + 1].focus();
      }
    });
    input.addEventListener('keydown', function (e) {
      if (e.key === 'Backspace' && !input.value && index > 0) {
        otpDigits[index - 1].focus();
      }
      if (e.key === 'Enter') {
        e.preventDefault();
        submitOtpBtn.click();
      }
    });
    input.addEventListener('paste', function (e) {
      e.preventDefault();
      const pasted = (e.clipboardData.getData('text') || '').replace(/\D/g, '').slice(0, 6);
      pasted.split('').forEach(function (ch, i) {
        if (otpDigits[i]) otpDigits[i].value = ch;
      });
      if (pasted.length >= 6) {
        otpDigits[5].focus();
      } else if (otpDigits[pasted.length]) {
        otpDigits[pasted.length].focus();
      }
    });
  });

  passwordInput.addEventListener('keyup', function (e) {
    const caps = e.getModifierState && e.getModifierState('CapsLock');
    capsWarning.classList.toggle('is-visible', !!caps);
  });

  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    hideFormError();

    const accountNumber = userIdInput.value.trim();
    const password = passwordInput.value;

    if (!accountNumber || !password) {
      showFormError('Enter your User ID and password to continue.');
      return;
    }

    setButtonLoading(submitLoginBtn, true);

    try {
      const res = await fetch(apiUrl('/login'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accountNumber, password })
      });

      const data = await res.json().catch(function () {
        return { success: false, message: 'Unable to reach the server. Please try again.' };
      });

      setButtonLoading(submitLoginBtn, false);

      if (!res.ok || !data.success) {
        showFormError(data.message || 'Invalid User ID or password. Please try again.');
        return;
      }

      passwordInput.value = '';
      openOtpModal(accountNumber);
    } catch (err) {
      setButtonLoading(submitLoginBtn, false);
      showFormError('Cannot reach the API at ' + apiUrl('') + '. Start Spring Boot on port 80, then open this page from http://localhost/usa/login.html');
    }
  });

  submitOtpBtn.addEventListener('click', async function () {
    showOtpError('');
    const accountNumber = NTAuth.getPendingLogin() || userIdInput.value.trim();
    const code = getOtpCode();

    if (!accountNumber) {
      showOtpError('Session expired. Sign in again with your password.');
      closeOtpModal();
      return;
    }

    if (code.length !== 6) {
      showOtpError('Enter all 6 digits of your verification code.');
      return;
    }

    setButtonLoading(submitOtpBtn, true);

    try {
      const res = await fetch(apiUrl('/login/verify-otp'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accountNumber, code })
      });

      const data = await res.json().catch(function () {
        return { success: false, message: 'Unable to verify code.' };
      });

      setButtonLoading(submitOtpBtn, false);

      if (!res.ok || !data.success) {
        showOtpError(data.message || 'Invalid or expired verification code.');
        otpDigits[0].focus();
        return;
      }

      if (rememberInput.checked) {
        localStorage.setItem(REMEMBER_KEY, accountNumber);
      } else {
        localStorage.removeItem(REMEMBER_KEY);
      }

      NTAuth.completeLogin(accountNumber);
      window.location.href = 'index.html';
    } catch (err) {
      setButtonLoading(submitOtpBtn, false);
      showOtpError('Network error while verifying code.');
    }
  });

  resendOtpBtn.addEventListener('click', async function () {
    showOtpError('');
    const accountNumber = NTAuth.getPendingLogin();
    if (!accountNumber) {
      showOtpError('Sign in with your password first.');
      return;
    }

    resendOtpBtn.disabled = true;

    try {
      const res = await fetch(apiUrl('/login/resend-otp'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accountNumber })
      });

      const data = await res.json().catch(function () {
        return { success: false };
      });

      if (!res.ok || !data.success) {
        showOtpError(data.message || 'Unable to resend. Sign in again with your password.');
      } else {
        showOtpError('');
        otpModalAlert.hidden = false;
        otpModalAlert.style.background = '#ecfdf5';
        otpModalAlert.style.borderColor = '#a7f3d0';
        otpModalAlert.style.color = '#047857';
        otpModalAlert.textContent = 'A new code was sent to your authenticator app. Open the app and enter the latest 6-digit code.';
        clearOtpDigits();
        otpDigits[0].focus();
      }
    } catch (err) {
      showOtpError('Network error while resending code.');
    } finally {
      resendOtpBtn.disabled = false;
    }
  });

  backBtn.addEventListener('click', closeOtpModal);
  closeOtpBtn.addEventListener('click', closeOtpModal);

  otpModal.querySelector('.login-otp-modal__backdrop').addEventListener('click', function () {
    /* keep modal open — must verify or use Back */
  });

  /* Security info carousel */
  const slides = document.querySelectorAll('.info-slide');
  const dots = document.querySelectorAll('.info-dots button');
  let slideIndex = 0;
  let carouselTimer;

  function goToSlide(index) {
    slideIndex = (index + slides.length) % slides.length;
    slides.forEach(function (slide, i) {
      slide.classList.toggle('is-active', i === slideIndex);
    });
    dots.forEach(function (dot, i) {
      dot.classList.toggle('is-active', i === slideIndex);
      dot.setAttribute('aria-selected', i === slideIndex ? 'true' : 'false');
    });
  }

  function startCarousel() {
    clearInterval(carouselTimer);
    carouselTimer = setInterval(function () {
      goToSlide(slideIndex + 1);
    }, 8000);
  }

  dots.forEach(function (dot, i) {
    dot.addEventListener('click', function () {
      goToSlide(i);
      startCarousel();
    });
  });

  if (slides.length > 0) {
    goToSlide(0);
    startCarousel();
  }
})();
