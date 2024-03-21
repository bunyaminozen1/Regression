package opc.junit.multi.users;

import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import io.restassured.response.Response;
import opc.enums.mailhog.MailHogSms;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.passwords.LostPasswordResumeModel;
import opc.models.multi.passwords.LostPasswordStartModel;
import opc.models.multi.users.UsersModel;
import opc.models.multi.users.UsersModel.Builder;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.mailhog.MailhogService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.PasswordsService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_REQUEST_TOO_LONG;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public abstract class AbstractPatchUserTests extends AbstractUserTests {

    final private static int mobileChangeLimit = 3;

    private final static String VERIFICATION_CODE = "123456";

    @Test
    public void PatchUser_AllFields_Success() {

        final User newUser = createNewUser();
        final String password = createPassword(newUser.id);

        final UsersModel patchUserDetails = new Builder()
                .setName(RandomStringUtils.randomAlphabetic(5))
                .setSurname(RandomStringUtils.randomAlphabetic(10))
                .setEmail(RandomStringUtils.randomAlphanumeric(10) + "@weavr.io")
                .setDateOfBirth(new DateOfBirthModel(1980, 4, 5))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(newUser.id))
                .body("identity.id", equalTo(newUser.identityId))
                .body("identity.type", equalTo(newUser.identityType.name()))
                .body("name", equalTo(patchUserDetails.getName()))
                .body("surname", equalTo(patchUserDetails.getSurname()))
                .body("email", equalTo(patchUserDetails.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(patchUserDetails.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(patchUserDetails.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(patchUserDetails.getDateOfBirth().getDay()));

        AuthenticationService.loginWithPassword(new LoginModel(newUser.userDetails.getEmail(), new PasswordModel(password)), getSecretKey())
                .then()
                .statusCode(SC_FORBIDDEN);

        AuthenticationService.loginWithPassword(new LoginModel(patchUserDetails.getEmail(), new PasswordModel(password)), getSecretKey())
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void PatchUser_Name_Success() {
        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setName(RandomStringUtils.randomAlphabetic(5))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(newUser.id))
                .body("identity.id", equalTo(newUser.identityId))
                .body("identity.type", equalTo(newUser.identityType.name()))
                .body("name", equalTo(patchUserDetails.getName()))
                .body("surname", equalTo(newUser.userDetails.getSurname()))
                .body("email", equalTo(newUser.userDetails.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(newUser.userDetails.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(newUser.userDetails.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(newUser.userDetails.getDateOfBirth().getDay()));
    }

    @Test
    public void PatchUser_Surname_Success() {
        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setSurname(RandomStringUtils.randomAlphabetic(14))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(newUser.id))
                .body("identity.id", equalTo(newUser.identityId))
                .body("identity.type", equalTo(newUser.identityType.name()))
                .body("name", equalTo(newUser.userDetails.getName()))
                .body("surname", equalTo(patchUserDetails.getSurname()))
                .body("email", equalTo(newUser.userDetails.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(newUser.userDetails.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(newUser.userDetails.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(newUser.userDetails.getDateOfBirth().getDay()));
    }

    @Test
    public void PatchUser_DateOfBirth_Success() {
        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setDateOfBirth(new DateOfBirthModel(1981, 11, 11))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(newUser.id))
                .body("identity.id", equalTo(newUser.identityId))
                .body("identity.type", equalTo(newUser.identityType.name()))
                .body("name", equalTo(newUser.userDetails.getName()))
                .body("surname", equalTo(newUser.userDetails.getSurname()))
                .body("email", equalTo(newUser.userDetails.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(patchUserDetails.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(patchUserDetails.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(patchUserDetails.getDateOfBirth().getDay()));
    }

    @Test
    public void PatchUser_Email() throws SQLException {
        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
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
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(newUser.userDetails.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(newUser.userDetails.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(newUser.userDetails.getDateOfBirth().getDay()));

        final Map<Integer, Map<String, String>> newEmailNonce = getEmailNonce(newUser.id, newUser.identityType);
        assertEquals(1, newEmailNonce.size());
        assertEquals(patchUserDetails.getEmail(), newEmailNonce.get(0).get("email_used"));
        assertNull(newEmailNonce.get(0).get("sent_at"));
    }

    @Test
    public void PatchUser_PatchMobileCountryCode_Success() {
        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setMobile(new MobileNumberModel("+49", newUser.userDetails.getMobile().getNumber()))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
    }

    @Test
    public void PatchUser_PatchMobileNumber_Success() {
        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setMobile(MobileNumberModel.random())
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
    }

    @Test
    public void PatchUser_PatchMobileWithUniqueNumber_Success() {
        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setMobile(MobileNumberModel.random())
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
    }

    @Test
    public void PatchUser_PatchMobileWithNonUniqueNumberSameIdentityType_MobileNotUnique() {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        if (getIdentityType().equals(IdentityType.CORPORATE)) {
            final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                    corporateProfileId, secretKey);
            UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporate.getRight());

        } else {
            final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
                    consumerProfileId, secretKey);
            UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumer.getRight());
        }

        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setMobile(usersModel.getMobile())
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
    }

    @Test
    public void PatchUser_PatchMobileWithNonUniqueNumberCrossIdentityType_Success() {

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();

        if (getIdentityType().equals(IdentityType.CORPORATE)) {
            final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
                    consumerProfileId, secretKey);
            UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumer.getRight());

        } else {
            final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                    corporateProfileId, secretKey);
            UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporate.getRight());
        }

        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setMobile(usersModel.getMobile())
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
    }

    @Test
    public void PatchUser_MobileChangeLimitUpdateMobileFieldNull_Success() {
        final User newUser = createNewUser();

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final UsersModel patchUserDetails = new Builder()
                            .setMobile(null)
                            .build();

                    UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("mobile.countryCode", equalTo(newUser.userDetails.getMobile().getCountryCode()))
                            .body("mobile.number", equalTo(newUser.userDetails.getMobile().getNumber()));
                });

        final UsersModel patchUserDetails = new Builder()
                .setMobile(MobileNumberModel.random())
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
    }

    @Test
    public void PatchUser_MobileChangeLimitByUpdateNewNumber_LimitExceeded() {
        final User newUser = createNewUser();

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final UsersModel patchUserDetails = new Builder()
                            .setMobile(MobileNumberModel.random())
                            .build();

                    UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                            .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
                });

        final UsersModel patchUserDetails = new Builder()
                .setMobile(MobileNumberModel.random())
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NO_CHANGE_LIMIT_EXCEEDED"));
    }

    @Test
    public void PatchUser_UpdateMobileMultipleTimesWithItsOwnNumber_Success() {
        final User newUser = createNewUser();

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final UsersModel patchUserDetails = new Builder()
                            .setMobile(newUser.userDetails.getMobile())
                            .build();

                    UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("mobile.countryCode", equalTo(newUser.userDetails.getMobile().getCountryCode()))
                            .body("mobile.number", equalTo(newUser.userDetails.getMobile().getNumber()));
                });

        final UsersModel patchUserDetails = new Builder()
                .setMobile(MobileNumberModel.random())
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
    }

    @Test
    public void PatchUser_UpdateAnotherFieldAfterMobileChangeLimitExceeded_Success() {
        final User newUser = createNewUser();

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final UsersModel patchUserDetails = new Builder()
                            .setMobile(MobileNumberModel.random())
                            .build();

                    UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                            .then()
                            .statusCode(SC_OK)
                            .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                            .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
                });

        final UsersModel patchUserDetails = new Builder()
                .setMobile(MobileNumberModel.random())
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NO_CHANGE_LIMIT_EXCEEDED"));

        final UsersModel patchUserDetails2 = new Builder()
                .setName(RandomStringUtils.randomAlphabetic(5))
                .setSurname(RandomStringUtils.randomAlphabetic(5))
                .build();

        UsersService.patchUser(patchUserDetails2, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("name", equalTo(patchUserDetails2.getName()))
                .body("surname", equalTo(patchUserDetails2.getSurname()));
    }

    @Test
    public void PatchUser_UpdateMobileLimitThreeTimes_Invalid() {
        final User newUser = createNewUser();
        final int mobileChangeLimit = 3;

        IntStream.range(0, mobileChangeLimit)
                .forEach(i -> {
                    final UsersModel patchUserDetails = new Builder()
                            .setMobile(MobileNumberModel.random())
                            .build();

                    UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                            .then()
                            .statusCode(SC_OK);
                });

        final UsersModel patchUserDetails = new Builder()
                .setMobile(MobileNumberModel.random())
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("MOBILE_NO_CHANGE_LIMIT_EXCEEDED"));
    }

    @Test
    public void PatchUser_RootUser_NotFound() {
        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setName(RandomStringUtils.randomAlphabetic(5))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.identityId, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PatchUser_InvalidName() {

        createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setName(RandomStringUtils.randomAlphanumeric(25))
                .build();

        UsersService.createUser(patchUserDetails, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchUser_InvalidSurname() {

        createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setSurname(RandomStringUtils.randomAlphanumeric(25))
                .build();

        UsersService.createUser(patchUserDetails, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchUser_InvalidEmail() {
        createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setName(RandomStringUtils.randomAlphabetic(5))
                .setSurname(RandomStringUtils.randomAlphabetic(10))
                .setEmail(RandomStringUtils.randomAlphanumeric(25))
                .build();

        UsersService.createUser(patchUserDetails, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @MethodSource("emailProvider")
    public void PatchUser_ChangeEmailHavingApostropheOrSingleQuotes_Success(final String email) {

        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setName(RandomStringUtils.randomAlphabetic(5))
                .setSurname(RandomStringUtils.randomAlphabetic(10))
                .setEmail(email)
                .setDateOfBirth(new DateOfBirthModel(1980, 4, 5))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PatchUser_InvalidEmailFormat_BadRequest() {

        createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setEmail(String.format("%s.@weavrusertest.io", RandomStringUtils.randomAlphanumeric(6)))
                .build();

        UsersService.createUser(patchUserDetails, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchUser_EmailNotUnique() {

        final User user1 = createNewUser();
        final User user2 = createNewUser();

        UsersService.patchUser(new UsersModel.Builder().setEmail(user1.userDetails.getEmail()).build(), getSecretKey(), user2.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_UNIQUE"));
    }

    @Test
    public void PatchUser_UserNotFound() {
        final UsersModel user = UsersModel.DefaultUsersModel().build();
        UsersService.patchUser(user, getSecretKey(), RandomStringUtils.randomNumeric(18), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PatchUser_IdentityImpersonator_Forbidden() {

        final User newUser = createNewUser();
        createPassword(newUser.id);

        final UsersModel patchUserDetails = new Builder()
                .setName(RandomStringUtils.randomAlphabetic(5))
                .setSurname(RandomStringUtils.randomAlphabetic(10))
                .setEmail(RandomStringUtils.randomAlphanumeric(10) + "@weavr.io")
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getBackofficeImpersonateToken(newUser.identityId, newUser.identityType), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PatchUser_InvalidDateOfBirthDay_BadRequest() {

        createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setDateOfBirth(new DateOfBirthModel(1990, 11, 32))
                .build();

        UsersService.createUser(patchUserDetails, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchUser_InvalidDateOfBirthMonth_BadRequest() {

        createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setDateOfBirth(new DateOfBirthModel(1899, 11, 11))
                .build();

        UsersService.createUser(patchUserDetails, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchUser_CrossIdentityType_NotFound() {
        final User newUser = createNewUser();
        String newIdentityToken;

        if (newUser.identityType.equals(IdentityType.CORPORATE)) {
            newIdentityToken = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey).getRight();
        } else {
            newIdentityToken = CorporatesHelper.createAuthenticatedVerifiedCorporate(corporateProfileId, secretKey).getRight();
        }

        final UsersModel patchUserDetails = new Builder()
                .setName(RandomStringUtils.randomAlphabetic(5))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, newIdentityToken, Optional.empty())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void PatchUser_CrossIdentity_Forbidden() {
        final User newUser = createNewUser();
        String newIdentityToken;

        if (newUser.identityType.equals(IdentityType.CONSUMER)) {
            newIdentityToken = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey).getRight();
        } else {
            newIdentityToken = CorporatesHelper.createAuthenticatedVerifiedCorporate(corporateProfileId, secretKey).getRight();
        }

        final UsersModel patchUserDetails = new Builder()
                .setName(RandomStringUtils.randomAlphabetic(5))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, newIdentityToken, Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", ""})
    public void PatchUsers_InvalidMobileNumber_BadRequest(final String mobileNumber) {
        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setMobile(new MobileNumberModel("+356", mobileNumber))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PatchUsers_NoMobileNumberCountryCode_BadRequest() {

        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setMobile(new MobileNumberModel("", "123456"))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", "123", "+"})
    public void PatchUsers_InvalidMobileNumberCountryCode_MobileOrCountryCodeInvalid(final String mobileNumberCountryCode) {

        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setMobile(new MobileNumberModel(mobileNumberCountryCode, "123456"))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    /**
     * Cases for the ticket <a href="https://weavr-payments.atlassian.net/browse/DEV-4974">...</a>
     * 1. Create 2 users with the same mobile number
     * 2. Create third user without mobile number
     * 3. Patch third user with the same mobile number and country code as previous ones.
     */
    @Test
    public void PatchUser_SeveralUsersSameNumberPatchMobileNotUniqueNumber_Success() {
        final String mobileNumber = MobileNumberModel.random().getNumber();

        final UsersModel firstUserModel = UsersModel.DefaultUsersModel()
                .setMobile(new MobileNumberModel("+356", mobileNumber))
                .build();
        UsersHelper.createUser(firstUserModel, getSecretKey(), getAuthToken());

        final UsersModel secondUserModel = UsersModel.DefaultUsersModel()
                .setMobile(new MobileNumberModel("+356", mobileNumber))
                .build();
        UsersHelper.createUser(secondUserModel, getSecretKey(), getAuthToken());

        final UsersModel thirdUserModel = UsersModel.DefaultUsersModel()
                .setMobile(null)
                .build();
        final String thirdUserId = UsersHelper.createUser(thirdUserModel, getSecretKey(), getAuthToken());

        final UsersModel patchUserDetails = new Builder()
                .setMobile(new MobileNumberModel("+356", mobileNumber))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), thirdUserId, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
    }

    @Test
    public void PatchUser_SeveralUsersSameNumberPatchMobileUniqueNumber_Success() {
        final String mobileNumber = MobileNumberModel.random().getNumber();

        final UsersModel firstUserModel = UsersModel.DefaultUsersModel()
                .setMobile(new MobileNumberModel("+356", mobileNumber))
                .build();
        UsersHelper.createUser(firstUserModel, getSecretKey(), getAuthToken());

        final UsersModel secondUserModel = UsersModel.DefaultUsersModel()
                .setMobile(new MobileNumberModel("+356", mobileNumber))
                .build();
        UsersHelper.createUser(secondUserModel, getSecretKey(), getAuthToken());

        final UsersModel thirdUserModel = UsersModel.DefaultUsersModel()
                .setMobile(null)
                .build();
        final String thirdUserId = UsersHelper.createUser(thirdUserModel, getSecretKey(), getAuthToken());

        final UsersModel patchUserDetails = new Builder()
                .setMobile(MobileNumberModel.random())
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), thirdUserId, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
    }

    @Test
    public void PatchUser_CheckMobileChangeSmsOldNumberVerified_Success() throws InterruptedException {
        final UsersModel model = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(model, getSecretKey(), getAuthToken());
        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), getSecretKey(), user.getRight());

        final UsersModel patchUserDetails = new Builder()
                .setMobile(MobileNumberModel.random())
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), user.getLeft(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), getSecretKey(), user.getRight());

        Thread.sleep(60);
        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(model.getMobile().getNumber());

        assertEquals(String.format(MailHogSms.SCA_CHANGE_SMS.getSmsText(),
                        StringUtils.right(model.getMobile().getNumber(), 4),
                        StringUtils.right(patchUserDetails.getMobile().getNumber(), 4)),
                sms.getBody());
    }

    @Test
    public void PatchUser_CheckMobileChangeSmsOldNumberNotVerified_NoSms() {
        final UsersModel model = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(model, getSecretKey(), getAuthToken());

        final UsersModel patchUserDetails = new Builder()
                .setMobile(MobileNumberModel.random())
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), user.getLeft(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));

        AuthenticationFactorsHelper.enrolAndVerifyOtp(VERIFICATION_CODE, EnrolmentChannel.SMS.name(), getSecretKey(), user.getRight());

        MailhogService.getMailHogSms(model.getMobile().getNumber())
                .then()
                .statusCode(200)
                .body("items[0]", nullValue());
    }

    @Test
    public void PatchUser_NewEmailVerifiedMobileNumberOtpNotEnrolledSecurityRule24H_Success() throws InterruptedException {

        final User newUser = createNewUser();

        final UsersModel patchUserEmail = new Builder()
                .setEmail(RandomStringUtils.randomAlphanumeric(10) + "@weavr.io")
                .build();

        UsersService.patchUser(patchUserEmail, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(newUser.id))
                .body("identity.id", equalTo(newUser.identityId))
                .body("identity.type", equalTo(newUser.identityType.name()))
                .body("name", equalTo(newUser.userDetails.getName()))
                .body("surname", equalTo(newUser.userDetails.getSurname()))
                .body("email", equalTo(patchUserEmail.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(newUser.userDetails.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(newUser.userDetails.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(newUser.userDetails.getDateOfBirth().getDay()));

        //No blocking time to check the new functionality, user can update the mobile number once otp was not enrolled

        final UsersModel patchUserMobile = new Builder()
                .setMobile(MobileNumberModel.random())
                .build();

        UsersService.patchUser(patchUserMobile, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserMobile.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserMobile.getMobile().getNumber()));

        UsersService.getUser(secretKey, newUser.id, getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(newUser.id))
                .body("identity.id", equalTo(newUser.identityId))
                .body("identity.type", equalTo(newUser.identityType.name()))
                .body("name", equalTo(newUser.userDetails.getName()))
                .body("surname", equalTo(newUser.userDetails.getSurname()))
                .body("email", equalTo(patchUserEmail.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(newUser.userDetails.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(newUser.userDetails.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(newUser.userDetails.getDateOfBirth().getDay()))
                .body("mobile.countryCode", equalTo(patchUserMobile.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserMobile.getMobile().getNumber()));
    }

    @Test
    public void PatchUser_ResumeLostPasswordMobileNumberOtpNotEnrolledSecurityRule24_Success() {

        final User newUser = createNewUser();

        PasswordsService.startLostPassword(new LostPasswordStartModel(newUser.userDetails.getEmail()), secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        final LostPasswordResumeModel lostPasswordResumeModel =
                LostPasswordResumeModel
                        .newBuilder()
                        .setEmail(newUser.userDetails.getEmail())
                        .setNewPassword(new PasswordModel("NewPass1234"))
                        .setNonce(TestHelper.VERIFICATION_CODE)
                        .build();

        PasswordsService.resumeLostPassword(lostPasswordResumeModel, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());

        //No blocking time to check the new functionality, user can update the mobile number once otp was not enrolled

        final UsersModel patchUserMobile = new Builder()
                .setMobile(MobileNumberModel.random())
                .build();

        UsersService.patchUser(patchUserMobile, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserMobile.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserMobile.getMobile().getNumber()));

        UsersService.getUser(secretKey, newUser.id, getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(newUser.id))
                .body("identity.id", equalTo(newUser.identityId))
                .body("identity.type", equalTo(newUser.identityType.name()))
                .body("name", equalTo(newUser.userDetails.getName()))
                .body("surname", equalTo(newUser.userDetails.getSurname()))
                .body("email", equalTo(newUser.userDetails.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(newUser.userDetails.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(newUser.userDetails.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(newUser.userDetails.getDateOfBirth().getDay()))
                .body("mobile.countryCode", equalTo(patchUserMobile.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserMobile.getMobile().getNumber()));
    }

    @Test
    public void PatchUser_PatchMobileNumberCountryCodeCharacterLimitOne_Success() {

        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setMobile(new MobileNumberModel("+1", String.format("82923%s", RandomStringUtils.randomNumeric(5))))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void PatchUser_PatchMobileNumberCountryCodeCharacterLimitTwoToFour_Success() {

        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setMobile(new MobileNumberModel("+49",
                        String.format("30%s", RandomStringUtils.randomNumeric(6))))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
    }

    @Test
    public void PatchUser_PatchMobileNumberCountryCodeCharacterLimitFourToSix_Success() {

        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setMobile(new MobileNumberModel("+1829",
                        String.format("23%s", RandomStringUtils.randomNumeric(5))))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+1-1829", "001-1829"})
    public void PatchUser_PatchMobileNumberCountryCodeCharacterLimitMoreThanSix_BadRequest(final String countryCode) {

        final User newUser = createNewUser();

        final UsersModel patchUserDetails = new Builder()
                .setMobile(new MobileNumberModel(countryCode, String.format("23%s", RandomStringUtils.randomNumeric(5))))
                .build();

        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[0].message", equalTo("request.mobile.countryCode: must match \"^\\+[0-9]+$\""))
                .body("_embedded.errors[1].message", equalTo("request.mobile.countryCode: size must be between 1 and 6"));
    }

    @Test
    public void PatchUser_UpdateTag_Success() {
        final User newUser = createNewUser();

        //Update tag first time
        final UsersModel firstPatch = new Builder()
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .build();

        UsersService.patchUser(firstPatch, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(newUser.id))
                .body("tag", equalTo(firstPatch.getTag()));

        final Map<String, Object> filter = new HashMap<>();
        filter.put("id", newUser.id);
        UsersService.getUsers(getSecretKey(), Optional.of(filter), getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("users[0].id", equalTo(newUser.id))
                .body("users[0].tag", equalTo(firstPatch.getTag()));

        //Update tag second time
        final UsersModel secondPatch = new Builder()
                .setTag(RandomStringUtils.randomAlphabetic(5))
                .build();

        UsersService.patchUser(secondPatch, getSecretKey(), newUser.id, getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", equalTo(newUser.id))
                .body("tag", equalTo(secondPatch.getTag()));

        UsersService.getUsers(getSecretKey(), Optional.of(filter), getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("users[0].id", equalTo(newUser.id))
                .body("users[0].tag", equalTo(secondPatch.getTag()));
    }

    // TODO Idempotency tests to be updated when this operation becomes idempotent

//    @Test
//    public void PatchUser_SameIdempotencyRefDifferentPayload_BadRequest() {
//        final User newUser = createNewUser();
//
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//
//        final UsersModel firstPatchUserDetails = new UsersModel.Builder()
//                .setName(RandomStringUtils.randomAlphabetic(5))
//                .build();
//        UsersService.patchUser(firstPatchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_OK);
//
//        final UsersModel secondPatchUserDetails = new UsersModel.Builder()
//                .setName(RandomStringUtils.randomAlphabetic(5))
//                .build();
//        UsersService.patchUser(secondPatchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_BAD_REQUEST);
////        TODO returns 200
//    }
//
//    @Test
//    public void PatchUser_SameIdempotencyRefSamePayload_Success() {
//        final User newUser = createNewUser();
//
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final UsersModel patchUserDetails = new UsersModel.Builder()
//                .setName(RandomStringUtils.randomAlphabetic(5))
//                .build();
//        responses.add(UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.of(idempotencyReference)));
//        responses.add(UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.of(idempotencyReference)));
//
//        responses.forEach(response ->
//                response.then()
//                        .statusCode(SC_OK)
//                        .body("id", equalTo(newUser.id))
//                        .body("identity.id", equalTo(newUser.identityId))
//                        .body("identity.type", equalTo(newUser.identityType.name()))
//                        .body("name", equalTo(patchUserDetails.getName()))
//                        .body("surname", equalTo(newUser.userDetails.getSurname()))
//                        .body("email", equalTo(newUser.userDetails.getEmail()))
//                        .body("active", equalTo(true))
//                        .body("dateOfBirth.year", equalTo(newUser.userDetails.getDateOfBirth().getYear()))
//                        .body("dateOfBirth.month", equalTo(newUser.userDetails.getDateOfBirth().getMonth()))
//                        .body("dateOfBirth.day", equalTo(newUser.userDetails.getDateOfBirth().getDay())));
//
//        assertEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
//        assertEquals(responses.get(0).jsonPath().getString("name"), responses.get(1).jsonPath().getString("name"));
//    }
//
//    @Test
//    public void PatchUser_DifferentIdempotencyRefSamePayload_Success() {
//        final User newUser = createNewUser();
//
//        final String firstIdempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final String secondIdempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//
//        final UsersModel patchUserDetails = new UsersModel.Builder()
//                .setName(RandomStringUtils.randomAlphabetic(5))
//                .build();
//        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.of(firstIdempotencyReference))
//                .then()
//                .statusCode(SC_OK);
//
//        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.of(secondIdempotencyReference))
//                .then()
//                .statusCode(SC_OK);
//        //        TODO is this response correct?
//    }
//
//    @Test
//    public void PatchUser_LongIdempotencyRef_BadRequest() {
//        final User newUser = createNewUser();
//
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(8000);
//
//        final UsersModel patchUserDetails = new UsersModel.Builder()
//                .setName(RandomStringUtils.randomAlphabetic(5))
//                .build();
//        UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_REQUEST_TOO_LONG);
//    }
//
//    @Test
//    public void PatchUser_SameIdempotencyRefDifferentPayloadInitialCallFailed_BadRequest() {
//        final User newUser = createNewUser();
//
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//
//        final UsersModel firstPatchUserDetails = new UsersModel.Builder()
//                .setName(RandomStringUtils.randomAlphabetic(25))
//                .build();
//        UsersService.patchUser(firstPatchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_BAD_REQUEST);
//
//        final UsersModel secondPatchUserDetails = new UsersModel.Builder()
//                .setName(RandomStringUtils.randomAlphabetic(5))
//                .build();
//        UsersService.patchUser(secondPatchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.of(idempotencyReference))
//                .then()
//                .statusCode(SC_BAD_REQUEST);
////        TODO returns 200
//    }
//
//    @Test
//    public void PatchUser_SameIdempotencyRefSamePayloadReferenceExpired_Success() throws InterruptedException {
//        final User newUser = createNewUser();
//
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final UsersModel patchUserDetails = new UsersModel.Builder()
//                .setName(RandomStringUtils.randomAlphabetic(5))
//                .build();
//        responses.add(UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.of(idempotencyReference)));
//
//        TimeUnit.SECONDS.sleep(18);
//
//        responses.add(UsersService.patchUser(patchUserDetails, getSecretKey(), newUser.id, getAuthToken(), Optional.of(idempotencyReference)));
//
//        responses.forEach(response ->
//                response.then()
//                        .statusCode(SC_OK)
//                        .body("id", equalTo(newUser.id))
//                        .body("identity.id", equalTo(newUser.identityId))
//                        .body("identity.type", equalTo(newUser.identityType.name()))
//                        .body("surname", equalTo(newUser.userDetails.getSurname()))
//                        .body("email", equalTo(newUser.userDetails.getEmail()))
//                        .body("active", equalTo(true))
//                        .body("dateOfBirth.year", equalTo(newUser.userDetails.getDateOfBirth().getYear()))
//                        .body("dateOfBirth.month", equalTo(newUser.userDetails.getDateOfBirth().getMonth()))
//                        .body("dateOfBirth.day", equalTo(newUser.userDetails.getDateOfBirth().getDay())));
//
//
//        assertEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
//        assertEquals(responses.get(0).jsonPath().getString("name"), responses.get(1).jsonPath().getString("name"));
//    }
//
//    @Test
//    public void PatchUser_SameIdempotencyRefDifferentPayloadDifferentUser_Success() {
//        final User firstUser = createNewUser();
//        final User secondUser = createNewUser();
//
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final UsersModel firstPatchUserDetails = new UsersModel.Builder()
//                .setName(RandomStringUtils.randomAlphabetic(5))
//                .build();
//        responses.add(UsersService.patchUser(firstPatchUserDetails, getSecretKey(), firstUser.id, getAuthToken(), Optional.of(idempotencyReference)));
//
//        final UsersModel secondPatchUserDetails = new UsersModel.Builder()
//                .setName(RandomStringUtils.randomAlphabetic(5))
//                .build();
//        responses.add(UsersService.patchUser(secondPatchUserDetails, getSecretKey(), secondUser.id, getAuthToken(), Optional.of(idempotencyReference)));
//
//        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
//        assertEquals(responses.get(0).jsonPath().getString("name"), firstPatchUserDetails.getName());
//        assertEquals(responses.get(0).jsonPath().getString("surname"), firstUser.userDetails.getSurname());
//        assertEquals(responses.get(1).jsonPath().getString("name"), secondPatchUserDetails.getName());
//        assertEquals(responses.get(1).jsonPath().getString("surname"), secondUser.userDetails.getSurname());
//
//    }
//
//    @Test
//    public void PatchUser_SameIdempotencyRefSamePayloadDifferentUser_Success() {
//        final User firstUser = createNewUser();
//        final User secondUser = createNewUser();
//
//        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
//        final List<Response> responses = new ArrayList<>();
//
//        final UsersModel patchUserDetails = new UsersModel.Builder()
//                .setName(RandomStringUtils.randomAlphabetic(5))
//                .build();
//        responses.add(UsersService.patchUser(patchUserDetails, getSecretKey(), firstUser.id, getAuthToken(), Optional.of(idempotencyReference)));
//        responses.add(UsersService.patchUser(patchUserDetails, getSecretKey(), secondUser.id, getAuthToken(), Optional.of(idempotencyReference)));
//
//        assertNotEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
//        assertEquals(responses.get(0).jsonPath().getString("name"), responses.get(1).jsonPath().getString("name"));
//        assertEquals(responses.get(0).jsonPath().getString("surname"), firstUser.userDetails.getSurname());
//        assertEquals(responses.get(1).jsonPath().getString("surname"), secondUser.userDetails.getSurname());
//    }

    protected abstract IdentityType getIdentityType();

    private static Stream<Arguments> emailProvider() {
        return Stream.of(
                arguments(String.format("%s's@weavrtest.io", RandomStringUtils.randomAlphabetic(5))),
                arguments(String.format("'%s'@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
        );
    }
}
