package opc.models.multi.sends;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import commons.enums.Currency;
import opc.enums.opc.ManagedInstrumentType;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import org.apache.commons.lang3.RandomStringUtils;

public class SendFundsModel {
    private final String profileId;
    private final String tag;
    private final ManagedInstrumentTypeId source;
    private final ManagedInstrumentTypeId destination;
    private final CurrencyAmount destinationAmount;
    private final String description;
    private final String scheduledTimestamp;

    public SendFundsModel(final Builder builder) {
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

    public String getDescription() {return description; }

    public String getScheduledTimestamp() {return scheduledTimestamp; }

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

        public SendFundsModel build() { return new SendFundsModel(this); }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder DefaultSendsModel(final String profileId,
                                            final String managedAccountId,
                                            final String managedCardId,
                                            final String currency,
                                            final Long amount){
        return new Builder()
                .setProfileId(profileId)
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setDestinationAmount(new CurrencyAmount(currency, amount))
                .setSource(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                .setDestination(new ManagedInstrumentTypeId(managedCardId, ManagedInstrumentType.MANAGED_CARDS))
                .setDescription(RandomStringUtils.randomAlphabetic(10));
    }

    public static Builder DefaultSendsModel(final String profileId,
                                            final ManagedInstrumentType sourceInstrumentType,
                                            final String sourceInstrumentId,
                                            final ManagedInstrumentType destinationInstrumentType,
                                            final String destinationInstrumentId,
                                            final String currency,
                                            final Long amount){
        return new Builder()
            .setProfileId(profileId)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDestinationAmount(new CurrencyAmount(currency, amount))
            .setSource(new ManagedInstrumentTypeId(sourceInstrumentId, sourceInstrumentType))
            .setDestination(new ManagedInstrumentTypeId(destinationInstrumentId, destinationInstrumentType));
    }

    @SneakyThrows
    public static String sendFundsString(final String profileId,
                                         final String sourceManagedAccountId,
                                         final String destinationManagedAccountId) {
        return new ObjectMapper().writeValueAsString(new Builder()
                .setProfileId(profileId)
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .setDestinationAmount(new CurrencyAmount(Currency.EUR.name(), Long.parseLong(RandomStringUtils.randomNumeric(2))))
                .setSource(new ManagedInstrumentTypeId(sourceManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                .setDestination(new ManagedInstrumentTypeId(destinationManagedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                .build());
    }
}
