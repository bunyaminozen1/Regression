package opc.models.webhook;

import java.util.LinkedHashMap;


public class WebhookManagedAccountDepositEventModel {
    private LinkedHashMap<String, String> id;
    private LinkedHashMap<String, String> owner;
    private String transactionId;
    private LinkedHashMap<String, String> transactionAmount;
    private String transactionTimestamp;
    private String emailAddress;
    private String state;
    private String senderName;
    private String senderIban;
    private String senderReference;

    public LinkedHashMap<String, String> getId() {
        return id;
    }

    public WebhookManagedAccountDepositEventModel setId(LinkedHashMap<String, String> id) {
        this.id = id;
        return this;
    }

    public LinkedHashMap<String, String> getOwner() {
        return owner;
    }

    public WebhookManagedAccountDepositEventModel setOwner(LinkedHashMap<String, String> owner) {
        this.owner = owner;
        return this;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public WebhookManagedAccountDepositEventModel setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public LinkedHashMap<String, String> getTransactionAmount() {
        return transactionAmount;
    }

    public WebhookManagedAccountDepositEventModel setTransactionAmount(LinkedHashMap<String, String> transactionAmount) {
        this.transactionAmount = transactionAmount;
        return this;
    }

    public String getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public WebhookManagedAccountDepositEventModel setTransactionTimestamp(String transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
        return this;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public WebhookManagedAccountDepositEventModel setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }

    public String getState() {
        return state;
    }

    public WebhookManagedAccountDepositEventModel setState(String state) {
        this.state = state;
        return this;
    }

    public String getSenderName() {
        return senderName;
    }

    public WebhookManagedAccountDepositEventModel setSenderName(String senderName) {
        this.senderName = senderName;
        return this;
    }

    public String getSenderIban() {
        return senderIban;
    }

    public WebhookManagedAccountDepositEventModel setSenderIban(String senderIban) {
        this.senderIban = senderIban;
        return this;
    }

    public String getSenderReference() {
        return senderReference;
    }

    public WebhookManagedAccountDepositEventModel setSenderReference(String senderReference) {
        this.senderReference = senderReference;
        return this;
    }
}
