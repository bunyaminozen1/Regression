package opc.models.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookSepaBankDetailsModel {
  @JsonProperty("iban")
  private String iban;

  @JsonProperty("bankIdentifierCode")
  private String bankIdentifierCode;

  public String getIban() {
    return iban;
  }

  public String getBankIdentifierCode() {
    return bankIdentifierCode;
  }
}
