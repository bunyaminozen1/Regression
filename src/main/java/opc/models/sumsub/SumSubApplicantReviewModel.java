package opc.models.sumsub;

public class SumSubApplicantReviewModel {

    private String reviewId;
    private String attemptId;
    private int attemptCnt;
    private boolean reprocessing;
    private String levelName;
    private int levelAutoCheckMode;
    private String createDate;
    private String reviewStatus;
    private int priority;
    private boolean autoChecked;
    private String reviewReasonCode;

    public String getReviewId() {
        return reviewId;
    }

    public SumSubApplicantReviewModel setReviewId(String reviewId) {
        this.reviewId = reviewId;
        return this;
    }

    public String getAttemptId() {
        return attemptId;
    }

    public SumSubApplicantReviewModel setAttemptId(String attemptId) {
        this.attemptId = attemptId;
        return this;
    }

    public int getAttemptCnt() {
        return attemptCnt;
    }

    public SumSubApplicantReviewModel setAttemptCnt(int attemptCnt) {
        this.attemptCnt = attemptCnt;
        return this;
    }

    public boolean isReprocessing() {
        return reprocessing;
    }

    public SumSubApplicantReviewModel setReprocessing(boolean reprocessing) {
        this.reprocessing = reprocessing;
        return this;
    }

    public String getLevelName() {
        return levelName;
    }

    public SumSubApplicantReviewModel setLevelName(String levelName) {
        this.levelName = levelName;
        return this;
    }

    public int getLevelAutoCheckMode () { return levelAutoCheckMode; }

    public SumSubApplicantReviewModel setLevelAutoCheckMode (int levelAutoCheckMode){
        this.levelAutoCheckMode = levelAutoCheckMode;
        return this;
    }

    public String getCreateDate() {
        return createDate;
    }

    public SumSubApplicantReviewModel setCreateDate(String createDate) {
        this.createDate = createDate;
        return this;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public SumSubApplicantReviewModel setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public SumSubApplicantReviewModel setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public boolean isAutoChecked() {
        return autoChecked;
    }

    public SumSubApplicantReviewModel setAutoChecked(boolean autoChecked) {
        this.autoChecked = autoChecked;
        return this;
    }

    public String getReviewReasonCode() {
        return reviewReasonCode;
    }

    public SumSubApplicantReviewModel setReviewReasonCode(String reviewReasonCode) {
        this.reviewReasonCode = reviewReasonCode;
        return this;
    }
}
