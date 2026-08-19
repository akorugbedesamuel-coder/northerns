package project.northerntrust.app.entity;

import org.hibernate.annotations.GenericGenerator;
import project.northerntrust.app.entity.enums.ProductKey;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "statement_lines")
public class StatementLine {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_key", length = 20)
    private ProductKey productKey;

    @Column(nullable = false)
    private LocalDate lineDate;

    @Column(length = 255)
    private String description;

    @Column(length = 100)
    private String source;

    @Column(name = "line_type", length = 50)
    private String lineType;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(length = 30)
    private String status;

    @Column(length = 30)
    private String channel;

    @Column(length = 100)
    private String asset;

    @Column(precision = 15, scale = 4)
    private BigDecimal units;

    @Column(precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "rate_label", length = 20)
    private String rateLabel;

    @Column(precision = 15, scale = 2)
    private BigDecimal charged;

    @Column(precision = 15, scale = 2)
    private BigDecimal payment;

    @Column(name = "credit_available", precision = 15, scale = 2)
    private BigDecimal creditAvailable;

    @Column(name = "transfer_reference", length = 50)
    private String transferReference;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ProductKey getProductKey() {
        return productKey;
    }

    public void setProductKey(ProductKey productKey) {
        this.productKey = productKey;
    }

    public LocalDate getLineDate() {
        return lineDate;
    }

    public void setLineDate(LocalDate lineDate) {
        this.lineDate = lineDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public BigDecimal getUnits() {
        return units;
    }

    public void setUnits(BigDecimal units) {
        this.units = units;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getRateLabel() {
        return rateLabel;
    }

    public void setRateLabel(String rateLabel) {
        this.rateLabel = rateLabel;
    }

    public BigDecimal getCharged() {
        return charged;
    }

    public void setCharged(BigDecimal charged) {
        this.charged = charged;
    }

    public BigDecimal getPayment() {
        return payment;
    }

    public void setPayment(BigDecimal payment) {
        this.payment = payment;
    }

    public BigDecimal getCreditAvailable() {
        return creditAvailable;
    }

    public void setCreditAvailable(BigDecimal creditAvailable) {
        this.creditAvailable = creditAvailable;
    }

    public String getTransferReference() {
        return transferReference;
    }

    public void setTransferReference(String transferReference) {
        this.transferReference = transferReference;
    }
}
