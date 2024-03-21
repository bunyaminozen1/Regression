package fpi.paymentrun.identities.buyerauthorisedusers;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.models.ConsumeUserInviteModel;
import fpi.paymentrun.services.AuthenticationService;
import fpi.paymentrun.services.BuyersAuthorisedUsersService;
import opc.junit.helpers.TestHelper;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.emptyString;

@Tag(PluginsTags.PAYMENT_RUN_AUTH_USERS)
public class ConsumeAuthorisedUserInviteTests extends BasePaymentRunSetup {

    private static Pair<String, String> buyer;

    @BeforeAll
    public static void TestSetup() {
        buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
    }

    @Test
    public void ConsumeInvite_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(TestHelper.VERIFICATION_CODE)
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, user.getLeft(), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", not(emptyString()));

        AuthenticationService.loginWithPassword(new LoginModel(user.getRight().getEmail(), consumeUserInviteModel.getPassword()), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", not(emptyString()));

    }

    @Test
    public void ConsumeInvite_AlreadyConsumed_PasswordAlreadyCreated() {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(TestHelper.VERIFICATION_CODE)
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, user.getLeft(), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_ALREADY_CREATED"));
    }

    @Test
    public void ConsumeInvite_RootUser_InviteOrInviteCodeInvalid() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(TestHelper.VERIFICATION_CODE)
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, buyer.getLeft(), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVITE_OR_INVITE_CODE_INVALID"));
    }

    @Test
    public void ConsumeInvite_WrongCode_InviteOrInviteCodeInvalid() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(RandomStringUtils.randomNumeric(6))
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, user.getLeft(), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVITE_OR_INVITE_CODE_INVALID"));
    }

    @Test
    public void ConsumeInvite_InvalidInviteCode_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(RandomStringUtils.randomAlphabetic(6))
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, user.getLeft(), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.inviteCode: must match \"^[0-9]*$\""));
    }

    @Test
    public void ConsumeInvite_NullInviteCode_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(null)
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, user.getLeft(), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST).body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("inviteCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));;
    }

    @Test
    public void ConsumeInvite_EmptyInviteCode_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode("")
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, user.getLeft(), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Bad Request"));
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 7})
    public void ConsumeInvite_InvalidInviteCodeSize_BadRequest(final int inviteCodeSize) {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(RandomStringUtils.randomNumeric(inviteCodeSize))
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, user.getLeft(), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.inviteCode: size must be between 6 and 6"));
    }

    @Test
    public void ConsumeInvite_NoPasswordModel_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(TestHelper.VERIFICATION_CODE)
                        .password(null)
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, user.getLeft(), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("password"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @ParameterizedTest
    @EmptySource
    public void ConsumeInvite_EmptyPassword_BadRequest(final String password) {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(TestHelper.VERIFICATION_CODE)
                        .password(new PasswordModel(password))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, user.getLeft(), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.password.value: must not be blank"));
    }

    @Test
    public void ConsumeInvite_NullPassword_BadRequest() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(TestHelper.VERIFICATION_CODE)
                        .password(new PasswordModel(null))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, user.getLeft(), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("password")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("value"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void ConsumeInvite_UnknownUserId_UnresolvedIdentity() {

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(TestHelper.VERIFICATION_CODE)
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, RandomStringUtils.randomNumeric(18), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_IDENTITY"));
    }

    @Test
    public void ConsumeInvite_InvalidUserId_BadRequest() {
        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(TestHelper.VERIFICATION_CODE)
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, RandomStringUtils.randomAlphabetic(18), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("user_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void ConsumeInvite_NoUserId_NotFound() {

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(TestHelper.VERIFICATION_CODE)
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, "", secretKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ConsumeInvite_InvalidSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(TestHelper.VERIFICATION_CODE)
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, user.getLeft(), RandomStringUtils.randomAlphabetic(10))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ConsumeInvite_OtherProgrammeSecretKey_Forbidden() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(TestHelper.VERIFICATION_CODE)
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, user.getLeft(), secretKeyAppTwo)
                .then()
                .statusCode(SC_FORBIDDEN);
//        get 500: it's a proxy from multi endpoint. On multi also 500
    }

    @Test
    public void ConsumeInvite_NoSecretKey_BadRequest() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyerAuthorisedUserHelper.sendInvite(user.getLeft(), secretKey, buyer.getRight());

        final ConsumeUserInviteModel consumeUserInviteModel =
                ConsumeUserInviteModel.builder()
                        .inviteCode(TestHelper.VERIFICATION_CODE)
                        .password(new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                        .build();

        BuyersAuthorisedUsersService.consumeUserInvite(consumeUserInviteModel, user.getLeft(), "")
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
