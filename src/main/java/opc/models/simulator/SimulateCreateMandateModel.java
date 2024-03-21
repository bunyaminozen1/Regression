package opc.models.simulator;

import opc.enums.opc.ManagedInstrumentType;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.shared.ManagedInstrumentTypeId;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class SimulateCreateMandateModel {

    private final String ddiId;
    private final String merchantReference;
    private final String merchantName;
    private final String merchantAccountNumber;
    private final String merchantSortCode;
    private final ManagedInstrumentTypeId accountId;
    private final DirectDebitAccountDetailsModel accountDetails;

    public SimulateCreateMandateModel(final Builder builder) {
        this.ddiId = builder.ddiId;
        this.merchantReference = builder.merchantReference;
        this.merchantName = builder.merchantName;
        this.merchantAccountNumber = builder.merchantAccountNumber;
        this.merchantSortCode = builder.merchantSortCode;
        this.accountId = builder.accountId;
        this.accountDetails = builder.accountDetails;
    }

    public String getDdiId() {
        return ddiId;
    }

    public String getMerchantReference() {
        return merchantReference;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getMerchantAccountNumber() {
        return merchantAccountNumber;
    }

    public String getMerchantSortCode() {
        return merchantSortCode;
    }

    public ManagedInstrumentTypeId getAccountId() {
        return accountId;
    }

    public DirectDebitAccountDetailsModel getAccountDetails() {
        return accountDetails;
    }

    public static class Builder {
        private String ddiId;
        private String merchantReference;
        private String merchantName;
        private String merchantAccountNumber;
        private String merchantSortCode;
        private ManagedInstrumentTypeId accountId;
        private DirectDebitAccountDetailsModel accountDetails;

        public Builder setDdiId(String ddiId) {
            this.ddiId = ddiId;
            return this;
        }

        public Builder setMerchantReference(String merchantReference) {
            this.merchantReference = merchantReference;
            return this;
        }

        public Builder setMerchantName(String merchantName) {
            this.merchantName = merchantName;
            return this;
        }

        public Builder setMerchantAccountNumber(String merchantAccountNumber) {
            this.merchantAccountNumber = merchantAccountNumber;
            return this;
        }

        public Builder setMerchantSortCode(String merchantSortCode) {
            this.merchantSortCode = merchantSortCode;
            return this;
        }

        public Builder setAccountId(ManagedInstrumentTypeId accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder setAccountDetails(DirectDebitAccountDetailsModel accountDetails) {
            this.accountDetails = accountDetails;
            return this;
        }

        public SimulateCreateMandateModel build() { return new SimulateCreateMandateModel(this); }
    }

    public static Builder builder() { return new Builder(); }

    public static Builder createMandateByAccountId(final Pair<String, FasterPaymentsBankDetailsModel> managedAccount) {

        final String id = RandomStringUtils.randomNumeric(15);

        return new Builder()
                .setMerchantReference(String.format("Mandate%s", id))
                .setMerchantName(String.format("Merchant%s", id))
                .setMerchantAccountNumber(String.format("AccountNo%s", id))
                .setMerchantSortCode(managedAccount.getRight().getSortCode())
                .setAccountId(new ManagedInstrumentTypeId(managedAccount.getLeft(), ManagedInstrumentType.MANAGED_ACCOUNTS));
    }

    public static Builder createMandateByAccountDetails(final FasterPaymentsBankDetailsModel bankDetails) {

        final String id = RandomStringUtils.randomNumeric(10);

        return new Builder()
                .setMerchantReference(String.format("Mandate%s", id))
                .setMerchantName(String.format("Merchant%s", id))
                .setMerchantAccountNumber(String.format("AccountNo%s", id))
                .setMerchantSortCode(bankDetails.getSortCode())
                .setAccountDetails(DirectDebitAccountDetailsModel.builder()
                        .setAccountNumber(bankDetails.getAccountNumber())
                        .setSortCode(bankDetails.getSortCode())
                        .build());
    }
}
