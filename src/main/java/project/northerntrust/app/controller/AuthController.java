package project.northerntrust.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import project.northerntrust.app.dto.LoginRequest;
import project.northerntrust.app.dto.LoginResponse;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.dto.OtpVerifyRequest;
import project.northerntrust.app.dto.RegisterRequest;
import project.northerntrust.app.service.AuthenticationService;
import project.northerntrust.app.service.NotificationService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private NotificationService notificationService;

    /** Step 1: validate User ID + password, send LOGIN OTP to server console. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse response = authenticationService.login(loginRequest);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body(response);
    }

    /** Step 2: verify LOGIN OTP and complete sign-in. */
    @PostMapping("/login/verify-otp")
    public ResponseEntity<LoginResponse> verifyLoginOtp(
            @RequestBody OtpVerifyRequest request,
            HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        LoginResponse response = authenticationService.verifyLoginOtp(
                request.getAccountNumber(),
                request.getCode(),
                ip,
                httpRequest.getHeader("User-Agent"));
        if (response.isSuccess()) {
            notificationService.recordLogin(
                    request.getAccountNumber(),
                    ip,
                    httpRequest.getHeader("User-Agent"));
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body(response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/login/resend-otp")
    public ResponseEntity<LoginResponse> resendLoginOtp(@RequestBody Map<String, String> body) {
        String accountNumber = body != null ? body.get("accountNumber") : null;
        LoginResponse response = authenticationService.resendLoginOtp(accountNumber);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(400).body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        MessageResponse response = authenticationService.register(registerRequest);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}
