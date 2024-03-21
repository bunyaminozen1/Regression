package opc.models.innovator;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;

public class RevenueResponseModel {

    //@JsonIgnore
    private LinkedHashMap<String, String> txId;
    //@JsonIgnore
    private LinkedHashMap<String, String> amount;
    //@JsonIgnore
    private RevenueFeeModel fee;
    //@JsonIgnore
    private String txTimestamp;

    public LinkedHashMap<String, String> getTxId() {
        return txId;
    }

    public RevenueResponseModel setTxId(LinkedHashMap<String, String> txId) {
        this.txId = txId;
        return this;
    }

    public LinkedHashMap<String, String> getAmount() {
        return amount;
    }

    public RevenueResponseModel setAmount(LinkedHashMap<String, String> amount) {
        this.amount = amount;
        return this;
    }

    public RevenueFeeModel getFee() {
        return fee;
    }

    public RevenueResponseModel setFee(RevenueFeeModel fee) {
        this.fee = fee;
        return this;
    }

    public String getTxTimestamp() {
        return txTimestamp;
    }

    public RevenueResponseModel setTxTimestamp(String txTimestamp) {
        this.txTimestamp = txTimestamp;
        return this;
    }
}