package opc.models.admin;

public class PagingLimitModel {
    private int offset;
    private int limit;

    public int getOffset() {
        return offset;
    }

    public PagingLimitModel setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public PagingLimitModel setLimit(int limit) {
        this.limit = limit;
        return this;
    }
}
