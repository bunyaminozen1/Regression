package opc.models.multi.beneficiaries;

import static opc.models.multi.beneficiaries.BankAccountDetailsModel.FasterPaymentsBankAccountDetails;
import static opc.models.multi.beneficiaries.BankAccountDetailsModel.SEPABankAccountDetails;
import static opc.models.multi.beneficiaries.InstrumentModel.Instrument;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import opc.enums.opc.ManagedInstrumentType;
import org.apache.commons.lang3.RandomStringUtils;

public class BeneficiaryDetailsModel {
  @JsonProperty("instrument")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final InstrumentModel instrument;
  @JsonProperty("address")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String address;
  @JsonProperty("bankName")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String bankName;
  @JsonProperty("bankAddress")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String bankAddress;
  @JsonProperty("bankCountry")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String bankCountry;
  @JsonProperty("bankAccountDetails")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final BankAccountDetailsModel  bankAccountDetails;


  public BeneficiaryDetailsModel(final BeneficiaryDetailsModel.Builder builder) {
    this.instrument = builder.instrument;
    this.address = builder.address;
    this.bankName = builder.bankName;
    this.bankAddress = builder.bankAddress;
    this.bankCountry = builder.bankCountry;
    this.bankAccountDetails = builder.bankAccountDetails;
  }

  public InstrumentModel getInstrument() {
    return instrument;
  }
  private String getAddress()  { return address; }
  private String getBankName()  { return bankName; }
  private String getBankAddress()  { return bankAddress; }
  private String getBankCountry()  { return bankCountry; }
  private BankAccountDetailsModel getBankAccountDetails() {return bankAccountDetails; }

  public static class Builder {
    private InstrumentModel instrument;
    private String address;
    private String bankName;
    private String bankAddress;
    private String bankCountry;
    private BankAccountDetailsModel  bankAccountDetails;


    public BeneficiaryDetailsModel.Builder setInstrument(InstrumentModel instrument) {
      this.instrument = instrument;
      return this;
    }

    public BeneficiaryDetailsModel.Builder setAddress(String address) {
      this.address = address;
      return this;
    }

    public BeneficiaryDetailsModel.Builder setBankName(String bankName) {
      this.bankName = bankName;
      return this;
    }

    public BeneficiaryDetailsModel.Builder setBankAddress(String bankAddress) {
      this.bankAddress = bankAddress;
      return this;
    }

    public BeneficiaryDetailsModel.Builder setBankCountry(String bankCountry) {
      this.bankCountry = bankCountry;
      return this;
    }

    public BeneficiaryDetailsModel.Builder setBankAccountDetails(BankAccountDetailsModel bankAccountDetails) {
      this.bankAccountDetails = bankAccountDetails;
      return this;
    }

    public BeneficiaryDetailsModel build() { return new BeneficiaryDetailsModel(this); }
  }

  public static BeneficiaryDetailsModel.Builder builder() {
    return new BeneficiaryDetailsModel.Builder();
  }

  public static BeneficiaryDetailsModel.Builder InstrumentBeneficiaryDetails(final ManagedInstrumentType managedInstrumentType,
                                                                             final String managedAccountId) {
    return new BeneficiaryDetailsModel.Builder()
        .setInstrument(Instrument(managedInstrumentType, managedAccountId).build());
  }

  public static BeneficiaryDetailsModel.Builder SEPABankAccountBeneficiaryDetails(final String iban,
                                                                                  final String bankIdentifierCode ) {
    return new BeneficiaryDetailsModel.Builder()
        .setAddress(RandomStringUtils.randomAlphabetic(5))
        .setBankName(RandomStringUtils.randomAlphabetic(5))
        .setBankCountry("MT")
        .setBankAddress(RandomStringUtils.randomAlphabetic(5))
        .setBankAccountDetails(SEPABankAccountDetails(iban, bankIdentifierCode).build());
  }

  public static BeneficiaryDetailsModel.Builder FasterPaymentsBankAccountBeneficiaryDetails(final String accountNumber,
                                                                                            final String sortCode ) {
    return new BeneficiaryDetailsModel.Builder()
        .setAddress(RandomStringUtils.randomAlphabetic(5))
        .setBankName(RandomStringUtils.randomAlphabetic(5))
        .setBankCountry("MT")
        .setBankAddress(RandomStringUtils.randomAlphabetic(5))
        .setBankAccountDetails(FasterPaymentsBankAccountDetails(accountNumber, sortCode).build());
  }
}