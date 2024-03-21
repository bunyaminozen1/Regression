package opc.models.multi.managedcards;

import opc.enums.opc.CardMode;

public class PrepaidCardModeDetailsModel extends CardDetailsModel {

    private final String currency;
    private final long availableBalance;

    public PrepaidCardModeDetailsModel(final Builder builder) {
        super(builder.cardMode);
        this.currency = builder.currency;
        this.availableBalance = builder.availableBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public long getAvailableBalance() {
        return availableBalance;
    }

    public static class Builder {
        private String currency;
        private long availableBalance;
        private CardMode cardMode;

        public Builder setCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder setAvailableBalance(long availableBalance) {
            this.availableBalance = availableBalance;
            return this;
        }

        public Builder setCardMode(CardMode cardMode) {
            this.cardMode = cardMode;
            return this;
        }

        public PrepaidCardModeDetailsModel build() { return new PrepaidCardModeDetailsModel(this); }
    }

    public static Builder builder() { return new Builder(); }
}
