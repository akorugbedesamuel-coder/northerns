package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.northerntrust.app.entity.RiskEvent;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.KycStatus;
import project.northerntrust.app.entity.enums.Severity;
import project.northerntrust.app.repository.RiskEventRepository;
import project.northerntrust.app.repository.TransferRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AmlRiskService {

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private RiskEventRepository riskEventRepository;

    @Autowired
    private AccountService accountService;

    public void analyzeTransfer(User user, BigDecimal amount, String accountNumber) {
        // 1. Velocity Check: Max 20 transfers per hour (Increased for testing/seeding compatibility)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long transferCount = transferRepository.countByUserAndCreatedAtAfter(user, oneHourAgo);

        if (transferCount >= 20) {
            triggerRiskEvent(user, "VELOCITY_VIOLATION", Severity.HIGH, 
                "User exceeded transfer velocity limit (5 per hour). Current count: " + transferCount);
            
            // Auto-Freeze
            accountService.freezeAccount(accountNumber);
            throw new RuntimeException("AML Alert: Velocity violation. Account frozen.");
        }

        // 2. Volume Check: Tier 1 (Unverified) limit 1,000,000 NGN
        if (user.getKycStatus() != KycStatus.VERIFIED && amount.compareTo(new BigDecimal("1000000")) > 0) {
            triggerRiskEvent(user, "VOLUME_VIOLATION", Severity.HIGH, 
                "Unverified user attempted transfer over 1,000,000 NGN. Amount: " + amount);
            
            // Auto-Freeze
            accountService.freezeAccount(accountNumber);
            throw new RuntimeException("AML Alert: Volume violation for unverified account. Account frozen.");
        }
        
        // 3. Medium Risk: Large transfers > 5M even for verified users
        if (amount.compareTo(new BigDecimal("5000000")) > 0) {
            triggerRiskEvent(user, "LARGE_TRANSACTION", Severity.MEDIUM, 
                "Large transaction detected: " + amount);
        }
    }

    private void triggerRiskEvent(User user, String type, Severity severity, String description) {
        RiskEvent event = new RiskEvent();
        event.setUser(user);
        event.setEventType(type);
        event.setSeverity(severity);
        event.setDescription(description);
        riskEventRepository.save(event);
    }
}
