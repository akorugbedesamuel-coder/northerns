# Northern Trust Family Office Technology — User Guide

Welcome to Northern Trust Family Office Technology. This guide walks you through every feature of your banking portal — from signing in to managing transfers, beneficiaries, statements, and your profile.

---

## Table of Contents

1. [Signing In](#1-signing-in)
2. [Your Dashboard (Overview)](#2-your-dashboard-overview)
3. [Understanding Your Accounts](#3-understanding-your-accounts)
4. [Making Transfers](#4-making-transfers)
   - 4.1 Internal Transfers
   - 4.2 ACH Transfers
   - 4.3 Wire Transfers (Domestic)
   - 4.4 International Transfers (SWIFT)
5. [Managing Beneficiaries](#5-managing-beneficiaries)
6. [Statements & Documents](#6-statements--documents)
7. [Analytics & Portfolio](#7-analytics--portfolio)
8. [Notifications & Alerts](#8-notifications--alerts)
9. [Your Profile & Settings](#9-your-profile--settings)
10. [Admin Console (Treasury Administration)](#10-admin-console-treasury-administration)
11. [Security & Verification](#11-security--verification)
12. [Frequently Asked Questions](#12-frequently-asked-questions)

---

## 1. Signing In

### How to access your account

1. Open your web browser and go to the Northern Trust login page.
2. Enter your **User ID** (this is your account number, e.g. `2214578903`).
3. Enter your **Password**.
4. Click **Continue**.

### Two-Step Verification (OTP)

After entering your password, you will be asked for a **6-digit verification code**. This is an extra security layer to protect your account.

- The code is sent to your **registered authenticator app** or **registered device**.
- Open your authenticator app, find the Northern Trust entry, and type the 6-digit code.
- Click **Verify & Sign In**.

> **Tip:** If you did not receive the code, click **Resend code** on the verification screen.

### Remember Me

Check the **Remember Me** box on the login screen to save your User ID for next time. Your password and verification code are never saved — you will always need to enter those.

---

## 2. Your Dashboard (Overview)

After signing in, you land on the **Overview Portal**. This is your home screen.

### What you see

- **Greeting bar** — Shows your name, last login time, and device information.
- **Account cards** — A horizontal carousel showing all four of your accounts with current balances.
- **Recent Activity** — A table of your most recent transactions across all accounts.
- **Wiring Instructions** — Your checking account details for receiving incoming wire transfers.
- **Quick Actions** — Shortcuts to common tasks:
  - **Send Money** — Opens the transfer form.
  - **Add Beneficiary** — Opens the form to add a new payee.
  - **Download Statement** — Export your latest statement as a PDF.
  - **Manage Cards** — View or manage your payment cards.

### Hiding and showing balances

Each account card has an **eye icon**. Click it to mask or reveal the balance amount. This is useful if you are sharing your screen.

---

## 3. Understanding Your Accounts

Your Northern Trust account includes four sub-accounts:

| Account | Type | What it's for |
|---|---|---|
| **Total Checking** (CHK-xxxx) | Checking | Day-to-day spending, bill payments, transfers |
| **Savings Vault** (SAV-xxxx) | Savings | Earns interest (APY shown on card). 6 withdrawals per month allowed by regulation |
| **NT Premium Credit Line** (CRD-xxxx) | Credit | Revolving credit facility. Shows owed amount and available limit |
| **Managed Portfolio** (INV-xxxx) | Investment | Managed investment holdings. Shows daily performance (today's change and ROI) |

### Current vs. Available Balance

- **Current Balance** — The total balance including pending transactions.
- **Available Balance** — What you can actually spend right now (current balance minus holds and pending debits).

---

## 4. Making Transfers

Navigate to **Cash Movement and Trading** in the left sidebar, then choose the type of transfer.

### 4.1 Internal Transfers

Move money between your own Northern Trust accounts.

1. Click **Internal Transfer** in the sidebar.
2. **From Account** — Select the account to send money from (e.g. Checking).
3. **To Account** — Select the account to receive money (e.g. Savings).
4. **Amount** — Enter the dollar amount.
5. **Transfer Type** — Choose Instant, Scheduled, Recurring Weekly, or Recurring Monthly.
6. **Memo** — Optional note for your records.
7. Click **Review & Continue** to see a summary, then **Confirm** to submit.

Internal transfers settle **instantly** with no fee.

---

### 4.2 ACH Transfers

Send or receive money via the ACH (Automated Clearing House) network — the standard US electronic payment system.

1. Click **ACH Transfer** in the sidebar.
2. **Direction** — Choose:
   - **ACH Credit (Push)** — You send money to the external account.
   - **ACH Debit (Pull)** — You pull money from an external account (requires authorization on file).
3. **Funding Source** — Select which Northern Trust account to use.
4. **External Beneficiary** — Select a saved beneficiary from the dropdown, **or** choose **"+ Direct Transfer (enter details manually)"** to type in the recipient's details yourself.
5. **Recipient Details** (shown when a beneficiary is selected or Direct Transfer is chosen):
   - Recipient Name
   - Account Number
   - Routing Number (ABA — 9 digits for US banks)
   - Bank Name
   - Bank Address
6. **Amount** — Enter the dollar amount.
7. **Effective Date** — Choose when the transfer should be initiated.
8. **Delivery Speed**:
   - **Standard ACH** — Free, settles in 1–3 business days.
   - **Same-Day ACH** — $15 fee, settles the same business day (must submit before cutoff time).
9. **SEC Code** — Choose PPD (personal), CCD (business), WEB (online), or TEL (telephone).
10. **Schedule** — One-time, Weekly, Biweekly, Monthly, or Quarterly.
11. **Memo** — Optional description.
12. Click **Review & Continue**, check the summary, then **Confirm**.

**Fees:**
- Standard ACH: **Free**
- Same-Day ACH: **$15.00**

> **Note:** ACH debits above $25,000 may be held for treasury approval. ACH debits to savings accounts are limited to 6 per month by Regulation D.

---

### 4.3 Wire Transfers (Domestic — Fedwire)

Send money quickly via the Federal Reserve wire network. Domestic wires settle **same day**.

1. Click **Wire Transfer** in the sidebar.
2. **Wire Network** — Select **Domestic (Fedwire)**.
3. **Funding Source** — Select your Northern Trust account.
4. **External Beneficiary** — Select a saved beneficiary, or choose **"+ Direct Transfer"** to enter details manually.
5. **Recipient Details**:
   - Recipient Name (full legal name)
   - Account Number
   - Routing Number (ABA — 9 digits)
   - Bank Name
   - Bank Address
6. **Amount** — Enter the dollar amount in USD.
7. **Priority**:
   - **Standard** — Settles same business day.
   - **Same-Day** — Settles same business day (no extra fee).
   - **Urgent** — Priority processing, **+$15 surcharge**.
8. **Transfer Purpose** — Describe why you are sending this wire.
9. **AML/OFAC Compliance** — Check the box to acknowledge anti-money laundering compliance.
10. Click **Review & Continue**, check the summary, then **Confirm**.

**Fees:**
- Standard/Same-Day Domestic Wire: **$25.00**
- Urgent Domestic Wire: **$40.00** ($25 + $15 urgency surcharge)

> **Compliance:** Wires of $100,000 or more are automatically placed on an **AML compliance hold** for review by the treasury team. You will be notified of the hold status.

---

### 4.4 International Transfers (SWIFT)

Send money to a recipient in another country using the SWIFT network.

1. Click **International Transfer** in the sidebar.
2. **Transfer Protocol** — Select SWIFT (default for most international wires).
3. **Funding Source** — Select your Northern Trust account.
4. **Priority & Settlement**:
   - **Standard (T+2)** — Settles in 2 business days.
   - **Priority Global (T+1)** — Settles next business day.
   - **Urgent Treasury (Same-Day)** — Settles same day (+$15 surcharge).
5. **Global Beneficiary** — Select a saved beneficiary, or choose **"+ Direct Transfer"** to enter details manually.
6. **Recipient Details**:
   - Recipient Full Name
   - Receiving Bank (bank name)
   - Bank Address
   - IBAN (International Bank Account Number)
   - SWIFT / BIC Code (8 or 11 characters)
   - Intermediary Bank Routing (optional)
7. **Recipient Currency** — Choose the currency the recipient should receive: EUR, GBP, CHF, JPY, CAD, AUD, SGD, or HKD.
8. **Transfer Amount** — Enter the amount in the **recipient's currency**. The USD equivalent is calculated automatically using the displayed FX rate.
9. **Transfer Purpose** — Select from: Family Support, Property Purchase, Investment, Invoice, or Tax Payment.
10. **Compliance Checkboxes** — Check both:
    - OFAC/FATF sanctions screening acknowledgment
    - Intermediary bank delay acknowledgment
11. Click **Review & Continue**, check the summary, then **Confirm**.

**Fees:**
- International Wire Fee: **$35.00** + **0.5% of the USD equivalent**
- Urgent Treasury surcharge: **+$15.00**

**FX Rate:** The exchange rate is displayed with a live countdown timer. The rate includes a 0.25% institutional spread. The rate refreshes every 30 seconds.

---

## 5. Managing Beneficiaries

Beneficiaries are the people or organizations you regularly send money to. Saving a beneficiary speeds up future transfers.

### Viewing beneficiaries

1. Click **Beneficiaries** in the sidebar under **Accounts**.
2. You will see a grid of all saved beneficiaries, each showing:
   - Name and relationship description
   - Bank name and account (last 4 digits)
   - Trust level (Trusted, Verified, New, or Blocked)
   - Transfer limits (single and daily)
   - Address and contact information

### Adding a new beneficiary

1. Click the **+ Add Beneficiary** button.
2. Fill in the form:
   - **Display Name** — The name you want to see for this payee.
   - **Relationship** — How you describe the relationship (e.g. "Legal Counsel", "Property Manager").
   - **Beneficiary Type** — BANK (external), INTERNAL (your own accounts), CREDIT (loan repayment), or INVESTMENT.
   - **Bank Name** — The name of the recipient's bank.
   - **Account Number** — The recipient's bank account number.
   - **Routing / SWIFT Code** — ABA routing number for US banks, or SWIFT/BIC code for international.
   - **Country** — The country of the recipient's bank.
   - **Physical Address** — The recipient's address.
3. Click **Save Beneficiary**.

### Editing or blocking a beneficiary

- Click the **three-dot menu** on any beneficiary card.
- Choose **Edit** to update details, or **Block** to prevent transfers to this beneficiary.
- Blocked beneficiaries cannot be selected when making a transfer.

---

## 6. Statements & Documents

### Viewing statements

1. Click **Statements** in the sidebar under **Forms & Statements**.
2. Choose an account from the tabs: Checking, Savings, Credit, or Investment.
3. Statements are displayed with date, description, amount, and running balance.
4. Use the **search bar** and **date filters** to find specific transactions.
5. Switch between **table view** and **timeline view**.

### Downloading statements

1. On the Statements page, click **Download PDF**.
2. Select the date range and account.
3. A PDF statement will be generated with the Northern Trust letterhead, account details, and all transactions.

---

## 7. Analytics & Portfolio

### Viewing analytics

1. Click **Portfolio Dashboard** under **Reporting and Analytics** in the sidebar.
2. You will see:
   - Monthly income vs. expenses chart
   - Transfer volume by type (ACH, Wire, Internal)
   - Account balance trends over time
   - Fee summary

### KPI Intelligence

- **Total Volume** — Sum of all transfers in the selected period.
- **Net Flow** — Money in minus money out.
- **Active Beneficiaries** — Number of unique recipients.
- **Avg. Transaction Size** — Average transfer amount.

---

## 8. Notifications & Alerts

Click the **bell icon** in the top header to view notifications.

Notifications include:
- Transfer confirmations
- Compliance holds requiring attention
- Statement availability
- Security alerts (new login, password changes)
- Beneficiary updates

Each notification shows the date, type, and a brief description. Click a notification to navigate to the relevant section.

---

## 9. Your Profile & Settings

Click your **name** in the top-right corner to open the user menu.

### Profile information

- **Full Name** — Your registered name.
- **Client ID** — Your Northern Trust client identifier.
- **Email** — Registered email address.
- **Phone** — Registered phone number.
- **Date of Birth** — For identity verification.
- **Address** — Your registered mailing address.

### Changing your password

1. Open the user menu and select **Change Password**.
2. Enter your current password.
3. Enter and confirm your new password.
4. Click **Update Password**.

### Signing out

Click **Sign Out** in the user menu. For security, always sign out when you are done, especially on shared or public devices.

---

## 10. Admin Console (Treasury Administration)

The Admin Console is for treasury administrators who approve and monitor transfers across all client accounts.

### Accessing the Admin Console

1. Navigate to the Admin Console page (separate from the client portal).
2. Enter your **Admin User ID** (e.g. `100000`).
3. Enter your **Admin Password**.
4. Click **Sign In**.

### Dashboard overview

The admin dashboard shows six key metrics:
- **Pending Approvals** — Transfers waiting for treasury review.
- **Total Transfers** — All transfers in the system.
- **Total Volume** — Dollar volume of all transfers.
- **Settled** — Successfully completed transfers.
- **On Hold** — Transfers flagged for compliance review.
- **Failed / Returned** — Transfers that failed or were returned.

### Approving or rejecting transfers

1. Click the **Approvals** tab.
2. Each pending transfer shows: reference, date, user, type, counterparty, amount, risk level, and status.
3. Click **Approve** to release the transfer, or **Reject** to decline it.
4. You may add a note explaining your decision.

### Viewing all transfers

Click the **All Transfers** tab to see every transfer in the system. Use the search bar to filter by reference number, user, or counterparty.

### Managing accounts

Click the **Accounts** tab to view all client accounts with balances, available funds, and product types.

---

## 11. Security & Verification

### How your account is protected

- **Two-Step Verification** — Every sign-in requires your password plus a 6-digit code from your authenticator app.
- **Session Timeout** — Inactive sessions are automatically closed after a period of inactivity.
- **Transfer OTP** — Large or high-risk transfers (over $10,000 to new beneficiaries, or over $100,000) require an additional verification code.
- **AML Compliance** — All transfers are screened against anti-money laundering and OFAC sanctions lists.
- **Compliance Holds** — Wires of $100,000 or more are placed on hold for treasury team review before funds are released.
- **Beneficiary Trust Levels** — New beneficiaries are flagged and subject to lower transfer limits until verified.
- **Device Recognition** — Your last sign-in device and IP address are recorded and displayed.

### What to do if you suspect unauthorized access

1. Sign out immediately from all devices.
2. Change your password.
3. Contact your relationship manager or the Northern Trust security team.

---

## 12. Frequently Asked Questions

### How long do transfers take?

| Transfer Type | Settlement Time |
|---|---|
| Internal | Instant |
| ACH Standard | 1–3 business days |
| ACH Same-Day | Same business day |
| Domestic Wire | Same business day |
| International SWIFT (Standard) | 2 business days (T+2) |
| International SWIFT (Priority) | 1 business day (T+1) |
| International SWIFT (Urgent) | Same business day |

### What are the fees?

| Transfer Type | Fee |
|---|---|
| Internal | Free |
| ACH Standard | Free |
| ACH Same-Day | $15.00 |
| Domestic Wire | $25.00 |
| Domestic Wire (Urgent) | $40.00 |
| International Wire | $35.00 + 0.5% of USD equivalent |
| International Wire (Urgent) | $50.00 + 0.5% of USD equivalent |

### Can I cancel a pending transfer?

ACH transfers that are still in "Pending NACHA Batch" status may be cancelable. Wire transfers that have already been sent to the Fedwire or SWIFT network **cannot** be cancelled. Contact your relationship manager immediately if you need to recall a wire.

### What is a compliance hold?

When a transfer exceeds certain thresholds (e.g. $100,000 for wires, or patterns that trigger AML screening), the system places the transfer on hold. An administrator must review and approve it before funds are released. This is a regulatory requirement to prevent money laundering and fraud.

### How do I add a new payee?

Go to **Beneficiaries** and click **+ Add Beneficiary**. Fill in their name, bank details, and address. You can also choose **"+ Direct Transfer"** on any transfer form to send money to someone without saving them as a beneficiary.

### What currencies are supported for international transfers?

EUR (Euro), GBP (British Pound), JPY (Japanese Yen), CHF (Swiss Franc), CAD (Canadian Dollar), AUD (Australian Dollar), SGD (Singapore Dollar), and HKD (Hong Kong Dollar).

### Who do I contact for help?

- **Technical Support** — Your relationship manager
- **Security Concerns** — Northern Trust Security Center
- **Transfer Questions** — Treasury Administration team

---

*© 2026 Northern Trust Corporation. Family Office Technology. Member FDIC.*
