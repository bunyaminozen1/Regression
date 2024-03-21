package opc.models.simulator;

import opc.models.shared.CurrencyAmount;

public class SimulateCardMerchantRefundByIdModel {
    private final String merchantName;
    private final String merchantId;
    private final String merchantCategoryCode;
    private final CurrencyAmount transactionAmount;
    private final String transactionCountry;
    private final Boolean initiateBiometricThreeDSecure;

    public SimulateCardMerchantRefundByIdModel(final Builder builder) {
        this.merchantName = builder.merchantName;
        this.merchantId = builder.merchantId;
        this.merchantCategoryCode = builder.merchantCategoryCode;
        this.transactionAmount = builder.transactionAmount;
        this.transactionCountry = builder.transactionCountry;
        this.initiateBiometricThreeDSecure = builder.initiateBiometricThreeDSecure;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getMerchantCategoryCode() {
        return merchantCategoryCode;
    }

    public CurrencyAmount getTransactionAmount() {
        return transactionAmount;
    }

    public String getTransactionCountry() {
        return transactionCountry;
    }

    public Boolean isInitiateBiometricThreeDSecure() { return initiateBiometricThreeDSecure; }

    public static class Builder {
        private String merchantName;
        private String merchantId;
        private String merchantCategoryCode;
        private CurrencyAmount transactionAmount;
        private String transactionCountry;
        private Boolean initiateBiometricThreeDSecure;

        public Builder setMerchantName(String merchantName) {
            this.merchantName = merchantName;
            return this;
        }

        public Builder setMerchantId(String merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public Builder setMerchantCategoryCode(String merchantCategoryCode) {
            this.merchantCategoryCode = merchantCategoryCode;
            return this;
        }

        public Builder setTransactionAmount(CurrencyAmount transactionAmount) {
            this.transactionAmount = transactionAmount;
            return this;
        }

        public Builder setTransactionCountry(String transactionCountry) {
            this.transactionCountry = transactionCountry;
            return this;
        }


        public Builder setInitiateBiometricThreeDSecure(Boolean initiateBiometricThreeDSecure) {
            this.initiateBiometricThreeDSecure = initiateBiometricThreeDSecure;
            return this;
        }

        public SimulateCardMerchantRefundByIdModel build() { return new SimulateCardMerchantRefundByIdModel(this); }
    }

    public static Builder builder(){
        return new Builder()
                .setMerchantName("Refundable.com")
                .setTransactionCountry("MLT");
    }
}
