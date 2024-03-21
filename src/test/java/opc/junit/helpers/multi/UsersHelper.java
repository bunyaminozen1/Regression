package opc.junit.helpers.multi;

import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.secure.SecureHelper;
import opc.models.multi.users.ConsumerUserInviteModel;
import opc.models.multi.users.UserVerifyEmailModel;
import opc.models.multi.users.UsersModel;
import opc.models.multi.users.ValidateUserInviteModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class UsersHelper {
    public static String VERIFICATION_CODE = TestHelper.OTP_VERIFICATION_CODE;

    public static Pair<String, String> createAuthenticatedUser(final String secretKey,
                                                               final String authenticationToken) {
        final UsersModel usersModel =
                UsersModel.DefaultUsersModel().build();

        return createAuthenticatedUser(usersModel, secretKey, authenticationToken);
    }

    public static Pair<String, String> createAuthenticatedUser(final UsersModel usersModel,
                                                               final String secretKey,
                                                               final String authenticationToken) {
        final String userId =
                TestHelper.ensureAsExpected(15,
                                () -> UsersService.createUser(usersModel, secretKey, authenticationToken, Optional.empty()),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        inviteUser(secretKey, userId, authenticationToken);
        validateUserInvite(secretKey, userId);
        final String token = consumeUserInvite(secretKey, userId);

        return Pair.of(userId, token);
    }

    public static Pair<String, String> createEnrolledAuthenticatedUser(final UsersModel modelUser,
                                                         final String secretKey,
                                                         final String identityToken) {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(modelUser, secretKey, identityToken);
        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, user.getRight());
        return user;
    }

    public static Pair<String, String> createEnrolledBiometricAuthenticatedUser(final UsersModel modelUser,
                                                                       final String sharedKey,
                                                                       final String secretKey,
                                                                       final String identityToken) {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(modelUser, secretKey, identityToken);
        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());

        return user;
    }

    public static Pair<String, String> createAuthyEnrolledUser(final UsersModel modelUser,
                                                                       final String secretKey,
                                                                       final String identityToken) {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(modelUser, secretKey, identityToken);
        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey, user.getRight());
        return user;
    }

    public static String createUser(final String secretKey,
                                    final String authenticationToken) {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        return TestHelper.ensureAsExpected(15, () -> UsersService.createUser(usersModel, secretKey, authenticationToken, Optional.empty()), SC_OK)
                .jsonPath()
                .get("id");
    }

    public static String createUser(final UsersModel usersModel,
                                    final String secretKey,
                                    final String authenticationToken) {
        return TestHelper.ensureAsExpected(15, () -> UsersService.createUser(usersModel, secretKey, authenticationToken, Optional.empty()), SC_OK)
                .jsonPath()
                .get("id");
    }

    public static void inviteUser(final String secretKey,
                                  final String userId,
                                  final String authenticationToken) {
        TestHelper.ensureAsExpected(15,
                () -> UsersService.inviteUser(secretKey, userId, authenticationToken),
                SC_NO_CONTENT);
    }

    public static void validateUserInvite(final String secretKey,
                                          final String userId) {
        TestHelper.ensureAsExpected(15,
                () -> UsersService.validateUserInvite(new ValidateUserInviteModel(TestHelper.VERIFICATION_CODE), secretKey, userId),
                SC_NO_CONTENT);
    }

    public static String consumeUserInvite(final String secretKey,
                                           final String userId) {
        return TestHelper.ensureAsExpected(15,
                        () -> UsersService.consumeUserInvite(
                                new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(secretKey))),
                                secretKey, userId),
                        SC_OK)
                .jsonPath().get("token");
    }

    public static void updateUser(final UsersModel usersModel, final String secretKey, final String userId, final String authenticationToken) {
        TestHelper.ensureAsExpected(15, () -> UsersService.patchUser(usersModel, secretKey, userId, authenticationToken, Optional.empty()), SC_OK);
    }

    public static Pair<String, String> createEnrolledUser(final UsersModel usersModel,
                                                          final String secretKey,
                                                          final String authenticationToken) {
        final String userId =
                TestHelper.ensureAsExpected(15,
                                () -> UsersService.createUser(usersModel, secretKey, authenticationToken, Optional.empty()),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        inviteUser(secretKey, userId, authenticationToken);
        validateUserInvite(secretKey, userId);
        final String token = consumeUserInvite(secretKey, userId);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, token);

        return Pair.of(userId, token);
    }

    public static String startUserKyc(final String secretKey, final String token) {
        return TestHelper.ensureAsExpected(15,
                        () -> UsersService.startUserKyc(secretKey, token),
                        SC_OK)
                .jsonPath()
                .get("reference");
    }

    public static void verifyEmail(final String secretKey, final String userEmail) {
        TestHelper.ensureAsExpected(15,
                () -> UsersService.sendEmailVerification(new SendEmailVerificationModel(userEmail), secretKey),
                SC_NO_CONTENT);

        TestHelper.ensureAsExpected(15,
                () -> UsersService.verifyUserEmail(UserVerifyEmailModel.builder()
                        .email(userEmail)
                        .verificationCode(TestHelper.VERIFICATION_CODE).build(), secretKey),
                SC_NO_CONTENT);
    }

    public static Pair<String, String> createEnrolledAllFactorsUser(final UsersModel usersModel,
                                                                    final String identityToken,
                                                                    final String secretKey,
                                                                    final String sharedKey) {
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, identityToken);

        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE,
                EnrolmentChannel.SMS.name(), secretKey, user.getRight());

        AuthenticationFactorsHelper.enrolAndVerifyAuthyPush(user.getLeft(), secretKey, user.getRight());

        SecureHelper.enrolAndVerifyBiometric(user.getLeft(), sharedKey, secretKey, user.getRight());
        return user;
    }
}