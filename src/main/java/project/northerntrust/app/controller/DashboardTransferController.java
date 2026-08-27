package project.northerntrust.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.northerntrust.app.dto.*;
import project.northerntrust.app.entity.Account;
import project.northerntrust.app.entity.Beneficiary;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.*;
import project.northerntrust.app.repository.BeneficiaryRepository;
import project.northerntrust.app.repository.TransferRepository;
import project.northerntrust.app.service.DashboardService;
import project.northerntrust.app.service.TransferService;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transfers")
@CrossOrigin(origins = "*")
public class DashboardTransferController {

    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private TransferService transferService;
    @Autowired
    private BeneficiaryRepository beneficiaryRepository;
    @Autowired
    private TransferRepository transferRepository;

    @PostMapping("/internal")
    public ResponseEntity<MessageResponse> internal(
            @RequestParam(defaultValue = "8902410001") String accountNumber,
            @RequestBody Map<String, Object> body) {
        User user = dashboardService.requireUser(accountNumber);
        String fromKey = (String) body.get("fromAccountKey");
        String toKey = (String) body.get("toAccountKey");
        BigDecimal amount = new BigDecimal(body.get("amount").toString());

        Account from = dashboardService.resolveSourceAccount(user, fromKey)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
        Account to = dashboardService.resolveSourceAccount(user, toKey)
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));

        TransferRequest req = new TransferRequest();
        req.setFromAccountNumber(from.getAccountNumber());
        req.setToAccountNumber(to.getAccountNumber());
        req.setAmount(amount);
        req.setDescription(body.getOrDefault("memo", "Internal transfer").toString());
        return ResponseEntity.ok(transferService.performInternalTransfer(req));
    }

    @PostMapping("/ach")
    public ResponseEntity<MessageResponse> ach(
            @RequestParam(defaultValue = "8902410001") String accountNumber,
            @RequestBody Map<String, Object> body) {
        User user = dashboardService.requireUser(accountNumber);
        Account from = resolveSource(user, body);
        ExternalTransferRequest req = buildRequest(user, from, body, "ACH");

        Beneficiary ben = null;
        String beneficiaryId = body.containsKey("beneficiaryId") ? body.get("beneficiaryId").toString() : null;
        if (beneficiaryId != null) {
            ben = beneficiaryRepository.findByBeneficiaryCodeAndUser(beneficiaryId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));
        }

        MessageResponse response = transferService.performAchTransfer(req, ben);
        return enrichAndReturn(req, response, from.getDisplayName());
    }

    @PostMapping("/wire")
    public ResponseEntity<MessageResponse> wire(
            @RequestParam(defaultValue = "8902410001") String accountNumber,
            @RequestBody Map<String, Object> body) {
        User user = dashboardService.requireUser(accountNumber);
        Account from = resolveSource(user, body);
        String network = body.getOrDefault("network", "domestic").toString();
        String type = "swift".equalsIgnoreCase(network) ? "SWIFT" : "WIRE";
        ExternalTransferRequest req = buildRequest(user, from, body, type);
        MessageResponse response = transferService.performExternalTransfer(req);
        return enrichAndReturn(req, response, from.getDisplayName());
    }

    @PostMapping("/international")
    public ResponseEntity<MessageResponse> international(
            @RequestParam(defaultValue = "8902410001") String accountNumber,
            @RequestBody Map<String, Object> body) {
        User user = dashboardService.requireUser(accountNumber);
        Account from = resolveSource(user, body);
        ExternalTransferRequest req = buildRequest(user, from, body, "SWIFT");
        MessageResponse response = transferService.performExternalTransfer(req);
        return enrichAndReturn(req, response, from.getDisplayName());
    }

    private Account resolveSource(User user, Map<String, Object> body) {
        String sourceKey = body.getOrDefault("sourceAccountKey", "checking").toString();
        return dashboardService.resolveSourceAccount(user, sourceKey)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
    }

    private ExternalTransferRequest buildRequest(User user, Account from, Map<String, Object> body, String transferType) {
        ExternalTransferRequest req = new ExternalTransferRequest();
        req.setFromAccountNumber(from.getAccountNumber());
        req.setTransferType(transferType);
        req.setAmount(new BigDecimal(body.get("amount").toString()));
        req.setDescription(body.getOrDefault("memo", body.getOrDefault("purpose", "Transfer")).toString());

        req.setDirection(body.getOrDefault("direction", "CREDIT").toString());
        req.setEffectiveDate(body.getOrDefault("effectiveDate", "").toString());
        req.setSecCode(body.getOrDefault("secCode", "").toString());
        req.setScheduling(body.getOrDefault("scheduling", "once").toString());
        req.setSameDay(Boolean.parseBoolean(String.valueOf(body.getOrDefault("sameDay", "false"))));
        req.setAchAuthAck(Boolean.parseBoolean(String.valueOf(body.getOrDefault("achAuthAck", "false"))));
        req.setPriority(body.getOrDefault("priority", "Standard").toString());
        req.setCurrency(body.getOrDefault("currency", "USD").toString());
        try {
            if (body.get("fxRate") != null && !body.get("fxRate").toString().isBlank()) {
                req.setFxRate(new BigDecimal(body.get("fxRate").toString()));
            }
            if (body.get("usdEquivalent") != null && !body.get("usdEquivalent").toString().isBlank()) {
                req.setUsdEquivalent(new BigDecimal(body.get("usdEquivalent").toString()));
            }
        } catch (NumberFormatException ignored) {
        }
        if (body.get("fee") != null) {
            try {
                req.setFee(new BigDecimal(body.get("fee").toString()));
            } catch (NumberFormatException ignored) {
                req.setFee(null);
            }
        }

        String beneficiaryId = body.containsKey("beneficiaryId") ? body.get("beneficiaryId").toString() : null;
        if (beneficiaryId != null) {
            Beneficiary ben = beneficiaryRepository.findByBeneficiaryCodeAndUser(beneficiaryId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));
            req.setBankName(firstNonBlank(body.containsKey("bankName") ? body.get("bankName").toString() : "", ben.getBankName()));
            req.setRecipientAccount(firstNonBlank(body.containsKey("accountNumber") ? body.get("accountNumber").toString() : "", ben.getAccountNumber()));
            req.setRecipientName(firstNonBlank(body.containsKey("recipientName") ? body.get("recipientName").toString() : "", ben.getDisplayName()));
            String bodySwift = body.containsKey("swiftBic") ? body.get("swiftBic").toString() : "";
            String bodyRouting = body.containsKey("routingNumber") ? body.get("routingNumber").toString() : "";
            req.setSwiftCode(firstNonBlank(bodySwift, ben.getRoutingOrSwift()));
            req.setRoutingNumber(firstNonBlank(bodyRouting, ben.getRoutingOrSwift()));
            req.setRecipientAddress(firstNonBlank(body.containsKey("bankAddress") ? body.get("bankAddress").toString() : "", ben.getBankAddress()));
            req.setCountry(ben.getCountry());
            ben.setLastUsedAt(java.time.LocalDateTime.now());
            beneficiaryRepository.save(ben);
        } else {
            req.setBankName(body.getOrDefault("bankName", "External Bank").toString());
            req.setRecipientAccount(body.getOrDefault("accountNumber", "").toString());
            req.setRecipientName(body.getOrDefault("recipientName", "Recipient").toString());
            req.setSwiftCode(body.getOrDefault("swiftBic", "").toString());
            req.setIban(body.getOrDefault("iban", "").toString());
            req.setRoutingNumber(body.getOrDefault("routingNumber", "").toString());
            req.setRecipientAddress(body.getOrDefault("bankAddress", "").toString());
            req.setCountry(body.getOrDefault("country", "").toString());
        }
        return req;
    }

    private ResponseEntity<MessageResponse> enrichAndReturn(ExternalTransferRequest req, MessageResponse response, String sourceLabel) {
        if (response.isSuccess() || response.isHeld()) {
            String ref = response.getReference();
            if (ref == null) {
                ref = extractReference(response.getMessage());
            }
            String finalRef = ref;
            if (finalRef != null && !finalRef.isEmpty()) {
                transferRepository.findByReference(finalRef).ifPresent(t -> {
                    t.setCounterpartyName(req.getRecipientName());
                    t.setSourceAccountLabel(sourceLabel);
                    transferRepository.save(t);
                });
            }
        }
        return ResponseEntity.ok(response);
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.trim().isEmpty()) {
            return primary.trim();
        }
        return fallback;
    }

    private String extractReference(String message) {
        if (message == null) return "";
        int idx = message.indexOf("Reference: ");
        if (idx >= 0) {
            String ref = message.substring(idx + 11).trim();
            int dot = ref.indexOf('.');
            return dot > 0 ? ref.substring(0, dot) : ref;
        }
        return "";
    }
}
