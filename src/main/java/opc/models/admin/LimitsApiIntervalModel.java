package opc.models.admin;

public class LimitsApiIntervalModel {

    private final String tumbling;

    public LimitsApiIntervalModel(final Builder builder) {
        this.tumbling = builder.tumbling;
    }

    public String getTumbling() {
        return tumbling;
    }

    public static class Builder {
        private String tumbling;

        public Builder setTumbling(String tumbling) {
            this.tumbling = tumbling;
            return this;
        }

        public LimitsApiIntervalModel build() { return new LimitsApiIntervalModel(this);}
    }

    public static Builder builder(){
        return new Builder();
    }
}
