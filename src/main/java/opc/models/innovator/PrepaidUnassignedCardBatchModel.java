package opc.models.innovator;

import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.InstrumentType;

import java.util.Optional;

public class PrepaidUnassignedCardBatchModel {
    private final String currency;
    private final AbstractCreateUnassignedCardBatchRequest prepaidUnassignedCardBatchRequest;

    public PrepaidUnassignedCardBatchModel(final Builder builder) {
        this.currency = builder.currency;
        this.prepaidUnassignedCardBatchRequest = builder.prepaidUnassignedCardBatchRequest;
    }

    public String getCurrency() {
        return currency;
    }

    public AbstractCreateUnassignedCardBatchRequest getPrepaidUnassignedCardBatchRequest() {
        return prepaidUnassignedCardBatchRequest;
    }

    public static class Builder {
        private String currency;
        private AbstractCreateUnassignedCardBatchRequest prepaidUnassignedCardBatchRequest;

        public Builder setCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder setPrepaidUnassignedCardBatchRequest(AbstractCreateUnassignedCardBatchRequest prepaidUnassignedCardBatchRequest) {
            this.prepaidUnassignedCardBatchRequest = prepaidUnassignedCardBatchRequest;
            return this;
        }

        public PrepaidUnassignedCardBatchModel build(){ return new PrepaidUnassignedCardBatchModel(this); }
    }

    public static Builder DefaultPrepaidUnassignedCardBatchModel(final String managedCardsProfileId,
                                                                 final String currency,
                                                                 final CardLevelClassification cardLevelClassification,
                                                                 final InstrumentType instrumentType,
                                                                 final String verificationCode,
                                                                 final Optional<Integer> batchSize) {
        return new Builder()
                .setCurrency(currency)
                .setPrepaidUnassignedCardBatchRequest(AbstractCreateUnassignedCardBatchRequest
                        .DefaultCardPoolReplenishModel(managedCardsProfileId, cardLevelClassification, instrumentType, verificationCode, batchSize).build());
    }
}
