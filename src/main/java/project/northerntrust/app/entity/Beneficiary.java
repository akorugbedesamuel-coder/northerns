package project.northerntrust.app.entity;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import project.northerntrust.app.entity.enums.BeneficiaryStatus;
import project.northerntrust.app.entity.enums.BeneficiaryType;
import project.northerntrust.app.entity.enums.TrustLevel;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "beneficiaries")
public class Beneficiary {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "beneficiary_code", unique = true, length = 20)
    private String beneficiaryCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "beneficiary_type", nullable = false)
    private BeneficiaryType beneficiaryType;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(length = 100)
    private String relationship;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_name", length = 150)
    private String accountName;

    @Column(length = 100)
    private String nickname;

    @Column(name = "routing_or_swift", length = 50)
    private String routingOrSwift;

    @Column(length = 100)
    private String country;

    @Column(name = "destination_user_id", length = 50)
    private String destinationUserId;

    @Column(name = "email_or_phone", length = 150)
    private String emailOrPhone;

    @Column(name = "credit_account_id", length = 50)
    private String creditAccountId;

    @Column(name = "portfolio_id", length = 50)
    private String portfolioId;

    @Column(name = "destination_details_json", columnDefinition = "TEXT")
    private String destinationDetailsJson;

    @Column(name = "single_limit", precision = 15, scale = 2)
    private BigDecimal singleLimit = BigDecimal.ZERO;

    @Column(name = "daily_limit", precision = 15, scale = 2)
    private BigDecimal dailyLimit = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BeneficiaryStatus status = BeneficiaryStatus.ACTIVE;

    @Column(name = "is_trusted")
    private Boolean trusted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_level", length = 20)
    private TrustLevel trustLevel = TrustLevel.New;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    public Beneficiary() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBeneficiaryCode() {
        return beneficiaryCode;
    }

    public void setBeneficiaryCode(String beneficiaryCode) {
        this.beneficiaryCode = beneficiaryCode;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BeneficiaryType getBeneficiaryType() {
        return beneficiaryType;
    }

    public void setBeneficiaryType(BeneficiaryType beneficiaryType) {
        this.beneficiaryType = beneficiaryType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRoutingOrSwift() {
        return routingOrSwift;
    }

    public void setRoutingOrSwift(String routingOrSwift) {
        this.routingOrSwift = routingOrSwift;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDestinationUserId() {
        return destinationUserId;
    }

    public void setDestinationUserId(String destinationUserId) {
        this.destinationUserId = destinationUserId;
    }

    public String getEmailOrPhone() {
        return emailOrPhone;
    }

    public void setEmailOrPhone(String emailOrPhone) {
        this.emailOrPhone = emailOrPhone;
    }

    public String getCreditAccountId() {
        return creditAccountId;
    }

    public void setCreditAccountId(String creditAccountId) {
        this.creditAccountId = creditAccountId;
    }

    public String getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    public String getDestinationDetailsJson() {
        return destinationDetailsJson;
    }

    public void setDestinationDetailsJson(String destinationDetailsJson) {
        this.destinationDetailsJson = destinationDetailsJson;
    }

    public BigDecimal getSingleLimit() {
        return singleLimit;
    }

    public void setSingleLimit(BigDecimal singleLimit) {
        this.singleLimit = singleLimit;
    }

    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public BeneficiaryStatus getStatus() {
        return status;
    }

    public void setStatus(BeneficiaryStatus status) {
        this.status = status;
    }

    public Boolean getTrusted() {
        return trusted;
    }

    public void setTrusted(Boolean trusted) {
        this.trusted = trusted;
    }

    public TrustLevel getTrustLevel() {
        return trustLevel;
    }

    public void setTrustLevel(TrustLevel trustLevel) {
        this.trustLevel = trustLevel;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
