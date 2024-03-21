package opc.junit.multi.authenticationfactors;

import opc.enums.mailhog.MailHogSms;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.database.AuthFactorsSimulatorDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.models.innovator.CreateCorporateProfileModel;
import commons.models.innovator.IdentityProfileAuthenticationModel;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.consumers.CreateConsumerModel;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.multi.users.UsersModel;
import commons.models.MobileNumberModel;
import opc.models.shared.VerificationModel;
import opc.services.innovator.InnovatorService;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.multi.AuthenticationService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.sql.SQLException;
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

public class EnrolDeviceByOtpViaSmsTests extends BaseAuthenticationFactorsSetup {

    @Test
    public void EnrolUser_CorporateRoot_Success() throws SQLException {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(corporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(createCorporateModel, secretKey);

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), corporate.getRight())
                                    .then()
                                    .statusCode(SC_OK)
                                    .body("factors[0].type", equalTo("OTP"))
                                    .body("factors[0].channel", equalTo("SMS"))
                                    .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createCorporateModel.getRootUser().getMobile().getNumber());
        assertEquals(MailHogSms.SCA_SMS_ENROL.getFrom(), sms.getFrom());
        assertEquals(String.format("%s%s@weavr.io", createCorporateModel.getRootUser().getMobile().getCountryCode(),
                createCorporateModel.getRootUser().getMobile().getNumber()), sms.getTo());
        assertEquals(String.format(MailHogSms.SCA_SMS_ENROL.getSmsText(), programmeName,
                AuthFactorsSimulatorDatabaseHelper.getLatestFakeOtp(corporate.getLeft()).get(0).get("token")), sms.getBody());
    }

    @Test
    public void EnrolUser_ConsumerRoot_Success() throws SQLException {

        final CreateConsumerModel createConsumerModel = CreateConsumerModel.DefaultCreateConsumerModel(consumerProfileId).build();
        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(createConsumerModel, secretKey);

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                                    .then()
                                    .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), consumer.getRight())
                                    .then()
                                    .statusCode(SC_OK)
                                    .body("factors[0].type", equalTo("OTP"))
                                    .body("factors[0].channel", equalTo("SMS"))
                                    .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(createConsumerModel.getRootUser().getMobile().getNumber());
        assertEquals(MailHogSms.SCA_SMS_ENROL.getFrom(), sms.getFrom());
        assertEquals(String.format("%s%s@weavr.io", createConsumerModel.getRootUser().getMobile().getCountryCode(),
                createConsumerModel.getRootUser().getMobile().getNumber()), sms.getTo());
        assertEquals(String.format(MailHogSms.SCA_SMS_ENROL.getSmsText(), programmeName,
                AuthFactorsSimulatorDatabaseHelper.getLatestFakeOtp(consumer.getLeft()).get(0).get("token")), sms.getBody());
    }

    @Test
    public void EnrolUser_AuthenticatedUser_Success() throws SQLException {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumer.getRight());

        final UsersModel updateUser = UsersModel.builder().setEmail(usersModel.getEmail()).setMobile(MobileNumberModel.random()).build();
        UsersHelper.updateUser(updateUser, secretKey, user.getLeft(), user.getRight());

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, user.getRight())
                                    .then()
                                    .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight())
                                    .then()
                                    .statusCode(SC_OK)
                                    .body("factors[0].type", equalTo("OTP"))
                                    .body("factors[0].channel", equalTo("SMS"))
                                    .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        final MailHogMessageResponse sms = MailhogHelper.getMailHogSms(updateUser.getMobile().getNumber());
        assertEquals(MailHogSms.SCA_SMS_ENROL.getFrom(), sms.getFrom());
        assertEquals(String.format("%s%s@weavr.io", updateUser.getMobile().getCountryCode(),
                updateUser.getMobile().getNumber()), sms.getTo());
        assertEquals(String.format(MailHogSms.SCA_SMS_ENROL.getSmsText(), programmeName,
                AuthFactorsSimulatorDatabaseHelper.getLatestFakeOtp(user.getLeft()).get(0).get("token")), sms.getBody());
    }

    @Test
    public void EnrolUser_AuthenticatedUserNoMobileNumber_MobileNumberNotAvailable() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        final UsersModel usersModel =
                UsersModel.DefaultUsersModel()
                        .setMobile(null)
                        .build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, consumer.getRight());

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, user.getRight())
                                    .then()
                                    .statusCode(SC_CONFLICT)
                                    .body("errorCode", equalTo("MOBILE_NUMBER_NOT_AVAILABLE"));
    }

    @Test
    public void EnrolUser_AlreadyEnrolledNotVerified_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), corporate.getRight())
                                    .then()
                                    .statusCode(SC_OK)
                                    .body("factors[0].type", equalTo("OTP"))
                                    .body("factors[0].channel", equalTo("SMS"))
                                    .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, corporate.getRight())
                .then()
                .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), corporate.getRight())
                                    .then()
                                    .statusCode(SC_OK)
                                    .body("factors[0].type", equalTo("OTP"))
                                    .body("factors[0].channel", equalTo("SMS"))
                                    .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
    }

    @Test
    public void EnrolUser_AlreadyEnrolledAndVerified_ChannelAlreadyRegistered() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, corporate.getRight())
                                    .then()
                                    .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), corporate.getRight())
                                    .then()
                                    .statusCode(SC_OK)
                                    .body("factors[0].type", equalTo("OTP"))
                                    .body("factors[0].channel", equalTo("SMS"))
                                    .body("factors[0].status", equalTo("PENDING_VERIFICATION"));

        AuthenticationFactorsService.verifyEnrolment(new VerificationModel("123456"), EnrolmentChannel.SMS.name(), secretKey, corporate.getRight())
                                    .then()
                                    .statusCode(SC_NO_CONTENT);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), corporate.getRight())
                                    .then()
                                    .statusCode(SC_OK)
                                    .body("factors[0].type", equalTo("OTP"))
                                    .body("factors[0].channel", equalTo("SMS"))
                                    .body("factors[0].status", equalTo("ACTIVE"));

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, corporate.getRight())
                                    .then()
                                    .statusCode(SC_CONFLICT)
                                    .body("errorCode", equalTo("CHANNEL_ALREADY_REGISTERED"));
    }

    @ParameterizedTest
    @EnumSource(value = EnrolmentChannel.class, names = { "EMAIL", "UNKNOWN" })
    public void EnrolUser_UnknownChannel_BadRequest(final EnrolmentChannel enrolmentChannel) {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationFactorsService.enrolOtp(enrolmentChannel.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void EnrolUser_NoChannel_NotFound() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        AuthenticationFactorsService.enrolOtp("", secretKey, corporate.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void EnrolUser_EmptyChannelValue_NotFound() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        AuthenticationFactorsService.enrolOtp("", secretKey, corporate.getRight())
                .then()
                .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void EnrolUser_DifferentInnovatorApiKey_Forbidden() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        final Triple<String, String, String> innovator =
                TestHelper.registerLoggedInInnovatorWithProgramme();

        final String otherInnovatorSecretKey =
                InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), otherInnovatorSecretKey, corporate.getRight())
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void EnrolUser_UserLoggedOut_Unauthorised() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationService.logout(secretKey, consumer.getRight());

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey, consumer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void EnrolUser_InvalidApiKey_Unauthorised() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), "abc", consumer.getRight())
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void EnrolUser_NoApiKey_BadRequest() {

        final Pair<String, String> consumer = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey);

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), "", consumer.getRight())
                .then()
                .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void EnrolUser_BackofficeImpersonator_Forbidden() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), secretKey,
                getBackofficeImpersonateToken(corporate.getLeft(), IdentityType.CORPORATE))
                .then()
                .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void EnrolUser_OtpSms_NotSupportedByProfile_ChannelNotSupported() {

        final CreateCorporateProfileModel createCorporateProfileModel =
                CreateCorporateProfileModel.DefaultCreateCorporateProfileModel()
                        .setAccountInformationFactors(IdentityProfileAuthenticationModel.DefaultAccountInfoIdentityProfileAuthenticationScheme())
                        .setPaymentInitiationFactors(IdentityProfileAuthenticationModel.PaymentInitIdentityProfileAuthenticationScheme("EMAIL_OTP"))
                        .setBeneficiaryManagementFactors(IdentityProfileAuthenticationModel.BeneficiaryManagementIdentityProfileAuthenticationScheme("EMAIL_OTP"))
                        .build();

        final Pair<String, String> innovatorDetails =
                InnovatorHelper.createNewInnovatorWithCorporateProfile(createCorporateProfileModel);

        final Pair<String, String> corporate =
                CorporatesHelper.createAuthenticatedCorporate(innovatorDetails.getRight(), innovatorDetails.getLeft(), TestHelper.DEFAULT_PASSWORD);

        AuthenticationFactorsService.enrolOtp(EnrolmentChannel.SMS.name(), innovatorDetails.getLeft(), corporate.getRight())
                                    .then()
                                    .statusCode(SC_CONFLICT)
                                    .body("errorCode", equalTo("CHANNEL_NOT_SUPPORTED"));
    }
}
