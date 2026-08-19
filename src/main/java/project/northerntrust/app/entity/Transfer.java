package project.northerntrust.app.entity;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import project.northerntrust.app.entity.enums.DisplayTransferStatus;
import project.northerntrust.app.entity.enums.TransferDirection;
import project.northerntrust.app.entity.enums.TransferStatus;
import project.northerntrust.app.entity.enums.TransferType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false)
    private TransferType transferType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferDirection direction;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'CREATED'")
    private TransferStatus status = TransferStatus.CREATED;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "display_status", length = 40)
    private DisplayTransferStatus displayStatus;

    @Column(name = "counterparty_name", length = 200)
    private String counterpartyName;

    @Column(name = "source_account_label", length = 100)
    private String sourceAccountLabel;

    @Column(name = "usd_equivalent", precision = 15, scale = 2)
    private BigDecimal usdEquivalent;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "pending_approval")
    private Boolean pendingApproval = false;

    @Column(name = "approval_country", length = 100)
    private String approvalCountry;

    @Column(name = "compliance_flags_json", columnDefinition = "TEXT")
    private String complianceFlagsJson;

    @Column(name = "compliance_json", columnDefinition = "TEXT")
    private String complianceJson;

    @Column(name = "approval_history_json", columnDefinition = "TEXT")
    private String approvalHistoryJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "transfer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TransferDetail transferDetail;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransferStatusLog> statusLogs;

    @OneToOne(mappedBy = "transfer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private TransferFee transferFee;

    @OneToOne(mappedBy = "transfer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Settlement settlement;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LedgerEntry> ledgerEntries;

    public Transfer() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Account getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(Account fromAccount) {
        this.fromAccount = fromAccount;
    }

    public Account getToAccount() {
        return toAccount;
    }

    public void setToAccount(Account toAccount) {
        this.toAccount = toAccount;
    }

    public TransferType getTransferType() {
        return transferType;
    }

    public void setTransferType(TransferType transferType) {
        this.transferType = transferType;
    }

    public TransferDirection getDirection() {
        return direction;
    }

    public void setDirection(TransferDirection direction) {
        this.direction = direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public TransferDetail getTransferDetail() {
        return transferDetail;
    }

    public void setTransferDetail(TransferDetail transferDetail) {
        this.transferDetail = transferDetail;
    }

    public List<TransferStatusLog> getStatusLogs() {
        return statusLogs;
    }

    public void setStatusLogs(List<TransferStatusLog> statusLogs) {
        this.statusLogs = statusLogs;
    }

    public TransferFee getTransferFee() {
        return transferFee;
    }

    public void setTransferFee(TransferFee transferFee) {
        this.transferFee = transferFee;
    }

    public Settlement getSettlement() {
        return settlement;
    }

    public void setSettlement(Settlement settlement) {
        this.settlement = settlement;
    }

    public List<LedgerEntry> getLedgerEntries() {
        return ledgerEntries;
    }

    public void setLedgerEntries(List<LedgerEntry> ledgerEntries) {
        this.ledgerEntries = ledgerEntries;
    }

    public DisplayTransferStatus getDisplayStatus() {
        return displayStatus;
    }

    public void setDisplayStatus(DisplayTransferStatus displayStatus) {
        this.displayStatus = displayStatus;
    }

    public String getCounterpartyName() {
        return counterpartyName;
    }

    public void setCounterpartyName(String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }

    public String getSourceAccountLabel() {
        return sourceAccountLabel;
    }

    public void setSourceAccountLabel(String sourceAccountLabel) {
        this.sourceAccountLabel = sourceAccountLabel;
    }

    public BigDecimal getUsdEquivalent() {
        return usdEquivalent;
    }

    public void setUsdEquivalent(BigDecimal usdEquivalent) {
        this.usdEquivalent = usdEquivalent;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Boolean getPendingApproval() {
        return pendingApproval;
    }

    public void setPendingApproval(Boolean pendingApproval) {
        this.pendingApproval = pendingApproval;
    }

    public String getApprovalCountry() {
        return approvalCountry;
    }

    public void setApprovalCountry(String approvalCountry) {
        this.approvalCountry = approvalCountry;
    }

    public String getComplianceFlagsJson() {
        return complianceFlagsJson;
    }

    public void setComplianceFlagsJson(String complianceFlagsJson) {
        this.complianceFlagsJson = complianceFlagsJson;
    }

    public String getComplianceJson() {
        return complianceJson;
    }

    public void setComplianceJson(String complianceJson) {
        this.complianceJson = complianceJson;
    }

    public String getApprovalHistoryJson() {
        return approvalHistoryJson;
    }

    public void setApprovalHistoryJson(String approvalHistoryJson) {
        this.approvalHistoryJson = approvalHistoryJson;
    }
}
