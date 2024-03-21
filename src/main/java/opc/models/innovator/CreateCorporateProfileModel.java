package opc.models.innovator;

import commons.models.innovator.IdentityProfileAuthenticationModel;
import opc.enums.opc.CompanyType;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ProgrammeConfigsModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CreateCorporateProfileModel {
    private final String code;
    private final String payletTypeCode;
    private final List<CompanyType> companyType;
    private final IdentityProfileAuthenticationModel accountInformationFactors;
    private final IdentityProfileAuthenticationModel paymentInitiationFactors;
    private final IdentityProfileAuthenticationModel threedsInitiationFactors;
    private final String emailVerificationUrl;
    private final String baseUrl;
    private final String kybProviderKey;
    private final String amlProviderKey;
    private final List<FeeModel> customFee;
    private final IdentityProfileAuthenticationModel beneficiaryManagementFactors;
    private final IdentityProfileAuthenticationModel variableRecurrentPaymentInitiationFactors;

    public CreateCorporateProfileModel(final Builder builder) {
        this.code = builder.code;
        this.payletTypeCode = builder.payletTypeCode;
        this.companyType = builder.companyType;
        this.accountInformationFactors = builder.accountInformationFactors;
        this.paymentInitiationFactors = builder.paymentInitiationFactors;
        this.threedsInitiationFactors = builder.threedsInitiationFactors;
        this.emailVerificationUrl = builder.emailVerificationUrl;
        this.baseUrl = builder.baseUrl;
        this.kybProviderKey = builder.kybProviderKey;
        this.amlProviderKey = builder.amlProviderKey;
        this.customFee = builder.customFee;
        this.beneficiaryManagementFactors = builder.beneficiaryManagementFactors;
        this.variableRecurrentPaymentInitiationFactors = builder.variableRecurrentPaymentInitiationFactors;
    }

    public String getCode() {
        return code;
    }

    public String getPayletTypeCode() {
        return payletTypeCode;
    }

    public List<CompanyType> getCompanyType() {
        return companyType;
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

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getKybProviderKey() {
        return kybProviderKey;
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
    public IdentityProfileAuthenticationModel getVariableRecurrentPaymentInitiationFactors() {
        return variableRecurrentPaymentInitiationFactors;
    }

    public static class Builder {

        private String code;
        private String payletTypeCode;
        private List<CompanyType> companyType;
        private IdentityProfileAuthenticationModel accountInformationFactors;
        private IdentityProfileAuthenticationModel paymentInitiationFactors;
        private IdentityProfileAuthenticationModel threedsInitiationFactors;
        private String emailVerificationUrl;
        private String baseUrl;
        private String kybProviderKey;
        private String amlProviderKey;
        private List<FeeModel> customFee;
        private IdentityProfileAuthenticationModel beneficiaryManagementFactors;
        private IdentityProfileAuthenticationModel variableRecurrentPaymentInitiationFactors;

        public Builder setCode(String code) {
            this.code = code;
            return this;
        }

        public Builder setPayletTypeCode(String payletTypeCode) {
            this.payletTypeCode = payletTypeCode;
            return this;
        }

        public Builder setCompanyType(List<CompanyType> companyType) {
            this.companyType = companyType;
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

        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder setKybProviderKey(String kybProviderKey) {
            this.kybProviderKey = kybProviderKey;
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

        public Builder setVariableRecurrentPaymentInitiationFactors(final IdentityProfileAuthenticationModel variableRecurrentPaymentInitiationFactors) {
            this.variableRecurrentPaymentInitiationFactors = variableRecurrentPaymentInitiationFactors;
            return this;
        }

        public CreateCorporateProfileModel build() {
            return new CreateCorporateProfileModel(this);
        }
    }

    public static Builder DefaultCreateCorporateProfileModel() {
        return new Builder()
                .setCode("default_corporates")
                .setPayletTypeCode("default_corporates")
                .setCompanyType(Arrays.asList(CompanyType.values()))
                .setAccountInformationFactors(IdentityProfileAuthenticationModel.DefaultAccountInfoIdentityProfileAuthenticationScheme())
                .setPaymentInitiationFactors(IdentityProfileAuthenticationModel.DefaultPaymentInitIdentityProfileAuthenticationScheme())
                .setEmailVerificationUrl("{{emailVerificationURL}}")
                .setBaseUrl("https://www.fake.com")
                .setKybProviderKey("SUM_SUB")
                .setAmlProviderKey("SUM_SUB")
                .setCustomFee(Collections.singletonList(new FeeModel()
                        .setFeeKey("PRINTED_CARD_ACCOUNT_STATEMENT")
                        .setFee(Collections.singletonList(
                                new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 103L),
                                                        new CurrencyAmount("GBP", 108L),
                                                        new CurrencyAmount("USD", 106L))))))))
                .setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel.DefaultBeneficiaryManagementIdentityProfileAuthenticationScheme());
    }

    public static Builder DefaultThreeDSCreateCorporateProfileModel() {
        return new Builder()
                .setCode("default_corporates")
                .setPayletTypeCode("default_corporates")
                .setCompanyType(Arrays.asList(CompanyType.values()))
                .setAccountInformationFactors(IdentityProfileAuthenticationModel.AllAccountInformationFactorsIdentityProfileAuthenticationScheme())
                .setPaymentInitiationFactors(IdentityProfileAuthenticationModel.AllFactorsIdentityProfileAuthenticationScheme())
                .setThreedsInitiationFactors(IdentityProfileAuthenticationModel.DefaultThreeDsIdentityProfileAuthenticationScheme())
                .setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel.AllFactorsIdentityProfileAuthenticationScheme())
                .setEmailVerificationUrl("{{emailVerificationURL}}")
                .setBaseUrl("https://www.fake.com")
                .setKybProviderKey("SUM_SUB")
                .setAmlProviderKey("SUM_SUB")
                .setCustomFee(Collections.singletonList(new FeeModel()
                        .setFeeKey("PRINTED_CARD_ACCOUNT_STATEMENT")
                        .setFee(Collections.singletonList(
                                new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 103L),
                                                        new CurrencyAmount("GBP", 108L),
                                                        new CurrencyAmount("USD", 106L))))))));
    }

    public static Builder SecondaryThreeDSCreateCorporateProfileModel() {
        return new Builder()
                .setCode("default_corporates")
                .setPayletTypeCode("default_corporates")
                .setCompanyType(Arrays.asList(CompanyType.values()))
                .setAccountInformationFactors(IdentityProfileAuthenticationModel.AllAccountInformationFactorsIdentityProfileAuthenticationScheme())
                .setPaymentInitiationFactors(IdentityProfileAuthenticationModel.AllFactorsIdentityProfileAuthenticationScheme())
                .setThreedsInitiationFactors(IdentityProfileAuthenticationModel.SecondaryThreeDsIdentityProfileAuthenticationScheme())
                .setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel.AllFactorsIdentityProfileAuthenticationScheme())
                .setEmailVerificationUrl("{{emailVerificationURL}}")
                .setBaseUrl("https://www.fake.com")
                .setKybProviderKey("SUM_SUB")
                .setAmlProviderKey("SUM_SUB")
                .setCustomFee(Collections.singletonList(new FeeModel()
                        .setFeeKey("PRINTED_CARD_ACCOUNT_STATEMENT")
                        .setFee(Collections.singletonList(
                                new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 103L),
                                                        new CurrencyAmount("GBP", 108L),
                                                        new CurrencyAmount("USD", 106L))))))));
    }

    public static Builder DefaultCreatePluginCorporateProfileModel() {
        return new Builder()
                .setCode("default_buyer")
                .setPayletTypeCode("default_buyer")
                .setCompanyType(Arrays.asList(CompanyType.values()))
                .setAccountInformationFactors(IdentityProfileAuthenticationModel.AllAccountInformationFactorsIdentityProfileAuthenticationScheme())
                .setPaymentInitiationFactors(IdentityProfileAuthenticationModel.AllFactorsIdentityProfileAuthenticationScheme())
                .setVariableRecurrentPaymentInitiationFactors(IdentityProfileAuthenticationModel.AllFactorsIdentityProfileAuthenticationScheme())
                .setEmailVerificationUrl("{{emailVerificationURL}}")
                .setBaseUrl("https://www.fake.com")
                .setKybProviderKey("SUM_SUB")
                .setAmlProviderKey("SUM_SUB")
                .setCustomFee(Collections.singletonList(new FeeModel()
                        .setFeeKey("PRINTED_CARD_ACCOUNT_STATEMENT")
                        .setFee(Collections.singletonList(
                                new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 103L),
                                                        new CurrencyAmount("GBP", 108L),
                                                        new CurrencyAmount("USD", 106L))))))))
                .setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel.AllFactorsIdentityProfileAuthenticationScheme());
    }

    public static Builder DefaultCreateCorporateProfileModel(final ProgrammeConfigsModel programmeConfigs) {
        return new Builder()
                .setCode("default_corporates")
                .setPayletTypeCode("default_corporates")
                .setCompanyType(Arrays.asList(CompanyType.values()))
                .setAccountInformationFactors(IdentityProfileAuthenticationModel.dynamicIdentityProfileAuthenticationScheme(programmeConfigs.getAccountInformationFactors()))
                .setPaymentInitiationFactors(IdentityProfileAuthenticationModel.dynamicIdentityProfileAuthenticationScheme(programmeConfigs.getPaymentInitiationFactors()))
                .setEmailVerificationUrl("{{emailVerificationURL}}")
                .setBaseUrl("https://www.fake.com")
                .setKybProviderKey("SUM_SUB")
                .setAmlProviderKey("SUM_SUB")
                .setCustomFee(Collections.singletonList(new FeeModel()
                        .setFeeKey("PRINTED_CARD_ACCOUNT_STATEMENT")
                        .setFee(Collections.singletonList(
                                new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 103L),
                                                        new CurrencyAmount("GBP", 108L),
                                                        new CurrencyAmount("USD", 106L))))))))
                .setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel.dynamicIdentityProfileAuthenticationScheme(programmeConfigs.getBeneficiaryManagementFactors()));
    }

    public static Builder builder() {
        return new Builder();
    }
}
