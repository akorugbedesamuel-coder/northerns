(function () {
  const TOKEN_KEY = 'nt_admin_token';
  const NAME_KEY = 'nt_admin_name';

  const API = (global.NT_ADMIN_API) || (window.location.protocol.startsWith('http')
    ? window.location.origin + '/api/admin'
    : 'http://localhost:80/api/admin');

  const $ = (id) => document.getElementById(id);
  let currentTab = 'approvals';
  let transfersCache = [];

  function token() { return sessionStorage.getItem(TOKEN_KEY); }
  function headers() {
    const h = { 'Content-Type': 'application/json' };
    if (token()) h['X-Admin-Token'] = token();
    return h;
  }

  async function api(path, opts) {
    const res = await fetch(API + path, Object.assign({ headers: headers() }, opts || {}));
    let data = null;
    try { data = await res.json(); } catch (e) { /* non-json */ }
    if (res.status === 401 && !path.includes('/login')) {
      logoutLocal();
      return Promise.reject(new Error('Session expired. Please sign in again.'));
    }
    if (!res.ok) {
      throw new Error((data && data.message) || ('HTTP ' + res.status));
    }
    return data;
  }

  function fmt(n, withSymbol) {
    const x = Number(n || 0);
    const s = Math.abs(x).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    return (withSymbol ? (x < 0 ? '-$' : '$') : '') + s;
  }

  function shortDate(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) + ' ' +
      d.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  }

  function statusClass(status) {
    const s = (status || '').toLowerCase();
    if (s.includes('settled') || s === 'success') return 'admin-status--settled';
    if (s.includes('hold') || s.includes('awaiting') || s.includes('pending') || s.includes('blocked')) return 'admin-status--hold';
    if (s.includes('fail') || s.includes('returned') || s.includes('reversed')) return 'admin-status--failed';
    return 'admin-status--processing';
  }

  function riskClass(level) {
    const l = (level || '').toLowerCase();
    if (l === 'critical') return 'admin-risk--critical';
    if (l === 'high') return 'admin-risk--high';
    if (l === 'medium') return 'admin-risk--medium';
    return 'admin-risk--low';
  }

  /* ——— Toast ——— */
  function toast(msg, isError) {
    const el = $('adminToast');
    el.textContent = msg;
    el.hidden = false;
    el.className = 'admin-toast ' + (isError ? 'is-error' : 'is-success');
    clearTimeout(el._t);
    el._t = setTimeout(() => { el.hidden = true; }, 3200);
  }

  /* ——— Login ——— */
  function showLoginError(msg) {
    const el = $('adminLoginAlert');
    el.textContent = msg;
    el.hidden = !msg;
  }

  $('adminLoginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    showLoginError('');
    const accountNumber = $('adminUserId').value.trim();
    const password = $('adminPassword').value;
    if (!accountNumber || !password) { showLoginError('Enter your admin User ID and password.'); return; }
    const btn = $('adminLoginBtn');
    btn.disabled = true;
    try {
      const res = await api('/login', {
        method: 'POST',
        body: JSON.stringify({ accountNumber, password })
      });
      if (!res || !res.success) { showLoginError((res && res.message) || 'Invalid admin credentials.'); return; }
      sessionStorage.setItem(TOKEN_KEY, res.token);
      sessionStorage.setItem(NAME_KEY, res.name || accountNumber);
      enterConsole();
    } catch (err) {
      showLoginError(err.message || 'Cannot reach the admin API.');
    } finally {
      btn.disabled = false;
    }
  });

  $('logoutBtn').addEventListener('click', async () => {
    try { await api('/logout', { method: 'POST' }); } catch (e) { /* ignore */ }
    logoutLocal();
  });

  function logoutLocal() {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(NAME_KEY);
    $('consoleView').hidden = true;
    $('loginView').hidden = false;
    $('adminPassword').value = '';
  }

  function enterConsole() {
    $('loginView').hidden = true;
    $('consoleView').hidden = false;
    $('adminSessionName').textContent = sessionStorage.getItem(NAME_KEY) || 'Admin';
    loadConsole();
  }

  /* ——— Console ——— */
  async function loadConsole() {
    try {
      const [overview, approvals, transfers, accounts] = await Promise.all([
        api('/overview'),
        api('/approvals/pending'),
        api('/transfers'),
        api('/accounts')
      ]);
      renderStats(overview);
      renderApprovals(approvals);
      transfersCache = transfers || [];
      renderTransfers(transfersCache);
      renderAccounts(accounts || []);
    } catch (err) {
      toast(err.message, true);
    }
  }

  function renderStats(o) {
    if (!o) return;
    $('statPending').textContent = o.pendingApprovals || 0;
    $('statTotal').textContent = o.totalTransfers || 0;
    $('statVolume').textContent = fmt(o.totalVolume, true);
    $('statSettled').textContent = o.settled || 0;
    $('statHeld').textContent = o.held || 0;
    $('statFailed').textContent = o.failed || 0;
    const badge = $('pendingBadge');
    badge.textContent = (o.pendingApprovals || 0) + ' pending';
    badge.className = 'admin-badge' + ((o.pendingApprovals || 0) === 0 ? ' is-clear' : '');
  }

  function renderApprovals(items) {
    const body = $('approvalsBody');
    if (!items || !items.length) {
      body.innerHTML = '<tr><td colspan="9" class="admin-empty">Queue is clear — no transfers awaiting approval.</td></tr>';
      return;
    }
    body.innerHTML = items.map(t => `
      <tr>
        <td class="mono">${t.reference || '—'}</td>
        <td>${shortDate(t.createdAt)}</td>
        <td>${esc(t.user || '—')}<br><span style="color:var(--nt-muted);font-size:11px;">${esc(t.userAccountNumber || '')}</span></td>
        <td>${esc(t.type || '—')}</td>
        <td>${esc(t.counterparty || '—')}</td>
        <td style="font-weight:700;">${esc(t.currency || 'USD')} ${fmt(t.amount)}</td>
        <td><span class="admin-risk ${riskClass(t.riskLevel)}">${t.riskScore != null ? t.riskScore + ' · ' : ''}${esc(t.riskLevel || '—')}</span></td>
        <td><span class="admin-status ${statusClass(t.displayStatus)}">${esc(t.displayStatus || '—')}</span></td>
        <td style="white-space:nowrap;">
          <button class="btn-action approve" onclick="window.NTAdmin.approve('${t.reference}')">Approve</button>
          <button class="btn-action reject" onclick="window.NTAdmin.reject('${t.reference}')">Reject</button>
          <button class="btn-action escalate" onclick="window.NTAdmin.escalate('${t.reference}')">Escalate</button>
        </td>
      </tr>`).join('');
  }

  function renderTransfers(items) {
    const body = $('transfersBody');
    if (!items || !items.length) {
      body.innerHTML = '<tr><td colspan="8" class="admin-empty">No transfers found.</td></tr>';
      return;
    }
    body.innerHTML = items.map(t => `
      <tr>
        <td class="mono">${t.reference || '—'}</td>
        <td>${shortDate(t.createdAt)}</td>
        <td>${esc(t.user || '—')}</td>
        <td>${esc(t.type || '—')}</td>
        <td>${esc(t.counterparty || '—')}</td>
        <td style="font-weight:700;">${esc(t.currency || 'USD')} ${fmt(t.amount)}</td>
        <td><span class="admin-status ${statusClass(t.displayStatus)}">${esc(t.displayStatus || '—')}</span></td>
        <td>${t.pendingApproval ? '<span class="admin-status admin-status--hold">Awaiting</span>' : '<span style="color:var(--nt-muted);">—</span>'}</td>
      </tr>`).join('');
  }

  function renderAccounts(items) {
    const body = $('accountsBody');
    if (!items || !items.length) {
      body.innerHTML = '<tr><td colspan="6" class="admin-empty">No accounts found.</td></tr>';
      return;
    }
    body.innerHTML = items.map(a => `
      <tr>
        <td class="mono">${esc(a.accountNumber || '—')}</td>
        <td>${esc(a.owner || '—')}</td>
        <td>${esc(a.productKey || '—')}</td>
        <td>${esc(a.currency || '—')}</td>
        <td style="font-weight:700;">${fmt(a.balance)}</td>
        <td>${fmt(a.availableBalance)}</td>
      </tr>`).join('');
  }

  function esc(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }

  async function doAction(reference, action) {
    try {
      const res = await api('/approvals/' + encodeURIComponent(reference) + '/' + action, { method: 'POST' });
      toast(res && res.message ? res.message : 'Action applied');
      await loadConsole();
    } catch (err) {
      toast(err.message, true);
    }
  }

  window.NTAdmin = {
    approve: (ref) => doAction(ref, 'approve'),
    reject: (ref) => doAction(ref, 'reject'),
    escalate: (ref) => doAction(ref, 'escalate')
  };

  /* ——— Tabs ——— */
  document.querySelectorAll('.admin-tab').forEach(btn => {
    btn.addEventListener('click', () => {
      currentTab = btn.getAttribute('data-tab');
      document.querySelectorAll('.admin-tab').forEach(b => b.classList.toggle('is-active', b === btn));
      document.querySelectorAll('.admin-tab-panel').forEach(p => p.classList.toggle('is-active', p.id === 'tab-' + currentTab));
    });
  });

  $('refreshConsoleBtn').addEventListener('click', () => loadConsole().catch(() => {}));

  $('transfersSearch').addEventListener('input', (e) => {
    const q = (e.target.value || '').toLowerCase();
    renderTransfers(transfersCache.filter(t => {
      return [t.reference, t.user, t.userAccountNumber, t.counterparty, t.type, t.displayStatus]
        .some(v => v && String(v).toLowerCase().includes(q));
    }));
  });

  /* ——— Boot ——— */
  if (token()) {
    enterConsole();
  } else {
    $('loginView').hidden = false;
  }
})();