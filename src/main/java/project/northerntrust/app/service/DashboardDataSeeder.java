package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import project.northerntrust.app.entity.*;
import project.northerntrust.app.entity.enums.*;
import project.northerntrust.app.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Order(2)
public class DashboardDataSeeder implements CommandLineRunner {

    public static final String DEMO_ACCOUNT_NUMBER = "8902410001";

    private static final BigDecimal CHECKING_BALANCE = new BigDecimal("318750.00");
    private static final BigDecimal SAVINGS_BALANCE = new BigDecimal("35180.38");
    private static final BigDecimal CREDIT_LIMIT = new BigDecimal("15000.00");
    private static final BigDecimal CREDIT_AVAILABLE = new BigDecimal("200.00");
    private static final BigDecimal CREDIT_OWED = CREDIT_LIMIT.subtract(CREDIT_AVAILABLE);
    private static final BigDecimal INVEST_MARKET_VALUE = new BigDecimal("169400000.00");

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private KycRepository kycRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private BeneficiaryRepository beneficiaryRepository;
    @Autowired
    private TransferRepository transferRepository;
    @Autowired
    private StatementLineRepository statementLineRepository;
    @Autowired
    private PaymentCardRepository paymentCardRepository;
    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByAccountNumber(DEMO_ACCOUNT_NUMBER).isPresent()) {
            refreshExistingDemoUser();
            return;
        }

        User user = new User();
        user.setAccountNumber(DEMO_ACCOUNT_NUMBER);
        user.setClientId("USR-89024");
        user.setFirstName("Alexander J");
        user.setLastName("Skarsgard");
        user.setEmail("alexander.skarsgard@northerntrust.com");
        user.setPhoneNumber("+1 (555) 890-2401");
        user.setDateOfBirth(LocalDate.of(1985, 4, 12));
        user.setStreetAddress("1400 Northern Trust Plaza, Suite 4200");
        user.setCity("Chicago");
        user.setState("Illinois");
        user.setPostalCode("60603");
        user.setCountry("United States");
        user.setPassword(passwordEncoder.encode("NorthernTrust1!"));
        user.setTransactionPinHash(passwordEncoder.encode("1234"));
        user.setKycStatus(KycStatus.VERIFIED);
        user.setAccountStatus(UserStatus.ACTIVE);
        user.setLastLoginAt(LocalDateTime.now().minusHours(2));
        user.setLastLoginIp("12.34.56.78");
        user.setLastLoginUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
        user = userRepository.save(user);

        KycProfile kyc = new KycProfile();
        kyc.setUser(user);
        kyc.setSsn("457-89-0124");
        kyc.setTaxId("98-7654321");
        kyc.setBvn("22345678901");
        kyc.setNin("NT-US-89024-KYC");
        kyc.setIdType("US_PASSPORT");
        kyc.setIdNumber("X12345678");
        kyc.setNationality("Swedish-American (Dual)");
        kyc.setOccupation("Family Office Principal / Entertainment");
        kyc.setResidentialAddress("1400 Northern Trust Plaza, Suite 4200, Chicago, IL 60603, USA");
        kyc.setVerificationStatus(VerificationStatus.VERIFIED);
        kycRepository.save(kyc);

        Account checking = createAccount(user, "CHK-89024001", ProductKey.CHECKING, AccountType.CHECKING,
                "Checking Account", "Spending", CHECKING_BALANCE, new BigDecimal("-22.50"), "USD");
        Account savings = createAccount(user, "SAV-89024002", ProductKey.SAVINGS, AccountType.SAVINGS,
                "Savings Vault", "Vault", SAVINGS_BALANCE, BigDecimal.ZERO, "USD");
        savings.setApyPercent(new BigDecimal("4.50"));
        savings.setEarnedThisPeriod(new BigDecimal("158.31"));
        accountRepository.save(savings);
        Account credit = createAccount(user, "CRD-89024003", ProductKey.CREDIT, AccountType.CREDIT,
                "NT Premium Credit Line", null, CREDIT_AVAILABLE, BigDecimal.ZERO, "USD");
        credit.setAmountOwed(CREDIT_OWED);
        credit.setCreditLimit(CREDIT_LIMIT);
        credit.setBalance(CREDIT_AVAILABLE);
        credit.setAvailableBalance(CREDIT_AVAILABLE);
        accountRepository.save(credit);
        Account invest = createAccount(user, "INV-89024004", ProductKey.INVEST, AccountType.INVESTMENT,
                "Managed Portfolio", null, INVEST_MARKET_VALUE, BigDecimal.ZERO, "USD");
        invest.setMarketValue(INVEST_MARKET_VALUE);
        invest.setTodayChange(new BigDecimal("245.00"));
        invest.setTodayChangePct(new BigDecimal("1.42"));
        invest.setRoiPercent(new BigDecimal("12.40"));
        accountRepository.save(invest);

        PaymentCard card = new PaymentCard();
        card.setUser(user);
        card.setLastFour("4920");
        card.setCardHolder("ALEXANDER S");
        card.setExpires("09/31");
        card.setFrozen(false);
        paymentCardRepository.save(card);

        seedBeneficiaries(user);
        seedAllStatementLines(user);
        seedAllTransfers(user, checking, savings, credit, invest);
    }

    private Account createAccount(User user, String number, ProductKey key, AccountType type,
                                  String displayName, String badge, BigDecimal balance,
                                  BigDecimal pending, String currency) {
        Account a = new Account();
        a.setUser(user);
        a.setAccountNumber(number);
        a.setProductKey(key);
        a.setAccountType(type);
        a.setDisplayName(displayName);
        a.setBadge(badge);
        a.setBalance(balance);
        a.setPendingAmount(pending);
        a.setAvailableBalance(balance);
        a.setCurrency(currency);
        a.setStatus(AccountStatus.ACTIVE);
        a.setDailyTransferLimit(new BigDecimal("1000000.00"));
        a.setMonthlyTransferLimit(new BigDecimal("10000000.00"));
        Account saved = accountRepository.save(a);

        if (ledgerEntryRepository.findByAccountOrderByCreatedAtDesc(saved).isEmpty()) {
            LedgerEntry seed = new LedgerEntry();
            seed.setAccount(saved);
            seed.setEntryType(EntryType.CREDIT);
            seed.setAmount(balance);
            seed.setBalanceAfter(balance);
            ledgerEntryRepository.save(seed);
        }
        return saved;
    }

    private void seedBeneficiaries(User user) {
        saveBen(user, "BEN-001", BeneficiaryType.BANK, "Skarsgard Family Trust", "Family Trust",
                "JPMorgan Chase Bank", "•••• •••• •••• 9012", "021000021", "United States",
                new BigDecimal("2500"), new BigDecimal("5000"), TrustLevel.Trusted, true,
                LocalDateTime.now().minusDays(3), "270 Park Avenue, New York, NY 10017, United States");
        saveBen(user, "BEN-002", BeneficiaryType.INTERNAL, "Alexander S Vault", "Personal Reserve Sweep",
                null, null, null, null, new BigDecimal("10000"), new BigDecimal("20000"),
                TrustLevel.Trusted, true, LocalDateTime.now().minusDays(2), null);
        patchBen("BEN-002", b -> {
            b.setDestinationUserId("USR-89024");
            b.setEmailOrPhone("alexander.skarsgard@northerntrust.com");
        });
        saveBen(user, "BEN-003", BeneficiaryType.CREDIT, "NT Credit Repayment Facility", "Primary Debt Servicer",
                null, null, null, null, new BigDecimal("2500"), new BigDecimal("2500"),
                TrustLevel.Verified, true, LocalDateTime.now().minusDays(7), null);
        patchBen("BEN-003", b -> b.setCreditAccountId("NT-CRD-8902"));
        saveBen(user, "BEN-004", BeneficiaryType.INVESTMENT, "Vanguard Managed Index Fund", "Corporate Treasury Invest",
                null, null, null, null, new BigDecimal("5000"), new BigDecimal("10000"),
                TrustLevel.Verified, true, LocalDateTime.now().minusDays(16), null);
        patchBen("BEN-004", b -> b.setPortfolioId("PORT-TREAS-01"));
        saveBen(user, "BEN-005", BeneficiaryType.BANK, "Zürich Private Asset Clearing", "Offshore Portfolio Target",
                "UBS Switzerland AG", "•••• •••• •••• 4410", "UBSWCH22XXX", "Switzerland",
                new BigDecimal("500"), new BigDecimal("1000"), TrustLevel.New, false, null,
                "Bahnhofstrasse 45, 8001 Zurich, Switzerland");
        Beneficiary b6 = saveBen(user, "BEN-006", BeneficiaryType.INTERNAL, "Compliance Transit Escrow", "Audit Lock Clearing",
                null, null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, TrustLevel.Blocked, false, null, null);
        b6.setStatus(BeneficiaryStatus.BLOCKED);
        b6.setDestinationUserId("USR-COMP-01");
        b6.setEmailOrPhone("audit@northerntrust.com");
        beneficiaryRepository.save(b6);
    }

    private void patchBen(String code, java.util.function.Consumer<Beneficiary> fn) {
        beneficiaryRepository.findByBeneficiaryCode(code).ifPresent(b -> {
            fn.accept(b);
            beneficiaryRepository.save(b);
        });
    }

    private Beneficiary saveBen(User user, String code, BeneficiaryType type, String name, String rel,
                                String bank, String acct, String routing, String country,
                                BigDecimal single, BigDecimal daily, TrustLevel trust, boolean isTrusted,
                                LocalDateTime lastUsed, String bankAddress) {
        Beneficiary b = new Beneficiary();
        b.setUser(user);
        b.setBeneficiaryCode(code);
        b.setBeneficiaryType(type);
        b.setDisplayName(name);
        b.setRelationship(rel);
        b.setBankName(bank);
        b.setAccountNumber(acct);
        b.setAccountName(name);
        b.setRoutingOrSwift(routing);
        b.setCountry(country);
        b.setBankAddress(bankAddress);
        b.setSingleLimit(single);
        b.setDailyLimit(daily);
        b.setTrustLevel(trust);
        b.setTrusted(isTrusted);
        b.setStatus(BeneficiaryStatus.ACTIVE);
        b.setLastUsedAt(lastUsed);
        return beneficiaryRepository.save(b);
    }

    private void seedAllStatementLines(User user) {
        // Unified ledger (all products)
        unified(user, null, "2026-05-19", "Google Cloud Platform", "NT Premium Credit Line", "Fee", "-124.50", CREDIT_AVAILABLE.toPlainString(), "Card");
        unified(user, ProductKey.CHECKING, "2026-05-18", "Enterprise Inbound Revenue", "Checking Account", "Deposit", "1852.00", CHECKING_BALANCE.toPlainString(), "Wire");
        unified(user, ProductKey.INVEST, "2026-05-18", "Vanguard Treasury Fund Div", "Managed Portfolio", "Interest", "142.30", INVEST_MARKET_VALUE.toPlainString(), "Internal");
        unified(user, ProductKey.CHECKING, "2026-05-16", "Stripe Pay-out Settlement", "Checking Account", "Deposit", "624.00", "6393.30", "ACH");
        unified(user, ProductKey.SAVINGS, "2026-05-15", "Monthly Yield Accrual", "Savings Vault", "Interest", "131.92", "35180.38", "Internal");
        unified(user, null, "2026-05-15", "Salesforce Cloud Services", "NT Premium Credit Line", "Fee", "-87.50", "12624.50", "Card");
        unified(user, ProductKey.CHECKING, "2026-05-14", "Corporate Card Payment", "Checking Account", "Withdrawal", "-185.00", "5769.30", "ACH");
        unified(user, null, "2026-05-10", "Figma Enterprise Subscription", "NT Premium Credit Line", "Fee", "-18.00", "12712.00", "Card");
        unified(user, ProductKey.INVEST, "2026-05-05", "SPY S&P 500 Rebalance Buy", "Managed Portfolio", "Investments", "-1200.00", "17357.70", "Internal");
        unified(user, null, "2026-05-05", "GitHub Enterprise Suite", "NT Premium Credit Line", "Fee", "-52.00", "12730.00", "Card");
        unified(user, ProductKey.SAVINGS, "2026-05-01", "Reserve Sweep", "Savings Vault", "Deposit", "500.00", "35048.46", "Internal");
        unified(user, ProductKey.CHECKING, "2026-04-28", "Capital Reserve Sweep", "Checking Account", "Withdrawal", "-500.00", "5954.30", "ACH");
        unified(user, ProductKey.INVEST, "2026-04-20", "BlackRock Global Bond Yield", "Managed Portfolio", "Interest", "118.50", "18557.70", "Internal");
        unified(user, ProductKey.SAVINGS, "2026-04-15", "Monthly Yield Accrual", "Savings Vault", "Interest", "125.00", "34548.46", "Internal");
        unified(user, ProductKey.INVEST, "2026-04-05", "NASDAQ 100 Index Purchase", "Managed Portfolio", "Investments", "-1500.00", "18439.20", "Internal");
        unified(user, ProductKey.SAVINGS, "2026-04-01", "Reserve Sweep", "Savings Vault", "Deposit", "350.00", "34423.46", "Internal");
        unified(user, ProductKey.SAVINGS, "2026-03-15", "Monthly Yield Accrual", "Savings Vault", "Interest", "121.20", "34073.46", "Internal");

        checking(user, "2026-05-18", "Enterprise Inbound Revenue", "1852.00", CHECKING_BALANCE.toPlainString(), "Wire", "Deposit");
        checking(user, "2026-05-16", "Stripe Pay-out Settlement", "624.00", "6393.30", "ACH", "Deposit");
        checking(user, "2026-05-14", "Corporate Card Payment", "-185.00", "5769.30", "ACH", "Withdrawal");
        checking(user, "2026-05-02", "Office Supply Logistics", "-24.00", "5954.30", "ACH", "Withdrawal");
        checking(user, "2026-04-28", "Capital Reserve Sweep", "-500.00", "5978.30", "ACH", "Withdrawal");

        savings(user, "2026-05-15", "Interest", "131.92", "4.50%", "35180.38");
        savings(user, "2026-05-01", "Deposit", "500.00", "4.50%", "35048.46");
        savings(user, "2026-04-15", "Interest", "125.00", "4.50%", "34548.46");
        savings(user, "2026-04-01", "Deposit", "350.00", "4.50%", "34423.46");
        savings(user, "2026-03-15", "Interest", "121.20", "4.50%", "34073.46");

        credit(user, "2026-05-19", "Card Purchase (Google Cloud Platform)", "-124.50", "0.00", CREDIT_OWED.toPlainString(), CREDIT_AVAILABLE.toPlainString());
        credit(user, "2026-05-10", "Card Purchase (Figma Design)", "-18.00", "0.00", CREDIT_OWED.toPlainString(), CREDIT_AVAILABLE.toPlainString());

        invest(user, "2026-05-18", "Vanguard Treasury Fund", "Dividend", "28.46", "17.50", "142.30", INVEST_MARKET_VALUE.toPlainString());
        invest(user, "2026-05-05", "SPY S&P 500 ETF Index", "Buy", "240.00", "5.00", "-1200.00", "17357.70");
        invest(user, "2026-04-20", "BlackRock Global Bond", "Dividend", "11.85", "10.00", "118.50", "18557.70");
        invest(user, "2026-04-05", "NASDAQ 100 Index Trust", "Buy", "37.50", "40.00", "-1500.00", "18439.20");
        invest(user, "2026-03-20", "Vanguard Treasury Fund", "Dividend", "22.80", "5.00", "114.00", "19939.20");
    }

    private void unified(User user, ProductKey key, String date, String desc, String source, String type,
                         String amount, String balance, String channel) {
        addStmt(user, key, LocalDate.parse(date), desc, source, type, new BigDecimal(amount),
                new BigDecimal(balance), "Completed", channel);
    }

    private void checking(User user, String date, String desc, String amount, String balance, String channel, String type) {
        StatementLine line = baseStmt(user, ProductKey.CHECKING, date, desc, amount, balance);
        line.setChannel(channel);
        line.setLineType(type);
        line.setSource("Checking Account");
        statementLineRepository.save(line);
    }

    private void savings(User user, String date, String type, String amount, String rate, String balance) {
        StatementLine line = baseStmt(user, ProductKey.SAVINGS, date, type, amount, balance);
        line.setLineType(type);
        line.setRateLabel(rate);
        line.setSource("Savings Vault");
        statementLineRepository.save(line);
    }

    private void credit(User user, String date, String type, String charged, String payment, String balance, String creditAvail) {
        StatementLine line = baseStmt(user, ProductKey.CREDIT, date, type, "0", balance);
        line.setLineType(type);
        line.setCharged(new BigDecimal(charged));
        line.setPayment(new BigDecimal(payment));
        line.setBalanceAfter(new BigDecimal(balance));
        line.setCreditAvailable(new BigDecimal(creditAvail));
        line.setSource("NT Premium Credit Line");
        statementLineRepository.save(line);
    }

    private void invest(User user, String date, String asset, String type, String units, String price, String value, String balance) {
        StatementLine line = baseStmt(user, ProductKey.INVEST, date, asset, value, balance);
        line.setAsset(asset);
        line.setLineType(type);
        line.setUnits(new BigDecimal(units));
        line.setPrice(new BigDecimal(price));
        line.setAmount(new BigDecimal(value));
        line.setSource("Managed Portfolio");
        statementLineRepository.save(line);
    }

    private StatementLine baseStmt(User user, ProductKey key, String date, String desc, String amount, String balance) {
        StatementLine line = new StatementLine();
        line.setUser(user);
        line.setProductKey(key);
        line.setLineDate(LocalDate.parse(date));
        line.setDescription(desc);
        line.setAmount(new BigDecimal(amount));
        line.setBalanceAfter(new BigDecimal(balance));
        line.setStatus("Completed");
        return line;
    }

    private void addStmt(User user, ProductKey key, LocalDate date, String desc, String source,
                         String type, BigDecimal amount, BigDecimal bal, String status, String channel) {
        StatementLine line = new StatementLine();
        line.setUser(user);
        line.setProductKey(key);
        line.setLineDate(date);
        line.setDescription(desc);
        line.setSource(source);
        line.setLineType(type);
        line.setAmount(amount);
        line.setBalanceAfter(bal);
        line.setStatus(status);
        line.setChannel(channel);
        statementLineRepository.save(line);
    }

    private void seedAllTransfers(User user, Account checking, Account savings, Account credit, Account invest) {
        tx(user, checking, "NT-TXH-0091", TransferType.WIRE, "Global Trust Bank (Fedwire)", "Checking Account",
                "825.00", "USD", DisplayTransferStatus.Settled, false, 10, "Low");
        tx(user, checking, "NT-TXH-0090", TransferType.SWIFT, "Quantum Global Holdings", "Checking Account",
                "15732.50", "EUR", DisplayTransferStatus.OFAC_Hold, true, 92, "Critical", "Cyprus (CY)");
        tx(user, checking, "NT-TXH-0089", TransferType.ACH, "Vanguard Group (External)", "Checking Account",
                "450.00", "USD", DisplayTransferStatus.Processing, false, 42, "Medium");
        tx(user, savings, "NT-TXH-0088", TransferType.INTERNAL, "Savings Vault → Checking", "Savings Vault",
                "2000.00", "USD", DisplayTransferStatus.Settled, false, 5, "Low");
        tx(user, credit, "NT-TXH-0087", TransferType.WIRE, "Acme Corp Real Estate (SWIFT)", "Apex Credit Line",
                "2500.00", "USD", DisplayTransferStatus.Compliance_Hold, true, 75, "High");
        tx(user, checking, "NT-TXH-0086", TransferType.SWIFT, "Mayfair Partners Ltd (GBP)", "Checking Account",
                "634.00", "USD", DisplayTransferStatus.Settled, false, 20, "Low");
        tx(user, checking, "NT-TXH-0085", TransferType.ACH, "BlackRock Fund Services", "Checking Account",
                "1250.00", "USD", DisplayTransferStatus.Settled, false, 15, "Low");
        tx(user, checking, "NT-TXH-0084", TransferType.WIRE, "Erste Bank Vienna (SWIFT/EUR)", "Checking Account",
                "327.60", "USD", DisplayTransferStatus.Settled, false, 12, "Low");
        tx(user, invest, "NT-TXH-0083", TransferType.INTERNAL, "Managed Portfolio → Checking", "Managed Portfolio",
                "5000.00", "USD", DisplayTransferStatus.Settled, false, 8, "Low");
        tx(user, checking, "NT-TXH-0082", TransferType.ACH, "Fidelity Investments", "Checking Account",
                "750.00", "USD", DisplayTransferStatus.Returned, false, 35, "Medium");
        tx(user, checking, "NT-TXH-0081", TransferType.WIRE, "Citi Private Bank (Fedwire)", "Checking Account",
                "11000.00", "USD", DisplayTransferStatus.Settled, false, 25, "Low");
        tx(user, checking, "NT-TXH-0080", TransferType.SWIFT, "Nomura Securities (JPY)", "Checking Account",
                "189.00", "USD", DisplayTransferStatus.Failed, false, 55, "Medium");

        tx(user, checking, "TRX-9984-CR", TransferType.SWIFT, "Quantum Global Holdings", "Checking Account",
                "14500.00", "EUR", DisplayTransferStatus.Blocked_OFAC_Review, true, 92, "Critical", "Cyprus (CY)");
        tx(user, credit, "TRX-4412-HR", TransferType.WIRE, "Acme Corp Real Estate", "Apex Credit Line",
                "2500.00", "USD", DisplayTransferStatus.Awaiting_Treasury_Approval, true, 75, "High");
        tx(user, checking, "TRX-1192-MR", TransferType.ACH, "Vanguard Group (External)", "Checking Account",
                "450.00", "USD", DisplayTransferStatus.Awaiting_Verification, true, 42, "Medium");

        enrichPending("TRX-9984-CR", "15732.50",
                "[\"Large amount threshold exceeded\",\"High-risk jurisdiction (Cyprus)\",\"New Beneficiary\",\"Behavioral velocity anomaly\"]",
                "[{\"time\":\"09:14 AM\",\"actor\":\"E. Musk (Initiator)\",\"action\":\"Created Draft\"},{\"time\":\"09:15 AM\",\"actor\":\"System MFA\",\"action\":\"OTP Verified\"}]");
        enrichPending("NT-TXH-0090", "15732.50",
                "[\"Large amount threshold exceeded\",\"High-risk jurisdiction (Cyprus)\",\"New Beneficiary\"]",
                "[{\"time\":\"09:15 AM\",\"actor\":\"Compliance Engine\",\"action\":\"Halted: OFAC Review Required\"}]");
    }

    private void tx(User user, Account from, String ref, TransferType type, String counterparty, String source,
                    String amount, String currency, DisplayTransferStatus status, boolean pending,
                    int risk, String riskLevel) {
        tx(user, from, ref, type, counterparty, source, amount, currency, status, pending, risk, riskLevel, null);
    }

    private void tx(User user, Account from, String ref, TransferType type, String counterparty, String source,
                    String amount, String currency, DisplayTransferStatus status, boolean pending,
                    int risk, String riskLevel, String country) {
        Transfer t = new Transfer();
        t.setReference(ref);
        t.setUser(user);
        t.setFromAccount(from);
        t.setTransferType(type);
        t.setDirection(type == TransferType.SWIFT ? TransferDirection.INTERNATIONAL : TransferDirection.DOMESTIC);
        t.setAmount(new BigDecimal(amount));
        t.setCurrency(currency);
        t.setDescription(counterparty);
        t.setStatus(pending ? TransferStatus.PENDING_PROCESSING : TransferStatus.SUCCESS);
        t.setDisplayStatus(status);
        t.setCounterpartyName(counterparty);
        t.setSourceAccountLabel(source);
        t.setUsdEquivalent(new BigDecimal(amount));
        t.setPendingApproval(pending);
        t.setRiskScore(risk);
        t.setRiskLevel(riskLevel);
        t.setApprovalCountry(country);
        if (pending) {
            t.setComplianceJson("{\"aml\":\"Passed\",\"ofac\":\"Passed\",\"sanctions\":\"Passed\",\"pep\":\"Passed\"}");
            if (status == DisplayTransferStatus.Blocked_OFAC_Review || status == DisplayTransferStatus.OFAC_Hold) {
                t.setComplianceJson("{\"aml\":\"Under Review\",\"ofac\":\"Flagged\",\"sanctions\":\"Passed\",\"pep\":\"Passed\"}");
            }
        }
        transferRepository.save(t);
    }

    private void enrichPending(String ref, String usdEq, String flags, String history) {
        transferRepository.findByReference(ref).ifPresent(t -> {
            t.setUsdEquivalent(new BigDecimal(usdEq));
            t.setComplianceFlagsJson(flags);
            t.setApprovalHistoryJson(history);
            transferRepository.save(t);
        });
    }

    private void refreshExistingDemoUser() {
        User user = userRepository.findByAccountNumber(DEMO_ACCOUNT_NUMBER).orElse(null);
        if (user == null) {
            return;
        }

        boolean hasActivity = transferRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .anyMatch(t -> t.getCreatedAt() != null
                        && t.getCreatedAt().isAfter(user.getCreatedAt().plusMinutes(2)));
        if (hasActivity) {
            // Preserve real user activity: do not reset balances on restart.
            return;
        }

        user.setFirstName("Alexander J");
        user.setLastName("Skarsgard");
        user.setEmail("alexander.skarsgard@northerntrust.com");
        if (user.getLastLoginAt() == null) {
            user.setLastLoginAt(LocalDateTime.now().minusHours(2));
        }
        if (user.getLastLoginIp() == null) {
            user.setLastLoginIp("12.34.56.78");
        }
        if (user.getLastLoginUserAgent() == null) {
            user.setLastLoginUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
        }
        userRepository.save(user);

        for (Account account : accountRepository.findByUser(user)) {
            switch (account.getProductKey()) {
                case CHECKING:
                    account.setBalance(CHECKING_BALANCE);
                    account.setAvailableBalance(CHECKING_BALANCE);
                    account.setPendingAmount(new BigDecimal("-22.50"));
                    break;
                case SAVINGS:
                    account.setBalance(SAVINGS_BALANCE);
                    account.setAvailableBalance(SAVINGS_BALANCE);
                    break;
                case CREDIT:
                    account.setCreditLimit(CREDIT_LIMIT);
                    account.setAmountOwed(CREDIT_OWED);
                    account.setBalance(CREDIT_AVAILABLE);
                    account.setAvailableBalance(CREDIT_AVAILABLE);
                    break;
                case INVEST:
                    account.setBalance(INVEST_MARKET_VALUE);
                    account.setAvailableBalance(INVEST_MARKET_VALUE);
                    account.setMarketValue(INVEST_MARKET_VALUE);
                    break;
                default:
                    break;
            }
            accountRepository.save(account);
        }

        paymentCardRepository.findByUser(user).ifPresent(card -> {
            card.setCardHolder("ALEXANDER S");
            paymentCardRepository.save(card);
        });

        beneficiaryRepository.findByUser(user).forEach(b -> {
            if ("BEN-001".equals(b.getBeneficiaryCode())) {
                b.setDisplayName("Skarsgard Family Trust");
            } else if ("BEN-002".equals(b.getBeneficiaryCode())) {
                b.setDisplayName("Alexander S Vault");
                b.setEmailOrPhone("alexander.skarsgard@northerntrust.com");
            }
            beneficiaryRepository.save(b);
        });
    }
}
