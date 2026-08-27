package project.northerntrust.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.service.BeneficiaryService;
import project.northerntrust.app.service.DashboardService;
import project.northerntrust.app.service.OtpService;
import project.northerntrust.app.entity.enums.BeneficiaryStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private OtpService otpService;
    @Autowired
    private BeneficiaryService beneficiaryService;

    @GetMapping("/dashboard/overview")
    public ResponseEntity<Map<String, Object>> overview(
            @RequestParam(defaultValue = "2214578903") String accountNumber) {
        return ResponseEntity.ok(dashboardService.getOverview(accountNumber));
    }

    @GetMapping("/client/profile")
    public ResponseEntity<Map<String, Object>> clientProfile(
            @RequestParam(defaultValue = "2214578903") String accountNumber) {
        return ResponseEntity.ok(dashboardService.getClientProfile(accountNumber));
    }

    @GetMapping("/accounts/balances")
    public ResponseEntity<Map<String, Object>> allBalances(
            @RequestParam(defaultValue = "2214578903") String accountNumber) {
        return ResponseEntity.ok(dashboardService.getAccountBalances(accountNumber));
    }

    @PostMapping("/beneficiaries")
    public ResponseEntity<MessageResponse> createBeneficiary(
            @RequestParam(defaultValue = "2214578903") String accountNumber,
            @RequestBody Map<String, Object> body) {
        MessageResponse response = beneficiaryService.createBeneficiaryV1(accountNumber, body);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PatchMapping("/beneficiaries/{code}/limits")
    public ResponseEntity<MessageResponse> updateBeneficiaryLimits(
            @PathVariable String code,
            @RequestParam(defaultValue = "2214578903") String accountNumber,
            @RequestParam BigDecimal single,
            @RequestParam BigDecimal daily) {
        return ResponseEntity.ok(beneficiaryService.updateLimits(accountNumber, code, single, daily));
    }

    @PatchMapping("/beneficiaries/{code}/trust")
    public ResponseEntity<MessageResponse> updateBeneficiaryTrust(
            @PathVariable String code,
            @RequestParam(defaultValue = "2214578903") String accountNumber,
            @RequestParam boolean trusted) {
        return ResponseEntity.ok(beneficiaryService.updateTrust(accountNumber, code, trusted));
    }

    @PatchMapping("/beneficiaries/{code}/status")
    public ResponseEntity<MessageResponse> updateBeneficiaryStatus(
            @PathVariable String code,
            @RequestParam(defaultValue = "2214578903") String accountNumber,
            @RequestParam String status) {
        return ResponseEntity.ok(beneficiaryService.updateStatus(accountNumber, code, BeneficiaryStatus.valueOf(status)));
    }

    @GetMapping("/beneficiaries")
    public ResponseEntity<List<Map<String, Object>>> beneficiaries(
            @RequestParam(defaultValue = "2214578903") String accountNumber,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(dashboardService.getBeneficiaries(accountNumber, type, search));
    }

    @GetMapping("/transfers/history")
    public ResponseEntity<Map<String, Object>> transferHistory(
            @RequestParam(defaultValue = "2214578903") String accountNumber,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String period,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "8") int size) {
        return ResponseEntity.ok(dashboardService.getTransferHistory(accountNumber, search, type, status, period, page, size));
    }

    @GetMapping("/statements")
    public ResponseEntity<List<Map<String, Object>>> statements(
            @RequestParam(defaultValue = "2214578903") String accountNumber,
            @RequestParam(defaultValue = "unified") String tab,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(dashboardService.getStatements(accountNumber, tab, period, type, status, search));
    }

    @GetMapping("/approvals/pending")
    public ResponseEntity<List<Map<String, Object>>> pendingApprovals(
            @RequestParam(defaultValue = "2214578903") String accountNumber) {
        return ResponseEntity.ok(dashboardService.getPendingApprovals(accountNumber));
    }

    @PostMapping("/approvals/{reference}/{action}")
    public ResponseEntity<MessageResponse> approvalAction(
            @PathVariable String reference,
            @PathVariable String action,
            @RequestParam(defaultValue = "2214578903") String accountNumber) {
        return ResponseEntity.ok(dashboardService.approveTransfer(accountNumber, reference, action));
    }

    @GetMapping("/analytics/dashboard")
    public ResponseEntity<Map<String, Object>> analytics(
            @RequestParam(defaultValue = "2214578903") String accountNumber,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) String accountScope,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String txType) {
        return ResponseEntity.ok(dashboardService.getAnalytics(
                accountNumber, timeRange, accountScope, currency, txType));
    }

    @GetMapping("/fx/rates")
    public ResponseEntity<Map<String, Object>> fxRates() {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("base", "USD");
        body.put("rates", dashboardService.getFxRates());
        return ResponseEntity.ok(body);
    }

    @PatchMapping("/cards/freeze")
    public ResponseEntity<MessageResponse> freezeCard(
            @RequestParam(defaultValue = "2214578903") String accountNumber,
            @RequestParam boolean frozen) {
        return ResponseEntity.ok(dashboardService.freezeCard(accountNumber, frozen));
    }

    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, Object>> requestOtp(
            @RequestParam(defaultValue = "2214578903") String accountNumber,
            @RequestParam(defaultValue = "TRANSFER") String purpose) {
        return ResponseEntity.ok(otpService.requestOtp(accountNumber, purpose));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @RequestParam(defaultValue = "2214578903") String accountNumber,
            @RequestParam String code,
            @RequestParam(defaultValue = "TRANSFER") String purpose) {
        project.northerntrust.app.entity.enums.OtpPurpose otpPurpose;
        try {
            otpPurpose = project.northerntrust.app.entity.enums.OtpPurpose.valueOf(purpose.trim().toUpperCase());
        } catch (Exception e) {
            otpPurpose = project.northerntrust.app.entity.enums.OtpPurpose.TRANSFER;
        }
        return ResponseEntity.ok(otpService.verifyOtp(accountNumber, code, otpPurpose));
    }

    @GetMapping("/accounts/balance")
    public ResponseEntity<Map<String, Object>> accountBalance(
            @RequestParam(defaultValue = "2214578903") String accountNumber,
            @RequestParam(defaultValue = "checking") String productKey) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("productKey", productKey);
        body.put("availableBalance", dashboardService.getAccountBalance(accountNumber, productKey));
        return ResponseEntity.ok(body);
    }
}
