const fs = require('fs');
const path = require('path');
const file = path.join(__dirname, 'index.html');
let html = fs.readFileSync(file, 'utf8');

html = html.replace(
  /      \/\/ Mock data representing robust operational transactions[\s\S]*?      \];\n\n      function initOverviewChart/,
  `      global.transactionsMock = global.transactionsMock || [];
      global.unifiedMock = global.unifiedMock || [];
      global.checkingMock = global.checkingMock || [];
      global.savingsMock = global.savingsMock || [];
      global.creditMock = global.creditMock || [];
      global.investMock = global.investMock || [];
      global.beneficiariesMock = global.beneficiariesMock || [];
      var transactionsMock = global.transactionsMock;
      var unifiedMock = global.unifiedMock;
      var checkingMock = global.checkingMock;
      var savingsMock = global.savingsMock;
      var creditMock = global.creditMock;
      var investMock = global.investMock;
      var beneficiariesMock = global.beneficiariesMock;

      window.ntAccountBal = function (key) {
        if (global.NTApi && typeof global.NTApi.getAccountBalance === 'function') {
          return global.NTApi.getAccountBalance(key);
        }
        const raw = global.ntAccountBalances && global.ntAccountBalances[key];
        return raw != null ? Number(raw) : null;
      };

      window.ntAccountMeta = function (key) {
        if (global.NTApi && typeof global.NTApi.getAccountMeta === 'function') {
          return global.NTApi.getAccountMeta(key);
        }
        return global.ntAccountMeta && global.ntAccountMeta[key] ? global.ntAccountMeta[key] : null;
      };

      window.ntStmtEndBalance = function (tab) {
        const meta = window.ntAccountMeta(tab);
        if (meta) {
          if (tab === 'credit') return Number(meta.owed);
          if (tab === 'invest') return Number(meta.marketValue);
          return Number(meta.balance);
        }
        return window.ntAccountBal(tab);
      };

      window.ntBalanceEntry = function (key) {
        const b = global.NTApi && global.NTApi.state && global.NTApi.state.balances && global.NTApi.state.balances[key];
        if (b) {
          return {
            name: b.name,
            val: Number(b.available != null ? b.available : b.balance),
            limit: Number(b.dailyTransferLimit || b.limit || 0) || null
          };
        }
        const v = window.ntAccountBal(key);
        return v != null ? { name: key, val: v, limit: null } : null;
      };

      function initOverviewChart`
);

html = html.replace(
  /let availableBalance = 8245\.3;[\s\S]*?else if \(source === "invest"\) availableBalance = 17500\.0;/,
  `let availableBalance = window.ntAccountBal(source);
          if (availableBalance == null) {
            alert("Account balances are not loaded yet. Please wait for the dashboard to connect.");
            return;
          }`
);

html = html.replace(
  /if \(stmtActiveTab === "checking"\) endBal = 8245\.3;[\s\S]*?else endBal = 0;/,
  'endBal = window.ntStmtEndBalance(stmtActiveTab) ?? (data.length ? Number(data[0].balance) : 0);'
);

html = html.replace(
  /\? 8245\.3[\s\S]*?: 12500\.0\) - amount/,
  '? (window.ntAccountBal(source) ?? 0)) - amount'
);

html = html.replace(/balance: 8245\.3 - amount/g, 'balance: (window.ntAccountBal("checking") ?? 0) - amount');

html = html.replace(
  /const balances = \{\n          checking: \{ name: "Checking Account", val: 8245\.3, limit: 10000\.0 \},\n          credit: \{\n            name: "NT Premium Credit Line",\n            val: 12500\.0,\n            limit: 15000\.0,\n          \},\n        \};/,
  `const balances = {
          checking: window.ntBalanceEntry("checking"),
          credit: window.ntBalanceEntry("credit"),
        };`
);

html = html.replace(
  /const balances = \{ checking: 8245\.3, credit: 12500\.0, invest: 17500\.0 \};/g,
  'const balances = { checking: window.ntAccountBal("checking"), credit: window.ntAccountBal("credit"), invest: window.ntAccountBal("invest") };'
);

html = html.replace(
  /checking: \{ name: "Checking Account", val: 8245\.3 \},\n          credit: \{ name: "Apex Credit Line", val: 12500\.0 \}/g,
  'checking: window.ntBalanceEntry("checking"),\n          credit: window.ntBalanceEntry("credit")'
);

html = html.replace(
  /checking: \{ val: 8245\.3, limit: 10000\.0 \},\n          credit: \{ val: 12500\.0, limit: 15000\.0 \}/g,
  'checking: window.ntBalanceEntry("checking"),\n          credit: window.ntBalanceEntry("credit")'
);

html = html.replace(
  /checking: \{ name: "Checking Account", val: 8245\.3 \},\n          savings: \{ name: "Savings Vault", val: 35180\.38 \},\n          credit: \{ name: "NT Premium Credit Line", val: 12500\.0 \},\n          invest: \{ name: "Managed Portfolio", val: 17500\.0 \}/g,
  'checking: window.ntBalanceEntry("checking"),\n          savings: window.ntBalanceEntry("savings"),\n          credit: window.ntBalanceEntry("credit"),\n          invest: window.ntBalanceEntry("invest")'
);

html = html.replace(/checking: 8245\.3,\n          credit: 12500\.0,/g, 'checking: window.ntAccountBal("checking"),\n          credit: window.ntAccountBal("credit"),');

html = html.replace(
  /checking: 8245\.3,\n          savings: 35180\.38,\n          credit: 12500\.0,\n          invest: 17500\.0,/g,
  'checking: window.ntAccountBal("checking"),\n          savings: window.ntAccountBal("savings"),\n          credit: window.ntAccountBal("credit"),\n          invest: window.ntAccountBal("invest"),'
);

html = html.replace(
  /checking: \{ name: "Checking Account", val: 8245\.3 \},\n          credit: \{ name: "Apex Credit Line", val: 12500\.0 \},\n          invest: \{ name: "Managed Portfolio", val: 17500\.0 \}/g,
  'checking: window.ntBalanceEntry("checking"),\n          credit: window.ntBalanceEntry("credit"),\n          invest: window.ntBalanceEntry("invest")'
);

html = html.replace(/cardMonogram: "SKJ"/, 'cardMonogram: "AJS"');
html = html.replace(/data-real-value="\$169,754,130\.38"/, 'data-real-value="—"');
html = html.replace(/data-real-value="\$169,754,107\.88"/, 'data-real-value="—"');
html = html.replace(/<div class="sub-account-value">\$318,750\.00<\/div>/, '<div class="sub-account-value">—</div>');
html = html.replace(/<span>Balance: \$318,772\.50<\/span>/, '<span>Balance: —</span>');
html = html.replace(/<div class="sub-account-value" style="color: #b91c1c;">[\s\S]*?<\/div>\n                <\/div>\n                <div>\n                  <div class="sub-account-info-row">\n                    <span>Owed: \$14,800\.00<\/span>/,
  '<div class="sub-account-value" style="color: #b91c1c;">—</div>\n                </div>\n                <div>\n                  <div class="sub-account-info-row">\n                    <span>Owed: —</span>'
);
html = html.replace(/<span>Available Credit: \$200\.00<\/span>/, '<span>Available Credit: —</span>');
html = html.replace(/data-real-value="\$169,400,000\.00"/, 'data-real-value="—"');

html = html.replace(/<option value="checking">Checking Account \(Avail: \$318,750\.00\)<\/option>/g, '<option value="checking">Checking Account</option>');
html = html.replace(/<option value="savings">Savings Vault \(Avail: \$35,180\.38\)<\/option>/g, '<option value="savings">Savings Vault</option>');
html = html.replace(/<option value="credit">NT Premium Credit Line \(Avail: \$200\.00\)<\/option>/g, '<option value="credit">NT Premium Credit Line</option>');
html = html.replace(/<option value="invest">Managed Portfolio \(Avail: \$169,400,000\.00\)<\/option>/g, '<option value="invest">Managed Portfolio</option>');
html = html.replace(/<option value="credit">Apex Credit Line \(Avail: \$200\.00\)<\/option>/g, '<option value="credit">NT Premium Credit Line</option>');
html = html.replace(/ \(Avail: \$318,750\.00\)/g, '');
html = html.replace(/ \(Balance: \$318,750\.00\)/g, '');
html = html.replace(/ \(Balance: \$35,180\.38\)/g, '');
html = html.replace(/ \(Available Credit: \$200\.00\)/g, '');
html = html.replace(/ \(Available Cash: \$169,400,000\.00\)/g, '');

html = html.replace(/outstandingBalance: 2500/, 'outstandingBalance: null');

fs.writeFileSync(file, html);
console.log('Patched', file);
