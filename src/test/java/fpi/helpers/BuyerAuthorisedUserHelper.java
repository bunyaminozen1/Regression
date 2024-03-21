package fpi.helpers;

import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.models.ConsumeUserInviteModel;
import fpi.paymentrun.services.BuyersAuthorisedUsersService;
import opc.enums.opc.EnrolmentChannel;
import opc.junit.helpers.TestHelper;
import opc.models.shared.PasswordModel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

public class BuyerAuthorisedUserHelper {
    public static String VERIFICATION_CODE = TestHelper.VERIFICATION_CODE;

    public static Pair<String, BuyerAuthorisedUserModel> createUser(final BuyerAuthorisedUserModel createBuyerAuthorisedUserModel,
                                                                    final String secretKey,
                                                                    final String buyerToken) {

        final String id =
                TestHelper.ensureAsExpected(30,
                                () -> BuyersAuthorisedUsersService.createUser(createBuyerAuthorisedUserModel, secretKey, buyerToken),
                                SC_OK)
                        .jsonPath()
                        .get("id");

        return Pair.of(id, createBuyerAuthorisedUserModel);
    }

    public static Pair<String, BuyerAuthorisedUserModel> createUser(final String secretKey,
                                                                    final String buyerToken) {
        final BuyerAuthorisedUserModel userModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        return createUser(userModel, secretKey, buyerToken);
    }

    public static Triple<String, BuyerAuthorisedUserModel, String> createAuthenticatedUser(final BuyerAuthorisedUserModel createBuyerAuthorisedUserModel,
                                                                                           final String secretKey,
                                                                                           final String buyerToken) {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser(createBuyerAuthorisedUserModel, secretKey, buyerToken);

        TestHelper.ensureAsExpected(30,
                () -> BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey, buyerToken),
                SC_NO_CONTENT);

        final String token =
                TestHelper.ensureAsExpected(30,
                                () -> BuyersAuthorisedUsersService.consumeUserInvite(ConsumeUserInviteModel.builder()
                                                .inviteCode(TestHelper.VERIFICATION_CODE)
                                                .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                                                .build(),
                                        user.getLeft(), secretKey),
                                SC_OK)
                        .jsonPath()
                        .get("token");

        return Triple.of(user.getLeft(), user.getRight(), token);
    }


    public static Triple<String, BuyerAuthorisedUserModel, String> createAuthenticatedUser(final String secretKey,
                                                                                           final String buyerToken) {

        final BuyerAuthorisedUserModel userModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        return createAuthenticatedUser(userModel, secretKey, buyerToken);
    }

    public static Triple<String, BuyerAuthorisedUserModel, String> createEnrolledAuthenticatedUser(final String secretKey,
                                                                                                   final String buyerToken) {
        final Triple<String, BuyerAuthorisedUserModel, String> authUser = BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyerToken);
        AuthenticationHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, authUser.getRight());

        return authUser;
    }

    public static void sendInvite(final String userId,
                                  final String secretKey,
                                  final String buyerToken) {

        TestHelper.ensureAsExpected(30,
                () -> BuyersAuthorisedUsersService.sendUserInvite(userId, secretKey, buyerToken),
                SC_NO_CONTENT);
    }

    public static void deactivateUser(final String userId,
                                      final String secretKey,
                                      final String buyerToken) {

        TestHelper.ensureAsExpected(30,
                () -> BuyersAuthorisedUsersService.deactivateUser(userId, secretKey, buyerToken),
                SC_NO_CONTENT);
    }

    public static Pair<String, String> createEnrolledSteppedUpUser(final String secretKey,
                                                                   final String token) {
        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser(secretKey, token);
        AuthenticationHelper.enrolAndVerifyOtp(TestHelper.VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, user.getRight());
        AuthenticationHelper.startAndVerifyStepup(TestHelper.VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, user.getRight());
        return Pair.of(user.getLeft(), user.getRight());
    }

    public static void assignCreatorRole(final String userId,
                                         final String secretKey,
                                         final String buyerToken) {
        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.creatorRoleUsersModel().build();
        TestHelper.ensureAsExpected(120,
                () -> BuyersAuthorisedUsersService.updateUser(updateUserModel, userId, secretKey, buyerToken),
                SC_OK);
    }

    public static void assignControllerRole(final String userId,
                                            final String secretKey,
                                            final String buyerToken) {
        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.controllerRoleUsersModel().build();
        TestHelper.ensureAsExpected(120,
                () -> BuyersAuthorisedUsersService.updateUser(updateUserModel, userId, secretKey, buyerToken),
                SC_OK);
    }

    public static void assignAllRoles(final String userId,
                                      final String secretKey,
                                      final String buyerToken) {
        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.allRolesUsersModel().build();
        TestHelper.ensureAsExpected(120,
                () -> BuyersAuthorisedUsersService.updateUser(updateUserModel, userId, secretKey, buyerToken),
                SC_OK);
    }
}
