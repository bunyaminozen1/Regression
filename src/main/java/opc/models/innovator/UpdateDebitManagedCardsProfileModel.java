package opc.models.innovator;

public class UpdateDebitManagedCardsProfileModel {
    private final AbstractUpdateManagedCardsProfileModel updateManagedCardsProfileRequest;

    public UpdateDebitManagedCardsProfileModel(final Builder builder) {
        this.updateManagedCardsProfileRequest = builder.updateManagedCardsProfileRequest;
    }

    public AbstractUpdateManagedCardsProfileModel getUpdateManagedCardsProfileRequest() {
        return updateManagedCardsProfileRequest;
    }

    public static class Builder {
        private AbstractUpdateManagedCardsProfileModel updateManagedCardsProfileRequest;

        public Builder setUpdateManagedCardsProfileRequest(AbstractUpdateManagedCardsProfileModel updateManagedCardsProfileRequest) {
            this.updateManagedCardsProfileRequest = updateManagedCardsProfileRequest;
            return this;
        }

        public UpdateDebitManagedCardsProfileModel build() { return new UpdateDebitManagedCardsProfileModel(this); }
    }

    public static Builder builder() { return new Builder(); }
}
