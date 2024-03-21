package opc.models.shared;

public class PagingModel {
    private final int offset;
    private final int limit;

    public PagingModel(final int offset, final int limit) {
        this.offset = offset;
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }
}
