package opc.models.admin;

import java.util.List;

public class CorporatesWithKybResponseModel {
    private List<CorporateWithKybResponseModel> corporateWithKyb;
    private Long count;
    private Long responseCount;

    public List<CorporateWithKybResponseModel> getCorporateWithKyb() {
        return corporateWithKyb;
    }

    public void setCorporateWithKyb(List<CorporateWithKybResponseModel> corporateWithKyb) {
        this.corporateWithKyb = corporateWithKyb;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Long getResponseCount() {
        return responseCount;
    }

    public void setResponseCount(Long responseCount) {
        this.responseCount = responseCount;
    }

}
