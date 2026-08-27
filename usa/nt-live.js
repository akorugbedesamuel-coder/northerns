/**
 * Northern Trust — wires all dashboard interactions to Spring Boot /api/v1
 */
(function (global) {
  'use strict';

  const fmt = (n) => '$' + Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  const TOAST_DEFAULTS = {
    success: { title: 'Success', ms: 4500 },
    error: { title: 'Something went wrong', ms: 6500 },
    warn: { title: 'Attention', ms: 5500 },
    info: { title: 'Notice', ms: 4500 }
  };

  const TOAST_ICONS = {
    success: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
    error: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
    info: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>',
    warn: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>'
  };

  function escapeToastHtml(text) {
    if (text == null) return '';
    return String(text)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  /** Split "Title: details" into heading + body when it reads naturally. */
  function parseToastCopy(msg, type) {
    const def = TOAST_DEFAULTS[type] || TOAST_DEFAULTS.info;
    const raw = String(msg == null ? '' : msg).trim();
    if (!raw) return { title: def.title, description: '' };

    const colon = raw.indexOf(':');
    if (colon > 0 && colon < 56) {
      const head = raw.slice(0, colon).trim();
      const body = raw.slice(colon + 1).trim();
      if (body && head.length <= 48) {
        return { title: head, description: body };
      }
    }
    return { title: def.title, description: raw };
  }

  function getToastHost() {
    let host = document.getElementById('ntToastHost');
    if (!host) {
      host = document.createElement('div');
      host.id = 'ntToastHost';
      host.setAttribute('aria-live', 'polite');
      host.setAttribute('aria-relevant', 'additions');
      document.body.appendChild(host);
    }
    return host;
  }

  const NTUI = {
    toast(msg, type = 'info', ms) {
      const host = getToastHost();
      const def = TOAST_DEFAULTS[type] || TOAST_DEFAULTS.info;
      const duration = typeof ms === 'number' ? ms : def.ms;
      const copy = parseToastCopy(msg, type);

      while (host.children.length >= 5) {
        dismissToast(host.children[0], true);
      }

      const el = document.createElement('div');
      el.className = 'nt-toast nt-toast--' + type;
      el.setAttribute('role', type === 'error' ? 'alert' : 'status');
      el.style.setProperty('--nt-toast-duration', duration + 'ms');

      el.innerHTML =
        '<div class="nt-toast__icon" aria-hidden="true">' + (TOAST_ICONS[type] || TOAST_ICONS.info) + '</div>' +
        '<div class="nt-toast__body">' +
          '<p class="nt-toast__title">' + escapeToastHtml(copy.title) + '</p>' +
          (copy.description
            ? '<p class="nt-toast__description">' + escapeToastHtml(copy.description) + '</p>'
            : '') +
        '</div>' +
        '<button type="button" class="nt-toast__close" aria-label="Dismiss notification">' +
          '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">' +
            '<line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>' +
          '</svg>' +
        '</button>' +
        '<div class="nt-toast__progress" aria-hidden="true"></div>';

      const closeBtn = el.querySelector('.nt-toast__close');
      let dismissTimer;
      let remaining = duration;
      let pausedAt = 0;

      function dismiss(immediate) {
        clearTimeout(dismissTimer);
        dismissToast(el, immediate);
      }

      function scheduleDismiss(delay) {
        clearTimeout(dismissTimer);
        dismissTimer = setTimeout(function () { dismiss(false); }, delay);
      }

      closeBtn.addEventListener('click', function (e) {
        e.stopPropagation();
        dismiss(true);
      });

      el.addEventListener('mouseenter', function () {
        pausedAt = Date.now();
        clearTimeout(dismissTimer);
        el.classList.add('nt-toast--paused');
      });

      el.addEventListener('mouseleave', function () {
        if (pausedAt) {
          remaining -= Date.now() - pausedAt;
          pausedAt = 0;
        }
        el.classList.remove('nt-toast--paused');
        if (remaining > 0) scheduleDismiss(remaining);
        else dismiss(false);
      });

      host.appendChild(el);
      requestAnimationFrame(function () {
        requestAnimationFrame(function () { el.classList.add('nt-toast--visible'); });
      });

      scheduleDismiss(duration);
    },

    success(m, ms) { this.toast(m, 'success', ms); },
    error(m, ms) { this.toast(m, 'error', ms); },
    warn(m, ms) { this.toast(m, 'warn', ms); },
    info(m, ms) { this.toast(m, 'info', ms); },
    confirm(msg) { return global.confirm(msg); }
  };

  function dismissToast(el, immediate) {
    if (!el || el.classList.contains('nt-toast--leaving')) return;
    el.classList.remove('nt-toast--visible');
    el.classList.add('nt-toast--leaving');
    const removeMs = immediate ? 0 : 280;
    setTimeout(function () { el.remove(); }, removeMs);
  }

  function balance(key) {
    return Number(global.ntAccountBalances?.[key] ?? 0);
  }

  function balanceEntry(key) {
    const b = global.NTApi?.state?.balances?.[key];
    return b ? { val: Number(b.available ?? b.balance), name: b.name, limit: 50000 } : { val: balance(key), name: key, limit: 50000 };
  }

  function needsOtp(amount, ben) {
    if (amount > 5000) return true;
    if (ben && ben.trustLevel === 'New' && amount > 1000) return true;
    if (ben && ben.trustLevel === 'Verified' && amount > 2500) return true;
    return false;
  }

  async function runWithOtp(callback, amount, ben, force) {
    if (!global.NTApi?.state?.connected) {
      NTUI.error('Unable to reach banking services. Please try again later.');
      return;
    }
    if (!force && !needsOtp(amount, ben)) return callback();
    global.ensureSharedModals?.();
    try {
      const req = await global.NTApi.requestOtp();
      global.otpSuccessCallback = async () => {
        try { await callback(); } catch (e) { NTUI.error(e.message); }
      };
      const errEl = document.getElementById('otpErrorMsg');
      if (errEl) errEl.style.display = 'none';
      document.querySelectorAll('.otp-digit-input').forEach(el => { el.value = ''; el.classList.remove('success'); });
      document.getElementById('modalOTP').classList.add('active');
      setTimeout(() => document.getElementById('otpDigit1')?.focus(), 100);
    } catch (e) {
      NTUI.error('OTP request failed: ' + e.message);
    }
  }

  async function refreshAfterMutation() {
    if (!global.NTApi) return;
    await global.NTApi.refreshAll();
    if (typeof global.refreshStatementsView === 'function') global.refreshStatementsView();
    if (typeof global.renderTxHistory === 'function' && global.NTApi.state.connected) {
      await global.renderTxHistory();
    }
    if (global.NTApi.refreshNotifications) {
      await global.NTApi.refreshNotifications();
    }
  }

  function setLiveBadge(connected) {
    const b = document.getElementById('ntLiveBadge');
    if (!b) return;
    b.className = connected ? 'live' : 'offline';
    b.textContent = connected ? 'Live data' : 'Offline';
  }

  function hideDemoOtpWhenLive() {
    if (global.NTApi?.state?.connected) {
      const block = document.getElementById('mockOtpDemoBlock');
      if (block) block.classList.add('nt-api-hidden');
    }
  }

  /* ——— OTP verify ——— */
  global.validateOTPCode = async function () {
    const code = ['otpDigit1','otpDigit2','otpDigit3','otpDigit4','otpDigit5','otpDigit6']
      .map(id => document.getElementById(id)?.value || '').join('');
    if (!global.NTApi?.state?.connected) {
      if (code === global.generatedOTP) {
        document.querySelectorAll('.otp-digit-input').forEach(el => el.classList.add('success'));
        setTimeout(() => {
          global.closeBenModal('modalOTP');
          if (global.otpSuccessCallback) global.otpSuccessCallback();
        }, 400);
      } else {
        document.getElementById('otpErrorMsg').style.display = 'block';
      }
      return;
    }
    try {
      const res = await global.NTApi.verifyOtp(code);
      if (res.valid) {
        document.querySelectorAll('.otp-digit-input').forEach(el => el.classList.add('success'));
        setTimeout(() => {
          global.closeBenModal('modalOTP');
          if (global.otpSuccessCallback) global.otpSuccessCallback();
        }, 400);
      } else {
        document.getElementById('otpErrorMsg').style.display = 'block';
        document.getElementById('otpErrorMsg').textContent = res.message || 'Invalid code';
      }
    } catch (e) {
      NTUI.error(e.message);
    }
  };

  global._ntLiveValidateOTPCode = global.validateOTPCode;

  /* ——— Card freeze (delegated) ——— */
  document.getElementById('dynamicContainer')?.addEventListener('change', async (e) => {
    if (e.target.id !== 'freezeSwitch' || !global.NTApi?.state?.connected) return;
    try {
      const res = await global.NTApi.freezeCard(e.target.checked);
      const label = document.getElementById('cardStatusLabel');
      if (label) {
        label.innerText = e.target.checked ? 'Temporarily Frozen' : 'Active & Unfrozen';
        label.style.color = e.target.checked ? '#ef4444' : '#334155';
      }
      const statusText = e.target.checked ? 'Card Restricted' : 'Card Re-activated';
      NTUI.success(`${statusText}: ${res.message || 'Security state updated successfully'}`);
      if (global.NTNotifications) {
        await global.NTNotifications.refreshBadge();
        if (global.currentActiveView === 'Notifications') {
          await global.NTNotifications.renderCenter();
        }
      }
    } catch (err) {
      e.target.checked = !e.target.checked;
      NTUI.error(err.message);
    }
  });

  /* ——— Beneficiaries ——— */
  global.handleFormAddBen = async function (event) {
    event.preventDefault();
    const body = {
      type: document.getElementById('addBenType').value,
      displayName: document.getElementById('addBenName').value,
      relationship: document.getElementById('addBenRel').value,
      trustLevel: document.getElementById('addBenTrust').value,
      singleLimit: document.getElementById('addBenLimitSingle').value,
      dailyLimit: document.getElementById('addBenLimitDaily').value,
      bankName: document.getElementById('addBankName')?.value,
      accountNumber: document.getElementById('addBankAccount')?.value,
      routingOrSwift: document.getElementById('addBankRouting')?.value
    };
    if (global.NTApi?.state?.connected) {
      try {
        const res = await global.NTApi.createBeneficiary(body);
        NTUI.success(`Beneficiary registered: ${body.displayName}` + (res.reference ? ' (' + res.reference + ')' : '') + '.');
        global.closeBenModal('modalAddBen');
        if (global.NTApi.refreshBeneficiaries) await global.NTApi.refreshBeneficiaries();
        if (global.NTApi.refreshNotifications) await global.NTApi.refreshNotifications();
        if (typeof global.renderTabContent === 'function') global.renderTabContent('Beneficiaries');
        return;
      } catch (e) { NTUI.error(e.message); return; }
    }
    return global._handleFormAddBenOrig?.(event);
  };

  global.handleFormSetLimits = async function (event) {
    event.preventDefault();
    const code = document.getElementById('limitsBenId').value;
    const single = document.getElementById('limitsSingleInput').value;
    const daily = document.getElementById('limitsDailyInput').value;
    if (global.NTApi?.state?.connected) {
      try {
        const res = await global.NTApi.apiPatch('/beneficiaries/' + code + '/limits', { single, daily });
        NTUI.success(`Limits Updated: Velocity thresholds modified successfully.`);
        global.closeBenModal('modalSetLimits');
        if (global.NTApi.refreshBeneficiaries) await global.NTApi.refreshBeneficiaries();
        if (global.NTApi.refreshNotifications) await global.NTApi.refreshNotifications();
        return;
      } catch (e) { NTUI.error(e.message); return; }
    }
    return global._handleFormSetLimitsOrig?.(event);
  };

  global.toggleBlockedState = async function (benId) {
    const ben = global.beneficiariesMock?.find(b => b.beneficiaryId === benId);
    if (!ben) return;
    if (global.NTApi?.state?.connected) {
      const newStatus = ben.status === 'BLOCKED' ? 'ACTIVE' : 'BLOCKED';
      try {
        await global.NTApi.updateBeneficiaryStatus(benId, newStatus);
        if (global.NTApi.refreshBeneficiaries) await global.NTApi.refreshBeneficiaries();
        if (global.NTApi.refreshNotifications) await global.NTApi.refreshNotifications();
        const statusMsg = newStatus === 'BLOCKED' ? 'Access Restricted' : 'Access Restored';
        NTUI.success(`${statusMsg}: ${ben.displayName} status updated.`);
        return;
      } catch (e) { NTUI.error(e.message); return; }
    }
    return global._toggleBlockedStateOrig?.(benId);
  };

  global.toggleTrustState = async function (benId) {
    const ben = global.beneficiariesMock?.find(b => b.beneficiaryId === benId);
    if (!ben) return;
    if (global.NTApi?.state?.connected) {
      const trusted = ben.trustLevel !== 'Trusted';
      try {
        await global.NTApi.updateBeneficiaryTrust(benId, trusted);
        if (global.NTApi.refreshBeneficiaries) await global.NTApi.refreshBeneficiaries();
        if (global.NTApi.refreshNotifications) await global.NTApi.refreshNotifications();
        const trustMsg = trusted ? 'Trust Verified' : 'Trust Revoked';
        NTUI.success(`${trustMsg}: Relationship verification state updated.`);
        return;
      } catch (e) { NTUI.error(e.message); return; }
    }
    return global._toggleTrustStateOrig?.(benId);
  };

  /* ——— Send money ——— */
  global.handleFormSendMoney = async function (event) {
    event.preventDefault();
    const amount = parseFloat(document.getElementById('sendMoneyAmount').value);
    const source = document.getElementById('sendMoneySource').value;
    const ben = global.currentSendBen;
    if (!ben || !amount) return;
    if (amount > ben.transferLimits.single) {
      NTUI.error('Exceeds single transfer limit of ' + fmt(ben.transferLimits.single));
      return;
    }
    if (amount > balance(source)) {
      NTUI.error('Insufficient funds in ' + source);
      return;
    }
    const execute = async () => {
      if (!global.NTApi?.state?.connected) {
        global._executeApprovedPayoutOrig?.(amount, source);
        return;
      }
      try {
        let res;
        if (ben.type === 'INTERNAL') {
          const to = ben.destinationDetails?.userId?.includes('89024') ? 'savings' : 'checking';
          res = await global.NTApi.postInternalTransfer({ fromAccountKey: source, toAccountKey: to, amount, memo: 'Payout to ' + ben.displayName });
        } else {
          res = await global.NTApi.postAchTransfer({
            sourceAccountKey: source, beneficiaryId: ben.beneficiaryId, amount,
            memo: 'Payout to ' + ben.displayName, direction: 'credit'
          });
        }
        if (res.success) {
          NTUI.success(`Transaction Settled: $${amount.toLocaleString()} USD dispatched.`);
          global.closeBenModal('modalSendMoney');
          await refreshAfterMutation();
          global.refreshSourceAccountDropdowns?.();
        } else NTUI.error(res.message);
      } catch (e) { NTUI.error(e.message); }
    };
    await runWithOtp(execute, amount, ben);
  };

  global._ntLiveHandleFormSendMoney = global.handleFormSendMoney;

  global.updateFundingBalancePreview = function () {
    const source = document.getElementById('sendMoneySource')?.value;
    const el = document.getElementById('sendMoneyBalanceHint');
    if (el && source) el.textContent = 'Available: ' + fmt(balance(source));
  };

  global._ntLiveUpdateFundingBalancePreview = global.updateFundingBalancePreview;

  /* ——— ACH ——— */
  global.handleACHTransferSubmit = async function (event) {
    event.preventDefault();
    const src = document.getElementById('achSourceAccount').value;
    const benId = document.getElementById('achBeneficiary').value;
    const amt = parseFloat(document.getElementById('achAmount').value) || 0;
    const ben = global.beneficiariesMock?.find(b => b.beneficiaryId === benId);
    const bal = balanceEntry(src);
    const direction = document.querySelector('input[name="achDirection"]:checked')?.value || 'credit';
    const sameDay = document.querySelector('input[name="achDelivery"]:checked')?.value === 'same_day';
    const fee = global.achFeeFor ? global.achFeeFor(sameDay) : (sameDay ? 15 : 0);
    const total = direction === 'credit' ? amt + fee : amt - fee;
    if (!window.validateACHTransferForm?.({ force: true })) return;
    if (direction === 'credit' && total > bal.val) {
      NTUI.error('Insufficient funds'); return;
    }
    const execute = async () => {
      if (!global.NTApi?.state?.connected) return global._handleACHTransferSubmitOrig?.(event);
      if (typeof window.triggerTransferOverlay === 'function') {
        window.triggerTransferOverlay(async () => {
          try {
            const res = await global.NTApi.postAchTransfer({
              sourceAccountKey: src, beneficiaryId: benId, amount: amt,
              memo: document.getElementById('achMemo')?.value || 'ACH Transfer',
              direction,
              effectiveDate: document.getElementById('achEffectiveDate')?.value,
              secCode: document.getElementById('achSecCode')?.value || 'PPD',
              scheduling: document.getElementById('achScheduling')?.value || 'once',
              sameDay,
              achAuthAck: document.getElementById('achAuthAck')?.checked || false,
              recipientName: document.getElementById('achRecipientName')?.value || ben?.displayName || '',
              accountNumber: document.getElementById('achRecipientAccount')?.value || '',
              routingNumber: document.getElementById('achRoutingNumber')?.value || '',
              bankName: document.getElementById('achBankName')?.value || '',
              bankAddress: document.getElementById('achBankAddress')?.value || '',
              fee
            });
            if (res.success) {
              const settle = res.settlementDate || document.getElementById('achReviewSettlementDate')?.innerText || '1–3 business days';
              NTUI.success(`ACH ${direction === 'debit' ? 'Debit' : 'Credit'} queued: $${amt.toLocaleString()} USD. Settlement: ${settle}` + (res.reference ? ` (Ref: ${res.reference})` : ''));
              document.getElementById('achTransferForm')?.reset();
              if (global._achFormDirty) global._achFormDirty = {};
              global.updateACHTransferPreview?.();
              await refreshAfterMutation();
              global.refreshSourceAccountDropdowns?.();
            } else NTUI.error(res.message);
          } catch (e) { NTUI.error(e.message); }
        });
      } else {
        // Fallback if overlay helper is missing
        try {
          const res = await global.NTApi.postAchTransfer({
            sourceAccountKey: src, beneficiaryId: benId, amount: amt,
            memo: document.getElementById('achMemo')?.value || 'ACH Transfer',
            direction,
            effectiveDate: document.getElementById('achEffectiveDate')?.value,
            secCode: document.getElementById('achSecCode')?.value || 'PPD',
            scheduling: document.getElementById('achScheduling')?.value || 'once',
            sameDay,
            achAuthAck: document.getElementById('achAuthAck')?.checked || false,
              recipientName: document.getElementById('achRecipientName')?.value || ben?.displayName || '',
              accountNumber: document.getElementById('achRecipientAccount')?.value || '',
              routingNumber: document.getElementById('achRoutingNumber')?.value || '',
              bankName: document.getElementById('achBankName')?.value || '',
              bankAddress: document.getElementById('achBankAddress')?.value || '',
            fee
          });
          if (res.success) {
            const settle = res.settlementDate || document.getElementById('achReviewSettlementDate')?.innerText || '1–3 business days';
            NTUI.success(`ACH ${direction === 'debit' ? 'Debit' : 'Credit'} queued: $${amt.toLocaleString()} USD. Settlement: ${settle}` + (res.reference ? ` (Ref: ${res.reference})` : ''));
            document.getElementById('achTransferForm')?.reset();
            if (global._achFormDirty) global._achFormDirty = {};
            global.updateACHTransferPreview?.();
            await refreshAfterMutation();
            global.refreshSourceAccountDropdowns?.();
          } else NTUI.error(res.message);
        } catch (e) { NTUI.error(e.message); }
      }
    };
    await runWithOtp(execute, amt, ben);
  };

  global._ntLiveHandleACHTransferSubmit = global.handleACHTransferSubmit;

  async function handleWireComplianceHold(res) {
    const ref = res.reference || '';
    NTUI.warn(
      'AML / Risk Controls: Wire initiated but placed on compliance hold' +
        (ref ? ' (Ref: ' + ref + ')' : '') +
        '. Funds were not released. Volume spikes, structuring patterns, or savings withdrawal limits may require manual treasury review.',
      14000
    );
    if (global.NTNotifications) {
      await global.NTNotifications.refreshBadge();
    }
    document.getElementById('wireTransferForm')?.reset();
    if (global._wireFormDirty) global._wireFormDirty = {};
    global.fillWireBeneficiaryFromSelection?.();
    global.toggleWireFields?.();
    global.updateWireTransferPreview?.();
    await refreshAfterMutation();
    global.refreshSourceAccountDropdowns?.();
  }

  /* ——— Wire ——— */
  global.handleWireTransferSubmit = async function (event) {
    event.preventDefault();
    const isSwift = document.querySelector('input[name="wireType"]:checked')?.value === 'swift';
    const src = document.getElementById('wireSourceAccount').value;
    const benId = document.getElementById('wireBeneficiary').value;
    const amt = parseFloat(document.getElementById('wireAmount').value) || 0;
    const priority = document.getElementById('wirePriority')?.value || 'Standard';
    const currency = isSwift
      ? (document.getElementById('wireSwiftCurrency')?.value || 'USD')
      : 'USD';
    const rate = global.WIRE_FX_RATES?.[currency] || 1;
    const usdPrincipal = isSwift ? amt * rate : amt;
    const fee = global.wireFeeFor ? global.wireFeeFor(isSwift, usdPrincipal, priority) : (isSwift ? 35 + usdPrincipal * 0.005 : 25);
    const ben = global.beneficiariesMock?.find(b => b.beneficiaryId === benId);
    if (!window.validateWireTransferForm?.({ force: true })) return;
    const execute = async () => {
      if (!global.NTApi?.state?.connected) return global._handleWireTransferSubmitOrig?.(event);
      if (typeof window.triggerTransferOverlay === 'function') {
        window.triggerTransferOverlay(async () => {
          try {
            const res = await global.NTApi.postWireTransfer({
              sourceAccountKey: src, beneficiaryId: benId, amount: amt,
              network: isSwift ? 'swift' : 'domestic',
              memo: document.getElementById('wireMemo')?.value || 'Wire transfer',
              amlAcknowledged: document.getElementById('wireAmlAck')?.checked,
              priority,
              currency,
              fxRate: isSwift ? rate : 1,
              usdEquivalent: usdPrincipal,
              recipientName: document.getElementById('wireRecipientName')?.value || ben?.displayName || '',
              accountNumber: document.getElementById('wireRecipientAccount')?.value || '',
              routingNumber: document.getElementById('wireDomesticRouting')?.value || '',
              bankName: document.getElementById('wireBankName')?.value || '',
              bankAddress: document.getElementById('wireBankAddress')?.value || '',
              fee
            });
            if (res.held) {
              await handleWireComplianceHold(res, ben);
            } else if (res.success) {
              const settle = document.getElementById('wireReviewSettlement')?.innerText || 'same-day';
              NTUI.success(
                `${isSwift ? 'SWIFT wire queued' : 'Fedwire settled'}: ${ben ? ben.displayName : 'recipient'}. ` +
                (res.reference ? `Ref: ${res.reference}. ` : '') + `Settlement: ${settle}`
              );
              document.getElementById('wireTransferForm')?.reset();
              if (global._wireFormDirty) global._wireFormDirty = {};
              global.fillWireBeneficiaryFromSelection?.();
              global.updateWireTransferPreview?.();
              await refreshAfterMutation();
              global.refreshSourceAccountDropdowns?.();
            } else NTUI.error(res.message);
          } catch (e) { NTUI.error(e.message); }
        });
      } else {
        try {
          const res = await global.NTApi.postWireTransfer({
            sourceAccountKey: src, beneficiaryId: benId, amount: amt,
            network: isSwift ? 'swift' : 'domestic',
            memo: document.getElementById('wireMemo')?.value || 'Wire transfer',
            amlAcknowledged: document.getElementById('wireAmlAck')?.checked,
            priority,
            currency,
            fxRate: isSwift ? rate : 1,
            usdEquivalent: usdPrincipal,
              recipientName: document.getElementById('wireRecipientName')?.value || ben?.displayName || '',
              accountNumber: document.getElementById('wireRecipientAccount')?.value || '',
              routingNumber: document.getElementById('wireDomesticRouting')?.value || '',
              bankName: document.getElementById('wireBankName')?.value || '',
              bankAddress: document.getElementById('wireBankAddress')?.value || '',
            fee
          });
          if (res.held) {
            await handleWireComplianceHold(res, ben);
          } else if (res.success) {
            const settle = document.getElementById('wireReviewSettlement')?.innerText || 'same-day';
            NTUI.success(
              `${isSwift ? 'SWIFT wire queued' : 'Fedwire settled'}: ${ben ? ben.displayName : 'recipient'}. ` +
              (res.reference ? `Ref: ${res.reference}. ` : '') + `Settlement: ${settle}`
            );
            document.getElementById('wireTransferForm')?.reset();
            if (global._wireFormDirty) global._wireFormDirty = {};
            global.fillWireBeneficiaryFromSelection?.();
            global.updateWireTransferPreview?.();
            await refreshAfterMutation();
            global.refreshSourceAccountDropdowns?.();
          } else NTUI.error(res.message);
        } catch (e) { NTUI.error(e.message); }
      }
    };
    // Wires always require strong multi-factor authentication.
    await runWithOtp(execute, amt, ben, true);
  };

  global._ntLiveHandleWireTransferSubmit = global.handleWireTransferSubmit;

  /* ——— International ——— */
  let fxRatesCache = { EUR: 0.92, GBP: 0.79, JPY: 157.2, CHF: 0.90 };

  global._updateIntlTransferPreviewOrig = global.updateIntlTransferPreview;
  global.updateIntlTransferPreview = function () {
    if (global._updateIntlTransferPreviewOrig) global._updateIntlTransferPreviewOrig();
    const src = document.getElementById('intlSourceAccount')?.value;
    const amt = parseFloat(document.getElementById('intlAmount')?.value) || 0;
    const rates = { EUR: 1.08, GBP: 1.27, JPY: 0.0064, CHF: 1.10, NGN: 0.00065 };
    const cur = document.getElementById('intlTargetCurrency')?.value || 'EUR';
    const rate = rates[cur] || 1;
    const fee = 35.00; // Fixed international service fee
    
    if (!src) return;
    const s = balanceEntry(src);
    const o = document.getElementById('intlPreviewSourceOld');
    const n = document.getElementById('intlPreviewSourceNew');
    
    if (o) o.innerText = fmt(s.val);
    if (n) {
      const usdPrincipal = amt * rate;
      n.innerText = fmt(s.val - (usdPrincipal + fee));
    }
  };

  global.handleIntlTransferSubmit = async function (event) {
    event.preventDefault();
    const src = document.getElementById('intlSourceAccount').value;
    const benId = document.getElementById('intlBeneficiary').value;
    const amt = parseFloat(document.getElementById('intlAmount')?.value) || 0;
    const recipientName = (document.getElementById('intlRecipientName')?.value || '').trim();
    if (!recipientName) {
      NTUI.error('Recipient full name is required for international transfers.');
      document.getElementById('intlRecipientName')?.focus();
      return;
    }
    const bankName = (document.getElementById('intlBankName')?.value || '').trim();
    const bankAddress = (document.getElementById('intlBankAddress')?.value || '').trim();
    const iban = (document.getElementById('intlIban')?.value || '').trim();
    const swiftBic = (document.getElementById('intlSwiftBic')?.value || '').trim();
    const ben = global.beneficiariesMock?.find(b => b.beneficiaryId === benId);
    const execute = async () => {
      if (!global.NTApi?.state?.connected) return global._handleIntlTransferSubmitOrig?.(event);
      const payload = {
        sourceAccountKey: src, beneficiaryId: benId,
        amount: amt, targetCurrency: document.getElementById('intlTargetCurrency')?.value,
        purpose: document.getElementById('intlPurpose')?.value,
        recipientName: recipientName, bankName: bankName, bankAddress: bankAddress,
        iban: iban, swiftBic: swiftBic
      };
      if (typeof window.triggerTransferOverlay === 'function') {
        window.triggerTransferOverlay(async () => {
          try {
            const res = await global.NTApi.postIntlTransfer(payload);
            if (res.success) {
              NTUI.success(`Global FX Settled: ${amt.toLocaleString()} ${document.getElementById('intlTargetCurrency')?.value} dispatched to ${recipientName}.`);
              document.getElementById('intlTransferForm')?.reset();
              await refreshAfterMutation();
              global.refreshSourceAccountDropdowns?.();
            } else NTUI.error(res.message);
          } catch (e) { NTUI.error(e.message); }
        });
      } else {
        try {
          const res = await global.NTApi.postIntlTransfer(payload);
          if (res.success) {
            NTUI.success(`Global FX Settled: ${amt.toLocaleString()} ${document.getElementById('intlTargetCurrency')?.value} dispatched to ${recipientName}.`);
            document.getElementById('intlTransferForm')?.reset();
            await refreshAfterMutation();
            global.refreshSourceAccountDropdowns?.();
          } else NTUI.error(res.message);
        } catch (e) { NTUI.error(e.message); }
      }
    };
    await runWithOtp(execute, amt, ben);
  };

  global._ntLiveHandleIntlTransferSubmit = global.handleIntlTransferSubmit;

  /* ——— Internal transfer preview uses live balances ——— */
  global.updateInternalTransferPreview = function () {
    const src = document.getElementById('transferFromAccount')?.value;
    const dst = document.getElementById('transferToAccount')?.value;
    const amt = parseFloat(document.getElementById('transferAmount')?.value) || 0;
    
    // Clear warning banner first
    const w = document.getElementById('transferWarningBanner');
    if (w) w.style.display = 'none';

    global.validateInternalTransferForm?.();

    if (!src) return;
    const s = balanceEntry(src);
    const oldEl = document.getElementById('previewSourceOld');
    const newEl = document.getElementById('previewSourceNew');
    
    if (oldEl) oldEl.innerText = fmt(s.val);
    if (newEl) newEl.innerText = fmt(s.val - amt);
    
    if (amt > s.val) {
      if (w) { 
        w.style.display = 'block'; 
        w.innerHTML = '⚠️ <strong>Insufficient Liquidity:</strong> The requested amount exceeds available funds in this account.'; 
      }
    }

    if (dst && dst !== src) {
      const d = balanceEntry(dst);
      const dOldEl = document.getElementById('previewDestOld');
      const dNewEl = document.getElementById('previewDestNew');
      if (dOldEl) dOldEl.innerText = fmt(d.val);
      if (dNewEl) dNewEl.innerText = fmt(d.val + amt);
    } else {
      const dOldEl = document.getElementById('previewDestOld');
      const dNewEl = document.getElementById('previewDestNew');
      if (dOldEl) dOldEl.innerText = '—';
      if (dNewEl) dNewEl.innerText = '—';
    }

    if (src && dst && src === dst) {
      if (w) {
        w.style.display = 'block';
        w.innerHTML = '⚠️ Source and Destination accounts cannot be the same.';
      }
    }
  };

  global._updateACHTransferPreviewOrig = global.updateACHTransferPreview;
  global.updateACHTransferPreview = function () {
    if (global._updateACHTransferPreviewOrig) global._updateACHTransferPreviewOrig();
    const src = document.getElementById('achSourceAccount')?.value;
    const direction = document.querySelector('input[name="achDirection"]:checked')?.value || 'credit';
    const amt = parseFloat(document.getElementById('achAmount')?.value) || 0;
    const sameDay = document.querySelector('input[name="achDelivery"]:checked')?.value === 'same_day';
    const fee = global.achFeeFor ? global.achFeeFor(sameDay) : (sameDay ? 15 : 0);
    const total = direction === 'credit' ? amt + fee : amt - fee;
    
    if (!src) return;
    const s = balanceEntry(src);
    const o = document.getElementById('achPreviewSourceOld');
    const n = document.getElementById('achPreviewSourceNew');
    
    if (o) o.innerText = fmt(s.val);
    if (n) {
      const newBal = direction === 'credit' ? s.val - total : s.val + total;
      n.innerText = fmt(newBal);
    }
  };

  global._updateWireTransferPreviewOrig = global.updateWireTransferPreview;
  global.updateWireTransferPreview = function () {
    if (global._updateWireTransferPreviewOrig) global._updateWireTransferPreviewOrig();
    const src = document.getElementById('wireSourceAccount')?.value;
    const amt = parseFloat(document.getElementById('wireAmount')?.value) || 0;
    const isSwift = document.querySelector('input[name="wireType"]:checked')?.value === 'swift';
    const priority = document.getElementById('wirePriority')?.value || 'Standard';

    if (!src) return;
    const s = balanceEntry(src);
    const o = document.getElementById('wirePreviewSourceOld');
    const n = document.getElementById('wirePreviewSourceNew');

    if (o) o.innerText = fmt(s.val);
    if (n) {
      // Wire amount in USD + fee (same model the backend charges)
      let usdPrincipal = amt;
      if (isSwift) {
        const rates = global.WIRE_FX_RATES || { EUR: 1.09, GBP: 1.25, JPY: 0.0065, CHF: 1.11 };
        const cur = document.getElementById('wireSwiftCurrency')?.value || 'USD';
        usdPrincipal = amt * (rates[cur] || 1);
      }
      const fee = global.wireFeeFor ? global.wireFeeFor(isSwift, usdPrincipal, priority) : 25.0;
      n.innerText = fmt(s.val - (usdPrincipal + fee));
    }
  };

  /* ——— Approvals ——— */
  global.handleApprovalAction = async function (action, id) {
    const item = global.mockPendingApprovals?.find(i => i.id === id);
    if (!item) return;
    if (action === 'escalate') {
      if (global.NTApi?.state?.connected) {
        try {
          const res = await global.NTApi.approve(id, 'escalate');
          NTUI.success(res.message);
          await global.NTApi.reloadView('PendingApprovals');
        } catch (e) { NTUI.error(e.message); }
      } else {
        NTUI.warn('Escalated to Treasury Compliance Officer.');
      }
      return;
    }
    const msg = action === 'approve'
      ? `Authorize ${id} (${item.currency} ${Number(item.amount).toLocaleString()})?`
      : `Reject and reverse ${id}?`;
    if (!NTUI.confirm(msg)) return;
    if (global.NTApi?.state?.connected) {
      try {
        const res = await global.NTApi.approve(id, action);
        NTUI.success(res.message);
        await global.NTApi.reloadView('PendingApprovals');
        return;
      } catch (e) { NTUI.error(e.message); return; }
    }
    return global._handleApprovalActionOrig?.(action, id);
  };

  /* ——— Pending approvals: orig skips mock interval when API connected (patched in HTML) ——— */

  /* ——— Transfer history from API ——— */
  if (typeof global.renderTxHistory === 'function') {
    global._renderTxHistoryOrig = global.renderTxHistory;
  }
  global.renderTxHistory = async function () {
    if (!global.NTApi?.state?.connected) return global._renderTxHistoryOrig?.();
    const searchVal = document.getElementById('txhSearch')?.value || '';
    const typeVal = document.getElementById('txhTypeFilter')?.value || 'all';
    const statusVal = document.getElementById('txhStatusFilter')?.value || 'all';
    const periodVal = document.getElementById('txhPeriodFilter')?.value || 'all';
    const page = global.txhCurrentPage || 1;
    try {
      const data = await global.NTApi.apiGet('/transfers/history', {
        search: searchVal, type: typeVal, status: statusVal, period: periodVal, page, size: 8
      });
      global.masterTxLedger = (data.items || []).map(tx => ({
        id: tx.id, date: tx.date, type: tx.type, counterparty: tx.counterparty,
        source: tx.source, amount: tx.amount, currency: tx.currency || 'USD', status: tx.status
      }));
      if (data.summary) {
        const v = document.getElementById('txhTotalVolume');
        if (v) v.innerText = fmt(data.summary.totalVolume);
        const s = document.getElementById('txhTotalSettled');
        if (s) s.innerText = data.summary.settledCount;
        const h = document.getElementById('txhTotalHeld');
        if (h) h.innerText = data.summary.onHoldCount;
        const f = document.getElementById('txhTotalFailed');
        if (f) f.innerText = data.summary.failedCount;
      }
      global._renderTxHistoryOrig?.();
    } catch (e) { NTUI.error(e.message); }
  };

  global.txhGoPage = function (page) {
    global.txhCurrentPage = page;
    global.renderTxHistory();
  };

  function wireTransferHistoryFilters() {
    ['txhSearch','txhTypeFilter','txhStatusFilter','txhPeriodFilter'].forEach(id => {
      const el = document.getElementById(id);
      if (el && !el.dataset.ntTxh) {
        el.dataset.ntTxh = '1';
        const go = () => { global.txhCurrentPage = 1; global.renderTxHistory(); };
        el.addEventListener('change', go);
        if (id === 'txhSearch') el.addEventListener('input', debounce(go, 350));
      }
    });
  }

  const _origInitTxh = global.initTransferHistoryView;
  global.initTransferHistoryView = function () {
    if (_origInitTxh) _origInitTxh();
    wireTransferHistoryFilters();
    if (global.NTApi?.state?.connected) global.renderTxHistory();
  };

  /* ——— Analytics from API ——— */
  function syncAnalyticsDataFromState() {
    const existing = global.analyticsData || {};
    const analytics = global.NTApi?.state?.analytics || {};
    global.analyticsData = {
      ...existing,
      kpis: analytics.kpis || existing.kpis || {},
      charts: analytics.charts || existing.charts || {},
      insights: Array.isArray(analytics.insights) ? analytics.insights : (Array.isArray(existing.insights) ? existing.insights : []),
      transferBreakdown: analytics.transferBreakdown || existing.transferBreakdown || {},
      tables: analytics.tables || existing.tables || {},
      filters: existing.filters || {
        timeRange: '7d',
        accountScope: 'all',
        currency: 'all',
        txType: 'all'
      },
      connected: !!global.NTApi?.state?.connected
    };
  }

  global.updateAnalytics = async function () {
    if (global.NTApi?.state?.connected) {
      const params = global.NTReporting?.getAnalyticsQueryParams
        ? global.NTReporting.getAnalyticsQueryParams()
        : { timeRange: document.getElementById('analyticsTimeRange')?.value || '7d' };
      try {
        global.NTApi.state.analytics = await global.NTApi.apiGet('/analytics/dashboard', params);
        syncAnalyticsDataFromState();
      } catch (e) {
        console.warn('[NT Live] Analytics refresh failed:', e.message);
      }
    }
    if (global.NTReporting && typeof global.NTReporting.updateAnalytics === 'function') {
      global.NTReporting.updateAnalytics();
    } else if (typeof global._updateAnalyticsOrig === 'function') {
      global._updateAnalyticsOrig();
    }
  };

  global.exportAnalyticsReport = function (format) {
    const rows = [['Metric', 'Value']];
    const k = global.analyticsData?.kpis || {};
    Object.keys(k).forEach(key => rows.push([key, k[key]]));
    const csv = rows.map(r => r.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'nt_analytics_' + format + '_' + new Date().toISOString().split('T')[0] + '.csv';
    a.click();
    NTUI.success('Analytics exported as ' + format.toUpperCase());
  };

  global.exportDrilldownView = function () {
    // Get stored drilldown data
    const data = global.currentDrilldownData;
    if (!data) {
      NTUI.info('No drilldown data available to export');
      return;
    }

    // Build CSV
    const csvRows = [];
    const title = data.title.replace(/[^a-zA-Z0-9_\- ]/g, '');
    csvRows.push(`# Drilldown: ${title}`);
    csvRows.push(`# Generated: ${new Date(data.generatedAt).toLocaleString()}`);
    csvRows.push(`# Total Records: ${data.transactions.length}`);
    csvRows.push(`# Total Volume: $${data.totalVolume.toLocaleString(undefined, { minimumFractionDigits: 2 })}`);
    csvRows.push(`# Filters: Time=${data.filters.timeRange}, Account=${data.filters.accountScope}, Currency=${data.filters.currency}`);
    csvRows.push(''); // Empty line to separate metadata
    csvRows.push('Date,Description,Amount,Status'); // CSV header
    
    data.transactions.forEach(tx => {
      // Escape any quotes and commas for CSV safety
      const desc = (tx.desc || tx.asset || tx.type || 'Transaction').replace(/"/g, '""');
      const amount = tx.amount !== undefined ? Math.abs(tx.amount) : (tx.value !== undefined ? Math.abs(tx.value) : 0);
      const status = (tx.status || 'Completed').replace(/"/g, '""');
      csvRows.push(`"${tx.date || new Date().toISOString().split('T')[0]}","${desc}","${amount.toFixed(2)}","${status}"`);
    });
    
    // Create and download CSV file
    const csvContent = csvRows.join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `nt_drilldown_${title.toLowerCase().replace(/\s+/g, '_')}_${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    NTUI.success('Drilldown exported successfully');
  };

  /* ——— Statements refetch ——— */
  async function refetchStatementsFromApi() {
    if (!global.NTApi?.state?.connected || typeof global.getStatementActiveTab !== 'function') return;
    const tab = global.getStatementActiveTab();
    const period = document.getElementById('stmtPeriodSelect')?.value;
    const type = document.getElementById('stmtTypeFilter')?.value;
    const status = document.getElementById('stmtStatusFilter')?.value;
    const search = document.getElementById('stmtSearchInput')?.value;
    try {
      const data = await global.NTApi.apiGet('/statements', { tab, period, type, status, search });
      const mapped = global.NTApi.mapStatements ? null : null;
      const mapFn = (list, t) => {
        if (!list) return [];
        if (t === 'checking') return list.map(s => ({ date: s.date, desc: s.description, amount: s.amount, balance: s.balanceAfter, channel: s.channel, type: s.type }));
        if (t === 'savings') return list.map(s => ({ date: s.date, type: s.type, amount: s.amount, rate: s.rate || '4.50%', balance: s.balanceAfter }));
        if (t === 'credit') return list.map(s => ({ date: s.date, type: s.type, charged: s.charged, payment: s.payment, balance: s.balanceAfter, credit: s.creditAvailable }));
        if (t === 'invest') return list.map(s => ({ date: s.date, asset: s.asset || s.description, type: s.type, units: s.units, price: s.price, value: s.amount, balance: s.balanceAfter }));
        return list.map(s => ({ date: s.date, desc: s.description, source: s.source, type: s.type, amount: s.amount, balance: s.balanceAfter, status: s.status, channel: s.channel }));
      };
      if (tab === 'unified') global.unifiedMock = mapFn(data, 'unified');
      else if (tab === 'checking') global.checkingMock = mapFn(data, 'checking');
      else if (tab === 'savings') global.savingsMock = mapFn(data, 'savings');
      else if (tab === 'credit') global.creditMock = mapFn(data, 'credit');
      else if (tab === 'invest') global.investMock = mapFn(data, 'invest');
      global.refreshStatementsView?.();
    } catch (e) { NTUI.error('Statements: ' + e.message); }
  }

  function wireStatementFilters() {
    ['stmtPeriodSelect','stmtTypeFilter','stmtStatusFilter','stmtSearchInput'].forEach(id => {
      const el = document.getElementById(id);
      if (el && !el.dataset.ntWired) {
        el.dataset.ntWired = '1';
        el.addEventListener('change', refetchStatementsFromApi);
        if (id === 'stmtSearchInput') el.addEventListener('input', debounce(refetchStatementsFromApi, 400));
      }
    });
  }

  function debounce(fn, ms) {
    let t;
    return () => { clearTimeout(t); t = setTimeout(fn, ms); };
  }

  function stashOriginalHandlers() {
    const names = [
      'handleFormAddBen','handleFormSetLimits','toggleBlockedState','toggleTrustState',
      'handleACHTransferSubmit','handleWireTransferSubmit','handleIntlTransferSubmit',
      'initPendingApprovalsView','renderTxHistory','handleApprovalAction',
      'updateACHTransferPreview','updateWireTransferPreview','updateIntlTransferPreview',
      'executeApprovedPayout','handleFormSendMoney'
    ];
    names.forEach(n => {
      if (typeof global[n] === 'function' && !global['_' + n + 'Orig']) {
        global['_' + n + 'Orig'] = global[n];
      }
    });
    if (typeof global.initPendingApprovalsView === 'function' && !global._initPendingApprovalsViewOrig) {
      global._initPendingApprovalsViewOrig = global.initPendingApprovalsView;
    }
    if (typeof global.renderTxHistory === 'function' && !global._renderTxHistoryOrig) {
      global._renderTxHistoryOrig = global.renderTxHistory;
    }
    if (typeof global.updateAnalytics === 'function' && !global._updateAnalyticsOrig) {
      global._updateAnalyticsOrig = global.updateAnalytics;
    }
    if (typeof global.executeApprovedPayout === 'function' && !global._executeApprovedPayoutOrig) {
      global._executeApprovedPayoutOrig = global.executeApprovedPayout;
    }
  }

  function patchInitStatements() {
    const orig = global.initStatementsView;
    if (!orig || orig._ntPatched) return;
    global.initStatementsView = function () {
      orig();
      wireStatementFilters();
      refetchStatementsFromApi();
    };
    global.initStatementsView._ntPatched = true;
  }

  function patchAnalyticsHelpers() {
    global._renderKPICardsOnly = function () {
      if (global.NTReporting && typeof global.NTReporting.renderKPICards === 'function') {
        global.NTReporting.renderKPICards();
        return;
      }
    };
    global._renderInsightsFromApi = function () {
      const list = document.getElementById('insightList');
      const insights = global.NTApi?.state?.analytics?.insights || [];
      if (list && insights.length) {
        list.innerHTML = insights.map(t => `<div class="insight-item"><p>${t}</p></div>`).join('');
      }
    };
  }

  async function loadFxRates() {
    try {
      const r = await global.NTApi.apiGet('/fx/rates');
      if (r.rates) {
        Object.keys(r.rates).forEach(k => { fxRatesCache[k] = Number(r.rates[k]); });
      }
    } catch (e) { /* keep defaults */ }
  }

  async function initLiveLayer() {
    stashOriginalHandlers();
    patchInitStatements();
    patchAnalyticsHelpers();
    hideDemoOtpWhenLive();

    const origRender = global.renderTabContent;
    if (origRender && !origRender._ntLive) {
      global.renderTabContent = function (viewName) {
        origRender(viewName);
        setTimeout(() => {
          if (viewName === 'Overview' && global.NTApi?.state?.connected) {
            const card = global.NTApi.state.overview?.card;
            if (card && document.getElementById('freezeSwitch')) {
              document.getElementById('freezeSwitch').checked = !!card.frozen;
            }
            global.NTApi.hydrateOverview();
          }
          if (viewName === 'Statements') wireStatementFilters();
          if (global.NTApi?.state?.connected) global.NTApi.reloadView(viewName);
        }, 80);
      };
      global.renderTabContent._ntLive = true;
    }

    if (global.NTApi) {
      await global.NTApi.loadDashboard();
      setLiveBadge(global.NTApi.state.connected);
      await loadFxRates();
      if (global.NTApi.state.connected) {
        const sendHint = document.getElementById('sendMoneyBalanceHint');
        if (!sendHint) {
          const sel = document.getElementById('sendMoneySource');
          if (sel?.parentElement) {
            const p = document.createElement('p');
            p.id = 'sendMoneyBalanceHint';
            p.style.cssText = 'font-size:12px;color:#64748b;margin-top:6px;';
            sel.parentElement.appendChild(p);
          }
        }
        global.updateFundingBalancePreview?.();
      }
    }
  }

  global.NTUI = NTUI;
  global.NTLive = { init: initLiveLayer, refetchStatementsFromApi, balance, fmt };

})(window);
