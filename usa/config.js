/**
 * Static-host API configuration.
 * SPLIT_HOST = true  -> frontend on a static host, API on a separate backend
 *                       (overrides below apply on any non-localhost host).
 * SPLIT_HOST = false -> everything on ONE Render Web Service (same origin):
 *                       no overrides are applied, the app auto-uses its own origin.
 */
(function () {
  const SPLIT_HOST = false;
  const isLocal =
    location.hostname === 'localhost' || location.hostname === '127.0.0.1';
  if (!SPLIT_HOST || isLocal) return;

  window.NT_API_BASE = 'https://nttrust-backend.onrender.com/api/v1';
  window.NT_AUTH_API = 'https://nttrust-backend.onrender.com/api/auth';
  window.NT_LOGIN_URL = 'login.html';
})();