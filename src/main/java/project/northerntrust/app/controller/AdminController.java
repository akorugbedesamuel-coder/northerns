package project.northerntrust.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.service.AdminService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String accountNumber = body != null ? body.get("accountNumber") : null;
        String password = body != null ? body.get("password") : null;
        Map<String, Object> result = adminService.login(accountNumber, password);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(401).body(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        adminService.logout(token);
        return ResponseEntity.ok(new MessageResponse(true, "Signed out"));
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminService.validateToken(token)) {
            return unauthorized();
        }
        return ResponseEntity.ok(adminService.getOverview());
    }

    @GetMapping("/approvals/pending")
    public ResponseEntity<?> pending(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminService.validateToken(token)) {
            return unauthorized();
        }
        return ResponseEntity.ok(adminService.getPendingApprovals());
    }

    @GetMapping("/transfers")
    public ResponseEntity<?> transfers(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminService.validateToken(token)) {
            return unauthorized();
        }
        return ResponseEntity.ok(adminService.getAllTransfers());
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> accounts(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminService.validateToken(token)) {
            return unauthorized();
        }
        return ResponseEntity.ok(adminService.getAccounts());
    }

    @PostMapping("/approvals/{reference}/{action}")
    public ResponseEntity<?> approvalAction(
            @PathVariable String reference,
            @PathVariable String action,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminService.validateToken(token)) {
            return unauthorized();
        }
        MessageResponse response = adminService.approve(reference, action);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", "Unauthorized: sign in to the admin console first");
        return ResponseEntity.status(401).body(body);
    }
}
