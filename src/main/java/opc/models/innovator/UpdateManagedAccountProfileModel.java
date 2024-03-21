package opc.models.innovator;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public class UpdateManagedAccountProfileModel {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String code;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final boolean hasTag;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String tag;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<String> currency;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<String> fiProvider;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<String> channelProvider;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<FeeDetailsModel> depositFee;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<DepositTypeFeeModel> depositTypeFee;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String proxyFiProvider;

    public UpdateManagedAccountProfileModel(final Builder builder) {
        this.code = builder.code;
        this.hasTag = builder.hasTag;
        this.tag = builder.tag;
        this.currency = builder.currency;
        this.fiProvider = builder.fiProvider;
        this.channelProvider = builder.channelProvider;
        this.depositFee = builder.depositFee;
        this.depositTypeFee = builder.depositTypeFee;
        this.proxyFiProvider = builder.proxyFiProvider;
    }

    public String getCode() {
        return code;
    }

    public boolean isHasTag() {
        return hasTag;
    }

    public String getTag() {
        return tag;
    }

    public List<String> getCurrency() {
        return currency;
    }

    public List<String> getFiProvider() {
        return fiProvider;
    }

    public List<String> getChannelProvider() {
        return channelProvider;
    }

    public List<FeeDetailsModel> getDepositFee() {
        return depositFee;
    }

    public List<DepositTypeFeeModel> getDepositTypeFee() {
        return depositTypeFee;
    }

    public String getProxyFiProvider() { return proxyFiProvider; }

    public static class Builder {
        private String code;
        private boolean hasTag;
        private String tag;
        private List<String> currency;
        private List<String> fiProvider;
        private List<String> channelProvider;
        private List<FeeDetailsModel> depositFee;
        private List<DepositTypeFeeModel> depositTypeFee;
        private String proxyFiProvider;

        public Builder setCode(String code) {
            this.code = code;
            return this;
        }

        public Builder setHasTag(boolean hasTag) {
            this.hasTag = hasTag;
            return this;
        }

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder setCurrency(List<String> currency) {
            this.currency = currency;
            return this;
        }

        public Builder setFiProvider(List<String> fiProvider) {
            this.fiProvider = fiProvider;
            return this;
        }

        public Builder setChannelProvider(List<String> channelProvider) {
            this.channelProvider = channelProvider;
            return this;
        }

        public Builder setDepositFee(List<FeeDetailsModel> depositFee) {
            this.depositFee = depositFee;
            return this;
        }

        public Builder setDepositTypeFee(List<DepositTypeFeeModel> depositTypeFee) {
            this.depositTypeFee = depositTypeFee;
            return this;
        }

        public Builder setProxyFiProvider(String proxyFiProvider) {
            this.proxyFiProvider = proxyFiProvider;
            return this;
        }

        public UpdateManagedAccountProfileModel build() { return new UpdateManagedAccountProfileModel(this); }
    }

    public static Builder builder() { return new Builder(); }
}
