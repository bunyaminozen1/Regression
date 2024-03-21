package opc.models.innovator;

import opc.enums.opc.DefaultTimeoutDecision;
import opc.enums.opc.IdentityType;
import opc.models.multi.managedcards.AuthForwardingModel;
import opc.models.shared.CurrencyAmount;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AbstractUpdateManagedCardsProfileModel {
    private final String code;
    private final boolean hasTag;
    private final List<String> tag;
    private final String cardType;
    private final String cardBrand;
    private final int expiryPeriodMonths;
    private final String renewalType;
    private final boolean singleSpend;
    private final List<FeeDetailsModel> purchaseFee;
    private final List<FeeDetailsModel> refundFee;
    private final List<FeeDetailsModel> chargebackFee;
    private final List<FeeDetailsModel> atmWithdrawalFee;
    private final List<FeeDetailsModel> cardBalanceInquiryFee;
    private final String forexFeePercentage;
    private final String cardBureau;
    private final String unassignedCardPoolEnabled;

    private final DigitalWalletsEnabledModel digitalWalletsEnabled;

    private final AuthForwardingModel authForwarding;

    public AbstractUpdateManagedCardsProfileModel(final Builder builder) {
        this.code = builder.code;
        this.hasTag = builder.hasTag;
        this.tag = builder.tag;
        this.cardType = builder.cardType;
        this.cardBrand = builder.cardBrand;
        this.expiryPeriodMonths = builder.expiryPeriodMonths;
        this.renewalType = builder.renewalType;
        this.singleSpend = builder.singleSpend;
        this.purchaseFee = builder.purchaseFee;
        this.refundFee = builder.refundFee;
        this.chargebackFee = builder.chargebackFee;
        this.atmWithdrawalFee = builder.atmWithdrawalFee;
        this.cardBalanceInquiryFee = builder.cardBalanceInquiryFee;
        this.forexFeePercentage = builder.forexFeePercentage;
        this.cardBureau = builder.cardBureau;
        this.unassignedCardPoolEnabled = builder.unassignedCardPoolEnabled;
        this.digitalWalletsEnabled = builder.digitalWalletsEnabled;
        this.authForwarding = builder.authForwarding;
    }

    public String getCode() {
        return code;
    }

    public boolean isHasTag() {
        return hasTag;
    }

    public List<String> getTag() {
        return tag;
    }

    public String getCardType() {
        return cardType;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public int getExpiryPeriodMonths() {
        return expiryPeriodMonths;
    }

    public String getRenewalType() {
        return renewalType;
    }

    public boolean isSingleSpend() {
        return singleSpend;
    }

    public List<FeeDetailsModel> getPurchaseFee() {
        return purchaseFee;
    }

    public List<FeeDetailsModel> getRefundFee() {
        return refundFee;
    }

    public List<FeeDetailsModel> getChargebackFee() {
        return chargebackFee;
    }

    public List<FeeDetailsModel> getAtmWithdrawalFee() {
        return atmWithdrawalFee;
    }

    public List<FeeDetailsModel> getCardBalanceInquiryFee() {
        return cardBalanceInquiryFee;
    }

    public String getForexFeePercentage() {
        return forexFeePercentage;
    }

    public String getCardBureau() {
        return cardBureau;
    }

    public String getUnassignedCardPoolEnabled() {
        return unassignedCardPoolEnabled;
    }

    public DigitalWalletsEnabledModel getDigitalWalletsEnabled() { return digitalWalletsEnabled; }

    public AuthForwardingModel getAuthForwarding() { return authForwarding; }

    public static class Builder {
        private String code;
        private boolean hasTag;
        private List<String> tag;
        private String cardType;
        private String cardBrand;
        private int expiryPeriodMonths;
        private String renewalType;
        private boolean singleSpend;
        private List<FeeDetailsModel> purchaseFee;
        private List<FeeDetailsModel> refundFee;
        private List<FeeDetailsModel> chargebackFee;
        private List<FeeDetailsModel> atmWithdrawalFee;
        private List<FeeDetailsModel> cardBalanceInquiryFee;
        private String forexFeePercentage;
        private String cardBureau;
        private String unassignedCardPoolEnabled;
        private DigitalWalletsEnabledModel digitalWalletsEnabled;
        private AuthForwardingModel authForwarding;

        public Builder setCode(String code) {
            this.code = code;
            return this;
        }

        public Builder setHasTag(boolean hasTag) {
            this.hasTag = hasTag;
            return this;
        }

        public Builder setTag(List<String> tag) {
            this.tag = tag;
            return this;
        }

        public Builder setCardType(String cardType) {
            this.cardType = cardType;
            return this;
        }

        public Builder setCardBrand(String cardBrand) {
            this.cardBrand = cardBrand;
            return this;
        }

        public Builder setExpiryPeriodMonths(int expiryPeriodMonths) {
            this.expiryPeriodMonths = expiryPeriodMonths;
            return this;
        }

        public Builder setRenewalType(String renewalType) {
            this.renewalType = renewalType;
            return this;
        }

        public Builder setSingleSpend(boolean singleSpend) {
            this.singleSpend = singleSpend;
            return this;
        }

        public Builder setPurchaseFee(List<FeeDetailsModel> purchaseFee) {
            this.purchaseFee = purchaseFee;
            return this;
        }

        public Builder setRefundFee(List<FeeDetailsModel> refundFee) {
            this.refundFee = refundFee;
            return this;
        }

        public Builder setChargebackFee(List<FeeDetailsModel> chargebackFee) {
            this.chargebackFee = chargebackFee;
            return this;
        }

        public Builder setAtmWithdrawalFee(List<FeeDetailsModel> atmWithdrawalFee) {
            this.atmWithdrawalFee = atmWithdrawalFee;
            return this;
        }

        public Builder setCardBalanceInquiryFee(List<FeeDetailsModel> cardBalanceInquiryFee) {
            this.cardBalanceInquiryFee = cardBalanceInquiryFee;
            return this;
        }

        public Builder setForexFeePercentage(String forexFeePercentage) {
            this.forexFeePercentage = forexFeePercentage;
            return this;
        }

        public Builder setCardBureau(String cardBureau) {
            this.cardBureau = cardBureau;
            return this;
        }

        public Builder setUnassignedCardPoolEnabled(String unassignedCardPoolEnabled) {
            this.unassignedCardPoolEnabled = unassignedCardPoolEnabled;
            return this;
        }

        public Builder setDigitalWalletsEnabled(DigitalWalletsEnabledModel digitalWalletsEnabled) {
            this.digitalWalletsEnabled = digitalWalletsEnabled;
            return this;
        }

        public Builder setAuthForwarding(AuthForwardingModel authForwarding) {
            this.authForwarding = authForwarding;
            return this;
        }

        public AbstractUpdateManagedCardsProfileModel build() { return new AbstractUpdateManagedCardsProfileModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }



    public static Builder DefaultAbstractUpdateManagedCardsProfileModel(final IdentityType identityType, final String cardFundingType){
        final String code = String.format("%s_managed_cards", identityType.name().toLowerCase());

        return new Builder()
                .setCode(code)
                .setCardType("VIRTUAL")
                .setCardBrand("MASTERCARD")
                .setExpiryPeriodMonths(36)
                .setRenewalType("NO_RENEW")
                .setSingleSpend(false)
                .setForexFeePercentage("ONE_PERCENT")
                .setCardBureau("NITECREST")
                .setPurchaseFee(Collections.singletonList(new FeeDetailsModel()
                        .setFee(new FeeValuesModel()
                                .setType("FLAT")
                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 12L),
                                        new CurrencyAmount("GBP", 10L),
                                        new CurrencyAmount("USD", 14L))))))
                .setUnassignedCardPoolEnabled("TRUE")
                .setDigitalWalletsEnabled(new DigitalWalletsEnabledModel(false, false))
                .setAuthForwarding(new AuthForwardingModel(false, DefaultTimeoutDecision.DECLINE.name()));
    }
}
