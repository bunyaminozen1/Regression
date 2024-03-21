package opc.models.simulator;

public class DirectDebitAccountDetailsModel {

    private final String accountNumber;
    private final String sortCode;

    public DirectDebitAccountDetailsModel(final Builder builder) {
        this.accountNumber = builder.accountNumber;
        this.sortCode = builder.sortCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getSortCode() {
        return sortCode;
    }

    public static class Builder {
        private String accountNumber;
        private String sortCode;

        public Builder setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public Builder setSortCode(String sortCode) {
            this.sortCode = sortCode;
            return this;
        }

        public DirectDebitAccountDetailsModel build() { return new DirectDebitAccountDetailsModel(this); }
    }

    public static Builder builder() { return new Builder(); }
}
