package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.northerntrust.app.dto.*;
import project.northerntrust.app.entity.*;
import project.northerntrust.app.entity.enums.*;
import project.northerntrust.app.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");

    @Autowired
    private UserRepository userRepository;
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
    private KycRepository kycRepository;
    @Autowired
    private TransferService transferService;
    @Autowired
    private OtpService otpService;

    public User requireUser(String accountNumber) {
        return userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + accountNumber));
    }

    public Map<String, Object> getClientProfile(String accountNumber) {
        User user = requireUser(accountNumber);
        Map<String, Object> profile = buildClientDto(user);
        kycRepository.findByUser(user).ifPresent(kyc -> {
            profile.put("ssnMasked", maskSsn(kyc.getSsn()));
            profile.put("taxIdMasked", maskSensitiveId(kyc.getTaxId()));
            profile.put("bvnMasked", maskSensitiveId(kyc.getBvn()));
            profile.put("ninMasked", maskSensitiveId(kyc.getNin()));
            profile.put("idType", kyc.getIdType());
            profile.put("idNumberMasked", maskSensitiveId(kyc.getIdNumber()));
            profile.put("nationality", kyc.getNationality());
            profile.put("occupation", kyc.getOccupation());
            profile.put("residentialAddress", kyc.getResidentialAddress());
            profile.put("verificationStatus", kyc.getVerificationStatus().name());
        });
        profile.put("kycStatus", user.getKycStatus().name());
        profile.put("accountStatus", user.getAccountStatus().name());
        profile.put("lastLoginAt", user.getLastLoginAt());
        profile.put("lastLoginLocation", user.getLastLoginIp());
        profile.put("lastLoginUserAgent", user.getLastLoginUserAgent());
        return profile;
    }

    private Map<String, Object> buildClientDto(User user) {
        Map<String, Object> client = new LinkedHashMap<>();
        client.put("displayName", user.getFirstName() + " " + user.getLastName());
        client.put("firstName", user.getFirstName());
        client.put("lastName", user.getLastName());
        client.put("userId", user.getClientId() != null ? user.getClientId() : "USR-89024");
        client.put("accountNumber", user.getAccountNumber());
        client.put("email", user.getEmail());
        client.put("phoneNumber", user.getPhoneNumber());
        client.put("dateOfBirth", user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null);
        client.put("streetAddress", user.getStreetAddress());
        client.put("city", user.getCity());
        client.put("state", user.getState());
        client.put("postalCode", user.getPostalCode());
        client.put("country", user.getCountry());
        return client;
    }

    private String maskSsn(String ssn) {
        if (ssn == null || ssn.length() < 4) {
            return "***-**-****";
        }
        return "***-**-" + ssn.substring(ssn.length() - 4);
    }

    private String maskSensitiveId(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        if (value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }

    public Map<String, Object> getOverview(String accountNumber) {
        User user = requireUser(accountNumber);
        List<Account> accounts = accountRepository.findByUser(user);

        BigDecimal totalBalance = accounts.stream()
                .map(a -> {
                    if (a.getProductKey() == ProductKey.CREDIT && a.getCreditLimit() != null) {
                        return a.getCreditLimit().subtract(a.getAmountOwed() != null ? a.getAmountOwed() : BigDecimal.ZERO);
                    }
                    return a.getMarketValue() != null ? a.getMarketValue() : a.getBalance();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pending = accounts.stream()
                .map(a -> a.getPendingAmount() != null ? a.getPendingAmount().abs() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("client", buildClientDto(user));
        result.put("profile", getClientProfile(accountNumber));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalPortfolioBalance", totalBalance);
        summary.put("availableBalance", totalBalance.subtract(pending));
        summary.put("pendingSettlements", pending);
        summary.put("monthOverMonthChangePct", 2.4);
        result.put("summary", summary);

        result.put("accounts", accounts.stream().map(this::toAccountDto).collect(Collectors.toList()));
        result.put("recentActivity", getRecentActivity(user, 5));
        result.put("alerts", buildAlerts(user));
        result.put("savingsGoal", savingsGoal());
        result.put("card", getCardDto(user));
        long pendingCount = transferRepository.findByUserAndPendingApprovalTrueOrderByRiskScoreDesc(user).size();
        result.put("insights", Arrays.asList(
                "Portfolio up 2.4% month-over-month driven by managed equity allocation.",
                pendingCount + " transfer(s) awaiting compliance review — highest risk score 92.",
                "Savings Vault APY holding at 4.50% — consider reserve sweep."));
        return result;
    }

    public Map<String, Object> getAccountBalances(String accountNumber) {
        User user = requireUser(accountNumber);
        Map<String, Object> balances = new LinkedHashMap<>();
        for (Account a : accountRepository.findByUser(user)) {
            String key = a.getProductKey().name().toLowerCase();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", a.getDisplayName());
            entry.put("available", a.getAvailableBalance());
            entry.put("balance", a.getBalance());
            entry.put("accountNumber", a.getAccountNumber());
            if (a.getProductKey() == ProductKey.CREDIT) {
                entry.put("available", a.getCreditLimit().subtract(
                        a.getAmountOwed() != null ? a.getAmountOwed() : BigDecimal.ZERO));
            }
            balances.put(key, entry);
        }
        return balances;
    }

    private Map<String, Object> toAccountDto(Account a) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("key", a.getProductKey().name().toLowerCase());
        dto.put("name", a.getDisplayName());
        dto.put("badge", a.getBadge());
        dto.put("balance", a.getBalance());
        dto.put("currency", a.getCurrency());
        dto.put("accountNumber", a.getAccountNumber());
        if (a.getProductKey() == ProductKey.CHECKING) {
            BigDecimal pending = a.getPendingAmount() != null ? a.getPendingAmount() : BigDecimal.ZERO;
            dto.put("ledgerBalance", a.getBalance().subtract(pending));
            dto.put("pendingAmount", pending);
        }
        if (a.getProductKey() == ProductKey.SAVINGS) {
            dto.put("apyPct", a.getApyPercent());
            dto.put("earnedThisPeriod", a.getEarnedThisPeriod());
        }
        if (a.getProductKey() == ProductKey.CREDIT) {
            dto.put("owed", a.getAmountOwed());
            dto.put("limit", a.getCreditLimit());
            dto.put("availableCredit", a.getCreditLimit().subtract(a.getAmountOwed() != null ? a.getAmountOwed() : BigDecimal.ZERO));
        }
        if (a.getProductKey() == ProductKey.INVEST) {
            dto.put("marketValue", a.getMarketValue());
            dto.put("todayChange", a.getTodayChange());
            dto.put("todayChangePct", a.getTodayChangePct());
            dto.put("roiPct", a.getRoiPercent());
        }
        return dto;
    }

    private List<Map<String, Object>> getRecentActivity(User user, int limit) {
        return statementLineRepository.findByUserOrderByLineDateDesc(user).stream()
                .limit(limit)
                .map(line -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", line.getLineDate().format(DISPLAY_DATE));
                    m.put("description", line.getDescription());
                    m.put("counterparty", line.getSource());
                    m.put("type", line.getChannel() != null ? line.getChannel() + " Transfer" : line.getLineType());
                    m.put("amount", line.getAmount());
                    m.put("currency", "USD");
                    return m;
                }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildAlerts(User user) {
        long pending = transferRepository.findByUserAndPendingApprovalTrueOrderByRiskScoreDesc(user).size();
        List<Map<String, Object>> alerts = new ArrayList<>();
        if (pending > 0) {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("severity", "danger");
            a.put("title", pending + " transfer(s) require approval");
            a.put("body", "Compliance holds detected on high-risk international wires.");
            alerts.add(a);
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("severity", "info");
        info.put("title", "Savings goal on track");
        info.put("body", "You are 60% toward your $150,000 reserve target.");
        alerts.add(info);
        return alerts;
    }

    private Map<String, Object> savingsGoal() {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("target", 150000);
        g.put("saved", 90000);
        g.put("autoSaveEnabled", true);
        g.put("autoSavePct", 5);
        return g;
    }

    private Map<String, Object> getCardDto(User user) {
        return paymentCardRepository.findByUser(user).map(card -> {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("lastFour", card.getLastFour());
            c.put("holder", card.getCardHolder());
            c.put("expires", card.getExpires());
            c.put("frozen", card.getFrozen());
            return c;
        }).orElse(Collections.emptyMap());
    }

    public List<Map<String, Object>> getBeneficiaries(String accountNumber, String type, String search) {
        User user = requireUser(accountNumber);
        List<Beneficiary> list = beneficiaryRepository.findByUser(user);
        return list.stream()
                .filter(b -> type == null || type.isEmpty() || "all".equalsIgnoreCase(type)
                        || b.getBeneficiaryType().name().equalsIgnoreCase(type))
                .filter(b -> search == null || search.isEmpty()
                        || b.getDisplayName().toLowerCase().contains(search.toLowerCase())
                        || (b.getAccountNumber() != null && b.getAccountNumber().contains(search)))
                .map(this::toBeneficiaryDto)
                .collect(Collectors.toList());
    }

    public Map<String, Object> toBeneficiaryDto(Beneficiary b) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("beneficiaryId", b.getBeneficiaryCode());
        dto.put("type", b.getBeneficiaryType().name());
        dto.put("displayName", b.getDisplayName());
        dto.put("relationship", b.getRelationship());
        dto.put("destinationDetails", buildDestinationDetails(b));
        Map<String, Object> limits = new LinkedHashMap<>();
        limits.put("single", b.getSingleLimit());
        limits.put("daily", b.getDailyLimit());
        dto.put("transferLimits", limits);
        dto.put("status", b.getStatus().name());
        dto.put("isTrusted", b.getTrusted());
        dto.put("trustLevel", b.getTrustLevel().name());
        dto.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate().toString() : "");
        dto.put("lastUsedAt", b.getLastUsedAt() != null
                ? b.getLastUsedAt().format(DISPLAY_DATE) : "Never Used");
        return dto;
    }

    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            return "";
        }
        if (accountNumber.contains("•")) {
            return accountNumber;
        }
        String digits = accountNumber.replaceAll("\\s+", "");
        if (digits.length() <= 4) {
            return digits;
        }
        return "•••• •••• •••• " + digits.substring(digits.length() - 4);
    }

    private Map<String, Object> buildDestinationDetails(Beneficiary b) {
        Map<String, Object> d = new LinkedHashMap<>();
        switch (b.getBeneficiaryType()) {
            case BANK:
                d.put("bankName", b.getBankName());
                d.put("accountNumber", maskAccountNumber(b.getAccountNumber()));
                d.put("routingOrSwift", b.getRoutingOrSwift());
                d.put("bankAddress", b.getBankAddress());
                d.put("country", b.getCountry());
                break;
            case INTERNAL:
                d.put("userId", b.getDestinationUserId());
                d.put("emailOrPhone", b.getEmailOrPhone());
                break;
            case CREDIT:
                d.put("creditAccountId", b.getCreditAccountId());
                d.put("accountType", "NT_CREDIT_LINE");
                d.put("outstandingBalance", 14800.00);
                d.put("dueDate", "June 15, 2026");
                break;
            case INVESTMENT:
                d.put("portfolioId", b.getPortfolioId());
                d.put("brokerOrManagedAccount", true);
                d.put("riskLevel", "Low Risk");
                d.put("marketValue", 169400000.00);
                break;
            default:
                break;
        }
        return d;
    }

    public Map<String, Object> getTransferHistory(String accountNumber, String search, String type,
                                                  String status, String period, int page, int size) {
        User user = requireUser(accountNumber);
        List<Transfer> all = transferRepository.findByUserOrderByCreatedAtDesc(user);

        LocalDateTime cutoff = periodCutoff(period);
        List<Transfer> filtered = all.stream()
                .filter(t -> cutoff == null || (t.getCreatedAt() != null && t.getCreatedAt().isAfter(cutoff)))
                .filter(t -> type == null || "all".equalsIgnoreCase(type) || mapTransferType(t).equalsIgnoreCase(type))
                .filter(t -> status == null || "all".equalsIgnoreCase(status)
                        || statusMatches(t, status))
                .filter(t -> search == null || search.isEmpty()
                        || t.getReference().toLowerCase().contains(search.toLowerCase())
                        || (t.getCounterpartyName() != null && t.getCounterpartyName().toLowerCase().contains(search.toLowerCase())))
                .collect(Collectors.toList());

        BigDecimal totalVol = filtered.stream().map(Transfer::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long settled = filtered.stream().filter(t -> t.getDisplayStatus() == DisplayTransferStatus.Settled).count();
        long held = filtered.stream().filter(t -> t.getDisplayStatus() != null && Arrays.asList(
                DisplayTransferStatus.Compliance_Hold, DisplayTransferStatus.OFAC_Hold,
                DisplayTransferStatus.Processing, DisplayTransferStatus.Awaiting_Treasury_Approval,
                DisplayTransferStatus.Awaiting_Verification, DisplayTransferStatus.Blocked_OFAC_Review
        ).contains(t.getDisplayStatus())).count();
        long failed = filtered.stream().filter(t -> t.getDisplayStatus() == DisplayTransferStatus.Failed
                || t.getDisplayStatus() == DisplayTransferStatus.Returned).count();

        int total = filtered.size();
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, size);
        int from = Math.min(total, (safePage - 1) * safeSize);
        int to = Math.min(total, from + safeSize);
        List<Map<String, Object>> items = filtered.subList(from, to).stream()
                .map(this::toTransferHistoryItem)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalVolume", totalVol);
        summary.put("settledCount", settled);
        summary.put("onHoldCount", held);
        summary.put("failedCount", failed);
        result.put("summary", summary);
        result.put("items", items);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", total);
        return result;
    }

    private Map<String, Object> toTransferHistoryItem(Transfer t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getReference());
        m.put("date", toIsoTimestamp(t.getCreatedAt()));
        m.put("type", mapTransferType(t));
        m.put("counterparty", t.getCounterpartyName());
        m.put("source", t.getSourceAccountLabel());
        m.put("amount", t.getAmount());
        m.put("currency", t.getCurrency());
        m.put("status", displayStatusLabel(t.getDisplayStatus()));
        return m;
    }

    private String toIsoTimestamp(LocalDateTime value) {
        if (value == null) {
            return LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toString();
        }
        return value.atZone(ZoneId.systemDefault()).toInstant().toString();
    }

    private String mapTransferType(Transfer t) {
        if (t.getTransferType() == TransferType.INTERNAL) return "Internal";
        if (t.getTransferType() == TransferType.ACH) return "ACH";
        if (t.getTransferType() == TransferType.WIRE) return "Wire";
        if (t.getTransferType() == TransferType.SWIFT) return "International";
        return t.getTransferType().name();
    }

    private String displayStatusLabel(DisplayTransferStatus s) {
        if (s == null) return "Processing";
        if (s == DisplayTransferStatus.Blocked_OFAC_Review) return "Blocked - OFAC Review";
        if (s == DisplayTransferStatus.Awaiting_Treasury_Approval) return "Awaiting Treasury Approval";
        if (s == DisplayTransferStatus.Awaiting_Verification) return "Awaiting Verification";
        if (s == DisplayTransferStatus.Pending_NACHA_Batch) return "Pending NACHA Batch";
        return s.name().replace('_', ' ');
    }

    private boolean statusMatches(Transfer t, String statusFilter) {
        String label = displayStatusLabel(t.getDisplayStatus());
        return label.equalsIgnoreCase(statusFilter)
                || label.replace(" - ", " ").replace("-", " ").equalsIgnoreCase(statusFilter.replace(" - ", " "));
    }

    private LocalDateTime periodCutoff(String period) {
        if (period == null || "all".equalsIgnoreCase(period)) return null;
        LocalDate today = LocalDate.now();
        if ("today".equalsIgnoreCase(period)) return today.atStartOfDay();
        if ("7d".equalsIgnoreCase(period)) return today.minusDays(6).atStartOfDay();
        if ("30d".equalsIgnoreCase(period)) return today.minusDays(29).atStartOfDay();
        return null;
    }

    public List<Map<String, Object>> getStatements(String accountNumber, String tab, String period,
                                                   String type, String status, String search) {
        User user = requireUser(accountNumber);
        List<StatementLine> lines = statementLineRepository.findByUserOrderByLineDateDesc(user);

        ProductKey keyFilter = tabToProductKey(tab);
        LocalDate periodStart = statementPeriodStart(period);

        return lines.stream()
                .filter(l -> keyFilter == null || keyFilter.equals(l.getProductKey()))
                .filter(l -> periodStart == null || !l.getLineDate().isBefore(periodStart))
                .filter(l -> type == null || type.isEmpty() || "all".equalsIgnoreCase(type)
                        || (l.getLineType() != null && l.getLineType().equalsIgnoreCase(type)))
                .filter(l -> status == null || status.isEmpty() || "all".equalsIgnoreCase(status)
                        || (l.getStatus() != null && l.getStatus().equalsIgnoreCase(status)))
                .filter(l -> search == null || search.isEmpty()
                        || (l.getDescription() != null && l.getDescription().toLowerCase().contains(search.toLowerCase())))
                .map(this::toStatementDto)
                .collect(Collectors.toList());
    }

    private ProductKey tabToProductKey(String tab) {
        if (tab == null || "unified".equalsIgnoreCase(tab) || "all".equalsIgnoreCase(tab)) return null;
        if ("checking".equalsIgnoreCase(tab)) return ProductKey.CHECKING;
        if ("savings".equalsIgnoreCase(tab)) return ProductKey.SAVINGS;
        if ("credit".equalsIgnoreCase(tab)) return ProductKey.CREDIT;
        if ("invest".equalsIgnoreCase(tab)) return ProductKey.INVEST;
        return null;
    }

    private LocalDate statementPeriodStart(String period) {
        if (period == null || "all".equalsIgnoreCase(period)) return null;
        if ("this_month".equalsIgnoreCase(period)) return LocalDate.now().withDayOfMonth(1);
        if ("last_3_months".equalsIgnoreCase(period)) return LocalDate.now().minusMonths(3);
        return null;
    }

    private Map<String, Object> toStatementDto(StatementLine l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", l.getLineDate().toString());
        m.put("description", l.getDescription());
        m.put("source", l.getSource());
        m.put("type", l.getLineType());
        m.put("amount", l.getAmount());
        m.put("balanceAfter", l.getBalanceAfter());
        m.put("status", l.getStatus());
        m.put("channel", l.getChannel());
        if (l.getAsset() != null) m.put("asset", l.getAsset());
        if (l.getUnits() != null) m.put("units", l.getUnits());
        if (l.getPrice() != null) m.put("price", l.getPrice());
        if (l.getRateLabel() != null) m.put("rate", l.getRateLabel());
        if (l.getCharged() != null) m.put("charged", l.getCharged());
        if (l.getPayment() != null) m.put("payment", l.getPayment());
        if (l.getCreditAvailable() != null) m.put("creditAvailable", l.getCreditAvailable());
        return m;
    }

    public List<Map<String, Object>> getPendingApprovals(String accountNumber) {
        User user = requireUser(accountNumber);
        return transferRepository.findByUserAndPendingApprovalTrueOrderByRiskScoreDesc(user).stream()
                .map(this::toApprovalDto)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toApprovalDto(Transfer t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getReference());
        m.put("type", mapTransferType(t) + " Transfer");
        m.put("amount", t.getAmount());
        m.put("currency", t.getCurrency());
        m.put("usdEquivalent", t.getUsdEquivalent() != null ? t.getUsdEquivalent() : t.getAmount());
        m.put("source", t.getSourceAccountLabel());
        m.put("beneficiary", t.getCounterpartyName());
        m.put("country", t.getApprovalCountry() != null ? t.getApprovalCountry() : "United States (US)");
        m.put("riskScore", t.getRiskScore());
        m.put("riskLevel", t.getRiskLevel());
        m.put("status", displayStatusLabel(t.getDisplayStatus()));
        m.put("timestamp", toIsoTimestamp(t.getCreatedAt()));
        m.put("flags", parseJsonArray(t.getComplianceFlagsJson()));
        m.put("compliance", parseJsonObject(t.getComplianceJson()));
        m.put("history", parseJsonArray(t.getApprovalHistoryJson()));
        return m;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyList();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.isEmpty()) return Collections.emptyMap();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @Transactional
    public MessageResponse approveTransfer(String accountNumber, String reference, String action) {
        User user = requireUser(accountNumber);
        Transfer t = transferRepository.findByReferenceAndUser(reference, user)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found"));
        if ("approve".equalsIgnoreCase(action)) {
            return transferService.settleHeldTransfer(reference);
        } else if ("reject".equalsIgnoreCase(action)) {
            return transferService.reverseHeldTransfer(reference);
        } else if ("escalate".equalsIgnoreCase(action)) {
            return transferService.escalateHeldTransfer(reference);
        }
        return new MessageResponse(false, "Unknown action: " + action);
    }

    public Map<String, Object> getAnalytics(String accountNumber, String timeRange,
                                            String accountScope, String currency, String txType) {
        User user = requireUser(accountNumber);
        List<Account> accounts = accountRepository.findByUser(user);
        LocalDate periodStart = analyticsPeriodStart(timeRange);
        LocalDateTime transferCutoff = periodStart.atStartOfDay();

        List<StatementLine> allLines = statementLineRepository.findByUserOrderByLineDateDesc(user);
        List<StatementLine> scopedLines = allLines.stream()
                .filter(l -> !l.getLineDate().isBefore(periodStart))
                .filter(l -> matchesAccountScope(l.getProductKey(), accountScope))
                .filter(l -> lineMatchesTxType(l, txType))
                .collect(Collectors.toList());

        List<Transfer> allTransfers = transferRepository.findByUserOrderByCreatedAtDesc(user);
        List<Transfer> scopedTransfers = allTransfers.stream()
                .filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().isBefore(transferCutoff))
                .filter(t -> transferMatchesAccountScope(t, accountScope))
                .filter(t -> transferMatchesCurrency(t, currency))
                .filter(t -> transferMatchesTxType(t, txType))
                .collect(Collectors.toList());

        BigDecimal totalBalance = scopedTotalBalance(accounts, accountScope);
        BigDecimal portfolioPerformance = accounts.stream()
                .filter(a -> a.getProductKey() == ProductKey.INVEST)
                .map(Account::getRoiPercent)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        BigDecimal inflow = scopedLines.stream()
                .map(StatementLine::getAmount)
                .filter(Objects::nonNull)
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outflow = scopedLines.stream()
                .map(StatementLine::getAmount)
                .filter(Objects::nonNull)
                .filter(a -> a.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netCashFlow = inflow.subtract(outflow);

        List<StatementLine> feeLines = allLines.stream()
                .filter(l -> !l.getLineDate().isBefore(periodStart))
                .filter(l -> matchesAccountScope(l.getProductKey(), accountScope))
                .filter(l -> l.getLineType() != null && "Fee".equalsIgnoreCase(l.getLineType()))
                .collect(Collectors.toList());
        BigDecimal fxFeesPaid = feeLines.stream()
                .map(StatementLine::getAmount)
                .filter(Objects::nonNull)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalTransfers = scopedTransfers.size();
        long settled = scopedTransfers.stream()
                .filter(t -> t.getDisplayStatus() == DisplayTransferStatus.Settled).count();
        long failed = scopedTransfers.stream()
                .filter(t -> t.getDisplayStatus() == DisplayTransferStatus.Failed
                        || t.getDisplayStatus() == DisplayTransferStatus.Returned).count();
        long pending = scopedTransfers.stream()
                .filter(t -> Boolean.TRUE.equals(t.getPendingApproval())).count();

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalBalance", totalBalance);
        kpis.put("netCashFlow", netCashFlow);
        kpis.put("totalTransfers", totalTransfers);
        kpis.put("fxFeesPaid", fxFeesPaid);
        kpis.put("approvalRate", totalTransfers == 0 ? 0 : settled * 100.0 / totalTransfers);
        kpis.put("failedRate", totalTransfers == 0 ? 0 : failed * 100.0 / totalTransfers);
        kpis.put("portfolioPerformance", portfolioPerformance);
        kpis.put("monthOverMonthChangePct", 2.4);

        Map<String, Integer> breakdown = new LinkedHashMap<>();
        breakdown.put("Internal", (int) scopedTransfers.stream()
                .filter(t -> t.getTransferType() == TransferType.INTERNAL).count());
        breakdown.put("ACH", (int) scopedTransfers.stream()
                .filter(t -> t.getTransferType() == TransferType.ACH).count());
        breakdown.put("Wire", (int) scopedTransfers.stream()
                .filter(t -> t.getTransferType() == TransferType.WIRE).count());
        breakdown.put("International", (int) scopedTransfers.stream()
                .filter(t -> t.getTransferType() == TransferType.SWIFT).count());

        Map<String, Object> charts = new LinkedHashMap<>();
        charts.putAll(buildCashFlowChart(scopedLines));
        charts.put("accountDistribution", buildAccountDistribution(accounts));
        charts.put("fxExposure", buildFxExposure(scopedTransfers));
        charts.put("riskCompliance", buildRiskCompliance(scopedTransfers));
        charts.put("approvalFunnel", buildApprovalFunnel(scopedTransfers));

        Map<String, Object> tables = new LinkedHashMap<>();
        tables.put("cashFlowDetail", buildCashFlowDetail(scopedLines));
        tables.put("transferDetail", buildTransferDetail(scopedTransfers));
        tables.put("allocationDetail", buildAllocationDetail(accounts));
        tables.put("fxDetail", buildFxDetail(scopedTransfers, feeLines));
        tables.put("riskDetail", buildRiskDetail(scopedTransfers));
        tables.put("approvalDetail", buildApprovalDetail(scopedTransfers));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kpis", kpis);
        result.put("transferBreakdown", breakdown);
        result.put("pendingApprovals", pending);
        result.put("charts", charts);
        result.put("tables", tables);
        result.put("insights", buildAnalyticsInsights(kpis, breakdown, pending, scopedTransfers));
        return result;
    }

    private LocalDate analyticsPeriodStart(String timeRange) {
        LocalDate today = LocalDate.now();
        if (timeRange == null || "7d".equalsIgnoreCase(timeRange)) {
            return today.minusDays(6);
        }
        if ("today".equalsIgnoreCase(timeRange)) {
            return today;
        }
        if ("30d".equalsIgnoreCase(timeRange)) {
            return today.minusDays(29);
        }
        if ("quarter".equalsIgnoreCase(timeRange)) {
            return today.minusMonths(3);
        }
        if ("year".equalsIgnoreCase(timeRange)) {
            return today.minusYears(1);
        }
        return today.minusDays(6);
    }

    private ProductKey accountScopeToKey(String accountScope) {
        if (accountScope == null || accountScope.isEmpty() || "all".equalsIgnoreCase(accountScope)) {
            return null;
        }
        if ("checking".equalsIgnoreCase(accountScope)) return ProductKey.CHECKING;
        if ("savings".equalsIgnoreCase(accountScope)) return ProductKey.SAVINGS;
        if ("credit".equalsIgnoreCase(accountScope)) return ProductKey.CREDIT;
        if ("invest".equalsIgnoreCase(accountScope)) return ProductKey.INVEST;
        return null;
    }

    private boolean matchesAccountScope(ProductKey key, String accountScope) {
        ProductKey scope = accountScopeToKey(accountScope);
        return scope == null || scope == key;
    }

    private boolean transferMatchesAccountScope(Transfer transfer, String accountScope) {
        ProductKey scope = accountScopeToKey(accountScope);
        if (scope == null) {
            return true;
        }
        return transfer.getFromAccount() != null && transfer.getFromAccount().getProductKey() == scope;
    }

    private boolean transferMatchesCurrency(Transfer transfer, String currency) {
        if (currency == null || currency.isEmpty() || "all".equalsIgnoreCase(currency)) {
            return true;
        }
        return transfer.getCurrency() != null && currency.equalsIgnoreCase(transfer.getCurrency());
    }

    private boolean lineMatchesTxType(StatementLine line, String txType) {
        if (txType == null || txType.isEmpty() || "all".equalsIgnoreCase(txType)) {
            return true;
        }
        if ("fees".equalsIgnoreCase(txType)) {
            return line.getLineType() != null && "Fee".equalsIgnoreCase(line.getLineType());
        }
        if ("investments".equalsIgnoreCase(txType)) {
            return line.getProductKey() == ProductKey.INVEST
                    || "Investments".equalsIgnoreCase(line.getLineType())
                    || ("Interest".equalsIgnoreCase(line.getLineType())
                    && line.getProductKey() == ProductKey.INVEST);
        }
        if ("transfers".equalsIgnoreCase(txType)) {
            return line.getLineType() != null && ("Deposit".equalsIgnoreCase(line.getLineType())
                    || "Withdrawal".equalsIgnoreCase(line.getLineType()));
        }
        if ("fx".equalsIgnoreCase(txType) || "approvals".equalsIgnoreCase(txType)) {
            return false;
        }
        return true;
    }

    private boolean transferMatchesTxType(Transfer transfer, String txType) {
        if (txType == null || txType.isEmpty() || "all".equalsIgnoreCase(txType)
                || "transfers".equalsIgnoreCase(txType)) {
            return true;
        }
        if ("fx".equalsIgnoreCase(txType)) {
            return transfer.getTransferType() == TransferType.SWIFT;
        }
        if ("approvals".equalsIgnoreCase(txType)) {
            return Boolean.TRUE.equals(transfer.getPendingApproval());
        }
        if ("investments".equalsIgnoreCase(txType)) {
            return transfer.getTransferType() == TransferType.INTERNAL
                    && transfer.getFromAccount() != null
                    && transfer.getFromAccount().getProductKey() == ProductKey.INVEST;
        }
        if ("fees".equalsIgnoreCase(txType)) {
            return false;
        }
        return true;
    }

    private BigDecimal accountContribution(Account account) {
        if (account.getProductKey() == ProductKey.CREDIT && account.getCreditLimit() != null) {
            return account.getCreditLimit().subtract(
                    account.getAmountOwed() != null ? account.getAmountOwed() : BigDecimal.ZERO);
        }
        return account.getMarketValue() != null ? account.getMarketValue() : account.getBalance();
    }

    private BigDecimal scopedTotalBalance(List<Account> accounts, String accountScope) {
        ProductKey scope = accountScopeToKey(accountScope);
        return accounts.stream()
                .filter(a -> scope == null || a.getProductKey() == scope)
                .map(this::accountContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Object> buildCashFlowChart(List<StatementLine> lines) {
        LocalDate end = LocalDate.now();
        DateTimeFormatter dayLabel = DateTimeFormatter.ofPattern("EEE");
        List<String> labels = new ArrayList<>();
        List<BigDecimal> inflows = new ArrayList<>();
        List<BigDecimal> outflows = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = end.minusDays(i);
            labels.add(dayLabel.format(day));
            BigDecimal in = BigDecimal.ZERO;
            BigDecimal out = BigDecimal.ZERO;
            for (StatementLine line : lines) {
                if (!day.equals(line.getLineDate()) || line.getAmount() == null) {
                    continue;
                }
                if (line.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
                    in = in.add(line.getAmount());
                } else {
                    out = out.add(line.getAmount().abs());
                }
            }
            inflows.add(in);
            outflows.add(out);
        }
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("cashFlowLabels", labels);
        chart.put("cashFlowInflow", inflows);
        chart.put("cashFlowOutflow", outflows);
        return chart;
    }

    private List<Map<String, Object>> buildAccountDistribution(List<Account> accounts) {
        return accounts.stream().map(a -> {
            Map<String, Object> slice = new LinkedHashMap<>();
            slice.put("name", a.getDisplayName());
            slice.put("value", accountContribution(a));
            return slice;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildFxExposure(List<Transfer> transfers) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Transfer transfer : transfers) {
            String cur = transfer.getCurrency() != null ? transfer.getCurrency() : "USD";
            BigDecimal amount = transfer.getUsdEquivalent() != null ? transfer.getUsdEquivalent() : transfer.getAmount();
            totals.merge(cur, amount != null ? amount : BigDecimal.ZERO, BigDecimal::add);
        }
        if (totals.isEmpty()) {
            totals.put("USD", BigDecimal.ZERO);
        }
        BigDecimal grandTotal = totals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (grandTotal.compareTo(BigDecimal.ZERO) == 0) {
            grandTotal = BigDecimal.ONE;
        }
        List<Map<String, Object>> exposure = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("currency", entry.getKey());
            row.put("exposure", entry.getValue()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(grandTotal, 0, RoundingMode.HALF_UP)
                    .intValue());
            row.put("volatility", fxVolatility(entry.getKey()));
            exposure.add(row);
        }
        exposure.sort((a, b) -> Integer.compare((Integer) b.get("exposure"), (Integer) a.get("exposure")));
        return exposure;
    }

    private String fxVolatility(String currency) {
        if ("JPY".equalsIgnoreCase(currency)) {
            return "High";
        }
        if ("EUR".equalsIgnoreCase(currency) || "GBP".equalsIgnoreCase(currency)) {
            return "Medium";
        }
        return "Low";
    }

    private List<Map<String, Object>> buildRiskCompliance(List<Transfer> transfers) {
        long highRisk = transfers.stream()
                .filter(t -> t.getRiskScore() != null && t.getRiskScore() >= 75).count();
        long flagged = transfers.stream()
                .filter(t -> t.getDisplayStatus() != null && t.getDisplayStatus() != DisplayTransferStatus.Settled).count();
        long complianceHolds = transfers.stream()
                .filter(t -> t.getDisplayStatus() != null && Arrays.asList(
                        DisplayTransferStatus.Compliance_Hold,
                        DisplayTransferStatus.OFAC_Hold,
                        DisplayTransferStatus.Blocked_OFAC_Review,
                        DisplayTransferStatus.Awaiting_Treasury_Approval,
                        DisplayTransferStatus.Awaiting_Verification
                ).contains(t.getDisplayStatus())).count();
        long sanctionTriggers = transfers.stream()
                .filter(t -> t.getDisplayStatus() != null && Arrays.asList(
                        DisplayTransferStatus.OFAC_Hold,
                        DisplayTransferStatus.Blocked_OFAC_Review
                ).contains(t.getDisplayStatus())).count();

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(riskCell("High-Risk Transfers", highRisk, highRisk > 0 ? "critical" : "low"));
        rows.add(riskCell("Flagged Transactions", flagged, flagged > 5 ? "high" : flagged > 0 ? "medium" : "low"));
        rows.add(riskCell("Compliance Holds", complianceHolds, complianceHolds > 0 ? "medium" : "low"));
        rows.add(riskCell("Sanction Triggers", sanctionTriggers, sanctionTriggers > 0 ? "critical" : "low"));
        return rows;
    }

    private Map<String, Object> riskCell(String category, long count, String level) {
        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("category", category);
        cell.put("count", count);
        cell.put("level", level);
        return cell;
    }

    private List<Map<String, Object>> buildApprovalFunnel(List<Transfer> transfers) {
        long initiated = transfers.size();
        long pendingApproval = transfers.stream()
                .filter(t -> Boolean.TRUE.equals(t.getPendingApproval())).count();
        long approved = transfers.stream()
                .filter(t -> t.getDisplayStatus() == DisplayTransferStatus.Settled).count();
        long rejected = transfers.stream()
                .filter(t -> t.getDisplayStatus() == DisplayTransferStatus.Failed
                        || t.getDisplayStatus() == DisplayTransferStatus.Returned).count();
        long escalated = transfers.stream()
                .filter(t -> t.getDisplayStatus() != null && Arrays.asList(
                        DisplayTransferStatus.Compliance_Hold,
                        DisplayTransferStatus.OFAC_Hold,
                        DisplayTransferStatus.Blocked_OFAC_Review
                ).contains(t.getDisplayStatus())).count();

        List<Map<String, Object>> funnel = new ArrayList<>();
        funnel.add(funnelStage("Initiated", initiated, "#94a3b8"));
        funnel.add(funnelStage("Pending Approval", pendingApproval, "#d97706"));
        funnel.add(funnelStage("Approved", approved, "#115740"));
        funnel.add(funnelStage("Rejected", rejected, "#dc2626"));
        funnel.add(funnelStage("Escalated", escalated, "#7c3aed"));
        return funnel;
    }

    private Map<String, Object> funnelStage(String stage, long count, String color) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("stage", stage);
        row.put("value", count);
        row.put("color", color);
        return row;
    }

    private List<Map<String, Object>> buildCashFlowDetail(List<StatementLine> lines) {
        LocalDate end = LocalDate.now();
        DateTimeFormatter dayLabel = DateTimeFormatter.ofPattern("EEE");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = end.minusDays(i);
            BigDecimal in = BigDecimal.ZERO;
            BigDecimal out = BigDecimal.ZERO;
            for (StatementLine line : lines) {
                if (!day.equals(line.getLineDate()) || line.getAmount() == null) {
                    continue;
                }
                if (line.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
                    in = in.add(line.getAmount());
                } else {
                    out = out.add(line.getAmount().abs());
                }
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("day", dayLabel.format(day));
            row.put("inflow", in);
            row.put("outflow", out);
            row.put("net", in.subtract(out));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> buildTransferDetail(List<Transfer> transfers) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String type : Arrays.asList("Internal", "ACH", "Wire", "International")) {
            List<Transfer> bucket = transfers.stream()
                    .filter(t -> type.equals(mapTransferType(t)))
                    .collect(Collectors.toList());
            BigDecimal volume = bucket.stream()
                    .map(Transfer::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", type);
            row.put("count", bucket.size());
            row.put("volume", volume);
            row.put("avgSize", bucket.isEmpty() ? BigDecimal.ZERO
                    : volume.divide(BigDecimal.valueOf(bucket.size()), 2, RoundingMode.HALF_UP));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> buildAllocationDetail(List<Account> accounts) {
        BigDecimal total = accounts.stream()
                .map(this::accountContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            total = BigDecimal.ONE;
        }
        BigDecimal finalTotal = total;
        return accounts.stream().map(a -> {
            BigDecimal value = accountContribution(a);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("account", a.getDisplayName());
            row.put("marketValue", value);
            row.put("weight", value.multiply(BigDecimal.valueOf(100))
                    .divide(finalTotal, 1, RoundingMode.HALF_UP));
            row.put("dayChange", accountDayChange(a));
            return row;
        }).collect(Collectors.toList());
    }

    private String accountDayChange(Account account) {
        if (account.getProductKey() == ProductKey.INVEST && account.getTodayChangePct() != null) {
            BigDecimal pct = account.getTodayChangePct();
            return (pct.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + pct.setScale(1, RoundingMode.HALF_UP) + "%";
        }
        return "—";
    }

    private List<Map<String, Object>> buildFxDetail(List<Transfer> transfers, List<StatementLine> feeLines) {
        List<Map<String, Object>> exposure = buildFxExposure(transfers);
        Map<String, BigDecimal> feeTotals = new LinkedHashMap<>();
        for (StatementLine line : feeLines) {
            feeTotals.merge("USD", line.getAmount() != null ? line.getAmount().abs() : BigDecimal.ZERO, BigDecimal::add);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> item : exposure) {
            Map<String, Object> row = new LinkedHashMap<>();
            String currency = (String) item.get("currency");
            row.put("currency", currency);
            row.put("exposure", item.get("exposure") + "%");
            row.put("volatility", item.get("volatility"));
            row.put("feesYtd", feeTotals.getOrDefault(currency, BigDecimal.ZERO));
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> buildRiskDetail(List<Transfer> transfers) {
        Map<String, List<Transfer>> grouped = new LinkedHashMap<>();
        grouped.put("OFAC Hold", transfers.stream()
                .filter(t -> t.getDisplayStatus() == DisplayTransferStatus.OFAC_Hold
                        || t.getDisplayStatus() == DisplayTransferStatus.Blocked_OFAC_Review)
                .collect(Collectors.toList()));
        grouped.put("Compliance Hold", transfers.stream()
                .filter(t -> t.getDisplayStatus() == DisplayTransferStatus.Compliance_Hold
                        || t.getDisplayStatus() == DisplayTransferStatus.Awaiting_Treasury_Approval
                        || t.getDisplayStatus() == DisplayTransferStatus.Awaiting_Verification)
                .collect(Collectors.toList()));
        grouped.put("High Risk Score", transfers.stream()
                .filter(t -> t.getRiskScore() != null && t.getRiskScore() >= 75)
                .collect(Collectors.toList()));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, List<Transfer>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            Transfer latest = entry.getValue().get(0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", entry.getKey());
            row.put("count", entry.getValue().size());
            row.put("severity", latest.getRiskLevel() != null ? latest.getRiskLevel() : "Medium");
            row.put("lastUpdated", latest.getCreatedAt() != null
                    ? latest.getCreatedAt().toLocalDate().format(DISPLAY_DATE) : "Today");
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> buildApprovalDetail(List<Transfer> transfers) {
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(approvalStageRow("Pending", transfers.stream()
                .filter(t -> Boolean.TRUE.equals(t.getPendingApproval())).count()));
        rows.add(approvalStageRow("Approved", transfers.stream()
                .filter(t -> t.getDisplayStatus() == DisplayTransferStatus.Settled).count()));
        rows.add(approvalStageRow("Rejected", transfers.stream()
                .filter(t -> t.getDisplayStatus() == DisplayTransferStatus.Failed
                        || t.getDisplayStatus() == DisplayTransferStatus.Returned).count()));
        return rows;
    }

    private Map<String, Object> approvalStageRow(String stage, long count) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("stage", stage);
        row.put("count", count);
        row.put("avgTime", count == 0 ? "—" : "Live");
        row.put("slaMet", count == 0 ? "—" : "100%");
        return row;
    }

    private List<String> buildAnalyticsInsights(Map<String, Object> kpis, Map<String, Integer> breakdown,
                                                long pendingApprovals, List<Transfer> transfers) {
        List<String> insights = new ArrayList<>();
        BigDecimal performance = (BigDecimal) kpis.get("portfolioPerformance");
        insights.add(String.format("Managed portfolio ROI is %s%% based on live account valuations.",
                performance.setScale(1, RoundingMode.HALF_UP)));
        insights.add(String.format("Net cash flow for the selected period is $%s from statement activity.",
                ((BigDecimal) kpis.get("netCashFlow")).setScale(2, RoundingMode.HALF_UP)));
        if (pendingApprovals > 0) {
            insights.add(pendingApprovals + " transfer(s) in the selected window still require authorization.");
        }
        int international = breakdown.getOrDefault("International", 0);
        if (international > 0) {
            insights.add(international + " international transfer(s) recorded in the selected period.");
        }
        long highRisk = transfers.stream()
                .filter(t -> t.getRiskScore() != null && t.getRiskScore() >= 75).count();
        if (highRisk > 0) {
            insights.add(highRisk + " high-risk transfer(s) flagged for compliance review.");
        }
        insights.add(String.format("Approval rate is %.1f%% across filtered transfer activity.",
                (Double) kpis.get("approvalRate")));
        return insights;
    }

    public Map<String, BigDecimal> getFxRates() {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        rates.put("EUR", new BigDecimal("0.92"));
        rates.put("GBP", new BigDecimal("0.79"));
        rates.put("JPY", new BigDecimal("157.20"));
        rates.put("CHF", new BigDecimal("0.90"));
        return rates;
    }

    public Optional<Account> resolveSourceAccount(User user, String productKey) {
        try {
            ProductKey key = ProductKey.valueOf(productKey.toUpperCase());
            return accountRepository.findByUserAndProductKey(user, key);
        } catch (Exception e) {
            return accountRepository.findByUser(user).stream().findFirst();
        }
    }

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public MessageResponse freezeCard(String accountNumber, boolean frozen) {
        User user = requireUser(accountNumber);
        PaymentCard card = paymentCardRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        card.setFrozen(frozen);
        paymentCardRepository.save(card);
        notificationService.recordCardFreeze(accountNumber, frozen);
        return new MessageResponse(true, frozen ? "Card frozen" : "Card unfrozen");
    }

    public BigDecimal getAccountBalance(String accountNumber, String productKey) {
        User user = requireUser(accountNumber);
        return resolveSourceAccount(user, productKey)
                .map(Account::getAvailableBalance)
                .orElse(BigDecimal.ZERO);
    }
}
