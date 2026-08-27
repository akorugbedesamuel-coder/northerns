package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import project.northerntrust.app.entity.Account;
import project.northerntrust.app.entity.PaymentRail;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.AccountType;
import project.northerntrust.app.entity.enums.PaymentRailType;
import project.northerntrust.app.repository.AccountRepository;
import project.northerntrust.app.repository.PaymentRailRepository;
import project.northerntrust.app.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class SystemDataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentRailRepository paymentRailRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        seedSystemAccounts();
        seedAdminUser();
        seedPaymentRails();
    }

    private void seedAdminUser() {
        if (userRepository.existsByAccountNumber("100000")) {
            return;
        }
        User admin = new User();
        admin.setAccountNumber("100000");
        admin.setPassword(passwordEncoder.encode("NT@Admin2026!"));
        admin.setFirstName("Bank");
        admin.setLastName("Administrator");
        admin.setEmail("admin@northerntrust.com");
        admin.setPhoneNumber("+1 (312) 555-0100");
        admin.setAdminRole(project.northerntrust.app.entity.enums.AdminRole.ADMIN);
        admin.setKycStatus(project.northerntrust.app.entity.enums.KycStatus.VERIFIED);
        userRepository.save(admin);
    }

    private void seedSystemAccounts() {
        // Create a root System user to own these accounts if not exists
        Optional<User> sysUserOpt = userRepository.findByEmail("system@northerntrust.com");
        User sysUser;
        if (sysUserOpt.isEmpty()) {
            sysUser = new User();
            sysUser.setEmail("system@northerntrust.com");
            sysUser.setPassword(passwordEncoder.encode("SecureSystemPass123!"));
            sysUser.setFirstName("System");
            sysUser.setLastName("Bank");
            sysUser.setPhoneNumber("+00000000000");
            sysUser.setAccountNumber("0000000000"); // Master System ID
            sysUser = userRepository.save(sysUser);
        } else {
            sysUser = sysUserOpt.get();
        }

        // Seed Fee Revenue Account
        if (accountRepository.findByAccountNumber("FEE_REVENUE_01").isEmpty()) {
            Account feeAccount = new Account();
            feeAccount.setUser(sysUser);
            feeAccount.setAccountNumber("FEE_REVENUE_01");
            feeAccount.setAccountType(AccountType.CURRENT);
            feeAccount.setCurrency("USD");
            accountRepository.save(feeAccount);
        }

        // Seed Outbound Suspense Account
        if (accountRepository.findByAccountNumber("OUTBOUND_SUSPENSE_01").isEmpty()) {
            Account suspenseAccount = new Account();
            suspenseAccount.setUser(sysUser);
            suspenseAccount.setAccountNumber("OUTBOUND_SUSPENSE_01");
            suspenseAccount.setAccountType(AccountType.CURRENT);
            suspenseAccount.setCurrency("USD");
            accountRepository.save(suspenseAccount);
        }
    }

    private void seedPaymentRails() {
        // ACH
        if (paymentRailRepository.findByRailType(PaymentRailType.ACH).isEmpty()) {
            PaymentRail ach = new PaymentRail();
            ach.setRailType(PaymentRailType.ACH);
            ach.setProcessingTime("1-3 Business Days");
            ach.setFeeFixed(BigDecimal.ZERO);
            ach.setFeePercentage(BigDecimal.ZERO);
            ach.setMaxLimit(new BigDecimal("5000000"));
            ach.setSupportedCurrencies("USD,EUR,GBP");
            paymentRailRepository.save(ach);
        }

        // SWIFT
        if (paymentRailRepository.findByRailType(PaymentRailType.SWIFT).isEmpty()) {
            PaymentRail swift = new PaymentRail();
            swift.setRailType(PaymentRailType.SWIFT);
            swift.setProcessingTime("2-5 Business Days");
            swift.setFeeFixed(new BigDecimal("35.00"));
            swift.setFeePercentage(new BigDecimal("0.005"));
            swift.setMaxLimit(new BigDecimal("100000000"));
            swift.setSupportedCurrencies("USD,EUR,GBP");
            paymentRailRepository.save(swift);
        }

        // WIRE
        if (paymentRailRepository.findByRailType(PaymentRailType.WIRE).isEmpty()) {
            PaymentRail wire = new PaymentRail();
            wire.setRailType(PaymentRailType.WIRE);
            wire.setProcessingTime("Same Day");
            wire.setFeeFixed(new BigDecimal("25.00"));
            wire.setFeePercentage(BigDecimal.ZERO);
            wire.setMaxLimit(new BigDecimal("20000000"));
            wire.setSupportedCurrencies("USD,EUR,GBP");
            paymentRailRepository.save(wire);
        }
    }
}
