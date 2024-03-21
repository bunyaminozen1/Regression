package opc.models.multi.directdebit;

import opc.enums.opc.CollectionRejectionReason;

public class RejectCollectionModel {

    private final CollectionRejectionReason rejectionReason;

    public RejectCollectionModel(final Builder builder) {
        this.rejectionReason = builder.rejectionReason;
    }

    public CollectionRejectionReason getRejectionReason() {
        return rejectionReason;
    }

    public static class Builder {
        private CollectionRejectionReason rejectionReason;

        public Builder setRejectionReason(CollectionRejectionReason rejectionReason) {
            this.rejectionReason = rejectionReason;
            return this;
        }

        public RejectCollectionModel build() { return new RejectCollectionModel(this); }
    }

    public static RejectCollectionModel rejectCollectionModel(final CollectionRejectionReason collectionRejectionReason) {
        return new Builder().setRejectionReason(collectionRejectionReason).build();
    }
}
