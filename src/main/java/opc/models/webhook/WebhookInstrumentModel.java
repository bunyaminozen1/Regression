package opc.models.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookInstrumentModel {
  @JsonProperty("type")
  private String type;
  @JsonProperty("id")
  private String id;

  public String getType() {
    return type;
  }

  public String getId() {
    return id;
  }
}
