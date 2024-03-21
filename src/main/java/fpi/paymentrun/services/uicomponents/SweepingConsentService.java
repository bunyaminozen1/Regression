package fpi.paymentrun.services.uicomponents;

import fpi.paymentrun.models.IssueSweepingConsentChallengeModel;
import fpi.paymentrun.models.VerifySweepingConsentModel;
import fpi.paymentrun.services.BaseService;
import io.restassured.response.Response;

public class SweepingConsentService extends BaseService {

    public static Response issueSweepingConsentChallenge(final IssueSweepingConsentChallengeModel issueSweepingConsentChallengeModel,
                                                         final String authenticationToken,
                                                         final String enrolmentChannel,
                                                         final String sharedKey) {
        return getBodyAuthenticatedRequest(issueSweepingConsentChallengeModel, authenticationToken)
                .header("programme-key", sharedKey)
                .pathParam("channel", enrolmentChannel)
                .when()
                .post(String.format("%s/ui_components/v1/sweeping_consents/challenges/otp/{channel}/challenges", getPaymentRunEnvironmentPrefix()));
    }

    /**
     * Method for negative cases to check response if call without apiKey
     */
    public static Response issueSweepingConsentChallengeNoApiKey(final IssueSweepingConsentChallengeModel issueSweepingConsentChallengeModel,
                                                         final String authenticationToken,
                                                         final String enrolmentChannel) {
        return getBodyAuthenticatedRequest(issueSweepingConsentChallengeModel, authenticationToken)
                .pathParam("channel", enrolmentChannel)
                .when()
                .post(String.format("%s/ui_components/v1/sweeping_consents/challenges/otp/{channel}/challenges", getPaymentRunEnvironmentPrefix()));
    }

    public static Response verifySweepingConsentChallenge(final VerifySweepingConsentModel verifySweepingConsentModel,
                                                          final String authenticationToken,
                                                          final String scaChallengeId,
                                                          final String enrolmentChannel,
                                                          final String sharedKey) {
        return getBodyAuthenticatedRequest(verifySweepingConsentModel, authenticationToken)
                .header("programme-key", sharedKey)
                .pathParam("scaChallengeId", scaChallengeId)
                .pathParam("channel", enrolmentChannel)
                .when()
                .post(String.format("%s/ui_components/v1/sweeping_consents/challenges/{scaChallengeId}/otp/{channel}/verify", getPaymentRunEnvironmentPrefix()));
    }

    /**
     * Method for negative cases to check response if call without apiKey
     */
    public static Response verifySweepingConsentChallengeNoApiKey(final VerifySweepingConsentModel verifySweepingConsentModel,
                                                          final String authenticationToken,
                                                          final String scaChallengeId,
                                                          final String enrolmentChannel) {
        return getBodyAuthenticatedRequest(verifySweepingConsentModel, authenticationToken)
                .pathParam("scaChallengeId", scaChallengeId)
                .pathParam("channel", enrolmentChannel)
                .when()
                .post(String.format("%s/ui_components/v1/sweeping_consents/challenges/{scaChallengeId}/otp/{channel}/verify", getPaymentRunEnvironmentPrefix()));
    }

    /**
     * Job for sweeping failed payment and unmatched IWTs
     * The API Secret Key - internally set api key to run jobs
     * QA api-key: JP5WqY96xlt7RcmsrhJ7E5bIE
     * FB api-key: 4d68a70cdb834fff1
     */
    public static Response executeSweepingJob() {
        return getApiKeyRequest(getSweepingJobApiKey())
                .when()
                .post(String.format("%s/jobs/sweeping/execute", getPaymentRunEnvironmentPrefix()));
    }
}
