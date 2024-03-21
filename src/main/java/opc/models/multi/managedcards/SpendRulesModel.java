package opc.models.multi.managedcards;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import opc.enums.opc.LimitInterval;
import opc.models.shared.CurrencyAmount;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpendRulesModel {
    private final List<String> allowedMerchantCategories;
    private final List<String> blockedMerchantCategories;
    private final List<String> allowedMerchantIds;
    private final List<String> blockedMerchantIds;
    private final Boolean allowContactless;
    private final Boolean allowAtm;
    private final Boolean allowECommerce;
    private final Boolean allowCashback;
    private final Boolean allowCreditAuthorisations;
    private final List<SpendLimitModel> spendLimit;
    private final Long minTransactionAmount;
    private final Long maxTransactionAmount;
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

    public Boolean isAllowContactless() {
        return allowContactless;
    }

    public Boolean isAllowAtm() {
        return allowAtm;
    }

    public Boolean isAllowECommerce() {
        return allowECommerce;
    }

    public Boolean isAllowCashback() {
        return allowCashback;
    }

    public Boolean isAllowCreditAuthorisations() {
        return allowCreditAuthorisations;
    }

    public List<SpendLimitModel> getSpendLimit() {
        return spendLimit;
    }

    public Long getMinTransactionAmount() {
        return minTransactionAmount;
    }

    public Long getMaxTransactionAmount() {
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
        private Boolean allowContactless;
        private Boolean allowAtm;
        private Boolean allowECommerce;
        private Boolean allowCashback;
        private Boolean allowCreditAuthorisations;
        private List<SpendLimitModel> spendLimit;
        private Long minTransactionAmount;
        private Long maxTransactionAmount;
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

        public Builder setAllowContactless(Boolean allowContactless) {
            this.allowContactless = allowContactless;
            return this;
        }

        public Builder setAllowAtm(Boolean allowAtm) {
            this.allowAtm = allowAtm;
            return this;
        }

        public Builder setAllowECommerce(Boolean allowECommerce) {
            this.allowECommerce = allowECommerce;
            return this;
        }

        public Builder setAllowCashback(Boolean allowCashback) {
            this.allowCashback = allowCashback;
            return this;
        }

        public Builder setAllowCreditAuthorisations(Boolean allowCreditAuthorisations) {
            this.allowCreditAuthorisations = allowCreditAuthorisations;
            return this;
        }

        public Builder setSpendLimit(List<SpendLimitModel> spendLimit) {
            this.spendLimit = spendLimit;
            return this;
        }

        public Builder setMinTransactionAmount(Long minTransactionAmount) {
            this.minTransactionAmount = minTransactionAmount;
            return this;
        }

        public Builder setMaxTransactionAmount(Long maxTransactionAmount) {
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

    public static Builder builder() {
        return new Builder();
    }

    public static Builder DefaultSpendRulesModel() {
        return new Builder()
                .setAllowedMerchantCategories(Collections.singletonList("test"))
                .setBlockedMerchantCategories(Collections.singletonList("test"))
                .setAllowedMerchantIds(Collections.singletonList("test"))
                .setBlockedMerchantIds(Collections.singletonList("test"))
                .setAllowContactless(true)
                .setAllowAtm(false)
                .setAllowECommerce(true)
                .setAllowCashback(false)
                .setAllowCreditAuthorisations(true)
                .setMaxTransactionAmount(10000L)
                .setMinTransactionAmount(100L)
                .setAllowedMerchantCountries(Collections.singletonList("MT"))
                .setBlockedMerchantCountries(Collections.singletonList("IT"));
    }

    public static Builder fullDefaultSpendRulesModel(final String currency) {
        return new Builder()
                .setAllowedMerchantCategories(Collections.singletonList("test"))
                .setBlockedMerchantCategories(Collections.singletonList("test"))
                .setAllowedMerchantIds(Collections.singletonList("test"))
                .setBlockedMerchantIds(Collections.singletonList("test"))
                .setAllowContactless(true)
                .setAllowAtm(false)
                .setAllowECommerce(true)
                .setAllowCashback(false)
                .setAllowCreditAuthorisations(true)
                .setMaxTransactionAmount(10000L)
                .setMinTransactionAmount(100L)
                .setAllowedMerchantCountries(Collections.singletonList("MT"))
                .setBlockedMerchantCountries(Collections.singletonList("IT"))
                .setSpendLimit(Collections.singletonList(new SpendLimitModel(new CurrencyAmount(currency, 100L), LimitInterval.DAILY)));
    }

    public static Builder nullableSpendRulesModel() {
        final var nullableList = Stream.of("test", null)
            .collect(Collectors.toList());
        return DefaultSpendRulesModel()
            .setAllowedMerchantCategories(nullableList)
            .setBlockedMerchantCategories(nullableList)
            .setAllowedMerchantIds(nullableList)
            .setBlockedMerchantIds(nullableList)
            .setAllowedMerchantCountries(Stream.of("MT", null)
                .collect(Collectors.toList()))
            .setBlockedMerchantCountries(Stream.of("IT", null)
                .collect(Collectors.toList()));
    }

    @SneakyThrows
    public static String createFullDefaultSpendRulesModelString(final String currency) {
        return new ObjectMapper().writeValueAsString(fullDefaultSpendRulesModel(currency).build());
    }

    @SneakyThrows
    public static String createDefaultSpendRulesModelString() {
        return new ObjectMapper().writeValueAsString(DefaultSpendRulesModel().build());
    }

    @SneakyThrows
    public static String patchSpendRulesModelString() {
        return new ObjectMapper().writeValueAsString(SpendRulesModel.builder().setAllowedMerchantCategories(Collections.singletonList("8877665544")).build());
    }
}
