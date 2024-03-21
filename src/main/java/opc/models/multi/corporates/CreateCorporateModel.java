package opc.models.multi.corporates;

import com.fasterxml.jackson.databind.ObjectMapper;
import commons.enums.Currency;
import commons.models.CompanyModel;
import lombok.SneakyThrows;
import opc.enums.opc.CorporateSourceOfFunds;
import opc.enums.opc.Industry;
import org.apache.commons.lang3.RandomStringUtils;

public class CreateCorporateModel {
    private final String profileId;
    private final String tag;
    private final CorporateRootUserModel rootUser;
    private final CompanyModel company;
    private final boolean acceptedTerms;
    private final String ipAddress;
    private final String baseCurrency;
    private final CorporateSourceOfFunds sourceOfFunds;
    private final String sourceOfFundsOther;
    private final Industry industry;
    private final String feeGroup;

    public CreateCorporateModel(final Builder builder) {
        this.profileId = builder.profileId;
        this.tag = builder.tag;
        this.rootUser = builder.rootUser;
        this.company = builder.company;
        this.acceptedTerms = builder.acceptedTerms;
        this.ipAddress = builder.ipAddress;
        this.baseCurrency = builder.baseCurrency;
        this.sourceOfFunds = builder.sourceOfFunds;
        this.sourceOfFundsOther = builder.sourceOfFundsOther;
        this.industry = builder.industry;
        this.feeGroup = builder.feeGroup;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getTag() {
        return tag;
    }

    public CorporateRootUserModel getRootUser() {
        return rootUser;
    }

    public CompanyModel getCompany() {
        return company;
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

    public CorporateSourceOfFunds getSourceOfFunds() {
        return sourceOfFunds;
    }

    public String getSourceOfFundsOther() {
        return sourceOfFundsOther;
    }

    public Industry getIndustry() {
        return industry;
    }

    public String getFeeGroup() {
        return feeGroup;
    }

    public static class Builder {
        private String profileId;
        private String tag;
        private CorporateRootUserModel rootUser;
        private CompanyModel company;
        private boolean acceptedTerms;
        private String ipAddress;
        private String baseCurrency;
        private CorporateSourceOfFunds sourceOfFunds;
        private String sourceOfFundsOther;
        private Industry industry;
        private String feeGroup;

        public Builder setProfileId(String profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setRootUser(CorporateRootUserModel rootUser) {
            this.rootUser = rootUser;
            return this;
        }

        public Builder setCompany(CompanyModel company) {
            this.company = company;
            return this;
        }

        public Builder setAcceptedTerms(boolean acceptedTerms) {
            this.acceptedTerms = acceptedTerms;
            return this;
        }

        public Builder setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder setBaseCurrency(String baseCurrency) {
            this.baseCurrency = baseCurrency;
            return this;
        }

        public Builder setSourceOfFunds(CorporateSourceOfFunds sourceOfFunds) {
            this.sourceOfFunds = sourceOfFunds;
            return this;
        }

        public Builder setSourceOfFundsOther(String sourceOfFundsOther) {
            this.sourceOfFundsOther = sourceOfFundsOther;
            return this;
        }

        public Builder setIndustry(Industry industry) {
            this.industry = industry;
            return this;
        }

        public Builder setFeeGroup(String feeGroup) {
            this.feeGroup = feeGroup;
            return this;
        }

        public CreateCorporateModel build(){ return new CreateCorporateModel(this);}
    }

    public static Builder DefaultCreateCorporateModel(final String profileId){
        final Builder builder = new Builder();
        builder.setProfileId(profileId);
        builder.setTag(RandomStringUtils.randomAlphabetic(5));
        builder.setRootUser(CorporateRootUserModel.DefaultRootUserModel().build());
        builder.setCompany(CompanyModel.defaultCompanyModel().build());
        builder.setAcceptedTerms(true);
        builder.setIpAddress("127.0.0.1");
        builder.setBaseCurrency(Currency.getRandomCurrency().toString());
        builder.setSourceOfFunds(CorporateSourceOfFunds.getRandomSourceOfFunds());
        builder.setIndustry(Industry.getRandomIndustry());
        builder.setFeeGroup("DEFAULT");
        final CorporateSourceOfFunds randomSourceOfFunds = CorporateSourceOfFunds.getRandomSourceOfFunds();
        builder.setSourceOfFunds(randomSourceOfFunds);
        if (CorporateSourceOfFunds.OTHER.equals(randomSourceOfFunds)) {
            builder.setSourceOfFundsOther("Other SoF");
        }
        return builder;
    }

    @SneakyThrows
    public static String createCorporateString(final String profileId, final String email) {
        return new ObjectMapper().writeValueAsString(DefaultCreateCorporateModel(profileId)
                        .setRootUser(CorporateRootUserModel.DefaultRootUserModel()
                                .setEmail(email)
                                .build())
                .build());
    }

    public static Builder EurCurrencyCreateCorporateModel(final String profileId){
        final Builder builder = new Builder();
        builder.setProfileId(profileId);
        builder.setTag(RandomStringUtils.randomAlphabetic(5));
        builder.setRootUser(CorporateRootUserModel.DefaultRootUserModel().build());
        builder.setCompany(CompanyModel.defaultCompanyModel().build());
        builder.setAcceptedTerms(true);
        builder.setIpAddress("127.0.0.1");
        builder.setBaseCurrency(Currency.EUR.name());
        builder.setSourceOfFunds(CorporateSourceOfFunds.getRandomSourceOfFunds());
        builder.setIndustry(Industry.getRandomIndustry());
        builder.setFeeGroup("DEFAULT");
        final CorporateSourceOfFunds randomSourceOfFunds = CorporateSourceOfFunds.getRandomSourceOfFunds();
        builder.setSourceOfFunds(randomSourceOfFunds);
        if (CorporateSourceOfFunds.OTHER.equals(randomSourceOfFunds)) {
            builder.setSourceOfFundsOther("Other SoF");
        }
        return builder;
    }

    public static Builder CurrencyCreateCorporateModel(final String profileId, Currency currency){
        final Builder builder = new Builder();
        builder.setProfileId(profileId);
        builder.setTag(RandomStringUtils.randomAlphabetic(5));
        builder.setRootUser(CorporateRootUserModel.DefaultRootUserModel().build());
        builder.setCompany(CompanyModel.defaultCompanyModel().build());
        builder.setAcceptedTerms(true);
        builder.setIpAddress("127.0.0.1");
        builder.setBaseCurrency(currency.name());
        builder.setSourceOfFunds(CorporateSourceOfFunds.getRandomSourceOfFunds());
        builder.setIndustry(Industry.getRandomIndustry());
        builder.setFeeGroup("DEFAULT");
        final CorporateSourceOfFunds randomSourceOfFunds = CorporateSourceOfFunds.getRandomSourceOfFunds();
        builder.setSourceOfFunds(randomSourceOfFunds);
        if (CorporateSourceOfFunds.OTHER.equals(randomSourceOfFunds)) {
            builder.setSourceOfFundsOther("Other SoF");
        }
        return builder;
    }

    public static Builder dataCreateCorporateModel(final String profileId){
        final Builder builder = new Builder();
        builder.setProfileId(profileId);
        builder.setTag(RandomStringUtils.randomAlphabetic(5));
        builder.setRootUser(CorporateRootUserModel.dataRootUserModel().build());
        builder.setCompany(CompanyModel.dataCompanyModel().build());
        builder.setAcceptedTerms(true);
        builder.setIpAddress("127.0.0.1");
        builder.setBaseCurrency(Currency.getRandomCurrency().toString());
        builder.setSourceOfFunds(CorporateSourceOfFunds.getRandomSourceOfFunds());
        builder.setIndustry(Industry.getRandomIndustry());
        builder.setFeeGroup("DEFAULT");
        final CorporateSourceOfFunds randomSourceOfFunds = CorporateSourceOfFunds.getRandomSourceOfFunds();
        builder.setSourceOfFunds(randomSourceOfFunds);
        if (CorporateSourceOfFunds.OTHER.equals(randomSourceOfFunds)) {
            builder.setSourceOfFundsOther("Other SoF");
        }
        return builder;
    }
}
