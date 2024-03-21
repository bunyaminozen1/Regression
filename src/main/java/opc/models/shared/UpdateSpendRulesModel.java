package opc.models.shared;

import opc.enums.opc.LimitInterval;
import opc.models.admin.TransactionAmountModel;

import java.util.Collections;
import java.util.List;

public class UpdateSpendRulesModel {
    private final RulesModel allowedMerchantCategories;
    private final RulesModel blockedMerchantCategories;
    private final RulesModel allowedMerchantIds;
    private final RulesModel blockedMerchantIds;
    private final String allowContactless;
    private final String allowAtm;
    private final String allowECommerce;
    private final String allowCashback;
    private final String allowCreditAuthorisations;
    private final List<SpendLimitModel> spendLimit;
    private final TransactionAmountModel minTransactionAmount;
    private final TransactionAmountModel maxTransactionAmount;
    private final RulesModel allowedMerchantCountries;
    private final RulesModel blockedMerchantCountries;

    public UpdateSpendRulesModel(final Builder builder) {
        this.allowedMerchantCategories = builder.allowedMerchantCategories;
        this.blockedMerchantCategories = builder.blockedMerchantCategories;
        this.allowedMerchantIds = builder.allowedMerchantIds;
        this.blockedMerchantIds = builder.blockedMerchantIds;
        this.allowContactless = builder.allowContactless;
        this.allowAtm = builder.allowAtm;
        this.allowECommerce = builder.allowECommerce;
        this.allowCashback = builder.allowCashback;
        this.allowCreditAuthorisations = builder.allowCreditAuthorisations;
        this.spendLimit = builder.spendLimit;
        this.minTransactionAmount = builder.minTransactionAmount;
        this.maxTransactionAmount = builder.maxTransactionAmount;
        this.allowedMerchantCountries = builder.allowedMerchantCountries;
        this.blockedMerchantCountries = builder.blockedMerchantCountries;
    }

    public RulesModel getAllowedMerchantCategories() {
        return allowedMerchantCategories;
    }

    public RulesModel getBlockedMerchantCategories() {
        return blockedMerchantCategories;
    }

    public RulesModel getAllowedMerchantIds() {
        return allowedMerchantIds;
    }

    public RulesModel getBlockedMerchantIds() {
        return blockedMerchantIds;
    }

    public String getAllowContactless() {
        return allowContactless;
    }

    public String getAllowAtm() {
        return allowAtm;
    }

    public String getAllowECommerce() {
        return allowECommerce;
    }

    public String getAllowCashback() {
        return allowCashback;
    }

    public String getAllowCreditAuthorisations() {
        return allowCreditAuthorisations;
    }

    public List<SpendLimitModel> getSpendLimit() {
        return spendLimit;
    }

    public TransactionAmountModel getMinTransactionAmount() {
        return minTransactionAmount;
    }

    public TransactionAmountModel getMaxTransactionAmount() {
        return maxTransactionAmount;
    }

    public RulesModel getAllowedMerchantCountries() {
        return allowedMerchantCountries;
    }

    public RulesModel getBlockedMerchantCountries() {
        return blockedMerchantCountries;
    }

    public static class Builder {
        private RulesModel allowedMerchantCategories;
        private RulesModel blockedMerchantCategories;
        private RulesModel allowedMerchantIds;
        private RulesModel blockedMerchantIds;
        private String allowContactless;
        private String allowAtm;
        private String allowECommerce;
        private String allowCashback;
        private String allowCreditAuthorisations;
        private List<SpendLimitModel> spendLimit;
        private TransactionAmountModel minTransactionAmount;
        private TransactionAmountModel maxTransactionAmount;
        private RulesModel allowedMerchantCountries;
        private RulesModel blockedMerchantCountries;

        public Builder setAllowedMerchantCategories(RulesModel allowedMerchantCategories) {
            this.allowedMerchantCategories = allowedMerchantCategories;
            return this;
        }

        public Builder setBlockedMerchantCategories(RulesModel blockedMerchantCategories) {
            this.blockedMerchantCategories = blockedMerchantCategories;
            return this;
        }

        public Builder setAllowedMerchantIds(RulesModel allowedMerchantIds) {
            this.allowedMerchantIds = allowedMerchantIds;
            return this;
        }

        public Builder setBlockedMerchantIds(RulesModel blockedMerchantIds) {
            this.blockedMerchantIds = blockedMerchantIds;
            return this;
        }

        public Builder setAllowContactless(String allowContactless) {
            this.allowContactless = allowContactless;
            return this;
        }

        public Builder setAllowAtm(String allowAtm) {
            this.allowAtm = allowAtm;
            return this;
        }

        public Builder setAllowECommerce(String allowECommerce) {
            this.allowECommerce = allowECommerce;
            return this;
        }

        public Builder setAllowCashback(String allowCashback) {
            this.allowCashback = allowCashback;
            return this;
        }

        public Builder setAllowCreditAuthorisations(String allowCreditAuthorisations) {
            this.allowCreditAuthorisations = allowCreditAuthorisations;
            return this;
        }

        public Builder setSpendLimit(List<SpendLimitModel> spendLimit) {
            this.spendLimit = spendLimit;
            return this;
        }

        public Builder setMinTransactionAmount(TransactionAmountModel minTransactionAmount) {
            this.minTransactionAmount = minTransactionAmount;
            return this;
        }

        public Builder setMaxTransactionAmount(TransactionAmountModel maxTransactionAmount) {
            this.maxTransactionAmount = maxTransactionAmount;
            return this;
        }

        public Builder setAllowedMerchantCountries(RulesModel allowedMerchantCountries) {
            this.allowedMerchantCountries = allowedMerchantCountries;
            return this;
        }

        public Builder setBlockedMerchantCountries(RulesModel blockedMerchantCountries) {
            this.blockedMerchantCountries = blockedMerchantCountries;
            return this;
        }

        public UpdateSpendRulesModel build() { return new UpdateSpendRulesModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder DefaultSpendRulesModel() {
        return new Builder()
                .setAllowedMerchantCategories(RulesModel.defaultRulesModel(Collections.singletonList("test")))
                .setBlockedMerchantCategories(RulesModel.defaultRulesModel(Collections.singletonList("test")))
                .setAllowedMerchantIds(RulesModel.defaultRulesModel(Collections.singletonList("test")))
                .setBlockedMerchantIds(RulesModel.defaultRulesModel(Collections.singletonList("test")))
                .setAllowContactless("TRUE")
                .setAllowAtm("FALSE")
                .setAllowECommerce("TRUE")
                .setAllowCashback("FALSE")
                .setAllowCreditAuthorisations("TRUE")
                .setMaxTransactionAmount(TransactionAmountModel.builder().setValue(10000L).setHasValue(true).build())
                .setMinTransactionAmount(TransactionAmountModel.builder().setValue(100L).setHasValue(true).build())
                .setAllowedMerchantCountries(RulesModel.defaultRulesModel(Collections.singletonList("MT")))
                .setBlockedMerchantCountries(RulesModel.defaultRulesModel(Collections.singletonList("IT")));
    }

    public static Builder fullDefaultSpendRulesModel(final String currency) {
        return new Builder()
                .setAllowedMerchantCategories(RulesModel.defaultRulesModel(Collections.singletonList("test")))
                .setBlockedMerchantCategories(RulesModel.defaultRulesModel(Collections.singletonList("test")))
                .setAllowedMerchantIds(RulesModel.defaultRulesModel(Collections.singletonList("test")))
                .setBlockedMerchantIds(RulesModel.defaultRulesModel(Collections.singletonList("test")))
                .setAllowContactless("TRUE")
                .setAllowAtm("FALSE")
                .setAllowECommerce("TRUE")
                .setAllowCashback("FALSE")
                .setAllowCreditAuthorisations("TRUE")
                .setMaxTransactionAmount(TransactionAmountModel.builder().setValue(10000L).setHasValue(true).build())
                .setMinTransactionAmount(TransactionAmountModel.builder().setValue(100L).setHasValue(true).build())
                .setAllowedMerchantCountries(RulesModel.defaultRulesModel(Collections.singletonList("MT")))
                .setBlockedMerchantCountries(RulesModel.defaultRulesModel(Collections.singletonList("IT")))
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(currency, 100L), LimitInterval.DAILY)));
    }
}