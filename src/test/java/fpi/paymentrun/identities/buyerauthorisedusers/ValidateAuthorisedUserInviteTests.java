package fpi.paymentrun.identities.buyerauthorisedusers;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.models.ValidateUserInviteModel;
import fpi.paymentrun.services.BuyersAuthorisedUsersService;
import opc.junit.helpers.TestHelper;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_AUTH_USERS)
public class ValidateAuthorisedUserInviteTests extends BasePaymentRunSetup {

    private static Pair<String, String> buyer;

    @BeforeAll
    public static void TestSetup() {
        buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
    }

    @Test
    public void ValidateInvite_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        BuyersAuthorisedUsersService.validateUserInvite(ValidateUserInviteModel.validateModel(TestHelper.VERIFICATION_CODE), user.getLeft(), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ValidateInvite_RootUserId_InviteOrInviteCodeInvalid() {

        BuyersAuthorisedUsersService.validateUserInvite(ValidateUserInviteModel.validateModel(TestHelper.VERIFICATION_CODE), buyer.getLeft(), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVITE_OR_INVITE_CODE_INVALID"));
    }

    @Test
    public void ValidateInvite_UnknownVerificationToken_InviteOrInviteCodeInvalid() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        BuyersAuthorisedUsersService.validateUserInvite(ValidateUserInviteModel.validateModel(RandomStringUtils.randomNumeric(6)), user.getLeft(), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVITE_OR_INVITE_CODE_INVALID"));
    }

    @Test
    public void ValidateInvite_InvalidVerificationCode_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        BuyersAuthorisedUsersService.validateUserInvite(ValidateUserInviteModel.validateModel(RandomStringUtils.randomAlphabetic(6)), user.getLeft(), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.inviteCode: must match \"^[0-9]*$\""));
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 7})
    public void ValidateInvite_InvalidInviteCodeSize_BadRequest(final int inviteCodeSize) {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        BuyersAuthorisedUsersService.validateUserInvite(ValidateUserInviteModel.validateModel(RandomStringUtils.randomNumeric(inviteCodeSize)), user.getLeft(), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.inviteCode: size must be between 6 and 6"));
    }

    @Test
    public void ValidateInvite_AlreadyConsumed_InviteOrInviteCodeInvalid() {
        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        BuyersAuthorisedUsersService.validateUserInvite(ValidateUserInviteModel.validateModel(TestHelper.VERIFICATION_CODE), user.getLeft(), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVITE_OR_INVITE_CODE_INVALID"));
    }

    @Test
    @DisplayName("ValidateInvite_UnknownUserId_NotFound - get 409: DEV-6370 opened to return 404")
    public void ValidateInvite_UnknownUserId_NotFound() {

        BuyersAuthorisedUsersService.validateUserInvite(ValidateUserInviteModel.validateModel(TestHelper.VERIFICATION_CODE),
                        RandomStringUtils.randomNumeric(18), secretKey)
                .then()
                .statusCode(SC_CONFLICT);
    }

    @Test
    public void ValidateInvite_NoUserId_NotFound() {

        BuyersAuthorisedUsersService.validateUserInvite(ValidateUserInviteModel.validateModel(TestHelper.VERIFICATION_CODE), "", secretKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ValidateInvite_InvalidUserId_BadRequest() {

        BuyersAuthorisedUsersService.validateUserInvite(ValidateUserInviteModel.validateModel(TestHelper.VERIFICATION_CODE), RandomStringUtils.randomAlphabetic(18), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("user_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void ValidateInvite_InvalidSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.validateUserInvite(ValidateUserInviteModel.validateModel(TestHelper.VERIFICATION_CODE), user.getLeft(), RandomStringUtils.randomAlphabetic(10))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("ValidateInvite_OtherProgrammeSecretKey_Unauthorised - get 204: DEV-6370 opened to return 403")
    public void ValidateInvite_OtherProgrammeSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.validateUserInvite(ValidateUserInviteModel.validateModel(TestHelper.VERIFICATION_CODE), user.getLeft(), secretKeyAppTwo)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ValidateInvite_NoSecretKey_BadRequest() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.validateUserInvite(ValidateUserInviteModel.validateModel(TestHelper.VERIFICATION_CODE), user.getLeft(), "")
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    private Pair<String, BuyerAuthorisedUserModel> createUser() {
        return BuyerAuthorisedUserHelper.createUser(secretKey, buyer.getRight());
    }

    private Triple<String, BuyerAuthorisedUserModel, String> createAuthenticatedUser() {
        return BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyer.getRight());
    }
}
