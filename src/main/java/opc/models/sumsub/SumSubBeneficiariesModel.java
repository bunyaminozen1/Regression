package opc.models.sumsub;

import java.util.List;

public class SumSubBeneficiariesModel {

    public String getApplicantId() {
        return applicantId;
    }

    public SumSubBeneficiariesModel setApplicantId(String applicantId) {
        this.applicantId = applicantId;
        return this;
    }

    public List<String> getPositions() {
        return positions;
    }

    public SumSubBeneficiariesModel setPositions(List<String> positions) {
        this.positions = positions;
        return this;
    }

    public String getType() {
        return type;
    }

    public SumSubBeneficiariesModel setType(String type) {
        this.type = type;
        return this;
    }

    public boolean isInRegistry() {
        return inRegistry;
    }

    public SumSubBeneficiariesModel setInRegistry(boolean inRegistry) {
        this.inRegistry = inRegistry;
        return this;
    }

    public List<Integer> getImageIds() {
        return imageIds;
    }

    public SumSubBeneficiariesModel setImageIds(List<Integer> imageIds) {
        this.imageIds = imageIds;
        return this;
    }

    public String getApplicant() {
        return applicant;
    }

    public SumSubBeneficiariesModel setApplicant(String applicant) {
        this.applicant = applicant;
        return this;
    }

    private String applicantId;
    private List<String> positions;
    private String type;
    private boolean inRegistry;
    private List<Integer> imageIds;
    private String applicant;
}
