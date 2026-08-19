package project.northerntrust.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.service.AccountService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping("/{accountNumber}/freeze")
    public ResponseEntity<MessageResponse> freezeAccount(@PathVariable String accountNumber) {
        MessageResponse response = accountService.freezeAccount(accountNumber);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/{accountNumber}/unfreeze")
    public ResponseEntity<MessageResponse> unfreezeAccount(@PathVariable String accountNumber) {
        MessageResponse response = accountService.unfreezeAccount(accountNumber);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/create")
    public ResponseEntity<MessageResponse> createAccount(
            @RequestParam String userAccountNumber, 
            @RequestParam String type, 
            @RequestParam(defaultValue = "USD") String currency) {
        
        MessageResponse response = accountService.createAdditionalAccount(userAccountNumber, type, currency);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }
}
