package project.northerntrust.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.northerntrust.app.entity.Account;
import project.northerntrust.app.entity.User;
import project.northerntrust.app.entity.enums.ProductKey;
import project.northerntrust.app.entity.enums.Severity;
import project.northerntrust.app.entity.enums.TransferType;
import project.northerntrust.app.repository.TransferRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AML / risk and savings-withdrawal policy for outbound wires.
 * All wire and SWIFT transfers are held for manual review (no funds released to rail).
 */
@Service
public class WireComplianceService {

    /** Reg D–style savings outgoing transfer cap per month */
    public static final int SAVINGS_MONTHLY_OUTGOING_LIMIT = 6;

    /** Typical monthly wire volume used for spike detection (demo profile) */
    private static final BigDecimal BASELINE_MONTHLY_WIRE_USD = new BigDecimal("500");

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private AmlRiskService amlRiskService;

    public WireHoldDecision evaluateWireTransfer(User user, Account sender, BigDecimal amount,
                                                 TransferType transferType, boolean international) {
        List<String> reasons = new ArrayList<>();

        if (transferType == TransferType.WIRE || transferType == TransferType.SWIFT) {
            reasons.add("Outbound wire transfers are screened under Anti-Money Laundering (AML) controls.");
            reasons.add("Sudden volume spikes, structuring patterns, or rapid repetitive wires may trigger a hold.");
        }

        if (amount != null && amount.compareTo(new BigDecimal("100000")) >= 0) {
            reasons.add("Amount materially exceeds typical monthly wire activity (e.g. $500/month baseline vs. large international wires).");
        } else if (amount != null && amount.compareTo(new BigDecimal("10000")) > 0
                && amount.compareTo(BASELINE_MONTHLY_WIRE_USD.multiply(new BigDecimal("20"))) > 0) {
            reasons.add("Transfer volume spike detected relative to your recent wire profile.");
        }

        if (international) {
            reasons.add("International / SWIFT destinations may require enhanced due diligence and OFAC screening.");
        }

        if (sender.getProductKey() == ProductKey.SAVINGS) {
            LocalDateTime monthStart = LocalDateTime.now()
                    .with(TemporalAdjusters.firstDayOfMonth())
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            long outgoingThisMonth = transferRepository.countByFromAccountAndCreatedAtAfterAndTransferTypeIn(
                    sender, monthStart,
                    Arrays.asList(TransferType.WIRE, TransferType.SWIFT, TransferType.ACH));

            if (outgoingThisMonth >= SAVINGS_MONTHLY_OUTGOING_LIMIT) {
                reasons.add("Savings withdrawal limit reached: "
                        + SAVINGS_MONTHLY_OUTGOING_LIMIT
                        + " outgoing transfers per month (Regulation D–style restriction). Further transfers may incur fees or temporary restriction.");
            } else if (outgoingThisMonth >= SAVINGS_MONTHLY_OUTGOING_LIMIT - 1) {
                reasons.add("Approaching savings outgoing limit: "
                        + (outgoingThisMonth + 1) + " of " + SAVINGS_MONTHLY_OUTGOING_LIMIT + " allowed this month.");
            }
        }

        amlRiskService.analyzeTransfer(user, amount, sender.getAccountNumber());

        String summary = "Wire initiated — placed on compliance hold. Funds have not been released. "
                + "Manual treasury review is required before settlement.";

        return new WireHoldDecision(true, reasons, summary);
    }

    public static final class WireHoldDecision {
        private final boolean hold;
        private final List<String> reasons;
        private final String summaryMessage;

        public WireHoldDecision(boolean hold, List<String> reasons, String summaryMessage) {
            this.hold = hold;
            this.reasons = reasons != null ? reasons : new ArrayList<String>();
            this.summaryMessage = summaryMessage;
        }

        public boolean isHold() {
            return hold;
        }

        public List<String> getReasons() {
            return reasons;
        }

        public String getSummaryMessage() {
            return summaryMessage;
        }
    }
}
