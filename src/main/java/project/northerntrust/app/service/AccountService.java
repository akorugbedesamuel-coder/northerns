package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.entity.Account;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.AccountStatus;
import project.northerntrust.app.entity.enums.AccountType;
import project.northerntrust.app.repository.AccountRepository;
import project.northerntrust.app.repository.UserRepository;

import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    public MessageResponse freezeAccount(String accountNumber) {
        Optional<Account> accountOpt = accountRepository.findByAccountNumber(accountNumber);
        if (accountOpt.isEmpty()) {
            return new MessageResponse(false, "Account not found.");
        }

        Account account = accountOpt.get();
        account.setStatus(AccountStatus.FROZEN);
        accountRepository.save(account);

        return new MessageResponse(true, "Account suspended successfully.");
    }

    public MessageResponse unfreezeAccount(String accountNumber) {
        Optional<Account> accountOpt = accountRepository.findByAccountNumber(accountNumber);
        if (accountOpt.isEmpty()) {
            return new MessageResponse(false, "Account not found.");
        }

        Account account = accountOpt.get();
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);

        return new MessageResponse(true, "Account is now active.");
    }

    public MessageResponse createAdditionalAccount(String userAccountNumber, String type, String currency) {
        Optional<User> userOpt = userRepository.findByAccountNumber(userAccountNumber);
        if (userOpt.isEmpty()) {
            return new MessageResponse(false, "User not found.");
        }

        User user = userOpt.get();
        Account newAccount = new Account();
        newAccount.setUser(user);
        
        // Generate a new 10 digit account number
        String newAccountNumber = String.valueOf((long) (Math.random() * 9000000000L) + 1000000000L);
        newAccount.setAccountNumber(newAccountNumber);
        
        try {
            newAccount.setAccountType(AccountType.valueOf(type.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return new MessageResponse(false, "Invalid account type. Use SAVINGS or CURRENT.");
        }
        
        newAccount.setCurrency(currency.toUpperCase());
        
        accountRepository.save(newAccount);

        return new MessageResponse(true, "New " + currency + " " + type + " account created successfully. Account Number: " + newAccountNumber);
    }
}
