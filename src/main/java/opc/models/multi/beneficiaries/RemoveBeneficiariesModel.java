package opc.models.multi.beneficiaries;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RemoveBeneficiariesModel {
  @JsonProperty("beneficiaryIds")
  private final List<String> beneficiaryIds;

  public RemoveBeneficiariesModel(final RemoveBeneficiariesModel.Builder builder) {
    this.beneficiaryIds = builder.beneficiaryIds;
  }

  private List<String> getBeneficiaryIds()  { return beneficiaryIds; }

  public static class Builder {
    private List<String> beneficiaryIds;

    public RemoveBeneficiariesModel.Builder setBeneficiaryIds(List<String> beneficiaryIds) {
      this.beneficiaryIds = beneficiaryIds;
      return this;
    }

    public RemoveBeneficiariesModel build() { return new RemoveBeneficiariesModel(this); }
  }

  public static RemoveBeneficiariesModel.Builder builder() {
    return new RemoveBeneficiariesModel.Builder();
  }

  public static RemoveBeneficiariesModel.Builder Remove(final List<String> listOfBeneficiaries) {
    return new Builder().setBeneficiaryIds(listOfBeneficiaries);
  }
}