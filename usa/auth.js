/**
 * Client session helpers — login.html + index.html (dashboard)
 */
(function (global) {
  const AUTH_KEY = 'nt_account_number';
  const VERIFIED_KEY = 'nt_login_verified';
  const PENDING_KEY = 'nt_pending_login';

  function isLoggedIn() {
    return !!(
      sessionStorage.getItem(AUTH_KEY) &&
      sessionStorage.getItem(VERIFIED_KEY) === 'true'
    );
  }

  function getAccountNumber() {
    return sessionStorage.getItem(AUTH_KEY) || '';
  }

  function completeLogin(accountNumber) {
    sessionStorage.removeItem(PENDING_KEY);
    sessionStorage.setItem(AUTH_KEY, accountNumber);
    sessionStorage.setItem(VERIFIED_KEY, 'true');
  }

  function setPendingLogin(accountNumber) {
    sessionStorage.setItem(PENDING_KEY, accountNumber);
  }

  function getPendingLogin() {
    return sessionStorage.getItem(PENDING_KEY) || '';
  }

  function clearPendingLogin() {
    sessionStorage.removeItem(PENDING_KEY);
  }

  function logout() {
    sessionStorage.removeItem(AUTH_KEY);
    sessionStorage.removeItem(VERIFIED_KEY);
    sessionStorage.removeItem(PENDING_KEY);
    const loginUrl = global.NT_LOGIN_URL || 'login.html';
    window.location.href = loginUrl;
  }

  function requireAuth() {
    if (!isLoggedIn() && !global.NT_ACCOUNT_NUMBER) {
      const loginUrl = global.NT_LOGIN_URL || 'login.html';
      window.location.replace(loginUrl);
      return false;
    }
    return true;
  }

  function resolveApiBase(pathSuffix) {
    if (global.NT_AUTH_API) {
      return global.NT_AUTH_API.replace(/\/$/, '') + pathSuffix;
    }
    if (global.location && global.location.protocol.startsWith('http')) {
      return global.location.origin + '/api/auth' + pathSuffix;
    }
    return 'http://localhost:80/api/auth' + pathSuffix;
  }

  global.NTAuth = {
    AUTH_KEY,
    VERIFIED_KEY,
    PENDING_KEY,
    isLoggedIn,
    getAccountNumber,
    completeLogin,
    setPendingLogin,
    getPendingLogin,
    clearPendingLogin,
    logout,
    requireAuth,
    resolveApiBase
  };
})(window);
