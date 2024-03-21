package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookLoginPasswordEventModel {

  public LinkedHashMap <String, String> credential;
  public LinkedHashMap <String, String> identity;
  public String publishedTimestamp;
  public String status;
  public String type;

  public LinkedHashMap<String, String> getCredential() {
    return credential;
  }

  public void setCredential(LinkedHashMap<String, String> credential) {
    this.credential = credential;
  }

  public LinkedHashMap<String, String> getIdentity() {
    return identity;
  }

  public void setIdentity(LinkedHashMap<String, String> identity) {
    this.identity = identity;
  }

  public String getPublishedTimestamp() {
    return publishedTimestamp;
  }

  public void setPublishedTimestamp(String publishedTimestamp) {
    this.publishedTimestamp = publishedTimestamp;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }
}
