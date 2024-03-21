package opc.junit.multi.corporates;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.MobileVerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.CorporatesService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Disabled
public class SendMobileVerificationTests extends BaseCorporatesSetup {

    private String authenticationToken;
    private String corporateId;

    @BeforeEach
    public void Setup(){
        final CreateCorporateModel createCorporateModel =
                CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();

        final Pair<String, String> authenticatedCorporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);
        authenticationToken = authenticatedCorporate.getRight();
        corporateId = authenticatedCorporate.getLeft();
    }

    @Test
    public void SendMobileVerification_Mobile_Success() {
        CorporatesService.sendMobileVerification(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("timeLeftForRetry", notNullValue())
                .body("retriesLeft", equalTo(5));
    }

    @Test
    public void SendMobileVerification_InvalidApiKey_Unauthorised(){

        CorporatesService.sendMobileVerification("abc", authenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendMobileVerification_NoApiKey_Unauthorised(){

        CorporatesService.sendMobileVerification("", authenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendMobileVerification_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        CorporatesService.sendMobileVerification(secretKey, authenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendMobileVerification_RootUserLoggedOut_Unauthorised(){

        final String token = CorporatesHelper.createUnauthenticatedCorporate(corporateProfileId, secretKey).getRight();

        CorporatesService.sendMobileVerification(secretKey, token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendMobileVerification_MobileAlreadySent_FrequencyExceeded() {
        sendSuccessfulMobileVerification();

        CorporatesService.sendMobileVerification(secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FREQUENCY_EXCEEDED"));
    }

    @Test
    public void SendMobileVerification_MobileAlreadyVerified_AlreadyVerified() {
        sendSuccessfulMobileVerification();

        CorporatesService.verifyMobile(new MobileVerificationModel(TestHelper.VERIFICATION_CODE), secretKey, authenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        CorporatesService.sendMobileVerification(secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ALREADY_VERIFIED"));
    }

    @Test
    public void SendMobileVerification_MobileWithFailedVerificationAttempt_FrequencyExceeded() {
        sendSuccessfulMobileVerification();

        CorporatesService.verifyMobile(new MobileVerificationModel("1111"), secretKey, authenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        CorporatesService.sendMobileVerification(secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FREQUENCY_EXCEEDED"));
    }

    @Test
    public void SendMobileVerification_BackofficeImpersonator_Forbidden() {
        CorporatesService.sendMobileVerification(secretKey, getBackofficeImpersonateToken(corporateId, IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private void sendSuccessfulMobileVerification(){
        TestHelper.ensureAsExpected(15,
                () -> CorporatesService.sendMobileVerification(secretKey, authenticationToken),
                SC_OK)
                .then()
                .body("timeLeftForRetry", notNullValue())
                .body("retriesLeft", equalTo(5));
    }
}