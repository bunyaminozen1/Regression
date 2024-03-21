package opc.models.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookTrustedBeneficiaryModel {
  @JsonProperty("trustLevel")
  private String trustLevel;
  @JsonProperty("group")
  private String group;
  @JsonProperty("beneficiaryInformation")
  private WebhookTrustedBeneficiaryInformationModel beneficiaryInformation;
  @JsonProperty("beneficiaryDetails")
  private WebhookTrustedBeneficiaryDetailsModel beneficiaryDetails;
  @JsonProperty("state")
  private String state;
  @JsonProperty("validationFailure")
  private String validationFailure;
  @JsonProperty("id")
  private String id;

  public String getTrustLevel() {
    return trustLevel;
  }

  public String getGroup() {
    return group;
  }

  public WebhookTrustedBeneficiaryInformationModel getBeneficiaryInformation() {
    return beneficiaryInformation;
  }

  public WebhookTrustedBeneficiaryDetailsModel getBeneficiaryDetails() {
    return beneficiaryDetails;
  }

  public String getState() {
    return state;
  }

  public String getValidationFailure() {
    return validationFailure;
  }

  public String getId() {
    return id;
  }
}