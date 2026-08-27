/**
 * Northern Trust — live API. Sign in at /usa/login.html first.
 */
(function (global) {
  if (typeof global.NTAuth !== 'undefined' && !global.NTAuth.isLoggedIn() && !global.NT_ACCOUNT_NUMBER) {
    if (typeof window !== 'undefined' && !window.location.pathname.includes('login.html')) {
      window.location.replace('login.html');
    }
    return;
  }

  const API_BASE = global.NT_API_BASE || (global.location && global.location.protocol.startsWith('http')
    ? global.location.origin + '/api/v1'
    : 'http://localhost:80/api/v1');
  const ACCOUNT = global.NT_ACCOUNT_NUMBER ||
    (typeof global.NTAuth !== 'undefined' ? global.NTAuth.getAccountNumber() : '') ||
    sessionStorage.getItem('nt_account_number') || '';

  async function apiGet(path, params) {
    const url = new URL(API_BASE + path);
    url.searchParams.set('accountNumber', ACCOUNT);
    if (params) Object.entries(params).forEach(([k, v]) => { if (v != null && v !== '') url.searchParams.set(k, v); });
    const res = await fetch(url.toString());
    if (!res.ok) throw new Error('HTTP ' + res.status + ': ' + (await res.text()));
    return res.json();
  }

  async function apiPost(path, body, params) {
    const url = new URL(API_BASE + path);
    url.searchParams.set('accountNumber', ACCOUNT);
    if (params) Object.entries(params).forEach(([k, v]) => { if (v != null) url.searchParams.set(k, v); });
    const res = await fetch(url.toString(), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  }

  async function apiPatch(path, params) {
    const url = new URL(API_BASE + path);
    url.searchParams.set('accountNumber', ACCOUNT);
    if (params) Object.entries(params).forEach(([k, v]) => { if (v != null) url.searchParams.set(k, v); });
    const res = await fetch(url.toString(), { method: 'PATCH' });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  }

  const state = {
    overview: null,
    profile: null,
    balances: null,
    beneficiaries: null,
    transferHistory: null,
    statements: {},
    pendingApprovals: null,
    analytics: null,
    notifications: null,
    connected: false,
    ready: false
  };

  function mergeAnalyticsState(next, connected) {
    const existing = global.analyticsData || {};
    return {
      ...existing,
      kpis: next?.kpis || existing.kpis || {},
      charts: next?.charts || existing.charts || {},
      insights: Array.isArray(next?.insights) ? next.insights : (Array.isArray(existing.insights) ? existing.insights : []),
      transferBreakdown: next?.transferBreakdown || existing.transferBreakdown || {},
      tables: next?.tables || existing.tables || {},
      filters: existing.filters || {
        timeRange: '7d',
        accountScope: 'all',
        currency: 'all',
        txType: 'all'
      },
      connected: connected != null ? connected : (existing.connected || false)
    };
  }

  global.ntAccountBalances = {
    checking: 318750.00,
    savings: 35180.38,
    credit: 200.00,
    invest: 169400000.00
  };

  function fmtMoney(n) {
    const x = Number(n);
    const neg = x < 0;
    return (neg ? '-' : '') + '$' + Math.abs(x).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  function formatNTDate(dateStr) {
    if (!dateStr) return '';
    if (dateStr.includes('-')) {
      const parts = dateStr.split('-');
      const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
      const mIdx = parseInt(parts[1], 10) - 1;
      return `${months[mIdx]} ${parseInt(parts[2], 10)}`;
    }
    return dateStr.split(',')[0];
  }

  function mapStatements(list, tab) {
    if (!list || !list.length) return [];
    if (tab === 'checking') {
      return list.map(s => ({
        date: s.date, desc: s.description, amount: s.amount, balance: s.balanceAfter,
        channel: s.channel, type: s.type
      }));
    }
    if (tab === 'savings') {
      return list.map(s => ({
        date: s.date, type: s.type, amount: s.amount, rate: s.rate || '4.50%', balance: s.balanceAfter
      }));
    }
    if (tab === 'credit') {
      return list.map(s => ({
        date: s.date, type: s.type, charged: s.charged, payment: s.payment,
        balance: s.balanceAfter, credit: s.creditAvailable
      }));
    }
    if (tab === 'invest') {
      return list.map(s => ({
        date: s.date, asset: s.asset || s.description, type: s.type,
        units: s.units, price: s.price, value: s.amount, balance: s.balanceAfter
      }));
    }
    return list.map(s => ({
      date: s.date, desc: s.description, source: s.source, type: s.type,
      amount: s.amount, balance: s.balanceAfter, status: s.status, channel: s.channel
    }));
  }

  function applyGlobalMocks() {
    global.beneficiariesMock = mapBeneficiaries(state.beneficiaries);
    if (state.transferHistory && state.transferHistory.items) {
      global.masterTxLedger = state.transferHistory.items.map(tx => ({
        id: tx.id, date: tx.date, type: tx.type, counterparty: tx.counterparty,
        source: tx.source, amount: tx.amount, currency: tx.currency || 'USD', status: tx.status
      }));
    }
    if (state.pendingApprovals) {
      global.mockPendingApprovals = state.pendingApprovals.map(a => ({
        id: a.id, type: a.type, amount: a.amount, currency: a.currency,
        usdEquivalent: a.usdEquivalent, source: a.source, beneficiary: a.beneficiary,
        country: a.country, riskScore: a.riskScore, riskLevel: a.riskLevel,
        status: a.status, timestamp: a.timestamp,
        flags: a.flags || [], compliance: a.compliance || {}, history: a.history || []
      }));
    }
    global.unifiedMock = mapStatements(state.statements.unified, 'unified');
    global.checkingMock = mapStatements(state.statements.checking, 'checking');
    global.savingsMock = mapStatements(state.statements.savings, 'savings');
    global.creditMock = mapStatements(state.statements.credit, 'credit');
    global.investMock = mapStatements(state.statements.invest, 'invest');
    if (state.overview && state.overview.recentActivity) {
      const uniqueItems = [];
      const seen = new Set();
      state.overview.recentActivity.forEach(r => {
        const key = `${r.date}|${r.description}|${r.amount}`;
        if (!seen.has(key)) {
          seen.add(key);
          uniqueItems.push({
            date: r.date, desc: r.description, recipient: r.counterparty,
            amount: r.amount, type: r.type, typeClass: r.amount < 0 ? 'type-card' : 'type-wire'
          });
        }
      });
      global.transactionsMock = uniqueItems;
    }
    if (state.analytics) {
      global.analyticsData = mergeAnalyticsState(state.analytics, true);
    }
  }

  function mapBeneficiaries(list) {
    return (list || []).map(b => ({
      beneficiaryId: b.beneficiaryId, type: b.type, displayName: b.displayName,
      relationship: b.relationship, destinationDetails: b.destinationDetails,
      transferLimits: b.transferLimits, status: b.status, isTrusted: b.isTrusted,
      createdAt: b.createdAt, lastUsedAt: b.lastUsedAt, trustLevel: b.trustLevel
    }));
  }

  function syncBalances() {
    if (!state.balances) return;
    Object.keys(state.balances).forEach(key => {
      const b = state.balances[key];
      global.ntAccountBalances[key] = Number(b.available != null ? b.available : b.balance);
    });
  }

  function abbrevDevice(userAgent) {
    if (!userAgent) return '';
    const ua = userAgent;
    const browser =
      /Edg\//i.test(ua) ? 'EDG' :
      /OPR\/|Opera/i.test(ua) ? 'OPR' :
      /Chrome\/|CriOS\//i.test(ua) ? 'CHR' :
      /Firefox\/|FxiOS\//i.test(ua) ? 'FFX' :
      /SamsungBrowser\//i.test(ua) ? 'SGB' :
      /Version\/.*Safari\//i.test(ua) ? 'SAF' :
      /MSIE|Trident\//i.test(ua) ? 'IE' : 'BRW';
    const os =
      /Windows/i.test(ua) ? 'WIN' :
      /iPhone|iPad|iPod/i.test(ua) ? 'iOS' :
      /Mac OS X/i.test(ua) ? 'MAC' :
      /Android/i.test(ua) ? 'AND' :
      /Linux/i.test(ua) ? 'LNX' : 'OS?';
    const type =
      /Android|iPhone/i.test(ua) && /Mobile/i.test(ua) ? 'MOB' :
      /iPad|Tablet|Silk\//i.test(ua) ? 'TAB' : 'DSK';
    return browser + '/' + os + '/' + type;
  }

  function hydrateSession() {
    const meta = document.getElementById('headerClientMeta');
    if (!meta) return;
    const p = state.profile;
    let login = 'May 24, 2026, 9:14 AM CT';
    if (p && p.lastLoginAt) {
      try {
        login = new Intl.DateTimeFormat('en-US', {
          dateStyle: 'medium', timeStyle: 'short', timeZone: 'America/Chicago'
        }).format(new Date(p.lastLoginAt)) + ' CT';
      } catch (e) { /* keep default */ }
    }
    const loc = (p && p.lastLoginLocation) || 'Chicago, IL';
    const device = abbrevDevice(p && p.lastLoginUserAgent);
    meta.innerHTML = 'Last sign-in: ' + login + ' · ' + loc +
      (device ? ' · ' + device : '') +
      ' · <a href="#" id="reportSuspiciousLink" style="color:#3b82f6;text-decoration:none;font-weight:600;">Report suspicious activity</a>';
  }

  function hydrateHeader() {
    const p = state.profile || (state.overview && state.overview.profile) || (state.overview && state.overview.client);
    if (!p) {
      hydrateSession();
      return;
    }
    const name = p.displayName || (p.firstName + ' ' + p.lastName);
    const el = document.getElementById('headerClientName');
    if (el) el.textContent = name;
    hydrateSession();
  }

  function showConnectionBanner(msg) {
    const b = document.getElementById('ntConnectionBanner');
    const m = document.getElementById('ntConnectionBannerMsg');
    if (b && m) {
      m.textContent = msg || 'Unable to reach banking services. Please try again later.';
      b.classList.add('visible');
    }
  }

  function hideConnectionBanner() {
    const b = document.getElementById('ntConnectionBanner');
    if (b) b.classList.remove('visible');
  }

  const BALANCE_MASK_TEXT = '****';

  function isBalanceVisible(balanceId) {
    return sessionStorage.getItem(`nt_balance_${balanceId}_visible`) === 'true';
  }

  function setBalanceVisible(balanceId, visible) {
    sessionStorage.setItem(`nt_balance_${balanceId}_visible`, visible ? 'true' : 'false');
  }

  function applySensitiveBalance(balanceId) {
    const visible = isBalanceVisible(balanceId);
    // Find all elements with this data-balance-id
    document.querySelectorAll(`[data-balance-id="${balanceId}"]`).forEach(el => {
      if (el.classList.contains('nt-sensitive-balance')) {
        const real = el.getAttribute('data-real-value');
        if (real) {
          el.textContent = visible ? real : BALANCE_MASK_TEXT;
          el.classList.toggle('stat-value--masked', !visible);
        }
      }
      if (el.classList.contains('balance-visibility-toggle')) {
        el.classList.toggle('is-visible', visible);
        el.setAttribute('aria-label', visible ? 'Hide balance' : 'Show balance');
        el.setAttribute('title', visible ? 'Hide balance' : 'Show balance');
      }
    });
  }

  function applyAllSensitiveBalances() {
    // Get all unique data-balance-id values
    const balanceIds = new Set();
    document.querySelectorAll('[data-balance-id]').forEach(el => {
      const id = el.getAttribute('data-balance-id');
      if (id) balanceIds.add(id);
    });
    balanceIds.forEach(id => applySensitiveBalance(id));
  }

  function setSensitiveBalance(el, formattedValue) {
    if (!el) return;
    el.setAttribute('data-real-value', formattedValue);
    el.classList.add('nt-sensitive-balance');
    const balanceId = el.getAttribute('data-balance-id');
    const visible = balanceId ? isBalanceVisible(balanceId) : false;
    el.textContent = visible ? formattedValue : BALANCE_MASK_TEXT;
    el.classList.toggle('stat-value--masked', !visible);
  }

  let pendingBalanceId = null;

  function initBalanceVisibilityToggles() {
    document.querySelectorAll('[data-balance-toggle]').forEach(btn => {
      if (btn._ntBalanceToggleBound) return;
      btn._ntBalanceToggleBound = true;
      btn.addEventListener('click', async e => {
        e.preventDefault();
        e.stopPropagation();
        const balanceId = btn.getAttribute('data-balance-id');
        if (!balanceId) return;
        const currentVisible = isBalanceVisible(balanceId);
        
        if (currentVisible) {
          setBalanceVisible(balanceId, false);
          applySensitiveBalance(balanceId);
        } else {
          pendingBalanceId = balanceId;
          try {
            await apiPost('/otp/request', null, { purpose: 'VIEW_BALANCE' });
            openBalanceOtpModal();
          } catch (err) {
            console.error('Failed to request OTP', err);
            if (!state.connected) {
              setBalanceVisible(balanceId, true);
              applySensitiveBalance(balanceId);
            } else {
              alert('Unable to request verification code. Please try again.');
            }
          }
        }
      });
    });
    applyAllSensitiveBalances();
    initBalanceOtpModal();
  }

  function openBalanceOtpModal() {
    const modal = document.getElementById('balanceOtpModal');
    if (!modal) return;
    modal.hidden = false;
    document.body.style.overflow = 'hidden';
    const digits = Array.from(modal.querySelectorAll('.login-otp-digit'));
    digits.forEach(el => el.value = '');
    const alertEl = document.getElementById('balanceOtpModalAlert');
    if (alertEl) {
      alertEl.hidden = true;
      alertEl.style.background = '';
      alertEl.style.borderColor = '';
      alertEl.style.color = '';
    }
    if (digits[0]) digits[0].focus();
  }

  function closeBalanceOtpModal() {
    const modal = document.getElementById('balanceOtpModal');
    if (!modal) return;
    modal.hidden = true;
    document.body.style.overflow = '';
    pendingBalanceId = null;
  }

  function initBalanceOtpModal() {
    const modal = document.getElementById('balanceOtpModal');
    if (!modal || modal._ntOtpBound) return;
    modal._ntOtpBound = true;

    const digits = Array.from(modal.querySelectorAll('.login-otp-digit'));
    const submitBtn = document.getElementById('submitBalanceOtp');
    const resendBtn = document.getElementById('resendBalanceOtp');
    const cancelBtn = document.getElementById('cancelBalanceOtp');
    const closeBtn = document.getElementById('closeBalanceOtpModal');
    const alertEl = document.getElementById('balanceOtpModalAlert');

    function showError(msg) {
      if (!alertEl) return;
      alertEl.textContent = msg;
      alertEl.hidden = !msg;
      alertEl.style.background = '';
      alertEl.style.borderColor = '';
      alertEl.style.color = '';
    }

    digits.forEach((input, index) => {
      input.addEventListener('input', () => {
        input.value = input.value.replace(/\D/g, '').slice(0, 1);
        if (input.value && index < digits.length - 1) {
          digits[index + 1].focus();
        }
      });
      input.addEventListener('keydown', e => {
        if (e.key === 'Backspace' && !input.value && index > 0) {
          digits[index - 1].focus();
        }
        if (e.key === 'Enter') {
          e.preventDefault();
          submitBtn.click();
        }
      });
      input.addEventListener('paste', e => {
        e.preventDefault();
        const pasted = (e.clipboardData.getData('text') || '').replace(/\D/g, '').slice(0, 6);
        pasted.split('').forEach((ch, i) => {
          if (digits[i]) digits[i].value = ch;
        });
        if (pasted.length >= 6) {
          digits[5].focus();
        } else if (digits[pasted.length]) {
          digits[pasted.length].focus();
        }
      });
    });

    submitBtn.addEventListener('click', async () => {
      showError('');
      const code = digits.map(el => el.value.trim()).join('');
      if (code.length !== 6) {
        showError('Enter all 6 digits of your verification code.');
        return;
      }

      submitBtn.disabled = true;
      submitBtn.classList.add('is-loading');

      try {
        const res = await apiPost('/otp/verify', null, { code, purpose: 'VIEW_BALANCE' });
        if (res && res.valid) {
          if (pendingBalanceId) {
            setBalanceVisible(pendingBalanceId, true);
            applySensitiveBalance(pendingBalanceId);
          }
          closeBalanceOtpModal();
        } else {
          showError(res.message || 'Invalid or expired verification code.');
          digits[0].focus();
        }
      } catch (err) {
        showError('Network error while verifying code.');
      } finally {
        submitBtn.disabled = false;
        submitBtn.classList.remove('is-loading');
      }
    });

    resendBtn.addEventListener('click', async () => {
      showError('');
      resendBtn.disabled = true;
      try {
        await apiPost('/otp/request', null, { purpose: 'VIEW_BALANCE' });
        alertEl.hidden = false;
        alertEl.style.background = '#ecfdf5';
        alertEl.style.borderColor = '#a7f3d0';
        alertEl.style.color = '#047857';
        alertEl.textContent = 'A new code was sent to your authenticator app.';
        digits.forEach(el => el.value = '');
        digits[0].focus();
      } catch (err) {
        showError('Network error while resending code.');
      } finally {
        resendBtn.disabled = false;
      }
    });

    cancelBtn.addEventListener('click', closeBalanceOtpModal);
    closeBtn.addEventListener('click', closeBalanceOtpModal);
  }

  function hydrateOverview() {
    const o = state.overview;
    if (!o) return;
    const portfolioEl = document.getElementById('statTotalPortfolio');
    const availableEl = document.getElementById('statAvailableBalance');
    if (portfolioEl && o.summary) setSensitiveBalance(portfolioEl, fmtMoney(o.summary.totalPortfolioBalance));
    if (availableEl && o.summary) setSensitiveBalance(availableEl, fmtMoney(o.summary.availableBalance));
    const pendingEl = document.querySelector('.stat-card:nth-child(3) .stat-value');
    if (pendingEl && o.summary) pendingEl.textContent = fmtMoney(o.summary.pendingSettlements);
    const mom = document.querySelector('.badge-positive');
    if (mom && o.summary) mom.textContent = '+' + o.summary.monthOverMonthChangePct + '%';

    const cardMap = { checking: 'subCardChecking', savings: 'subCardSavings', credit: 'subCardCredit', invest: 'subCardInvest' };
    (o.accounts || []).forEach(acc => {
      const el = document.getElementById(cardMap[acc.key]);
      if (!el) return;
      const val = el.querySelector('.sub-account-value');
      if (val) {
        if (acc.key === 'invest') val.textContent = fmtMoney(acc.marketValue);
        else if (acc.key === 'credit') val.textContent = fmtMoney(acc.availableCredit);
        else val.textContent = fmtMoney(acc.balance);
      }
      const rows = el.querySelectorAll('.sub-account-info-row span');
      if (acc.key === 'checking' && rows.length >= 2) {
        if (acc.ledgerBalance != null) rows[0].textContent = 'Balance: ' + fmtMoney(acc.ledgerBalance);
        if (acc.pendingAmount != null) rows[1].textContent = 'Pending: ' + fmtMoney(acc.pendingAmount);
      }
      if (acc.key === 'savings' && rows.length >= 1 && acc.apyPct) {
        rows[0].textContent = 'APY: ' + acc.apyPct + '% · Earned: ' + fmtMoney(acc.earnedThisPeriod);
      }
      if (acc.key === 'credit' && rows.length >= 1 && acc.owed != null) {
        rows[0].textContent = 'Owed: ' + fmtMoney(acc.owed) + ' · Limit: ' + fmtMoney(acc.limit);
      }
      if (acc.key === 'invest' && rows.length >= 1) {
        rows[0].textContent = 'Today: ' + fmtMoney(acc.todayChange) + ' (' + acc.todayChangePct + '%) · ROI ' + acc.roiPct + '%';
      }
    });

    const tbody = document.getElementById('activityTableBody');
    if (tbody && global.transactionsMock && global.transactionsMock.length) {
      tbody.innerHTML = global.transactionsMock.map(tx => `
        <tr>
          <td>${formatNTDate(tx.date)}</td>
          <td><span style="font-weight: 600;">${tx.desc}</span></td>
          <td><span style="font-size: 11px; color: #64748b;">Checking Account</span></td>
          <td><span class="type-label ${tx.typeClass}">${tx.type}</span></td>
          <td style="text-align: right;" class="${tx.amount > 0 ? 'amount-credit' : 'amount-debit'}">
            ${tx.amount > 0 ? '+' : ''}${Number(tx.amount).toLocaleString(undefined, { minimumFractionDigits: 2 })} USD
          </td>
        </tr>`).join('');
    }

    hydrateHeader();
    if (global.NT_updateBalancesAsOf) global.NT_updateBalancesAsOf();

    if ((global.currentActiveView === 'Overview' || !global.currentActiveView) &&
        typeof global.switchActiveSubAccount === 'function') {
      global.switchActiveSubAccount(global._activeSubAccount || 'checking', false, { force: true });
    }

    initBalanceVisibilityToggles();
  }

  async function loadStatementsAll() {
    const tabs = ['unified', 'checking', 'savings', 'credit', 'invest'];
    const results = await Promise.allSettled(tabs.map(async tab => {
      state.statements[tab] = await apiGet('/statements', { tab });
    }));
    results.forEach((r, i) => {
      if (r.status === 'rejected') console.warn('[NT API] Statements load failed for', tabs[i], r.reason?.message);
    });
  }

  async function loadDashboard() {
    const loading = document.getElementById('ntLoadingBanner');
    if (loading) loading.style.display = 'block';
    try {
      const core = await Promise.allSettled([
        apiGet('/dashboard/overview'),
        apiGet('/client/profile'),
        apiGet('/accounts/balances'),
        apiGet('/beneficiaries'),
        apiGet('/approvals/pending')
      ]);

      const [overview, profile, balances, beneficiaries, pendingApprovals] = core.map(r =>
        r.status === 'fulfilled' ? r.value : null
      );

      if (!overview && !balances) {
        throw new Error('Critical API calls failed');
      }

      state.overview = overview || state.overview;
      state.profile = profile || state.profile;
      state.balances = balances || state.balances;
      state.beneficiaries = beneficiaries || state.beneficiaries;
      state.pendingApprovals = pendingApprovals || state.pendingApprovals;

      syncBalances();
      applyGlobalMocks();

      state.connected = true;
      state.ready = true;
      applyGlobalMocks();
      syncBalances();
      hideConnectionBanner();
      const mockBlock = document.getElementById('mockOtpDemoBlock');
      if (mockBlock) mockBlock.classList.add('nt-api-hidden');
      if (global.NTNotifications) {
        global.NTNotifications.refreshBadge().catch(() => {});
      }
      const displayName = state.profile?.displayName || ACCOUNT;
      console.info('[NT API] Live data ready —', ACCOUNT, displayName);

      Promise.allSettled([
        apiGet('/transfers/history', { page: 1, size: 50 }),
        apiGet('/analytics/dashboard')
      ]).then(extras => {
        if (extras[0].status === 'fulfilled') state.transferHistory = extras[0].value;
        if (extras[1].status === 'fulfilled') state.analytics = extras[1].value;
      }).catch(() => {});

      loadStatementsAll().catch(() => {});
    } catch (e) {
      console.warn('[NT API] Backend unreachable — embedded mocks only.', e.message);
      state.connected = false;
      state.ready = true;
      showConnectionBanner('Unable to reach banking services. Please try again later.');
    } finally {
      if (loading) loading.style.display = 'none';
    }
  }

  async function reloadView(viewName) {
    if (!state.connected) return;
    if (viewName === 'TransferHistory' && typeof global.renderTxHistory === 'function') {
      await global.renderTxHistory();
    }
    if (viewName === 'PendingApprovals') {
      state.pendingApprovals = await apiGet('/approvals/pending');
      applyGlobalMocks();
      if (typeof global.initPendingApprovalsView === 'function') global.initPendingApprovalsView();
    }
    if (viewName === 'Beneficiaries') {
      await refreshBeneficiaries();
    }
    if (global.NTReporting && global.NTReporting.isReportingView(viewName)) {
      if (typeof global.updateAnalytics === 'function') {
        await global.updateAnalytics();
      }
    }
    if (viewName === 'Notifications' && global.NTNotifications) {
      await global.NTNotifications.renderCenter();
    }
  }

  async function refreshBeneficiaries() {
    if (!state.connected) return;
    state.beneficiaries = await apiGet('/beneficiaries');
    applyGlobalMocks();
    if (!document.getElementById('benGridContainer')) return;
    if (typeof global.renderBeneficiaries === 'function') {
      global.renderBeneficiaries();
    } else if (typeof global.initBeneficiariesView === 'function') {
      global.initBeneficiariesView();
    }
  }

  async function refreshNotifications() {
    if (!global.NTNotifications) return;
    await global.NTNotifications.refreshBadge();
    if (global.currentActiveView === 'Notifications') {
      await global.NTNotifications.renderCenter();
    }
  }

  global.NTApi = {
    state, loadDashboard, hydrateOverview, hydrateHeader, reloadView, refreshBeneficiaries, refreshNotifications,
    initBalanceVisibilityToggles, applySensitiveBalance, applyAllSensitiveBalances, isBalanceVisible, setBalanceVisible,
    apiGet, apiPost, apiPatch, ACCOUNT, API_BASE,
    postInternalTransfer: body => apiPost('/transfers/internal', body),
    postAchTransfer: body => apiPost('/transfers/ach', body),
    postWireTransfer: body => apiPost('/transfers/wire', body),
    postIntlTransfer: body => apiPost('/transfers/international', body),
    createBeneficiary: body => apiPost('/beneficiaries', body),
    updateBeneficiaryLimits: (code, single, daily) => apiPatch('/beneficiaries/' + code + '/limits', { single, daily }),
    updateBeneficiaryTrust: (code, trusted) => apiPatch('/beneficiaries/' + code + '/trust', { trusted }),
    updateBeneficiaryStatus: (code, status) => apiPatch('/beneficiaries/' + code + '/status', { status }),
    fetchStatements: (params) => apiGet('/statements', params),
    requestOtp: () => apiPost('/otp/request'),
    verifyOtp: code => apiPost('/otp/verify', null, { code }),
    approve: (ref, action) => apiPost('/approvals/' + ref + '/' + action),
    freezeCard: frozen => apiPatch('/cards/freeze', { frozen }),
    fetchNotifications: () => apiGet('/notifications'),
    fetchUnreadCount: () => apiGet('/notifications/unread-count'),
    markNotificationRead: id => apiPatch('/notifications/' + id + '/read'),
    markAllNotificationsRead: () => apiPatch('/notifications/read-all'),
    refreshAll: async () => { await loadDashboard(); hydrateOverview(); }
  };
})(window);
