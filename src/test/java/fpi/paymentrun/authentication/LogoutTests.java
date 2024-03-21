package fpi.paymentrun.authentication;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.CreatePaymentRunModel;
import fpi.paymentrun.services.AuthenticationService;
import fpi.paymentrun.services.PaymentRunsService;
import opc.helpers.ModelHelper;
import opc.services.multiprivate.MultiPrivateService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

@Tag(PluginsTags.PAYMENT_RUN_AUTHENTICATION)
public class LogoutTests extends BasePaymentRunSetup {
    private String token;

    @BeforeEach
    public void Setup() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        token = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey).getRight();
    }

    @Test
    public void Logout_RootUser_Success() {
        MultiPrivateService.validateAccessToken(token, secretKey)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.logout(secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);

        MultiPrivateService.validateAccessToken(token, secretKey)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Logout_LogoutTwice_Unauthorized() {
        AuthenticationService.logout(secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.logout(secretKey, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Logout_ActionWhenLoggedOut_Unauthorized() {
        AuthenticationService.logout(secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);

        final Pair<String, String> accountNumberAndSortCode = ModelHelper.generateRandomValidFasterPaymentsBankDetails();
        final CreatePaymentRunModel createPaymentRunModel =
                CreatePaymentRunModel.defaultCreatePaymentRunFasterPaymentsBankAccountModel(accountNumberAndSortCode.getLeft(), accountNumberAndSortCode.getRight()).build();

        PaymentRunsService.createPaymentRun(createPaymentRunModel, secretKey, token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Logout_DifferentProgrammeKeys_Forbidden() {
        AuthenticationService.logout(secretKeyAppTwo, token)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void Logout_NoApiKey_BadRequest() {
        AuthenticationService.logout("", token)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void Logout_IncorrectSecretKey_Unauthorized() {
        AuthenticationService.logout("intelli", token)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Logout_IncorrectToken_Unauthorized() {
        AuthenticationService.logout(secretKey, "token")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
