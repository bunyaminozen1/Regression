package opc.models.webhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookBankAccountDetailsBeneficiaryModel {
  @JsonProperty("address")
  private String address;

  @JsonProperty("bankName")
  private String bankName;

  @JsonProperty("bankAddress")
  private String bankAddress;

  @JsonProperty("country")
  private String country;

  @JsonProperty("sepaBankDetails")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private WebhookSepaBankDetailsModel sepaBankDetails;

  @JsonProperty("fasterPaymentsBankDetails")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private WebhookFasterPaymentsBankDetailsModel fasterPaymentsBankDetails;

  public String getAddress() {
    return address;
  }

  public String getBankName() {
    return bankName;
  }

  public String getBankAddress() {
    return bankAddress;
  }

  public String getCountry() {
    return country;
  }

  public WebhookSepaBankDetailsModel getSepaBankDetails() {
    return sepaBankDetails;
  }

  public WebhookFasterPaymentsBankDetailsModel getFasterPaymentsBankDetails() {
    return fasterPaymentsBankDetails;
  }
}
