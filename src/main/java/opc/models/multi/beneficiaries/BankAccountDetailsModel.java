package opc.models.multi.beneficiaries;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BankAccountDetailsModel {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("iban")
  private final String iban;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("bankIdentifierCode")
  private final String bankIdentifierCode;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("accountNumber")
  private final String accountNumber;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("sortCode")
  private final String sortCode;


  public BankAccountDetailsModel(final BankAccountDetailsModel.Builder builder) {
    this.iban = builder.iban;
    this.bankIdentifierCode = builder.bankIdentifierCode;
    this.accountNumber = builder.accountNumber;
    this.sortCode = builder.sortCode;
  }

  public String getIban() {
    return iban;
  }

  public String getBankIdentifierCode() {
    return bankIdentifierCode;
  }

  public String getAccountNumber() {
    return accountNumber;
  }

  public String getSortCode() {
    return sortCode;
  }

  public static class Builder {
    private String iban;
    private String bankIdentifierCode;
    private String accountNumber;
    private String sortCode;

    public BankAccountDetailsModel.Builder setIban(String iban) {
      this.iban = iban;
      return this;
    }

    public BankAccountDetailsModel.Builder setBankIdentifierCode(String bankIdentifierCode) {
      this.bankIdentifierCode = bankIdentifierCode;
      return this;
    }

    public BankAccountDetailsModel.Builder setAccountNumber(String accountNumber) {
      this.accountNumber = accountNumber;
      return this;
    }

    public BankAccountDetailsModel.Builder setSortCode(String sortCode) {
      this.sortCode = sortCode;
      return this;
    }

    public BankAccountDetailsModel build() { return new BankAccountDetailsModel(this); }
  }

  public static BankAccountDetailsModel.Builder builder() {
    return new BankAccountDetailsModel.Builder();
  }

  public static BankAccountDetailsModel.Builder SEPABankAccountDetails(final String iban, final String bankIdentifierCode) {
    return new BankAccountDetailsModel.Builder()
        .setIban(iban)
        .setBankIdentifierCode(bankIdentifierCode);
  }

  public static BankAccountDetailsModel.Builder FasterPaymentsBankAccountDetails(final String accountNumber, final String sortCode) {
    return new BankAccountDetailsModel.Builder()
        .setAccountNumber(accountNumber)
        .setSortCode(sortCode);
  }
}