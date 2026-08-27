/**
 * Reporting & Analytics — submenu views, charts, KPIs, insights.
 */
(function (global) {
  'use strict';

  const REPORTING_VIEWS = [
    'AnalyticsDashboard',
    'CashFlowReport',
    'TransferReport',
    'PortfolioAllocation',
    'FxExposureReport',
    'RiskComplianceReport',
    'ApprovalMetrics',
    'InsightsReports'
  ];

  const VIEW_LABELS = {
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

  const SECTION_META = {
    AnalyticsDashboard: {
      portal: 'Strategic Insights',
      title: 'Portfolio Dashboard',
      subtitle: 'Consolidated KPIs, charts, and operational intelligence across all accounts.'
    },
    CashFlowReport: {
      portal: 'Strategic Insights',
      title: 'Cash Flow Analysis',
      subtitle: 'Inflow vs outflow trends, liquidity movement, and weekly settlement patterns.'
    },
    TransferReport: {
      portal: 'Strategic Insights',
      title: 'Transfer Activity',
      subtitle: 'Volume by rail — internal, ACH, wire, and international — with settlement status.'
    },
    PortfolioAllocation: {
      portal: 'Strategic Insights',
      title: 'Portfolio Allocation',
      subtitle: 'Asset distribution across checking, savings, credit, and managed portfolios.'
    },
    FxExposureReport: {
      portal: 'Strategic Insights',
      title: 'FX Exposure',
      subtitle: 'Currency concentration, volatility bands, and cross-border fee impact.'
    },
    RiskComplianceReport: {
      portal: 'Strategic Insights',
      title: 'Risk & Compliance',
      subtitle: 'Flagged transactions, OFAC holds, and compliance queue health.'
    },
    ApprovalMetrics: {
      portal: 'Strategic Insights',
      title: 'Approval Performance',
      subtitle: 'Authorization funnel, SLA timing, and dual-control workflow metrics.'
    },
    InsightsReports: {
      portal: 'Strategic Insights',
      title: 'Insights & Exports',
      subtitle: 'AI-generated observations and downloadable audit packs.'
    }
  };

  global.analyticsData = global.analyticsData || {
    kpis: {},
    charts: {},
    insights: [],
    filters: { timeRange: '7d', accountScope: 'all', currency: 'all', txType: 'all' }
  };
  
  // Fallback initializations for mock data to prevent timing issues
  global.masterTxLedger = global.masterTxLedger || [
    { id: 'NT-TXH-0091', date: '2026-05-21T08:14:00Z', type: 'Wire', counterparty: 'Global Trust Bank (Fedwire)', source: 'Checking Account', amount: 825, currency: 'USD', status: 'Settled' },
    { id: 'NT-TXH-0090', date: '2026-05-21T07:55:00Z', type: 'International', counterparty: 'Quantum Global Holdings', source: 'Checking Account', amount: 15732.50, currency: 'USD', status: 'OFAC Hold' },
    { id: 'NT-TXH-0089', date: '2026-05-20T16:30:00Z', type: 'ACH', counterparty: 'Vanguard Group (External)', source: 'Checking Account', amount: 450, currency: 'USD', status: 'Processing' },
    { id: 'NT-TXH-0088', date: '2026-05-20T14:22:00Z', type: 'Internal', counterparty: 'Savings Vault → Checking', source: 'Savings Vault', amount: 2000, currency: 'USD', status: 'Settled' },
    { id: 'NT-TXH-0087', date: '2026-05-20T11:05:00Z', type: 'Wire', counterparty: 'Acme Corp Real Estate (SWIFT)', source: 'Apex Credit Line', amount: 2500, currency: 'USD', status: 'Compliance Hold' },
    { id: 'NT-TXH-0086', date: '2026-05-19T17:44:00Z', type: 'International', counterparty: 'Mayfair Partners Ltd (GBP)', source: 'Checking Account', amount: 634, currency: 'USD', status: 'Settled' },
    { id: 'NT-TXH-0085', date: '2026-05-19T13:10:00Z', type: 'ACH', counterparty: 'BlackRock Fund Services', source: 'Checking Account', amount: 1250, currency: 'USD', status: 'Settled' },
    { id: 'NT-TXH-0084', date: '2026-05-19T09:58:00Z', type: 'Wire', counterparty: 'Erste Bank Vienna (SWIFT/EUR)', source: 'Checking Account', amount: 327.60, currency: 'USD', status: 'Settled' },
    { id: 'NT-TXH-0083', date: '2026-05-18T15:30:00Z', type: 'Internal', counterparty: 'Managed Portfolio → Checking', source: 'Managed Portfolio', amount: 5000, currency: 'USD', status: 'Settled' },
    { id: 'NT-TXH-0082', date: '2026-05-18T10:00:00Z', type: 'ACH', counterparty: 'Fidelity Investments', source: 'Checking Account', amount: 750, currency: 'USD', status: 'Returned' },
    { id: 'NT-TXH-0081', date: '2026-05-17T17:00:00Z', type: 'Wire', counterparty: 'Citi Private Bank (Fedwire)', source: 'Checking Account', amount: 11000, currency: 'USD', status: 'Settled' },
    { id: 'NT-TXH-0080', date: '2026-05-17T09:15:00Z', type: 'International', counterparty: 'Nomura Securities (JPY)', source: 'Checking Account', amount: 189, currency: 'USD', status: 'Failed' },
  ];
  
  global.unifiedMock = global.unifiedMock || [
    { date: '2026-05-19', desc: 'Google Cloud Platform', source: 'NT Premium Credit Line', type: 'Fee', amount: -124.50, balance: 200.00, status: 'Completed', channel: 'Card' },
    { date: '2026-05-18', desc: 'Enterprise Inbound Revenue', source: 'Checking Account', type: 'Deposit', amount: 1852.00, balance: 318750.00, status: 'Completed', channel: 'Wire' },
    { date: '2026-05-18', desc: 'Vanguard Treasury Fund Div', source: 'Managed Portfolio', type: 'Interest', amount: 142.30, balance: 169400000.00, status: 'Completed', channel: 'Internal' },
    { date: '2026-05-16', desc: 'Stripe Pay-out Settlement', source: 'Checking Account', type: 'Deposit', amount: 624.00, balance: 6393.30, status: 'Completed', channel: 'ACH' },
    { date: '2026-05-15', desc: 'Monthly Yield Accrual', source: 'Savings Vault', type: 'Interest', amount: 243.75, balance: 35180.38, status: 'Completed', channel: 'Internal' },
    { date: '2026-05-15', desc: 'Salesforce Cloud Services', source: 'NT Premium Credit Line', type: 'Fee', amount: -87.50, balance: 12624.50, status: 'Completed', channel: 'Card' },
    { date: '2026-05-14', desc: 'Corporate Card Payment', source: 'Checking Account', type: 'Withdrawal', amount: -185.00, balance: 5769.30, status: 'Completed', channel: 'ACH' },
    { date: '2026-05-10', desc: 'Figma Enterprise Subscription', source: 'NT Premium Credit Line', type: 'Fee', amount: -18.00, balance: 200.00, status: 'Completed', channel: 'Card' },
    { date: '2026-05-05', desc: 'SPY S&P 500 Rebalance Buy', source: 'Managed Portfolio', type: 'Investments', amount: -1200.00, balance: 17357.70, status: 'Completed', channel: 'Internal' },
    { date: '2026-05-05', desc: 'GitHub Enterprise Suite', source: 'NT Premium Credit Line', type: 'Fee', amount: -52.00, balance: 12712.00, status: 'Completed', channel: 'Card' },
  ];
  
  global.investMock = global.investMock || [
    { date: '2026-05-18', asset: 'Vanguard Treasury Fund', type: 'Dividend', units: 28.46, price: 5.00, value: 142.30, balance: 169400000.00 },
    { date: '2026-05-05', asset: 'SPY S&P 500 ETF Index', type: 'Buy', units: 24.00, price: 50.00, value: -1200.00, balance: 17357.70 },
    { date: '2026-04-20', asset: 'BlackRock Global Bond', type: 'Dividend', units: 11.85, price: 10.00, value: 118.50, balance: 16157.70 },
    { date: '2026-04-05', asset: 'NASDAQ 100 Index Trust', type: 'Buy', units: 37.50, price: 40.00, value: -1500.00, balance: 16039.20 },
    { date: '2026-03-20', asset: 'Vanguard Treasury Fund', type: 'Dividend', units: 22.80, price: 5.00, value: 114.00, balance: 15239.20 },
  ];
  
  global.mockPendingApprovals = global.mockPendingApprovals || [];
  global.ntAccountBalances = global.ntAccountBalances || {
    checking: 318750.00,
    savings: 35180.38,
    credit: 200.00,
    invest: 169400000.00
  };

  let analyticsRefreshInterval = null;
  let currentReportingView = 'AnalyticsDashboard';

  function isReportingView(viewName) {
    return REPORTING_VIEWS.indexOf(viewName) >= 0 || viewName === 'ReportingAnalytics';
  }

  function normalizeView(viewName) {
    return viewName === 'ReportingAnalytics' ? 'AnalyticsDashboard' : viewName;
  }

  function sharedFiltersHtml() {
    return `
      <div class="global-filters">
        <div class="filter-group">
          <label class="filter-label">Time Range</label>
          <select class="filter-select" id="analyticsTimeRange" onchange="updateAnalytics()">
            <option value="today">Today</option>
            <option value="7d" selected>Last 7 days</option>
            <option value="30d">Last 30 days</option>
            <option value="quarter">Quarter</option>
            <option value="year">Year</option>
          </select>
        </div>
        <div class="filter-group">
          <label class="filter-label">Account Scope</label>
          <select class="filter-select" id="analyticsAccountScope" onchange="updateAnalytics()">
            <option value="all" selected>All Accounts</option>
            <option value="checking">Checking Account</option>
            <option value="savings">Savings Vault</option>
            <option value="credit">Apex Credit Line</option>
            <option value="invest">Managed Portfolio</option>
          </select>
        </div>
        <div class="filter-group">
          <label class="filter-label">Region / Currency</label>
          <select class="filter-select" id="analyticsCurrency" onchange="updateAnalytics()">
            <option value="all" selected>All Currencies</option>
            <option value="USD">USD</option>
            <option value="EUR">EUR</option>
            <option value="GBP">GBP</option>
            <option value="JPY">JPY</option>
            <option value="CHF">CHF</option>
          </select>
        </div>
        <div class="filter-group">
          <label class="filter-label">Transaction Type</label>
          <select class="filter-select" id="analyticsTxType" onchange="updateAnalytics()">
            <option value="all" selected>All Types</option>
            <option value="transfers">Transfers</option>
            <option value="fees">Fees</option>
            <option value="fx">FX Conversions</option>
            <option value="investments">Investments</option>
            <option value="approvals">Approvals</option>
          </select>
        </div>
      </div>`;
  }

  function exportButtonsHtml() {
    return `
      <div class="analytics-export-controls">
        <button type="button" class="export-btn primary" onclick="exportAnalyticsReport('pdf')">Export Report (PDF)</button>
        <button type="button" class="export-btn" onclick="exportAnalyticsReport('csv')">Export Data (CSV)</button>
        <button type="button" class="export-btn" onclick="exportAnalyticsReport('json')">Export Audit Pack (JSON)</button>
      </div>`;
  }

  const TEMPLATES = {
    AnalyticsDashboard: function () {
      return `
        <div class="analytics-header report-page-header">
          <div>
            <h1 class="analytics-title">Portfolio Dashboard</h1>
            <p class="analytics-subtitle">Consolidated performance, liquidity, and risk metrics.</p>
          </div>
          ${exportButtonsHtml()}
        </div>
        ${sharedFiltersHtml()}
        <div class="kpi-intelligence-row" id="kpiCardsContainer"></div>
        <div class="analytics-grid">
          <div class="analytics-chart-card"><div class="chart-header"><h3 class="chart-title">Cash Flow Analysis</h3><p class="chart-subtitle">Inflow vs Outflow</p></div><div class="chart-container" id="cashFlowChart"></div></div>
          <div class="analytics-chart-card"><div class="chart-header"><h3 class="chart-title">Transfer Activity</h3><p class="chart-subtitle">By transfer type</p></div><div class="chart-container" id="transferActivityChart"></div></div>
          <div class="analytics-chart-card"><div class="chart-header"><h3 class="chart-title">Account Distribution</h3><p class="chart-subtitle">Portfolio allocation</p></div><div class="chart-container" id="accountDistributionChart"></div></div>
          <div class="analytics-chart-card"><div class="chart-header"><h3 class="chart-title">FX Exposure</h3><p class="chart-subtitle">Currency exposure</p></div><div class="chart-container" id="fxExposureChart"></div></div>
          <div class="analytics-chart-card"><div class="chart-header"><h3 class="chart-title">Risk & Compliance</h3><p class="chart-subtitle">Risk heatmap</p></div><div class="chart-container" id="riskComplianceChart"></div></div>
          <div class="analytics-chart-card"><div class="chart-header"><h3 class="chart-title">Approval Performance</h3><p class="chart-subtitle">Funnel analysis</p></div><div class="chart-container" id="approvalPerformanceChart"></div></div>
        </div>
        <div class="insight-engine">
          <div class="insight-header"><div class="insight-icon"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/></svg></div><h3 class="insight-title">Portfolio Insights</h3></div>
          <div class="insight-list" id="insightList"></div>
          <p class="analytics-disclaimer">Insights are for operational planning only — not investment advice.</p>
        </div>`;
    },

    CashFlowReport: function () {
      return `
        <div class="report-page-header"><h1 class="analytics-title">Cash Flow Analysis</h1><p class="analytics-subtitle">Track liquidity inflows and outflows across your family office accounts.</p></div>
        ${sharedFiltersHtml()}
        <div class="kpi-intelligence-row" id="kpiCardsContainer"></div>
        <div class="analytics-grid" style="grid-template-columns: 1fr;">
          <div class="analytics-chart-card" style="min-height: 320px;"><div class="chart-header"><h3 class="chart-title">Weekly Cash Flow</h3></div><div class="chart-container" id="cashFlowChart" style="min-height: 260px;"></div></div>
        </div>
        <div class="report-detail-table-wrap" id="cashFlowDetailTable"></div>`;
    },

    TransferReport: function () {
      return `
        <div class="report-page-header"><h1 class="analytics-title">Transfer Activity</h1><p class="analytics-subtitle">Breakdown of internal, ACH, wire, and international movements.</p></div>
        ${sharedFiltersHtml()}
        <div class="analytics-grid" style="grid-template-columns: 1fr 1fr;">
          <div class="analytics-chart-card"><div class="chart-header"><h3 class="chart-title">Transfers by Type</h3></div><div class="chart-container" id="transferActivityChart"></div></div>
          <div class="analytics-chart-card"><div class="chart-header"><h3 class="chart-title">Approval Funnel</h3></div><div class="chart-container" id="approvalPerformanceChart"></div></div>
        </div>
        <div class="report-detail-table-wrap" id="transferDetailTable"></div>`;
    },

    PortfolioAllocation: function () {
      return `
        <div class="report-page-header"><h1 class="analytics-title">Portfolio Allocation</h1><p class="analytics-subtitle">Market value distribution across account products.</p></div>
        ${sharedFiltersHtml()}
        <div class="kpi-intelligence-row" id="kpiCardsContainer"></div>
        <div class="analytics-grid" style="grid-template-columns: 1fr 1fr;">
          <div class="analytics-chart-card"><div class="chart-container" id="accountDistributionChart"></div></div>
          <div class="report-detail-table-wrap" id="allocationDetailTable"></div>
        </div>`;
    },

    FxExposureReport: function () {
      return `
        <div class="report-page-header"><h1 class="analytics-title">FX Exposure</h1><p class="analytics-subtitle">Currency concentration and volatility indicators.</p></div>
        ${sharedFiltersHtml()}
        <div class="analytics-grid" style="grid-template-columns: 1fr;">
          <div class="analytics-chart-card"><div class="chart-container" id="fxExposureChart" style="min-height: 240px;"></div></div>
        </div>
        <div class="report-detail-table-wrap" id="fxDetailTable"></div>`;
    },

    RiskComplianceReport: function () {
      return `
        <div class="report-page-header"><h1 class="analytics-title">Risk & Compliance</h1><p class="analytics-subtitle">Operational risk signals and compliance holds.</p></div>
        ${sharedFiltersHtml()}
        <div class="analytics-grid" style="grid-template-columns: 1fr 1fr;">
          <div class="analytics-chart-card"><div class="chart-container" id="riskComplianceChart"></div></div>
          <div class="report-detail-table-wrap" id="riskDetailTable"></div>
        </div>`;
    },

    ApprovalMetrics: function () {
      return `
        <div class="report-page-header"><h1 class="analytics-title">Approval Performance</h1><p class="analytics-subtitle">Dual-control workflow and authorization SLA metrics.</p></div>
        ${sharedFiltersHtml()}
        <div class="kpi-intelligence-row" id="kpiCardsContainer"></div>
        <div class="analytics-grid" style="grid-template-columns: 1fr;">
          <div class="analytics-chart-card"><div class="chart-container" id="approvalPerformanceChart" style="min-height: 280px;"></div></div>
        </div>
        <div class="report-detail-table-wrap" id="approvalDetailTable"></div>`;
    },

    InsightsReports: function () {
      return `
        <div class="analytics-header report-page-header">
          <div><h1 class="analytics-title">Insights & Exports</h1><p class="analytics-subtitle">Generated observations and regulatory export packs.</p></div>
          ${exportButtonsHtml()}
        </div>
        ${sharedFiltersHtml()}
        <div class="insight-engine" style="margin-top: 8px;">
          <div class="insight-header"><div class="insight-icon"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/></svg></div><h3 class="insight-title">Operational Insights</h3></div>
          <div class="insight-list" id="insightList"></div>
          <p class="analytics-disclaimer">Insights are generated from historical account activity.</p>
        </div>
        <div class="report-detail-table-wrap" id="insightsMetaTable"></div>`;
    }
  };

  /* ——— Chart & KPI engines (render only if container exists) ——— */

  function getCanonicalBalances() {
    const b = global.ntAccountBalances || {};
    const checking = Number(b.checking) || 318750.00;
    const savings = Number(b.savings) || 35180.38;
    const credit = Number(b.credit) || 200.00;
    const invest = Number(b.invest) || 169400000.00;
    const total = checking + savings + credit + invest;
    return {
      checking: checking,
      savings: savings,
      credit: credit,
      invest: invest,
      total: total,
      accounts: [
        { name: 'Checking Account', value: checking },
        { name: 'Savings Vault', value: savings },
        { name: 'Managed Portfolio', value: invest },
        { name: 'NT Premium Credit Line', value: credit }
      ]
    };
  }

  function scopedTotalBalance(scope) {
    const balances = getCanonicalBalances();
    if (scope === 'checking') return balances.checking;
    if (scope === 'savings') return balances.savings;
    if (scope === 'credit') return balances.credit;
    if (scope === 'invest') return balances.invest;
    return balances.total;
  }

  function getTransferBreakdownFromLedger() {
    const ledger = global.masterTxLedger || [];
    const counts = { Internal: 0, ACH: 0, Wire: 0, International: 0 };
    ledger.forEach(function (tx) {
      if (tx.type === 'Internal') counts.Internal++;
      else if (tx.type === 'ACH') counts.ACH++;
      else if (tx.type === 'Wire') counts.Wire++;
      else if (tx.type === 'International') counts.International++;
    });
    return counts;
  }

  function getTransferStatsFromLedger() {
    const ledger = global.masterTxLedger || [];
    const total = ledger.length || 1;
    const settled = ledger.filter(function (tx) { return tx.status === 'Settled'; }).length;
    const failed = ledger.filter(function (tx) {
      return tx.status === 'Failed' || tx.status === 'Returned';
    }).length;
    return {
      total: total,
      settled: settled,
      failed: failed,
      approvalRate: total ? (settled / total) * 100 : 0,
      failedRate: total ? (failed / total) * 100 : 0
    };
  }

  function calculateKPIs() {
    const f = global.analyticsData.filters;
    let mult = f.timeRange === 'today' ? 0.1 : f.timeRange === '30d' ? 4 : f.timeRange === 'quarter' ? 12 : f.timeRange === 'year' ? 52 : 1;
    const api = global.NTApi?.state?.analytics?.kpis;
    if (api && global.NTApi?.state?.connected) {
      global.analyticsData.kpis = {
        totalBalance: Number(api.totalBalance),
        netCashFlow: Number(api.netCashFlow),
        totalTransfers: Number(api.totalTransfers),
        fxFeesPaid: Number(api.fxFeesPaid),
        approvalRate: Number(api.approvalRate),
        failedRate: Number(api.failedRate),
        portfolioPerformance: Number(api.portfolioPerformance)
      };
      return;
    }
    const transferStats = getTransferStatsFromLedger();
    global.analyticsData.kpis = {
      totalBalance: scopedTotalBalance(f.accountScope),
      netCashFlow: 1852 * mult,
      totalTransfers: Math.max(1, Math.round(transferStats.total * mult)),
      fxFeesPaid: 125 * mult,
      approvalRate: transferStats.approvalRate,
      failedRate: transferStats.failedRate,
      portfolioPerformance: 12.4
    };
  }

  function renderKPICards() {
    const container = document.getElementById('kpiCardsContainer');
    if (!container) return;
    const kpis = [
      { label: 'Total Balance', value: global.analyticsData.kpis.totalBalance, format: 'currency', trend: liveAnalytics()?.kpis?.monthOverMonthChangePct || 2.4, positive: true },
      { label: 'Net Cash Flow', value: global.analyticsData.kpis.netCashFlow, format: 'currency', trend: 12, positive: true },
      { label: 'Total Transfers', value: global.analyticsData.kpis.totalTransfers, format: 'number', trend: 8.5, positive: true },
      { label: 'FX Fees Paid', value: global.analyticsData.kpis.fxFeesPaid, format: 'currency', trend: -5.2, positive: false },
      { label: 'Approval Rate', value: global.analyticsData.kpis.approvalRate, format: 'percent', trend: 1.2, positive: true },
      { label: 'Failed Rate', value: global.analyticsData.kpis.failedRate, format: 'percent', trend: -0.8, positive: true },
      { label: 'Portfolio Performance', value: global.analyticsData.kpis.portfolioPerformance, format: 'percent', trend: 3.1, positive: true }
    ];
    container.innerHTML = kpis.map(function (kpi) {
      const v = kpi.format === 'currency' ? '$' + Number(kpi.value).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
        : kpi.format === 'percent' ? Number(kpi.value).toFixed(1) + '%' : Number(kpi.value).toLocaleString();
      const tc = kpi.positive ? 'positive' : 'negative';
      return '<div class="kpi-card" role="button" tabindex="0" onclick="openDrilldown(\'' + kpi.label + '\')"><div class="kpi-label">' + kpi.label + '</div><div class="kpi-value">' + v + '</div><div class="kpi-trend ' + tc + '"><span>' + (kpi.trend > 0 ? '↑' : '↓') + ' ' + Math.abs(kpi.trend) + '%</span></div></div>';
    }).join('');
  }

  function isLiveAnalytics() {
    return !!(global.NTApi?.state?.connected && global.NTApi?.state?.analytics);
  }

  function liveAnalytics() {
    return isLiveAnalytics() ? global.NTApi.state.analytics : null;
  }

  function getAnalyticsQueryParams() {
    syncFiltersFromDom();
    const f = global.analyticsData.filters;
    return {
      timeRange: f.timeRange,
      accountScope: f.accountScope,
      currency: f.currency,
      txType: f.txType
    };
  }

  function chartDataFromApi() {
    if (!isLiveAnalytics()) {
      return { charts: null, breakdown: null, tables: null };
    }
    const a = global.NTApi.state.analytics;
    return { charts: a.charts || null, breakdown: a.transferBreakdown || null, tables: a.tables || null };
  }

  function getCashFlowSeriesFallback() {
    const labels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const inflow = new Array(labels.length).fill(0);
    const outflow = new Array(labels.length).fill(0);

    const rows = Array.isArray(global.unifiedMock) ? global.unifiedMock : [];
    rows.forEach(function (tx) {
      const d = new Date(tx.date);
      if (Number.isNaN(d.getTime())) return;
      const idx = (d.getDay() + 6) % 7;
      const amt = Number(tx.amount);
      if (!Number.isFinite(amt) || amt === 0) return;
      if (amt > 0) inflow[idx] += amt;
      if (amt < 0) outflow[idx] += Math.abs(amt);
    });

    const maxV = Math.max.apply(null, inflow.concat(outflow)) || 0;
    if (maxV > 0) return { days: labels, inflow: inflow, outflow: outflow };

    return {
      days: labels,
      inflow: [1200, 950, 1852, 880, 1420, 450, 620],
      outflow: [350, 420, 280, 1245, 185, 87, 24]
    };
  }

  function renderCashFlowChart() {
    const el = document.getElementById('cashFlowChart');
    if (!el) return;
    const api = chartDataFromApi().charts;
    const hasApiLabels = isLiveAnalytics() && Array.isArray(api?.cashFlowLabels) && api.cashFlowLabels.length > 0;
    const hasApiInflow = isLiveAnalytics() && Array.isArray(api?.cashFlowInflow) && api.cashFlowInflow.length > 0;
    const hasApiOutflow = isLiveAnalytics() && Array.isArray(api?.cashFlowOutflow) && api.cashFlowOutflow.length > 0;

    const fallback = getCashFlowSeriesFallback();
    const days = hasApiLabels ? api.cashFlowLabels : fallback.days;
    const inflow = hasApiInflow ? api.cashFlowInflow.map(Number) : fallback.inflow;
    const outflow = hasApiOutflow ? api.cashFlowOutflow.map(Number) : fallback.outflow;
    const h = 200, bw = 20, gap = 35;
    const seriesMax = Math.max.apply(null, days.map(function (_, i) {
      const inV = Math.abs(Number(inflow[i]) || 0);
      const outV = Math.abs(Number(outflow[i]) || 0);
      return Math.max(inV, outV);
    })) || 0;
    if (seriesMax <= 0) {
      el.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:180px;color:#64748b;font-size:12px;">No cash flow data available</div>';
      return;
    }

    const w = Math.max(400, days.length * (bw * 2 + gap) + 40);
    let svg = '<svg width="100%" viewBox="0 0 ' + w + ' ' + h + '">';
    days.forEach(function (day, i) {
      const x = i * (bw * 2 + gap) + 20;
      const inV = Math.abs(Number(inflow[i]) || 0);
      const outV = Math.abs(Number(outflow[i]) || 0);
      const ih = (inV / seriesMax) * (h - 50);
      const oh = (outV / seriesMax) * (h - 50);
      svg += '<rect x="' + x + '" y="' + (h - ih - 20) + '" width="' + bw + '" height="' + ih + '" fill="#115740" rx="2"/>';
      svg += '<rect x="' + (x + bw + 2) + '" y="' + (h - oh - 20) + '" width="' + bw + '" height="' + oh + '" fill="#b3995d" rx="2"/>';
      svg += '<text x="' + (x + bw) + '" y="' + (h - 5) + '" text-anchor="middle" font-size="10" fill="#64748b">' + (day || ('Day ' + (i + 1))) + '</text>';
    });
    svg += '</svg><div class="chart-legend"><div class="legend-item"><div class="legend-color" style="background:#115740"></div><span>Inflow</span></div><div class="legend-item"><div class="legend-color" style="background:#b3995d"></div><span>Outflow</span></div></div>';
    el.innerHTML = svg;
  }

  function renderTransferActivityChart() {
    const el = document.getElementById('transferActivityChart');
    if (!el) return;
    const b = isLiveAnalytics() ? chartDataFromApi().breakdown : getTransferBreakdownFromLedger();
    const data = [
      { type: 'Internal', value: b.Internal || 0, color: '#115740' },
      { type: 'ACH', value: b.ACH || 0, color: '#b3995d' },
      { type: 'Wire', value: b.Wire || 0, color: '#075985' },
      { type: 'International', value: b.International || 0, color: '#d97706' }
    ];
    const maxV = Math.max.apply(null, data.map(function (d) { return d.value; })) || 1;
    let svg = '<svg width="100%" viewBox="0 0 400 200">';
    data.forEach(function (item, i) {
      const y = i * 50 + 20;
      const barW = (item.value / maxV) * 280;
      svg += '<text x="0" y="' + (y + 18) + '" font-size="11" fill="#475569" font-weight="600">' + item.type + '</text>';
      svg += '<rect x="80" y="' + y + '" width="' + barW + '" height="28" fill="' + item.color + '" rx="4"/>';
      svg += '<text x="' + (barW + 90) + '" y="' + (y + 18) + '" font-size="12" font-weight="700">' + item.value + '</text>';
    });
    svg += '</svg>';
    el.innerHTML = svg;
  }

  function buildAccountDistributionData(slices) {
    const colors = ['#115740', '#b3995d', '#075985', '#dc2626'];
    const source = slices && slices.length ? slices : getCanonicalBalances().accounts;
    const total = source.reduce(function (sum, x) { return sum + Number(x.value); }, 0) || 1;
    return source.map(function (s, i) {
      return {
        name: s.name,
        value: Math.round(Number(s.value) / total * 100),
        color: colors[i % colors.length]
      };
    });
  }

  function renderAccountDistributionChart() {
    const el = document.getElementById('accountDistributionChart');
    if (!el) return;
    const slices = isLiveAnalytics() ? chartDataFromApi().charts?.accountDistribution : null;
    const data = buildAccountDistributionData(slices);
    const total = data.reduce(function (s, d) { return s + d.value; }, 0) || 1;
    const w = 400, h = 200, cx = 140, cy = 100, r = 70;
    let svg = '<svg width="100%" viewBox="0 0 ' + w + ' ' + h + '">';
    var start = 0;
    data.forEach(function (item) {
      var ang = (item.value / total) * 2 * Math.PI;
      var x1 = cx + r * Math.cos(start), y1 = cy + r * Math.sin(start);
      var x2 = cx + r * Math.cos(start + ang), y2 = cy + r * Math.sin(start + ang);
      var large = ang > Math.PI ? 1 : 0;
      svg += '<path d="M ' + cx + ' ' + cy + ' L ' + x1 + ' ' + y1 + ' A ' + r + ' ' + r + ' 0 ' + large + ' 1 ' + x2 + ' ' + y2 + ' Z" fill="' + item.color + '" stroke="#fff" stroke-width="2"/>';
      start += ang;
    });
    svg += '<circle cx="' + cx + '" cy="' + cy + '" r="40" fill="#fff"/>';
    var ly = 20;
    data.forEach(function (item) {
      svg += '<rect x="260" y="' + ly + '" width="12" height="12" fill="' + item.color + '" rx="2"/>';
      svg += '<text x="278" y="' + (ly + 10) + '" font-size="10" fill="#475569">' + item.name + ' (' + item.value + '%)</text>';
      ly += 22;
    });
    svg += '</svg>';
    el.innerHTML = svg;
  }

  function renderFXExposureChart() {
    const el = document.getElementById('fxExposureChart');
    if (!el) return;
    var data = isLiveAnalytics() ? (chartDataFromApi().charts?.fxExposure || []) : [
      { currency: 'USD', exposure: 85, volatility: 'Low' },
      { currency: 'EUR', exposure: 8, volatility: 'Medium' },
      { currency: 'GBP', exposure: 4, volatility: 'Medium' },
      { currency: 'JPY', exposure: 2, volatility: 'High' },
      { currency: 'CHF', exposure: 1, volatility: 'Low' }
    ];
    var html = '<div style="display:flex;flex-direction:column;gap:12px;padding:12px;">';
    data.forEach(function (item) {
      var vc = item.volatility === 'Low' ? 'risk-low' : item.volatility === 'Medium' ? 'risk-medium' : 'risk-high';
      html += '<div style="display:flex;align-items:center;gap:12px;"><span style="width:40px;font-weight:600;font-size:12px;">' + item.currency + '</span><div style="flex:1;height:22px;background:#f1f5f9;border-radius:4px;"><div style="width:' + item.exposure + '%;height:100%;background:linear-gradient(90deg,#115740,#b3995d);border-radius:4px;"></div></div><span style="width:36px;font-weight:700;font-size:12px;">' + item.exposure + '%</span><span class="risk-heatmap-cell ' + vc + '" style="width:56px;text-align:center;font-size:10px;">' + item.volatility + '</span></div>';
    });
    html += '</div>';
    el.innerHTML = html;
  }

  function renderRiskComplianceChart() {
    const el = document.getElementById('riskComplianceChart');
    if (!el) return;
    var data = isLiveAnalytics() ? (chartDataFromApi().charts?.riskCompliance || []) : [
      { category: 'High-Risk Transfers', count: 3, level: 'critical' },
      { category: 'Flagged Transactions', count: 12, level: 'high' },
      { category: 'Compliance Holds', count: 5, level: 'medium' },
      { category: 'Sanction Triggers', count: 0, level: 'low' }
    ];
    var html = '<div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;padding:12px;">';
    data.forEach(function (item) {
      html += '<div class="risk-heatmap-cell risk-' + item.level + '" style="padding:16px;text-align:center;"><div style="font-size:22px;font-weight:700;">' + item.count + '</div><div style="font-size:9px;text-transform:uppercase;margin-top:4px;">' + item.category + '</div></div>';
    });
    html += '</div>';
    el.innerHTML = html;
  }

  function renderApprovalPerformanceChart() {
    const el = document.getElementById('approvalPerformanceChart');
    if (!el) return;
    var apiFunnel = isLiveAnalytics() ? chartDataFromApi().charts?.approvalFunnel : null;
    var initiated = apiFunnel ? apiFunnel.reduce(function (max, item) { return Math.max(max, Number(item.value) || 0); }, 0) : 0;
    var data = apiFunnel ? apiFunnel.map(function (item) {
      var count = Number(item.value) || 0;
      return {
        stage: item.stage,
        value: initiated ? Math.round(count / initiated * 100) : count,
        count: count,
        color: item.color || '#94a3b8'
      };
    }) : [
      { stage: 'Initiated', value: 100, count: 100, color: '#94a3b8' },
      { stage: 'Pending Approval', value: 75, count: 75, color: '#d97706' },
      { stage: 'Approved', value: 65, count: 65, color: '#115740' },
      { stage: 'Rejected', value: 8, count: 8, color: '#dc2626' },
      { stage: 'Escalated', value: 2, count: 2, color: '#7c3aed' }
    ];
    var html = '<div class="approval-funnel" style="display:flex;flex-direction:column;gap:12px;padding:12px;">';
    data.forEach(function (item) {
      html += '<div class="approval-funnel-stage" role="button" tabindex="0" style="cursor:pointer;padding:12px;background:#fff;border:1px solid #e2e8f0;border-radius:8px;" onclick="openDrilldown(\'' + item.stage + ' Approvals\')">' +
        '<div style="display:flex;justify-content:space-between;font-size:12px;font-weight:600;margin-bottom:8px;"><span>' + item.stage + '</span><span>' + (item.count != null ? item.count : item.value) + '</span></div>' +
        '<div style="height:8px;background:#f1f5f9;border-radius:4px;overflow:hidden;"><div style="width:' + item.value + '%;height:100%;background:' + item.color + ';border-radius:4px;"></div></div>' +
        '</div>';
    });
    html += '</div>';
    el.innerHTML = html;
  }

  function generateInsights() {
    var f = global.analyticsData.filters || { timeRange: '7d', accountScope: 'all', currency: 'all', txType: 'all' };
    var k = global.analyticsData.kpis || {};

    function startDateForRange(timeRange) {
      var now = new Date();
      var d = new Date(now);
      if (timeRange === 'today') {
        d.setHours(0, 0, 0, 0);
        return d;
      }
      var days = timeRange === '30d' ? 30 : timeRange === 'quarter' ? 90 : timeRange === 'year' ? 365 : 7;
      d.setDate(d.getDate() - days);
      return d;
    }

    function isWithinRange(dateStr, start) {
      if (!dateStr) return false;
      var dt = new Date(dateStr);
      if (Number.isNaN(dt.getTime())) return false;
      return dt.getTime() >= start.getTime();
    }

    function normalizeScopeSource(src) {
      return String(src || '').toLowerCase();
    }

    function matchesAccountScope(tx) {
      if (!f.accountScope || f.accountScope === 'all') return true;
      var s = normalizeScopeSource(tx.source || tx.account || tx.accountName);
      if (f.accountScope === 'checking') return s.indexOf('checking') >= 0;
      if (f.accountScope === 'savings') return s.indexOf('savings') >= 0 || s.indexOf('vault') >= 0;
      if (f.accountScope === 'credit') return s.indexOf('credit') >= 0 || s.indexOf('line') >= 0;
      if (f.accountScope === 'invest') return s.indexOf('portfolio') >= 0 || s.indexOf('managed') >= 0 || s.indexOf('invest') >= 0;
      return true;
    }

    function matchesCurrency(tx) {
      if (!f.currency || f.currency === 'all') return true;
      var c = (tx.currency || tx.ccy || '').toString().toUpperCase();
      return c ? c === f.currency : f.currency === 'USD';
    }

    function txTypeBucket(tx) {
      var t = String(tx.type || '').toLowerCase();
      var desc = String(tx.desc || tx.description || tx.asset || '').toLowerCase();
      if (t === 'ach' || t === 'wire' || t === 'internal' || t === 'international' || t === 'transfer') return 'transfers';
      if (t.indexOf('fee') >= 0 || desc.indexOf('fee') >= 0) return 'fees';
      if (t.indexOf('fx') >= 0 || desc.indexOf('fx') >= 0) return 'fx';
      if (t.indexOf('invest') >= 0 || desc.indexOf('buy') >= 0 || desc.indexOf('dividend') >= 0 || t.indexOf('dividend') >= 0) return 'investments';
      if (t.indexOf('approval') >= 0) return 'approvals';
      return 'other';
    }

    function matchesTxType(tx) {
      if (!f.txType || f.txType === 'all') return true;
      return txTypeBucket(tx) === f.txType;
    }

    var start = startDateForRange(f.timeRange);

    var unified = Array.isArray(global.unifiedMock) ? global.unifiedMock : [];
    var net = unified.reduce(function (sum, tx) {
      if (!isWithinRange(tx.date, start)) return sum;
      if (!matchesAccountScope(tx)) return sum;
      if (!matchesCurrency(tx)) return sum;
      if (!matchesTxType(tx)) return sum;
      return sum + (Number(tx.amount) || 0);
    }, 0);

    var pending = Array.isArray(global.mockPendingApprovals) ? global.mockPendingApprovals : [];
    var pendingCount = pending.reduce(function (count, a) {
      if (!isWithinRange(a.timestamp, start)) return count;
      var st = String(a.status || '').toLowerCase();
      if (st.indexOf('pending') >= 0 || st.indexOf('review') >= 0) return count + 1;
      return count;
    }, 0);

    var ledger = Array.isArray(global.masterTxLedger) ? global.masterTxLedger : [];
    var intlCount = ledger.reduce(function (count, tx) {
      if (String(tx.type || '') !== 'International') return count;
      if (!isWithinRange(tx.date, start)) return count;
      if (!matchesCurrency(tx)) return count;
      if (!matchesAccountScope(tx)) return count;
      if (!matchesTxType({ type: 'International' })) return count;
      return count + 1;
    }, 0);

    function isHighRiskStatus(status) {
      var s = String(status || '').toLowerCase();
      return s.indexOf('ofac') >= 0 || s.indexOf('compliance') >= 0 || s.indexOf('hold') >= 0 || s.indexOf('sanction') >= 0 || s.indexOf('flag') >= 0;
    }

    var highRiskCount = 0;
    highRiskCount += ledger.reduce(function (count, tx) {
      if (!isWithinRange(tx.date, start)) return count;
      if (!isHighRiskStatus(tx.status)) return count;
      if (!matchesCurrency(tx)) return count;
      if (!matchesAccountScope(tx)) return count;
      return count + 1;
    }, 0);
    highRiskCount += pending.reduce(function (count, a) {
      if (!isWithinRange(a.timestamp, start)) return count;
      var lvl = String(a.riskLevel || '').toLowerCase();
      var score = Number(a.riskScore);
      if (lvl === 'high' || lvl === 'critical' || (Number.isFinite(score) && score >= 80) || isHighRiskStatus(a.status)) return count + 1;
      return count;
    }, 0);

    var roi = Number(k.portfolioPerformance);
    var roiText = Number.isFinite(roi) ? roi.toFixed(1) : '0.0';
    var roiBasis = isLiveAnalytics() ? 'based on live analytics feed.' : 'based on statement-derived activity.';

    var approval = Number(k.approvalRate);
    var approvalText = Number.isFinite(approval) ? approval.toFixed(1) : '0.0';

    function money(n) {
      var x = Number(n) || 0;
      return '$' + x.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    global.analyticsData.insights = [
      'Managed portfolio ROI is ' + roiText + '% ' + roiBasis,
      'Net cash flow for the selected period is ' + money(net) + ' from statement activity.',
      pendingCount + ' transfer(s) in the selected window still require authorization.',
      intlCount + ' international transfer(s) recorded in the selected period.',
      highRiskCount + ' high-risk transfer(s) flagged for compliance review.',
      'Approval rate is ' + approvalText + '% across filtered transfer activity.'
    ];
  }

  function renderInsights() {
    var el = document.getElementById('insightList');
    if (!el) return;
    el.innerHTML = global.analyticsData.insights.map(function (t) {
      return '<div class="insight-item"><div class="insight-bullet"></div><div class="insight-text">' + t + '</div></div>';
    }).join('');
  }

  function renderDetailTables() {
    var live = liveAnalytics();
    var tables = live?.tables;

    var cashTbl = document.getElementById('cashFlowDetailTable');
    if (cashTbl) {
      if (isLiveAnalytics() && tables?.cashFlowDetail?.length) {
        var liveCashRows = tables.cashFlowDetail.map(function (row) {
          var net = Number(row.net) || 0;
          return '<tr><td>' + row.day + '</td><td>$' + Number(row.inflow).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) +
            '</td><td>$' + Number(row.outflow).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) +
            '</td><td style="color:' + (net >= 0 ? '#115740' : '#dc2626') + ';font-weight:600;">$' +
            net.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + '</td></tr>';
        }).join('');
        cashTbl.innerHTML = detailTableHtml('Daily Cash Flow Detail', ['Day', 'Inflow', 'Outflow', 'Net'], liveCashRows);
      } else {
        var days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
        var rows = days.map(function (d, i) {
          return '<tr><td>' + d + '</td><td>$' + (1200 + i * 80).toLocaleString() + '</td><td>$' + (800 + i * 40).toLocaleString() + '</td><td style="color:#115740;font-weight:600;">$' + (400 + i * 40).toLocaleString() + '</td></tr>';
        }).join('');
        cashTbl.innerHTML = detailTableHtml('Daily Cash Flow Detail', ['Day', 'Inflow', 'Outflow', 'Net'], rows);
      }
    }
    var trTbl = document.getElementById('transferDetailTable');
    if (trTbl) {
      if (isLiveAnalytics() && tables?.transferDetail?.length) {
        var liveTransferRows = tables.transferDetail.map(function (row) {
          return '<tr><td>' + row.type + '</td><td>' + row.count + '</td><td>$' +
            Number(row.volume).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 }) +
            '</td><td>$' + Number(row.avgSize).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 }) + '</td></tr>';
        }).join('');
        trTbl.innerHTML = detailTableHtml('Transfer Summary', ['Type', 'Count', 'Volume', 'Avg Size'], liveTransferRows);
      } else {
        var breakdown = getTransferBreakdownFromLedger();
        var ledger = global.masterTxLedger || [];
        var transferRows = ['Internal', 'ACH', 'Wire', 'International'].map(function (type) {
          var count = breakdown[type] || 0;
          var volume = ledger.filter(function (tx) { return tx.type === type; }).reduce(function (sum, tx) {
            return sum + Math.abs(Number(tx.amount) || 0);
          }, 0);
          var avg = count ? volume / count : 0;
          return '<tr><td>' + type + '</td><td>' + count + '</td><td>$' + volume.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 }) +
            '</td><td>$' + avg.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 }) + '</td></tr>';
        }).join('');
        trTbl.innerHTML = detailTableHtml('Transfer Summary', ['Type', 'Count', 'Volume', 'Avg Size'], transferRows);
      }
    }
    var allocTbl = document.getElementById('allocationDetailTable');
    if (allocTbl) {
      if (isLiveAnalytics() && tables?.allocationDetail?.length) {
        var liveAllocRows = tables.allocationDetail.map(function (row) {
          var change = row.dayChange || '—';
          var changeStyle = String(change).indexOf('-') === 0 ? 'color:#dc2626' : String(change).indexOf('+') === 0 ? 'color:#16a34a' : '';
          return '<tr><td>' + row.account + '</td><td>$' + Number(row.marketValue).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) +
            '</td><td>' + row.weight + '%</td><td' + (changeStyle ? ' style="' + changeStyle + '"' : '') + '>' + change + '</td></tr>';
        }).join('');
        allocTbl.innerHTML = detailTableHtml('Account Breakdown', ['Account', 'Market Value', 'Weight', 'Day Change'], liveAllocRows);
      } else {
        var balances = getCanonicalBalances();
        var dayChanges = {
          'Checking Account': '-0.2%',
          'Savings Vault': '+0.4%',
          'Managed Portfolio': '+1.2%',
          'NT Premium Credit Line': '—'
        };
        var allocRows = balances.accounts.map(function (account) {
          var weight = balances.total ? (account.value / balances.total * 100).toFixed(1) : '0.0';
          var change = dayChanges[account.name] || '—';
          var changeStyle = change.indexOf('-') === 0 ? 'color:#dc2626' : change.indexOf('+') === 0 ? 'color:#16a34a' : '';
          return '<tr><td>' + account.name + '</td><td>$' + Number(account.value).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) +
            '</td><td>' + weight + '%</td><td' + (changeStyle ? ' style="' + changeStyle + '"' : '') + '>' + change + '</td></tr>';
        }).join('');
        allocTbl.innerHTML = detailTableHtml('Account Breakdown', ['Account', 'Market Value', 'Weight', 'Day Change'], allocRows);
      }
    }
    var fxTbl = document.getElementById('fxDetailTable');
    if (fxTbl) {
      if (isLiveAnalytics() && tables?.fxDetail?.length) {
        var liveFxRows = tables.fxDetail.map(function (row) {
          return '<tr><td>' + row.currency + '</td><td>' + row.exposure + '</td><td>' + row.volatility + '</td><td>$' +
            Number(row.feesYtd).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + '</td></tr>';
        }).join('');
        fxTbl.innerHTML = detailTableHtml('FX Positions', ['Currency', 'Exposure %', 'Volatility', 'Fees YTD'], liveFxRows);
      } else {
        fxTbl.innerHTML = detailTableHtml('FX Positions', ['Currency', 'Exposure %', 'Volatility', 'Fees YTD'],
          '<tr><td>USD</td><td>85%</td><td>Low</td><td>$42.00</td></tr><tr><td>EUR</td><td>8%</td><td>Medium</td><td>$28.50</td></tr><tr><td>GBP</td><td>4%</td><td>Medium</td><td>$12.00</td></tr>');
      }
    }
    var riskTbl = document.getElementById('riskDetailTable');
    if (riskTbl) {
      if (isLiveAnalytics() && tables?.riskDetail?.length) {
        var liveRiskRows = tables.riskDetail.map(function (row) {
          return '<tr><td>' + row.category + '</td><td>' + row.count + '</td><td>' + row.severity + '</td><td>' + row.lastUpdated + '</td></tr>';
        }).join('');
        riskTbl.innerHTML = detailTableHtml('Open Risk Events', ['Category', 'Count', 'Severity', 'Last Updated'], liveRiskRows);
      } else {
        riskTbl.innerHTML = detailTableHtml('Open Risk Events', ['Category', 'Count', 'Severity', 'Last Updated'],
          '<tr><td>OFAC Hold</td><td>2</td><td>Critical</td><td>Today</td></tr><tr><td>Policy Limit</td><td>5</td><td>High</td><td>Yesterday</td></tr><tr><td>Velocity Alert</td><td>3</td><td>Medium</td><td>2 days ago</td></tr>');
      }
    }
    var apprTbl = document.getElementById('approvalDetailTable');
    if (apprTbl) {
      if (isLiveAnalytics() && tables?.approvalDetail?.length) {
        var liveApprRows = tables.approvalDetail.map(function (row) {
          return '<tr><td>' + row.stage + '</td><td>' + row.count + '</td><td>' + row.avgTime + '</td><td>' + row.slaMet + '</td></tr>';
        }).join('');
        apprTbl.innerHTML = detailTableHtml('Approval SLA', ['Stage', 'Count', 'Avg Time', 'SLA Met'], liveApprRows);
      } else {
        apprTbl.innerHTML = detailTableHtml('Approval SLA', ['Stage', 'Count', 'Avg Time', 'SLA Met'],
          '<tr><td>Pending</td><td>8</td><td>2.4 hrs</td><td>94%</td></tr><tr><td>Approved</td><td>42</td><td>18 min</td><td>99%</td></tr><tr><td>Rejected</td><td>3</td><td>45 min</td><td>100%</td></tr>');
      }
    }
    var insTbl = document.getElementById('insightsMetaTable');
    if (insTbl) {
      insTbl.innerHTML = detailTableHtml('Export Catalog', ['Report', 'Format', 'Description'],
        '<tr><td>Portfolio Dashboard</td><td>PDF / CSV</td><td>Full KPI and chart pack</td></tr><tr><td>Audit Trail</td><td>JSON</td><td>Machine-readable event log</td></tr><tr><td>Compliance Summary</td><td>PDF</td><td>Risk and approval metrics</td></tr>');
    }
  }

  function detailTableHtml(title, headers, bodyRows) {
    var head = headers.map(function (h) { return '<th>' + h + '</th>'; }).join('');
    return '<div class="report-detail-table"><div class="report-detail-table__title">' + title + '</div><table class="drilldown-table"><thead><tr>' + head + '</tr></thead><tbody>' + bodyRows + '</tbody></table></div>';
  }

  function syncFiltersFromDom() {
    global.analyticsData.filters.timeRange = document.getElementById('analyticsTimeRange')?.value || '7d';
    global.analyticsData.filters.accountScope = document.getElementById('analyticsAccountScope')?.value || 'all';
    global.analyticsData.filters.currency = document.getElementById('analyticsCurrency')?.value || 'all';
    global.analyticsData.filters.txType = document.getElementById('analyticsTxType')?.value || 'all';
  }

  function updateAnalytics() {
    syncFiltersFromDom();
    calculateKPIs();
    renderKPICards();
    renderCashFlowChart();
    renderTransferActivityChart();
    renderAccountDistributionChart();
    renderFXExposureChart();
    renderRiskComplianceChart();
    renderApprovalPerformanceChart();
    generateInsights();
    renderInsights();
    renderDetailTables();
  }

  function startRefresh() {
    if (analyticsRefreshInterval) clearInterval(analyticsRefreshInterval);
    analyticsRefreshInterval = setInterval(function () {
      if (!isReportingView(global.currentActiveView)) return;
      if (typeof global.updateAnalytics === 'function') {
        global.updateAnalytics();
      } else {
        updateAnalytics();
      }
    }, 30000);
  }

  function renderView(viewName) {
    var v = normalizeView(viewName);
    if (!TEMPLATES[v]) v = 'AnalyticsDashboard';
    currentReportingView = v;
    var container = document.getElementById('dynamicContainer');
    if (!container) return;
    container.innerHTML = TEMPLATES[v]();
    // Always call updateAnalytics — use the global one if available
    if (typeof global.updateAnalytics === 'function') {
      global.updateAnalytics();
    } else {
      updateAnalytics();
    }
    startRefresh();
  }

  function getSectionMeta(viewName) {
    var v = normalizeView(viewName);
    return SECTION_META[v] || SECTION_META.AnalyticsDashboard;
  }

  function getLabel(viewName) {
    return VIEW_LABELS[normalizeView(viewName)] || viewName;
  }

  /* Drilldown — KPI card detail drawer */
  global.openDrilldown = function (title) {
    var drawer = document.getElementById('drilldownDrawer');
    var overlay = document.getElementById('drilldownOverlay');
    var titleEl = document.getElementById('drilldownTitle');
    var contentEl = document.getElementById('drilldownContent');
    if (!drawer || !contentEl) return;
    titleEl.textContent = title;

    var transactions = [];
    var unified = global.unifiedMock || [];
    var invest = global.investMock || [];
    var pending = global.mockPendingApprovals || [];

    if (title.indexOf('FX') >= 0) {
      transactions = unified.filter(function (tx) {
        return (tx.desc || '').toLowerCase().indexOf('fee') >= 0;
      }).slice(0, 10);
    } else if (title.indexOf('Approval') >= 0 || title.indexOf('Failed') >= 0) {
      transactions = pending.map(function (tx) {
        return {
          date: new Date(tx.timestamp).toISOString().split('T')[0],
          desc: tx.type + ' to ' + tx.beneficiary,
          amount: tx.amount,
          status: tx.status
        };
      }).slice(0, 10);
    } else if (title.indexOf('Portfolio') >= 0) {
      transactions = invest.slice(0, 10);
    } else {
      transactions = unified.slice(0, 10);
    }

    var tableRows = transactions.map(function (tx) {
      var date = tx.date || new Date().toISOString().split('T')[0];
      var desc = tx.desc || tx.type || 'Transaction';
      var amount = tx.amount !== undefined ? Math.abs(tx.amount) : 0;
      var status = tx.status || 'Completed';
      return '<tr><td>' + date + '</td><td>' + desc + '</td><td>$' + amount.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + '</td><td>' + status + '</td></tr>';
    }).join('');

    var totalVol = transactions.reduce(function (sum, tx) {
      return sum + (tx.amount !== undefined ? Math.abs(tx.amount) : 0);
    }, 0);
    var filters = global.analyticsData.filters;

    // Store drilldown data globally for export
    global.currentDrilldownData = {
      title: title,
      transactions: transactions,
      totalVolume: totalVol,
      filters: filters,
      generatedAt: new Date().toISOString()
    };

    contentEl.innerHTML =
      '<div class="drilldown-section"><div class="drilldown-section-title">Breakdown View</div>' +
      '<table class="drilldown-table"><thead><tr><th>Date</th><th>Description</th><th>Amount</th><th>Status</th></tr></thead><tbody>' +
      (tableRows || '<tr><td colspan="4" style="text-align:center;color:#94a3b8;padding:20px;">No transactions found</td></tr>') +
      '</tbody></table></div>' +
      '<div class="drilldown-section"><div class="drilldown-section-title">Summary</div>' +
      '<div style="font-size:12px;color:#475569;line-height:1.8;">' +
      '<div><strong>Total Records:</strong> ' + transactions.length + '</div>' +
      '<div><strong>Total Volume:</strong> $' + totalVol.toLocaleString(undefined, { minimumFractionDigits: 2 }) + '</div></div></div>' +
      '<div class="drilldown-section"><div class="drilldown-section-title">Filters</div>' +
      '<div style="font-size:12px;color:#475569;line-height:1.8;">' +
      '<div><strong>Time:</strong> ' + filters.timeRange + '</div>' +
      '<div><strong>Account:</strong> ' + filters.accountScope + '</div>' +
      '<div><strong>Currency:</strong> ' + filters.currency + '</div>' +
      '<div><strong>Updated:</strong> ' + new Date().toLocaleString() + '</div></div></div>';

    drawer.classList.add('active');
    overlay.classList.add('active');
  };

  global.closeDrilldown = global.closeDrilldown || function () {
    document.getElementById('drilldownDrawer')?.classList.remove('active');
    document.getElementById('drilldownOverlay')?.classList.remove('active');
  };

  global.initReportingAnalyticsView = function () { renderView('AnalyticsDashboard'); };
  global.updateAnalytics = function () {
    if (typeof global._ntUpdateAnalyticsAsync === 'function') {
      global._ntUpdateAnalyticsAsync();
      return;
    }
    updateAnalytics();
  };

  global.NTReporting = {
    REPORTING_VIEWS: REPORTING_VIEWS,
    isReportingView: isReportingView,
    normalizeView: normalizeView,
    renderView: renderView,
    updateAnalytics: updateAnalytics,
    getSectionMeta: getSectionMeta,
    getLabel: getLabel,
    calculateKPIs: calculateKPIs,
    renderKPICards: renderKPICards,
    getAnalyticsQueryParams: getAnalyticsQueryParams,
    isLiveAnalytics: isLiveAnalytics
  };
})(window);
