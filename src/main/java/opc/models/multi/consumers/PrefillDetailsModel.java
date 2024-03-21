package opc.models.multi.consumers;


public class PrefillDetailsModel {
    private final String name;
    private final String value;

    public PrefillDetailsModel(final Builder builder) {
        this.name = builder.name;
        this.value = builder.value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public static class Builder {
        private String name;
        private String value;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setValue(String value) {
            this.value = value;
            return this;
        }

        public PrefillDetailsModel build() { return new PrefillDetailsModel(this); }
    }
}
