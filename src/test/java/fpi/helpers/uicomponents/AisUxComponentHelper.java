package fpi.helpers.uicomponents;

import fpi.paymentrun.models.AisUxInstitutionResponseModel;
import fpi.paymentrun.models.CreateAisAuthorisationUrlRequestModel;
import fpi.paymentrun.models.ReAuthoriseAisConsentModel;
import fpi.paymentrun.services.uicomponents.AisUxComponentService;
import io.restassured.response.Response;
import opc.junit.helpers.TestHelper;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_CREATED;

public class AisUxComponentHelper {

    public static String getInstitutionId(final String buyerToken,
                                          final String sharedKey) {
        return TestHelper.ensureAsExpected(15,
                        () -> AisUxComponentService.getInstitutionsList(buyerToken, sharedKey), SC_OK)
                .jsonPath()
                .get("institutions[3].id");
    }

    public static AisUxInstitutionResponseModel getInstitution(final String buyerToken,
                                                               final String sharedKey) {
        return TestHelper.ensureAsExpected(15,
                        () -> AisUxComponentService.getInstitutionsList(buyerToken, sharedKey), SC_OK)
                .as(AisUxInstitutionResponseModel.class);
    }

    public static String createAuthorisationUrlMockBank(final String buyerToken,
                                                        final String sharedKey) {
        final CreateAisAuthorisationUrlRequestModel createAuthorisationUrlRequestModel =
                CreateAisAuthorisationUrlRequestModel.defaultCreateAuthorisationUrlRequestModel("mock-bank").build();

        return TestHelper.ensureAsExpected(15,
                        () -> AisUxComponentService.createAuthorisationUrl(createAuthorisationUrlRequestModel, buyerToken, sharedKey),
                        SC_CREATED)
                .then()
                .extract()
                .jsonPath()
                .get("authorisationUrl");
    }

    public static String getConsentId(final String buyerToken,
                                      final String sharedKey,
                                      final String linkedAccountId) {
        return TestHelper.ensureAsExpected(15,
                        () -> AisUxComponentService.getAisConsentLinkedAccountInfo(buyerToken, sharedKey, linkedAccountId), SC_OK)
                .then()
                .extract()
                .jsonPath()
                .get("linkedAccounts[0].consent.consentId");
    }

    public static Response getAisConsentLinkedAccountInfo(final String buyerToken,
                                                          final String sharedKey,
                                                          final String linkedAccountId) {
        return TestHelper.ensureAsExpected(15,
                () -> AisUxComponentService.getAisConsentLinkedAccountInfo(buyerToken, sharedKey, linkedAccountId),
                SC_OK);
    }

    public static String reAuthoriseAisConsent(final String consentId,
                                               final String buyerToken,
                                               final String sharedKey) {
        final ReAuthoriseAisConsentModel reAuthoriseAisConsentModel =
                ReAuthoriseAisConsentModel.defaultReAuthoriseAisConsentModel().build();
        return TestHelper.ensureAsExpected(15,
                        () -> AisUxComponentService.reAuthoriseAisConsent(reAuthoriseAisConsentModel, consentId, buyerToken, sharedKey),
                        SC_CREATED)
                .then()
                .extract()
                .jsonPath()
                .get("authorisationUrl");
    }
}
