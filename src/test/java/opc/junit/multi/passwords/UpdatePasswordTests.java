package opc.junit.multi.passwords;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.UpdatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.PasswordModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.PasswordsService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.concurrent.TimeUnit;

import static opc.enums.mailhog.MailHogEmail.PASSWORD_CHANGED_ALERT;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdatePasswordTests extends BasePasswordSetup {

    private static CreateCorporateModel createCorporateModel;
    private static CreateConsumerModel createConsumerModel;

    @Test
    public void UpdatePassword_Corporate_Success() throws InterruptedException {
        final Pair<String, String> corporate = createAuthenticatedCorporate();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
                .body("passwordInfo.identityId.id", equalTo(corporate.getLeft()))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());

        assertPasswordUpdatedAlertEmail(createCorporateModel.getRootUser().getEmail(),
                createCorporateModel.getRootUser().getName(), createCorporateModel.getRootUser().getSurname());
    }

    @Test
    public void UpdatePassword_CorporateAuthUser_Success() throws InterruptedException {
        final Pair<String, String> corporate = createAuthenticatedCorporate();

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporate.getRight());

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
                .body("passwordInfo.identityId.id", equalTo(corporate.getLeft()))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());

        assertPasswordUpdatedAlertEmail(usersModel.getEmail(), usersModel.getName(), usersModel.getSurname());
    }

    @Test
    public void UpdatePassword_Consumer_Success() throws InterruptedException {
        final Pair<String, String> consumer = createAuthenticatedConsumer();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(IdentityType.CONSUMER.name()))
                .body("passwordInfo.identityId.id", equalTo(consumer.getLeft()))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());

        assertPasswordUpdatedAlertEmail(createConsumerModel.getRootUser().getEmail(),
                createConsumerModel.getRootUser().getName(), createConsumerModel.getRootUser().getSurname());
    }

    @Test
    public void UpdatePassword_ConsumerAuthUser_Success() throws InterruptedException {
        final Pair<String, String> consumer = createAuthenticatedConsumer();

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumer.getRight());

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.identityId.type", equalTo(IdentityType.CONSUMER.name()))
                .body("passwordInfo.identityId.id", equalTo(consumer.getLeft()))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());

        assertPasswordUpdatedAlertEmail(usersModel.getEmail(), usersModel.getName(),usersModel.getSurname());
    }

    @Test
    public void UpdatePassword_InvalidApiKey_Unauthorised(){
        final Pair<String, String> corporate = createAuthenticatedCorporate();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, "abc", corporate.getRight())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdatePassword_NoApiKey_BadRequest(){
        final Pair<String, String> corporate = createAuthenticatedCorporate();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, "", corporate.getRight())
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void UpdatePassword_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final Pair<String, String> corporate = createAuthenticatedCorporate();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.DEFAULT_PASSWORD),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, corporate.getRight())
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UpdatePassword_RootUserLoggedOut_Unauthorised() {
        final Pair<String, String> corporate = createAuthenticatedCorporate();

        AuthenticationService.logout(secretKey, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, corporate.getRight())
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdatePassword_OldPasswordDoesNotMatch_PasswordIncorrect(){
        final Pair<String, String> corporate = createAuthenticatedCorporate();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(RandomStringUtils.randomAlphanumeric(10)),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_INCORRECT"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void UpdatePassword_OldPasswordNotProvided_BadRequest(final String password){
        final Pair<String, String> corporate = createAuthenticatedCorporate();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(password),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void UpdatePassword_NewPasswordNotProvided_BadRequest(final String password){
        final Pair<String, String> corporate = createAuthenticatedCorporate();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                        new PasswordModel(password));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void UpdatePassword_OldPasswordModelNotProvided_BadRequest(){
        final Pair<String, String> corporate = createAuthenticatedCorporate();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(null,
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void UpdatePassword_NewPasswordModelNotProvided_BadRequest(){
        final Pair<String, String> corporate = createAuthenticatedCorporate();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                        null);

        PasswordsService.updatePassword(updatePasswordModel, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void UpdatePassword_UpdatePasswordAfterTooShortAndTooLongErrors_Success(){
        final Pair<String, String> corporate = createAuthenticatedCorporate();

        final UpdatePasswordModel tooLongPasswordModel =
            new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                new PasswordModel(RandomStringUtils.randomAlphanumeric(51)));

        PasswordsService.updatePassword(tooLongPasswordModel, secretKey, corporate.getRight())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("PASSWORD_TOO_LONG"));

        final UpdatePasswordModel tooShortPasswordModel =
            new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                new PasswordModel(RandomStringUtils.randomAlphanumeric(7)));

        PasswordsService.updatePassword(tooShortPasswordModel, secretKey, corporate.getRight())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));

        final UpdatePasswordModel updatePasswordModel =
            new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));


        PasswordsService.updatePassword(updatePasswordModel, secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .body("passwordInfo.identityId.type", equalTo(IdentityType.CORPORATE.name()))
            .body("passwordInfo.identityId.id", equalTo(corporate.getLeft()))
            .body("passwordInfo.expiryDate", equalTo(0))
            .body("token", notNullValue());
    }

    @Test
    public void UpdatePassword_LongPassword_PasswordTooLong(){
        final Pair<String, String> corporate = createAuthenticatedCorporate();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(51)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_LONG"));
    }

    @Test
    public void UpdatePassword_ShortPassword_PasswordTooShort(){
        final Pair<String, String> corporate = createAuthenticatedCorporate();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(7)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));
    }

    //TODO - Change security model and add more tests
//    @Test
//    public void UpdatePassword_NewPasswordSameAsOld_SameAsOld(){
//        final Pair<String, String> corporate = createAuthenticatedCorporate();
//
//        final UpdatePasswordModel updatePasswordModel =
//                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
//                        new PasswordModel(TestHelper.getDefaultPassword(secretKey)));
//
//        PasswordsService.updatePassword(updatePasswordModel, secretKey, corporate.getRight())
//                .then()
//                .statusCode(SC_CONFLICT)
//                .body("errorCode", equalTo("PASSWORD_SAME_AS_OLD"));
//    }

    @Test
    public void UpdatePassword_BackofficeCorporateImpersonator_Forbidden(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final String corporateId = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey).getLeft();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UpdatePassword_BackofficeConsumerImpersonator_Forbidden(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final String consumerId = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey).getLeft();

        final UpdatePasswordModel updatePasswordModel =
                new UpdatePasswordModel(new PasswordModel(TestHelper.getDefaultPassword(secretKey)),
                        new PasswordModel(RandomStringUtils.randomAlphanumeric(10)));

        PasswordsService.updatePassword(updatePasswordModel, secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private Pair<String, String> createAuthenticatedCorporate(){
        createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        return CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
    }

    private Pair<String, String> createAuthenticatedConsumer(){
        createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        return ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
    }

    private void assertPasswordUpdatedAlertEmail(final String receiverEmail,
                                                 final String receiverName,
                                                 final String receiverSurname) throws InterruptedException {

        TimeUnit.SECONDS.sleep(5);
        final MailHogMessageResponse actualMail = MailhogHelper.getMailHogEmail(receiverEmail);

        assertEquals(PASSWORD_CHANGED_ALERT.getFrom(), actualMail.getFrom());
        assertEquals(receiverEmail, actualMail.getTo());
        assertEquals(PASSWORD_CHANGED_ALERT.getSubject(), actualMail.getSubject());
        assertEquals(String.format(PASSWORD_CHANGED_ALERT.getEmailText(), receiverName, receiverSurname), actualMail.getBody());
    }
}
