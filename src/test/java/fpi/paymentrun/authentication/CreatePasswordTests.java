package fpi.paymentrun.authentication;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.services.AuthenticationService;
import fpi.paymentrun.services.BuyersService;
import opc.junit.helpers.TestHelper;
import opc.models.multi.passwords.CreatePasswordModel;
import opc.models.shared.PasswordModel;
import opc.services.innovator.InnovatorService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN_AUTHENTICATION)
public class CreatePasswordTests extends BasePaymentRunSetup {

    @Test
    public void CreatePassword_AdminUser_Success() {
        final String userId = createBuyer();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        AuthenticationService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.buyerId", equalTo(userId))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    @Test
    public void CreatePassword_InvalidApiKey_Unauthorised() {
        final String userId = createBuyer();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        AuthenticationService.createPassword(createPasswordModel, userId, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePassword_NoApiKey_Unauthorised() {
        final String userId = createBuyer();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        AuthenticationService.createPassword(createPasswordModel, userId, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePassword_DifferentInnovatorApiKey_Unauthorised() {
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final String userId = createBuyer();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.DEFAULT_COMPLEX_PASSWORD)).build();

        AuthenticationService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void CreatePassword_UnknownCorporateId_NotFound() {
        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        AuthenticationService.createPassword(createPasswordModel, "123456789", secretKey)
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void CreatePassword_AlreadyCreated_Conflict() {
        final String userId = createBuyer();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        AuthenticationService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_OK);

        AuthenticationService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_ALREADY_CREATED"));
    }

    @Test
    public void CreatePassword_CheckComplexity_PasswordTooSimple() {
        final String userId = createBuyer();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.DEFAULT_PASSWORD)).build();

        AuthenticationService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SIMPLE"));
    }

    @Test
    public void CreatePassword_LongPassword_PasswordTooLong() {
        final String userId = createBuyer();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(RandomStringUtils.randomAlphanumeric(51))).build();

        AuthenticationService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_LONG"));
    }

    @Test
    public void CreatePassword_ShortPassword_PasswordTooShort() {
        final String userId = createBuyer();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(RandomStringUtils.randomAlphanumeric(3))).build();

        AuthenticationService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSWORD_TOO_SHORT"));
    }

    @Test
    public void CreatePassword_NullPassword_BadRequest() {
        final String userId = createBuyer();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(null)).build();

        AuthenticationService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of("password")))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("value"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void CreatePassword_EmptyPassword_BadRequest() {
        final String userId = createBuyer();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel("")).build();

        AuthenticationService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.password.value: must not be blank"));
    }

    @Test
    public void CreatePassword_NoPasswordModel_BadRequest() {
        final String userId = createBuyer();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(null).build();

        AuthenticationService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("password"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void CreatePassword_OtherProgrammeApiKey_NotFound() {
        final String userId = createBuyer();

        final CreatePasswordModel createPasswordModel = CreatePasswordModel
                .newBuilder()
                .setPassword(new PasswordModel(TestHelper.getDefaultPassword(secretKey))).build();

        AuthenticationService.createPassword(createPasswordModel, userId, secretKeyAppTwo)
                .then()
                .statusCode(SC_NOT_FOUND);

        AuthenticationService.createPassword(createPasswordModel, userId, secretKey)
                .then()
                .statusCode(SC_OK)
                .body("passwordInfo.buyerId", equalTo(userId))
                .body("passwordInfo.expiryDate", equalTo(0))
                .body("token", notNullValue());
    }

    private String createBuyer() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        return TestHelper.ensureAsExpected(15,
                        () -> BuyersService.createBuyer(createBuyerModel, secretKey),
                        SC_OK)
                .jsonPath()
                .get("id");
    }
}
