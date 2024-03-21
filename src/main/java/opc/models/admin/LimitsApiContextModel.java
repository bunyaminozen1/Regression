package opc.models.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LimitsApiContextModel {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("PROGRAMME_ID")
    private final String programmeIdContext;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("IDENTITY_TYPE")
    private final String identityTypeContext;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("IDENTITY_ID")
    private final String identityIdContext;

    public LimitsApiContextModel(final Builder builder) {
        this.programmeIdContext = builder.programmeIdContext;
        this.identityIdContext = builder.identityIdContext;
        this.identityTypeContext = builder.identityTypeContext;
    }

    public String getProgrammeIdContext() {
        return programmeIdContext;
    }

    public String getIdentityTypeContext() {
        return identityTypeContext;
    }

    public String getIdentityIdContext() {
        return identityIdContext;
    }

    public static class Builder {
        private String programmeIdContext;
        private String identityTypeContext;
        private String identityIdContext;

        public Builder setProgrammeIdContext(String programmeIdContext) {
            this.programmeIdContext = programmeIdContext;
            return this;
        }

        public Builder setSepaInstantLimitContext(String programmeIdContext,
                                                  String identityIdContext,
                                                  String identityTypeContext) {
            this.programmeIdContext = programmeIdContext;
            this.identityIdContext = identityIdContext;
            this.identityTypeContext = identityTypeContext;
            return this;
        }

        public Builder setSepaInstantDefaultLimitContext(String identityTypeContext) {
            this.identityTypeContext = identityTypeContext;
            return this;
        }

        public LimitsApiContextModel build() { return new LimitsApiContextModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
