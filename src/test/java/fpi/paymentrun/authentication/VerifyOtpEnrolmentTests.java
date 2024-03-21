package fpi.paymentrun.authentication;

import commons.enums.State;
import commons.models.MobileNumberModel;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.AuthenticationHelper;
import fpi.helpers.BuyerAuthorisedUserHelper;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.AdminUserModel;
import fpi.paymentrun.models.BuyerAuthorisedUserModel;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.models.UpdateBuyerModel;
import fpi.paymentrun.services.AuthenticationService;
import fpi.paymentrun.services.BuyersService;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.database.AuthFactorsDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.shared.VerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationFactorsService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;

@Tag(PluginsTags.PAYMENT_RUN_AUTHENTICATION_FACTORS)
public class VerifyOtpEnrolmentTests extends BasePaymentRunSetup {
    private final static String VERIFICATION_CODE = "123456";

    @Test
    public void VerifyEnrolment_AdminUser_Success() {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                        secretKey, userToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), userToken)
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("ACTIVE"));

        BuyersService.getBuyer(secretKey, userToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobileNumberVerified", equalTo(true));
    }

    @Test
    public void VerifyEnrolment_AuthUser_Success() {
        final String buyerToken = BuyersHelper.createEnrolledSteppedUpBuyer(secretKey).getRight();
        final Triple<String, BuyerAuthorisedUserModel, String> authUser =
                BuyerAuthorisedUserHelper.createAuthenticatedUser(secretKey, buyerToken);

        enrolDeviceByOtp(authUser.getRight());

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                        secretKey, authUser.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), authUser.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void VerifyEnrolment_UserNotEnrolled_VerificationCodeInvalid() {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                        secretKey, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEnrolment_AlreadyVerified_ChannelAlreadyRegistered() {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                        secretKey, userToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                        secretKey, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_ALREADY_REGISTERED"));
    }

    @ParameterizedTest
    @EnumSource(value = EnrolmentChannel.class, names = {"EMAIL", "UNKNOWN"})
    public void VerifyEnrolment_UnknownChannel_BadRequest(final EnrolmentChannel enrolmentChannel) {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), enrolmentChannel.name(),
                        secretKey, userToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("channel"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void VerifyEnrolment_EmptyChannel_BadRequest() {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), "",
                        secretKey, userToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEnrolment_DifferentInnovatorApiKey_Forbidden() {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                        secretKey, userToken)
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifyEnrolment_UserLoggedOut_Unauthorised() {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        AuthenticationHelper.logout(secretKey, userToken);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                        secretKey, userToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyEnrolment_InvalidApiKey_Unauthorised() {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                        "abc", userToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void VerifyEnrolment_NoApiKey_BadRequest() {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                        "", userToken)
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void VerifyEnrolment_BackofficeImpersonator_Forbidden() {
        final Pair<String, String> user = BuyersHelper.createAuthenticatedBuyer(secretKey);
        enrolDeviceByOtp(user.getRight());

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE), EnrolmentChannel.SMS.name(),
                        secretKey, getBackofficeImpersonateToken(user.getLeft(), IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void VerifyEnrolment_UnknownVerificationCode_VerificationCodeInvalid() {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        AuthenticationService.verifyEnrolment(new VerificationModel(RandomStringUtils.randomNumeric(6)), EnrolmentChannel.SMS.name(),
                        secretKey, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_INVALID"));
    }

    @Test
    public void VerifyEnrolment_InvalidVerificationCode_BadRequest() {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        AuthenticationService.verifyEnrolment(new VerificationModel(RandomStringUtils.randomAlphabetic(6)), EnrolmentChannel.SMS.name(),
                        secretKey, userToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.verificationCode: must match \"^[0-9]*$\""));
    }

    @Test
    public void VerifyEnrolment_EmptyVerificationCode_BadRequest() {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        AuthenticationService.verifyEnrolment(new VerificationModel(""), EnrolmentChannel.SMS.name(),
                        secretKey, userToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Bad Request"));
    }

    @Test
    public void VerifyEnrolment_NullVerificationCode_BadRequest() {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        AuthenticationService.verifyEnrolment(new VerificationModel(null), EnrolmentChannel.SMS.name(),
                        secretKey, userToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("verificationCode"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("REQUIRED"));
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 7})
    public void VerifyEnrolment_SizeVerificationCode_BadRequest(final int verificationCodeLength) {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        AuthenticationService.verifyEnrolment(new VerificationModel(RandomStringUtils.randomNumeric(verificationCodeLength)),
                        EnrolmentChannel.SMS.name(), secretKey, userToken)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("request.verificationCode: size must be between 6 and 6"));
    }

    @Test
    @Disabled("Check simulator expiry")
    public void VerifyEnrolment_ExpiredChallenge_VerificationCodeExpired() throws InterruptedException {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();
        enrolDeviceByOtp(userToken);

        Thread.sleep(61000);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE),
                        EnrolmentChannel.SMS.name(), secretKey, userToken)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_CODE_EXPIRED"));
    }

    @Test
    public void VerifyEnrolment_UpdateMobileNumberBeforeEnrolment_Success() {
        final String userToken = BuyersHelper.createAuthenticatedBuyer(secretKey).getRight();

        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(MobileNumberModel.random())
                        .build())
                .build();

        BuyersHelper.updateBuyer(updateBuyerModel, secretKey, userToken);

        enrolDeviceByOtp(userToken);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), userToken)
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(updateBuyerModel.getAdminUser().getMobile().getNumber());
        assertEquals(String.format("%s%s", updateBuyerModel.getAdminUser().getMobile().getCountryCode(),
                updateBuyerModel.getAdminUser().getMobile().getNumber()), sms.getTo().split("@")[0]);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE),
                        EnrolmentChannel.SMS.name(), secretKey, userToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), userToken)
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("ACTIVE"));

        BuyersService.getBuyer(secretKey, userToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()))
                .body("adminUser.mobileNumberVerified", equalTo(true));
    }

    @Test
    public void VerifyEnrolment_UpdateMobileNumberBeforeVerify_Success() {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        final String userToken = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey).getRight();
        enrolDeviceByOtp(userToken);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), userToken)
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createBuyerModel.getAdminUser().getMobile().getNumber());
        assertEquals(String.format("%s%s", createBuyerModel.getAdminUser().getMobile().getCountryCode(),
                createBuyerModel.getAdminUser().getMobile().getNumber()), sms.getTo().split("@")[0]);

        //Update Mobile Number
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(MobileNumberModel.random())
                        .build())
                .build();

        BuyersHelper.updateBuyer(updateBuyerModel, secretKey, userToken);

        //Start enrolment again
        enrolDeviceByOtp(userToken);

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(updateBuyerModel.getAdminUser().getMobile().getNumber());

        assertEquals(String.format("%s%s", updateBuyerModel.getAdminUser().getMobile().getCountryCode(),
                updateBuyerModel.getAdminUser().getMobile().getNumber()), newsSms.getTo().split("@")[0]);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE),
                        EnrolmentChannel.SMS.name(), secretKey, userToken)
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), userToken)
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("ACTIVE"));

        BuyersService.getBuyer(secretKey, userToken)
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobile.countryCode", equalTo(updateBuyerModel.getAdminUser().getMobile().getCountryCode()))
                .body("adminUser.mobile.number", equalTo(updateBuyerModel.getAdminUser().getMobile().getNumber()))
                .body("adminUser.mobileNumberVerified", equalTo(true));
    }

    @Test
    public void VerifyEnrolment_UpdateMobileNumberAfterVerify_Success() throws SQLException {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();

        final Pair<String, String> user = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);
        enrolDeviceByOtp(user.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createBuyerModel.getAdminUser().getMobile().getNumber());
        assertEquals(String.format("%s%s", createBuyerModel.getAdminUser().getMobile().getCountryCode(),
                createBuyerModel.getAdminUser().getMobile().getNumber()), sms.getTo().split("@")[0]);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE),
                        EnrolmentChannel.SMS.name(), secretKey, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("ACTIVE"));

        BuyersService.getBuyer(secretKey, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobileNumberVerified", equalTo(true));

        final Map<String, String> channelStatus = AuthFactorsDatabaseHelper.getChannel(
                user.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.VERIFIED.name(), channelStatus.get("channel_status"));

        //Update Mobile Number
        final UpdateBuyerModel updateBuyerModel = UpdateBuyerModel.builder()
                .adminUser(AdminUserModel.builder()
                        .mobile(MobileNumberModel.random())
                        .build())
                .build();

        BuyersHelper.updateBuyer(updateBuyerModel, secretKey, user.getRight());

        BuyersService.getBuyer(secretKey, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobileNumberVerified", equalTo(false));

        final Map<String, String> newChannelStatus = AuthFactorsDatabaseHelper.getChannel(
                user.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.EXPIRED.name(), newChannelStatus.get("channel_status"));

        //Start enrolment again
        enrolDeviceByOtp(user.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse newsSms = MailhogHelper.getMailHogSms(updateBuyerModel.getAdminUser().getMobile().getNumber());
        assertEquals(String.format("%s%s", updateBuyerModel.getAdminUser().getMobile().getCountryCode(),
                updateBuyerModel.getAdminUser().getMobile().getNumber()), newsSms.getTo().split("@")[0]);

        AuthenticationService.verifyEnrolment(new VerificationModel(VERIFICATION_CODE),
                        EnrolmentChannel.SMS.name(), secretKey, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        BuyersService.getBuyer(secretKey, user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("adminUser.mobileNumberVerified", equalTo(true));

        final Map<String, String> lastChannelStatus = AuthFactorsDatabaseHelper.getChannel(
                user.getLeft(), "SMS_OTP").get(0);

        assertEquals(State.VERIFIED.name(), lastChannelStatus.get("channel_status"));
    }

    private void enrolDeviceByOtp(final String token) {
        AuthenticationHelper.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, token);
    }
}
