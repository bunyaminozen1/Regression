package fpi.paymentrun.identities.buyerauthorisedusers;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.paymentrun.enums.AuthorisedUserRole;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.services.BuyersAuthorisedUsersService;
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
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN_AUTH_USERS)
public class GetAuthorisedUserTests extends BasePaymentRunSetup {

    private static Pair<String, String> buyer;

    @BeforeAll
    public static void TestSetup() {
        buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
    }
    
    @Test
    public void GetUser_AdminRoleRootUser_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKey, buyer.getRight())
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
                .body("surname", equalTo(user.getRight().getSurname()))
                .body("roles[0]", equalTo(user.getRight().getRoles().get(0)))
                .body("buyerId", equalTo(buyer.getLeft()));
    }

    @Test
    public void GetUser_MultipleRolesRootUser_Success() {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);

        final Pair<String, BuyerAuthorisedUserModel> user = BuyerAuthorisedUserHelper.createUser(secretKey, buyer.getRight());

        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKey, buyer.getRight())
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
                .body("surname", equalTo(user.getRight().getSurname()))
                .body("roles[0]", equalTo(user.getRight().getRoles().get(0)))
                .body("buyerId", equalTo(buyer.getLeft()));
    }

    @Test
    public void GetUser_AuthorisedUser_Forbidden() {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKey, user.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetUsers_AssigneeMoreThenOneRole_Success() {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createMultipleRolesAuthenticatedUser();

        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("active", equalTo(true))
                .body("dateOfBirth.year", equalTo(user.getMiddle().getDateOfBirth().getYear()))
                .body("dateOfBirth.month", equalTo(user.getMiddle().getDateOfBirth().getMonth()))
                .body("dateOfBirth.day", equalTo(user.getMiddle().getDateOfBirth().getDay()))
                .body("email", equalTo(user.getMiddle().getEmail()))
                .body("id", notNullValue())
                .body("mobile.countryCode", equalTo(user.getMiddle().getMobile().getCountryCode()))
                .body("mobile.number", equalTo(user.getMiddle().getMobile().getNumber()))
                .body("name", equalTo(user.getMiddle().getName()))
                .body("surname", equalTo(user.getMiddle().getSurname()))
                .body("roles[0]", equalTo(user.getMiddle().getRoles().get(1)))
                .body("roles[1]", equalTo(user.getMiddle().getRoles().get(0)))
                .body("buyerId", equalTo(buyer.getLeft()));
    }

    @Test
    public void GetUser_CrossRootIdentity_Forbidden() {

        final Pair<String, String> otherBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKey, otherBuyer.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetUser_CrossAuthorisedUser_Forbidden() {

        final Pair<String, String> otherBuyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
        final Pair<String, BuyerAuthorisedUserModel> user = BuyerAuthorisedUserHelper.createUser(secretKey, otherBuyer.getRight());

        final Triple<String, BuyerAuthorisedUserModel, String> user1 = createAuthenticatedUser();

        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKey, user1.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetUser_WrongToken_Unauthorized() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKey, RandomStringUtils.randomNumeric(18))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetUser_NoToken_Unauthorized() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKey, null)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetUser_UnknownUserId_NotFound() {

        BuyersAuthorisedUsersService.getUser(RandomStringUtils.randomNumeric(18), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetUser_InvalidUserId_BadRequest() {

        BuyersAuthorisedUsersService.getUser(RandomStringUtils.randomAlphabetic(18), secretKey, buyer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("user_id"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void GetUser_InvalidSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.getUser(user.getLeft(), RandomStringUtils.randomAlphabetic(10), buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetUser_OtherProgrammeSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.getUser(user.getLeft(), secretKeyAppTwo, buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetUser_NoSecretKey_Unauthorised() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.getUser(user.getLeft(), "", buyer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private Pair<String, BuyerAuthorisedUserModel> createUser() {
        return BuyerAuthorisedUserHelper.createUser(secretKey, buyer.getRight());
    }

    private Triple<String, BuyerAuthorisedUserModel, String> createAuthenticatedUser() {
        return BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyer.getRight());
    }

    private Triple<String, BuyerAuthorisedUserModel, String> createMultipleRolesAuthenticatedUser() {
        final BuyerAuthorisedUserModel createBuyerAuthorisedUserModel =
                BuyerAuthorisedUserModel.defaultUsersModel()
                        .roles(List.of(AuthorisedUserRole.CREATOR.name(), AuthorisedUserRole.CONTROLLER.name()))
                        .build();
        return BuyerAuthorisedUserHelper.createAuthenticatedUser(createBuyerAuthorisedUserModel, secretKey, buyer.getRight());
    }
}