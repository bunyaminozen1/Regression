package opc.models.innovator;

import opc.enums.opc.IdentityType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UpdatePrepaidManagedCardsProfileModel {
    private final AbstractUpdateManagedCardsProfileModel updateManagedCardsProfileRequest;
    private final List<String> currency;
    private final List<String> fiProvider;
    private final List<String> channelProvider;

    public UpdatePrepaidManagedCardsProfileModel(final Builder builder) {
        this.updateManagedCardsProfileRequest = builder.updateManagedCardsProfileRequest;
        this.currency = builder.currency;
        this.fiProvider = builder.fiProvider;
        this.channelProvider = builder.channelProvider;
    }

    public AbstractUpdateManagedCardsProfileModel getUpdateManagedCardsProfileRequest() {
        return updateManagedCardsProfileRequest;
    }

    public List<String> getCurrency() {
        return currency;
    }

    public List<String> getFiProvider() {
        return fiProvider;
    }

    public List<String> getChannelProvider() {
        return channelProvider;
    }

    public static class Builder {
        private AbstractUpdateManagedCardsProfileModel updateManagedCardsProfileRequest;
        private List<String> currency;
        private List<String> fiProvider;
        private List<String> channelProvider;

        public Builder setUpdateManagedCardsProfileRequest(AbstractUpdateManagedCardsProfileModel updateManagedCardsProfileRequest) {
            this.updateManagedCardsProfileRequest = updateManagedCardsProfileRequest;
            return this;
        }

        public Builder setCurrency(List<String> currency) {
            this.currency = currency;
            return this;
        }

        public Builder setFiProvider(List<String> fiProvider) {
            this.fiProvider = fiProvider;
            return this;
        }

        public Builder setChannelProvider(List<String> channelProvider) {
            this.channelProvider = channelProvider;
            return this;
        }

        public UpdatePrepaidManagedCardsProfileModel build() { return new UpdatePrepaidManagedCardsProfileModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder DefaultUpdatePrepaidManagedCardsProfileModel(final IdentityType identityType) {
        return new Builder()
                .setCurrency(Arrays.asList("EUR", "USD", "GBP"))
                .setFiProvider(Collections.singletonList("paynetics_eea"))
                .setChannelProvider(Collections.singletonList("gps"))
                .setUpdateManagedCardsProfileRequest(AbstractUpdateManagedCardsProfileModel
                        .DefaultAbstractUpdateManagedCardsProfileModel(identityType, "PREPAID").build());
    }
}
