package opc.models.secure;

public class AdditionalPropertiesModel {

    private final String value;
    private final boolean permanent;

    public AdditionalPropertiesModel(final Builder builder) {
        this.value = builder.value;
        this.permanent = builder.permanent;
    }

    public String getValue() {
        return value;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public static class Builder {
        private String value;
        private boolean permanent;

        public Builder setValue(String value) {
            this.value = value;
            return this;
        }

        public Builder setPermanent(boolean permanent) {
            this.permanent = permanent;
            return this;
        }

        public AdditionalPropertiesModel build() { return new AdditionalPropertiesModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
