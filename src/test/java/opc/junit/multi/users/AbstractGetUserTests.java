package opc.junit.multi.users;

import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.multi.users.UsersModel;
import opc.models.shared.PagingModel;
import opc.services.admin.AdminService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public abstract class AbstractGetUserTests extends AbstractUserTests {

    @Test
    public void GetUser_Success() {
        final User newUser = createNewUser();
        UsersService.getUser(getSecretKey(), newUser.id, getAuthToken())
                    .then()
                    .statusCode(SC_OK)
                    .body("id", equalTo(newUser.id))
                    .body("identity.id", equalTo(newUser.identityId))
                    .body("identity.type", equalTo(newUser.identityType.name()))
                    .body("name", equalTo(newUser.userDetails.getName()))
                    .body("surname", equalTo(newUser.userDetails.getSurname()))
                    .body("email", equalTo(newUser.userDetails.getEmail()))
                    .body("dateOfBirth.year", equalTo(newUser.userDetails.getDateOfBirth().getYear()))
                    .body("dateOfBirth.month", equalTo(newUser.userDetails.getDateOfBirth().getMonth()))
                    .body("dateOfBirth.day", equalTo(newUser.userDetails.getDateOfBirth().getDay()))
                    .body("active", equalTo(true));
    }

    @Test
    public void GetUser_NotFound() {
        UsersService.getUser(getSecretKey(), RandomStringUtils.randomNumeric(18), getAuthToken())
                    .then()
                    .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetUsers_Success() {
        final User user = createNewUser();

        final String newIdentityToken;

        if (user.identityType.equals(IdentityType.CORPORATE)) {
            newIdentityToken = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey).getRight();
        } else {
            newIdentityToken = CorporatesHelper.createAuthenticatedVerifiedCorporate(corporateProfileId, secretKey).getRight();
        }

        UsersHelper.createAuthenticatedUser(secretKey, newIdentityToken);

        UsersService.getUsers(getSecretKey(), Optional.empty(), getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("users[0].id", notNullValue())
                .body("users[0].identity.type", equalTo(user.identityType.name()))
                .body("users[0].identity.id", equalTo(user.identityId))
                .body("users[0].name", equalTo(user.userDetails.getName()))
                .body("users[0].surname", equalTo(user.userDetails.getSurname()))
                .body("users[0].email", equalTo(user.userDetails.getEmail()))
                .body("users[0].dateOfBirth.year", equalTo(user.userDetails.getDateOfBirth().getYear()))
                .body("users[0].dateOfBirth.month", equalTo(user.userDetails.getDateOfBirth().getMonth()))
                .body("users[0].dateOfBirth.day", equalTo(user.userDetails.getDateOfBirth().getDay()))
                .body("users[0].active", equalTo(true))
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1));
    }

    @Test
    public void GetUser_RootUser_NotFound() {
        final User user = createNewUser();
        UsersService.getUser(getSecretKey(), user.identityId, getAuthToken())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Disabled
    @ParameterizedTest
    @MethodSource("getArguments")
    public void GetUsers_Paging(final int count, final int limit) {

        final Set<String> createdUserIds = IntStream.range(1, count)
                                                    .mapToObj(i -> createNewUser())
                                                    .map(u -> u.id)
                                                    .collect(Collectors.toSet());

        int acc = 0;
        int itemCount = 1;
        final Set<String> seenUserIds = new HashSet<>();
        while (acc < itemCount) {

            final PagingModel pagingModel = new PagingModel(acc, limit);
            final Map<String, Object> filters = new HashMap<>();
            filters.put("paging[offset]", pagingModel.getOffset());
            filters.put("paging[limit]", pagingModel.getLimit());

            final ValidatableResponse response = UsersService.getUsers(getSecretKey(), Optional.of(filters), getAuthToken())
                                                             .then()
                                                             .statusCode(SC_OK)
                                                             .body("count", equalTo(count))
                                                             .body("responseCount", lessThanOrEqualTo(limit));

            final JsonPath jsonPath = response.extract().jsonPath();
            itemCount = jsonPath.getInt("count");
            acc += jsonPath.getInt("responseCount");

            final List<Map<String, String>> usersResponse = jsonPath.getJsonObject("users");
            seenUserIds.addAll(usersResponse.stream().map(m -> m.get("id")).collect(Collectors.toSet()));
        }

        Assertions.assertEquals(itemCount, seenUserIds.size());

        final long disjunc = Stream.concat(createdUserIds.stream(), seenUserIds.stream())
                                   .distinct()
                                   .filter(e -> !createdUserIds.contains(e) || !seenUserIds.contains(e))
                                   .count();

        Assertions.assertEquals(1, disjunc, String.format("%s Missing or unexpected users in response", disjunc)); // 1 expected because of root

    }

    // user count + limit per page
    private static Stream<Arguments> getArguments() {
        final Random random = new Random();
        return random.ints(5, 0, 20)
                     .mapToObj(i -> Arguments.of(i, 1 + random.nextInt(i * 2)));
    }

    @Test
    public void GetUsers_FilterActiveInactive_Success() {

        final User firstUser = createNewUser();
        final User secondUser = createNewUser();

        final String adminToken = AdminService.loginAdmin();
        if(firstUser.identityType.equals(IdentityType.CONSUMER)){
            AdminHelper.deactivateConsumerUser(getIdentityId(), firstUser.id, adminToken);
        }else {
            AdminHelper.deactivateCorporateUser(getIdentityId(), firstUser.id, adminToken);
        }

        final Map<String, Object> filter = new HashMap<>();
        filter.put("active", false);
        UsersService.getUsers(getSecretKey(), Optional.of(filter), getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1))
                .body("users[0].email", equalTo(firstUser.userDetails.getEmail()))
                .body("users[0].id", equalTo(firstUser.id));

        filter.put("active", true);
        UsersService.getUsers(getSecretKey(), Optional.of(filter), getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1))
                .body("users[0].email", equalTo(secondUser.userDetails.getEmail()))
                .body("users[0].id", equalTo(secondUser.id));
    }

    @Test
    public void GetUsers_FilterByEmail_Success() {
        final User firstUser = createNewUser();
        final User secondUser = createNewUser();

        final Map<String, Object> filter = new HashMap<>();
        filter.put("email", firstUser.userDetails.getEmail());
        UsersService.getUsers(getSecretKey(), Optional.of(filter), getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1))
                .body("users[0].email", equalTo(firstUser.userDetails.getEmail()))
                .body("users[0].id", equalTo(firstUser.id));

        filter.put("email", secondUser.userDetails.getEmail());
        UsersService.getUsers(getSecretKey(), Optional.of(filter), getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1))
                .body("users[0].email", equalTo(secondUser.userDetails.getEmail()))
                .body("users[0].id", equalTo(secondUser.id));
    }

    @Test
    public void GetUsers_FilterByTag_Success() {

        final UsersModel firstUserModel = UsersModel.DefaultUsersModel().setTag(RandomStringUtils.randomAlphanumeric(5)).build();
        final Pair<String, String> firstUserWithTag = UsersHelper.createAuthenticatedUser(firstUserModel, secretKey, getAuthToken());

        final UsersModel secondUserModel = UsersModel.DefaultUsersModel().setTag(RandomStringUtils.randomAlphabetic(5)).build();
        final Pair<String, String> secondUserWithTag = UsersHelper.createAuthenticatedUser(secondUserModel, secretKey, getAuthToken());

        final Map<String, Object> filter = new HashMap<>();
        filter.put("tag", firstUserModel.getTag());
        UsersService.getUsers(getSecretKey(), Optional.of(filter), getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1))
                .body("users[0].email", equalTo(firstUserModel.getEmail()))
                .body("users[0].id", equalTo(firstUserWithTag.getLeft()))
                .body("users[0].tag", equalTo(firstUserModel.getTag()));


        filter.put("tag", secondUserModel.getTag());
        UsersService.getUsers(getSecretKey(), Optional.of(filter), getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(1))
                .body("responseCount", equalTo(1))
                .body("users[0].email", equalTo(secondUserModel.getEmail()))
                .body("users[0].id", equalTo(secondUserWithTag.getLeft()))
                .body("users[0].tag", equalTo(secondUserModel.getTag()));

        filter.put("tag", RandomStringUtils.randomAlphanumeric(5));
        UsersService.getUsers(getSecretKey(), Optional.of(filter), getAuthToken())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));
    }

    @Test
    public void GetUser_IdentityImpersonator_Forbidden() {
        final User newUser = createNewUser();

        UsersService.getUser(getSecretKey(), newUser.id, getBackofficeImpersonateToken(newUser.identityId, newUser.identityType))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetUsers_IdentityImpersonator_Forbidden() {
        final User newUser = createNewUser();

        UsersService.getUsers(getSecretKey(), Optional.empty(), getBackofficeImpersonateToken(newUser.identityId, newUser.identityType))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetUser_CrossIdentityType_NotFound() {
        final User newUser = createNewUser();
        final String newIdentityToken;

        if (newUser.identityType.equals(IdentityType.CORPORATE)) {
            newIdentityToken = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey).getRight();
        } else {
            newIdentityToken = CorporatesHelper.createAuthenticatedVerifiedCorporate(corporateProfileId, secretKey).getRight();
        }

        UsersService.getUser(getSecretKey(), newUser.id, newIdentityToken)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void GetUser_CrossIdentity_Forbidden() {
        final User newUser = createNewUser();
        final String newIdentityToken;

        if (newUser.identityType.equals(IdentityType.CONSUMER)) {
            newIdentityToken = ConsumersHelper.createAuthenticatedVerifiedConsumer(consumerProfileId, secretKey).getRight();
        } else {
            newIdentityToken = CorporatesHelper.createAuthenticatedVerifiedCorporate(corporateProfileId, secretKey).getRight();
        }

        UsersService.getUser(getSecretKey(), newUser.id, newIdentityToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    protected abstract String getIdentityId();
}
