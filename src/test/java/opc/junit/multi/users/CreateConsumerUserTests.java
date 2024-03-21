package opc.junit.multi.users;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.GetUsersFiltersModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.PasswordModel;
import opc.services.adminnew.AdminService;
import opc.services.innovator.InnovatorService;
import opc.services.multi.PasswordsService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

public class CreateConsumerUserTests extends AbstractCreateUserTests {

    private String consumerId;
    private String authenticationToken;
    private String consumerEmail;

    @BeforeEach
    public void BeforeEach() {
        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        consumerId = authenticatedConsumer.getLeft();
        authenticationToken = authenticatedConsumer.getRight();
        consumerEmail = createConsumerModel.getRootUser().getEmail();
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
        AdminService.getConsumerAllUsers(AdminService.loginAdmin(), getIdentityId(), Optional.of(filtersModel))
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", equalTo(userId))
                .body("user[0].name", equalTo(user.getName()))
                .body("user[0].surname", equalTo(user.getSurname()))
                .body("user[0].email", equalTo(user.getEmail()))
                .body("user[0].tag", equalTo(user.getTag()));

        InnovatorService.getConsumerAllUsers(InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword), getIdentityId(), Optional.of(filtersModel))
                .then()
                .statusCode(SC_OK)
                .body("user[0].id", equalTo(userId))
                .body("user[0].name", equalTo(user.getName()))
                .body("user[0].surname", equalTo(user.getSurname()))
                .body("user[0].email", equalTo(user.getEmail()))
                .body("user[0].tag", equalTo(user.getTag()));

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
        return this.consumerId;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CONSUMER;
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
        return this.consumerEmail;
    }

}
