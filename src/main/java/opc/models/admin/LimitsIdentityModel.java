package opc.models.admin;

public class LimitsIdentityModel {

    private final String type;
    private final String id;

    public LimitsIdentityModel(final Builder builder) {
        this.type = builder.type;
        this.id = builder.id;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public static class Builder {
        private String type;
        private String id;

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public LimitsIdentityModel build() {return new LimitsIdentityModel(this); }
    }

    public static Builder builder() { return new Builder(); }
}
