package opc.models.multi.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import opc.enums.opc.ConsumerSourceOfFunds;
import commons.enums.Currency;
import org.apache.commons.lang3.RandomStringUtils;

public class CreateConsumerModel {
    private final String profileId;
    private final String tag;
    private final ConsumerRootUserModel rootUser;
    private final boolean acceptedTerms;
    private final String ipAddress;
    private final String baseCurrency;
    private final ConsumerSourceOfFunds sourceOfFunds;
    private final String sourceOfFundsOther;
    private final String feeGroup;

    public CreateConsumerModel(final CreateConsumerModel.Builder builder) {
        this.profileId = builder.profileId;
        this.tag = builder.tag;
        this.rootUser = builder.rootUser;
        this.acceptedTerms = builder.acceptedTerms;
        this.ipAddress = builder.ipAddress;
        this.baseCurrency = builder.baseCurrency;
        this.sourceOfFunds = builder.sourceOfFunds;
        this.sourceOfFundsOther = builder.sourceOfFundsOther;
        this.feeGroup = builder.feeGroup;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getTag() {
        return tag;
    }

    public ConsumerRootUserModel getRootUser() {
        return rootUser;
    }

    public boolean getAcceptedTerms() {
        return acceptedTerms;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public ConsumerSourceOfFunds getSourceOfFunds() {
        return sourceOfFunds;
    }

    public String getSourceOfFundsOther() {
        return sourceOfFundsOther;
    }

    public String getFeeGroup() {
        return feeGroup;
    }

    public static class Builder {
        private String profileId;
        private String tag;
        private ConsumerRootUserModel rootUser;
        private boolean acceptedTerms;
        private String ipAddress;
        private String baseCurrency;
        private ConsumerSourceOfFunds sourceOfFunds;
        private String sourceOfFundsOther;
        private String feeGroup;

        public CreateConsumerModel.Builder setProfileId(String profileId) {
            this.profileId = profileId;
            return this;
        }

        public CreateConsumerModel.Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public CreateConsumerModel.Builder setRootUser(ConsumerRootUserModel rootUser) {
            this.rootUser = rootUser;
            return this;
        }

        public CreateConsumerModel.Builder setAcceptedTerms(boolean acceptedTerms) {
            this.acceptedTerms = acceptedTerms;
            return this;
        }

        public CreateConsumerModel.Builder setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public CreateConsumerModel.Builder setBaseCurrency(String baseCurrency) {
            this.baseCurrency = baseCurrency;
            return this;
        }

        public CreateConsumerModel.Builder setSourceOfFunds(ConsumerSourceOfFunds sourceOfFunds) {
            this.sourceOfFunds = sourceOfFunds;
            return this;
        }

        public CreateConsumerModel.Builder setSourceOfFundsOther(String sourceOfFundsOther) {
            this.sourceOfFundsOther = sourceOfFundsOther;
            return this;
        }

        public CreateConsumerModel.Builder setFeeGroup(String feeGroup) {
            this.feeGroup = feeGroup;
            return this;
        }

        public CreateConsumerModel build(){ return new CreateConsumerModel(this);}
    }

    public static CreateConsumerModel.Builder DefaultCreateConsumerModel(final String profileId){
        final Builder builder = new Builder();
        builder.setProfileId(profileId);
        builder.setTag(RandomStringUtils.randomAlphabetic(5));
        builder.setRootUser(ConsumerRootUserModel.DefaultRootUserModel().build());
        builder.setAcceptedTerms(true);
        builder.setIpAddress("127.0.0.1");
        builder.setBaseCurrency(Currency.getRandomCurrency().toString());
        final ConsumerSourceOfFunds randomSourceOfFunds = ConsumerSourceOfFunds.getRandomSourceOfFunds();
        builder.setSourceOfFunds(randomSourceOfFunds);
        if (ConsumerSourceOfFunds.OTHER.equals(randomSourceOfFunds)) {
           builder.setSourceOfFundsOther("Other SoF");
        }
        return builder;

    }

    @SneakyThrows
    public static String createConsumerString(final String profileId, final String email) {
        return new ObjectMapper().writeValueAsString(DefaultCreateConsumerModel(profileId)
                        .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                                .setEmail(email)
                                .build())
                .build());
    }

    public static CreateConsumerModel.Builder EurCurrencyCreateConsumerModel(final String profileId){
        final Builder builder = new Builder();
        builder.setProfileId(profileId);
        builder.setTag(RandomStringUtils.randomAlphabetic(5));
        builder.setRootUser(ConsumerRootUserModel.DefaultRootUserModel().build());
        builder.setAcceptedTerms(true);
        builder.setIpAddress("127.0.0.1");
        builder.setBaseCurrency(Currency.EUR.name());
        final ConsumerSourceOfFunds randomSourceOfFunds = ConsumerSourceOfFunds.getRandomSourceOfFunds();
        builder.setSourceOfFunds(randomSourceOfFunds);
        if (ConsumerSourceOfFunds.OTHER.equals(randomSourceOfFunds)) {
            builder.setSourceOfFundsOther("Other SoF");
        }
        return builder;
    }

    public static CreateConsumerModel.Builder CurrencyCreateConsumerModel(final String profileId,
                                                                          final Currency currency) {
        final Builder builder = new Builder();
        builder.setProfileId(profileId);
        builder.setTag(RandomStringUtils.randomAlphabetic(5));
        builder.setRootUser(ConsumerRootUserModel.DefaultRootUserModel().build());
        builder.setAcceptedTerms(true);
        builder.setIpAddress("127.0.0.1");
        builder.setBaseCurrency(currency.name());
        final ConsumerSourceOfFunds randomSourceOfFunds = ConsumerSourceOfFunds.getRandomSourceOfFunds();
        builder.setSourceOfFunds(randomSourceOfFunds);
        if (ConsumerSourceOfFunds.OTHER.equals(randomSourceOfFunds)) {
            builder.setSourceOfFundsOther("Other SoF");
        }
        return builder;
    }

    public static CreateConsumerModel.Builder NoOccupationCreateConsumerModel(final String profileId){
        final Builder builder = new Builder();
        builder.setProfileId(profileId);
        builder.setTag(RandomStringUtils.randomAlphabetic(5));
        builder.setRootUser(ConsumerRootUserModel.RootUserModelNoOccupation().build());
        builder.setAcceptedTerms(true);
        builder.setIpAddress("127.0.0.1");
        builder.setBaseCurrency(Currency.EUR.name());
        final ConsumerSourceOfFunds randomSourceOfFunds = ConsumerSourceOfFunds.getRandomSourceOfFunds();
        builder.setSourceOfFunds(randomSourceOfFunds);
        if (ConsumerSourceOfFunds.OTHER.equals(randomSourceOfFunds)) {
            builder.setSourceOfFundsOther("Other SoF");
        }
        return builder;
    }

    public static CreateConsumerModel.Builder dataCreateConsumerModel(final String profileId){
        final Builder builder = new Builder();
        builder.setProfileId(profileId);
        builder.setTag(RandomStringUtils.randomAlphabetic(5));
        builder.setRootUser(ConsumerRootUserModel.dataRootUserModel().build());
        builder.setAcceptedTerms(true);
        builder.setIpAddress("127.0.0.1");
        builder.setBaseCurrency(Currency.getRandomCurrency().toString());
        final ConsumerSourceOfFunds randomSourceOfFunds = ConsumerSourceOfFunds.getRandomSourceOfFunds();
        builder.setSourceOfFunds(randomSourceOfFunds);
        if (ConsumerSourceOfFunds.OTHER.equals(randomSourceOfFunds)) {
            builder.setSourceOfFundsOther("Other SoF");
        }
        return builder;

    }
}