package opc.models.innovator;

public class TemplateContextModel {

    private final String type;
    private final LocaleModel locale;

    public TemplateContextModel(final Builder builder) {
        this.type = builder.type;
        this.locale = builder.locale;
    }

    public String getType() {
        return type;
    }

    public LocaleModel getLocale() {
        return locale;
    }

    public static class Builder {
        private String type;
        private LocaleModel locale;

        public String getType() {
            return type;
        }

        public Builder setType(String type) {
            this.type = type;
            return this;
        }

        public LocaleModel getLocale() {
            return locale;
        }

        public Builder setLocale(LocaleModel locale) {
            this.locale = locale;
            return this;
        }

        public TemplateContextModel build() { return new TemplateContextModel(this); }
    }

    public static Builder builder() { return new Builder(); }
}
