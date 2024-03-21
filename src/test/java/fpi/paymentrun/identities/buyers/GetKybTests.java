package fpi.paymentrun.identities.buyers;

import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.services.BuyersService;
import opc.enums.opc.IdentityType;
import opc.tags.PluginsTags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

@Tag(PluginsTags.PAYMENT_RUN_BUYERS)
public class GetKybTests extends BasePaymentRunSetup {

    @Test
    public void GetKyb_AdminRoleBuyerKybVerified_Success() {
        final String buyerToken = BuyersHelper.createAuthenticatedVerifiedBuyer(secretKey).getRight();
        BuyersService.getKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("kybStatus", equalTo("APPROVED"))
                .body("ongoingKybStatus", equalTo("APPROVED"));
    }

    @Test
    public void GetKyb_KybNotStarted_Success() {
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        BuyersService.getKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("kybStatus", equalTo("NOT_STARTED"))
                .body("ongoingKybStatus", equalTo("NOT_STARTED"));
    }

    @Test
    public void GetKyb_KybInitiated_Success() {
        final String buyerToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        BuyersHelper.startKyb(secretKey, buyerToken);
        BuyersService.getKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("kybStatus", equalTo("INITIATED"))
                .body("ongoingKybStatus", equalTo("INITIATED"));
    }

    @Test
    public void GetKyb_MultipleRolesBuyerKybVerified_Success() {
        final String buyerToken = BuyersHelper.createAuthenticatedVerifiedBuyer(secretKey).getRight();
        BuyersHelper.assignAllRoles(secretKey, buyerToken);
        BuyersService.getKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_OK)
                .body("kybStatus", equalTo("APPROVED"))
                .body("ongoingKybStatus", equalTo("APPROVED"));
    }

    @Test
    public void GetKyb_AuthUserToken_Forbidden() {
        final String buyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final String userToken =
                BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyerToken).getRight();
        BuyersService.getKyb(secretKey, userToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void GetKyb_InvalidApiKey_Unauthorised() {
        final String buyerToken = BuyersHelper.createAuthenticatedVerifiedBuyer(secretKey).getRight();
        BuyersService.getKyb("abc", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetKyb_EmptyApiKey_Unauthorised() {
        final String buyerToken = BuyersHelper.createAuthenticatedVerifiedBuyer(secretKey).getRight();
        BuyersService.getKyb("", buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetKyb_NoApiKey_BadRequest() {
        final String buyerToken = BuyersHelper.createAuthenticatedVerifiedBuyer(secretKey).getRight();
        BuyersService.getKybNoApiKey(buyerToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("api-key"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @Test
    public void GetKyb_InvalidToken_Unauthorised() {
        BuyersService.getKyb(secretKey, "abc")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetKyb_NoToken_Unauthorised() {
        BuyersService.getKyb(secretKey, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetKyb_DifferentProgrammeApiKey_Unauthorised() {
        final String buyerToken = BuyersHelper.createAuthenticatedVerifiedBuyer(secretKey).getRight();
        BuyersService.getKyb(secretKeyAppTwo, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetKyb_UserLoggedOut_Unauthorised() {
        final String buyerToken = BuyersHelper.createAuthenticatedVerifiedBuyer(secretKey).getRight();
        AuthenticationHelper.logout(secretKey, buyerToken);
        BuyersService.getKyb(secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void GetKyb_BackofficeImpersonator_Unauthorised() {
        final String buyerId = BuyersHelper.createAuthenticatedVerifiedBuyer(secretKey).getLeft();
        BuyersService.getKyb(secretKey, getBackofficeImpersonateToken(buyerId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }
}
