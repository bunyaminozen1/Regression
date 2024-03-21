package opc.models.backoffice;

import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;

public class TransferModel {

    private final String profileId;
    private final String tag;
    private final ManagedInstrumentTypeId source;
    private final ManagedInstrumentTypeId destination;
    private final CurrencyAmount destinationAmount;

    public TransferModel(final Builder builder) {
        this.profileId = builder.profileId;
        this.tag = builder.tag;
        this.source = builder.source;
        this.destination = builder.destination;
        this.destinationAmount = builder.destinationAmount;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getTag() {
        return tag;
    }

    public ManagedInstrumentTypeId getSource() {
        return source;
    }

    public ManagedInstrumentTypeId getDestination() {
        return destination;
    }

    public CurrencyAmount getDestinationAmount() {
        return destinationAmount;
    }

    public static class Builder {
        private String profileId;
        private String tag;
        private ManagedInstrumentTypeId source;
        private ManagedInstrumentTypeId destination;
        private CurrencyAmount destinationAmount;

        public Builder setProfileId(String profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setSource(ManagedInstrumentTypeId source) {
            this.source = source;
            return this;
        }

        public Builder setDestination(ManagedInstrumentTypeId destination) {
            this.destination = destination;
            return this;
        }

        public Builder setDestinationAmount(CurrencyAmount destinationAmount) {
            this.destinationAmount = destinationAmount;
            return this;
        }

        public TransferModel build() { return new TransferModel(this); }
    }

    public static Builder newBuilder() {
        return new Builder();
    }
}
