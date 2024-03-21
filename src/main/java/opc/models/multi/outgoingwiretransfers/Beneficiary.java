package opc.models.multi.outgoingwiretransfers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import opc.helpers.ModelHelper;
import opc.enums.opc.CannedIbanState;
import org.apache.commons.lang3.RandomStringUtils;

public class Beneficiary {
    @JsonProperty("name")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String name;
    @JsonProperty("address")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String address;
    @JsonProperty("bankName")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String bankName;
    @JsonProperty("bankAddress")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String bankAddress;
    @JsonProperty("bankCountry")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String bankCountry;
    @JsonProperty("bankAccountDetails")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Object bankAccountDetails;
    @JsonProperty("beneficiaryId")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String beneficiaryId;

    public Beneficiary(final Builder builder) {
        this.name = builder.name;
        this.address = builder.address;
        this.bankName = builder.bankName;
        this.bankAddress = builder.bankAddress;
        this.bankCountry = builder.bankCountry;
        this.bankAccountDetails = builder.bankAccountDetails;
        this.beneficiaryId = builder.beneficiaryId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getBankName() {
        return bankName;
    }

    public String getBankAddress() {
        return bankAddress;
    }

    public String getBankCountry() {
        return bankCountry;
    }

    public Object getBankAccountDetails() {
        return bankAccountDetails;
    }
    public String getBeneficiaryId() { return beneficiaryId; }

    public static class Builder {
        private String name;
        private String address;
        private String bankName;
        private String bankAddress;
        private String bankCountry;
        private Object bankAccountDetails;
        private String beneficiaryId;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder setBankName(String bankName) {
            this.bankName = bankName;
            return this;
        }

        public Builder setBankAddress(String bankAddress) {
            this.bankAddress = bankAddress;
            return this;
        }

        public Builder setBankCountry(String bankCountry) {
            this.bankCountry = bankCountry;
            return this;
        }

        public Builder setBankAccountDetails(Object bankAccountDetails) {
            this.bankAccountDetails = bankAccountDetails;
            return this;
        }

        public Builder setBeneficiaryId(String beneficiaryId) {
            this.beneficiaryId = beneficiaryId;
            return this;
        }

        public Beneficiary build() { return new Beneficiary(this); }
    }
    public static Builder builder() {
        return new Builder();
    }

    public static Builder DefaultBeneficiary() {
        return new Builder()
                .setName(RandomStringUtils.randomAlphabetic(5))
                .setAddress(RandomStringUtils.randomAlphabetic(5))
                .setBankName(RandomStringUtils.randomAlphabetic(5))
                .setBankAddress(RandomStringUtils.randomAlphabetic(5))
                .setBankCountry("MT");
    }

    public static Builder DefaultBeneficiaryId(final String beneficiaryId) {
        return new Builder()
            .setBeneficiaryId(beneficiaryId);
    }

    public static Builder DefaultBeneficiaryWithSepa(final CannedIbanState state) {
        return DefaultBeneficiary()
            .setBankAccountDetails(new SepaBankDetailsModel(state.getIban(), ModelHelper.generateRandomValidBankIdentifierNumber()));
    }

    public static Builder DefaultBeneficiaryWithFasterPayments() {
        return DefaultBeneficiary()
                .setBankAccountDetails(new FasterPaymentsBankDetailsModel(RandomStringUtils.randomNumeric(8),
                        RandomStringUtils.randomNumeric(6).toUpperCase()));
    }

    public static Builder DefaultBeneficiaryWithSepa() {
        return DefaultBeneficiary()
            .setBankAccountDetails(new SepaBankDetailsModel(ModelHelper.generateRandomValidIban(), ModelHelper.generateRandomValidBankIdentifierNumber()));
    }
}