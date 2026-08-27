package project.northerntrust.app.entity;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transfer_details")
public class TransferDetail {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", nullable = false)
    private Transfer transfer;

    @Column(name = "bank_name", length = 150)
    private String bankName;

    @Column(name = "bank_code", length = 50)
    private String bankCode;

    @Column(name = "swift_code", length = 20)
    private String swiftCode;

    @Column(name = "routing_number", length = 20)
    private String routingNumber;

    @Column(length = 34)
    private String iban;

    @Column(name = "sort_code", length = 20)
    private String sortCode;

    @Column(name = "recipient_name", length = 150)
    private String recipientName;

    @Column(name = "recipient_account", length = 50)
    private String recipientAccount;

    @Column(name = "recipient_address", columnDefinition = "TEXT")
    private String recipientAddress;

    @Column(length = 100)
    private String country;

    // ACH-specific metadata (standard NACHA fields)
    @Column(name = "ach_direction", length = 10)
    private String achDirection;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "sec_code", length = 10)
    private String secCode;

    @Column(name = "scheduling", length = 20)
    private String scheduling;

    @Column(name = "same_day")
    private Boolean sameDay;

    @Column(name = "expected_settlement_date")
    private LocalDate expectedSettlementDate;

    // Wire-specific metadata
    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "fx_rate", precision = 18, scale = 6)
    private java.math.BigDecimal fxRate;

    @Column(name = "usd_equivalent", precision = 18, scale = 2)
    private java.math.BigDecimal usdEquivalent;

    @Column(name = "fee", precision = 15, scale = 2)
    private java.math.BigDecimal fee;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    public TransferDetail() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public void setTransfer(Transfer transfer) {
        this.transfer = transfer;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
    }

    public String getRoutingNumber() {
        return routingNumber;
    }

    public void setRoutingNumber(String routingNumber) {
        this.routingNumber = routingNumber;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getSortCode() {
        return sortCode;
    }

    public void setSortCode(String sortCode) {
        this.sortCode = sortCode;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientAccount() {
        return recipientAccount;
    }

    public void setRecipientAccount(String recipientAccount) {
        this.recipientAccount = recipientAccount;
    }

    public String getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getAchDirection() {
        return achDirection;
    }

    public void setAchDirection(String achDirection) {
        this.achDirection = achDirection;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getSecCode() {
        return secCode;
    }

    public void setSecCode(String secCode) {
        this.secCode = secCode;
    }

    public String getScheduling() {
        return scheduling;
    }

    public void setScheduling(String scheduling) {
        this.scheduling = scheduling;
    }

    public Boolean getSameDay() {
        return sameDay;
    }

    public void setSameDay(Boolean sameDay) {
        this.sameDay = sameDay;
    }

    public LocalDate getExpectedSettlementDate() {
        return expectedSettlementDate;
    }

    public void setExpectedSettlementDate(LocalDate expectedSettlementDate) {
        this.expectedSettlementDate = expectedSettlementDate;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public java.math.BigDecimal getFxRate() {
        return fxRate;
    }

    public void setFxRate(java.math.BigDecimal fxRate) {
        this.fxRate = fxRate;
    }

    public java.math.BigDecimal getUsdEquivalent() {
        return usdEquivalent;
    }

    public void setUsdEquivalent(java.math.BigDecimal usdEquivalent) {
        this.usdEquivalent = usdEquivalent;
    }

    public java.math.BigDecimal getFee() {
        return fee;
    }

    public void setFee(java.math.BigDecimal fee) {
        this.fee = fee;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
