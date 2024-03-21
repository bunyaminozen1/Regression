package opc.models.innovator;

import commons.models.innovator.IdentityProfileAuthenticationModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ProgrammeConfigsModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CreateConsumerProfileModel {
    private final String code;
    private final String payletTypeCode;
    private final IdentityProfileAuthenticationModel accountInformationFactors;
    private final IdentityProfileAuthenticationModel paymentInitiationFactors;
    private final IdentityProfileAuthenticationModel threedsInitiationFactors;
    private final String emailVerificationUrl;
    private final String emailValidationProvider;
    private final String baseUrl;
    private final String kycProviderKey;
    private final String amlProviderKey;
    private final List<FeeModel> customFee;
    private final IdentityProfileAuthenticationModel beneficiaryManagementFactors;

    public CreateConsumerProfileModel(final Builder builder) {
        this.code = builder.code;
        this.payletTypeCode = builder.payletTypeCode;
        this.accountInformationFactors = builder.accountInformationFactors;
        this.paymentInitiationFactors = builder.paymentInitiationFactors;
        this.threedsInitiationFactors = builder.threedsInitiationFactors;
        this.emailVerificationUrl = builder.emailVerificationUrl;
        this.emailValidationProvider = builder.emailValidationProvider;
        this.baseUrl = builder.baseUrl;
        this.kycProviderKey = builder.kycProviderKey;
        this.amlProviderKey = builder.amlProviderKey;
        this.customFee = builder.customFee;
        this.beneficiaryManagementFactors = builder.beneficiaryManagementFactors;
    }

    public String getCode() {
        return code;
    }

    public String getPayletTypeCode() {
        return payletTypeCode;
    }

    public IdentityProfileAuthenticationModel getAccountInformationFactors() {
        return accountInformationFactors;
    }

    public IdentityProfileAuthenticationModel getPaymentInitiationFactors() {
        return paymentInitiationFactors;
    }

    public IdentityProfileAuthenticationModel getThreedsInitiationFactors() {
        return threedsInitiationFactors;
    }

    public String getEmailVerificationUrl() {
        return emailVerificationUrl;
    }
    public String getEmailValidationProvider() { return emailValidationProvider; }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getKycProviderKey() {
        return kycProviderKey;
    }

    public String getAmlProviderKey() {
        return amlProviderKey;
    }

    public List<FeeModel> getCustomFee() {
        return customFee;
    }

    public IdentityProfileAuthenticationModel getBeneficiaryManagementFactors() {
        return beneficiaryManagementFactors;
    }

    public static class Builder {

        private String code;
        private String payletTypeCode;
        private IdentityProfileAuthenticationModel accountInformationFactors;
        private IdentityProfileAuthenticationModel paymentInitiationFactors;
        private IdentityProfileAuthenticationModel threedsInitiationFactors;
        private String emailVerificationUrl;
        private String emailValidationProvider;
        private String baseUrl;
        private String kycProviderKey;
        private String amlProviderKey;
        private List<FeeModel> customFee;
        private IdentityProfileAuthenticationModel beneficiaryManagementFactors;

        public Builder setCode(String code) {
            this.code = code;
            return this;
        }

        public Builder setPayletTypeCode(String payletTypeCode) {
            this.payletTypeCode = payletTypeCode;
            return this;
        }

        public Builder setAccountInformationFactors(final IdentityProfileAuthenticationModel accountInformationFactors) {
            this.accountInformationFactors = accountInformationFactors;
            return this;
        }

        public Builder setPaymentInitiationFactors(final IdentityProfileAuthenticationModel paymentInitiationFactors) {
            this.paymentInitiationFactors = paymentInitiationFactors;
            return this;
        }

        public Builder setThreedsInitiationFactors(final IdentityProfileAuthenticationModel threedsInitiationFactors) {
            this.threedsInitiationFactors = threedsInitiationFactors;
            return this;
        }

        public Builder setEmailVerificationUrl(String emailVerificationUrl) {
            this.emailVerificationUrl = emailVerificationUrl;
            return this;
        }

        public Builder setEmailValidationProvider(String emailValidationProvider) {
            this.emailValidationProvider = emailValidationProvider;
            return this;
        }

        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder setKycProviderKey(String kycProviderKey) {
            this.kycProviderKey = kycProviderKey;
            return this;
        }

        public Builder setAmlProviderKey(String amlProviderKey) {
            this.amlProviderKey = amlProviderKey;
            return this;
        }

        public Builder setCustomFee(List<FeeModel> customFee) {
            this.customFee = customFee;
            return this;
        }

        public Builder setBeneficiaryManagementFactors(final IdentityProfileAuthenticationModel beneficiaryManagementFactors) {
            this.beneficiaryManagementFactors = beneficiaryManagementFactors;
            return this;
        }

        public CreateConsumerProfileModel build() {
            return new CreateConsumerProfileModel(this);
        }
    }

    public static CreateConsumerProfileModel DefaultCreateConsumerProfileModel() {
        return new Builder()
                .setCode("default_consumers")
                .setPayletTypeCode("default_consumers")
                .setAccountInformationFactors(IdentityProfileAuthenticationModel.DefaultAccountInfoIdentityProfileAuthenticationScheme())
                .setPaymentInitiationFactors(IdentityProfileAuthenticationModel.DefaultPaymentInitIdentityProfileAuthenticationScheme())
                .setEmailVerificationUrl("https://qa.weavr.io")
                .setEmailValidationProvider("WEAVR")
                .setBaseUrl("https://www.fakeconsumer.com")
                .setKycProviderKey("SUM_SUB")
                .setAmlProviderKey("SUM_SUB")
                .setCustomFee(Collections.singletonList(new FeeModel()
                        .setFeeKey("PRINTED_CARD_ACCOUNT_STATEMENT")
                        .setFee(Collections.singletonList(
                                new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 103L),
                                                        new CurrencyAmount("GBP", 108L),
                                                        new CurrencyAmount("USD", 106L))))))))
                .setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel.DefaultBeneficiaryManagementIdentityProfileAuthenticationScheme())
                .setEmailValidationProvider("WEAVR")
                .build();
    }

    public static CreateConsumerProfileModel DefaultThreeDSCreateConsumerProfileModel() {
        return new Builder()
                .setCode("default_consumers")
                .setPayletTypeCode("default_consumers")
                .setAccountInformationFactors(IdentityProfileAuthenticationModel.AllAccountInformationFactorsIdentityProfileAuthenticationScheme())
                .setPaymentInitiationFactors(IdentityProfileAuthenticationModel.AllAccountInformationFactorsIdentityProfileAuthenticationScheme())
                .setThreedsInitiationFactors(IdentityProfileAuthenticationModel.DefaultThreeDsIdentityProfileAuthenticationScheme())
                .setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel.AllAccountInformationFactorsIdentityProfileAuthenticationScheme())
                .setEmailVerificationUrl("https://qa.weavr.io")
                .setEmailValidationProvider("WEAVR")
                .setBaseUrl("https://www.fakeconsumer.com")
                .setKycProviderKey("SUM_SUB")
                .setAmlProviderKey("SUM_SUB")
                .setCustomFee(Collections.singletonList(new FeeModel()
                        .setFeeKey("PRINTED_CARD_ACCOUNT_STATEMENT")
                        .setFee(Collections.singletonList(
                                new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 103L),
                                                        new CurrencyAmount("GBP", 108L),
                                                        new CurrencyAmount("USD", 106L))))))))
                .setEmailValidationProvider("WEAVR")
                .build();
    }

    public static CreateConsumerProfileModel SecondaryThreeDSCreateConsumerProfileModel() {
        return new Builder()
                .setCode("default_consumers")
                .setPayletTypeCode("default_consumers")
                .setAccountInformationFactors(IdentityProfileAuthenticationModel.AllAccountInformationFactorsIdentityProfileAuthenticationScheme())
                .setPaymentInitiationFactors(IdentityProfileAuthenticationModel.AllAccountInformationFactorsIdentityProfileAuthenticationScheme())
                .setThreedsInitiationFactors(IdentityProfileAuthenticationModel.SecondaryThreeDsIdentityProfileAuthenticationScheme())
                .setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel.AllAccountInformationFactorsIdentityProfileAuthenticationScheme())
                .setEmailVerificationUrl("https://qa.weavr.io")
                .setBaseUrl("https://www.fakeconsumer.com")
                .setKycProviderKey("SUM_SUB")
                .setAmlProviderKey("SUM_SUB")
                .setCustomFee(Collections.singletonList(new FeeModel()
                        .setFeeKey("PRINTED_CARD_ACCOUNT_STATEMENT")
                        .setFee(Collections.singletonList(
                                new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 103L),
                                                        new CurrencyAmount("GBP", 108L),
                                                        new CurrencyAmount("USD", 106L))))))))
                .setEmailValidationProvider("WEAVR")
                .build();
    }

    public static CreateConsumerProfileModel DefaultCreateConsumerProfileModel(final ProgrammeConfigsModel programmeConfigs) {
        return new Builder()
                .setCode("default_consumers")
                .setPayletTypeCode("default_consumers")
                .setAccountInformationFactors(IdentityProfileAuthenticationModel.dynamicIdentityProfileAuthenticationScheme(programmeConfigs.getAccountInformationFactors()))
                .setPaymentInitiationFactors(IdentityProfileAuthenticationModel.dynamicIdentityProfileAuthenticationScheme(programmeConfigs.getPaymentInitiationFactors()))
                .setEmailVerificationUrl("https://qa.weavr.io")
                .setBaseUrl("https://www.fakeconsumer.com")
                .setKycProviderKey("SUM_SUB")
                .setAmlProviderKey("SUM_SUB")
                .setCustomFee(Collections.singletonList(new FeeModel()
                        .setFeeKey("PRINTED_CARD_ACCOUNT_STATEMENT")
                        .setFee(Collections.singletonList(
                                new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 103L),
                                                        new CurrencyAmount("GBP", 108L),
                                                        new CurrencyAmount("USD", 106L))))))))
                .setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel.dynamicIdentityProfileAuthenticationScheme(programmeConfigs.getBeneficiaryManagementFactors()))
                .setEmailValidationProvider("WEAVR")
                .build();
    }
}