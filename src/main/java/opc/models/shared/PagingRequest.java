package opc.models.shared;

public class PagingRequest {
    private PagingModel paging;

    public PagingRequest(final int offset, final int limit) {
        this.paging = new PagingModel(offset, limit);
    }

    public PagingModel getPaging() {
        return paging;
    }

    public PagingRequest setPaging(PagingModel paging) {
        this.paging = paging;
        return this;
    }
}
