package opc.models.innovator;

import opc.enums.opc.LimitInterval;
import opc.models.admin.TransactionAmountModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.SpendLimitModel;
import opc.models.shared.UpdateSpendRulesModel;

import java.util.Collections;
import java.util.List;

public class SpendRulesModel {
    private final List<String> allowedMerchantCategories;
    private final List<String> blockedMerchantCategories;
    private final List<String> allowedMerchantIds;
    private final List<String> blockedMerchantIds;
    private final String allowContactless;
    private final String allowAtm;
    private final String allowECommerce;
    private final String allowCashback;
    private final String allowCreditAuthorisations;
    private final List<SpendLimitModel> spendLimit;
    private final TransactionAmountModel minTransactionAmount;
    private final TransactionAmountModel maxTransactionAmount;
    private final List<String> allowedMerchantCountries;
    private final List<String> blockedMerchantCountries;

    public SpendRulesModel(final Builder builder) {
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

    public List<String> getAllowedMerchantCategories() {
        return allowedMerchantCategories;
    }

    public List<String> getBlockedMerchantCategories() {
        return blockedMerchantCategories;
    }

    public List<String> getAllowedMerchantIds() {
        return allowedMerchantIds;
    }

    public List<String> getBlockedMerchantIds() {
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

    public List<String> getAllowedMerchantCountries() {
        return allowedMerchantCountries;
    }

    public List<String> getBlockedMerchantCountries() {
        return blockedMerchantCountries;
    }

    public static class Builder {
        private List<String> allowedMerchantCategories;
        private List<String> blockedMerchantCategories;
        private List<String> allowedMerchantIds;
        private List<String> blockedMerchantIds;
        private String allowContactless;
        private String allowAtm;
        private String allowECommerce;
        private String allowCashback;
        private String allowCreditAuthorisations;
        private List<SpendLimitModel> spendLimit;
        private TransactionAmountModel minTransactionAmount;
        private TransactionAmountModel maxTransactionAmount;
        private List<String> allowedMerchantCountries;
        private List<String> blockedMerchantCountries;

        public Builder setAllowedMerchantCategories(List<String> allowedMerchantCategories) {
            this.allowedMerchantCategories = allowedMerchantCategories;
            return this;
        }

        public Builder setBlockedMerchantCategories(List<String> blockedMerchantCategories) {
            this.blockedMerchantCategories = blockedMerchantCategories;
            return this;
        }

        public Builder setAllowedMerchantIds(List<String> allowedMerchantIds) {
            this.allowedMerchantIds = allowedMerchantIds;
            return this;
        }

        public Builder setBlockedMerchantIds(List<String> blockedMerchantIds) {
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

        public Builder setAllowedMerchantCountries(List<String> allowedMerchantCountries) {
            this.allowedMerchantCountries = allowedMerchantCountries;
            return this;
        }

        public Builder setBlockedMerchantCountries(List<String> blockedMerchantCountries) {
            this.blockedMerchantCountries = blockedMerchantCountries;
            return this;
        }

        public SpendRulesModel build() { return new SpendRulesModel(this); }
    }

    public static SpendRulesModel.Builder builder() {
        return new SpendRulesModel.Builder();
    }

    public static SpendRulesModel.Builder DefaultSpendRulesModel() {
        return new SpendRulesModel.Builder()
                .setAllowedMerchantCategories(Collections.singletonList("test"))
                .setBlockedMerchantCategories(Collections.singletonList("test"))
                .setAllowedMerchantIds(Collections.singletonList("test"))
                .setBlockedMerchantIds(Collections.singletonList("test"))
                .setAllowContactless("TRUE")
                .setAllowAtm("FALSE")
                .setAllowECommerce("TRUE")
                .setAllowCashback("FALSE")
                .setAllowCreditAuthorisations("TRUE")
                .setMaxTransactionAmount(TransactionAmountModel.builder().setValue(10000L).setHasValue(true).build())
                .setMinTransactionAmount(TransactionAmountModel.builder().setValue(100L).setHasValue(true).build())
                .setAllowedMerchantCountries(Collections.singletonList("MT"))
                .setBlockedMerchantCountries(Collections.singletonList("IT"));
    }

    public static SpendRulesModel.Builder fullDefaultSpendRulesModel(final String currency) {
        return new SpendRulesModel.Builder()
                .setAllowedMerchantCategories(Collections.singletonList("test"))
                .setBlockedMerchantCategories(Collections.singletonList("test"))
                .setAllowedMerchantIds(Collections.singletonList("test"))
                .setBlockedMerchantIds(Collections.singletonList("test"))
                .setAllowContactless("TRUE")
                .setAllowAtm("FALSE")
                .setAllowECommerce("TRUE")
                .setAllowCashback("FALSE")
                .setAllowCreditAuthorisations("TRUE")
                .setMaxTransactionAmount(TransactionAmountModel.builder().setValue(10000L).setHasValue(true).build())
                .setMinTransactionAmount(TransactionAmountModel.builder().setValue(100L).setHasValue(true).build())
                .setAllowedMerchantCountries(Collections.singletonList("MT"))
                .setBlockedMerchantCountries(Collections.singletonList("IT"))
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(currency, 100L), LimitInterval.DAILY)));
    }

    public static SpendRulesModel convertSpendRules(final UpdateSpendRulesModel updateSpendRulesModel) {
        return new SpendRulesModel.Builder()
                .setAllowedMerchantCategories(updateSpendRulesModel.getAllowedMerchantCategories().getValue())
                .setBlockedMerchantCategories(updateSpendRulesModel.getBlockedMerchantCategories().getValue())
                .setAllowedMerchantIds(updateSpendRulesModel.getAllowedMerchantIds().getValue())
                .setBlockedMerchantIds(updateSpendRulesModel.getBlockedMerchantIds().getValue())
                .setAllowContactless(updateSpendRulesModel.getAllowContactless())
                .setAllowAtm(updateSpendRulesModel.getAllowAtm())
                .setAllowECommerce(updateSpendRulesModel.getAllowECommerce())
                .setAllowCashback(updateSpendRulesModel.getAllowCashback())
                .setAllowCreditAuthorisations(updateSpendRulesModel.getAllowCreditAuthorisations())
                .setMaxTransactionAmount(updateSpendRulesModel.getMaxTransactionAmount())
                .setMinTransactionAmount(updateSpendRulesModel.getMinTransactionAmount())
                .setAllowedMerchantCountries(updateSpendRulesModel.getAllowedMerchantCountries().getValue())
                .setBlockedMerchantCountries(updateSpendRulesModel.getBlockedMerchantCountries().getValue())
                .setSpendLimit(updateSpendRulesModel.getSpendLimit()).build();
    }
}
