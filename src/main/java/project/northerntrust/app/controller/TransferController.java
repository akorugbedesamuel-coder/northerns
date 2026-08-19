package project.northerntrust.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.northerntrust.app.dto.LedgerHistoryResponse;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.dto.TransferRequest;
import project.northerntrust.app.service.TransferService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TransferController {

    @Autowired
    private TransferService transferService;

    @PostMapping("/transfers/internal")
    public ResponseEntity<MessageResponse> performInternalTransfer(@Valid @RequestBody TransferRequest request) {
        MessageResponse response = transferService.performInternalTransfer(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/transfers/external")
    public ResponseEntity<MessageResponse> performExternalTransfer(@Valid @RequestBody project.northerntrust.app.dto.ExternalTransferRequest request) {
        MessageResponse response = transferService.performExternalTransfer(request);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/accounts/{accountNumber}/history")
    public ResponseEntity<?> getTransactionHistory(@PathVariable String accountNumber) {
        try {
            List<LedgerHistoryResponse> history = transferService.getAccountHistory(accountNumber);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/accounts/{accountNumber}/verify-balance")
    public ResponseEntity<MessageResponse> verifyLedgerBalance(@PathVariable String accountNumber) {
        MessageResponse response = transferService.verifyLedgerBalance(accountNumber);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(409).body(response); // Conflict or state mismatch
        }
    }
}
