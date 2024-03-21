package opc.models.openbanking;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigInteger;

public class ConsentResponseModel {

  @JsonProperty("createdTimestamp")
  private BigInteger createdTimestamp;

  @JsonProperty("expiry")
  private BigInteger expiry;

  @JsonProperty("id")
  private String id;

  @JsonProperty("lastUpdated")
  private BigInteger lastUpdated;

  @JsonProperty("state")
  private String state;

  @JsonProperty("tppName")
  private String tppName;

  @JsonProperty("tppId")
  private String tppId;

  public BigInteger getCreatedTimestamp() {
    return this.createdTimestamp;
  }

  public void setCreatedTimestamp(BigInteger createdTimestamp) {
    this.createdTimestamp = createdTimestamp;
  }

  public BigInteger getExpiry() {
    return this.expiry;
  }

  public void setExpiry(BigInteger expiry) {
    this.expiry = expiry;
  }

  public String getId() {
    return this.id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public BigInteger getLastUpdated() {
    return this.lastUpdated;
  }

  public void setLastUpdated(BigInteger lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public String getState() {
    return this.state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getTppId() {
    return this.tppId;
  }

  public void setTppId(String tppId) {
    this.tppId = tppId;
  }

  public String getTppName() {
    return this.tppName;
  }

  public void setTppName(String tppName) {
    this.tppName = tppName;
  }
}


