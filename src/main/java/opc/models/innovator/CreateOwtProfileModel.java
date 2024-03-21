package opc.models.innovator;

import opc.models.shared.CurrencyAmount;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CreateOwtProfileModel {
    private final String code;
    private final String payletTypeCode;
    private final List<String> supportedType;
    private final List<OwtFeeModel> fee;

    public CreateOwtProfileModel(final Builder builder) {
        this.code = builder.code;
        this.payletTypeCode = builder.payletTypeCode;
        this.supportedType = builder.supportedType;
        this.fee = builder.fee;
    }

    public String getCode() {
        return code;
    }

    public String getPayletTypeCode() {
        return payletTypeCode;
    }

    public List<String> getSupportedType() {
        return supportedType;
    }

    public List<OwtFeeModel> getFee() {
        return fee;
    }

    public static class Builder {
        private String code;
        private String payletTypeCode;
        private List<String> supportedType;
        private List<OwtFeeModel> fee;

        public Builder setCode(String code) {
            this.code = code;
            return this;
        }

        public Builder setPayletTypeCode(String payletTypeCode) {
            this.payletTypeCode = payletTypeCode;
            return this;
        }

        public Builder setSupportedType(List<String> supportedType) {
            this.supportedType = supportedType;
            return this;
        }

        public Builder setFee(List<OwtFeeModel> fee) {
            this.fee = fee;
            return this;
        }

        public CreateOwtProfileModel build() {
            return new CreateOwtProfileModel(this);
        }
    }



    public static CreateOwtProfileModel DefaultCreateOwtProfileModel() {
        return new CreateOwtProfileModel.Builder()
                .setCode("default_owts")
                .setPayletTypeCode("default_owts")
                .setSupportedType(Arrays.asList("SEPA", "FASTER_PAYMENTS"))
                .setFee(Arrays.asList(
                        new OwtFeeModel()
                                .setType("SEPA")
                                .setFees(Collections.singletonList(
                                        new FeeDetailsModel()
                                                .setName("DEFAULT")
                                                .setFee(
                                                        new FeeValuesModel()
                                                                .setType("FLAT")
                                                                .setFlatAmount(Arrays.asList(
                                                                        new CurrencyAmount("EUR", 101L),
                                                                        new CurrencyAmount("GBP", 103L),
                                                                        new CurrencyAmount("USD", 107L)))))),
                        new OwtFeeModel()
                                .setType("FASTER_PAYMENTS")
                                .setFees(Collections.singletonList(
                                        new FeeDetailsModel()
                                                .setName("DEFAULT")
                                                .setFee(
                                                        new FeeValuesModel()
                                                                .setType("FLAT")
                                                                .setFlatAmount(Arrays.asList(
                                                                        new CurrencyAmount("EUR", 100L),
                                                                        new CurrencyAmount("GBP", 102L),
                                                                        new CurrencyAmount("USD", 105L))))))))
                .build();
    }

    public static CreateOwtProfileModel DefaultCreatePluginPaymentProfileModel() {
        return new CreateOwtProfileModel.Builder()
                .setCode("default_payment")
                .setPayletTypeCode("default_payment")
                .setSupportedType(Arrays.asList("SEPA", "FASTER_PAYMENTS")).build();
    }

    public static CreateOwtProfileModel DefaultCreatePluginSweepProfileModel() {
        return new CreateOwtProfileModel.Builder()
                .setCode("default_sweep")
                .setPayletTypeCode("default_sweep")
                .setSupportedType(Arrays.asList("SEPA", "FASTER_PAYMENTS")).build();
    }
}
