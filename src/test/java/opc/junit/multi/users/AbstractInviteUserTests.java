package opc.junit.multi.users;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.users.ConsumerUserInviteModel;
import opc.models.multi.users.UsersModel;
import opc.models.multi.users.ValidateUserInviteModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.AuthenticationService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import static opc.enums.opc.IdentityType.CORPORATE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public abstract class AbstractInviteUserTests extends AbstractUserTests {

    @Test
    public void SendInvite_RootUser_Success() {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendInvite_NonRootUser_Success() {

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(UsersModel.DefaultUsersModel().build(), secretKey, getAuthToken());

        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendInvite_MultipleInvites() {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_NO_CONTENT);
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void SendInvite_EmailUpdatedBeforeSendingInvite_Success() throws SQLException {
        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new UsersModel.Builder()
                .setEmail(RandomStringUtils.randomAlphanumeric(10) + "@weavr.io")
                .build();

        final Map<Integer, Map<String, String>> preUpdateNonce = getEmailNonce(newUser.id, newUser.identityType);
        assertEquals(1, preUpdateNonce.size());
        assertEquals(newUser.userDetails.getEmail(), preUpdateNonce.get(0).get("email_used"));
        assertNull(preUpdateNonce.get(0).get("sent_at"));

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(newUser.id))
                .body("identity.id", equalTo(newUser.identityId))
                .body("identity.type", equalTo(newUser.identityType.name()))
                .body("name", equalTo(newUser.userDetails.getName()))
                .body("surname", equalTo(newUser.userDetails.getSurname()))
                .body("email", equalTo(patchUserDetails.getEmail()))
                .body("active", equalTo(true));

        final Map<Integer, Map<String, String>> newEmailNonce = getEmailNonce(newUser.id, newUser.identityType);
        assertEquals(1, newEmailNonce.size());
        assertEquals(patchUserDetails.getEmail(), newEmailNonce.get(0).get("email_used"));
        assertNull(newEmailNonce.get(0).get("sent_at"));

        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertEquals(0, MailhogHelper.getMailhogEmailCount(newUser.userDetails.getEmail()));
        assertEquals(1, MailhogHelper.getMailhogEmailCount(patchUserDetails.getEmail()));
    }

    @Test
    public void SendInvite_EmailUpdatedBeforeConsuming_ResendNewEmailSuccess() throws SQLException {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        final Map<Integer, Map<String, String>> preUpdateNonce = getEmailNonce(newUser.id, newUser.identityType);
        assertEquals(1, preUpdateNonce.size());
        assertEquals(newUser.userDetails.getEmail(), preUpdateNonce.get(0).get("email_used"));
        assertNotNull(preUpdateNonce.get(0).get("sent_at"));

        assertEquals(1, MailhogHelper.getMailhogEmailCount(newUser.userDetails.getEmail()));

        final UsersModel usersModel =
                UsersModel.builder().setEmail(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10))).build();
        UsersService.patchUser(usersModel, secretKey, newUser.id, getAuthToken(), Optional.empty()).then().statusCode(SC_OK);

        final Map<Integer, Map<String, String>> newEmailNonce = getEmailNonce(newUser.id, newUser.identityType);
        assertEquals(1, newEmailNonce.size());
        assertEquals(usersModel.getEmail(), newEmailNonce.get(0).get("email_used"));
        assertNotNull(newEmailNonce.get(0).get("sent_at"));

        assertEquals(1, MailhogHelper.getMailhogEmailCount(usersModel.getEmail()));
    }

    @Test
    public void SendInvite_EmailUpdatedAfterConsuming_ResendNewEmailSuccess() throws SQLException {
        final User newUser = createNewUser();

        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(secretKey))),
                getSecretKey(),
                newUser.id)
                .then()
                .statusCode(SC_OK)
                .body("token", not(emptyString()));

        final Map<Integer, Map<String, String>> preUpdateNonce = getEmailNonce(newUser.id, newUser.identityType);
        assertEquals(0, preUpdateNonce.size());

        assertEquals(1, MailhogHelper.getMailhogEmailCount(newUser.userDetails.getEmail()));

        final UsersModel usersModel =
                UsersModel.builder().setEmail(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10))).build();
        UsersService.patchUser(usersModel, secretKey, newUser.id, getAuthToken(), Optional.empty()).then().statusCode(SC_OK);

        final Map<Integer, Map<String, String>> postUpdateNonce = getEmailNonce(newUser.id, newUser.identityType);
        assertEquals(0, postUpdateNonce.size());

        assertEquals(0, MailhogHelper.getMailhogEmailCount(usersModel.getEmail()));
    }

    @Test
    public void SendInvite_RootUser_NotFound() {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.identityId, getAuthToken())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendInvite_UnknownUser_NotFound() {
        UsersService.inviteUser(getSecretKey(), RandomStringUtils.randomNumeric(18), getAuthToken())
                    .then()
                    .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendInvite_InviteAlreadyConsumed_Conflict() {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_NO_CONTENT);
        
        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(secretKey))),
                                       getSecretKey(),
                                       newUser.id)
                    .then()
                    .statusCode(SC_OK);

        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVITE_ALREADY_CONSUMED"));
    }

    @Test
    public void SendInvite_IdentityImpersonator_Forbidden() {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getBackofficeImpersonateToken(newUser.identityId, newUser.identityType))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendInvite_CrossIdentityType_NotFound() {
        final User newUser = createNewUser();
        final String newIdentityToken;

        if (newUser.identityType.equals(CORPORATE)) {
            newIdentityToken = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey).getRight();
        } else {
            newIdentityToken = CorporatesHelper.createAuthenticatedVerifiedCorporate(corporateProfileId, secretKey).getRight();
        }

        UsersService.inviteUser(getSecretKey(), newUser.id, newIdentityToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void SendInvite_CrossIdentity_Forbidden() {
        final User newUser = createNewUser();
        final String newIdentityToken;

        if (newUser.identityType.equals(IdentityType.CONSUMER)) {
            newIdentityToken = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey).getRight();
        } else {
            newIdentityToken = CorporatesHelper.createAuthenticatedVerifiedCorporate(corporateProfileId, secretKey).getRight();
        }

        UsersService.inviteUser(getSecretKey(), newUser.id, newIdentityToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendInvite_OtherProgrammeSecretKey_Forbidden() {
        final User newUser = createNewUser();

        UsersService.inviteUser(secretKeyAppTwo, newUser.id, getAuthToken())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendInvite_NoSecretKey_BadRequest() {
        final User newUser = createNewUser();

        UsersService.inviteUser("", newUser.id, getAuthToken())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendInvite_InvalidSecretKey_Unauthorised() {
        final User newUser = createNewUser();

        UsersService.inviteUser("abc", newUser.id, getAuthToken())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendInvite_NoToken_Unauthorised() {
        final User newUser = createNewUser();

        UsersService.inviteUser(getSecretKey(), newUser.id, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendInvite_InvalidToken_Unauthorised() {
        final User newUser = createNewUser();

        UsersService.inviteUser(getSecretKey(), newUser.id, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendInvite_InvalidUserId_NotFound() {
        UsersService.inviteUser(getSecretKey(), RandomStringUtils.randomNumeric(8), getAuthToken())
                .then()
                .statusCode(SC_NOT_FOUND);
    }
    @Test
    public void SendInvite_NoUserId_MethodNotAllowed() {
        UsersService.inviteUser(getSecretKey(), "", getAuthToken())
                .then()
                .statusCode(SC_METHOD_NOT_ALLOWED);
    }

    @Test
    public void ValidateInvite_Success() {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_NO_CONTENT);
        
        UsersService.validateUserInvite(new ValidateUserInviteModel(TestHelper.VERIFICATION_CODE), getSecretKey(), newUser.id)
                    .then()
                    .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ValidateInvite_RootUser_InviteOrInviteCodeInvalid() {
        final User newUser = createNewUser();

        UsersService.validateUserInvite(new ValidateUserInviteModel(TestHelper.VERIFICATION_CODE), getSecretKey(), newUser.identityId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVITE_OR_INVITE_CODE_INVALID"));
    }

    @Test
    @Disabled
//    TODO: what expired time?
    public void ValidateInvite_Expired() {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_NO_CONTENT);
        
        UsersService.validateUserInvite(new ValidateUserInviteModel(TestHelper.VERIFICATION_CODE), getSecretKey(), newUser.id)
                    .then()
                    .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void ValidateInvite_UnknownVerificationToken_InviteOrInviteCodeInvalid() {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_NO_CONTENT);
        UsersService.validateUserInvite(new ValidateUserInviteModel(RandomStringUtils.randomNumeric(6)), getSecretKey(), newUser.id)
                    .then()
                    .statusCode(SC_CONFLICT)
                    .body("errorCode", equalTo("INVITE_OR_INVITE_CODE_INVALID"));
    }

    @Test
    public void ValidateInvite_InvalidVerificationToken_BadRequest() {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);
        UsersService.validateUserInvite(new ValidateUserInviteModel(RandomStringUtils.randomAlphabetic(6)), getSecretKey(), newUser.id)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.inviteCode: must match \"^[0-9]*$\""));
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 7})
    public void ValidateInvite_InvalidInviteCodeSize_BadRequest(final int inviteCodeSize) {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);
        UsersService.validateUserInvite(new ValidateUserInviteModel(RandomStringUtils.randomNumeric(inviteCodeSize)), getSecretKey(), newUser.id)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.inviteCode: size must be between 6 and 6"));
    }

    @Test
    public void ValidateInvite_AlreadyConsumed_Conflict() {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_NO_CONTENT);
        
        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(secretKey))),
                                       getSecretKey(),
                                       newUser.id)
                    .then()
                    .statusCode(SC_OK);
        UsersService.validateUserInvite(new ValidateUserInviteModel(TestHelper.VERIFICATION_CODE), getSecretKey(), newUser.id)
                    .then()
                    .statusCode(SC_CONFLICT)
                    .body("errorCode", equalTo("INVITE_OR_INVITE_CODE_INVALID"));
    }

    @Test
    @DisplayName("ValidateInvite_OtherProgrammeSecretKey_Forbidden - DEV-6370 opened to return 403")
    public void ValidateInvite_OtherProgrammeSecretKey_Forbidden() {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                .then()
                .statusCode(SC_NO_CONTENT);

        UsersService.validateUserInvite(new ValidateUserInviteModel(TestHelper.VERIFICATION_CODE), secretKeyAppTwo, newUser.id)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    @DisplayName("ValidateInvite_UnknownUserId_NotFound - DEV-6370 opened to return 404")
    public void ValidateInvite_UnknownUserId_NotFound() {
        UsersService.validateUserInvite(new ValidateUserInviteModel(TestHelper.VERIFICATION_CODE), getSecretKey(), RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_CONFLICT);
    }

    @Test
    public void ValidateInvite_NoUserId_NotFound() {
        UsersService.validateUserInvite(new ValidateUserInviteModel(TestHelper.VERIFICATION_CODE), getSecretKey(), "")
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void ValidateInvite_NoSecretKey_BadRequest() {
        final User newUser = createNewUser();

        UsersService.validateUserInvite(new ValidateUserInviteModel(TestHelper.VERIFICATION_CODE), "", newUser.id)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ValidateInvite_InvalidSecretKey_Unauthorised() {
        final User newUser = createNewUser();

        UsersService.validateUserInvite(new ValidateUserInviteModel(TestHelper.VERIFICATION_CODE), "abc", newUser.id)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ValidateInvite_NoVerificationToken_BadRequest() {
        final User newUser = createNewUser();

        UsersService.validateUserInvite(new ValidateUserInviteModel(""), getSecretKey(), newUser.id)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ConsumeInvite_Success() {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_NO_CONTENT);
        
        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(secretKey))),
                                       getSecretKey(),
                                       newUser.id)
                    .then()
                    .statusCode(SC_OK)
                    .body("token", not(emptyString()));

        AuthenticationService.loginWithPassword(new LoginModel(newUser.userDetails.getEmail(), new PasswordModel(TestHelper.getDefaultPassword(secretKey))),getSecretKey())
                .then()
                .statusCode(SC_OK)
                .body("token", not(emptyString()));

    }

    @Test
    public void ConsumeInvite_AlreadyConsumed_PasswordAlreadyCreated() {
        final User newUser = createNewUser();
        UsersService.inviteUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_NO_CONTENT);
        
        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(secretKey))),
                                       getSecretKey(),
                                       newUser.id)
                    .then()
                    .statusCode(SC_OK)
                    .body("token", not(emptyString()));

        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(secretKey))), getSecretKey(), newUser.id)
                    .then()
                    .statusCode(SC_CONFLICT)
                    .body("errorCode", equalTo("PASSWORD_ALREADY_CREATED"));
    }

    @Test
    public void ConsumeInvite_RootUser_InviteOrInviteCodeInvalid() {

        final User newUser = createNewUser();

        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(secretKey))),
                getSecretKey(),
                newUser.identityId)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVITE_OR_INVITE_CODE_INVALID"));
    }

    @Test
    public void ConsumeInvite_InvalidCode_InviteOrInviteCodeInvalid() {

        final User newUser = createNewUser();

        UsersService.consumeUserInvite(new ConsumerUserInviteModel(RandomStringUtils.randomNumeric(6), new PasswordModel(TestHelper.getDefaultPassword(secretKey))),
                getSecretKey(),
                newUser.id)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("INVITE_OR_INVITE_CODE_INVALID"));
    }

    @Test
    public void ConsumeInvite_InvalidInviteCode_BadRequest() {
        UsersService.consumeUserInvite(new ConsumerUserInviteModel(RandomStringUtils.randomAlphabetic(6), new PasswordModel(TestHelper.getDefaultPassword(secretKey))),
                                       getSecretKey(),
                                      RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.inviteCode: must match \"^[0-9]*$\""));
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 7})
    public void ConsumeInvite_InvalidInviteCodeSize_BadRequest(final int inviteCodeSize) {
        UsersService.consumeUserInvite(new ConsumerUserInviteModel(RandomStringUtils.randomNumeric(inviteCodeSize), new PasswordModel(TestHelper.getDefaultPassword(secretKey))),
                        getSecretKey(),
                        RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.inviteCode: size must be between 6 and 6"));
    }

    @Test
    public void ConsumeInvite_UnknownUser_UnresolvedIdentity() {
        UsersService.consumeUserInvite(new ConsumerUserInviteModel(RandomStringUtils.randomNumeric(6), new PasswordModel(TestHelper.getDefaultPassword(secretKey))),
                        getSecretKey(),
                        RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("UNRESOLVED_IDENTITY"));
    }

    @Test
    public void ConsumeInvite_InvalidUserId_BadRequest() {
        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(secretKey))),
                        getSecretKey(),
                        RandomStringUtils.randomAlphabetic(18))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("userId: must match \"^[0-9]+$\""));
    }

    @Test
    public void ConsumeInvite_NoPassword_BadRequest() {

        final User newUser = createNewUser();

        final ConsumerUserInviteModel consumerUserInviteModel =
                new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(secretKey)))
                .setPassword(null);

        UsersService.consumeUserInvite(consumerUserInviteModel,
                getSecretKey(),
                newUser.identityId)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void ConsumeInvite_InvalidPassword_BadRequest(final String password) {

        final User newUser = createNewUser();

        final ConsumerUserInviteModel consumerUserInviteModel =
                new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(password));

        UsersService.consumeUserInvite(consumerUserInviteModel,
                getSecretKey(),
                newUser.identityId)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ConsumeInvite_OtherProgrammeSecretKey_Forbidden() {
        final User user = createNewUser();
        UsersHelper.inviteUser(getSecretKey(), user.id, getAuthToken());

        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(getSecretKey()))),
                        secretKeyAppTwo,
                        user.id)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void ConsumeInvite_NoSecretKey_BadRequest() {
        final User user = createNewUser();
        UsersHelper.inviteUser(getSecretKey(), user.id, getAuthToken());

        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(getSecretKey()))),
                        "",
                        user.id)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void ConsumeInvite_InvalidSecretKey_Unauthorised() {
        final User user = createNewUser();
        UsersHelper.inviteUser(getSecretKey(), user.id, getAuthToken());

        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(getSecretKey()))),
                        "abc",
                        user.id)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void ConsumeInvite_NoUserId_NotFound() {
        UsersService.consumeUserInvite(new ConsumerUserInviteModel(TestHelper.VERIFICATION_CODE, new PasswordModel(TestHelper.getDefaultPassword(getSecretKey()))),
                        getSecretKey(),
                        "")
                .then()
                .statusCode(SC_NOT_FOUND);
    }
}
