package opc.models.webhook;

import java.util.LinkedHashMap;

public class WebhookBiometricLoginEventModel {
  public String authToken;
  public String challengeId;
  public LinkedHashMap <String, String> credential;
  public String publishedTimestamp;
  public String status;
  public String type;

  public String getAuthToken() {
    return authToken;
  }

  public void setAuthToken(String authToken) {
    this.authToken = authToken;
  }

  public String getChallengeId() {
    return challengeId;
  }

  public void setChallengeId(String challengeId) {
    this.challengeId = challengeId;
  }

  public LinkedHashMap<String, String> getCredential() {
    return credential;
  }

  public void setCredential(LinkedHashMap<String, String> credential) {
    this.credential = credential;
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
