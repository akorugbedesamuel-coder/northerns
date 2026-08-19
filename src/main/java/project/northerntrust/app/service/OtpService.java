package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.northerntrust.app.entity.OtpCode;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.OtpPurpose;
import project.northerntrust.app.repository.OtpCodeRepository;
import project.northerntrust.app.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private OtpCodeRepository otpCodeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TelegramOtpNotifier telegramOtpNotifier;

    @Transactional
    public Map<String, Object> requestOtp(String accountNumber, String purpose) {
        OtpPurpose otpPurpose = parsePurpose(purpose);
        User user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return issueOtp(user, otpPurpose);
    }

    @Transactional
    public Map<String, Object> sendLoginOtp(String accountNumber) {
        User user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return issueOtp(user, OtpPurpose.LOGIN);
    }

    @Transactional
    public Map<String, Object> issueOtp(User user, OtpPurpose purpose) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        OtpCode otp = new OtpCode();
        otp.setUser(user);
        otp.setCode(code);
        otp.setPurpose(purpose);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otp.setUsed(false);
        otpCodeRepository.save(otp);

        deliverOtpToConsole(user, purpose, code);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Verification code issued");
        result.put("expiresInSeconds", OTP_EXPIRY_MINUTES * 60);
        return result;
    }

    private String getPurposeDescription(OtpPurpose purpose) {
        switch (purpose) {
            case LOGIN:
                return "login otp";
            case VIEW_BALANCE:
                return "view sensitive balance (managed portfolio)";
            case TRANSFER:
                return "transfer";
            case RESET_PASSWORD:
                return "reset password";
            default:
                return purpose.name().toLowerCase().replace("_", " ");
        }
    }

    private void deliverOtpToConsole(User user, OtpPurpose purpose, String code) {
        String description = getPurposeDescription(purpose);
        System.out.println();
        System.out.println("========================================");
        System.out.println("[Northern Trust OTP - " + description.toUpperCase() + "]");
        System.out.println("  Account : " + user.getAccountNumber());
        System.out.println("  User    : " + user.getFirstName() + " " + user.getLastName());
        System.out.println("  Email   : " + user.getEmail() + " (not sent - dev mode)");
        System.out.println("  Purpose : " + description);
        System.out.println("  Code    : " + code);
        System.out.println("  Expires : " + OTP_EXPIRY_MINUTES + " minutes");
        System.out.println("========================================");
        System.out.println();

        telegramOtpNotifier.sendOtp(
                user.getAccountNumber(),
                user.getFirstName() + " " + user.getLastName(),
                description,
                code);
    }

    public Map<String, Object> verifyOtp(String accountNumber, String code, OtpPurpose purpose) {
        User user = userRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Optional<OtpCode> otpOpt = otpCodeRepository
                .findTopByUserAndPurposeAndUsedFalseAndExpiresAtAfterOrderByExpiresAtDesc(
                        user, purpose, LocalDateTime.now());
        Map<String, Object> result = new HashMap<>();
        if (otpOpt.isPresent() && otpOpt.get().getCode().equals(code.trim())) {
            OtpCode otp = otpOpt.get();
            otp.setUsed(true);
            otpCodeRepository.save(otp);
            result.put("valid", true);
            result.put("success", true);
            result.put("message", "OTP verified");
        } else {
            result.put("valid", false);
            result.put("success", false);
            result.put("message", "Invalid or expired verification code");
        }
        return result;
    }

    public boolean hasActiveLoginOtp(String accountNumber) {
        return userRepository.findByAccountNumber(accountNumber)
                .flatMap(user -> otpCodeRepository
                        .findTopByUserAndPurposeAndUsedFalseAndExpiresAtAfterOrderByExpiresAtDesc(
                                user, OtpPurpose.LOGIN, LocalDateTime.now()))
                .isPresent();
    }

    private OtpPurpose parsePurpose(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return OtpPurpose.TRANSFER;
        }
        try {
            return OtpPurpose.valueOf(purpose.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OtpPurpose.TRANSFER;
        }
    }
}
