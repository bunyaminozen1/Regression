package opc.models.simulator;

import opc.enums.opc.TestMerchant;
import opc.helpers.ModelHelper;
import opc.models.shared.CurrencyAmount;

import java.time.Instant;

public class SimulateCardAuthModel {

    private final String merchantName;
    private final String merchantId;
    private final String merchantCategoryCode;
    private final CurrencyAmount transactionAmount;
    private final Long relatedAuthorisationId;
    private final Long authTransactionTimestamp;
    private final boolean isOverruled;
    private final int overrulingNotificationDelaySeconds;
    private final String transactionCountry;
    private final String invalidAuthCode;

    public SimulateCardAuthModel(final Builder builder) {
        this.merchantName = builder.merchantName;
        this.merchantId = builder.merchantId;
        this.merchantCategoryCode = builder.merchantCategoryCode;
        this.transactionAmount = builder.transactionAmount;
        this.relatedAuthorisationId = builder.relatedAuthorisationId;
        this.authTransactionTimestamp = builder.authTransactionTimestamp;
        this.isOverruled = builder.isOverruled;
        this.overrulingNotificationDelaySeconds = builder.overrulingNotificationDelaySeconds;
        this.transactionCountry = builder.transactionCountry;
        this.invalidAuthCode = builder.invalidAuthCode;
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

    public Long getRelatedAuthorisationId() {
        return relatedAuthorisationId;
    }

    public Long getAuthTransactionTimestamp() {
        return authTransactionTimestamp;
    }

    public boolean getIsOverruled() {
        return isOverruled;
    }

    public int getOverrulingNotificationDelaySeconds() {
        return overrulingNotificationDelaySeconds;
    }

    public String getTransactionCountry() {
        return transactionCountry;
    }

    public String getInvalidAuthCode() {
        return invalidAuthCode;
    }

    public static class Builder{

        private String merchantName;
        private String merchantId;
        private String merchantCategoryCode;
        private CurrencyAmount transactionAmount;
        private Long relatedAuthorisationId;
        private Long authTransactionTimestamp;
        private boolean isOverruled;
        private int overrulingNotificationDelaySeconds;
        private String transactionCountry;
        private String invalidAuthCode;

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

        public Builder setRelatedAuthorisationId(Long relatedAuthorisationId) {
            this.relatedAuthorisationId = relatedAuthorisationId;
            return this;
        }

        public Builder setAuthTransactionTimestamp(Long authTransactionTimestamp) {
            this.authTransactionTimestamp = authTransactionTimestamp;
            return this;
        }

        public Builder setIsOverruled(boolean isOverruled) {
            this.isOverruled = isOverruled;
            return this;
        }

        public Builder setOverrulingNotificationDelaySeconds(int overrulingNotificationDelaySeconds) {
            this.overrulingNotificationDelaySeconds = overrulingNotificationDelaySeconds;
            return this;
        }

        public Builder setTransactionCountry(String transactionCountry) {
            this.transactionCountry = transactionCountry;
            return this;
        }

        public Builder setInvalidAuthCode(String invalidAuthCode) {
            this.invalidAuthCode = invalidAuthCode;
            return this;
        }

        public SimulateCardAuthModel build() { return new SimulateCardAuthModel(this); }
    }

    public static Builder DefaultCardAuthModel(final CurrencyAmount purchaseAmount){
        final TestMerchant merchant = ModelHelper.merchantDetails(purchaseAmount.getCurrency());

        return new Builder()
                .setMerchantName(merchant.getMerchantName())
                .setTransactionAmount(purchaseAmount)
                .setAuthTransactionTimestamp(Instant.now().toEpochMilli())
                .setTransactionCountry("MLT");
    }
}
