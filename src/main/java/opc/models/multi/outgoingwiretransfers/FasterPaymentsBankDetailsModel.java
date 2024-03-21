package opc.models.multi.outgoingwiretransfers;

public class FasterPaymentsBankDetailsModel {
    private String accountNumber;
    private String sortCode;

    public FasterPaymentsBankDetailsModel(final String accountNumber, final String sortCode) {
        this.accountNumber = accountNumber;
        this.sortCode = sortCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public FasterPaymentsBankDetailsModel setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }

    public String getSortCode() {
        return sortCode;
    }

    public FasterPaymentsBankDetailsModel setSortCode(String sortCode) {
        this.sortCode = sortCode;
        return this;
    }
}
