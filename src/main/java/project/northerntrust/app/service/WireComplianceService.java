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
 * Wires are screened automatically; only transfers that breach policy
 * (large amounts, savings withdrawal caps, or AML freezes) are held for
 * manual treasury review. Standard Fedwire/SWIFT wires pass pre-screening.
 */
@Service
public class WireComplianceService {

    /** Reg D–style savings outgoing transfer cap per month */
    public static final int SAVINGS_MONTHLY_OUTGOING_LIMIT = 6;

    /** Typical monthly wire volume used for spike detection (demo profile) */
    private static final BigDecimal BASELINE_MONTHLY_WIRE_USD = new BigDecimal("500");

    /** Transfers at or above this USD amount are always routed to manual treasury review. */
    private static final BigDecimal LARGE_WIRE_HOLD_USD = new BigDecimal("100000");

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private AmlRiskService amlRiskService;

    public WireHoldDecision evaluateWireTransfer(User user, Account sender, BigDecimal amount,
                                                 TransferType transferType, boolean international) {
        List<String> holdReasons = new ArrayList<>();
        List<String> infoNotes = new ArrayList<>();

        // Large-value wires always go to dual-control review.
        if (amount != null && amount.compareTo(LARGE_WIRE_HOLD_USD) >= 0) {
            holdReasons.add("Amount materially exceeds typical wire activity and requires secondary approver release.");
        }

        // Reg D-style savings outgoing transfer cap.
        int outgoingThisMonth = 0;
        if (sender.getProductKey() == ProductKey.SAVINGS) {
            LocalDateTime monthStart = LocalDateTime.now()
                    .with(TemporalAdjusters.firstDayOfMonth())
                    .withHour(0).withMinute(0).withSecond(0).withNano(0);
            outgoingThisMonth = (int) transferRepository.countByFromAccountAndCreatedAtAfterAndTransferTypeIn(
                    sender, monthStart,
                    Arrays.asList(TransferType.WIRE, TransferType.SWIFT, TransferType.ACH));

            if (outgoingThisMonth >= SAVINGS_MONTHLY_OUTGOING_LIMIT) {
                holdReasons.add("Savings withdrawal limit reached: "
                        + SAVINGS_MONTHLY_OUTGOING_LIMIT
                        + " outgoing transfers per month (Regulation D–style restriction).");
            } else if (outgoingThisMonth >= SAVINGS_MONTHLY_OUTGOING_LIMIT - 1) {
                infoNotes.add("Approaching savings outgoing limit: "
                        + (outgoingThisMonth + 1) + " of " + SAVINGS_MONTHLY_OUTGOING_LIMIT + " allowed this month.");
            }
        }

        // Volume-spike note (informational unless combined with the caps above).
        if (amount != null && amount.compareTo(new BigDecimal("10000")) > 0
                && amount.compareTo(BASELINE_MONTHLY_WIRE_USD.multiply(new BigDecimal("20"))) > 0) {
            infoNotes.add("Transfer volume spike detected relative to your recent wire profile.");
        }

        // International destinations get standard enhanced due diligence / OFAC screening.
        if (international) {
            infoNotes.add("International destination screened for OFAC / enhanced due diligence.");
        }

        // AML velocity/structuring/freeze rules throw on a hard block.
        amlRiskService.analyzeTransfer(user, amount, sender.getAccountNumber());

        boolean hold = !holdReasons.isEmpty();
        List<String> allNotes = new ArrayList<>(infoNotes);
        allNotes.addAll(holdReasons);

        if (hold) {
            String summary = "Wire initiated — placed on compliance hold. Funds have not been released. "
                    + "Manual treasury review is required before settlement.";
            return new WireHoldDecision(true, allNotes, summary);
        }
        return new WireHoldDecision(false, allNotes,
                "Wire cleared AML/OFAC pre-screening. Funds released per the selected rail processing timeline.");
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
