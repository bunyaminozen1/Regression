package fpi.paymentrun.identities.buyerauthorisedusers;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import commons.enums.Roles;
import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.paymentrun.enums.AuthorisedUserRole;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.services.BuyersAuthorisedUsersService;
import opc.enums.opc.IdentityType;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

@Tag(PluginsTags.PAYMENT_RUN_AUTH_USERS)
public class CreateAuthorisedUsersTests extends BasePaymentRunSetup {

    private static Pair<String, String> buyer;

    @BeforeAll
    public static void TestSetup() {
        buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
    }

    @ParameterizedTest
    @EnumSource(AuthorisedUserRole.class)
    public void CreateUser_AdminRoleBuyer_Success(final AuthorisedUserRole authorisedUserRole) {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .roles(List.of(authorisedUserRole.name()))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", not(nullValue()))
                .body("buyerId", equalTo(buyer.getLeft()))
                .body("name", equalTo(userModel.getName()))
                .body("surname", equalTo(userModel.getSurname()))
                .body("email", equalTo(userModel.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(userModel.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(userModel.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(userModel.getDateOfBirth().getDay()))
                .body("mobile.countryCode", equalTo(userModel.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(userModel.getMobile().getNumber()))
                .body("roles[0]", equalTo(userModel.getRoles().get(0)));
    }

    @ParameterizedTest
    @EnumSource(AuthorisedUserRole.class)
    public void CreateUser_MultipleRolesBuyer_Success(final AuthorisedUserRole authorisedUserRole) {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .roles(List.of(authorisedUserRole.name()))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", not(nullValue()))
                .body("buyerId", equalTo(buyer.getLeft()))
                .body("name", equalTo(userModel.getName()))
                .body("surname", equalTo(userModel.getSurname()))
                .body("email", equalTo(userModel.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(userModel.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(userModel.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(userModel.getDateOfBirth().getDay()))
                .body("mobile.countryCode", equalTo(userModel.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(userModel.getMobile().getNumber()))
                .body("roles[0]", equalTo(userModel.getRoles().get(0)));
    }

    @Test
    public void CreateUser_AssignMultipleRoles_Success() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .roles(List.of(AuthorisedUserRole.CREATOR.name(), AuthorisedUserRole.CONTROLLER.name()))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", not(nullValue()))
                .body("buyerId", equalTo(buyer.getLeft()))
                .body("name", equalTo(userModel.getName()))
                .body("surname", equalTo(userModel.getSurname()))
                .body("email", equalTo(userModel.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(userModel.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(userModel.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(userModel.getDateOfBirth().getDay()))
                .body("mobile.countryCode", equalTo(userModel.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(userModel.getMobile().getNumber()))
                .body("roles[0]", equalTo(userModel.getRoles().get(0)))
                .body("roles[1]", equalTo(userModel.getRoles().get(1)));
    }

    @Test
    public void CreateUser_RequiredOnly_Success() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .mobile(null)
                        .dateOfBirth(null)
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", not(nullValue()))
                .body("buyerId", equalTo(buyer.getLeft()))
                .body("name", equalTo(userModel.getName()))
                .body("surname", equalTo(userModel.getSurname()))
                .body("email", equalTo(userModel.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", nullValue())
                .body("dateOfBirth.month", nullValue())
                .body("dateOfBirth.day", nullValue())
                .body("mobile.countryCode", nullValue())
                .body("mobile.number", nullValue())
                .body("roles[0]", equalTo(userModel.getRoles().get(0)));
    }

    @Test
    public void CreateUser_InvalidName_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .name(RandomStringUtils.randomAlphanumeric(25))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("name"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("SIZE"));
    }

    @Test
    public void CreateUser_NullName_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .name(null)
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("name"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void CreateUser_EmptyName_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .name("")
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.name: must not be blank"));
    }

    @Test
    public void CreateUser_InvalidSurname_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .surname(RandomStringUtils.randomAlphanumeric(25))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("surname"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("SIZE"));
    }

    @Test
    public void CreateUser_NullSurname_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .surname(null)
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("surname"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void CreateUser_EmptySurname_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .surname("")
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.surname: must not be blank"));
    }

    @Test
    public void CreateUser_InvalidEmail_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .email(RandomStringUtils.randomAlphanumeric(10))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void CreateUser_NullEmail_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .email(null)
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void CreateUser_EmptyEmail_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .email("")
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void CreateUser_BuyerWithoutStepUpToken_StepUpRequired() {
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyerToken)
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));
    }

    @Test
    public void CreateUser_CreateByAuthUser_Forbidden() {
        final String userToken = BuyerAuthorisedUserHelper.createEnrolledAuthenticatedUser(secretKey, buyer.getRight()).getRight();
        AuthenticationHelper.startStepup(secretKey, userToken);

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .build();

//        Admin role required
        BuyersAuthorisedUsersService.createUser(userModel, secretKey, userToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreateUser_EmailNotUnique_Conflict() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK);

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_UNIQUE"));
    }

    @Test
    public void CreateUser_IdentityImpersonator_Unauthorised() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, getBackofficeImpersonateToken(buyer.getLeft(), IdentityType.CORPORATE))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateUser_InvalidDateOfBirthYear_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .dateOfBirth(new DateOfBirthModel(2101, 12, 11))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params[0]", equalTo("dateOfBirth"))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("year"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_MOST"));
    }

    @Test
    public void CreateUser_InvalidDateOfBirthMonth_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .dateOfBirth(new DateOfBirthModel(1980, 13, 11))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params[0]", equalTo("dateOfBirth"))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("month"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_MOST"));
    }

    @Test
    public void CreateUser_InvalidDateOfBirthDay_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .dateOfBirth(new DateOfBirthModel(1980, 11, 32))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params[0]", equalTo("dateOfBirth"))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("day"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_MOST"));
    }

    @Test
    public void CreateUser_MobileNotUnique_Success() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", not(nullValue()))
                .body("buyerId", equalTo(buyer.getLeft()))
                .body("name", equalTo(userModel.getName()))
                .body("surname", equalTo(userModel.getSurname()))
                .body("email", equalTo(userModel.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(userModel.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(userModel.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(userModel.getDateOfBirth().getDay()))
                .body("mobile.countryCode", equalTo(userModel.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(userModel.getMobile().getNumber()))
                .body("roles[0]", equalTo(userModel.getRoles().get(0)));

        final BuyerAuthorisedUserModel userModelSameMobile =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .mobile(userModel.getMobile())
                        .build();

        BuyersAuthorisedUsersService.createUser(userModelSameMobile, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("id", not(nullValue()))
                .body("buyerId", equalTo(buyer.getLeft()))
                .body("name", equalTo(userModelSameMobile.getName()))
                .body("surname", equalTo(userModelSameMobile.getSurname()))
                .body("email", equalTo(userModelSameMobile.getEmail()))
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(userModelSameMobile.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(userModelSameMobile.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(userModelSameMobile.getDateOfBirth().getDay()))
                .body("mobile.countryCode", equalTo(userModel.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(userModel.getMobile().getNumber()))
                .body("roles[0]", equalTo(userModelSameMobile.getRoles().get(0)));
    }

    @Test
    public void CreateUser_InvalidEmailFormat_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .email(String.format("%s.@weavrusertest.io", RandomStringUtils.randomAlphanumeric(6)))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", ""})
    public void CreateUser_InvalidMobileNumber_BadRequest(final String mobileNumber) {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .mobile(new MobileNumberModel("+356", mobileNumber))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("number"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void CreateUser_EmptyMobileNumberCountryCode_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .mobile(new MobileNumberModel("", "123456"))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("countryCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void CreateUser_NullMobileNumberCountryCode_BadRequest() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .mobile(new MobileNumberModel(null, "123456"))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("countryCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"abc", "123", "+", "49"})
    public void CreateUser_InvalidMobileNumberCountryCode_MobileOrCountryCodeInvalid(final String mobileNumberCountryCode) {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .mobile(new MobileNumberModel(mobileNumberCountryCode, "123456"))
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("countryCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void CreateUser_InvalidToken_Unauthorised() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, RandomStringUtils.randomAlphabetic(10))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateUser_NoToken_Unauthorised() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, secretKey, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateUser_InvalidSecretKey_Unauthorised() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, RandomStringUtils.randomAlphabetic(10), buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateUser_NoSecretKey_Unauthorised() {

        final BuyerAuthorisedUserModel userModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .build();

        BuyersAuthorisedUsersService.createUser(userModel, "", buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreateUser_InvalidRoleName_BadRequest() {

        final BuyerAuthorisedUserModel createUserModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .roles(List.of(RandomStringUtils.randomAlphanumeric(25)))
                        .build();

        BuyersAuthorisedUsersService.createUser(createUserModel, secretKey, buyer.getRight())
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("roles", "0")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("roles"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void CreateUser_AdminRoleInModel_BadRequest() {

        final BuyerAuthorisedUserModel createUserModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .roles(List.of(Roles.ADMIN.name()))
                        .build();

        BuyersAuthorisedUsersService.createUser(createUserModel, secretKey, buyer.getRight())
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("roles", "0")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("roles"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void CreateUser_NullRoleName_BadRequest() {

        final BuyerAuthorisedUserModel createUserModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .roles(null)
                        .build();

        BuyersAuthorisedUsersService.createUser(createUserModel, secretKey, buyer.getRight())
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("roles"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void CreateUser_EmptyRoleName_BadRequest() {

        final BuyerAuthorisedUserModel createUserModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .roles(Collections.singletonList(""))
                        .build();

        BuyersAuthorisedUsersService.createUser(createUserModel, secretKey, buyer.getRight())
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("roles", "0")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("roles"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }
}
