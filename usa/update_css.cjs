const fs = require('fs');

const css = `
      :root {
        --bg-body: #f8fafc;
        --bg-card: rgba(255, 255, 255, 0.85);
        --bg-header: rgba(255, 255, 255, 0.7);
        --bg-sidebar: rgba(255, 255, 255, 0.6);
        --text-main: #0f172a;
        --text-muted: #64748b;
        --primary: #2563eb;
        --primary-hover: #1d4ed8;
        --primary-glow: rgba(37, 99, 235, 0.15);
        --shadow-sm: 0 4px 12px rgba(15, 23, 42, 0.03);
        --shadow-md: 0 8px 24px rgba(15, 23, 42, 0.05);
        --shadow-lg: 0 20px 40px rgba(15, 23, 42, 0.08);
        --shadow-float: 0 24px 48px rgba(37, 99, 235, 0.12);
        --radius-sm: 8px;
        --radius-md: 16px;
        --radius-lg: 20px;
        --radius-xl: 24px;
        --glass-blur: blur(24px);
        --glass-border: 1px solid rgba(255, 255, 255, 0.6);
      }

      * {
        box-sizing: border-box;
      }

      html, body {
        height: 100%;
        overflow: hidden;
      }

      body {
        margin: 0;
        padding: 0;
        font-family: 'Outfit', 'Inter', Arial, Helvetica, sans-serif;
        background: radial-gradient(circle at top left, #e0e7ff, #f8fafc 40%),
                    radial-gradient(circle at bottom right, #dbeafe, #f8fafc 40%);
        background-color: var(--bg-body);
        color: var(--text-main);
        display: flex;
        flex-direction: column;
      }

      /* Custom Webkit Scrollbar */
      ::-webkit-scrollbar {
        width: 8px;
        height: 8px;
      }
      ::-webkit-scrollbar-track {
        background: transparent;
      }
      ::-webkit-scrollbar-thumb {
        background: #cbd5e1;
        border-radius: 10px;
      }
      ::-webkit-scrollbar-thumb:hover {
        background: #94a3b8;
      }

      header {
        margin: 0;
        padding: 0;
        z-index: 50;
      }

      input {
        border: 1px solid #cbd5e1;
        outline: none;
        padding: 10px 14px;
        border-radius: var(--radius-sm);
        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
        font-family: inherit;
        background: rgba(255,255,255,0.8);
      }

      input:focus {
        border-color: var(--primary);
        box-shadow: 0 0 0 4px var(--primary-glow);
        background: white;
      }

      /* Hover micro-animations for sidebar links */
      .sidebar-link {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px 18px;
        cursor: pointer;
        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
        border-radius: var(--radius-sm);
        margin: 4px 12px;
        color: var(--text-muted);
        text-decoration: none;
        position: relative;
        overflow: hidden;
      }

      .sidebar-link::before {
        content: '';
        position: absolute;
        top: 0; left: 0; width: 100%; height: 100%;
        background: linear-gradient(90deg, rgba(37,99,235,0.1), transparent);
        opacity: 0;
        transition: opacity 0.3s;
        z-index: -1;
      }

      .sidebar-link:hover {
        color: var(--primary);
        transform: translateX(4px);
      }
      
      .sidebar-link:hover::before {
        opacity: 1;
      }

      .sidebar-link.active {
        background-color: var(--primary);
        color: white;
        font-weight: 600;
        box-shadow: var(--shadow-sm);
      }
      .sidebar-link.active::before { display: none; }

      /* Dropdown styles inside content */
      .dropdown {
        position: relative;
        display: inline-block;
      }

      .dropdown-btn {
        background-color: white;
        color: var(--text-main);
        border: var(--glass-border);
        padding: 10px 20px;
        font-size: 14px;
        font-weight: 600;
        border-radius: var(--radius-sm);
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 8px;
        box-shadow: var(--shadow-sm);
        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
        font-family: inherit;
      }

      .dropdown-btn:hover {
        background-color: #f8fafc;
        transform: translateY(-1px);
        box-shadow: var(--shadow-md);
      }

      .dropdown-menu {
        display: none;
        position: absolute;
        right: 0;
        top: calc(100% + 8px);
        background-color: rgba(255,255,255,0.95);
        backdrop-filter: blur(10px);
        min-width: 200px;
        box-shadow: var(--shadow-lg);
        border: var(--glass-border);
        border-radius: var(--radius-md);
        padding: 8px;
        list-style: none;
        z-index: 100;
        transform-origin: top right;
        animation: dropFade 0.2s cubic-bezier(0.16, 1, 0.3, 1) forwards;
      }

      @keyframes dropFade {
        from { opacity: 0; transform: scale(0.95) translateY(-10px); }
        to { opacity: 1; transform: scale(1) translateY(0); }
      }

      .dropdown.active .dropdown-menu {
        display: block;
      }

      .dropdown-item {
        padding: 10px 14px;
        font-size: 14px;
        color: var(--text-muted);
        cursor: pointer;
        border-radius: 6px;
        transition: all 0.2s;
        font-weight: 500;
      }

      .dropdown-item:hover {
        background-color: rgba(37,99,235,0.08);
        color: var(--primary);
        padding-left: 18px;
      }

      /* Sidebar dropdown submenu styles */
      .sidebar-submenu {
        display: none;
        list-style: none;
        padding-left: 36px;
        margin: 0;
        background-color: transparent;
      }

      .sidebar-submenu.active {
        display: block;
        animation: slideDown 0.3s ease-out;
      }

      @keyframes slideDown {
        from { opacity: 0; transform: translateY(-10px); }
        to { opacity: 1; transform: translateY(0); }
      }

      .submenu-item {
        padding: 10px 16px;
        font-size: 13.5px;
        color: var(--text-muted);
        cursor: pointer;
        display: block;
        transition: all 0.2s ease;
        text-decoration: none;
        border-radius: var(--radius-sm);
        margin: 2px 12px 2px 0;
      }
      .submenu-item:hover {
        color: var(--primary);
        background: rgba(255,255,255,0.5);
      }

      /* Overview Component Layouts */
      .overview-grid {
        display: grid;
        grid-template-columns: 2fr 1.2fr;
        gap: 32px;
        margin-top: 32px;
      }

      @media (max-width: 1024px) {
        .overview-grid {
          grid-template-columns: 1fr;
        }
      }

      /* Metric Cards */
      .stats-container {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
        gap: 24px;
        margin-top: 20px;
      }

      .stat-card {
        background-color: var(--bg-card);
        backdrop-filter: var(--glass-blur);
        border: var(--glass-border);
        border-radius: var(--radius-md);
        padding: 24px;
        box-shadow: var(--shadow-md);
        transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
        position: relative;
        overflow: hidden;
      }

      .stat-card::before {
        content: "";
        position: absolute;
        top: 0; left: -100%;
        width: 50%; height: 100%;
        background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent);
        transform: skewX(-20deg);
        transition: left 0.7s;
      }

      .stat-card:hover {
        transform: translateY(-6px);
        box-shadow: var(--shadow-lg);
        border-color: rgba(255,255,255,1);
      }
      .stat-card:hover::before {
        left: 200%;
      }

      .stat-label {
        font-size: 13px;
        font-weight: 600;
        color: var(--text-muted);
        text-transform: uppercase;
        letter-spacing: 1px;
      }

      .stat-value {
        font-size: 28px;
        font-weight: 700;
        color: var(--text-main);
        margin: 12px 0;
        letter-spacing: -0.5px;
      }

      .stat-subtext {
        font-size: 13px;
        color: var(--text-muted);
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 500;
      }

      .badge-positive {
        background-color: #dcfce7;
        color: #166534;
        padding: 4px 8px;
        border-radius: 6px;
        font-weight: 700;
      }

      /* Generic card component */
      .overview-card {
        background-color: var(--bg-card);
        backdrop-filter: var(--glass-blur);
        border: var(--glass-border);
        border-radius: var(--radius-lg);
        padding: 32px;
        box-shadow: var(--shadow-md);
        margin-bottom: 32px;
        transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
      }
      .overview-card:hover {
        box-shadow: var(--shadow-lg);
      }

      .card-header-flex {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 24px;
      }

      .card-title-lg {
        font-size: 18px;
        font-weight: 700;
        color: var(--text-main);
        letter-spacing: -0.5px;
      }

      /* Activity Table styles */
      .activity-table {
        width: 100%;
        border-collapse: separate;
        border-spacing: 0 8px;
        text-align: left;
      }

      .activity-table th {
        padding: 12px 16px;
        font-size: 12px;
        font-weight: 600;
        color: var(--text-muted);
        text-transform: uppercase;
        letter-spacing: 0.5px;
        border-bottom: 2px solid rgba(0,0,0,0.05);
      }

      .activity-table td {
        padding: 16px;
        background: rgba(255,255,255,0.5);
        font-size: 14px;
        color: var(--text-main);
        font-weight: 500;
      }
      .activity-table tr td:first-child { border-radius: 12px 0 0 12px; }
      .activity-table tr td:last-child { border-radius: 0 12px 12px 0; }
      
      .activity-table tr:hover td {
        background: rgba(255,255,255,0.9);
        box-shadow: 0 2px 10px rgba(0,0,0,0.02);
      }

      .amount-credit {
        color: #16a34a;
        font-weight: 700;
      }

      .amount-debit {
        color: #ef4444;
        font-weight: 700;
      }

      .type-label {
        font-size: 12px;
        padding: 4px 10px;
        border-radius: 20px;
        font-weight: 700;
        display: inline-block;
      }

      .type-card { background-color: #dbeafe; color: #1d4ed8; }
      .type-ach { background-color: #ede9fe; color: #6d28d9; }
      .type-wire { background-color: #fef3c7; color: #b45309; }

      /* Quick Actions */
      .action-btn-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(110px, 1fr));
        gap: 16px;
      }

      .quick-action-btn {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        background: linear-gradient(145deg, #ffffff, #f1f5f9);
        border: var(--glass-border);
        border-radius: var(--radius-md);
        padding: 20px 12px;
        color: var(--text-muted);
        cursor: pointer;
        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
        text-align: center;
        font-size: 13px;
        font-weight: 600;
        gap: 12px;
        box-shadow: var(--shadow-sm);
      }

      .quick-action-btn:hover {
        background: white;
        color: var(--primary);
        transform: translateY(-4px);
        box-shadow: var(--shadow-md);
      }

      .quick-action-btn svg {
        width: 24px;
        height: 24px;
        stroke: currentColor;
        stroke-width: 2.5;
        fill: none;
        transition: transform 0.3s;
      }
      .quick-action-btn:hover svg {
        transform: scale(1.1);
      }

      /* Digital card element - WOW Factor */
      .bank-card-container {
        background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 50%, #312e81 100%);
        border-radius: var(--radius-lg);
        padding: 32px;
        color: white;
        position: relative;
        overflow: hidden;
        box-shadow: var(--shadow-float);
        transition: transform 0.5s cubic-bezier(0.25, 0.8, 0.25, 1), box-shadow 0.5s;
        transform-style: preserve-3d;
        border: 1px solid rgba(255,255,255,0.1);
      }

      .bank-card-container:hover {
        transform: translateY(-8px) rotateX(2deg) rotateY(2deg);
        box-shadow: 0 30px 60px rgba(15, 23, 42, 0.4);
      }

      /* Metallic Shimmers */
      .bank-card-container::before {
        content: '';
        position: absolute;
        top: -50%; left: -50%;
        width: 200%; height: 200%;
        background: radial-gradient(circle at center, rgba(255,255,255,0.15) 0%, transparent 60%);
        opacity: 0;
        transition: opacity 0.5s;
        pointer-events: none;
      }
      .bank-card-container:hover::before { opacity: 1; }
      
      .bank-card-container::after {
        content: '';
        position: absolute;
        width: 300px;
        height: 300px;
        background: rgba(255,255,255,0.03);
        border-radius: 50%;
        top: -100px;
        right: -100px;
        border: 1px solid rgba(255,255,255,0.05);
      }

      .card-chip {
        width: 48px;
        height: 36px;
        background: linear-gradient(135deg, #fcd34d 0%, #fbbf24 50%, #d97706 100%);
        border-radius: 6px;
        margin-bottom: 32px;
        position: relative;
        box-shadow: inset 0 1px 2px rgba(255,255,255,0.4), 0 2px 4px rgba(0,0,0,0.2);
        overflow: hidden;
      }
      /* Chip Details */
      .card-chip::before {
        content:''; position:absolute; top: 10px; left: 0; right: 0; height: 1px; background: rgba(0,0,0,0.2);
        box-shadow: 0 14px 0 rgba(0,0,0,0.2);
      }
      .card-chip::after {
        content:''; position:absolute; top: 0; bottom: 0; left: 16px; width: 1px; background: rgba(0,0,0,0.2);
        box-shadow: 14px 0 0 rgba(0,0,0,0.2);
      }

      .card-num {
        font-size: 24px;
        font-family: 'Space Mono', monospace, sans-serif;
        letter-spacing: 4px;
        margin-bottom: 24px;
        text-shadow: 0 2px 4px rgba(0,0,0,0.3);
      }

      .card-details-flex {
        display: flex;
        justify-content: space-between;
        font-size: 13px;
        opacity: 0.9;
        text-transform: uppercase;
        letter-spacing: 1px;
        font-weight: 600;
      }

      .toggle-switch-container {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 24px;
        padding-top: 16px;
        border-top: 1px solid rgba(255,255,255,0.1);
      }

      /* Switch styling */
      .switch {
        position: relative;
        display: inline-block;
        width: 48px;
        height: 26px;
      }

      .switch input { opacity: 0; width: 0; height: 0; }

      .slider {
        position: absolute;
        cursor: pointer;
        top: 0; left: 0; right: 0; bottom: 0;
        background-color: rgba(255,255,255,0.2);
        transition: .4s;
        border-radius: 26px;
        border: 1px solid rgba(255,255,255,0.1);
      }

      .slider:before {
        position: absolute;
        content: "";
        height: 18px;
        width: 18px;
        left: 4px;
        bottom: 3px;
        background-color: white;
        transition: .4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
        border-radius: 50%;
        box-shadow: 0 2px 4px rgba(0,0,0,0.2);
      }

      input:checked + .slider {
        background-color: #22c55e;
      }

      input:checked + .slider:before {
        transform: translateX(20px);
      }

      /* Alerts */
      .alert-item {
        display: flex;
        gap: 16px;
        padding: 16px;
        background: rgba(254, 243, 199, 0.8);
        backdrop-filter: blur(8px);
        border-left: 4px solid #f59e0b;
        border-radius: var(--radius-sm);
        font-size: 14px;
        margin-bottom: 16px;
        color: #b45309;
        font-weight: 500;
        box-shadow: var(--shadow-sm);
      }

      .alert-item.danger {
        background: rgba(254, 226, 226, 0.8);
        border-left-color: #ef4444;
        color: #991b1b;
      }

      .alert-item.info {
        background: rgba(219, 234, 254, 0.8);
        border-left-color: #3b82f6;
        color: #1e40af;
      }

      /* Progress Bar */
      .progress-bar-bg {
        width: 100%;
        height: 10px;
        background-color: #e2e8f0;
        border-radius: 10px;
        margin: 12px 0;
        overflow: hidden;
        box-shadow: inset 0 1px 2px rgba(0,0,0,0.05);
      }

      /* Sub-Accounts Grid */
      .sub-accounts-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
        gap: 24px;
        margin-top: 24px;
        margin-bottom: 32px;
      }
      .sub-account-card {
        background: rgba(255,255,255,0.7);
        backdrop-filter: blur(16px);
        border: var(--glass-border);
        border-radius: var(--radius-md);
        padding: 24px;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
        box-shadow: var(--shadow-sm);
        position: relative;
        overflow: hidden;
      }
      .sub-account-card::after {
        content: '';
        position: absolute;
        bottom: 0; left: 0; width: 100%; height: 3px;
        background: linear-gradient(90deg, var(--primary), #60a5fa);
        opacity: 0;
        transition: opacity 0.3s;
      }
      .sub-account-card:hover {
        transform: translateY(-4px);
        box-shadow: var(--shadow-md);
        background: white;
      }
      .sub-account-card:hover::after { opacity: 1; }
      
      .sub-account-card.selected {
        border-color: var(--primary);
        background-color: white;
        box-shadow: var(--shadow-md), 0 0 0 2px var(--primary-glow);
      }
      .sub-account-card.selected::after { opacity: 1; }

      .sub-account-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
      }
      .sub-account-badge {
        font-size: 11px;
        font-weight: 700;
        padding: 4px 10px;
        border-radius: 8px;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }
      .badge-checking { background-color: #dbeafe; color: #1d4ed8; }
      .badge-savings { background-color: #dcfce7; color: #15803d; }
      .badge-mkt { background-color: #ede9fe; color: #6d28d9; }
      .badge-fixed { background-color: #fef3c7; color: #b45309; }
      .badge-credit { background-color: #ffe4e6; color: #be123c; }
      .badge-loan { background-color: #f1f5f9; color: #475569; }
      .badge-invest { background-color: #ccfbf1; color: #0f766e; }

      .sub-account-title {
        font-size: 15px;
        font-weight: 700;
        color: var(--text-main);
      }
      .sub-account-value {
        font-size: 26px;
        font-weight: 700;
        color: var(--text-main);
        margin: 8px 0;
        letter-spacing: -0.5px;
      }
      .sub-account-info-row {
        display: flex;
        justify-content: space-between;
        font-size: 13px;
        color: var(--text-muted);
        margin-top: 8px;
        font-weight: 500;
      }

      /* Statements Viewport Custom Styles */
      .statement-tab-container {
        display: flex;
        gap: 16px;
        border-bottom: 2px solid #e2e8f0;
        margin-bottom: 24px;
        overflow-x: auto;
      }
      .statement-tab-btn {
        background: none;
        border: none;
        padding: 14px 20px;
        font-size: 15px;
        font-weight: 600;
        color: var(--text-muted);
        cursor: pointer;
        border-bottom: 3px solid transparent;
        transition: all 0.3s;
        white-space: nowrap;
        margin-bottom: -2px;
        font-family: inherit;
      }
      .statement-tab-btn:hover {
        color: var(--text-main);
      }
      .statement-tab-btn.active {
        color: var(--primary);
        border-bottom-color: var(--primary);
      }
      .filter-toolbar {
        display: flex;
        gap: 20px;
        flex-wrap: wrap;
        background: rgba(255,255,255,0.6);
        backdrop-filter: blur(10px);
        padding: 20px;
        border-radius: var(--radius-md);
        border: var(--glass-border);
        margin-bottom: 24px;
        align-items: flex-end;
        box-shadow: var(--shadow-sm);
      }
      .filter-group {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .filter-label {
        font-size: 12px;
        font-weight: 700;
        color: var(--text-muted);
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }
      .filter-select, .filter-input {
        background-color: white;
        border: 1px solid #cbd5e1;
        border-radius: 8px;
        padding: 10px 16px;
        font-size: 14px;
        color: var(--text-main);
        outline: none;
        transition: all 0.3s;
        font-family: inherit;
        box-shadow: 0 1px 2px rgba(0,0,0,0.02);
      }
      .filter-select:focus, .filter-input:focus {
        border-color: var(--primary);
        box-shadow: 0 0 0 3px var(--primary-glow);
      }
      .view-mode-bar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 24px;
      }
      .view-modes {
        display: flex;
        gap: 8px;
        background-color: rgba(241, 245, 249, 0.8);
        padding: 6px;
        border-radius: 10px;
        border: 1px solid #e2e8f0;
      }
      .view-mode-btn {
        background: none;
        border: none;
        padding: 8px 16px;
        font-size: 13px;
        font-weight: 600;
        color: var(--text-muted);
        border-radius: 6px;
        cursor: pointer;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        gap: 8px;
      }
      .view-mode-btn.active {
        background-color: white;
        color: var(--text-main);
        box-shadow: var(--shadow-sm);
      }
      
      /* Timeline view style card */
      .timeline-feed {
        border-left: 2px solid #cbd5e1;
        margin-left: 24px;
        padding-left: 32px;
        position: relative;
        display: flex;
        flex-direction: column;
        gap: 32px;
      }
      .timeline-node { position: relative; }
      .timeline-node::before {
        content: '';
        position: absolute;
        left: -43px;
        top: 6px;
        width: 20px;
        height: 20px;
        border-radius: 50%;
        background-color: white;
        border: 4px solid var(--primary);
        box-shadow: 0 0 0 6px var(--primary-glow);
        z-index: 2;
      }
      .timeline-node.debit::before {
        border-color: #ef4444;
        box-shadow: 0 0 0 6px rgba(239, 68, 68, 0.15);
      }
      .timeline-node.interest::before {
        border-color: #10b981;
        box-shadow: 0 0 0 6px rgba(16, 185, 129, 0.15);
      }
      .timeline-item-card {
        background-color: white;
        border: 1px solid #e2e8f0;
        border-radius: var(--radius-md);
        padding: 24px;
        box-shadow: var(--shadow-sm);
        transition: transform 0.3s, box-shadow 0.3s;
      }
      .timeline-item-card:hover {
        transform: translateX(4px);
        box-shadow: var(--shadow-md);
      }
      .timeline-item-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
      }
      
      /* Bank Official PDF Mock view card */
      .pdf-statement-canvas {
        background-color: white;
        border: 1px solid #cbd5e1;
        border-radius: var(--radius-sm);
        box-shadow: var(--shadow-lg);
        padding: 64px;
        max-width: 850px;
        margin: 24px auto;
        font-family: 'Outfit', sans-serif;
        color: var(--text-main);
      }
      .pdf-corporate-header {
        display: flex;
        justify-content: space-between;
        border-bottom: 3px solid #0f172a;
        padding-bottom: 24px;
        margin-bottom: 40px;
      }
      .pdf-summary-grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 16px;
        background-color: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: var(--radius-sm);
        padding: 20px;
        margin-bottom: 40px;
      }
      .pdf-summary-box {
        display: flex;
        flex-direction: column;
        gap: 6px;
      }
      .pdf-summary-label {
        font-size: 11px;
        font-weight: 700;
        color: var(--text-muted);
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }
      .pdf-summary-val {
        font-size: 16px;
        font-weight: 700;
        color: var(--text-main);
      }

      /* Beneficiaries */
      .beneficiary-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
        gap: 24px;
        margin-top: 24px;
      }
      .beneficiary-card {
        background-color: var(--bg-card);
        backdrop-filter: var(--glass-blur);
        border: var(--glass-border);
        border-radius: var(--radius-md);
        padding: 24px;
        box-shadow: var(--shadow-sm);
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
        position: relative;
      }
      .beneficiary-card:hover {
        transform: translateY(-4px);
        box-shadow: var(--shadow-lg);
        background: white;
      }
      .beneficiary-header-row {
        display: flex;
        gap: 16px;
        align-items: flex-start;
      }
      .beneficiary-avatar {
        width: 56px;
        height: 56px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
        font-size: 18px;
        flex-shrink: 0;
        box-shadow: 0 4px 10px rgba(0,0,0,0.05);
      }
      .avatar-bank { background: linear-gradient(135deg, #d1fae5, #ecfdf5); color: #059669; border: 2px solid white; }
      .avatar-internal { background: linear-gradient(135deg, #dbeafe, #e0e7ff); color: #4f46e5; border: 2px solid white; }
      .avatar-credit { background: linear-gradient(135deg, #fee2e2, #fff1f2); color: #e11d48; border: 2px solid white; }
      .avatar-investment { background: linear-gradient(135deg, #fef3c7, #fffbeb); color: #d97706; border: 2px solid white; }
      
      .beneficiary-meta { flex-grow: 1; min-width: 0; }
      .beneficiary-name {
        font-size: 16px;
        font-weight: 700;
        color: var(--text-main);
        margin: 0 0 6px 0;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .beneficiary-subtext {
        font-size: 13px;
        color: var(--text-muted);
        margin: 0 0 8px 0;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        font-weight: 500;
      }
      .beneficiary-details-box {
        background-color: rgba(248, 250, 252, 0.8);
        border-radius: var(--radius-sm);
        padding: 12px 16px;
        margin-top: 16px;
        font-size: 13px;
        color: var(--text-muted);
        line-height: 1.5;
        border: 1px solid #e2e8f0;
      }
      .beneficiary-card-footer {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-top: 20px;
        padding-top: 16px;
        border-top: 1px solid rgba(0,0,0,0.05);
      }
      .beneficiary-limits-badge {
        font-size: 12px;
        font-weight: 600;
        color: var(--text-muted);
      }
      
      /* Security and Risk Level Badges */
      .badge-trust {
        font-size: 11px;
        font-weight: 700;
        text-transform: uppercase;
        padding: 4px 10px;
        border-radius: 9999px;
        letter-spacing: 0.5px;
        display: inline-block;
      }
      .badge-trust-trusted { background-color: #dcfce7; color: #166534; }
      .badge-trust-verified { background-color: #dbeafe; color: #1e40af; }
      .badge-trust-new { background-color: #fef3c7; color: #92400e; }
      .badge-trust-blocked { background-color: #fee2e2; color: #991b1b; }

      /* Custom Actions Dropdown inside Card */
      .beneficiary-options-btn {
        background: none;
        border: none;
        color: #94a3b8;
        cursor: pointer;
        padding: 6px;
        border-radius: 6px;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s ease;
      }
      .beneficiary-options-btn:hover {
        background-color: #f1f5f9;
        color: var(--text-main);
      }
      .actions-dropdown {
        position: absolute;
        top: 60px;
        right: 24px;
        background: rgba(255,255,255,0.95);
        backdrop-filter: blur(10px);
        border: var(--glass-border);
        border-radius: var(--radius-sm);
        box-shadow: var(--shadow-lg);
        z-index: 50;
        min-width: 160px;
        display: none;
        flex-direction: column;
        padding: 6px 0;
        animation: dropFade 0.2s cubic-bezier(0.16, 1, 0.3, 1) forwards;
      }
      .actions-dropdown.active { display: flex; }
      .actions-dropdown-item {
        padding: 10px 16px;
        font-size: 13.5px;
        color: var(--text-main);
        text-align: left;
        background: none;
        border: none;
        width: 100%;
        cursor: pointer;
        transition: background-color 0.2s;
        font-weight: 500;
        font-family: inherit;
      }
      .actions-dropdown-item:hover {
        background-color: rgba(37,99,235,0.08);
        color: var(--primary);
      }
      .actions-dropdown-item.danger-text { color: #ef4444; }
      .actions-dropdown-item.danger-text:hover {
        background-color: #fef2f2;
        color: #dc2626;
      }

      /* Modal Window Overlays */
      .modal-overlay {
        position: fixed;
        top: 0; left: 0; width: 100%; height: 100%;
        background-color: rgba(15, 23, 42, 0.4);
        backdrop-filter: blur(8px);
        display: none;
        align-items: center;
        justify-content: center;
        z-index: 1000;
        animation: fadeIn 0.3s ease-out;
      }
      .modal-overlay.active { display: flex; }
      .modal-sheet {
        background: rgba(255,255,255,0.95);
        backdrop-filter: blur(16px);
        border-radius: var(--radius-xl);
        width: 100%;
        max-width: 520px;
        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.4);
        padding: 32px;
        position: relative;
        animation: slideUp 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
        border: var(--glass-border);
      }
      .modal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 24px;
      }
      .modal-title {
        font-size: 22px;
        font-weight: 700;
        color: var(--text-main);
        margin: 0;
        letter-spacing: -0.5px;
      }
      .modal-close-btn {
        background: #f1f5f9;
        border: none;
        color: var(--text-muted);
        cursor: pointer;
        padding: 8px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.2s;
      }
      .modal-close-btn:hover {
        background-color: #e2e8f0;
        color: var(--text-main);
        transform: rotate(90deg);
      }
      .modal-form-group { margin-bottom: 20px; }
      .modal-label {
        display: block;
        font-size: 13px;
        font-weight: 600;
        color: var(--text-muted);
        margin-bottom: 8px;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }
      .modal-input {
        width: 100%;
        padding: 12px 16px;
        font-size: 15px;
        border: 1px solid #cbd5e1;
        border-radius: 10px;
        outline: none;
        transition: all 0.3s;
        color: var(--text-main);
        background: white;
        font-family: inherit;
      }
      .modal-input:focus {
        border-color: var(--primary);
        box-shadow: 0 0 0 4px var(--primary-glow);
      }
      
      /* Multi-Digit OTP Input Board */
      .otp-digits-container {
        display: flex;
        justify-content: space-between;
        gap: 12px;
        margin: 24px 0;
      }
      .otp-digit-input {
        width: 60px;
        height: 60px;
        border: 2px solid #cbd5e1;
        border-radius: 12px;
        text-align: center;
        font-size: 24px;
        font-weight: 700;
        color: var(--text-main);
        outline: none;
        transition: all 0.2s;
        background: white;
      }
      .otp-digit-input:focus {
        border-color: var(--primary);
        box-shadow: 0 0 0 4px var(--primary-glow);
        transform: translateY(-2px);
      }
      .otp-digit-input.success {
        border-color: #10b981;
        background-color: #f0fdf4;
        color: #059669;
      }
      
      /* Keyframe animations */
      @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
      @keyframes slideUp { from { transform: translateY(24px) scale(0.98); opacity: 0; } to { transform: translateY(0) scale(1); opacity: 1; } }
      
      .otp-banner-info {
        background-color: rgba(239, 246, 255, 0.8);
        border-left: 4px solid var(--primary);
        padding: 16px;
        border-radius: var(--radius-sm);
        font-size: 13px;
        color: #1e3a8a;
        line-height: 1.5;
        margin-bottom: 24px;
        font-weight: 500;
      }

      /* Specific elements updates */
      header > div {
        background: var(--bg-header) !important;
        backdrop-filter: var(--glass-blur) !important;
        border-bottom: var(--glass-border) !important;
        color: var(--text-main) !important;
      }
      header svg path { fill: var(--primary) !important; }
      header svg circle { fill: var(--primary) !important; stroke: var(--primary) !important; }
      header svg rect { stroke: var(--primary) !important; }
      header svg polyline { stroke: white !important; }
      header .user-name { color: var(--text-main) !important; }
      header .brand-name { color: var(--text-main) !important; font-weight: 700; }
      
      /* Make sidebar container glass */
      main > div:first-child {
        background: var(--bg-sidebar) !important;
        backdrop-filter: var(--glass-blur) !important;
        border-right: var(--glass-border) !important;
        box-shadow: var(--shadow-md) !important;
      }
      
      /* Make main container glass */
      main > div:last-child > div {
        background: var(--bg-card) !important;
        backdrop-filter: var(--glass-blur) !important;
        border: var(--glass-border) !important;
        box-shadow: var(--shadow-lg) !important;
      }
`;

const htmlPath = 'C:/Users/elon_/eclipse-workspace/project.northerntrust/usa/index.html';
let html = fs.readFileSync(htmlPath, 'utf8');

// Also inject the Google Font in <head>
html = html.replace(/<link href="https:\/\/fonts\.googleapis\.com\/css2\?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">/, 
  '<link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">');

// We need to change the header text color so it's readable on the light glass header
html = html.replace(/color: rgb\(218, 218, 218\);/g, 'color: var(--text-muted);');
html = html.replace(/color: rgb\(220, 220, 220\);/g, 'color: var(--text-main); font-weight: 700;');
html = html.replace(/fill="white"/g, 'fill="currentColor"');

// Replace the style tag completely
html = html.replace(/<style>[\s\S]*?<\/style>/, '<style>\\n' + css + '\\n    </style>');

fs.writeFileSync(htmlPath, html);
console.log('CSS updated successfully!');
