package opc.models.simulator;

import opc.models.shared.CurrencyAmount;

public class SimulateCardPurchaseByIdModel {
    private final String merchantName;
    private final String merchantId;
    private final String merchantCategoryCode;
    private final CurrencyAmount transactionAmount;
    private final String transactionCountry;
    private final Long forexFee;
    private final Long forexPadding;
    private final boolean cardPresent;
    private final String cardHolderPresent;
    private final Boolean atmWithdrawal;
    private final AdditionalMerchantDataModel additionalMerchantData;
    private final Boolean initiateBiometricThreeDSecure;

    public SimulateCardPurchaseByIdModel(final Builder builder) {
        this.merchantName = builder.merchantName;
        this.merchantId = builder.merchantId;
        this.merchantCategoryCode = builder.merchantCategoryCode;
        this.transactionAmount = builder.transactionAmount;
        this.transactionCountry = builder.transactionCountry;
        this.forexFee = builder.forexFee;
        this.forexPadding = builder.forexPadding;
        this.cardPresent = builder.cardPresent;
        this.cardHolderPresent = builder.cardHolderPresent;
        this.atmWithdrawal = builder.atmWithdrawal;
        this.additionalMerchantData = builder.additionalMerchantData;
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

    public Long getForexFee() {
        return forexFee;
    }

    public Long getForexPadding() {
        return forexPadding;
    }

    public boolean isCardPresent() {return cardPresent; }

    public String getCardHolderPresent() { return cardHolderPresent; }

    public Boolean getAtmWithdrawal() {
        return atmWithdrawal;
    }

    public AdditionalMerchantDataModel getAdditionalMerchantData() { return additionalMerchantData; }

    public Boolean isInitiateBiometricThreeDSecure() { return initiateBiometricThreeDSecure; }


    public static class Builder {
        private String merchantName;
        private String merchantId;
        private String merchantCategoryCode;
        private CurrencyAmount transactionAmount;
        private String transactionCountry;
        private Long forexFee;
        private Long forexPadding;
        private boolean cardPresent;
        private String cardHolderPresent;
        private Boolean atmWithdrawal;
        private AdditionalMerchantDataModel additionalMerchantData;
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

        public Builder setForexFee(Long forexFee) {
            this.forexFee = forexFee;
            return this;
        }

        public Builder setForexPadding(Long forexPadding) {
            this.forexPadding = forexPadding;
            return this;
        }

        public Builder setCardPresent(boolean cardPresent) {
            this.cardPresent = cardPresent;
            return this;
        }

        public Builder setCardHolderPresent(String cardHolderPresent) {
            this.cardHolderPresent = cardHolderPresent;
            return this;
        }

        public Builder setAtmWithdrawal(Boolean atmWithdrawal) {
            this.atmWithdrawal = atmWithdrawal;
            return this;
        }

        public Builder setAdditionalMerchantData(AdditionalMerchantDataModel additionalMerchantData) {
            this.additionalMerchantData = additionalMerchantData;
            return this;
        }

        public Builder setInitiateBiometricThreeDSecure(Boolean initiateBiometricThreeDSecure) {
            this.initiateBiometricThreeDSecure = initiateBiometricThreeDSecure;
            return this;
        }

        public SimulateCardPurchaseByIdModel build() { return new SimulateCardPurchaseByIdModel(this); }
    }

    public static Builder builder(){
        return new Builder()
                .setMerchantName("Amazon IT")
                .setTransactionCountry("MLT")
                .setMerchantId("123456789")
                .setForexFee(0L)
                .setForexPadding(0L);
    }
}
