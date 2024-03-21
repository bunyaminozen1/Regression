package opc.models.innovator;

import opc.models.shared.CurrencyAmount;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CreateSendProfileModel {
    private final String code;
    private final String payletTypeCode;
    private final String forexProvider;
    private final List<FeeDetailsModel> sendCardToAccountFee;
    private final List<FeeDetailsModel> sendAccountToCardFee;
    private final List<FeeDetailsModel> sendCardToCardFee;
    private final List<FeeDetailsModel> sendAccountToAccountFee;

    public CreateSendProfileModel(final Builder builder) {
        this.code = builder.code;
        this.payletTypeCode = builder.payletTypeCode;
        this.forexProvider = builder.forexProvider;
        this.sendCardToAccountFee = builder.sendCardToAccountFee;
        this.sendAccountToCardFee = builder.sendAccountToCardFee;
        this.sendCardToCardFee = builder.sendCardToCardFee;
        this.sendAccountToAccountFee = builder.sendAccountToAccountFee;
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

    public List<FeeDetailsModel> getSendCardToAccountFee() {
        return sendCardToAccountFee;
    }

    public List<FeeDetailsModel> getSendAccountToCardFee() {
        return sendAccountToCardFee;
    }

    public List<FeeDetailsModel> getSendCardToCardFee() {
        return sendCardToCardFee;
    }

    public List<FeeDetailsModel> getSendAccountToAccountFee() {
        return sendAccountToAccountFee;
    }

    public static class Builder {

        private String code;
        private String payletTypeCode;
        private String forexProvider;
        private List<FeeDetailsModel> sendCardToAccountFee;
        private List<FeeDetailsModel> sendAccountToCardFee;
        private List<FeeDetailsModel> sendCardToCardFee;
        private List<FeeDetailsModel> sendAccountToAccountFee;

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

        public Builder setSendCardToAccountFee(List<FeeDetailsModel> sendCardToAccountFee) {
            this.sendCardToAccountFee = sendCardToAccountFee;
            return this;
        }

        public Builder setSendAccountToCardFee(List<FeeDetailsModel> sendAccountToCardFee) {
            this.sendAccountToCardFee = sendAccountToCardFee;
            return this;
        }

        public Builder setSendCardToCardFee(List<FeeDetailsModel> sendCardToCardFee) {
            this.sendCardToCardFee = sendCardToCardFee;
            return this;
        }

        public Builder setSendAccountToAccountFee(List<FeeDetailsModel> sendAccountToAccountFee) {
            this.sendAccountToAccountFee = sendAccountToAccountFee;
            return this;
        }

        public CreateSendProfileModel build() {
            return new CreateSendProfileModel(this);
        }
    }

    public static CreateSendProfileModel DefaultCreateSendProfileModel() {
        return new CreateSendProfileModel.Builder()
                .setCode("default_send")
                .setPayletTypeCode("default_send")
                .setForexProvider("ecb")
                .setSendCardToAccountFee(Collections.singletonList(new FeeDetailsModel()
                        .setName("DEFAULT")
                        .setFee(new FeeValuesModel()
                                .setType("FLAT")
                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 100L),
                                        new CurrencyAmount("GBP", 102L),
                                        new CurrencyAmount("USD", 105L)))
                                .setPercentage(new PercentageModel().setValue(0).setScale(0)))))
                .setSendAccountToCardFee(Collections.singletonList(new FeeDetailsModel()
                        .setName("DEFAULT")
                        .setFee(new FeeValuesModel()
                                .setType("FLAT")
                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 100L),
                                        new CurrencyAmount("GBP", 102L),
                                        new CurrencyAmount("USD", 105L)))
                                .setPercentage(new PercentageModel().setValue(0).setScale(0)))))
                .setSendCardToCardFee(Collections.singletonList(new FeeDetailsModel()
                        .setName("DEFAULT")
                        .setFee(new FeeValuesModel()
                                .setType("FLAT")
                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 100L),
                                        new CurrencyAmount("GBP", 102L),
                                        new CurrencyAmount("USD", 105L)))
                                .setPercentage(new PercentageModel().setValue(0).setScale(0)))))
                .setSendAccountToAccountFee(Collections.singletonList(new FeeDetailsModel()
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
