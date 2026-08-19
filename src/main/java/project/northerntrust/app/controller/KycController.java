package project.northerntrust.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.service.KycService;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

    @Autowired
    private KycService kycService;

    @PostMapping("/submit")
    public ResponseEntity<MessageResponse> submitKyc(
            @RequestParam String accountNumber,
            @RequestParam String bvn,
            @RequestParam String nin,
            @RequestParam String idType,
            @RequestParam String idNumber) {
        
        MessageResponse response = kycService.submitKyc(accountNumber, bvn, nin, idType, idNumber);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/admin/approve/{accountNumber}")
    public ResponseEntity<MessageResponse> approveKyc(@PathVariable String accountNumber) {
        MessageResponse response = kycService.approveKyc(accountNumber);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
