package project.northerntrust.app.entity;

import org.hibernate.annotations.GenericGenerator;
import project.northerntrust.app.entity.enums.PaymentRailType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment_rails")
public class PaymentRail {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "rail_type", unique = true)
    private PaymentRailType railType;

    @Column(name = "processing_time", length = 50)
    private String processingTime;

    @Column(name = "max_limit", precision = 15, scale = 2)
    private BigDecimal maxLimit;

    @Column(name = "fee_fixed", precision = 15, scale = 2)
    private BigDecimal feeFixed;

    @Column(name = "fee_percentage", precision = 7, scale = 4)
    private BigDecimal feePercentage;

    @Column(name = "supported_currencies", columnDefinition = "TEXT")
    private String supportedCurrencies;

    @Column(name = "is_active", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean isActive = true;

    public PaymentRail() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PaymentRailType getRailType() {
        return railType;
    }

    public void setRailType(PaymentRailType railType) {
        this.railType = railType;
    }

    public String getProcessingTime() {
        return processingTime;
    }

    public void setProcessingTime(String processingTime) {
        this.processingTime = processingTime;
    }

    public BigDecimal getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(BigDecimal maxLimit) {
        this.maxLimit = maxLimit;
    }

    public BigDecimal getFeeFixed() {
        return feeFixed;
    }

    public void setFeeFixed(BigDecimal feeFixed) {
        this.feeFixed = feeFixed;
    }

    public BigDecimal getFeePercentage() {
        return feePercentage;
    }

    public void setFeePercentage(BigDecimal feePercentage) {
        this.feePercentage = feePercentage;
    }

    public String getSupportedCurrencies() {
        return supportedCurrencies;
    }

    public void setSupportedCurrencies(String supportedCurrencies) {
        this.supportedCurrencies = supportedCurrencies;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
}
