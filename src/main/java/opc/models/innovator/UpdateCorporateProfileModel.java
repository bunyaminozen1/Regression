package opc.models.innovator;

import commons.models.innovator.IdentityProfileAuthenticationModel;

import java.util.List;

public class UpdateCorporateProfileModel {
    private final List<FeeModel> customFee;
    private final IdentityProfileAuthenticationModel accountInformationFactors;
    private final IdentityProfileAuthenticationModel paymentInitiationFactors;
    private final IdentityProfileAuthenticationModel beneficiaryManagementFactors;

    public UpdateCorporateProfileModel(final Builder builder) {
        this.customFee = builder.customFee;
        this.accountInformationFactors = builder.accountInformationFactors;
        this.paymentInitiationFactors = builder.paymentInitiationFactors;
        this.beneficiaryManagementFactors = builder.beneficiaryManagementFactors;
    }

    public List<FeeModel> getCustomFee() {
        return customFee;
    }

    public IdentityProfileAuthenticationModel getAccountInformationFactors(){return accountInformationFactors;}
    public IdentityProfileAuthenticationModel getPaymentInitiationFactors(){return paymentInitiationFactors;}
    public IdentityProfileAuthenticationModel getBeneficiaryManagementFactors(){return beneficiaryManagementFactors;}

    public static class Builder {
        private List<FeeModel> customFee;
        private IdentityProfileAuthenticationModel accountInformationFactors;
        private IdentityProfileAuthenticationModel paymentInitiationFactors;
        private IdentityProfileAuthenticationModel beneficiaryManagementFactors;

        public UpdateCorporateProfileModel.Builder setCustomFee(List<FeeModel> customFee) {
            this.customFee = customFee;
            return this;
        }

        public UpdateCorporateProfileModel.Builder setAccountInformationFactors(IdentityProfileAuthenticationModel accountInformationFactors) {
            this.accountInformationFactors = accountInformationFactors;
            return this;
        }

        public UpdateCorporateProfileModel.Builder setPaymentInitiationFactors(IdentityProfileAuthenticationModel paymentInitiationFactors) {
            this.paymentInitiationFactors = paymentInitiationFactors;
            return this;
        }

        public UpdateCorporateProfileModel.Builder setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel beneficiaryManagementFactors) {
            this.beneficiaryManagementFactors = beneficiaryManagementFactors;
            return this;
        }

        public UpdateCorporateProfileModel build() {
            return new UpdateCorporateProfileModel(this);
        }
    }

    public static UpdateCorporateProfileModel.Builder builder() {
        return new UpdateCorporateProfileModel.Builder();
    }
}
