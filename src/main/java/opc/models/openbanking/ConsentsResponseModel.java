package opc.models.openbanking;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;

public class ConsentsResponseModel {

  @JsonProperty("consents")
  private ArrayList<ConsentResponseModel> consents;

  @JsonProperty("count")
  private Integer count;

  @JsonProperty("responseCount")
  private Integer responseCount;

  public ArrayList<ConsentResponseModel> getConsents() {
    return this.consents;
  }

  public void setConsents(ArrayList<ConsentResponseModel> consents) {
    this.consents = consents;
  }

  public int getCount() {
    return this.count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public int getResponseCount() {
    return this.responseCount;
  }

  public void setResponseCount(int responseCount) {
    this.responseCount = responseCount;
  }

}
