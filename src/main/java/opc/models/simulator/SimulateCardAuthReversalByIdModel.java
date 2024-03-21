package opc.models.simulator;

import opc.enums.opc.TestMerchant;
import opc.helpers.ModelHelper;
import opc.models.shared.CurrencyAmount;

public class SimulateCardAuthReversalByIdModel {
    private final String merchantName;
    private final Long relatedAuthorisationId;
    private final CurrencyAmount transactionAmount;

    public SimulateCardAuthReversalByIdModel(final Builder builder) {

        this.merchantName = builder.merchantName;
        this.relatedAuthorisationId = builder.relatedAuthorisationId;
        this.transactionAmount = builder.transactionAmount;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public Long getRelatedAuthorisationId() {
        return relatedAuthorisationId;
    }

    public CurrencyAmount getTransactionAmount() {
        return transactionAmount;
    }

    public static class Builder {
        private String merchantName;
        private Long relatedAuthorisationId;
        private CurrencyAmount transactionAmount;

        public Builder setMerchantName(String merchantName) {
            this.merchantName = merchantName;
            return this;
        }

        public Builder setRelatedAuthorisationId(Long relatedAuthorisationId) {
            this.relatedAuthorisationId = relatedAuthorisationId;
            return this;
        }

        public Builder setTransactionAmount(CurrencyAmount transactionAmount) {
            this.transactionAmount = transactionAmount;
            return this;
        }

        public SimulateCardAuthReversalByIdModel build() { return new SimulateCardAuthReversalByIdModel(this); }
    }

    public static Builder DefaultCardAuthModel(final CurrencyAmount purchaseAmount){
        final TestMerchant merchant = ModelHelper.merchantDetails(purchaseAmount.getCurrency());

        return new Builder()
                .setMerchantName(merchant.getMerchantName())
                .setTransactionAmount(purchaseAmount);
    }
}
