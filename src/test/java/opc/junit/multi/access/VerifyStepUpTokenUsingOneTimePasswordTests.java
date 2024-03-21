package opc.junit.multi.access;

import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.database.AuthFactorsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.models.shared.VerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;

public class VerifyStepUpTokenUsingOneTimePasswordTests extends BaseAuthenticationSetup {

    private final static String VERIFICATION_CODE = "123456";

    @Test
    public void VerifyStepUpToken_CorporateRoot_Success() {
        final Pair<String, String> corporate = CorporatesHelper.createEnrolledCorporate(corporateProfileId, secretKey);

        startSuccessfulStepup(corporate.getRight());

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyStepUpToken_ConsumerRoot_Success() {
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(consumerProfileId, secretKey);

        startSuccessfulStepup(consumer.getRight());

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyStepUpToken_NoApiKey_BadRequest() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), "", consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyStepUpToken_NoVerificationCode_BadRequest() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.verifyStepup(new VerificationModel(""), EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyStepUpToken_InvalidChannel_BadRequest() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.AUTHY.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "7"})
    public void VerifyStepUpToken_MaxMinVerificationCode_BadRequest(final String verificationCode) {
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(consumerProfileId, secretKey);

        startSuccessfulStepup(consumer.getRight());

        AuthenticationService.verifyStepup(new VerificationModel(verificationCode), EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.verificationCode: size must be between 6 and 6"));
    }

    @Test
    public void VerifyStepUpToken_UnknownVerificationCode_VerificationCodeInvalid() {
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(consumerProfileId, secretKey);

        startSuccessfulStepup(consumer.getRight());

        AuthenticationService.verifyStepup(new VerificationModel(RandomStringUtils.randomAlphanumeric(6)), EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.verificationCode: must match \"^[0-9]*$\""));
    }


    //TODO: Test case for CHANNEL_NOT_SUPPORTED will be finished after bug fixing (https://weavr-payments.atlassian.net/browse/DEV-2626)
//    @Test
//    public void VerifyStepUpToken_ChannelNotSupported() {
//        create enrolled Consumer at appThree
//        String corporateProfileIdAppThree = applicationThree.getCorporatesProfileId();
//        String secretKeyAppThree = applicationThree.getSecretKey();
//        String programmeIdAppThree = applicationThree.getProgrammeId();

//        String innovatorEmail = applicationThree.getInnovatorEmail();
//        String innovatorPassword = applicationThree.getInnovatorPassword();
//        String innovatorToken = InnovatorHelper.loginInnovator(innovatorEmail, innovatorPassword);
//
////        change profile - add factors
//        final IdentityProfileAuthenticationModel identityProfileAuthenticationModel = IdentityProfileAuthenticationModel.DefaultAccountInfoIdentityProfileAuthenticationScheme();
//        final UpdateCorporateProfileModel updateCorporateProfileModel = UpdateCorporateProfileModel.builder().setAccountInformationFactors(identityProfileAuthenticationModel).build();
//
//        AdminService.updateCorporateProfile(updateCorporateProfileModel, innovatorToken, programmeIdAppThree, corporateProfileIdAppThree)
//                .then()
//                .statusCode(SC_OK);

//        start Stepup
//        final Pair<String, String> corporate = CorporatesHelper.createEnrolledCorporate(corporateProfileIdAppThree, secretKeyAppThree);
//
//        AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secretKeyAppThree, corporate.getRight())
//                .then()
//                .statusCode(SC_NO_CONTENT);

//        change profile to default (delete extended factors)
//        final IdentityProfileAuthenticationModel identityProfileAuthenticationModel2 = IdentityProfileAuthenticationModel.DefaultAccountInfoIdentityProfileAuthenticationScheme();
//        final UpdateCorporateProfileModel updateCorporateProfileModel2 = UpdateCorporateProfileModel.builder().setAccountInformationFactors(identityProfileAuthenticationModel2).build();
//
//
//        AdminService.updateCorporateProfile(updateCorporateProfileModel2, innovatorToken, programmeIdAppThree, corporateProfileIdAppThree)
//                .then()
//                .statusCode(SC_OK);

//        verifyStepup
//        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, corporate.getRight())
//                .then()
//                .statusCode(SC_CONFLICT)
//                .body("errorCode", equalTo("CHANNEL_NOT_SUPPORTED"));
//    }


    @Test
    public void VerifyStepUpToken_VerificationCodeExpired() throws SQLException {
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(consumerProfileId, secretKey);

        startSuccessfulStepup(consumer.getRight());

        AuthFactorsDatabaseHelper.updateAccountInformationState("EXPIRED", consumer.getLeft());

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_EXPIRED"));
    }

    @Test
    public void VerifyStepUpToken_RequestAlreadyProcessed_StateInvalid() {
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(consumerProfileId, secretKey);

        startSuccessfulStepup(consumer.getRight());

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("ALREADY_VERIFIED"));
    }

    @Test
    public void VerifyStepUpToken_InvalidApiKey_Unauthorised() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), "123", consumer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyStepUpToken_InvalidToken_Unauthorised() {
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, RandomStringUtils.randomAlphanumeric(5))
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyStepUpToken_RootUserLoggedOut_Unauthorised() {
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(consumerProfileId, secretKey);

        startSuccessfulStepup(consumer.getRight());

        AuthenticationService.logout(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyStepUpToken_BackofficeImpersonator_Forbidden() {
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey,
                        getBackofficeImpersonateToken(corporate.getLeft(), IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifyStepUpToken_DifferentInnovatorApiKey_Forbidden() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String otherInnovatorSecretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), otherInnovatorSecretKey, consumer.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifyStepUpToken_OtherApplicationSecretKey_Forbidden() {
        final Pair<String, String> consumer = ConsumersHelper.createEnrolledConsumer(consumerProfileId, secretKey);

        startSuccessfulStepup(consumer.getRight());

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), applicationThree.getSecretKey(), consumer.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifyStepUpToken_NoChannel_BadRequest() {
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), "", secretKey, consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyStepUpToken_NoToken_Unauthorised() {
        AuthenticationService.verifyStepup(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, "")
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    private void startSuccessfulStepup(String token) {
        AuthenticationService.startStepup(EnrolmentChannel.SMS.name(), secretKey, token)
                .then()
                .statusCode(SC_NO_CONTENT);
    }

}
