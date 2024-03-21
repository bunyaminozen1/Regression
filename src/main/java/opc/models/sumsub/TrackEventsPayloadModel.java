package opc.models.sumsub;

public class TrackEventsPayloadModel {

    private final String locale;
    private final String flowType;
    private final String applicantId;
    private final boolean forceDocapture;
    private final boolean ssLivenessAltLivenessServerUrl;
    private final boolean ssLivenessFilterExtremeAngles;
    private final String idDocType;
    private final String idDocSetType;

    public TrackEventsPayloadModel(final Builder builder) {
        this.locale = builder.locale;
        this.flowType = builder.flowType;
        this.applicantId = builder.applicantId;
        this.forceDocapture = builder.forceDocapture;
        this.ssLivenessAltLivenessServerUrl = builder.ssLivenessAltLivenessServerUrl;
        this.ssLivenessFilterExtremeAngles = builder.ssLivenessFilterExtremeAngles;
        this.idDocType = builder.idDocType;
        this.idDocSetType = builder.idDocSetType;
    }

    public String getLocale() {
        return locale;
    }

    public String getFlowType() {
        return flowType;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public boolean isForceDocapture() {
        return forceDocapture;
    }

    public boolean isSsLivenessAltLivenessServerUrl() {
        return ssLivenessAltLivenessServerUrl;
    }

    public boolean isSsLivenessFilterExtremeAngles() {
        return ssLivenessFilterExtremeAngles;
    }

    public String getIdDocType() {
        return idDocType;
    }

    public String getIdDocSetType() {
        return idDocSetType;
    }

    public static class Builder {
        private String locale;
        private String flowType;
        private String applicantId;
        private boolean forceDocapture;
        private boolean ssLivenessAltLivenessServerUrl;
        private boolean ssLivenessFilterExtremeAngles;
        private String idDocType;
        private String idDocSetType;

        public Builder setLocale(String locale) {
            this.locale = locale;
            return this;
        }

        public Builder setFlowType(String flowType) {
            this.flowType = flowType;
            return this;
        }

        public Builder setApplicantId(String applicantId) {
            this.applicantId = applicantId;
            return this;
        }

        public Builder setForceDocapture(boolean forceDocapture) {
            this.forceDocapture = forceDocapture;
            return this;
        }

        public Builder setSsLivenessAltLivenessServerUrl(boolean ssLivenessAltLivenessServerUrl) {
            this.ssLivenessAltLivenessServerUrl = ssLivenessAltLivenessServerUrl;
            return this;
        }

        public Builder setSsLivenessFilterExtremeAngles(boolean ssLivenessFilterExtremeAngles) {
            this.ssLivenessFilterExtremeAngles = ssLivenessFilterExtremeAngles;
            return this;
        }

        public Builder setIdDocType(String idDocType) {
            this.idDocType = idDocType;
            return this;
        }

        public Builder setIdDocSetType(String idDocSetType) {
            this.idDocSetType = idDocSetType;
            return this;
        }

        public TrackEventsPayloadModel build() {
            return new TrackEventsPayloadModel(this);
        }
    }

    public static Builder defaultCompanyTracking(final String applicantId){
        return new Builder()
                .setLocale("en")
                .setFlowType("standalone")
                .setApplicantId(applicantId)
                .setForceDocapture(false)
                .setSsLivenessAltLivenessServerUrl(false)
                .setSsLivenessFilterExtremeAngles(true)
                .setIdDocSetType("COMPANY");
    }
}
