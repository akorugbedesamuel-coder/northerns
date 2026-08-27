package project.northerntrust.app.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ExternalTransferRequest {

    @NotBlank(message = "Sender account number is required")
    private String fromAccountNumber;

    @NotBlank(message = "Transfer type (ACH, SWIFT, WIRE) is required")
    private String transferType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.0", message = "Transfer amount must be greater than 0")
    private BigDecimal amount;

    private String description;

    @NotBlank(message = "Recipient bank name is required")
    private String bankName;

    private String swiftCode;
    private String routingNumber;
    private String iban;
    
    @NotBlank(message = "Recipient account/IBAN is required")
    private String recipientAccount;
    
    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    private String recipientAddress;

    /** Recipient's country (used for wire TransferDetail). */
    private String country;

    // ACH-specific fields
    /** CREDIT (push) or DEBIT (pull). */
    private String direction;

    /** ISO-8601 date (yyyy-MM-dd) the transfer should be initiated. */
    private String effectiveDate;

    /** NACHA SEC code, e.g. PPD, CCD, WEB, TEL. */
    private String secCode;

    /** Scheduling type: once, weekly, biweekly, monthly, quarterly. */
    private String scheduling;

    /** True when the user selected same-day ACH (fee applies). */
    private Boolean sameDay;

    /** Mandatory for ACH DEBIT (pull) — user authorization on file. */
    private Boolean achAuthAck;

    /** Fee quoted to the user at submission (used for audit consistency). */
    private BigDecimal fee;

    // Wire-specific fields
    /** Wire priority: Standard, SameDay, Urgent (Urgent adds a surcharge on the WIRE rail). */
    private String priority;

    /** Target currency code for SWIFT wires (e.g. EUR, GBP, JPY, CHF). */
    private String currency;

    /** FX rate quoted to the user (USD per foreign unit) for SWIFT wires. */
    private BigDecimal fxRate;

    /** USD-equivalent principal for the ledger (equals amount for USD wires). */
    private BigDecimal usdEquivalent;

    // Getters and Setters
    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public void setFromAccountNumber(String fromAccountNumber) {
        this.fromAccountNumber = fromAccountNumber;
    }

    public String getTransferType() {
        return transferType;
    }

    public void setTransferType(String transferType) {
        this.transferType = transferType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
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

    public String getRecipientAccount() {
        return recipientAccount;
    }

    public void setRecipientAccount(String recipientAccount) {
        this.recipientAccount = recipientAccount;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
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

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
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

    public Boolean getAchAuthAck() {
        return achAuthAck;
    }

    public void setAchAuthAck(Boolean achAuthAck) {
        this.achAuthAck = achAuthAck;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
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

    public BigDecimal getFxRate() {
        return fxRate;
    }

    public void setFxRate(BigDecimal fxRate) {
        this.fxRate = fxRate;
    }

    public BigDecimal getUsdEquivalent() {
        return usdEquivalent;
    }

    public void setUsdEquivalent(BigDecimal usdEquivalent) {
        this.usdEquivalent = usdEquivalent;
    }
}
