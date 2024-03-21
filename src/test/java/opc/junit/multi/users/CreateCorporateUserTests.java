package opc.junit.multi.users;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.GetUsersFiltersModel;
import opc.models.admin.ContextPropertiesV2Model;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.services.adminnew.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.PasswordsService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static opc.enums.mailhog.MailHogEmail.NEW_USER_CREATED_ALERT;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class CreateCorporateUserTests extends AbstractCreateUserTests {

    private String corporateId;
    private String authenticationToken;
    private String corporateEmail;
    private CreateCorporateModel createCorporateModel;

    @BeforeAll
    public static void enableAlert() {
        setAlertNewUserAddedProperty(true);
    }

    @BeforeEach
    public void BeforeEach() {
        createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> authenticatedConsumer = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        corporateId = authenticatedConsumer.getLeft();
        authenticationToken = authenticatedConsumer.getRight();
        corporateEmail = createCorporateModel.getRootUser().getEmail();
    }

    @Test
    public void PostUsers_WithTagField_Success() {
        final UsersModel user = UsersModel.DefaultUsersModel().setTag(RandomStringUtils.randomAlphanumeric(5)).build();
        String userId = UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", not(nullValue()))
                .body("name", equalTo(user.getName()))
                .body("surname", equalTo(user.getSurname()))
                .body("email", equalTo(user.getEmail()))
                .body("tag", equalTo(user.getTag()))
                .extract().jsonPath().getString("id");

        final GetUsersFiltersModel filtersModel = GetUsersFiltersModel.builder().tag(user.getTag()).build();
        AdminService.getCorporateAllUsers(AdminService.loginAdmin(), getIdentityId(), Optional.of(filtersModel))
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", equalTo(userId))
                .body("user[0].name", equalTo(user.getName()))
                .body("user[0].surname", equalTo(user.getSurname()))
                .body("user[0].email", equalTo(user.getEmail()))
                .body("user[0].tag", equalTo(user.getTag()));

        InnovatorService.getCorporateAllUsers(InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), getIdentityId(), Optional.of(filtersModel))
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", equalTo(userId))
                .body("user[0].name", equalTo(user.getName()))
                .body("user[0].surname", equalTo(user.getSurname()))
                .body("user[0].email", equalTo(user.getEmail()))
                .body("user[0].tag", equalTo(user.getTag()));
    }

    @Test
    public void PostUsers_AlertRootUserAndFirstCreatedEmailVerifiedAuthorizedUser_Success() {

        //Create first email verified Auth User, this will be alerted when a new user is created
        final UsersModel firstUserModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> authenticatedUser = UsersHelper.createAuthenticatedUser(firstUserModel, getSecretKey(), getAuthToken());

        //Check root user is alerted because of new auth user creation
        assertNewUserCreatedAlertEmail(getRootEmail(), createCorporateModel.getRootUser().getName(), firstUserModel.getName(),
                firstUserModel.getSurname(), firstUserModel.getEmail(), getRootEmail());

        //Create second email verified Auth User, this will NOT be alerted when a new user is created, it will be checked line 111
        final UsersModel secondUserModel = UsersModel.DefaultUsersModel().build();
        UsersHelper.createAuthenticatedUser(secondUserModel, getSecretKey(), authenticatedUser.getRight());

        //Check root user is alerted because of new auth user creation
        assertNewUserCreatedAlertEmail(getRootEmail(), createCorporateModel.getRootUser().getName(), secondUserModel.getName(),
                secondUserModel.getSurname(), secondUserModel.getEmail(), firstUserModel.getEmail());

        //Check first created auth user is alerted because of new auth user creation
        assertNewUserCreatedAlertEmail(firstUserModel.getEmail(), firstUserModel.getName(), secondUserModel.getName(),
                secondUserModel.getSurname(), secondUserModel.getEmail(), firstUserModel.getEmail());

        //Create another auth user to see expected mails are sent to root and first created auth user
        final UsersModel thirdUserModel = UsersModel.DefaultUsersModel().build();
        UsersHelper.createAuthenticatedUser(thirdUserModel, getSecretKey(), authenticatedUser.getRight());

        //Check root user is alerted because of new auth user creation
        assertNewUserCreatedAlertEmail(getRootEmail(), createCorporateModel.getRootUser().getName(), thirdUserModel.getName(),
                thirdUserModel.getSurname(), thirdUserModel.getEmail(), firstUserModel.getEmail());

        //Check first created auth user is alerted because of new auth user creation
        assertNewUserCreatedAlertEmail(firstUserModel.getEmail(), firstUserModel.getName(), thirdUserModel.getName(),
                thirdUserModel.getSurname(), thirdUserModel.getEmail(), firstUserModel.getEmail());

        //Check alert mail is not sent to second created user
        final MailHogMessageResponse mailToSecondAuthUser = MailhogHelper.getMailHogEmail(secondUserModel.getEmail());
        assertNotEquals(NEW_USER_CREATED_ALERT.getSubject(), mailToSecondAuthUser.getSubject());
    }

    @Test
    public void PostUsers_AlertJustRootUserSinceFirstCreatedAuthorizedUserNotVerifyEmail_Success() {

        //Create first email verified Auth User, this will NOT be alerted when a new user is created because email is not verified
        final UsersModel firstUserModel = UsersModel.DefaultUsersModel().build();
        UsersHelper.createUser(firstUserModel, getSecretKey(), getAuthToken());
        UsersService.sendEmailVerification(new SendEmailVerificationModel(firstUserModel.getEmail()), getSecretKey());

        //Check root user is alerted because of new auth user creation
        assertNewUserCreatedAlertEmail(getRootEmail(), createCorporateModel.getRootUser().getName(), firstUserModel.getName(),
                firstUserModel.getSurname(), firstUserModel.getEmail(), getRootEmail());

        //Create second email verified Auth User, this will NOT be alerted when a new user is created, it will be checked line 111
        final UsersModel secondUserModel = UsersModel.DefaultUsersModel().build();
        UsersHelper.createAuthenticatedUser(secondUserModel, getSecretKey(), getAuthToken());

        //Check root user is alerted because of new auth user creation
        assertNewUserCreatedAlertEmail(getRootEmail(), createCorporateModel.getRootUser().getName(), secondUserModel.getName(),
                secondUserModel.getSurname(), secondUserModel.getEmail(), getRootEmail());

        //Check first created auth user is NOT alerted because of new auth user creation
        final MailHogMessageResponse mailToFirstAuthUser = MailhogHelper.getMailHogEmail(secondUserModel.getEmail());
        assertNotEquals(NEW_USER_CREATED_ALERT.getSubject(), mailToFirstAuthUser.getSubject());

        //Create another auth user to see expected mails are sent to root and first created auth user
        final UsersModel thirdUserModel = UsersModel.DefaultUsersModel().build();
        UsersHelper.createAuthenticatedUser(thirdUserModel, getSecretKey(), getAuthToken());

        //Check root user is alerted because of new auth user creation
        assertNewUserCreatedAlertEmail(getRootEmail(), createCorporateModel.getRootUser().getName(), thirdUserModel.getName(),
                thirdUserModel.getSurname(), thirdUserModel.getEmail(), getRootEmail());

        //Check first created auth user is NOT alerted because of new auth user creation
        final MailHogMessageResponse getLastMailToAuthUser = MailhogHelper.getMailHogEmail(secondUserModel.getEmail());
        assertNotEquals(NEW_USER_CREATED_ALERT.getSubject(), getLastMailToAuthUser.getSubject());
    }

    @Test
    public void PostUsers_NewUserCreatedAlertDisabledForProgrammeNoAlert_Success() {
        setAlertNewUserAddedProperty(false);

        //Create first email verified Auth User
        final UsersModel firstUserModel = UsersModel.DefaultUsersModel().build();
        UsersHelper.createAuthenticatedUser(firstUserModel, getSecretKey(), getAuthToken());

        //Check root user is NOT alerted because of new auth user creation
        final MailHogMessageResponse mailToRootUser = MailhogHelper.getMailHogEmail(getRootEmail());
        assertNotEquals(NEW_USER_CREATED_ALERT.getSubject(), mailToRootUser.getSubject());

        setAlertNewUserAddedProperty(true);
    }

    @Override
    protected String getSecretKey() {
        return secretKey;
    }

    @Override
    protected String getAuthToken() {
        return this.authenticationToken;
    }

    @Override
    protected String getIdentityId() {
        return this.corporateId;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CORPORATE;
    }

    @Override
    protected String createPassword(String userId) {
        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, userId, secretKey);
        return createPasswordModel.getPassword().getValue();
    }

    @Override
    protected String getRootEmail() {
        return this.corporateEmail;
    }

    private void assertNewUserCreatedAlertEmail(final String receiverEmail,
                                                final String receiverName,
                                                final String addedUserName,
                                                final String addedUserSurname,
                                                final String addedUserEmail,
                                                final String inviterEmail){

        final MailHogMessageResponse actualMail = MailhogHelper.getMailHogEmail(receiverEmail);

        assertEquals(NEW_USER_CREATED_ALERT.getFrom(), actualMail.getFrom());
        assertEquals(receiverEmail, actualMail.getTo());
        assertEquals(NEW_USER_CREATED_ALERT.getSubject(), actualMail.getSubject());
        assertEquals(String.format(NEW_USER_CREATED_ALERT.getEmailText(), receiverName, addedUserName, addedUserSurname, addedUserEmail, inviterEmail, innovatorName), actualMail.getBody());
    }

    private static void setAlertNewUserAddedProperty(final boolean isEnabled){
        final ContextPropertiesV2Model model = ContextPropertiesV2Model.setAlertNewUserAddedProperty(applicationOne.getProgrammeId(), isEnabled);
        AdminService.setContextPropertyV2(model, adminToken, "ALERT_NEW_USER_ADDED_ENABLE")
                .then()
                .statusCode(SC_NO_CONTENT);

        AdminService.getContextPropertyV2(List.of(Map.of("PROGRAMME_ID", applicationOne.getProgrammeId())),
                        adminToken, "ALERT_NEW_USER_ADDED_ENABLE")
                .then()
                .statusCode(SC_OK)
                .body("result[0].context.PROGRAMME_ID", equalTo(applicationOne.getProgrammeId()))
                .body("result[0].value.stringValue", equalTo(String.valueOf(isEnabled)));
    }

    @AfterAll
    public static void disableAlert() {
        setAlertNewUserAddedProperty(false);
    }
}
