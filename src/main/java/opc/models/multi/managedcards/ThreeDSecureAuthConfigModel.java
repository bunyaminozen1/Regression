package opc.models.multi.managedcards;

public class ThreeDSecureAuthConfigModel {

  private final String linkedUserId;
  private final String primaryChannel;
  private final String fallbackChannel;

  public ThreeDSecureAuthConfigModel(final Builder builder) {
    this.linkedUserId = builder.linkedUserId;
    this.primaryChannel = builder.primaryChannel;
    this.fallbackChannel = builder.fallbackChannel;
  }

  public String getLinkedUserId() {
    return linkedUserId;
  }

  public String getPrimaryChannel() {
    return primaryChannel;
  }

  public String getFallbackChannel() {
    return fallbackChannel;
  }

  public static class Builder {

    private String linkedUserId;
    private String primaryChannel;
    private String fallbackChannel;

    public Builder setLinkedUserId(String linkedUserId) {
      this.linkedUserId = linkedUserId;
      return this;
    }

    public Builder setPrimaryChannel(String primaryChannel) {
      this.primaryChannel = primaryChannel;
      return this;
    }

    public Builder setFallbackChannel(String fallbackChannel) {
      this.fallbackChannel = fallbackChannel;
      return this;
    }

    public ThreeDSecureAuthConfigModel build() {
      return new ThreeDSecureAuthConfigModel(this);
    }
  }
  public static Builder builder(){ return new Builder(); }

  public static Builder DefaultThreeDSecureAuthConfigModel(final String linkedUserId) {
    return new Builder()
        .setLinkedUserId(linkedUserId)
        .setPrimaryChannel("BIOMETRICS")
        .setFallbackChannel("OTP_SMS");
  }

  public static Builder AuthyThreeDSecureAuthConfigModel(final String linkedUserId) {
    return new Builder()
            .setLinkedUserId(linkedUserId)
            .setPrimaryChannel("TWILIO_AUTHY")
            .setFallbackChannel("OTP_SMS");
  }

}
