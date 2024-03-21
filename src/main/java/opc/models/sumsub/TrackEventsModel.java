package opc.models.sumsub;

public class TrackEventsModel {

    private final String activity;
    private final TrackEventsPayloadModel payload;

    public TrackEventsModel(final Builder builder) {
        this.activity = builder.activity;
        this.payload = builder.payload;
    }

    public String getActivity() {
        return activity;
    }

    public TrackEventsPayloadModel getPayload() {
        return payload;
    }

    public static class Builder {
        private String activity;
        private TrackEventsPayloadModel payload;

        public Builder setActivity(String activity) {
            this.activity = activity;
            return this;
        }

        public Builder setPayload(TrackEventsPayloadModel payload) {
            this.payload = payload;
            return this;
        }

        public TrackEventsModel build() { return new TrackEventsModel(this);}
    }

    public static Builder defaultCompanyTracking(final String applicantId){
        return new Builder()
                .setActivity("user:completed:step.COMPANY")
                .setPayload(TrackEventsPayloadModel.defaultCompanyTracking(applicantId).build());
    }
}
