const API_BASE = 'http://localhost/api';

// State Management
let currentView = 'dashboard';
let charts = {};

async function fetchTransactions() {
  try {
    const response = await fetch(`${API_BASE}/accounts/100100100/history`);
    if (!response.ok) throw new Error('API unreachable');
    const data = await response.json();
    renderTransactions(data);
    renderMiniHistory(data.slice(0, 5));
  } catch (error) {
    console.warn('Backend not running, using mock data');
    const mockData = getMockData();
    renderTransactions(mockData);
    renderMiniHistory(mockData.slice(0, 5));
  }
}

function renderTransactions(transactions) {
  const tbody = document.getElementById('transaction-table-body');
  if (!tbody) return;
  
  tbody.innerHTML = '';
  transactions.forEach(tx => {
    const row = document.createElement('tr');
    const amountClass = tx.entryType === 'DEBIT' ? 'amount-debit' : 'amount-credit';
    const amountPrefix = tx.entryType === 'DEBIT' ? '-' : '+';
    const statusClass = getStatusClass(tx.status || 'SUCCESS');

    row.innerHTML = `
      <td style="font-family: monospace; font-weight: 600;">${tx.reference || 'TRX-DEFAULT'}</td>
      <td style="opacity: 0.7;">${new Date(tx.date || Date.now()).toLocaleDateString()}</td>
      <td>${tx.description || 'System Transfer'}</td>
      <td>${tx.recipientName || 'Internal Transfer'}</td>
      <td class="${amountClass}">${amountPrefix}${tx.amount.toLocaleString()} NGN</td>
      <td><span class="status-badge ${statusClass}">${tx.status || 'SUCCESS'}</span></td>
    `;
    tbody.appendChild(row);
  });
}

function renderMiniHistory(transactions) {
  const tbody = document.getElementById('mini-history-body');
  if (!tbody) return;
  tbody.innerHTML = '';
  transactions.forEach(tx => {
    const row = document.createElement('tr');
    const amountClass = tx.entryType === 'DEBIT' ? 'amount-debit' : 'amount-credit';
    const statusClass = getStatusClass(tx.status || 'SUCCESS');

    row.innerHTML = `
      <td style="font-family: monospace; font-size: 0.8rem;">${tx.reference}</td>
      <td class="${amountClass}">${tx.amount.toLocaleString()}</td>
      <td><span class="status-badge ${statusClass}" style="font-size: 0.65rem; padding: 2px 6px;">${tx.status}</span></td>
    `;
    tbody.appendChild(row);
  });
}

function getStatusClass(status) {
  switch (status) {
    case 'SUCCESS': return 'status-success';
    case 'PENDING_PROCESSING': return 'status-pending';
    case 'FAILED': return 'status-failed';
    case 'SENT_TO_RAIL': return 'status-rail';
    default: return 'status-success';
  }
}

// Navigation Logic
function switchView(view) {
  currentView = view;
  
  // Toggle sections
  document.getElementById('dashboard-section').style.display = view === 'dashboard' ? 'block' : 'none';
  document.getElementById('transactions-section').style.display = view === 'cash' ? 'block' : 'none';
  document.getElementById('stats-summary').style.display = view === 'cash' ? 'grid' : 'none';
  document.getElementById('analytics-section').style.display = view === 'analytics' ? 'block' : 'none';
  document.getElementById('global-section').style.display = view === 'global' ? 'block' : 'none';
  document.getElementById('compliance-section').style.display = view === 'compliance' ? 'block' : 'none';
  
  // Update Header Text
  const title = document.getElementById('page-title-text');
  const breadcrumb = document.getElementById('breadcrumb-text');
  const subtitle = document.getElementById('page-subtitle-text');

  const viewData = {
    dashboard: {
      title: 'Good Morning, Deborah',
      bread: 'Family Office > Global Dashboard',
      subtitle: 'Snapshot of cash positions, recent activity, and shortcuts to common money-movement tasks.'
    },
    cash: {
      title: 'Transaction Hub',
      bread: 'Cash Movement and Trading > Money Movement',
      subtitle: 'Monitor balances, initiate transfers, and review recent activity across your family office accounts.'
    },
    analytics: {
      title: 'Performance Insights',
      bread: 'Management > Reporting & Analytics',
      subtitle: 'Portfolio performance, allocation, and monthly transaction volume across linked accounts.'
    },
    global: {
      title: 'Global Payments',
      bread: 'Money Movement > External Transfers',
      subtitle: 'Send domestic or cross-border payments via ACH, wire, or SWIFT with live fee estimates.'
    },
    compliance: {
      title: 'Compliance Portal',
      bread: 'Administration > KYC and Verification',
      subtitle: 'Complete identity verification to lift limits and view screening history for this relationship.'
    }
  };

  if (viewData[view]) {
    title.innerText = viewData[view].title;
    breadcrumb.innerText = viewData[view].bread;
    if (subtitle) subtitle.innerText = viewData[view].subtitle;
  }

  // Update nav active state
  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
  const activeNavId = `nav-${view}`;
  if (document.getElementById(activeNavId)) {
    document.getElementById(activeNavId).classList.add('active');
  }
  
  if (view === 'analytics') initCharts();
}

// Fee Calculation
function updateFees() {
  const amount = parseFloat(document.getElementById('global-amount').value) || 0;
  const rail = document.getElementById('rail-selector').value;
  let fee = 0;

  if (rail === 'WIRE') fee = 5000;
  if (rail === 'SWIFT') fee = 15000 + (amount * 0.01);

  document.getElementById('estimated-fee').innerText = `${fee.toLocaleString()} NGN`;
  document.getElementById('total-deduction').innerText = `${(amount + fee).toLocaleString()} NGN`;
  
  // Show/Hide SWIFT fields
  document.getElementById('swift-fields').style.display = rail === 'SWIFT' ? 'block' : 'none';
}

// Chart Logic
function initCharts() {
  const ctxPerformance = document.getElementById('performanceChart').getContext('2d');
  const ctxAllocation = document.getElementById('allocationChart').getContext('2d');
  const ctxVolume = document.getElementById('volumeChart').getContext('2d');

  // Destroy existing charts if they exist
  Object.values(charts).forEach(chart => chart.destroy());

  // Performance Chart (Line)
  charts.performance = new Chart(ctxPerformance, {
    type: 'line',
    data: {
      labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
      datasets: [{
        label: 'Assets',
        data: [68000, 70000, 71500, 72200, 73425, 75000],
        borderColor: '#004225',
        backgroundColor: 'rgba(0, 66, 37, 0.1)',
        fill: true,
        tension: 0.4
      }, {
        label: 'Target',
        data: [65000, 68000, 70000, 72000, 74000, 76000],
        borderColor: '#b3995d',
        borderDash: [5, 5],
        fill: false,
        tension: 0
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        y: { beginAtZero: false, grid: { color: 'rgba(0,0,0,0.05)' } },
        x: { grid: { display: false } }
      }
    }
  });

  // Allocation Chart (Doughnut)
  charts.allocation = new Chart(ctxAllocation, {
    type: 'doughnut',
    data: {
      labels: ['Cash', 'Equities', 'Fixed Income', 'Real Estate'],
      datasets: [{
        data: [25, 45, 20, 10],
        backgroundColor: ['#004225', '#b3995d', '#075985', '#2c2c2c'],
        borderWidth: 0
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'right', labels: { boxWidth: 12, font: { size: 11 } } }
      },
      cutout: '70%'
    }
  });

  // Volume Chart (Bar)
  charts.volume = new Chart(ctxVolume, {
    type: 'bar',
    data: {
      labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'],
      datasets: [{
        label: 'Inbound',
        data: [1200, 1900, 300, 500, 200],
        backgroundColor: '#004225'
      }, {
        label: 'Outbound',
        data: [800, 1200, 1500, 700, 900],
        backgroundColor: '#b3995d'
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        y: { stacked: true, grid: { color: 'rgba(0,0,0,0.05)' } },
        x: { stacked: true, grid: { display: false } }
      }
    }
  });
}

function getMockData() {
  return [
    { reference: 'TRX-B928X1', date: '2026-05-15T10:00:00', description: 'Monthly Maintenance Fee', recipientName: 'Northern Trust', amount: 15, entryType: 'DEBIT', status: 'SUCCESS' },
    { reference: 'TRX-K291L0', date: '2026-05-16T09:15:00', description: 'Tuition Payment', recipientName: 'Stanford University', amount: 2450, entryType: 'DEBIT', status: 'SENT_TO_RAIL' },
    { reference: 'TRX-V281P9', date: '2026-05-16T12:00:00', description: 'Dividend Payout', recipientName: 'Tesla Inc.', amount: 850, entryType: 'CREDIT', status: 'SUCCESS' },
    { reference: 'TRX-M182C7', date: '2026-05-16T14:30:00', description: 'Internal Funding', recipientName: 'Savings Account', amount: 5000, entryType: 'DEBIT', status: 'PENDING_PROCESSING' }
  ];
}

// Event Listeners
document.addEventListener('DOMContentLoaded', () => {
  fetchTransactions();
  
  document.getElementById('nav-dashboard').addEventListener('click', () => switchView('dashboard'));
  document.getElementById('nav-cash').addEventListener('click', () => switchView('cash'));
  document.getElementById('nav-analytics').addEventListener('click', () => switchView('analytics'));
  document.getElementById('nav-global').addEventListener('click', () => switchView('global'));
  document.getElementById('nav-compliance').addEventListener('click', () => switchView('compliance'));
  document.getElementById('nav-administration').addEventListener('click', () => switchView('compliance')); // Shared for now
  
  document.getElementById('btn-view-all').addEventListener('click', () => switchView('cash'));
  document.getElementById('btn-compliance').addEventListener('click', () => switchView('compliance'));

  // Global Payments Logic
  document.getElementById('global-amount').addEventListener('input', updateFees);
  document.getElementById('rail-selector').addEventListener('change', updateFees);
  
  // Initial Fee Calculation
  updateFees();

  // Start with Dashboard
  switchView('dashboard');
});
