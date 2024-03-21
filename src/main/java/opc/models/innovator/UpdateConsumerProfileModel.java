package opc.models.innovator;

import java.util.List;

public class UpdateConsumerProfileModel {
    private final List<FeeModel> customFee;

    public UpdateConsumerProfileModel(final UpdateConsumerProfileModel.Builder builder) {
        this.customFee = builder.customFee;
    }

    public List<FeeModel> getCustomFee() {
        return customFee;
    }

    public static class Builder {
        private List<FeeModel> customFee;

        public UpdateConsumerProfileModel.Builder setCustomFee(List<FeeModel> customFee) {
            this.customFee = customFee;
            return this;
        }

        public UpdateConsumerProfileModel build() {
            return new UpdateConsumerProfileModel(this);
        }
    }

    public static UpdateConsumerProfileModel.Builder builder() {
        return new UpdateConsumerProfileModel.Builder();
    }
}
