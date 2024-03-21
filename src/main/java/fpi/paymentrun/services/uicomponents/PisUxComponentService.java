package fpi.paymentrun.services.uicomponents;

import fpi.paymentrun.models.CreatePisAuthorisationUrlRequestModel;
import fpi.paymentrun.services.BaseService;
import io.restassured.response.Response;

public class PisUxComponentService extends BaseService {

    public static Response getConsentInfo(final String reference,
                                          final String authenticationToken,
                                          final String sharedKey) {
        return getProgrammeKeyAuthenticationRequest(sharedKey, authenticationToken)
                .pathParam("reference", reference)
                .when()
                .get(String.format("%s/ui_components/v1/ob_pis/consent_info/{reference}", getPaymentRunEnvironmentPrefix()));
    }

    public static Response createAuthorisationUrl(final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel,
                                                  final String authenticationToken,
                                                  final String sharedKey) {
        return getBodyProgrammeKeyAuthenticationRequest(createPisAuthorisationUrlRequestModel, sharedKey, authenticationToken)
                .when()
                .post(String.format("%s/ui_components/v1/ob_pis/auth_url", getPaymentRunEnvironmentPrefix()));
    }

    /**
     * Method for NoApiKey cases
     */
    public static Response createAuthorisationUrlNoSharedKey(final CreatePisAuthorisationUrlRequestModel createPisAuthorisationUrlRequestModel,
                                                             final String authenticationToken) {
        return getBodyAuthenticatedRequest(createPisAuthorisationUrlRequestModel, authenticationToken)
                .when()
                .post(String.format("%s/ui_components/v1/ob_pis/auth_url", getPaymentRunEnvironmentPrefix()));
    }
}