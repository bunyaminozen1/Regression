package opc.models.multi.consumers;

import opc.enums.opc.KycLevel;

import java.util.List;

public class StartKycModel {

    public StartKycModel(final Builder builder) {
        this.kycLevel = builder.kycLevel;
        this.prefillDetails = builder.prefillDetails;
    }

    private final KycLevel kycLevel;

    private final List<PrefillDetailsModel> prefillDetails;

    public KycLevel getKycLevel() {
        return kycLevel;
    }

    public List<PrefillDetailsModel> getPrefillDetails() {
        return prefillDetails;
    }

    public static class Builder {
        private KycLevel kycLevel;
        private List<PrefillDetailsModel> prefillDetails;

        public Builder setKycLevel(KycLevel kycLevel) {
            this.kycLevel = kycLevel;
            return this;
        }

        public Builder setPrefillDetails(List<PrefillDetailsModel> prefillDetails) {
            this.prefillDetails = prefillDetails;
            return this;
        }

        public StartKycModel build() { return new StartKycModel(this); }
    }

    public static StartKycModel startKycModel(final KycLevel kycLevel) {
        return new Builder().setKycLevel(kycLevel).build();
    }

    public static StartKycModel startKycModelWithPrefillDetails(final KycLevel kycLevel, final List<PrefillDetailsModel> prefillDetails) {
        return new Builder().setKycLevel(kycLevel).setPrefillDetails(prefillDetails).build();
    }

    public static Builder builder() {
        return new Builder();
    }
}
