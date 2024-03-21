package fpi.helpers;

import fpi.paymentrun.services.AuthenticationService;
import fpi.paymentrun.services.uicomponents.PaymentRunConsentService;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.VerificationModel;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class AuthenticationHelper {
    public static String createUserPassword(final String userId,
                                            final String secretKey) {
        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.createPassword(createPasswordModel, userId, secretKey),
                SC_OK);

        return createPasswordModel.getPassword().getValue();
    }

    public static void enrolOtp(final String channel,
                                final String secretKey,
                                final String token) {
        TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.enrolOtp(channel, secretKey, token),
                SC_NO_CONTENT);
    }

    public static void verifyOtpEnrolment(final String verificationCode,
                                          final String channel,
                                          final String secretKey,
                                          final String token) {
        TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.verifyEnrolment(new VerificationModel(verificationCode), channel, secretKey, token),
                SC_NO_CONTENT);
    }

    public static void enrolAndVerifyOtp(final String verificationCode,
                                         final String channel,
                                         final String secretKey,
                                         final String token) {
        enrolOtp(channel, secretKey, token);
        verifyOtpEnrolment(verificationCode, channel, secretKey, token);
    }

    public static void verifyStepup(final String verificationCode,
                                    final String channel,
                                    final String secretKey,
                                    final String token) {
        TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.verifyStepup(new VerificationModel(verificationCode), channel, secretKey, token),
                SC_NO_CONTENT);
    }

    public static void startStepup(final String secretKey,
                                   final String token) {
        TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secretKey, token),
                SC_NO_CONTENT);
    }

    public static void startAndVerifyStepup(final String verificationCode,
                                            final String channel,
                                            final String secretKey,
                                            final String token) {
        startStepup(secretKey, token);
        verifyStepup(verificationCode, channel, secretKey, token);
    }

    public static void logout(final String secretKey,
                              final String token) {
        TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.logout(secretKey, token),
                SC_NO_CONTENT);
    }

    public static String login(final String email,
                               final String password,
                               final String secretKey) {
        return TestHelper.ensureAsExpected(15,
                        () -> AuthenticationService.loginWithPassword(new LoginModel(email, new PasswordModel(password)), secretKey),
                        SC_OK)
                .then()
                .extract()
                .jsonPath()
                .get("token");
    }

    public static void startIssueScaPaymentRun(final String token,
                                               final String sharedKey,
                                               final String paymentRunId) {
        TestHelper.ensureAsExpected(15,
                () -> PaymentRunConsentService.issueScaChallengeRequest(token, sharedKey, paymentRunId, EnrolmentChannel.SMS.name()),
                SC_NO_CONTENT);
    }

    public static void verifyScaPaymentRun(final String verificationCode,
                                           final String token,
                                           final String sharedKey,
                                           final String channel,
                                           final String paymentRunId) {

        TestHelper.ensureAsExpected(15,
                () -> PaymentRunConsentService.verifyScaChallengeRequest(new VerificationModel(verificationCode), token, sharedKey, paymentRunId, channel),
                SC_NO_CONTENT);
    }

    public static void startAndVerifyScaPaymentRun(final String verificationCode,
                                                   final String sharedKey,
                                                   final String token,
                                                   final String channel,
                                                   final String paymentRunId) {

        startIssueScaPaymentRun(token, sharedKey, paymentRunId);
        verifyScaPaymentRun(verificationCode, token, sharedKey, channel, paymentRunId);
    }

}
