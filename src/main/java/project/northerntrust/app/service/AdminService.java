package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.entity.Account;
import project.northerntrust.app.entity.Transfer;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.AdminRole;
import project.northerntrust.app.entity.enums.DisplayTransferStatus;
import project.northerntrust.app.entity.enums.ProductKey;
import project.northerntrust.app.repository.AccountRepository;
import project.northerntrust.app.repository.TransferRepository;
import project.northerntrust.app.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminService {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final Map<String, String> activeTokens = new ConcurrentHashMap<>();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private TransferService transferService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Map<String, Object> login(String accountNumber, String password) {
        Map<String, Object> result = new LinkedHashMap<>();
        Optional<User> userOpt = userRepository.findByAccountNumber(accountNumber);
        if (userOpt.isEmpty() || userOpt.get().getAdminRole() == null
                || userOpt.get().getAdminRole() != AdminRole.ADMIN) {
            result.put("success", false);
            result.put("message", "Invalid admin credentials");
            return result;
        }
        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            result.put("success", false);
            result.put("message", "Invalid admin credentials");
            return result;
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        activeTokens.put(token, user.getAccountNumber());
        result.put("success", true);
        result.put("token", token);
        result.put("name", user.getFirstName() + " " + user.getLastName());
        result.put("accountNumber", user.getAccountNumber());
        return result;
    }

    public boolean validateToken(String token) {
        return token != null && activeTokens.containsKey(token);
    }

    public void logout(String token) {
        if (token != null) {
            activeTokens.remove(token);
        }
    }

    public User requireAdmin(String token) {
        String accountNumber = activeTokens.get(token);
        if (accountNumber == null) {
            throw new IllegalArgumentException("Unauthorized: invalid or expired admin session");
        }
        return userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Unauthorized: invalid or expired admin session"));
    }

    public Map<String, Object> getOverview() {
        List<Transfer> all = transferRepository.findAllByOrderByCreatedAtDesc();
        long pending = transferRepository.findByPendingApprovalTrueOrderByRiskScoreDesc().size();
        BigDecimal totalVolume = all.stream()
                .map(Transfer::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long settled = all.stream()
                .filter(t -> t.getDisplayStatus() == DisplayTransferStatus.Settled).count();
        long held = all.stream()
                .filter(t -> t.getDisplayStatus() != null && t.getDisplayStatus() != DisplayTransferStatus.Settled
                        && t.getDisplayStatus() != DisplayTransferStatus.Failed
                        && t.getDisplayStatus() != DisplayTransferStatus.Returned).count();
        long failed = all.stream()
                .filter(t -> t.getDisplayStatus() == DisplayTransferStatus.Failed
                        || t.getDisplayStatus() == DisplayTransferStatus.Returned).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalTransfers", all.size());
        result.put("pendingApprovals", pending);
        result.put("totalVolume", totalVolume);
        result.put("settled", settled);
        result.put("held", held);
        result.put("failed", failed);
        result.put("recentActivity", all.stream().limit(10).map(this::toTransferDto).collect(java.util.stream.Collectors.toList()));
        return result;
    }

    public List<Map<String, Object>> getPendingApprovals() {
        return transferRepository.findByPendingApprovalTrueOrderByRiskScoreDesc().stream()
                .map(this::toTransferDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Map<String, Object>> getAllTransfers() {
        return transferRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toTransferDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Map<String, Object>> getAccounts() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Account a : accountRepository.findAll()) {
            Map<String, Object> row = new LinkedHashMap<>();
            User u = a.getUser();
            row.put("accountNumber", a.getAccountNumber());
            row.put("displayName", a.getDisplayName());
            row.put("productKey", a.getProductKey() != null ? a.getProductKey().name() : "CURRENT");
            row.put("currency", a.getCurrency());
            row.put("balance", a.getBalance());
            row.put("availableBalance", a.getAvailableBalance());
            row.put("owner", u.getFirstName() + " " + u.getLastName());
            row.put("ownerAccountNumber", u.getAccountNumber());
            if (a.getProductKey() == ProductKey.CREDIT && a.getCreditLimit() != null) {
                row.put("creditLimit", a.getCreditLimit());
                row.put("amountOwed", a.getAmountOwed());
            }
            rows.add(row);
        }
        return rows;
    }

    @Transactional
    public MessageResponse approve(String reference, String action) {
        if ("approve".equalsIgnoreCase(action)) {
            return transferService.settleHeldTransfer(reference);
        } else if ("reject".equalsIgnoreCase(action)) {
            return transferService.reverseHeldTransfer(reference);
        } else if ("escalate".equalsIgnoreCase(action)) {
            return transferService.escalateHeldTransfer(reference);
        }
        return new MessageResponse(false, "Unknown action: " + action);
    }

    private Map<String, Object> toTransferDto(Transfer t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId() != null ? t.getId().toString() : null);
        m.put("reference", t.getReference());
        m.put("user", t.getUser() != null ? t.getUser().getFirstName() + " " + t.getUser().getLastName() : null);
        m.put("userAccountNumber", t.getUser() != null ? t.getUser().getAccountNumber() : null);
        m.put("type", t.getTransferType() != null ? t.getTransferType().name() : null);
        m.put("amount", t.getAmount());
        m.put("currency", t.getCurrency());
        m.put("status", t.getStatus() != null ? t.getStatus().name() : null);
        m.put("displayStatus", t.getDisplayStatus() != null ? displayStatusLabel(t.getDisplayStatus()) : "Processing");
        m.put("pendingApproval", Boolean.TRUE.equals(t.getPendingApproval()));
        m.put("riskScore", t.getRiskScore());
        m.put("riskLevel", t.getRiskLevel());
        m.put("counterparty", t.getCounterpartyName());
        m.put("source", t.getSourceAccountLabel());
        m.put("createdAt", toIsoTimestamp(t.getCreatedAt()));
        m.put("date", t.getCreatedAt() != null ? DISPLAY_DATE.format(t.getCreatedAt()) : "Today");
        return m;
    }

    private String toIsoTimestamp(LocalDateTime value) {
        if (value == null) {
            return LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toString();
        }
        return value.atZone(ZoneId.systemDefault()).toInstant().toString();
    }

    private String displayStatusLabel(DisplayTransferStatus s) {
        if (s == null) return "Processing";
        if (s == DisplayTransferStatus.Blocked_OFAC_Review) return "Blocked - OFAC Review";
        if (s == DisplayTransferStatus.Awaiting_Treasury_Approval) return "Awaiting Treasury Approval";
        if (s == DisplayTransferStatus.Awaiting_Verification) return "Awaiting Verification";
        if (s == DisplayTransferStatus.Pending_NACHA_Batch) return "Pending NACHA Batch";
        return s.name().replace('_', ' ');
    }
}
