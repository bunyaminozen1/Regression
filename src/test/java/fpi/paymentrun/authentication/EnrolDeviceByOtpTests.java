package fpi.paymentrun.authentication;

import commons.models.innovator.IdentityProfileAuthenticationModel;
import fpi.paymentrun.BasePaymentRunSetup;
import fpi.helpers.BuyersHelper;
import fpi.paymentrun.models.CreateBuyerModel;
import fpi.paymentrun.services.AuthenticationService;
import opc.enums.mailhog.MailHogSms;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.database.AuthFactorsSimulatorDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.models.innovator.UpdateCorporateProfileModel;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.shared.VerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationFactorsService;
import opc.tags.PluginsTags;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(PluginsTags.PAYMENT_RUN_AUTHENTICATION_FACTORS)
public class EnrolDeviceByOtpTests extends BasePaymentRunSetup {

    @Test
    public void EnrolDevice_AdminUser_Success() throws SQLException {
        final CreateBuyerModel createBuyerModel =
                CreateBuyerModel.defaultCreateBuyerModel().build();
        final Pair<String, String> user = BuyersHelper.createAuthenticatedBuyer(createBuyerModel, secretKey);

        AuthenticationService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createBuyerModel.getAdminUser().getMobile().getNumber());
        assertEquals(MailHogSms.SCA_SMS_ENROL.getFrom(), sms.getFrom());
        assertEquals(String.format("%s%s@weavr.io", createBuyerModel.getAdminUser().getMobile().getCountryCode(),
                createBuyerModel.getAdminUser().getMobile().getNumber()), sms.getTo());
        assertEquals(String.format(MailHogSms.SCA_SMS_ENROL.getSmsText(), programmeName,
                AuthFactorsSimulatorDatabaseHelper.getLatestFakeOtp(user.getLeft()).get(0).get("token")), sms.getBody());
    }

    @Test
    public void EnrolDevice_AlreadyEnrolledNotVerified_Success() {
        final Pair<String, String> user = BuyersHelper.createAuthenticatedBuyer(secretKey);

        AuthenticationService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        AuthenticationService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
    }

    @Test
    public void EnrolDevice_AlreadyEnrolledAndVerified_ChannelAlreadyRegistered() {
        final Pair<String, String> user = BuyersHelper.createAuthenticatedBuyer(secretKey);

        AuthenticationService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        AuthenticationService.verifyEnrolment(new VerificationModel("123456"), EnrolmentChannel.SMS.name(), secretKey, user.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("OTP"))
                .body("factors[0].channel", equalTo("SMS"))
                .body("factors[0].status", equalTo("ACTIVE"));

        AuthenticationService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, user.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_ALREADY_REGISTERED"));
    }

    @ParameterizedTest
    @EnumSource(value = EnrolmentChannel.class, names = {"EMAIL", "UNKNOWN"})
    public void EnrolDevice_UnknownChannel_BadRequest(final EnrolmentChannel enrolmentChannel) {
        final Pair<String, String> user = BuyersHelper.createAuthenticatedBuyer(secretKey);

        AuthenticationService.enrolOtp(enrolmentChannel.name(), secretKey, user.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo("Invalid request"))
                .body("syntaxErrors.invalidFields[0].params", equalTo(List.of()))
                .body("syntaxErrors.invalidFields[0].fieldName", equalTo("channel"))
                .body("syntaxErrors.invalidFields[0].error", equalTo("NOT_IN"));
    }

    @Test
    public void EnrolDevice_NoChannel_NotFound() {
        final Pair<String, String> user = BuyersHelper.createAuthenticatedBuyer(secretKey);

        AuthenticationService.enrolOtp("", secretKey, user.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void EnrolDevice_DifferentInnovatorApiKey_Forbidden() {
        final Pair<String, String> user = BuyersHelper.createAuthenticatedBuyer(secretKey);

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String secretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        AuthenticationService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, user.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void EnrolDevice_UserLoggedOut_Unauthorised() {
        final String buyerToken = BuyersHelper.createUnauthenticatedBuyer(secretKey).getRight();

        AuthenticationService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, buyerToken)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void EnrolDevice_InvalidApiKey_Unauthorised() {
        final Pair<String, String> user = BuyersHelper.createAuthenticatedBuyer(secretKey);

        AuthenticationService.enrolOtp(EnrolmentChannel.SMS.name(), "abc", user.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void EnrolDevice_NoApiKey_BadRequest() {
        final Pair<String, String> user = BuyersHelper.createAuthenticatedBuyer(secretKey);

        AuthenticationService.enrolOtp(EnrolmentChannel.SMS.name(), "", user.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void EnrolDevice_BackofficeImpersonator_Forbidden() {
        final Pair<String, String> user = BuyersHelper.createAuthenticatedBuyer(secretKey);

        AuthenticationService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey,
                        getBackofficeImpersonateToken(user.getLeft(), IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void EnrolDevice_NotSupportedByProfile_ChannelNotSupported() {

        UpdateCorporateProfileModel updateCorporateProfileModel =
                UpdateCorporateProfileModel.builder()
                        .setAccountInformationFactors(IdentityProfileAuthenticationModel.DefaultAccountInfoIdentityProfileAuthenticationScheme())
                        .setPaymentInitiationFactors(IdentityProfileAuthenticationModel.PaymentInitIdentityProfileAuthenticationScheme("EMAIL_OTP"))
                        .setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel.BeneficiaryManagementIdentityProfileAuthenticationScheme("EMAIL_OTP"))
                        .build();
        InnovatorHelper.updateCorporateProfile(updateCorporateProfileModel, innovatorTokenAppTwo, programmeIdAppTwo, corporateProfileIdAppTwo);

        final Pair<String, String> user = BuyersHelper.createAuthenticatedBuyer(secretKeyAppTwo, TestHelper.DEFAULT_COMPLEX_PASSWORD);

        AuthenticationService.enrolOtp(EnrolmentChannel.SMS.name(), secretKeyAppTwo, user.getRight())
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("CHANNEL_NOT_SUPPORTED"));

        UpdateCorporateProfileModel revertCorporateProfileModel =
                UpdateCorporateProfileModel.builder()
                        .setAccountInformationFactors(IdentityProfileAuthenticationModel.DefaultAccountInfoIdentityProfileAuthenticationScheme())
                        .setPaymentInitiationFactors(IdentityProfileAuthenticationModel.DefaultPaymentInitIdentityProfileAuthenticationScheme())
                        .setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel.DefaultBeneficiaryManagementIdentityProfileAuthenticationScheme())
                        .build();

        InnovatorHelper.updateCorporateProfile(revertCorporateProfileModel, innovatorTokenAppTwo, programmeIdAppTwo, corporateProfileIdAppTwo);
    }
}
