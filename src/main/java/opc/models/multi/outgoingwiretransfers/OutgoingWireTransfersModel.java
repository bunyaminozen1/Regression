package opc.models.multi.outgoingwiretransfers;

import com.github.javafaker.Faker;
import opc.enums.opc.ManagedInstrumentType;
import opc.enums.opc.OwtType;
import opc.models.shared.CurrencyAmount;
import opc.models.shared.ManagedInstrumentTypeId;
import org.apache.commons.lang3.RandomStringUtils;

public class OutgoingWireTransfersModel {
    private final String profileId;
    private final String tag;
    private final ManagedInstrumentTypeId sourceInstrument;
    private final CurrencyAmount transferAmount;
    private final String description;
    private final Beneficiary destinationBeneficiary;
    private final String scheduledTimestamp;

    public OutgoingWireTransfersModel(final Builder builder) {
        this.profileId = builder.profileId;
        this.tag = builder.tag;
        this.sourceInstrument = builder.sourceInstrument;
        this.transferAmount = builder.transferAmount;
        this.description = builder.description;
        this.destinationBeneficiary = builder.destinationBeneficiary;
        this.scheduledTimestamp = builder.scheduledTimestamp;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getTag() {
        return tag;
    }

    public ManagedInstrumentTypeId getSourceInstrument() {
        return sourceInstrument;
    }

    public CurrencyAmount getTransferAmount() {
        return transferAmount;
    }

    public String getDescription() {
        return description;
    }

    public Beneficiary getDestinationBeneficiary() {
        return destinationBeneficiary;
    }
    public String getScheduledTimestamp() {return scheduledTimestamp; }

    public static class Builder {
        private String profileId;
        private String tag;
        private ManagedInstrumentTypeId sourceInstrument;
        private CurrencyAmount transferAmount;
        private String description;
        private Beneficiary destinationBeneficiary;
        private String scheduledTimestamp;

        public Builder setProfileId(String profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setSourceInstrument(ManagedInstrumentTypeId sourceInstrument) {
            this.sourceInstrument = sourceInstrument;
            return this;
        }

        public Builder setTransferAmount(CurrencyAmount transferAmount) {
            this.transferAmount = transferAmount;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setDestinationBeneficiary(Beneficiary destinationBeneficiary) {
            this.destinationBeneficiary = destinationBeneficiary;
            return this;
        }

        public Builder setScheduledTimestamp(String scheduledTimestamp) {
            this.scheduledTimestamp = scheduledTimestamp;
            return this;
        }

        public OutgoingWireTransfersModel build() { return new OutgoingWireTransfersModel(this); }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder DefaultOutgoingWireTransfersModel(final String profileId,
                                                            final String managedAccountId,
                                                            final String currency,
                                                            final Long amount,
                                                            final OwtType owtType){
        return new Builder()
                .setProfileId(profileId)
                .setTag(RandomStringUtils.randomAlphabetic(15))
                .setDescription(RandomStringUtils.randomAlphabetic(5))
                .setDestinationBeneficiary(owtType.equals(OwtType.SEPA) ?
                        Beneficiary.DefaultBeneficiaryWithSepa().build() :
                        Beneficiary.DefaultBeneficiaryWithFasterPayments().build())
                .setSourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                .setTransferAmount(new CurrencyAmount(currency, amount));
    }

    public static Builder BeneficiaryOutgoingWireTransfersModel(final String profileId,
                                                            final String managedAccountId,
                                                            final String beneficiaryId,
                                                            final String currency,
                                                            final Long amount){
        return new Builder()
            .setProfileId(profileId)
            .setTag(RandomStringUtils.randomAlphabetic(5))
            .setDescription(RandomStringUtils.randomAlphabetic(5))
            .setDestinationBeneficiary(Beneficiary.DefaultBeneficiaryId(beneficiaryId).build())
            .setSourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
            .setTransferAmount(new CurrencyAmount(currency, amount));

    }

    public static Builder dataOutgoingWireTransfersModel(final String profileId,
                                                            final String managedAccountId,
                                                            final String currency,
                                                            final Long amount,
                                                            final OwtType owtType){
        final Faker faker = new Faker();

        return new Builder()
                .setProfileId(profileId)
                .setDescription(faker.dog().name())
                .setTag(faker.dog().name())
                .setDestinationBeneficiary(owtType.equals(OwtType.SEPA) ?
                        Beneficiary.DefaultBeneficiaryWithSepa().build() :
                        Beneficiary.DefaultBeneficiaryWithFasterPayments().build())
                .setSourceInstrument(new ManagedInstrumentTypeId(managedAccountId, ManagedInstrumentType.MANAGED_ACCOUNTS))
                .setTransferAmount(new CurrencyAmount(currency, amount));
    }
}