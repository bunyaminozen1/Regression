package opc.junit.helpers.multi;

import opc.junit.helpers.TestHelper;
import opc.models.shared.Identity;
import opc.models.shared.LoginModel;
import opc.models.shared.LoginWithBiometricModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.VerificationModel;
import opc.services.multi.AuthenticationService;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class AuthenticationHelper {

    public static String login(final String email,
                               final String secretKey) {
        return login(email, TestHelper.getDefaultPassword(secretKey), secretKey);
    }

    public static String login(final String email,
                               final String password,
                               final String secretKey) {
        return TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.loginWithPassword(new LoginModel(email, new PasswordModel(password)), secretKey),
                SC_OK).jsonPath().getString("token");
    }

    public static void logout(final String token,
                              final String secretKey) {
        TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.logout(secretKey, token),
                SC_NO_CONTENT);
    }

    public static void startStepup(final String channel,
                                   final String secretKey,
                                   final String token) {
        TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.startStepup(channel, secretKey, token),
                SC_NO_CONTENT);
    }

    public static void verifyStepup(final String verificationCode,
                                    final String channel,
                                    final String secretKey,
                                    final String token) {
        TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.verifyStepup(new VerificationModel(verificationCode), channel, secretKey, token),
                SC_NO_CONTENT);
    }

    public static void startAndVerifyStepup(final String verificationCode,
                                            final String channel,
                                            final String secretKey,
                                            final String token) {
        startStepup(channel, secretKey, token);
        verifyStepup(verificationCode, channel, secretKey, token);
    }

    public static String issuePushStepup(final String channel,
                                         final String secretKey,
                                         final String token) {
        return TestHelper.ensureAsExpected(60,
                        () -> AuthenticationService.issuePushStepup(channel, secretKey, token),
                        SC_OK)
                .jsonPath()
                .get("id");
    }

    public static String requestAccessToken(final Identity identity,
                                            final String secretKey,
                                            final String token) {
        return TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.accessToken(identity, secretKey, token),
                SC_OK).jsonPath().getString("token");
    }

    public static String loginWithBiometric(final LoginWithBiometricModel login,
                                              final String secretKey) {
        return TestHelper.ensureAsExpected(15,
                () -> AuthenticationService.loginWithBiometric(login, secretKey),
                SC_OK).jsonPath().getString("challengeId");
    }
}
