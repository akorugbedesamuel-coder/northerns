package project.northerntrust.app.dto;

import javax.validation.constraints.NotBlank;

public class BeneficiaryRequest {

    @NotBlank(message = "User account number is required")
    private String userAccountNumber;

    @NotBlank(message = "Recipient account number is required")
    private String accountNumber;

    @NotBlank(message = "Recipient bank name is required")
    private String bankName;

    @NotBlank(message = "Recipient account name is required")
    private String accountName;

    private String nickname;

    // Getters and Setters
    public String getUserAccountNumber() {
        return userAccountNumber;
    }

    public void setUserAccountNumber(String userAccountNumber) {
        this.userAccountNumber = userAccountNumber;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
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
}
