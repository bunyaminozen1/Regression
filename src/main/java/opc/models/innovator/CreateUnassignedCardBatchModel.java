package opc.models.innovator;

import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.InstrumentType;

import java.util.Optional;

public class CreateUnassignedCardBatchModel {
    private final PrepaidUnassignedCardBatchModel prepaidCardBatchRequest;
    private final String cardFundingType;
    private final DebitUnassignedCardBatchModel debitCardBatchRequest;

    public CreateUnassignedCardBatchModel(final Builder builder) {
        this.prepaidCardBatchRequest = builder.prepaidCardBatchRequest;
        this.cardFundingType = builder.cardFundingType;
        this.debitCardBatchRequest = builder.debitCardBatchRequest;
    }

    public PrepaidUnassignedCardBatchModel getPrepaidCardBatchRequest() {
        return prepaidCardBatchRequest;
    }

    public String getCardFundingType() {
        return cardFundingType;
    }

    public DebitUnassignedCardBatchModel getDebitCardBatchRequest() {
        return debitCardBatchRequest;
    }

    public static class Builder {
        private PrepaidUnassignedCardBatchModel prepaidCardBatchRequest;
        private String cardFundingType;
        private DebitUnassignedCardBatchModel debitCardBatchRequest;

        public Builder setPrepaidCardBatchRequest(PrepaidUnassignedCardBatchModel prepaidCardBatchRequest) {
            this.prepaidCardBatchRequest = prepaidCardBatchRequest;
            return this;
        }

        public Builder setCardFundingType(String cardFundingType) {
            this.cardFundingType = cardFundingType;
            return this;
        }

        public Builder setDebitCardBatchRequest(DebitUnassignedCardBatchModel debitCardBatchRequest) {
            this.debitCardBatchRequest = debitCardBatchRequest;
            return this;
        }

        public CreateUnassignedCardBatchModel build() { return new CreateUnassignedCardBatchModel(this); }
    }

    public static Builder DefaultCreatePrepaidUnassignedCardBatchModel(final String managedCardsProfileId,
                                                                       final String currency,
                                                                       final CardLevelClassification cardLevelClassification,
                                                                       final InstrumentType instrumentType,
                                                                       final String verificationCode,
                                                                       final Optional<Integer> batchSize){
        return new Builder()
                .setCardFundingType("PREPAID")
                .setPrepaidCardBatchRequest(PrepaidUnassignedCardBatchModel
                        .DefaultPrepaidUnassignedCardBatchModel(managedCardsProfileId, currency, cardLevelClassification, instrumentType, verificationCode, batchSize).build());
    }

    public static Builder DefaultCreateDebitUnassignedCardBatchModel(final String managedCardsProfileId,
                                                                     final String managedAccountId,
                                                                     final CardLevelClassification cardLevelClassification,
                                                                     final InstrumentType instrumentType,
                                                                     final String verificationCode,
                                                                     final Optional<Integer> batchSize){
        return new Builder()
                .setCardFundingType("DEBIT")
                .setDebitCardBatchRequest(DebitUnassignedCardBatchModel
                        .DefaultDebitUnassignedCardBatchModel(managedCardsProfileId, managedAccountId, cardLevelClassification, instrumentType, verificationCode, batchSize).build());
    }
}
