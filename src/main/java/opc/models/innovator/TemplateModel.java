package opc.models.innovator;

public class TemplateModel {

    private final String senderAddress;
    private final String senderName;
    private final String subjectTemplate;
    private final String contentTemplate;

    public TemplateModel(final Builder builder) {
        this.senderAddress = builder.senderAddress;
        this.senderName = builder.senderName;
        this.subjectTemplate = builder.subjectTemplate;
        this.contentTemplate = builder.contentTemplate;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public String getContentTemplate() {
        return contentTemplate;
    }

    public static class Builder {
        private String senderAddress;
        private String senderName;
        private String subjectTemplate;
        private String contentTemplate;

        public String getSenderAddress() {
            return senderAddress;
        }

        public Builder setSenderAddress(String senderAddress) {
            this.senderAddress = senderAddress;
            return this;
        }

        public String getSenderName() {
            return senderName;
        }

        public Builder setSenderName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public String getSubjectTemplate() {
            return subjectTemplate;
        }

        public Builder setSubjectTemplate(String subjectTemplate) {
            this.subjectTemplate = subjectTemplate;
            return this;
        }

        public String getContentTemplate() {
            return contentTemplate;
        }

        public Builder setContentTemplate(String contentTemplate) {
            this.contentTemplate = contentTemplate;
            return this;
        }

        public TemplateModel build() { return new TemplateModel(this); }
    }

    public static Builder builder(){
        return new Builder();
    }
}
