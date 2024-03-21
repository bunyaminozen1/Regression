package fpi.paymentrun.authentication;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.services.AuthenticationService;
import opc.enums.opc.IdentityType;
import opc.enums.opc.UserType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.shared.LoginModel;
import opc.models.shared.PasswordModel;
import opc.models.simulator.AdditionalPropertiesModel;
import opc.models.simulator.TokenizeModel;
import opc.models.simulator.TokenizePropertiesModel;
import opc.services.innovator.InnovatorService;
import opc.services.simulator.SimulatorService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN_AUTHENTICATION)
public class LoginWithPasswordTests extends BasePaymentRunSetup {
    private String userId;
    private String userEmail;
    private String userPassword;

    @BeforeEach
    public void Setup() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        userId = BuyersHelper.createBuyer(createBuyerModel, secretKey);
        userEmail = createBuyerModel.getAdminUser().getEmail();
        BuyersHelper.verifyEmail(userEmail, secretKey);
        userPassword = AuthenticationHelper.createUserPassword(userId, secretKey);
    }

    @Test
    public void LoginWithPassword_AdminUser_Success() {
        AuthenticationService.loginWithPassword(new LoginModel(userEmail, new PasswordModel(userPassword)), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("identity.id", equalTo(userId))
                .body("identity.type", equalTo(IdentityType.BUYER.name()))
                .body("credentials.type", equalTo(UserType.ADMIN.name()))
                .body("credentials.id", equalTo(userId));
    }

    @Test
    public void LoginWithPassword_TokenizedPassword_Success() {
        final String token =
                AuthenticationService.loginWithPassword(new LoginModel(userEmail, new PasswordModel(userPassword)), secretKey)
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath().get("token");

        final String tokenizedPassword =
                SimulatorService.tokenize(secretKey,
                                new TokenizeModel(new TokenizePropertiesModel(new AdditionalPropertiesModel(userPassword, "PASSWORD"))), token)
                        .then().statusCode(SC_OK).extract().jsonPath().get("tokens.additionalProp1");

        AuthenticationService.loginWithPassword(new LoginModel(userEmail, new PasswordModel(tokenizedPassword)), secretKey)
                .then()
                .statusCode(SC_OK)
                .body("token", notNullValue())
                .body("identity.id", equalTo(userId))
                .body("identity.type", equalTo(IdentityType.BUYER.name()))
                .body("credentials.type", equalTo(UserType.ADMIN.name()))
                .body("credentials.id", equalTo(userId));
    }

    @Test
    public void LoginWithPassword_ExpiredPassword_Conflict() {
        final String credentialsId =
                AuthenticationService.loginWithPassword(new LoginModel(userEmail, new PasswordModel(userPassword)), secretKey)
                        .then()
                        .statusCode(SC_OK)
                        .extract().jsonPath().get("credentials.id");

        SimulatorHelper.simulateSecretExpiry(secretKey, credentialsId);

        AuthenticationService.loginWithPassword(new LoginModel(userEmail, new PasswordModel(userPassword)), secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("token", notNullValue());
    }

    @Test
    public void LoginWithPassword_NotCreated_Forbidden() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        BuyersHelper.createBuyer(createBuyerModel, secretKey);

        AuthenticationService.loginWithPassword(new LoginModel(createBuyerModel.getAdminUser().getEmail(),
                        new PasswordModel(TestHelper.getDefaultPassword(secretKey))), secretKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void LoginWithPassword_InvalidEmail_BadRequest() {
        AuthenticationService.loginWithPassword(new LoginModel(RandomStringUtils.randomAlphanumeric(10),
                        new PasswordModel(userPassword)), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void LoginWithPassword_NullEmail_BadRequest() {
        AuthenticationService.loginWithPassword(new LoginModel(null,
                        new PasswordModel(userPassword)), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void LoginWithPassword_EmptyEmail_BadRequest() {
        AuthenticationService.loginWithPassword(new LoginModel("",
                        new PasswordModel(userPassword)), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void LoginWithPassword_InvalidEmailFormat_BadRequest() {
        AuthenticationService.loginWithPassword(new LoginModel(String.format("%s.@weavrtest.io", RandomStringUtils.randomAlphanumeric(6)),
                        new PasswordModel(userPassword)), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("email"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REGEX"));
    }

    @Test
    public void LoginWithPassword_UnknownPassword_Forbidden() {
        AuthenticationService.loginWithPassword(new LoginModel(userEmail, new PasswordModel(RandomStringUtils.randomAlphanumeric(10))), secretKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void LoginWithPassword_UnknownEmail_Forbidden() {
        AuthenticationService.loginWithPassword(new LoginModel(UUID.randomUUID() + "@weavr.io", new PasswordModel(userPassword)), secretKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void LoginWithPassword_EmptyPassword_BadRequest() {
        AuthenticationService.loginWithPassword(new LoginModel(userEmail, new PasswordModel("")), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.password.value: must not be blank"));
    }

    @Test
    public void LoginWithPassword_NullPassword_BadRequest() {
        AuthenticationService.loginWithPassword(new LoginModel(userEmail, new PasswordModel(null)), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("password")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("value"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void LoginWithPassword_NullPasswordModel_BadRequest() {
        AuthenticationService.loginWithPassword(new LoginModel(userEmail, null), secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("password"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void LoginWithPassword_InvalidApiKey_Unauthorised() {
        AuthenticationService.loginWithPassword(new LoginModel(userEmail, new PasswordModel(userPassword)), "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void LoginWithPassword_NoApiKey_BadRequest() {
        AuthenticationService.loginWithPassword(new LoginModel(userEmail, new PasswordModel(userPassword)), "")
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void LoginWithPassword_DifferentInnovatorApiKey_Forbidden() {
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        AuthenticationService.loginWithPassword(new LoginModel(userEmail, new PasswordModel(userPassword)), secretKey)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void CreatePassword_OtherProgrammeApiKey_Forbidden() {
        AuthenticationService.loginWithPassword(new LoginModel(userEmail, new PasswordModel(userPassword)), secretKeyAppTwo)
                .then()
                .statusCode(SC_FORBIDDEN);
        ;
    }
}
