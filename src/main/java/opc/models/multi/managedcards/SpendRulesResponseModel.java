package opc.models.multi.managedcards;

import java.util.List;

public class SpendRulesResponseModel {

    public List<String> allowedMerchantCategories;
    public List<String> blockedMerchantCategories;
    public List<String> allowedMerchantIds;
    public List<String> blockedMerchantIds;
    public String allowContactless;
    public String allowAtm;
    public String allowECommerce;
    public String allowCashback;
    public String allowCreditAuthorisations;
    public List<SpendLimitModel> spendLimit;
    public Long minTransactionAmount;
    public Long maxTransactionAmount;
    public List<String> allowedMerchantCountries;
    public List<String> blockedMerchantCountries;
    public String authForwardingEnabled;

    public List<String> getAllowedMerchantCategories() {
        return allowedMerchantCategories;
    }

    public SpendRulesResponseModel setAllowedMerchantCategories(List<String> allowedMerchantCategories) {
        this.allowedMerchantCategories = allowedMerchantCategories;
        return this;
    }

    public List<String> getBlockedMerchantCategories() {
        return blockedMerchantCategories;
    }

    public SpendRulesResponseModel setBlockedMerchantCategories(List<String> blockedMerchantCategories) {
        this.blockedMerchantCategories = blockedMerchantCategories;
        return this;
    }

    public List<String> getAllowedMerchantIds() {
        return allowedMerchantIds;
    }

    public SpendRulesResponseModel setAllowedMerchantIds(List<String> allowedMerchantIds) {
        this.allowedMerchantIds = allowedMerchantIds;
        return this;
    }

    public List<String> getBlockedMerchantIds() {
        return blockedMerchantIds;
    }

    public SpendRulesResponseModel setBlockedMerchantIds(List<String> blockedMerchantIds) {
        this.blockedMerchantIds = blockedMerchantIds;
        return this;
    }

    public String isAllowContactless() {
        return allowContactless;
    }

    public SpendRulesResponseModel setAllowContactless(String allowContactless) {
        this.allowContactless = allowContactless;
        return this;
    }

    public String isAllowAtm() {
        return allowAtm;
    }

    public SpendRulesResponseModel setAllowAtm(String allowAtm) {
        this.allowAtm = allowAtm;
        return this;
    }

    public String isAllowECommerce() {
        return allowECommerce;
    }

    public SpendRulesResponseModel setAllowECommerce(String allowECommerce) {
        this.allowECommerce = allowECommerce;
        return this;
    }

    public String isAllowCashback() {
        return allowCashback;
    }

    public SpendRulesResponseModel setAllowCashback(String allowCashback) {
        this.allowCashback = allowCashback;
        return this;
    }

    public String isAllowCreditAuthorisations() {
        return allowCreditAuthorisations;
    }

    public SpendRulesResponseModel setAllowCreditAuthorisations(String allowCreditAuthorisations) {
        this.allowCreditAuthorisations = allowCreditAuthorisations;
        return this;
    }

    public List<SpendLimitModel> getSpendLimit() {
        return spendLimit;
    }

    public SpendRulesResponseModel setSpendLimit(List<SpendLimitModel> spendLimit) {
        this.spendLimit = spendLimit;
        return this;
    }

    public Long getMinTransactionAmount() {
        return minTransactionAmount;
    }

    public SpendRulesResponseModel setMinTransactionAmount(Long minTransactionAmount) {
        this.minTransactionAmount = minTransactionAmount;
        return this;
    }

    public Long getMaxTransactionAmount() {
        return maxTransactionAmount;
    }

    public SpendRulesResponseModel setMaxTransactionAmount(Long maxTransactionAmount) {
        this.maxTransactionAmount = maxTransactionAmount;
        return this;
    }

    public List<String> getAllowedMerchantCountries() {
        return allowedMerchantCountries;
    }

    public SpendRulesResponseModel setAllowedMerchantCountries(List<String> allowedMerchantCountries) {
        this.allowedMerchantCountries = allowedMerchantCountries;
        return this;
    }

    public List<String> getBlockedMerchantCountries() {
        return blockedMerchantCountries;
    }

    public SpendRulesResponseModel setBlockedMerchantCountries(List<String> blockedMerchantCountries) {
        this.blockedMerchantCountries = blockedMerchantCountries;
        return this;
    }

    public String isAuthForwardingEnabled() {
        return authForwardingEnabled;
    }

    public SpendRulesResponseModel setAuthForwardingEnabled(String authForwardingEnabled) {
        this.authForwardingEnabled = authForwardingEnabled;
        return this;
    }
}
