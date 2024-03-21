package opc.models.multi.outgoingwiretransfers;

public class SepaBankDetailsModel {

    private String iban;
    private String bankIdentifierCode;

    public SepaBankDetailsModel(final String iban, final String bankIdentifierCode) {
        this.iban = iban;
        this.bankIdentifierCode = bankIdentifierCode;
    }

    public String getIban() {
        return iban;
    }

    public SepaBankDetailsModel setIban(String iban) {
        this.iban = iban;
        return this;
    }

    public String getBankIdentifierCode() {
        return bankIdentifierCode;
    }

    public SepaBankDetailsModel setBankIdentifierCode(String bankIdentifierCode) {
        this.bankIdentifierCode = bankIdentifierCode;
        return this;
    }
}
