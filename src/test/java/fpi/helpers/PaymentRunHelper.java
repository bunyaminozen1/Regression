package fpi.helpers;

import fpi.paymentrun.models.innovator.CreatePaymentRunProfilesModel;
import fpi.paymentrun.models.innovator.PaymentRunProfilesResponse;
import fpi.paymentrun.services.innovator.InnovatorService;
import opc.junit.helpers.TestHelper;

import static org.apache.http.HttpStatus.SC_CREATED;

public class PaymentRunHelper {

    public static PaymentRunProfilesResponse createPaymentRunProfiles(final String innovatorName,
                                                                      final String token,
                                                                      final String programmeId) {
        return TestHelper.ensureAsExpected(15,
                        () -> InnovatorService.createPaymentRunProfiles(CreatePaymentRunProfilesModel.defaultCreatePaymentRunProgrammesModel(innovatorName),
                                token,
                                programmeId), SC_CREATED)
                .as(PaymentRunProfilesResponse.class);
    }
}
