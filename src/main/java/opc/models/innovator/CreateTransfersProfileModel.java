package opc.models.innovator;

import opc.models.shared.CurrencyAmount;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CreateTransfersProfileModel {
    private final String code;
    private final String payletTypeCode;
    private final String forexProvider;
    private final List<FeeDetailsModel> transferCardToAccountFee;
    private final List<FeeDetailsModel> transferAccountToCardFee;
    private final List<FeeDetailsModel> transferCardToCardFee;
    private final List<FeeDetailsModel> transferAccountToAccountFee;

    public CreateTransfersProfileModel(final Builder builder) {
        this.code = builder.code;
        this.payletTypeCode = builder.payletTypeCode;
        this.forexProvider = builder.forexProvider;
        this.transferCardToAccountFee = builder.transferCardToAccountFee;
        this.transferAccountToCardFee = builder.transferAccountToCardFee;
        this.transferCardToCardFee = builder.transferCardToCardFee;
        this.transferAccountToAccountFee = builder.transferAccountToAccountFee;
    }

    public String getCode() {
        return code;
    }

    public String getPayletTypeCode() {
        return payletTypeCode;
    }

    public String getForexProvider() {
        return forexProvider;
    }

    public List<FeeDetailsModel> getTransferCardToAccountFee() {
        return transferCardToAccountFee;
    }

    public List<FeeDetailsModel> getTransferAccountToCardFee() {
        return transferAccountToCardFee;
    }

    public List<FeeDetailsModel> getTransferCardToCardFee() {
        return transferCardToCardFee;
    }

    public List<FeeDetailsModel> getTransferAccountToAccountFee() {
        return transferAccountToAccountFee;
    }

    public static class Builder {

        private String code;
        private String payletTypeCode;
        private String forexProvider;
        private List<FeeDetailsModel> transferCardToAccountFee;
        private List<FeeDetailsModel> transferAccountToCardFee;
        private List<FeeDetailsModel> transferCardToCardFee;
        private List<FeeDetailsModel> transferAccountToAccountFee;

        public Builder setCode(String code) {
            this.code = code;
            return this;
        }

        public Builder setPayletTypeCode(String payletTypeCode) {
            this.payletTypeCode = payletTypeCode;
            return this;
        }

        public Builder setForexProvider(String forexProvider) {
            this.forexProvider = forexProvider;
            return this;
        }

        public Builder setTransferCardToAccountFee(List<FeeDetailsModel> transferCardToAccountFee) {
            this.transferCardToAccountFee = transferCardToAccountFee;
            return this;
        }

        public Builder setTransferAccountToCardFee(List<FeeDetailsModel> transferAccountToCardFee) {
            this.transferAccountToCardFee = transferAccountToCardFee;
            return this;
        }

        public Builder setTransferCardToCardFee(List<FeeDetailsModel> transferCardToCardFee) {
            this.transferCardToCardFee = transferCardToCardFee;
            return this;
        }

        public Builder setTransferAccountToAccountFee(List<FeeDetailsModel> transferAccountToAccountFee) {
            this.transferAccountToAccountFee = transferAccountToAccountFee;
            return this;
        }

        public CreateTransfersProfileModel build() {
            return new CreateTransfersProfileModel(this);
        }
    }

    public static CreateTransfersProfileModel DefaultCreateTransfersProfileModel() {
        return new CreateTransfersProfileModel.Builder()
                .setCode("default_transfers")
                .setPayletTypeCode("default_transfers")
                .setForexProvider("ecb")
                .setTransferCardToAccountFee(Collections.singletonList(new FeeDetailsModel()
                        .setName("DEFAULT")
                        .setFee(new FeeValuesModel()
                                .setType("FLAT")
                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 100L),
                                        new CurrencyAmount("GBP", 102L),
                                        new CurrencyAmount("USD", 105L)))
                                .setPercentage(new PercentageModel().setValue(0).setScale(0)))))
                .setTransferAccountToCardFee(Collections.singletonList(new FeeDetailsModel()
                        .setName("DEFAULT")
                        .setFee(new FeeValuesModel()
                                .setType("FLAT")
                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 100L),
                                        new CurrencyAmount("GBP", 102L),
                                        new CurrencyAmount("USD", 105L)))
                                .setPercentage(new PercentageModel().setValue(0).setScale(0)))))
                .setTransferCardToCardFee(Collections.singletonList(new FeeDetailsModel()
                        .setName("DEFAULT")
                        .setFee(new FeeValuesModel()
                                .setType("FLAT")
                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 100L),
                                        new CurrencyAmount("GBP", 102L),
                                        new CurrencyAmount("USD", 105L)))
                                .setPercentage(new PercentageModel().setValue(0).setScale(0)))))
                .setTransferAccountToAccountFee(Collections.singletonList(new FeeDetailsModel()
                        .setName("DEFAULT")
                        .setFee(new FeeValuesModel()
                                .setType("FLAT")
                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 100L),
                                        new CurrencyAmount("GBP", 102L),
                                        new CurrencyAmount("USD", 105L)))
                                .setPercentage(new PercentageModel().setValue(0).setScale(0)))))
                .build();
    }
}
