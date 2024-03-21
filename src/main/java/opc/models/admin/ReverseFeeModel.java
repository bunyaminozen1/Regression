package opc.models.admin;

import opc.models.shared.ManagedInstrumentTypeId;
import opc.models.shared.TxTypeIdModel;

public class ReverseFeeModel {

    private final TxTypeIdModel txId;
    private final ManagedInstrumentTypeId instrumentId;
    private final String note;

    public ReverseFeeModel(final Builder builder) {
        this.txId = builder.txId;
        this.instrumentId = builder.instrumentId;
        this.note = builder.note;
    }

    public TxTypeIdModel getTxId() {
        return txId;
    }

    public ManagedInstrumentTypeId getInstrumentId() {
        return instrumentId;
    }

    public String getNote() {
        return note;
    }

    public static class Builder {
        private TxTypeIdModel txId;
        private ManagedInstrumentTypeId instrumentId;
        private String note;

        public Builder setTxId(TxTypeIdModel txId) {
            this.txId = txId;
            return this;
        }

        public Builder setInstrumentId(ManagedInstrumentTypeId instrumentId) {
            this.instrumentId = instrumentId;
            return this;
        }

        public Builder setNote(String note) {
            this.note = note;
            return this;
        }

        public ReverseFeeModel build() { return new ReverseFeeModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }
}
