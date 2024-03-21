package opc.models.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookFasterPaymentsBankDetailsModel {
  @JsonProperty("accountNumber")
  private String accountNumber;

  @JsonProperty("sortCode")
  private String sortCode;

  public String getAccountNumber() {
    return accountNumber;
  }

  public String getSortCode() {
    return sortCode;
  }
}
