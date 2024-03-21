package fpi.paymentrun.services.innovator;

import fpi.paymentrun.models.innovator.CreatePaymentRunProfilesModel;
import fpi.paymentrun.services.BaseService;
import io.restassured.response.Response;

public class InnovatorService extends BaseService {

    public static Response createPaymentRunProfiles(final CreatePaymentRunProfilesModel createPaymentRunProfilesModel,
                                                    final String token,
                                                    final String programmeId) {
        return getBodyAuthenticatedRequest(createPaymentRunProfilesModel, token)
                .pathParam("programme_id", programmeId)
                .when()
                .post(String.format("%s/embedder_portal/programmes/{programme_id}/profiles", getPaymentRunEnvironmentPrefix()));
    }
}
