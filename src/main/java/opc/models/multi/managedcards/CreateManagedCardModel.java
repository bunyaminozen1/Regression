package opc.models.multi.managedcards;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

import com.github.javafaker.Faker;
import lombok.SneakyThrows;
import opc.enums.opc.CountryCode;
import opc.enums.opc.ManagedCardMode;
import opc.models.shared.AddressModel;
import org.apache.commons.lang3.RandomStringUtils;

public class CreateManagedCardModel {
    private final String profileId;
    private final String tag;
    private final String friendlyName;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String currency;
    private final String nameOnCard;
    private final String nameOnCardLine2;
    private final String cardholderMobileNumber;
    private final AddressModel billingAddress;
    private final String mode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String parentManagedAccountId;
    private final DigitalWalletsModel digitalWallets;
    private final String authForwardingDefaultTimeoutDecision;
    private final ThreeDSecureAuthConfigModel threeDSecureAuthConfig;
    private final List<ExternalDataModel> externalData;
    private final String renewalType;

    public CreateManagedCardModel(final Builder builder) {
        this.profileId = builder.profileId;
        this.tag = builder.tag;
        this.friendlyName = builder.friendlyName;
        this.currency = builder.currency;
        this.nameOnCard = builder.nameOnCard;
        this.nameOnCardLine2 = builder.nameOnCardLine2;
        this.cardholderMobileNumber = builder.cardholderMobileNumber;
        this.billingAddress = builder.billingAddress;
        this.mode = builder.mode;
        this.parentManagedAccountId = builder.parentManagedAccountId;
        this.digitalWallets = builder.digitalWallets;
        this.authForwardingDefaultTimeoutDecision = builder.authForwardingDefaultTimeoutDecision;
        this.threeDSecureAuthConfig = builder.threeDSecureAuthConfig;
        this.externalData = builder.externalData;
        this.renewalType = builder.renewalType;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getTag() {
        return tag;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getCurrency() {
        return currency;
    }

    public String getNameOnCard() {
        return nameOnCard;
    }

    public String getNameOnCardLine2() {
        return nameOnCardLine2;
    }

    public String getCardholderMobileNumber() {
        return cardholderMobileNumber;
    }

    public AddressModel getBillingAddress() {
        return billingAddress;
    }

    public String getMode() {
        return mode;
    }

    public String getParentManagedAccountId() {
        return parentManagedAccountId;
    }

    public DigitalWalletsModel getDigitalWallets() {
        return digitalWallets;
    }

    public String getAuthForwardingDefaultTimeoutDecision() {
        return authForwardingDefaultTimeoutDecision;
    }

    public ThreeDSecureAuthConfigModel getThreeDSecureAuthConfig() {
        return threeDSecureAuthConfig;
    }

    public List<ExternalDataModel> getExternalData() { return externalData; }

    public String getRenewalType() { return renewalType; }

    public static class Builder {
        private String profileId;
        private String tag;
        private String friendlyName;
        private String currency;
        private String nameOnCard;
        private String nameOnCardLine2;
        private String cardholderMobileNumber;
        private AddressModel billingAddress;
        private String mode;
        private String parentManagedAccountId;
        private DigitalWalletsModel digitalWallets;
        private String authForwardingDefaultTimeoutDecision;
        private ThreeDSecureAuthConfigModel threeDSecureAuthConfig;
        private List<ExternalDataModel> externalData;
        private String renewalType;

        public Builder setProfileId(String profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setFriendlyName(String friendlyName) {
            this.friendlyName = friendlyName;
            return this;
        }

        public Builder setCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder setNameOnCard(String nameOnCard) {
            this.nameOnCard = nameOnCard;
            return this;
        }

        public Builder setNameOnCardLine2(String nameOnCardLine2) {
            this.nameOnCardLine2 = nameOnCardLine2;
            return this;
        }

        public Builder setCardholderMobileNumber(String cardholderMobileNumber) {
            this.cardholderMobileNumber = cardholderMobileNumber;
            return this;
        }

        public Builder setBillingAddress(AddressModel billingAddress) {
            this.billingAddress = billingAddress;
            return this;
        }

        public Builder setMode(String mode) {
            this.mode = mode;
            return this;
        }

        public Builder setParentManagedAccountId(String parentManagedAccountId) {
            this.parentManagedAccountId = parentManagedAccountId;
            return this;
        }

        public Builder setDigitalWallets(DigitalWalletsModel digitalWallets) {
            this.digitalWallets = digitalWallets;
            return this;
        }

        public Builder setAuthForwardingDefaultTimeoutDecision(String authForwardingDefaultTimeoutDecision) {
            this.authForwardingDefaultTimeoutDecision = authForwardingDefaultTimeoutDecision;
            return this;
        }

        public Builder setThreeDSecureAuthConfig(
                ThreeDSecureAuthConfigModel threeDSecureAuthConfig) {
            this.threeDSecureAuthConfig = threeDSecureAuthConfig;
            return this;
        }

        public Builder setExternalData (List<ExternalDataModel> externalData) {
            this.externalData = externalData;
            return this;
        }

        public Builder setRenewalType(String renewalType) {
            this.renewalType = renewalType;
            return this;
        }

        public CreateManagedCardModel build() {
            return new CreateManagedCardModel(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder DefaultCreateManagedCardModel(final String managedCardProfileId) {
        return new Builder()
                .setProfileId(managedCardProfileId)
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setNameOnCard(String.format("%s-%s", RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5)))
                .setCardholderMobileNumber(String.format("+356%s", RandomStringUtils.randomNumeric(8)))
                .setBillingAddress(AddressModel.DefaultAddressModel().setCountry(CountryCode.MT).build())
                .setDigitalWallets(DigitalWalletsModel.builder()
                        .setWalletsEnabled(false)
                        .build());
    }

    public static Builder DefaultCreatePrepaidManagedCardModel(final String managedCardProfileId, final String currency) {
        return DefaultCreateManagedCardModel(managedCardProfileId)
                .setCurrency(currency)
                .setMode(ManagedCardMode.PREPAID_MODE.name())
                .setDigitalWallets(DigitalWalletsModel.builder()
                        .setWalletsEnabled(false)
                        .build());
    }

    public static Builder DefaultCreateDebitManagedCardModel(final String managedCardProfileId, final String parentManagedAccountId) {
        return DefaultCreateManagedCardModel(managedCardProfileId)
                .setParentManagedAccountId(parentManagedAccountId)
                .setMode(ManagedCardMode.DEBIT_MODE.name())
                .setDigitalWallets(DigitalWalletsModel.builder()
                        .setWalletsEnabled(false)
                        .build());
    }

    public static Builder DefaultCreateDebitManagedCardModel(final String managedCardProfileId,
                                                             final String parentManagedAccountId,
                                                             final String currency) {
        return DefaultCreateManagedCardModel(managedCardProfileId)
                .setParentManagedAccountId(parentManagedAccountId)
                .setCurrency(currency)
                .setMode(ManagedCardMode.DEBIT_MODE.name())
                .setDigitalWallets(DigitalWalletsModel.builder()
                        .setWalletsEnabled(false)
                        .build());
    }

    public static Builder DefaultCreateDebitManagedCardAuthForwardingModel(final String managedCardProfileId,
                                                                           final String parentManagedAccountId,
                                                                           final String authForwardingDefaultTimeoutDecision) {
        return DefaultCreateManagedCardModel(managedCardProfileId)
                .setParentManagedAccountId(parentManagedAccountId)
                .setMode(ManagedCardMode.DEBIT_MODE.name())
                .setAuthForwardingDefaultTimeoutDecision(authForwardingDefaultTimeoutDecision)
                .setDigitalWallets(DigitalWalletsModel.builder()
                        .setWalletsEnabled(false)
                        .build());
    }

    public static Builder ThreeDSecureCreatePrepaidManagedCardModel(final String managedCardProfileId,
                                                                    final String linkedUserId) {
        return new Builder()
                .setProfileId(managedCardProfileId)
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setCurrency("EUR")
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setNameOnCard(String.format("%s-%s", RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5)))
                .setBillingAddress(AddressModel.DefaultAddressModel().setCountry(CountryCode.MT).build())
                .setMode(ManagedCardMode.PREPAID_MODE.name())
                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.DefaultThreeDSecureAuthConfigModel(linkedUserId).build());
    }

    public static Builder ThreeDSecureCreateDebitManagedCardModel(final String managedCardProfileId,
                                                                  final String linkedUserId,
                                                                  final String parentManagedAccountId) {
        return new Builder()
                .setProfileId(managedCardProfileId)
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setCurrency("EUR")
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setNameOnCard(String.format("%s-%s", RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5)))
                .setBillingAddress(AddressModel.DefaultAddressModel().setCountry(CountryCode.MT).build())
                .setMode(ManagedCardMode.DEBIT_MODE.name())
                .setParentManagedAccountId(parentManagedAccountId)
                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.DefaultThreeDSecureAuthConfigModel(linkedUserId).build());
    }

    public static Builder AuthyThreeDSecureCreateDebitManagedCardModel(final String managedCardProfileId,
                                                                       final String linkedUserId,
                                                                       final String parentManagedAccountId) {
        return new Builder()
                .setProfileId(managedCardProfileId)
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setCurrency("EUR")
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setNameOnCard(String.format("%s-%s", RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5)))
                .setBillingAddress(AddressModel.DefaultAddressModel().setCountry(CountryCode.MT).build())
                .setMode(ManagedCardMode.DEBIT_MODE.name())
                .setParentManagedAccountId(parentManagedAccountId)
                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.AuthyThreeDSecureAuthConfigModel(linkedUserId).build());
    }

    public static Builder AuthyThreeDSecureCreatePrepaidManagedCardModel(final String managedCardProfileId,
                                                                         final String linkedUserId) {
        return new Builder()
                .setProfileId(managedCardProfileId)
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setCurrency("EUR")
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setNameOnCard(String.format("%s-%s", RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5)))
                .setBillingAddress(AddressModel.DefaultAddressModel().setCountry(CountryCode.MT).build())
                .setMode(ManagedCardMode.PREPAID_MODE.name())
                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.AuthyThreeDSecureAuthConfigModel(linkedUserId).build());
    }

    public static Builder ThreeDSecureCreatePrepaidManagedCardModelPrimaryOTP(final String managedCardProfileId,
                                                                              final String linkedUserId) {
        return new Builder()
                .setProfileId(managedCardProfileId)
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setCurrency("EUR")
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setNameOnCard(String.format("%s-%s", RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5)))
                .setBillingAddress(AddressModel.DefaultAddressModel().setCountry(CountryCode.MT).build())
                .setMode(ManagedCardMode.PREPAID_MODE.name())
                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(linkedUserId)
                        .setPrimaryChannel("OTP_SMS").build());
    }

    public static Builder ThreeDSecureCreateDebitManagedCardModelPrimaryOTP(final String managedCardProfileId,
                                                                            final String linkedUserId,
                                                                            final String parentManagedAccountId) {
        return new Builder()
                .setProfileId(managedCardProfileId)
                .setFriendlyName(RandomStringUtils.randomAlphabetic(5))
                .setCurrency("EUR")
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setNameOnCard(String.format("%s-%s", RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5)))
                .setBillingAddress(AddressModel.DefaultAddressModel().setCountry(CountryCode.MT).build())
                .setMode(ManagedCardMode.DEBIT_MODE.name())
                .setParentManagedAccountId(parentManagedAccountId)
                .setThreeDSecureAuthConfig(ThreeDSecureAuthConfigModel.builder()
                        .setLinkedUserId(linkedUserId)
                        .setPrimaryChannel("OTP_SMS").build());
    }

    @SneakyThrows
    public static String createPrepaidManagedCardStringModel(final String managedCardProfileId,
                                                             final String currency) {
        return new ObjectMapper().writeValueAsString(DefaultCreateManagedCardModel(managedCardProfileId)
                .setCurrency(currency)
                .setMode(ManagedCardMode.PREPAID_MODE.name())
                .setDigitalWallets(DigitalWalletsModel.builder()
                        .setWalletsEnabled(false)
                        .build()).build());
    }

    public static Builder dataCreateManagedCardModel(final String managedCardProfileId) {

        final Faker faker = new Faker();

        return new Builder()
                .setProfileId(managedCardProfileId)
                .setFriendlyName(faker.dog().name())
                .setTag(faker.dog().name())
                .setNameOnCard(String.format("%s-%s", faker.name().firstName(), faker.name().lastName()))
                .setCardholderMobileNumber(String.format("+356%s", RandomStringUtils.randomNumeric(8)))
                .setBillingAddress(AddressModel.dataAddressModel().setCountry(CountryCode.MT).build())
                .setDigitalWallets(DigitalWalletsModel.builder()
                        .setWalletsEnabled(false)
                        .build());
    }

    public static Builder dataCreatePrepaidManagedCardModel(final String managedCardProfileId, final String currency) {
        return dataCreateManagedCardModel(managedCardProfileId)
                .setCurrency(currency)
                .setMode(ManagedCardMode.PREPAID_MODE.name())
                .setDigitalWallets(DigitalWalletsModel.builder()
                        .setWalletsEnabled(false)
                        .build());
    }
}