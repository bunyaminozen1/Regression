package opc.models.sumsub;

public class ApproveDocumentModel {

    private String reviewAnswer;

    public ApproveDocumentModel(final String reviewAnswer) {
        this.reviewAnswer = reviewAnswer;
    }

    public String getReviewAnswer() {
        return reviewAnswer;
    }

    public ApproveDocumentModel setReviewAnswer(String reviewAnswer) {
        this.reviewAnswer = reviewAnswer;
        return this;
    }
}
