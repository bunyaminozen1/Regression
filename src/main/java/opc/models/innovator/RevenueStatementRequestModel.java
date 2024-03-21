package opc.models.innovator;

import opc.models.shared.PagingModel;
import opc.models.shared.TxTypeIdModel;

import java.util.List;

public class RevenueStatementRequestModel {

    private final PagingModel paging;
    private final Long createdFrom;
    private final Long createdTo;
    private final TxTypeIdModel feeInstrumentOwner;
    private final List<String> currencies;
    private final TxTypeIdModel txId;
    private final String txIdType;

    public RevenueStatementRequestModel(final Builder builder) {
        this.paging = builder.paging;
        this.createdFrom = builder.createdFrom;
        this.createdTo = builder.createdTo;
        this.feeInstrumentOwner = builder.feeInstrumentOwner;
        this.currencies = builder.currencies;
        this.txId = builder.txId;
        this.txIdType = builder.txIdType;
    }

    public PagingModel getPaging() {
        return paging;
    }

    public Long getCreatedFrom() {
        return createdFrom;
    }

    public Long getCreatedTo() {
        return createdTo;
    }

    public TxTypeIdModel getFeeInstrumentOwner() {
        return feeInstrumentOwner;
    }

    public List<String> getCurrencies() {
        return currencies;
    }

    public TxTypeIdModel getTxId() {
        return txId;
    }

    public String getTxIdType() {
        return txIdType;
    }

    public static class Builder {
        private PagingModel paging;
        private Long createdFrom;
        private Long createdTo;
        private TxTypeIdModel feeInstrumentOwner;
        private List<String> currencies;
        private TxTypeIdModel txId;
        private String txIdType;

        public Builder setPaging(PagingModel paging) {
            this.paging = paging;
            return this;
        }

        public Builder setCreatedFrom(Long createdFrom) {
            this.createdFrom = createdFrom;
            return this;
        }

        public Builder setCreatedTo(Long createdTo) {
            this.createdTo = createdTo;
            return this;
        }

        public Builder setFeeInstrumentOwner(TxTypeIdModel feeInstrumentOwner) {
            this.feeInstrumentOwner = feeInstrumentOwner;
            return this;
        }

        public Builder setCurrencies(List<String> currencies) {
            this.currencies = currencies;
            return this;
        }

        public Builder setTxId(TxTypeIdModel txId) {
            this.txId = txId;
            return this;
        }

        public Builder setTxIdType(String txIdType) {
            this.txIdType = txIdType;
            return this;
        }

        public RevenueStatementRequestModel build() { return new RevenueStatementRequestModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }
}