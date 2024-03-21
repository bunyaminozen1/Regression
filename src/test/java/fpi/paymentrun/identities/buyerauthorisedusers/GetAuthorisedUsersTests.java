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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN_AUTH_USERS)
public class GetAuthorisedUsersTests extends BasePaymentRunSetup {

    private Pair<String, String> buyer;

    @BeforeEach
    public void TestSetup() {
        buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);
    }

    @Test
    public void GetUsers_RootUser_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user = createUser();

        BuyersAuthorisedUsersService.getUsers(secretKey, buyer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("users[0].active", equalTo(true))
                .body("users[0].dateOfBirth.year", equalTo(user.getRight().getDateOfBirth().getYear()))
                .body("users[0].dateOfBirth.month", equalTo(user.getRight().getDateOfBirth().getMonth()))
                .body("users[0].dateOfBirth.day", equalTo(user.getRight().getDateOfBirth().getDay()))
                .body("users[0].email", equalTo(user.getRight().getEmail()))
                .body("users[0].id", notNullValue())
                .body("users[0].mobile.countryCode", equalTo(user.getRight().getMobile().getCountryCode()))
                .body("users[0].mobile.number", equalTo(user.getRight().getMobile().getNumber()))
                .body("users[0].name", equalTo(user.getRight().getName()))
                .body("users[0].surname", equalTo(user.getRight().getSurname()))
                .body("users[0].buyerId", equalTo(buyer.getLeft()))
                .body("users[0].roles[0]", equalTo(user.getRight().getRoles().get(0)))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetUsers_MultipleRolesRootUser_Success() {
        final Pair<String, String> buyer = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey);

        final Pair<String, BuyerAuthorisedUserModel> user = BuyerAuthorisedUserHelper.createUser(secretKey, buyer.getRight());

        BuyersHelper.assignAllRoles(secretKey, buyer.getRight());

        BuyersAuthorisedUsersService.getUsers(secretKey, buyer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("users[0].active", equalTo(true))
                .body("users[0].dateOfBirth.year", equalTo(user.getRight().getDateOfBirth().getYear()))
                .body("users[0].dateOfBirth.month", equalTo(user.getRight().getDateOfBirth().getMonth()))
                .body("users[0].dateOfBirth.day", equalTo(user.getRight().getDateOfBirth().getDay()))
                .body("users[0].email", equalTo(user.getRight().getEmail()))
                .body("users[0].id", notNullValue())
                .body("users[0].mobile.countryCode", equalTo(user.getRight().getMobile().getCountryCode()))
                .body("users[0].mobile.number", equalTo(user.getRight().getMobile().getNumber()))
                .body("users[0].name", equalTo(user.getRight().getName()))
                .body("users[0].surname", equalTo(user.getRight().getSurname()))
                .body("users[0].buyerId", equalTo(buyer.getLeft()))
                .body("users[0].roles[0]", equalTo(user.getRight().getRoles().get(0)))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetUsers_AuthorisedUser_Forbidden() {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createAuthenticatedUser();

        BuyersAuthorisedUsersService.getUsers(secretKey, user.getRight(), Optional.empty())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetUsers_AssigneeMoreThenOneRole_Success() {

        final Triple<String, BuyerAuthorisedUserModel, String> user = createMultipleRolesAuthenticatedUser();

        BuyersAuthorisedUsersService.getUsers(secretKey, buyer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("users[0].active", equalTo(true))
                .body("users[0].dateOfBirth.year", equalTo(user.getMiddle().getDateOfBirth().getYear()))
                .body("users[0].dateOfBirth.month", equalTo(user.getMiddle().getDateOfBirth().getMonth()))
                .body("users[0].dateOfBirth.day", equalTo(user.getMiddle().getDateOfBirth().getDay()))
                .body("users[0].email", equalTo(user.getMiddle().getEmail()))
                .body("users[0].id", notNullValue())
                .body("users[0].mobile.countryCode", equalTo(user.getMiddle().getMobile().getCountryCode()))
                .body("users[0].mobile.number", equalTo(user.getMiddle().getMobile().getNumber()))
                .body("users[0].name", equalTo(user.getMiddle().getName()))
                .body("users[0].surname", equalTo(user.getMiddle().getSurname()))
                .body("users[0].buyerId", equalTo(buyer.getLeft()))
                .body("users[0].roles[0]", equalTo(user.getMiddle().getRoles().get(1)))
                .body("users[0].roles[1]", equalTo(user.getMiddle().getRoles().get(0)))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetUsers_SeveralUsers_Success() {

        final Pair<String, BuyerAuthorisedUserModel> user1 = createUser();
        final Pair<String, BuyerAuthorisedUserModel> user2 = createUser();

        BuyersAuthorisedUsersService.getUsers(secretKey, buyer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("users[0].active", equalTo(true))
                .body("users[0].dateOfBirth.year", equalTo(user1.getRight().getDateOfBirth().getYear()))
                .body("users[0].dateOfBirth.month", equalTo(user1.getRight().getDateOfBirth().getMonth()))
                .body("users[0].dateOfBirth.day", equalTo(user1.getRight().getDateOfBirth().getDay()))
                .body("users[0].email", equalTo(user1.getRight().getEmail()))
                .body("users[0].id", notNullValue())
                .body("users[0].mobile.countryCode", equalTo(user1.getRight().getMobile().getCountryCode()))
                .body("users[0].mobile.number", equalTo(user1.getRight().getMobile().getNumber()))
                .body("users[0].name", equalTo(user1.getRight().getName()))
                .body("users[0].surname", equalTo(user1.getRight().getSurname()))
                .body("users[0].buyerId", equalTo(buyer.getLeft()))
                .body("users[0].roles[0]", equalTo(user1.getRight().getRoles().get(0)))
                .body("users[1].active", equalTo(true))
                .body("users[1].dateOfBirth.year", equalTo(user2.getRight().getDateOfBirth().getYear()))
                .body("users[1].dateOfBirth.month", equalTo(user2.getRight().getDateOfBirth().getMonth()))
                .body("users[1].dateOfBirth.day", equalTo(user2.getRight().getDateOfBirth().getDay()))
                .body("users[1].email", equalTo(user2.getRight().getEmail()))
                .body("users[1].id", notNullValue())
                .body("users[1].mobile.countryCode", equalTo(user2.getRight().getMobile().getCountryCode()))
                .body("users[1].mobile.number", equalTo(user2.getRight().getMobile().getNumber()))
                .body("users[1].name", equalTo(user2.getRight().getName()))
                .body("users[1].surname", equalTo(user2.getRight().getSurname()))
                .body("users[1].buyerId", equalTo(buyer.getLeft()))
                .body("users[1].roles[0]", equalTo(user2.getRight().getRoles().get(0)))
                .body("count", equalTo(2))
                .body("responseCount", equalTo(2));
    }

    @Test
    public void GetUsers_OffsetLimitFiltersCheck_Success() {

        createUser();
        final Pair<String, BuyerAuthorisedUserModel> user2 = createUser();
        createUser();

        final Map<String, Object> filters = new HashMap<>();
        filters.put("offset", 1);
        filters.put("limit", 1);

        BuyersAuthorisedUsersService.getUsers(secretKey, buyer.getRight(), Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("users[0].active", equalTo(true))
                .body("users[0].dateOfBirth.year", equalTo(user2.getRight().getDateOfBirth().getYear()))
                .body("users[0].dateOfBirth.month", equalTo(user2.getRight().getDateOfBirth().getMonth()))
                .body("users[0].dateOfBirth.day", equalTo(user2.getRight().getDateOfBirth().getDay()))
                .body("users[0].email", equalTo(user2.getRight().getEmail()))
                .body("users[0].id", notNullValue())
                .body("users[0].mobile.countryCode", equalTo(user2.getRight().getMobile().getCountryCode()))
                .body("users[0].mobile.number", equalTo(user2.getRight().getMobile().getNumber()))
                .body("users[0].name", equalTo(user2.getRight().getName()))
                .body("users[0].surname", equalTo(user2.getRight().getSurname()))
                .body("users[0].buyerId", equalTo(buyer.getLeft()))
                .body("users[0].roles[0]", equalTo(user2.getRight().getRoles().get(0)))
                .body("count", equalTo(3))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetUsers_EmailFilterCheck_Success() {

        createUser();
        final Pair<String, BuyerAuthorisedUserModel> user2 = createUser();

        final Map<String, Object> filters = new HashMap<>();
        filters.put("email", user2.getRight().getEmail());

        BuyersAuthorisedUsersService.getUsers(secretKey, buyer.getRight(), Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("users[0].active", equalTo(true))
                .body("users[0].dateOfBirth.year", equalTo(user2.getRight().getDateOfBirth().getYear()))
                .body("users[0].dateOfBirth.month", equalTo(user2.getRight().getDateOfBirth().getMonth()))
                .body("users[0].dateOfBirth.day", equalTo(user2.getRight().getDateOfBirth().getDay()))
                .body("users[0].email", equalTo(user2.getRight().getEmail()))
                .body("users[0].id", notNullValue())
                .body("users[0].mobile.countryCode", equalTo(user2.getRight().getMobile().getCountryCode()))
                .body("users[0].mobile.number", equalTo(user2.getRight().getMobile().getNumber()))
                .body("users[0].name", equalTo(user2.getRight().getName()))
                .body("users[0].surname", equalTo(user2.getRight().getSurname()))
                .body("users[0].buyerId", equalTo(buyer.getLeft()))
                .body("users[0].roles[0]", equalTo(user2.getRight().getRoles().get(0)))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetUsers_ActiveFilterCheck_Success() {

        createUser();
        final Pair<String, BuyerAuthorisedUserModel> user2 = createUser();

        BuyerAuthorisedUserHelper.deactivateUser(user2.getLeft(), secretKey, buyer.getRight());

        final Map<String, Object> filters = new HashMap<>();
        filters.put("active", false);

        BuyersAuthorisedUsersService.getUsers(secretKey, buyer.getRight(), Optional.of(filters))
                .then()
                .statusCode(SC_OK)
                .body("users[0].active", equalTo(false))
                .body("users[0].dateOfBirth.year", equalTo(user2.getRight().getDateOfBirth().getYear()))
                .body("users[0].dateOfBirth.month", equalTo(user2.getRight().getDateOfBirth().getMonth()))
                .body("users[0].dateOfBirth.day", equalTo(user2.getRight().getDateOfBirth().getDay()))
                .body("users[0].email", equalTo(user2.getRight().getEmail()))
                .body("users[0].id", notNullValue())
                .body("users[0].mobile.countryCode", equalTo(user2.getRight().getMobile().getCountryCode()))
                .body("users[0].mobile.number", equalTo(user2.getRight().getMobile().getNumber()))
                .body("users[0].name", equalTo(user2.getRight().getName()))
                .body("users[0].surname", equalTo(user2.getRight().getSurname()))
                .body("users[0].buyerId", equalTo(buyer.getLeft()))
                .body("users[0].roles[0]", equalTo(user2.getRight().getRoles().get(0)))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetUsers_InvalidToken_Unauthorized() {
        BuyersAuthorisedUsersService.getUsers(secretKey, RandomStringUtils.randomNumeric(18), Optional.empty())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetUsers_NoUsers_Success() {
        final String otherBuyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        BuyersAuthorisedUsersService.getUsers(secretKey, otherBuyerToken, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetUsers_InvalidSecretKey_Unauthorised() {

        createUser();

        BuyersAuthorisedUsersService.getUsers(RandomStringUtils.randomAlphabetic(10), buyer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetUsers_OtherProgrammeSecretKey_Unauthorised() {

        createUser();

        BuyersAuthorisedUsersService.getUsers(secretKeyAppTwo, buyer.getRight(), Optional.empty())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetUsers_NoSecretKey_Unauthorised() {

        createUser();

        BuyersAuthorisedUsersService.getUsers("", buyer.getRight(), Optional.empty())
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
