/**
 * Section-aware header dropdown — menu options change by portal area.
 */
(function (global) {
  'use strict';

  const REPORTING_VIEW_LABELS = global.NTReporting && global.NTReporting.REPORTING_VIEWS
    ? global.NTReporting.REPORTING_VIEWS.reduce(function (acc, v) {
        acc[v] = global.NTReporting.getLabel(v);
        return acc;
      }, { ReportingAnalytics: 'Portfolio Dashboard' })
    : {
      AnalyticsDashboard: 'Portfolio Dashboard',
      CashFlowReport: 'Cash Flow',
      TransferReport: 'Transfer Activity',
      PortfolioAllocation: 'Portfolio Allocation',
      FxExposureReport: 'FX Exposure',
      RiskComplianceReport: 'Risk & Compliance',
      ApprovalMetrics: 'Approval Metrics',
      InsightsReports: 'Insights & Exports',
      ReportingAnalytics: 'Portfolio Dashboard'
    };

  const VIEW_LABELS = Object.assign({
    Overview: 'Overview',
    Statements: 'Statements',
    TaxForms: 'Tax Forms',
    WireInstructions: 'Wire Instructions',
    RegulatoryFilings: 'Regulatory Filings',
    Beneficiaries: 'Beneficiaries',
    CashPosition: 'Cash Position',
    BillsPayables: 'Bills & Payables',
    Receivables: 'Receivables',
    ExpensesBudgeting: 'Expenses & Budgeting',
    TaxCenter: 'Tax Center',
    DocumentVault: 'Document Vault',
    InvestmentOverview: 'Portfolio Overview',
    HoldingsPositions: 'Holdings & Positions',
    PerformanceReturns: 'Performance & Returns',
    AllocationRebalancing: 'Allocation & Rebalancing',
    InvestmentActivity: 'Investment Activity',
    InvestmentDocuments: 'Investment Documents',
    InternalTransfer: 'Internal Transfer',
    ACHTransfer: 'ACH Transfer',
    WireTransfer: 'Wire Transfer',
    InternationalTransfer: 'International Transfer',
    PendingApprovals: 'Pending Approvals',
    TransferHistory: 'Transfer History',
    Notifications: 'Notifications'
  }, REPORTING_VIEW_LABELS);

  const REPORTING_VIEWS = global.NTReporting
    ? global.NTReporting.REPORTING_VIEWS.concat(['ReportingAnalytics'])
    : ['AnalyticsDashboard', 'CashFlowReport', 'TransferReport', 'PortfolioAllocation',
      'FxExposureReport', 'RiskComplianceReport', 'ApprovalMetrics', 'InsightsReports', 'ReportingAnalytics'];

  const VIEW_TO_SECTION = {
    Overview: 'accounts',
    Statements: 'documents',
    TaxForms: 'documents',
    WireInstructions: 'documents',
    RegulatoryFilings: 'documents',
    Beneficiaries: 'accounts',
    CashPosition: 'financial',
    BillsPayables: 'financial',
    Receivables: 'financial',
    ExpensesBudgeting: 'financial',
    TaxCenter: 'financial',
    DocumentVault: 'financial',
    InvestmentOverview: 'investments',
    HoldingsPositions: 'investments',
    PerformanceReturns: 'investments',
    AllocationRebalancing: 'investments',
    InvestmentActivity: 'investments',
    InvestmentDocuments: 'investments',
    InternalTransfer: 'cash',
    ACHTransfer: 'cash',
    WireTransfer: 'cash',
    InternationalTransfer: 'cash',
    PendingApprovals: 'cash',
    TransferHistory: 'cash',
    Notifications: 'notifications'
  };

  REPORTING_VIEWS.forEach(function (v) {
    VIEW_TO_SECTION[v] = 'reporting';
  });

  const SECTION_MENUS = {
    accounts: {
      buttonPrefix: 'Accounts',
      items: [
        { value: 'Overview', label: 'Overview' },
        { value: 'Beneficiaries', label: 'Beneficiaries' }
      ]
    },
    documents: {
      buttonPrefix: 'Document Center',
      items: [
        { value: 'Statements', label: 'Statements' },
        { value: 'TaxForms', label: 'Tax Forms' },
        { value: 'WireInstructions', label: 'Wire Instructions' },
        { value: 'RegulatoryFilings', label: 'Regulatory Filings' }
      ]
    },
    financial: {
      buttonPrefix: 'Financial Management',
      items: [
        { value: 'CashPosition', label: 'Cash Position' },
        { value: 'BillsPayables', label: 'Bills & Payables' },
        { value: 'Receivables', label: 'Receivables' },
        { value: 'ExpensesBudgeting', label: 'Expenses & Budgeting' },
        { value: 'TaxCenter', label: 'Tax Center' },
        { value: 'DocumentVault', label: 'Document Vault' }
      ]
    },
    investments: {
      buttonPrefix: 'Investment Insights',
      items: [
        { value: 'InvestmentOverview', label: 'Portfolio Overview' },
        { value: 'HoldingsPositions', label: 'Holdings & Positions' },
        { value: 'PerformanceReturns', label: 'Performance & Returns' },
        { value: 'AllocationRebalancing', label: 'Allocation & Rebalancing' },
        { value: 'InvestmentActivity', label: 'Investment Activity' },
        { value: 'InvestmentDocuments', label: 'Investment Documents' }
      ]
    },
    cash: {
      buttonPrefix: 'Cash Movement',
      items: [
        { value: 'InternalTransfer', label: 'Internal Transfer' },
        { value: 'ACHTransfer', label: 'ACH Transfer' },
        { value: 'WireTransfer', label: 'Wire Transfer' },
        { value: 'InternationalTransfer', label: 'International Transfer' },
        { value: 'PendingApprovals', label: 'Pending Approvals' },
        { value: 'TransferHistory', label: 'Transfer History' }
      ]
    },
    reporting: {
      buttonPrefix: 'Reporting & Analytics',
      items: [
        { value: 'AnalyticsDashboard', label: 'Portfolio Dashboard' },
        { value: 'CashFlowReport', label: 'Cash Flow' },
        { value: 'TransferReport', label: 'Transfer Activity' },
        { value: 'PortfolioAllocation', label: 'Portfolio Allocation' },
        { value: 'FxExposureReport', label: 'FX Exposure' },
        { value: 'RiskComplianceReport', label: 'Risk & Compliance' },
        { value: 'ApprovalMetrics', label: 'Approval Metrics' },
        { value: 'InsightsReports', label: 'Insights & Exports' }
      ]
    },
    notifications: {
      buttonPrefix: 'Alerts',
      items: [
        { value: 'Notifications', label: 'Notification Center' }
      ]
    }
  };

  function resolveSection(viewName) {
    if (global.NTReporting && global.NTReporting.isReportingView(viewName)) {
      return 'reporting';
    }
    return VIEW_TO_SECTION[viewName] || 'accounts';
  }

  function getLabel(viewName) {
    if (global.NTReporting && global.NTReporting.isReportingView(viewName)) {
      return global.NTReporting.getLabel(viewName);
    }
    return VIEW_LABELS[viewName] || viewName;
  }

  function sync(viewName) {
    const root = document.getElementById('layoutDropdown');
    const menu = document.getElementById('layoutDropdownMenu');
    const labelEl = document.getElementById('layoutDropdownLabel');
    const btn = document.getElementById('layoutDropdownBtn');
    if (!root || !menu || !labelEl) return;

    const normalized = global.NTReporting && global.NTReporting.isReportingView(viewName)
      ? global.NTReporting.normalizeView(viewName)
      : viewName;
    const sectionKey = resolveSection(viewName);
    const section = SECTION_MENUS[sectionKey] || SECTION_MENUS.accounts;
    const currentLabel = getLabel(viewName);

    labelEl.textContent = currentLabel;
    if (btn) {
      btn.setAttribute('title', section.buttonPrefix + ' — switch view');
      btn.setAttribute('aria-label', 'Switch view within ' + section.buttonPrefix);
    }

    menu.innerHTML = '';
    const header = document.createElement('li');
    header.className = 'dropdown-group-label';
    header.setAttribute('role', 'presentation');
    header.textContent = section.buttonPrefix;
    menu.appendChild(header);

    section.items.forEach(function (item) {
      const li = document.createElement('li');
      const itemVal = item.value === 'ReportingAnalytics' ? 'AnalyticsDashboard' : item.value;
      li.className = 'dropdown-item' + (itemVal === normalized ? ' is-active' : '');
      li.setAttribute('role', 'option');
      li.setAttribute('data-value', item.value);
      li.setAttribute('aria-selected', itemVal === normalized ? 'true' : 'false');
      li.textContent = item.label;
      menu.appendChild(li);
    });

    root.dataset.section = sectionKey;
  }

  function init(onSelect) {
    const root = document.getElementById('layoutDropdown');
    const menu = document.getElementById('layoutDropdownMenu');
    const btn = document.getElementById('layoutDropdownBtn');
    if (!root || !menu || !btn) return;

    btn.addEventListener('click', function (e) {
      e.stopPropagation();
      const open = root.classList.toggle('active');
      btn.setAttribute('aria-expanded', open ? 'true' : 'false');
    });

    menu.addEventListener('click', function (e) {
      const item = e.target.closest('.dropdown-item');
      if (!item) return;
      const val = item.getAttribute('data-value');
      if (val && typeof onSelect === 'function') {
        onSelect(val);
      }
      root.classList.remove('active');
      btn.setAttribute('aria-expanded', 'false');
    });

    document.addEventListener('click', function () {
      root.classList.remove('active');
      btn.setAttribute('aria-expanded', 'false');
    });

    sync(global.currentActiveView || 'Overview');
  }

  global.NTSectionDropdown = {
    sync,
    init,
    resolveSection,
    getLabel,
    SECTION_MENUS
  };
})(window);
