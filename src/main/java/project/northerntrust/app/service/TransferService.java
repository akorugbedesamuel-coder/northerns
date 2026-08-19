package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.northerntrust.app.dto.ExternalTransferRequest;
import project.northerntrust.app.dto.LedgerHistoryResponse;
import project.northerntrust.app.dto.MessageResponse;
import project.northerntrust.app.dto.TransferRequest;
import project.northerntrust.app.entity.*;
import project.northerntrust.app.entity.enums.*;
import project.northerntrust.app.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransferService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private TransferStatusLogRepository transferStatusLogRepository;

    @Autowired
    private PaymentRailRepository paymentRailRepository;

    @Autowired
    private TransferDetailRepository transferDetailRepository;

    @Autowired
    private TransferFeeRepository transferFeeRepository;

    @Autowired
    private StatementLineRepository statementLineRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private AmlRiskService amlRiskService;

    @Autowired
    private WireComplianceService wireComplianceService;

    @Autowired
    private AuditLogService auditLogService;

    @Transactional
    public MessageResponse performInternalTransfer(TransferRequest request) {
        // ... (existing implementation) ...
        Transfer transfer = new Transfer();
        transfer.setReference("TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transfer.setTransferType(TransferType.INTERNAL);
        transfer.setDirection(TransferDirection.DOMESTIC);
        transfer.setAmount(request.getAmount());
        transfer.setDescription(request.getDescription());
        transfer.setStatus(TransferStatus.CREATED);

        Optional<Account> fromAccountOpt = accountRepository.findByAccountNumber(request.getFromAccountNumber());
        Optional<Account> toAccountOpt = accountRepository.findByAccountNumber(request.getToAccountNumber());

        if (fromAccountOpt.isEmpty()) {
            return new MessageResponse(false, "Sender account not found.");
        }
        if (toAccountOpt.isEmpty()) {
            return new MessageResponse(false, "Recipient account not found.");
        }

        Account sender = fromAccountOpt.get();
        Account receiver = toAccountOpt.get();

        // AML CHECK
        try {
            amlRiskService.analyzeTransfer(sender.getUser(), request.getAmount(), sender.getAccountNumber());
        } catch (RuntimeException e) {
            transfer.setUser(sender.getUser());
            transfer.setFromAccount(sender);
            transfer.setToAccount(receiver);
            transfer.setCurrency(sender.getCurrency());
            transfer = transferRepository.save(transfer);
            logAndFail(transfer, e.getMessage());
            return new MessageResponse(false, e.getMessage());
        }

        transfer.setUser(sender.getUser());
        transfer.setFromAccount(sender);
        transfer.setToAccount(receiver);
        transfer.setCurrency(sender.getCurrency());
        transfer.setCounterpartyName(receiver.getDisplayName());
        transfer.setSourceAccountLabel(sender.getDisplayName());
        
        transfer = transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.CREATED, "Transfer request received.");

        if (sender.getStatus() != AccountStatus.ACTIVE) {
            logAndFail(transfer, "Sender account is not ACTIVE.");
            return new MessageResponse(false, "Sender account is frozen or closed.");
        }

        if (receiver.getStatus() != AccountStatus.ACTIVE) {
            logAndFail(transfer, "Recipient account is not ACTIVE.");
            return new MessageResponse(false, "Recipient account is frozen or closed.");
        }

        if (!sender.getCurrency().equals(receiver.getCurrency())) {
            logAndFail(transfer, "Cross-currency transfers are not supported yet.");
            return new MessageResponse(false, "Sender and receiver must have the same currency.");
        }

        if (sender.getAvailableBalance().compareTo(request.getAmount()) < 0) {
            logAndFail(transfer, "Insufficient funds.");
            return new MessageResponse(false, "Insufficient funds.");
        }

        if (request.getAmount().compareTo(sender.getDailyTransferLimit()) > 0) {
            logAndFail(transfer, "Amount exceeds daily transfer limit.");
            return new MessageResponse(false, "Amount exceeds daily transfer limit.");
        }

        transfer.setStatus(TransferStatus.VALIDATED);
        transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.VALIDATED, "Balances and limits verified successfully.");

        transfer.setStatus(TransferStatus.PENDING_PROCESSING);
        transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.PENDING_PROCESSING, "Deducting funds from sender.");

        sender.setBalance(sender.getBalance().subtract(request.getAmount()));
        sender.setAvailableBalance(sender.getAvailableBalance().subtract(request.getAmount()));
        accountRepository.save(sender);

        receiver.setBalance(receiver.getBalance().add(request.getAmount()));
        receiver.setAvailableBalance(receiver.getAvailableBalance().add(request.getAmount()));
        accountRepository.save(receiver);

        createLedgerEntry(sender, transfer, EntryType.DEBIT, request.getAmount(), sender.getBalance());
        createLedgerEntry(receiver, transfer, EntryType.CREDIT, request.getAmount(), receiver.getBalance());

        createStatementLine(sender, sender.getProductKey(), "Internal Transfer to " + receiver.getDisplayName(),
                sender.getDisplayName(), "Withdrawal", request.getAmount().negate(), sender.getBalance(), "Internal", "Completed", transfer.getReference());
        createStatementLine(receiver, receiver.getProductKey(), "Internal Transfer from " + sender.getDisplayName(),
                receiver.getDisplayName(), "Deposit", request.getAmount(), receiver.getBalance(), "Internal", "Completed", transfer.getReference());

        transfer.setStatus(TransferStatus.SUCCESS);
        transfer.setDisplayStatus(DisplayTransferStatus.Settled);
        transfer.setPendingApproval(false);
        transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.SUCCESS, "Transfer complete. Ledger entries written.");

        recordTransferSuccess(sender.getUser(), transfer, receiver.getDisplayName(), "Internal transfer");

        return new MessageResponse(true, "Internal transfer successful. Reference: " + transfer.getReference());
    }

    @Transactional
    public MessageResponse performExternalTransfer(ExternalTransferRequest request) {
        // 1. CREATED
        Transfer transfer = new Transfer();
        transfer.setReference("EXT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        try {
            transfer.setTransferType(TransferType.valueOf(request.getTransferType().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return new MessageResponse(false, "Invalid transfer type.");
        }
        
        transfer.setDirection(transfer.getTransferType() == TransferType.SWIFT ? TransferDirection.INTERNATIONAL : TransferDirection.DOMESTIC);
        transfer.setAmount(request.getAmount());
        transfer.setDescription(request.getDescription());
        transfer.setStatus(TransferStatus.CREATED);

        Optional<Account> fromAccountOpt = accountRepository.findByAccountNumber(request.getFromAccountNumber());
        if (fromAccountOpt.isEmpty()) {
            return new MessageResponse(false, "Sender account not found.");
        }
        Account sender = fromAccountOpt.get();

        // AML CHECK
        try {
            amlRiskService.analyzeTransfer(sender.getUser(), request.getAmount(), sender.getAccountNumber());
        } catch (RuntimeException e) {
            transfer.setUser(sender.getUser());
            transfer.setFromAccount(sender);
            transfer.setCurrency(sender.getCurrency());
            transfer = transferRepository.save(transfer);
            logAndFail(transfer, e.getMessage());
            return new MessageResponse(false, e.getMessage());
        }

        transfer.setUser(sender.getUser());
        transfer.setFromAccount(sender);
        transfer.setCurrency(sender.getCurrency());
        transfer.setSourceAccountLabel(sender.getDisplayName());
        transfer.setCounterpartyName(request.getRecipientName());
        transfer.setDisplayStatus(DisplayTransferStatus.Processing);
        
        transfer = transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.CREATED, "External transfer request received.");

        boolean international = transfer.getTransferType() == TransferType.SWIFT
                || transfer.getDirection() == TransferDirection.INTERNATIONAL;

        // Fetch Payment Rail Rules
        Optional<PaymentRail> railOpt = paymentRailRepository.findByRailType(PaymentRailType.valueOf(request.getTransferType().toUpperCase()));
        if (railOpt.isEmpty() || !railOpt.get().getActive()) {
            logAndFail(transfer, "Payment rail unavailable.");
            return new MessageResponse(false, "Payment rail unavailable.");
        }
        PaymentRail rail = railOpt.get();

        // Calculate Fees
        BigDecimal feeAmount = rail.getFeeFixed().add(request.getAmount().multiply(rail.getFeePercentage()));
        BigDecimal totalDeduction = request.getAmount().add(feeAmount);

        // 2. VALIDATION
        if (sender.getStatus() != AccountStatus.ACTIVE) {
            logAndFail(transfer, "Sender account is not ACTIVE.");
            return new MessageResponse(false, "Sender account is frozen or closed.");
        }

        if (sender.getAvailableBalance().compareTo(totalDeduction) < 0) {
            logAndFail(transfer, "Insufficient funds to cover amount and fees (" + feeAmount + " " + sender.getCurrency() + ").");
            return new MessageResponse(false, "Insufficient funds to cover amount and fees.");
        }

        if (request.getAmount().compareTo(sender.getDailyTransferLimit()) > 0) {
            logAndFail(transfer, "Amount exceeds daily transfer limit.");
            return new MessageResponse(false, "Amount exceeds daily transfer limit.");
        }

        transfer.setStatus(TransferStatus.VALIDATED);
        transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.VALIDATED, "Balances, limits, and fees (" + feeAmount + ") verified.");

        // 3. PENDING PROCESSING & DOUBLE ENTRY SYSTEM ACCOUNTS
        transfer.setStatus(TransferStatus.PENDING_PROCESSING);
        transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.PENDING_PROCESSING, "Deducting funds and routing to system accounts.");

        // Deduct from Sender
        sender.setBalance(sender.getBalance().subtract(totalDeduction));
        sender.setAvailableBalance(sender.getAvailableBalance().subtract(totalDeduction));
        accountRepository.save(sender);
        createLedgerEntry(sender, transfer, EntryType.DEBIT, totalDeduction, sender.getBalance());
        createStatementLine(sender, sender.getProductKey(), request.getDescription(),
                sender.getDisplayName(), "Withdrawal", totalDeduction.negate(), sender.getBalance(),
                request.getTransferType(), "Pending", transfer.getReference());

        // Credit to System Fee Revenue Account
        Account feeAccount = accountRepository.findByAccountNumber("FEE_REVENUE_01").orElseThrow();
        feeAccount.setBalance(feeAccount.getBalance().add(feeAmount));
        feeAccount.setAvailableBalance(feeAccount.getAvailableBalance().add(feeAmount));
        accountRepository.save(feeAccount);
        createLedgerEntry(feeAccount, transfer, EntryType.CREDIT, feeAmount, feeAccount.getBalance());

        // Credit to System Outbound Suspense Account (waiting to leave bank)
        Account suspenseAccount = accountRepository.findByAccountNumber("OUTBOUND_SUSPENSE_01").orElseThrow();
        suspenseAccount.setBalance(suspenseAccount.getBalance().add(request.getAmount()));
        suspenseAccount.setAvailableBalance(suspenseAccount.getAvailableBalance().add(request.getAmount()));
        accountRepository.save(suspenseAccount);
        createLedgerEntry(suspenseAccount, transfer, EntryType.CREDIT, request.getAmount(), suspenseAccount.getBalance());

        // 4. Save Metadata
        TransferDetail details = new TransferDetail();
        details.setTransfer(transfer);
        details.setBankName(request.getBankName());
        details.setRoutingNumber(request.getRoutingNumber());
        details.setSwiftCode(request.getSwiftCode());
        details.setIban(request.getIban());
        details.setRecipientAccount(request.getRecipientAccount());
        details.setRecipientName(request.getRecipientName());
        details.setRecipientAddress(request.getRecipientAddress());
        transferDetailRepository.save(details);

        TransferFee feeRecord = new TransferFee();
        feeRecord.setTransfer(transfer);
        feeRecord.setFeeAmount(feeAmount);
        feeRecord.setFeeType(rail.getFeePercentage().compareTo(BigDecimal.ZERO) > 0 ? FeeType.PERCENTAGE : FeeType.FLAT);
        feeRecord.setChargedTo(FeeChargedTo.SENDER);
        transferFeeRepository.save(feeRecord);

        // 5. RAIL ROUTING — wires and large ACH are placed on hold (funds reserved for settlement).
        if (transfer.getTransferType() == TransferType.WIRE || transfer.getTransferType() == TransferType.SWIFT) {
            return placeWireOnComplianceHold(transfer, sender, request, rail, feeAmount, international);
        }
        if (transfer.getTransferType() == TransferType.ACH && request.getAmount().compareTo(new BigDecimal("25000")) > 0) {
            return placeAchOnTreasuryHold(transfer, sender, request, feeAmount);
        }

        // 6. Small ACH settles immediately.
        return settleTransferNow(transfer, suspenseAccount, rail, feeAmount);
    }

    private void createLedgerEntry(Account account, Transfer transfer, EntryType type, BigDecimal amount, BigDecimal balanceAfter) {
        LedgerEntry entry = new LedgerEntry();
        entry.setAccount(account);
        entry.setTransfer(transfer);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setBalanceAfter(balanceAfter);
        ledgerEntryRepository.save(entry);
    }

    private MessageResponse placeWireOnComplianceHold(Transfer transfer, Account sender,
                                                      ExternalTransferRequest request, PaymentRail rail,
                                                      BigDecimal feeAmount, boolean international) {
        WireComplianceService.WireHoldDecision hold = wireComplianceService.evaluateWireTransfer(
                sender.getUser(), sender, request.getAmount(), transfer.getTransferType(), international);

        transfer.setDisplayStatus(DisplayTransferStatus.Compliance_Hold);
        transfer.setPendingApproval(true);
        transfer.setRiskScore(88);
        transfer.setRiskLevel("High");
        transfer.setStatus(TransferStatus.PENDING_PROCESSING);
        transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.PENDING_PROCESSING,
                "Compliance hold — funds reserved. " + String.join(" ", hold.getReasons()));

        updateStatementStatus(transfer, "Hold");

        String railLabel = transfer.getTransferType() == TransferType.SWIFT ? "SWIFT wire" : "Fedwire";
        notificationService.recordWireComplianceHold(
                sender.getUser().getAccountNumber(),
                transfer.getReference(),
                request.getAmount(),
                request.getRecipientName(),
                railLabel,
                hold.getReasons());
        auditLogService.record(sender.getUser(), "TRANSFER_COMPLIANCE_HOLD", "TRANSFER", transfer.getId(),
                auditLogService.detailsOf(
                        "reference", transfer.getReference(),
                        "amount", request.getAmount(),
                        "fee", feeAmount,
                        "counterparty", request.getRecipientName(),
                        "rail", railLabel,
                        "reasons", String.join("; ", hold.getReasons())));

        return new MessageResponse(false, hold.getSummaryMessage(), true, transfer.getReference());
    }

    private MessageResponse placeAchOnTreasuryHold(Transfer transfer, Account sender,
                                                   ExternalTransferRequest request, BigDecimal feeAmount) {
        transfer.setDisplayStatus(DisplayTransferStatus.Awaiting_Treasury_Approval);
        transfer.setPendingApproval(true);
        transfer.setStatus(TransferStatus.PENDING_PROCESSING);
        transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.PENDING_PROCESSING,
                "ACH above $25,000 placed on hold for treasury approval. Funds reserved.");

        updateStatementStatus(transfer, "Hold");

        List<String> reasons = java.util.Arrays.asList(
                "Amount exceeds the automated ACH threshold of $25,000.",
                "Requires treasury authorization before release to the NACHA batch.");
        notificationService.recordWireComplianceHold(
                sender.getUser().getAccountNumber(),
                transfer.getReference(),
                request.getAmount(),
                request.getRecipientName(),
                "ACH",
                reasons);
        auditLogService.record(sender.getUser(), "TRANSFER_TREASURY_HOLD", "TRANSFER", transfer.getId(),
                auditLogService.detailsOf(
                        "reference", transfer.getReference(),
                        "amount", request.getAmount(),
                        "fee", feeAmount,
                        "counterparty", request.getRecipientName(),
                        "rail", "ACH"));

        return new MessageResponse(false, "ACH transfer placed on hold for treasury approval. Funds are reserved until authorized.", true, transfer.getReference());
    }

    /**
     * Settles a transfer that had its funds reserved in the outbound suspense account.
     * Releases the principal to the external rail and records settlement details.
     */
    private MessageResponse settleTransferNow(Transfer transfer, Account suspenseAccount,
                                              PaymentRail rail, BigDecimal feeAmount) {
        suspenseAccount.setBalance(suspenseAccount.getBalance().subtract(transfer.getAmount()));
        suspenseAccount.setAvailableBalance(suspenseAccount.getAvailableBalance().subtract(transfer.getAmount()));
        accountRepository.save(suspenseAccount);
        createLedgerEntry(suspenseAccount, transfer, EntryType.DEBIT, transfer.getAmount(), suspenseAccount.getBalance());

        Settlement settlement = new Settlement();
        settlement.setTransfer(transfer);
        settlement.setSettlementStatus(SettlementStatus.SETTLED);
        settlement.setSettlementDate(java.time.LocalDateTime.now());
        settlement.setExternalReference("RAIL-" + transfer.getReference());
        settlementRepository.save(settlement);

        transfer.setStatus(TransferStatus.SUCCESS);
        transfer.setDisplayStatus(DisplayTransferStatus.Settled);
        transfer.setPendingApproval(false);
        transferRepository.save(transfer);
        String railName = rail != null ? rail.getRailType().name() : transfer.getTransferType().name();
        String processingTime = rail != null ? rail.getProcessingTime() : null;
        logStatus(transfer, TransferStatus.SUCCESS, "Settled. Funds released to " + railName
                + (processingTime != null ? " network. Processing time: " + processingTime : "") + ".");

        updateStatementStatus(transfer, "Completed");

        recordTransferSuccess(transfer.getUser(), transfer, transfer.getCounterpartyName(),
                railName + " transfer");

        return new MessageResponse(true, "External transfer completed. Reference: " + transfer.getReference() + ". Fee charged: " + feeAmount);
    }

    /**
     * Approves a held transfer: releases reserved funds to the rail and records settlement.
     */
    @Transactional
    public MessageResponse settleHeldTransfer(String reference) {
        Transfer transfer = transferRepository.findByReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + reference));
        if (transfer.getPendingApproval() == null || !transfer.getPendingApproval()) {
            return new MessageResponse(false, "Transfer is not awaiting approval.");
        }
        PaymentRail rail = paymentRailRepository.findByRailType(PaymentRailType.valueOf(transfer.getTransferType().name()))
                .orElse(null);
        Account suspense = accountRepository.findByAccountNumber("OUTBOUND_SUSPENSE_01")
                .orElseThrow(() -> new IllegalArgumentException("Suspense account not found"));
        BigDecimal fee = transfer.getTransferFee() != null && transfer.getTransferFee().getFeeAmount() != null
                ? transfer.getTransferFee().getFeeAmount() : BigDecimal.ZERO;
        return settleTransferNow(transfer, suspense, rail, fee);
    }

    /**
     * Rejects a held transfer: reverses the reservation, credits the sender back (principal + fee),
     * and reverses the fee/suspense ledger entries.
     */
    @Transactional
    public MessageResponse reverseHeldTransfer(String reference) {
        Transfer transfer = transferRepository.findByReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + reference));
        if (transfer.getPendingApproval() == null || !transfer.getPendingApproval()) {
            return new MessageResponse(false, "Transfer is not awaiting approval.");
        }
        Account sender = transfer.getFromAccount();
        Account suspense = accountRepository.findByAccountNumber("OUTBOUND_SUSPENSE_01")
                .orElseThrow(() -> new IllegalArgumentException("Suspense account not found"));
        Account feeAccount = accountRepository.findByAccountNumber("FEE_REVENUE_01")
                .orElseThrow(() -> new IllegalArgumentException("Fee account not found"));
        BigDecimal fee = transfer.getTransferFee() != null && transfer.getTransferFee().getFeeAmount() != null
                ? transfer.getTransferFee().getFeeAmount() : BigDecimal.ZERO;
        BigDecimal totalDeduction = transfer.getAmount().add(fee);

        sender.setBalance(sender.getBalance().add(totalDeduction));
        sender.setAvailableBalance(sender.getAvailableBalance().add(totalDeduction));
        accountRepository.save(sender);
        createLedgerEntry(sender, transfer, EntryType.CREDIT, totalDeduction, sender.getBalance());

        suspense.setBalance(suspense.getBalance().subtract(transfer.getAmount()));
        suspense.setAvailableBalance(suspense.getAvailableBalance().subtract(transfer.getAmount()));
        accountRepository.save(suspense);
        createLedgerEntry(suspense, transfer, EntryType.DEBIT, transfer.getAmount(), suspense.getBalance());

        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            feeAccount.setBalance(feeAccount.getBalance().subtract(fee));
            feeAccount.setAvailableBalance(feeAccount.getAvailableBalance().subtract(fee));
            accountRepository.save(feeAccount);
            createLedgerEntry(feeAccount, transfer, EntryType.DEBIT, fee, feeAccount.getBalance());
        }

        Settlement settlement = new Settlement();
        settlement.setTransfer(transfer);
        settlement.setSettlementStatus(SettlementStatus.FAILED);
        settlement.setSettlementDate(java.time.LocalDateTime.now());
        settlement.setExternalReference("REJECTED-" + transfer.getReference());
        settlementRepository.save(settlement);

        transfer.setStatus(TransferStatus.REVERSED);
        transfer.setDisplayStatus(DisplayTransferStatus.Returned);
        transfer.setPendingApproval(false);
        transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.REVERSED, "Transfer rejected and reversed. Funds returned to sender.");

        updateStatementStatus(transfer, "Reversed");

        notificationService.recordTransferFailed(
                sender.getUser().getAccountNumber(),
                transfer.getReference(),
                transfer.getAmount(),
                transfer.getCounterpartyName(),
                "Transfer was rejected by treasury and the funds returned.",
                transfer.getTransferType() + " transfer");
        auditLogService.record(transfer.getUser(), "TRANSFER_REVERSED", "TRANSFER", transfer.getId(),
                auditLogService.detailsOf(
                        "reference", transfer.getReference(),
                        "amount", transfer.getAmount(),
                        "fee", fee,
                        "reason", "Rejected by treasury"));

        return new MessageResponse(true, "Transfer rejected and reversed. Funds returned to sender. Reference: " + transfer.getReference());
    }

    /**
     * Escalates a held transfer back to a compliance hold (keeps funds reserved).
     */
    @Transactional
    public MessageResponse escalateHeldTransfer(String reference) {
        Transfer transfer = transferRepository.findByReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + reference));
        transfer.setDisplayStatus(DisplayTransferStatus.Compliance_Hold);
        transfer.setPendingApproval(true);
        transfer.setRiskScore(95);
        transfer.setRiskLevel("Critical");
        transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.PENDING_PROCESSING, "Escalated to treasury compliance officer.");
        return new MessageResponse(true, "Transfer escalated for further review. Reference: " + reference);
    }

    private void logAndFail(Transfer transfer, String message) {
        transfer.setStatus(TransferStatus.FAILED);
        transfer.setDisplayStatus(DisplayTransferStatus.Failed);
        transfer.setPendingApproval(false);
        transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.FAILED, message);
        if (transfer.getUser() != null) {
            String kind = transfer.getTransferType() != null
                    ? transfer.getTransferType().name() + " transfer" : "Transfer";
            notificationService.recordTransferFailed(
                    transfer.getUser().getAccountNumber(),
                    transfer.getReference(),
                    transfer.getAmount(),
                    transfer.getCounterpartyName(),
                    message,
                    kind);
            auditLogService.record(transfer.getUser(), "TRANSFER_FAILED", "TRANSFER", transfer.getId(),
                    auditLogService.detailsOf(
                            "reference", transfer.getReference(),
                            "amount", transfer.getAmount(),
                            "reason", message));
        }
    }

    private void recordTransferSuccess(User user, Transfer transfer, String counterparty, String transferKind) {
        notificationService.recordTransfer(
                user.getAccountNumber(),
                transfer.getReference(),
                transfer.getAmount(),
                counterparty,
                transferKind);
        auditLogService.record(user, "TRANSFER_COMPLETED", "TRANSFER", transfer.getId(),
                auditLogService.detailsOf(
                        "reference", transfer.getReference(),
                        "amount", transfer.getAmount(),
                        "counterparty", counterparty,
                        "transferKind", transferKind,
                        "status", "SUCCESS"));
    }

    private void logStatus(Transfer transfer, TransferStatus status, String message) {
        TransferStatusLog log = new TransferStatusLog();
        log.setTransfer(transfer);
        log.setStatus(status);
        log.setMessage(message);
        transferStatusLogRepository.save(log);
    }

    private void createStatementLine(Account account, ProductKey key, String desc, String source, String type,
                                     BigDecimal amount, BigDecimal balanceAfter, String channel, String status,
                                     String transferReference) {
        StatementLine line = new StatementLine();
        line.setUser(account.getUser());
        line.setProductKey(key);
        line.setLineDate(LocalDate.now());
        line.setDescription(desc);
        line.setSource(source);
        line.setLineType(type);
        line.setAmount(amount);
        line.setBalanceAfter(balanceAfter);
        line.setStatus(status);
        line.setChannel(channel);
        line.setTransferReference(transferReference);
        statementLineRepository.save(line);
    }

    private void updateStatementStatus(Transfer transfer, String status) {
        User user = transfer.getUser();
        String ref = transfer.getReference();
        statementLineRepository.findFirstByUserAndTransferReferenceAndLineTypeOrderByLineDateDesc(
                        user, ref, "Withdrawal")
                .ifPresent(line -> {
                    line.setStatus(status);
                    statementLineRepository.save(line);
                });
    }

    public List<LedgerHistoryResponse> getAccountHistory(String accountNumber) {
        Optional<Account> accountOpt = accountRepository.findByAccountNumber(accountNumber);
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found");
        }

        List<LedgerEntry> entries = ledgerEntryRepository.findByAccountOrderByCreatedAtDesc(accountOpt.get());

        return entries.stream().map(entry -> {
            LedgerHistoryResponse response = new LedgerHistoryResponse();
            response.setTransactionId(entry.getId().toString());
            response.setReference(entry.getTransfer() != null ? entry.getTransfer().getReference() : "N/A");
            response.setEntryType(entry.getEntryType());
            response.setAmount(entry.getAmount());
            response.setBalanceAfter(entry.getBalanceAfter());
            response.setDate(entry.getCreatedAt());
            response.setDescription(entry.getTransfer() != null ? entry.getTransfer().getDescription() : "");
            return response;
        }).collect(Collectors.toList());
    }

    public MessageResponse verifyLedgerBalance(String accountNumber) {
        Optional<Account> accountOpt = accountRepository.findByAccountNumber(accountNumber);
        if (accountOpt.isEmpty()) {
            return new MessageResponse(false, "Account not found");
        }

        Account account = accountOpt.get();
        List<LedgerEntry> entries = ledgerEntryRepository.findByAccountOrderByCreatedAtDesc(account);

        BigDecimal reconstructedBalance = BigDecimal.ZERO;
        
        for (LedgerEntry entry : entries) {
            if (entry.getEntryType() == EntryType.CREDIT) {
                reconstructedBalance = reconstructedBalance.add(entry.getAmount());
            } else if (entry.getEntryType() == EntryType.DEBIT) {
                reconstructedBalance = reconstructedBalance.subtract(entry.getAmount());
            }
        }

        // We removed the hardcoded +50,000 NGN because AuthenticationService now correctly creates a CREDIT ledger entry on signup!
        if (account.getBalance().compareTo(reconstructedBalance) == 0) {
            return new MessageResponse(true, "Ledger verified. Database balance (" + account.getBalance() + ") matches immutable ledger perfectly.");
        } else {
            return new MessageResponse(false, "LEDGER MISMATCH! DB Balance: " + account.getBalance() + " | Reconstructed: " + reconstructedBalance);
        }
    }
}
