package opc.models.simulator;

import opc.models.shared.CurrencyAmount;

public class SimulateOctMerchantRefundModel {
    private String merchantName;
    private CurrencyAmount transactionAmount;

    public SimulateOctMerchantRefundModel(final String merchantName, final CurrencyAmount transactionAmount) {
        this.merchantName = merchantName;
        this.transactionAmount = transactionAmount;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public SimulateOctMerchantRefundModel setMerchantName(String merchantName) {
        this.merchantName = merchantName;
        return this;
    }

    public CurrencyAmount getTransactionAmount() {
        return transactionAmount;
    }

    public SimulateOctMerchantRefundModel setTransactionAmount(CurrencyAmount transactionAmount) {
        this.transactionAmount = transactionAmount;
        return this;
    }
}
