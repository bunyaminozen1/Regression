package opc.models.innovator;

import opc.enums.opc.IdentityType;
import opc.models.shared.CurrencyAmount;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CreateManagedCardProfileModel {
    private final String code;
    private final String payletTypeCode;
    private final List<String> currency;
    private final List<String> fiProvider;
    private final List<String> channelProvider;
    private final List<FeeDetailsModel> purchaseFee;
    private final String cardType;
    private final String cardBrand;
    private final String cardLevelClassification;
    private final int expiryPeriodMonths;
    private final String renewalType;
    private final boolean singleSpend;
    private final String defaultNameOnCard;
    private final String forexFeePercentage;
    private final String cardBureau;
    private final boolean unassignedCardPoolEnabled;

    public CreateManagedCardProfileModel(final Builder builder) {
        this.code = builder.code;
        this.payletTypeCode = builder.payletTypeCode;
        this.currency = builder.currency;
        this.fiProvider = builder.fiProvider;
        this.channelProvider = builder.channelProvider;
        this.purchaseFee = builder.purchaseFee;
        this.cardType = builder.cardType;
        this.cardBrand = builder.cardBrand;
        this.cardLevelClassification = builder.cardLevelClassification;
        this.expiryPeriodMonths = builder.expiryPeriodMonths;
        this.renewalType = builder.renewalType;
        this.singleSpend = builder.singleSpend;
        this.defaultNameOnCard = builder.defaultNameOnCard;
        this.forexFeePercentage = builder.forexFeePercentage;
        this.cardBureau = builder.cardBureau;
        this.unassignedCardPoolEnabled = builder.unassignedCardPoolEnabled;
    }

    public String getCode() {
        return code;
    }

    public String getPayletTypeCode() {
        return payletTypeCode;
    }

    public List<String> getCurrency() {
        return currency;
    }

    public List<String> getFiProvider() {
        return fiProvider;
    }

    public List<String> getChannelProvider() {
        return channelProvider;
    }

    public List<FeeDetailsModel> getPurchaseFee() {
        return purchaseFee;
    }

    public String getCardType() {
        return cardType;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public String getCardLevelClassification() {
        return cardLevelClassification;
    }

    public int getExpiryPeriodMonths() {
        return expiryPeriodMonths;
    }

    public String getRenewalType() {
        return renewalType;
    }

    public boolean getSingleSpend() {
        return singleSpend;
    }

    public String getDefaultNameOnCard() {
        return defaultNameOnCard;
    }

    public String getForexFeePercentage() {
        return forexFeePercentage;
    }

    public String getCardBureau() {
        return cardBureau;
    }

    public boolean getUnassignedCardPoolEnabled() {
        return unassignedCardPoolEnabled;
    }

    public static class Builder {

        private String code;
        private String payletTypeCode;
        private List<String> currency;
        private List<String> fiProvider;
        private List<String> channelProvider;
        private List<FeeDetailsModel> purchaseFee;
        private String cardType;
        private String cardBrand;
        private String cardLevelClassification;
        private int expiryPeriodMonths;
        private String renewalType;
        private boolean singleSpend;
        private String defaultNameOnCard;
        private String forexFeePercentage;
        private String cardBureau;
        private boolean unassignedCardPoolEnabled;

        public Builder setCode(String code) {
            this.code = code;
            return this;
        }

        public Builder setPayletTypeCode(String payletTypeCode) {
            this.payletTypeCode = payletTypeCode;
            return this;
        }

        public Builder setCurrency(List<String> currency) {
            this.currency = currency;
            return this;
        }

        public Builder setFiProvider(List<String> fiProvider) {
            this.fiProvider = fiProvider;
            return this;
        }

        public Builder setChannelProvider(List<String> channelProvider) {
            this.channelProvider = channelProvider;
            return this;
        }

        public Builder setPurchaseFee(List<FeeDetailsModel> purchaseFee) {
            this.purchaseFee = purchaseFee;
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

        public Builder setCardLevelClassification(String cardLevelClassification) {
            this.cardLevelClassification = cardLevelClassification;
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

        public Builder setDefaultNameOnCard(String defaultNameOnCard) {
            this.defaultNameOnCard = defaultNameOnCard;
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

        public Builder setUnassignedCardPoolEnabled(boolean unassignedCardPoolEnabled) {
            this.unassignedCardPoolEnabled = unassignedCardPoolEnabled;
            return this;
        }

        public CreateManagedCardProfileModel build() {
            return new CreateManagedCardProfileModel(this);
        }
    }

    public static CreateManagedCardProfileModel DefaultCreateManagedCardProfileModel(final IdentityType identityType) {
        final String code = String.format("%s_managed_cards", identityType.toString()).toLowerCase();

        return new Builder()
                .setCode(code)
                .setPayletTypeCode(code)
                .setCurrency(Arrays.asList("EUR", "USD", "GBP"))
                .setFiProvider(Collections.singletonList("paynetics_eea"))
                .setChannelProvider(Collections.singletonList("gps"))
                .setCardType("VIRTUAL")
                .setCardBrand("MASTERCARD")
                .setCardLevelClassification("CONSUMER")
                .setExpiryPeriodMonths(36)
                .setRenewalType("NO_RENEW")
                .setSingleSpend(false)
                .setDefaultNameOnCard("default")
                .setForexFeePercentage("TWO_PERCENT")
                .setCardBureau("NITECREST")
                .setPurchaseFee(Arrays.asList(new FeeDetailsModel()
                        .setName("DEFAULT")
                        .setFee(new FeeValuesModel()
                                .setType("FLAT")
                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 12L),
                                        new CurrencyAmount("GBP", 10L),
                                        new CurrencyAmount("USD", 14L)))
                                .setPercentage(new PercentageModel().setValue(0).setScale(2))),

                        new FeeDetailsModel()
                                .setName("STANDARD")
                                .setFee(new FeeValuesModel()
                                        .setType("FLAT")
                                        .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 12L),
                                                new CurrencyAmount("GBP", 10L),
                                                new CurrencyAmount("USD", 14L)))
                                        .setPercentage(new PercentageModel().setValue(0).setScale(2))),

                        new FeeDetailsModel()
                                .setName("VIP")
                                .setFee(new FeeValuesModel()
                                        .setType("FLAT")
                                        .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 12L),
                                                new CurrencyAmount("GBP", 10L),
                                                new CurrencyAmount("USD", 14L)))
                                        .setPercentage(new PercentageModel().setValue(0).setScale(2)))))
                .setUnassignedCardPoolEnabled(true)
                .build();
    }
}
