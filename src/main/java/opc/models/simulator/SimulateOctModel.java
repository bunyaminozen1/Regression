package opc.models.simulator;

import opc.models.shared.CurrencyAmount;

public class SimulateOctModel {
    private final String cardNumber;
    private final String cvv;
    private final String expiryDate;
    private final String merchantName;
    private final CurrencyAmount transactionAmount;

    public SimulateOctModel(final Builder builder) {
        this.cardNumber = builder.cardNumber;
        this.cvv = builder.cvv;
        this.expiryDate = builder.expiryDate;
        this.merchantName = builder.merchantName;
        this.transactionAmount = builder.transactionAmount;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public String getCvv() {
        return cvv;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public CurrencyAmount getTransactionAmount() {
        return transactionAmount;
    }

    public static class Builder {
        private String cardNumber;
        private String cvv;
        private String expiryDate;
        private String merchantName;
        private CurrencyAmount transactionAmount;

        public Builder setCardNumber(String cardNumber) {
            this.cardNumber = cardNumber;
            return this;
        }

        public Builder setCvv(String cvv) {
            this.cvv = cvv;
            return this;
        }

        public Builder setExpiryDate(String expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder setMerchantName(String merchantName) {
            this.merchantName = merchantName;
            return this;
        }

        public Builder setTransactionAmount(CurrencyAmount transactionAmount) {
            this.transactionAmount = transactionAmount;
            return this;
        }

        public SimulateOctModel build() { return new SimulateOctModel(this); }
    }

    public static Builder builder() {
        return new Builder()
                .setMerchantName("BetFair Winnings Division");
    }
}
