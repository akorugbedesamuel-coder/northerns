package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.northerntrust.app.dto.BeneficiaryRequest;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.entity.Beneficiary;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.*;
import project.northerntrust.app.repository.BeneficiaryRepository;
import project.northerntrust.app.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class BeneficiaryService {

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    public MessageResponse saveBeneficiary(BeneficiaryRequest request) {
        Optional<User> userOpt = userRepository.findByAccountNumber(request.getUserAccountNumber());
        if (userOpt.isEmpty()) {
            return new MessageResponse(false, "User not found.");
        }

        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setUser(userOpt.get());
        beneficiary.setAccountNumber(request.getAccountNumber());
        beneficiary.setBankName(request.getBankName());
        beneficiary.setAccountName(request.getAccountName());
        beneficiary.setNickname(request.getNickname());

        beneficiary = beneficiaryRepository.save(beneficiary);

        User user = userOpt.get();
        auditLogService.record(user, "BENEFICIARY_CREATED", "BENEFICIARY", beneficiary.getId(),
                auditLogService.detailsOf(
                        "beneficiaryCode", beneficiary.getBeneficiaryCode(),
                        "displayName", beneficiary.getDisplayName(),
                        "bankName", beneficiary.getBankName()));
        notificationService.recordBeneficiaryAdded(
                user.getAccountNumber(),
                beneficiary.getBeneficiaryCode(),
                beneficiary.getDisplayName(),
                beneficiary.getBeneficiaryType() != null ? beneficiary.getBeneficiaryType().name() : "BANK",
                beneficiary.getStatus() != null ? beneficiary.getStatus().name() : "ACTIVE");

        return new MessageResponse(true, "Beneficiary saved successfully.");
    }

    public List<Beneficiary> getUserBeneficiaries(String userAccountNumber) {
        Optional<User> userOpt = userRepository.findByAccountNumber(userAccountNumber);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }

        return beneficiaryRepository.findByUser(userOpt.get());
    }

    public List<Map<String, Object>> getBeneficiariesV1(String accountNumber, String type, String search) {
        return dashboardService.getBeneficiaries(accountNumber, type, search);
    }

    @Transactional
    public MessageResponse createBeneficiaryV1(String accountNumber, Map<String, Object> body) {
        User user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (body.get("type") == null || body.get("displayName") == null) {
            return new MessageResponse(false, "Beneficiary type and display name are required.");
        }

        Beneficiary b = new Beneficiary();
        b.setUser(user);
        b.setBeneficiaryCode("BEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        BeneficiaryType type = BeneficiaryType.valueOf(body.get("type").toString().toUpperCase());
        b.setBeneficiaryType(type);
        b.setDisplayName(body.get("displayName").toString().trim());
        if (body.containsKey("relationship")) {
            b.setRelationship(body.get("relationship").toString().trim());
        }

        switch (type) {
            case BANK:
                if (body.containsKey("bankName")) {
                    b.setBankName(body.get("bankName").toString().trim());
                }
                if (body.containsKey("accountNumber")) {
                    String acct = body.get("accountNumber").toString().trim();
                    b.setAccountNumber(acct);
                    b.setAccountName(body.containsKey("accountName")
                            ? body.get("accountName").toString().trim() : b.getDisplayName());
                }
                if (body.containsKey("routingOrSwift")) {
                    b.setRoutingOrSwift(body.get("routingOrSwift").toString().trim());
                }
                if (body.containsKey("bankAddress")) {
                    b.setBankAddress(body.get("bankAddress").toString().trim());
                }
                if (body.containsKey("country")) {
                    b.setCountry(body.get("country").toString().trim());
                }
                break;
            case INTERNAL:
                if (body.containsKey("userId")) {
                    b.setDestinationUserId(body.get("userId").toString().trim());
                } else if (body.containsKey("destinationUserId")) {
                    b.setDestinationUserId(body.get("destinationUserId").toString().trim());
                }
                if (body.containsKey("emailOrPhone")) {
                    b.setEmailOrPhone(body.get("emailOrPhone").toString().trim());
                }
                break;
            case CREDIT:
                if (body.containsKey("creditAccountId")) {
                    b.setCreditAccountId(body.get("creditAccountId").toString().trim());
                }
                break;
            case INVESTMENT:
                if (body.containsKey("portfolioId")) {
                    b.setPortfolioId(body.get("portfolioId").toString().trim());
                }
                break;
            default:
                break;
        }

        b.setSingleLimit(new BigDecimal(body.getOrDefault("singleLimit", "100000").toString()));
        b.setDailyLimit(new BigDecimal(body.getOrDefault("dailyLimit", "250000").toString()));

        String trust = body.containsKey("trustLevel") ? body.get("trustLevel").toString()
                : body.getOrDefault("trust", "New").toString();
        if ("Trusted".equalsIgnoreCase(trust)) {
            b.setTrustLevel(TrustLevel.Trusted);
            b.setTrusted(true);
            b.setStatus(BeneficiaryStatus.ACTIVE);
        } else if ("Blocked".equalsIgnoreCase(trust)) {
            b.setTrustLevel(TrustLevel.Blocked);
            b.setStatus(BeneficiaryStatus.BLOCKED);
            b.setTrusted(false);
        } else if ("Verified".equalsIgnoreCase(trust)) {
            b.setTrustLevel(TrustLevel.Verified);
            b.setTrusted(false);
            b.setStatus(BeneficiaryStatus.ACTIVE);
        } else {
            b.setTrustLevel(TrustLevel.New);
            b.setTrusted(false);
            b.setStatus(BeneficiaryStatus.PENDING_REVIEW);
        }

        b = beneficiaryRepository.save(b);

        auditLogService.record(user, "BENEFICIARY_CREATED", "BENEFICIARY", b.getId(),
                auditLogService.detailsOf(
                        "beneficiaryCode", b.getBeneficiaryCode(),
                        "displayName", b.getDisplayName(),
                        "type", b.getBeneficiaryType().name(),
                        "status", b.getStatus().name(),
                        "trustLevel", b.getTrustLevel().name()));
        notificationService.recordBeneficiaryAdded(
                accountNumber,
                b.getBeneficiaryCode(),
                b.getDisplayName(),
                b.getBeneficiaryType().name(),
                b.getStatus().name());

        MessageResponse response = new MessageResponse(true, "Beneficiary saved successfully.");
        response.setReference(b.getBeneficiaryCode());
        return response;
    }

    @Transactional
    public MessageResponse updateLimits(String accountNumber, String beneficiaryCode, BigDecimal single, BigDecimal daily) {
        User user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Beneficiary b = beneficiaryRepository.findByBeneficiaryCodeAndUser(beneficiaryCode, user)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));
        b.setSingleLimit(single);
        b.setDailyLimit(daily);
        beneficiaryRepository.save(b);
        String detail = "Single limit $" + single.toPlainString() + ", daily limit $" + daily.toPlainString();
        auditLogService.record(user, "BENEFICIARY_LIMITS_UPDATED", "BENEFICIARY", b.getId(),
                auditLogService.detailsOf("beneficiaryCode", beneficiaryCode, "singleLimit", single, "dailyLimit", daily));
        notificationService.recordBeneficiaryUpdated(accountNumber, beneficiaryCode, b.getDisplayName(),
                "LIMITS", detail);
        return new MessageResponse(true, "Limits updated");
    }

    @Transactional
    public MessageResponse updateTrust(String accountNumber, String beneficiaryCode, boolean trusted) {
        User user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Beneficiary b = beneficiaryRepository.findByBeneficiaryCodeAndUser(beneficiaryCode, user)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));
        b.setTrusted(trusted);
        b.setTrustLevel(trusted ? TrustLevel.Trusted : TrustLevel.Verified);
        beneficiaryRepository.save(b);
        String detail = trusted ? "Marked as Trusted" : "Set to Verified (MFA required above policy limits)";
        auditLogService.record(user, "BENEFICIARY_TRUST_UPDATED", "BENEFICIARY", b.getId(),
                auditLogService.detailsOf("beneficiaryCode", beneficiaryCode, "trusted", trusted));
        notificationService.recordBeneficiaryUpdated(accountNumber, beneficiaryCode, b.getDisplayName(),
                "TRUST", detail);
        return new MessageResponse(true, "Trust updated");
    }

    @Transactional
    public MessageResponse updateStatus(String accountNumber, String beneficiaryCode, BeneficiaryStatus status) {
        User user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Beneficiary b = beneficiaryRepository.findByBeneficiaryCodeAndUser(beneficiaryCode, user)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));
        b.setStatus(status);
        if (status == BeneficiaryStatus.BLOCKED) {
            b.setTrustLevel(TrustLevel.Blocked);
        }
        beneficiaryRepository.save(b);
        String detail = "Status changed to " + status.name();
        auditLogService.record(user, "BENEFICIARY_STATUS_UPDATED", "BENEFICIARY", b.getId(),
                auditLogService.detailsOf("beneficiaryCode", beneficiaryCode, "status", status.name()));
        notificationService.recordBeneficiaryUpdated(accountNumber, beneficiaryCode, b.getDisplayName(),
                "STATUS", detail);
        return new MessageResponse(true, "Status updated");
    }
}
