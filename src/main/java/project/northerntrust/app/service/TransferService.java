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

    @Autowired
    private TelegramAlertService telegramAlertService;

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

        // USD principal actually debited: SWIFT converts the foreign principal at the quoted rate.
        boolean swiftWithFx = transfer.getTransferType() == TransferType.SWIFT
                && request.getUsdEquivalent() != null
                && request.getUsdEquivalent().compareTo(BigDecimal.ZERO) > 0;
        BigDecimal principalUsd = swiftWithFx ? request.getUsdEquivalent() : request.getAmount();

        Optional<Account> fromAccountOpt = accountRepository.findByAccountNumber(request.getFromAccountNumber());
        if (fromAccountOpt.isEmpty()) {
            return new MessageResponse(false, "Sender account not found.");
        }
        Account sender = fromAccountOpt.get();

        // AML CHECK
        try {
            amlRiskService.analyzeTransfer(sender.getUser(), principalUsd, sender.getAccountNumber());
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

        // Calculate Fees (rail fee + urgent surcharge on domestic Fedwire)
        BigDecimal urgentSurcharge = transfer.getTransferType() == TransferType.WIRE
                && request.getPriority() != null && "urgent".equalsIgnoreCase(request.getPriority())
                ? new BigDecimal("15.00") : BigDecimal.ZERO;
        BigDecimal percentBase = (transfer.getTransferType() == TransferType.SWIFT
                && request.getUsdEquivalent() != null)
                ? request.getUsdEquivalent()
                : request.getAmount();
        BigDecimal feeAmount = rail.getFeeFixed().add(percentBase.multiply(rail.getFeePercentage()))
                .add(urgentSurcharge);
        BigDecimal totalDeduction = principalUsd.add(feeAmount);

        // 2. VALIDATION
        if (sender.getStatus() != AccountStatus.ACTIVE) {
            logAndFail(transfer, "Sender account is not ACTIVE.");
            return new MessageResponse(false, "Sender account is frozen or closed.");
        }

        if (sender.getAvailableBalance().compareTo(totalDeduction) < 0) {
            logAndFail(transfer, "Insufficient funds to cover amount and fees (" + feeAmount + " " + sender.getCurrency() + ").");
            return new MessageResponse(false, "Insufficient funds to cover amount and fees.");
        }

        if (principalUsd.compareTo(sender.getDailyTransferLimit()) > 0) {
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
        suspenseAccount.setBalance(suspenseAccount.getBalance().add(principalUsd));
        suspenseAccount.setAvailableBalance(suspenseAccount.getAvailableBalance().add(principalUsd));
        accountRepository.save(suspenseAccount);
        createLedgerEntry(suspenseAccount, transfer, EntryType.CREDIT, principalUsd, suspenseAccount.getBalance());

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
        details.setCountry(request.getCountry());
        details.setPriority(request.getPriority());
        details.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        details.setFxRate(request.getFxRate() != null ? request.getFxRate() : java.math.BigDecimal.ONE);
        details.setUsdEquivalent(request.getUsdEquivalent() != null ? request.getUsdEquivalent() : request.getAmount());
        details.setFee(feeAmount);
        transferDetailRepository.save(details);

        TransferFee feeRecord = new TransferFee();
        feeRecord.setTransfer(transfer);
        feeRecord.setFeeAmount(feeAmount);
        feeRecord.setFeeType(rail.getFeePercentage().compareTo(BigDecimal.ZERO) > 0 ? FeeType.PERCENTAGE : FeeType.FLAT);
        feeRecord.setChargedTo(FeeChargedTo.SENDER);
        transferFeeRepository.save(feeRecord);

        // 5. RAIL ROUTING — risk-based screening for wires; large ACH goes to treasury review.
        if (transfer.getTransferType() == TransferType.WIRE || transfer.getTransferType() == TransferType.SWIFT) {
            WireComplianceService.WireHoldDecision screening = wireComplianceService.evaluateWireTransfer(
                    sender.getUser(), sender, principalUsd, transfer.getTransferType(), international);
            if (screening.isHold()) {
                return placeWireOnComplianceHold(transfer, sender, request, rail, feeAmount, screening);
            }
            if (transfer.getTransferType() == TransferType.WIRE) {
                // Domestic Fedwire settles same-day after passing pre-screening.
                details.setExpectedSettlementDate(LocalDate.now());
                transferDetailRepository.save(details);
                return settleTransferNow(transfer, suspenseAccount, rail, feeAmount);
            }
            // International SWIFT: funds reserved; wire is In Transit until the value date.
            LocalDate swiftSettlementDate = computeWireSettlementDate(2);
            details.setExpectedSettlementDate(swiftSettlementDate);
            transferDetailRepository.save(details);
            transfer.setStatus(TransferStatus.PENDING_PROCESSING);
            transfer.setDisplayStatus(DisplayTransferStatus.In_Transit);
            transfer.setPendingApproval(false);
            transferRepository.save(transfer);
            logStatus(transfer, TransferStatus.PENDING_PROCESSING,
                    "Queued to SWIFT network. Expected settlement: " + swiftSettlementDate + ".");
            updateStatementStatus(transfer, "In Transit");

            String summary = "SWIFT wire queued for cross-border settlement. Reference: " + transfer.getReference()
                    + ". Fee: $" + feeAmount + ". Expected settlement: " + swiftSettlementDate;
            return new MessageResponse(true, summary, false, transfer.getReference());
        }
        if (transfer.getTransferType() == TransferType.ACH && request.getAmount().compareTo(new BigDecimal("25000")) > 0) {
            return placeAchOnTreasuryHold(transfer, sender, request, feeAmount);
        }

        // 6. Small ACH settles immediately.
        return settleTransferNow(transfer, suspenseAccount, rail, feeAmount);
    }

    /** Returns the first non-blank value among the candidates. */
    private String firstNonEmpty(String... candidates) {
        for (String c : candidates) {
            if (c != null && !c.trim().isEmpty()) {
                return c.trim();
            }
        }
        return null;
    }

    /** Returns today + {@code businessDays} business days (weekends skipped). */
    private LocalDate computeWireSettlementDate(int businessDays) {
        LocalDate d = LocalDate.now();
        int added = 0;
        while (added < businessDays) {
            d = d.plusDays(1);
            if (d.getDayOfWeek() != java.time.DayOfWeek.SATURDAY
                    && d.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return d;
    }

    /**
     * Standard ACH origination flow (NACHA-compliant).
     * Handles CREDIT (push) and DEBIT (pull) directions, effective/settlement dates,
     * same-day fees, mandatory debit authorization, beneficiary status and limit checks.
     */
    @Transactional
    public MessageResponse performAchTransfer(ExternalTransferRequest request, Beneficiary beneficiary) {
        boolean credit = !"DEBIT".equalsIgnoreCase(request.getDirection());
        boolean sameDay = Boolean.TRUE.equals(request.getSameDay());
        BigDecimal fee = sameDay ? new BigDecimal("15.00") : BigDecimal.ZERO;

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return new MessageResponse(false, "Amount must be greater than 0.");
        }
        if (request.getEffectiveDate() != null && !request.getEffectiveDate().isBlank()) {
            try {
                LocalDate eff = LocalDate.parse(request.getEffectiveDate());
                if (eff.isBefore(LocalDate.now())) {
                    return new MessageResponse(false, "Effective date cannot be in the past.");
                }
            } catch (Exception e) {
                return new MessageResponse(false, "Invalid effective date.");
            }
        }
        if (!credit && !Boolean.TRUE.equals(request.getAchAuthAck())) {
            return new MessageResponse(false, "A debit authorization is required to initiate an ACH debit.");
        }
        if (beneficiary != null) {
            if (beneficiary.getStatus() == BeneficiaryStatus.BLOCKED) {
                return new MessageResponse(false, "Beneficiary is blocked and cannot receive transfers.");
            }
            if (beneficiary.getStatus() == BeneficiaryStatus.PENDING_REVIEW) {
                return new MessageResponse(false, "Beneficiary is pending compliance review and cannot be used yet.");
            }
            if (beneficiary.getSingleLimit() != null && beneficiary.getSingleLimit().compareTo(BigDecimal.ZERO) > 0
                    && request.getAmount().compareTo(beneficiary.getSingleLimit()) > 0) {
                return new MessageResponse(false, "Amount exceeds the single-transfer limit for this beneficiary.");
            }
            if (beneficiary.getDailyLimit() != null && beneficiary.getDailyLimit().compareTo(BigDecimal.ZERO) > 0
                    && request.getAmount().compareTo(beneficiary.getDailyLimit()) > 0) {
                return new MessageResponse(false, "Amount exceeds the daily limit for this beneficiary.");
            }
        }

        Optional<Account> fromAccountOpt = accountRepository.findByAccountNumber(request.getFromAccountNumber());
        if (fromAccountOpt.isEmpty()) {
            return new MessageResponse(false, "Sender account not found.");
        }
        Account sender = fromAccountOpt.get();

        Transfer transfer = new Transfer();
        transfer.setReference("ACH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transfer.setTransferType(TransferType.ACH);
        transfer.setDirection(TransferDirection.DOMESTIC);
        transfer.setAmount(request.getAmount());
        transfer.setDescription(request.getDescription());
        transfer.setStatus(TransferStatus.CREATED);

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
        transfer.setCounterpartyName(beneficiary != null ? beneficiary.getDisplayName() : request.getRecipientName());
        transfer.setDisplayStatus(DisplayTransferStatus.Processing);
        transfer = transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.CREATED, "ACH origination request received.");

        if (sender.getStatus() != AccountStatus.ACTIVE) {
            logAndFail(transfer, "Sender account is not ACTIVE.");
            return new MessageResponse(false, "Sender account is frozen or closed.");
        }

        BigDecimal totalDebit = credit ? request.getAmount().add(fee) : fee;
        if (sender.getAvailableBalance().compareTo(totalDebit) < 0) {
            logAndFail(transfer, credit
                    ? "Insufficient funds to cover amount and ACH fee."
                    : "Insufficient available balance to cover the ACH debit fee.");
            return new MessageResponse(false, credit
                    ? "Insufficient funds to cover amount and ACH fee."
                    : "Insufficient available balance to cover the ACH debit fee.");
        }
        if (request.getAmount().compareTo(sender.getDailyTransferLimit()) > 0) {
            logAndFail(transfer, "Amount exceeds daily transfer limit.");
            return new MessageResponse(false, "Amount exceeds daily transfer limit.");
        }

        transfer.setStatus(TransferStatus.VALIDATED);
        transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.VALIDATED, "Authorization, limits, and balances verified. Fee: " + fee + ".");

        transfer.setStatus(TransferStatus.PENDING_PROCESSING);
        transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.PENDING_PROCESSING, "Posting " + (credit ? "debit" : "credit") + " entries.");

        if (credit) {
            sender.setBalance(sender.getBalance().subtract(totalDebit));
            sender.setAvailableBalance(sender.getAvailableBalance().subtract(totalDebit));
            accountRepository.save(sender);
            createLedgerEntry(sender, transfer, EntryType.DEBIT, totalDebit, sender.getBalance());
            createStatementLine(sender, sender.getProductKey(), "ACH Credit to " + transfer.getCounterpartyName(),
                    sender.getDisplayName(), "Withdrawal", totalDebit.negate(), sender.getBalance(),
                    "ACH", "Pending", transfer.getReference());

            Account suspense = accountRepository.findByAccountNumber("OUTBOUND_SUSPENSE_01").orElseThrow();
            suspense.setBalance(suspense.getBalance().add(request.getAmount()));
            suspense.setAvailableBalance(suspense.getAvailableBalance().add(request.getAmount()));
            accountRepository.save(suspense);
            createLedgerEntry(suspense, transfer, EntryType.CREDIT, request.getAmount(), suspense.getBalance());
        } else {
            // DEBIT (pull): funds arrive; the service fee is charged to the account.
            sender.setBalance(sender.getBalance().add(request.getAmount()).subtract(fee));
            sender.setAvailableBalance(sender.getAvailableBalance().add(request.getAmount()).subtract(fee));
            accountRepository.save(sender);
            createLedgerEntry(sender, transfer, EntryType.CREDIT, request.getAmount(), sender.getBalance());
            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                createLedgerEntry(sender, transfer, EntryType.DEBIT, fee, sender.getBalance());
            }
            createStatementLine(sender, sender.getProductKey(), "ACH Debit from " + transfer.getCounterpartyName(),
                    sender.getDisplayName(), "Deposit", request.getAmount().subtract(fee), sender.getBalance(),
                    "ACH", "Pending", transfer.getReference());
        }

        if (fee.compareTo(BigDecimal.ZERO) > 0) {
            Account feeAccount = accountRepository.findByAccountNumber("FEE_REVENUE_01").orElseThrow();
            feeAccount.setBalance(feeAccount.getBalance().add(fee));
            feeAccount.setAvailableBalance(feeAccount.getAvailableBalance().add(fee));
            accountRepository.save(feeAccount);
            createLedgerEntry(feeAccount, transfer, EntryType.CREDIT, fee, feeAccount.getBalance());
        }

        TransferFee feeRecord = new TransferFee();
        feeRecord.setTransfer(transfer);
        feeRecord.setFeeAmount(fee);
        feeRecord.setFeeType(FeeType.FLAT);
        feeRecord.setChargedTo(FeeChargedTo.SENDER);
        transferFeeRepository.save(feeRecord);

        LocalDate effective = parseEffectiveDate(request.getEffectiveDate());
        LocalDate settlementDate = computeSettlementDate(effective, sameDay);
        TransferDetail details = new TransferDetail();
        details.setTransfer(transfer);
        details.setBankName(firstNonEmpty(request.getBankName(), beneficiary != null ? beneficiary.getBankName() : null));
        details.setRoutingNumber(firstNonEmpty(request.getRoutingNumber(), beneficiary != null ? beneficiary.getRoutingOrSwift() : null));
        details.setSwiftCode(firstNonEmpty(request.getSwiftCode(), beneficiary != null ? beneficiary.getRoutingOrSwift() : null));
        details.setRecipientAccount(firstNonEmpty(request.getRecipientAccount(), beneficiary != null ? beneficiary.getAccountNumber() : null));
        details.setRecipientName(firstNonEmpty(request.getRecipientName(), beneficiary != null ? beneficiary.getDisplayName() : null));
        details.setRecipientAddress(request.getRecipientAddress());
        details.setCountry(firstNonEmpty(request.getCountry(), beneficiary != null ? beneficiary.getCountry() : null));
        details.setAchDirection(credit ? "CREDIT" : "DEBIT");
        details.setEffectiveDate(effective);
        details.setSecCode(request.getSecCode());
        details.setScheduling(request.getScheduling());
        details.setSameDay(sameDay);
        details.setExpectedSettlementDate(settlementDate);
        transferDetailRepository.save(details);

        // ACH above $25,000 is placed on treasury hold (NACHA threshold policy).
        if (credit && request.getAmount().compareTo(new BigDecimal("25000")) > 0) {
            return placeAchOnTreasuryHold(transfer, sender, request, fee);
        }

        Settlement settlement = new Settlement();
        settlement.setTransfer(transfer);
        settlement.setSettlementStatus(SettlementStatus.SETTLED);
        settlement.setSettlementDate(settlementDate.atTime(23, 59, 59));
        settlement.setExternalReference("RAIL-" + transfer.getReference());
        settlementRepository.save(settlement);

        // Release the reserved principal from the outbound suspense account now that
        // the NACHA batch entry is recorded (held transfers keep their reservation).
        if (credit) {
            Account suspenseRelease = accountRepository.findByAccountNumber("OUTBOUND_SUSPENSE_01").orElseThrow();
            suspenseRelease.setBalance(suspenseRelease.getBalance().subtract(request.getAmount()));
            suspenseRelease.setAvailableBalance(suspenseRelease.getAvailableBalance().subtract(request.getAmount()));
            accountRepository.save(suspenseRelease);
            createLedgerEntry(suspenseRelease, transfer, EntryType.DEBIT, request.getAmount(), suspenseRelease.getBalance());
        }

        transfer.setStatus(TransferStatus.SUCCESS);
        transfer.setDisplayStatus(DisplayTransferStatus.Settled);
        transfer.setPendingApproval(false);
        transferRepository.save(transfer);
        logStatus(transfer, TransferStatus.SUCCESS,
                "Queued to NACHA batch. Expected settlement: " + settlementDate
                        + (sameDay ? " (same-day)" : " (standard)") + ".");

        updateStatementStatus(transfer, "Completed");
        recordTransferSuccess(transfer.getUser(), transfer, transfer.getCounterpartyName(),
                credit ? "ACH credit transfer" : "ACH debit transfer");

        String summary = (credit ? "ACH credit queued. " : "ACH debit initiated. ")
                + "Reference: " + transfer.getReference() + ". Fee: $" + fee
                + ". Expected settlement: " + settlementDate;
        return new MessageResponse(true, summary, false, transfer.getReference());
    }

    private LocalDate parseEffectiveDate(String effectiveDate) {
        if (effectiveDate == null || effectiveDate.isBlank()) return LocalDate.now();
        try {
            return LocalDate.parse(effectiveDate);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private LocalDate computeSettlementDate(LocalDate effective, boolean sameDay) {
        LocalDate settle = sameDay ? effective : effective.plusDays(1);
        while (settle.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || settle.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            settle = settle.plusDays(1);
        }
        return settle;
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
                                                       BigDecimal feeAmount,
                                                       WireComplianceService.WireHoldDecision hold) {
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

        telegramAlertService.transferEvent("TRANSFER HELD FOR APPROVAL", transfer,
                request.getUsdEquivalent(),
                "Reasons: " + String.join("; ", hold.getReasons()));

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

        telegramAlertService.transferEvent("ACH TRANSFER HELD FOR TREASURY APPROVAL", transfer,
                null,
                "Amount exceeds the automated ACH threshold of $25,000.");

        return new MessageResponse(false, "ACH transfer placed on hold for treasury approval. Funds are reserved until authorized.", true, transfer.getReference());
    }

    /**
     * USD principal actually reserved/moved for this transfer. SWIFT wires reserve the
     * USD equivalent of the foreign principal (converted at the quoted rate); every
     * other rail moves the transfer amount as-is.
     */
    private BigDecimal resolveUsdPrincipal(Transfer transfer) {
        if (transfer.getTransferType() == TransferType.SWIFT && transfer.getId() != null) {
            Optional<TransferDetail> detailOpt = transferDetailRepository.findByTransfer_Id(transfer.getId());
            if (detailOpt.isPresent()) {
                BigDecimal usd = detailOpt.get().getUsdEquivalent();
                if (usd != null && usd.compareTo(BigDecimal.ZERO) > 0) {
                    return usd;
                }
            }
        }
        return transfer.getAmount();
    }

    /**
     * Settles a transfer that had its funds reserved in the outbound suspense account.
     * Releases the principal to the external rail and records settlement details.
     */
    private MessageResponse settleTransferNow(Transfer transfer, Account suspenseAccount,
                                              PaymentRail rail, BigDecimal feeAmount) {
        boolean treasuryApproved = Boolean.TRUE.equals(transfer.getPendingApproval());
        BigDecimal principal = resolveUsdPrincipal(transfer);
        suspenseAccount.setBalance(suspenseAccount.getBalance().subtract(principal));
        suspenseAccount.setAvailableBalance(suspenseAccount.getAvailableBalance().subtract(principal));
        accountRepository.save(suspenseAccount);
        createLedgerEntry(suspenseAccount, transfer, EntryType.DEBIT, principal, suspenseAccount.getBalance());

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

        if (treasuryApproved || principal.compareTo(new BigDecimal("10000")) >= 0) {
            telegramAlertService.transferEvent(treasuryApproved
                            ? "TRANSFER APPROVED & SETTLED" : "LARGE TRANSFER SETTLED",
                    transfer, principal,
                    treasuryApproved ? "Released by treasury approval." : null);
        }

        return new MessageResponse(true, "External transfer completed. Reference: " + transfer.getReference() + ". Fee charged: " + feeAmount,
                false, transfer.getReference());
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
        BigDecimal principal = resolveUsdPrincipal(transfer);
        BigDecimal totalDeduction = principal.add(fee);

        sender.setBalance(sender.getBalance().add(totalDeduction));
        sender.setAvailableBalance(sender.getAvailableBalance().add(totalDeduction));
        accountRepository.save(sender);
        createLedgerEntry(sender, transfer, EntryType.CREDIT, totalDeduction, sender.getBalance());

        suspense.setBalance(suspense.getBalance().subtract(principal));
        suspense.setAvailableBalance(suspense.getAvailableBalance().subtract(principal));
        accountRepository.save(suspense);
        createLedgerEntry(suspense, transfer, EntryType.DEBIT, principal, suspense.getBalance());

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

        telegramAlertService.transferEvent("TRANSFER DECLINED", transfer, principal,
                "Rejected by treasury. Principal + fee returned to sender.");

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
        telegramAlertService.transferEvent("TRANSFER ESCALATED", transfer,
                resolveUsdPrincipal(transfer),
                "Escalated to treasury compliance officer. Funds remain reserved.");
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
