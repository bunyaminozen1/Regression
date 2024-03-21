package fpi.helpers.uicomponents;

import fpi.paymentrun.models.CreatePisAuthorisationUrlRequestModel;
import fpi.paymentrun.services.uicomponents.PisUxComponentService;
import opc.junit.helpers.TestHelper;

import static org.apache.http.HttpStatus.SC_CREATED;

public class PisUxComponentsHelper {
    public static String createAuthorisationUrl(final String paymentRunReference,
                                                final String buyerToken,
                                                final String sharedKey){
        final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel =
                CreatePisAuthorisationUrlRequestModel.defaultCreatePisAuthorisationUrlRequestModel(paymentRunReference).build();

        return TestHelper.ensureAsExpected(15,
                ()-> PisUxComponentService.createAuthorisationUrl(createPisAuthorisationUrlRequestModel, buyerToken, sharedKey),
                SC_CREATED)
                .then()
                .extract()
                .jsonPath()
                .get("authorisationUrl");
    }
}
