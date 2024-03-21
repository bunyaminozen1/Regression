package opc.junit.multi.authenticationfactors;

import commons.enums.State;
import commons.models.MobileNumberModel;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.database.AuthFactorsDatabaseHelper;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.consumers.PatchConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.corporates.PatchCorporateModel;
import opc.models.multi.users.UsersModel;
import opc.models.multi.users.UsersModel.Builder;
import opc.models.shared.VerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.multi.AuthenticationService;
import opc.services.multi.ConsumersService;
import opc.services.multi.CorporatesService;
import opc.services.multi.UsersService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VerifyOtpUserEnrolmentTests extends BaseAuthenticationFactorsSetup {

    private final static String VERIFICATION_CODE = "123456";

    @Test
    public void VerifyEnrolment_Corporate_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        enrolUser(corporate.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), corporate.getRight())
                                    .then()
                                    .statusCode(SC_OK)
                                    .body("factors[0].type", equalTo("OTP"))
                                    .body("factors[0].channel", equalTo("SMS"))
                                    .body("factors[0].status", equalTo("ACTIVE"));

        CorporatesService.getCorporates(secretKey, corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobileNumberVerified", equalTo(true));
    }

    @Test
    public void VerifyEnrolment_Consumer_Success() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        enrolUser(consumer.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, consumer.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), consumer.getRight())
                                    .then()
                                    .statusCode(SC_OK)
                                    .body("factors[0].type", equalTo("OTP"))
                                    .body("factors[0].channel", equalTo("SMS"))
                                    .body("factors[0].status", equalTo("ACTIVE"));

        ConsumersService.getConsumers(secretKey, consumer.getRight())
                .then()
                .statusCode(SC_OK)
                .body("rootUser.mobileNumberVerified", equalTo(true));
    }

    @Test
    public void VerifyEnrolment_AuthenticatedCorporateUser_Success() throws SQLException {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporate.getRight());

        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        enrolUser(user.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
                                    .then()
                                    .statusCode(SC_OK)
                                    .body("factors[0].type", equalTo("OTP"))
                                    .body("factors[0].channel", equalTo("SMS"))
                                    .body("factors[0].status", equalTo("ACTIVE"));

        assertEquals("1", CorporatesDatabaseHelper.getCorporateUser(user.getLeft()).get(0).get("mobile_number_verified"));
    }

    @Test
    public void VerifyEnrolment_AuthenticatedConsumerUser_Success() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, consumer.getRight());

        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        enrolUser(user.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void VerifyEnrolment_AuthenticatedCorporateUserUpdateMobileNumber_AuthFactorInactive() throws SQLException {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporate.getRight());

        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        enrolUser(user.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));

        assertEquals("1", CorporatesDatabaseHelper.getCorporateUser(user.getLeft()).get(0).get("mobile_number_verified"));

        final UsersModel updateUser2 = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser2, secretKey, user.getLeft(), user.getRight());

        // when user update mobile number auth factor status changes to inactive
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("INACTIVE"));

        assertEquals("0", CorporatesDatabaseHelper.getCorporateUser(user.getLeft()).get(0).get("mobile_number_verified"));

        //user enroll with new number
        enrolUser(user.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));

        assertEquals("1", CorporatesDatabaseHelper.getCorporateUser(user.getLeft()).get(0).get("mobile_number_verified"));
    }

    @Test
    public void VerifyEnrolment_AuthenticatedConsumerUser_UpdateMobileNumber_AuthFactorInactive() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, consumer.getRight());

        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        enrolUser(user.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));

        final UsersModel updateUser2 = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser2, secretKey, user.getLeft(), user.getRight());

        // when user update mobile number auth factor status changes to inactive
        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("INACTIVE"));

        //user enroll with new number
        enrolUser(user.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void VerifyEnrolment_AuthenticatedUser_VerificationCodeInvalid() {
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(secretKey, corporate.getRight());

        final UsersModel updateUser = UsersModel.builder().setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        enrolUser(user.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(RandomStringUtils.randomNumeric(6)), EnrolmentChannel.SMS.name(), secretKey, user.getRight())
                                    .then()
                                    .statusCode(SC_CONFLICT)
                                    .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEnrolment_UserNotEnrolled_Conflict() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, corporate.getRight())
                .then()
                                    .log().all()
                .statusCode(SC_CONFLICT);
    }

    @Test
    public void VerifyEnrolment_AlreadyVerified_Conflict() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        enrolUser(corporate.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, corporate.getRight())
                .then()
                .statusCode(SC_CONFLICT);
    }

    @ParameterizedTest
    @EnumSource(value = EnrolmentChannel.class, names = { "EMAIL", "UNKNOWN" })
    public void VerifyEnrolment_UnknownChannel_BadRequest(final EnrolmentChannel enrolmentChannel) {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        enrolUser(corporate.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), enrolmentChannel.name(),
                secretKey, corporate.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    @DisplayName("VerifyEnrolment_NoChannel_NotFound - DEV-6848 opened to return 404")
    public void VerifyEnrolment_NoChannel_NotFound() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        enrolUser(consumer.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), "",
                secretKey, consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    @DisplayName("VerifyEnrolment_EmptyChannelValue_NotFound - DEV-6848 opened to return 404")
    public void VerifyEnrolment_EmptyChannelValue_NotFound() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        enrolUser(consumer.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), "",
                secretKey, consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEnrolment_DifferentInnovatorApiKey_Forbidden() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        enrolUser(corporate.getRight());

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String otherInnovatorSecretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                otherInnovatorSecretKey, corporate.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifyEnrolment_UserLoggedOut_Unauthorised() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        enrolUser(corporate.getRight());

        AuthenticationService.logout(secretKey, corporate.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, corporate.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyEnrolment_InvalidApiKey_Unauthorised() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        enrolUser(corporate.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                "abc", corporate.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyEnrolment_NoApiKey_BadRequest() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        enrolUser(corporate.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                "", corporate.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEnrolment_BackofficeImpersonator_Forbidden() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        enrolUser(corporate.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, getBackofficeImpersonateToken(corporate.getLeft(), IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifyEnrolment_UnknownVerificationCode_VerificationCodeInvalid() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        enrolUser(consumer.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(RandomStringUtils.randomNumeric(6)),
                EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEnrolment_InvalidVerificationCode_BadRequest() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        enrolUser(consumer.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(RandomStringUtils.randomAlphabetic(6)),
                EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.verificationCode: must match \"^[0-9]*$\""));
    }

    @ParameterizedTest
    @NullAndEmptySource
    public void VerifyEnrolment_NoVerificationCode_BadRequest(final String verificationCode) {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        enrolUser(consumer.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(verificationCode),
                EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 7})
    public void VerifyEnrolment_VerificationCodeLengthChecks_BadRequest(final int verificationCodeLength) {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        enrolUser(consumer.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(RandomStringUtils.randomNumeric(verificationCodeLength)),
                        EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.verificationCode: size must be between 6 and 6"));
    }

    @Test
    public void VerifyEnrolment_NoVerificationCodeModel_BadRequest() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        enrolUser(consumer.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(null),
                EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }


    @Test
    @Disabled("Check simulator expiry")
    public void VerifyEnrolment_ExpiredChallenge() throws InterruptedException {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        enrolUser(corporate.getRight());

        Thread.sleep(61000);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(), secretKey, corporate.getRight())
                                    .then()
                                    .statusCode(SC_CONFLICT)
                                    .body("errorCode", equalTo("VERIFICATION_CODE_EXPIRED"));
    }
    @Test
    public void VerifyEnrolment_DeactivatedCorporateUser_Conflict(){
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        Pair<String, String> authenticatedUser = UsersHelper.createAuthenticatedUser(secretKey, corporate.getRight());

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey,authenticatedUser.getRight());

        UsersService.deactivateUser(secretKey,authenticatedUser.getLeft(),corporate.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE),
                EnrolmentChannel.SMS.name(),secretKey,authenticatedUser.getRight())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("CREDENTIALS_INACTIVE"));
    }
    @Test
    public void VerifyEnrolment_DeactivatedConsumerUser_Conflict(){
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        Pair<String, String> authenticatedUser = UsersHelper.createAuthenticatedUser(secretKey, consumer.getRight());

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey,authenticatedUser.getRight());

        UsersService.deactivateUser(secretKey,authenticatedUser.getLeft(),consumer.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE),
                EnrolmentChannel.SMS.name(),secretKey,authenticatedUser.getRight())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("CREDENTIALS_INACTIVE"));
    }
    @Test
    public void VerifyEnrolment_ReactivatedCorporateUser_Success(){
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        Pair<String, String> authenticatedUser = UsersHelper.createAuthenticatedUser(secretKey, corporate.getRight());

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey,authenticatedUser.getRight());

        UsersService.deactivateUser(secretKey,authenticatedUser.getLeft(),corporate.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE),
                EnrolmentChannel.SMS.name(),secretKey,authenticatedUser.getRight())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("CREDENTIALS_INACTIVE"));

        UsersService.activateUser(secretKey,authenticatedUser.getLeft(),corporate.getRight());
        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE),
                EnrolmentChannel.SMS.name(),secretKey,authenticatedUser.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);
    }
    @Test
    public void VerifyEnrolment_ReactivatedConsumerUser_Success(){
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);
        Pair<String, String> authenticatedUser = UsersHelper.createAuthenticatedUser(secretKey, consumer.getRight());

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey,authenticatedUser.getRight());

        UsersService.deactivateUser(secretKey,authenticatedUser.getLeft(),consumer.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE),
                EnrolmentChannel.SMS.name(),secretKey,authenticatedUser.getRight())
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("CREDENTIALS_INACTIVE"));

        UsersService.activateUser(secretKey,authenticatedUser.getLeft(),consumer.getRight());

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE),
                EnrolmentChannel.SMS.name(),secretKey,authenticatedUser.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);
    }

    @Test
    public void VerifyEnrolment_CorporateRootUpdateMobileNumberBeforeEnrolment_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        final PatchCorporateModel patchCorporateModel =
            PatchCorporateModel.newBuilder()
                .setMobile(MobileNumberModel.random())
                .build();

        CorporatesHelper.patchCorporate(patchCorporateModel, secretKey, corporate.getRight());

        enrolUser(corporate.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(patchCorporateModel.getMobile().getNumber());
        assertEquals(String.format("%s%s", patchCorporateModel.getMobile().getCountryCode(),
            patchCorporateModel.getMobile().getNumber()), sms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, corporate.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));

        CorporatesService.getCorporates(secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
            .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()))
            .body("rootUser.mobileNumberVerified", equalTo(true));

    }

    @Test
    public void VerifyEnrolment_ConsumerRootUpdateMobileNumberBeforeEnrolment_Success() {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        final PatchConsumerModel patchConsumerModel =
            PatchConsumerModel.newBuilder()
                .setMobile(MobileNumberModel.random())
                .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, consumer.getRight(), Optional.empty());

        enrolUser(consumer.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), consumer.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(patchConsumerModel.getMobile().getNumber());
        assertEquals(String.format("%s%s", patchConsumerModel.getMobile().getCountryCode(),
            patchConsumerModel.getMobile().getNumber()), sms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, consumer.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), consumer.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));

        ConsumersService.getConsumers(secretKey, consumer.getRight())
            .then()
            .statusCode(SC_OK)
            .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
            .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()))
            .body("rootUser.mobileNumberVerified", equalTo(true));
    }

    @Test
    public void VerifyEnrolment_CorporateAuthorizedUserUpdateMobileNumberBeforeEnrolment_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporate.getRight());

        final UsersModel patchUserDetails = new Builder()
            .setMobile(MobileNumberModel.random())
            .build();

        UsersService.patchUser(patchUserDetails, secretKey, user.getLeft(), user.getRight(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
            .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));

        enrolUser(user.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(patchUserDetails.getMobile().getNumber());
        assertEquals(String.format("%s%s", patchUserDetails.getMobile().getCountryCode(),
            patchUserDetails.getMobile().getNumber()), sms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));

        UsersService.getUser(secretKey, user.getLeft(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
            .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
    }

    @Test
    public void VerifyEnrolment_ConsumerAuthorizedUserUpdateMobileNumberBeforeEnrolment_Success() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
            consumerProfileId, secretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumer.getRight());

        final UsersModel patchUserDetails = new Builder()
            .setMobile(MobileNumberModel.random())
            .build();

        UsersService.patchUser(patchUserDetails, secretKey, user.getLeft(), user.getRight(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
            .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));

        enrolUser(user.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(patchUserDetails.getMobile().getNumber());
        assertEquals(String.format("%s%s", patchUserDetails.getMobile().getCountryCode(),
            patchUserDetails.getMobile().getNumber()), sms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));

        UsersService.getUser(secretKey, user.getLeft(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
            .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));
    }

    @Test
    public void VerifyEnrolment_CorporateRootUpdateMobileNumberBeforeVerify_Success(){

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        enrolUser(corporate.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createCorporateModel.getRootUser().getMobile().getNumber());
        assertEquals(String.format("%s%s", createCorporateModel.getRootUser().getMobile().getCountryCode(),
            createCorporateModel.getRootUser().getMobile().getNumber()), sms.getTo().split("@")[0]);

        //Update Mobile Number
        final PatchCorporateModel patchCorporateModel =
            PatchCorporateModel.newBuilder()
                .setMobile(MobileNumberModel.random())
                .build();

        CorporatesService.patchCorporate(patchCorporateModel, secretKey, corporate.getRight(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("rootUser.mobile.countryCode", equalTo(patchCorporateModel.getMobile().getCountryCode()))
            .body("rootUser.mobile.number", equalTo(patchCorporateModel.getMobile().getNumber()));

        //Start enrolment again
        enrolUser(corporate.getRight());

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(patchCorporateModel.getMobile().getNumber());

        assertEquals(String.format("%s%s", patchCorporateModel.getMobile().getCountryCode(),
            patchCorporateModel.getMobile().getNumber()), newsSms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, corporate.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));

        CorporatesService.getCorporates(secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .body("rootUser.mobileNumberVerified", equalTo(true));
    }

    @Test
    public void VerifyEnrolment_ConsumerRootUpdateMobileNumberBeforeVerify_Success(){

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        enrolUser(consumer.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), consumer.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createConsumerModel.getRootUser().getMobile().getNumber());
        assertEquals(String.format("%s%s", createConsumerModel.getRootUser().getMobile().getCountryCode(),
            createConsumerModel.getRootUser().getMobile().getNumber()), sms.getTo().split("@")[0]);

        //Update Mobile Number
        final PatchConsumerModel patchConsumerModel =
            PatchConsumerModel.newBuilder()
                .setMobile(MobileNumberModel.random())
                .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, consumer.getRight(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("rootUser.mobile.countryCode", equalTo(patchConsumerModel.getMobile().getCountryCode()))
            .body("rootUser.mobile.number", equalTo(patchConsumerModel.getMobile().getNumber()));

        //Start enrolment again
        enrolUser(consumer.getRight());

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(patchConsumerModel.getMobile().getNumber());

        assertEquals(String.format("%s%s", patchConsumerModel.getMobile().getCountryCode(),
            patchConsumerModel.getMobile().getNumber()), newsSms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, consumer.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), consumer.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));

        ConsumersService.getConsumers(secretKey, consumer.getRight())
            .then()
            .statusCode(SC_OK)
            .body("rootUser.mobileNumberVerified", equalTo(true));
    }

    @Test
    public void VerifyEnrolment_CorporateAuthorizedUserUpdateMobileNumberBeforeVerify_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporate.getRight());

        enrolUser(user.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(usersModel.getMobile().getNumber());
        assertEquals(String.format("%s%s", usersModel.getMobile().getCountryCode(),
            usersModel.getMobile().getNumber()), sms.getTo().split("@")[0]);

        //Update Mobile Number
        final UsersModel patchUserDetails = new Builder()
            .setMobile(MobileNumberModel.random())
            .build();

        UsersService.patchUser(patchUserDetails, secretKey, user.getLeft(), user.getRight(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
            .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));

        //Start enrolment again
        enrolUser(user.getRight());

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(patchUserDetails.getMobile().getNumber());

        assertEquals(String.format("%s%s", patchUserDetails.getMobile().getCountryCode(),
            patchUserDetails.getMobile().getNumber()), newsSms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void VerifyEnrolment_ConsumerAuthorizedUserUpdateMobileNumberBeforeVerify_Success() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumer.getRight());

        enrolUser(user.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(usersModel.getMobile().getNumber());
        assertEquals(String.format("%s%s", usersModel.getMobile().getCountryCode(),
            usersModel.getMobile().getNumber()), sms.getTo().split("@")[0]);

        //Update Mobile Number
        final UsersModel patchUserDetails = new Builder()
            .setMobile(MobileNumberModel.random())
            .build();

        UsersService.patchUser(patchUserDetails, secretKey, user.getLeft(), user.getRight(), Optional.empty())
            .then()
            .statusCode(SC_OK)
            .body("mobile.countryCode", equalTo(patchUserDetails.getMobile().getCountryCode()))
            .body("mobile.number", equalTo(patchUserDetails.getMobile().getNumber()));

        //Start enrolment again
        enrolUser(user.getRight());

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(patchUserDetails.getMobile().getNumber());

        assertEquals(String.format("%s%s", patchUserDetails.getMobile().getCountryCode(),
            patchUserDetails.getMobile().getNumber()), newsSms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void VerifyEnrolment_CorporateRootUpdateMobileNumberAfterVerify_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        enrolUser(corporate.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createCorporateModel.getRootUser().getMobile().getNumber());
        assertEquals(String.format("%s%s", createCorporateModel.getRootUser().getMobile().getCountryCode(),
            createCorporateModel.getRootUser().getMobile().getNumber()), sms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, corporate.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));

        CorporatesService.getCorporates(secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .body("rootUser.mobileNumberVerified", equalTo(true));

        final Map<String, String> channelStatus = AuthFactorsDatabaseHelper.getChannel(
            corporate.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.VERIFIED.name(), channelStatus.get("channel_status"));

        //Update Mobile Number
        final PatchCorporateModel patchCorporateModel =
            PatchCorporateModel.newBuilder()
                .setMobile(MobileNumberModel.random())
                .build();

        CorporatesHelper.patchCorporate(patchCorporateModel, secretKey, corporate.getRight());

        CorporatesService.getCorporates(secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .body("rootUser.mobileNumberVerified", equalTo(false));

        final Map<String, String> newChannelStatus = AuthFactorsDatabaseHelper.getChannel(
            corporate.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.EXPIRED.name(), newChannelStatus.get("channel_status"));

        //start enrolment again
        enrolUser(corporate.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(patchCorporateModel.getMobile().getNumber());
        assertEquals(String.format("%s%s", patchCorporateModel.getMobile().getCountryCode(),
            patchCorporateModel.getMobile().getNumber()), newsSms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, corporate.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        CorporatesService.getCorporates(secretKey, corporate.getRight())
            .then()
            .statusCode(SC_OK)
            .body("rootUser.mobileNumberVerified", equalTo(true));

        final Map<String, String> lastChannelStatus = AuthFactorsDatabaseHelper.getChannel(
            corporate.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.VERIFIED.name(), lastChannelStatus.get("channel_status"));
    }

    @Test
    public void VerifyEnrolment_ConsumerRootUpdateMobileNumberAfterVerify_Success() throws SQLException {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        enrolUser(consumer.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), consumer.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createConsumerModel.getRootUser().getMobile().getNumber());
        assertEquals(String.format("%s%s", createConsumerModel.getRootUser().getMobile().getCountryCode(),
            createConsumerModel.getRootUser().getMobile().getNumber()), sms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, consumer.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), consumer.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));

        ConsumersService.getConsumers(secretKey, consumer.getRight())
            .then()
            .statusCode(SC_OK)
            .body("rootUser.mobileNumberVerified", equalTo(true));

        final Map<String, String> channelStatus = AuthFactorsDatabaseHelper.getChannel(
            consumer.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.VERIFIED.name(), channelStatus.get("channel_status"));

        //Update Mobile Number
        final PatchConsumerModel patchConsumerModel =
            PatchConsumerModel.newBuilder()
                .setMobile(MobileNumberModel.random())
                .build();

        ConsumersService.patchConsumer(patchConsumerModel, secretKey, consumer.getRight(), Optional.empty());

        ConsumersService.getConsumers(secretKey, consumer.getRight())
            .then()
            .statusCode(SC_OK)
            .body("rootUser.mobileNumberVerified", equalTo(false));

        final Map<String, String> newChannelStatus = AuthFactorsDatabaseHelper.getChannel(
            consumer.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.EXPIRED.name(), newChannelStatus.get("channel_status"));

        //start enrolment again
        enrolUser(consumer.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), consumer.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(patchConsumerModel.getMobile().getNumber());

        assertEquals(String.format("%s%s", patchConsumerModel.getMobile().getCountryCode(),
           patchConsumerModel.getMobile().getNumber()), newsSms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, consumer.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        ConsumersService.getConsumers(secretKey, consumer.getRight())
            .then()
            .statusCode(SC_OK)
            .body("rootUser.mobileNumberVerified", equalTo(true));

        final Map<String, String> lastChannelStatus = AuthFactorsDatabaseHelper.getChannel(
            consumer.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.VERIFIED.name(), lastChannelStatus.get("channel_status"));
    }

    @Test
    public void VerifyEnrolment_CorporateAuthorizedUserUpdateMobileNumberAfterVerify_Success() throws SQLException {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, corporate.getRight());

        enrolUser(user.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(usersModel.getMobile().getNumber());
        assertEquals(String.format("%s%s", usersModel.getMobile().getCountryCode(),
            usersModel.getMobile().getNumber()), sms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));

        final Map<String, String> channelStatus = AuthFactorsDatabaseHelper.getChannel(
            user.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.VERIFIED.name(), channelStatus.get("channel_status"));

        //Update Mobile Number
        final UsersModel patchUserDetails = new Builder()
            .setMobile(MobileNumberModel.random())
            .build();

        UsersHelper.updateUser(patchUserDetails, secretKey, user.getLeft(), user.getRight());

        final Map<String, String> newChannelStatus = AuthFactorsDatabaseHelper.getChannel(
            user.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.EXPIRED.name(), newChannelStatus.get("channel_status"));

        //start enrolment again
        enrolUser(user.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(patchUserDetails.getMobile().getNumber());

        assertEquals(String.format("%s%s", patchUserDetails.getMobile().getCountryCode(),
            patchUserDetails.getMobile().getNumber()), newsSms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        final Map<String, String> lastChannelStatus = AuthFactorsDatabaseHelper.getChannel(
            user.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.VERIFIED.name(), lastChannelStatus.get("channel_status"));
    }

    @Test
    public void VerifyEnrolment_ConsumerAuthorizedUserUpdateMobileNumberAfterVerify_Success() throws SQLException {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(
            consumerProfileId, secretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumer.getRight());

        enrolUser(user.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(usersModel.getMobile().getNumber());
        assertEquals(String.format("%s%s", usersModel.getMobile().getCountryCode(),
            usersModel.getMobile().getNumber()), sms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("ACTIVE"));

        final Map<String, String> channelStatus = AuthFactorsDatabaseHelper.getChannel(
            user.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.VERIFIED.name(), channelStatus.get("channel_status"));

        //Update Mobile Number
        final UsersModel patchUserDetails = new Builder()
            .setMobile(MobileNumberModel.random())
            .build();

        UsersHelper.updateUser(patchUserDetails, secretKey, user.getLeft(), user.getRight());

        final Map<String, String> newChannelStatus = AuthFactorsDatabaseHelper.getChannel(
            user.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.EXPIRED.name(), newChannelStatus.get("channel_status"));

        //start enrolment again
        enrolUser(user.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
            .then()
            .statusCode(SC_OK)
            .body("factors[0].type", equalTo("OTP"))
            .body("factors[0].channel", equalTo("SMS"))
            .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(patchUserDetails.getMobile().getNumber());

        assertEquals(String.format("%s%s", patchUserDetails.getMobile().getCountryCode(),
           patchUserDetails.getMobile().getNumber()), newsSms.getTo().split("@")[0]);

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                secretKey, user.getRight())
            .then()
            .statusCode(SC_NO_CONTENT);

        final Map<String, String> lastChannelStatus = AuthFactorsDatabaseHelper.getChannel(
            user.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.VERIFIED.name(), lastChannelStatus.get("channel_status"));
    }

    private void enrolUser(final String token){
        AuthenticationFactorsHelper.enrolOtpUser(EnrolmentChannel.SMS.name(), secretKey, token);
    }
}
