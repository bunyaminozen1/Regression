package opc.models.webhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookInstrumentDetailsBeneficiaryModel {
  @JsonProperty("instrument")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private WebhookInstrumentModel instrument;

  public WebhookInstrumentModel getInstrument() {
    return instrument;
  }
}
