package opc.models.innovator;

import java.util.Collections;
import java.util.List;

public class CreateOutgoingDirectDebitProfileModel {

    private final String code;
    private final String payletTypeCode;
    private final List<String> tag;
    private final Long linkedInstrumentProfileId;
    private final String fiProvider;

    public CreateOutgoingDirectDebitProfileModel(final Builder builder) {
        this.code = builder.code;
        this.payletTypeCode = builder.payletTypeCode;
        this.tag = builder.tag;
        this.linkedInstrumentProfileId = builder.linkedInstrumentProfileId;
        this.fiProvider = builder.fiProvider;
    }

    public String getCode() {
        return code;
    }

    public String getPayletTypeCode() {
        return payletTypeCode;
    }

    public List<String> getTag() {
        return tag;
    }

    public Long getLinkedInstrumentProfileId() {
        return linkedInstrumentProfileId;
    }

    public String getFiProvider() {
        return fiProvider;
    }

    public static class Builder {
        private String code;
        private String payletTypeCode;
        private List<String> tag;
        private Long linkedInstrumentProfileId;
        private String fiProvider;

        public Builder setCode(String code) {
            this.code = code;
            return this;
        }

        public Builder setPayletTypeCode(String payletTypeCode) {
            this.payletTypeCode = payletTypeCode;
            return this;
        }

        public Builder setTag(List<String> tag) {
            this.tag = tag;
            return this;
        }

        public Builder setLinkedInstrumentProfileId(Long linkedInstrumentProfileId) {
            this.linkedInstrumentProfileId = linkedInstrumentProfileId;
            return this;
        }

        public Builder setFiProvider(String fiProvider) {
            this.fiProvider = fiProvider;
            return this;
        }

        public CreateOutgoingDirectDebitProfileModel build() { return new CreateOutgoingDirectDebitProfileModel(this); }
    }

    public static CreateOutgoingDirectDebitProfileModel DefaultCreateOddProfileModel(final String managedAccountsProfileId) {
        return new CreateOutgoingDirectDebitProfileModel.Builder()
                .setCode("default_odds")
                .setPayletTypeCode("default_odds")
                .setFiProvider("modulr")
                .setTag(Collections.singletonList("test"))
                .setLinkedInstrumentProfileId(Long.parseLong(managedAccountsProfileId)).build();
    }
}
