package opc.models.multi.beneficiaries;

import static opc.models.multi.beneficiaries.BeneficiaryDetailsModel.FasterPaymentsBankAccountBeneficiaryDetails;
import static opc.models.multi.beneficiaries.BeneficiaryDetailsModel.InstrumentBeneficiaryDetails;
import static opc.models.multi.beneficiaries.BeneficiaryDetailsModel.SEPABankAccountBeneficiaryDetails;
import static opc.models.multi.beneficiaries.BeneficiaryInformationModel.NameBeneficiaryInformationModel;

import com.fasterxml.jackson.annotation.JsonProperty;
import opc.enums.opc.IdentityType;
import opc.enums.opc.ManagedInstrumentType;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Arrays;
import java.util.List;

public class BeneficiaryModel {
  @JsonProperty("trustLevel")
  private final String trustLevel;
  @JsonProperty("group")
  private final String group;
  @JsonProperty("externalRefs")
  private final List<String> externalRefs;
  @JsonProperty("beneficiaryInformation")
  private final BeneficiaryInformationModel beneficiaryInformation;
  @JsonProperty("beneficiaryDetails")
  private final BeneficiaryDetailsModel beneficiaryDetails;

  public BeneficiaryModel(final BeneficiaryModel.Builder builder) {
    this.trustLevel = builder.trustLevel;
    this.group = builder.group;
    this.externalRefs = builder.externalRefs;
    this.beneficiaryInformation = builder.beneficiaryInformation;
    this.beneficiaryDetails = builder.beneficiaryDetails;
  }

  public String getTrustLevel() {
    return trustLevel;
  }

  public String getGroup() {
    return group;
  }
  public List<String> getExternalRefs() { return externalRefs; }

  public BeneficiaryInformationModel getBeneficiaryInformation() {
    return beneficiaryInformation;
  }

  public BeneficiaryDetailsModel getBeneficiaryDetails() {
    return beneficiaryDetails;
  }

  public static class Builder {
    private String trustLevel;
    private String group;
    private List<String> externalRefs;
    private BeneficiaryInformationModel beneficiaryInformation;
    private BeneficiaryDetailsModel beneficiaryDetails;


    public BeneficiaryModel.Builder setTrustLevel(String trustLevel) {
      this.trustLevel = trustLevel;
      return this;
    }

    public BeneficiaryModel.Builder setGroup(String group) {
      this.group = group;
      return this;
    }

    public BeneficiaryModel.Builder setExternalRefs(List<String> externalRefs) {
       this.externalRefs = externalRefs;
       return this;
    }

    public BeneficiaryModel.Builder setBeneficiaryInformation(BeneficiaryInformationModel beneficiaryInformation) {
      this.beneficiaryInformation = beneficiaryInformation;
      return this;
    }

    public BeneficiaryModel.Builder setBeneficiaryDetails(BeneficiaryDetailsModel beneficiaryDetails) {
      this.beneficiaryDetails = beneficiaryDetails;
      return this;
    }

    public BeneficiaryModel build() { return new BeneficiaryModel(this); }
  }
  public static BeneficiaryModel.Builder builder() {
    return new BeneficiaryModel.Builder();
  }

  public static BeneficiaryModel.Builder DefaultBeneficiary() {
    return new BeneficiaryModel.Builder()
        .setTrustLevel("TRUSTED")
        .setGroup(RandomStringUtils.randomAlphabetic(5))
        .setExternalRefs(Arrays.asList(RandomStringUtils.randomNumeric(5) , RandomStringUtils.randomNumeric(5)));
  }

  public static BeneficiaryModel.Builder InstrumentBeneficiary(final IdentityType identityType,
                                                               final ManagedInstrumentType managedInstrumentType,
                                                               final String businessName,
                                                               final String instrumentId) {
      return DefaultBeneficiary()
          .setBeneficiaryInformation(NameBeneficiaryInformationModel(identityType, businessName).build())
          .setBeneficiaryDetails(InstrumentBeneficiaryDetails(managedInstrumentType, instrumentId).build());
  }

  public static BeneficiaryModel.Builder SEPABankAccountBeneficiary(final IdentityType identityType,
                                                                    final String name,
                                                                    final String iban,
                                                                    final String bankIdentifierCode) {
      return DefaultBeneficiary()
          .setBeneficiaryInformation(NameBeneficiaryInformationModel(identityType, name).build())
          .setBeneficiaryDetails(SEPABankAccountBeneficiaryDetails(iban, bankIdentifierCode).build());
  }

  public static BeneficiaryModel.Builder FasterPaymentsBankAccountBeneficiary(final IdentityType identityType,
                                                                              final String name,
                                                                              final String accountNumber,
                                                                              final String sortCode) {
    return DefaultBeneficiary()
        .setBeneficiaryInformation(NameBeneficiaryInformationModel(identityType, name).build())
        .setBeneficiaryDetails(FasterPaymentsBankAccountBeneficiaryDetails(accountNumber, sortCode).build());
  }
}