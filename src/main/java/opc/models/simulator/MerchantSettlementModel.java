package opc.models.simulator;

import opc.enums.opc.TestMerchant;
import opc.helpers.ModelHelper;
import opc.models.shared.CurrencyAmount;

public class MerchantSettlementModel {
    private final String type;
    private final Boolean fullAndFinal;
    private final CurrencyAmount settlementAmount;
    private final String merchantId;
    private final String merchantName;
    private final String merchantCategoryCode;
    private final String authorisationId;
    private final ConversionRateModel transactionToSettlementConversionRate;

    public MerchantSettlementModel(final Builder builder) {
        this.type = builder.type;
        this.fullAndFinal = builder.fullAndFinal;
        this.settlementAmount = builder.settlementAmount;
        this.merchantId = builder.merchantId;
        this.merchantName = builder.merchantName;
        this.merchantCategoryCode = builder.merchantCategoryCode;
        this.authorisationId = builder.authorisationId;
        this.transactionToSettlementConversionRate = builder.transactionToSettlementConversionRate;
    }

    public String getType() {
        return type;
    }

    public Boolean getFullAndFinal() {
        return fullAndFinal;
    }

    public CurrencyAmount getSettlementAmount() {
        return settlementAmount;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getMerchantCategoryCode() {
        return merchantCategoryCode;
    }

    public String getAuthorisationId() {
        return authorisationId;
    }

    public ConversionRateModel getTransactionToSettlementConversionRate() {
        return transactionToSettlementConversionRate;
    }

    public static class Builder{
        private String type;
        private Boolean fullAndFinal;
        private CurrencyAmount settlementAmount;
        private String merchantId;
        private String merchantName;
        private String merchantCategoryCode;
        private String authorisationId;
        private ConversionRateModel transactionToSettlementConversionRate;

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setFullAndFinal(Boolean fullAndFinal) {
            this.fullAndFinal = fullAndFinal;
            return this;
        }

        public Builder setSettlementAmount(CurrencyAmount settlementAmount) {
            this.settlementAmount = settlementAmount;
            return this;
        }

        public Builder setMerchantId(String merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public Builder setMerchantName(String merchantName) {
            this.merchantName = merchantName;
            return this;
        }

        public Builder setMerchantCategoryCode(String merchantCategoryCode) {
            this.merchantCategoryCode = merchantCategoryCode;
            return this;
        }

        public Builder setAuthorisationId(String authorisationId) {
            this.authorisationId = authorisationId;
            return this;
        }

        public Builder setTransactionToSettlementConversionRate(ConversionRateModel transactionToSettlementConversionRate) {
            this.transactionToSettlementConversionRate = transactionToSettlementConversionRate;
            return this;
        }

        public MerchantSettlementModel build(){ return new MerchantSettlementModel(this); }
    }

    public static Builder DefaultMerchantSettlement(final CurrencyAmount purchaseAmount,
                                                    final String authorisationId){
        final TestMerchant merchant = ModelHelper.merchantDetails(purchaseAmount.getCurrency());

        return new Builder()
                .setType("SALE_PURCHASE")
                .setFullAndFinal(true)
                .setSettlementAmount(purchaseAmount)
                .setMerchantId(merchant.getMerchantId())
                .setMerchantName(merchant.getMerchantName())
                .setMerchantCategoryCode(merchant.getMerchantCategoryCode())
                .setAuthorisationId(authorisationId)
                .setTransactionToSettlementConversionRate(new ConversionRateModel(1));
    }
}
