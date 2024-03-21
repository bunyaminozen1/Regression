package opc.junit.multi.passwords;

import opc.enums.mailhog.MailHogEmail;
import opc.junit.database.PasswordDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.innovator.DeactivateIdentityModel;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.multi.users.UsersModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.Map;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StartLostPasswordTests extends BasePasswordSetup {

    protected static String corporateId;
    protected static String consumerId;
    protected static String corporateAuthenticationToken;
    protected static String consumerAuthenticationToken;
    protected static CreateCorporateModel corporateDetails;
    protected static CreateConsumerModel consumerDetails;

    @BeforeAll
    public static void TestSetup() {
        corporateSetup();
        consumerSetup();
    }

    @Test
    public void StartLostPassword_Corporate_Success() throws SQLException {

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final Map<Integer, Map<String, String>> passwordNonce = PasswordDatabaseHelper.getPasswordNonce(corporateId);
        assertEquals(1, passwordNonce.size());
        assertEquals(TestHelper.VERIFICATION_CODE, passwordNonce.get(0).get("nonce"));
    }

    @Test
    public void StartLostPassword_Consumer_Success() throws SQLException {

        PasswordsService.startLostPassword(new LostPasswordStartModel(consumerDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final Map<Integer, Map<String, String>> passwordNonce =
                PasswordDatabaseHelper.getPasswordNonce(consumerId);
        assertEquals(1, passwordNonce.size());
        assertEquals(TestHelper.VERIFICATION_CODE, passwordNonce.get(0).get("nonce"));
    }

    @Test
    public void StartLostPassword_CorporateEmailCheck_Success() {

        PasswordsService.startLostPassword(new LostPasswordStartModel(corporateDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(corporateDetails.getRootUser().getEmail());
        assertEquals(MailHogEmail.CORPORATE_PASSWORD_RESET.getFrom(), email.getFrom());
        assertEquals(MailHogEmail.CORPORATE_PASSWORD_RESET.getSubject(), email.getSubject());
        assertEquals(corporateDetails.getRootUser().getEmail(), email.getTo());
        assertTrue(String.format(MailHogEmail.CORPORATE_PASSWORD_RESET.getEmailText(), corporateDetails.getRootUser().getEmail()).equalsIgnoreCase(email.getBody()));
    }

    @Test
    public void StartLostPassword_ConsumerEmailCheck_Success() {

        PasswordsService.startLostPassword(new LostPasswordStartModel(consumerDetails.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(consumerDetails.getRootUser().getEmail());
        assertEquals(MailHogEmail.CONSUMER_PASSWORD_RESET.getFrom(), email.getFrom());
        assertEquals(MailHogEmail.CONSUMER_PASSWORD_RESET.getSubject(), email.getSubject());
        assertEquals(consumerDetails.getRootUser().getEmail(), email.getTo());
        assertTrue(String.format(MailHogEmail.CONSUMER_PASSWORD_RESET.getEmailText(), consumerDetails.getRootUser().getEmail()).equalsIgnoreCase(email.getBody()));
    }

    @Test
    public void StartLostPassword_AuthenticatedUserEmailCheck_Success() {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        PasswordsService.startLostPassword(new LostPasswordStartModel(usersModel.getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final MailHogMessageResponse email = MailhogHelper.getMailHogEmail(usersModel.getEmail());
        assertEquals(MailHogEmail.CORPORATE_PASSWORD_RESET.getFrom(), email.getFrom());
        assertEquals(MailHogEmail.CORPORATE_PASSWORD_RESET.getSubject(), email.getSubject());
        assertEquals(usersModel.getEmail(), email.getTo());
        assertTrue(String.format(MailHogEmail.CORPORATE_PASSWORD_RESET.getEmailText(), usersModel.getEmail()).equalsIgnoreCase(email.getBody()));
    }

    @Test
    public void StartLostPassword_CorporateUser_Success() throws SQLException {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        PasswordsService.startLostPassword(new LostPasswordStartModel(usersModel.getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final Map<Integer, Map<String, String>> passwordNonce = PasswordDatabaseHelper.getPasswordNonce(user.getLeft());
        assertEquals(1, passwordNonce.size());
        assertEquals(TestHelper.VERIFICATION_CODE, passwordNonce.get(0).get("nonce"));
    }

    @Test
    public void StartLostPassword_CorporateInactive_AccountNotFound() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final String corporateId = CorporatesHelper.createAuthenticatedVerifiedCorporate(createCorporateModel, secretKey).getLeft();

        InnovatorHelper.deactivateCorporate(new DeactivateIdentityModel(false, "ACCOUNT_REVIEW"),
                corporateId, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        PasswordsService.startLostPassword(new LostPasswordStartModel(createCorporateModel.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    public void StartLostPassword_ConsumerInactive_AccountNotFound() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final String consumerId = ConsumersHelper.createAuthenticatedVerifiedConsumer(createConsumerModel, secretKey).getLeft();

        InnovatorHelper.deactivateConsumer(new DeactivateIdentityModel(false, "ACCOUNT_REVIEW"),
                consumerId, InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        PasswordsService.startLostPassword(new LostPasswordStartModel(createConsumerModel.getRootUser().getEmail()), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    public void StartLostPassword_CorporateUserInactive_AccountNotFound() {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporateAuthenticationToken);

        InnovatorHelper.deactivateCorporateUser(corporateId, user.getLeft(),
                InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        PasswordsService.startLostPassword(new LostPasswordStartModel(usersModel.getEmail()), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    public void StartLostPassword_ConsumerUserInactive_AccountNotFound() {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        final Pair<String, String> user =
                UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumerAuthenticationToken);

        InnovatorHelper.deactivateConsumerUser(consumerId, user.getLeft(),
                InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword));

        PasswordsService.startLostPassword(new LostPasswordStartModel(usersModel.getEmail()), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    public void StartLostPassword_InvalidApiKey_Unauthorised(){

        PasswordsService.startLostPassword(new LostPasswordStartModel(consumerDetails.getRootUser().getEmail()), "abc")
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartLostPassword_NoApiKey_BadRequest(){

        PasswordsService.startLostPassword(new LostPasswordStartModel(consumerDetails.getRootUser().getEmail()), "")
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void StartLostPassword_DifferentInnovatorApiKey_AccountNotFound(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        PasswordsService.startLostPassword(new LostPasswordStartModel(consumerDetails.getRootUser().getEmail()), secretKey)
                .then().statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ACCOUNT_NOT_FOUND"));
    }

    @Test
    public void StartLostPassword_UnknownEmail_Conflict(){
        PasswordsService.startLostPassword(
                new LostPasswordStartModel(String.format("%s@weavrunknown.io", RandomStringUtils.randomAlphanumeric(10))), secretKey)
                .then()
                .statusCode(SC_CONFLICT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc"})
    public void StartLostPassword_InvalidEmail_BadRequest(final String email){
        PasswordsService.startLostPassword(
                new LostPasswordStartModel(email), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void StartLostPassword_InvalidEmailFormat_BadRequest(){
        PasswordsService.startLostPassword(
                new LostPasswordStartModel(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6))), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    private static void consumerSetup() {
        consumerDetails =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(consumerDetails, secretKey);
        final String consumerUserId = authenticatedConsumer.getLeft();
        consumerId = authenticatedConsumer.getLeft();
        consumerAuthenticationToken = authenticatedConsumer.getRight();

        ConsumersHelper.verifyKyc(secretKey, consumerUserId);
    }

    private static void corporateSetup() {
        corporateDetails =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(corporateDetails, secretKey);
        final String corporateUserId = authenticatedCorporate.getLeft();
        corporateId = authenticatedCorporate.getLeft();
        corporateAuthenticationToken = authenticatedCorporate.getRight();

        CorporatesHelper.verifyKyb(secretKey, corporateUserId);
    }
}
