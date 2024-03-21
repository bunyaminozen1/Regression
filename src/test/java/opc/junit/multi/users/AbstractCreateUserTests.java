package opc.junit.multi.users;

import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import io.restassured.response.Response;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.backoffice.BackofficeHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.services.multi.AuthenticationService;
import opc.services.multi.PasswordsService;
import opc.services.multi.UsersService;
import opc.tags.MultiTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_REQUEST_TOO_LONG;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Execution(ExecutionMode.CONCURRENT)
@Tag(MultiTags.MULTI)
@Tag(MultiTags.USERS)
public abstract class AbstractCreateUserTests extends BaseUsersSetup {

    protected abstract String getSecretKey();

    protected abstract String getAuthToken();

    protected abstract String getIdentityId();

    protected abstract IdentityType getIdentityType();

    protected abstract String createPassword(String userId);

    protected abstract String getRootEmail();

    @Test
    public void PostUsers_SameIdempotencyRefDifferentPayload_BadRequest() {
        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        final UsersModel firstUserModel = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(firstUserModel, getSecretKey(), getAuthToken(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_OK);

        final UsersModel secondUserModel = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(secondUserModel, getSecretKey(), getAuthToken(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Payloads do not match for the same idempotency-ref."));
    }

    @Test
    public void PostUsers_SameIdempotencyRefSamePayload_Success() {
        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        final UsersModel userModel = UsersModel.DefaultUsersModel().build();

        responses.add(UsersService.createUser(userModel, getSecretKey(), getAuthToken(), Optional.of(idempotencyReference)));
        responses.add(UsersService.createUser(userModel, getSecretKey(), getAuthToken(), Optional.of(idempotencyReference)));

        responses.forEach(response ->
                response.then()
                        .statusCode(SC_OK)
                        .body("id", not(nullValue()))
                        .body("identity.id", equalTo(getIdentityId()))
                        .body("identity.type", equalTo(getIdentityType().name()))
                        .body("name", equalTo(userModel.getName()))
                        .body("surname", equalTo(userModel.getSurname()))
                        .body("email", equalTo(userModel.getEmail()))
                        .body("active", equalTo(true))
                        .body("dateOfBirth.year", equalTo(userModel.getDateOfBirth().getYear()))
                        .body("dateOfBirth.month", equalTo(userModel.getDateOfBirth().getMonth()))
                        .body("dateOfBirth.day", equalTo(userModel.getDateOfBirth().getDay())));

        assertEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertEquals("1",
                UsersService.getUsers(getSecretKey(), Optional.empty(), getAuthToken()).jsonPath().getString("count"));
    }

    @Test
    public void PostUsers_DifferentIdempotencyRefSamePayload_EmailNotUnique() {

        final UsersModel userModel = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(userModel, getSecretKey(), getAuthToken(), Optional.of(RandomStringUtils.randomAlphanumeric(20)))
                .then()
                .statusCode(SC_OK)
                .body("id", not(nullValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("name", equalTo(userModel.getName()))
                .body("surname", equalTo(userModel.getSurname()))
                .body("email", equalTo(userModel.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(userModel.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(userModel.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(userModel.getDateOfBirth().getDay()));

        UsersService.createUser(userModel, getSecretKey(), getAuthToken(), Optional.of(RandomStringUtils.randomAlphanumeric(20)))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_UNIQUE"));
    }

    @Test
    public void PostUsers_DifferentIdempotencyRefDifferentPayload_Success() {

        final UsersModel userModel = UsersModel.DefaultUsersModel().build();
        final UsersModel userModel1 = UsersModel.DefaultUsersModel().build();

        UsersService.createUser(userModel, getSecretKey(), getAuthToken(), Optional.of(RandomStringUtils.randomAlphanumeric(20)))
                .then()
                .statusCode(SC_OK)
                .body("id", not(nullValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("name", equalTo(userModel.getName()))
                .body("surname", equalTo(userModel.getSurname()))
                .body("email", equalTo(userModel.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(userModel.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(userModel.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(userModel.getDateOfBirth().getDay()));

        UsersService.createUser(userModel1, getSecretKey(), getAuthToken(), Optional.of(RandomStringUtils.randomAlphanumeric(20)))
                .then()
                .statusCode(SC_OK)
                .body("id", not(nullValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("name", equalTo(userModel1.getName()))
                .body("surname", equalTo(userModel1.getSurname()))
                .body("email", equalTo(userModel1.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(userModel1.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(userModel1.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(userModel1.getDateOfBirth().getDay()));
    }

    @Test
    public void PostUsers_SameIdempotencyRefSamePayloadWithChange_Success() {
        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);
        final List<Response> responses = new ArrayList<>();

        final UsersModel userModel = UsersModel.DefaultUsersModel().build();

        responses.add(UsersService.createUser(userModel, getSecretKey(), getAuthToken(), Optional.of(idempotencyReference)));

        final UsersModel updateUser = UsersModel.builder().setName(RandomStringUtils.randomAlphabetic(5)).build();
        UsersHelper.updateUser(updateUser, getSecretKey(), responses.get(0).then().extract().jsonPath().getString("id"), getAuthToken());

        responses.add(UsersService.createUser(userModel, getSecretKey(), getAuthToken(), Optional.of(idempotencyReference)));

        responses.forEach(response ->
                response.then()
                        .statusCode(SC_OK)
                        .body("id", not(nullValue()))
                        .body("identity.id", equalTo(getIdentityId()))
                        .body("identity.type", equalTo(getIdentityType().name()))
                        .body("surname", equalTo(userModel.getSurname()))
                        .body("email", equalTo(userModel.getEmail()))
                        .body("active", equalTo(true))
                        .body("dateOfBirth.year", equalTo(userModel.getDateOfBirth().getYear()))
                        .body("dateOfBirth.month", equalTo(userModel.getDateOfBirth().getMonth()))
                        .body("dateOfBirth.day", equalTo(userModel.getDateOfBirth().getDay())));

        assertEquals(responses.get(0).jsonPath().getString("id"), responses.get(1).jsonPath().getString("id"));
        assertEquals(responses.get(0).jsonPath().getString("name"), userModel.getName());
        assertEquals(responses.get(1).jsonPath().getString("name"), updateUser.getName());
        assertEquals("1",
                UsersService.getUsers(getSecretKey(), Optional.empty(), getAuthToken()).jsonPath().getString("count"));
    }

    @Test
    public void PostUsers_LongIdempotencyRef_RequestTooLong() {
        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(8000);

        final UsersModel userModel = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(userModel, getSecretKey(), getAuthToken(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_REQUEST_TOO_LONG);
    }

    @Test
    public void PostUsers_SameIdempotencyRefDifferentPayloadInitialCallFailed_BadRequest() {
        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        final UsersModel usersModel =
                UsersModel.DefaultUsersModel().build();

        UsersHelper.createAuthenticatedUser(usersModel, secretKey, getAuthToken());

        final UsersModel firstUserModel = UsersModel.DefaultUsersModel()
                .setEmail(usersModel.getEmail())
                .build();

        UsersService.createUser(firstUserModel, getSecretKey(), getAuthToken(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_CONFLICT);

        final UsersModel secondUserModel = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(secondUserModel, getSecretKey(), getAuthToken(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Payloads do not match for the same idempotency-ref."));
    }

    @Test
    public void PostUsers_SameIdempotencyRefDifferentPayloadInitialCallFailed_Success() {
        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        final UsersModel existingUserModel =
                UsersModel.DefaultUsersModel().build();

        final String existingUser =
                UsersHelper.createAuthenticatedUser(existingUserModel, secretKey, getAuthToken())
                        .getLeft();

        final UsersModel usersModel = UsersModel.DefaultUsersModel()
                .setEmail(existingUserModel.getEmail())
                .build();

        UsersService.createUser(usersModel, getSecretKey(), getAuthToken(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_CONFLICT);

        UsersHelper.updateUser(UsersModel.builder()
                .setEmail(String.format("%s@weavrusertest.io", RandomStringUtils.randomAlphabetic(10))).build(),
                secretKey, existingUser, getAuthToken());

        UsersService.createUser(usersModel, getSecretKey(), getAuthToken(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_OK)
                .body("id", not(nullValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("name", equalTo(usersModel.getName()))
                .body("surname", equalTo(usersModel.getSurname()))
                .body("email", equalTo(usersModel.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(usersModel.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(usersModel.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(usersModel.getDateOfBirth().getDay()));
    }

    @Test
    public void PostUsers_SameIdempotencyRefSamePayloadReferenceExpired_EmailNotUnique() throws InterruptedException {

        final String idempotencyReference = RandomStringUtils.randomAlphanumeric(20);

        final UsersModel userModel = UsersModel.DefaultUsersModel().build();

        UsersService.createUser(userModel, getSecretKey(), getAuthToken(), Optional.of(idempotencyReference))
                .then()
                .statusCode(SC_OK)
                .body("id", not(nullValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("name", equalTo(userModel.getName()))
                .body("surname", equalTo(userModel.getSurname()))
                .body("email", equalTo(userModel.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(userModel.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(userModel.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(userModel.getDateOfBirth().getDay()));

        TimeUnit.SECONDS.sleep(18);

        UsersService.createUser(userModel, getSecretKey(), getAuthToken(), Optional.of(RandomStringUtils.randomAlphanumeric(20)))
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_UNIQUE"));
    }

    @Test
    public void PostUsers_Success() {
        final UsersModel user = UsersModel.DefaultUsersModel().build();
        String userId = UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", not(nullValue()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("name", equalTo(user.getName()))
                .body("surname", equalTo(user.getSurname()))
                .body("email", equalTo(user.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(user.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(user.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(user.getDateOfBirth().getDay()))
                .extract()
                .jsonPath()
                .getString("id");
        final String password = createPassword(userId);
        AuthenticationService.loginWithPassword(new LoginModel(user.getEmail(), new PasswordModel(password)), getSecretKey())
                .then()
                .statusCode(SC_OK)
                .body("token", not(emptyString()));
    }

    @Test
    public void PostUsers_RequiredOnly_Success() {
        final UsersModel user = UsersModel.DefaultUsersModel()
                .setDateOfBirth(null)
                .build();
        String userId = UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("id", notNullValue())
                .body("identity.id", equalTo(getIdentityId()))
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("name", equalTo(user.getName()))
                .body("surname", equalTo(user.getSurname()))
                .body("email", equalTo(user.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", nullValue())
                .body("dateOfBirth.month", nullValue())
                .body("dateOfBirth.day", nullValue())
                .extract()
                .jsonPath()
                .getString("id");
        final String password = createPassword(userId);
        AuthenticationService.loginWithPassword(new LoginModel(user.getEmail(), new PasswordModel(password)), getSecretKey())
                .then()
                .statusCode(SC_OK)
                .body("token", not(emptyString()));
    }

    @Test
    public void PostUsers_InvalidName() {
        final UsersModel user = UsersModel.DefaultUsersModel()
                .setName(RandomStringUtils.randomAlphanumeric(25))
                .build();
        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PostUsers_InvalidSurname() {
        final UsersModel user = UsersModel.DefaultUsersModel()
                .setSurname(RandomStringUtils.randomAlphanumeric(25))
                .build();
        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PostUsers_InvalidEmail() {
        final UsersModel user = UsersModel.DefaultUsersModel()
                .setEmail(RandomStringUtils.randomAlphanumeric(10))
                .build();

        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PostUsers_EmailNotUnique_Conflict() {

        final UsersModel user = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK);

        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_UNIQUE"));
    }

    @Disabled("Irreproducible")
    @Test
    public void PostUsers_SecretTypeNotSupportedByProfile() {
        final UsersModel user = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("SECRET_TYPE_NOT_SUPPORTED_BY_PROFILE"));
    }

    @Test
    public void PostUsers_IdentityImpersonator_Forbidden() {

        final UsersModel user = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(user, getSecretKey(), getBackofficeImpersonateToken(getIdentityId(), getIdentityType()), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void PostUsers_InvalidDateOfBirthMonth_BadRequest() {
        final UsersModel user = UsersModel.DefaultUsersModel()
                .setDateOfBirth(new DateOfBirthModel(2101, 12, 11))
                .build();

        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PostUsers_InvalidDateOfBirthDay_BadRequest() {
        final UsersModel user = UsersModel.DefaultUsersModel()
                .setDateOfBirth(new DateOfBirthModel(1980, 11, 32))
                .build();

        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PostUsers_MobileNotUnique_Success() {

        final UsersModel user1 = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(user1, getSecretKey(), getAuthToken(), Optional.empty());

        final UsersModel user2 = UsersModel.DefaultUsersModel().setMobile(user1.getMobile()).build();
        UsersService.createUser(user2, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(user2.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(user2.getMobile().getNumber()));
    }

    @ParameterizedTest
    @MethodSource("emailProvider")
    public void PostUsers_EmailHavingApostropheOrSingleQuotes_Success(final String email) {

        final UsersModel user = UsersModel.DefaultUsersModel().setEmail(email).build();

        final String userId = UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("id");

        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.DEFAULT_PASSWORD)).build();

        PasswordsService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_OK);

        AuthenticationService.loginWithPassword(new LoginModel(email, new PasswordModel(TestHelper.DEFAULT_PASSWORD)), secretKey)
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().get("token");

        UsersHelper.verifyEmail(secretKey, email);
    }

    @Test
    public void PostUsers_InvalidEmailFormat_BadRequest() {
        final UsersModel user = UsersModel.DefaultUsersModel()
                .setEmail(String.format("%s.@weavrusertest.io", RandomStringUtils.randomAlphanumeric(6)))
                .build();

        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", ""})
    public void PostUsers_InvalidMobileNumber_BadRequest(final String mobileNumber) {
        final UsersModel user = UsersModel.DefaultUsersModel()
                .setMobile(new MobileNumberModel("+356", mobileNumber))
                .build();
        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PostUsers_NoMobileNumberCountryCode_BadRequest() {

        final UsersModel user = UsersModel.DefaultUsersModel()
                .setMobile(new MobileNumberModel("", "123456"))
                .build();
        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", "123", "+"})
    public void PostUsers_InvalidMobileNumberCountryCode_MobileOrCountryCodeInvalid(final String mobileNumberCountryCode) {

        final UsersModel user = UsersModel.DefaultUsersModel()
                .setMobile(new MobileNumberModel(mobileNumberCountryCode, "123456"))
                .build();
        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PostUsers_NoSecretKey_BadRequest() {

        final UsersModel user = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(user, "", getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void PostUsers_InvalidSecretKey_Unauthorised() {

        final UsersModel user = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(user, "abc", getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PostUsers_NoToken_Unauthorised() {

        final UsersModel user = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(user, getSecretKey(), "", Optional.empty())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PostUsers_InvalidToken_Unauthorised() {

        final UsersModel user = UsersModel.DefaultUsersModel().build();
        UsersService.createUser(user, getSecretKey(), "abc", Optional.empty())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void PostUsers_PatchMobileNumberCountryCodeCharacterLimitOne_BadRequest() {

        final UsersModel user = UsersModel.DefaultUsersModel()
                .setMobile(new MobileNumberModel("+1", String.format("82923%s", RandomStringUtils.randomNumeric(5))))
                .build();

        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(user.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(user.getMobile().getNumber()));
    }

    @Test
    public void PostUsers_PatchMobileNumberCountryCodeCharacterLimitTwoToFour_Success() {

        final UsersModel user = UsersModel.DefaultUsersModel()
                .setMobile(new MobileNumberModel("+49",
                        String.format("30%s", RandomStringUtils.randomNumeric(6))))
                .build();

        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(user.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(user.getMobile().getNumber()));
    }

    @Test
    public void PostUsers_PatchMobileNumberCountryCodeCharacterLimitFourToSix_Success() {

        final UsersModel user = UsersModel.DefaultUsersModel()
                .setMobile(new MobileNumberModel("+1829",
                        String.format("23%s", RandomStringUtils.randomNumeric(5))))
                .build();

        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(user.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(user.getMobile().getNumber()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"+1-1829", "001-1829"})
    public void PostUsers_PatchMobileNumberCountryCodeCharacterLimitMoreThanSix_BadRequest(final String countryCode) {

        final UsersModel user = UsersModel.DefaultUsersModel()
                .setMobile(new MobileNumberModel(countryCode, String.format("23%s", RandomStringUtils.randomNumeric(5))))
                .build();

        UsersService.createUser(user, getSecretKey(), getAuthToken(), Optional.empty())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("_embedded.errors[1].message", equalTo("request.mobile.countryCode: size must be between 1 and 6"));
    }


    protected static String getBackofficeImpersonateToken(final String identityId, final IdentityType identityType) {
        return BackofficeHelper.impersonateIdentity(identityId, identityType, secretKey);
    }

    protected static Stream<Arguments> emailProvider() {
        return Stream.of(
                arguments(String.format("%s's@weavrtest.io", RandomStringUtils.randomAlphabetic(5))),
                arguments(String.format("'%s'@weavrtest.io", RandomStringUtils.randomAlphabetic(5)))
        );
    }
}
