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
public class DeactivateAuthorisedUserTests extends BasePaymentRunSetup {

    private static Pair<String, String> buyer;

    @BeforeAll
    public static void TestSetup() {
        buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
    }

    @Test
    public void DeactivateUser_RootUserToken_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.deactivateUser(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(false));
    }

    @Test
    public void DeactivateUser_MultipleRolesRootUserToken_Success() {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);

        final Pair<String, BuyerAuthorisedUserModel> user = BuyerAuthorisedUserHelper.createUser(secretKey, buyer.getRight());

        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        BuyersAuthorisedUsersService.deactivateUser(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(false));
    }

    @Test
    public void DeactivateUser_AuthorisedUserToken_Forbidden() {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        BuyersAuthorisedUsersService.deactivateUser(user.getLeft(), secretKey, user.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);

        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true));
    }

    @Test
    public void DeactivateUser_RootUser_Forbidden() {
        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        BuyersAuthorisedUsersService.deactivateUser(buyer.getLeft(), secretKey, user.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void DeactivateUser_AlreadyInactive_Success() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.deactivateUser(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersAuthorisedUsersService.deactivateUser(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void DeactivateUser_IdentityImpersonator_Unauthorised() {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        BuyersAuthorisedUsersService.deactivateUser(user.getLeft(), secretKey, getBackofficeImpersonateToken(buyer.getLeft(), IdentityType.CORPORATE))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void DeactivateUser_UnknownUserId_NotFound() {
        BuyersAuthorisedUsersService.deactivateUser(RandomStringUtils.randomNumeric(18),
                        secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DeactivateUser_InvalidUserId_BadRequest() {

        BuyersAuthorisedUsersService.deactivateUser(RandomStringUtils.randomAlphabetic(18),
                        secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("user_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void DeactivateUser_NoUserId_NotFound() {

        BuyersAuthorisedUsersService.deactivateUser("",
                        secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void DeactivateUser_OtherBuyerRootToken_Forbidden() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        final String otherBuyerToken =
                BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        BuyersAuthorisedUsersService.deactivateUser(user.getLeft(),
                        secretKey, otherBuyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void DeactivateUser_OtherBuyerAuthorisedUserToken_Forbidden() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        final String otherBuyerToken =
                BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String otherUserToken =
                BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, otherBuyerToken).getRight();

        BuyersAuthorisedUsersService.deactivateUser(user.getLeft(),
                        secretKey, otherUserToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void DeactivateUser_InvalidToken_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.deactivateUser(user.getLeft(),
                        secretKey, RandomStringUtils.randomAlphabetic(10))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void DeactivateUser_NoToken_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.deactivateUser(user.getLeft(),
                        secretKey, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void DeactivateUser_InvalidSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.deactivateUser(user.getLeft(),
                        RandomStringUtils.randomAlphabetic(10), buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void DeactivateUser_OtherProgrammeSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.deactivateUser(user.getLeft(),
                        secretKeyAppTwo, buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void DeactivateUser_NoSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.deactivateUser(user.getLeft(),
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
}