package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import project.northerntrust.app.dto.LoginRequest;
import project.northerntrust.app.dto.LoginResponse;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.dto.RegisterRequest;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.OtpPurpose;
import project.northerntrust.app.repository.UserRepository;
import project.northerntrust.app.repository.AccountRepository;

import java.time.LocalDateTime;

import java.util.Map;
import java.util.Optional;

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private project.northerntrust.app.repository.LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    /**
     * Step 1: verify credentials against the database, then issue a LOGIN OTP (console only).
     */
    public LoginResponse login(LoginRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByAccountNumber(loginRequest.getAccountNumber());

        if (userOptional.isEmpty()) {
            return new LoginResponse(false, false, "Invalid User ID or password");
        }

        User user = userOptional.get();
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return new LoginResponse(false, false, "Invalid User ID or password");
        }

        otpService.sendLoginOtp(user.getAccountNumber());

        return new LoginResponse(
                true,
                true,
                "Enter the verification code to complete sign-in."
        );
    }

    /**
     * Step 2: verify LOGIN OTP and complete authentication.
     */
    public LoginResponse verifyLoginOtp(String accountNumber, String code, String ipAddress, String userAgent) {
        if (accountNumber == null || accountNumber.isBlank() || code == null || code.isBlank()) {
            return new LoginResponse(false, false, "Account number and verification code are required");
        }

        if (!userRepository.findByAccountNumber(accountNumber).isPresent()) {
            return new LoginResponse(false, false, "Invalid or expired verification code");
        }

        Map<String, Object> result = otpService.verifyOtp(accountNumber.trim(), code.trim(), OtpPurpose.LOGIN);
        if (Boolean.TRUE.equals(result.get("valid"))) {
            User user = userRepository.findByAccountNumber(accountNumber).get();
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ipAddress);
            user.setLastLoginUserAgent(userAgent);
            userRepository.save(user);
            return new LoginResponse(true, false, "Login successful");
        }
        return new LoginResponse(false, false, (String) result.get("message"));
    }

    public LoginResponse resendLoginOtp(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return new LoginResponse(false, false, "User ID is required");
        }

        Optional<User> userOptional = userRepository.findByAccountNumber(accountNumber.trim());
        if (userOptional.isEmpty() || !otpService.hasActiveLoginOtp(accountNumber.trim())) {
            return new LoginResponse(false, false, "Sign in again with your password to request a new code");
        }

        otpService.sendLoginOtp(accountNumber.trim());
        return new LoginResponse(true, true, "A new verification code has been issued.");
    }

    public MessageResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByAccountNumber(registerRequest.getAccountNumber())) {
            return new MessageResponse(false, "Account number already exists");
        }
        
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return new MessageResponse(false, "Email already exists");
        }

        User newUser = new User();
        newUser.setAccountNumber(registerRequest.getAccountNumber());
        newUser.setFirstName(registerRequest.getFirstName());
        newUser.setLastName(registerRequest.getLastName());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPhoneNumber(registerRequest.getPhoneNumber());
        
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setTransactionPinHash(passwordEncoder.encode(registerRequest.getTransactionPin()));
        
        User savedUser = userRepository.save(newUser);
        
        project.northerntrust.app.entity.Account newAccount = new project.northerntrust.app.entity.Account();
        newAccount.setUser(savedUser);
        newAccount.setAccountNumber(registerRequest.getAccountNumber());
        newAccount.setAccountType(project.northerntrust.app.entity.enums.AccountType.SAVINGS);
        
        java.math.BigDecimal initialBalance = new java.math.BigDecimal("50000.00");
        newAccount.setBalance(initialBalance);
        newAccount.setAvailableBalance(initialBalance);
        
        project.northerntrust.app.entity.Account savedAccount = accountRepository.save(newAccount);
        
        project.northerntrust.app.entity.LedgerEntry seedEntry = new project.northerntrust.app.entity.LedgerEntry();
        seedEntry.setAccount(savedAccount);
        seedEntry.setEntryType(project.northerntrust.app.entity.enums.EntryType.CREDIT);
        seedEntry.setAmount(initialBalance);
        seedEntry.setBalanceAfter(initialBalance);
        ledgerEntryRepository.save(seedEntry);
        
        return new MessageResponse(true, "User registered successfully with a seeded account");
    }
}
