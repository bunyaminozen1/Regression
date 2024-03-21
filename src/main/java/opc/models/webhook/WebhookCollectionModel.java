package opc.models.webhook;

import opc.models.shared.CurrencyAmountResponse;
import opc.models.shared.TypeIdModel;

public class WebhookCollectionModel {

    private String id;
    private String mandateId;
    private CurrencyAmountResponse amount;
    private Long settlementTimestamp;
    private String status;
    private String merchantName;
    private String merchantNumber;
    private String merchantReference;
    private TypeIdModel instrumentId;

    public String getId() {
        return id;
    }

    public WebhookCollectionModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getMandateId() {
        return mandateId;
    }

    public WebhookCollectionModel setMandateId(String mandateId) {
        this.mandateId = mandateId;
        return this;
    }

    public CurrencyAmountResponse getAmount() {
        return amount;
    }

    public WebhookCollectionModel setAmount(CurrencyAmountResponse amount) {
        this.amount = amount;
        return this;
    }

    public Long getSettlementTimestamp() {
        return settlementTimestamp;
    }

    public WebhookCollectionModel setSettlementTimestamp(Long settlementTimestamp) {
        this.settlementTimestamp = settlementTimestamp;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public WebhookCollectionModel setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public WebhookCollectionModel setMerchantName(String merchantName) {
        this.merchantName = merchantName;
        return this;
    }

    public String getMerchantNumber() {
        return merchantNumber;
    }

    public WebhookCollectionModel setMerchantNumber(String merchantNumber) {
        this.merchantNumber = merchantNumber;
        return this;
    }

    public String getMerchantReference() {
        return merchantReference;
    }

    public WebhookCollectionModel setMerchantReference(String merchantReference) {
        this.merchantReference = merchantReference;
        return this;
    }

    public TypeIdModel getInstrumentId() {
        return instrumentId;
    }

    public WebhookCollectionModel setInstrumentId(TypeIdModel instrumentId) {
        this.instrumentId = instrumentId;
        return this;
    }
}
