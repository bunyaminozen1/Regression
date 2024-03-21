package fpi.paymentrun.identities.buyerauthorisedusers;

import commons.enums.Roles;
import commons.models.DateOfBirthModel;
import commons.models.MobileNumberModel;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.services.AuthenticationService;
import fpi.paymentrun.services.BuyersAuthorisedUsersService;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN_AUTH_USERS)
public class UpdateAuthorisedUserTests extends BasePaymentRunSetup {

    private static Pair<String, String> buyer;

    @BeforeAll
    public static void TestSetup() {
        buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
    }

    @Test
    public void UpdateUser_AllFieldsAdminRoleBuyer_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(updateUserModel.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(updateUserModel.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(updateUserModel.getDateOfBirth().getDay()))
                .body("email", equalTo(updateUserModel.getEmail()))
                .body("id", notNullValue())
                .body("mobile.countryCode", equalTo(updateUserModel.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(updateUserModel.getMobile().getNumber()))
                .body("name", equalTo(updateUserModel.getName()))
                .body("surname", equalTo(updateUserModel.getSurname()))
                .body("buyerId", equalTo(buyer.getLeft()))
                .body("roles[0]", equalTo(updateUserModel.getRoles().get(0)))
                .extract()
                .jsonPath()
                .getString("id");

        final String userPassword = AuthenticationHelper.createUserPassword(user.getLeft(), secretKey);

        AuthenticationService.loginWithPassword(new LoginModel(user.getRight().getEmail(), new PasswordModel(userPassword)), secretKey)
                .then()
                .statusCode(SC_FORBIDDEN);

        AuthenticationService.loginWithPassword(new LoginModel(updateUserModel.getEmail(), new PasswordModel(userPassword)), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void UpdateUser_MultipleRolesBuyer_Success() {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        final Pair<String, BuyerAuthorisedUserModel> user = BuyerAuthorisedUserHelper.createUser(secretKey, buyer.getRight());

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(updateUserModel.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(updateUserModel.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(updateUserModel.getDateOfBirth().getDay()))
                .body("email", equalTo(updateUserModel.getEmail()))
                .body("id", notNullValue())
                .body("mobile.countryCode", equalTo(updateUserModel.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(updateUserModel.getMobile().getNumber()))
                .body("name", equalTo(updateUserModel.getName()))
                .body("surname", equalTo(updateUserModel.getSurname()))
                .body("buyerId", equalTo(buyer.getLeft()))
                .body("roles[0]", equalTo(updateUserModel.getRoles().get(0)))
                .extract()
                .jsonPath()
                .getString("id");

        final String userPassword = AuthenticationHelper.createUserPassword(user.getLeft(), secretKey);

        AuthenticationService.loginWithPassword(new LoginModel(user.getRight().getEmail(), new PasswordModel(userPassword)), secretKey)
                .then()
                .statusCode(SC_FORBIDDEN);

        AuthenticationService.loginWithPassword(new LoginModel(updateUserModel.getEmail(), new PasswordModel(userPassword)), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue());
    }

    @Test
    public void UpdateUser_AuthorisedUserToken_Forbidden() {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, user.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UpdateUser_AssignMultipleRoles_Success() {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.allRolesUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("roles", equalTo(List.of(Roles.CONTROLLER.name(), Roles.CREATOR.name())));
    }

    @Test
    public void UpdateUser_NoRoles_BadRequest() {
        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .roles(Collections.singletonList(""))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("roles", "0")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("roles"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void UpdateUser_InvalidRole_BadRequest() {
        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .roles(Collections.singletonList("abc"))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("roles", "0")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("roles"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void UpdateUser_AdminRole_BadRequest() {
        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .roles(Collections.singletonList(Roles.ADMIN.name()))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("roles", "0")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("roles"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void UpdateUser_Name_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .name(RandomStringUtils.randomAlphabetic(5))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(user.getRight().getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(user.getRight().getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(user.getRight().getDateOfBirth().getDay()))
                .body("email", equalTo(user.getRight().getEmail()))
                .body("id", notNullValue())
                .body("mobile.countryCode", equalTo(user.getRight().getMobile().getCountryCode()))
                .body("mobile.number", equalTo(user.getRight().getMobile().getNumber()))
                .body("name", equalTo(updateUserModel.getName()))
                .body("surname", equalTo(user.getRight().getSurname()))
                .body("buyerId", equalTo(buyer.getLeft()))
                .body("roles[0]", equalTo(user.getRight().getRoles().get(0)));
    }

    @Test
    public void UpdateUser_Surname_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .surname(RandomStringUtils.randomAlphabetic(5))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(user.getRight().getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(user.getRight().getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(user.getRight().getDateOfBirth().getDay()))
                .body("email", equalTo(user.getRight().getEmail()))
                .body("id", notNullValue())
                .body("mobile.countryCode", equalTo(user.getRight().getMobile().getCountryCode()))
                .body("mobile.number", equalTo(user.getRight().getMobile().getNumber()))
                .body("name", equalTo(user.getRight().getName()))
                .body("surname", equalTo(updateUserModel.getSurname()))
                .body("buyerId", equalTo(buyer.getLeft()))
                .body("roles[0]", equalTo(user.getRight().getRoles().get(0)));
    }

    @Test
    public void UpdateUser_DateOfBirth_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .dateOfBirth(new DateOfBirthModel(1981, 11, 11))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(updateUserModel.getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(updateUserModel.getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(updateUserModel.getDateOfBirth().getDay()))
                .body("email", equalTo(user.getRight().getEmail()))
                .body("id", notNullValue())
                .body("mobile.countryCode", equalTo(user.getRight().getMobile().getCountryCode()))
                .body("mobile.number", equalTo(user.getRight().getMobile().getNumber()))
                .body("name", equalTo(user.getRight().getName()))
                .body("surname", equalTo(user.getRight().getSurname()))
                .body("buyerId", equalTo(buyer.getLeft()))
                .body("roles[0]", equalTo(user.getRight().getRoles().get(0)));
    }

    @Test
    public void UpdateUser_MobileNumber_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .mobile(MobileNumberModel.random()).build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(user.getRight().getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(user.getRight().getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(user.getRight().getDateOfBirth().getDay()))
                .body("email", equalTo(user.getRight().getEmail()))
                .body("id", notNullValue())
                .body("mobile.countryCode", equalTo(updateUserModel.getMobile().getCountryCode()))
                .body("mobile.number", equalTo(updateUserModel.getMobile().getNumber()))
                .body("name", equalTo(user.getRight().getName()))
                .body("surname", equalTo(user.getRight().getSurname()))
                .body("buyerId", equalTo(buyer.getLeft()))
                .body("roles[0]", equalTo(user.getRight().getRoles().get(0)))
                .extract()
                .jsonPath()
                .getString("id");
    }

    @Test
    public void UpdateUser_InvalidName_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .name((RandomStringUtils.randomAlphanumeric(25)))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("name"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("SIZE"));
    }

    @Test
    public void UpdateUser_InvalidSurname_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .surname((RandomStringUtils.randomAlphanumeric(25)))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("surname"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("SIZE"));
    }

    @Test
    public void UpdateUser_InvalidEmail_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .email((RandomStringUtils.randomAlphanumeric(25)))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void UpdateUser_InvalidEmailFormat_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .email(String.format("%s.@weavrusertest.io", RandomStringUtils.randomAlphanumeric(6)))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void UpdateUser_EmailNotUnique_Conflict() {
        final Pair<String, BuyerAuthorisedUserModel> user1 = createUser();
        final Pair<String, BuyerAuthorisedUserModel> user2 = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .email(user1.getRight().getEmail())
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user2.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("EMAIL_NOT_UNIQUE"));
    }

    @Test
    public void UpdateUser_RandomUser_NotFound() {
        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, RandomStringUtils.randomNumeric(10), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void UpdateUser_InvalidDateOfBirth_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .dateOfBirth(new DateOfBirthModel(1990, 11, 32))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("dateOfBirth")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("day"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_MOST"));
    }

    @Test
    public void UpdateUser_InvalidMonthOfBirth_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .dateOfBirth(new DateOfBirthModel(1990, 13, 30))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("dateOfBirth")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("month"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_MOST"));
    }

    @Test
    public void UpdateUser_InvalidYearOfBirth_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .dateOfBirth(new DateOfBirthModel(9999, 12, 30))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("dateOfBirth")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("year"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("AT_MOST"));
    }

    @Test()
    public void UpdateUser_NoMobileNumber_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .mobile(new MobileNumberModel("+356", ""))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("number"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"))
                .body("syntaxErrors.invalidFields[1].params", equalTo(List.of("mobile")))
                .body("syntaxErrors.invalidFields[1].fieldName", equalTo("number"))
                .body("syntaxErrors.invalidFields[1].error", equalTo("SIZE"));
    }

    @Test()
    public void UpdateUser_InvalidMobileNumber_BadRequest() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.builder()
                .mobile(new MobileNumberModel("+356", "abc"))
                .build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("mobile")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("number"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void UpdateUser_SeveralUsersSameNumber_Success() {
        final Pair<String, BuyerAuthorisedUserModel> user1 = createUser();
        final Pair<String, BuyerAuthorisedUserModel> user2 = createUser();

        final BuyerAuthorisedUserModel buyerAuthorisedUserModel =
                BuyerAuthorisedUserModel.builder()
                        .mobile(user1.getRight().getMobile())
                        .build();

        BuyersAuthorisedUsersService.updateUser(buyerAuthorisedUserModel, user2.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("mobile.countryCode", equalTo(user1.getRight().getMobile().getCountryCode()))
                .body("mobile.number", equalTo(user1.getRight().getMobile().getNumber()));
    }

    @Test
    public void UpdateUser_UnknownUserId_NotFound() {
        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, RandomStringUtils.randomNumeric(18), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void UpdateUser_NoUserId_NotFound() {
        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, "", secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void UpdateUser_InvalidUserId_BadRequest() {
        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, RandomStringUtils.randomAlphabetic(18), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("user_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void UpdateUser_InvalidSecretKey_Unauthorised() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), RandomStringUtils.randomAlphabetic(10), buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdateUser_OtherProgrammeSecretKey_Unauthorised() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKeyAppTwo, buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdateUser_NoSecretKey_Unauthorised() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), "", buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdateUser_OtherBuyerRootToken_Forbidden() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        final String otherBuyerToken =
                BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, otherBuyerToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UpdateUser_OtherBuyerAuthorisedUserToken_Forbidden() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();
        final String otherBuyerToken =
                BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String otherUserToken =
                BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, otherBuyerToken).getRight();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, otherUserToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void UpdateUser_InvalidToken_Unauthorised() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, RandomStringUtils.randomAlphabetic(10))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void UpdateUser_NoToken_Unauthorised() {
        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        final BuyerAuthorisedUserModel updateUserModel = BuyerAuthorisedUserModel.defaultUsersModel().build();

        BuyersAuthorisedUsersService.updateUser(updateUserModel, user.getLeft(), secretKey, "")
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
