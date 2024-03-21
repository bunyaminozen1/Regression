package opc.models.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookConsumerBeneficiaryTypeModel {
  @JsonProperty("fullName")
  private String fullName;

  public String getFullName() {
    return fullName;
  }
}
