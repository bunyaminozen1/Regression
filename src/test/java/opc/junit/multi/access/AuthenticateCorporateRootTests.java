package opc.junit.multi.access;

import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.multi.users.UsersModel;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.simulator.AdditionalPropertiesModel;
import opc.models.simulator.TokenizeModel;
import opc.models.simulator.TokenizePropertiesModel;
import opc.services.multi.AuthenticationService;
import opc.services.multi.PasswordsService;
import opc.services.simulator.SimulatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static opc.enums.opc.UserType.ROOT;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

public class AuthenticateCorporateRootTests extends AbstractAuthenticationTests {

    private String identityId;
    private String corporateRootEmail;
    private String corporatePassword;

    @BeforeEach
    public void BeforeEach() {
        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        this.identityId = CorporatesHelper.createCorporate(createCorporateModel, secretKey);
        this.corporateRootEmail = createCorporateModel.getRootUser().getEmail();
        this.corporatePassword = CorporatesHelper.createCorporatePassword(identityId, secretKey);

        /**
         * Email verification necessity was removed temporarily because of DEV-6274-hotfix
         */
        //CorporatesHelper.verifyEmail(corporateRootEmail, secretKey);
    }

    @Test
    public void LoginWithPassword_Root_Success() {
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
    public void LoginWithPassword_ExpiredPassword_Conflict() {

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
    public void LoginWithPassword_User_Success() {
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
        final String identityToken = AuthenticationHelper.login(getLoginEmail(), getSecretKey());

        AuthenticationService.logout(secretKey, identityToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void LoginWithPassword_NotCreated_Forbidden() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        CorporatesHelper.createCorporate(createCorporateModel, secretKey);

        AuthenticationService.loginWithPassword(new LoginModel(createCorporateModel.getRootUser().getEmail(),
                new PasswordModel(TestHelper.getDefaultPassword(secretKey))), getSecretKey())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Override
    public String getSecretKey() {
        return secretKey;
    }

    @Override
    public String getLoginEmail() {
        return this.corporateRootEmail;
    }

    public String getLoginPassword() {
        return this.corporatePassword;
    }

    @Override
    protected String getIdentityId() {
        return this.identityId;
    }

    @Override
    protected IdentityType getIdentityType() {
        return IdentityType.CORPORATE;
    }

    @Override
    protected UserType getUserType() {
        return ROOT;
    }

    @Override
    protected String createPassword(final String userId) {
        final CreatePasswordModel createPasswordModel = CreatePasswordModel.newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        PasswordsService.createPassword(createPasswordModel, userId, secretKey);
        return createPasswordModel.getPassword().getValue();
    }
}
