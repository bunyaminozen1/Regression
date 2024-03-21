package opc.models.innovator;

import opc.models.shared.PagingModel;

public class GetReportsModel {

    private final PagingModel paging;
    private final String category;

    public GetReportsModel(final Builder builder) {
        this.paging = builder.paging;
        this.category = builder.category;
    }

    public PagingModel getPaging() {
        return paging;
    }

    public String getCategory() {
        return category;
    }

    public static class Builder {
        private PagingModel paging;
        private String category;

        public Builder setPaging(PagingModel paging) {
            this.paging = paging;
            return this;
        }

        public Builder setCategory(String category) {
            this.category = category;
            return this;
        }

        public GetReportsModel build() { return new GetReportsModel(this); }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static GetReportsModel defaultGetReportsModel() {
        return new Builder()
                .setPaging(new PagingModel(0, 10))
                .build();
    }
}
