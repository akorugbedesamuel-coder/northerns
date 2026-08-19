package project.northerntrust.app.entity;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import project.northerntrust.app.entity.enums.AccountStatus;
import project.northerntrust.app.entity.enums.AccountType;
import project.northerntrust.app.entity.enums.ProductKey;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'USD'")
    private String currency = "USD";

    @Column(precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "available_balance", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 0.00")
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "daily_transfer_limit", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 1000000.00")
    private BigDecimal dailyTransferLimit = new BigDecimal("1000000.00");

    @Column(name = "monthly_transfer_limit", precision = 15, scale = 2, columnDefinition = "DECIMAL(15,2) DEFAULT 10000000.00")
    private BigDecimal monthlyTransferLimit = new BigDecimal("10000000.00");

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'ACTIVE'")
    private AccountStatus status = AccountStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_key", length = 20)
    private ProductKey productKey;

    @Column(name = "display_name", length = 150)
    private String displayName;

    @Column(name = "pending_amount", precision = 15, scale = 2)
    private BigDecimal pendingAmount = BigDecimal.ZERO;

    @Column(name = "apy_percent", precision = 5, scale = 2)
    private BigDecimal apyPercent;

    @Column(name = "earned_this_period", precision = 15, scale = 2)
    private BigDecimal earnedThisPeriod;

    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "amount_owed", precision = 15, scale = 2)
    private BigDecimal amountOwed;

    @Column(name = "market_value", precision = 15, scale = 2)
    private BigDecimal marketValue;

    @Column(name = "today_change", precision = 15, scale = 2)
    private BigDecimal todayChange;

    @Column(name = "today_change_pct", precision = 5, scale = 2)
    private BigDecimal todayChangePct;

    @Column(name = "roi_percent", precision = 5, scale = 2)
    private BigDecimal roiPercent;

    @Column(length = 50)
    private String badge;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "fromAccount", fetch = FetchType.LAZY)
    private List<Transfer> outgoingTransfers;

    @OneToMany(mappedBy = "toAccount", fetch = FetchType.LAZY)
    private List<Transfer> incomingTransfers;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LedgerEntry> ledgerEntries;

    public Account() {
    }

    // Getters and Setters
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

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getDailyTransferLimit() {
        return dailyTransferLimit;
    }

    public void setDailyTransferLimit(BigDecimal dailyTransferLimit) {
        this.dailyTransferLimit = dailyTransferLimit;
    }

    public BigDecimal getMonthlyTransferLimit() {
        return monthlyTransferLimit;
    }

    public void setMonthlyTransferLimit(BigDecimal monthlyTransferLimit) {
        this.monthlyTransferLimit = monthlyTransferLimit;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Transfer> getOutgoingTransfers() {
        return outgoingTransfers;
    }

    public void setOutgoingTransfers(List<Transfer> outgoingTransfers) {
        this.outgoingTransfers = outgoingTransfers;
    }

    public List<Transfer> getIncomingTransfers() {
        return incomingTransfers;
    }

    public void setIncomingTransfers(List<Transfer> incomingTransfers) {
        this.incomingTransfers = incomingTransfers;
    }

    public List<LedgerEntry> getLedgerEntries() {
        return ledgerEntries;
    }

    public void setLedgerEntries(List<LedgerEntry> ledgerEntries) {
        this.ledgerEntries = ledgerEntries;
    }

    public ProductKey getProductKey() {
        return productKey;
    }

    public void setProductKey(ProductKey productKey) {
        this.productKey = productKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public BigDecimal getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(BigDecimal pendingAmount) {
        this.pendingAmount = pendingAmount;
    }

    public BigDecimal getApyPercent() {
        return apyPercent;
    }

    public void setApyPercent(BigDecimal apyPercent) {
        this.apyPercent = apyPercent;
    }

    public BigDecimal getEarnedThisPeriod() {
        return earnedThisPeriod;
    }

    public void setEarnedThisPeriod(BigDecimal earnedThisPeriod) {
        this.earnedThisPeriod = earnedThisPeriod;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getAmountOwed() {
        return amountOwed;
    }

    public void setAmountOwed(BigDecimal amountOwed) {
        this.amountOwed = amountOwed;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }

    public BigDecimal getTodayChange() {
        return todayChange;
    }

    public void setTodayChange(BigDecimal todayChange) {
        this.todayChange = todayChange;
    }

    public BigDecimal getTodayChangePct() {
        return todayChangePct;
    }

    public void setTodayChangePct(BigDecimal todayChangePct) {
        this.todayChangePct = todayChangePct;
    }

    public BigDecimal getRoiPercent() {
        return roiPercent;
    }

    public void setRoiPercent(BigDecimal roiPercent) {
        this.roiPercent = roiPercent;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }
}
