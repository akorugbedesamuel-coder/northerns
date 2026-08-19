package project.northerntrust.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.entity.Notification;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.NotificationSeverity;
import project.northerntrust.app.entity.enums.NotificationType;
import project.northerntrust.app.repository.NotificationRepository;
import project.northerntrust.app.repository.PaymentCardRepository;
import project.northerntrust.app.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class NotificationService {

    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PaymentCardRepository paymentCardRepository;
    @Autowired
    private ObjectMapper objectMapper;

    private String cardLast4(String accountNumber) {
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) return "••••";
        return paymentCardRepository.findByUser(user)
                .map(card -> card.getLastFour() != null ? card.getLastFour() : "••••")
                .orElse("••••");
    }

    @Transactional
    public Notification create(User user, NotificationType type, NotificationSeverity severity,
                               String title, String message, String referenceId, Map<String, String> meta) {
        Notification n = new Notification();
        n.setUser(user);
        n.setNotificationType(type);
        n.setSeverity(severity != null ? severity : NotificationSeverity.INFO);
        n.setTitle(title);
        n.setMessage(message);
        n.setReferenceId(referenceId);
        n.setRead(false);
        if (meta != null && !meta.isEmpty()) {
            try {
                n.setMetadata(objectMapper.writeValueAsString(meta));
            } catch (JsonProcessingException e) {
                n.setMetadata("{}");
            }
        }
        return notificationRepository.save(n);
    }

    public void recordLogin(String accountNumber, String ipAddress, String userAgent) {
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) return;

        String location = resolveLocation(ipAddress);
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("ipAddress", ipAddress != null ? ipAddress : "Unknown");
        meta.put("location", location);
        meta.put("userAgent", userAgent != null ? userAgent : "Unknown browser");
        meta.put("device", summarizeUserAgent(userAgent));

        create(user, NotificationType.LOGIN, NotificationSeverity.INFO,
                "Successful sign-in",
                "Your Northern Trust account was accessed from " + location + ".",
                null, meta);
    }

    public void recordCardFreeze(String accountNumber, boolean frozen) {
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) return;
        String last4 = cardLast4(accountNumber);

        if (frozen) {
            create(user, NotificationType.CARD_LOCK, NotificationSeverity.WARNING,
                    "Debit card temporarily frozen",
                    "ATM and POS transactions on your linked card (•••• " + last4 + ") have been blocked. Online banking remains available.",
                    null, meta("action", "freeze", "cardLast4", last4));
        } else {
            create(user, NotificationType.CARD_UNLOCK, NotificationSeverity.INFO,
                    "Debit card reactivated",
                    "Your card (•••• " + last4 + ") is active again for ATM withdrawals and point-of-sale purchases worldwide.",
                    null, meta("action", "unfreeze", "cardLast4", last4));
        }
    }

    public void recordTransfer(String accountNumber, String reference, BigDecimal amount,
                               String counterparty, String transferKind) {
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) return;

        String amt = amount != null ? amount.toPlainString() : "0";
        Map<String, String> meta = transferMeta(reference, amt, counterparty, transferKind, "SETTLED");

        create(user, NotificationType.TRANSFER, NotificationSeverity.INFO,
                transferKind + " completed",
                "Transfer of $" + amt + (counterparty != null && !counterparty.isEmpty()
                        ? " to " + counterparty : "") + " has been processed.",
                reference, meta);
    }

    public void recordTransferFailed(String accountNumber, String reference, BigDecimal amount,
                                     String counterparty, String reason, String transferKind) {
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) return;

        String amt = amount != null ? amount.toPlainString() : "0";
        Map<String, String> meta = transferMeta(reference, amt, counterparty, transferKind, "FAILED");
        meta.put("reason", reason != null ? reason : "Transfer could not be completed");

        create(user, NotificationType.TRANSFER, NotificationSeverity.WARNING,
                transferKind + " not completed",
                (reason != null ? reason : "Transfer failed") + (reference != null && !reference.isEmpty()
                        ? " (Ref: " + reference + ")" : ""),
                reference, meta);
    }

    public void recordBeneficiaryAdded(String accountNumber, String beneficiaryCode, String displayName,
                                       String beneficiaryType, String status) {
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) return;

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("beneficiaryCode", beneficiaryCode != null ? beneficiaryCode : "");
        meta.put("displayName", displayName != null ? displayName : "");
        meta.put("beneficiaryType", beneficiaryType != null ? beneficiaryType : "");
        meta.put("status", status != null ? status : "ACTIVE");

        NotificationSeverity severity = "PENDING_REVIEW".equalsIgnoreCase(status)
                ? NotificationSeverity.WARNING : NotificationSeverity.INFO;
        String body = displayName + " was registered as a pre-approved payee";
        if ("PENDING_REVIEW".equalsIgnoreCase(status)) {
            body += " and is pending compliance review before first transfer.";
        } else {
            body += " and is available for outbound transfers.";
        }

        create(user, NotificationType.BENEFICIARY, severity,
                "Beneficiary added",
                body,
                beneficiaryCode, meta);
    }

    public void recordBeneficiaryUpdated(String accountNumber, String beneficiaryCode, String displayName,
                                         String updateKind, String detail) {
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) return;

        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("beneficiaryCode", beneficiaryCode != null ? beneficiaryCode : "");
        meta.put("displayName", displayName != null ? displayName : "");
        meta.put("updateKind", updateKind != null ? updateKind : "UPDATE");
        if (detail != null) {
            meta.put("detail", detail);
        }

        create(user, NotificationType.BENEFICIARY, NotificationSeverity.INFO,
                "Beneficiary updated",
                (displayName != null ? displayName + ": " : "") + (detail != null ? detail : updateKind),
                beneficiaryCode, meta);
    }

    private static Map<String, String> transferMeta(String reference, String amt, String counterparty,
                                                    String transferKind, String status) {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("reference", reference != null ? reference : "");
        meta.put("amount", amt);
        meta.put("counterparty", counterparty != null ? counterparty : "");
        meta.put("transferKind", transferKind != null ? transferKind : "Transfer");
        meta.put("status", status);
        return meta;
    }

    public void recordSecurityAlert(String accountNumber, String title, String message) {
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) return;
        create(user, NotificationType.SECURITY, NotificationSeverity.WARNING, title, message, null, null);
    }

    /**
     * Wire initiated but held — AML / risk controls and savings limits.
     */
    public void recordWireComplianceHold(String accountNumber, String reference, BigDecimal amount,
                                         String counterparty, String railLabel, List<String> reasons) {
        User user = userRepository.findByAccountNumber(accountNumber).orElse(null);
        if (user == null) return;

        String amt = amount != null ? amount.toPlainString() : "0";
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("reference", reference != null ? reference : "");
        meta.put("amount", amt);
        meta.put("counterparty", counterparty != null ? counterparty : "");
        meta.put("transferKind", railLabel != null ? railLabel : "Wire");
        meta.put("status", "COMPLIANCE_HOLD");
        if (reasons != null) {
            for (int i = 0; i < reasons.size() && i < 5; i++) {
                meta.put("reason" + (i + 1), reasons.get(i));
            }
        }

        StringBuilder body = new StringBuilder();
        body.append("Your ").append(railLabel != null ? railLabel : "wire")
                .append(" for $").append(amt);
        if (counterparty != null && !counterparty.isEmpty()) {
            body.append(" to ").append(counterparty);
        }
        body.append(" was logged (Ref: ").append(reference).append(") but is on hold. Funds were not released.\n\n");
        body.append("AML / Risk controls: banks may freeze or restrict outgoing transfers when volume spikes suddenly, ");
        body.append("structuring/smurfing patterns are detected, or rapid repetitive transfers occur. ");
        body.append("Large international wires vs. typical monthly activity may require manual review.\n\n");
        body.append("Savings limits: some accounts restrict the number of outgoing withdrawals per month ");
        body.append("(historically Regulation D allowed six per month in the U.S.). Exceeding limits may trigger fees or temporary restrictions.");

        create(user, NotificationType.COMPLIANCE, NotificationSeverity.WARNING,
                "Wire transfer on compliance hold",
                body.toString(),
                reference, meta);
    }

    public Map<String, Object> listForUser(String accountNumber) {
        User user = requireUser(accountNumber);
        ensureSeeded(user);

        List<Notification> all = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        long unread = notificationRepository.countByUserAndIsReadFalse(user);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Notification n : all) {
            items.add(toDto(n));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("unreadCount", unread);
        result.put("totalCount", items.size());
        return result;
    }

    public Map<String, Object> unreadCount(String accountNumber) {
        User user = requireUser(accountNumber);
        ensureSeeded(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("unreadCount", notificationRepository.countByUserAndIsReadFalse(user));
        return result;
    }

    @Transactional
    public MessageResponse markRead(String accountNumber, UUID notificationId) {
        User user = requireUser(accountNumber);
        Notification n = notificationRepository.findByIdAndUser(notificationId, user)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        n.setRead(true);
        notificationRepository.save(n);
        return new MessageResponse(true, "Marked as read");
    }

    @Transactional
    public MessageResponse markAllRead(String accountNumber) {
        User user = requireUser(accountNumber);
        List<Notification> unread = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        for (Notification n : unread) {
            if (!Boolean.TRUE.equals(n.getRead())) {
                n.setRead(true);
            }
        }
        notificationRepository.saveAll(unread);
        return new MessageResponse(true, "All notifications marked as read");
    }

    @Transactional
    public void ensureSeeded(User user) {
        if (notificationRepository.findByUserOrderByCreatedAtDesc(user).isEmpty()) {
            seedDemoNotifications(user);
        }
    }

    @Transactional
    public void seedDemoNotifications(User user) {
        Map<String, String> loginMeta = new LinkedHashMap<>();
        loginMeta.put("ipAddress", "73.42.118.204");
        loginMeta.put("location", "Chicago, IL, United States");
        loginMeta.put("device", "Chrome on Windows");
        loginMeta.put("userAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/122");

        create(user, NotificationType.LOGIN, NotificationSeverity.INFO,
                "Successful sign-in",
                "Your account was accessed from Chicago, IL.",
                null, loginMeta);

        create(user, NotificationType.SECURITY, NotificationSeverity.WARNING,
                "New device recognized",
                "A sign-in attempt was observed from Portland, OR. If this was not you, contact your relationship manager immediately.",
                null, meta("ipAddress", "198.51.100.42", "location", "Portland, OR, United States"));

        Map<String, String> wireMeta = new LinkedHashMap<>();
        wireMeta.put("reference", "TRX-W892X1");
        wireMeta.put("amount", "50000.00");
        wireMeta.put("counterparty", "Apex Global Holdings");
        create(user, NotificationType.TRANSACTION, NotificationSeverity.INFO,
                "Wire transfer posted",
                "Outbound wire of $50,000.00 to Apex Global Holdings has been released for settlement.",
                "TRX-W892X1", wireMeta);

        Map<String, String> pendMeta = new LinkedHashMap<>();
        pendMeta.put("reference", "TRX-PEND-441");
        pendMeta.put("amount", "50000.00");
        create(user, NotificationType.COMPLIANCE, NotificationSeverity.CRITICAL,
                "Transfer requires authorization",
                "A pending outbound transfer exceeded your daily policy limit and is awaiting compliance approval.",
                "TRX-PEND-441", pendMeta);

        create(user, NotificationType.CARD_LOCK, NotificationSeverity.WARNING,
                "Debit card temporarily frozen",
                "Card ending 4920 was frozen from the mobile banking channel.",
                null, meta("cardLast4", "4920", "action", "freeze"));

        create(user, NotificationType.SYSTEM, NotificationSeverity.INFO,
                "Statement available",
                "Your April 2026 consolidated account statement is ready to download in Forms & Statements.",
                null, meta("period", "April 2026"));
    }

    private static Map<String, String> meta(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private User requireUser(String accountNumber) {
        return userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + accountNumber));
    }

    private Map<String, Object> toDto(Notification n) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", n.getId().toString());
        dto.put("type", n.getNotificationType().name());
        dto.put("severity", n.getSeverity().name());
        dto.put("title", n.getTitle());
        dto.put("message", n.getMessage());
        dto.put("summary", truncate(n.getMessage(), 120));
        dto.put("referenceId", n.getReferenceId());
        dto.put("read", Boolean.TRUE.equals(n.getRead()));
        dto.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
        dto.put("displayTime", n.getCreatedAt() != null ? DISPLAY_FMT.format(n.getCreatedAt()) : "");
        dto.put("metadata", parseMetadata(n.getMetadata()));
        return dto;
    }

    private Map<String, String> parseMetadata(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }

    private String resolveLocation(String ip) {
        if (ip == null || ip.isBlank() || "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "Chicago, IL, United States (local)";
        }
        return "Chicago, IL, United States";
    }

    private String summarizeUserAgent(String ua) {
        if (ua == null || ua.isBlank()) return "Unknown device";
        if (ua.contains("Chrome")) return "Chrome browser";
        if (ua.contains("Firefox")) return "Firefox browser";
        if (ua.contains("Safari")) return "Safari browser";
        return "Web browser";
    }
}
