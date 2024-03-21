package fpi.paymentrun.identities.buyers;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.services.BuyersService;
import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.services.innovator.InnovatorService;
import opc.services.simulator.SimulatorService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Tag(PluginsTags.PAYMENT_RUN_BUYERS)
public class StartKybTests extends BasePaymentRunSetup {

    private String buyerId;
    private String buyerToken;

    @BeforeEach
    public void Setup() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        buyerId = buyer.getLeft();
        buyerToken = buyer.getRight();
    }

    @Test
    public void StartKyb_AdminRoleBuyerToken_Success() {
        BuyersService.startKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue());
    }

    @Test
    public void StartKyb_MultipleRolesBuyerToken_Success() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> buyer = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        final String buyerToken = buyer.getRight();

        BuyersHelper.assignAllRoles(secretKey, buyerToken);

        BuyersService.startKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue());
    }

    @Test
    public void StartKyb_AuthUserToken_Forbidden() {
        final String buyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String userToken =
                BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyerToken).getRight();

        BuyersService.startKyb(secretKey, userToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void StartKyb_InvalidApiKey_Unauthorised() {
        BuyersService.startKyb("abc", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartKyb_EmptyApiKey_Unauthorised() {
        BuyersService.startKyb("", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartKyb_NoApiKey_BadRequest() {
        BuyersService.startKybNoApiKey(buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("api-key"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void StartKyb_InvalidToken_Unauthorised() {
        BuyersService.startKyb(secretKey, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartKyb_NoToken_Unauthorised() {
        BuyersService.startKyb(secretKey, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartKyb_DifferentInnovatorApiKey_Unauthorised() {
        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath()
                        .get("secretKey");

        BuyersService.startKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartKyb_UserLoggedOut_Unauthorised() {
        AuthenticationHelper.logout(secretKey, buyerToken);

        BuyersService.startKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void StartKyb_Approved_KybAlreadyApproved() {
        BuyersService.startKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("reference", notNullValue());

        SimulatorService.simulateKybApproval(secretKey, buyerId)
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersService.startKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("KYB_ALREADY_APPROVED"));
    }

    @Test
    public void StartKyb_BackofficeImpersonator_Unauthorised() {
        BuyersService.startKyb(secretKey, getBackofficeImpersonateToken(buyerId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
