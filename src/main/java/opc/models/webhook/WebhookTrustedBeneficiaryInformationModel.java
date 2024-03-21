package opc.models.webhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookTrustedBeneficiaryInformationModel {
  @JsonProperty("businessBeneficiaryType")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private WebhookBusinessBeneficiaryTypeModel businessBeneficiaryType;

  @JsonProperty("consumerBeneficiaryType")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private WebhookConsumerBeneficiaryTypeModel consumerBeneficiaryType;

  public WebhookBusinessBeneficiaryTypeModel getBusinessBeneficiaryType() {
    return businessBeneficiaryType;
  }

  public WebhookConsumerBeneficiaryTypeModel getConsumerBeneficiaryType() {
    return consumerBeneficiaryType;
  }
}
