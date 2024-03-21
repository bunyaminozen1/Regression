package opc.models.innovator;

import opc.enums.opc.CardLevelClassification;
import opc.enums.opc.InstrumentType;

import java.util.Optional;

public class DebitUnassignedCardBatchModel {
    private final String parentManagedAccountId;
    private final AbstractCreateUnassignedCardBatchRequest debitUnassignedCardBatchRequest;

    public DebitUnassignedCardBatchModel(final Builder builder) {
        this.parentManagedAccountId = builder.parentManagedAccountId;
        this.debitUnassignedCardBatchRequest = builder.debitUnassignedCardBatchRequest;
    }

    public String getParentManagedAccountId() {
        return parentManagedAccountId;
    }

    public AbstractCreateUnassignedCardBatchRequest getDebitUnassignedCardBatchRequest() {
        return debitUnassignedCardBatchRequest;
    }

    public static class Builder {
        private String parentManagedAccountId;
        private AbstractCreateUnassignedCardBatchRequest debitUnassignedCardBatchRequest;

        public Builder setParentManagedAccountId(String parentManagedAccountId) {
            this.parentManagedAccountId = parentManagedAccountId;
            return this;
        }

        public Builder setDebitUnassignedCardBatchRequest(AbstractCreateUnassignedCardBatchRequest debitUnassignedCardBatchRequest) {
            this.debitUnassignedCardBatchRequest = debitUnassignedCardBatchRequest;
            return this;
        }

        public DebitUnassignedCardBatchModel build(){ return new DebitUnassignedCardBatchModel(this); }
    }

    public static Builder DefaultDebitUnassignedCardBatchModel(final String managedCardsProfileId,
                                                               final String managedAccountId,
                                                               final CardLevelClassification cardLevelClassification,
                                                               final InstrumentType instrumentType,
                                                               final String verificationCode,
                                                               final Optional<Integer> batchSize) {
        return new Builder()
                .setParentManagedAccountId(managedAccountId)
                .setDebitUnassignedCardBatchRequest(AbstractCreateUnassignedCardBatchRequest
                        .DefaultCardPoolReplenishModel(managedCardsProfileId, cardLevelClassification, instrumentType, verificationCode, batchSize).build());
    }
}
