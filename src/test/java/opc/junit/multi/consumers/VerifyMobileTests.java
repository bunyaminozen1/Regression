package opc.junit.multi.consumers;

import opc.enums.opc.IdentityType;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.shared.MobileVerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.ConsumersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.apache.http.HttpStatus.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

@Disabled
public class VerifyMobileTests extends BaseConsumersSetup {

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
    public void VerifyMobile_Success() {
        sendSuccessfulVerification();

        final MobileVerificationModel mobileVerificationModel =
                new MobileVerificationModel(TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyMobile(mobileVerificationModel, secretKey, authenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyMobile_InvalidApiKey_Unauthorised(){
        sendSuccessfulVerification();

        final MobileVerificationModel mobileVerificationModel =
                new MobileVerificationModel(TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyMobile(mobileVerificationModel, "abc", authenticationToken)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyMobile_NoApiKey_BadRequest(){
        sendSuccessfulVerification();

        final MobileVerificationModel mobileVerificationModel =
                new MobileVerificationModel(TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyMobile(mobileVerificationModel, "", authenticationToken)
                .then().statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyMobile_DifferentInnovatorApiKey_Forbidden(){
        sendSuccessfulVerification();

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();
        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        final MobileVerificationModel mobileVerificationModel =
                new MobileVerificationModel(TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyMobile(mobileVerificationModel, secretKey, authenticationToken)
                .then().statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifyMobile_RootUserLoggedOut_Unauthorised(){

        final String token = ConsumersHelper.createUnauthenticatedConsumer(consumerProfileId, secretKey).getRight();

        final MobileVerificationModel mobileVerificationModel =
                new MobileVerificationModel(TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyMobile(mobileVerificationModel, secretKey, token)
                .then().statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyMobile_UnknownVerificationCode_VerificationCodeInvalid(){
        final MobileVerificationModel mobileVerificationModel =
                new MobileVerificationModel(RandomStringUtils.randomNumeric(6));

        ConsumersService.verifyMobile(mobileVerificationModel, secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyMobile_MinVerificationCode_BadRequest(){
        final MobileVerificationModel mobileVerificationModel =
                new MobileVerificationModel(RandomStringUtils.randomNumeric(5));

        ConsumersService.verifyMobile(mobileVerificationModel, secretKey, authenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", containsString("request.verificationCode: size must be between 6 and 6"));
    }

    @Test
    public void VerifyMobile_MaxVerificationCode_BadRequest(){
        final MobileVerificationModel mobileVerificationModel =
                new MobileVerificationModel(RandomStringUtils.randomNumeric(7));

        ConsumersService.verifyMobile(mobileVerificationModel, secretKey, authenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", containsString("request.verificationCode: size must be between 6 and 6"));
    }

    @Test
    public void VerifyMobile_InvalidVerificationCode_BadRequest(){
        final MobileVerificationModel mobileVerificationModel =
                new MobileVerificationModel(RandomStringUtils.randomAlphanumeric(6));

        ConsumersService.verifyMobile(mobileVerificationModel, secretKey, authenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", containsString("request.verificationCode: must match \"^[0-9]+$\""));
    }

    @Test
    public void VerifyMobile_NoVerificationCode_BadRequest(){
        final MobileVerificationModel mobileVerificationModel =
                new MobileVerificationModel("");

        ConsumersService.verifyMobile(mobileVerificationModel, secretKey, authenticationToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyMobile_VerificationCodeNotSent_VerificationCodeInvalid(){
        final MobileVerificationModel mobileVerificationModel =
                new MobileVerificationModel(TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyMobile(mobileVerificationModel, secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyMobile_AlreadyVerified_VerificationCodeInvalid(){
        sendSuccessfulVerification();

        final MobileVerificationModel mobileVerificationModel =
                new MobileVerificationModel(TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyMobile(mobileVerificationModel, secretKey, authenticationToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        ConsumersService.verifyMobile(mobileVerificationModel, secretKey, authenticationToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyMobile_BackofficeImpersonator_Forbidden() {
        sendSuccessfulVerification();

        final MobileVerificationModel mobileVerificationModel =
                new MobileVerificationModel(TestHelper.VERIFICATION_CODE);

        ConsumersService.verifyMobile(mobileVerificationModel, secretKey, getBackofficeImpersonateToken(consumerId, IdentityType.CONSUMER))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    private void sendSuccessfulVerification(){
        TestHelper.ensureAsExpected(15,
                () -> ConsumersService.sendMobileVerification(secretKey, authenticationToken),
                SC_OK);
    }
}