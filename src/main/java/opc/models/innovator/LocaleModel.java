package opc.models.innovator;

public class LocaleModel {

    private final String language;
    private final String country;

    public LocaleModel(final Builder builder) {
        this.language = builder.language;
        this.country = builder.country;
    }

    public String getLanguage() {
        return language;
    }

    public String getCountry() {
        return country;
    }

    public static class Builder {
        private String language;
        private String country;

        public String getLanguage() {
            return language;
        }

        public Builder setLanguage(String language) {
            this.language = language;
            return this;
        }

        public String getCountry() {
            return country;
        }

        public Builder setCountry(String country) {
            this.country = country;
            return this;
        }

        public LocaleModel build() { return new LocaleModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
