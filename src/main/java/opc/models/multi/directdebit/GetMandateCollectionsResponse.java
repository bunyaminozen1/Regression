package opc.models.multi.directdebit;

import java.util.List;

public class GetMandateCollectionsResponse {

    private List<GetCollectionModel> collection;
    private Long count;
    private Long responseCount;

    public List<GetCollectionModel> getCollection() {
        return collection;
    }

    public GetMandateCollectionsResponse setCollection(List<GetCollectionModel> collection) {
        this.collection = collection;
        return this;
    }

    public Long getCount() {
        return count;
    }

    public GetMandateCollectionsResponse setCount(Long count) {
        this.count = count;
        return this;
    }

    public Long getResponseCount() {
        return responseCount;
    }

    public GetMandateCollectionsResponse setResponseCount(Long responseCount) {
        this.responseCount = responseCount;
        return this;
    }
}
