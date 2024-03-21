package opc.models.sumsub;

import opc.enums.sumsub.ReviewRejectType;

import java.util.List;

public class ReviewResultModel {

    private final String reviewAnswer;
    private final List<String> rejectLabels;
    private final ReviewRejectType reviewRejectType;
    private final String moderationComment;

    public ReviewResultModel(final Builder builder) {
        this.reviewAnswer = builder.reviewAnswer;
        this.rejectLabels = builder.rejectLabels;
        this.reviewRejectType = builder.reviewRejectType;
        this.moderationComment = builder.moderationComment;
    }

    public String getReviewAnswer() {
        return reviewAnswer;
    }

    public List<String> getRejectLabels() {
        return rejectLabels;
    }

    public ReviewRejectType getReviewRejectType() {
        return reviewRejectType;
    }

    public String getModerationComment() {
        return moderationComment;
    }

    public static class Builder {
        private String reviewAnswer;
        private List<String> rejectLabels;
        private ReviewRejectType reviewRejectType;
        private String moderationComment;

        public Builder setReviewAnswer(String reviewAnswer) {
            this.reviewAnswer = reviewAnswer;
            return this;
        }

        public Builder setRejectLabels(List<String> rejectLabels) {
            this.rejectLabels = rejectLabels;
            return this;
        }

        public Builder setReviewRejectType(ReviewRejectType reviewRejectType) {
            this.reviewRejectType = reviewRejectType;
            return this;
        }

        public Builder setModerationComment(String moderationComment) {
            this.moderationComment = moderationComment;
            return this;
        }

        public ReviewResultModel build() { return new ReviewResultModel(this); }
    }

    public static Builder builder(){ return new Builder(); }
}
