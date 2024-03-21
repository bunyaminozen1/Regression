package opc.models.admin;

import java.util.List;

public class ConsumersWithKycResponseModel {

    private List<ConsumerWithKycResponseModel> consumerWithKyc;
    private Long count;
    private Long responseCount;

    public List<ConsumerWithKycResponseModel> getConsumerWithKyc() {
        return consumerWithKyc;
    }

    public void setConsumerWithKyc(List<ConsumerWithKycResponseModel> consumerWithKyc) {
        this.consumerWithKyc = consumerWithKyc;
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
