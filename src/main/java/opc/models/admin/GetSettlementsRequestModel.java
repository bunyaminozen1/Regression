package opc.models.admin;

import java.util.List;

public class GetSettlementsRequestModel {

    private final String cardId;
    private final List<String> state;
    private final List<String> merchantTransactionType;

    public GetSettlementsRequestModel(final Builder builder) {
        this.cardId = builder.cardId;
        this.state = builder.state;
        this.merchantTransactionType = builder.merchantTransactionType;
    }

    public String getCardId() {
        return cardId;
    }

    public List<String> getState() {
        return state;
    }

    public List<String> getMerchantTransactionType() {
        return merchantTransactionType;
    }

    public static class Builder {
        private String cardId;
        private List<String> state;
        private List<String> merchantTransactionType;

        public Builder setCardId(String cardId) {
            this.cardId = cardId;
            return this;
        }

        public Builder setState(List<String> state) {
            this.state = state;
            return this;
        }

        public Builder setMerchantTransactionType(List<String> merchantTransactionType) {
            this.merchantTransactionType = merchantTransactionType;
            return this;
        }

        public GetSettlementsRequestModel build() { return new GetSettlementsRequestModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }
}
