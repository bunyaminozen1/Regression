package fpi.helpers;

import fpi.paymentrun.services.uicomponents.PaymentRunConsentService;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.models.shared.VerificationModel;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;

public class ChallengesHelper {
    protected static final String VERIFICATION_CODE = TestHelper.VERIFICATION_CODE;

    public static void issueScaChallenge(final String token,
                                         final String sharedKey,
                                         final String paymentRunId) {
        TestHelper.ensureAsExpected(15,
                () -> PaymentRunConsentService.issueScaChallengeRequest(token, sharedKey, paymentRunId, EnrolmentChannel.SMS.name()),
                SC_NO_CONTENT);
    }

    public static void verifyScaChallenge(final String token,
                                          final String sharedKey,
                                          final String paymentRunId) {
        TestHelper.ensureAsExpected(15,
                () -> PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(VERIFICATION_CODE), token, sharedKey, paymentRunId, EnrolmentChannel.SMS.name()),
                SC_NO_CONTENT);
    }

    public static void issueAndVerifyScaChallenge(final String token,
                                                  final String sharedKey,
                                                  final String paymentRunId) {
        issueScaChallenge(token, sharedKey, paymentRunId);
        verifyScaChallenge(token, sharedKey,  paymentRunId);
    }
}
