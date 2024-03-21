package opc.models.sumsub;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import opc.enums.sumsub.CallbackReviewStatus;
import opc.enums.sumsub.CallbackType;

public class SumSubCallbackModel {

    private final String type;
    private final String reviewStatus;
    private final String applicantId;
    private final String externalUserId;
    private final ReviewResultModel reviewResult;
    public final String createdAt;

    public SumSubCallbackModel(final Builder builder) {
        this.type = builder.type.getValue();
        this.reviewStatus = builder.reviewStatus.getValue();
        this.applicantId = builder.applicantId;
        this.externalUserId = builder.externalUserId;
        this.reviewResult = builder.reviewResult;
        this.createdAt=builder.createdAt;
    }

    public String getType() {
        return type;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public ReviewResultModel getReviewResult() {
        return reviewResult;
    }
    public String getCreatedAt(){return createdAt;}

    public static class Builder {
        private CallbackType type;
        private CallbackReviewStatus reviewStatus;
        private String applicantId;
        private String externalUserId;
        private ReviewResultModel reviewResult;
        private String createdAt;

        public Builder setType(CallbackType type) {
            this.type = type;
            return this;
        }

        public Builder setReviewStatus(CallbackReviewStatus reviewStatus) {
            this.reviewStatus = reviewStatus;
            return this;
        }

        public Builder setApplicantId(String applicantId) {
            this.applicantId = applicantId;
            return this;
        }

        public Builder setExternalUserId(String externalUserId) {
            this.externalUserId = externalUserId;
            return this;
        }

        public Builder setReviewResult(ReviewResultModel reviewResult) {
            this.reviewResult = reviewResult;
            return this;
        }
        public Builder setCreatedAt(){
            this.createdAt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ").format(Calendar.getInstance().getTime());
            return this;
        }

        public SumSubCallbackModel build() { return new SumSubCallbackModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }
}
