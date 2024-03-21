package opc.models.innovator;

import org.apache.commons.lang3.RandomStringUtils;

public class EmailTemplateModel {
    private final TemplateModel template;
    private final TemplateContextModel context;

    public EmailTemplateModel(final Builder builder) {
        this.template = builder.template;
        this.context = builder.context;
    }

    public TemplateModel getTemplate() {
        return template;
    }

    public TemplateContextModel getContext() {
        return context;
    }

    public static class Builder {
        private TemplateModel template;
        private TemplateContextModel context;

        public TemplateModel getTemplate() {
            return template;
        }

        public Builder setTemplate(TemplateModel template) {
            this.template = template;
            return this;
        }

        public TemplateContextModel getContext() {
            return context;
        }

        public Builder setContext(TemplateContextModel context) {
            this.context = context;
            return this;
        }

        public EmailTemplateModel build() { return new EmailTemplateModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder defaultEmailTemplateModel(final String emailTemplateType) {
        return new Builder()
                .setTemplate(TemplateModel.builder()
                        .setSenderAddress(String.format("%s@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
                        .setSenderName(String.format("%s %s", RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5)))
                        .setSubjectTemplate(RandomStringUtils.randomAlphabetic(5))
                        .setContentTemplate(RandomStringUtils.randomAlphabetic(5))
                        .build())
                .setContext(TemplateContextModel.builder()
                        .setLocale(LocaleModel.builder()
                                .setCountry("MT")
                                .setLanguage("en")
                                .build())
                        .setType(emailTemplateType)
                        .build());
    }
}
