package opc.junit.multi.consumers;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.MobileVerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ConsumersService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@Disabled
public class SendMobileVerificationTests extends BaseConsumersSetup {

    private String authenticationToken;
    private String consumerId;

    @BeforeEach
    public void Setup(){
        final CreateConsumerModel createConsumerModel =
                CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();

        final Pair<String, String> authenticatedConsumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);
        authenticationToken = authenticatedConsumer.getRight();
        consumerId = authenticatedConsumer.getLeft();
    }

    @Test
    public void SendMobileVerification_Mobile_Success() {
        ConsumersService.sendMobileVerification(secretKey, authenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("timeLeftForRetry", notNullValue())
                .body("retriesLeft", equalTo(5));
    }

    @Test
    public void SendMobileVerification_InvalidApiKey_Unauthorised(){

        ConsumersService.sendMobileVerification("abc", authenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendMobileVerification_NoApiKey_BadRequest(){

        ConsumersService.sendMobileVerification("", authenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void SendMobileVerification_DifferentInnovatorApiKey_Forbidden(){

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        ConsumersService.sendMobileVerification(secretKey, authenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void SendMobileVerification_RootUserLoggedOut_Unauthorised(){

        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        ConsumersService.sendMobileVerification(secretKey, token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void SendMobileVerification_MobileAlreadySent_FrequencyExceeded() {
        sendSuccessfulMobileVerification();

        ConsumersService.sendMobileVerification(secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FREQUENCY_EXCEEDED"));
    }

    @Test
    public void SendMobileVerification_MobileAlreadyVerified_AlreadyVerified() {
        sendSuccessfulMobileVerification();

        ConsumersService.verifyMobile(new MobileVerificationModel(TestHelper.VERIFICATION_CODE), secretKey, authenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.sendMobileVerification(secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ALREADY_VERIFIED"));
    }

    @Test
    public void SendMobileVerification_MobileWithFailedVerificationAttempt_FrequencyExceeded() {
        sendSuccessfulMobileVerification();

        ConsumersService.verifyMobile(new MobileVerificationModel("1111"), secretKey, authenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);

        ConsumersService.sendMobileVerification(secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("FREQUENCY_EXCEEDED"));
    }

    @Test
    public void SendMobileVerification_BackofficeImpersonator_Forbidden() {
        ConsumersService.sendMobileVerification(secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private void sendSuccessfulMobileVerification(){
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.sendMobileVerification(secretKey, authenticationToken),
                SC_OK)
                .then()
                .body("timeLeftForRetry", notNullValue())
                .body("retriesLeft", equalTo(5));
    }
}