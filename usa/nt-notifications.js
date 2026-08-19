/**
 * Notification center — bell badge, list view, expandable items.
 */
(function (global) {
  const expanded = new Set();
  let cached = { items: [], unreadCount: 0 };

  const TYPE_LABELS = {
    CARD_LOCK: 'Card frozen',
    CARD_UNLOCK: 'Card active',
    TRANSACTION: 'Transaction',
    LOGIN: 'Sign-in',
    SECURITY: 'Security',
    TRANSFER: 'Transfer',
    COMPLIANCE: 'Compliance',
    BENEFICIARY: 'Beneficiary',
    SYSTEM: 'System'
  };

  function iconForType(type) {
    const icons = {
      CARD_LOCK: '&#128274;',
      CARD_UNLOCK: '&#9989;',
      TRANSACTION: '&#128176;',
      LOGIN: '&#128100;',
      SECURITY: '&#9888;&#65039;',
      TRANSFER: '&#8644;',
      COMPLIANCE: '&#128203;',
      BENEFICIARY: '&#128101;',
      SYSTEM: '&#8505;&#65039;'
    };
    return icons[type] || '&#128276;';
  }

  function severityClass(severity) {
    if (severity === 'CRITICAL') return 'notif-item--critical';
    if (severity === 'WARNING') return 'notif-item--warning';
    return 'notif-item--info';
  }

  function formatMeta(metadata) {
    if (!metadata || typeof metadata !== 'object') return '';
    const rows = [];
    if (metadata.ipAddress) rows.push(['IP address', metadata.ipAddress]);
    if (metadata.location) rows.push(['Location', metadata.location]);
    if (metadata.device) rows.push(['Device', metadata.device]);
    if (metadata.userAgent) rows.push(['Browser', metadata.userAgent]);
    if (metadata.reference) rows.push(['Reference', metadata.reference]);
    if (metadata.amount) rows.push(['Amount', '$' + Number(metadata.amount).toLocaleString(undefined, { minimumFractionDigits: 2 })]);
    if (metadata.counterparty) rows.push(['Counterparty', metadata.counterparty]);
    if (metadata.cardLast4) rows.push(['Card', '•••• ' + metadata.cardLast4]);
    if (metadata.period) rows.push(['Period', metadata.period]);
    if (metadata.transferKind) rows.push(['Type', metadata.transferKind]);
    if (metadata.status) rows.push(['Status', metadata.status]);
    if (metadata.reason1) rows.push(['Review reason', metadata.reason1]);
    if (metadata.beneficiaryCode) rows.push(['Beneficiary', metadata.beneficiaryCode]);
    if (metadata.displayName) rows.push(['Payee', metadata.displayName]);
    if (metadata.beneficiaryType) rows.push(['Payee type', metadata.beneficiaryType]);
    if (metadata.updateKind) rows.push(['Update', metadata.updateKind]);
    if (metadata.detail) rows.push(['Details', metadata.detail]);
    if (metadata.reason) rows.push(['Reason', metadata.reason]);
    if (!rows.length) return '';
    return '<dl class="notif-meta-grid">' + rows.map(function (r) {
      return '<dt>' + escapeHtml(r[0]) + '</dt><dd>' + escapeHtml(r[1]) + '</dd>';
    }).join('') + '</dl>';
  }

  function escapeHtml(s) {
    if (!s) return '';
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  function updateBellBadge(count) {
    const badge = document.getElementById('notifBellBadge');
    if (!badge) return;
    const n = Number(count) || 0;
    if (n > 0) {
      badge.hidden = false;
      badge.textContent = n > 99 ? '99+' : String(n);
      badge.setAttribute('aria-label', n + ' unread notifications');
    } else {
      badge.hidden = true;
      badge.textContent = '';
    }
  }

  async function refreshBadge() {
    if (!global.NTApi?.apiGet) return;
    try {
      const data = await global.NTApi.apiGet('/notifications/unread-count');
      updateBellBadge(data.unreadCount);
      cached.unreadCount = data.unreadCount || 0;
    } catch (e) {
      console.warn('[NT Notifications] badge refresh failed', e.message);
    }
  }

  async function load() {
    if (!global.NTApi?.apiGet) return cached;
    const data = await global.NTApi.apiGet('/notifications');
    cached = {
      items: data.items || [],
      unreadCount: data.unreadCount || 0,
      totalCount: data.totalCount || 0
    };
    if (global.NTApi.state) {
      global.NTApi.state.notifications = cached;
    }
    updateBellBadge(cached.unreadCount);
    return cached;
  }

  function renderList(container) {
    if (!container) return;
    const summary = document.getElementById('notifUnreadSummary');
    if (summary) {
      summary.textContent = cached.unreadCount > 0
        ? cached.unreadCount + ' unread · ' + cached.totalCount + ' total'
        : 'All caught up · ' + cached.totalCount + ' notifications';
    }

    if (!cached.items.length) {
      container.innerHTML = '<div class="notif-empty">No notifications yet.</div>';
      return;
    }

    container.innerHTML = cached.items.map(function (item) {
      const isOpen = expanded.has(item.id);
      const unread = !item.read;
      return (
        '<article class="notif-item ' + severityClass(item.severity) + (unread ? ' notif-item--unread' : '') + (isOpen ? ' notif-item--expanded' : '') + '" data-id="' + item.id + '">' +
          '<button type="button" class="notif-item__header" aria-expanded="' + isOpen + '" onclick="NTNotifications.toggle(\'' + item.id + '\')">' +
            '<span class="notif-item__icon" aria-hidden="true">' + iconForType(item.type) + '</span>' +
            '<span class="notif-item__main">' +
              '<span class="notif-item__title-row">' +
                '<span class="notif-item__title">' + escapeHtml(item.title) + '</span>' +
                '<span class="notif-item__chip">' + escapeHtml(TYPE_LABELS[item.type] || item.type) + '</span>' +
              '</span>' +
              '<span class="notif-item__summary">' + escapeHtml(item.summary || item.message) + '</span>' +
              '<span class="notif-item__time">' + escapeHtml(item.displayTime || '') + '</span>' +
            '</span>' +
            (unread ? '<span class="notif-item__dot" title="Unread"></span>' : '') +
            '<span class="notif-item__chevron" aria-hidden="true">' + (isOpen ? '&#9650;' : '&#9660;') + '</span>' +
          '</button>' +
          '<div class="notif-item__body" id="notif-body-' + item.id + '"' + (isOpen ? '' : ' hidden') + '>' +
            '<p class="notif-item__message">' + escapeHtml(item.message) + '</p>' +
            formatMeta(item.metadata) +
            (item.referenceId ? '<p class="notif-item__ref"><strong>Reference:</strong> ' + escapeHtml(item.referenceId) + '</p>' : '') +
          '</div>' +
        '</article>'
      );
    }).join('');
  }

  async function renderCenter() {
    const list = document.getElementById('notificationsList');
    if (!list) return;
    list.innerHTML = '<div class="notif-loading">Loading notifications…</div>';
    try {
      await load();
      renderList(list);
    } catch (e) {
      list.innerHTML = '<div class="notif-empty">Unable to load notifications. ' + escapeHtml(e.message) + '</div>';
    }
  }

  async function toggle(id) {
    const body = document.getElementById('notif-body-' + id);
    const item = cached.items.find(function (n) { return n.id === id; });
    if (!body || !item) return;

    if (expanded.has(id)) {
      expanded.delete(id);
      body.hidden = true;
    } else {
      expanded.add(id);
      body.hidden = false;
      if (!item.read && global.NTApi?.apiPatch) {
        try {
          await global.NTApi.apiPatch('/notifications/' + id + '/read');
          item.read = true;
          await refreshBadge();
        } catch (e) {
          console.warn('[NT Notifications] mark read failed', e.message);
        }
      }
    }
    renderList(document.getElementById('notificationsList'));
  }

  async function markAllRead() {
    if (!global.NTApi?.apiPatch) return;
    try {
      await global.NTApi.apiPatch('/notifications/read-all');
      cached.items.forEach(function (n) { n.read = true; });
      cached.unreadCount = 0;
      updateBellBadge(0);
      renderList(document.getElementById('notificationsList'));
      if (global.NTUI?.toast) global.NTUI.toast('All notifications marked as read', 'success');
    } catch (e) {
      if (global.NTUI?.error) global.NTUI.error(e.message);
    }
  }

  global.NTNotifications = {
    load,
    refreshBadge,
    renderCenter,
    toggle,
    markAllRead,
    updateBellBadge,
    getCached: function () { return cached; }
  };

  document.addEventListener('DOMContentLoaded', function () {
    if (global.NTApi?.state?.ready) refreshBadge();
  });
})(window);
