package opc.models.multi.corporates;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import opc.enums.opc.CorporateSourceOfFunds;
import commons.enums.Currency;
import opc.enums.opc.Industry;
import commons.models.DateOfBirthModel;
import opc.models.shared.AddressModel;
import commons.models.MobileNumberModel;
import org.apache.commons.lang3.RandomStringUtils;

public class PatchCorporateModel {
    private final String tag;
    private final String name;
    private final String surname;
    private final String email;
    private final MobileNumberModel mobile;
    private final Industry industry;
    private final CorporateSourceOfFunds sourceOfFunds;
    private final String sourceOfFundsOther;
    private final AddressModel companyBusinessAddress;
    private final String feeGroup;
    private final String baseCurrency;
    private final DateOfBirthModel dateOfBirth;

    public PatchCorporateModel(final Builder builder) {
        this.tag = builder.tag;
        this.name = builder.name;
        this.surname = builder.surname;
        this.email = builder.email;
        this.mobile = builder.mobile;
        this.industry = builder.industry;
        this.sourceOfFunds = builder.sourceOfFunds;
        this.sourceOfFundsOther = builder.sourceOfFundsOther;
        this.companyBusinessAddress = builder.companyBusinessAddress;
        this.feeGroup = builder.feeGroup;
        this.baseCurrency = builder.baseCurrency;
        this.dateOfBirth = builder.dateOfBirth;
    }

    public String getTag() {
        return tag;
    }

    public String getName() {
        return name;
    }

    public String getSurname() {
        return surname;
    }

    public String getEmail() {
        return email;
    }

    public MobileNumberModel getMobile() {
        return mobile;
    }

    public Industry getIndustry() {
        return industry;
    }

    public CorporateSourceOfFunds getSourceOfFunds() {
        return sourceOfFunds;
    }

    public String getSourceOfFundsOther() {
        return sourceOfFundsOther;
    }

    public AddressModel getCompanyBusinessAddress() {
        return companyBusinessAddress;
    }

    public String getFeeGroup() {
        return feeGroup;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public DateOfBirthModel getDateOfBirth() {
        return dateOfBirth;
    }

    public static class Builder {
        private String tag;
        private String name;
        private String surname;
        private String email;
        private MobileNumberModel mobile;
        private Industry industry;
        private CorporateSourceOfFunds sourceOfFunds;
        private String sourceOfFundsOther;
        private AddressModel companyBusinessAddress;
        private String feeGroup;
        private String baseCurrency;
        private DateOfBirthModel dateOfBirth;

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setSurname(String surname) {
            this.surname = surname;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder setMobile(MobileNumberModel mobile) {
            this.mobile = mobile;
            return this;
        }

        public Builder setIndustry(Industry industry) {
            this.industry = industry;
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

        public Builder setCompanyBusinessAddress(AddressModel companyBusinessAddress) {
            this.companyBusinessAddress = companyBusinessAddress;
            return this;
        }

        public Builder setFeeGroup(String feeGroup) {
            this.feeGroup = feeGroup;
            return this;
        }

        public Builder setBaseCurrency(String baseCurrency) {
            this.baseCurrency = baseCurrency;
            return this;
        }

        public Builder setDateOfBirth(DateOfBirthModel dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public PatchCorporateModel build(){ return new PatchCorporateModel(this);}
    }

    public static Builder newBuilder(){
        return new Builder();
    }

    public static Builder DefaultPatchCorporateModel(){
        return new Builder()
                .setTag(RandomStringUtils.randomAlphabetic(6))
                .setEmail(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(6)))
                .setBaseCurrency(Currency.getRandomCurrency().toString())
                .setSourceOfFunds(CorporateSourceOfFunds.getRandomSourceOfFunds())
                .setSourceOfFundsOther(CorporateSourceOfFunds.getRandomSourceOfFunds().toString())
                .setIndustry(Industry.getRandomIndustry())
                .setCompanyBusinessAddress(AddressModel.RandomAddressModel())
                .setSurname(RandomStringUtils.randomAlphabetic(6))
                .setName(RandomStringUtils.randomAlphabetic(6))
                .setMobile(MobileNumberModel.random())
                .setDateOfBirth(new DateOfBirthModel(1991, 2, 2))
                .setFeeGroup("DEFAULT");
    }

    @SneakyThrows
    public static String patchCorporateString() {
        return new ObjectMapper().writeValueAsString(DefaultPatchCorporateModel().setEmail(null).setMobile(null).build());
    }
}
