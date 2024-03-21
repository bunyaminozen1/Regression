package fpi.paymentrun.services.uicomponents;

import fpi.paymentrun.models.CreateAisAuthorisationUrlRequestModel;
import fpi.paymentrun.models.ReAuthoriseAisConsentModel;
import fpi.paymentrun.services.BaseService;
import io.restassured.response.Response;
import opc.enums.opc.UrlType;

public class AisUxComponentService extends BaseService {

    public static Response getInstitutionsList(final String authenticationToken,
                                               final String sharedKey) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", authenticationToken))
                .header("origin", getBaseUrl(UrlType.BASE))
                .when()
                .get(String.format("%s/ui_components/v1/ob_ais/institutions", getPaymentRunEnvironmentPrefix()));
    }

    public static Response createAuthorisationUrl(final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel,
                                                  final String authenticationToken,
                                                  final String sharedKey) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", authenticationToken))
                .header("origin", getBaseUrl(UrlType.BASE))
                .body(createAuthorisationUrlRequestModel)
                .when()
                .post(String.format("%s/ui_components/v1/ob_ais/auth_url", getPaymentRunEnvironmentPrefix()));
    }

    public static Response getAisConsentInfo(final String authenticationToken,
                                             final String sharedKey) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", authenticationToken))
                .header("origin", getBaseUrl(UrlType.BASE))
                .when()
                .get(String.format("%s/ui_components/v1/ob_ais/context", getPaymentRunEnvironmentPrefix()));
    }

    public static Response getAisConsentLinkedAccountInfo(final String authenticationToken,
                                                          final String sharedKey,
                                                          final String linkedAccountId) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", authenticationToken))
                .header("origin", getBaseUrl(UrlType.BASE))
                .pathParam("linked_account_id", linkedAccountId)
                .when()
                .get(String.format("%s/ui_components/v1/ob_ais/linked_accounts/{linked_account_id}/context", getPaymentRunEnvironmentPrefix()));
    }

    public static Response reAuthoriseAisConsent(final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel,
                                                 final String consentId,
                                                 final String authenticationToken,
                                                 final String sharedKey) {
        return restAssured()
                .header("Content-type", "application/json")
                .header("programme-key", sharedKey)
                .header("Authorization", String.format("Bearer %s", authenticationToken))
                .header("origin", getBaseUrl(UrlType.BASE))
                .body(reAuthoriseAisConsentModel)
                .pathParam("consent_id", consentId)
                .when()
                .post(String.format("%s/ui_components/v1/ob_ais/consents/{consent_id}/reauthorise", getPaymentRunEnvironmentPrefix()));
    }
}