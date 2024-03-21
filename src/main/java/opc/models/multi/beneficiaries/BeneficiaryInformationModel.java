package opc.models.multi.beneficiaries;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import opc.enums.opc.IdentityType;

public class BeneficiaryInformationModel {
  @JsonProperty("fullName")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String fullName;
  @JsonProperty("businessName")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private final String businessName;

  public BeneficiaryInformationModel(final BeneficiaryInformationModel.Builder builder) {
    this.fullName = builder.fullName;
    this.businessName = builder.businessName;
  }

  public String getFullName() {
    return fullName;
  }
  private String getBusinessName()  { return businessName; }

  public static class Builder {
    private String fullName;
    private String businessName;

    public BeneficiaryInformationModel.Builder setFullName(String fullName) {
      this.fullName = fullName;
      return this;
    }

    public BeneficiaryInformationModel.Builder setBusinessName(String businessName) {
      this.businessName = businessName;
      return this;
    }

    public BeneficiaryInformationModel build() { return new BeneficiaryInformationModel(this); }
  }

  public static BeneficiaryInformationModel.Builder builder() {
    return new BeneficiaryInformationModel.Builder();
  }

  public static BeneficiaryInformationModel.Builder NameBeneficiaryInformationModel(final IdentityType identityType,
                                                                                    final String name) {
    return identityType == IdentityType.CORPORATE ? new Builder()
        .setBusinessName(name) : new Builder()
        .setFullName(name);
  }
}