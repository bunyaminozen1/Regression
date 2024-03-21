package opc.models.multi.managedcards;

import opc.enums.opc.CardMode;

public class DebitCardModeDetailsModel extends CardDetailsModel {

    private final String managedAccountId;
    private final long availableToSpend;
    private final String interval;

    public DebitCardModeDetailsModel(final Builder builder) {
        super(builder.cardMode);
        this.managedAccountId = builder.managedAccountId;
        this.availableToSpend = builder.availableToSpend;
        this.interval = builder.interval;
    }

    public String getManagedAccountId() {
        return managedAccountId;
    }

    public long getAvailableToSpend() {
        return availableToSpend;
    }

    public String getInterval() {
        return interval;
    }

    public static class Builder {
        private String managedAccountId;
        private long availableToSpend;
        private String interval;
        private CardMode cardMode;

        public Builder setManagedAccountId(String managedAccountId) {
            this.managedAccountId = managedAccountId;
            return this;
        }

        public Builder setAvailableToSpend(long availableToSpend) {
            this.availableToSpend = availableToSpend;
            return this;
        }

        public Builder setInterval(String interval) {
            this.interval = interval;
            return this;
        }

        public Builder setCardMode(CardMode cardMode) {
            this.cardMode = cardMode;
            return this;
        }

        public DebitCardModeDetailsModel build() { return new DebitCardModeDetailsModel(this); }
    }

    public static Builder builder() { return new Builder(); }
}
