package spi.openbanking.openbanking;

import commons.enums.Currency;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import spi.openbanking.helpers.OpenBankingHelper;
import spi.openbanking.models.CreateOutgoingWireTransferAuthorisationModel;
import spi.openbanking.models.PayeeModel;
import spi.openbanking.models.PayerModel;
import spi.openbanking.models.PaymentModel;
import spi.openbanking.services.OpenBankingService;

import java.util.List;

import static org.apache.http.HttpStatus.SC_CREATED;

public class CreateOutgoingWireTransferAuthorisationTests {

    @Test
    public void CreateOwtAuthorisation_Success() {

        final CreateOutgoingWireTransferAuthorisationModel createOwtAuthorisation =
                CreateOutgoingWireTransferAuthorisationModel.builder()
                        .payer(PayerModel.builder().accountId("64c11f209b0c3467ec9a1cda").build())
                        .payments(List.of(PaymentModel.builder()
                                .idempotencyId(RandomStringUtils.randomAlphabetic(10))
                                .payee(PayeeModel.builder()
                                        .name("Name Surname")
                                        .sortCode("262368")
                                        .accountNumber("36832861")
                                        .build())
                                .amount(4)
                                .currency(Currency.GBP.name())
                                .reference("fdp")
                                .contextType("BILL")
                                .build()))
                        .paymentDate("2023-07-27")
                        .callbackUrl("https://goal.com/")
                        .state("EXAMPLE")
                        .build();

        OpenBankingService.createOutgoingWireTransferAuthorisation(createOwtAuthorisation, OpenBankingHelper.API_KEY)
                .then()
                .statusCode(SC_CREATED);
    }
}
