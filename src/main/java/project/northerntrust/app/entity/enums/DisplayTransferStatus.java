package project.northerntrust.app.entity.enums;

public enum DisplayTransferStatus {
    Settled,
    Processing,
    Compliance_Hold,
    OFAC_Hold,
    Returned,
    Failed,
    Pending_NACHA_Batch,
    Awaiting_Treasury_Approval,
    Awaiting_Verification,
    Blocked_OFAC_Review
}
