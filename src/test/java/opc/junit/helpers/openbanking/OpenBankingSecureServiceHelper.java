package opc.junit.helpers.openbanking;

import opc.junit.helpers.TestHelper;
import opc.models.openbanking.LoginModel;
import opc.models.openbanking.VerificationModel;
import opc.services.openbanking.OpenBankingSecureService;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class OpenBankingSecureServiceHelper {

    public static String login(final String email,
                               final String sharedKey,
                               final String tppId) {
        return TestHelper.ensureAsExpected(15,
                () -> OpenBankingSecureService.login(sharedKey, LoginModel.defaultLoginModel(email), tppId),
                SC_OK).jsonPath().getString("token");
    }

    public static void authoriseConsent(final String sharedKey,
                                        final String authenticationToken,
                                        final String tppId,
                                        final String consentId) {
        TestHelper.ensureAsExpected(10,
                () -> OpenBankingSecureService.authoriseConsent(sharedKey, authenticationToken, tppId, consentId),
                SC_OK);
    }

    public static void authoriseAndVerifyPayment(final String sharedKey,
                                                 final String authenticationToken,
                                                 final String tppId,
                                                 final String paymentConsentId) {
        authorisePayment(sharedKey, authenticationToken, tppId, paymentConsentId);
        initiatePaymentChallenge(sharedKey, authenticationToken, tppId, paymentConsentId);
        verifyPaymentChallenge(sharedKey, authenticationToken, tppId, paymentConsentId, VerificationModel.defaultVerification());
    }

    public static void authorisePayment(final String sharedKey,
                                        final String authenticationToken,
                                        final String tppId,
                                        final String paymentConsentId) {
        TestHelper.ensureAsExpected(10,
                () -> OpenBankingSecureService.authorisePayment(sharedKey, authenticationToken, tppId, paymentConsentId),
                SC_OK);
    }

    public static void initiatePaymentChallenge(final String sharedKey,
                                                final String authenticationToken,
                                                final String tppId,
                                                final String paymentConsentId) {
        TestHelper.ensureAsExpected(10,
                () -> OpenBankingSecureService.initiatePaymentChallenge(sharedKey, authenticationToken, tppId, paymentConsentId),
                SC_OK);
    }

    public static void verifyPaymentChallenge(final String sharedKey,
                                              final String authenticationToken,
                                              final String tppId,
                                              final String paymentConsentId,
                                              final VerificationModel verificationModel) {
        TestHelper.ensureAsExpected(10,
                () -> OpenBankingSecureService.verifyPaymentChallenge(sharedKey, authenticationToken, tppId, paymentConsentId, verificationModel),
                SC_OK);
    }

    public static void logout(final String sharedKey,
                              final String token,
                              final String tppId) {
        TestHelper.ensureAsExpected(15,
                () -> OpenBankingSecureService.logout(sharedKey, token, tppId),
                SC_NO_CONTENT);
    }
}
