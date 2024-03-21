package opc.models.multi.transfers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import commons.enums.Currency;
import opc.enums.opc.ManagedInstrumentType;
import opc.models.multi.sends.SendFundsModel;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class TransferFundsModel {
    private final String profileId;
    private final String tag;
    private final ManagedInstrumentTypeId source;
    private final ManagedInstrumentTypeId destination;
    private final CurrencyAmount destinationAmount;
    private final String description;
    private final String scheduledTimestamp;

    public TransferFundsModel(final Builder builder) {
        this.profileId = builder.profileId;
        this.tag = builder.tag;
        this.source = builder.source;
        this.destination = builder.destination;
        this.destinationAmount = builder.destinationAmount;
        this.description = builder.description;
        this.scheduledTimestamp = builder.scheduledTimestamp;
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

    public String getDescription() {
        return description;
    }

    public String getScheduledTimestamp() {
        return scheduledTimestamp;
    }

    public static class Builder {
        private String profileId;
        private String tag;
        private ManagedInstrumentTypeId source;
        private ManagedInstrumentTypeId destination;
        private CurrencyAmount destinationAmount;
        private String description;
        private String scheduledTimestamp;

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

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setScheduledTimestamp(String scheduledTimestamp) {
            this.scheduledTimestamp = scheduledTimestamp;
            return this;
        }

        public TransferFundsModel build() {
            return new TransferFundsModel(this);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder DefaultTransfersModel(final String profileId,
                                                final String currency,
                                                final Long transferAmount,
                                                final Pair<String, ManagedInstrumentType> sourceInstrument,
                                                final Pair<String, ManagedInstrumentType> destinationInstrument){
        return new Builder()
                .setProfileId(profileId)
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setDestinationAmount(new CurrencyAmount(currency, transferAmount))
                .setSource(new ManagedInstrumentTypeId(sourceInstrument.getLeft(), sourceInstrument.getRight()))
                .setDestination(new ManagedInstrumentTypeId(destinationInstrument.getLeft(), destinationInstrument.getRight()))
                .setDescription(RandomStringUtils.randomAlphabetic(10));
    }

    @SneakyThrows
    public static String transferFundsString(final String profileId,
                                             final String sourceManagedAccountId,
                                             final String destinationManagedAccountId) {
        return new ObjectMapper().writeValueAsString(new SendFundsModel.Builder()
                .setProfileId(profileId)
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), Long.parseLong(RandomStringUtils.randomNumeric(2))))
                .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                .build());
    }
}