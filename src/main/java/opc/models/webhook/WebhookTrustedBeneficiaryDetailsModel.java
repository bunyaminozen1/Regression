package opc.models.webhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookTrustedBeneficiaryDetailsModel {
  @JsonProperty("bankAccountDetailsBeneficiary")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private WebhookBankAccountDetailsBeneficiaryModel bankAccountDetailsBeneficiary;

  @JsonProperty("instrumentDetailsBeneficiary")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private WebhookInstrumentDetailsBeneficiaryModel instrumentDetailsBeneficiary;

  public WebhookBankAccountDetailsBeneficiaryModel getBankAccountDetailsBeneficiary() {
    return bankAccountDetailsBeneficiary;
  }

  public WebhookInstrumentDetailsBeneficiaryModel getInstrumentDetailsBeneficiary() {
    return instrumentDetailsBeneficiary;
  }
}
