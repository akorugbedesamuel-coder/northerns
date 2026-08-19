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
        return ResponseEntity.ok(performExternal(accountNumber, body, "ACH"));
    }

    @PostMapping("/wire")
    public ResponseEntity<MessageResponse> wire(
            @RequestParam(defaultValue = "8902410001") String accountNumber,
            @RequestBody Map<String, Object> body) {
        String network = body.getOrDefault("network", "domestic").toString();
        String type = "swift".equalsIgnoreCase(network) ? "SWIFT" : "WIRE";
        return ResponseEntity.ok(performExternal(accountNumber, body, type));
    }

    @PostMapping("/international")
    public ResponseEntity<MessageResponse> international(
            @RequestParam(defaultValue = "8902410001") String accountNumber,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(performExternal(accountNumber, body, "SWIFT"));
    }

    private MessageResponse performExternal(String accountNumber, Map<String, Object> body, String transferType) {
        User user = dashboardService.requireUser(accountNumber);
        String sourceKey = body.getOrDefault("sourceAccountKey", "checking").toString();
        Account from = dashboardService.resolveSourceAccount(user, sourceKey)
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));

        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String beneficiaryId = body.containsKey("beneficiaryId") ? body.get("beneficiaryId").toString() : null;

        ExternalTransferRequest req = new ExternalTransferRequest();
        req.setFromAccountNumber(from.getAccountNumber());
        req.setTransferType(transferType);
        req.setAmount(amount);
        req.setDescription(body.getOrDefault("memo", body.getOrDefault("purpose", "Transfer")).toString());

        if (beneficiaryId != null) {
            Beneficiary ben = beneficiaryRepository.findByBeneficiaryCodeAndUser(beneficiaryId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));
            req.setBankName(ben.getBankName());
            req.setRecipientAccount(ben.getAccountNumber());
            req.setRecipientName(ben.getDisplayName());
            req.setSwiftCode(ben.getRoutingOrSwift());
            ben.setLastUsedAt(java.time.LocalDateTime.now());
            beneficiaryRepository.save(ben);
        } else {
            req.setBankName(body.getOrDefault("bankName", "External Bank").toString());
            req.setRecipientAccount(body.getOrDefault("accountNumber", "").toString());
            req.setRecipientName(body.getOrDefault("recipientName", "Recipient").toString());
            req.setSwiftCode(body.getOrDefault("swiftBic", "").toString());
            req.setIban(body.getOrDefault("iban", "").toString());
            req.setRoutingNumber(body.getOrDefault("routingNumber", "").toString());
        }

        MessageResponse response = transferService.performExternalTransfer(req);

        if (response.isSuccess() || response.isHeld()) {
            String ref = response.getReference();
            if (ref == null) {
                ref = extractReference(response.getMessage());
            }
            String finalRef = ref;
            if (finalRef != null && !finalRef.isEmpty()) {
                transferRepository.findByReference(finalRef).ifPresent(t -> {
                    t.setCounterpartyName(req.getRecipientName());
                    t.setSourceAccountLabel(from.getDisplayName());
                    transferRepository.save(t);
                });
            }
        }
        return response;
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
