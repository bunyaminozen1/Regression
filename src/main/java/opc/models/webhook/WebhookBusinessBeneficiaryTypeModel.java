package opc.models.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookBusinessBeneficiaryTypeModel {
  @JsonProperty("businessName")
  private String businessName;

  public String getBusinessName() {
    return businessName;
  }
}
