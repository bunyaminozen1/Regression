package opc.models.multi.directdebit;

import opc.models.shared.CurrencyAmountResponse;
import opc.models.shared.TypeIdResponseModel;

public class GetCollectionModel {

    private String id;
    private String mandateId;
    private CurrencyAmountResponse amount;
    private String state;
    private String merchantName;
    private String merchantNumber;
    private String merchantReference;
    private Long settlementTimestamp;
    private TypeIdResponseModel instrumentId;

    public String getId() {
        return id;
    }

    public GetCollectionModel setId(String id) {
        this.id = id;
        return this;
    }

    public String getMandateId() {
        return mandateId;
    }

    public GetCollectionModel setMandateId(String mandateId) {
        this.mandateId = mandateId;
        return this;
    }

    public CurrencyAmountResponse getAmount() {
        return amount;
    }

    public GetCollectionModel setAmount(CurrencyAmountResponse amount) {
        this.amount = amount;
        return this;
    }

    public String getState() {
        return state;
    }

    public GetCollectionModel setState(String state) {
        this.state = state;
        return this;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public GetCollectionModel setMerchantName(String merchantName) {
        this.merchantName = merchantName;
        return this;
    }

    public String getMerchantNumber() {
        return merchantNumber;
    }

    public GetCollectionModel setMerchantNumber(String merchantNumber) {
        this.merchantNumber = merchantNumber;
        return this;
    }

    public String getMerchantReference() {
        return merchantReference;
    }

    public GetCollectionModel setMerchantReference(String merchantReference) {
        this.merchantReference = merchantReference;
        return this;
    }

    public Long getSettlementTimestamp() {
        return settlementTimestamp;
    }

    public GetCollectionModel setSettlementTimestamp(Long settlementTimestamp) {
        this.settlementTimestamp = settlementTimestamp;
        return this;
    }

    public TypeIdResponseModel getInstrumentId() {
        return instrumentId;
    }

    public GetCollectionModel setInstrumentId(TypeIdResponseModel instrumentId) {
        this.instrumentId = instrumentId;
        return this;
    }
}
