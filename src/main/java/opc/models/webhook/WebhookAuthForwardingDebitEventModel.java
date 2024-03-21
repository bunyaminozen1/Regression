package opc.models.webhook;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class WebhookAuthForwardingDebitEventModel {
    private String cardId;
    private String transactionId;
    private String authorisationType;
    private LinkedHashMap<String, String> sourceAmount;
    private LinkedHashMap<String, String> transactionAmount;
    private LinkedHashMap<String, String> totalTransactionCost;
    private String transactionTimestamp;
    private LinkedHashMap<String, String> merchantData;
    private LinkedHashMap<String, String> owner;
    private String cardholderPresent;
    private boolean cardPresent;
    private String authCode;
    private LinkedHashMap<String, String> forexPadding;
    private LinkedHashMap<String, String> forexFee;
    private String mode;
    private String parentManagedAccountId;
    private ArrayList<WebhookAvailableToSpendModel> availableToSpend;

    public String getCardId() { return cardId; }
    public String getTransactionId() { return transactionId; }
    public String getAuthorisationType() {return authorisationType; }
    public LinkedHashMap<String, String> getSourceAmount() { return sourceAmount; }
    public LinkedHashMap<String, String> getTransactionAmount() { return transactionAmount; }
    public LinkedHashMap<String, String> getTotalTransactionCost() { return totalTransactionCost; }
    public String getTransactionTimestamp() { return transactionTimestamp; }
    public LinkedHashMap<String, String> getMerchantData() { return merchantData; }
    public LinkedHashMap<String, String> getOwner() { return owner; }
    public String getCardholderPresent() { return cardholderPresent; }
    public boolean isCardPresent() { return cardPresent; }
    public String getAuthCode() { return authCode; }
    public LinkedHashMap<String, String> getForexPadding() { return forexPadding; }
    public LinkedHashMap<String, String> getForexFee() { return forexFee; }
    public String getMode() { return mode; }
    public String getParentManagedAccountId() { return parentManagedAccountId; }
    public ArrayList<WebhookAvailableToSpendModel> getAvailableToSpend() { return availableToSpend; }
}

