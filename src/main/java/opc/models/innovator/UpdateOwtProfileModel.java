package opc.models.innovator;

public class UpdateOwtProfileModel {
  private final boolean returnOwtFee;

  public UpdateOwtProfileModel(final Builder builder) {
    this.returnOwtFee = builder.returnOwtFee;
  }

  public boolean isReturnOwtFee() {
    return returnOwtFee;
  }


  public static class Builder {
    private boolean returnOwtFee;

    public Builder setReturnOwtFee(boolean returnOwtFee) {
      this.returnOwtFee = returnOwtFee;
      return this;
    }

    public UpdateOwtProfileModel build() {
      return new UpdateOwtProfileModel(this);
    }
  }
}
