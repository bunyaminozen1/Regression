package opc.junit.secure.biometric;

import commons.enums.State;
import io.restassured.response.Response;
import opc.enums.opc.EnrolmentChannel;
import opc.enums.opc.IdentityType;
import opc.junit.database.AuthSessionsDatabaseHelper;
import opc.junit.database.ConsumersDatabaseHelper;
import opc.junit.database.CorporatesDatabaseHelper;
import opc.junit.helpers.TestHelper;
import opc.junit.helpers.admin.AdminHelper;
import opc.junit.helpers.innovator.InnovatorHelper;
import opc.junit.helpers.multi.AuthenticationFactorsHelper;
import opc.junit.helpers.multi.AuthenticationHelper;
import opc.junit.helpers.multi.ConsumersHelper;
import opc.junit.helpers.multi.CorporatesHelper;
import opc.junit.helpers.multi.UsersHelper;
import opc.junit.helpers.simulator.SimulatorHelper;
import opc.models.mailhog.MailHogMessageResponse;
import opc.models.multi.users.UsersModel;
import opc.models.secure.EnrolBiometricModel;
import opc.models.secure.VerificationBiometricModel;
import opc.models.shared.ProgrammeDetailsModel;
import opc.models.testmodels.IdentityDetails;
import opc.services.mailhog.MailhogService;
import opc.services.multi.AuthenticationFactorsService;
import opc.services.multi.ManagedCardsService;
import opc.services.secure.SecureService;
import opc.services.simulator.SimulatorService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static opc.enums.mailhog.MailHogSms.SCA_NEW_ENROLMENT_ALERT;
import static opc.services.admin.AdminService.loginAdmin;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public abstract class AbstractEnrolBiometricTests extends BaseBiometricSetup {

    @Test
    public void Biometric_EnrolRootUserPendingVerification_Success() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        issueEnrolChallenge(identity.getToken(), enrolBiometricModel);

        assertAuthFactorsState(identity.getToken(), State.PENDING_VERIFICATION.name());
        verifyEnrollingDeviceId(identity.getId(), enrolBiometricModel.getDeviceId(), Optional.empty());
    }

    @Test
    public void Biometric_EnrolAuthenticatedUserPendingVerification_Success() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, identity.getToken());

        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(user.getRight(), sharedKey);

        issueEnrolChallenge(user.getRight(), enrolBiometricModel);

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), user.getRight()).then();

        assertAuthFactorsState(user.getRight(), State.PENDING_VERIFICATION.name());
        verifyEnrollingDeviceId(user.getLeft(), enrolBiometricModel.getDeviceId(), Optional.empty());
    }

    /**
     * This test is checking if new enrolment with the same device is successful after first enrolment. Also  it confirms
     * that an alert sms is not sent to identity after new enrolment since identity mobile is not verified
     */
    @Test
    public void EnrolUser_EnrolAgainAfterRootUserVerified_Success() throws SQLException {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        //First enrolment
        final String linkingCode = issueEnrolChallenge(identity.getToken(), enrolBiometricModel);
        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), linkingCode);

        assertAuthFactorsState(identity.getToken(), State.ACTIVE.name());

        //Second enrolment
        final String newLinkingCode = issueEnrolChallenge(identity.getToken(), enrolBiometricModel);
        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), newLinkingCode);

        verifyDeviceId(identity.getId(), enrolBiometricModel.getDeviceId(), Optional.empty());

        //Since identity mobile is not verified, there shouldn't be sent any sms to identity
        final Map<Integer, Map<String, String>> smsAudit = identity.getIdentityType().equals(IdentityType.CONSUMER)
                ? ConsumersDatabaseHelper.getConsumerSmsNewestToOldest(identity.getId())
                : CorporatesDatabaseHelper.getCorporateSmsNewestToOldest(identity.getId());

        assertNull(smsAudit.get(0));
    }

    /**
     * This test is checking if new enrolment with the same device is successful after first enrolment. Also  it confirms
     * that an alert sms is not sent to user after new enrolment since user mobile is not verified
     */
    @Test
    public void EnrolUser_EnrolAgainAfterAuthenticatedUserVerified_Success() throws SQLException {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final UsersModel usersModel = UsersModel.DefaultUsersModel().build();
        final Pair<String, String> user = UsersHelper.createAuthenticatedUser(usersModel, secretKey, identity.getToken());

        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(user.getRight(), sharedKey);

        //First enrolment
        final String linkingCode = issueEnrolChallenge(user.getRight(), enrolBiometricModel);
        SimulatorHelper.simulateEnrolmentLinking(secretKey, user.getLeft(), linkingCode);

        assertAuthFactorsState(user.getRight(), "ACTIVE");

        //Second enrolment
        final String newLinkingCode = issueEnrolChallenge(user.getRight(), enrolBiometricModel);
        SimulatorHelper.simulateEnrolmentLinking(secretKey, user.getLeft(), newLinkingCode);

        verifyDeviceId(user.getLeft(), enrolBiometricModel.getDeviceId(), Optional.empty());

        //Since identity mobile is not verified, there shouldn't be sent any sms to identity
        final Map<Integer, Map<String, String>> smsAudit = identity.getIdentityType().equals(IdentityType.CONSUMER)
                ? ConsumersDatabaseHelper.getConsumerSmsNewestToOldest(user.getLeft())
                : CorporatesDatabaseHelper.getCorporateSmsNewestToOldest(user.getLeft());

        assertNull(smsAudit.get(0));
    }

    @Test
    public void EnrolUser_EnrolWithAnotherUserRegisteredDevice_Success() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        final String linkingCode = issueEnrolChallenge(identity.getToken(), enrolBiometricModel);

        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), linkingCode);

        assertAuthFactorsState(identity.getToken(), State.ACTIVE.name());

        final IdentityDetails newIdentity = getIdentity(passcodeApp);

        final String newIdentityRandom = getRandom(newIdentity.getToken(), sharedKey);

        final EnrolBiometricModel newEnrolBiometricModel = EnrolBiometricModel.builder()
                .random(newIdentityRandom)
                .deviceId(enrolBiometricModel.getDeviceId())
                .build();

        SecureService.enrolDeviceBiometric(newIdentity.getToken(), sharedKey, newEnrolBiometricModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("DEVICE_ALREADY_REGISTERED"));

        verifyDeviceId(identity.getId(), enrolBiometricModel.getDeviceId(), Optional.empty());
    }

    //TODO Removed until DEV-4446(Remove the deviceid when unlinking a device) is completed
    @Test
    public void Biometric_EnrolDeviceAgainAfterUnlinkDevice_Success() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        final String linkingCode = issueEnrolChallenge(identity.getToken(), enrolBiometricModel);

        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), linkingCode);

        assertAuthFactorsState(identity.getToken(), "ACTIVE");

        SimulatorHelper.simulateEnrolmentUnlinking(secretKey, identity.getId(), linkingCode);

        verifyDeviceId(identity.getId(), null, Optional.empty());

        SecureService.enrolDeviceBiometric(identity.getToken(), sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_OK)
                .body("linkingCode", notNullValue());
    }

    @Test
    public void Biometric_EnrolDeviceAgainAfterReject_Success() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        final String linkingCode = issueEnrolChallenge(identity.getToken(), enrolBiometricModel);

        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), linkingCode);

        assertAuthFactorsState(identity.getToken(), "ACTIVE");

        SimulatorService.rejectOkayIdentity(secretKey, identity.getId())
                .then()
                .statusCode(SC_NO_CONTENT);

        assertAuthFactorsState(identity.getToken(), "INACTIVE");

        verifyDeviceId(identity.getId(), null, Optional.empty());

        SecureService.enrolDeviceBiometric(identity.getToken(), sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_OK)
                .body("linkingCode", notNullValue());
    }

    //TODO Need to create a bug for secure api calls with invalid shared key. They return 500
    //  @Test
    //  public void EnrolUser_DifferentInnovatorSharedKey_Forbidden() {
    //
    //    final Triple<String, String, String> innovator =
    //        TestHelper.registerLoggedInInnovatorWithProgramme();
    //
    //    final String newSharedKey =
    //        InnovatorService.getProgrammeDetails(innovator.getRight(), innovator.getMiddle()).jsonPath().get("secretKey");
    //
    //    final String random = SecureService.associate(sharedKey, identity.getToken())
    //        .then()
    //        .statusCode(SC_OK)
    //        .extract().jsonPath().getString("random");
    //
    //    final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
    //        .random(random)
    //        .deviceId(RandomStringUtils.randomAlphanumeric(40))
    //        .build();
    //
    //    SecureService.enrolDeviceBiometric(identity.getToken(), newSharedKey, enrolBiometricModel)
    //        .then()
    //        .statusCode(SC_FORBIDDEN);
    //  }
    //  @Test
    //  public void EnrolUser_InvalidSharedKey_Forbidden() {
    //
    //    final String random = SecureService.associate(sharedKey, identity.getToken())
    //        .then()
    //        .statusCode(SC_OK)
    //        .extract().jsonPath().getString("random");
    //
    //    final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
    //        .random(random)
    //        .deviceId(RandomStringUtils.randomAlphanumeric(40))
    //        .build();
    //
    //    SecureService.enrolDeviceBiometric(identity.getToken(), RandomStringUtils.randomAlphanumeric(8), enrolBiometricModel)
    //        .then()
    //        .statusCode(SC_FORBIDDEN);
    //  }
    @Test
    public void Biometric_EnrolUserWithoutSharedKey_RequiredProgrammeKey() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        SecureService.enrolDeviceBiometric(identity.getToken(), "", enrolBiometricModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("invalidFields[0].error", equalTo("REQUIRED"))
                .body("invalidFields[0].fieldName", equalTo("programme-key"));
    }

    @Test
    public void Biometric_EnrolUserWithoutAuthenticationToken_Unauthorized() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        SecureService.enrolDeviceBiometric("", sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Biometric_EnrolUserInvalidAuthenticationToken_Unauthorized() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        SecureService.enrolDeviceBiometric(RandomStringUtils.randomAlphanumeric(8), sharedKey,
                        enrolBiometricModel)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Biometric_EnrolUserLoggedOut_Unauthorized() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        AuthenticationHelper.logout(identity.getToken(), secretKey);

        SecureService.enrolDeviceBiometric(identity.getToken(), sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Biometric_EnrolUserCrossIdentityToken_Unauthorized() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        final String newIdentityToken;

        if (identity.getIdentityType().equals(IdentityType.CORPORATE)) {
            newIdentityToken = ConsumersHelper.createAuthenticatedConsumer(consumerProfileId, secretKey)
                    .getRight();
        } else {
            newIdentityToken = CorporatesHelper.createAuthenticatedCorporate(corporateProfileId,
                    secretKey).getRight();
        }

        SecureService.enrolDeviceBiometric(newIdentityToken, sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Biometric_EnrolUserNoRandomRequest_RequiredRandom() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        SecureService.enrolDeviceBiometric(identity.getToken(), sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validation.fields[0].name", equalTo("random"))
                .body("validation.fields[0].errors[0].type", equalTo("REQUIRED"));
    }

    @Test
    public void Biometric_EnrolUserNoDeviceId_RequiredDeviceId() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final String random = getRandom(identity.getToken(), sharedKey);
        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(random)
                .build();

        SecureService.enrolDeviceBiometric(identity.getToken(), sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_BAD_REQUEST)
                .body("validation.fields[0].name", equalTo("deviceId"))
                .body("validation.fields[0].errors[0].type", equalTo("REQUIRED"));
    }


    @Test
    public void Biometric_EnrolUserInvalidRandomRequest_Unauthorized() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = EnrolBiometricModel.builder()
                .random(RandomStringUtils.randomAlphanumeric(5))
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();

        SecureService.enrolDeviceBiometric(identity.getToken(), sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void EnrolUser_BackofficeImpersonator_Forbidden() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        SecureService.enrolDeviceBiometric(
                        getBackofficeImpersonateToken(identity.getId(), identity.getIdentityType()), sharedKey,
                        enrolBiometricModel)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Biometric_EnrolUserWithoutSettingPasscode_PasscodeNotSet() {

        //Enable biometric for programme level
        InnovatorHelper.enableOkay(applicationThree.getProgrammeId(), innovatorToken);

        final IdentityDetails identity = getIdentity(applicationThree);

        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), applicationThree.getSharedKey());

        SecureService.enrolDeviceBiometric(identity.getToken(), applicationThree.getSharedKey(), enrolBiometricModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("PASSCODE_NOT_SET"));

        InnovatorHelper.disableOkay(applicationThree.getProgrammeId(), innovatorToken);
    }

    @Test
    public void Biometric_ProgrammeNotSupportBiometricFor_ChannelNotSupported() {

        //Enable biometric for programme level
        InnovatorHelper.enableOkay(applicationFour.getProgrammeId(), innovatorToken);

        final IdentityDetails identity = getIdentity(applicationFour);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), applicationFour.getSharedKey());

        SecureService.enrolDeviceBiometric(identity.getToken(), applicationFour.getSharedKey(), enrolBiometricModel)
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("CHANNEL_NOT_SUPPORTED"));

        //Disable biometric for programme level
        InnovatorHelper.disableOkay(applicationFour.getProgrammeId(), innovatorToken);
    }

    @Test
    public void Biometric_DisabledForProgrammeLevel_ProgrammeNotEnrolled() {

        //Using applicationTwo that biometric is disabled for it
        final IdentityDetails identity = getIdentity(applicationTwo);

        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), applicationTwo.getSharedKey());

        SecureService.enrolDeviceBiometric(identity.getToken(), applicationTwo.getSharedKey(), enrolBiometricModel)
            .then()
            .statusCode(SC_CONFLICT)
            .body("errorCode", equalTo("PROGRAMME_NOT_ENROLLED"));
    }

    @Test
    public void EnrolUser_ReturnStepUpTokenAfterEnrolment_Success() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        AdminHelper.setSca(loginAdmin(), programmeId, true, true);

        ManagedCardsService.getManagedCards(secretKey, Optional.empty(), identity.getToken())
                .then()
                .statusCode(SC_FORBIDDEN)
                .body("errorCode", equalTo("STEP_UP_REQUIRED"));


        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        final String linkingCode = issueEnrolChallenge(identity.getToken(), enrolBiometricModel);

        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), linkingCode);

        assertAuthFactorsState(identity.getToken(), State.ACTIVE.name());

        ManagedCardsService.getManagedCards(secretKey, Optional.empty(), identity.getToken())
                .then()
                .statusCode(SC_OK)
                .body("count", equalTo(0))
                .body("responseCount", equalTo(0));

        AdminHelper.setSca(loginAdmin(), programmeId, false, false);
    }

    /**
     * This test confirms that an alert sms is sent to identity from second enrolment with the same device id
     * since identity mobile is verified
     */

    @Test
    public void EnrolUser_RootUserSendSmsFromSecondEnrolmentWithSameDevice_Success() {

        final IdentityDetails identity = getIdentity(passcodeApp);

        //In order to get sms, identity mobile should be verified
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, identity.getToken());

        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        //First enrolment to auth factor
        final String linkingCode = issueEnrolChallenge(identity.getToken(), enrolBiometricModel);
        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), linkingCode);

        assertAuthFactorsState(identity.getToken(), State.ACTIVE.name());

        //Second enrolment, an alert sms should be sent to identity mobile
        final String secondLinkingCode = issueEnrolChallenge(identity.getToken(), enrolBiometricModel);
        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), secondLinkingCode);

        final MailHogMessageResponse sms = getMailHogSms(getIdentityMobileNumber());
        assertEquals(SCA_NEW_ENROLMENT_ALERT.getFrom(), sms.getFrom());
        assertEquals(String.format("%s%s@weavr.io", getIdentityMobileCountryCode(), getIdentityMobileNumber()), sms.getTo());
        assertEquals(getSmsAudit(identity.getId()), sms.getBody());

        //Third enrolment, an alert sms should be sent to identity mobile
        final String thirdLinkingCode = issueEnrolChallenge(identity.getToken(), enrolBiometricModel);
        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), thirdLinkingCode);

        final MailHogMessageResponse secondSms = getMailHogSms(getIdentityMobileNumber());
        assertEquals(SCA_NEW_ENROLMENT_ALERT.getFrom(), secondSms.getFrom());
        assertEquals(String.format("%s%s@weavr.io", getIdentityMobileCountryCode(), getIdentityMobileNumber()), secondSms.getTo());
        assertEquals(getSmsAudit(identity.getId()), secondSms.getBody());
    }

    /**
     * This test confirms that an alert sms is sent to identity from second enrolment with the new device id
     * since identity mobile is verified
     */
    @Test
    public void EnrolUser_RootUserSendSmsFromSecondEnrolment_Success() {

        final IdentityDetails identity = getIdentity(passcodeApp);

        //In order to get sms, identity mobile should be verified
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, identity.getToken());

        //First enrolment to auth factor
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);
        final String linkingCode = issueEnrolChallenge(identity.getToken(), enrolBiometricModel);
        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), linkingCode);
        assertAuthFactorsState(identity.getToken(), State.ACTIVE.name());

        //Second enrolment, an alert sms should be sent to identity mobile
        final EnrolBiometricModel secondEnrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);
        final String secondLinkingCode = issueEnrolChallenge(identity.getToken(), secondEnrolBiometricModel);
        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), secondLinkingCode);

        final MailHogMessageResponse sms = getMailHogSms(getIdentityMobileNumber());
        assertEquals(SCA_NEW_ENROLMENT_ALERT.getFrom(), sms.getFrom());
        assertEquals(String.format("%s%s@weavr.io", getIdentityMobileCountryCode(), getIdentityMobileNumber()), sms.getTo());
        assertEquals(getSmsAudit(identity.getId()), sms.getBody());

        //Third enrolment, an alert sms should be sent to identity mobile
        final EnrolBiometricModel thirdEnrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);
        final String thirdLinkingCode = issueEnrolChallenge(identity.getToken(), thirdEnrolBiometricModel);
        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), thirdLinkingCode);

        final MailHogMessageResponse secondSms = getMailHogSms(getIdentityMobileNumber());
        assertEquals(SCA_NEW_ENROLMENT_ALERT.getFrom(), secondSms.getFrom());
        assertEquals(String.format("%s%s@weavr.io", getIdentityMobileCountryCode(), getIdentityMobileNumber()), secondSms.getTo());
        assertEquals(getSmsAudit(identity.getId()), secondSms.getBody());
    }

    /**
     * DEV-6285 this story allow user request SMS OTP challenge for biometrics enrolment
     */

    @Test
    public void Biometric_EnrolRootUserOtpNotVerifiedEnrollmentWithOtpPendingVerification_Success() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        SecureService.enrolDeviceBiometricWithOtp(identity.getToken(), sharedKey, enrolBiometricModel)
                 .then()
                .statusCode(SC_OK);

        final VerificationBiometricModel verificationBiometricModel = VerificationBiometricModel.builder()
                        .verificationCode(TestHelper.OTP_VERIFICATION_CODE)
                        .random(enrolBiometricModel.getRandom()).build();

        SecureService.verifyBiometricEnrolmentWithOtp(identity.getToken(), sharedKey,
                verificationBiometricModel)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .getString("linkingCode");

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), identity.getToken());

        assertAuthFactorsState(identity.getToken(), State.PENDING_VERIFICATION.name());
        verifyEnrollingDeviceId(identity.getId(), enrolBiometricModel.getDeviceId(), Optional.empty());
    }

    @Test
    public void Biometric_EnrolRootUserOtpNotVerifiedEnrollmentWithOtpWrongVerificationCode_Conflict() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        SecureService.enrolDeviceBiometricWithOtp(identity.getToken(), sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_OK);

        final VerificationBiometricModel verificationBiometricModel = VerificationBiometricModel.builder()
                .verificationCode(RandomStringUtils.randomAlphanumeric(6))
                .random(enrolBiometricModel.getRandom()).build();

        SecureService.verifyBiometricEnrolmentWithOtp(identity.getToken(), sharedKey,
                        verificationBiometricModel)
                .then()
                .statusCode(SC_CONFLICT)
                .body("errorCode", equalTo("VERIFICATION_FAILED"));
    }

    @Test
    public void Biometric_EnrolRootUserOtpNotVerifiedEnrollmentWithOtpWrongRandom_Unauthorised() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        SecureService.enrolDeviceBiometricWithOtp(identity.getToken(), sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_OK);

        final VerificationBiometricModel verificationBiometricModel = VerificationBiometricModel.builder()
                .verificationCode(TestHelper.OTP_VERIFICATION_CODE)
                .random(RandomStringUtils.randomAlphanumeric(7)).build();

        SecureService.verifyBiometricEnrolmentWithOtp(identity.getToken(), sharedKey,
                        verificationBiometricModel)
                .then()
                .statusCode(SC_UNAUTHORIZED);
    }

    @Test
    public void Biometric_EnrolRootUserOtpVerifiedEnrollmentWithOtpPendingVerification_Success() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        AuthenticationFactorsHelper.enrolAndVerifyOtp(TestHelper.OTP_VERIFICATION_CODE, EnrolmentChannel.SMS.name(), secretKey, identity.getToken());

        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        SecureService.enrolDeviceBiometricWithOtp(identity.getToken(), sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_OK);

        final VerificationBiometricModel verificationBiometricModel = VerificationBiometricModel.builder()
                .verificationCode(TestHelper.OTP_VERIFICATION_CODE)
                .random(enrolBiometricModel.getRandom()).build();

        SecureService.verifyBiometricEnrolmentWithOtp(identity.getToken(), sharedKey,
                        verificationBiometricModel)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .getString("linkingCode");

        AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), identity.getToken())
                        .then()
                        .statusCode(SC_OK)
                        .body("factors[0].type", equalTo("OTP"))
                        .body("factors[0].channel", equalTo("SMS"))
                        .body("factors[0].status", equalTo("ACTIVE"))
                        .body("factors[1].type", equalTo("PUSH"))
                        .body("factors[1].channel", equalTo("BIOMETRIC"))
                        .body("factors[1].status", equalTo("PENDING_VERIFICATION"));

        verifyEnrollingDeviceId(identity.getId(), enrolBiometricModel.getDeviceId(), Optional.empty());
    }

    /**
     * This test is checking if new enrolment with a different device being updated correctly.
     */
    @Test
    public void EnrolUser_EnrolAgainWithDifferentDevice_Success() {

        final IdentityDetails identity = getIdentity(passcodeApp);
        final EnrolBiometricModel enrolBiometricModel = getEnrolBiometricModel(identity.getToken(), sharedKey);

        //First enrolment
        final String linkingCode = issueEnrolChallenge(identity.getToken(), enrolBiometricModel);
        verifyEnrollingDeviceId(identity.getId(), enrolBiometricModel.getDeviceId(), Optional.empty());
        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), linkingCode);
        verifyDeviceId(identity.getId(), enrolBiometricModel.getDeviceId(), Optional.empty());

        assertAuthFactorsState(identity.getToken(), State.ACTIVE.name());

        //Second enrolment
        final EnrolBiometricModel enrolBiometricModel1 = getEnrolBiometricModel(identity.getToken(), sharedKey);
        final String newLinkingCode = issueEnrolChallenge(identity.getToken(), enrolBiometricModel1);
        // At this point the device id will still be the old one, but the enrolling device id is the new device
        verifyEnrollingDeviceId(identity.getId(), enrolBiometricModel1.getDeviceId(), Optional.of(enrolBiometricModel.getDeviceId()));
        SimulatorHelper.simulateEnrolmentLinking(secretKey, identity.getId(), newLinkingCode);

        verifyDeviceId(identity.getId(), enrolBiometricModel1.getDeviceId(), Optional.empty());
    }


    protected EnrolBiometricModel getEnrolBiometricModel(final String identityToken, final String sharedKey){

        final String random = getRandom(identityToken, sharedKey);

        return EnrolBiometricModel.builder()
                .random(random)
                .deviceId(RandomStringUtils.randomAlphanumeric(40))
                .build();
    }

    /**
     * This method calls /app/secure/api/session/associate endpoint
     *
     * @param token (identity token)
     * @return a random number
     */
    protected String getRandom(final String token, final String sharedKey) {

        return SecureService.associate(sharedKey, token, Optional.empty())
                .then()
                .statusCode(SC_OK)
                .extract().jsonPath().getString("random");
    }

    /**
     * This method calls /app/secure/api/session/authentication_factors/biometric/setup endpoint to
     * enrol a device
     *
     * @return linkingCode
     */
    protected String issueEnrolChallenge(final String token,
                                       final EnrolBiometricModel enrolBiometricModel) {
        return SecureService.enrolDeviceBiometric(token, sharedKey, enrolBiometricModel)
                .then()
                .statusCode(SC_OK)
                .extract()
                .jsonPath()
                .getString("linkingCode");
    }

    /**
     * This method asserts authentication factors status of an identity
     *
     * @param identityToken  is identity/user token
     * @param expectedStatus is expected status
     */
    protected void assertAuthFactorsState(final String identityToken,
                                        final String expectedStatus) {

        TestHelper.ensureAsExpected(15,
                () -> AuthenticationFactorsService.getAuthenticationFactors(secretKey, Optional.empty(), identityToken),
                x -> x.statusCode() == SC_OK && x.jsonPath().get("factors[0].status").equals(expectedStatus),
                Optional.of(String.format("Expecting 200 with an authentication factor in state %s, check logged payload", expectedStatus)));
    }

    /**
     * This method check if device id in DB matches with device id provided by user device
     */
    public static void verifyDeviceId(final String identityId, final String deviceId, final Optional<String> enrollingDeviceId) {

        TestHelper.ensureDatabaseResultAsExpected(10,
                () -> AuthSessionsDatabaseHelper.getBiometricFactor(identityId),
                x -> Objects.equals(x.get(0).get("device_id"), deviceId) && Objects.equals(x.get(0).get("enrolling_device_id"), enrollingDeviceId.orElse(null)),
                Optional.of(String.format("Retrieved device id does not match device with id %s as expected", deviceId)));
    }

    /**
     * This method check if the enrolling device id in DB matches with device id provided by user device
     */
    public static void verifyEnrollingDeviceId(final String identityId, final String enrollingDeviceId, final Optional<String> deviceId) {

        TestHelper.ensureDatabaseResultAsExpected(10,
                () -> AuthSessionsDatabaseHelper.getBiometricFactor(identityId),
                x -> Objects.equals(x.get(0).get("enrolling_device_id"), enrollingDeviceId) && Objects.equals(x.get(0).get("device_id"), deviceId.orElse(null)),
                Optional.of(String.format("Retrieved device id does not match device with id %s as expected", enrollingDeviceId)));
    }


    public static MailHogMessageResponse getMailHogSms(final String mobileNumber){

        final Response response = TestHelper.ensureAsExpected(15,
                () -> MailhogService.getMailHogSms(mobileNumber),
                x -> x.statusCode() == SC_OK && x.jsonPath().getString("items[0].Content.Body").equals(SCA_NEW_ENROLMENT_ALERT.getSmsText()),
                Optional.of(String.format("Text is not %s, see logged payload", SCA_NEW_ENROLMENT_ALERT.getSmsText())));

        return MailHogMessageResponse.convertSms(response);

    }

    protected abstract IdentityDetails getIdentity(final ProgrammeDetailsModel programme);
    protected abstract String getIdentityMobileNumber();
    protected abstract String getIdentityMobileCountryCode();
    protected abstract String getSmsAudit(final String identityId);
}


