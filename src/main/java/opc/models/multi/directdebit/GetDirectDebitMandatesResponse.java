package opc.models.multi.directdebit;

import java.util.List;

public class GetDirectDebitMandatesResponse {

    private List<GetMandateModel> mandate;
    private Long count;
    private Long responseCount;

    public List<GetMandateModel> getMandate() {
        return mandate;
    }

    public GetDirectDebitMandatesResponse setMandate(List<GetMandateModel> mandate) {
        this.mandate = mandate;
        return this;
    }

    public Long getCount() {
        return count;
    }

    public GetDirectDebitMandatesResponse setCount(Long count) {
        this.count = count;
        return this;
    }

    public Long getResponseCount() {
        return responseCount;
    }

    public GetDirectDebitMandatesResponse setResponseCount(Long responseCount) {
        this.responseCount = responseCount;
        return this;
    }
}
