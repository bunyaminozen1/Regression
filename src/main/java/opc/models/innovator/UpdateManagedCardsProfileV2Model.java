package opc.models.innovator;

public class UpdateManagedCardsProfileV2Model {
    private final UpdatePrepaidManagedCardsProfileModel updatePrepaidProfileRequest;
    private final UpdateDebitManagedCardsProfileModel updateDebitProfileRequest;
    private final String cardFundingType;

    public UpdateManagedCardsProfileV2Model(final Builder builder) {
        this.updatePrepaidProfileRequest = builder.updatePrepaidProfileRequest;
        this.updateDebitProfileRequest = builder.updateDebitProfileRequest;
        this.cardFundingType = builder.cardFundingType;
    }

    public UpdatePrepaidManagedCardsProfileModel getUpdatePrepaidProfileRequest() {
        return updatePrepaidProfileRequest;
    }

    public UpdateDebitManagedCardsProfileModel getUpdateDebitProfileRequest() {
        return updateDebitProfileRequest;
    }

    public String getCardFundingType() {
        return cardFundingType;
    }

    public static class Builder {
        private UpdatePrepaidManagedCardsProfileModel updatePrepaidProfileRequest;
        private UpdateDebitManagedCardsProfileModel updateDebitProfileRequest;
        private String cardFundingType;

        public Builder setUpdatePrepaidProfileRequest(UpdatePrepaidManagedCardsProfileModel updatePrepaidProfileRequest) {
            this.updatePrepaidProfileRequest = updatePrepaidProfileRequest;
            return this;
        }

        public Builder setUpdateDebitProfileRequest(UpdateDebitManagedCardsProfileModel updateDebitProfileRequest) {
            this.updateDebitProfileRequest = updateDebitProfileRequest;
            return this;
        }

        public Builder setCardFundingType(String cardFundingType) {
            this.cardFundingType = cardFundingType;
            return this;
        }

        public UpdateManagedCardsProfileV2Model build() { return new UpdateManagedCardsProfileV2Model(this); }
    }

    public static Builder builder(){
        return new Builder();
    }
}
