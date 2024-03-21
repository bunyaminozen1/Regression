package opc.models.webhook;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class WebhookTrustedBeneficiaryBatchModel {
  @JsonProperty("id")
  private String id;
  @JsonProperty("operation")
  private String operation;
  @JsonProperty("tag")
  private String tag;
  @JsonProperty("publishedTimestamp")
  private String publishedTimestamp;
  @JsonProperty("eventType")
  private String eventType;
  @JsonProperty("beneficiaries")
  private List<WebhookTrustedBeneficiaryModel> beneficiaries;

  public String getId() {
    return id;
  }

  public String getOperation() {
    return operation;
  }

  public String getTag() {
    return tag;
  }

  public String getPublishedTimestamp() {
    return publishedTimestamp;
  }

  public void setPublishedTimestamp(String publishedTimestamp) {
    this.publishedTimestamp = publishedTimestamp;
  }

  public String getEventType() {
    return eventType;
  }

  public List<WebhookTrustedBeneficiaryModel> getBeneficiaries() {
    return beneficiaries;
  }
}
