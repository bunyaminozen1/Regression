package fpi.paymentrun.identities.buyerauthorisedusers;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.services.BuyersAuthorisedUsersService;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static opc.enums.opc.IdentityType.CORPORATE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag(PluginsTags.PAYMENT_RUN_AUTH_USERS)
public class SendAuthorisedUserInviteTests extends BasePaymentRunSetup {

    private static Pair<String, String> buyer;

    @BeforeAll
    public static void TestSetup() {
        buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
    }

    @Test
    public void SendInvite_AdminRoleRootUser_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        
        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendInvite_MultipleRolesRootUser_Success() {

        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);

        final Pair<String, BuyerAuthorisedUserModel> user = BuyerAuthorisedUserHelper.createUser(secretKey, buyer.getRight());

        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendInvite_AuthorisedUser_Forbidden() {

        final String authenticatedUser = createAuthenticatedUser().getRight();

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey, authenticatedUser)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendInvite_MultipleInvites_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendInvite_EmailUpdatedBeforeSendingInvite_Success() throws SQLException {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .email(RandomStringUtils.randomAlphanumeric(10) + "@weavr.io")
                .build();

        final Map<Integer, Map<String, String>> preUpdateNonce = getEmailNonce(user.getLeft());
        assertEquals(1, preUpdateNonce.size());
        assertEquals(user.getRight().getEmail(), preUpdateNonce.get(0).get("email_used"));
        assertNull(preUpdateNonce.get(0).get("sent_at"));

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("email", equalTo(updateUserModel.getEmail()));

        final Map<Integer, Map<String, String>> newEmailNonce = getEmailNonce(user.getLeft());
        assertEquals(1, newEmailNonce.size());
        assertEquals(updateUserModel.getEmail(), newEmailNonce.get(0).get("email_used"));
        assertNull(newEmailNonce.get(0).get("sent_at"));

        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertEquals(0, MailhogHelper.getMailhogEmailCount(user.getRight().getEmail()));
        assertEquals(1, MailhogHelper.getMailhogEmailCount(updateUserModel.getEmail()));
    }

    @Test
    public void SendInvite_EmailUpdatedBeforeConsuming_ResendNewEmailSuccess() throws SQLException {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        final Map<Integer, Map<String, String>> preUpdateNonce = getEmailNonce(user.getLeft());
        assertEquals(1, preUpdateNonce.size());
        assertEquals(user.getRight().getEmail(), preUpdateNonce.get(0).get("email_used"));
        assertNotNull(preUpdateNonce.get(0).get("sent_at"));

        assertEquals(1, MailhogHelper.getMailhogEmailCount(user.getRight().getEmail()));

        final BuyerAuthorisedUserModel updateUserModel =
                BuyerAuthorisedUserModel.builder().email(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10))).build();
        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight()).then().statusCode(SC_OK);

        final Map<Integer, Map<String, String>> newEmailNonce = getEmailNonce(user.getLeft());
        assertEquals(1, newEmailNonce.size());
        assertEquals(updateUserModel.getEmail(), newEmailNonce.get(0).get("email_used"));
        assertNotNull(newEmailNonce.get(0).get("sent_at"));

        assertEquals(1, MailhogHelper.getMailhogEmailCount(updateUserModel.getEmail()));
    }

    @Test
    public void SendInvite_EmailUpdatedAfterConsuming_ResendNewEmailSuccess() throws SQLException {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        final Map<Integer, Map<String, String>> preUpdateNonce = getEmailNonce(user.getLeft());
        assertEquals(0, preUpdateNonce.size());

        assertEquals(1, MailhogHelper.getMailhogEmailCount(user.getMiddle().getEmail()));

        final BuyerAuthorisedUserModel updateUserModel =
                BuyerAuthorisedUserModel.builder().email(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10))).build();
        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight()).then().statusCode(SC_OK);

        final Map<Integer, Map<String, String>> postUpdateNonce = getEmailNonce(user.getLeft());
        assertEquals(0, postUpdateNonce.size());

        assertEquals(0, MailhogHelper.getMailhogEmailCount(updateUserModel.getEmail()));
    }

    @Test
    public void SendInvite_RootUserId_NotFound() {

        BuyersAuthorisedUsersService.sendUserInvite(buyer.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendInvite_AlreadyConsumed_InviteAlreadyConsumed() {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVITE_ALREADY_CONSUMED"));
    }

    @Test
    public void SendInvite_IdentityImpersonator_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey, getBackofficeImpersonateToken(buyer.getLeft(), CORPORATE))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendInvite_UnknownUserId_NotFound() {
        BuyersAuthorisedUsersService.sendUserInvite(RandomStringUtils.randomNumeric(18), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendInvite_NoUserId_NotFound() {
        BuyersAuthorisedUsersService.sendUserInvite("", secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendInvite_InvalidUserId_BadRequest() {
        BuyersAuthorisedUsersService.sendUserInvite(RandomStringUtils.randomAlphabetic(18), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("user_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void SendInvite_OtherBuyerToken_Forbidden() {

        final String otherBuyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey, otherBuyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendInvite_OtherBuyerAuthorisedUserToken_Forbidden() {

        final String otherBuyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String userToken = BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, otherBuyerToken).getRight();

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey, userToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendInvite_InvalidToken_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey,RandomStringUtils.randomAlphabetic(10))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendInvite_NoToken_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKey, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendInvite_InvalidSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), RandomStringUtils.randomAlphabetic(10), buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendInvite_OtherProgrammeSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), secretKeyAppTwo, buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendInvite_NoSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.sendUserInvite(user.getLeft(), "", buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private Pair<String, BuyerAuthorisedUserModel> createUser() {
        return BuyerAuthorisedUserHelper.createUser(secretKey, buyer.getRight());
    }

    private Triple<String, BuyerAuthorisedUserModel, String> createAuthenticatedUser() {
        return BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyer.getRight());
    }

    private Map<Integer, Map<String, String>> getEmailNonce(final String userId) throws SQLException {
        return CorporatesDatabaseHelper.getEmailNonce(userId);
    }
}
