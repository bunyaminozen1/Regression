package opc.models.multi.outgoingwiretransfers;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthyStartVerificationNotificationModel {

    @JsonProperty("IBAN")
    private String iban;

    @JsonProperty("Amount")
    private String amount;

    @JsonProperty("Currency")
    private String currency;

    @JsonProperty("Transaction Type")
    private String transactionType;

    @JsonProperty("To")
    private String to;

    @JsonProperty("BIC")
    private String bic;

    @JsonProperty("Account Number")
    private String accountNumber;

    @JsonProperty("Sort Code")
    private String sortCode;

    @JsonProperty("Code")
    private String code;

    public String getIban() {
        return iban;
    }

    public AuthyStartVerificationNotificationModel setIban(String iban) {
        this.iban = iban;
        return this;
    }

    public String getAmount() {
        return amount;
    }

    public AuthyStartVerificationNotificationModel setAmount(String amount) {
        this.amount = amount;
        return this;
    }

    public String getCurrency() {
        return currency;
    }

    public AuthyStartVerificationNotificationModel setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public AuthyStartVerificationNotificationModel setTransactionType(String transactionType) {
        this.transactionType = transactionType;
        return this;
    }

    public String getTo() {
        return to;
    }

    public AuthyStartVerificationNotificationModel setTo(String to) {
        this.to = to;
        return this;
    }

    public String getBic() {
        return bic;
    }

    public AuthyStartVerificationNotificationModel setBic(String bic) {
        this.bic = bic;
        return this;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public AuthyStartVerificationNotificationModel setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }

    public String getSortCode() {
        return sortCode;
    }

    public AuthyStartVerificationNotificationModel setSortCode(String sortCode) {
        this.sortCode = sortCode;
        return this;
    }

    public String getCode() {
        return code;
    }

    public AuthyStartVerificationNotificationModel setCode(String code) {
        this.code = code;
        return this;
    }
}
