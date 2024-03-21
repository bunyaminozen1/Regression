package opc.models.admin;

public class SetGlobalLimitModel {

    private final LimitContextModel context;
    private final WindowWidthModel windowWidth;
    private final LimitValueModel limitValue;
    private final String wideWindowMultiple;
    private final FinancialLimitValueModel financialLimitValue;

    public SetGlobalLimitModel(final Builder builder) {
        this.context = builder.context;
        this.windowWidth = builder.windowWidth;
        this.limitValue = builder.limitValue;
        this.wideWindowMultiple = builder.wideWindowMultiple;
        this.financialLimitValue = builder.financialLimitValue;
    }

    public LimitContextModel getContext() {
        return context;
    }

    public WindowWidthModel getWindowWidth() {
        return windowWidth;
    }

    public LimitValueModel getLimitValue() {
        return limitValue;
    }

    public String getWideWindowMultiple() {
        return wideWindowMultiple;
    }

    public FinancialLimitValueModel getFinancialLimitValue() {
        return financialLimitValue;
    }

    public static class Builder {
        private LimitContextModel context;
        private WindowWidthModel windowWidth;
        private LimitValueModel limitValue;
        private String wideWindowMultiple;
        private FinancialLimitValueModel financialLimitValue;

        public Builder setContext(LimitContextModel context) {
            this.context = context;
            return this;
        }

        public Builder setWindowWidth(WindowWidthModel windowWidth) {
            this.windowWidth = windowWidth;
            return this;
        }

        public Builder setLimitValue(LimitValueModel limitValue) {
            this.limitValue = limitValue;
            return this;
        }

        public Builder setWideWindowMultiple(String wideWindowMultiple) {
            this.wideWindowMultiple = wideWindowMultiple;
            return this;
        }

        public Builder setFinancialLimitValue(FinancialLimitValueModel financialLimitValue) {
            this.financialLimitValue = financialLimitValue;
            return this;
        }

        public SetGlobalLimitModel build() { return new SetGlobalLimitModel(this);}
    }

    public static Builder builder(){
        return new Builder();
    }
}
