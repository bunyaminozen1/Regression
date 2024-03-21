package opc.models.secure;

public class DetokenizeModel {

    private final String random;
    private final String token;
    private final boolean permanent;

    public DetokenizeModel(final Builder builder) {
        this.random = builder.random;
        this.token = builder.token;
        this.permanent = builder.permanent;
    }

    public String getRandom() {
        return random;
    }

    public String getToken() {
        return token;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public static class Builder {
        private String random;
        private String token;
        private boolean permanent;

        public Builder setRandom(String random) {
            this.random = random;
            return this;
        }

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public Builder setPermanent(boolean permanent) {
            this.permanent = permanent;
            return this;
        }

        public DetokenizeModel build() { return new DetokenizeModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
