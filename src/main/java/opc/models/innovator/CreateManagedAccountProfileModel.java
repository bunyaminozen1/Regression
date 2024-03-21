package opc.models.innovator;

import commons.enums.Jurisdiction;
import opc.enums.opc.IdentityType;
import opc.models.shared.CurrencyAmount;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CreateManagedAccountProfileModel {
    private final String code;
    private final String payletTypeCode;
    private final List<String> currency;
    private final List<String> fiProvider;
    private final List<String> channelProvider;
    private final List<FeeDetailsModel> depositFee;
    private final List<DepositTypeFeeModel> depositTypeFee;

    public CreateManagedAccountProfileModel(final Builder builder) {
        this.code = builder.code;
        this.payletTypeCode = builder.payletTypeCode;
        this.currency = builder.currency;
        this.fiProvider = builder.fiProvider;
        this.channelProvider = builder.channelProvider;
        this.depositFee = builder.depositFee;
        this.depositTypeFee = builder.depositTypeFee;
    }

    public String getCode() {
        return code;
    }

    public String getPayletTypeCode() {
        return payletTypeCode;
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

    public static class Builder {

        private String code;
        private String payletTypeCode;
        private List<String> currency;
        private List<String> fiProvider;
        private List<String> channelProvider;
        private List<FeeDetailsModel> depositFee;
        private List<DepositTypeFeeModel> depositTypeFee;

        public Builder setCode(String code) {
            this.code = code;
            return this;
        }

        public Builder setPayletTypeCode(String payletTypeCode) {
            this.payletTypeCode = payletTypeCode;
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

        public CreateManagedAccountProfileModel build() {
            return new CreateManagedAccountProfileModel(this);
        }
    }

    public static CreateManagedAccountProfileModel DefaultCreateManagedAccountProfileModel(final IdentityType identityType) {
        final String code = String.format("%s_managed_accounts", identityType.name().toLowerCase());

        return new Builder()
                .setCode(code)
                .setPayletTypeCode(code)
                .setCurrency(Arrays.asList("EUR", "USD", "GBP"))
                .setFiProvider(Collections.singletonList("paynetics_eea"))
                .setChannelProvider(Collections.singletonList("gps"))
                //TODO Make Charges Dynamic and Available Commonly
                .setDepositTypeFee(Arrays.asList(
                        DepositTypeFeeModel.builder()
                                .setType("SEPA")
                                .setFees(Collections.singletonList(new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 110L),
                                                        new CurrencyAmount("GBP", 111L),
                                                        new CurrencyAmount("USD", 112L))))))
                                .build(),
                        DepositTypeFeeModel.builder()
                                .setType("SWIFT")
                                .setFees(Collections.singletonList(new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 113L),
                                                        new CurrencyAmount("GBP", 114L),
                                                        new CurrencyAmount("USD", 115L))))))
                                .build(),
                        DepositTypeFeeModel.builder()
                                .setType("FASTER_PAYMENTS")
                                .setFees(Collections.singletonList(new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 116L),
                                                        new CurrencyAmount("GBP", 117L),
                                                        new CurrencyAmount("USD", 118L))))))
                                .build()))
                .build();
    }

    public static CreateManagedAccountProfileModel ModulrCreateManagedAccountProfileModel(final IdentityType identityType) {
        final String code = String.format("%s_managed_accounts", identityType.name().toLowerCase());

        return new Builder()
                .setCode(code)
                .setPayletTypeCode(code)
                .setCurrency(Arrays.asList("EUR", "GBP"))
                .setFiProvider(Collections.singletonList("modulr"))
                .setChannelProvider(Collections.singletonList("modulr"))
                //TODO Make Charges Dynamic and Available Commonly
                .setDepositTypeFee(Arrays.asList(
                        DepositTypeFeeModel.builder()
                                .setType("SEPA")
                                .setFees(Collections.singletonList(new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 110L),
                                                        new CurrencyAmount("GBP", 111L),
                                                        new CurrencyAmount("USD", 112L))))))
                                .build(),
                        DepositTypeFeeModel.builder()
                                .setType("SWIFT")
                                .setFees(Collections.singletonList(new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 113L),
                                                        new CurrencyAmount("GBP", 114L),
                                                        new CurrencyAmount("USD", 115L))))))
                                .build(),
                        DepositTypeFeeModel.builder()
                                .setType("FASTER_PAYMENTS")
                                .setFees(Collections.singletonList(new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 116L),
                                                        new CurrencyAmount("GBP", 117L),
                                                        new CurrencyAmount("USD", 118L))))))
                                .build()))
                .build();
    }

    public static CreateManagedAccountProfileModel PayneticsModulrCreateManagedAccountProfileModel(final IdentityType identityType) {
        final String code = String.format("%s_managed_accounts", identityType.name().toLowerCase());

        return new Builder()
                .setCode(code)
                .setPayletTypeCode(code)
                .setCurrency(Arrays.asList("EUR", "GBP"))
                .setFiProvider(Arrays.asList("modulr", "paynetics_eea"))
                .setChannelProvider(Arrays.asList("modulr", "paynetics_eea"))
                //TODO Make Charges Dynamic and Available Commonly
                .setDepositTypeFee(Arrays.asList(
                        DepositTypeFeeModel.builder()
                                .setType("SEPA")
                                .setFees(Collections.singletonList(new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 110L),
                                                        new CurrencyAmount("GBP", 111L),
                                                        new CurrencyAmount("USD", 112L))))))
                                .build(),
                        DepositTypeFeeModel.builder()
                                .setType("SWIFT")
                                .setFees(Collections.singletonList(new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 113L),
                                                        new CurrencyAmount("GBP", 114L),
                                                        new CurrencyAmount("USD", 115L))))))
                                .build(),
                        DepositTypeFeeModel.builder()
                                .setType("FASTER_PAYMENTS")
                                .setFees(Collections.singletonList(new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 116L),
                                                        new CurrencyAmount("GBP", 117L),
                                                        new CurrencyAmount("USD", 118L))))))
                                .build()))
                .build();
    }

    public static CreateManagedAccountProfileModel DefaultCreatePluginsManagedAccountProfileModel() {
        return new Builder()
                .setCode("default_zerobalance")
                .setPayletTypeCode("default_zerobalance")
                .setCurrency(List.of("GBP"))
                .setFiProvider(Collections.singletonList("paynetics_uk"))
                .setChannelProvider(Collections.singletonList("gps"))
                .build();
    }



    public static CreateManagedAccountProfileModel DefaultCreateManagedAccountProfileModel(final IdentityType identityType, final Jurisdiction jurisdiction) {
        final String code = String.format("%s_managed_accounts_%s", identityType.name().toLowerCase(), jurisdiction.name().toLowerCase());
        final String payletTypeCode = String.format("%s_managed_accounts", identityType.name().toLowerCase());

        return new Builder()
                .setCode(code)
                .setPayletTypeCode(payletTypeCode)
                .setCurrency(Arrays.asList("EUR", "USD", "GBP"))
                .setFiProvider(Collections.singletonList(String.format("paynetics_%s", jurisdiction.name().toLowerCase())))
                .setChannelProvider(Collections.singletonList("gps"))
                //TODO Make Charges Dynamic and Available Commonly
                .setDepositTypeFee(Arrays.asList(
                        DepositTypeFeeModel.builder()
                                .setType("SEPA")
                                .setFees(Collections.singletonList(new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 110L),
                                                        new CurrencyAmount("GBP", 111L),
                                                        new CurrencyAmount("USD", 112L))))))
                                .build(),
                        DepositTypeFeeModel.builder()
                                .setType("SWIFT")
                                .setFees(Collections.singletonList(new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 113L),
                                                        new CurrencyAmount("GBP", 114L),
                                                        new CurrencyAmount("USD", 115L))))))
                                .build(),
                        DepositTypeFeeModel.builder()
                                .setType("FASTER_PAYMENTS")
                                .setFees(Collections.singletonList(new FeeDetailsModel().setFee(
                                        new FeeValuesModel().setType("FLAT")
                                                .setFlatAmount(Arrays.asList(new CurrencyAmount("EUR", 116L),
                                                        new CurrencyAmount("GBP", 117L),
                                                        new CurrencyAmount("USD", 118L))))))
                                .build()))
                .build();
    }
}
