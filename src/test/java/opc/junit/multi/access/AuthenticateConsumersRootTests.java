package opc.junit.multi.access;

import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.consumers.ConsumerRootUserModel;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.EmailVerificationModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.shared.SendEmailVerificationModel;
import opc.models.simulator.AdditionalPropertiesModel;
import opc.models.simulator.TokenizeModel;
import opc.models.simulator.TokenizePropertiesModel;
import opc.services.multi.AuthenticationService;
import opc.services.multi.ConsumersService;
import opc.services.multi.PasswordsService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * With the latest changes related to consumer login flow success login will be possible only after email verification,
 * if email was not verified a consumer user get SC_FORBIDDEN 403
 * Main ticket: https://weavr-payments.atlassian.net/browse/DEV-5756
 */

public class AuthenticateConsumersRootTests extends AbstractAuthenticationTests {

    private String identityId;
    private String consumerRootEmail;
    private String consumerPassword;

    @BeforeEach
    public void BeforeEach() {
        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        this.identityId = ConsumersHelper.createConsumer(createConsumerModel, secretKey);
        this.consumerRootEmail = createConsumerModel.getRootUser().getEmail();
        this.consumerPassword = ConsumersHelper.createConsumerPassword(identityId, secretKey);

        ConsumersHelper.verifyEmail(consumerRootEmail, secretKey);
    }

    @Test
    public void LoginWithPassword_RootUserVerifiedEmail_Success() {
        ConsumersHelper.verifyEmail(consumerRootEmail, secretKey);

        AuthenticationService.loginWithPassword(new LoginModel(getLoginEmail(), new PasswordModel(getLoginPassword())), getSecretKey())
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("credentials.type", equalTo(getUserType().name()))
                .body("credentials.id", equalTo(getIdentityId()));
    }

    @Test
    public void LoginWithPassword_TokenizedPassword_Success() {
        ConsumersHelper.verifyEmail(consumerRootEmail, secretKey);

        final String token =
                AuthenticationService.loginWithPassword(new LoginModel(getLoginEmail(), new PasswordModel(getLoginPassword())), getSecretKey())
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath().get("token");

        final String tokenizedPassword =
                SimulatorService.tokenize(secretKey,
                                new TokenizeModel(new TokenizePropertiesModel(new AdditionalPropertiesModel(getLoginPassword(), "PASSWORD"))), token)
                        .then().statusCode(SC_OK).extract().jsonPath().get("tokens.additionalProp1");

        AuthenticationService.loginWithPassword(new LoginModel(getLoginEmail(), new PasswordModel(tokenizedPassword)), getSecretKey())
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("credentials.type", equalTo(getUserType().name()))
                .body("credentials.id", equalTo(getIdentityId()));
    }

    @Test
    public void LoginWithPassword_ExpiredPassword_Success() {
        ConsumersHelper.verifyEmail(consumerRootEmail, secretKey);

        final String credentialsId = AuthenticationService.loginWithPassword(new LoginModel(getLoginEmail(), new PasswordModel(getLoginPassword())), getSecretKey())
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .get("credentials.id");

        SimulatorHelper.simulateSecretExpiry(getSecretKey(), credentialsId);

        AuthenticationService.loginWithPassword(new LoginModel(getLoginEmail(), new PasswordModel(getLoginPassword())), getSecretKey())
                .then()
                .statusCode(SC_CONFLICT)
                .body("token", notNullValue());
    }

    @Test
    public void LoginWithPassword_UserVerifiedEmail_Success() {
        ConsumersHelper.verifyEmail(consumerRootEmail, secretKey);

        final String rootToken =
                AuthenticationService.loginWithPassword(new LoginModel(getLoginEmail(), new PasswordModel(getLoginPassword())), getSecretKey())
                        .then()
                        .statusCode(SC_OK)
                        .extract()
                        .jsonPath()
                        .get("token");

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final String userId = UsersHelper.createUser(usersModel, getSecretKey(), rootToken);
        final String userPassword = createPassword(userId);

        AuthenticationService.loginWithPassword(new LoginModel(usersModel.getEmail(), new PasswordModel(userPassword)), getSecretKey())
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("identity.id", equalTo(getIdentityId()))
                .body("credentials.type", equalTo(UserType.USER.name()))
                .body("credentials.id", equalTo(userId));
    }

    @Test
    public void Logout_Success() {
        ConsumersHelper.verifyEmail(consumerRootEmail, secretKey);

        final String identityToken = AuthenticationHelper.login(getLoginEmail(), getSecretKey());

        AuthenticationService.logout(secretKey, identityToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void LoginWithPassword_NotCreated_Forbidden() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .build())
                .build();
        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty());

        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(createConsumerModel.getRootUser().getEmail()), getSecretKey())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.loginWithPassword(new LoginModel(createConsumerModel.getRootUser().getEmail(),
                new PasswordModel(TestHelper.getDefaultPassword(secretKey))), getSecretKey())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void LoginWithPassword_RootVerifiedEmailLoginAfterExpiresLimit_Success() throws InterruptedException {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .build())
                .build();
        final Pair<String, String> consumerId = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        AuthenticationService.loginWithPassword(new LoginModel(createConsumerModel.getRootUser().getEmail(), new PasswordModel(getLoginPassword())), getSecretKey())
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("identity.id", equalTo(consumerId.getLeft()))
                .body("credentials.type", equalTo(getUserType().name()))
                .body("credentials.id", equalTo(consumerId.getLeft()));

//      Login after exceeding the verification limit
        TimeUnit.SECONDS.sleep(90);

        AuthenticationService.loginWithPassword(new LoginModel(createConsumerModel.getRootUser().getEmail(), new PasswordModel(getLoginPassword())), getSecretKey())
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("identity.id", equalTo(consumerId.getLeft()))
                .body("credentials.type", equalTo(getUserType().name()))
                .body("credentials.id", equalTo(consumerId.getLeft()));
    }

    @Test
    public void LoginRootWithPassword_VerifiedEmailByAnotherUser_Success() throws InterruptedException {
//      Create a first consumer and do not verify the email
        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .build())
                .build();
        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty());

//      Create a new consumer with the same Email and verify that Email after exceeding the verification limit of first consumer
        TimeUnit.SECONDS.sleep(90);

        final CreateConsumerModel createNewConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                .setEmail(createConsumerModel.getRootUser().getEmail())
                .build())
                .build();

        final String newConsumerPassword = "Pass12345";
        final Pair<String, String> newConsumer = ConsumersHelper.createAuthenticatedConsumerWithSetupPassword(createNewConsumerModel, secretKey, newConsumerPassword);

//      Login of second consumer after verification
        AuthenticationService.loginWithPassword(new LoginModel(createConsumerModel.getRootUser().getEmail(), new PasswordModel(newConsumerPassword)), getSecretKey())
                .then()
                .statusCode(SC_OK)
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("identity.id", equalTo(newConsumer.getLeft()))
                .body("credentials.type", equalTo(getUserType().name()))
                .body("credentials.id", equalTo(newConsumer.getLeft()));

//      Login after exceeding the verification limit
        TimeUnit.SECONDS.sleep(90);

        AuthenticationService.loginWithPassword(new LoginModel(createConsumerModel.getRootUser().getEmail(), new PasswordModel(newConsumerPassword)), getSecretKey())
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("identity.type", equalTo(getIdentityType().name()))
                .body("identity.id", equalTo(newConsumer.getLeft()))
                .body("credentials.type", equalTo(getUserType().name()))
                .body("credentials.id", equalTo(newConsumer.getLeft()));

//      Login of first consumer
        AuthenticationService.loginWithPassword(new LoginModel(createConsumerModel.getRootUser().getEmail(), new PasswordModel(TestHelper.getDefaultPassword(secretKey))), getSecretKey())
                .then()
                .statusCode(SC_FORBIDDEN);

    }

    @Test
    public void LoginWithPassword_RootUnverifiedEmail_Forbidden() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId)
                .setRootUser(ConsumerRootUserModel.DefaultRootUserModel()
                        .build())
                .build();
        ConsumersService.createConsumer(createConsumerModel, secretKey, Optional.empty());

        AuthenticationService.loginWithPassword(new LoginModel(createConsumerModel.getRootUser().getEmail(), new PasswordModel(TestHelper.getDefaultPassword(secretKey))), getSecretKey())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Override
    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public String getLoginEmail() {
        return this.consumerRootEmail;
    }

    public String getLoginPassword() {
        return this.consumerPassword;
    }

    @Override
    protected String getIdentityId() {
        return this.identityId;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CONSUMER;
    }

    @Override
    protected UserType getUserType() {
        return UserType.ROOT;
    }

    protected String verifyEmail (final String userEmail)
    {
        ConsumersService.sendEmailVerification(new SendEmailVerificationModel(userEmail), getSecretKey())
                .then()
                .statusCode(SC_NO_CONTENT);

        final EmailVerificationModel emailVerificationModel =
                new EmailVerificationModel(userEmail, TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyEmail(emailVerificationModel, getSecretKey())
                .then()
                .statusCode(SC_NO_CONTENT);
        return userEmail;
    }
    @Override
    protected String createPassword(final String userId) {
        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, userId, secretKey);
        return createPasswordModel.getPassword().getValue();
    }
}
