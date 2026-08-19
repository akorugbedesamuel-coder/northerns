package project.northerntrust.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.northerntrust.app.dto.BeneficiaryRequest;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.entity.Beneficiary;
import project.northerntrust.app.service.BeneficiaryService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {

    @Autowired
    private BeneficiaryService beneficiaryService;

    @PostMapping("/save")
    public ResponseEntity<MessageResponse> saveBeneficiary(@Valid @RequestBody BeneficiaryRequest request) {
        MessageResponse response = beneficiaryService.saveBeneficiary(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/{userAccountNumber}")
    public ResponseEntity<?> getUserBeneficiaries(@PathVariable String userAccountNumber) {
        try {
            List<Beneficiary> beneficiaries = beneficiaryService.getUserBeneficiaries(userAccountNumber);
            return ResponseEntity.ok(beneficiaries);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(false, e.getMessage()));
        }
    }
}
