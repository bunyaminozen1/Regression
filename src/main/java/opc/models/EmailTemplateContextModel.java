package opc.models;

import opc.models.innovator.LocaleModel;
import opc.models.innovator.TemplateContextModel;

public class EmailTemplateContextModel {

    private final TemplateContextModel context;

    public EmailTemplateContextModel(final Builder builder) {
        this.context = builder.context;
    }

    public TemplateContextModel getContext() {
        return context;
    }

    public static class Builder {
        private TemplateContextModel context;

        public TemplateContextModel getContext() {
            return context;
        }

        public Builder setContext(TemplateContextModel context) {
            this.context = context;
            return this;
        }

        public EmailTemplateContextModel build() { return new EmailTemplateContextModel(this); }
    }

    public static Builder builder() { return new Builder(); }

    public static Builder defaultEmailTemplateContextModel(final String emailTemplateType) {
        return new Builder()
                .setContext(TemplateContextModel.builder()
                        .setType(emailTemplateType)
                        .setLocale(LocaleModel.builder()
                                .setLanguage("en")
                                .setCountry("MT")
                                .build())
                        .build());
    }
}
