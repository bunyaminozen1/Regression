package fpi.paymentrun.identities.buyerauthorisedusers;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.services.BuyersAuthorisedUsersService;
import opc.enums.opc.IdentityType;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_AUTH_USERS)
public class ActivateAuthorisedUserTests extends BasePaymentRunSetup {

    private static Pair<String, String> buyer;

    @BeforeAll
    public static void TestSetup() {
        buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
    }

    @Test
    public void ActivateUser_AdminRoleBuyerToken_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyerAuthorisedUserHelper.deactivateUser(user.getLeft(), secretKey, buyer.getRight());
        assertUserState(user.getLeft(), false);

        BuyersAuthorisedUsersService.activateUser(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertUserState(user.getLeft(), true);
    }

    @Test
    public void ActivateUser_MultipleRolesBuyerToken_Success() {
        final String otherBuyerToken =
                BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();

        final Pair<String, BuyerAuthorisedUserModel> user = BuyerAuthorisedUserHelper.createUser(secretKey, otherBuyerToken);

        BuyerAuthorisedUserHelper.deactivateUser(user.getLeft(), secretKey, otherBuyerToken);
        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKey, otherBuyerToken)
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(false));

        BuyersHelper.assignAllRoles(secretKey, otherBuyerToken);

        BuyersAuthorisedUsersService.activateUser(user.getLeft(), secretKey, otherBuyerToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKey, otherBuyerToken)
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true));
    }

    @Test
    public void ActivateUser_DeactivatedUserToken_Unauthorised() {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        BuyerAuthorisedUserHelper.deactivateUser(user.getLeft(), secretKey, buyer.getRight());
        assertUserState(user.getLeft(), false);

        BuyersAuthorisedUsersService.activateUser(user.getLeft(), secretKey, user.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ActivateUser_AuthUserToken_Forbidden() {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        BuyersAuthorisedUsersService.activateUser(buyer.getLeft(), secretKey, user.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ActivateUser_AlreadyActive_Success() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.activateUser(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertUserState(user.getLeft(), true);
    }

    @Test
    public void ActivateUser_IdentityImpersonator_Unauthorised() {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        BuyersAuthorisedUsersService.activateUser(user.getLeft(), secretKey, getBackofficeImpersonateToken(buyer.getLeft(), IdentityType.CORPORATE))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ActivateUser_UnknownUserId_NotFound() {
        BuyersAuthorisedUsersService.activateUser(RandomStringUtils.randomNumeric(18),
                        secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ActivateUser_InvalidUserId_BadRequest() {

        BuyersAuthorisedUsersService.activateUser(RandomStringUtils.randomAlphabetic(18),
                        secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("user_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void ActivateUser_NoUserId_NotFound() {

        BuyersAuthorisedUsersService.activateUser("",
                        secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ActivateUser_OtherBuyerRootToken_Forbidden() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final String otherBuyerToken =
                BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        BuyersAuthorisedUsersService.activateUser(user.getLeft(), secretKey, otherBuyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ActivateUser_OtherBuyerAuthorisedUserToken_Forbidden() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final String otherBuyerToken =
                BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String otherUserToken =
                BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, otherBuyerToken).getRight();

        BuyersAuthorisedUsersService.activateUser(user.getLeft(), secretKey, otherUserToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ActivateUser_InvalidToken_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.activateUser(user.getLeft(),
                        secretKey, RandomStringUtils.randomAlphabetic(10))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ActivateUser_NoToken_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.activateUser(user.getLeft(),
                        secretKey, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ActivateUser_InvalidSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.activateUser(user.getLeft(),
                        RandomStringUtils.randomAlphabetic(10), buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ActivateUser_OtherProgrammeSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.activateUser(user.getLeft(),
                        secretKeyAppTwo, buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ActivateUser_NoSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.activateUser(user.getLeft(),
                        "", buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private Pair<String, BuyerAuthorisedUserModel> createUser() {
        return BuyerAuthorisedUserHelper.createUser(secretKey, buyer.getRight());
    }

    private Triple<String, BuyerAuthorisedUserModel, String> createAuthenticatedUser() {
        return BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyer.getRight());
    }

    private void assertUserState(final String userId,
                                 final boolean isActive) {
        BuyersAuthorisedUsersService.getUser(userId, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(isActive));
    }
}