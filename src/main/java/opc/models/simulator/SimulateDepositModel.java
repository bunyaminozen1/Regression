package opc.models.simulator;

import com.github.javafaker.Faker;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import opc.enums.opc.DepositType;
import opc.models.multi.outgoingwiretransfers.FasterPaymentsBankDetailsModel;
import opc.models.multi.outgoingwiretransfers.SepaBankDetailsModel;
import opc.models.shared.CurrencyAmount;
import org.apache.commons.lang3.RandomStringUtils;

@Builder
@Getter
@Setter
public class SimulateDepositModel {
    private final CurrencyAmount depositAmount;
    private final String reference;
    private final String senderName;
    private final SepaBankDetailsModel sepa;
    private final FasterPaymentsBankDetailsModel fasterPayments;

    public static SimulateDepositModel defaultSimulateModel(final CurrencyAmount depositAmount) {
        return SimulateDepositModel.builder()
                .depositAmount(depositAmount)
                .reference("RefTest123")
                .senderName("Sender Test")
                .build();
    }

    public static SimulateDepositModel defaultSimulateModel(final CurrencyAmount depositAmount,
                                                            final DepositType depositType) {
        final SimulateDepositModelBuilder simulateDepositBuilder =
                SimulateDepositModel.builder()
                        .depositAmount(depositAmount)
                        .reference(String.format("%s reference", depositType.name()))
                        .senderName((String.format("%s sender", depositType.name())));

        switch (depositType) {
            case SEPA:
                simulateDepositBuilder.sepa(new SepaBankDetailsModel("TS123123123213213124", "TS12TEST124"));
                break;
            case FASTER_PAYMENTS:
                simulateDepositBuilder.fasterPayments(new FasterPaymentsBankDetailsModel(RandomStringUtils.randomNumeric(8), RandomStringUtils.randomNumeric(6).toUpperCase()));
                break;
        }

        return simulateDepositBuilder.build();
    }

    public static SimulateDepositModel dataSimulateModel(final CurrencyAmount depositAmount) {
        final Faker faker = new Faker();
        return SimulateDepositModel.builder()
                .depositAmount(depositAmount)
                .reference(faker.beer().name())
                .senderName(String.format("%s %s", faker.name().firstName(), faker.name().lastName()))
                .build();
    }
}
