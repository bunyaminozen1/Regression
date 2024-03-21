package opc.junit.smoke;

import opc.enums.mailhog.MailHogSms;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.LimitInterval;
import commons.enums.State;
import opc.junit.database.AuthFactorsSimulatorDatabaseHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.mailhog.MailhogHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.corporates.CreateCorporateModel;
import opc.models.shared.VerificationModel;
import opc.services.admin.AdminService;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.multi.ConsumersService;
import opc.services.multi.CorporatesService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthenticationFactorsTests extends BaseSmokeSetup {
    private static String existingConsumerAuthenticationToken;
    private final static EnrolmentChannel enrolmentChannel = EnrolmentChannel.AUTHY;
    private final static String VERIFICATION_CODE = "123456";

    @BeforeAll
    public static void Setup() {

        existingConsumerAuthenticationToken = getExistingConsumerDetailsPasscodeApp().getLeft();

        final String adminToken = AdminService.loginAdmin();
        final Map<LimitInterval, Integer> resetCount = ImmutableMap.of(LimitInterval.ALWAYS, 100000);

        AdminHelper.resetProgrammeAuthyLimitsCounter(passcodeApp.getProgrammeId(), adminToken);
        AdminHelper.setProgrammeAuthyChallengeLimit(passcodeApp.getProgrammeId(), resetCount, adminToken);

    }

    @Test
    public void EnrolDeviseByOtpUser_CorporateRoot_Success() throws SQLException {

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
    public void EnrolDeviseByPushUser_CorporateRoot_Success() {

        final CreateCorporateModel createCorporateModel = CreateCorporateModel.DefaultCreateCorporateModel(
                passcodeAppCorporateProfileId).build();
        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(
                createCorporateModel, passcodeAppSecretKey);

        AuthenticationFactorsService.enrolPush(enrolmentChannel.name(), passcodeAppSecretKey, corporate.getRight());

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
                        corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo(enrolmentChannel.name()))
                .body("factors[0].status", equalTo("PENDING_VERIFICATION"));
    }

    @Test
    public void GetAuthFactors_ExistingConsumerPush_Success() {
        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(),
                        existingConsumerAuthenticationToken)
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("BIOMETRIC"))
                .body("factors[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void VerifyEnrolment_AcceptCorporate_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        AuthenticationFactorsHelper.enrolAuthyPushUser(passcodeAppSecretKey, corporate.getRight());

        SimulatorHelper.acceptAuthyIdentity(passcodeAppSecretKey, corporate.getLeft(), corporate.getRight(), State.ACTIVE);

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("ACTIVE"));
    }

    @Test
    public void VerifyEnrolment_RejectCorporate_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(passcodeAppCorporateProfileId, passcodeAppSecretKey);
        AuthenticationFactorsHelper.enrolAuthyPushUser(passcodeAppSecretKey, corporate.getRight());

        SimulatorHelper.rejectAuthyIdentity(passcodeAppSecretKey, corporate.getLeft(), corporate.getRight(), State.INACTIVE);

        AuthenticationFactorsService.getAuthenticationFactors(passcodeAppSecretKey, Optional.empty(), corporate.getRight())
                .then()
                .statusCode(SC_OK)
                .body("factors[0].type", equalTo("PUSH"))
                .body("factors[0].channel", equalTo("AUTHY"))
                .body("factors[0].status", equalTo("INACTIVE"));
    }

    @Test
    public void VerifyEnrolment_Corporate_Success() {

        final Pair<String, String> corporate = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId, secretKey);
        AuthenticationFactorsHelper.enrolOtpUser(EnrolmentChannel.SMS.name(), secretKey, corporate.getRight());

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
        AuthenticationFactorsHelper.enrolOtpUser(EnrolmentChannel.SMS.name(), secretKey, consumer.getRight());

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

}
